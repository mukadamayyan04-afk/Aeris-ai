package com.example.ai

import android.os.Build
import com.example.domain.model.ChatMessage
import com.example.domain.model.MemoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.Locale

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

    private var isCancelled = false

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
            memoryUsageMb = 12L
        )
        _lifecycleState.value = LocalModelLifecycleState.NOT_DOWNLOADED
        Result.success(Unit)
    }

    override suspend fun isModelLoaded(): Boolean = _state.value.isLoaded

    override fun generateStream(
        prompt: String,
        history: List<ChatMessage>,
        memories: List<MemoryItem>
    ): Flow<String> = flow {
        throw IllegalStateException(
            "Neural offline chat is unavailable: the llama.cpp JNI inference bridge is not implemented in this build."
        )
    }.flowOn(Dispatchers.Default)

    override suspend fun cancelGeneration() {
        isCancelled = true
    }

    private fun synthesizeLocalResponse(prompt: String, memories: List<MemoryItem>): String {
        val lower = prompt.lowercase(Locale.ROOT).trim()

        val relevantMem = memories.filter { lower.contains(it.key.lowercase(Locale.ROOT)) }
        val memoryContext = if (relevantMem.isNotEmpty()) {
            " [Recalled from local memory: " + relevantMem.joinToString("; ") { "${it.key} = ${it.content}" } + "]"
        } else ""

        return when {
            lower.contains("who are you") || lower.contains("what is aeris") -> {
                "I am AERIS AI, your offline-first native Android personal assistant. I run locally on your device with private Room storage.$memoryContext"
            }
            lower.contains("flashlight") || lower.contains("torch") -> {
                "Checking device flashlight action now."
            }
            lower.contains("timer") || lower.contains("alarm") -> {
                "Device timer and alarm actions are linked to your native Android Clock service."
            }
            lower.contains("memory") || lower.contains("remember") -> {
                if (lower.startsWith("remember") || lower.contains("save memory")) {
                    "I can store that in your persistent local Room Memory Bank."
                } else {
                    if (memories.isEmpty()) {
                        "Your local memory bank is currently empty. You can teach me facts, preferences, or notes anytime in the Memory tab."
                    } else {
                        "You currently have ${memories.size} saved memories in your local Room storage:$memoryContext"
                    }
                }
            }
            lower.contains("diagnostics") || lower.contains("ram") || lower.contains("specs") -> {
                "System Diagnostics: Device check indicates ${if (isArm64) "ARM64-v8a architecture (Redmi 13 5G optimized)" else "Standard CPU"}. Memory usage is nominal."
            }
            lower.contains("work mode") -> {
                "Work Mode can be activated anytime to rotate into the multi-pane landscape console."
            }
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey aeris") -> {
                "Namaste Sir ji! AERIS HUD is online, telemetry is nominal, and local models are ready.$memoryContext"
            }
            lower.contains("help") -> {
                "AERIS AI Capabilities:\n• Real-time chat and speech input/output\n• Dual Normal HUD & Work Mode landscape console\n• Persistent Room memory, Project Workspaces & Search\n• Multi-provider AI routing & GGUF local weights\n• Comprehensive Device Diagnostics"
            }
            else -> {
                "Acknowledged, Sir ji: \"$prompt\". Processed locally via AERIS On-Device Core.$memoryContext"
            }
        }
    }
}
