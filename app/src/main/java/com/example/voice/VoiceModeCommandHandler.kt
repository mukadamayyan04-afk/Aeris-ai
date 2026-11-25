package com.example.voice

import com.example.domain.model.OperatingMode

sealed class VoiceModeCommandResult {
    data class SwitchMode(
        val targetMode: OperatingMode,
        val returnToHome: Boolean = false,
        val normalizedCommand: String
    ) : VoiceModeCommandResult()

    data class Ambiguous(
        val suggestedMode: OperatingMode,
        val promptMessage: String,
        val originalTranscript: String,
        val returnToHome: Boolean = false
    ) : VoiceModeCommandResult()

    data object NotAModeCommand : VoiceModeCommandResult()
}

/**
 * Parses recognized voice transcripts for AERIS display/operating mode switches.
 * Strictly ignores unrelated speech and conversational questions while asking
 * confirmation on ambiguous mode directives.
 */
object VoiceModeCommandHandler {

    private val workModeExactPhrases = setOf(
        "enter landscape mode",
        "switch to work mode",
        "activate command center",
        "landscape mode on",
        "turn on landscape mode",
        "enable landscape mode",
        "switch to landscape mode",
        "open landscape mode",
        "enter work mode",
        "activate work mode",
        "open work mode",
        "start work mode",
        "work mode on",
        "turn on work mode",
        "enable work mode",
        "enter command center",
        "open command center",
        "switch to command center",
        "command center on"
    )

    private val normalModeExactPhrases = setOf(
        "exit work mode",
        "leave work mode",
        "close work mode",
        "stop work mode",
        "work mode off",
        "turn off work mode",
        "disable work mode",
        "switch to normal mode",
        "enter normal mode",
        "return to normal mode",
        "go to normal mode",
        "normal mode on",
        "turn on normal mode",
        "portrait mode on",
        "enter portrait mode",
        "switch to portrait mode",
        "turn on portrait mode",
        "enable portrait mode",
        "exit landscape mode",
        "landscape mode off",
        "turn off landscape mode",
        "exit command center",
        "close command center"
    )

    private val returnToHomePhrases = setOf(
        "return to home",
        "go back to home",
        "back to home",
        "return home",
        "go home",
        "switch to home"
    )

    private val questionPrefixes = listOf(
        "what ",
        "why ",
        "how ",
        "when ",
        "where ",
        "who ",
        "explain ",
        "tell me ",
        "can you explain ",
        "what is ",
        "whats "
    )

