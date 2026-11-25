package com.example.ai

import android.graphics.Bitmap
import com.example.domain.model.ActionType

enum class AiProviderType(
    val displayName: String,
    val defaultBaseUrl: String = "",
    val suggestedModelId: String = "",
    val requiresApiKey: Boolean = false
) {
    GEMINI_FIREBASE(
        displayName = "Google Gemini (Firebase AI Logic)"
    ),
    OPENAI_COMPATIBLE(
        displayName = "OpenAI API-Compatible",
        defaultBaseUrl = "https://api.openai.com/v1",
        suggestedModelId = "gpt-4o-mini",
        requiresApiKey = false
    ),
    OPENROUTER(
        displayName = "OpenRouter",
        defaultBaseUrl = "https://openrouter.ai/api/v1",
        suggestedModelId = "meta-llama/llama-3.3-70b-instruct:free",
        requiresApiKey = true
    ),
    GROQ(
        displayName = "Groq (Fast LPU)",
        defaultBaseUrl = "https://api.groq.com/openai/v1",
        suggestedModelId = "llama-3.1-8b-instant",
        requiresApiKey = true
    ),
    HUGGING_FACE(
        displayName = "Hugging Face Inference",
        defaultBaseUrl = "https://router.huggingface.co/v1",
        suggestedModelId = "Qwen/Qwen2.5-7B-Instruct",
        requiresApiKey = true
    ),
    GPT_OSS_HOSTED(
        displayName = "GPT-OSS Hosted Models",
        defaultBaseUrl = "",
        suggestedModelId = "gpt-oss-20b",
        requiresApiKey = false
    ),
    QWEN_HOSTED(
        displayName = "Qwen Hosted Models",
        defaultBaseUrl = "",
        suggestedModelId = "qwen2.5-7b-instruct",
        requiresApiKey = false
    ),
    CLOUDFLARE_WORKERS_AI(
        displayName = "Cloudflare Workers AI",
        defaultBaseUrl = "https://api.cloudflare.com/client/v4/accounts/ACCOUNT_ID/ai/v1",
        suggestedModelId = "@cf/meta/llama-3.1-8b-instruct",
        requiresApiKey = true
    ),
    LOCAL_GGUF(
        displayName = "Local GGUF (llama.cpp)"
    ),
    ON_DEVICE_CORE(
        displayName = "AERIS On-Device Core (Offline)"
    )
}

enum class AiOperatingMode(val title: String, val subtitle: String) {
    HYBRID("Hybrid Mode", "Connected online AI + automatic local fallback"),
    ONLINE_ONLY("Online-Only Mode", "Use connected online AI only; refuses local fallback"),
    LOCAL_ONLY("Local-Only Mode", "Use downloaded local model / offline core; zero cloud requests")
}

enum class LocalModelLifecycleState(val label: String) {
    NOT_DOWNLOADED("Not Downloaded"),
    DOWNLOADING("Downloading"),
    PAUSED("Paused"),
    READY("Ready"),
    LOADING("Loading"),
    RUNNING("Running"),
    ERROR("Error")
}

enum class ProviderConnectionUiStatus(val label: String) {
    NOT_CONNECTED("Not Connected"),
    NEEDS_AUTHENTICATION("Needs Authentication"),
    CONNECTING("Connecting"),
    CONNECTED("Connected"),
    ERROR("Error"),
    RATE_LIMITED("Rate Limited"),
    TEMPORARILY_UNAVAILABLE("Temporarily Unavailable"),
    DISABLED("Disabled")
}

enum class AerisPersonalityStyle(val title: String, val directive: String) {
    CALM_PROFESSIONAL("Calm & Professional", "Poised, intelligent, warm, clear, technically sharp, and calm."),
    CONCISE_TECHNICAL("Concise & Technical", "Crisp, engineering-focused, structured, and minimal fluff."),
    WARM_FRIENDLY("Warm & Supportive", "Approachable, encouraging, empathetic, and polite."),
    LIGHT_WIT("Lightly Witty", "Poised, sharp, and lightly witty when appropriate while staying helpful.")
}

enum class AerisResponseLength(val title: String, val directive: String) {
    CONCISE("Concise", "Keep replies brief (2-5 clear sentences) unless full code or step-by-step detail is requested."),
    BALANCED("Balanced", "Give well-proportioned answers with practical explanation without rambling."),
    DETAILED("Detailed", "Provide thorough, comprehensive explanations, edge cases, and examples.")
}

