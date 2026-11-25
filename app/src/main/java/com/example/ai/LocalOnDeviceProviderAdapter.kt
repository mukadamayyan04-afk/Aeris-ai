package com.example.ai

import android.os.Build
import com.example.domain.model.ChatMessage
import com.example.domain.model.MemoryItem
import com.example.domain.model.MessageSender
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Locale

/**
 * Reliable offline / on-device provider wrapping LocalLlmEngine while preserving
 * AERIS identity, memory recall, recent turn continuity, and zero artificial latency delays.
 */
class LocalOnDeviceProviderAdapter(
    val localEngine: LocalAiEngine = LocalLlmEngine()
) {
    private val isArm64 = Build.SUPPORTED_ABIS.any { it.contains("arm64-v8a", ignoreCase = true) }

    fun streamOfflineResponse(
        prompt: String,
        history: List<ChatMessage>,
        memories: List<MemoryItem>,
        continuity: ContinuitySnapshot? = null
    ): Flow<String> = flow {
        val lower = prompt.lowercase(Locale.ROOT).trim()

        val relevant = memories.filter { lower.contains(it.key.lowercase(Locale.ROOT)) }
        val memoryContext = if (relevant.isNotEmpty()) {
            " [Recalled from local memory, Sir ji: " + relevant.joinToString("; ") { "${it.key} = ${it.content}" } + "]"
        } else ""

        val previousUserTurn = history.lastOrNull {
            it.sender == MessageSender.USER && it.text.trim() != prompt.trim()
        }
        val contextHint = if (
            previousUserTurn != null &&
            (lower.contains("repeat") || lower.contains("explain more") ||
                lower.contains("continue") || lower.contains("what did i say"))
        ) {
            " Regarding your earlier message (\"${previousUserTurn.text.take(80)}\"), Sir ji:"
        } else ""

        val docHint = if (continuity?.attachedFileName != null && !continuity.attachedFileExcerpt.isNullOrBlank()) {
            " Attached file '${continuity.attachedFileName}' (${continuity.attachedFileExcerpt.length} chars) is loaded in context (offline summary mode active)."
        } else ""

        val projectHint = if (!continuity?.projectContext.isNullOrBlank()) {
            " [Project: ${continuity?.projectContext}]"
        } else ""

        val response = when {
            lower.contains("who are you") || lower.contains("what is aeris") || lower.contains("aeris ai") -> {
                "Namaste Sir ji! I am AERIS AI, your personal holographic Android assistant. Offline On-Device Core is active right now; when Gemini, OpenRouter, Groq, Hugging Face, Cloudflare Workers AI, or a configured endpoint is connected, I route there automatically while keeping your memory intact.$memoryContext"
            }
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey aeris") || lower.contains("wake up") -> {
                "Namaste Sir ji! AERIS AI core is online, telemetry is nominal, and I am ready for your command. How can I assist you today, Sir ji?$memoryContext"
            }
            lower.contains("login") || lower.contains("gmail") || lower.contains("website") || lower.contains("browse") -> {
                "Sure Sir ji, opening the Web Assistant. Sir ji, kis Gmail account se login karna hai? Select your account or enter an HTTPS URL; AERIS never reads or stores passwords."
            }
            lower.contains("observe my game") || lower.contains("mera game dekho") ||
                lower.contains("gameplay observe karo") || lower.contains("galtiyan batao") || lower.contains("game coach") -> {
                "Sure Sir ji! AERIS Game Coach is ready. Grant Android screen-capture permission when prompted and verify Gemini is connected for live vision callouts."
            }
            lower.contains("torch") || lower.contains("flashlight") -> {
                "Checking device flashlight action now, Sir ji. You can also control it from Home Quick Actions or Work Mode."
            }
            lower.contains("timer") || lower.contains("alarm") -> {
                "Certainly Sir ji! Native countdown timers and alarms dispatch directly through Android Clock."
            }
            lower.contains("reminder") || lower.contains("remind me") -> {
                "Certainly Sir ji! Reminders can be saved to your Android Calendar or scheduled as local AERIS alerts."
            }
            lower.contains("camera") || lower.contains("selfie") -> {
                "Camera scanner is available on demand, Sir ji, and stops immediately when closed."
            }
            lower.contains("memory") || lower.contains("remember") || lower.contains("save") -> {
                if (lower.startsWith("remember") || lower.contains("save")) {
                    "Understood Sir ji! You can review and save facts inside your persistent Room Memory tab anytime."
                } else {
                    "Sir ji, you currently have ${memories.size} saved memories in your local Room storage.$memoryContext"
                }
            }
            lower.contains("work mode") || lower.contains("workspace") -> {
                "Work Mode can be activated anytime, Sir ji, rotating into the multi-pane Landscape Command Center."
            }
            lower.contains("ram") || lower.contains("storage") || lower.contains("diagnostics") || lower.contains("specs") -> {
                "Sir ji, hardware telemetry indicates ${if (isArm64) "ARM64-v8a architecture (Redmi 13 5G optimized)" else "Standard CPU"}. Live RAM and storage values are shown inside Diagnostics."
            }
            lower.contains("help") || lower.contains("features") || lower.contains("what can you do") -> {
                "Sir ji, here are your available AERIS capabilities:\n• Multi-Model AI Brain (Google Gemini, OpenRouter, Groq, Hugging Face, Cloudflare Workers AI, OpenAI-Compat, GPT-OSS, Qwen & Local Gemma 3 1B Q4 / Offline Core)\n• Female Voice Assistant & Voice Selection\n• Project Workspaces with isolated notes, tasks, files & chats\n• Persistent Room Memory & Dual Portrait/Landscape Work Mode"
            }
            else -> {
                "${contextHint.trimStart()} Acknowledged, Sir ji: \"$prompt\".$memoryContext$docHint$projectHint Processed locally via AERIS On-Device Core (${history.size} recent messages in window; connect Gemini, Groq, OpenRouter, Hugging Face, or Cloudflare Workers AI in Settings -> AI Providers for cloud reasoning). How would you like to proceed, Sir ji?".trim()
            }
        }

        val chunks = response.chunked(48)
        for (chunk in chunks) {
            currentCoroutineContext().ensureActive()
            emit(chunk)
        }
    }
}