    fun parse(
        rawTranscript: String,
        currentMode: OperatingMode = OperatingMode.NORMAL
    ): VoiceModeCommandResult {
        val normalizedFull = normalize(rawTranscript)
        if (normalizedFull.isBlank()) return VoiceModeCommandResult.NotAModeCommand

        val withoutWakePrefix = stripLeadingAssistantName(normalizedFull)

        // 1. Check explicit return-to-home commands first
        if (withoutWakePrefix in returnToHomePhrases) {
            return VoiceModeCommandResult.SwitchMode(
                targetMode = OperatingMode.NORMAL,
                returnToHome = true,
                normalizedCommand = withoutWakePrefix
            )
        }

        // 2. Check explicit Work Mode commands
        if (withoutWakePrefix in workModeExactPhrases) {
            return VoiceModeCommandResult.SwitchMode(
                targetMode = OperatingMode.WORK,
                returnToHome = false,
                normalizedCommand = withoutWakePrefix
            )
        }

        // 3. Check explicit Normal Mode commands
        if (withoutWakePrefix in normalModeExactPhrases) {
            return VoiceModeCommandResult.SwitchMode(
                targetMode = OperatingMode.NORMAL,
                returnToHome = false,
                normalizedCommand = withoutWakePrefix
            )
        }

        // 4. Ignore informational questions or long unrelated sentences
        if (questionPrefixes.any { withoutWakePrefix.startsWith(it) } ||
            rawTranscript.trim().endsWith("?")
        ) {
            return VoiceModeCommandResult.NotAModeCommand
        }

        val words = withoutWakePrefix.split(" ").filter { it.isNotBlank() }
        if (words.size > 7) {
            return VoiceModeCommandResult.NotAModeCommand
        }

        // 5. Check ambiguous short mode/orientation directives
        val mentionsWorkOrLandscape =
            withoutWakePrefix.contains("work mode") ||
                withoutWakePrefix.contains("landscape") ||
                withoutWakePrefix.contains("command center")

        val mentionsNormalOrPortrait =
            withoutWakePrefix.contains("normal mode") ||
                withoutWakePrefix.contains("portrait")

        val mentionsGenericModeSwitch =
            withoutWakePrefix.contains("switch mode") ||
                withoutWakePrefix.contains("change mode") ||
                withoutWakePrefix.contains("toggle mode") ||
                withoutWakePrefix.contains("change orientation") ||
                withoutWakePrefix.contains("rotate screen") ||
                withoutWakePrefix.contains("screen mode") ||
                withoutWakePrefix == "mode" ||
                withoutWakePrefix == "work"

        return when {
            mentionsWorkOrLandscape && mentionsNormalOrPortrait -> {
                val opposite = if (currentMode == OperatingMode.WORK) OperatingMode.NORMAL else OperatingMode.WORK
                VoiceModeCommandResult.Ambiguous(
                    suggestedMode = opposite,
                    promptMessage = "Did you want to switch to ${opposite.title}, Sir ji?",
                    originalTranscript = rawTranscript
                )
            }
            mentionsWorkOrLandscape -> {
                val wantsOff = words.any { it in setOf("off", "exit", "close", "stop", "leave", "disable") }
                if (wantsOff) {
                    VoiceModeCommandResult.SwitchMode(
                        targetMode = OperatingMode.NORMAL,
                        returnToHome = false,
                        normalizedCommand = withoutWakePrefix
                    )
                } else {
                    val target = if (currentMode == OperatingMode.WORK) OperatingMode.NORMAL else OperatingMode.WORK
                    VoiceModeCommandResult.Ambiguous(
                        suggestedMode = target,
                        promptMessage = "Confirm switching to ${target.title}, Sir ji?",
                        originalTranscript = rawTranscript
                    )
                }
            }
            mentionsNormalOrPortrait -> {
                val wantsOff = words.any { it in setOf("off", "exit", "disable") }
                val target = if (wantsOff) OperatingMode.WORK else OperatingMode.NORMAL
                VoiceModeCommandResult.Ambiguous(
                    suggestedMode = target,
                    promptMessage = "Confirm switching to ${target.title}, Sir ji?",
                    originalTranscript = rawTranscript
                )
            }
            mentionsGenericModeSwitch -> {
                val opposite = if (currentMode == OperatingMode.WORK) OperatingMode.NORMAL else OperatingMode.WORK
                VoiceModeCommandResult.Ambiguous(
                    suggestedMode = opposite,
                    promptMessage = "Switch from ${currentMode.title} to ${opposite.title}, Sir ji?",
                    originalTranscript = rawTranscript
                )
            }
            else -> VoiceModeCommandResult.NotAModeCommand
        }
    }

    internal fun normalize(input: String): String {
        return input
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun stripLeadingAssistantName(normalized: String): String {
        val prefixes = listOf(
            "hey aeris please ",
            "ok aeris please ",
            "hey aeris ",
            "ok aeris ",
            "aeris please ",
            "aeris ",
            "please "
        )
        var current = normalized
        var changed = true
        while (changed) {
            changed = false
            for (prefix in prefixes) {
                if (current.startsWith(prefix)) {
                    current = current.removePrefix(prefix).trim()
                    changed = true
                    break
                }
            }
        }
        return current
    }
}
