package com.example.ai

import android.content.Context
import android.os.Build
import com.example.data.database.UserFeedbackDao
import com.example.domain.model.ChatMessage
import com.example.domain.model.MemoryItem
import com.example.domain.model.UserFeedbackRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong

enum class ActiveAiProvider {
    GEMINI_FIREBASE,
    OPENAI_COMPATIBLE,
    OPENROUTER,
    GROQ,
    HUGGING_FACE,
    CLOUDFLARE_WORKERS_AI,
    GPT_OSS_HOSTED,
    QWEN_HOSTED,
    LOCAL_GGUF_MODEL,
    ON_DEVICE_OFFLINE
}

/**
 * AERIS AI Independent Brain Orchestrator:
 * Coordinates all 11 modular AI engines + PerformanceTelemetryTracker:
 * - Streams first tokens immediately
 * - Tracks real Android prep, TTFT, network delay, generation throughput, and failover latency
 * - Supports Google Gemini, OpenAI-Compat, OpenRouter, Groq, Hugging Face, Cloudflare Workers AI, GPT-OSS, Qwen, Local GGUF & Offline Core
 * - Enforces Operating Mode (Hybrid / Online-Only / Local-Only), Free-Only Mode, Manual Lock, Local Fallback, and ContinuitySnapshot
 */
