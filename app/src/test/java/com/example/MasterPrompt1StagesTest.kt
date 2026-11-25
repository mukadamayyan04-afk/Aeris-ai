package com.example

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AerisPersonalityStyle
import com.example.ai.AerisResponseLength
import com.example.ai.AiOperatingMode
import com.example.ai.AiProviderType
import com.example.ai.CredentialVault
import com.example.ai.GeminiFirebaseProviderAdapter
import com.example.ai.LocalLlmEngine
import com.example.ai.LocalModelLifecycleState
import com.example.ai.ModelHealthQuotaManager
import com.example.ai.ModelHealthState
import com.example.ai.ModelHealthStatus
import com.example.ai.ModelManager
import com.example.ai.MultiModelRouter
import com.example.ai.OpenAiCompatibleProviderAdapter
import com.example.ai.PersonalityEngine
import com.example.ai.PromptCreationEngine
import com.example.ai.ProviderConnectionUiStatus
import com.example.ai.ProviderDirectory
import com.example.ai.ProviderEndpointConfig
import com.example.ai.ProviderIntegrationManager
import com.example.ai.ReasoningPlanningEngine
import com.example.ai.UserFeedbackPreferenceEngine
import com.example.data.database.AerisDatabase
import com.example.data.repository.ProjectRepository
import com.example.domain.model.VoiceState
import com.example.ui.MainViewModel
import com.example.ui.chat.ChatScreen
import com.example.ui.components.HolographicAvatar
import com.example.ui.memory.MemoryScreen
import com.example.ui.settings.AiProvidersScreen
import com.example.ui.settings.ApiKeyEntryScreen
import com.example.ui.settings.resolveProviderConnectionUiStatus
import com.example.ui.voice.VoiceAssistantScreen
import com.example.voice.splitIntoSpeechChunks
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
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MasterPrompt1StagesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ==========================
    // STAGE 1: LOCAL GEMMA 3 1B, SHA-256, RAM GUARD & AI OPERATING MODES
    // ==========================

    @Test
    fun stage1_model_manager_recommends_gemma3_1b_q4_and_handles_part_and_sha256(): Unit = runBlocking {
        val modelManager = ModelManager(context)
        val models = modelManager.availableModels.first()
        val gemmaDefault = models.firstOrNull { it.id == ModelManager.DEFAULT_RECOMMENDED_MODEL_ID }
        assertNotNull(gemmaDefault)
        assertTrue(gemmaDefault!!.isDefaultRecommended)
        assertEquals(1500, gemmaDefault.requiredRamMb)
        assertTrue(modelManager.canOfferFirstTimeSetup(alreadyPrompted = false))
        assertFalse(modelManager.canOfferFirstTimeSetup(alreadyPrompted = true))

        val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
        val partFile = File(modelsDir, "${ModelManager.DEFAULT_RECOMMENDED_MODEL_ID}.gguf.part")
        partFile.writeText("partial-gguf-bytes")
        modelManager.refreshModelCatalog()
        assertEquals(
            LocalModelLifecycleState.PAUSED,
            modelManager.getLifecycleState(ModelManager.DEFAULT_RECOMMENDED_MODEL_ID)
        )
        modelManager.cancelDownload(ModelManager.DEFAULT_RECOMMENDED_MODEL_ID)
        assertEquals(
            LocalModelLifecycleState.NOT_DOWNLOADED,
            modelManager.getLifecycleState(ModelManager.DEFAULT_RECOMMENDED_MODEL_ID)
        )

        // SHA-256 verification on imported model bytes
        val testName = "sha-check-test.gguf"
        val sampleBytes = "AERIS-GGUF-HEADER-CHECK".toByteArray()
        val importRes = modelManager.registerImportedModel(testName, sampleBytes)
        assertTrue(importRes.isSuccess)
        val importedFile = File(importRes.getOrThrow())
        val expectedHash = modelManager.computeSha256(importedFile)
        assertNotNull(expectedHash)
        assertEquals(64, expectedHash!!.length)
        assertTrue(modelManager.verifySha256(importedFile, expectedHash))
        assertFalse(modelManager.verifySha256(importedFile, "0".repeat(64)))
        importedFile.delete()
    }

    @Test
    fun stage1_local_engine_ram_guard_and_operating_modes_enforced(): Unit = runBlocking {
        val localEngine = LocalLlmEngine()
        val insufficient = localEngine.loadModelWithRamCheck(
            modelPath = "/tmp/gemma-3-1b-it-q4.gguf",
            modelId = "gemma-3-1b-it-q4",
            requiredRamMb = 1500,
            availableRamMb = 900
        )
        assertTrue(insufficient.isFailure)
        assertEquals(LocalModelLifecycleState.ERROR, localEngine.lifecycleState.value)

        val modelFile = File(context.cacheDir, "gemma-3-1b-it-q4-test.gguf").apply {
            writeBytes(byteArrayOf(0x47, 0x47, 0x55, 0x46))
        }
        val sufficientRamButMissingRuntime = localEngine.loadModelWithRamCheck(
            modelPath = modelFile.absolutePath,
            modelId = "gemma-3-1b-it-q4",
            requiredRamMb = 1500,
            availableRamMb = 4096
        )
        // RAM headroom and a placeholder GGUF file must never be reported as a loaded neural model.
        assertTrue(sufficientRamButMissingRuntime.isFailure)
        assertEquals(LocalModelLifecycleState.ERROR, localEngine.lifecycleState.value)
        assertFalse(localEngine.isModelLoaded())
        modelFile.delete()

        // Operating mode routing enforcement
        val manager = ProviderIntegrationManager(context)
        val router = MultiModelRouter(manager)
        val plan = ReasoningPlanningEngine().analyze("Explain Kotlin coroutines")

        val localOnly = router.selectRoute(
            plan = plan,
            freeOnlyMode = true,
            autoSwitchEnabled = true,
            preferredModelId = GeminiFirebaseProviderAdapter.DEFAULT_GEMINI_MODEL,
            networkAvailable = true,
            operatingMode = AiOperatingMode.LOCAL_ONLY
        )
        assertEquals(AiProviderType.ON_DEVICE_CORE, localOnly.selectedModel.provider)
        assertTrue(
            localOnly.blockedModels.any {
                it.contains(GeminiFirebaseProviderAdapter.DEFAULT_GEMINI_MODEL) && it.contains("Local-Only")
            }
        )

        val onlineOfflineBlocked = router.selectRoute(
            plan = plan,
            freeOnlyMode = true,
            autoSwitchEnabled = true,
            preferredModelId = GeminiFirebaseProviderAdapter.DEFAULT_GEMINI_MODEL,
            networkAvailable = false,
            operatingMode = AiOperatingMode.ONLINE_ONLY
        )
        assertTrue(onlineOfflineBlocked.candidateOrder.isEmpty())
        assertTrue(onlineOfflineBlocked.noFreeCloudReason?.contains("Online-Only Mode", ignoreCase = true) == true)

        val hybridNoFallbackOffline = router.selectRoute(
            plan = plan,
            freeOnlyMode = true,
            autoSwitchEnabled = true,
            preferredModelId = GeminiFirebaseProviderAdapter.DEFAULT_GEMINI_MODEL,
            networkAvailable = false,
            operatingMode = AiOperatingMode.HYBRID,
            localFallbackEnabled = false
        )
        assertTrue(hybridNoFallbackOffline.candidateOrder.isEmpty())
        assertTrue(hybridNoFallbackOffline.noFreeCloudReason?.contains("Local Fallback is disabled", ignoreCase = true) == true)
    }

    // ==========================
    // STAGE 2: CLOUDFLARE, MASKED KEYS, HONEST STATUS & KEY ENTRY UI
    // ==========================

    @Test
    fun stage2_cloudflare_url_builder_key_masking_and_honest_connection_status(): Unit = runBlocking {
        val cfUrl = ProviderDirectory.buildCloudflareBaseUrl("abc123account999")
        assertEquals("https://api.cloudflare.com/client/v4/accounts/abc123account999/ai/v1", cfUrl)
        assertEquals("••••••••st99", CredentialVault.maskKey("tok_secret_test99"))
        assertEquals("Not saved", CredentialVault.maskKey(""))

        // Saving a key WITHOUT testing must NEVER show CONNECTED
        val statusSavedOnly = resolveProviderConnectionUiStatus(
            isEnabled = true,
            providerType = AiProviderType.CLOUDFLARE_WORKERS_AI,
            hasSavedKey = true,
            hasEndpointUrl = true,
            health = ModelHealthState(
                modelId = ProviderIntegrationManager.CLOUDFLARE_DESCRIPTOR_ID,
                status = ModelHealthStatus.AVAILABLE
            )
        )
        assertEquals(ProviderConnectionUiStatus.NOT_CONNECTED, statusSavedOnly)

        val statusDisabled = resolveProviderConnectionUiStatus(
            isEnabled = false,
            providerType = AiProviderType.GROQ,
            hasSavedKey = true,
            hasEndpointUrl = true,
            health = ModelHealthState("groq", status = ModelHealthStatus.CONNECTED)
        )
        assertEquals(ProviderConnectionUiStatus.DISABLED, statusDisabled)

        // Cloudflare SSE via OpenAiCompatibleProviderAdapter
        val sse = """
            data: {"choices":[{"delta":{"content":"Cloudflare "}}]}

            data: {"choices":[{"delta":{"content":"Ready"}}]}

            data: [DONE]
        """.trimIndent()
        val mockClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val req = chain.request()
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(sse.toResponseBody("text/event-stream".toMediaType()))
                    .build()
            })
            .build()
        val healthMgr = ModelHealthQuotaManager()
        val adapter = OpenAiCompatibleProviderAdapter(healthMgr, mockClient)
        val prompt = PromptCreationEngine(PersonalityEngine(), UserFeedbackPreferenceEngine()).build(
            userPrompt = "Ping",
            plan = ReasoningPlanningEngine().analyze("Ping"),
            history = emptyList(),
            relevantMemories = emptyList(),
            continuity = null,
            approvedFeedback = emptyList()
        )
        val out = adapter.streamChat(
            descriptorId = ProviderIntegrationManager.CLOUDFLARE_DESCRIPTOR_ID,
            config = ProviderEndpointConfig(
                providerType = AiProviderType.CLOUDFLARE_WORKERS_AI,
                baseUrl = cfUrl,
                modelId = "@cf/meta/llama-3.1-8b-instruct",
                bearerToken = "test-cloudflare-token"
            ),
            builtPrompt = prompt
        ).toList().joinToString("")
        assertEquals("Cloudflare Ready", out)
        assertEquals(ModelHealthStatus.CONNECTED, healthMgr.getHealth(ProviderIntegrationManager.CLOUDFLARE_DESCRIPTOR_ID).status)
    }

    @Test
    fun stage2_compose_api_key_entry_never_prefills_plaintext_saved_key() {
        val vm = MainViewModel(context)
        val secretToken = "test-groq-key-9876"
        vm.saveEndpointConfig(
            providerType = AiProviderType.GROQ,
            baseUrl = AiProviderType.GROQ.defaultBaseUrl,
            modelId = "llama-3.1-8b-instant",
            token = secretToken
        )

        composeRule.setContent {
            ApiKeyEntryScreen(
                viewModel = vm,
                providerType = AiProviderType.GROQ,
                onBack = {}
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("api_key_entry_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("api_key_masked_preview").assertTextContains("••••9876", substring = true)
        // Plaintext secret token must NEVER appear in any rendered node
        assertTrue(composeRule.onAllNodesWithText(secretToken, substring = true).fetchSemanticsNodes().isEmpty())

        // Clean up saved key
        vm.removeProviderKey(AiProviderType.GROQ)
    }

    @Test
    fun stage2_compose_ai_providers_screen_shows_modes_and_not_connected_until_tested() {
        val vm = MainViewModel(context)
        vm.saveEndpointConfig(
            providerType = AiProviderType.OPENROUTER,
            baseUrl = AiProviderType.OPENROUTER.defaultBaseUrl,
            modelId = "meta-llama/llama-3.3-70b-instruct:free",
            token = "test-openrouter-key-5566"
        )
        composeRule.setContent {
            AiProvidersScreen(
                viewModel = vm,
                onBack = {},
                onOpenApiKeyEntry = {},
                onNavigateToLocalModels = {}
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("ai_providers_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_mode_hybrid").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_mode_online_only").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_mode_local_only").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_providers_list")
            .performScrollToNode(hasTestTag("provider_card_openrouter"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("provider_status_openrouter")
            .assertTextContains("Not Connected (Key Saved)", substring = true)

        vm.removeProviderKey(AiProviderType.OPENROUTER)
    }

    // ==========================
    // STAGE 3: ROOM MIGRATION 3->4, PROJECT WORKSPACES & MEMORY CLEAR ALL
    // ==========================

    @Test
    fun stage3_room_migration_3_to_4_preserves_data_and_computes_project_progress(): Unit = runBlocking {
        val dbName = "migration_3_4_test_${System.nanoTime()}.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `conversations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `summary` TEXT NOT NULL DEFAULT '', `projectContext` TEXT NOT NULL DEFAULT '', `activeModelId` TEXT)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `chat_messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `conversationId` INTEGER NOT NULL, `sender` TEXT NOT NULL, `text` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `modelId` TEXT, `isError` INTEGER NOT NULL DEFAULT 0, `attachmentName` TEXT, `feedbackRating` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`conversationId`) REFERENCES `conversations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_conversationId` ON `chat_messages` (`conversationId`)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `memories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `key` TEXT NOT NULL, `content` TEXT NOT NULL, `category` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `isUserConfirmed` INTEGER NOT NULL, `projectTag` TEXT NOT NULL DEFAULT '', `isSensitive` INTEGER NOT NULL DEFAULT 0)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `voice_tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `conversationId` INTEGER, `transcript` TEXT NOT NULL, `responsePreview` TEXT NOT NULL, `sttEngine` TEXT NOT NULL, `ttsEngine` TEXT NOT NULL, `voiceId` TEXT NOT NULL, `languageTag` TEXT NOT NULL, `status` TEXT NOT NULL, `errorMessage` TEXT, `durationMs` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `model_metadata` (`modelId` TEXT NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `parameterSize` TEXT NOT NULL, `quantization` TEXT NOT NULL, `fileSizeBytes` INTEGER NOT NULL, `requiredRamMb` INTEGER NOT NULL, `isInstalled` INTEGER NOT NULL, `localFilePath` TEXT, `downloadUrl` TEXT NOT NULL, `license` TEXT NOT NULL, `isRecommendedForDevice` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`modelId`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `reminders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `triggerAtMillis` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, `source` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `user_feedback` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `messageId` INTEGER, `rating` INTEGER NOT NULL, `category` TEXT NOT NULL, `ruleText` TEXT NOT NULL, `isApproved` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")

                    db.execSQL("INSERT INTO conversations (id, title, createdAt, updatedAt, summary, projectContext, activeModelId) VALUES (10, 'Legacy v3 Chat', 1000, 2000, 'Kept cleanly', 'Core', 'gemini-3.8-flash')")
                    db.execSQL("INSERT INTO chat_messages (id, conversationId, sender, text, timestamp, modelId, isError) VALUES (10, 10, 'USER', 'Preserved message', 1500, NULL, 0)")
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
        assertEquals("Legacy v3 Chat", convs.first().title)
        assertEquals(null, convs.first().projectId)

        val repo = ProjectRepository(roomDb.projectDao())
        val projectId = repo.createProject("AERIS Android Sprint", "Stage 3 verification")
        val task1 = repo.addTask(projectId, "Verify Room migration")
        val task2 = repo.addTask(projectId, "Build APK")
        repo.addNote(projectId, "Spec Note", "Keep all data local")
        repo.addFile(projectId, "readme.txt", "content://mock/readme.txt", "Hello", 5L)

        assertEquals(0, repo.getProjectById(projectId)?.progressPercent)
        repo.toggleTask(task1, true, projectId)
        assertEquals(50, repo.getProjectById(projectId)?.progressPercent)
        repo.toggleTask(task2, true, projectId)
        assertEquals(100, repo.getProjectById(projectId)?.progressPercent)

        repo.deleteProject(projectId)
        assertTrue(repo.getNotesForProject(projectId).first().isEmpty())
        assertTrue(repo.getTasksForProject(projectId).first().isEmpty())
        assertTrue(repo.getFilesForProject(projectId).first().isEmpty())

        roomDb.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun stage3_compose_memory_screen_shows_projects_button_and_clear_all_confirmation() {
        runBlocking {
            AerisDatabase.getInstance(context).memoryDao().insert(
                com.example.data.database.entities.MemoryEntity(
                    key = "Device",
                    content = "Redmi 13 5G",
                    category = "Personal Info"
                )
            )
        }
        val vm = MainViewModel(context)
        composeRule.setContent {
            MemoryScreen(viewModel = vm, onNavigateToProjects = {})
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("open_project_workspaces_button").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("clear_all_memories_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("clear_all_memories_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_clear_all_memories_button").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("clear_all_memories_button").fetchSemanticsNodes().isEmpty()
        }
    }

    // ==========================
    // STAGE 4: HOLOGRAM REDUCE MOTION, TTS CHUNKING, PERSONALITY & EDIT-BEFORE-SEND
    // ==========================

    @Test
    fun stage4_tts_chunk_splitter_and_personality_engine_work_accurately() {
        val longText = "Hello Sir ji. AERIS is analyzing your Kotlin project structure now! " +
            "We have completed Stages 1 to 4 cleanly? " +
            "Let us verify chunking limits carefully across sentences so pause and resume remain responsive."
        val chunks = splitIntoSpeechChunks(longText, maxChunkChars = 90)
        assertTrue(chunks.size >= 2)
        assertTrue(chunks.all { it.length <= 90 })
        assertTrue(chunks.joinToString(" ").contains("AERIS is analyzing"))

        val personality = PersonalityEngine()
        val directive = personality.buildIdentityDirectives(
            userName = "Sir ji",
            style = AerisPersonalityStyle.CONCISE_TECHNICAL,
            responseLength = AerisResponseLength.CONCISE
        )
        assertTrue(directive.contains("Concise & Technical"))
        assertTrue(directive.contains("2-5 clear sentences"))
        assertTrue(directive.contains("NEVER output Devanagari"))
        assertTrue(directive.contains("Never claim human consciousness"))
    }

    @Test
    fun stage4_compose_hologram_static_mode_and_voice_assistant_controls_and_chat_voice_entry() {
        composeRule.setContent {
            HolographicAvatar(
                voiceState = VoiceState.THINKING,
                rmsLevel = 4.0f,
                animationsEnabled = false
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("AERIS AI Holographic Avatar (${VoiceState.THINKING.label})").assertIsDisplayed()
    }

    @Test
    fun stage4_compose_voice_assistant_edit_before_send_and_pause_button() {
        val vm = MainViewModel(context)
        composeRule.setContent {
            VoiceAssistantScreen(
                viewModel = vm,
                onBack = {}
            )
        }
        composeRule.waitForIdle()
        // Mic privacy pill must stay hidden when mic is IDLE (never fake mic active)
        composeRule.onNodeWithTag("voice_privacy_pill").assertDoesNotExist()
        composeRule.onNodeWithTag("voice_edit_before_send_toggle")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("voice_editable_transcript_input")
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput("Check battery status")
        composeRule.onNodeWithTag("voice_send_transcript_button")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("action_pause_resume_speech")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun stage4_compose_chat_screen_displays_voice_mode_entry_button() {
        val vm = MainViewModel(context)
        composeRule.setContent {
            ChatScreen(viewModel = vm)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chat_voice_mode_button").assertIsDisplayed()
    }
}
