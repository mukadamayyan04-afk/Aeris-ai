package com.example.voice

import com.example.data.repository.VoiceRepository
import com.example.domain.model.AerisVoice
import com.example.domain.model.TtsEngineMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * VoiceSelectionManager (AerisVoiceSelectionManager):
 * Connects installed TTS engines & voices with persistent DataStore preferences via VoiceRepository.
 * Restores saved TTS engine and voice on launch and publishes honest fallback notices if unavailable.
 */
class AerisVoiceSelectionManager(
    private val ttsEngine: AndroidTtsEngine,
    private val voiceRepository: VoiceRepository,
    private val scope: CoroutineScope
) : VoiceSelectionRepository {

    override val availableVoicesFlow: Flow<List<AerisVoice>> = ttsEngine.availableVoices
    override val selectedVoiceFlow: Flow<AerisVoice?> = ttsEngine.selectedVoice
    override val selectedVoiceIdFlow: Flow<String?> = voiceRepository.selectedVoiceIdFlow

    val availableEngines: StateFlow<List<TtsEngineMetadata>> = ttsEngine.availableEngines
    val activeEnginePackage: StateFlow<String> = ttsEngine.activeEnginePackage
    val selectedTtsEngineFlow: Flow<String?> = voiceRepository.selectedTtsEngineFlow

    private val _fallbackNotice = MutableStateFlow<String?>(null)
    val fallbackNotice: StateFlow<String?> = _fallbackNotice.asStateFlow()

    init {
        ttsEngine.setOnVoiceFallbackListener { notice ->
            _fallbackNotice.value = notice
        }

        // Restore persisted TTS engine first (if any), then restore saved voice ID
        scope.launch {
            val savedEngine = voiceRepository.selectedTtsEngineFlow.first()
            if (!savedEngine.isNullOrBlank()) {
                ttsEngine.switchEngine(savedEngine)
            }

            voiceRepository.selectedVoiceIdFlow.collect { savedVoiceId ->
                if (!savedVoiceId.isNullOrBlank()) {
                    val notice = ttsEngine.selectVoiceWithFallback(savedVoiceId)
                    if (notice != null) {
                        _fallbackNotice.value = notice
                    }
                }
            }
        }
    }

    override suspend fun saveSelectedVoiceId(voiceId: String) {
        voiceRepository.saveSelectedVoiceId(voiceId)
        val notice = ttsEngine.selectVoiceWithFallback(voiceId)
        _fallbackNotice.value = notice
    }

    suspend fun selectVoice(voice: AerisVoice) {
        _fallbackNotice.value = null
        ttsEngine.setVoice(voice)
        voiceRepository.saveSelectedVoiceId(voice.id)
    }

    suspend fun switchTtsEngine(packageName: String) {
        if (packageName.isBlank()) return
        _fallbackNotice.value = null
        voiceRepository.saveSelectedTtsEngine(packageName)
        ttsEngine.switchEngine(packageName)
    }

    override suspend fun getSelectedVoice(): AerisVoice? = ttsEngine.selectedVoice.value

    override suspend fun refreshInstalledVoices(): List<AerisVoice> {
        ttsEngine.refreshVoices()
        return ttsEngine.availableVoices.value
    }

    fun clearFallbackNotice() {
        _fallbackNotice.value = null
    }
}
