package com.example.ai

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.TimeUnit

/**
 * Engine 5: Model Health and Quota Manager
 * Tracks honest live connectivity, TTFT, total latency, quota/rate-limit headers,
 * auth status, discovered models, and cooldown backoffs.
 * External/cloud providers NEVER start marked as Connected until verified by a real check or call.
 */
class ModelHealthQuotaManager {

    private val _healthStates = MutableStateFlow<Map<String, ModelHealthState>>(emptyMap())
    val healthStates: StateFlow<Map<String, ModelHealthState>> = _healthStates.asStateFlow()

    fun getHealth(modelId: String): ModelHealthState {
        return _healthStates.value[modelId] ?: ModelHealthState(
            modelId = modelId,
            status = if (modelId == LOCAL_CORE_MODEL_ID) ModelHealthStatus.CONNECTED else ModelHealthStatus.NOT_TESTED,
            hasAuthentication = modelId == LOCAL_CORE_MODEL_ID
        )
    }

    fun isCallable(modelId: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val state = getHealth(modelId)
        if ((state.status == ModelHealthStatus.QUOTA_EXCEEDED ||
                state.status == ModelHealthStatus.RATE_LIMITED) &&
            state.isInCooldown(nowMillis)
        ) {
            return false
        }
        if (state.status == ModelHealthStatus.NEEDS_AUTH && state.lastCheckedAtMillis > 0L) {
            // Do not keep spamming a provider that rejected auth until user updates credentials or re-tests
            return false
        }
        return true
    }

    fun markTesting(modelId: String) {
        _healthStates.update { current ->
            val prev = current[modelId] ?: ModelHealthState(modelId)
            current + (modelId to prev.copy(status = ModelHealthStatus.TESTING, lastError = null))
        }
    }

    fun markAvailable(
        modelId: String,
        hasAuth: Boolean,
        quotaNote: String? = null
    ) {
        _healthStates.update { current ->
            val prev = current[modelId] ?: ModelHealthState(modelId)
            val preservedConnected = prev.status == ModelHealthStatus.CONNECTED && prev.lastSuccessfulAtMillis > 0L
            current + (modelId to prev.copy(
                status = if (preservedConnected) ModelHealthStatus.CONNECTED else ModelHealthStatus.AVAILABLE,
                hasAuthentication = hasAuth,
                quotaStatus = quotaNote ?: prev.quotaStatus,
                lastError = null
            ))
        }
    }

    fun markNeedsAuth(modelId: String, reason: String) {
        val now = System.currentTimeMillis()
        _healthStates.update { current ->
            val prev = current[modelId] ?: ModelHealthState(modelId)
            current + (modelId to prev.copy(
                status = ModelHealthStatus.NEEDS_AUTH,
                hasAuthentication = false,
                lastError = reason,
                lastCheckedAtMillis = now
            ))
        }
    }

    fun markUnavailable(modelId: String, reason: String) {
        val now = System.currentTimeMillis()
        _healthStates.update { current ->
            val prev = current[modelId] ?: ModelHealthState(modelId)
            current + (modelId to prev.copy(
                status = ModelHealthStatus.UNAVAILABLE,
                lastError = reason,
                lastCheckedAtMillis = now
            ))
        }
    }

