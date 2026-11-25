package com.example.ai

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.core.content.FileProvider
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.FirebaseAIException
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

private const val TAG = "ImageGenerationManager"

enum class ImageAspectOption(val label: String) {
    SQUARE("1:1 Square"),
    WIDESCREEN("16:9 Wide")
}

/**
 * Engine 11: Image Generation Manager
 * Separated cleanly from text-only chat providers.
 * Uses Firebase AI Logic ("gemini-nano-banana-2.1") by default or user-configured OpenAI-compatible
 * /images/generations endpoints.
 * Strictly prohibits procedural Canvas fallbacks or fake image generation on failure.
 */
class ImageGenerationManager(
    private val context: Context,
    private val healthManager: ModelHealthQuotaManager,
    private val geminiAdapter: GeminiFirebaseProviderAdapter
) {
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    suspend fun enhancePrompt(rawPrompt: String): Result<String> {
        if (rawPrompt.isBlank()) return Result.failure(IllegalArgumentException("Enter a prompt first, Sir ji."))
        return geminiAdapter.enhanceImagePrompt(rawPrompt)
    }

    suspend fun testImageModelConnection(): Result<String> = withContext(Dispatchers.IO) {
        healthManager.markTesting(DEFAULT_IMAGE_MODEL)
        val start = System.currentTimeMillis()
        try {
            val config = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
            val model = Firebase.ai.generativeModel(
                modelName = DEFAULT_IMAGE_MODEL,
                generationConfig = config
            )
            // Verify model endpoint reachability with lightweight image synthesis
            val response = withTimeout(60_000L) {
                model.generateContent("Minimal cyan glowing orb on pure black background")
            }
            val hasBitmap = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()?.firstOrNull()?.image != null
            val latency = (System.currentTimeMillis() - start).coerceAtLeast(1L)
            if (hasBitmap) {
                healthManager.markConnected(DEFAULT_IMAGE_MODEL, latency)
                Result.success("Gemini Nano Banana 2.1 connected (${latency}ms)")
            } else {
                val msg = "Model responded without an image payload."
                healthManager.markFailure(DEFAULT_IMAGE_MODEL, ModelHealthStatus.ERROR, msg)
                Result.failure(IllegalStateException(msg))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: TimeoutCancellationException) {
            val msg = "Image model test timed out after 60s."
            healthManager.markFailure(DEFAULT_IMAGE_MODEL, ModelHealthStatus.ERROR, msg)
            Result.failure(IllegalStateException(msg))
        } catch (e: QuotaExceededException) {
            val msg = "Image quota exceeded / Firebase Blaze billing required: ${e.localizedMessage}"
            healthManager.markFailure(DEFAULT_IMAGE_MODEL, ModelHealthStatus.QUOTA_EXCEEDED, msg)
            Result.failure(IllegalStateException(msg))
        } catch (e: FirebaseAIException) {
            val msg = formatImageError(e.localizedMessage ?: e.message ?: "Firebase AI error")
            healthManager.markFailure(DEFAULT_IMAGE_MODEL, ModelHealthStatus.ERROR, msg)
            Result.failure(IllegalStateException(msg))
        } catch (e: Exception) {
            val msg = formatImageError(e.localizedMessage ?: "Image connection check failed.")
            healthManager.markFailure(DEFAULT_IMAGE_MODEL, ModelHealthStatus.ERROR, msg)
            Result.failure(IllegalStateException(msg))
        }
    }

    suspend fun generateImage(
        prompt: String,
        aspectOption: ImageAspectOption = ImageAspectOption.SQUARE,
        allowPaidOrBlazeModels: Boolean = false,
        customEndpoint: ProviderEndpointConfig? = null
    ): ImageGenResult = withContext(Dispatchers.IO) {
        if (prompt.isBlank()) {
            return@withContext ImageGenResult.Error("Please enter an image prompt, Sir ji.")
        }

        // If user explicitly configured a custom OpenAI-compatible image endpoint and enabled it
        if (customEndpoint != null && customEndpoint.isConfigured && !allowPaidOrBlazeModels) {
            return@withContext generateViaCustomEndpoint(prompt, customEndpoint)
        }

        if (!allowPaidOrBlazeModels) {
            return@withContext ImageGenResult.Error(
                "Gemini Nano Banana 2.1 requires a Firebase Blaze billing plan. " +
                    "Free-Only Mode is currently ON. Either disable Free-Only Mode in Settings/Models " +
                    "after enabling Blaze on your Firebase project, or configure a custom /images/generations endpoint."
            )
        }

        val startedAt = System.currentTimeMillis()
        try {
            val config = if (aspectOption == ImageAspectOption.WIDESCREEN) {
                generationConfig {
                    responseModalities = listOf(ResponseModality.IMAGE)
                    imageConfig = ImageConfig.builder()
                        .setAspectRatio(AspectRatio.LANDSCAPE_16x9)
                        .setImageSize(ImageSize.SIZE_1K)
                        .build()
                }
            } else {
                generationConfig {
                    responseModalities = listOf(ResponseModality.IMAGE)
                }
            }

            val model = Firebase.ai.generativeModel(
                modelName = DEFAULT_IMAGE_MODEL,
                generationConfig = config
            )

            val bitmap = withTimeout(60_000L) {
                val response = model.generateContent(prompt)
                response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                    ?.image
            }

            if (bitmap != null) {
                healthManager.markConnected(DEFAULT_IMAGE_MODEL, (System.currentTimeMillis() - startedAt).coerceAtLeast(1L))
                ImageGenResult.Success(bitmap = bitmap, modelUsed = DEFAULT_IMAGE_MODEL)
            } else {
                val msg = "No image was returned by $DEFAULT_IMAGE_MODEL."
                healthManager.markFailure(DEFAULT_IMAGE_MODEL, ModelHealthStatus.ERROR, msg)
                ImageGenResult.Error(msg)
            }
        } catch (e: TimeoutCancellationException) {
            val msg = "Image generation timed out after 60 seconds."
            healthManager.markFailure(DEFAULT_IMAGE_MODEL, ModelHealthStatus.ERROR, msg)
            ImageGenResult.Error(msg)
        } catch (e: QuotaExceededException) {
            val msg = "Rate limit or Blaze quota exceeded: ${e.localizedMessage ?: "Try again later."}"
            healthManager.markFailure(DEFAULT_IMAGE_MODEL, ModelHealthStatus.QUOTA_EXCEEDED, msg)
            ImageGenResult.Error(msg)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            val msg = formatImageError(e.localizedMessage ?: "Image generation failed.")
            healthManager.markFailure(DEFAULT_IMAGE_MODEL, ModelHealthStatus.ERROR, msg)
            ImageGenResult.Error(msg)
        }
    }

    private fun generateViaCustomEndpoint(
        prompt: String,
        config: ProviderEndpointConfig
    ): ImageGenResult {
        return try {
            val base = config.baseUrl.trim().trimEnd('/')
            val url = if (base.endsWith("/v1")) "$base/images/generations" else "$base/v1/images/generations"
            val payload = JSONObject()
                .put("model", config.modelId.ifBlank { "gpt-image-1" })
                .put("prompt", prompt)
                .put("n", 1)
                .put("response_format", "b64_json")
                .toString()
            val reqBuilder = Request.Builder()
                .url(url)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            if (config.bearerToken.isNotBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer ${config.bearerToken}")
            }
            httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return ImageGenResult.Error("Custom image endpoint HTTP ${resp.code}: ${raw.take(200)}")
                }
                val json = JSONObject(raw)
                val first = json.optJSONArray("data")?.optJSONObject(0)
                val b64 = first?.optString("b64_json").orEmpty()
                if (b64.isNotBlank()) {
                    val bytes = Base64.decode(b64, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        ImageGenResult.Success(bitmap = bmp, modelUsed = config.modelId)
                    } else {
                        ImageGenResult.Error("Endpoint returned invalid image bytes.")
                    }
                } else {
                    ImageGenResult.Error("Endpoint did not return base64 image data.")
                }
            }
        } catch (e: Exception) {
            ImageGenResult.Error(e.localizedMessage ?: "Custom image generation failed.")
        }
    }

    suspend fun saveBitmapToGallery(bitmap: Bitmap): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val filename = "AERIS_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AERIS")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext Result.failure(IllegalStateException("MediaStore rejected insert"))

            resolver.openOutputStream(uri)?.use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    throw IllegalStateException("PNG compression failed")
                }
            } ?: throw IllegalStateException("Failed to open output stream")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createShareIntent(bitmap: Bitmap): Result<Intent> = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.cacheDir, "shared_images").apply { mkdirs() }
            val file = File(dir, "aeris_studio_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Result.success(Intent.createChooser(intent, "Share AERIS Image"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatImageError(raw: String): String {
        return when {
            raw.contains("403") || raw.contains("AppCheck", ignoreCase = true) ->
                "App Check attestation failed (HTTP 403): $raw"
            raw.contains("billing", ignoreCase = true) || raw.contains("Blaze", ignoreCase = true) ->
                "Firebase Blaze billing plan required for Gemini image models: $raw"
            else -> raw
        }
    }

    companion object {
        const val DEFAULT_IMAGE_MODEL = "gemini-nano-banana-2.1"
    }
}
