package com.example.ai

import com.example.data.database.UserFeedbackDao
import com.example.data.database.entities.UserFeedbackEntity
import com.example.domain.model.UserFeedbackRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Engine 10: User Feedback and Preference Engine
 * Persists user feedback & explicit style/workflow preferences in Room.
 * Strictly honest: AERIS never retrains model weights; it applies user-approved rules at prompt time.
 */
class UserFeedbackPreferenceEngine(
    private val dao: UserFeedbackDao? = null
) {
    fun getAllFeedback(): Flow<List<UserFeedbackRecord>>? =
        dao?.getAllFeedback()?.map { list -> list.map { it.toDomain() } }

    fun getApprovedFeedback(): Flow<List<UserFeedbackRecord>>? =
        dao?.getApprovedFeedback()?.map { list -> list.map { it.toDomain() } }

    suspend fun recordFeedback(
        messageId: Long?,
        rating: Int,
        category: String,
        ruleText: String,
        autoApprove: Boolean = false
    ): Long {
        if (dao == null) return 0L
        return dao.insert(
            UserFeedbackEntity(
                messageId = messageId,
                rating = rating,
                category = category,
                ruleText = ruleText.trim(),
                isApproved = autoApprove
            )
        )
    }

    suspend fun setRuleApproved(id: Long, approved: Boolean) {
        dao?.setApproved(id, approved)
    }

    suspend fun deleteFeedback(id: Long) {
        dao?.deleteById(id)
    }

    suspend fun clearAll() {
        dao?.clearAll()
    }

    fun formatApprovedPreferences(approved: List<UserFeedbackRecord>): String {
        if (approved.isEmpty()) return ""
        return approved.take(8).joinToString("\n") { "- [${it.category}] ${it.ruleText}" }
    }

    fun suggestRuleFromFeedback(rating: Int, responsePreview: String): Pair<String, String> {
        return if (rating > 0) {
            "Style" to "User liked clear, structured guidance similar to: \"${responsePreview.take(60)}...\""
        } else {
            "Style" to "Avoid overly generic/verbose answers; stay direct and practical for Sir ji."
        }
    }
}
