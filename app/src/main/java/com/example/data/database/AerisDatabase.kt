package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

@Database(
    entities = [
        ConversationEntity::class,
        ChatMessageEntity::class,
        MemoryEntity::class,
        VoiceTaskEntity::class,
        LocalModelEntity::class,
        ReminderEntity::class,
        UserFeedbackEntity::class,
        ProjectEntity::class,
        ProjectNoteEntity::class,
        ProjectTaskEntity::class,
        ProjectFileEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AerisDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun voiceTaskDao(): VoiceTaskDao
    abstract fun localModelDao(): LocalModelDao
    abstract fun reminderDao(): ReminderDao
    abstract fun userFeedbackDao(): UserFeedbackDao
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AerisDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `voice_tasks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `conversationId` INTEGER,
                        `transcript` TEXT NOT NULL,
                        `responsePreview` TEXT NOT NULL,
                        `sttEngine` TEXT NOT NULL,
                        `ttsEngine` TEXT NOT NULL,
                        `voiceId` TEXT NOT NULL,
                        `languageTag` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `errorMessage` TEXT,
                        `durationMs` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `model_metadata` (
                        `modelId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `parameterSize` TEXT NOT NULL,
                        `quantization` TEXT NOT NULL,
                        `fileSizeBytes` INTEGER NOT NULL,
                        `requiredRamMb` INTEGER NOT NULL,
                        `isInstalled` INTEGER NOT NULL,
                        `localFilePath` TEXT,
                        `downloadUrl` TEXT NOT NULL,
                        `license` TEXT NOT NULL,
                        `isRecommendedForDevice` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`modelId`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Safe non-destructive migration from v2 to v3.
         * Adds continuity metadata, message feedback/attachment fields, memory tags, reminders, and feedback tables.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `conversations` ADD COLUMN `summary` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `conversations` ADD COLUMN `projectContext` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `conversations` ADD COLUMN `activeModelId` TEXT")

                db.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `modelId` TEXT")
                db.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `isError` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `attachmentName` TEXT")
                db.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `feedbackRating` INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE `memories` ADD COLUMN `projectTag` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `memories` ADD COLUMN `isSensitive` INTEGER NOT NULL DEFAULT 0")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reminders` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `triggerAtMillis` INTEGER NOT NULL,
                        `source` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_feedback` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `messageId` INTEGER,
                        `rating` INTEGER NOT NULL,
                        `category` TEXT NOT NULL,
                        `ruleText` TEXT NOT NULL,
                        `isApproved` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Safe non-destructive migration from v3 to v4.
         * Adds Project Workspaces (projects, project_notes, project_tasks, project_files)
         * and optional projectId linkage on conversations.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `conversations` ADD COLUMN `projectId` INTEGER")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `projects` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `summary` TEXT NOT NULL,
                        `progressPercent` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `project_notes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `projectId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_notes_projectId` ON `project_notes` (`projectId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `project_tasks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `projectId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `isDone` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_tasks_projectId` ON `project_tasks` (`projectId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `project_files` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `projectId` INTEGER NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `uriString` TEXT NOT NULL,
                        `previewText` TEXT NOT NULL,
                        `sizeBytes` INTEGER NOT NULL,
                        `addedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_files_projectId` ON `project_files` (`projectId`)")
            }
        }

        fun getInstance(context: Context): AerisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AerisDatabase::class.java,
                    "aeris_ai.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
