package com.example.voice

import com.example.domain.model.AerisVoice
import com.example.domain.model.VoiceState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Prompt 1 Integration Contracts for AERIS Voice Subsystem.
 * These interfaces form the architectural foundation for Prompt 2's backend implementation:
 * - Room database storage
 * - DataStore preferences
 * - Local streaming STT & ONNX/Piper neural candidate engines
 * - Wake-word detection service
 * - Android AudioSession focus management
 */

/**
 * High-level orchestration manager for all voice subsystems.
 */
interface VoiceEngineManager {
    val voiceState: StateFlow<VoiceState>
    val activeSttEngineName: StateFlow<String>
    val activeTtsEngineName: StateFlow<String>
    val isOfflineReady: StateFlow<Boolean>
    val isMicPermissionGranted: StateFlow<Boolean>

    fun initialize()
    fun release()
    fun onMicrophonePermissionResult(granted: Boolean)
}

/**
 * Speech Recognition (STT) interface contract for local and candidate engines.
 */
interface SpeechRecognitionManager {
    val isListening: StateFlow<Boolean>
    val liveTranscript: StateFlow<String>
    val audioRmsLevel: StateFlow<Float>
    val recognitionError: StateFlow<String?>
    val isOfflineModelInstalled: StateFlow<Boolean>
    val currentRecognitionLanguage: StateFlow<String>

    fun startListening(onPartial: (String) -> Unit = {}, onFinal: (String) -> Unit)
    fun stopListening()
    fun cancelListening()
    fun setRecognitionLanguage(languageTag: String)
}

/**
 * Text-to-Speech (TTS) manager contract for native & neural synthesis.
 */
interface TextToSpeechManager {
    val isSpeaking: StateFlow<Boolean>
    val isEngineReady: StateFlow<Boolean>
    val availableVoices: StateFlow<List<AerisVoice>>
    val selectedVoice: StateFlow<AerisVoice?>
    val speechRate: StateFlow<Float>
    val speechPitch: StateFlow<Float>

    fun speak(text: String, utteranceId: String = System.currentTimeMillis().toString())
    fun stopSpeaking()
    fun stop() = stopSpeaking()
    fun setSpeechRate(rate: Float)
    fun setPitch(pitch: Float)
    fun setVoice(voice: AerisVoice)
    fun previewVoice(voice: AerisVoice, sampleText: String)
    fun refreshVoices()
}

/**
 * Repository interface for querying and saving chosen TTS voice configurations.
 */
interface VoiceSelectionRepository {
    val availableVoicesFlow: Flow<List<AerisVoice>>
    val selectedVoiceFlow: Flow<AerisVoice?>
    val selectedVoiceIdFlow: Flow<String?>

    suspend fun saveSelectedVoiceId(voiceId: String)
    suspend fun getSelectedVoice(): AerisVoice?
    suspend fun refreshInstalledVoices(): List<AerisVoice>
}

/**
 * Persistent preferences for voice output, input, wake-words, and audio behavior.
 */
interface VoicePreferencesRepository {
    val voiceResponsesEnabledFlow: Flow<Boolean>
    val autoPlayResponsesFlow: Flow<Boolean>
    val voiceInputEnabledFlow: Flow<Boolean>
    val pushToTalkEnabledFlow: Flow<Boolean>
    val recognitionLanguageFlow: Flow<String>
    val speechRateFlow: Flow<Float>
    val speechPitchFlow: Flow<Float>
    val wakeWordEnabledFlow: Flow<Boolean>
    val wakeWordPhraseFlow: Flow<String>

    suspend fun setVoiceResponsesEnabled(enabled: Boolean)
    suspend fun setAutoPlayResponses(enabled: Boolean)
    suspend fun setVoiceInputEnabled(enabled: Boolean)
    suspend fun setPushToTalkEnabled(enabled: Boolean)
    suspend fun setRecognitionLanguage(language: String)
    suspend fun setSpeechRate(rate: Float)
    suspend fun setSpeechPitch(pitch: Float)
    suspend fun setWakeWordEnabled(enabled: Boolean)
    suspend fun setWakeWordPhrase(phrase: String)
}

/**
 * Contract for on-device wake-word detection services ("Hey AERIS", "AERIS, wake up").
 */
interface WakeWordManager {
    val isWakeWordListening: StateFlow<Boolean>
    val isServiceRunning: StateFlow<Boolean>
    val detectedPhrase: StateFlow<String?>
    val selectedPhrase: StateFlow<String>

    fun startListening()
    fun stopListening()
    fun setPhrase(phrase: String)
}

/**
 * Audio focus and hardware session manager for microphone, playback, and haptics.
 */
interface AudioSessionManager {
    val hasAudioRecordPermission: Boolean
    val isAudioOutputMuted: Boolean

    fun requestAudioFocusForPlayback(): Boolean
    fun abandonAudioFocus()
    fun requestRecordPermissionRationale(): Boolean
}
