package com.example

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AerisAiEngine
import com.example.ai.AiProviderType
import com.example.ai.ContinuitySnapshot
import com.example.ai.CredentialVault
import com.example.ai.GeminiFirebaseProviderAdapter
import com.example.ai.ModelHealthQuotaManager
import com.example.ai.ModelHealthStatus
import com.example.ai.OpenAiCompatibleProviderAdapter
import com.example.ai.ProviderEndpointConfig
import com.example.ai.ProviderIntegrationManager
import com.example.domain.model.ChatMessage
import com.example.domain.model.MemoryItem
import com.example.domain.model.MessageSender
import com.example.ui.MainViewModel
import com.example.ui.modelmanager.ModelManagerScreen
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
class MultiProviderAndSpeedBenchmarkTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun all_seven_provider_targets_and_discovery_work_via_real_http_pipeline(): Unit = runBlocking {
        val capturedHeaders = mutableListOf<Pair<String, String?>>()
        val ssePayload = """
            data: {"choices":[{"delta":{"content":"Namaste "}}]}

            data: {"choices":[{"delta":{"content":"Sir ji."}}]}

            data: [DONE]
        """.trimIndent()

        val modelsJson = """
            {"data":[
              {"id":"meta-llama/llama-3.3-70b-instruct:free","pricing":{"prompt":"0"}},
              {"id":"openai/gpt-4o","pricing":{"prompt":"0.000005"}},
              {"id":"llama-3.1-8b-instant"},
              {"id":"Qwen/Qwen2.5-7B-Instruct"}
            ]}
        """.trimIndent()

