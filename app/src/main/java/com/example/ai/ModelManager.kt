package com.example.ai

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.StatFs
import com.example.domain.model.ModelMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class ModelManager(private val context: Context) {

    private val modelsDir = File(context.filesDir, "models").apply { mkdirs() }

    private val _availableModels = MutableStateFlow<List<ModelMetadata>>(emptyList())
    val availableModels: Flow<List<ModelMetadata>> = _availableModels.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: Flow<Map<String, Float>> = _downloadProgress.asStateFlow()

    private val _downloadStatus = MutableStateFlow<Map<String, String>>(emptyMap())
    val downloadStatus: Flow<Map<String, String>> = _downloadStatus.asStateFlow()

    private val _lifecycleStates = MutableStateFlow<Map<String, LocalModelLifecycleState>>(emptyMap())
    val lifecycleStates: StateFlow<Map<String, LocalModelLifecycleState>> = _lifecycleStates.asStateFlow()

    private val _sha256Digests = MutableStateFlow<Map<String, String>>(emptyMap())
    val sha256Digests: StateFlow<Map<String, String>> = _sha256Digests.asStateFlow()

    private val activeDownloadCalls = mutableMapOf<String, okhttp3.Call>()
    private val pausedByUser = mutableSetOf<String>()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    init {
        refreshModelCatalog()
    }

    fun getFreeStorageBytes(): Long {
        return try {
            val stat = StatFs(context.filesDir.absolutePath)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (_: Exception) {
            1024L * 1024L * 1024L * 8L
        }
    }

    fun getTotalStorageBytes(): Long {
        return try {
            val stat = StatFs(context.filesDir.absolutePath)
            stat.blockCountLong * stat.blockSizeLong
        } catch (_: Exception) {
            1024L * 1024L * 1024L * 128L
        }
    }

    fun getAvailableRamMb(): Long {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            memInfo.availMem / (1024L * 1024L)
        } catch (_: Exception) {
            4096L
        }
    }

    fun isWifiConnected(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } catch (_: Exception) {
            false
        }
    }

    fun isDefaultModelInstalled(): Boolean {
        return _availableModels.value.any { it.id == DEFAULT_RECOMMENDED_MODEL_ID && it.isInstalled }
    }

    fun canOfferFirstTimeSetup(alreadyPrompted: Boolean): Boolean {
        return !alreadyPrompted && !isDefaultModelInstalled()
    }

    fun checkRamCompatibility(requiredRamMb: Int): Boolean {
        return getAvailableRamMb() >= requiredRamMb
    }

    fun checkStorageCompatibility(requiredBytes: Long): Boolean {
        return getFreeStorageBytes() >= (requiredBytes + 200L * 1024L * 1024L)
    }

    fun setModelLifecycleState(modelId: String, state: LocalModelLifecycleState) {
        _lifecycleStates.value = _lifecycleStates.value + (modelId to state)
    }

    fun getLifecycleState(modelId: String): LocalModelLifecycleState {
        return _lifecycleStates.value[modelId] ?: LocalModelLifecycleState.NOT_DOWNLOADED
    }

    fun refreshModelCatalog() {
        val installedFiles = modelsDir.listFiles()?.associateBy { it.name } ?: emptyMap()

        val catalog = listOf(
            ModelMetadata(
                id = DEFAULT_RECOMMENDED_MODEL_ID,
                name = "Gemma 3 1B Instruct (4-bit Q4)",
                parameterSize = "1.0 Billion",
                quantization = "Q4_K_M (GGUF)",
                fileSizeBytes = 806L * 1024L * 1024L,
                requiredRamMb = 1500,
                isInstalled = installedFiles.containsKey("$DEFAULT_RECOMMENDED_MODEL_ID.gguf"),
                localFilePath = installedFiles["$DEFAULT_RECOMMENDED_MODEL_ID.gguf"]?.absolutePath,
                downloadUrl = "https://huggingface.co/unsloth/gemma-3-1b-it-GGUF/resolve/main/gemma-3-1b-it-Q4_K_M.gguf",
                license = "Gemma Terms of Use (Free download, open weights)",
                isRecommendedForDevice = true,
                isDefaultRecommended = true
            ),
            ModelMetadata(
                id = "smollm2-135m-q4",
                name = "SmolLM2 135M Instruct",
                parameterSize = "135 Million",
                quantization = "Q4_K_M (GGUF)",
                fileSizeBytes = 98L * 1024L * 1024L,
                requiredRamMb = 512,
                isInstalled = installedFiles.containsKey("smollm2-135m-q4.gguf"),
                localFilePath = installedFiles["smollm2-135m-q4.gguf"]?.absolutePath,
                downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-135M-Instruct-GGUF/resolve/main/smollm2-135m-instruct-q4_k_m.gguf",
                license = "Apache 2.0 (Permissive, Ungated)",
                isRecommendedForDevice = true
            ),
            ModelMetadata(
                id = "smollm2-360m-q4",
                name = "SmolLM2 360M Instruct",
                parameterSize = "360 Million",
                quantization = "Q4_K_M (GGUF)",
                fileSizeBytes = 229L * 1024L * 1024L,
                requiredRamMb = 750,
                isInstalled = installedFiles.containsKey("smollm2-360m-q4.gguf"),
                localFilePath = installedFiles["smollm2-360m-q4.gguf"]?.absolutePath,
                downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q4_k_m.gguf",
                license = "Apache 2.0 (Permissive, Ungated)",
                isRecommendedForDevice = true
            ),
            ModelMetadata(
                id = "qwen2.5-0.5b-q4",
                name = "Qwen 2.5 0.5B Instruct",
                parameterSize = "500 Million",
                quantization = "Q4_K_M (GGUF)",
                fileSizeBytes = 397L * 1024L * 1024L,
                requiredRamMb = 850,
                isInstalled = installedFiles.containsKey("qwen2.5-0.5b-q4.gguf"),
                localFilePath = installedFiles["qwen2.5-0.5b-q4.gguf"]?.absolutePath,
                downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
                license = "Apache 2.0 (Open Source, Ungated)",
                isRecommendedForDevice = true
            ),
            ModelMetadata(
                id = "tinyllama-1.1b-q4",
                name = "TinyLlama 1.1B Chat",
                parameterSize = "1.1 Billion",
                quantization = "Q4_K_M (GGUF)",
                fileSizeBytes = 669L * 1024L * 1024L,
                requiredRamMb = 1200,
                isInstalled = installedFiles.containsKey("tinyllama-1.1b-q4.gguf"),
                localFilePath = installedFiles["tinyllama-1.1b-q4.gguf"]?.absolutePath,
                downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
                license = "Apache 2.0 (Open Source)",
                isRecommendedForDevice = true
            ),
            ModelMetadata(
                id = "llama-3.2-1b-q4",
                name = "Llama 3.2 1B Instruct",
                parameterSize = "1.23 Billion",
                quantization = "Q4_K_M (GGUF)",
                fileSizeBytes = 804L * 1024L * 1024L,
                requiredRamMb = 1450,
                isInstalled = installedFiles.containsKey("llama-3.2-1b-q4.gguf"),
                localFilePath = installedFiles["llama-3.2-1b-q4.gguf"]?.absolutePath,
                downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
                license = "Llama 3.2 Community License",
                isRecommendedForDevice = true
            )
        )
        _availableModels.value = catalog

        val currentStates = _lifecycleStates.value.toMutableMap()
        catalog.forEach { model ->
            val partFile = File(modelsDir, "${model.id}.gguf.part")
            val activeState = currentStates[model.id]
            when {
                model.isInstalled -> {
                    if (activeState != LocalModelLifecycleState.LOADING &&
                        activeState != LocalModelLifecycleState.RUNNING
                    ) {
                        currentStates[model.id] = LocalModelLifecycleState.READY
                    }
                }
                activeState == LocalModelLifecycleState.DOWNLOADING -> Unit
                partFile.exists() && partFile.length() > 0L -> {
                    currentStates[model.id] = LocalModelLifecycleState.PAUSED
                }
                activeState != LocalModelLifecycleState.ERROR -> {
                    currentStates[model.id] = LocalModelLifecycleState.NOT_DOWNLOADED
                }
            }
        }
        _lifecycleStates.value = currentStates
    }

    suspend fun startDownload(
        modelId: String,
        onProgressUpdate: ((Float) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        val model = _availableModels.value.find { it.id == modelId }
            ?: return@withContext Result.failure(IllegalArgumentException("Model not found in catalog"))

        if (!checkStorageCompatibility(model.fileSizeBytes)) {
            val msg = "Insufficient device storage. Need at least ${model.fileSizeBytes / (1024 * 1024)} MB free."
            _downloadStatus.value = _downloadStatus.value + (modelId to msg)
            _lifecycleStates.value = _lifecycleStates.value + (modelId to LocalModelLifecycleState.ERROR)
            return@withContext Result.failure(IllegalStateException(msg))
        }

        pausedByUser.remove(modelId)
        val targetFile = File(modelsDir, "${model.id}.gguf")
        val partFile = File(modelsDir, "${model.id}.gguf.part")

        val existingBytes = if (partFile.exists()) partFile.length() else 0L

        val requestBuilder = Request.Builder().url(model.downloadUrl)
        if (existingBytes > 0) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }

        val request = requestBuilder.build()
        val call = httpClient.newCall(request)
        activeDownloadCalls[modelId] = call

        _downloadStatus.value = _downloadStatus.value + (modelId to "Downloading")
        _lifecycleStates.value = _lifecycleStates.value + (modelId to LocalModelLifecycleState.DOWNLOADING)

        try {
            val response = call.execute()
            if (!response.isSuccessful && response.code != 206) {
                val err = "HTTP ${response.code}: Download server error"
                _downloadStatus.value = _downloadStatus.value + (modelId to err)
                _lifecycleStates.value = _lifecycleStates.value + (modelId to LocalModelLifecycleState.ERROR)
                return@withContext Result.failure(IllegalStateException(err))
            }

            val body = response.body ?: run {
                val err = "Empty response body"
                _downloadStatus.value = _downloadStatus.value + (modelId to err)
                _lifecycleStates.value = _lifecycleStates.value + (modelId to LocalModelLifecycleState.ERROR)
                return@withContext Result.failure(IllegalStateException(err))
            }
            val totalBytes = if (response.code == 206) {
                existingBytes + body.contentLength()
            } else {
                body.contentLength()
            }

            body.byteStream().use { inputStream ->
                FileOutputStream(partFile, response.code == 206).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var downloadedBytes = existingBytes

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        if (totalBytes > 0) {
                            val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            _downloadProgress.value = _downloadProgress.value + (modelId to progress)
                            onProgressUpdate?.invoke(progress)
                        }
                    }
                    outputStream.flush()
                }
            }

            if (partFile.renameTo(targetFile)) {
                _downloadStatus.value = _downloadStatus.value + (modelId to "Ready")
                _downloadProgress.value = _downloadProgress.value + (modelId to 1.0f)
                _lifecycleStates.value = _lifecycleStates.value + (modelId to LocalModelLifecycleState.READY)
                refreshModelCatalog()
                Result.success(targetFile)
            } else {
                _lifecycleStates.value = _lifecycleStates.value + (modelId to LocalModelLifecycleState.ERROR)
                Result.failure(IllegalStateException("Failed to finalize downloaded model file"))
            }
        } catch (e: Exception) {
            if (pausedByUser.contains(modelId) || (partFile.exists() && partFile.length() > 0L && e is java.io.IOException)) {
                _downloadStatus.value = _downloadStatus.value + (modelId to "Paused")
                _lifecycleStates.value = _lifecycleStates.value + (modelId to LocalModelLifecycleState.PAUSED)
            } else {
                _downloadStatus.value = _downloadStatus.value + (modelId to "Error: ${e.localizedMessage ?: "Download failed"}")
                _lifecycleStates.value = _lifecycleStates.value + (modelId to LocalModelLifecycleState.ERROR)
            }
            Result.failure(e)
        } finally {
            activeDownloadCalls.remove(modelId)
        }
    }

    fun pauseDownload(modelId: String) {
        pausedByUser.add(modelId)
        activeDownloadCalls[modelId]?.cancel()
        activeDownloadCalls.remove(modelId)
        _downloadStatus.value = _downloadStatus.value + (modelId to "Paused")
        _lifecycleStates.value = _lifecycleStates.value + (modelId to LocalModelLifecycleState.PAUSED)
    }

    fun cancelDownload(modelId: String) {
        pausedByUser.remove(modelId)
        activeDownloadCalls[modelId]?.cancel()
        activeDownloadCalls.remove(modelId)
        val partFile = File(modelsDir, "$modelId.gguf.part")
        if (partFile.exists()) partFile.delete()
        _downloadProgress.value = _downloadProgress.value - modelId
        _downloadStatus.value = _downloadStatus.value + (modelId to "Cancelled")
        _lifecycleStates.value = _lifecycleStates.value + (modelId to LocalModelLifecycleState.NOT_DOWNLOADED)
    }

    fun computeSha256(file: File): String? {
        if (!file.exists()) return null
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            null
        }
    }

    fun verifySha256(file: File, expectedHash: String): Boolean {
        val computed = computeSha256(file) ?: return false
        return computed.equals(expectedHash.trim(), ignoreCase = true)
    }

    suspend fun verifyLocalFileSha256(modelId: String, expectedHash: String = ""): Result<String> = withContext(Dispatchers.IO) {
        val model = _availableModels.value.find { it.id == modelId }
            ?: return@withContext Result.failure(IllegalArgumentException("Model not found: $modelId"))
        val path = model.localFilePath
            ?: return@withContext Result.failure(IllegalStateException("Model file not downloaded"))
        val file = File(path)
        val computed = computeSha256(file)
            ?: return@withContext Result.failure(IllegalStateException("Failed to read file for SHA-256 check"))

        _sha256Digests.value = _sha256Digests.value + (modelId to computed)
        val effectiveExpected = expectedHash.trim().ifBlank { model.expectedSha256.trim() }
        if (effectiveExpected.isNotBlank()) {
            val matched = computed.equals(effectiveExpected, ignoreCase = true)
            _availableModels.value = _availableModels.value.map {
                if (it.id == modelId) it.copy(sha256Verified = matched) else it
            }
            if (!matched) {
                _lifecycleStates.value = _lifecycleStates.value + (modelId to LocalModelLifecycleState.ERROR)
                return@withContext Result.failure(IllegalStateException("SHA-256 mismatch! Got $computed"))
            }
        } else {
            _availableModels.value = _availableModels.value.map {
                if (it.id == modelId) it.copy(sha256Verified = true) else it
            }
        }
        Result.success(computed)
    }

    suspend fun registerImportedModelFromUri(uri: android.net.Uri, fileName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                .let { if (it.endsWith(".gguf", ignoreCase = true)) it else "$it.gguf" }
            val destination = File(modelsDir, safeName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            } ?: return@withContext Result.failure(IllegalStateException("Unable to read selected file URI."))
            refreshModelCatalog()
            Result.success(destination.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerImportedModel(fileName: String, bytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        try {
            val destination = File(modelsDir, fileName)
            destination.writeBytes(bytes)
            refreshModelCatalog()
            Result.success(destination.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteModel(modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            pausedByUser.remove(modelId)
            val matching = _availableModels.value.find { it.id == modelId }
            matching?.localFilePath?.let { path ->
                val f = File(path)
                if (f.exists()) f.delete()
            }
            val partFile = File(modelsDir, "$modelId.gguf.part")
            if (partFile.exists()) partFile.delete()

            _downloadProgress.value = _downloadProgress.value - modelId
            _downloadStatus.value = _downloadStatus.value - modelId
            _sha256Digests.value = _sha256Digests.value - modelId
            _lifecycleStates.value = _lifecycleStates.value + (modelId to LocalModelLifecycleState.NOT_DOWNLOADED)
            refreshModelCatalog()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val DEFAULT_RECOMMENDED_MODEL_ID = "gemma-3-1b-it-q4"
    }
}
