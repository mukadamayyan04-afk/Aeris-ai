package com.example.ai

import com.example.domain.model.ActionType
import java.util.Locale

/**
 * Engine 7: Tool and Task Execution Engine
 * Parses actionable Android intents and tool triggers cleanly without executing unverified or unsafe commands.
 */
class ToolTaskExecutionEngine {

    data class ParsedToolRequest(
        val actionType: ActionType,
        val detail: String,
        val requiresConfirmation: Boolean,
        val rationale: String
    )

    fun detectActionRequest(prompt: String): ParsedToolRequest? {
        val lower = prompt.lowercase(Locale.ROOT).trim()
        return when {
            lower.contains("turn on flashlight") || lower.contains("flashlight on") || lower.contains("torch on") ->
                ParsedToolRequest(ActionType.FLASHLIGHT, "ON", requiresConfirmation = false, rationale = "Hardware torch activation")
            lower.contains("turn off flashlight") || lower.contains("flashlight off") || lower.contains("torch off") ->
                ParsedToolRequest(ActionType.FLASHLIGHT, "OFF", requiresConfirmation = false, rationale = "Hardware torch deactivation")
            lower.contains("set a timer") || lower.contains("set timer") -> {
                val mins = Regex("(\\d+)\\s*(min|minute|minutes)").find(lower)?.groupValues?.get(1)?.toIntOrNull()
                val secs = Regex("(\\d+)\\s*(sec|second|seconds)").find(lower)?.groupValues?.get(1)?.toIntOrNull()
                val totalSeconds = when {
                    mins != null -> mins * 60
                    secs != null -> secs
                    else -> 60
                }
                ParsedToolRequest(ActionType.TIMER, "${totalSeconds}s", requiresConfirmation = false, rationale = "Android Clock timer")
            }
            lower.contains("set alarm") || lower.contains("set an alarm") -> {
                ParsedToolRequest(ActionType.ALARM, prompt, requiresConfirmation = false, rationale = "Android Clock alarm")
            }
            lower.contains("remind me") || lower.contains("create reminder") || lower.contains("set reminder") -> {
                ParsedToolRequest(ActionType.REMINDER, prompt, requiresConfirmation = false, rationale = "Calendar / Local reminder")
            }
            lower.contains("check battery") || lower.contains("battery level") || lower.contains("battery status") -> {
                ParsedToolRequest(ActionType.BATTERY_CHECK, "battery", requiresConfirmation = false, rationale = "Read battery telemetry")
            }
            lower.contains("check storage") || lower.contains("storage space") || lower.contains("free storage") -> {
                ParsedToolRequest(ActionType.STORAGE_INFO, "storage", requiresConfirmation = false, rationale = "Inspect internal storage")
            }
            lower.contains("what time is it") || lower.contains("current time") || lower.contains("date and time") -> {
                ParsedToolRequest(ActionType.DATE_TIME, "datetime", requiresConfirmation = false, rationale = "Read local date and time")
            }
            lower.contains("open settings") || lower.contains("open overlay settings") ||
                lower.contains("open wifi settings") || lower.contains("open bluetooth settings") -> {
                ParsedToolRequest(ActionType.SETTINGS_PAGE, prompt, requiresConfirmation = true, rationale = "Navigate to Android Settings")
            }
            lower.startsWith("open ") && lower.length > 6 && !lower.contains("chat") && !lower.contains("work mode") -> {
                val appHint = prompt.substringAfter("open ", "").trim()
                ParsedToolRequest(ActionType.OPEN_APP, appHint, requiresConfirmation = true, rationale = "Launch external Android app")
            }
            else -> null
        }
    }
}
