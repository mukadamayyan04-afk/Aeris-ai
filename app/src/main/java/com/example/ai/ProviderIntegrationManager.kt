package com.example.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.domain.model.ChatMessage
import com.example.domain.model.MemoryItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

/**
 * Engine 4: Provider Integration Manager
 * Registers all supported AI provider targets:
 * 1. Google Gemini (Firebase AI Logic + App Check)
 * 2. OpenAI API-Compatible Providers
 * 3. OpenRouter
 * 4. Groq (Fast LPU)
 * 5. Hugging Face Inference Router
 * 6. Compatible GPT-OSS Hosted Models
 * 7. Compatible Qwen Hosted Models
 * 8. Local GGUF & AERIS On-Device Core (Offline)
 *
 * Updates provider health honestly — external endpoints never claim Connected until tested.
 */
class ProviderIntegrationManager(
    private val context: Context,
    val healthManager: ModelHealthQuotaManager = ModelHealthQuotaManager(),
    val geminiAdapter: GeminiFirebaseProviderAdapter = GeminiFirebaseProviderAdapter(healthManager),
    val openAiAdapter: OpenAiCompatibleProviderAdapter = OpenAiCompatibleProviderAdapter(healthManager),
    val localAdapter: LocalOnDeviceProviderAdapter = LocalOnDeviceProviderAdapter()
) {
    private val _endpointConfigs = MutableStateFlow<Map<AiProviderType, ProviderEndpointConfig>>(emptyMap())
    val endpointConfigs: StateFlow<Map<AiProviderType, ProviderEndpointConfig>> = _endpointConfigs.asStateFlow()

    private val _disabledProviders = MutableStateFlow<Set<AiProviderType>>(emptySet())
    val disabledProviders: StateFlow<Set<AiProviderType>> = _disabledProviders.asStateFlow()

    private val _activeGgufModelId = MutableStateFlow<String?>(null)
    val activeGgufModelId: StateFlow<String?> = _activeGgufModelId.asStateFlow()

    fun setProviderEnabled(providerType: AiProviderType, enabled: Boolean) {
        _disabledProviders.update { current ->
            if (enabled) current - providerType else current + providerType
        }
        val existing = _endpointConfigs.value[providerType]
        if (existing != null) {
            _endpointConfigs.update { it + (providerType to existing.copy(isEnabled = enabled)) }
        }
    }

    fun setDisabledProviders(providers: Set<AiProviderType>) {
        _disabledProviders.value = providers
    }

    fun isProviderEnabled(providerType: AiProviderType): Boolean {
        if (providerType in _disabledProviders.value) return false
        return _endpointConfigs.value[providerType]?.isEnabled ?: true
    }

    fun updateEndpointConfig(config: ProviderEndpointConfig) {
        _endpointConfigs.update { current -> current + (config.providerType to config) }
        if (!config.isEnabled) {
            _disabledProviders.update { it + config.providerType }
        } else {
            _disabledProviders.update { it - config.providerType }
        }
        val descriptorId = descriptorIdForHostedProvider(config.providerType) ?: return
        when {
            config.baseUrl.isBlank() || config.modelId.isBlank() ||
                config.baseUrl.contains("ACCOUNT_ID", ignoreCase = true) -> {
                healthManager.markUnavailable(
                    descriptorId,
                    if (config.baseUrl.contains("ACCOUNT_ID", ignoreCase = true)) {
                        "Enter Cloudflare Account ID to complete endpoint URL."
                    } else {
                        "Endpoint Base URL or Model ID not configured."
                    }
                )
            }
            config.providerType.requiresApiKey && config.bearerToken.isBlank() -> {
                healthManager.markNeedsAuth(descriptorId, "Enter ${config.providerType.displayName} API token to authenticate.")
            }
            else -> {
                val quotaNote = if (config.providerType == AiProviderType.OPENROUTER &&
                    config.modelId.endsWith(":free", ignoreCase = true)
                ) {
                    "OpenRouter :free model configured (not tested)"
                } else {
                    "Configured (not tested)"
                }
                healthManager.markAvailable(
                    modelId = descriptorId,
                    hasAuth = !config.providerType.requiresApiKey || config.bearerToken.isNotBlank(),
                    quotaNote = quotaNote
                )
            }
        }
    }

    fun removeEndpointConfig(providerType: AiProviderType) {
        _endpointConfigs.update { current ->
            current + (providerType to ProviderEndpointConfig(providerType = providerType, bearerToken = "", isEnabled = isProviderEnabled(providerType)))
        }
        val descriptorId = descriptorIdForHostedProvider(providerType) ?: return
        healthManager.markNeedsAuth(descriptorId, "API key removed. Not connected.")
    }

    fun setLoadedGgufModel(modelId: String?) {
        _activeGgufModelId.value = modelId
        if (!modelId.isNullOrBlank()) {
            healthManager.markConnected(GGUF_MODEL_DESCRIPTOR_ID, 25L, ttftMs = 25L)
        } else {
            healthManager.markUnavailable(GGUF_MODEL_DESCRIPTOR_ID, "No GGUF file loaded")
        }
    }

    fun catalogDescriptors(): List<AiModelDescriptor> {
        val openaiCfg = _endpointConfigs.value[AiProviderType.OPENAI_COMPATIBLE]
        val openRouterCfg = _endpointConfigs.value[AiProviderType.OPENROUTER]
        val groqCfg = _endpointConfigs.value[AiProviderType.GROQ]
        val hfCfg = _endpointConfigs.value[AiProviderType.HUGGING_FACE]
        val cfCfg = _endpointConfigs.value[AiProviderType.CLOUDFLARE_WORKERS_AI]
        val gptOssCfg = _endpointConfigs.value[AiProviderType.GPT_OSS_HOSTED]
        val qwenCfg = _endpointConfigs.value[AiProviderType.QWEN_HOSTED]

        val openRouterModel = openRouterCfg?.modelId?.takeIf { it.isNotBlank() } ?: AiProviderType.OPENROUTER.suggestedModelId
        val openRouterIsFree = openRouterModel.endsWith(":free", ignoreCase = true)

        return listOf(
            AiModelDescriptor(
                id = GeminiFirebaseProviderAdapter.DEFAULT_GEMINI_MODEL,
                displayName = "Gemini 3.8 Flash (Firebase)",
                provider = AiProviderType.GEMINI_FIREBASE,
                modelName = GeminiFirebaseProviderAdapter.DEFAULT_GEMINI_MODEL,
                isFreeTierEligible = true,
                requiresBilling = false,
                supportsVision = true,
                isLightweightFast = false,
                strengths = setOf(
                    AiTaskCategory.GENERAL_CHAT,
                    AiTaskCategory.CODING,
                    AiTaskCategory.MATH_STEM,
                    AiTaskCategory.SUMMARIZATION,
                    AiTaskCategory.STUDY_HELP,
                    AiTaskCategory.TRANSLATION,
                    AiTaskCategory.WEB_RESEARCH,
                    AiTaskCategory.GAME_COACH
                ),
                priorityScore = 95,
                disclosureNote = "Default free-tier Gemini via Firebase AI Logic + App Check (subject to Firebase quotas)."
            ),
            AiModelDescriptor(
                id = GeminiFirebaseProviderAdapter.LITE_GEMINI_MODEL,
                displayName = "Gemini 3.5 Flash-Lite (Fast)",
                provider = AiProviderType.GEMINI_FIREBASE,
                modelName = GeminiFirebaseProviderAdapter.LITE_GEMINI_MODEL,
                isFreeTierEligible = true,
                requiresBilling = false,
                supportsVision = true,
                isLightweightFast = true,
                strengths = setOf(
                    AiTaskCategory.GENERAL_CHAT,
                    AiTaskCategory.TRANSLATION,
                    AiTaskCategory.SUMMARIZATION,
                    AiTaskCategory.DEVICE_ACTION
                ),
                priorityScore = 88,
                disclosureNote = "Ultra-fast lightweight free-tier Gemini model optimized for ordinary replies."
            ),
            AiModelDescriptor(
                id = GeminiFirebaseProviderAdapter.ARGON_GEMINI_MODEL,
                displayName = "Gemini 4 Argon (Pro Reasoning)",
                provider = AiProviderType.GEMINI_FIREBASE,
                modelName = GeminiFirebaseProviderAdapter.ARGON_GEMINI_MODEL,
                isFreeTierEligible = false,
                requiresBilling = true,
                supportsVision = true,
                strengths = setOf(
                    AiTaskCategory.CODING,
                    AiTaskCategory.MATH_STEM,
                    AiTaskCategory.SUMMARIZATION
                ),
                priorityScore = 98,
                disclosureNote = "Requires Firebase Blaze billing plan. Blocked while Free-Only Mode is ON."
            ),
            AiModelDescriptor(
                id = OPENROUTER_DESCRIPTOR_ID,
                displayName = openRouterCfg?.modelId?.takeIf { it.isNotBlank() }?.let { "OpenRouter ($it)" }
                    ?: "OpenRouter API",
                provider = AiProviderType.OPENROUTER,
                modelName = openRouterModel,
                isFreeTierEligible = openRouterIsFree,
                requiresBilling = !openRouterIsFree,
                isLightweightFast = openRouterModel.contains("8b", true) || openRouterModel.contains("flash", true),
                strengths = setOf(
                    AiTaskCategory.GENERAL_CHAT,
                    AiTaskCategory.CODING,
                    AiTaskCategory.MATH_STEM,
                    AiTaskCategory.WEB_RESEARCH
                ),
                priorityScore = 86,
                disclosureNote = if (openRouterIsFree) {
                    "OpenRouter :free endpoint configured (subject to provider free rate limits)."
                } else {
                    "Paid/non-:free OpenRouter model selected. Enable ':free' model suffix for Free-Only Mode."
                }
            ),
            AiModelDescriptor(
                id = GROQ_DESCRIPTOR_ID,
                displayName = groqCfg?.modelId?.takeIf { it.isNotBlank() }?.let { "Groq LPU ($it)" }
                    ?: "Groq Fast Inference",
                provider = AiProviderType.GROQ,
                modelName = groqCfg?.modelId?.takeIf { it.isNotBlank() } ?: AiProviderType.GROQ.suggestedModelId,
                isFreeTierEligible = true,
                requiresBilling = false,
                isLightweightFast = true,
                strengths = setOf(
                    AiTaskCategory.GENERAL_CHAT,
                    AiTaskCategory.CODING,
                    AiTaskCategory.TRANSLATION,
                    AiTaskCategory.STUDY_HELP
                ),
                priorityScore = 90,
                disclosureNote = "Groq OpenAI-compatible LPU endpoint (requires user API key; Groq free-tier RPM/TPM limits apply)."
            ),
            AiModelDescriptor(
                id = HUGGING_FACE_DESCRIPTOR_ID,
                displayName = hfCfg?.modelId?.takeIf { it.isNotBlank() }?.let { "Hugging Face ($it)" }
                    ?: "Hugging Face Inference",
                provider = AiProviderType.HUGGING_FACE,
                modelName = hfCfg?.modelId?.takeIf { it.isNotBlank() } ?: AiProviderType.HUGGING_FACE.suggestedModelId,
                isFreeTierEligible = true,
                requiresBilling = false,
                isLightweightFast = false,
                strengths = setOf(
                    AiTaskCategory.GENERAL_CHAT,
                    AiTaskCategory.CODING,
                    AiTaskCategory.TRANSLATION
                ),
                priorityScore = 79,
                disclosureNote = "Hugging Face Router (/v1) inference; requires HF token & subject to HF serverless quota."
            ),
            AiModelDescriptor(
                id = CLOUDFLARE_DESCRIPTOR_ID,
                displayName = cfCfg?.modelId?.takeIf { it.isNotBlank() }?.let { "Cloudflare Workers AI ($it)" }
                    ?: "Cloudflare Workers AI",
                provider = AiProviderType.CLOUDFLARE_WORKERS_AI,
                modelName = cfCfg?.modelId?.takeIf { it.isNotBlank() } ?: AiProviderType.CLOUDFLARE_WORKERS_AI.suggestedModelId,
                isFreeTierEligible = true,
                requiresBilling = false,
                isLightweightFast = true,
                strengths = setOf(
                    AiTaskCategory.GENERAL_CHAT,
                    AiTaskCategory.CODING,
                    AiTaskCategory.TRANSLATION,
                    AiTaskCategory.SUMMARIZATION
                ),
                priorityScore = 84,
                disclosureNote = "Cloudflare Workers AI OpenAI-compatible endpoint (requires Account ID & API token; free daily Neurons quota applies)."
            ),
            AiModelDescriptor(
                id = OPENAI_COMPAT_DESCRIPTOR_ID,
                displayName = openaiCfg?.modelId?.takeIf { it.isNotBlank() }?.let { "OpenAI-Compat ($it)" }
                    ?: "OpenAI API-Compatible",
                provider = AiProviderType.OPENAI_COMPATIBLE,
                modelName = openaiCfg?.modelId ?: "",
                isFreeTierEligible = true,
                requiresBilling = false,
                strengths = setOf(
                    AiTaskCategory.GENERAL_CHAT,
                    AiTaskCategory.CODING,
                    AiTaskCategory.SUMMARIZATION
                ),
                priorityScore = 80,
                disclosureNote = "User-configured /v1/chat/completions endpoint; provider rate limits or key billing may apply."
            ),
            AiModelDescriptor(
                id = GPT_OSS_DESCRIPTOR_ID,
                displayName = gptOssCfg?.modelId?.takeIf { it.isNotBlank() }?.let { "GPT-OSS ($it)" }
                    ?: "GPT-OSS Hosted Models",
                provider = AiProviderType.GPT_OSS_HOSTED,
                modelName = gptOssCfg?.modelId?.takeIf { it.isNotBlank() } ?: AiProviderType.GPT_OSS_HOSTED.suggestedModelId,
                isFreeTierEligible = true,
                requiresBilling = false,
                strengths = setOf(
                    AiTaskCategory.CODING,
                    AiTaskCategory.MATH_STEM,
                    AiTaskCategory.GENERAL_CHAT
                ),
                priorityScore = 82,
                disclosureNote = "User-hosted or open-weights OpenAI-compatible GPT-OSS server."
            ),
            AiModelDescriptor(
                id = QWEN_DESCRIPTOR_ID,
                displayName = qwenCfg?.modelId?.takeIf { it.isNotBlank() }?.let { "Qwen ($it)" }
                    ?: "Qwen Hosted Models",
                provider = AiProviderType.QWEN_HOSTED,
                modelName = qwenCfg?.modelId?.takeIf { it.isNotBlank() } ?: AiProviderType.QWEN_HOSTED.suggestedModelId,
                isFreeTierEligible = true,
                requiresBilling = false,
                strengths = setOf(
                    AiTaskCategory.CODING,
                    AiTaskCategory.TRANSLATION,
                    AiTaskCategory.STUDY_HELP
                ),
                priorityScore = 81,
                disclosureNote = "User-configured Qwen compatible inference endpoint (DashScope/vLLM/Ollama)."
            ),
            AiModelDescriptor(
                id = GGUF_MODEL_DESCRIPTOR_ID,
                displayName = _activeGgufModelId.value?.let { "Local GGUF ($it)" } ?: "Local GGUF Engine",
                provider = AiProviderType.LOCAL_GGUF,
                modelName = _activeGgufModelId.value ?: "unloaded",
                isFreeTierEligible = true,
                requiresBilling = false,
                isLightweightFast = true,
                strengths = setOf(AiTaskCategory.GENERAL_CHAT, AiTaskCategory.DEVICE_ACTION),
                priorityScore = 60,
                disclosureNote = "Offline downloaded/imported GGUF weights (e.g., Gemma 3 1B Q4)."
            ),
            AiModelDescriptor(
                id = ModelHealthQuotaManager.LOCAL_CORE_MODEL_ID,
                displayName = "AERIS On-Device Core (Offline)",
                provider = AiProviderType.ON_DEVICE_CORE,
                modelName = ModelHealthQuotaManager.LOCAL_CORE_MODEL_ID,
                isFreeTierEligible = true,
                requiresBilling = false,
                isLightweightFast = true,
                strengths = setOf(
                    AiTaskCategory.DEVICE_ACTION,
                    AiTaskCategory.GENERAL_CHAT
                ),
                priorityScore = 30,
                disclosureNote = "Always-available 100% free offline rule & memory fallback."
            )
        )
    }

    fun isNetworkAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    suspend fun testModelConnection(modelId: String): Result<Long> {
        val descriptor = catalogDescriptors().firstOrNull { it.id == modelId }
            ?: return Result.failure(IllegalArgumentException("Unknown model: $modelId"))
        return when (descriptor.provider) {
            AiProviderType.GEMINI_FIREBASE -> {
                geminiAdapter.testConnection(descriptor.modelName).first()
            }
            AiProviderType.OPENAI_COMPATIBLE,
            AiProviderType.OPENROUTER,
            AiProviderType.GROQ,
            AiProviderType.HUGGING_FACE,
            AiProviderType.CLOUDFLARE_WORKERS_AI,
            AiProviderType.GPT_OSS_HOSTED,
            AiProviderType.QWEN_HOSTED -> {
                val cfg = _endpointConfigs.value[descriptor.provider]
                    ?: ProviderEndpointConfig(descriptor.provider)
                openAiAdapter.testConnection(descriptor.id, cfg)
            }
            AiProviderType.LOCAL_GGUF -> {
                if (!_activeGgufModelId.value.isNullOrBlank()) {
                    healthManager.markConnected(descriptor.id, 15L, ttftMs = 15L)
                    Result.success(15L)
                } else {
                    val msg = "No GGUF weight file is currently loaded."
                    healthManager.markUnavailable(descriptor.id, msg)
                    Result.failure(IllegalStateException(msg))
                }
            }
            AiProviderType.ON_DEVICE_CORE -> {
                healthManager.markConnected(descriptor.id, 5L, ttftMs = 5L)
                Result.success(5L)
            }
        }
    }

    suspend fun discoverProviderModels(modelId: String): Result<OpenAiCompatibleProviderAdapter.DiscoveryResult> {
        val descriptor = catalogDescriptors().firstOrNull { it.id == modelId }
            ?: return Result.failure(IllegalArgumentException("Unknown model: $modelId"))
        if (!descriptor.provider.isRemoteOpenAiCompatible()) {
            return Result.success(
                OpenAiCompatibleProviderAdapter.DiscoveryResult(
                    modelIds = if (descriptor.provider == AiProviderType.GEMINI_FIREBASE) {
                        GeminiFirebaseProviderAdapter.KNOWN_GEMINI_MODELS
                    } else {
                        listOf(descriptor.modelName)
                    },
                    freeModelIds = listOf(
                        GeminiFirebaseProviderAdapter.DEFAULT_GEMINI_MODEL,
                        GeminiFirebaseProviderAdapter.LITE_GEMINI_MODEL
                    ),
                    latencyMs = 1L
                )
            )
        }
        val cfg = _endpointConfigs.value[descriptor.provider] ?: ProviderEndpointConfig(descriptor.provider)
        return openAiAdapter.discoverModels(descriptor.id, cfg)
    }

    suspend fun testAllConfiguredProviders(freeOnlyMode: Boolean): Map<String, Result<Long>> = coroutineScope {
        val networkUp = isNetworkAvailable()
        val targets = catalogDescriptors().filter { desc ->
            if (!isProviderEnabled(desc.provider)) return@filter false
            if (freeOnlyMode && (desc.requiresBilling || !desc.isFreeTierEligible)) return@filter false
            when (desc.provider) {
                AiProviderType.GEMINI_FIREBASE -> networkUp
                AiProviderType.OPENAI_COMPATIBLE,
                AiProviderType.OPENROUTER,
                AiProviderType.GROQ,
                AiProviderType.HUGGING_FACE,
                AiProviderType.CLOUDFLARE_WORKERS_AI,
                AiProviderType.GPT_OSS_HOSTED,
                AiProviderType.QWEN_HOSTED -> {
                    val cfg = _endpointConfigs.value[desc.provider]
                    networkUp && cfg != null && cfg.hasBaseAndModelOnly
                }
                AiProviderType.LOCAL_GGUF -> !_activeGgufModelId.value.isNullOrBlank()
                AiProviderType.ON_DEVICE_CORE -> true
            }
        }
        targets.map { desc ->
            async { desc.id to testModelConnection(desc.id) }
        }.awaitAll().toMap()
    }

    fun streamWithDescriptor(
        descriptor: AiModelDescriptor,
        builtPrompt: PromptCreationEngine.BuiltPrompt,
        rawPrompt: String,
        history: List<ChatMessage>,
        memories: List<MemoryItem>,
        continuity: ContinuitySnapshot?
    ): Flow<String> {
        return when (descriptor.provider) {
            AiProviderType.GEMINI_FIREBASE -> {
                geminiAdapter.streamText(descriptor.modelName, builtPrompt)
            }
            AiProviderType.OPENAI_COMPATIBLE,
            AiProviderType.OPENROUTER,
            AiProviderType.GROQ,
            AiProviderType.HUGGING_FACE,
            AiProviderType.CLOUDFLARE_WORKERS_AI,
            AiProviderType.GPT_OSS_HOSTED,
            AiProviderType.QWEN_HOSTED -> {
                val cfg = _endpointConfigs.value[descriptor.provider]
                    ?: ProviderEndpointConfig(descriptor.provider)
                openAiAdapter.streamChat(descriptor.id, cfg, builtPrompt)
            }
            AiProviderType.LOCAL_GGUF,
            AiProviderType.ON_DEVICE_CORE -> {
                localAdapter.streamOfflineResponse(rawPrompt, history, memories, continuity)
            }
        }
    }

    private fun AiProviderType.isRemoteOpenAiCompatible(): Boolean = this in setOf(
        AiProviderType.OPENAI_COMPATIBLE,
        AiProviderType.OPENROUTER,
        AiProviderType.GROQ,
        AiProviderType.HUGGING_FACE,
        AiProviderType.CLOUDFLARE_WORKERS_AI,
        AiProviderType.GPT_OSS_HOSTED,
        AiProviderType.QWEN_HOSTED
    )

    fun descriptorIdForHostedProvider(type: AiProviderType): String? = when (type) {
        AiProviderType.OPENAI_COMPATIBLE -> OPENAI_COMPAT_DESCRIPTOR_ID
        AiProviderType.OPENROUTER -> OPENROUTER_DESCRIPTOR_ID
        AiProviderType.GROQ -> GROQ_DESCRIPTOR_ID
        AiProviderType.HUGGING_FACE -> HUGGING_FACE_DESCRIPTOR_ID
        AiProviderType.CLOUDFLARE_WORKERS_AI -> CLOUDFLARE_DESCRIPTOR_ID
        AiProviderType.GPT_OSS_HOSTED -> GPT_OSS_DESCRIPTOR_ID
        AiProviderType.QWEN_HOSTED -> QWEN_DESCRIPTOR_ID
        else -> null
    }

    companion object {
        const val OPENAI_COMPAT_DESCRIPTOR_ID = "openai-compatible-custom"
        const val OPENROUTER_DESCRIPTOR_ID = "openrouter-hosted"
        const val GROQ_DESCRIPTOR_ID = "groq-hosted"
        const val HUGGING_FACE_DESCRIPTOR_ID = "huggingface-inference"
        const val CLOUDFLARE_DESCRIPTOR_ID = "cloudflare-workers-ai"
        const val GPT_OSS_DESCRIPTOR_ID = "gpt-oss-hosted"
        const val QWEN_DESCRIPTOR_ID = "qwen-hosted"
        const val GGUF_MODEL_DESCRIPTOR_ID = "local-gguf-active"
    }
}
