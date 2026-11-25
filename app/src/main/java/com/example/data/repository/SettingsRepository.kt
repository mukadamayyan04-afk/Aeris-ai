package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.OperatingMode
import com.example.voice.VoicePreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aeris_settings")

class SettingsRepository(private val context: Context) : VoicePreferencesRepository {

    private object PreferencesKeys {
        val OPERATING_MODE = stringPreferencesKey("operating_mode")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val VOICE_VOLUME = floatPreferencesKey("voice_volume")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val ACTIVE_MODEL_ID = stringPreferencesKey("active_model_id")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val WAKE_WORD_PHRASE = stringPreferencesKey("wake_word_phrase")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        val VISUAL_EFFECTS_ENABLED = booleanPreferencesKey("visual_effects_enabled")
        val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
        val SELECTED_TTS_ENGINE = stringPreferencesKey("selected_tts_engine")
        val SELECTED_VOICE_ID = stringPreferencesKey("selected_voice_id")
        val VOICE_RESPONSES_ENABLED = booleanPreferencesKey("voice_responses_enabled")
        val AUTO_PLAY_RESPONSES = booleanPreferencesKey("auto_play_responses")
        val VOICE_INPUT_ENABLED = booleanPreferencesKey("voice_input_enabled")
        val PUSH_TO_TALK_ENABLED = booleanPreferencesKey("push_to_talk_enabled")
        val RECOGNITION_LANGUAGE = stringPreferencesKey("recognition_language")
        val BACKGROUND_LISTENING_ENABLED = booleanPreferencesKey("background_listening_enabled")

        // Stage 1: AI Brain & Multi-Model Provider Preferences
        val FREE_ONLY_MODE = booleanPreferencesKey("ai_free_only_mode")
        val AUTO_SWITCH_ENABLED = booleanPreferencesKey("ai_auto_switch_enabled")
        val PREFERRED_MODEL_ID = stringPreferencesKey("ai_preferred_model_id")
        val AI_OPERATING_MODE = stringPreferencesKey("ai_operating_mode")
        val MANUAL_MODEL_LOCK = booleanPreferencesKey("ai_manual_model_lock")
        val LOCAL_FALLBACK_ENABLED = booleanPreferencesKey("ai_local_fallback_enabled")
        val DISABLED_PROVIDERS = stringSetPreferencesKey("ai_disabled_providers")
        val DEFAULT_LOCAL_MODEL_PROMPTED = booleanPreferencesKey("ai_default_local_model_prompted")

        val PERSONALITY_STYLE = stringPreferencesKey("ai_personality_style")
        val RESPONSE_LENGTH = stringPreferencesKey("ai_response_length")

        val OPENAI_BASE_URL = stringPreferencesKey("ai_openai_base_url")
        val OPENAI_MODEL_ID = stringPreferencesKey("ai_openai_model_id")
        val OPENAI_TOKEN = stringPreferencesKey("ai_openai_token")

        val OPENROUTER_BASE_URL = stringPreferencesKey("ai_openrouter_base_url")
        val OPENROUTER_MODEL_ID = stringPreferencesKey("ai_openrouter_model_id")
        val OPENROUTER_TOKEN = stringPreferencesKey("ai_openrouter_token")

        val GROQ_BASE_URL = stringPreferencesKey("ai_groq_base_url")
        val GROQ_MODEL_ID = stringPreferencesKey("ai_groq_model_id")
        val GROQ_TOKEN = stringPreferencesKey("ai_groq_token")

        val HF_BASE_URL = stringPreferencesKey("ai_hf_base_url")
        val HF_MODEL_ID = stringPreferencesKey("ai_hf_model_id")
        val HF_TOKEN = stringPreferencesKey("ai_hf_token")

        val GPT_OSS_BASE_URL = stringPreferencesKey("ai_gpt_oss_base_url")
        val GPT_OSS_MODEL_ID = stringPreferencesKey("ai_gpt_oss_model_id")
        val GPT_OSS_TOKEN = stringPreferencesKey("ai_gpt_oss_token")

        val QWEN_BASE_URL = stringPreferencesKey("ai_qwen_base_url")
        val QWEN_MODEL_ID = stringPreferencesKey("ai_qwen_model_id")
        val QWEN_TOKEN = stringPreferencesKey("ai_qwen_token")

