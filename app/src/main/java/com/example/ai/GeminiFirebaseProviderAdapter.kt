package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import com.example.domain.model.MessageSender
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.FirebaseAIException
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "GeminiFirebaseAdapter"

/**
 * Real Google Gemini inference adapter backed by Firebase AI Logic (`Firebase.ai`) and App Check.
 * Caches GenerativeModel instances lazily to avoid repeated SDK initialization overhead,
 * tracks real TTFT and total generation latency, and never uses hardcoded API keys.
 */
class GeminiFirebaseProviderAdapter(
    private val healthManager: ModelHealthQuotaManager
) {
    private val pingModelCache = ConcurrentHashMap<String, GenerativeModel>()
    private val chatModelCache = object : LruCache<String, GenerativeModel>(8) {}

    private fun getOrCreatePingModel(modelId: String): GenerativeModel {
        return pingModelCache.getOrPut(modelId) {
            Firebase.ai.generativeModel(modelName = modelId)
        }
    }

    private fun getOrCreateChatModel(modelId: String, systemInstruction: String): GenerativeModel {
        val key = "$modelId:${systemInstruction.hashCode()}"
        synchronized(chatModelCache) {
            val existing = chatModelCache.get(key)
            if (existing != null) return existing
            val created = Firebase.ai.generativeModel(
                modelName = modelId,
                systemInstruction = content { text(systemInstruction) }
            )
            chatModelCache.put(key, created)
            return created
        }
    }

    fun testConnection(modelId: String): Flow<Result<Long>> = flow {
        healthManager.markTesting(modelId)
        val startedAt = System.currentTimeMillis()
        try {
            val responseText = withTimeout(25_000L) {
                getOrCreatePingModel(modelId).generateContent("Respond strictly with OK.").text.orEmpty()
            }
            val latency = (System.currentTimeMillis() - startedAt).coerceAtLeast(1L)
            if (responseText.isNotBlank()) {
                healthManager.markConnected(
                    modelId = modelId,
                    latencyMs = latency,
                    ttftMs = latency,
                    hasAuthentication = true,
                    quotaStatus = if (modelId == ARGON_GEMINI_MODEL) {
                        "Firebase Blaze plan required"
                    } else {
                        "Firebase Spark free-tier active (subject to Firebase quotas)"
                    },
                    discoveredModels = KNOWN_GEMINI_MODELS
                )
                emit(Result.success(latency))
            } else {
                val msg = "Model $modelId returned an empty ping response."
                healthManager.markFailure(modelId, ModelHealthStatus.ERROR, msg)
                emit(Result.failure(IllegalStateException(msg)))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: TimeoutCancellationException) {
            val msg = "Timed out checking Gemini ($modelId) after 25s."
            healthManager.markFailure(modelId, ModelHealthStatus.UNAVAILABLE, msg)
            emit(Result.failure(IllegalStateException(msg)))
        } catch (e: QuotaExceededException) {
            val msg = e.localizedMessage ?: "Gemini quota exceeded for $modelId."
            healthManager.markFailure(modelId, ModelHealthStatus.RATE_LIMITED, msg)
            emit(Result.failure(IllegalStateException(msg)))
        } catch (e: FirebaseAIException) {
            val (status, msg) = classifyFirebaseError(e)
            healthManager.markFailure(modelId, status, msg)
            emit(Result.failure(IllegalStateException(msg)))
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Failed to connect to Gemini ($modelId)."
            Log.e(TAG, msg, e)
            healthManager.markFailure(modelId, ModelHealthStatus.ERROR, msg)
            emit(Result.failure(IllegalStateException(msg)))
        }
    }.flowOn(Dispatchers.IO)

    fun streamText(
        modelId: String,
        builtPrompt: PromptCreationEngine.BuiltPrompt
    ): Flow<String> = flow {
        val startedAt = System.currentTimeMillis()
        var firstTokenMs: Long? = null
        try {
            val model = getOrCreateChatModel(modelId, builtPrompt.systemInstruction)
            val historyContents: List<Content> = builtPrompt.turns.map { turn ->
                val role = if (turn.sender == MessageSender.AERIS) "model" else "user"
                content(role) { text(turn.text) }
            }
            val chat = model.startChat(history = historyContents)
            var emittedAny = false
            withTimeout(55_000L) {
                chat.sendMessageStream(builtPrompt.userMessageText).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        if (firstTokenMs == null) {
                            firstTokenMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(1L)
                        }
                        emittedAny = true
                        emit(piece)
                    }
                }
            }
            val totalMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(1L)
            val ttft = firstTokenMs ?: totalMs
            if (emittedAny) {
                healthManager.markConnected(
                    modelId = modelId,
                    latencyMs = ttft,
                    ttftMs = ttft,
                    totalGenMs = totalMs,
                    hasAuthentication = true,
                    quotaStatus = if (modelId == ARGON_GEMINI_MODEL) {
                        "Firebase Blaze plan"
                    } else {
                        "Firebase Spark free-tier active"
                    }
                )
            } else {
                throw IllegalStateException("Gemini ($modelId) returned an empty stream.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: TimeoutCancellationException) {
            val msg = "Gemini ($modelId) timed out after 55s."
            healthManager.markFailure(modelId, ModelHealthStatus.UNAVAILABLE, msg)
            throw IllegalStateException(msg, e)
        } catch (e: QuotaExceededException) {
            val msg = e.localizedMessage ?: "Gemini free-tier quota exceeded for $modelId."
            healthManager.markFailure(modelId, ModelHealthStatus.RATE_LIMITED, msg)
            throw IllegalStateException(msg, e)
        } catch (e: FirebaseAIException) {
            val (status, msg) = classifyFirebaseError(e)
            healthManager.markFailure(modelId, status, msg)
            throw IllegalStateException(msg, e)
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Gemini ($modelId) inference failed."
            Log.e(TAG, msg, e)
            healthManager.markFailure(modelId, ModelHealthStatus.ERROR, msg)
            throw IllegalStateException(msg, e)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun analyzeGameplayBitmap(
        bitmap: Bitmap,
        gameName: String,
        modelId: String = DEFAULT_GEMINI_MODEL
    ): Result<String> = withContext(Dispatchers.IO) {
        val startedAt = System.currentTimeMillis()
        try {
            val prompt = """
                You are AERIS AI Game Coach observing a live screenshot from the user's Android game ($gameName).
                Address the user respectfully as "Sir ji" in concise Roman Hinglish / English (no Devanagari).
                Give 1–2 short, fair-play tactical callouts based ONLY on what is clearly visible on screen (minimap, positioning, health/ammo/resources, cooldowns, danger angles).
                Never suggest cheats, macros, exploits, or automated taps.
            """.trimIndent()
            val response = withTimeout(30_000L) {
                getOrCreatePingModel(modelId).generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )
            }
            val text = response.text?.trim().orEmpty()
            if (text.isNotEmpty()) {
                val ms = (System.currentTimeMillis() - startedAt).coerceAtLeast(1L)
                healthManager.markConnected(modelId, latencyMs = ms, ttftMs = ms)
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Gemini Vision returned empty coaching feedback."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: TimeoutCancellationException) {
            val msg = "Vision analysis timed out after 30s."
            healthManager.markFailure(modelId, ModelHealthStatus.UNAVAILABLE, msg)
            Result.failure(IllegalStateException(msg))
        } catch (e: QuotaExceededException) {
            val msg = e.localizedMessage ?: "Gemini quota exceeded during Game Coach vision."
            healthManager.markFailure(modelId, ModelHealthStatus.RATE_LIMITED, msg)
            Result.failure(IllegalStateException(msg))
        } catch (e: FirebaseAIException) {
            val (status, msg) = classifyFirebaseError(e)
            healthManager.markFailure(modelId, status, msg)
            Result.failure(IllegalStateException(msg))
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Vision analysis failed."
            Log.e(TAG, msg, e)
            healthManager.markFailure(modelId, ModelHealthStatus.ERROR, msg)
            Result.failure(IllegalStateException(msg))
        }
    }

    suspend fun enhanceImagePrompt(rawPrompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = withTimeout(25_000L) {
                getOrCreatePingModel(LITE_GEMINI_MODEL).generateContent(
                    "Rewrite this image prompt for high visual clarity, lighting, and composition in 1-2 concise English sentences without commentary: $rawPrompt"
                )
            }
            val enhanced = response.text?.trim().orEmpty()
            if (enhanced.isNotBlank()) Result.success(enhanced) else Result.failure(IllegalStateException("Empty enhanced prompt"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun classifyFirebaseError(e: FirebaseAIException): Pair<ModelHealthStatus, String> {
        val raw = e.localizedMessage ?: e.message ?: "Firebase AI error"
        return when {
            raw.contains("403") || raw.contains("AppCheck", ignoreCase = true) ->
                ModelHealthStatus.NEEDS_AUTH to
                    "App Check attestation failed (HTTP 403): Ensure Firebase App Check debug token or release Play/reCAPTCHA is active. ($raw)"
            raw.contains("429") || raw.contains("quota", ignoreCase = true) ->
                ModelHealthStatus.RATE_LIMITED to "Gemini quota or rate limit reached: $raw"
            raw.contains("billing", ignoreCase = true) ->
                ModelHealthStatus.QUOTA_EXCEEDED to "This Gemini model requires Firebase Blaze billing: $raw"
            else -> ModelHealthStatus.ERROR to raw
        }
    }

    companion object {
        const val DEFAULT_GEMINI_MODEL = "gemini-3.8-flash"
        const val LITE_GEMINI_MODEL = "gemini-3.5-flash-lite"
        const val ARGON_GEMINI_MODEL = "gemini-4-argon"
        val KNOWN_GEMINI_MODELS = listOf(
            DEFAULT_GEMINI_MODEL,
            LITE_GEMINI_MODEL,
            ARGON_GEMINI_MODEL,
            "gemini-nano-banana-2.1"
        )
    }
}
