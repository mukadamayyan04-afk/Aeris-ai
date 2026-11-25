package com.example.voice

import android.content.Context
import com.example.data.repository.VoiceRepository
import com.example.domain.model.AudioSessionMode
import com.example.domain.model.WakeWordEngineStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * WakeWordManager (AerisWakeWordManager):
 * Manages wake-word configuration ("Hey AERIS" / "AERIS, wake up"), foreground service lifecycle,
 * permission verification, and honest local KWS dependency diagnostics.
 */
class AerisWakeWordManager(
    private val context: Context,
    private val audioSessionManager: AerisAudioSessionManager,
    private val voiceRepository: VoiceRepository,
    private val scope: CoroutineScope
) : WakeWordManager {

    private val _isWakeWordListening = MutableStateFlow(false)
    override val isWakeWordListening: StateFlow<Boolean> = _isWakeWordListening.asStateFlow()

    override val isServiceRunning: StateFlow<Boolean> = AerisWakeWordService.isServiceRunning

    private val _detectedPhrase = MutableStateFlow<String?>(null)
    override val detectedPhrase: StateFlow<String?> = _detectedPhrase.asStateFlow()

    private val _selectedPhrase = MutableStateFlow("Hey AERIS")
    override val selectedPhrase: StateFlow<String> = _selectedPhrase.asStateFlow()

    val engineStatus: StateFlow<WakeWordEngineStatus> = AerisWakeWordService.engineStatus
    val statusDetail: StateFlow<String?> = AerisWakeWordService.statusDetail

    private val _capabilityReport = MutableStateFlow(AerisWakeWordService.refreshAvailability(context))
    val capabilityReport: StateFlow<LocalSpeechCapabilityReport> = _capabilityReport.asStateFlow()

    init {
        scope.launch {
            voiceRepository.wakeWordPhraseFlow.collect { phrase ->
                _selectedPhrase.value = phrase
            }
        }
        scope.launch {
            AerisWakeWordService.isServiceRunning.collect { running ->
                _isWakeWordListening.value = running
                if (running) {
                    audioSessionManager.transitionSessionMode(AudioSessionMode.WAKE_WORD_ACTIVE)
                } else if (audioSessionManager.sessionMode.value == AudioSessionMode.WAKE_WORD_ACTIVE) {
                    audioSessionManager.transitionSessionMode(AudioSessionMode.IDLE)
                }
            }
        }
        scope.launch {
            AerisWakeWordService.wakeWordDetectedEvent.collect { timestamp ->
                if (timestamp != null) {
                    _detectedPhrase.value = _selectedPhrase.value
                }
            }
        }
    }

    fun refreshCapabilities(): LocalSpeechCapabilityReport {
        val report = AerisWakeWordService.refreshAvailability(context)
        _capabilityReport.value = report
        return report
    }

    override fun startListening() {
        _capabilityReport.value = LocalSpeechModelDetector.inspect(context)
        val started = AerisWakeWordService.start(context, _selectedPhrase.value)
        _isWakeWordListening.value = started && AerisWakeWordService.isServiceRunning.value
        if (!started) {
            scope.launch {
                // Keep UI toggle truthful if foreground wake-word cannot start due to missing perm or KWS model
                voiceRepository.setWakeWordEnabled(false)
            }
        }
    }

    override fun stopListening() {
        AerisWakeWordService.stop(context)
        _isWakeWordListening.value = false
    }

    override fun setPhrase(phrase: String) {
        if (phrase.isBlank()) return
        _selectedPhrase.value = phrase
        scope.launch {
            voiceRepository.setWakeWordPhrase(phrase)
        }
    }

    fun clearDetectedPhrase() {
        _detectedPhrase.value = null
    }
}
