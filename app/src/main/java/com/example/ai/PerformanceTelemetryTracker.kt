package com.example.ai

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class RequestLatencySample(
    val requestId: Long,
    val modelId: String,
    val modelDisplayName: String,
    val provider: AiProviderType,
    val androidPreProcessMs: Long,
    val networkDelayMs: Long,
    val ttftMs: Long,
    val totalGenerationMs: Long,
    val switchLatencyMs: Long = 0L,
    val switchedFromModelId: String? = null,
    val tokensPerSecEstimate: Float = 0f,
    val chunkCount: Int = 0,
    val succeeded: Boolean = true,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class TelemetrySummary(
    val totalRequests: Int = 0,
    val successfulRequests: Int = 0,
    val failoverCount: Int = 0,
    val averageAndroidPrepMs: Long? = null,
    val averageNetworkDelayMs: Long? = null,
    val averageTtftMs: Long? = null,
    val averageTotalGenMs: Long? = null,
    val averageSwitchLatencyMs: Long? = null,
    val averageTokensPerSec: Float? = null,
    val lastSample: RequestLatencySample? = null
)

/**
 * Tracks real per-request timings (Android pre-processing, TTFT, network delay,
 * total generation time, model switching overhead, and streaming throughput).
 * Never synthesizes or hardcodes metrics; averages remain null until real requests execute.
 */
class PerformanceTelemetryTracker {

    private val _samples = MutableStateFlow<List<RequestLatencySample>>(emptyList())
    val samples: StateFlow<List<RequestLatencySample>> = _samples.asStateFlow()

    private val _summary = MutableStateFlow(TelemetrySummary())
    val summary: StateFlow<TelemetrySummary> = _summary.asStateFlow()

    fun recordSample(sample: RequestLatencySample) {
        _samples.update { current ->
            val updated = (listOf(sample) + current).take(50)
            recomputeSummary(updated)
            updated
        }
    }

    fun clear() {
        _samples.value = emptyList()
        _summary.value = TelemetrySummary()
    }

    private fun recomputeSummary(list: List<RequestLatencySample>) {
        if (list.isEmpty()) {
            _summary.value = TelemetrySummary()
            return
        }
        val ok = list.filter { it.succeeded }
        val switches = list.filter { it.switchLatencyMs > 0L || it.switchedFromModelId != null }
        _summary.value = TelemetrySummary(
            totalRequests = list.size,
            successfulRequests = ok.size,
            failoverCount = switches.size,
            averageAndroidPrepMs = list.map { it.androidPreProcessMs }.average().toLong(),
            averageNetworkDelayMs = if (ok.isNotEmpty()) ok.map { it.networkDelayMs }.average().toLong() else null,
            averageTtftMs = if (ok.isNotEmpty()) ok.map { it.ttftMs }.average().toLong() else null,
            averageTotalGenMs = if (ok.isNotEmpty()) ok.map { it.totalGenerationMs }.average().toLong() else null,
            averageSwitchLatencyMs = if (switches.isNotEmpty()) switches.map { it.switchLatencyMs }.average().toLong() else null,
            averageTokensPerSec = if (ok.isNotEmpty()) ok.map { it.tokensPerSecEstimate }.average().toFloat() else null,
            lastSample = list.first()
        )
    }
}
