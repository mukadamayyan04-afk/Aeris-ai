package com.example.ai

import com.example.domain.model.ActionType
import java.util.Locale

/**
 * Engine 2: Reasoning and Planning Engine
 * Classifies task category, complexity, tool/device-action intent, and vision requirements.
 */
class ReasoningPlanningEngine {

    fun analyze(
        prompt: String,
        hasAttachedDocument: Boolean = false,
        hasVisionAttachment: Boolean = false
    ): ReasoningPlan {
        val lower = prompt.lowercase(Locale.ROOT).trim()

        val suggestedAction: Pair<ActionType, String>? = when {
            lower.contains("flashlight on") || lower.contains("turn on flashlight") || lower.contains("torch on") ->
                ActionType.FLASHLIGHT to "ON"
            lower.contains("flashlight off") || lower.contains("turn off flashlight") || lower.contains("torch off") ->
                ActionType.FLASHLIGHT to "OFF"
            lower.contains("set a timer") || lower.contains("set timer") || lower.contains("timer for") ->
                ActionType.TIMER to prompt
            lower.contains("set alarm") || lower.contains("wake me up at") ->
                ActionType.ALARM to prompt
            lower.contains("remind me") || lower.contains("reminder") ->
                ActionType.REMINDER to prompt
            lower.contains("open settings") || lower.contains("wifi settings") || lower.contains("bluetooth settings") ->
                ActionType.SETTINGS_PAGE to prompt
            lower.contains("battery") && (lower.contains("check") || lower.contains("status") || lower.contains("level")) ->
                ActionType.BATTERY_CHECK to "STATUS"
            lower.contains("diagnostics") || lower.contains("system specs") || lower.contains("ram usage") ->
                ActionType.DIAGNOSTICS to "FULL"
            else -> null
        }

        val category = when {
            suggestedAction != null -> AiTaskCategory.DEVICE_ACTION
            hasVisionAttachment || lower.contains("game coach") || lower.contains("observe my game") ->
                AiTaskCategory.GAME_COACH
            lower.startsWith("generate image") || lower.startsWith("create image") ||
                lower.contains("draw a picture") || lower.contains("image of ") ->
                AiTaskCategory.IMAGE_GENERATION
            lower.contains("code") || lower.contains("kotlin") || lower.contains("python") ||
                lower.contains("bug") || lower.contains("gradle") || lower.contains("algorithm") ||
                lower.contains("function") ->
                AiTaskCategory.CODING
            lower.contains("solve") || lower.contains("equation") || lower.contains("calculate") ||
                lower.contains("integral") || lower.contains("physics") || lower.contains("math") ->
                AiTaskCategory.MATH_STEM
            lower.contains("summarize") || lower.contains("tldr") || lower.contains("summary") ||
                hasAttachedDocument ->
                AiTaskCategory.SUMMARIZATION
            lower.contains("translate") || lower.contains("hinglish") || lower.contains("in english") ->
                AiTaskCategory.TRANSLATION
            lower.contains("study") || lower.contains("exam") || lower.contains("flashcard") ||
                lower.contains("quiz me") ->
                AiTaskCategory.STUDY_HELP
            lower.contains("search web") || lower.contains("latest news") || lower.contains("research") ->
                AiTaskCategory.WEB_RESEARCH
            else -> AiTaskCategory.GENERAL_CHAT
        }

        val complexity = when {
            prompt.length > 260 || hasAttachedDocument -> 4
            category in setOf(AiTaskCategory.CODING, AiTaskCategory.MATH_STEM, AiTaskCategory.WEB_RESEARCH) -> 4
            category in setOf(AiTaskCategory.SUMMARIZATION, AiTaskCategory.STUDY_HELP, AiTaskCategory.TRANSLATION) -> 3
            else -> 2
        }

        return ReasoningPlan(
            category = category,
            complexityScore = complexity,
            requiresVision = hasVisionAttachment,
            suggestedActionType = suggestedAction?.first,
            suggestedActionDetail = suggestedAction?.second,
            rationale = "Task=${category.label}, Complexity=$complexity/5"
        )
    }
}
