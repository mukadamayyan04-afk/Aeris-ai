package com.example

import android.app.Application
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.OperatingMode
import com.example.ui.MainViewModel
import com.example.voice.VoiceModeCommandHandler
import com.example.voice.VoiceModeCommandResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoiceAndModeRepairRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun manifest_declares_adjustResize_and_orientation_config_changes() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val activityInfo = context.packageManager.getActivityInfo(
            ComponentName(context, MainActivity::class.java),
            PackageManager.GET_META_DATA
        )
        assertEquals(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,
            activityInfo.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST
        )
        assertTrue((activityInfo.configChanges and ActivityInfo.CONFIG_ORIENTATION) != 0)
        assertTrue((activityInfo.configChanges and ActivityInfo.CONFIG_SCREEN_SIZE) != 0)
        assertTrue((activityInfo.configChanges and ActivityInfo.CONFIG_SMALLEST_SCREEN_SIZE) != 0)
    }

    @Test
    fun voice_command_handler_parses_all_work_mode_and_normal_mode_commands() {
        val workCommands = listOf(
            "AERIS, enter landscape mode",
            "AERIS, switch to Work Mode",
            "AERIS, activate Command Center",
            "AERIS, landscape mode on"
        )
        for (cmd in workCommands) {
            val result = VoiceModeCommandHandler.parse(cmd, OperatingMode.NORMAL)
            assertTrue("Expected SwitchMode(WORK) for '$cmd'", result is VoiceModeCommandResult.SwitchMode)
            assertEquals(OperatingMode.WORK, (result as VoiceModeCommandResult.SwitchMode).targetMode)
        }

        val normalCommands = listOf(
            "AERIS, exit Work Mode",
            "AERIS, switch to Normal Mode",
            "AERIS, portrait mode on",
            "AERIS, return to Home"
        )
        for (cmd in normalCommands) {
            val result = VoiceModeCommandHandler.parse(cmd, OperatingMode.WORK)
            assertTrue("Expected SwitchMode(NORMAL) for '$cmd'", result is VoiceModeCommandResult.SwitchMode)
            val switch = result as VoiceModeCommandResult.SwitchMode
            assertEquals(OperatingMode.NORMAL, switch.targetMode)
            if (cmd.contains("Home")) {
                assertTrue(switch.returnToHome)
            }
        }
    }

    @Test
    fun voice_command_handler_asks_confirmation_when_ambiguous_and_ignores_unrelated_speech() {
        val ambiguous = listOf(
            "AERIS, switch mode",
            "change orientation",
            "AERIS work mode",
            "landscape or portrait"
        )
        for (cmd in ambiguous) {
            val result = VoiceModeCommandHandler.parse(cmd, OperatingMode.NORMAL)
            assertTrue("Expected Ambiguous for '$cmd' but got $result", result is VoiceModeCommandResult.Ambiguous)
        }

        val unrelated = listOf(
            "Hello AERIS, how are you?",
            "Tell me about work mode and how landscape works",
            "What is the time?",
            "Open camera and check my RAM",
            "Set a timer for two minutes"
        )
        for (speech in unrelated) {
            val result = VoiceModeCommandHandler.parse(speech, OperatingMode.NORMAL)
            assertEquals("Expected NotAModeCommand for '$speech'", VoiceModeCommandResult.NotAModeCommand, result)
        }
    }

    @Test
    fun home_work_mode_button_rotates_to_landscape_and_exit_restores_portrait() {
        composeRule.waitForIdle()

        // Starts in Portrait Normal Mode
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, composeRule.activity.requestedOrientation)

        // Tap Work Mode button on Home Screen (scroll if compact viewport)
        composeRule.onNodeWithTag("home_work_mode_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        // Activity requestedOrientation actually changed to landscape
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            composeRule.activity.requestedOrientation
        )

        // Work Mode HUD displays real controls and does NOT show fake hardcoded "Model: 1.7B Q4"
        composeRule.onNodeWithTag("work_mode_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("work_mode_exit_button").assertIsDisplayed()
        composeRule.onNodeWithTag("work_mode_input_field").assertIsDisplayed()
        composeRule.onNodeWithTag("work_mode_mic_button").assertIsDisplayed()
        composeRule.onNodeWithTag("work_mode_send_button").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Model: 1.7B Q4").fetchSemanticsNodes().isEmpty())

        // Exit Work Mode via visible Normal Mode button restores portrait orientation
        composeRule.onNodeWithTag("work_mode_exit_button").performClick()
        composeRule.waitForIdle()

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, composeRule.activity.requestedOrientation)
        composeRule.onNodeWithTag("aeris_home_screen").assertIsDisplayed()
    }

    @Test
    fun chat_screen_preserves_history_and_voice_commands_switch_modes_cleanly() {
        composeRule.waitForIdle()

        // Open Chat tab
        composeRule.onNodeWithTag("tab_chat").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("chat_screen_root").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_composer_container").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_input_field").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_composer_mic_button").assertIsDisplayed()
        composeRule.onNodeWithTag("send_button").assertIsDisplayed()

        // Send a message in Chat
        composeRule.onNodeWithTag("chat_input_field").performTextInput("Hello AERIS status check")
        composeRule.onNodeWithTag("send_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithText("Hello AERIS status check").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Hello AERIS status check").assertIsDisplayed()

        // Trigger Work Mode via ViewModel voice command processor while on Chat
        val vm = androidx.lifecycle.ViewModelProvider(composeRule.activity)[MainViewModel::class.java]
        composeRule.runOnUiThread {
            val handled = vm.processVoiceTranscriptForModeCommand("AERIS, switch to Work Mode")
            assertTrue(handled)
        }
        composeRule.waitForIdle()

        assertEquals(OperatingMode.WORK, vm.uiState.value.operatingMode)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, composeRule.activity.requestedOrientation)
        composeRule.onNodeWithTag("work_mode_screen").assertIsDisplayed()

        // Ambiguous voice command prompts confirmation instead of switching blindly
        composeRule.runOnUiThread {
            val handledAmbiguous = vm.processVoiceTranscriptForModeCommand("AERIS, change mode")
            assertTrue(handledAmbiguous)
        }
        composeRule.waitForIdle()
        assertNotNull(vm.pendingModeConfirmation.value)
        composeRule.onNodeWithTag("confirm_mode_switch_button").assertIsDisplayed()
        composeRule.onNodeWithTag("cancel_mode_switch_button").performClick()
        composeRule.waitForIdle()
        assertNull(vm.pendingModeConfirmation.value)
        assertEquals(OperatingMode.WORK, vm.uiState.value.operatingMode)

        // Unrelated speech does not trigger mode switch
        composeRule.runOnUiThread {
            val handledUnrelated = vm.processVoiceTranscriptForModeCommand("Tell me about artificial intelligence")
            assertFalse(handledUnrelated)
        }
        assertEquals(OperatingMode.WORK, vm.uiState.value.operatingMode)

        // Exit Work Mode via voice command restores Portrait & preserves chat message
        composeRule.runOnUiThread {
            val handledExit = vm.processVoiceTranscriptForModeCommand("AERIS, exit Work Mode")
            assertTrue(handledExit)
        }
        composeRule.waitForIdle()

        assertEquals(OperatingMode.NORMAL, vm.uiState.value.operatingMode)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, composeRule.activity.requestedOrientation)
        composeRule.onNodeWithTag("chat_screen_root").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithText("Hello AERIS status check").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Hello AERIS status check").assertIsDisplayed()
    }
}
