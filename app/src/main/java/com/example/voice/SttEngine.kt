package com.example.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.example.domain.model.AudioSessionMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

interface SttEngine : SpeechRecognitionManager {
    val isListeningFlow: Flow<Boolean>
    val partialTextFlow: Flow<String>
    val rmsLevelFlow: Flow<Float>
    val errorFlow: Flow<String?>

    fun startListening(onResult: (String) -> Unit)
    fun clearError()
    fun destroy()
}

/**
 * AndroidSpeechRecognizerEngine:
 * Production Speech-to-Text manager implementing both SttEngine and SpeechRecognitionManager contracts.
 * Coordinates with AerisAudioSessionManager, checks RECORD_AUDIO permission, prefers on-device offline STT,
 * provides live partial and final transcripts, and safely handles error recovery & cancellation.
 */
class AndroidSpeechRecognizerEngine(
    private val context: Context,
    private val audioSessionManager: AerisAudioSessionManager? = null
) : SttEngine {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var preferOnDevice = false

    private val _isListening = MutableStateFlow(false)
    override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    override val isListeningFlow: Flow<Boolean> = _isListening.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    override val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()
    override val partialTextFlow: Flow<String> = _liveTranscript.asStateFlow()

    private val _audioRmsLevel = MutableStateFlow(0f)
    override val audioRmsLevel: StateFlow<Float> = _audioRmsLevel.asStateFlow()
    override val rmsLevelFlow: Flow<Float> = _audioRmsLevel.asStateFlow()

    private val _recognitionError = MutableStateFlow<String?>(null)
    override val recognitionError: StateFlow<String?> = _recognitionError.asStateFlow()
    override val errorFlow: Flow<String?> = _recognitionError.asStateFlow()

    private val _isOfflineModelInstalled = MutableStateFlow(false)
    override val isOfflineModelInstalled: StateFlow<Boolean> = _isOfflineModelInstalled.asStateFlow()

    private val _currentRecognitionLanguage = MutableStateFlow(Locale.getDefault().toLanguageTag())
    override val currentRecognitionLanguage: StateFlow<String> = _currentRecognitionLanguage.asStateFlow()

    private val _activeEngineName = MutableStateFlow("Checking STT capabilities...")
    val activeEngineName: StateFlow<String> = _activeEngineName.asStateFlow()

    private var onPartialCallback: ((String) -> Unit)? = null
    private var onFinalCallback: ((String) -> Unit)? = null
    private var isCancelled = false

    init {
        refreshCapabilities()
    }

    fun refreshCapabilities() {
        val report = LocalSpeechModelDetector.inspect(context)
        preferOnDevice = report.isAndroidOnDeviceSttAvailable
        _isOfflineModelInstalled.value = report.isAndroidOnDeviceSttAvailable ||
            (report.isSherpaNativeLibPresent && report.isSherpaSttModelPresent) ||
            report.isVoskModelPresent
        _activeEngineName.value = when {
            report.isSherpaNativeLibPresent && report.isSherpaSttModelPresent -> "Sherpa-ONNX Local STT"
            report.isAndroidOnDeviceSttAvailable -> "Android On-Device Recognizer"
            report.isAndroidSttAvailable -> "Android SpeechRecognizer (Offline Requested)"
            else -> "No STT Service Installed"
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private fun hasRecordPermission(): Boolean {
        return audioSessionManager?.hasAudioRecordPermission
            ?: (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }

    private fun createRecognizerInstance(): SpeechRecognizer? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        ) {
            try {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } catch (_: Exception) {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    SpeechRecognizer.createSpeechRecognizer(context)
                } else null
            }
        } else if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    private fun ensureRecognizerInitialized(): Boolean {
        if (speechRecognizer != null) return true
        val created = createRecognizerInstance() ?: return false
        created.setRecognitionListener(createRecognitionListener())
        speechRecognizer = created
        return true
    }

    private fun resetRecognizerInstance() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
        speechRecognizer = null
    }

    private fun createRecognitionListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            if (isCancelled) return
            _isListening.value = true
            _recognitionError.value = null
            audioSessionManager?.transitionSessionMode(AudioSessionMode.STT_LISTENING)
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {
            if (!isCancelled) {
                _audioRmsLevel.value = rmsdB.coerceAtLeast(0f)
            }
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _isListening.value = false
            _audioRmsLevel.value = 0f
            audioSessionManager?.transitionSessionMode(AudioSessionMode.IDLE)
        }

        override fun onError(error: Int) {
            _isListening.value = false
            _audioRmsLevel.value = 0f
            audioSessionManager?.transitionSessionMode(AudioSessionMode.IDLE)

            if (isCancelled && error == SpeechRecognizer.ERROR_CLIENT) {
                return
            }

            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio capture hardware error. Check microphone availability."
                SpeechRecognizer.ERROR_CLIENT -> "Speech recognition session reset."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required for speech recognition."
                SpeechRecognizer.ERROR_NETWORK -> "Network speech unavailable. Install an Android offline speech pack in System Settings."
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition timed out waiting for network."
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please speak clearly and try again, Sir ji."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer was busy; session reset. Tap again to retry."
                SpeechRecognizer.ERROR_SERVER -> "Device speech service returned an error."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected before timeout."
                else -> "Speech recognition error (code $error)."
            }
            _recognitionError.value = errorMsg

            if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                error == SpeechRecognizer.ERROR_CLIENT ||
                error == SpeechRecognizer.ERROR_AUDIO
            ) {
                resetRecognizerInstance()
            }
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            _audioRmsLevel.value = 0f
            audioSessionManager?.transitionSessionMode(AudioSessionMode.IDLE)

            if (isCancelled) return

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognized = matches?.firstOrNull()?.trim().orEmpty()
            if (recognized.isNotEmpty()) {
                _liveTranscript.value = recognized
                onFinalCallback?.invoke(recognized)
            } else {
                _recognitionError.value = "No speech recognized."
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (isCancelled) return
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull()?.trim().orEmpty()
            if (partial.isNotEmpty()) {
                _liveTranscript.value = partial
                onPartialCallback?.invoke(partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    override fun startListening(onResult: (String) -> Unit) {
        startListening(onPartial = {}, onFinal = onResult)
    }

    override fun startListening(onPartial: (String) -> Unit, onFinal: (String) -> Unit) {
        isCancelled = false
        onPartialCallback = onPartial
        onFinalCallback = onFinal
        _liveTranscript.value = ""
        _recognitionError.value = null

        if (!hasRecordPermission()) {
            _recognitionError.value = "Microphone permission required. Grant RECORD_AUDIO permission to speak."
            _isListening.value = false
            return
        }

        refreshCapabilities()
        val report = LocalSpeechModelDetector.inspect(context)
        if (!report.isAndroidSttAvailable && !report.isAndroidOnDeviceSttAvailable) {
            _recognitionError.value = "No Speech Recognition service is installed on this Android device."
            _isListening.value = false
            return
        }

        runOnMain {
            if (!ensureRecognizerInitialized()) {
                _recognitionError.value = "Unable to initialize Android SpeechRecognizer."
                _isListening.value = false
                return@runOnMain
            }

            val langTag = _currentRecognitionLanguage.value.ifBlank { Locale.getDefault().toLanguageTag() }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            try {
                audioSessionManager?.transitionSessionMode(AudioSessionMode.STT_LISTENING)
                _isListening.value = true
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                _isListening.value = false
                audioSessionManager?.transitionSessionMode(AudioSessionMode.IDLE)
                _recognitionError.value = "Failed to start listening: ${e.message ?: "Unknown error"}"
                resetRecognizerInstance()
            }
        }
    }

    override fun stopListening() {
        runOnMain {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {
            }
            _isListening.value = false
            _audioRmsLevel.value = 0f
            audioSessionManager?.transitionSessionMode(AudioSessionMode.IDLE)
        }
    }

    override fun cancelListening() {
        isCancelled = true
        onPartialCallback = null
        onFinalCallback = null
        runOnMain {
            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {
            }
            _isListening.value = false
            _liveTranscript.value = ""
            _audioRmsLevel.value = 0f
            audioSessionManager?.transitionSessionMode(AudioSessionMode.IDLE)
        }
    }

    override fun setRecognitionLanguage(languageTag: String) {
        if (languageTag.isNotBlank()) {
            _currentRecognitionLanguage.value = languageTag
        }
    }

    override fun clearError() {
        _recognitionError.value = null
    }

    override fun destroy() {
        isCancelled = true
        onPartialCallback = null
        onFinalCallback = null
        runOnMain {
            resetRecognizerInstance()
            _isListening.value = false
            _audioRmsLevel.value = 0f
            audioSessionManager?.transitionSessionMode(AudioSessionMode.IDLE)
        }
    }
}
