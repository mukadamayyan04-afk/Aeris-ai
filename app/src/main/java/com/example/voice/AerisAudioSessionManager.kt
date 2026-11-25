package com.example.voice

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.domain.model.AudioSessionMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AerisAudioSessionManager:
 * Centralized audio focus, STT/TTS/WakeWord conflict prevention,
 * audio interruption handling (headphone unplug / phone call focus loss),
 * and hardware permission status.
 */
class AerisAudioSessionManager(private val context: Context) : AudioSessionManager {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _sessionMode = MutableStateFlow(AudioSessionMode.IDLE)
    val sessionMode: StateFlow<AudioSessionMode> = _sessionMode.asStateFlow()

    private val _hasAudioFocus = MutableStateFlow(false)
    val hasAudioFocus: StateFlow<Boolean> = _hasAudioFocus.asStateFlow()

    private val _interruptionReason = MutableStateFlow<String?>(null)
    val interruptionReason: StateFlow<String?> = _interruptionReason.asStateFlow()

    private var onAudioInterruptedCallback: ((String) -> Unit)? = null
    private var focusRequest: AudioFocusRequest? = null
    private var isReceiverRegistered = false

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                if (_sessionMode.value == AudioSessionMode.TTS_SPEAKING) {
                    _interruptionReason.value = "Audio output disconnected"
                    onAudioInterruptedCallback?.invoke("Audio output disconnected")
                }
            }
        }
    }

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                _hasAudioFocus.value = false
                if (_sessionMode.value == AudioSessionMode.TTS_SPEAKING ||
                    _sessionMode.value == AudioSessionMode.STT_LISTENING
                ) {
                    val reason = "Audio interrupted by another app or call"
                    _interruptionReason.value = reason
                    onAudioInterruptedCallback?.invoke(reason)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Transient ducking handled by OS for speech attributes
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                _hasAudioFocus.value = true
                _interruptionReason.value = null
            }
        }
    }

    init {
        registerNoisyReceiver()
    }

    private fun registerNoisyReceiver() {
        if (isReceiverRegistered) return
        try {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(becomingNoisyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(becomingNoisyReceiver, filter)
            }
            isReceiverRegistered = true
        } catch (_: Exception) {
            isReceiverRegistered = false
        }
    }

    fun setOnAudioInterruptedListener(listener: (String) -> Unit) {
        onAudioInterruptedCallback = listener
    }

    override val hasAudioRecordPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    val hasPostNotificationsPermission: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    override val isAudioOutputMuted: Boolean
        get() = try {
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
        } catch (_: Exception) {
            false
        }

    fun transitionSessionMode(newMode: AudioSessionMode) {
        _sessionMode.value = newMode
        if (newMode != AudioSessionMode.IDLE) {
            _interruptionReason.value = null
        }
    }

    override fun requestAudioFocusForPlayback(): Boolean {
        return try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attrs)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()
            focusRequest = request

            val result = audioManager.requestAudioFocus(request)
            val granted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            _hasAudioFocus.value = granted
            if (granted) {
                _sessionMode.value = AudioSessionMode.TTS_SPEAKING
            }
            granted
        } catch (_: Exception) {
            _hasAudioFocus.value = false
            false
        }
    }

    override fun abandonAudioFocus() {
        try {
            focusRequest?.let { req ->
                audioManager.abandonAudioFocusRequest(req)
            }
        } catch (_: Exception) {
        } finally {
            _hasAudioFocus.value = false
            if (_sessionMode.value == AudioSessionMode.TTS_SPEAKING) {
                _sessionMode.value = AudioSessionMode.IDLE
            }
        }
    }

    override fun requestRecordPermissionRationale(): Boolean {
        return !hasAudioRecordPermission
    }

    fun release() {
        abandonAudioFocus()
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(becomingNoisyReceiver)
            } catch (_: Exception) {
            }
            isReceiverRegistered = false
        }
        onAudioInterruptedCallback = null
        _sessionMode.value = AudioSessionMode.IDLE
    }
}
