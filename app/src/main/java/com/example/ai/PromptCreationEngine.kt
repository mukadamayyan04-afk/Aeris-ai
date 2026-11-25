package com.example.ai

import com.example.domain.model.ChatMessage
import com.example.domain.model.MemoryItem
import com.example.domain.model.MessageSender
import com.example.domain.model.UserFeedbackRecord

/**
 * Engine 6: Prompt Creation Engine
 * Smart Context Optimization:
 * - Scales conversation window & rolling summary strictly by task complexity so simple queries stay lean & fast.
 * - Includes compact continuity summary, relevant project context, relevant memories, and user-approved rules.
 */
class PromptCreationEngine(
    private val personalityEngine: PersonalityEngine,
    private val feedbackEngine: UserFeedbackPreferenceEngine
) {
    data class BuiltPrompt(
        val systemInstruction: String,
        val turns: List<ChatMessage>,
        val userMessageText: String
    )

    fun build(
        userPrompt: String,
        plan: ReasoningPlan,
        history: List<ChatMessage>,
        relevantMemories: List<MemoryItem>,
        continuity: ContinuitySnapshot?,
        approvedFeedback: List<UserFeedbackRecord> = emptyList(),
        userName: String = "Sir ji",
        personalityStyle: AerisPersonalityStyle = AerisPersonalityStyle.CALM_PROFESSIONAL,
        responseLength: AerisResponseLength = AerisResponseLength.BALANCED,
        customSystemInstruction: String = ""
    ): BuiltPrompt {
        val isSimpleQuery = plan.complexityScore <= 2 &&
            continuity?.attachedFileExcerpt.isNullOrBlank()

        val sb = StringBuilder(512)
        sb.append(
            personalityEngine.buildIdentityDirectives(
                userName = userName,
                style = personalityStyle,
                responseLength = responseLength
            )
        ).append('\n')
        sb.append("[TASK] ").append(plan.category.label)
            .append(" (Complexity ").append(plan.complexityScore).append("/5)\n")

        if (continuity != null) {
            if (continuity.projectContext.isNotBlank()) {
                sb.append("[PROJECT]: ").append(continuity.projectContext).append('\n')
            }
            if (continuity.rollingSummary.isNotBlank()) {
                val summaryBudget = if (isSimpleQuery) 280 else 600
                sb.append("[SUMMARY]: ").append(continuity.rollingSummary.take(summaryBudget)).append('\n')
            }
            if (!isSimpleQuery && continuity.activeTasksSummary.isNotBlank()) {
                sb.append("[TASKS]: ").append(continuity.activeTasksSummary).append('\n')
            }
        }

        if (relevantMemories.isNotEmpty()) {
            val maxMem = if (isSimpleQuery) 3 else 6
            sb.append("[MEMORY]:\n")
            relevantMemories.take(maxMem).forEach { m ->
                val tag = if (m.projectTag.isNotBlank()) " (${m.projectTag})" else ""
                sb.append("- ").append(m.key).append(tag).append(": ").append(m.content).append('\n')
            }
        }

        val feedbackRules = feedbackEngine.formatApprovedPreferences(approvedFeedback)
        if (feedbackRules.isNotBlank()) {
            sb.append("[PREFERENCES]:\n").append(feedbackRules).append('\n')
        }
        if (customSystemInstruction.isNotBlank()) {
            sb.append("[USER CUSTOM INSTRUCTIONS]:\n").append(customSystemInstruction.take(800)).append('\n')
        }

        val finalUserText = if (continuity?.attachedFileName != null && !continuity.attachedFileExcerpt.isNullOrBlank()) {
            buildString {
                append("[FILE: ").append(continuity.attachedFileName).append("]\n")
                append(continuity.attachedFileExcerpt.take(8000)).append("\n[/FILE]\n\n")
                append(userPrompt)
            }
        } else {
            userPrompt
        }

        val turnLimit = if (isSimpleQuery) 4 else 8
        val cleanHistory = history
            .filter { !it.isError && it.sender != MessageSender.SYSTEM && it.text.isNotBlank() }
            .takeLast(turnLimit)

        return BuiltPrompt(
            systemInstruction = sb.toString().trim(),
            turns = cleanHistory,
            userMessageText = finalUserText
        )
    }
}
