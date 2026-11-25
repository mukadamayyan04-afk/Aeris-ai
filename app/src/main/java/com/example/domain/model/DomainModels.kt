package com.example.domain.model

enum class OperatingMode(val title: String, val subtitle: String) {
    NORMAL("Normal Mode", "Portrait conversation & HUD"),
    WORK("Work Mode", "Landscape advanced workspace & telemetry"),
    AUTO("Auto-Detect", "Adapts seamlessly on device orientation")
}

enum class VoiceState(val label: String) {
    IDLE("READY"),
    LISTENING("LISTENING"),
    PROCESSING("RECOGNIZING"),
    THINKING("INFERRING"),
    RESPONDING("RESPONDING"),
    SPEAKING("VOCALIZING"),
    OFFLINE("OFFLINE"),
    ERROR("FAULT")
}

enum class MessageSender {
    USER, AERIS, SYSTEM
}

data class ChatMessage(
    val id: Long = 0,
    val conversationId: Long,
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val metadata: String? = null,
    val modelId: String? = null,
    val isError: Boolean = false,
    val attachmentName: String? = null,
    val feedbackRating: Int = 0 // -1 thumbs down, 0 none, 1 thumbs up
)

data class Conversation(
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messageCount: Int = 0,
    val lastMessagePreview: String? = null,
    val summary: String = "",
    val projectContext: String = "",
    val activeModelId: String? = null,
    val projectId: Long? = null
)

data class ProjectWorkspace(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val summary: String = "",
    val progressPercent: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ProjectNote(
    val id: Long = 0,
    val projectId: Long,
    val title: String,
    val content: String,
    val updatedAt: Long = System.currentTimeMillis()
)

data class ProjectTask(
    val id: Long = 0,
    val projectId: Long,
    val title: String,
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class ProjectFileRef(
    val id: Long = 0,
    val projectId: Long,
    val fileName: String,
    val uriString: String,
    val previewText: String = "",
    val sizeBytes: Long = 0L,
    val addedAt: Long = System.currentTimeMillis()
)

data class MemoryItem(
    val id: Long = 0,
    val key: String,
    val content: String,
    val category: String = "General",
    val createdAt: Long = System.currentTimeMillis(),
    val isUserConfirmed: Boolean = true,
    val projectTag: String = "",
    val isSensitive: Boolean = false
)

enum class ActionType {
    FLASHLIGHT,
    TIMER,
    ALARM,
    REMINDER,
    OPEN_APP,
    SETTINGS_PAGE,
    NOTIFICATION_ACTION,
    VOLUME,
    DIAGNOSTICS,
    BATTERY_CHECK,
    STORAGE_INFO,
    DATE_TIME,
    VIBRATE
}

enum class ActionExecutionStatus {
    PENDING, SUCCESS, FAILED, PERMISSION_REQUIRED
}

enum class DeviceActionLifecycleState(val label: String) {
    ACTION_REQUESTED("Action Requested"),
    WAITING_FOR_PERMISSION("Waiting for Permission"),
    CONFIRMATION_REQUIRED("Confirmation Required"),
    EXECUTING("Executing"),
    COMPLETED("Completed"),
    FAILED("Failed")
}

data class DeviceAction(
    val id: String,
    val type: ActionType,
    val label: String,
    val detail: String,
    val status: ActionExecutionStatus = ActionExecutionStatus.PENDING,
    val resultMessage: String? = null,
    val lifecycleState: DeviceActionLifecycleState = when (status) {
        ActionExecutionStatus.PENDING -> DeviceActionLifecycleState.ACTION_REQUESTED
        ActionExecutionStatus.SUCCESS -> DeviceActionLifecycleState.COMPLETED
        ActionExecutionStatus.FAILED -> DeviceActionLifecycleState.FAILED
        ActionExecutionStatus.PERMISSION_REQUIRED -> DeviceActionLifecycleState.WAITING_FOR_PERMISSION
    },
    val requiresConfirmation: Boolean = false,
    val permissionNeeded: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class AerisReminder(
    val id: Long = 0,
    val title: String,
    val triggerAtMillis: Long,
    val source: String = "Calendar / Local",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class UserFeedbackRecord(
    val id: Long = 0,
    val messageId: Long? = null,
    val rating: Int = 0, // -1 or 1
    val category: String = "Style", // Style, Language, Model, Workflow, Continuity
    val ruleText: String,
    val isApproved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class PermissionStateStatus(val label: String) {
    GRANTED("Granted"),
    DENIED("Denied"),
    RESTRICTED("Restricted / Settings"),
    NOT_REQUESTED("Not Requested")
}

data class PermissionStateItem(
    val id: String,
    val title: String,
    val androidPermission: String,
    val status: PermissionStateStatus,
    val explanation: String,
    val isRuntimeRequestable: Boolean,
    val isInstallTimeOnly: Boolean = false,
    val settingsAction: String? = null
)

enum class FeatureStatus(val label: String) {
    RECOGNIZED("Recognized"),
    PLANNED("Planned"),
    DEVELOPED("Developed"),
    IMPLEMENTED("Implemented"),
    INTEGRATED("Integrated"),
    TESTED("Tested"),
    COMPLETED("Completed"),
    UNAVAILABLE("Unavailable")
}

data class FeatureItem(
    val id: String,
    val name: String,
    val description: String,
    val status: FeatureStatus,
    val sourceFiles: List<String>,
    val dependencies: List<String>,
    val testStatus: String,
    val knownLimitations: String
)

data class ModelMetadata(
    val id: String,
    val name: String,
    val parameterSize: String,
    val quantization: String,
    val fileSizeBytes: Long,
    val requiredRamMb: Int,
    val isInstalled: Boolean,
    val localFilePath: String? = null,
    val downloadUrl: String,
    val license: String,
    val isRecommendedForDevice: Boolean,
    val expectedSha256: String = "",
    val sha256Verified: Boolean? = null,
    val isDefaultRecommended: Boolean = false
)

data class DeviceDiagnosticsInfo(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val totalStorageGb: Double,
    val freeStorageGb: Double,
    val supportedAbis: List<String>,
    val isArm64V8a: Boolean,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val deviceModel: String,
    val androidVersion: String,
    val redmiOptimized: Boolean
)

enum class VoiceGender(val label: String) {
    FEMALE("Female"),
    MALE("Male"),
    UNSPECIFIED("System / Neutral")
}

enum class VoiceQuality(val label: String) {
    VERY_HIGH("Ultra High Quality"),
    HIGH("High Quality"),
    NORMAL("Normal Quality"),
    LOW("Compact"),
    UNKNOWN("Standard")
}

data class AerisVoice(
    val id: String,
    val name: String,
    val locale: java.util.Locale,
    val languageDisplayName: String,
    val countryDisplayName: String,
    val gender: VoiceGender,
    val quality: VoiceQuality,
    val isNetworkRequired: Boolean,
    val isInstalledLocally: Boolean,
    val engineName: String,
    val features: Set<String> = emptySet()
)

data class TtsEngineMetadata(
    val packageName: String,
    val label: String,
    val isDefault: Boolean
)

enum class WakeWordEngineStatus(val label: String) {
    READY("Ready"),
    ACTIVE("Listening in Background"),
    DISABLED("Disabled"),
    PERMISSION_REQUIRED("Permission Required"),
    MISSING_DEPENDENCY("Missing KWS Native Lib / Model"),
    ERROR("Service Error")
}

enum class AudioSessionMode {
    IDLE,
    STT_LISTENING,
    TTS_SPEAKING,
    WAKE_WORD_ACTIVE
}

data class VoiceTaskRecord(
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
)
