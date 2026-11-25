package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AerisAiEngine
import com.example.data.database.AerisDatabase
import com.example.data.repository.SettingsRepository
import com.example.data.repository.VoiceRepositoryImpl
import com.example.domain.model.AudioSessionMode
import com.example.domain.model.ChatMessage
import com.example.domain.model.MemoryItem
import com.example.domain.model.MessageSender
import com.example.domain.model.ModelMetadata
import com.example.domain.model.VoiceGender
import com.example.domain.model.VoiceTaskRecord
import com.example.domain.model.WakeWordEngineStatus
import com.example.voice.AerisAudioSessionManager
import com.example.voice.AerisWakeWordService
import com.example.voice.AndroidSpeechRecognizerEngine
import com.example.voice.AndroidTtsEngine
import com.example.voice.LocalSpeechModelDetector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoiceBackendRobolectricTest {

    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        File(context.filesDir, "voice_models").deleteRecursively()
    }

    @Test
    fun room_migration_1_to_2_preserves_existing_history_and_adds_voice_tables(): Unit = runBlocking {
        val dbName = "migration_test_${System.nanoTime()}.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        // Create a v1 SQLite database manually with old schema and sample data
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `conversations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `chat_messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `conversationId` INTEGER NOT NULL, `sender` TEXT NOT NULL, `text` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, FOREIGN KEY(`conversationId`) REFERENCES `conversations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_chat_messages_conversationId` ON `chat_messages` (`conversationId`)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `memories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `key` TEXT NOT NULL, `content` TEXT NOT NULL, `category` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `isUserConfirmed` INTEGER NOT NULL)"
                    )
                    db.execSQL("INSERT INTO conversations (id, title, createdAt, updatedAt) VALUES (10, 'Legacy Chat', 1000, 2000)")
                    db.execSQL("INSERT INTO chat_messages (id, conversationId, sender, text, timestamp) VALUES (1, 10, 'USER', 'Preserved message Sir ji', 1500)")
                    db.execSQL("INSERT INTO memories (id, key, content, category, createdAt, isUserConfirmed) VALUES (1, 'Owner', 'Ayyan Mukadam', 'Personal', 1000, 1)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        helper.writableDatabase.close()
        helper.close()

        // Reopen with Room v4 + MIGRATION_1_2, MIGRATION_2_3 & MIGRATION_3_4
        val roomDb = Room.databaseBuilder(context, AerisDatabase::class.java, dbName)
            .addMigrations(
                AerisDatabase.MIGRATION_1_2,
                AerisDatabase.MIGRATION_2_3,
                AerisDatabase.MIGRATION_3_4
            )
            .allowMainThreadQueries()
            .build()

        val preservedConvs = roomDb.conversationDao().getAllConversations().first()
        assertEquals(1, preservedConvs.size)
        assertEquals("Legacy Chat", preservedConvs.first().title)

        val preservedMsgs = roomDb.chatMessageDao().getMessagesForConversation(10L).first()
        assertEquals(1, preservedMsgs.size)
        assertEquals("Preserved message Sir ji", preservedMsgs.first().text)

        val preservedMemories = roomDb.memoryDao().getAllMemories().first()
        assertEquals(1, preservedMemories.size)
        assertEquals("Owner", preservedMemories.first().key)

        // Verify new voice_tasks & model_metadata tables work after migration
        val taskId = roomDb.voiceTaskDao().insert(
            com.example.data.database.entities.VoiceTaskEntity.fromDomain(
                VoiceTaskRecord(
                    conversationId = 10L,
                    transcript = "Open diagnostics",
                    responsePreview = "Checking hardware Sir ji.",
                    sttEngine = "Android SpeechRecognizer",
                    ttsEngine = "Android System TTS",
                    voiceId = "en-us-x-sfg#female_1-local",
                    languageTag = "en-US",
                    status = "SUCCESS",
                    durationMs = 420L
                )
            )
        )
        assertTrue(taskId > 0)
        assertEquals(1, roomDb.voiceTaskDao().getTaskCount().first())

        roomDb.localModelDao().upsertAll(
            listOf(
                com.example.data.database.entities.LocalModelEntity.fromDomain(
                    ModelMetadata(
                        id = "smollm2-135m-q4",
                        name = "SmolLM2 135M",
                        parameterSize = "135M",
                        quantization = "Q4_K_M",
                        fileSizeBytes = 98_000_000L,
                        requiredRamMb = 512,
                        isInstalled = false,
                        localFilePath = null,
                        downloadUrl = "https://example.invalid/model.gguf",
                        license = "Apache 2.0",
                        isRecommendedForDevice = true
                    ),
                    category = "LLM"
                )
            )
        )
        assertEquals(1, roomDb.localModelDao().getAllModels().first().size)

        roomDb.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun voice_repository_persists_all_voice_preferences_in_datastore(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AerisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val settingsRepo = SettingsRepository(context)
        val voiceRepo = VoiceRepositoryImpl(db.voiceTaskDao(), db.localModelDao(), settingsRepo)

        voiceRepo.saveSelectedTtsEngine("com.google.android.tts")
        voiceRepo.saveSelectedVoiceId("en-in-x-enc-local")
        voiceRepo.setRecognitionLanguage("en-IN")
        voiceRepo.setSpeechRate(1.25f)
        voiceRepo.setSpeechPitch(0.95f)
        voiceRepo.setVoiceResponsesEnabled(true)
        voiceRepo.setAutoPlayResponses(true)
        voiceRepo.setVoiceInputEnabled(true)
        voiceRepo.setPushToTalkEnabled(true)
        voiceRepo.setWakeWordEnabled(false)
        voiceRepo.setWakeWordPhrase("AERIS, wake up")
        voiceRepo.setBackgroundListeningEnabled(true)

        assertEquals("com.google.android.tts", voiceRepo.selectedTtsEngineFlow.first())
        assertEquals("en-in-x-enc-local", voiceRepo.selectedVoiceIdFlow.first())
        assertEquals("en-IN", voiceRepo.recognitionLanguageFlow.first())
        assertEquals(1.25f, voiceRepo.speechRateFlow.first(), 0.01f)
        assertEquals(0.95f, voiceRepo.speechPitchFlow.first(), 0.01f)
        assertTrue(voiceRepo.voiceResponsesEnabledFlow.first())
        assertTrue(voiceRepo.autoPlayResponsesFlow.first())
        assertTrue(voiceRepo.voiceInputEnabledFlow.first())
        assertTrue(voiceRepo.pushToTalkEnabledFlow.first())
        assertFalse(voiceRepo.wakeWordEnabledFlow.first())
        assertEquals("AERIS, wake up", voiceRepo.wakeWordPhraseFlow.first())
        assertTrue(voiceRepo.backgroundListeningEnabledFlow.first())

        db.close()
    }

    @Test
    fun audio_session_manager_tracks_modes_and_becoming_noisy_interruption() {
        val manager = AerisAudioSessionManager(context)
        var interruptedReason: String? = null
        manager.setOnAudioInterruptedListener { reason ->
            interruptedReason = reason
        }

        manager.transitionSessionMode(AudioSessionMode.TTS_SPEAKING)
        assertEquals(AudioSessionMode.TTS_SPEAKING, manager.sessionMode.value)

        // Simulate headphone unplug broadcast while speaking
        context.sendBroadcast(Intent(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        assertEquals("Audio output disconnected", interruptedReason)
        manager.release()
    }

    @Test
    fun stt_engine_reports_missing_permission_and_supports_cancel_and_language_change() {
        val audioSession = AerisAudioSessionManager(context)
        val stt = AndroidSpeechRecognizerEngine(context, audioSession)

        stt.setRecognitionLanguage("en-IN")
        assertEquals("en-IN", stt.currentRecognitionLanguage.value)

        // Without RECORD_AUDIO permission, startListening must fail cleanly with explicit error
        stt.startListening(onFinal = {})
        assertFalse(stt.isListening.value)
        assertNotNull(stt.recognitionError.value)
        assertTrue(stt.recognitionError.value!!.contains("Microphone permission"))

        stt.cancelListening()
        assertFalse(stt.isListening.value)
        assertEquals("", stt.liveTranscript.value)

        stt.clearError()
        assertNull(stt.recognitionError.value)

        stt.destroy()
        audioSession.release()
    }

    @Test
    fun tts_engine_gender_classification_never_misclassifies_female_as_male() {
        val tts = AndroidTtsEngine(context)

        assertEquals(
            VoiceGender.FEMALE,
            tts.classifyVoiceGender("en-us-x-sfg#female_1-local", emptySet())
        )
        assertEquals(
            VoiceGender.MALE,
            tts.classifyVoiceGender("en-us-x-iom#male_1-local", emptySet())
        )
        assertEquals(
            VoiceGender.FEMALE,
            tts.classifyVoiceGender("voice-pack-01", setOf("gender=female"))
        )
        assertEquals(
            VoiceGender.UNSPECIFIED,
            tts.classifyVoiceGender("en-us-x-tpf-local", emptySet())
        )

        tts.setSpeechRate(5.0f)
        assertEquals(2.5f, tts.speechRate.value, 0.01f)
        tts.setPitch(0.1f)
        assertEquals(0.5f, tts.speechPitch.value, 0.01f)

        tts.shutdown()
    }

    @Test
    fun local_speech_model_detector_and_wake_word_report_missing_dependency_honestly() {
        val report = LocalSpeechModelDetector.inspect(context)
        assertFalse(report.isSherpaNativeLibPresent)
        assertFalse(report.isSherpaKwsModelPresent)
        assertNotNull(report.missingKwsReason)

        val started = AerisWakeWordService.start(context, "Hey AERIS")
        assertFalse("WakeWordService must not falsely start when mic perm or KWS lib is missing", started)
        assertFalse(AerisWakeWordService.isServiceRunning.value)
        val status = AerisWakeWordService.engineStatus.value
        assertTrue(
            status == WakeWordEngineStatus.PERMISSION_REQUIRED ||
                status == WakeWordEngineStatus.MISSING_DEPENDENCY
        )
    }

    @Test
    fun aeris_ai_engine_uses_history_and_memories_locally(): Unit = runBlocking {
        val engine = AerisAiEngine(context)
        val history = listOf(
            ChatMessage(conversationId = 1L, sender = MessageSender.USER, text = "Check my Redmi 13 specs"),
            ChatMessage(conversationId = 1L, sender = MessageSender.AERIS, text = "Checked Sir ji.")
        )
        val memories = listOf(MemoryItem(id = 1L, key = "Project", content = "AERIS AI"))

        val tokens = engine.generateStream(
            prompt = "explain more about my Project",
            history = history,
            memories = memories
        ).toList()

        val output = tokens.joinToString("")
        assertTrue(output.contains("Sir ji"))
        assertTrue(output.contains("AERIS AI"))
        assertTrue(output.contains("Check my Redmi 13 specs"))
    }
}
