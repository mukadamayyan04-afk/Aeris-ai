package com.example.ai

import com.example.domain.model.MessageSender
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.ConnectionPool
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Shared OkHttp SSE & Model Discovery adapter for all OpenAI API-compatible providers:
 * - OpenAI API-compatible endpoints
 * - OpenRouter (https://openrouter.ai/api/v1)
 * - Groq (https://api.groq.com/openai/v1)
 * - Hugging Face Inference Router (https://router.huggingface.co/v1)
 * - GPT-OSS hosted endpoints
 * - Qwen hosted endpoints
 *
 * Reuses HTTP/2 & keep-alive connections cleanly and parses real rate-limit headers.
 * Zero hardcoded API keys; all credentials come privately from user DataStore / CredentialVault.
 */
class OpenAiCompatibleProviderAdapter(
    private val healthManager: ModelHealthQuotaManager,
    private val httpClient: OkHttpClient = defaultPooledClient()
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    data class DiscoveryResult(
        val modelIds: List<String>,
        val freeModelIds: List<String>,
        val latencyMs: Long,
        val quotaNote: String? = null
    )

    suspend fun discoverModels(
        descriptorId: String,
        config: ProviderEndpointConfig
    ): Result<DiscoveryResult> = withContext(Dispatchers.IO) {
        if (config.baseUrl.isBlank() || config.baseUrl.contains("ACCOUNT_ID", ignoreCase = true)) {
            val msg = if (config.providerType == AiProviderType.CLOUDFLARE_WORKERS_AI) {
                "Enter your Cloudflare Account ID to complete the Workers AI URL."
            } else {
                "${config.providerType.displayName} has no Base URL configured."
            }
            healthManager.markUnavailable(descriptorId, msg)
            return@withContext Result.failure(IllegalStateException(msg))
        }
        if (config.providerType.requiresApiKey && config.bearerToken.isBlank()) {
            val msg = "${config.providerType.displayName} requires an API key/token."
            healthManager.markNeedsAuth(descriptorId, msg)
            return@withContext Result.failure(IllegalStateException(msg))
        }

        healthManager.markTesting(descriptorId)
        val startedAt = System.currentTimeMillis()
        try {
            val modelsUrl = normalizeBaseUrl(config.baseUrl) + "/models"
            val req = buildAuthorizedRequest(modelsUrl, config).get().build()
            httpClient.newCall(req).execute().use { resp ->
                val latency = (System.currentTimeMillis() - startedAt).coerceAtLeast(1L)
                val headersInfo = extractRateLimitHeaders(resp.headers)
                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string().orEmpty()
                    val parsed = parseModelsPayload(config.providerType, bodyStr)
                    val quotaDesc = headersInfo.quotaSummary ?: if (parsed.second.isNotEmpty()) {
                        "${parsed.second.size} free-eligible models found"
                    } else null
                    healthManager.markConnected(
                        modelId = descriptorId,
                        latencyMs = latency,
                        hasAuthentication = !config.providerType.requiresApiKey || config.bearerToken.isNotBlank(),
                        remainingRequests = headersInfo.remainingRequests,
                        remainingTokens = headersInfo.remainingTokens,
                        quotaStatus = quotaDesc,
                        discoveredModels = parsed.first
                    )
                    Result.success(
                        DiscoveryResult(
                            modelIds = parsed.first,
                            freeModelIds = parsed.second,
                            latencyMs = latency,
                            quotaNote = quotaDesc
                        )
                    )
                } else if (resp.code == 404 && config.providerType == AiProviderType.CLOUDFLARE_WORKERS_AI && config.modelId.isNotBlank()) {
                    // Some Cloudflare Workers AI gateways omit /ai/v1/models; verify directly via minimal /chat/completions ping
                    return@withContext testCompletionPing(descriptorId, config, startedAt)
                } else {
                    val bodySnippet = resp.body?.string()?.take(220).orEmpty()
                    val (status, msg) = mapHttpError(resp.code, bodySnippet)
                    healthManager.markFailure(
                        modelId = descriptorId,
                        status = status,
                        errorMessage = msg,
                        retryAfterSec = headersInfo.retryAfterSec,
                        remainingRequests = headersInfo.remainingRequests
                    )
                    Result.failure(IllegalStateException(msg))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val (status, msg) = mapNetworkException(config.baseUrl, e)
            healthManager.markFailure(descriptorId, status, msg)
            Result.failure(IllegalStateException(msg))
        }
    }

    private fun testCompletionPing(
        descriptorId: String,
        config: ProviderEndpointConfig,
        startedAt: Long
    ): Result<DiscoveryResult> {
        val chatUrl = normalizeBaseUrl(config.baseUrl) + "/chat/completions"
        val pingPayload = JSONObject()
            .put("model", config.modelId)
            .put("max_tokens", 1)
            .put("stream", false)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", "ping")))
            .toString()
        val pingReq = buildAuthorizedRequest(chatUrl, config)
            .post(pingPayload.toRequestBody(jsonMediaType))
            .build()
        return httpClient.newCall(pingReq).execute().use { pingResp ->
            val latency = (System.currentTimeMillis() - startedAt).coerceAtLeast(1L)
            val headersInfo = extractRateLimitHeaders(pingResp.headers)
            if (pingResp.isSuccessful) {
                healthManager.markConnected(
                    modelId = descriptorId,
                    latencyMs = latency,
                    hasAuthentication = !config.providerType.requiresApiKey || config.bearerToken.isNotBlank(),
                    remainingRequests = headersInfo.remainingRequests,
                    remainingTokens = headersInfo.remainingTokens,
                    quotaStatus = headersInfo.quotaSummary,
                    discoveredModels = listOf(config.modelId)
                )
                Result.success(
                    DiscoveryResult(
                        modelIds = listOf(config.modelId),
                        freeModelIds = listOf(config.modelId),
                        latencyMs = latency,
                        quotaNote = headersInfo.quotaSummary
                    )
                )
            } else {
                val snippet = pingResp.body?.string()?.take(220).orEmpty()
                val (status, msg) = mapHttpError(pingResp.code, snippet)
                healthManager.markFailure(
                    modelId = descriptorId,
                    status = status,
                    errorMessage = msg,
                    retryAfterSec = headersInfo.retryAfterSec,
                    remainingRequests = headersInfo.remainingRequests
                )
                Result.failure(IllegalStateException(msg))
            }
        }
    }

    suspend fun testConnection(
        descriptorId: String,
        config: ProviderEndpointConfig
    ): Result<Long> {
        return discoverModels(descriptorId, config).map { it.latencyMs }
    }

    fun streamChat(
        descriptorId: String,
        config: ProviderEndpointConfig,
        builtPrompt: PromptCreationEngine.BuiltPrompt
    ): Flow<String> = flow {
        if (config.baseUrl.isBlank() || config.modelId.isBlank() ||
            config.baseUrl.contains("ACCOUNT_ID", ignoreCase = true)
        ) {
            val msg = if (config.baseUrl.contains("ACCOUNT_ID", ignoreCase = true)) {
                "Cloudflare Account ID placeholder not replaced in Base URL."
            } else {
                "${config.providerType.displayName} is not configured."
            }
            healthManager.markUnavailable(descriptorId, msg)
            throw IllegalStateException(msg)
        }
        if (config.providerType.requiresApiKey && config.bearerToken.isBlank()) {
            val msg = "${config.providerType.displayName} requires authentication token."
            healthManager.markNeedsAuth(descriptorId, msg)
            throw IllegalStateException(msg)
        }

        val startedAt = System.currentTimeMillis()
        var firstTokenLatencyMs: Long? = null
        val url = normalizeBaseUrl(config.baseUrl) + "/chat/completions"

        val messagesJson = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", builtPrompt.systemInstruction))
            builtPrompt.turns.forEach { msg ->
                val role = if (msg.sender == MessageSender.AERIS) "assistant" else "user"
                put(JSONObject().put("role", role).put("content", msg.text))
            }
            put(JSONObject().put("role", "user").put("content", builtPrompt.userMessageText))
        }

        val payload = JSONObject()
            .put("model", config.modelId)
            .put("stream", true)
            .put("messages", messagesJson)
            .toString()

        val req = buildAuthorizedRequest(url, config)
            .post(payload.toRequestBody(jsonMediaType))
            .addHeader("Accept", "text/event-stream")
            .addHeader("Cache-Control", "no-cache")
            .build()

        var headersInfo = HeaderQuotaInfo()
        try {
            var receivedContent = false
            withTimeout(STREAM_TOTAL_TIMEOUT_MS) {
                httpClient.newCall(req).execute().use { response ->
                    headersInfo = extractRateLimitHeaders(response.headers)
                    if (!response.isSuccessful) {
                        val bodySnippet = response.body?.string()?.take(240).orEmpty()
                        val (status, err) = mapHttpError(response.code, bodySnippet)
                        healthManager.markFailure(
                            modelId = descriptorId,
                            status = status,
                            errorMessage = err,
                            retryAfterSec = headersInfo.retryAfterSec,
                            remainingRequests = headersInfo.remainingRequests
                        )
                        throw IllegalStateException(err)
                    }

                    val body = response.body ?: throw IllegalStateException("Empty response body from $url")
                    val reader = BufferedReader(InputStreamReader(body.byteStream(), Charsets.UTF_8), 2048)
                    reader.use { br ->
                        while (true) {
                            val line = br.readLine() ?: break
                            val trimmed = line.trim()
                            if (trimmed.isEmpty() || trimmed.startsWith(":")) continue
                            if (!trimmed.startsWith("data:")) {
                                if (trimmed.startsWith("{")) {
                                    val fallbackText = extractDeltaContent(trimmed)
                                    if (!fallbackText.isNullOrEmpty()) {
                                        if (firstTokenLatencyMs == null) {
                                            firstTokenLatencyMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(1L)
                                        }
                                        receivedContent = true
                                        emit(fallbackText)
                                    }
                                }
                                continue
                            }
                            val data = trimmed.removePrefix("data:").trim()
                            if (data == "[DONE]") break
                            if (data.isEmpty()) continue

                            val deltaText = extractDeltaContent(data)
                            if (!deltaText.isNullOrEmpty()) {
                                if (firstTokenLatencyMs == null) {
                                    firstTokenLatencyMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(1L)
                                }
                                receivedContent = true
                                emit(deltaText)
                            }
                        }
                    }
                }
            }

            if (receivedContent) {
                val totalMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(1L)
                val ttft = firstTokenLatencyMs ?: totalMs
                healthManager.markConnected(
                    modelId = descriptorId,
                    latencyMs = ttft,
                    ttftMs = ttft,
                    totalGenMs = totalMs,
                    hasAuthentication = true,
                    remainingRequests = headersInfo.remainingRequests,
                    remainingTokens = headersInfo.remainingTokens,
                    quotaStatus = headersInfo.quotaSummary
                )
            } else {
                val msg = "Hosted endpoint ${config.modelId} completed stream with zero tokens."
                healthManager.markFailure(descriptorId, ModelHealthStatus.ERROR, msg)
                throw IllegalStateException(msg)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: TimeoutCancellationException) {
            val msg = "${config.providerType.displayName} timed out waiting for response."
            healthManager.markFailure(descriptorId, ModelHealthStatus.UNAVAILABLE, msg)
            throw IllegalStateException(msg, e)
        } catch (e: Exception) {
            val existing = healthManager.getHealth(descriptorId).status
            if (existing != ModelHealthStatus.QUOTA_EXCEEDED &&
                existing != ModelHealthStatus.RATE_LIMITED &&
                existing != ModelHealthStatus.NEEDS_AUTH
            ) {
                val (status, msg) = mapNetworkException(config.baseUrl, e)
                healthManager.markFailure(descriptorId, status, msg)
            }
            throw e
        }
    }.flowOn(Dispatchers.IO)

    private fun buildAuthorizedRequest(url: String, config: ProviderEndpointConfig): Request.Builder {
        val builder = Request.Builder().url(url)
        if (config.bearerToken.isNotBlank()) {
            builder.addHeader("Authorization", "Bearer ${config.bearerToken}")
        }
        if (config.providerType == AiProviderType.OPENROUTER) {
            builder.addHeader("HTTP-Referer", "https://aeris.ai")
            builder.addHeader("X-Title", "AERIS AI")
        }
        return builder
    }

    internal fun parseModelsPayload(
        providerType: AiProviderType,
        jsonText: String
    ): Pair<List<String>, List<String>> {
        if (jsonText.isBlank()) return emptyList<String>() to emptyList()
        return try {
            val all = mutableListOf<String>()
            val free = mutableListOf<String>()
            val arr: JSONArray? = if (jsonText.trimStart().startsWith("[")) {
                JSONArray(jsonText)
            } else {
                val root = JSONObject(jsonText)
                root.optJSONArray("data") ?: root.optJSONArray("result")
            }
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id").trim().ifBlank { obj.optString("name").trim() }
                    if (id.isNotEmpty()) {
                        all.add(id)
                        val isFreeSuffix = id.endsWith(":free", ignoreCase = true)
                        val pricingObj = obj.optJSONObject("pricing")
                        val promptPrice = pricingObj?.optString("prompt")?.toDoubleOrNull()
                        if (isFreeSuffix ||
                            (providerType == AiProviderType.OPENROUTER && promptPrice != null && promptPrice == 0.0) ||
                            providerType == AiProviderType.CLOUDFLARE_WORKERS_AI
                        ) {
                            free.add(id)
                        }
                    }
                }
            }
            all.take(60) to free.take(40)
        } catch (_: Exception) {
            emptyList<String>() to emptyList()
        }
    }

    internal fun extractDeltaContent(jsonLine: String): String? {
        return try {
            val root = JSONObject(jsonLine)
            val choices = root.optJSONArray("choices") ?: return null
            if (choices.length() == 0) return null
            val first = choices.getJSONObject(0)
            val delta = first.optJSONObject("delta")
            if (delta != null && delta.has("content") && !delta.isNull("content")) {
                return delta.optString("content", "")
            }
            val message = first.optJSONObject("message")
            if (message != null && message.has("content") && !message.isNull("content")) {
                return message.optString("content", "")
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    internal data class HeaderQuotaInfo(
        val remainingRequests: String? = null,
        val remainingTokens: String? = null,
        val retryAfterSec: Long? = null,
        val quotaSummary: String? = null
    )

    internal fun extractRateLimitHeaders(headers: Headers): HeaderQuotaInfo {
        val remReq = headers["x-ratelimit-remaining-requests"] ?: headers["x-ratelimit-remaining"]
        val remTok = headers["x-ratelimit-remaining-tokens"]
        val retryAfter = headers["retry-after"]?.toLongOrNull()
        val parts = mutableListOf<String>()
        if (!remReq.isNullOrBlank()) parts.add("Req rem: $remReq")
        if (!remTok.isNullOrBlank()) parts.add("Tok rem: $remTok")
        return HeaderQuotaInfo(
            remainingRequests = remReq,
            remainingTokens = remTok,
            retryAfterSec = retryAfter,
            quotaSummary = if (parts.isNotEmpty()) parts.joinToString(" • ") else null
        )
    }

    internal fun mapHttpError(code: Int, bodySnippet: String): Pair<ModelHealthStatus, String> {
        return when (code) {
            401, 403 -> ModelHealthStatus.NEEDS_AUTH to "Authentication failed (HTTP $code). Verify API token."
            429 -> ModelHealthStatus.RATE_LIMITED to "Rate limit or free quota exceeded (HTTP 429). $bodySnippet".trim()
            402 -> ModelHealthStatus.QUOTA_EXCEEDED to "Billing or credit limit reached (HTTP 402)."
            503 -> ModelHealthStatus.UNAVAILABLE to "Provider/model currently loading or unavailable (HTTP 503). $bodySnippet".trim()
            in 500..599 -> ModelHealthStatus.ERROR to "Endpoint server error (HTTP $code). $bodySnippet".trim()
            else -> ModelHealthStatus.ERROR to "HTTP $code error: $bodySnippet".trim()
        }
    }

    private fun mapNetworkException(baseUrl: String, e: Exception): Pair<ModelHealthStatus, String> {
        return when (e) {
            is SocketTimeoutException, is ConnectException, is UnknownHostException ->
                ModelHealthStatus.UNAVAILABLE to "Network/host unreachable ($baseUrl): ${e.localizedMessage ?: e.javaClass.simpleName}"
            else -> ModelHealthStatus.ERROR to (e.localizedMessage ?: "Endpoint error")
        }
    }

    internal fun normalizeBaseUrl(raw: String): String {
        val cleaned = raw.trim().trimEnd('/')
        return if (cleaned.endsWith("/v1") || cleaned.contains("/v1/")) cleaned else "$cleaned/v1"
    }

    companion object {
        private const val STREAM_TOTAL_TIMEOUT_MS = 55_000L

        fun defaultPooledClient(): OkHttpClient = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