        val CLOUDFLARE_BASE_URL = stringPreferencesKey("ai_cloudflare_base_url")
        val CLOUDFLARE_MODEL_ID = stringPreferencesKey("ai_cloudflare_model_id")
        val CLOUDFLARE_TOKEN = stringPreferencesKey("ai_cloudflare_token")

        // Stage 5-8: Privacy, Notifications, Device Actions, Game Coach, Battery
        val DEVICE_ACTION_CONFIRM_SENSITIVE = booleanPreferencesKey("device_action_confirm_sensitive")
        val NOTIFICATION_ANNOUNCER_ENABLED = booleanPreferencesKey("notification_announcer_enabled")
        val NOTIFICATION_ALLOWED_APPS = stringSetPreferencesKey("notification_allowed_apps")
        val NOTIFICATION_HIDE_SENSITIVE = booleanPreferencesKey("notification_hide_sensitive")
        val GAME_COACH_CLOUD_ANALYSIS = booleanPreferencesKey("game_coach_cloud_analysis")
        val BATTERY_SAVER_HOLO = booleanPreferencesKey("battery_saver_holo")
        val WAKE_WORD_PRIVACY_MODE = booleanPreferencesKey("wake_word_privacy_mode")
        val START_ON_BOOT_OPT_IN = booleanPreferencesKey("start_on_boot_opt_in")
        val FLOATING_ASSISTANT_ENABLED = booleanPreferencesKey("floating_assistant_enabled")
        val CUSTOM_SYSTEM_INSTRUCTION = stringPreferencesKey("custom_system_instruction")
        val FEATURE_USAGE_COUNTS = stringPreferencesKey("feature_usage_counts")
    }