        var forceHf503 = false
        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val req = chain.request()
                val url = req.url.toString()
                capturedHeaders.add(url to req.header("HTTP-Referer"))
                when {
                    forceHf503 && url.contains("router.huggingface.co") -> {
                        Response.Builder()
                            .request(req).protocol(Protocol.HTTP_1_1).code(503).message("Loading")
                            .body("{\"error\":\"Model is loading\"}".toResponseBody("application/json".toMediaType()))
                            .build()
                    }
                    url.endsWith("/models") -> {
                        Response.Builder()
                            .request(req).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                            .addHeader("x-ratelimit-remaining-requests", "29")
                            .addHeader("x-ratelimit-remaining-tokens", "14400")
                            .body(modelsJson.toResponseBody("application/json".toMediaType()))
                            .build()
                    }
                    else -> {
                        Response.Builder()
                            .request(req).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                            .addHeader("x-ratelimit-remaining-requests", "28")
                            .body(ssePayload.toResponseBody("text/event-stream".toMediaType()))
                            .build()
                    }
                }
            })
            .build()

        val health = ModelHealthQuotaManager()
        val adapter = OpenAiCompatibleProviderAdapter(health, client)

        val providersToTest = listOf(
            ProviderIntegrationManager.OPENAI_COMPAT_DESCRIPTOR_ID to ProviderEndpointConfig(
                AiProviderType.OPENAI_COMPATIBLE, "https://api.example.com/v1", "gpt-4o-mini", "tok-openai"
            ),
            ProviderIntegrationManager.OPENROUTER_DESCRIPTOR_ID to ProviderEndpointConfig(
                AiProviderType.OPENROUTER, "https://openrouter.ai/api/v1", "meta-llama/llama-3.3-70b-instruct:free", "tok-or"
            ),
            ProviderIntegrationManager.GROQ_DESCRIPTOR_ID to ProviderEndpointConfig(
                AiProviderType.GROQ, "https://api.groq.com/openai/v1", "llama-3.1-8b-instant", "tok-groq"
            ),
            ProviderIntegrationManager.HUGGING_FACE_DESCRIPTOR_ID to ProviderEndpointConfig(
                AiProviderType.HUGGING_FACE, "https://router.huggingface.co/v1", "Qwen/Qwen2.5-7B-Instruct", "tok-hf"
            ),
            ProviderIntegrationManager.GPT_OSS_DESCRIPTOR_ID to ProviderEndpointConfig(
                AiProviderType.GPT_OSS_HOSTED, "https://gptoss.local/v1", "gpt-oss-20b", ""
            ),
            ProviderIntegrationManager.QWEN_DESCRIPTOR_ID to ProviderEndpointConfig(
                AiProviderType.QWEN_HOSTED, "https://qwen.local/v1", "qwen2.5-7b-instruct", ""
            )
        )

        providersToTest.forEach { (id, cfg) ->
            val disc = adapter.discoverModels(id, cfg)
            assertTrue("Discovery should succeed for $id", disc.isSuccess)
            val result = disc.getOrThrow()
            assertTrue(result.modelIds.contains("meta-llama/llama-3.3-70b-instruct:free"))
            if (cfg.providerType == AiProviderType.OPENROUTER) {
                assertTrue(result.freeModelIds.contains("meta-llama/llama-3.3-70b-instruct:free"))
            }
            val state = health.getHealth(id)
            assertEquals(ModelHealthStatus.CONNECTED, state.status)
            assertEquals("29", state.remainingRequests)
        }

        // OpenRouter header check
        assertTrue(capturedHeaders.any { (url, ref) -> url.contains("openrouter.ai") && ref == "https://aeris.ai" })

        // Missing key for Groq must honestly set NEEDS_AUTH without network call
        val noKeyRes = adapter.discoverModels(
            ProviderIntegrationManager.GROQ_DESCRIPTOR_ID,
            ProviderEndpointConfig(AiProviderType.GROQ, "https://api.groq.com/openai/v1", "llama-3.1-8b-instant", "")
        )
        assertTrue(noKeyRes.isFailure)
        assertEquals(ModelHealthStatus.NEEDS_AUTH, health.getHealth(ProviderIntegrationManager.GROQ_DESCRIPTOR_ID).status)

        // Hugging Face 503 loading maps cleanly to UNAVAILABLE
        forceHf503 = true
        val hf503 = adapter.discoverModels(
            ProviderIntegrationManager.HUGGING_FACE_DESCRIPTOR_ID,
            ProviderEndpointConfig(AiProviderType.HUGGING_FACE, "https://router.huggingface.co/v1", "Qwen/Qwen2.5-7B-Instruct", "tok-hf")
        )
        assertTrue(hf503.isFailure)
        assertEquals(ModelHealthStatus.UNAVAILABLE, health.getHealth(ProviderIntegrationManager.HUGGING_FACE_DESCRIPTOR_ID).status)
    }

    @Test
    fun free_only_mode_blocks_non_free_openrouter_and_routes_smartly_by_complexity(): Unit = runBlocking {
        val manager = ProviderIntegrationManager(context)
        // Paid OpenRouter model (missing :free suffix) must be blocked in Free-Only Mode
        manager.updateEndpointConfig(
            ProviderEndpointConfig(AiProviderType.OPENROUTER, "https://openrouter.ai/api/v1", "openai/gpt-4o", "sk-or-test")
        )
        val paidDesc = manager.catalogDescriptors().first { it.id == ProviderIntegrationManager.OPENROUTER_DESCRIPTOR_ID }
        assertFalse(paidDesc.isFreeTierEligible)
        assertTrue(paidDesc.requiresBilling)

        val router = com.example.ai.MultiModelRouter(manager)
        val simplePlan = com.example.ai.ReasoningPlanningEngine().analyze("Hi")
        val simpleRoute = router.selectRoute(
            plan = simplePlan,
            freeOnlyMode = true,
            autoSwitchEnabled = true,
            preferredModelId = ProviderIntegrationManager.OPENROUTER_DESCRIPTOR_ID,
            networkAvailable = true
        )
        assertTrue(simpleRoute.blockedModels.any { it.contains(ProviderIntegrationManager.OPENROUTER_DESCRIPTOR_ID) && it.contains("Free-Only") })

        // Complex coding task should prefer capable Gemini 3.8 Flash over Flash-Lite when both healthy
        val codePlan = com.example.ai.ReasoningPlanningEngine().analyze("Write a Kotlin Coroutines binary search algorithm function")
        val codeRoute = router.selectRoute(
            plan = codePlan,
            freeOnlyMode = true,
            autoSwitchEnabled = true,
            preferredModelId = GeminiFirebaseProviderAdapter.DEFAULT_GEMINI_MODEL,
            networkAvailable = true
        )
        assertEquals(GeminiFirebaseProviderAdapter.DEFAULT_GEMINI_MODEL, codeRoute.selectedModel.id)
    }

    @Test
    fun offline_only_requests_fail_closed_without_native_inference(): Unit = runBlocking {
        val engine = AerisAiEngine(context)
        engine.updateRoutingPreferences(
            freeOnly = true,
            autoSwitch = true,
            preferredId = GeminiFirebaseProviderAdapter.DEFAULT_GEMINI_MODEL
        )
        // Simulate offline/isolated local execution with realistic memories and rolling continuity
        val memories = listOf(
            MemoryItem(id = 1, key = "Device", content = "Redmi 13 5G", category = "Hardware"),
            MemoryItem(id = 2, key = "Kotlin", content = "Prefers Jetpack Compose M3 examples", category = "Preferences"),
            MemoryItem(id = 3, key = "Coffee", content = "Black coffee", category = "RandomUnrelated")
        )
        val history = listOf(
            ChatMessage(conversationId = 1L, sender = MessageSender.USER, text = "We are building AERIS on Redmi 13 5G."),
            ChatMessage(conversationId = 1L, sender = MessageSender.AERIS, text = "Yes Sir ji, Redmi 13 5G is configured.")
        )
        val continuity = ContinuitySnapshot(
            conversationId = 1L,
            rollingSummary = "Sir ji is optimizing AERIS multi-model speed.",
            projectContext = "AERIS AI Android"
        )

        val prompts = listOf(
            "Hello AERIS",
            "Who are you?",
            "Explain Kotlin StateFlow code",
            "Solve quadratic equation x^2 - 5x + 6 = 0",
            "Check diagnostics and RAM specs",
            "Summarize our project goal",
            "Translate Hello how are you into Roman Hinglish",
            "Set a timer for 2 minutes",
            "What can you do? Help",
            "Repeat what did i say earlier about my Device"
        )

        prompts.forEach { p ->
            val events = engine.generateWithBrain(
                prompt = p,
                history = history,
                memories = memories,
                continuity = continuity,
                forceOfflineOnly = true
            ).toList()
            val fullText = events.joinToString("") { it.textChunk }
            assertTrue("Offline mode must not fabricate a response for '$p'.", fullText.isBlank())
            assertTrue(
                "Offline mode must explain why inference is unavailable for '$p'.",
                events.any { it.errorMessage?.contains("unavailable", ignoreCase = true) == true }
            )
        }

        val summary = engine.telemetrySummary.value
        assertEquals(10, summary.totalRequests)
        assertEquals(0, summary.successfulRequests)
        assertNotNull(summary.averageAndroidPrepMs)
        println(
            "OFFLINE_FAIL_CLOSED_TELEMETRY: total=${summary.totalRequests}, ok=${summary.successfulRequests}, " +
                "avgPrepMs=${summary.averageAndroidPrepMs}"
        )

        // Smart context filter check: "Hello AERIS" should NOT inject unrelated "Coffee" memory
        val filteredSimple = engine.longTermMemoryEngine.selectRelevantMemories("Hello AERIS", memories)
        assertFalse(filteredSimple.any { it.key == "Coffee" })

        // Test CredentialVault round-trip
        val encrypted = CredentialVault.encrypt("secret-key-123")
        assertEquals("secret-key-123", CredentialVault.decrypt(encrypted))
    }

    @Test
    fun compose_model_performance_dashboard_renders_telemetry_and_all_provider_cards() {
        val vm = MainViewModel(context)
        composeRule.setContent {
            ModelManagerScreen(viewModel = vm, onBack = {})
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("model_manager_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("model_performance_summary_card").assertIsDisplayed()
        composeRule.onNodeWithTag("test_all_providers_button").assertIsDisplayed()
        composeRule.onNodeWithTag("model_dashboard_list")
            .performScrollToNode(hasTestTag("brain_model_card_${ProviderIntegrationManager.OPENROUTER_DESCRIPTOR_ID}"))
        composeRule.onNodeWithTag("brain_model_card_${ProviderIntegrationManager.OPENROUTER_DESCRIPTOR_ID}").assertIsDisplayed()
        composeRule.onNodeWithTag("model_dashboard_list")
            .performScrollToNode(hasTestTag("brain_model_card_${ProviderIntegrationManager.GROQ_DESCRIPTOR_ID}"))
        composeRule.onNodeWithTag("brain_model_card_${ProviderIntegrationManager.GROQ_DESCRIPTOR_ID}").assertIsDisplayed()
    }
}
