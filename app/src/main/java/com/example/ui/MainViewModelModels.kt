package com.example.ui

import com.example.ai.ToolTaskExecutionEngine
import com.example.domain.model.DeviceAction
import com.example.domain.model.DeviceDiagnosticsInfo
import com.example.domain.model.OperatingMode
import com.example.domain.model.VoiceState

sealed class NavigationEvent {
    data object EnterWorkMode : NavigationEvent()
    data class ExitWorkMode(val returnToHome: Boolean = false) : NavigationEvent()
}

data class PendingModeConfirmation(
    val targetMode: OperatingMode,
    val promptMessage: String,
    val returnToHome: Boolean = false
)

data class PendingDeviceActionConfirmation(
    val request: ToolTaskExecutionEngine.ParsedToolRequest,
    val originalPrompt: String
)

data class AttachedDocument(
    val fileName: String,
    val excerpt: String,
    val totalChars: Int
)

data class CoachAdviceEntry(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val sourceModel: String,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class MainUiState(
    val operatingMode: OperatingMode = OperatingMode.NORMAL,
    val voiceState: VoiceState = VoiceState.IDLE,
    val currentConversationId: Long? = null,
    val currentConversationTitle: String = "Primary Session",
    val streamingText: String? = null,
    val lastAction: DeviceAction? = null,
    val actionHistory: List<DeviceAction> = emptyList(),
    val diagnostics: DeviceDiagnosticsInfo? = null,
    val speechRms: Float = 0f,
    val errorMessage: String? = null
)