enum class ModelHealthStatus(val label: String) {
    CONNECTED("Connected"),
    AVAILABLE("Available"),
    NEEDS_AUTH("Needs Authentication"),
    RATE_LIMITED("Rate Limited"),
    QUOTA_EXCEEDED("Rate Limited"),
    UNAVAILABLE("Temporarily Unavailable"),
    NOT_TESTED("Not Connected"),
    UNCONNECTED("Not Connected"),
    TESTING("Connecting"),
    ERROR("Error")
}

enum class AiTaskCategory(val label: String) {
    GENERAL_CHAT("General Chat"),
    CODING("Coding & Architecture"),
    MATH_STEM("Math & STEM"),
    SUMMARIZATION("Summarization"),
    DEVICE_ACTION("Android Device Action"),
    IMAGE_GENERATION("Image Generation"),
    WEB_RESEARCH("Web Research"),
    GAME_COACH("Game Coach Vision"),
    TRANSLATION("Translation"),
    STUDY_HELP("Study Assistance")
}

data class AiModelDescriptor(
    val id: String,
    val displayName: String,
    val provider: AiProviderType,
    val modelName: String,
    val isFreeTierEligible: Boolean,
    val requiresBilling: Boolean = false,
    val supportsStreaming: Boolean = true,
    val supportsVision: Boolean = false,
    val supportsImageGeneration: Boolean = false,
    val isLightweightFast: Boolean = false,
    val strengths: Set<AiTaskCategory> = emptySet(),
    val priorityScore: Int = 50,
    val disclosureNote: String = ""
)

data class ModelHealthState(
    val modelId: String,
    val status: ModelHealthStatus = ModelHealthStatus.NOT_TESTED,
    val lastLatencyMs: Long? = null,
    val lastTtftMs: Long? = null,
    val lastTotalGenMs: Long? = null,
    val lastError: String? = null,
    val quotaStatus: String? = null,
    val remainingRequests: String? = null,
    val remainingTokens: String? = null,
    val hasAuthentication: Boolean = false,
    val discoveredModels: List<String> = emptyList(),
    val consecutiveFailures: Int = 0,
    val cooldownUntilMillis: Long = 0L,
    val lastCheckedAtMillis: Long = 0L,
    val lastSuccessfulAtMillis: Long = 0L
) {
    fun isInCooldown(nowMillis: Long = System.currentTimeMillis()): Boolean =
        cooldownUntilMillis > nowMillis
}

data class ProviderEndpointConfig(
    val providerType: AiProviderType,
    val baseUrl: String = providerType.defaultBaseUrl,
    val modelId: String = providerType.suggestedModelId,
    val bearerToken: String = "",
    val isEnabled: Boolean = true
) {
    val isConfigured: Boolean
        get() {
            if (baseUrl.isBlank() || modelId.isBlank()) return false
            if (baseUrl.contains("ACCOUNT_ID", ignoreCase = true)) return false
            return !providerType.requiresApiKey || bearerToken.isNotBlank()
        }

    val hasBaseAndModelOnly: Boolean
        get() = baseUrl.isNotBlank() &&
            modelId.isNotBlank() &&
            !baseUrl.contains("ACCOUNT_ID", ignoreCase = true)

    fun maskedToken(): String = CredentialVault.maskKey(bearerToken)
}

data class ReasoningPlan(
    val category: AiTaskCategory,
    val complexityScore: Int, // 1..5
    val requiresVision: Boolean = false,
    val suggestedActionType: ActionType? = null,
    val suggestedActionDetail: String? = null,
    val rationale: String
)

data class ContinuitySnapshot(
    val conversationId: Long,
    val rollingSummary: String,
    val projectContext: String,
    val attachedFileName: String? = null,
    val attachedFileExcerpt: String? = null,
    val activeTasksSummary: String = "",
    val previousModelId: String? = null
)

data class RoutingDecision(
    val selectedModel: AiModelDescriptor,
    val candidateOrder: List<String>,
    val switchedFromModelId: String? = null,
    val switchReason: String? = null,
    val blockedModels: List<String> = emptyList(),
    val noFreeCloudReason: String? = null
)

data class BrainGenerationEvent(
    val textChunk: String = "",
    val activeModelId: String = "",
    val activeModelName: String = "",
    val routingDecision: RoutingDecision? = null,
    val telemetrySample: RequestLatencySample? = null,
    val errorMessage: String? = null
)

sealed interface ImageGenResult {
    data object Idle : ImageGenResult
    data object Loading : ImageGenResult
    data class Success(
        val bitmap: Bitmap,
        val modelUsed: String,
        val enhancedPrompt: String? = null
    ) : ImageGenResult
    data class Error(val message: String) : ImageGenResult
}
