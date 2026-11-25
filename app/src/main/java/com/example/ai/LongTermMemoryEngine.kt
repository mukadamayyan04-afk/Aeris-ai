package com.example.ai

import com.example.domain.model.MemoryItem
import java.util.Locale

/**
 * Engine 9: Long-Term Memory Engine
 * Retrieves ONLY relevant Room-backed memories (skipping unrelated entries to reduce token bloat & latency)
 * and checks for sensitive fields requiring user consent.
 */
class LongTermMemoryEngine {

    fun selectRelevantMemories(
        prompt: String,
        memories: List<MemoryItem>,
        projectTag: String = "",
        maxItems: Int = 5
    ): List<MemoryItem> {
        if (memories.isEmpty()) return emptyList()
        val lower = prompt.lowercase(Locale.ROOT)
        val tokens = lower.split(Regex("\\W+")).filter { it.length >= 3 }.toSet()
        val askingPersonalProfile = lower.contains("remember") ||
            lower.contains("memory") ||
            lower.contains("about me") ||
            lower.contains("my name") ||
            lower.contains("preference")

        val scored = memories.map { item ->
            var score = 0
            val keyLower = item.key.lowercase(Locale.ROOT)
            val contentLower = item.content.lowercase(Locale.ROOT)
            if (lower.contains(keyLower)) score += 10
            if (projectTag.isNotBlank() && item.projectTag.equals(projectTag, ignoreCase = true)) score += 6
            tokens.forEach { token ->
                if (keyLower.contains(token)) score += 3
                if (contentLower.contains(token)) score += 2
            }
            if (askingPersonalProfile && (
                    item.category.equals("Preferences", ignoreCase = true) ||
                        item.category.equals("Personal", ignoreCase = true)
                    )
            ) {
                score += 4
            }
            item to score
        }

        return scored
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }
            .take(maxItems)
            .map { (item, _) -> item }
    }

    fun looksSensitive(key: String, content: String): Boolean {
        val combined = "$key $content".lowercase(Locale.ROOT)
        return listOf(
            "password", "passcode", "pin", "otp", "cvv", "card",
            "bank", "account number", "aadhaar", "pan", "secret", "token", "api key"
        ).any { combined.contains(it) }
    }
}
