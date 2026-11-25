package com.example.ui.coach

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.notifications.AerisNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * GameCoachService (Stage 7A):
 * Foreground Service under FOREGROUND_SERVICE_MEDIA_PROJECTION.
 * Implements Android 14+ MediaProjection.Callback and periodic downscaled VirtualDisplay + ImageReader
 * frame sampling (~every 4 seconds) for consented Gemini Vision coaching.
 * Zero cheats, zero automation, immediate teardown on exit.
 */
class GameCoachService : Service() {

    companion object {
        const val ACTION_START = "com.example.coach.START"
        const val ACTION_STOP = "com.example.coach.STOP"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        private val _isObserving = MutableStateFlow(false)
        val isObserving: StateFlow<Boolean> = _isObserving.asStateFlow()

        private val _observedGameTimeSeconds = MutableStateFlow(0)
        val observedGameTimeSeconds: StateFlow<Int> = _observedGameTimeSeconds.asStateFlow()

        private val _latestFrame = MutableStateFlow<Bitmap?>(null)
        val latestFrame: StateFlow<Bitmap?> = _latestFrame.asStateFlow()

        private val _captureStatus = MutableStateFlow("Standby")
        val captureStatus: StateFlow<String> = _captureStatus.asStateFlow()

        private var mediaProjection: MediaProjection? = null
    }

    private var timerJob: Job? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var lastFrameCapturedAt = 0L

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            stopObservation()
            stopForegroundCompat()
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopObservation()
                stopForegroundCompat()
                stopSelf()
            }
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            AerisNotificationManager.NOTIFICATION_ID_GAME_COACH,
                            buildCoachNotification(),
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                        )
                    } else {
                        startForeground(
                            AerisNotificationManager.NOTIFICATION_ID_GAME_COACH,
                            buildCoachNotification()
                        )
                    }
                } catch (e: Exception) {
                    _captureStatus.value = "Foreground MediaProjection failed: ${e.message}"
                    _isObserving.value = false
                    stopSelf()
                    return START_NOT_STICKY
                }

                if (resultCode != 0 && resultData != null) {
                    val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    mediaProjection = mpm.getMediaProjection(resultCode, resultData)?.apply {
                        registerCallback(projectionCallback, mainHandler)
                    }
                    setupVirtualDisplay()
                    _captureStatus.value = "Consented screen frame capture active (periodic sampling)"
                } else {
                    _captureStatus.value = "Screen capture permission payload missing"
                }

                _isObserving.value = true
                startObservingTimer()
            }
        }
        return START_NOT_STICKY
    }

    private fun setupVirtualDisplay() {
        val projection = mediaProjection ?: return
        try {
            val metrics = DisplayMetrics()
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            @Suppress("DEPRECATION")
            wm.defaultDisplay?.getRealMetrics(metrics)
            val width = 480
            val aspect = if (metrics.widthPixels > 0) {
                metrics.heightPixels.toFloat() / metrics.widthPixels.toFloat()
            } else 1.77f
            val height = (width * aspect).toInt().coerceIn(360, 1080)
            val density = metrics.densityDpi.takeIf { it > 0 } ?: DisplayMetrics.DENSITY_HIGH

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2).apply {
                setOnImageAvailableListener({ reader ->
                    val image = try { reader.acquireLatestImage() } catch (_: Exception) { null } ?: return@setOnImageAvailableListener
                    try {
                        val now = System.currentTimeMillis()
                        if (now - lastFrameCapturedAt >= 3500L) {
                            lastFrameCapturedAt = now
                            val plane = image.planes.firstOrNull() ?: return@setOnImageAvailableListener
                            val buffer = plane.buffer
                            val pixelStride = plane.pixelStride
                            val rowStride = plane.rowStride
                            val rowPadding = rowStride - pixelStride * image.width
                            val rawBitmap = Bitmap.createBitmap(
                                image.width + rowPadding / pixelStride,
                                image.height,
                                Bitmap.Config.ARGB_8888
                            )
                            rawBitmap.copyPixelsFromBuffer(buffer)
                            val cropped = Bitmap.createBitmap(rawBitmap, 0, 0, image.width, image.height)
                            if (rawBitmap !== cropped) rawBitmap.recycle()
                            _latestFrame.value = cropped
                        }
                    } catch (_: Exception) {
                    } finally {
                        image.close()
                    }
                }, mainHandler)
            }

            virtualDisplay = projection.createVirtualDisplay(
                "AerisGameCoachDisplay",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                mainHandler
            )
        } catch (e: Exception) {
            _captureStatus.value = "VirtualDisplay unavailable: ${e.message}"
        }
    }

    private fun startObservingTimer() {
        timerJob?.cancel()
        _observedGameTimeSeconds.value = 0
        timerJob = serviceScope.launch {
            while (_isObserving.value) {
                delay(1000)
                _observedGameTimeSeconds.value += 1
            }
        }
    }

    private fun stopObservation() {
        _isObserving.value = false
        _captureStatus.value = "Stopped"
        _latestFrame.value = null
        timerJob?.cancel()
        timerJob = null
        try {
            virtualDisplay?.release()
        } catch (_: Exception) {}
        virtualDisplay = null
        try {
            imageReader?.close()
        } catch (_: Exception) {}
        imageReader = null
        try {
            mediaProjection?.unregisterCallback(projectionCallback)
            mediaProjection?.stop()
        } catch (_: Exception) {}
        mediaProjection = null
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun buildCoachNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            2,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, GameCoachService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            3,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, AerisNotificationManager.CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.aeris_avatar_thumb)
            .setContentTitle("AERIS Game Coach Active")
            .setContentText("Consented screen observation active • Tap to open or Stop")
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.aeris_avatar_thumb, "Stop Coach", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopObservation()
        serviceScope.cancel()
    }
}
