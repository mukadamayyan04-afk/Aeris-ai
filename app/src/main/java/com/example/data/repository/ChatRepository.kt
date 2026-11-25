package com.example.data.repository

import com.example.data.database.ChatMessageDao
import com.example.data.database.ConversationDao
import com.example.data.database.entities.ChatMessageEntity
import com.example.data.database.entities.ConversationEntity
import com.example.domain.model.ChatMessage
import com.example.domain.model.Conversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ChatRepository {
    fun getAllConversations(): Flow<List<Conversation>>
    fun getConversationsForProject(projectId: Long): Flow<List<Conversation>>
    fun searchConversations(query: String): Flow<List<Conversation>>
    suspend fun getConversationById(id: Long): Conversation?
    suspend fun createConversation(
        title: String,
        projectId: Long? = null,
        projectContext: String = ""
    ): Long
    suspend fun renameConversation(id: Long, title: String)
    suspend fun updateConversationContinuity(
        id: Long,
        summary: String,
        projectContext: String,
        activeModelId: String?
    )
    suspend fun deleteConversation(id: Long)
    suspend fun clearAllConversations()

    fun getMessagesForConversation(conversationId: Long): Flow<List<ChatMessage>>
    suspend fun getMessagesSnapshot(conversationId: Long): List<ChatMessage>
    suspend fun getRecentMessagesSnapshot(conversationId: Long, limit: Int = 14): List<ChatMessage>
    suspend fun sendMessage(message: ChatMessage): Long
    suspend fun updateMessageFeedback(messageId: Long, rating: Int)
    suspend fun deleteMessage(messageId: Long)
    suspend fun searchMessages(query: String): List<ChatMessage>
}

class ChatRepositoryImpl(
    private val conversationDao: ConversationDao,
    private val messageDao: ChatMessageDao
) : ChatRepository {

    override fun getAllConversations(): Flow<List<Conversation>> {
        return conversationDao.getAllConversations().map { entities ->
            entities.map { entity ->
                val count = messageDao.getMessageCount(entity.id)
                val lastMsg = messageDao.getLastMessageForConversation(entity.id)?.text
                entity.toDomain(count, lastMsg)
            }
        }
    }

    override fun getConversationsForProject(projectId: Long): Flow<List<Conversation>> {
        return conversationDao.getConversationsForProject(projectId).map { entities ->
            entities.map { entity ->
                val count = messageDao.getMessageCount(entity.id)
                val lastMsg = messageDao.getLastMessageForConversation(entity.id)?.text
                entity.toDomain(count, lastMsg)
            }
        }
    }

    override fun searchConversations(query: String): Flow<List<Conversation>> {
        return conversationDao.searchConversations(query).map { entities ->
            entities.map { entity ->
                val count = messageDao.getMessageCount(entity.id)
                val lastMsg = messageDao.getLastMessageForConversation(entity.id)?.text
                entity.toDomain(count, lastMsg)
            }
        }
    }

    override suspend fun getConversationById(id: Long): Conversation? {
        val entity = conversationDao.getConversationById(id) ?: return null
        val count = messageDao.getMessageCount(id)
        val lastMsg = messageDao.getLastMessageForConversation(id)?.text
        return entity.toDomain(count, lastMsg)
    }

    override suspend fun createConversation(
        title: String,
        projectId: Long?,
        projectContext: String
    ): Long {
        val entity = ConversationEntity(
            title = title,
            projectId = projectId,
            projectContext = projectContext
        )
        return conversationDao.insert(entity)
    }

    override suspend fun renameConversation(id: Long, title: String) {
        conversationDao.renameConversation(id, title)
    }

    override suspend fun updateConversationContinuity(
        id: Long,
        summary: String,
        projectContext: String,
        activeModelId: String?
    ) {
        conversationDao.updateContinuity(id, summary, projectContext, activeModelId)
    }

    override suspend fun deleteConversation(id: Long) {
        messageDao.deleteForConversation(id)
        conversationDao.deleteById(id)
    }

    override suspend fun clearAllConversations() {
        conversationDao.clearAll()
    }

    override fun getMessagesForConversation(conversationId: Long): Flow<List<ChatMessage>> {
        return messageDao.getMessagesForConversation(conversationId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getMessagesSnapshot(conversationId: Long): List<ChatMessage> {
        return messageDao.getMessagesList(conversationId).map { it.toDomain() }
    }

    override suspend fun getRecentMessagesSnapshot(conversationId: Long, limit: Int): List<ChatMessage> {
        return messageDao.getRecentMessagesList(conversationId, limit).map { it.toDomain() }
    }

    override suspend fun sendMessage(message: ChatMessage): Long {
        val entity = ChatMessageEntity.fromDomain(message)
        val id = messageDao.insert(entity)
        conversationDao.touchConversation(message.conversationId)
        return id
    }

    override suspend fun updateMessageFeedback(messageId: Long, rating: Int) {
        messageDao.updateFeedbackRating(messageId, rating)
    }

    override suspend fun deleteMessage(messageId: Long) {
        messageDao.deleteById(messageId)
    }

    override suspend fun searchMessages(query: String): List<ChatMessage> {
        return messageDao.searchMessages(query).map { it.toDomain() }
    }
}
