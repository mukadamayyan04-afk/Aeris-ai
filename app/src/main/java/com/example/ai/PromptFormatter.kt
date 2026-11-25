package com.example.ai

import com.example.domain.model.ChatMessage
import com.example.domain.model.MemoryItem
import com.example.domain.model.MessageSender

object PromptFormatter {

    const val SYSTEM_PROMPT = """You are AERIS AI, a respectful, futuristic female digital holographic personal AI assistant for Android.
You always address the user respectfully as 'Sir ji'.
Your interface is a futuristic Black and Cyan/Electric-Blue holographic HUD.
Assist the user with clear, intelligent, honest, actionable guidance, phone tasks, coding, math/STEM, study help, summaries, web assistance, game coaching, and memory.
Always speak and respond in clear English or Roman Hinglish. Never use Devanagari script.
Be 100% honest about connectivity, permissions, device capabilities, and action status."""

    fun format(
        userPrompt: String,
        history: List<ChatMessage>,
        memories: List<MemoryItem>
    ): String {
        val sb = StringBuilder()
        sb.append("<|system|>\n")
        sb.append(SYSTEM_PROMPT).append("\n")

        if (memories.isNotEmpty()) {
            sb.append("\n[ACTIVE LONG-TERM MEMORY]:\n")
            memories.forEach { mem ->
                sb.append("• ").append(mem.key).append(": ").append(mem.content).append("\n")
            }
        }
        sb.append("<|end|>\n")

        val recentHistory = history.takeLast(6)
        for (msg in recentHistory) {
            when (msg.sender) {
                MessageSender.USER -> {
                    sb.append("<|user|>\n").append(msg.text).append("<|end|>\n")
                }
                MessageSender.AERIS -> {
                    sb.append("<|assistant|>\n").append(msg.text).append("<|end|>\n")
                }
                MessageSender.SYSTEM -> {
                    sb.append("<|system|>\n").append(msg.text).append("<|end|>\n")
                }
            }
        }

        sb.append("<|user|>\n").append(userPrompt).append("<|end|>\n")
        sb.append("<|assistant|>\n")
        return sb.toString()
    }
}
