package com.example.ai

import com.example.domain.model.ChatMessage
import com.example.domain.model.MessageSender

/**
 * Engine 8: Conversation Continuity Engine
 * Builds an explicit, honest textual handoff snapshot (summary, project context, attached doc, active tasks)
 * so switching models preserves context without falsely claiming internal weights or hidden state transfer.
 */
class ConversationContinuityEngine {

    fun buildSnapshot(
        conversationId: Long,
        existingSummary: String,
        projectContext: String,
        history: List<ChatMessage>,
        attachedFileName: String? = null,
        attachedFileExcerpt: String? = null,
        activeTasksSummary: String = "",
        previousModelId: String? = null
    ): ContinuitySnapshot {
        val computedSummary = if (existingSummary.isNotBlank()) {
            existingSummary
        } else {
            summarizeTurns(history)
        }
        return ContinuitySnapshot(
            conversationId = conversationId,
            rollingSummary = computedSummary,
            projectContext = projectContext,
            attachedFileName = attachedFileName,
            attachedFileExcerpt = attachedFileExcerpt,
            activeTasksSummary = activeTasksSummary,
            previousModelId = previousModelId
        )
    }

    fun summarizeTurns(history: List<ChatMessage>): String {
        if (history.isEmpty()) return ""
        val valid = history.filter { !it.isError && it.text.isNotBlank() }
        if (valid.isEmpty()) return ""
        val recent = valid.takeLast(6)
        return recent.joinToString(" | ") { msg ->
            val role = when (msg.sender) {
                MessageSender.USER -> "Sir ji"
                MessageSender.AERIS -> "AERIS"
                MessageSender.SYSTEM -> "System"
            }
            val cleaned = msg.text.replace('\n', ' ').trim()
            "$role: ${cleaned.take(120)}${if (cleaned.length > 120) "..." else ""}"
        }.take(600)
    }

    fun buildHandoffBannerText(fromModelId: String?, toModelId: String): String {
        if (fromModelId.isNullOrBlank() || fromModelId == toModelId) return ""
        return "Continuity handoff: $fromModelId -> $toModelId (conversation history, summary & active memories passed explicitly)"
    }
}
