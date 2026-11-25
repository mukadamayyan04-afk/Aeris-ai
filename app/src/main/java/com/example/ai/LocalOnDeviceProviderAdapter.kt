package com.example.ai

import com.example.domain.model.ChatMessage
import com.example.domain.model.MemoryItem
import kotlinx.coroutines.flow.Flow

/**
 * Compatibility adapter for the offline provider route.
 *
 * This adapter never synthesizes canned chat replies. It delegates to the actual
 * local inference engine, which reports a clear error until the native llama.cpp
 * JNI runtime and model-loading path are implemented and verified.
 */
class LocalOnDeviceProviderAdapter(
    val localEngine: LocalAiEngine = LocalLlmEngine()
) {
    @Suppress("UNUSED_PARAMETER")
    fun streamOfflineResponse(
        prompt: String,
        history: List<ChatMessage>,
        memories: List<MemoryItem>,
        continuity: ContinuitySnapshot? = null
    ): Flow<String> = localEngine.generateStream(prompt, history, memories)
}
