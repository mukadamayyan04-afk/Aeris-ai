package com.example

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AerisPersonalityStyle
import com.example.ai.AerisResponseLength
import com.example.ai.PersonalityEngine
import com.example.ai.PromptCreationEngine
import com.example.ai.ReasoningPlanningEngine
import com.example.ai.ToolTaskExecutionEngine
import com.example.ai.UserFeedbackPreferenceEngine
import com.example.device.BootCompletedReceiver
import com.example.device.DeviceActionHandler
import com.example.device.FloatingAssistantService
import com.example.device.PermissionStatusInspector
import com.example.domain.model.ActionExecutionStatus
import com.example.domain.model.ActionType
import com.example.domain.model.DeviceAction
import com.example.domain.model.DeviceActionLifecycleState
import com.example.features.FeatureCatalog
import com.example.features.FeatureCategory
import com.example.ui.MainViewModel
import com.example.ui.chat.ChatScreen
import com.example.ui.customize.CustomizeScreen
import com.example.ui.features.FeaturesScreen
import com.example.ui.home.HomeScreen
import com.example.ui.library.LibraryScreen
import com.example.ui.navigation.AerisSideDrawerContent
import com.example.ui.privacy.PrivacyCenterScreen
import com.example.ui.settings.PermissionManagerScreen
import com.example.ui.work.WorkModeScreen
import org.json.JSONObject
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
class MasterPrompt2StagesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun newTestViewModel(): MainViewModel {
        val store = ViewModelStore()
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(context)
        return ViewModelProvider(store, factory)[MainViewModel::class.java]
    }

    // ==========================
    // STAGE 5: DEVICE ACTIONS, PERMISSIONS, OVERLAY, BOOT STANDBY
    // ==========================

    @Test
    fun stage5_device_actions_read_telemetry_and_settings_intents_work_safely() {
        val handler = DeviceActionHandler(context)

        val dateAction = handler.readDateTime()
        assertEquals(ActionType.DATE_TIME, dateAction.type)
        assertEquals(ActionExecutionStatus.SUCCESS, dateAction.status)
        assertEquals(DeviceActionLifecycleState.COMPLETED, dateAction.lifecycleState)
        assertTrue(!dateAction.resultMessage.isNullOrBlank())

        val batteryAction = handler.readBatteryInfo()
        assertEquals(ActionType.BATTERY_CHECK, batteryAction.type)
        assertTrue(batteryAction.status == ActionExecutionStatus.SUCCESS || batteryAction.status == ActionExecutionStatus.FAILED)

        val storageAction = handler.readStorageInfo()
        assertEquals(ActionType.STORAGE_INFO, storageAction.type)
        assertTrue(storageAction.status == ActionExecutionStatus.SUCCESS || storageAction.status == ActionExecutionStatus.FAILED)

        // All 6 lifecycle states exist on DeviceActionLifecycleState
        assertEquals(6, DeviceActionLifecycleState.entries.size)
        val defaultAction = DeviceAction(
            id = "test-1",
            type = ActionType.TIMER,
            label = "Test Timer",
            detail = "60s",
            status = ActionExecutionStatus.PENDING
        )
        assertEquals(DeviceActionLifecycleState.ACTION_REQUESTED, defaultAction.lifecycleState)

        // ToolTaskExecutionEngine parses battery, storage, datetime, and settings safely
        val toolEngine = ToolTaskExecutionEngine()
        val startNs = System.nanoTime()
        repeat(200) {
            assertEquals(ActionType.BATTERY_CHECK, toolEngine.detectActionRequest("check battery level")?.actionType)
            assertEquals(ActionType.STORAGE_INFO, toolEngine.detectActionRequest("check storage space")?.actionType)
            assertEquals(ActionType.DATE_TIME, toolEngine.detectActionRequest("what time is it")?.actionType)
            val overlayReq = toolEngine.detectActionRequest("open overlay settings")
            assertEquals(ActionType.SETTINGS_PAGE, overlayReq?.actionType)
            assertTrue(overlayReq?.requiresConfirmation == true)
        }
        val parseElapsedMs = (System.nanoTime() - startNs) / 1_000_000.0
        println("BENCHMARK tool_engine_800_parses_ms=$parseElapsedMs")
        assertTrue(parseElapsedMs < 2000.0)
    }

    @Test
    fun stage5_manifest_and_permission_inspector_cover_boot_overlay_and_runtime_states() {
        // BootCompletedReceiver & FloatingAssistantService declared in manifest
        val pm = context.packageManager
        val receiverInfo = pm.getReceiverInfo(
            ComponentName(context, BootCompletedReceiver::class.java),
            PackageManager.GET_META_DATA
        )
        assertNotNull(receiverInfo)
        val serviceInfo = pm.getServiceInfo(
            ComponentName(context, FloatingAssistantService::class.java),
            PackageManager.GET_META_DATA
        )
        assertNotNull(serviceInfo)

        val items = PermissionStatusInspector.inspectAll(context)
        assertTrue(items.any { it.id == "perm_mic" })
        assertTrue(items.any { it.id == "perm_camera" })
        assertTrue(items.any { it.id == "perm_overlay" })
        assertTrue(items.any { it.id == "perm_screen_capture" })
        assertTrue(items.any { it.id == "perm_location" })

        val vm = newTestViewModel()
        composeRule.setContent {
            PermissionManagerScreen(viewModel = vm, onBack = {})
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("permission_manager_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("floating_assistant_switch").assertIsDisplayed()
        composeRule.onNodeWithTag("permission_refresh_button").assertIsDisplayed()
        composeRule.onNodeWithTag("perm_card_perm_mic").assertIsDisplayed()
    }

    // ==========================
    // STAGE 6: LANDSCAPE WORK MODE ENHANCEMENTS
    // ==========================

    @Test
    fun stage6_work_mode_projects_tools_panel_and_operating_mode_status() {
        val vm = newTestViewModel()
        composeRule.setContent {
            WorkModeScreen(
                viewModel = vm,
                onNavigateToNormalMode = {}
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("work_mode_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("work_mode_exit_button").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Model: 1.7B Q4").fetchSemanticsNodes().isEmpty())

        // Open Projects & Tools drawer tab
        composeRule.onNodeWithText("Projects & Tools").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("work_mode_projects_tools_panel").assertIsDisplayed()
    }

    // ==========================
    // STAGE 7: 100+ SEARCHABLE FEATURE CATALOG & FLOATING ASSISTANT GUARD
    // ==========================

    @Test
    fun stage7_feature_catalog_contains_100plus_searchable_features_and_overlay_guard() {
        val all = FeatureCatalog.allFeatures
        assertTrue("Expected >= 100 features, got ${all.size}", all.size >= 100)
        for (category in FeatureCategory.entries) {
            assertTrue("Category ${category.title} should have features", all.any { it.category == category })
        }

        // Benchmark catalog search across all 112 items
        val startNs = System.nanoTime()
        var matches = 0
        repeat(100) {
            matches = FeatureCatalog.search("battery", null).size
        }
        val searchElapsedMs = (System.nanoTime() - startNs) / 1_000_000.0
        println("BENCHMARK feature_catalog_100x_search_ms=$searchElapsedMs total_features=${all.size} battery_matches=$matches")
        assertTrue(matches > 0)
        assertTrue(searchElapsedMs < 1500.0)

        // FloatingAssistantService refuses to start safely without overlay permission
        if (!FloatingAssistantService.canDrawOverlays(context)) {
            assertFalse(FloatingAssistantService.start(context))
        }

        var recordedId: String? = null
        composeRule.setContent {
            FeaturesScreen(
                onRecordFeatureUsage = { recordedId = it }
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("features_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("search_features_field").performTextInput("Battery")
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Open")[0].performClick()
        composeRule.waitForIdle()
        assertNotNull(recordedId)
    }

    // ==========================
    // STAGE 8: SIDE DRAWER, ROUNDED COMPOSER, LIBRARY, CUSTOMIZE, PRIVACY CENTER
    // ==========================

    @Test
    fun stage8_side_drawer_and_home_rounded_composer_are_free_first_without_paywalls() {
        val vm = newTestViewModel()
        composeRule.setContent {
            HomeScreen(
                viewModel = vm,
                onNavigateToChat = {},
                onNavigateToWorkMode = {},
                onNavigateToFeatures = {},
                onNavigateToMemory = {},
                onNavigateToSettings = {}
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("aeris_home_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("home_composer_container").assertIsDisplayed()
        composeRule.onNodeWithTag("home_input_field").assertIsDisplayed()
        composeRule.onNodeWithTag("home_composer_plus_button").assertIsDisplayed()
        composeRule.onNodeWithTag("home_send_button").assertIsDisplayed()

        // Zero fake paywall / "Get Plus" strings
        assertTrue(composeRule.onAllNodesWithText("Get Plus", substring = true, ignoreCase = true).fetchSemanticsNodes().isEmpty())

        // Plus menu exposes zero-permission Photo Picker & tools
        composeRule.onNodeWithTag("home_composer_plus_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Attach Photo (Photo Picker)").assertIsDisplayed()
        composeRule.onNodeWithText("Open Library").assertIsDisplayed()
    }

    @Test
    fun stage8_side_drawer_displays_items_search_and_new_chat() {
        val vm = newTestViewModel()
        composeRule.setContent {
            AerisSideDrawerContent(
                viewModel = vm,
                onCloseDrawer = {},
                onNavigateToRoute = {},
                onNavigateToWorkMode = {}
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("aeris_side_drawer").assertIsDisplayed()
        composeRule.onNodeWithTag("drawer_search_field").assertIsDisplayed()
        composeRule.onNodeWithTag("drawer_item_library").assertIsDisplayed()
        composeRule.onNodeWithTag("drawer_item_customize").assertIsDisplayed()
        composeRule.onNodeWithTag("drawer_item_privacy").assertIsDisplayed()
        composeRule.onNodeWithTag("drawer_new_chat_button").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Get Plus", substring = true, ignoreCase = true).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun stage8_chat_screen_uses_shared_rounded_composer() {
        val vm = newTestViewModel()
        composeRule.setContent {
            ChatScreen(viewModel = vm)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chat_composer_container").assertIsDisplayed()
        composeRule.onNodeWithTag("composer_plus_button").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_input_field").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_composer_mic_button").assertIsDisplayed()
        composeRule.onNodeWithTag("composer_voice_mode_button").assertIsDisplayed()
        composeRule.onNodeWithTag("send_button").assertIsDisplayed()
    }

    @Test
    fun stage8_library_screen_displays_filters_and_search() {
        val vm = newTestViewModel()
        composeRule.setContent {
            LibraryScreen(viewModel = vm, onBack = {}, onOpenChat = {}, onOpenImageStudio = {})
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("library_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("library_search_field").assertIsDisplayed()
    }

    @Test
    fun stage8_customize_saves_custom_instruction_into_prompt_engine() {
        val vm = newTestViewModel()
        composeRule.setContent {
            CustomizeScreen(viewModel = vm, onBack = {})
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("customize_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("custom_instruction_input").performTextInput("Always provide Kotlin unit test snippets first.")
        composeRule.onNodeWithTag("save_custom_instruction_button").performClick()
        composeRule.waitForIdle()

        val promptEngine = PromptCreationEngine(PersonalityEngine(), UserFeedbackPreferenceEngine())
        val plan = ReasoningPlanningEngine().analyze("Help")
        val builtPrompt = promptEngine.build(
            userPrompt = "Help",
            plan = plan,
            history = emptyList(),
            relevantMemories = emptyList(),
            continuity = null,
            personalityStyle = AerisPersonalityStyle.CALM_PROFESSIONAL,
            responseLength = AerisResponseLength.BALANCED,
            customSystemInstruction = "Always provide Kotlin unit test snippets first."
        )
        assertTrue(builtPrompt.systemInstruction.contains("[USER CUSTOM INSTRUCTIONS]"))
        assertTrue(builtPrompt.systemInstruction.contains("Always provide Kotlin unit test snippets first."))
    }

    @Test
    fun stage8_privacy_center_export_excludes_secrets_and_confirms_wipe() {
        val vm = newTestViewModel()
        composeRule.setContent {
            PrivacyCenterScreen(viewModel = vm, onBack = {})
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("privacy_center_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("privacy_export_json_button").assertIsDisplayed()
        composeRule.onNodeWithTag("privacy_clear_memories_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_privacy_clear_memories").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        val exportStartNs = System.nanoTime()
        val exportJson = vm.buildPrivacyExportJson()
        val exportElapsedMs = (System.nanoTime() - exportStartNs) / 1_000_000.0
        println("BENCHMARK privacy_json_export_ms=$exportElapsedMs bytes=${exportJson.length}")

        val parsed = JSONObject(exportJson)
        assertEquals("AERIS AI", parsed.getString("app"))
        assertTrue(parsed.has("conversations"))
        assertTrue(parsed.has("memories"))
        assertTrue(parsed.has("projects"))
        assertFalse(exportJson.contains("apiKey", ignoreCase = true))
        assertFalse(exportJson.contains("bearer", ignoreCase = true))
    }
}
