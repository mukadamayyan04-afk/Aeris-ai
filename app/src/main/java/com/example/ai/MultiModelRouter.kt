package com.example.ai

/**
 * Engine 3: Multi-Model Router
 * Selects and safely routes among all eligible models using:
 * - Free-Only Mode ON by default (never silently switches to paid/billing models)
 * - Task complexity & capability fit (lightweight fast models for simple chat; capable models for coding/STEM)
 * - Real measured TTFT / latency & provider health (Available / Connected / Needs Auth / Rate Limited / Unavailable)
 * - Network availability & endpoint configuration completeness
 * - Preferred-model lock when auto-switch is OFF, or seamless continuity failover when ON
 */
class MultiModelRouter(
    private val providerManager: ProviderIntegrationManager
) {
    fun selectRoute(
        plan: ReasoningPlan,
        freeOnlyMode: Boolean,
        autoSwitchEnabled: Boolean,
        preferredModelId: String?,
        previousModelId: String? = null,
        networkAvailable: Boolean = providerManager.isNetworkAvailable(),
        excludeModelIds: Set<String> = emptySet(),
        nowMillis: Long = System.currentTimeMillis(),
        operatingMode: AiOperatingMode = AiOperatingMode.HYBRID,
        manualModelLock: Boolean = false,
        localFallbackEnabled: Boolean = true,
        disabledProviders: Set<AiProviderType> = providerManager.disabledProviders.value
    ): RoutingDecision {
        val catalog = providerManager.catalogDescriptors()
        val blocked = mutableListOf<String>()
        val effectiveAutoSwitch = autoSwitchEnabled && !manualModelLock

        val eligible = catalog.filter { model ->
            if (model.id in excludeModelIds) {
                return@filter false
            }
            val isLocalModel = model.provider == AiProviderType.ON_DEVICE_CORE ||
                model.provider == AiProviderType.LOCAL_GGUF
            val isRemote = !isLocalModel

            if (model.provider in disabledProviders || !providerManager.isProviderEnabled(model.provider)) {
                blocked.add("${model.id} (Disabled)")
                return@filter false
            }
            if (operatingMode == AiOperatingMode.LOCAL_ONLY && isRemote) {
                blocked.add("${model.id} (Blocked by Local-Only Mode)")
                return@filter false
            }
            if (operatingMode == AiOperatingMode.ONLINE_ONLY && isLocalModel) {
                blocked.add("${model.id} (Blocked by Online-Only Mode)")
                return@filter false
            }
            if (freeOnlyMode && (!model.isFreeTierEligible || model.requiresBilling)) {
                blocked.add("${model.id} (Blocked by Free-Only Mode)")
                return@filter false
            }
            if (isRemote && !networkAvailable) {
                blocked.add("${model.id} (Offline / Unavailable)")
                return@filter false
            }
            if (model.provider in setOf(
                    AiProviderType.OPENAI_COMPATIBLE,
                    AiProviderType.OPENROUTER,
                    AiProviderType.GROQ,
                    AiProviderType.HUGGING_FACE,
                    AiProviderType.CLOUDFLARE_WORKERS_AI,
                    AiProviderType.GPT_OSS_HOSTED,
                    AiProviderType.QWEN_HOSTED
                )
            ) {
                val cfg = providerManager.endpointConfigs.value[model.provider]
                if (cfg == null || !cfg.hasBaseAndModelOnly) {
                    blocked.add("${model.id} (Not Configured)")
                    return@filter false
                }
                if (model.provider.requiresApiKey && cfg.bearerToken.isBlank()) {
                    blocked.add("${model.id} (Needs Authentication)")
                    return@filter false
                }
            }
            if (model.provider == AiProviderType.LOCAL_GGUF &&
                providerManager.activeGgufModelId.value.isNullOrBlank()
            ) {
                blocked.add("${model.id} (No GGUF loaded)")
                return@filter false
            }
            val health = providerManager.healthManager.getHealth(model.id)
            if (!providerManager.healthManager.isCallable(model.id, nowMillis)) {
                val reason = when (health.status) {
                    ModelHealthStatus.NEEDS_AUTH -> "Needs Authentication"
                    ModelHealthStatus.RATE_LIMITED, ModelHealthStatus.QUOTA_EXCEEDED -> "Quota/Rate-Limit Cooldown"
                    else -> "Cooldown"
                }
                blocked.add("${model.id} ($reason)")
                return@filter false
            }
            true
        }

        val ordered = eligible.sortedByDescending { model ->
            scoreModel(model, plan, preferredModelId, effectiveAutoSwitch)
        }

        val localCoreFallback = catalog.first { it.id == ModelHealthQuotaManager.LOCAL_CORE_MODEL_ID }
        val localGgufCandidate = ordered.firstOrNull { it.provider == AiProviderType.LOCAL_GGUF }

        var noRouteError: String? = null
        val selected = if (!effectiveAutoSwitch && !preferredModelId.isNullOrBlank()) {
            val lockedMatch = ordered.firstOrNull { it.id == preferredModelId }
            when {
                lockedMatch != null -> lockedMatch
                operatingMode == AiOperatingMode.HYBRID && localFallbackEnabled -> {
                    localGgufCandidate ?: ordered.firstOrNull { it.id == ModelHealthQuotaManager.LOCAL_CORE_MODEL_ID }
                        ?: localCoreFallback
                }
                else -> {
                    noRouteError = when (operatingMode) {
                        AiOperatingMode.ONLINE_ONLY ->
                            "Preferred model '$preferredModelId' is unavailable and Online-Only Mode forbids local fallback."
                        AiOperatingMode.LOCAL_ONLY ->
                            "Locked local model '$preferredModelId' is not ready in Local-Only Mode."
                        AiOperatingMode.HYBRID ->
                            "Locked model '$preferredModelId' is unavailable and Local Fallback is disabled."
                    }
                    localCoreFallback
                }
            }
        } else {
            val remoteOrAny = if (operatingMode == AiOperatingMode.ONLINE_ONLY) {
                ordered.firstOrNull()
            } else if (operatingMode == AiOperatingMode.HYBRID && !localFallbackEnabled) {
                ordered.firstOrNull {
                    it.provider != AiProviderType.ON_DEVICE_CORE
                }
            } else {
                ordered.firstOrNull()
            }
            if (remoteOrAny != null) {
                remoteOrAny
            } else {
                noRouteError = when (operatingMode) {
                    AiOperatingMode.ONLINE_ONLY ->
                        if (!networkAvailable) {
                            "Device is offline and Online-Only Mode prohibits local fallback."
                        } else {
                            "No online AI model is currently eligible (${blocked.take(3).joinToString("; ")}). Local fallback is blocked by Online-Only Mode."
                        }
                    AiOperatingMode.LOCAL_ONLY ->
                        "No local model available (${blocked.take(2).joinToString("; ")})."
                    AiOperatingMode.HYBRID ->
                        "No cloud model available and Local Fallback is disabled (${blocked.take(3).joinToString("; ")})."
                }
                localCoreFallback
            }
        }

        val noFreeCloudReason = noRouteError ?: if (
            selected.provider == AiProviderType.ON_DEVICE_CORE &&
            operatingMode != AiOperatingMode.LOCAL_ONLY &&
            freeOnlyMode &&
            ordered.none {
                it.provider != AiProviderType.ON_DEVICE_CORE && it.provider != AiProviderType.LOCAL_GGUF
            }
        ) {
            if (!networkAvailable) {
                "Device is offline. Using AERIS On-Device Core."
            } else {
                "No eligible free-tier cloud model is currently available (${blocked.take(3).joinToString("; ")}). Free-Only Mode prevented paid model fallback."
            }
        } else null

        val switchReason = when {
            !previousModelId.isNullOrBlank() && previousModelId != selected.id -> {
                val prevHealth = providerManager.healthManager.getHealth(previousModelId)
                when {
                    prevHealth.status == ModelHealthStatus.QUOTA_EXCEEDED ||
                        prevHealth.status == ModelHealthStatus.RATE_LIMITED ->
                        "Switched from $previousModelId due to quota/rate limit"
                    prevHealth.status == ModelHealthStatus.NEEDS_AUTH ->
                        "Switched from $previousModelId (authentication required)"
                    prevHealth.status == ModelHealthStatus.UNAVAILABLE ->
                        "Switched from $previousModelId (provider unavailable / timed out)"
                    prevHealth.status == ModelHealthStatus.ERROR ->
                        "Switched from $previousModelId after connection error"
                    plan.category in selected.strengths &&
                        selected.id != preferredModelId &&
                        !preferredModelId.isNullOrBlank() ->
                        "Auto-selected ${selected.displayName} for ${plan.category.label}"
                    else -> "Continuity transition from $previousModelId to ${selected.displayName}"
                }
            }
            !preferredModelId.isNullOrBlank() && preferredModelId != selected.id &&
                blocked.any { it.startsWith(preferredModelId) } ->
                "Preferred model $preferredModelId unavailable; using ${selected.displayName}"
            else -> null
        }

        return RoutingDecision(
            selectedModel = selected,
            candidateOrder = ordered.map { it.id },
            switchedFromModelId = if (switchReason != null) previousModelId else null,
            switchReason = switchReason,
            blockedModels = blocked,
            noFreeCloudReason = noFreeCloudReason
        ).let { decision ->
            if (noRouteError != null) {
                decision.copy(candidateOrder = emptyList())
            } else decision
        }
    }

    private fun scoreModel(
        model: AiModelDescriptor,
        plan: ReasoningPlan,
        preferredModelId: String?,
        autoSwitchEnabled: Boolean
    ): Int {
        var score = model.priorityScore
        val health = providerManager.healthManager.getHealth(model.id)
        when (health.status) {
            ModelHealthStatus.CONNECTED -> score += 40
            ModelHealthStatus.AVAILABLE -> score += 20
            ModelHealthStatus.NOT_TESTED, ModelHealthStatus.UNCONNECTED -> {
                if (model.provider == AiProviderType.GEMINI_FIREBASE) score += 15 else score -= 5
            }
            ModelHealthStatus.ERROR, ModelHealthStatus.UNAVAILABLE -> score -= 45
            ModelHealthStatus.NEEDS_AUTH -> score -= 70
            ModelHealthStatus.RATE_LIMITED -> score -= 65
            ModelHealthStatus.QUOTA_EXCEEDED -> score -= 80
            ModelHealthStatus.TESTING -> Unit
        }

        val measuredLatency = health.lastTtftMs ?: health.lastLatencyMs
        if (measuredLatency != null && model.provider != AiProviderType.ON_DEVICE_CORE) {
            when {
                measuredLatency <= 1000L -> score += 24
                measuredLatency <= 2200L -> score += 12
                measuredLatency > 4500L -> score -= 18
            }
        }

        if (plan.complexityScore <= 2 && model.isLightweightFast && model.provider != AiProviderType.ON_DEVICE_CORE) {
            score += 18
        } else if (plan.complexityScore >= 4 && !model.isLightweightFast &&
            model.provider != AiProviderType.ON_DEVICE_CORE
        ) {
            score += 14
        }

        if (plan.category in model.strengths) score += 20
        if (plan.requiresVision && model.supportsVision) score += 30
        if (!preferredModelId.isNullOrBlank() && model.id == preferredModelId) {
            score += if (autoSwitchEnabled) 32 else 220
        }
        return score
    }
}
