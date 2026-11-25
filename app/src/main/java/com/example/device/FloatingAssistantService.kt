package com.example.device

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * Stage 7 User-Controlled Floating Assistant Orb (SYSTEM_ALERT_WINDOW).
 * - Requires explicit user opt-in and Android Overlay permission.
 * - Zero hidden screen reading and zero background mic capture.
 * - Immediately tears down all overlay views when closed or stopped.
 */
class FloatingAssistantService : Service() {

    private var windowManager: WindowManager? = null
    private var rootContainer: LinearLayout? = null
    private var actionPanel: LinearLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    companion object {
        const val ACTION_START = "com.example.device.FLOATING_START"
        const val ACTION_STOP = "com.example.device.FLOATING_STOP"
        const val EXTRA_TARGET_ROUTE = "aeris_launch_route"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun canDrawOverlays(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }

        fun start(context: Context): Boolean {
            if (!canDrawOverlays(context)) return false
            val intent = Intent(context, FloatingAssistantService::class.java).apply {
                action = ACTION_START
            }
            context.startService(intent)
            return true
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingAssistantService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
            _isRunning.value = false
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP || !canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (rootContainer == null) {
            attachFloatingOrb()
        }
        _isRunning.value = rootContainer != null
        return START_NOT_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachFloatingOrb() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        windowManager = wm

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 260
        }
        layoutParams = params

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }

        val orb = TextView(this).apply {
            text = "AERIS"
            textSize = 11f
            setTextColor(Color.parseColor("#00F0FF"))
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#E60B0F1A"))
                setStroke(4, Color.parseColor("#00F0FF"))
            }
        }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(20, 16, 20, 16)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f
                setColor(Color.parseColor("#F00E1424"))
                setStroke(2, Color.parseColor("#9B6DFF"))
            }
        }

        fun makeActionButton(label: String, onClick: () -> Unit): Button = Button(this).apply {
            text = label
            textSize = 11f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { onClick() }
        }

        panel.addView(makeActionButton("Open AERIS Chat") { launchMainRoute("chat") })
        panel.addView(makeActionButton("Voice Assistant") { launchMainRoute("voice_assistant") })
        panel.addView(makeActionButton("Hide Orb") {
            stop(this)
            stopSelf()
        })

        var downX = 0f
        var downY = 0f
        var startParamX = params.x
        var startParamY = params.y
        var moved = false

        orb.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startParamX = params.x
                    startParamY = params.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (downX - event.rawX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) moved = true
                    params.x = startParamX + dx
                    params.y = startParamY + dy
                    runCatching { wm.updateViewLayout(container, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        panel.visibility = if (panel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    }
                    true
                }
                else -> false
            }
        }

        container.addView(orb)
        container.addView(panel)
        actionPanel = panel

        try {
            wm.addView(container, params)
            rootContainer = container
        } catch (_: Exception) {
            rootContainer = null
            _isRunning.value = false
        }
    }

    private fun launchMainRoute(route: String) {
        actionPanel?.visibility = View.GONE
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TARGET_ROUTE, route)
        }
        startActivity(launchIntent)
    }

    override fun onDestroy() {
        rootContainer?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        rootContainer = null
        actionPanel = null
        _isRunning.value = false
        super.onDestroy()
    }
}
