package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AerisAiEngine
import com.example.ai.AerisPersonalityStyle
import com.example.ai.AerisResponseLength
import com.example.ai.AiModelDescriptor
import com.example.ai.AiOperatingMode
import com.example.ai.AiProviderType
import com.example.ai.GeminiFirebaseProviderAdapter
import com.example.ai.ImageAspectOption
import com.example.ai.ImageGenResult
import com.example.ai.LlmEngine
import com.example.ai.ModelEngineState
import com.example.ai.ModelHealthQuotaManager
import com.example.ai.ModelHealthState
import com.example.ai.ModelManager
import com.example.ai.ProviderEndpointConfig
import com.example.ai.RoutingDecision
import com.example.ai.ToolTaskExecutionEngine
import com.example.data.database.AerisDatabase
import com.example.data.repository.ChatRepository
import com.example.data.repository.ChatRepositoryImpl
import com.example.data.repository.MemoryRepository
import com.example.data.repository.MemoryRepositoryImpl
import com.example.data.repository.ProjectRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.VoiceRepository
import com.example.data.repository.VoiceRepositoryImpl
import com.example.device.DeviceActionHandler
import com.example.device.InstalledAppSummary
import com.example.device.PermissionStatusInspector
import com.example.diagnostics.DeviceDiagnostics
import com.example.domain.model.ActionExecutionStatus
import com.example.domain.model.ActionType
import com.example.domain.model.AerisReminder
import com.example.domain.model.AerisVoice
import com.example.domain.model.ChatMessage
import com.example.domain.model.DeviceAction
import com.example.domain.model.MemoryItem
import com.example.domain.model.MessageSender
import com.example.domain.model.OperatingMode
import com.example.domain.model.PermissionStateItem
import com.example.domain.model.ProjectFileRef
import com.example.domain.model.ProjectNote
import com.example.domain.model.ProjectTask
import com.example.domain.model.ProjectWorkspace
import com.example.domain.model.UserFeedbackRecord
import com.example.domain.model.VoiceState
import com.example.domain.model.WakeWordEngineStatus
import com.example.notifications.AerisNotificationListener
import com.example.tasks.AerisTaskManager
import com.example.tasks.TaskStatus
import com.example.ui.coach.GameCoachService
import com.example.voice.AerisAudioSessionManager
import com.example.voice.AerisVoiceEngineManager
import com.example.voice.AerisVoiceSelectionManager
import com.example.voice.AerisWakeWordManager
import com.example.voice.AerisWakeWordService
import com.example.voice.AndroidSpeechRecognizerEngine
import com.example.voice.AndroidTtsEngine
import com.example.voice.SttEngine
import com.example.voice.TtsEngine
import com.example.voice.VoiceModeCommandHandler
import com.example.voice.VoiceModeCommandResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AerisDatabase.getInstance(application)
    val chatRepository: ChatRepository = ChatRepositoryImpl(db.conversationDao(), db.chatMessageDao())
    val memoryRepository: MemoryRepository = MemoryRepositoryImpl(db.memoryDao())
    val projectRepository: ProjectRepository = ProjectRepository(db.projectDao())
    val settingsRepository: SettingsRepository = SettingsRepository(application)
    val voiceRepository: VoiceRepository = VoiceRepositoryImpl(
        voiceTaskDao = db.voiceTaskDao(),
        localModelDao = db.localModelDao(),
        settingsRepository = settingsRepository
    )

    val aerisAiEngine = AerisAiEngine(application, feedbackDao = db.userFeedbackDao())
    val hairaAiEngine = aerisAiEngine
    val llmEngine: LlmEngine = aerisAiEngine
    val modelManager: ModelManager = ModelManager(application)

    val audioSessionManager = AerisAudioSessionManager(application)
    private val androidTtsEngine = AndroidTtsEngine(application, audioSessionManager)
    private val androidSttEngine = AndroidSpeechRecognizerEngine(application, audioSessionManager)
    val ttsEngine: TtsEngine = androidTtsEngine
    val sttEngine: SttEngine = androidSttEngine

    val voiceSelectionManager = AerisVoiceSelectionManager(
        ttsEngine = androidTtsEngine,
        voiceRepository = voiceRepository,
        scope = viewModelScope
    )
    val wakeWordManager = AerisWakeWordManager(
        context = application,
        audioSessionManager = audioSessionManager,
        voiceRepository = voiceRepository,
        scope = viewModelScope
    )
    val voiceEngineManager = AerisVoiceEngineManager(
        audioSessionManager = audioSessionManager,
        sttEngine = androidSttEngine,
        ttsEngine = androidTtsEngine,
        voiceSelectionManager = voiceSelectionManager,
        wakeWordManager = wakeWordManager,
        voiceRepository = voiceRepository,
        scope = viewModelScope
    )

    val deviceActionHandler: DeviceActionHandler = DeviceActionHandler(application)
    val diagnosticsTracker: DeviceDiagnostics = DeviceDiagnostics(application)
    val taskManager: AerisTaskManager = AerisTaskManager(application)
    val tasks = taskManager.tasks

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>(extraBufferCapacity = 8)
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()

    private val _pendingModeConfirmation = MutableStateFlow<PendingModeConfirmation?>(null)
    val pendingModeConfirmation: StateFlow<PendingModeConfirmation?> = _pendingModeConfirmation.asStateFlow()

    private val _pendingDeviceAction = MutableStateFlow<PendingDeviceActionConfirmation?>(null)
    val pendingDeviceAction: StateFlow<PendingDeviceActionConfirmation?> = _pendingDeviceAction.asStateFlow()

    private val _modeSwitchBanner = MutableStateFlow<String?>(null)
    val modeSwitchBanner: StateFlow<String?> = _modeSwitchBanner.asStateFlow()

    private val _modelHandoffBanner = MutableStateFlow<String?>(null)
    val modelHandoffBanner: StateFlow<String?> = _modelHandoffBanner.asStateFlow()

    private val _attachedDocument = MutableStateFlow<AttachedDocument?>(null)
    val attachedDocument: StateFlow<AttachedDocument?> = _attachedDocument.asStateFlow()

    private val _attachedImageUri = MutableStateFlow<String?>(null)
    val attachedImageUri: StateFlow<String?> = _attachedImageUri.asStateFlow()

    private val _attachedImageName = MutableStateFlow<String?>(null)
    val attachedImageName: StateFlow<String?> = _attachedImageName.asStateFlow()

    private val _imageGenResult = MutableStateFlow<ImageGenResult>(ImageGenResult.Idle)
    val imageGenResult: StateFlow<ImageGenResult> = _imageGenResult.asStateFlow()

    private val _enhancedImagePrompt = MutableStateFlow<String?>(null)
    val enhancedImagePrompt: StateFlow<String?> = _enhancedImagePrompt.asStateFlow()

    private val _coachAdviceFeed = MutableStateFlow<List<CoachAdviceEntry>>(emptyList())
    val coachAdviceFeed: StateFlow<List<CoachAdviceEntry>> = _coachAdviceFeed.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppSummary>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppSummary>> = _installedApps.asStateFlow()

    private val _permissionItems = MutableStateFlow<List<PermissionStateItem>>(
        PermissionStatusInspector.inspectAll(application)
    )
    val permissionItems: StateFlow<List<PermissionStateItem>> = _permissionItems.asStateFlow()

    val aiEngineState: StateFlow<ModelEngineState> = aerisAiEngine.state
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            ModelEngineState(
                isLoaded = false,
                modelId = null,
                modelName = "No verified AI provider",
                engineType = "Provider connection not verified",
                memoryUsageMb = 0L,
                nativeLibraryStatus = "Waiting for a successful provider response",
                supportsArm64V8a = android.os.Build.SUPPORTED_ABIS.any { it.contains("arm64-v8a", ignoreCase = true) }
            )
        )

    val healthStates: StateFlow<Map<String, ModelHealthState>> =
        aerisAiEngine.healthQuotaManager.healthStates

    val telemetrySummary = aerisAiEngine.telemetrySummary
    val telemetrySamples = aerisAiEngine.telemetrySamples

    val lastRoutingDecision: StateFlow<RoutingDecision?> =
        aerisAiEngine.lastRoutingDecision

    val catalogModels: StateFlow<List<AiModelDescriptor>> = combine(
        aerisAiEngine.providerIntegrationManager.endpointConfigs,
        aerisAiEngine.providerIntegrationManager.activeGgufModelId
    ) { _, _ ->
        aerisAiEngine.providerIntegrationManager.catalogDescriptors()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        aerisAiEngine.providerIntegrationManager.catalogDescriptors()
    )

    private var activeGenerationJob: Job? = null
    private var messagesCollectJob: Job? = null
    private var modeChangedInSession: Boolean = false
    private var isAnalyzingCoachFrame: Boolean = false
    private val seenNotificationIds = mutableSetOf<String>()
    private val sessionStartedAtMillis = System.currentTimeMillis()
    private var lastHandledWakeTimestamp: Long = 0L
    private var lastSubmittedPromptKey: String = ""
    private var lastSubmittedAtMillis: Long = 0L

    val conversations = chatRepository.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    val memories = memoryRepository.getAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Stage 3: Project Workspaces state
    val projects: StateFlow<List<ProjectWorkspace>> = projectRepository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeProjectId = MutableStateFlow<Long?>(null)
    val activeProjectId: StateFlow<Long?> = _activeProjectId.asStateFlow()

    val activeProjectNotes: StateFlow<List<ProjectNote>> = _activeProjectId
        .flatMapLatest { pid -> if (pid == null) flowOf(emptyList()) else projectRepository.getNotesForProject(pid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProjectTasks: StateFlow<List<ProjectTask>> = _activeProjectId
        .flatMapLatest { pid -> if (pid == null) flowOf(emptyList()) else projectRepository.getTasksForProject(pid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProjectFiles: StateFlow<List<ProjectFileRef>> = _activeProjectId
        .flatMapLatest { pid -> if (pid == null) flowOf(emptyList()) else projectRepository.getFilesForProject(pid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProjectFiles: StateFlow<List<ProjectFileRef>> = projectRepository.getAllFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val models = modelManager.availableModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<AerisReminder>> = db.reminderDao().getAllReminders()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFeedback: StateFlow<List<UserFeedbackRecord>> =
        (aerisAiEngine.feedbackEngine.getAllFeedback() ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val approvedFeedback: StateFlow<List<UserFeedbackRecord>> =
        (aerisAiEngine.feedbackEngine.getApprovedFeedback() ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flashState = deviceActionHandler.isFlashlightOn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // AI Brain routing & provider configs
    val freeOnlyMode = settingsRepository.freeOnlyModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoSwitchEnabled = settingsRepository.autoSwitchEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val aiOperatingMode = settingsRepository.aiOperatingModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiOperatingMode.HYBRID)

    val manualModelLock = settingsRepository.manualModelLockFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val localFallbackEnabled = settingsRepository.localFallbackEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val disabledProviders = settingsRepository.disabledProvidersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val defaultLocalModelPrompted = settingsRepository.defaultLocalModelPromptedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val personalityStyle = settingsRepository.personalityStyleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AerisPersonalityStyle.CALM_PROFESSIONAL)

    val responseLength = settingsRepository.responseLengthFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AerisResponseLength.BALANCED)

    val preferredModelId = settingsRepository.preferredModelIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GeminiFirebaseProviderAdapter.DEFAULT_GEMINI_MODEL)

    val openAiConfig = settingsRepository.openAiConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple("", "", ""))

    val openRouterConfig = settingsRepository.openRouterConfigFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            Triple(AiProviderType.OPENROUTER.defaultBaseUrl, AiProviderType.OPENROUTER.suggestedModelId, "")
        )

    val groqConfig = settingsRepository.groqConfigFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            Triple(AiProviderType.GROQ.defaultBaseUrl, AiProviderType.GROQ.suggestedModelId, "")
        )

    val huggingFaceConfig = settingsRepository.huggingFaceConfigFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            Triple(AiProviderType.HUGGING_FACE.defaultBaseUrl, AiProviderType.HUGGING_FACE.suggestedModelId, "")
        )

    val cloudflareConfig = settingsRepository.cloudflareConfigFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            Triple(AiProviderType.CLOUDFLARE_WORKERS_AI.defaultBaseUrl, AiProviderType.CLOUDFLARE_WORKERS_AI.suggestedModelId, "")
        )

    val gptOssConfig = settingsRepository.gptOssConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple("", "gpt-oss-20b", ""))

    val qwenConfig = settingsRepository.qwenConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple("", "qwen2.5-7b-instruct", ""))

    // Settings flows
    val animationsEnabled = settingsRepository.animationsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val visualEffectsEnabled = settingsRepository.visualEffectsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val batterySaverHolo = settingsRepository.batterySaverHoloFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val memoryEnabled = settingsRepository.memoryEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val deviceActionConfirmSensitive = settingsRepository.deviceActionConfirmSensitiveFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notificationAnnouncerEnabled = settingsRepository.notificationAnnouncerEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val notificationAllowedApps = settingsRepository.notificationAllowedAppsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val notificationHideSensitive = settingsRepository.notificationHideSensitiveFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val gameCoachCloudAnalysisEnabled = settingsRepository.gameCoachCloudAnalysisFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val wakeWordPrivacyMode = settingsRepository.wakeWordPrivacyModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val startOnBootOptIn = settingsRepository.startOnBootOptInFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val floatingAssistantEnabled = settingsRepository.floatingAssistantEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isFloatingAssistantRunning: StateFlow<Boolean> = com.example.device.FloatingAssistantService.isRunning

    val customSystemInstruction = settingsRepository.customSystemInstructionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val featureUsageCounts = settingsRepository.featureUsageCountsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Voice flows
    val speechRate = voiceRepository.speechRateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val speechPitch = voiceRepository.speechPitchFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val wakeWordEnabled = voiceRepository.wakeWordEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val backgroundListeningEnabled = voiceRepository.backgroundListeningEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val availableVoices = androidTtsEngine.availableVoices
    val selectedVoice = androidTtsEngine.selectedVoice
    val availableTtsEngines = androidTtsEngine.availableEngines
    val activeTtsEnginePackage = androidTtsEngine.activeEnginePackage
    val activeSttEngineName = voiceEngineManager.activeSttEngineName
    val activeTtsEngineName = voiceEngineManager.activeTtsEngineName
    val isOfflineVoiceReady = voiceEngineManager.isOfflineReady
    val isSpeechPaused = voiceEngineManager.isSpeechPaused
    val isSttOffline = androidSttEngine.isOfflineModelInstalled

    val liveTranscript = androidSttEngine.liveTranscript
    val voiceError = voiceEngineManager.voiceError
    val voiceFallbackNotice = voiceSelectionManager.fallbackNotice

    val wakeWordStatus: StateFlow<WakeWordEngineStatus> = wakeWordManager.engineStatus
    val wakeWordStatusDetail: StateFlow<String?> = wakeWordManager.statusDetail
    val wakeWordServiceRunning: StateFlow<Boolean> = wakeWordManager.isServiceRunning
    val voiceCapabilityReport = wakeWordManager.capabilityReport

    val voiceTasks = voiceRepository.recentVoiceTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val voiceTaskCount = voiceRepository.voiceTaskCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val selectedVoiceId = voiceRepository.selectedVoiceIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val voiceResponsesEnabled = voiceRepository.voiceResponsesEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoPlayResponses = voiceRepository.autoPlayResponsesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val voiceInputEnabled = voiceRepository.voiceInputEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val pushToTalkEnabled = voiceRepository.pushToTalkEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val recognitionLanguage = voiceRepository.recognitionLanguageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en-US")

    val wakeWordPhrase = voiceRepository.wakeWordPhraseFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Hey AERIS")

    init {
        voiceEngineManager.initialize()

        // Sync AI Brain routing preferences
        viewModelScope.launch {
            combine(
                settingsRepository.freeOnlyModeFlow,
                settingsRepository.autoSwitchEnabledFlow,
                settingsRepository.preferredModelIdFlow,
                settingsRepository.aiOperatingModeFlow,
                settingsRepository.manualModelLockFlow
            ) { freeOnly, autoSwitch, preferred, opMode, manualLock ->
                listOf(freeOnly, autoSwitch, preferred, opMode, manualLock)
            }.combine(settingsRepository.localFallbackEnabledFlow) { arr, localFallback ->
                Pair(arr, localFallback)
            }.collect { (arr, localFallback) ->
                aerisAiEngine.updateRoutingPreferences(
                    freeOnly = arr[0] as Boolean,
                    autoSwitch = arr[1] as Boolean,
                    preferredId = arr[2] as String?,
                    operatingMode = arr[3] as AiOperatingMode,
                    manualModelLock = arr[4] as Boolean,
                    localFallbackEnabled = localFallback
                )
            }
        }

        viewModelScope.launch {
            settingsRepository.disabledProvidersFlow.collect { disabled ->
                aerisAiEngine.setDisabledProviders(disabled)
            }
        }

        viewModelScope.launch {
            combine(
                settingsRepository.personalityStyleFlow,
                settingsRepository.responseLengthFlow,
                settingsRepository.customSystemInstructionFlow
            ) { style, length, custom -> Triple(style, length, custom) }.collect { (style, length, custom) ->
                aerisAiEngine.updatePersonalityPreferences(style, length, custom)
            }
        }

        // Sync user-configured external provider endpoints & cached feedback rules
        viewModelScope.launch {
            approvedFeedback.collect { rules ->
                aerisAiEngine.updateCachedFeedback(rules)
            }
        }
        viewModelScope.launch {
            settingsRepository.openAiConfigFlow.collect { (url, model, token) ->
                aerisAiEngine.updateEndpointConfig(
                    ProviderEndpointConfig(AiProviderType.OPENAI_COMPATIBLE, url, model, token)
                )
            }
        }
        viewModelScope.launch {
            settingsRepository.openRouterConfigFlow.collect { (url, model, token) ->
                aerisAiEngine.updateEndpointConfig(
                    ProviderEndpointConfig(AiProviderType.OPENROUTER, url, model, token)
                )
            }
        }
        viewModelScope.launch {
            settingsRepository.groqConfigFlow.collect { (url, model, token) ->
                aerisAiEngine.updateEndpointConfig(
                    ProviderEndpointConfig(AiProviderType.GROQ, url, model, token)
                )
            }
        }
        viewModelScope.launch {
            settingsRepository.huggingFaceConfigFlow.collect { (url, model, token) ->
                aerisAiEngine.updateEndpointConfig(
                    ProviderEndpointConfig(AiProviderType.HUGGING_FACE, url, model, token)
                )
            }
        }
        viewModelScope.launch {
            settingsRepository.cloudflareConfigFlow.collect { (url, model, token) ->
                aerisAiEngine.updateEndpointConfig(
                    ProviderEndpointConfig(AiProviderType.CLOUDFLARE_WORKERS_AI, url, model, token)
                )
            }
        }
        viewModelScope.launch {
            settingsRepository.gptOssConfigFlow.collect { (url, model, token) ->
                aerisAiEngine.updateEndpointConfig(
                    ProviderEndpointConfig(AiProviderType.GPT_OSS_HOSTED, url, model, token)
                )
            }
        }
        viewModelScope.launch {
            settingsRepository.qwenConfigFlow.collect { (url, model, token) ->
                aerisAiEngine.updateEndpointConfig(
                    ProviderEndpointConfig(AiProviderType.QWEN_HOSTED, url, model, token)
                )
            }
        }

        viewModelScope.launch {
            projects.collect { list ->
                if (list.isNotEmpty() && (_activeProjectId.value == null || list.none { it.id == _activeProjectId.value })) {
                    _activeProjectId.value = list.first().id
                } else if (list.isEmpty()) {
                    _activeProjectId.value = null
                }
            }
        }

        // Initialize default conversation
        viewModelScope.launch {
            conversations.collect { convs ->
                if (convs.isEmpty()) {
                    val id = chatRepository.createConversation("AERIS Session")
                    _uiState.value = _uiState.value.copy(
                        currentConversationId = id,
                        currentConversationTitle = "AERIS Session"
                    )
                    loadConversationMessages(id)
                } else if (_uiState.value.currentConversationId == null) {
                    val first = convs.first()
                    _uiState.value = _uiState.value.copy(
                        currentConversationId = first.id,
                        currentConversationTitle = first.title
                    )
                    loadConversationMessages(first.id)
                }
            }
        }

        // Sync local GGUF model metadata into Room model_metadata table
        viewModelScope.launch {
            modelManager.availableModels.collect { modelList ->
                if (modelList.isNotEmpty()) {
                    voiceRepository.syncModels(modelList, category = "LLM")
                }
            }
        }

        // Restore initial operating mode once without racing in-session transitions
        viewModelScope.launch {
            val savedMode = settingsRepository.operatingModeFlow.first()
            if (!modeChangedInSession) {
                when (savedMode) {
                    OperatingMode.AUTO -> _uiState.value = _uiState.value.copy(operatingMode = OperatingMode.AUTO)
                    OperatingMode.WORK -> {
                        _uiState.value = _uiState.value.copy(operatingMode = OperatingMode.WORK)
                        _navigationEvents.tryEmit(NavigationEvent.EnterWorkMode)
                    }
                    OperatingMode.NORMAL -> _uiState.value = _uiState.value.copy(operatingMode = OperatingMode.NORMAL)
                }
            }
        }

        // Sync orchestrated VoiceState and errors into MainUiState
        viewModelScope.launch {
            voiceEngineManager.voiceState.collect { vState ->
                _uiState.value = _uiState.value.copy(voiceState = vState)
            }
        }

        viewModelScope.launch {
            voiceEngineManager.voiceError.collect { err ->
                _uiState.value = _uiState.value.copy(errorMessage = err)
            }
        }

        viewModelScope.launch {
            androidSttEngine.audioRmsLevel.collect { rms ->
                _uiState.value = _uiState.value.copy(speechRms = rms)
            }
        }

        // Notification Announcer listener loop (Stage 5)
        viewModelScope.launch {
            AerisNotificationListener.notificationEvents.collect { notif ->
                if (notif.timestamp >= sessionStartedAtMillis &&
                    seenNotificationIds.add(notif.id) &&
                    notificationAnnouncerEnabled.value &&
                    voiceResponsesEnabled.value &&
                    AerisNotificationListener.isPermissionGranted(getApplication())
                ) {
                    val allowed = notificationAllowedApps.value
                    if (notif.packageName in allowed) {
                        val spoken = AerisNotificationListener.formatAnnouncementText(
                            notification = notif,
                            hideSensitive = notificationHideSensitive.value
                        )
                        voiceEngineManager.speakText(spoken)
                    }
                }
            }
        }

        // Continuous Wake-Word -> Voice Command Loop (Stage 6)
        viewModelScope.launch {
            AerisWakeWordService.wakeWordDetectedEvent.collect { ts ->
                if (ts != null && ts > lastHandledWakeTimestamp && AerisWakeWordService.isServiceRunning.value) {
                    lastHandledWakeTimestamp = ts
                    startVoiceInput()
                }
            }
        }

        // Consented Game Coach screen frame sampling loop (Stage 7A)
        viewModelScope.launch {
            GameCoachService.latestFrame.collect { bitmap ->
                if (bitmap != null && GameCoachService.isObserving.value && !isAnalyzingCoachFrame) {
                    analyzeCoachFrame(bitmap)
                }
            }
        }

        refreshDiagnostics()
        refreshInstalledApps()
        refreshPermissionStates()
    }

    private fun analyzeCoachFrame(bitmap: android.graphics.Bitmap) {
        if (!gameCoachCloudAnalysisEnabled.value) {
            if (_coachAdviceFeed.value.none { it.text.contains("Cloud Vision Analysis is OFF") }) {
                _coachAdviceFeed.value = (
                    listOf(
                        CoachAdviceEntry(
                            text = "Screen frame sampled locally. Enable 'Live Gemini Vision Coach' above for AI screen callouts (requires internet; uses Gemini 3.8 Flash free-tier quota).",
                            sourceModel = "Local Screen Sampler"
                        )
                    ) + _coachAdviceFeed.value
                    ).take(15)
            }
            return
        }

        if (!aerisAiEngine.providerIntegrationManager.isNetworkAvailable()) {
            _coachAdviceFeed.value = (
                listOf(
                    CoachAdviceEntry(
                        text = "Offline: Cannot reach Gemini 3.8 Flash Vision right now, Sir ji.",
                        sourceModel = "Offline",
                        isError = true
                    )
                ) + _coachAdviceFeed.value
                ).take(15)
            return
        }

        isAnalyzingCoachFrame = true
        viewModelScope.launch {
            try {
                val res = aerisAiEngine.providerIntegrationManager.geminiAdapter.analyzeGameplayBitmap(
                    bitmap = bitmap,
                    gameName = "Active Screen Session"
                )
                val entry = res.fold(
                    onSuccess = { advice ->
                        CoachAdviceEntry(
                            text = advice,
                            sourceModel = GeminiFirebaseProviderAdapter.DEFAULT_GEMINI_MODEL
                        )
                    },
                    onFailure = { err ->
                        CoachAdviceEntry(
                            text = err.localizedMessage ?: "Vision coaching failed.",
                            sourceModel = GeminiFirebaseProviderAdapter.DEFAULT_GEMINI_MODEL,
                            isError = true
                        )
                    }
                )
                _coachAdviceFeed.value = (listOf(entry) + _coachAdviceFeed.value).take(15)
            } finally {
                isAnalyzingCoachFrame = false
            }
        }
    }

    fun clearCoachFeed() {
        _coachAdviceFeed.value = emptyList()
    }

    fun refreshDiagnostics() {
        _uiState.value = _uiState.value.copy(
            diagnostics = diagnosticsTracker.getDiagnostics()
        )
    }

    fun refreshInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = deviceActionHandler.listInstalledLaunchableApps()
            _installedApps.value = apps
        }
    }

    fun refreshPermissionStates() {
        _permissionItems.value = PermissionStatusInspector.inspectAll(getApplication())
    }

    fun markPermissionRequested(permission: String) {
        PermissionStatusInspector.markRequested(getApplication(), permission)
        refreshPermissionStates()
    }

    fun refreshVoiceCapabilities() {
        androidSttEngine.refreshCapabilities()
        wakeWordManager.refreshCapabilities()
    }

    // --- Stage 1 AI Brain & Multi-Model Controls ---

    fun setFreeOnlyMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFreeOnlyMode(enabled)
            if (enabled && preferredModelId.value == GeminiFirebaseProviderAdapter.ARGON_GEMINI_MODEL) {
                settingsRepository.setPreferredModelId(GeminiFirebaseProviderAdapter.DEFAULT_GEMINI_MODEL)
            }
        }
    }

    fun setAutoSwitchEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoSwitchEnabled(enabled) }
    }

    fun setAiOperatingMode(mode: AiOperatingMode) {
        viewModelScope.launch { settingsRepository.setAiOperatingMode(mode) }
    }

    fun setManualModelLock(locked: Boolean) {
        viewModelScope.launch { settingsRepository.setManualModelLock(locked) }
    }

    fun setLocalFallbackEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setLocalFallbackEnabled(enabled) }
    }

    fun setProviderEnabled(providerType: AiProviderType, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setProviderEnabled(providerType, enabled)
            aerisAiEngine.setProviderEnabled(providerType, enabled)
        }
    }

    fun removeProviderKey(providerType: AiProviderType) {
        viewModelScope.launch {
            settingsRepository.clearProviderConfig(providerType)
            aerisAiEngine.removeEndpointConfig(providerType)
        }
    }

    fun markDefaultLocalModelPrompted() {
        viewModelScope.launch { settingsRepository.setDefaultLocalModelPrompted(true) }
    }

    fun setPersonalityStyle(style: AerisPersonalityStyle) {
        viewModelScope.launch { settingsRepository.setPersonalityStyle(style) }
    }

    fun setResponseLength(length: AerisResponseLength) {
        viewModelScope.launch { settingsRepository.setResponseLength(length) }
    }

    fun setPreferredModelId(modelId: String?) {
        viewModelScope.launch { settingsRepository.setPreferredModelId(modelId) }
    }

    fun testModelConnection(modelId: String) {
        viewModelScope.launch {
            aerisAiEngine.testConnection(modelId)
        }
    }

    fun discoverProviderModels(modelId: String) {
        viewModelScope.launch {
            val res = aerisAiEngine.discoverModels(modelId)
            res.onSuccess { info ->
                val count = info.modelIds.size
                val msg = if (count > 0) "Discovered $count models in ${info.latencyMs}ms" else "Connected (${info.latencyMs}ms), no model list returned"
                Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(getApplication(), err.localizedMessage ?: "Model discovery failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun testAllConfiguredProviders() {
        viewModelScope.launch {
            val results = aerisAiEngine.testAllConfiguredProviders()
            val ok = results.values.count { it.isSuccess }
            Toast.makeText(getApplication(), "Tested ${results.size} providers: $ok connected", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearTelemetry() {
        aerisAiEngine.telemetryTracker.clear()
    }

    fun saveEndpointConfig(providerType: AiProviderType, baseUrl: String, modelId: String, token: String) {
        viewModelScope.launch {
            when (providerType) {
                AiProviderType.OPENAI_COMPATIBLE -> settingsRepository.setOpenAiConfig(baseUrl, modelId, token)
                AiProviderType.OPENROUTER -> settingsRepository.setOpenRouterConfig(baseUrl, modelId, token)
                AiProviderType.GROQ -> settingsRepository.setGroqConfig(baseUrl, modelId, token)
                AiProviderType.HUGGING_FACE -> settingsRepository.setHuggingFaceConfig(baseUrl, modelId, token)
                AiProviderType.CLOUDFLARE_WORKERS_AI -> settingsRepository.setCloudflareConfig(baseUrl, modelId, token)
                AiProviderType.GPT_OSS_HOSTED -> settingsRepository.setGptOssConfig(baseUrl, modelId, token)
                AiProviderType.QWEN_HOSTED -> settingsRepository.setQwenConfig(baseUrl, modelId, token)
                else -> Unit
            }
        }
    }

    fun loadGgufModel(modelPath: String, modelId: String) {
        viewModelScope.launch {
            val result = aerisAiEngine.loadModel(modelPath, modelId)
            val msg = if (result.isSuccess) {
                "Loaded GGUF weights ($modelId)."
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Failed to load GGUF weights."
            }
            Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun importGgufFromUri(uri: Uri, fileName: String) {
        viewModelScope.launch {
            val res = modelManager.registerImportedModelFromUri(uri, fileName)
            res.onSuccess { path ->
                Toast.makeText(getApplication(), "Imported $fileName into GGUF storage", Toast.LENGTH_SHORT).show()
                loadGgufModel(path, fileName.substringBeforeLast('.'))
            }.onFailure { err ->
                Toast.makeText(getApplication(), "Import failed: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun startGgufDownload(modelId: String) {
        viewModelScope.launch {
            val res = modelManager.startDownload(modelId)
            res.onFailure { err ->
                Toast.makeText(
                    getApplication(),
                    "Download failed: ${err.localizedMessage ?: "Network error"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun pauseGgufDownload(modelId: String) {
        modelManager.pauseDownload(modelId)
    }

    fun verifyGgufSha256(modelId: String) {
        viewModelScope.launch {
            val res = modelManager.verifyLocalFileSha256(modelId)
            val msg = res.fold(
                onSuccess = { digest -> "SHA-256 verified: ${digest.take(16)}..." },
                onFailure = { err -> err.localizedMessage ?: "SHA-256 verification failed" }
            )
            Toast.makeText(getApplication(), msg, Toast.LENGTH_LONG).show()
        }
    }

    fun deleteGgufModel(modelId: String) {
        viewModelScope.launch {
            modelManager.deleteModel(modelId)
        }
    }

    // --- Stage 3: Project Workspaces CRUD ---

    fun selectProject(projectId: Long) {
        _activeProjectId.value = projectId
    }

    fun createProject(name: String, description: String = "") {
        viewModelScope.launch {
            val id = projectRepository.createProject(name, description)
            _activeProjectId.value = id
        }
    }

    fun updateProjectSummary(projectId: Long, summary: String) {
        viewModelScope.launch { projectRepository.updateProjectSummary(projectId, summary) }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch { projectRepository.deleteProject(projectId) }
    }

    fun addProjectNote(projectId: Long, title: String, content: String) {
        viewModelScope.launch { projectRepository.addNote(projectId, title, content) }
    }

    fun deleteProjectNote(noteId: Long) {
        viewModelScope.launch { projectRepository.deleteNote(noteId) }
    }

    fun addProjectTask(projectId: Long, title: String) {
        viewModelScope.launch { projectRepository.addTask(projectId, title) }
    }

    fun toggleProjectTask(taskId: Long, isDone: Boolean, projectId: Long) {
        viewModelScope.launch { projectRepository.toggleTask(taskId, isDone, projectId) }
    }

    fun deleteProjectTask(taskId: Long, projectId: Long) {
        viewModelScope.launch { projectRepository.deleteTask(taskId, projectId) }
    }

    fun attachFileToProject(projectId: Long, uri: Uri, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                        val sb = StringBuilder()
                        val buf = CharArray(1024)
                        var n: Int
                        while (reader.read(buf).also { n = it } != -1 && sb.length < 2500) {
                            sb.append(buf, 0, n)
                        }
                        sb.toString()
                    }
                }.orEmpty()
                projectRepository.addFile(
                    projectId = projectId,
                    fileName = fileName,
                    uriString = uri.toString(),
                    previewText = content,
                    sizeBytes = content.length.toLong()
                )
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Could not attach file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun deleteProjectFile(fileId: Long) {
        viewModelScope.launch { projectRepository.deleteFile(fileId) }
    }

    fun openProjectChat(project: ProjectWorkspace) {
        viewModelScope.launch {
            val title = "${project.name} Chat"
            val id = chatRepository.createConversation(
                title = title,
                projectId = project.id,
                projectContext = project.name
            )
            selectConversation(id, title)
        }
    }

    // --- Stage 7B Image Studio ---

    fun enhanceImagePrompt(rawPrompt: String, onEnhanced: (String) -> Unit) {
        viewModelScope.launch {
            val res = aerisAiEngine.imageGenerationManager.enhancePrompt(rawPrompt)
            res.onSuccess { improved ->
                _enhancedImagePrompt.value = improved
                onEnhanced(improved)
            }.onFailure { err ->
                Toast.makeText(getApplication(), err.localizedMessage ?: "Prompt enhance failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun testImageModel() {
        viewModelScope.launch {
            val res = aerisAiEngine.imageGenerationManager.testImageModelConnection()
            val msg = res.getOrElse { it.localizedMessage ?: "Image model test failed" }
            Toast.makeText(getApplication(), msg, Toast.LENGTH_LONG).show()
        }
    }

    fun generateImage(prompt: String, aspect: ImageAspectOption) {
        viewModelScope.launch {
            _imageGenResult.value = ImageGenResult.Loading
            val customCfg = aerisAiEngine.providerIntegrationManager.endpointConfigs.value[AiProviderType.OPENAI_COMPATIBLE]
            val result = aerisAiEngine.imageGenerationManager.generateImage(
                prompt = prompt,
                aspectOption = aspect,
                allowPaidOrBlazeModels = !freeOnlyMode.value,
                customEndpoint = customCfg
            )
            _imageGenResult.value = result
        }
    }

    fun saveGeneratedImage(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch {
            val res = aerisAiEngine.imageGenerationManager.saveBitmapToGallery(bitmap)
            val msg = if (res.isSuccess) "Saved to Pictures/AERIS Gallery, Sir ji!" else "Save failed: ${res.exceptionOrNull()?.localizedMessage}"
            Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun shareGeneratedImage(bitmap: android.graphics.Bitmap, onLaunchShare: (Intent) -> Unit) {
        viewModelScope.launch {
            val res = aerisAiEngine.imageGenerationManager.createShareIntent(bitmap)
            res.onSuccess(onLaunchShare)
                .onFailure { Toast.makeText(getApplication(), "Share failed: ${it.localizedMessage}", Toast.LENGTH_SHORT).show() }
        }
    }

    fun clearImageResult() {
        _imageGenResult.value = ImageGenResult.Idle
    }

    // --- Operating Modes ---

    fun setOperatingMode(mode: OperatingMode) {
        modeChangedInSession = true
        when (mode) {
            OperatingMode.WORK -> enterWorkMode()
            OperatingMode.NORMAL -> exitWorkMode(returnToHome = false)
            OperatingMode.AUTO -> {
                _uiState.value = _uiState.value.copy(operatingMode = OperatingMode.AUTO)
                viewModelScope.launch { settingsRepository.setOperatingMode(OperatingMode.AUTO) }
                _modeSwitchBanner.value = "Auto-Detect mode enabled, Sir ji."
            }
        }
    }

    fun enterWorkMode(
        spokenConfirmation: String? = null,
        fromVoiceCommand: Boolean = false
    ) {
        modeChangedInSession = true
        val banner = if (fromVoiceCommand) {
            "Work Mode activated in landscape orientation, Sir ji."
        } else {
            "Switched to Work Mode (Landscape)"
        }
        _uiState.value = _uiState.value.copy(operatingMode = OperatingMode.WORK)
        _modeSwitchBanner.value = banner
        _navigationEvents.tryEmit(NavigationEvent.EnterWorkMode)
        viewModelScope.launch { settingsRepository.setOperatingMode(OperatingMode.WORK) }
        if (spokenConfirmation != null && voiceResponsesEnabled.value) {
            voiceEngineManager.speakText(spokenConfirmation)
        }
    }

    fun exitWorkMode(
        returnToHome: Boolean = false,
        spokenConfirmation: String? = null,
        fromVoiceCommand: Boolean = false
    ) {
        modeChangedInSession = true
        val banner = when {
            returnToHome -> "Returned to Home in portrait Normal Mode, Sir ji."
            fromVoiceCommand -> "Normal Mode portrait orientation restored, Sir ji."
            else -> "Switched to Normal Mode (Portrait)"
        }
        _uiState.value = _uiState.value.copy(operatingMode = OperatingMode.NORMAL)
        _modeSwitchBanner.value = banner
        _navigationEvents.tryEmit(NavigationEvent.ExitWorkMode(returnToHome = returnToHome))
        viewModelScope.launch { settingsRepository.setOperatingMode(OperatingMode.NORMAL) }
        if (spokenConfirmation != null && voiceResponsesEnabled.value) {
            voiceEngineManager.speakText(spokenConfirmation)
        }
    }

    fun confirmPendingModeSwitch() {
        val pending = _pendingModeConfirmation.value ?: return
        _pendingModeConfirmation.value = null
        when (pending.targetMode) {
            OperatingMode.WORK -> enterWorkMode(
                spokenConfirmation = "Activating Work Mode Command Center in landscape orientation, Sir ji.",
                fromVoiceCommand = true
            )
            OperatingMode.NORMAL -> exitWorkMode(
                returnToHome = pending.returnToHome,
                spokenConfirmation = if (pending.returnToHome) {
                    "Returning to Home in portrait mode, Sir ji."
                } else {
                    "Switching back to Normal Mode in portrait orientation, Sir ji."
                },
                fromVoiceCommand = true
            )
            OperatingMode.AUTO -> setOperatingMode(OperatingMode.AUTO)
        }
    }

    fun dismissPendingModeSwitch() {
        _pendingModeConfirmation.value = null
    }

    fun clearModeSwitchBanner() {
        _modeSwitchBanner.value = null
    }

    fun clearModelHandoffBanner() {
        _modelHandoffBanner.value = null
    }

    fun processVoiceTranscriptForModeCommand(recognizedText: String): Boolean {
        val trimmed = recognizedText.trim()
        if (trimmed.isBlank()) return false

        val startTime = System.currentTimeMillis()
        val convId = _uiState.value.currentConversationId

        return when (val result = VoiceModeCommandHandler.parse(trimmed, _uiState.value.operatingMode)) {
            is VoiceModeCommandResult.SwitchMode -> {
                if (result.targetMode == OperatingMode.WORK) {
                    val reply = "Activating Work Mode Command Center in landscape orientation, Sir ji."
                    enterWorkMode(spokenConfirmation = reply, fromVoiceCommand = true)
                    viewModelScope.launch {
                        voiceEngineManager.logVoiceTask(convId, trimmed, reply, "SUCCESS", durationMs = System.currentTimeMillis() - startTime)
                    }
                } else {
                    val reply = if (result.returnToHome) {
                        "Returning to Home in portrait orientation, Sir ji."
                    } else {
                        "Switching to Normal Mode in portrait orientation, Sir ji."
                    }
                    exitWorkMode(result.returnToHome, spokenConfirmation = reply, fromVoiceCommand = true)
                    viewModelScope.launch {
                        voiceEngineManager.logVoiceTask(convId, trimmed, reply, "SUCCESS", durationMs = System.currentTimeMillis() - startTime)
                    }
                }
                true
            }
            is VoiceModeCommandResult.Ambiguous -> {
                _pendingModeConfirmation.value = PendingModeConfirmation(
                    targetMode = result.suggestedMode,
                    promptMessage = result.promptMessage,
                    returnToHome = result.returnToHome
                )
                if (voiceResponsesEnabled.value) voiceEngineManager.speakText(result.promptMessage)
                true
            }
            VoiceModeCommandResult.NotAModeCommand -> false
        }
    }

    // --- Conversations, Continuity & Document Attachment ---

    fun selectConversation(id: Long, title: String) {
        _uiState.value = _uiState.value.copy(
            currentConversationId = id,
            currentConversationTitle = title
        )
        loadConversationMessages(id)
    }

    fun createNewConversation(title: String = "Session ${System.currentTimeMillis() % 1000}") {
        viewModelScope.launch {
            val id = chatRepository.createConversation(title)
            selectConversation(id, title)
        }
    }

    fun renameConversation(id: Long, newTitle: String) {
        viewModelScope.launch {
            chatRepository.renameConversation(id, newTitle)
            if (_uiState.value.currentConversationId == id) {
                _uiState.value = _uiState.value.copy(currentConversationTitle = newTitle)
            }
        }
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            chatRepository.deleteConversation(id)
            if (_uiState.value.currentConversationId == id) {
                val remaining = conversations.value.filter { it.id != id }
                if (remaining.isNotEmpty()) {
                    val next = remaining.first()
                    selectConversation(next.id, next.title)
                } else {
                    createNewConversation("New Session")
                }
            }
        }
    }

    fun setConversationProjectContext(projectContext: String) {
        val convId = _uiState.value.currentConversationId ?: return
        viewModelScope.launch {
            val conv = chatRepository.getConversationById(convId) ?: return@launch
            chatRepository.updateConversationContinuity(
                id = convId,
                summary = conv.summary,
                projectContext = projectContext.trim(),
                activeModelId = conv.activeModelId
            )
        }
    }

    private fun loadConversationMessages(conversationId: Long) {
        messagesCollectJob?.cancel()
        messagesCollectJob = viewModelScope.launch {
            chatRepository.getMessagesForConversation(conversationId).collect { msgList ->
                _messages.value = msgList
            }
        }
    }

    fun attachDocumentFromUri(uri: Uri, displayName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val stream = getApplication<Application>().contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Could not open selected file.")
                val text = stream.use { input ->
                    BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                        val sb = StringBuilder()
                        val buf = CharArray(2048)
                        var n: Int
                        while (reader.read(buf).also { n = it } != -1 && sb.length < 12_000) {
                            sb.append(buf, 0, n)
                        }
                        sb.toString()
                    }
                }
                if (text.isBlank()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Selected file is empty or unreadable.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    _attachedDocument.value = AttachedDocument(
                        fileName = displayName,
                        excerpt = text.take(4000),
                        totalChars = text.length
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Attachment failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun clearAttachedDocument() {
        _attachedDocument.value = null
    }

    fun attachImageFromPicker(uri: Uri?, displayName: String?) {
        _attachedImageUri.value = uri?.toString()
        _attachedImageName.value = displayName ?: uri?.lastPathSegment ?: "Selected Photo"
    }

    fun clearAttachedImage() {
        _attachedImageUri.value = null
        _attachedImageName.value = null
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            chatRepository.clearAllConversations()
            val id = chatRepository.createConversation("AERIS Session")
            selectConversation(id, "AERIS Session")
        }
    }

    // --- Chat Send / Regenerate / Retry / Feedback ---

    fun sendMessage(text: String) {
        sendMessage(text = text, isVoiceInitiated = false)
    }

    fun sendMessage(text: String, isVoiceInitiated: Boolean = false) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val nowMs = System.currentTimeMillis()
        val requestKey = "${_uiState.value.currentConversationId ?: -1}:$trimmed"
        if (activeGenerationJob?.isActive == true && requestKey == lastSubmittedPromptKey && (nowMs - lastSubmittedAtMillis) < 1200L) {
            return
        }
        if (requestKey == lastSubmittedPromptKey && (nowMs - lastSubmittedAtMillis) < 450L) {
            return
        }
        lastSubmittedPromptKey = requestKey
        lastSubmittedAtMillis = nowMs

        deviceActionHandler.vibrateHaptic(20)
        voiceEngineManager.stopSpeaking()
        activeGenerationJob?.cancel()

        val startTime = nowMs
        val currentAttachment = _attachedDocument.value

        viewModelScope.launch {
            val convId = _uiState.value.currentConversationId ?: run {
                val createdId = chatRepository.createConversation("AERIS Session")
                selectConversation(createdId, "AERIS Session")
                createdId
            }

            chatRepository.sendMessage(
                ChatMessage(
                    conversationId = convId,
                    sender = MessageSender.USER,
                    text = trimmed,
                    attachmentName = currentAttachment?.fileName
                )
            )

            // Parse & handle actionable Android intents safely
            val parsedTool = aerisAiEngine.toolTaskExecutionEngine.detectActionRequest(trimmed)
            if (parsedTool != null) {
                if (parsedTool.requiresConfirmation && deviceActionConfirmSensitive.value) {
                    _pendingDeviceAction.value = PendingDeviceActionConfirmation(parsedTool, trimmed)
                } else {
                    executeParsedDeviceAction(parsedTool, trimmed)
                }
            } else if (trimmed.lowercase().let { it.contains("flashlight") || it.contains("torch") }) {
                toggleFlashlight()
            }

            val conv = chatRepository.getConversationById(convId)
            val activeTaskSummary = tasks.value
                .filter { it.status == TaskStatus.RUNNING }
                .joinToString(", ") { "${it.name} (${it.progress}%)" }

            val currentHistory = chatRepository.getRecentMessagesSnapshot(convId, limit = 14)
            val currentMemories = if (memoryEnabled.value) memories.value else emptyList()
            val continuity = aerisAiEngine.continuityEngine.buildSnapshot(
                conversationId = convId,
                existingSummary = conv?.summary.orEmpty(),
                projectContext = conv?.projectContext.orEmpty(),
                history = currentHistory,
                attachedFileName = currentAttachment?.fileName,
                attachedFileExcerpt = currentAttachment?.excerpt,
                activeTasksSummary = activeTaskSummary,
                previousModelId = conv?.activeModelId
            )

            voiceEngineManager.setThinking(true)
            _uiState.value = _uiState.value.copy(
                voiceState = VoiceState.THINKING,
                streamingText = "",
                errorMessage = null
            )

            val stringBuilder = StringBuilder(512)
            var usedModelId = preferredModelId.value ?: GeminiFirebaseProviderAdapter.DEFAULT_GEMINI_MODEL
            var explicitErrorMsg: String? = null
            var lastUiPushMs = 0L

            activeGenerationJob = launch {
                try {
                    aerisAiEngine.generateWithBrain(
                        prompt = trimmed,
                        history = currentHistory,
                        memories = currentMemories,
                        continuity = continuity
                    ).collect { event ->
                        if (event.activeModelId.isNotBlank()) {
                            usedModelId = event.activeModelId
                        }
                        event.routingDecision?.let { decision ->
                            val banner = aerisAiEngine.continuityEngine.buildHandoffBannerText(
                                fromModelId = decision.switchedFromModelId,
                                toModelId = decision.selectedModel.displayName
                            )
                            if (banner.isNotBlank()) {
                                _modelHandoffBanner.value = banner
                            }
                        }
                        if (event.textChunk.isNotEmpty()) {
                            val isFirstToken = stringBuilder.isEmpty()
                            stringBuilder.append(event.textChunk)
                            val now = System.currentTimeMillis()
                            // Show the very first token immediately; coalesce subsequent frames (~35ms) to stop Compose UI thrash
                            if (isFirstToken || (now - lastUiPushMs) >= 35L) {
                                lastUiPushMs = now
                                _uiState.value = _uiState.value.copy(
                                    voiceState = VoiceState.RESPONDING,
                                    streamingText = stringBuilder.toString()
                                )
                            }
                        }
                        if (event.errorMessage != null) {
                            explicitErrorMsg = event.errorMessage
                        }
                    }

                    val finalResponse = stringBuilder.toString()
                    voiceEngineManager.setThinking(false)
                    _uiState.value = _uiState.value.copy(
                        streamingText = null,
                        voiceState = VoiceState.IDLE
                    )

                    if (finalResponse.isNotBlank()) {
                        chatRepository.sendMessage(
                            ChatMessage(
                                conversationId = convId,
                                sender = MessageSender.AERIS,
                                text = finalResponse,
                                modelId = usedModelId
                            )
                        )
                        val updatedHistory = chatRepository.getMessagesSnapshot(convId)
                        chatRepository.updateConversationContinuity(
                            id = convId,
                            summary = aerisAiEngine.continuityEngine.summarizeTurns(updatedHistory),
                            projectContext = conv?.projectContext.orEmpty(),
                            activeModelId = usedModelId
                        )
                        if (currentAttachment != null) {
                            _attachedDocument.value = null
                        }

                        if (voiceResponsesEnabled.value && (isVoiceInitiated || autoPlayResponses.value)) {
                            voiceEngineManager.speakText(finalResponse)
                        }

                        if (isVoiceInitiated) {
                            voiceEngineManager.logVoiceTask(
                                conversationId = convId,
                                transcript = trimmed,
                                responsePreview = finalResponse,
                                status = "SUCCESS",
                                durationMs = System.currentTimeMillis() - startTime
                            )
                        }
                    } else if (explicitErrorMsg != null) {
                        val errText = explicitErrorMsg ?: "Unable to complete response."
                        chatRepository.sendMessage(
                            ChatMessage(
                                conversationId = convId,
                                sender = MessageSender.AERIS,
                                text = errText,
                                modelId = usedModelId,
                                isError = true
                            )
                        )
                        _uiState.value = _uiState.value.copy(errorMessage = errText)
                    }
                } catch (e: CancellationException) {
                    voiceEngineManager.setThinking(false)
                    _uiState.value = _uiState.value.copy(streamingText = null, voiceState = VoiceState.IDLE)
                    throw e
                } catch (e: Exception) {
                    voiceEngineManager.setThinking(false)
                    val errText = e.message ?: "Inference failed"
                    chatRepository.sendMessage(
                        ChatMessage(
                            conversationId = convId,
                            sender = MessageSender.AERIS,
                            text = errText,
                            modelId = usedModelId,
                            isError = true
                        )
                    )
                    _uiState.value = _uiState.value.copy(
                        streamingText = null,
                        voiceState = VoiceState.IDLE,
                        errorMessage = errText
                    )
                    if (isVoiceInitiated) {
                        voiceEngineManager.logVoiceTask(
                            conversationId = convId,
                            transcript = trimmed,
                            responsePreview = "",
                            status = "ERROR",
                            errorMessage = errText,
                            durationMs = System.currentTimeMillis() - startTime
                        )
                    }
                }
            }
        }
    }

    fun regenerateLastResponse() {
        val currentMsgs = _messages.value
        val lastUserMsg = currentMsgs.lastOrNull { it.sender == MessageSender.USER } ?: return
        val lastMsg = currentMsgs.lastOrNull()
        viewModelScope.launch {
            if (lastMsg != null && lastMsg.sender == MessageSender.AERIS) {
                chatRepository.deleteMessage(lastMsg.id)
            }
            sendMessage(lastUserMsg.text)
        }
    }

    fun retryFailedMessage(errorMessage: ChatMessage) {
        val currentMsgs = _messages.value
        val idx = currentMsgs.indexOfFirst { it.id == errorMessage.id }
        val precedingUser = if (idx > 0) {
            currentMsgs.subList(0, idx).lastOrNull { it.sender == MessageSender.USER }
        } else {
            currentMsgs.lastOrNull { it.sender == MessageSender.USER }
        }
        viewModelScope.launch {
            chatRepository.deleteMessage(errorMessage.id)
            if (precedingUser != null) {
                sendMessage(precedingUser.text)
            }
        }
    }

    fun submitMessageFeedback(message: ChatMessage, rating: Int) {
        viewModelScope.launch {
            chatRepository.updateMessageFeedback(message.id, rating)
            val (cat, rule) = aerisAiEngine.feedbackEngine.suggestRuleFromFeedback(rating, message.text)
            aerisAiEngine.feedbackEngine.recordFeedback(
                messageId = message.id,
                rating = rating,
                category = cat,
                ruleText = rule,
                autoApprove = rating > 0
            )
            val toastText = if (rating > 0) "Positive feedback saved, Sir ji" else "Feedback logged — review rule in Settings"
            Toast.makeText(getApplication(), toastText, Toast.LENGTH_SHORT).show()
        }
    }

    fun approveFeedbackRule(id: Long, approved: Boolean) {
        viewModelScope.launch { aerisAiEngine.feedbackEngine.setRuleApproved(id, approved) }
    }

    fun deleteFeedbackRule(id: Long) {
        viewModelScope.launch { aerisAiEngine.feedbackEngine.deleteFeedback(id) }
    }

    fun stopGeneration() {
        activeGenerationJob?.cancel()
        viewModelScope.launch {
            llmEngine.cancelGeneration()
            voiceEngineManager.setThinking(false)
            val partial = _uiState.value.streamingText
            val convId = _uiState.value.currentConversationId
            if (!partial.isNullOrBlank() && convId != null) {
                chatRepository.sendMessage(
                    ChatMessage(
                        conversationId = convId,
                        sender = MessageSender.AERIS,
                        text = "$partial\n\n[Generation stopped by Sir ji]"
                    )
                )
            }
            _uiState.value = _uiState.value.copy(
                streamingText = null,
                voiceState = VoiceState.IDLE
            )
        }
    }

    // --- Device Actions & Confirmation (Stage 5) ---

    fun confirmDeviceAction() {
        val pending = _pendingDeviceAction.value ?: return
        _pendingDeviceAction.value = null
        viewModelScope.launch {
            executeParsedDeviceAction(pending.request, pending.originalPrompt)
        }
    }

    fun dismissDeviceAction() {
        val pending = _pendingDeviceAction.value ?: return
        _pendingDeviceAction.value = null
        recordAction(
            DeviceAction(
                id = "cancelled-${System.currentTimeMillis()}",
                type = pending.request.actionType,
                label = "Cancelled: ${pending.request.detail}",
                detail = "Operator declined sensitive device action",
                status = ActionExecutionStatus.FAILED,
                resultMessage = "Action cancelled by Sir ji."
            )
        )
    }

    private suspend fun executeParsedDeviceAction(
        parsed: ToolTaskExecutionEngine.ParsedToolRequest,
        rawPrompt: String
    ) {
        when (parsed.actionType) {
            ActionType.FLASHLIGHT -> toggleFlashlight(explicitState = parsed.detail == "ON")
            ActionType.TIMER -> {
                val secs = parsed.detail.removeSuffix("s").toIntOrNull() ?: 60
                setQuickTimer(secs)
            }
            ActionType.ALARM -> setQuickAlarm(7, 0)
            ActionType.REMINDER -> {
                val action = deviceActionHandler.createReminder(
                    title = rawPrompt.take(80),
                    delayMinutes = 15,
                    reminderDao = db.reminderDao()
                )
                recordAction(action)
            }
            ActionType.SETTINGS_PAGE -> {
                val action = deviceActionHandler.openSettingsPage(parsed.detail)
                recordAction(action)
            }
            ActionType.BATTERY_CHECK -> recordAction(deviceActionHandler.readBatteryInfo())
            ActionType.STORAGE_INFO -> recordAction(deviceActionHandler.readStorageInfo())
            ActionType.DATE_TIME -> recordAction(deviceActionHandler.readDateTime())
            ActionType.OPEN_APP -> launchApp(parsed.detail)
            else -> Unit
        }
    }

    fun runQuickDeviceCheck(type: ActionType) {
        val action = when (type) {
            ActionType.BATTERY_CHECK -> deviceActionHandler.readBatteryInfo()
            ActionType.STORAGE_INFO -> deviceActionHandler.readStorageInfo()
            ActionType.DATE_TIME -> deviceActionHandler.readDateTime()
            else -> return
        }
        recordAction(action)
        Toast.makeText(getApplication(), action.resultMessage ?: action.label, Toast.LENGTH_LONG).show()
    }

    fun createReminder(title: String, delayMinutes: Int) {
        viewModelScope.launch {
            val action = deviceActionHandler.createReminder(
                title = title,
                delayMinutes = delayMinutes,
                reminderDao = db.reminderDao()
            )
            recordAction(action)
            Toast.makeText(getApplication(), action.resultMessage ?: "Reminder scheduled", Toast.LENGTH_SHORT).show()
        }
    }

    fun markReminderCompleted(id: Long, completed: Boolean) {
        viewModelScope.launch { db.reminderDao().setCompleted(id, completed) }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch { db.reminderDao().deleteById(id) }
    }

    fun openSettingsPage(target: String) {
        val action = deviceActionHandler.openSettingsPage(target)
        recordAction(action)
    }

    // --- Voice Controls ---

    fun speakMessage(text: String) {
        if (!voiceResponsesEnabled.value) {
            _uiState.value = _uiState.value.copy(errorMessage = "Voice Responses are disabled in Voice Settings.")
            return
        }
        voiceEngineManager.speakText(text)
    }

    fun replayLastResponse() {
        if (!voiceResponsesEnabled.value) return
        voiceEngineManager.replayLastResponse()
    }

    fun startVoiceInput() {
        if (!voiceInputEnabled.value) {
            _uiState.value = _uiState.value.copy(errorMessage = "Voice Input is disabled in Voice Settings.")
            return
        }
        deviceActionHandler.vibrateHaptic(30)
        voiceEngineManager.startSttForVoice(
            onPartial = {},
            onFinal = { recognizedText ->
                if (recognizedText.isNotBlank()) {
                    if (!processVoiceTranscriptForModeCommand(recognizedText)) {
                        sendMessage(recognizedText, isVoiceInitiated = true)
                    }
                }
            }
        )
    }

    fun startListeningForComposer(
        onPartial: (String) -> Unit = {},
        onResult: (String) -> Unit
    ) {
        if (!voiceInputEnabled.value) {
            _uiState.value = _uiState.value.copy(errorMessage = "Voice Input is disabled in Voice Settings.")
            return
        }
        deviceActionHandler.vibrateHaptic(30)
        voiceEngineManager.startSttForVoice(
            onPartial = onPartial,
            onFinal = { recognizedText ->
                if (recognizedText.isNotBlank()) {
                    if (!processVoiceTranscriptForModeCommand(recognizedText)) {
                        onResult(recognizedText)
                    }
                }
            }
        )
    }

    fun stopVoiceInput() = voiceEngineManager.stopStt()

    fun cancelVoiceInput() = voiceEngineManager.cancelStt()

    fun sendEditedVoiceTranscript(transcript: String) {
        val trimmed = transcript.trim()
        if (trimmed.isBlank()) return
        if (!processVoiceTranscriptForModeCommand(trimmed)) {
            sendMessage(trimmed, isVoiceInitiated = true)
        }
    }

    fun pauseSpeechPlayback() {
        voiceEngineManager.pauseSpeaking()
    }

    fun resumeSpeechPlayback() {
        voiceEngineManager.resumeSpeaking()
    }

    fun stopSpeechPlayback() {
        voiceEngineManager.stopSpeaking()
        _uiState.value = _uiState.value.copy(voiceState = VoiceState.IDLE)
    }

    fun clearVoiceError() {
        voiceEngineManager.clearError()
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearVoiceFallbackNotice() = voiceSelectionManager.clearFallbackNotice()

    fun clearVoiceTasks() {
        viewModelScope.launch { voiceRepository.clearVoiceTasks() }
    }

    fun onMicrophonePermissionResult(granted: Boolean) {
        if (granted) markPermissionRequested(android.Manifest.permission.RECORD_AUDIO)
        voiceEngineManager.onMicrophonePermissionResult(granted)
    }

    fun toggleFlashlight(explicitState: Boolean? = null) {
        viewModelScope.launch {
            val action = deviceActionHandler.toggleFlashlight(explicitState)
            recordAction(action)
        }
    }

    fun setQuickTimer(seconds: Int = 60) = recordAction(deviceActionHandler.setTimer(seconds, "AERIS Timer"))

    fun setQuickAlarm(hour: Int = 7, min: Int = 0) = recordAction(deviceActionHandler.setAlarm(hour, min, "AERIS Wake Up"))

    fun launchApp(appName: String) = recordAction(deviceActionHandler.launchApp(appName))

    fun adjustVolume(raise: Boolean) = recordAction(deviceActionHandler.adjustMediaVolume(raise))

    private fun recordAction(action: DeviceAction) {
        _uiState.value = _uiState.value.copy(
            lastAction = action,
            actionHistory = listOf(action) + _uiState.value.actionHistory.take(19)
        )
    }

    // --- Memory CRUD ---

    fun saveMemory(key: String, content: String, category: String = "General", projectTag: String = "") {
        viewModelScope.launch {
            val sensitive = aerisAiEngine.longTermMemoryEngine.looksSensitive(key, content)
            memoryRepository.saveMemory(
                MemoryItem(
                    key = key,
                    content = content,
                    category = category,
                    projectTag = projectTag,
                    isSensitive = sensitive
                )
            )
        }
    }

    fun updateMemory(item: MemoryItem) {
        viewModelScope.launch {
            val sensitive = aerisAiEngine.longTermMemoryEngine.looksSensitive(item.key, item.content)
            memoryRepository.updateMemory(item.copy(isSensitive = sensitive))
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch { memoryRepository.deleteMemory(id) }
    }

    fun clearAllMemories() {
        viewModelScope.launch { memoryRepository.clearAllMemories() }
    }

    // --- Notification Announcer & Safety Controls ---

    fun setDeviceActionConfirmSensitive(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDeviceActionConfirmSensitive(enabled) }
    }

    fun setNotificationAnnouncerEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationAnnouncerEnabled(enabled) }
    }

    fun toggleNotificationAllowedApp(packageName: String) {
        viewModelScope.launch {
            val current = notificationAllowedApps.value.toMutableSet()
            if (packageName in current) current.remove(packageName) else current.add(packageName)
            settingsRepository.setNotificationAllowedApps(current)
        }
    }

    fun setNotificationHideSensitive(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationHideSensitive(enabled) }
    }

    fun setGameCoachCloudAnalysisEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setGameCoachCloudAnalysis(enabled) }
    }

    fun setBatterySaverHolo(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBatterySaverHolo(enabled) }
    }

    fun setWakeWordPrivacyMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setWakeWordPrivacyMode(enabled) }
    }

    fun setStartOnBootOptIn(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setStartOnBootOptIn(enabled) }
    }

    fun setFloatingAssistantEnabled(enabled: Boolean): Boolean {
        val ctx = getApplication<Application>()
        return if (enabled) {
            val started = com.example.device.FloatingAssistantService.start(ctx)
            viewModelScope.launch { settingsRepository.setFloatingAssistantEnabled(started) }
            refreshPermissionStates()
            started
        } else {
            com.example.device.FloatingAssistantService.stop(ctx)
            viewModelScope.launch { settingsRepository.setFloatingAssistantEnabled(false) }
            true
        }
    }

    fun setCustomSystemInstruction(instruction: String) {
        viewModelScope.launch { settingsRepository.setCustomSystemInstruction(instruction) }
    }

    fun recordFeatureUsage(featureId: String) {
        viewModelScope.launch { settingsRepository.incrementFeatureUsage(featureId) }
    }

    fun buildPrivacyExportJson(): String {
        val root = org.json.JSONObject()
        root.put("app", "AERIS AI")
        root.put("exportedAtMillis", System.currentTimeMillis())
        root.put("privacyNote", "API keys and KeyStore credentials are intentionally excluded from exports.")

        val convArr = org.json.JSONArray()
        conversations.value.forEach { c ->
            convArr.put(
                org.json.JSONObject()
                    .put("id", c.id)
                    .put("title", c.title)
                    .put("messageCount", c.messageCount)
                    .put("projectContext", c.projectContext)
                    .put("summary", c.summary)
                    .put("updatedAt", c.updatedAt)
            )
        }
        root.put("conversations", convArr)

        val memArr = org.json.JSONArray()
        memories.value.forEach { m ->
            memArr.put(
                org.json.JSONObject()
                    .put("id", m.id)
                    .put("key", m.key)
                    .put("category", m.category)
                    .put("content", if (m.isSensitive) "[REDACTED_SENSITIVE]" else m.content)
                    .put("projectTag", m.projectTag)
            )
        }
        root.put("memories", memArr)

        val remArr = org.json.JSONArray()
        reminders.value.forEach { r ->
            remArr.put(
                org.json.JSONObject()
                    .put("id", r.id)
                    .put("title", r.title)
                    .put("triggerAtMillis", r.triggerAtMillis)
                    .put("isCompleted", r.isCompleted)
            )
        }
        root.put("reminders", remArr)

        val projArr = org.json.JSONArray()
        projects.value.forEach { p ->
            projArr.put(
                org.json.JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("progressPercent", p.progressPercent)
                    .put("summary", p.summary)
            )
        }
        root.put("projects", projArr)
        return root.toString(2)
    }

    // --- Other Settings ---

    fun setAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAnimationsEnabled(enabled) }
    }

    fun setVisualEffectsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVisualEffectsEnabled(enabled) }
    }

    fun setMemoryEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMemoryEnabled(enabled) }
    }

    fun setSpeechRate(rate: Float) {
        viewModelScope.launch {
            voiceRepository.setSpeechRate(rate)
            androidTtsEngine.setSpeechRate(rate)
        }
    }

    fun setSpeechPitch(pitch: Float) {
        viewModelScope.launch {
            voiceRepository.setSpeechPitch(pitch)
            androidTtsEngine.setPitch(pitch)
        }
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        viewModelScope.launch {
            voiceRepository.setWakeWordEnabled(enabled)
            if (enabled) wakeWordManager.startListening() else wakeWordManager.stopListening()
        }
    }

    fun setBackgroundListeningEnabled(enabled: Boolean) {
        viewModelScope.launch {
            voiceRepository.setBackgroundListeningEnabled(enabled)
            if (!enabled && wakeWordManager.isServiceRunning.value) {
                wakeWordManager.stopListening()
                voiceRepository.setWakeWordEnabled(false)
            }
        }
    }

    fun switchTtsEngine(packageName: String) {
        viewModelScope.launch { voiceSelectionManager.switchTtsEngine(packageName) }
    }

    fun selectVoice(voice: AerisVoice) {
        viewModelScope.launch { voiceSelectionManager.selectVoice(voice) }
    }

    fun previewVoice(voice: AerisVoice, sampleText: String = "Hello Sir ji, I am AERIS. How may I assist you today?") {
        androidTtsEngine.previewVoice(voice, sampleText)
    }

    fun refreshVoices() = androidTtsEngine.refreshVoices()

    fun setVoiceResponsesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            voiceRepository.setVoiceResponsesEnabled(enabled)
            if (!enabled) voiceEngineManager.stopSpeaking()
        }
    }

    fun setAutoPlayResponses(enabled: Boolean) {
        viewModelScope.launch { voiceRepository.setAutoPlayResponses(enabled) }
    }

    fun setVoiceInputEnabled(enabled: Boolean) {
        viewModelScope.launch {
            voiceRepository.setVoiceInputEnabled(enabled)
            if (!enabled && androidSttEngine.isListening.value) voiceEngineManager.cancelStt()
        }
    }

    fun setPushToTalkEnabled(enabled: Boolean) {
        viewModelScope.launch { voiceRepository.setPushToTalkEnabled(enabled) }
    }

    fun setRecognitionLanguage(lang: String) {
        viewModelScope.launch {
            voiceRepository.setRecognitionLanguage(lang)
            androidSttEngine.setRecognitionLanguage(lang)
        }
    }

    fun setWakeWordPhrase(phrase: String) = wakeWordManager.setPhrase(phrase)

    fun resetVoiceSettingsToDefaults() {
        viewModelScope.launch {
            setSpeechRate(1.0f)
            setSpeechPitch(1.0f)
            setVoiceResponsesEnabled(true)
            setAutoPlayResponses(false)
            setVoiceInputEnabled(true)
            setPushToTalkEnabled(false)
            setRecognitionLanguage("en-US")
            setWakeWordPhrase("Hey AERIS")
            setWakeWordEnabled(false)
            setBackgroundListeningEnabled(false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngineManager.release()
    }
}
