package com.example

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AiModelDescriptor
import com.example.ai.AiProviderType
import com.example.ai.AiTaskCategory
import com.example.ai.ConversationContinuityEngine
import com.example.ai.GeminiFirebaseProviderAdapter
import com.example.ai.ModelHealthQuotaManager
import com.example.ai.ModelHealthStatus
import com.example.ai.MultiModelRouter
import com.example.ai.OpenAiCompatibleProviderAdapter
import com.example.ai.PersonalityEngine
import com.example.ai.PromptCreationEngine
import com.example.ai.ProviderEndpointConfig
import com.example.ai.ProviderIntegrationManager
import com.example.ai.ReasoningPlanningEngine
import com.example.ai.ToolTaskExecutionEngine
import com.example.ai.UserFeedbackPreferenceEngine
import com.example.data.database.AerisDatabase
import com.example.device.PermissionStatusInspector
import com.example.domain.model.ActionType
import com.example.domain.model.ChatMessage
import com.example.domain.model.MessageSender
import com.example.domain.model.PermissionStateStatus
import com.example.domain.model.UserFeedbackRecord
import com.example.domain.model.VoiceState
import com.example.notifications.AerisNotificationListener
import com.example.notifications.AnnouncedNotification
import com.example.ui.MainViewModel
import com.example.ui.components.HolographicAvatar
import com.example.ui.studio.ImageStudioScreen
import com.example.ui.voice.VoiceSettingsScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiBrainAndStageIntegrationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun free_only_mode_blocks_argon_by_default_and_allows_when_opted_in() {
        val manager = ProviderIntegrationManager(context)
        val router = MultiModelRouter(manager)
        val plan = ReasoningPlanningEngine().analyze("Write a Kotlin algorithm with dynamic programming")

        val freeDecision = router.selectRoute(
            plan = plan,
            freeOnlyMode = true,
            autoSwitchEnabled = true,
            preferredModelId = GeminiFirebaseProviderAdapter.ARGON_GEMINI_MODEL,
            networkAvailable = true
        )
        assertFalse(freeDecision.selectedModel.id == GeminiFirebaseProviderAdapter.ARGON_GEMINI_MODEL)
        assertTrue(
            freeDecision.blockedModels.any {
                it.contains(GeminiFirebaseProviderAdapter.ARGON_GEMINI_MODEL) && it.contains("Free-Only")
            }
        )

        val optedInDecision = router.selectRoute(
            plan = plan,
            freeOnlyMode = false,
            autoSwitchEnabled = true,
            preferredModelId = GeminiFirebaseProviderAdapter.ARGON_GEMINI_MODEL,
            networkAvailable = true
        )
        assertEquals(GeminiFirebaseProviderAdapter.ARGON_GEMINI_MODEL, optedInDecision.selectedModel.id)
    }

    @Test
    fun multi_model_router_never_activates_unconfigured_endpoints_and_honors_lock_and_failover() {
        val manager = ProviderIntegrationManager(context)
        val router = MultiModelRouter(manager)
        val plan = ReasoningPlanningEngine().analyze("Summarize today's study notes")

        // Unconfigured external endpoints must be blocked
        val initialDecision = router.selectRoute(
            plan = plan,
            freeOnlyMode = true,
            autoSwitchEnabled = true,
            preferredModelId = ProviderIntegrationManager.OPENAI_COMPAT_DESCRIPTOR_ID,
            networkAvailable = true
        )
        assertFalse(initialDecision.selectedModel.id == ProviderIntegrationManager.OPENAI_COMPAT_DESCRIPTOR_ID)
        assertTrue(
            initialDecision.blockedModels.any {
                it.contains(ProviderIntegrationManager.OPENAI_COMPAT_DESCRIPTOR_ID) && it.contains("Not Configured")
            }
        )

        // Manual lock (autoSwitchEnabled = false) keeps preferred free-tier Gemini even for non-primary strengths
        val lockedDecision = router.selectRoute(
            plan = plan,
            freeOnlyMode = true,
            autoSwitchEnabled = false,
            preferredModelId = GeminiFirebaseProviderAdapter.LITE_GEMINI_MODEL,
            networkAvailable = true
        )
        assertEquals(GeminiFirebaseProviderAdapter.LITE_GEMINI_MODEL, lockedDecision.selectedModel.id)

        // Quota exhaustion on DEFAULT_GEMINI_MODEL routes to fallback cleanly when autoSwitchEnabled = true
        manager.healthManager.markFailure(
            GeminiFirebaseProviderAdapter.DEFAULT_GEMINI_MODEL,
            ModelHealthStatus.QUOTA_EXCEEDED,
            "HTTP 429 quota reached"
        )
        val failoverDecision = router.selectRoute(
            plan = plan,
            freeOnlyMode = true,
            autoSwitchEnabled = true,
            preferredModelId = GeminiFirebaseProviderAdapter.DEFAULT_GEMINI_MODEL,
            previousModelId = GeminiFirebaseProviderAdapter.DEFAULT_GEMINI_MODEL,
            networkAvailable = true
        )
        assertFalse(failoverDecision.selectedModel.id == GeminiFirebaseProviderAdapter.DEFAULT_GEMINI_MODEL)
        assertEquals(GeminiFirebaseProviderAdapter.DEFAULT_GEMINI_MODEL, failoverDecision.switchedFromModelId)
        assertNotNull(failoverDecision.switchReason)
    }

    @Test
    fun openai_compatible_adapter_streams_sse_and_maps_http_errors(): Unit = runBlocking {
        val healthManager = ModelHealthQuotaManager()
        val sseBody = """
            data: {"choices":[{"delta":{"content":"Hello "}}]}

            data: {"choices":[{"delta":{"content":"Sir ji"}}]}

            data: [DONE]
        """.trimIndent()

        var mode = "SSE_OK"
        val mockClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val request = chain.request()
                when (mode) {
                    "SSE_OK" -> Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(sseBody.toResponseBody("text/event-stream".toMediaType()))
                        .build()
                    "ERR_401" -> Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(401)
                        .message("Unauthorized")
                        .body("invalid token".toResponseBody("application/json".toMediaType()))
                        .build()
                    else -> Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(429)
                        .message("Too Many Requests")
                        .body("rate limit exceeded".toResponseBody("application/json".toMediaType()))
                        .build()
                }
            })
            .build()

        val adapter = OpenAiCompatibleProviderAdapter(healthManager, mockClient)
        val cfg = ProviderEndpointConfig(
            providerType = AiProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://mock.endpoint.local",
            modelId = "qwen-test",
            bearerToken = "test-tok"
        )
        val prompt = PromptCreationEngine(PersonalityEngine(), UserFeedbackPreferenceEngine()).build(
            userPrompt = "Say hi",
            plan = ReasoningPlanningEngine().analyze("Say hi"),
            history = emptyList(),
            relevantMemories = emptyList(),
            continuity = null,
            approvedFeedback = emptyList()
        )

        val chunks = adapter.streamChat("openai-compatible-custom", cfg, prompt).toList()
        assertEquals("Hello Sir ji", chunks.joinToString(""))
        assertEquals(ModelHealthStatus.CONNECTED, healthManager.getHealth("openai-compatible-custom").status)

        mode = "ERR_401"
        val authRes = adapter.testConnection("openai-compatible-custom", cfg)
        assertTrue(authRes.isFailure)
        assertEquals(ModelHealthStatus.NEEDS_AUTH, healthManager.getHealth("openai-compatible-custom").status)
        assertTrue(authRes.exceptionOrNull()!!.message!!.contains("401"))

        mode = "ERR_429"
        val rateRes = adapter.testConnection("openai-compatible-custom", cfg)
        assertTrue(rateRes.isFailure)
        assertEquals(ModelHealthStatus.RATE_LIMITED, healthManager.getHealth("openai-compatible-custom").status)
    }

    @Test
    fun room_migration_2_to_3_preserves_v2_data_and_adds_v3_tables(): Unit = runBlocking {
        val dbName = "migration_2_3_test_${System.nanoTime()}.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `conversations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `chat_messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `conversationId` INTEGER NOT NULL, `sender` TEXT NOT NULL, `text` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, FOREIGN KEY(`conversationId`) REFERENCES `conversations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_conversationId` ON `chat_messages` (`conversationId`)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `memories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `key` TEXT NOT NULL, `content` TEXT NOT NULL, `category` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `isUserConfirmed` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `voice_tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `conversationId` INTEGER, `transcript` TEXT NOT NULL, `responsePreview` TEXT NOT NULL, `sttEngine` TEXT NOT NULL, `ttsEngine` TEXT NOT NULL, `voiceId` TEXT NOT NULL, `languageTag` TEXT NOT NULL, `status` TEXT NOT NULL, `errorMessage` TEXT, `durationMs` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `model_metadata` (`modelId` TEXT NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `parameterSize` TEXT NOT NULL, `quantization` TEXT NOT NULL, `fileSizeBytes` INTEGER NOT NULL, `requiredRamMb` INTEGER NOT NULL, `isInstalled` INTEGER NOT NULL, `localFilePath` TEXT, `downloadUrl` TEXT NOT NULL, `license` TEXT NOT NULL, `isRecommendedForDevice` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`modelId`))")
                    db.execSQL("INSERT INTO conversations (id, title, createdAt, updatedAt) VALUES (5, 'V2 Session', 1000, 2000)")
                    db.execSQL("INSERT INTO chat_messages (id, conversationId, sender, text, timestamp) VALUES (1, 5, 'USER', 'V2 kept safely', 1500)")
                    db.execSQL("INSERT INTO memories (id, key, content, category, createdAt, isUserConfirmed) VALUES (1, 'Device', 'Redmi 13 5G', 'Hardware', 1000, 1)")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        helper.writableDatabase.close()
        helper.close()

        val roomDb = Room.databaseBuilder(context, AerisDatabase::class.java, dbName)
            .addMigrations(
                AerisDatabase.MIGRATION_1_2,
                AerisDatabase.MIGRATION_2_3,
                AerisDatabase.MIGRATION_3_4
            )
            .allowMainThreadQueries()
            .build()

        val convs = roomDb.conversationDao().getAllConversations().first()
        assertEquals(1, convs.size)
        assertEquals("V2 Session", convs.first().title)

        roomDb.conversationDao().updateContinuity(5L, "Summary check", "Android Project", GeminiFirebaseProviderAdapter.DEFAULT_GEMINI_MODEL)
        val updatedConv = roomDb.conversationDao().getConversationById(5L)
        assertEquals("Summary check", updatedConv?.summary)
        assertEquals("Android Project", updatedConv?.projectContext)

        val msgs = roomDb.chatMessageDao().getMessagesForConversation(5L).first()
        assertEquals("V2 kept safely", msgs.first().text)

        val reminderId = roomDb.reminderDao().insert(
            com.example.data.database.entities.ReminderEntity(
                title = "Standup check",
                triggerAtMillis = 999999L,
                source = "Test"
            )
        )
        assertTrue(reminderId > 0)
        assertEquals(1, roomDb.reminderDao().getAllReminders().first().size)

        val fbId = roomDb.userFeedbackDao().insert(
            com.example.data.database.entities.UserFeedbackEntity(
                messageId = 1L,
                rating = 1,
                category = "Style",
                ruleText = "Stay concise",
                isApproved = true
            )
        )
        assertTrue(fbId > 0)
        assertEquals(1, roomDb.userFeedbackDao().getApprovedFeedback().first().size)

        roomDb.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun continuity_and_feedback_engines_format_prompt_context_cleanly() {
        val continuityEngine = ConversationContinuityEngine()
        val history = listOf(
            ChatMessage(conversationId = 1L, sender = MessageSender.USER, text = "Inspect Kotlin build"),
            ChatMessage(conversationId = 1L, sender = MessageSender.AERIS, text = "Checked build.gradle.kts Sir ji.")
        )
        val summary = continuityEngine.summarizeTurns(history)
        assertTrue(summary.contains("Sir ji: Inspect Kotlin build"))
        assertTrue(summary.contains("AERIS: Checked build.gradle.kts"))

        val banner = continuityEngine.buildHandoffBannerText("gemini-3.8-flash", "aeris-local-core")
        assertTrue(banner.contains("gemini-3.8-flash -> aeris-local-core"))

        val fbEngine = UserFeedbackPreferenceEngine()
        val formatted = fbEngine.formatApprovedPreferences(
            listOf(UserFeedbackRecord(id = 1L, rating = 1, category = "Style", ruleText = "Keep code snippets concise", isApproved = true))
        )
        assertTrue(formatted.contains("[Style] Keep code snippets concise"))
    }

    @Test
    fun notification_listener_redacts_sensitive_otp_and_bank_content() {
        val otpNotif = AnnouncedNotification(
            id = "n1",
            packageName = "com.android.mms",
            appName = "Messages",
            title = "Bank Alert",
            text = "Your OTP for INR 5000 UPI transaction is 482910",
            isSensitive = AerisNotificationListener.containsSensitiveContent("Bank Alert", "Your OTP for INR 5000 UPI transaction is 482910")
        )
        assertTrue(otpNotif.isSensitive)

        val redacted = AerisNotificationListener.formatAnnouncementText(otpNotif, hideSensitive = true)
        assertFalse(redacted.contains("482910"))
        assertTrue(redacted.contains("hidden for privacy"))

        val visible = AerisNotificationListener.formatAnnouncementText(otpNotif, hideSensitive = false)
        assertTrue(visible.contains("482910"))
    }

    @Test
    fun permission_inspector_and_tool_engine_classify_honestly() {
        val items = PermissionStatusInspector.inspectAll(context)
        val internet = items.first { it.id == "perm_internet" }
        assertTrue(internet.isInstallTimeOnly)
        assertFalse(internet.isRuntimeRequestable)

        val mic = items.first { it.id == "perm_mic" }
        assertTrue(mic.isRuntimeRequestable)
        assertTrue(mic.status == PermissionStateStatus.NOT_REQUESTED || mic.status == PermissionStateStatus.GRANTED)

        val listener = items.first { it.id == "perm_notif_listener" }
        assertEquals(PermissionStateStatus.RESTRICTED, listener.status)

        val tools = ToolTaskExecutionEngine()
        val timerReq = tools.detectActionRequest("Set a timer for 3 minutes")
        assertEquals(ActionType.TIMER, timerReq?.actionType)
        assertEquals("180s", timerReq?.detail)
        assertFalse(timerReq?.requiresConfirmation ?: true)

        val appReq = tools.detectActionRequest("open YouTube")
        assertEquals(ActionType.OPEN_APP, appReq?.actionType)
        assertTrue(appReq?.requiresConfirmation == true)
    }

    @Test
    fun compose_holographic_avatar_renders_all_8_voice_states_without_crash() {
        val stateHolder = androidx.compose.runtime.mutableStateOf(VoiceState.IDLE)
        composeRule.setContent {
            HolographicAvatar(voiceState = stateHolder.value, rmsLevel = 3.5f)
        }
        VoiceState.entries.forEach { state ->
            composeRule.runOnUiThread { stateHolder.value = state }
            composeRule.waitForIdle()
            composeRule.onNodeWithContentDescription("AERIS AI Holographic Avatar (${state.label})")
                .assertIsDisplayed()
        }
    }

    @Test
    fun compose_voice_settings_shows_aeris_wake_phrases_and_image_studio_shows_free_only_disclosure() {
        val vm = MainViewModel(context)
        composeRule.setContent {
            VoiceSettingsScreen(
                viewModel = vm,
                onBack = {},
                onNavigateToVoiceSelection = {}
            )
        }
        composeRule.waitForIdle()
        assertTrue(composeRule.onAllNodesWithText("Hey AERIS", substring = true).fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("Wake up AERIS", substring = true).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun compose_image_studio_free_only_disclosure_toggles_cleanly() {
        val vm = MainViewModel(context)
        composeRule.setContent {
            ImageStudioScreen(viewModel = vm, onBack = {})
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("image_studio_screen").assertIsDisplayed()
        composeRule.onNodeWithText("Free-Only Mode is ON").assertIsDisplayed()
        composeRule.onNodeWithTag("studio_allow_blaze_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithText("Blaze / Paid Image Models Opted-In").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Blaze / Paid Image Models Opted-In").assertIsDisplayed()
        // Restore Free-Only default
        vm.setFreeOnlyMode(true)
    }
}