    fun markConnected(
        modelId: String,
        latencyMs: Long,
        ttftMs: Long? = null,
        totalGenMs: Long? = null,
        hasAuthentication: Boolean = true,
        remainingRequests: String? = null,
        remainingTokens: String? = null,
        quotaStatus: String? = null,
        discoveredModels: List<String>? = null
    ) {
        val now = System.currentTimeMillis()
        _healthStates.update { current ->
            val prev = current[modelId]
            val smoothed = if (prev?.lastLatencyMs != null && latencyMs > 0) {
                (prev.lastLatencyMs + latencyMs) / 2
            } else {
                latencyMs
            }
            val smoothedTtft = when {
                ttftMs == null -> prev?.lastTtftMs
                prev?.lastTtftMs != null && ttftMs > 0 -> (prev.lastTtftMs + ttftMs) / 2
                else -> ttftMs
            }
            current + (modelId to ModelHealthState(
                modelId = modelId,
                status = ModelHealthStatus.CONNECTED,
                lastLatencyMs = smoothed,
                lastTtftMs = smoothedTtft,
                lastTotalGenMs = totalGenMs ?: prev?.lastTotalGenMs,
                lastError = null,
                quotaStatus = quotaStatus ?: prev?.quotaStatus,
                remainingRequests = remainingRequests ?: prev?.remainingRequests,
                remainingTokens = remainingTokens ?: prev?.remainingTokens,
                hasAuthentication = hasAuthentication,
                discoveredModels = discoveredModels ?: prev?.discoveredModels.orEmpty(),
                consecutiveFailures = 0,
                cooldownUntilMillis = 0L,
                lastCheckedAtMillis = now,
                lastSuccessfulAtMillis = now
            ))
        }
    }

    fun recordDiscoveredModels(
        modelId: String,
        models: List<String>,
        remainingRequests: String? = null,
        remainingTokens: String? = null,
        quotaStatus: String? = null
    ) {
        _healthStates.update { current ->
            val prev = current[modelId] ?: ModelHealthState(modelId)
            current + (modelId to prev.copy(
                discoveredModels = models,
                remainingRequests = remainingRequests ?: prev.remainingRequests,
                remainingTokens = remainingTokens ?: prev.remainingTokens,
                quotaStatus = quotaStatus ?: prev.quotaStatus
            ))
        }
    }

    fun markFailure(
        modelId: String,
        status: ModelHealthStatus,
        errorMessage: String,
        nowMillis: Long = System.currentTimeMillis(),
        retryAfterSec: Long? = null,
        remainingRequests: String? = null
    ) {
        _healthStates.update { current ->
            val prev = current[modelId] ?: ModelHealthState(modelId)
            val failures = prev.consecutiveFailures + 1
            val cooldownMs = when {
                retryAfterSec != null && retryAfterSec > 0 -> TimeUnit.SECONDS.toMillis(retryAfterSec.coerceAtMost(300L))
                status == ModelHealthStatus.QUOTA_EXCEEDED -> TimeUnit.MINUTES.toMillis(5)
                status == ModelHealthStatus.RATE_LIMITED -> TimeUnit.SECONDS.toMillis((15L * failures).coerceAtMost(120L))
                status == ModelHealthStatus.ERROR || status == ModelHealthStatus.UNAVAILABLE ->
                    TimeUnit.SECONDS.toMillis((8L * failures).coerceAtMost(45L))
                else -> 0L
            }
            current + (modelId to prev.copy(
                status = status,
                lastError = errorMessage,
                hasAuthentication = if (status == ModelHealthStatus.NEEDS_AUTH) false else prev.hasAuthentication,
                remainingRequests = remainingRequests ?: prev.remainingRequests,
                quotaStatus = if (status == ModelHealthStatus.RATE_LIMITED || status == ModelHealthStatus.QUOTA_EXCEEDED) {
                    "Quota / Rate Limit active"
                } else prev.quotaStatus,
                consecutiveFailures = failures,
                cooldownUntilMillis = if (cooldownMs > 0) nowMillis + cooldownMs else 0L,
                lastCheckedAtMillis = nowMillis
            ))
        }
    }

    fun markUnconnected(modelId: String, reason: String? = null) {
        _healthStates.update { current ->
            val prev = current[modelId]
            current + (modelId to ModelHealthState(
                modelId = modelId,
                status = ModelHealthStatus.NOT_TESTED,
                lastError = reason,
                discoveredModels = prev?.discoveredModels.orEmpty(),
                lastCheckedAtMillis = 0L
            ))
        }
    }

    companion object {
        const val LOCAL_CORE_MODEL_ID = "aeris-local-core"
    }
}