    val operatingModeFlow: Flow<OperatingMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[PreferencesKeys.OPERATING_MODE] ?: OperatingMode.NORMAL.name
        try {
            OperatingMode.valueOf(raw)
        } catch (_: Exception) {
            OperatingMode.NORMAL
        }
    }

    suspend fun setOperatingMode(mode: OperatingMode) {
        context.dataStore.edit { it[PreferencesKeys.OPERATING_MODE] = mode.name }
    }

    override val speechRateFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.SPEECH_RATE] ?: 1.0f
    }

    override suspend fun setSpeechRate(rate: Float) {
        context.dataStore.edit { it[PreferencesKeys.SPEECH_RATE] = rate }
    }

    override val speechPitchFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.SPEECH_PITCH] ?: 1.0f
    }

    override suspend fun setSpeechPitch(pitch: Float) {
        context.dataStore.edit { it[PreferencesKeys.SPEECH_PITCH] = pitch }
    }

    val voiceVolumeFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.VOICE_VOLUME] ?: 1.0f
    }

    suspend fun setVoiceVolume(volume: Float) {
        context.dataStore.edit { it[PreferencesKeys.VOICE_VOLUME] = volume }
    }

    val hapticsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.HAPTICS_ENABLED] ?: true
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.HAPTICS_ENABLED] = enabled }
    }

    val activeModelIdFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.ACTIVE_MODEL_ID] ?: "gemini-3.8-flash"
    }

    suspend fun setActiveModelId(modelId: String) {
        context.dataStore.edit { it[PreferencesKeys.ACTIVE_MODEL_ID] = modelId }
    }

    override val wakeWordEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.WAKE_WORD_ENABLED] ?: false
    }

    override suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.WAKE_WORD_ENABLED] = enabled }
    }

    override val wakeWordPhraseFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.WAKE_WORD_PHRASE] ?: "Hey AERIS"
    }

    override suspend fun setWakeWordPhrase(phrase: String) {
        context.dataStore.edit { it[PreferencesKeys.WAKE_WORD_PHRASE] = phrase }
    }

    val selectedTtsEngineFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.SELECTED_TTS_ENGINE]
    }

    suspend fun setSelectedTtsEngine(enginePackage: String) {
        context.dataStore.edit { it[PreferencesKeys.SELECTED_TTS_ENGINE] = enginePackage }
    }

    val selectedVoiceIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.SELECTED_VOICE_ID]
    }

    suspend fun setSelectedVoiceId(id: String) {
        context.dataStore.edit { it[PreferencesKeys.SELECTED_VOICE_ID] = id }
    }

    val backgroundListeningEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.BACKGROUND_LISTENING_ENABLED] ?: false
    }

    suspend fun setBackgroundListeningEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.BACKGROUND_LISTENING_ENABLED] = enabled }
    }

    override val voiceResponsesEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.VOICE_RESPONSES_ENABLED] ?: true
    }

    override suspend fun setVoiceResponsesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.VOICE_RESPONSES_ENABLED] = enabled }
    }

    override val autoPlayResponsesFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.AUTO_PLAY_RESPONSES] ?: false
    }

    override suspend fun setAutoPlayResponses(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_PLAY_RESPONSES] = enabled }
    }

    override val voiceInputEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.VOICE_INPUT_ENABLED] ?: true
    }

    override suspend fun setVoiceInputEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.VOICE_INPUT_ENABLED] = enabled }
    }

    override val pushToTalkEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.PUSH_TO_TALK_ENABLED] ?: false
    }

    override suspend fun setPushToTalkEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.PUSH_TO_TALK_ENABLED] = enabled }
    }

    override val recognitionLanguageFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.RECOGNITION_LANGUAGE] ?: "en-US"
    }

    override suspend fun setRecognitionLanguage(language: String) {
        context.dataStore.edit { it[PreferencesKeys.RECOGNITION_LANGUAGE] = language }
    }

    val animationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.ANIMATIONS_ENABLED] ?: true
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.ANIMATIONS_ENABLED] = enabled }
    }

    val visualEffectsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.VISUAL_EFFECTS_ENABLED] ?: true
    }

    suspend fun setVisualEffectsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.VISUAL_EFFECTS_ENABLED] = enabled }
    }

    val memoryEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.MEMORY_ENABLED] ?: true
    }

    suspend fun setMemoryEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.MEMORY_ENABLED] = enabled }
    }

    // --- Stage 1 AI Brain & Multi-Model Preferences ---

    val freeOnlyModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.FREE_ONLY_MODE] ?: true
    }

    suspend fun setFreeOnlyMode(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.FREE_ONLY_MODE] = enabled }
    }

    val autoSwitchEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.AUTO_SWITCH_ENABLED] ?: true
    }

    suspend fun setAutoSwitchEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_SWITCH_ENABLED] = enabled }
    }

    val aiOperatingModeFlow: Flow<com.example.ai.AiOperatingMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[PreferencesKeys.AI_OPERATING_MODE] ?: com.example.ai.AiOperatingMode.HYBRID.name
        try {
            com.example.ai.AiOperatingMode.valueOf(raw)
        } catch (_: Exception) {
            com.example.ai.AiOperatingMode.HYBRID
        }
    }

    suspend fun setAiOperatingMode(mode: com.example.ai.AiOperatingMode) {
        context.dataStore.edit { it[PreferencesKeys.AI_OPERATING_MODE] = mode.name }
    }

    val manualModelLockFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.MANUAL_MODEL_LOCK] ?: false
    }

    suspend fun setManualModelLock(locked: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.MANUAL_MODEL_LOCK] = locked }
    }

    val localFallbackEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.LOCAL_FALLBACK_ENABLED] ?: true
    }

    suspend fun setLocalFallbackEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.LOCAL_FALLBACK_ENABLED] = enabled }
    }

    val disabledProvidersFlow: Flow<Set<com.example.ai.AiProviderType>> = context.dataStore.data.map { prefs ->
        val rawSet = prefs[PreferencesKeys.DISABLED_PROVIDERS] ?: emptySet()
        rawSet.mapNotNull { name ->
            try {
                com.example.ai.AiProviderType.valueOf(name)
            } catch (_: Exception) {
                null
            }
        }.toSet()
    }

    suspend fun setProviderEnabled(providerType: com.example.ai.AiProviderType, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.DISABLED_PROVIDERS] ?: emptySet()
            prefs[PreferencesKeys.DISABLED_PROVIDERS] = if (enabled) {
                current - providerType.name
            } else {
                current + providerType.name
            }
        }
    }

    val defaultLocalModelPromptedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.DEFAULT_LOCAL_MODEL_PROMPTED] ?: false
    }

    suspend fun setDefaultLocalModelPrompted(prompted: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DEFAULT_LOCAL_MODEL_PROMPTED] = prompted }
    }

    val personalityStyleFlow: Flow<com.example.ai.AerisPersonalityStyle> = context.dataStore.data.map { prefs ->
        val raw = prefs[PreferencesKeys.PERSONALITY_STYLE] ?: com.example.ai.AerisPersonalityStyle.CALM_PROFESSIONAL.name
        try {
            com.example.ai.AerisPersonalityStyle.valueOf(raw)
        } catch (_: Exception) {
            com.example.ai.AerisPersonalityStyle.CALM_PROFESSIONAL
        }
    }

    suspend fun setPersonalityStyle(style: com.example.ai.AerisPersonalityStyle) {
        context.dataStore.edit { it[PreferencesKeys.PERSONALITY_STYLE] = style.name }
    }

    val responseLengthFlow: Flow<com.example.ai.AerisResponseLength> = context.dataStore.data.map { prefs ->
        val raw = prefs[PreferencesKeys.RESPONSE_LENGTH] ?: com.example.ai.AerisResponseLength.BALANCED.name
        try {
            com.example.ai.AerisResponseLength.valueOf(raw)
        } catch (_: Exception) {
            com.example.ai.AerisResponseLength.BALANCED
        }
    }

    suspend fun setResponseLength(length: com.example.ai.AerisResponseLength) {
        context.dataStore.edit { it[PreferencesKeys.RESPONSE_LENGTH] = length.name }
    }

    val preferredModelIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.PREFERRED_MODEL_ID]
    }

    suspend fun setPreferredModelId(modelId: String?) {
        context.dataStore.edit { prefs ->
            if (modelId.isNullOrBlank()) {
                prefs.remove(PreferencesKeys.PREFERRED_MODEL_ID)
            } else {
                prefs[PreferencesKeys.PREFERRED_MODEL_ID] = modelId
            }
        }
    }

    val openAiConfigFlow: Flow<Triple<String, String, String>> = context.dataStore.data.map { prefs ->
        Triple(
            prefs[PreferencesKeys.OPENAI_BASE_URL] ?: "",
            prefs[PreferencesKeys.OPENAI_MODEL_ID] ?: "",
            com.example.ai.CredentialVault.decrypt(prefs[PreferencesKeys.OPENAI_TOKEN] ?: "")
        )
    }

    suspend fun setOpenAiConfig(baseUrl: String, modelId: String, token: String) {
        context.dataStore.edit {
            it[PreferencesKeys.OPENAI_BASE_URL] = baseUrl.trim()
            it[PreferencesKeys.OPENAI_MODEL_ID] = modelId.trim()
            it[PreferencesKeys.OPENAI_TOKEN] = com.example.ai.CredentialVault.encrypt(token)
        }
    }

    val openRouterConfigFlow: Flow<Triple<String, String, String>> = context.dataStore.data.map { prefs ->
        Triple(
            prefs[PreferencesKeys.OPENROUTER_BASE_URL] ?: com.example.ai.AiProviderType.OPENROUTER.defaultBaseUrl,
            prefs[PreferencesKeys.OPENROUTER_MODEL_ID] ?: com.example.ai.AiProviderType.OPENROUTER.suggestedModelId,
            com.example.ai.CredentialVault.decrypt(prefs[PreferencesKeys.OPENROUTER_TOKEN] ?: "")
        )
    }

    suspend fun setOpenRouterConfig(baseUrl: String, modelId: String, token: String) {
        context.dataStore.edit {
            it[PreferencesKeys.OPENROUTER_BASE_URL] = baseUrl.trim()
            it[PreferencesKeys.OPENROUTER_MODEL_ID] = modelId.trim()
            it[PreferencesKeys.OPENROUTER_TOKEN] = com.example.ai.CredentialVault.encrypt(token)
        }
    }

    val groqConfigFlow: Flow<Triple<String, String, String>> = context.dataStore.data.map { prefs ->
        Triple(
            prefs[PreferencesKeys.GROQ_BASE_URL] ?: com.example.ai.AiProviderType.GROQ.defaultBaseUrl,
            prefs[PreferencesKeys.GROQ_MODEL_ID] ?: com.example.ai.AiProviderType.GROQ.suggestedModelId,
            com.example.ai.CredentialVault.decrypt(prefs[PreferencesKeys.GROQ_TOKEN] ?: "")
        )
    }

    suspend fun setGroqConfig(baseUrl: String, modelId: String, token: String) {
        context.dataStore.edit {
            it[PreferencesKeys.GROQ_BASE_URL] = baseUrl.trim()
            it[PreferencesKeys.GROQ_MODEL_ID] = modelId.trim()
            it[PreferencesKeys.GROQ_TOKEN] = com.example.ai.CredentialVault.encrypt(token)
        }
    }

    val huggingFaceConfigFlow: Flow<Triple<String, String, String>> = context.dataStore.data.map { prefs ->
        Triple(
            prefs[PreferencesKeys.HF_BASE_URL] ?: com.example.ai.AiProviderType.HUGGING_FACE.defaultBaseUrl,
            prefs[PreferencesKeys.HF_MODEL_ID] ?: com.example.ai.AiProviderType.HUGGING_FACE.suggestedModelId,
            com.example.ai.CredentialVault.decrypt(prefs[PreferencesKeys.HF_TOKEN] ?: "")
        )
    }

    suspend fun setHuggingFaceConfig(baseUrl: String, modelId: String, token: String) {
        context.dataStore.edit {
            it[PreferencesKeys.HF_BASE_URL] = baseUrl.trim()
            it[PreferencesKeys.HF_MODEL_ID] = modelId.trim()
            it[PreferencesKeys.HF_TOKEN] = com.example.ai.CredentialVault.encrypt(token)
        }
    }

    val gptOssConfigFlow: Flow<Triple<String, String, String>> = context.dataStore.data.map { prefs ->
        Triple(
            prefs[PreferencesKeys.GPT_OSS_BASE_URL] ?: "",
            prefs[PreferencesKeys.GPT_OSS_MODEL_ID] ?: "gpt-oss-20b",
            com.example.ai.CredentialVault.decrypt(prefs[PreferencesKeys.GPT_OSS_TOKEN] ?: "")
        )
    }

    suspend fun setGptOssConfig(baseUrl: String, modelId: String, token: String) {
        context.dataStore.edit {
            it[PreferencesKeys.GPT_OSS_BASE_URL] = baseUrl.trim()
            it[PreferencesKeys.GPT_OSS_MODEL_ID] = modelId.trim()
            it[PreferencesKeys.GPT_OSS_TOKEN] = com.example.ai.CredentialVault.encrypt(token)
        }
    }

    val qwenConfigFlow: Flow<Triple<String, String, String>> = context.dataStore.data.map { prefs ->
        Triple(
            prefs[PreferencesKeys.QWEN_BASE_URL] ?: "",
            prefs[PreferencesKeys.QWEN_MODEL_ID] ?: "qwen2.5-7b-instruct",
            com.example.ai.CredentialVault.decrypt(prefs[PreferencesKeys.QWEN_TOKEN] ?: "")
        )
    }

    suspend fun setQwenConfig(baseUrl: String, modelId: String, token: String) {
        context.dataStore.edit {
            it[PreferencesKeys.QWEN_BASE_URL] = baseUrl.trim()
            it[PreferencesKeys.QWEN_MODEL_ID] = modelId.trim()
            it[PreferencesKeys.QWEN_TOKEN] = com.example.ai.CredentialVault.encrypt(token)
        }
    }

    val cloudflareConfigFlow: Flow<Triple<String, String, String>> = context.dataStore.data.map { prefs ->
        Triple(
            prefs[PreferencesKeys.CLOUDFLARE_BASE_URL] ?: com.example.ai.AiProviderType.CLOUDFLARE_WORKERS_AI.defaultBaseUrl,
            prefs[PreferencesKeys.CLOUDFLARE_MODEL_ID] ?: com.example.ai.AiProviderType.CLOUDFLARE_WORKERS_AI.suggestedModelId,
            com.example.ai.CredentialVault.decrypt(prefs[PreferencesKeys.CLOUDFLARE_TOKEN] ?: "")
        )
    }

    suspend fun setCloudflareConfig(baseUrl: String, modelId: String, token: String) {
        context.dataStore.edit {
            it[PreferencesKeys.CLOUDFLARE_BASE_URL] = baseUrl.trim()
            it[PreferencesKeys.CLOUDFLARE_MODEL_ID] = modelId.trim()
            it[PreferencesKeys.CLOUDFLARE_TOKEN] = com.example.ai.CredentialVault.encrypt(token)
        }
    }

    suspend fun clearProviderConfig(providerType: com.example.ai.AiProviderType) {
        context.dataStore.edit { prefs ->
            when (providerType) {
                com.example.ai.AiProviderType.OPENAI_COMPATIBLE -> prefs.remove(PreferencesKeys.OPENAI_TOKEN)
                com.example.ai.AiProviderType.OPENROUTER -> prefs.remove(PreferencesKeys.OPENROUTER_TOKEN)
                com.example.ai.AiProviderType.GROQ -> prefs.remove(PreferencesKeys.GROQ_TOKEN)
                com.example.ai.AiProviderType.HUGGING_FACE -> prefs.remove(PreferencesKeys.HF_TOKEN)
                com.example.ai.AiProviderType.CLOUDFLARE_WORKERS_AI -> prefs.remove(PreferencesKeys.CLOUDFLARE_TOKEN)
                com.example.ai.AiProviderType.GPT_OSS_HOSTED -> prefs.remove(PreferencesKeys.GPT_OSS_TOKEN)
                com.example.ai.AiProviderType.QWEN_HOSTED -> prefs.remove(PreferencesKeys.QWEN_TOKEN)
                else -> Unit
            }
        }
    }

    // --- Stage 5-8 Controls ---

    val deviceActionConfirmSensitiveFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.DEVICE_ACTION_CONFIRM_SENSITIVE] ?: true
    }

    suspend fun setDeviceActionConfirmSensitive(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DEVICE_ACTION_CONFIRM_SENSITIVE] = enabled }
    }

    val notificationAnnouncerEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.NOTIFICATION_ANNOUNCER_ENABLED] ?: false
    }

    suspend fun setNotificationAnnouncerEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.NOTIFICATION_ANNOUNCER_ENABLED] = enabled }
    }

    val notificationAllowedAppsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.NOTIFICATION_ALLOWED_APPS] ?: emptySet()
    }

    suspend fun setNotificationAllowedApps(packages: Set<String>) {
        context.dataStore.edit { it[PreferencesKeys.NOTIFICATION_ALLOWED_APPS] = packages }
    }

    val notificationHideSensitiveFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.NOTIFICATION_HIDE_SENSITIVE] ?: true
    }

    suspend fun setNotificationHideSensitive(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.NOTIFICATION_HIDE_SENSITIVE] = enabled }
    }

    val gameCoachCloudAnalysisFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.GAME_COACH_CLOUD_ANALYSIS] ?: false
    }

    suspend fun setGameCoachCloudAnalysis(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.GAME_COACH_CLOUD_ANALYSIS] = enabled }
    }

    val batterySaverHoloFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.BATTERY_SAVER_HOLO] ?: false
    }

    suspend fun setBatterySaverHolo(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.BATTERY_SAVER_HOLO] = enabled }
    }

    val wakeWordPrivacyModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.WAKE_WORD_PRIVACY_MODE] ?: true
    }

    suspend fun setWakeWordPrivacyMode(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.WAKE_WORD_PRIVACY_MODE] = enabled }
    }

    val startOnBootOptInFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.START_ON_BOOT_OPT_IN] ?: false
    }

    suspend fun setStartOnBootOptIn(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.START_ON_BOOT_OPT_IN] = enabled }
    }

    val floatingAssistantEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.FLOATING_ASSISTANT_ENABLED] ?: false
    }

    suspend fun setFloatingAssistantEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.FLOATING_ASSISTANT_ENABLED] = enabled }
    }

    val customSystemInstructionFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.CUSTOM_SYSTEM_INSTRUCTION] ?: ""
    }

    suspend fun setCustomSystemInstruction(instruction: String) {
        context.dataStore.edit { it[PreferencesKeys.CUSTOM_SYSTEM_INSTRUCTION] = instruction.trim() }
    }

    val featureUsageCountsFlow: Flow<Map<String, Int>> = context.dataStore.data.map { prefs ->
        val raw = prefs[PreferencesKeys.FEATURE_USAGE_COUNTS].orEmpty()
        if (raw.isBlank()) {
            emptyMap()
        } else {
            raw.split(";").mapNotNull { entry ->
                val idx = entry.indexOf('=')
                if (idx <= 0) null else {
                    val key = entry.substring(0, idx)
                    val count = entry.substring(idx + 1).toIntOrNull() ?: 0
                    key to count
                }
            }.toMap()
        }
    }

    suspend fun incrementFeatureUsage(featureId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.FEATURE_USAGE_COUNTS].orEmpty()
                .split(";")
                .mapNotNull { entry ->
                    val idx = entry.indexOf('=')
                    if (idx <= 0) null else entry.substring(0, idx) to (entry.substring(idx + 1).toIntOrNull() ?: 0)
                }
                .toMap()
                .toMutableMap()
            current[featureId] = (current[featureId] ?: 0) + 1
            prefs[PreferencesKeys.FEATURE_USAGE_COUNTS] = current.entries.joinToString(";") { "${it.key}=${it.value}" }
        }
    }
}
