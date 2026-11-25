package com.example.voice

import com.example.data.repository.VoiceRepository
import com.example.domain.model.VoiceState
import com.example.domain.model.VoiceTaskRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * AerisVoiceEngineManager:
 * High-level orchestration manager implementing VoiceEngineManager.
 * Coordinates STT, TTS, VoiceSelection, WakeWord, AudioSession focus/interruptions,
 * and Room voice_tasks logging without STT/TTS collisions.
 */
class AerisVoiceEngineManager(
    val audioSessionManager: AerisAudioSessionManager,
    val sttEngine: AndroidSpeechRecognizerEngine,
    val ttsEngine: AndroidTtsEngine,
    val voiceSelectionManager: AerisVoiceSelectionManager,
    val wakeWordManager: AerisWakeWordManager,
    private val voiceRepository: VoiceRepository,
    private val scope: CoroutineScope
) : VoiceEngineManager {

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    override val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    override val activeSttEngineName: StateFlow<String> = sttEngine.activeEngineName

    private val _activeTtsEngineName = MutableStateFlow("Android System TTS")
    override val activeTtsEngineName: StateFlow<String> = _activeTtsEngineName.asStateFlow()

    private val _isOfflineReady = MutableStateFlow(false)
    override val isOfflineReady: StateFlow<Boolean> = _isOfflineReady.asStateFlow()

    private val _isMicPermissionGranted = MutableStateFlow(audioSessionManager.hasAudioRecordPermission)
    override val isMicPermissionGranted: StateFlow<Boolean> = _isMicPermissionGranted.asStateFlow()

    private val _voiceError = MutableStateFlow<String?>(null)
    val voiceError: StateFlow<String?> = _voiceError.asStateFlow()

    private var isThinking = false

    override fun initialize() {
        audioSessionManager.setOnAudioInterruptedListener { reason ->
            ttsEngine.stopSpeaking()
            if (sttEngine.isListening.value) {
                sttEngine.cancelListening()
            }
            _voiceError.value = reason
            if (!isThinking) {
                _voiceState.value = VoiceState.IDLE
            }
        }

        // Sync persisted voice settings into engines
        scope.launch {
            voiceRepository.speechRateFlow.collect { rate ->
                ttsEngine.setSpeechRate(rate)
            }
        }
        scope.launch {
            voiceRepository.speechPitchFlow.collect { pitch ->
                ttsEngine.setPitch(pitch)
            }
        }
        scope.launch {
            voiceRepository.recognitionLanguageFlow.collect { lang ->
                sttEngine.setRecognitionLanguage(lang)
            }
        }

        // Track TTS engine readiness and name
        scope.launch {
            combine(
                ttsEngine.isEngineReady,
                ttsEngine.selectedVoice,
                sttEngine.isOfflineModelInstalled
            ) { ttsReady, voice, sttOffline ->
                val ttsLabel = voice?.engineName?.takeIf { it.isNotBlank() } ?: "Android System TTS"
                _activeTtsEngineName.value = ttsLabel
                _isOfflineReady.value = ttsReady && sttOffline
            }.collect {}
        }

        // Synchronize VoiceState from STT, TTS, and Errors
        scope.launch {
            combine(
                sttEngine.isListening,
                ttsEngine.isSpeaking,
                sttEngine.recognitionError,
                ttsEngine.ttsError
            ) { listening, speaking, sttErr, ttsErr ->
                when {
                    listening -> VoiceState.LISTENING
                    speaking -> VoiceState.SPEAKING
                    isThinking -> VoiceState.THINKING
                    sttErr != null || ttsErr != null -> {
                        _voiceError.value = sttErr ?: ttsErr
                        VoiceState.ERROR
                    }
                    else -> VoiceState.IDLE
                }
            }.collect { newState ->
                _voiceState.value = newState
            }
        }
    }

    fun setThinking(thinking: Boolean) {
        isThinking = thinking
        if (thinking) {
            _voiceState.value = VoiceState.THINKING
        } else if (!sttEngine.isListening.value && !ttsEngine.isSpeaking.value) {
            _voiceState.value = if (_voiceError.value != null) VoiceState.ERROR else VoiceState.IDLE
        }
    }

    fun startSttForVoice(
        onPartial: (String) -> Unit = {},
        onFinal: (String) -> Unit
    ) {
        clearError()
        ttsEngine.stopSpeaking()
        if (wakeWordManager.isServiceRunning.value) {
            wakeWordManager.stopListening()
        }
        sttEngine.startListening(onPartial = onPartial, onFinal = onFinal)
    }

    fun stopStt() {
        sttEngine.stopListening()
    }

    fun cancelStt() {
        sttEngine.cancelListening()
        clearError()
        if (!isThinking && !ttsEngine.isSpeaking.value) {
            _voiceState.value = VoiceState.IDLE
        }
    }

    val isSpeechPaused: StateFlow<Boolean> = ttsEngine.isPausedFlow

    fun speakText(text: String) {
        if (text.isBlank()) return
        clearError()
        if (sttEngine.isListening.value) {
            sttEngine.cancelListening()
        }
        ttsEngine.speak(text)
    }

    fun pauseSpeaking() {
        ttsEngine.pauseSpeaking()
    }

    fun resumeSpeaking(): Boolean {
        clearError()
        if (sttEngine.isListening.value) {
            sttEngine.cancelListening()
        }
        return ttsEngine.resumeSpeaking()
    }

    fun stopSpeaking() {
        ttsEngine.stopSpeaking()
    }

    fun replayLastResponse(): Boolean {
        clearError()
        if (sttEngine.isListening.value) {
            sttEngine.cancelListening()
        }
        return ttsEngine.replayLastSpoken()
    }

    fun clearError() {
        _voiceError.value = null
        sttEngine.clearError()
        ttsEngine.clearError()
        if (!isThinking && !sttEngine.isListening.value && !ttsEngine.isSpeaking.value) {
            _voiceState.value = VoiceState.IDLE
        }
    }

    suspend fun logVoiceTask(
        conversationId: Long?,
        transcript: String,
        responsePreview: String,
        status: String,
        errorMessage: String? = null,
        durationMs: Long = 0L
    ) {
        voiceRepository.recordVoiceTask(
            VoiceTaskRecord(
                conversationId = conversationId,
                transcript = transcript.take(500),
                responsePreview = responsePreview.take(500),
                sttEngine = activeSttEngineName.value,
                ttsEngine = activeTtsEngineName.value,
                voiceId = ttsEngine.selectedVoice.value?.id ?: "system-default",
                languageTag = sttEngine.currentRecognitionLanguage.value,
                status = status,
                errorMessage = errorMessage,
                durationMs = durationMs
            )
        )
    }

    override fun onMicrophonePermissionResult(granted: Boolean) {
        _isMicPermissionGranted.value = granted
        wakeWordManager.refreshCapabilities()
        if (granted) {
            clearError()
        }
    }

    override fun release() {
        sttEngine.destroy()
        ttsEngine.shutdown()
        audioSessionManager.release()
    }
}
