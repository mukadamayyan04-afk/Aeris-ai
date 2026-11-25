package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.database.entities.ChatMessageEntity
import com.example.data.database.entities.ConversationEntity
import com.example.data.database.entities.LocalModelEntity
import com.example.data.database.entities.MemoryEntity
import com.example.data.database.entities.ProjectEntity
import com.example.data.database.entities.ProjectFileEntity
import com.example.data.database.entities.ProjectNoteEntity
import com.example.data.database.entities.ProjectTaskEntity
import com.example.data.database.entities.ReminderEntity
import com.example.data.database.entities.UserFeedbackEntity
import com.example.data.database.entities.VoiceTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun getConversationsForProject(projectId: Long): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: Long): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: ConversationEntity): Long

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Query("UPDATE conversations SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun renameConversation(id: Long, title: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET summary = :summary, projectContext = :projectContext, activeModelId = :activeModelId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateContinuity(
        id: Long,
        summary: String,
        projectContext: String,
        activeModelId: String?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE conversations SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touchConversation(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM conversations")
    suspend fun clearAll()

    @Query("SELECT * FROM conversations WHERE title LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchConversations(query: String): Flow<List<ConversationEntity>>
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesList(conversationId: Long): List<ChatMessageEntity>

    @Query("SELECT * FROM (SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT :limit) ORDER BY timestamp ASC")
    suspend fun getRecentMessagesList(conversationId: Long, limit: Int): List<ChatMessageEntity>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun getMessageCount(conversationId: Long): Int

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessageForConversation(conversationId: Long): ChatMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity): Long

    @Query("UPDATE chat_messages SET feedbackRating = :rating WHERE id = :messageId")
    suspend fun updateFeedbackRating(messageId: Long, rating: Int)

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteById(messageId: Long)

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteForConversation(conversationId: Long)

    @Query("SELECT * FROM chat_messages WHERE text LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    suspend fun searchMessages(query: String): List<ChatMessageEntity>
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE `key` LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR projectTag LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchMemories(query: String): Flow<List<MemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: MemoryEntity): Long

    @Update
    suspend fun update(memory: MemoryEntity)

    @Delete
    suspend fun delete(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM memories")
    suspend fun clearAll()
}

@Dao
interface VoiceTaskDao {
    @Query("SELECT * FROM voice_tasks ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentTasks(limit: Int = 50): Flow<List<VoiceTaskEntity>>

    @Query("SELECT COUNT(*) FROM voice_tasks")
    fun getTaskCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: VoiceTaskEntity): Long

    @Query("DELETE FROM voice_tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM voice_tasks")
    suspend fun clearAll()
}

@Dao
interface LocalModelDao {
    @Query("SELECT * FROM model_metadata ORDER BY updatedAt DESC")
    fun getAllModels(): Flow<List<LocalModelEntity>>

    @Query("SELECT * FROM model_metadata WHERE category = :category ORDER BY updatedAt DESC")
    fun getModelsByCategory(category: String): Flow<List<LocalModelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(model: LocalModelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(models: List<LocalModelEntity>)

    @Query("DELETE FROM model_metadata WHERE modelId = :modelId")
    suspend fun deleteById(modelId: String)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY triggerAtMillis ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Query("UPDATE reminders SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface UserFeedbackDao {
    @Query("SELECT * FROM user_feedback ORDER BY createdAt DESC")
    fun getAllFeedback(): Flow<List<UserFeedbackEntity>>

    @Query("SELECT * FROM user_feedback WHERE isApproved = 1 ORDER BY createdAt DESC")
    fun getApprovedFeedback(): Flow<List<UserFeedbackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(feedback: UserFeedbackEntity): Long

    @Query("UPDATE user_feedback SET isApproved = :approved WHERE id = :id")
    suspend fun setApproved(id: Long, approved: Boolean)

    @Query("DELETE FROM user_feedback WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM user_feedback")
    suspend fun clearAll()
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Query("UPDATE projects SET summary = :summary, progressPercent = :progressPercent, updatedAt = :updatedAt WHERE id = :projectId")
    suspend fun updateProjectSummaryAndProgress(
        projectId: Long,
        summary: String,
        progressPercent: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: Long)

    @Query("SELECT * FROM project_notes WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun getNotesForProject(projectId: Long): Flow<List<ProjectNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: ProjectNoteEntity): Long

    @Query("DELETE FROM project_notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)

    @Query("SELECT * FROM project_tasks WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getTasksForProject(projectId: Long): Flow<List<ProjectTaskEntity>>

    @Query("SELECT * FROM project_tasks WHERE projectId = :projectId")
    suspend fun getTasksListForProject(projectId: Long): List<ProjectTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ProjectTaskEntity): Long

    @Query("UPDATE project_tasks SET isDone = :isDone WHERE id = :taskId")
    suspend fun setTaskDone(taskId: Long, isDone: Boolean)

    @Query("DELETE FROM project_tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    @Query("SELECT * FROM project_files WHERE projectId = :projectId ORDER BY addedAt DESC")
    fun getFilesForProject(projectId: Long): Flow<List<ProjectFileEntity>>

    @Query("SELECT * FROM project_files ORDER BY addedAt DESC")
    fun getAllFiles(): Flow<List<ProjectFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: ProjectFileEntity): Long

    @Query("DELETE FROM project_files WHERE id = :fileId")
    suspend fun deleteFileById(fileId: Long)
}