class AerisAiEngine(
    private val context: Context,
    private val localEngine: LocalLlmEngine = LocalLlmEngine(),
    feedbackDao: UserFeedbackDao? = null
) : LlmEngine {

    private val isArm64 = Build.SUPPORTED_ABIS.any { it.contains("arm64-v8a", ignoreCase = true) }
    private val requestSeq = AtomicLong(0L)

    // 11 Modular AI Brain Engines + Real Performance Telemetry
    val personalityEngine = PersonalityEngine()
    val reasoningPlanningEngine = ReasoningPlanningEngine()
    val healthQuotaManager = ModelHealthQuotaManager()
    val telemetryTracker = PerformanceTelemetryTracker()
    val localProviderAdapter = LocalOnDeviceProviderAdapter(localEngine)
    val providerIntegrationManager = ProviderIntegrationManager(
        context = context,
        healthManager = healthQuotaManager,
        localAdapter = localProviderAdapter
    )
    val multiModelRouter = MultiModelRouter(providerIntegrationManager)
    val continuityEngine = ConversationContinuityEngine()
    val longTermMemoryEngine = LongTermMemoryEngine()
    val feedbackEngine = UserFeedbackPreferenceEngine(feedbackDao)
    val promptCreationEngine = PromptCreationEngine(personalityEngine, feedbackEngine)
    val toolTaskExecutionEngine = ToolTaskExecutionEngine()
    val imageGenerationManager = ImageGenerationManager(
        context = context,
        healthManager = healthQuotaManager,
        geminiAdapter = providerIntegrationManager.geminiAdapter
    )

    val telemetrySummary: StateFlow<TelemetrySummary> = telemetryTracker.summary
    val telemetrySamples: StateFlow<List<RequestLatencySample>> = telemetryTracker.samples

    private val _activeProvider = MutableStateFlow(ActiveAiProvider.ON_DEVICE_OFFLINE)
    val activeProvider: StateFlow<ActiveAiProvider> = _activeProvider.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _lastRoutingDecision = MutableStateFlow<RoutingDecision?>(null)
    val lastRoutingDecision: StateFlow<RoutingDecision?> = _lastRoutingDecision.asStateFlow()

    private val _freeOnlyMode = MutableStateFlow(true)
    val freeOnlyMode: StateFlow<Boolean> = _freeOnlyMode.asStateFlow()

    private val _autoSwitchEnabled = MutableStateFlow(true)
    val autoSwitchEnabled: StateFlow<Boolean> = _autoSwitchEnabled.asStateFlow()

    private val _preferredModelId = MutableStateFlow<String?>(GeminiFirebaseProviderAdapter.DEFAULT_GEMINI_MODEL)
    val preferredModelId: StateFlow<String?> = _preferredModelId.asStateFlow()

    private val _operatingMode = MutableStateFlow(AiOperatingMode.HYBRID)
    val operatingMode: StateFlow<AiOperatingMode> = _operatingMode.asStateFlow()

    private val _manualModelLock = MutableStateFlow(false)
    val manualModelLock: StateFlow<Boolean> = _manualModelLock.asStateFlow()

    private val _localFallbackEnabled = MutableStateFlow(true)
    val localFallbackEnabled: StateFlow<Boolean> = _localFallbackEnabled.asStateFlow()

    private val _personalityStyle = MutableStateFlow(AerisPersonalityStyle.CALM_PROFESSIONAL)
    val personalityStyle: StateFlow<AerisPersonalityStyle> = _personalityStyle.asStateFlow()

    private val _responseLength = MutableStateFlow(AerisResponseLength.BALANCED)
    val responseLength: StateFlow<AerisResponseLength> = _responseLength.asStateFlow()

    @Volatile
    private var cachedApprovedFeedback: List<UserFeedbackRecord> = emptyList()

    @Volatile
    private var customSystemInstruction: String = ""

    private val _state = MutableStateFlow(
        ModelEngineState(
            isLoaded = true,
            modelId = GeminiFirebaseProviderAdapter.DEFAULT_GEMINI_MODEL,
            modelName = "AERIS Brain (Gemini 3.8 Flash / Offline Standby)",
            engineType = "Independent 11-Engine Hybrid Router",
            memoryUsageMb = 48L,
            nativeLibraryStatus = "App Check + Multi-Provider Router Ready",
            supportsArm64V8a = isArm64
        )
    )
    override val state: Flow<ModelEngineState> = _state.asStateFlow()

    @Volatile
    private var isCancelled = false

    fun updateCachedFeedback(rules: List<UserFeedbackRecord>) {
        cachedApprovedFeedback = rules
    }

    fun updatePersonalityPreferences(
        style: AerisPersonalityStyle,
        responseLength: AerisResponseLength,
        customInstruction: String = customSystemInstruction
    ) {
        _personalityStyle.value = style
        _responseLength.value = responseLength
        customSystemInstruction = customInstruction
    }

    fun updateRoutingPreferences(
        freeOnly: Boolean,
        autoSwitch: Boolean,
        preferredId: String?,
        operatingMode: AiOperatingMode = _operatingMode.value,
        manualModelLock: Boolean = _manualModelLock.value,
        localFallbackEnabled: Boolean = _localFallbackEnabled.value
    ) {
        _freeOnlyMode.value = freeOnly
        _autoSwitchEnabled.value = autoSwitch
        _preferredModelId.value = preferredId
        _operatingMode.value = operatingMode
        _manualModelLock.value = manualModelLock
        _localFallbackEnabled.value = localFallbackEnabled
    }

    fun updateEndpointConfig(config: ProviderEndpointConfig) {
        providerIntegrationManager.updateEndpointConfig(config)
    }

    fun removeEndpointConfig(providerType: AiProviderType) {
        providerIntegrationManager.removeEndpointConfig(providerType)
    }

    fun setProviderEnabled(providerType: AiProviderType, enabled: Boolean) {
        providerIntegrationManager.setProviderEnabled(providerType, enabled)
    }

    fun setDisabledProviders(providers: Set<AiProviderType>) {
        providerIntegrationManager.setDisabledProviders(providers)
    }

    fun setProvider(provider: ActiveAiProvider) {
        _activeProvider.value = provider
        val modelId = when (provider) {
            ActiveAiProvider.GEMINI_FIREBASE -> GeminiFirebaseProviderAdapter.DEFAULT_GEMINI_MODEL
            ActiveAiProvider.OPENAI_COMPATIBLE -> ProviderIntegrationManager.OPENAI_COMPAT_DESCRIPTOR_ID
            ActiveAiProvider.OPENROUTER -> ProviderIntegrationManager.OPENROUTER_DESCRIPTOR_ID
            ActiveAiProvider.GROQ -> ProviderIntegrationManager.GROQ_DESCRIPTOR_ID
            ActiveAiProvider.HUGGING_FACE -> ProviderIntegrationManager.HUGGING_FACE_DESCRIPTOR_ID
            ActiveAiProvider.CLOUDFLARE_WORKERS_AI -> ProviderIntegrationManager.CLOUDFLARE_DESCRIPTOR_ID
            ActiveAiProvider.GPT_OSS_HOSTED -> ProviderIntegrationManager.GPT_OSS_DESCRIPTOR_ID
            ActiveAiProvider.QWEN_HOSTED -> ProviderIntegrationManager.QWEN_DESCRIPTOR_ID
            ActiveAiProvider.LOCAL_GGUF_MODEL -> ProviderIntegrationManager.GGUF_MODEL_DESCRIPTOR_ID
            ActiveAiProvider.ON_DEVICE_OFFLINE -> ModelHealthQuotaManager.LOCAL_CORE_MODEL_ID
        }
        _preferredModelId.value = modelId
        updateStateDisplay(modelId, modelId)
    }

    suspend fun testConnection(modelId: String): Result<Long> {
        return providerIntegrationManager.testModelConnection(modelId)
    }

    suspend fun discoverModels(modelId: String): Result<OpenAiCompatibleProviderAdapter.DiscoveryResult> {
        return providerIntegrationManager.discoverProviderModels(modelId)
    }

    suspend fun testAllConfiguredProviders(): Map<String, Result<Long>> {
        return providerIntegrationManager.testAllConfiguredProviders(_freeOnlyMode.value)
    }

    override suspend fun loadModel(modelPath: String, modelId: String): Result<Boolean> = withContext(Dispatchers.Default) {
        val result = localEngine.loadModel(modelPath, modelId)
        if (result.isSuccess) {
            providerIntegrationManager.setLoadedGgufModel(modelId)
            _activeProvider.value = ActiveAiProvider.LOCAL_GGUF_MODEL
            updateStateDisplay(modelId, "GGUF: $modelId")
        }
        result
    }

    override suspend fun unloadModel(): Result<Unit> = withContext(Dispatchers.Default) {
        val result = localEngine.unloadModel()
        providerIntegrationManager.setLoadedGgufModel(null)
        _activeProvider.value = ActiveAiProvider.ON_DEVICE_OFFLINE
        updateStateDisplay(ModelHealthQuotaManager.LOCAL_CORE_MODEL_ID, "AERIS On-Device Core (Offline)")
        result
    }

    override suspend fun isModelLoaded(): Boolean = _state.value.isLoaded

    override fun generateStream(
        prompt: String,
        history: List<ChatMessage>,
        memories: List<MemoryItem>
    ): Flow<String> = flow {
        generateWithBrain(
            prompt = prompt,
            history = history,
            memories = memories,
            continuity = null,
            forceOfflineOnly = false
        ).collect { event ->
            if (event.errorMessage != null && event.textChunk.isEmpty()) {
                throw IllegalStateException(event.errorMessage)
            }
            if (event.textChunk.isNotEmpty()) {
                emit(event.textChunk)
            }
        }
    }

    fun generateWithBrain(
        prompt: String,
        history: List<ChatMessage>,
        memories: List<MemoryItem>,
        continuity: ContinuitySnapshot?,
        forceOfflineOnly: Boolean = false,
        userName: String = "Sir ji"
    ): Flow<BrainGenerationEvent> = flow {
        isCancelled = false
        _lastError.value = null

        val effectiveMode = if (forceOfflineOnly) AiOperatingMode.LOCAL_ONLY else _operatingMode.value
        val effectiveNetworkAvailable = if (effectiveMode == AiOperatingMode.LOCAL_ONLY) {
            false
        } else {
            providerIntegrationManager.isNetworkAvailable()
        }

        val reqId = requestSeq.incrementAndGet()
        val prepStartMs = System.currentTimeMillis()

        val plan = reasoningPlanningEngine.analyze(
            prompt = prompt,
            hasAttachedDocument = !continuity?.attachedFileExcerpt.isNullOrBlank()
        )
        val relevantMemories = longTermMemoryEngine.selectRelevantMemories(
            prompt = prompt,
            memories = memories,
            projectTag = continuity?.projectContext.orEmpty(),
            maxItems = if (plan.complexityScore <= 2) 3 else 6
        )
        val approvedFeedback: List<UserFeedbackRecord> = cachedApprovedFeedback.ifEmpty {
            try {
                feedbackEngine.getApprovedFeedback()?.first() ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }
        val builtPrompt = promptCreationEngine.build(
            userPrompt = prompt,
            plan = plan,
            history = history,
            relevantMemories = relevantMemories,
            continuity = continuity,
            approvedFeedback = approvedFeedback,
            userName = userName,
            personalityStyle = _personalityStyle.value,
            responseLength = _responseLength.value,
            customSystemInstruction = customSystemInstruction
        )

        val androidPreProcessMs = (System.currentTimeMillis() - prepStartMs).coerceAtLeast(1L)

        val triedIds = mutableSetOf<String>()
        var currentPreviousModel = continuity?.previousModelId
        val maxAttempts = if (_autoSwitchEnabled.value && !_manualModelLock.value) {
            providerIntegrationManager.catalogDescriptors().size.coerceAtLeast(3)
        } else 1

        var completed = false
        var lastFailure: String? = null
        var firstAttemptFailTimeMs: Long? = null

        for (attempt in 0 until maxAttempts) {
            if (isCancelled) break
            if (attempt > 0) {
                delay((attempt * 120L).coerceAtMost(320L))
            }

            val decision = multiModelRouter.selectRoute(
                plan = plan,
                freeOnlyMode = _freeOnlyMode.value,
                autoSwitchEnabled = _autoSwitchEnabled.value,
                preferredModelId = if (effectiveMode == AiOperatingMode.LOCAL_ONLY) {
                    if (!providerIntegrationManager.activeGgufModelId.value.isNullOrBlank()) {
                        ProviderIntegrationManager.GGUF_MODEL_DESCRIPTOR_ID
                    } else {
                        ModelHealthQuotaManager.LOCAL_CORE_MODEL_ID
                    }
                } else _preferredModelId.value,
                previousModelId = currentPreviousModel,
                networkAvailable = effectiveNetworkAvailable,
                excludeModelIds = triedIds,
                operatingMode = effectiveMode,
                manualModelLock = _manualModelLock.value,
                localFallbackEnabled = _localFallbackEnabled.value,
                disabledProviders = providerIntegrationManager.disabledProviders.value
            )

            // If strict operatingMode (e.g. ONLINE_ONLY) or disabled localFallback yielded no eligible model:
            if (decision.candidateOrder.isEmpty()) {
                val reason = decision.noFreeCloudReason
                    ?: lastFailure
                    ?: "No AI provider matches your active Operating Mode (${effectiveMode.title})."
                _lastRoutingDecision.value = decision
                _lastError.value = reason
                emit(
                    BrainGenerationEvent(
                        activeModelId = decision.selectedModel.id,
                        activeModelName = decision.selectedModel.displayName,
                        routingDecision = decision,
                        errorMessage = reason
                    )
                )
                return@flow
            }

            val chosen = decision.selectedModel
            triedIds.add(chosen.id)
            _lastRoutingDecision.value = decision
            updateStateDisplay(chosen.id, chosen.displayName)
            syncProviderEnum(chosen.provider)

            emit(
                BrainGenerationEvent(
                    activeModelId = chosen.id,
                    activeModelName = chosen.displayName,
                    routingDecision = decision
                )
            )

            if (effectiveMode == AiOperatingMode.HYBRID &&
                attempt > 0 &&
                chosen.provider == AiProviderType.ON_DEVICE_CORE &&
                !lastFailure.isNullOrBlank()
            ) {
                val fallbackNotice = "[Notice: Cloud/remote free models unavailable — $lastFailure. Switched cleanly to AERIS On-Device Core.]\n\n"
                emit(
                    BrainGenerationEvent(
                        textChunk = fallbackNotice,
                        activeModelId = chosen.id,
                        activeModelName = chosen.displayName,
                        routingDecision = decision
                    )
                )
            }

            var streamedAnyToken = false
            val attemptStartMs = System.currentTimeMillis()
            var firstTokenMs: Long? = null
            var chunkCount = 0
            var totalChars = 0

            try {
                providerIntegrationManager.streamWithDescriptor(
                    descriptor = chosen,
                    builtPrompt = builtPrompt,
                    rawPrompt = prompt,
                    history = history,
                    memories = relevantMemories,
                    continuity = continuity
                ).collect { chunk ->
                    if (!isCancelled && chunk.isNotEmpty()) {
                        if (firstTokenMs == null) {
                            firstTokenMs = (System.currentTimeMillis() - attemptStartMs).coerceAtLeast(1L)
                        }
                        chunkCount++
                        totalChars += chunk.length
                        streamedAnyToken = true
                        emit(
                            BrainGenerationEvent(
                                textChunk = chunk,
                                activeModelId = chosen.id,
                                activeModelName = chosen.displayName,
                                routingDecision = decision
                            )
                        )
                    }
                }
                val totalGenMs = (System.currentTimeMillis() - attemptStartMs).coerceAtLeast(1L)
                val ttft = firstTokenMs ?: totalGenMs
                val switchLatency = if (firstAttemptFailTimeMs != null) {
                    (attemptStartMs - firstAttemptFailTimeMs).coerceAtLeast(0L)
                } else 0L
                val estWords = (totalChars / 4.5f).coerceAtLeast(1f)
                val tokensPerSec = if (totalGenMs > 0) (estWords * 1000f) / totalGenMs else 0f
                val sample = RequestLatencySample(
                    requestId = reqId,
                    modelId = chosen.id,
                    modelDisplayName = chosen.displayName,
                    provider = chosen.provider,
                    androidPreProcessMs = androidPreProcessMs,
                    networkDelayMs = ttft,
                    ttftMs = ttft,
                    totalGenerationMs = totalGenMs,
                    switchLatencyMs = switchLatency,
                    switchedFromModelId = decision.switchedFromModelId,
                    tokensPerSecEstimate = tokensPerSec,
                    chunkCount = chunkCount,
                    succeeded = true
                )
                telemetryTracker.recordSample(sample)
                emit(
                    BrainGenerationEvent(
                        activeModelId = chosen.id,
                        activeModelName = chosen.displayName,
                        routingDecision = decision,
                        telemetrySample = sample
                    )
                )
                completed = true
                break
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val failNow = System.currentTimeMillis()
                if (firstAttemptFailTimeMs == null) {
                    firstAttemptFailTimeMs = failNow
                }
                lastFailure = e.localizedMessage ?: "Model ${chosen.displayName} failed."
                _lastError.value = lastFailure
                currentPreviousModel = chosen.id

                val errSample = RequestLatencySample(
                    requestId = reqId,
                    modelId = chosen.id,
                    modelDisplayName = chosen.displayName,
                    provider = chosen.provider,
                    androidPreProcessMs = androidPreProcessMs,
                    networkDelayMs = (failNow - attemptStartMs).coerceAtLeast(1L),
                    ttftMs = (failNow - attemptStartMs).coerceAtLeast(1L),
                    totalGenerationMs = (failNow - attemptStartMs).coerceAtLeast(1L),
                    succeeded = false,
                    errorMessage = lastFailure
                )
                telemetryTracker.recordSample(errSample)

                if (streamedAnyToken || !_autoSwitchEnabled.value || _manualModelLock.value || chosen.provider == AiProviderType.ON_DEVICE_CORE) {
                    emit(
                        BrainGenerationEvent(
                            activeModelId = chosen.id,
                            activeModelName = chosen.displayName,
                            routingDecision = decision,
                            telemetrySample = errSample,
                            errorMessage = lastFailure
                        )
                    )
                    completed = streamedAnyToken
                    break
                }
            }
        }

        if (!completed && !isCancelled && lastFailure != null) {
            emit(
                BrainGenerationEvent(
                    activeModelId = _state.value.modelId.orEmpty(),
                    activeModelName = _state.value.modelName.orEmpty(),
                    errorMessage = lastFailure
                )
            )
        }
    }.flowOn(Dispatchers.Default)

    private fun updateStateDisplay(modelId: String, modelName: String) {
        _state.value = _state.value.copy(
            isLoaded = true,
            modelId = modelId,
            modelName = modelName,
            engineType = "AERIS 11-Engine Brain"
        )
    }

    private fun syncProviderEnum(provider: AiProviderType) {
        _activeProvider.value = when (provider) {
            AiProviderType.GEMINI_FIREBASE -> ActiveAiProvider.GEMINI_FIREBASE
            AiProviderType.OPENAI_COMPATIBLE -> ActiveAiProvider.OPENAI_COMPATIBLE
            AiProviderType.OPENROUTER -> ActiveAiProvider.OPENROUTER
            AiProviderType.GROQ -> ActiveAiProvider.GROQ
            AiProviderType.HUGGING_FACE -> ActiveAiProvider.HUGGING_FACE
            AiProviderType.CLOUDFLARE_WORKERS_AI -> ActiveAiProvider.CLOUDFLARE_WORKERS_AI
            AiProviderType.GPT_OSS_HOSTED -> ActiveAiProvider.GPT_OSS_HOSTED
            AiProviderType.QWEN_HOSTED -> ActiveAiProvider.QWEN_HOSTED
            AiProviderType.LOCAL_GGUF -> ActiveAiProvider.LOCAL_GGUF_MODEL
            AiProviderType.ON_DEVICE_CORE -> ActiveAiProvider.ON_DEVICE_OFFLINE
        }
    }

    override suspend fun cancelGeneration() {
        isCancelled = true
        localEngine.cancelGeneration()
    }
}

typealias HairaAiEngine = AerisAiEngine
