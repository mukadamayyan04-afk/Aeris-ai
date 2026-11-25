package com.example.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.AerisReminder
import com.example.domain.model.ChatMessage
import com.example.domain.model.Conversation
import com.example.domain.model.MemoryItem
import com.example.domain.model.MessageSender
import com.example.domain.model.ModelMetadata
import com.example.domain.model.ProjectFileRef
import com.example.domain.model.ProjectNote
import com.example.domain.model.ProjectTask
import com.example.domain.model.ProjectWorkspace
import com.example.domain.model.UserFeedbackRecord
import com.example.domain.model.VoiceTaskRecord
import java.io.File

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val summary: String = "",
    val projectContext: String = "",
    val activeModelId: String? = null,
    val projectId: Long? = null
) {
    fun toDomain(messageCount: Int = 0, lastMessagePreview: String? = null): Conversation = Conversation(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        messageCount = messageCount,
        lastMessagePreview = lastMessagePreview,
        summary = summary,
        projectContext = projectContext,
        activeModelId = activeModelId,
        projectId = projectId
    )
}

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val sender: String, // USER, AERIS, SYSTEM
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelId: String? = null,
    val isError: Boolean = false,
    val attachmentName: String? = null,
    val feedbackRating: Int = 0
) {
    fun toDomain(): ChatMessage = ChatMessage(
        id = id,
        conversationId = conversationId,
        sender = when (sender) {
            "AERIS" -> MessageSender.AERIS
            "SYSTEM" -> MessageSender.SYSTEM
            else -> MessageSender.USER
        },
        text = text,
        timestamp = timestamp,
        modelId = modelId,
        isError = isError,
        attachmentName = attachmentName,
        feedbackRating = feedbackRating
    )

    companion object {
        fun fromDomain(msg: ChatMessage): ChatMessageEntity = ChatMessageEntity(
            id = msg.id,
            conversationId = msg.conversationId,
            sender = msg.sender.name,
            text = msg.text,
            timestamp = msg.timestamp,
            modelId = msg.modelId,
            isError = msg.isError,
            attachmentName = msg.attachmentName,
            feedbackRating = msg.feedbackRating
        )
    }
}

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val content: String,
    val category: String = "General",
    val createdAt: Long = System.currentTimeMillis(),
    val isUserConfirmed: Boolean = true,
    val projectTag: String = "",
    val isSensitive: Boolean = false
) {
    fun toDomain(): MemoryItem = MemoryItem(
        id = id,
        key = key,
        content = content,
        category = category,
        createdAt = createdAt,
        isUserConfirmed = isUserConfirmed,
        projectTag = projectTag,
        isSensitive = isSensitive
    )

    companion object {
        fun fromDomain(item: MemoryItem): MemoryEntity = MemoryEntity(
            id = item.id,
            key = item.key,
            content = item.content,
            category = item.category,
            createdAt = item.createdAt,
            isUserConfirmed = item.isUserConfirmed,
            projectTag = item.projectTag,
            isSensitive = item.isSensitive
        )
    }
}

@Entity(tableName = "voice_tasks")
data class VoiceTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long? = null,
    val transcript: String,
    val responsePreview: String,
    val sttEngine: String,
    val ttsEngine: String,
    val voiceId: String,
    val languageTag: String,
    val status: String,
    val errorMessage: String? = null,
    val durationMs: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): VoiceTaskRecord = VoiceTaskRecord(
        id = id,
        conversationId = conversationId,
        transcript = transcript,
        responsePreview = responsePreview,
        sttEngine = sttEngine,
        ttsEngine = ttsEngine,
        voiceId = voiceId,
        languageTag = languageTag,
        status = status,
        errorMessage = errorMessage,
        durationMs = durationMs,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(record: VoiceTaskRecord): VoiceTaskEntity = VoiceTaskEntity(
            id = record.id,
            conversationId = record.conversationId,
            transcript = record.transcript,
            responsePreview = record.responsePreview,
            sttEngine = record.sttEngine,
            ttsEngine = record.ttsEngine,
            voiceId = record.voiceId,
            languageTag = record.languageTag,
            status = record.status,
            errorMessage = record.errorMessage,
            durationMs = record.durationMs,
            createdAt = record.createdAt
        )
    }
}

