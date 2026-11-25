package com.example.data.repository

import com.example.data.database.LocalModelDao
import com.example.data.database.VoiceTaskDao
import com.example.data.database.entities.LocalModelEntity
import com.example.data.database.entities.VoiceTaskEntity
import com.example.domain.model.ModelMetadata
import com.example.domain.model.VoiceTaskRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * VoiceRepository:
 * Central persistence layer for voice tasks, voice preferences, and local model metadata.
 */
interface VoiceRepository {
    val recentVoiceTasks: Flow<List<VoiceTaskRecord>>
    val voiceTaskCount: Flow<Int>
    val storedModels: Flow<List<ModelMetadata>>

    val selectedTtsEngineFlow: Flow<String?>
    val selectedVoiceIdFlow: Flow<String?>
    val recognitionLanguageFlow: Flow<String>
    val speechRateFlow: Flow<Float>
    val speechPitchFlow: Flow<Float>
    val voiceResponsesEnabledFlow: Flow<Boolean>
    val autoPlayResponsesFlow: Flow<Boolean>
    val voiceInputEnabledFlow: Flow<Boolean>
    val pushToTalkEnabledFlow: Flow<Boolean>
    val wakeWordEnabledFlow: Flow<Boolean>
    val wakeWordPhraseFlow: Flow<String>
    val backgroundListeningEnabledFlow: Flow<Boolean>

    suspend fun recordVoiceTask(record: VoiceTaskRecord): Long
    suspend fun deleteVoiceTask(id: Long)
    suspend fun clearVoiceTasks()
    suspend fun syncModels(models: List<ModelMetadata>, category: String = "LLM")

    suspend fun saveSelectedTtsEngine(enginePackage: String)
    suspend fun saveSelectedVoiceId(voiceId: String)
    suspend fun setRecognitionLanguage(language: String)
    suspend fun setSpeechRate(rate: Float)
    suspend fun setSpeechPitch(pitch: Float)
    suspend fun setVoiceResponsesEnabled(enabled: Boolean)
    suspend fun setAutoPlayResponses(enabled: Boolean)
    suspend fun setVoiceInputEnabled(enabled: Boolean)
    suspend fun setPushToTalkEnabled(enabled: Boolean)
    suspend fun setWakeWordEnabled(enabled: Boolean)
    suspend fun setWakeWordPhrase(phrase: String)
    suspend fun setBackgroundListeningEnabled(enabled: Boolean)
}

class VoiceRepositoryImpl(
    private val voiceTaskDao: VoiceTaskDao,
    private val localModelDao: LocalModelDao,
    private val settingsRepository: SettingsRepository
) : VoiceRepository {

    override val recentVoiceTasks: Flow<List<VoiceTaskRecord>> =
        voiceTaskDao.getRecentTasks(50).map { list -> list.map { it.toDomain() } }

    override val voiceTaskCount: Flow<Int> = voiceTaskDao.getTaskCount()

    override val storedModels: Flow<List<ModelMetadata>> =
        localModelDao.getAllModels().map { list -> list.map { it.toDomain() } }

    override val selectedTtsEngineFlow: Flow<String?> = settingsRepository.selectedTtsEngineFlow
    override val selectedVoiceIdFlow: Flow<String?> = settingsRepository.selectedVoiceIdFlow
    override val recognitionLanguageFlow: Flow<String> = settingsRepository.recognitionLanguageFlow
    override val speechRateFlow: Flow<Float> = settingsRepository.speechRateFlow
    override val speechPitchFlow: Flow<Float> = settingsRepository.speechPitchFlow
    override val voiceResponsesEnabledFlow: Flow<Boolean> = settingsRepository.voiceResponsesEnabledFlow
    override val autoPlayResponsesFlow: Flow<Boolean> = settingsRepository.autoPlayResponsesFlow
    override val voiceInputEnabledFlow: Flow<Boolean> = settingsRepository.voiceInputEnabledFlow
    override val pushToTalkEnabledFlow: Flow<Boolean> = settingsRepository.pushToTalkEnabledFlow
    override val wakeWordEnabledFlow: Flow<Boolean> = settingsRepository.wakeWordEnabledFlow
    override val wakeWordPhraseFlow: Flow<String> = settingsRepository.wakeWordPhraseFlow
    override val backgroundListeningEnabledFlow: Flow<Boolean> = settingsRepository.backgroundListeningEnabledFlow

    override suspend fun recordVoiceTask(record: VoiceTaskRecord): Long {
        return voiceTaskDao.insert(VoiceTaskEntity.fromDomain(record))
    }

    override suspend fun deleteVoiceTask(id: Long) {
        voiceTaskDao.deleteById(id)
    }

    override suspend fun clearVoiceTasks() {
        voiceTaskDao.clearAll()
    }

    override suspend fun syncModels(models: List<ModelMetadata>, category: String) {
        val entities = models.map { LocalModelEntity.fromDomain(it, category) }
        localModelDao.upsertAll(entities)
    }

    override suspend fun saveSelectedTtsEngine(enginePackage: String) {
        settingsRepository.setSelectedTtsEngine(enginePackage)
    }

    override suspend fun saveSelectedVoiceId(voiceId: String) {
        settingsRepository.setSelectedVoiceId(voiceId)
    }

    override suspend fun setRecognitionLanguage(language: String) {
        settingsRepository.setRecognitionLanguage(language)
    }

    override suspend fun setSpeechRate(rate: Float) {
        settingsRepository.setSpeechRate(rate)
    }

    override suspend fun setSpeechPitch(pitch: Float) {
        settingsRepository.setSpeechPitch(pitch)
    }

    override suspend fun setVoiceResponsesEnabled(enabled: Boolean) {
        settingsRepository.setVoiceResponsesEnabled(enabled)
    }

    override suspend fun setAutoPlayResponses(enabled: Boolean) {
        settingsRepository.setAutoPlayResponses(enabled)
    }

    override suspend fun setVoiceInputEnabled(enabled: Boolean) {
        settingsRepository.setVoiceInputEnabled(enabled)
    }

    override suspend fun setPushToTalkEnabled(enabled: Boolean) {
        settingsRepository.setPushToTalkEnabled(enabled)
    }

    override suspend fun setWakeWordEnabled(enabled: Boolean) {
        settingsRepository.setWakeWordEnabled(enabled)
    }

    override suspend fun setWakeWordPhrase(phrase: String) {
        settingsRepository.setWakeWordPhrase(phrase)
    }

    override suspend fun setBackgroundListeningEnabled(enabled: Boolean) {
        settingsRepository.setBackgroundListeningEnabled(enabled)
    }
}
