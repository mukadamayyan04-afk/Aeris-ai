package com.example.ai

import android.os.Build
import com.example.domain.model.ChatMessage
import com.example.domain.model.MemoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

interface LocalAiEngine : LlmEngine {
    val lifecycleState: StateFlow<LocalModelLifecycleState>
    fun checkRamForModel(requiredRamMb: Int, availableRamMb: Long): Boolean
}

class LocalLlmEngine : LocalAiEngine {

    private val isArm64 = Build.SUPPORTED_ABIS.any { it.contains("arm64-v8a", ignoreCase = true) }

    private var isNativeLibLoaded = false
    private var nativeInitStatus = "Uninitialized"

    private val _lifecycleState = MutableStateFlow(LocalModelLifecycleState.NOT_DOWNLOADED)
    override val lifecycleState: StateFlow<LocalModelLifecycleState> = _lifecycleState.asStateFlow()

    private val _state = MutableStateFlow(
        ModelEngineState(
            isLoaded = false,
            modelId = null,
            modelName = null,
            engineType = "llama.cpp local inference (runtime verification required)",
            memoryUsageMb = 0L,
            nativeLibraryStatus = "Native inference runtime not verified",
            supportsArm64V8a = isArm64
        )
    )
    override val state: Flow<ModelEngineState> = _state.asStateFlow()

    init {
        checkNativeBridge()
    }

    private fun checkNativeBridge() {
        try {
            System.loadLibrary("llama")
            isNativeLibLoaded = true
            nativeInitStatus = "llama.cpp JNI arm64-v8a active"
        } catch (_: UnsatisfiedLinkError) {
            isNativeLibLoaded = false
            nativeInitStatus = "libllama.so missing; neural local inference unavailable"
        }
        _state.value = _state.value.copy(
            nativeLibraryStatus = nativeInitStatus
        )
    }

    override fun checkRamForModel(requiredRamMb: Int, availableRamMb: Long): Boolean {
        return availableRamMb >= requiredRamMb
    }

    override suspend fun loadModel(modelPath: String, modelId: String): Result<Boolean> = withContext(Dispatchers.Default) {
        loadModelWithRamCheck(modelPath = modelPath, modelId = modelId, requiredRamMb = 0, availableRamMb = Long.MAX_VALUE)
    }

    suspend fun loadModelWithRamCheck(
        modelPath: String,
        modelId: String,
        requiredRamMb: Int,
        availableRamMb: Long
    ): Result<Boolean> = withContext(Dispatchers.Default) {
        if (modelPath.isBlank()) {
            _lifecycleState.value = LocalModelLifecycleState.ERROR
            return@withContext Result.failure(IllegalArgumentException("Model path cannot be blank"))
        }
        if (requiredRamMb > 0 && !checkRamForModel(requiredRamMb, availableRamMb)) {
            _lifecycleState.value = LocalModelLifecycleState.ERROR
            return@withContext Result.failure(
                IllegalStateException("Insufficient free RAM (${availableRamMb} MB available, ${requiredRamMb} MB required for $modelId).")
            )
        }
        if (!java.io.File(modelPath).isFile) {
            _lifecycleState.value = LocalModelLifecycleState.NOT_DOWNLOADED
            return@withContext Result.failure(
                IllegalArgumentException("Model file does not exist at the selected path.")
            )
        }
        if (!isNativeLibLoaded) {
            _lifecycleState.value = LocalModelLifecycleState.ERROR
            return@withContext Result.failure(
                IllegalStateException("Local model was not loaded: the llama.cpp native runtime is missing.")
            )
        }

        // A shared library alone does not prove that token inference is wired.
        // Keep loading blocked until the JNI inference bridge is implemented and verified.
        _lifecycleState.value = LocalModelLifecycleState.ERROR
        Result.failure(
            IllegalStateException("Local model loading is unavailable until the llama.cpp JNI inference bridge is implemented and verified.")
        )
    }

    override suspend fun unloadModel(): Result<Unit> = withContext(Dispatchers.Default) {
        _state.value = _state.value.copy(
            isLoaded = false,
            modelId = null,
            modelName = null,
            memoryUsageMb = 0L
        )
        _lifecycleState.value = LocalModelLifecycleState.NOT_DOWNLOADED
        Result.success(Unit)
    }

    override suspend fun isModelLoaded(): Boolean = _state.value.isLoaded

    override fun generateStream(
        prompt: String,
        history: List<ChatMessage>,
        memories: List<MemoryItem>
    ): Flow<String> = flow<String> {
        throw IllegalStateException(
            "Neural offline chat is unavailable: the llama.cpp JNI inference bridge is not implemented in this build."
        )
    }.flowOn(Dispatchers.Default)

    override suspend fun cancelGeneration() = Unit


}