@Entity(tableName = "model_metadata")
data class LocalModelEntity(
    @PrimaryKey
    val modelId: String,
    val name: String,
    val category: String,
    val parameterSize: String,
    val quantization: String,
    val fileSizeBytes: Long,
    val requiredRamMb: Int,
    val isInstalled: Boolean,
    val localFilePath: String? = null,
    val downloadUrl: String = "",
    val license: String = "",
    val isRecommendedForDevice: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): ModelMetadata = ModelMetadata(
        id = modelId,
        name = name,
        parameterSize = parameterSize,
        quantization = quantization,
        fileSizeBytes = fileSizeBytes,
        requiredRamMb = requiredRamMb,
        isInstalled = isInstalled && !localFilePath.isNullOrBlank() && File(localFilePath).exists(),
        localFilePath = localFilePath,
        downloadUrl = downloadUrl,
        license = license,
        isRecommendedForDevice = isRecommendedForDevice
    )

    companion object {
        fun fromDomain(model: ModelMetadata, category: String = "LLM"): LocalModelEntity = LocalModelEntity(
            modelId = model.id,
            name = model.name,
            category = category,
            parameterSize = model.parameterSize,
            quantization = model.quantization,
            fileSizeBytes = model.fileSizeBytes,
            requiredRamMb = model.requiredRamMb,
            isInstalled = model.isInstalled,
            localFilePath = model.localFilePath,
            downloadUrl = model.downloadUrl,
            license = model.license,
            isRecommendedForDevice = model.isRecommendedForDevice
        )
    }
}

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val triggerAtMillis: Long,
    val source: String = "Calendar / Local",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): AerisReminder = AerisReminder(
        id = id,
        title = title,
        triggerAtMillis = triggerAtMillis,
        source = source,
        isCompleted = isCompleted,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(item: AerisReminder): ReminderEntity = ReminderEntity(
            id = item.id,
            title = item.title,
            triggerAtMillis = item.triggerAtMillis,
            source = item.source,
            isCompleted = item.isCompleted,
            createdAt = item.createdAt
        )
    }
}

@Entity(tableName = "user_feedback")
data class UserFeedbackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val messageId: Long? = null,
    val rating: Int = 0,
    val category: String = "Style",
    val ruleText: String,
    val isApproved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): UserFeedbackRecord = UserFeedbackRecord(
        id = id,
        messageId = messageId,
        rating = rating,
        category = category,
        ruleText = ruleText,
        isApproved = isApproved,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(record: UserFeedbackRecord): UserFeedbackEntity = UserFeedbackEntity(
            id = record.id,
            messageId = record.messageId,
            rating = record.rating,
            category = record.category,
            ruleText = record.ruleText,
            isApproved = record.isApproved,
            createdAt = record.createdAt
        )
    }
}

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val summary: String = "",
    val progressPercent: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): ProjectWorkspace = ProjectWorkspace(
        id = id,
        name = name,
        description = description,
        summary = summary,
        progressPercent = progressPercent,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

@Entity(
    tableName = "project_notes",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class ProjectNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val title: String,
    val content: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): ProjectNote = ProjectNote(
        id = id,
        projectId = projectId,
        title = title,
        content = content,
        updatedAt = updatedAt
    )
}

@Entity(
    tableName = "project_tasks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class ProjectTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val title: String,
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): ProjectTask = ProjectTask(
        id = id,
        projectId = projectId,
        title = title,
        isDone = isDone,
        createdAt = createdAt
    )
}

@Entity(
    tableName = "project_files",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class ProjectFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val fileName: String,
    val uriString: String,
    val previewText: String = "",
    val sizeBytes: Long = 0L,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): ProjectFileRef = ProjectFileRef(
        id = id,
        projectId = projectId,
        fileName = fileName,
        uriString = uriString,
        previewText = previewText,
        sizeBytes = sizeBytes,
        addedAt = addedAt
    )
}

