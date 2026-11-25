package com.example.ai

import com.example.domain.model.ChatMessage
import com.example.domain.model.MemoryItem
import kotlinx.coroutines.flow.Flow

data class ModelEngineState(
    val isLoaded: Boolean,
    val modelId: String?,
    val modelName: String?,
    val engineType: String,
    val memoryUsageMb: Long,
    val nativeLibraryStatus: String,
    val supportsArm64V8a: Boolean
)

interface LlmEngine {
    val state: Flow<ModelEngineState>

    suspend fun loadModel(modelPath: String, modelId: String): Result<Boolean>
    suspend fun unloadModel(): Result<Unit>
    suspend fun isModelLoaded(): Boolean

    fun generateStream(
        prompt: String,
        history: List<ChatMessage> = emptyList(),
        memories: List<MemoryItem> = emptyList()
    ): Flow<String>

    suspend fun cancelGeneration()
}
