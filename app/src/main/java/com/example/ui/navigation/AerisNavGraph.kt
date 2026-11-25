package com.example.ui.navigation

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.domain.model.ActionType
import com.example.domain.model.OperatingMode
import com.example.features.FeatureRouteTarget
import com.example.ui.MainViewModel
import com.example.ui.NavigationEvent
import com.example.ui.chat.ChatScreen
import com.example.ui.components.AerisBottomNavBar
import com.example.ui.components.MainNavTab
import com.example.ui.customize.CustomizeScreen
import com.example.ui.features.FeaturesScreen
import com.example.ui.history.ChatHistoryScreen
import com.example.ui.home.HomeScreen
import com.example.ui.library.LibraryScreen
import com.example.ui.memory.MemoryScreen
import com.example.ui.modelmanager.ModelManagerScreen
import com.example.ui.privacy.PrivacyCenterScreen
import com.example.ui.settings.PermissionManagerScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.studio.ImageStudioScreen
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.HoloBlack
import com.example.ui.theme.HoloSurface
import com.example.ui.theme.TextCoolGrey
import com.example.ui.theme.TextWhite
import com.example.ui.theme.Typography
import com.example.ui.tracker.FeatureTrackerScreen
import com.example.ui.work.WorkModeScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AerisDestinations {
    const val HOME = "home"
    const val CHAT = "chat"
    const val FEATURES = "features"
    const val MEMORY = "memory"
    const val SETTINGS = "settings"
    const val WORK_MODE = "work_mode"
    const val HISTORY = "history"
    const val MODELS = "models"
    const val TRACKER = "tracker"
    const val WEB_ASSISTANT = "web_assistant"
    const val GAME_COACH = "game_coach"
    const val IMAGE_STUDIO = "image_studio"
    const val VOICE_ASSISTANT = "voice_assistant"
    const val VOICE_SELECTION = "voice_selection"
    const val VOICE_SETTINGS = "voice_settings"
    const val NOTIFICATIONS = "notifications"
    const val AI_PROVIDERS = "ai_providers"
    const val API_KEY_ENTRY = "api_key_entry/{provider}"
    const val PROJECTS = "projects"
    const val PERMISSIONS = "permissions"
    const val PRIVACY_CENTER = "privacy_center"
    const val LIBRARY = "library"
    const val CUSTOMIZE = "customize"

    fun apiKeyEntryRoute(provider: com.example.ai.AiProviderType): String =
        "api_key_entry/${provider.name}"
}

val HairaDestinations = AerisDestinations

@Composable
fun AerisNavGraph(
    viewModel: MainViewModel,
    launchRoute: String? = null,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val uiState by viewModel.uiState.collectAsState()
    val featureUsageCounts by viewModel.featureUsageCounts.collectAsState()
    val pendingConfirmation by viewModel.pendingModeConfirmation.collectAsState()
    val modeSwitchBanner by viewModel.modeSwitchBanner.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AerisDestinations.HOME

    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val primaryTabs = listOf(
        AerisDestinations.HOME,
        AerisDestinations.CHAT,
        AerisDestinations.FEATURES,
        AerisDestinations.MEMORY,
        AerisDestinations.SETTINGS
    )
    val isPrimaryTab = currentRoute in primaryTabs
    val showBottomBar = isPrimaryTab && !isLandscape && !isImeVisible

    val selectedTab = when (currentRoute) {
        AerisDestinations.CHAT -> MainNavTab.CHAT
        AerisDestinations.FEATURES -> MainNavTab.FEATURES
        AerisDestinations.MEMORY -> MainNavTab.MEMORY
        AerisDestinations.SETTINGS -> MainNavTab.SETTINGS
        else -> MainNavTab.HOME
    }

    val navigateSingleTop: (String) -> Unit = { target ->
        if (target != currentRoute) {
            if (target in primaryTabs) {
                navController.navigate(target) {
                    popUpTo(AerisDestinations.HOME) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            } else {
                navController.navigate(target) { launchSingleTop = true }
            }
        }
    }

    LaunchedEffect(launchRoute) {
        when (launchRoute) {
            AerisDestinations.CHAT -> navController.navigate(AerisDestinations.CHAT) { launchSingleTop = true }
            AerisDestinations.VOICE_ASSISTANT -> navController.navigate(AerisDestinations.VOICE_ASSISTANT) { launchSingleTop = true }
        }
    }

    LaunchedEffect(navController, viewModel) {
        viewModel.navigationEvents.collect { event ->
            val activeRoute = navController.currentDestination?.route
            when (event) {
                NavigationEvent.EnterWorkMode -> {
                    scope.launch { drawerState.close() }
                    if (activeRoute != AerisDestinations.WORK_MODE) {
                        navController.navigate(AerisDestinations.WORK_MODE) { launchSingleTop = true }
                    }
                }
                is NavigationEvent.ExitWorkMode -> {
                    if (event.returnToHome) {
                        navController.navigate(AerisDestinations.HOME) {
                            popUpTo(AerisDestinations.HOME) { inclusive = false }
                            launchSingleTop = true
                        }
                    } else if (activeRoute == AerisDestinations.WORK_MODE) {
                        val popped = navController.popBackStack()
                        if (!popped) {
                            navController.navigate(AerisDestinations.HOME) {
                                popUpTo(AerisDestinations.HOME) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(isLandscape, uiState.operatingMode) {
        if (uiState.operatingMode == OperatingMode.AUTO) {
            val activeRoute = navController.currentDestination?.route
            if (isLandscape && activeRoute in primaryTabs && activeRoute != AerisDestinations.CHAT) {
                navController.navigate(AerisDestinations.WORK_MODE) { launchSingleTop = true }
            } else if (!isLandscape && activeRoute == AerisDestinations.WORK_MODE) {
                navController.popBackStack()
            }
        }
    }

    LaunchedEffect(modeSwitchBanner) {
        if (modeSwitchBanner != null) {
            delay(2600)
            viewModel.clearModeSwitchBanner()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute != AerisDestinations.WORK_MODE,
        drawerContent = {
            AerisSideDrawerContent(
                viewModel = viewModel,
                onCloseDrawer = { scope.launch { drawerState.close() } },
                onNavigateToRoute = { route -> navigateSingleTop(route) },
                onNavigateToWorkMode = { viewModel.enterWorkMode() }
            )
        }
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = HoloBlack,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (showBottomBar) {
                        AerisBottomNavBar(
                            selectedTab = selectedTab,
                            onTabSelected = { tab ->
                                val targetRoute = when (tab) {
                                    MainNavTab.HOME -> AerisDestinations.HOME
                                    MainNavTab.CHAT -> AerisDestinations.CHAT
                                    MainNavTab.FEATURES -> AerisDestinations.FEATURES
                                    MainNavTab.MEMORY -> AerisDestinations.MEMORY
                                    MainNavTab.SETTINGS -> AerisDestinations.SETTINGS
                                }
                                navigateSingleTop(targetRoute)
                            }
                        )
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = AerisDestinations.HOME,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                ) {
                    composable(AerisDestinations.HOME) {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToChat = { navigateSingleTop(AerisDestinations.CHAT) },
                            onNavigateToWorkMode = { viewModel.enterWorkMode() },
                            onNavigateToFeatures = { navigateSingleTop(AerisDestinations.FEATURES) },
                            onNavigateToMemory = { navigateSingleTop(AerisDestinations.MEMORY) },
                            onNavigateToSettings = { navigateSingleTop(AerisDestinations.SETTINGS) },
                            onNavigateToVoiceAssistant = { navController.navigate(AerisDestinations.VOICE_ASSISTANT) },
                            onNavigateToNotifications = { navController.navigate(AerisDestinations.NOTIFICATIONS) },
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onSubmitPromptToChat = { prompt ->
                                viewModel.sendMessage(prompt)
                                navigateSingleTop(AerisDestinations.CHAT)
                            },
                            onNavigateToImageStudio = { navController.navigate(AerisDestinations.IMAGE_STUDIO) },
                            onNavigateToWebAssistant = { navController.navigate(AerisDestinations.WEB_ASSISTANT) },
                            onNavigateToLibrary = { navController.navigate(AerisDestinations.LIBRARY) }
                        )
                    }

                    composable(AerisDestinations.CHAT) {
                        BackHandler {
                            navController.navigate(AerisDestinations.HOME) {
                                popUpTo(AerisDestinations.HOME) { inclusive = true }
                            }
                        }
                        ChatScreen(
                            viewModel = viewModel,
                            onNavigateToHistory = { navController.navigate(AerisDestinations.HISTORY) },
                            onNavigateToVoiceAssistant = { navController.navigate(AerisDestinations.VOICE_ASSISTANT) },
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onNavigateToLibrary = { navController.navigate(AerisDestinations.LIBRARY) },
                            onNavigateToFeatures = { navigateSingleTop(AerisDestinations.FEATURES) }
                        )
                    }

                    composable(AerisDestinations.FEATURES) {
                        BackHandler {
                            navController.navigate(AerisDestinations.HOME) {
                                popUpTo(AerisDestinations.HOME) { inclusive = true }
                            }
                        }
                        FeaturesScreen(
                            onNavigateBack = {
                                navController.navigate(AerisDestinations.HOME) {
                                    popUpTo(AerisDestinations.HOME) { inclusive = true }
                                }
                            },
                            onNavigateToChat = { navigateSingleTop(AerisDestinations.CHAT) },
                            onLaunchPromptInChat = { prompt ->
                                viewModel.sendMessage(prompt)
                                navigateSingleTop(AerisDestinations.CHAT)
                            },
                            onNavigateToWorkMode = { viewModel.enterWorkMode() },
                            onNavigateToWebAssistant = { navController.navigate(AerisDestinations.WEB_ASSISTANT) },
                            onNavigateToGameCoach = { navController.navigate(AerisDestinations.GAME_COACH) },
                            onNavigateToImageStudio = { navController.navigate(AerisDestinations.IMAGE_STUDIO) },
                            onNavigateToVoiceAssistant = { navController.navigate(AerisDestinations.VOICE_ASSISTANT) },
                            onNavigateToVoiceSelection = { navController.navigate(AerisDestinations.VOICE_SELECTION) },
                            onNavigateToVoiceSettings = { navController.navigate(AerisDestinations.VOICE_SETTINGS) },
                            onNavigateToNotifications = { navController.navigate(AerisDestinations.NOTIFICATIONS) },
                            onNavigateToModels = { navController.navigate(AerisDestinations.MODELS) },
                            onNavigateToAiProviders = { navController.navigate(AerisDestinations.AI_PROVIDERS) },
                            onNavigateToMemory = { navigateSingleTop(AerisDestinations.MEMORY) },
                            onNavigateToProjects = { navController.navigate(AerisDestinations.PROJECTS) },
                            onNavigateToPermissions = { navController.navigate(AerisDestinations.PERMISSIONS) },
                            onNavigateToPrivacyCenter = { navController.navigate(AerisDestinations.PRIVACY_CENTER) },
                            onNavigateToLibrary = { navController.navigate(AerisDestinations.LIBRARY) },
                            onNavigateToCustomize = { navController.navigate(AerisDestinations.CUSTOMIZE) },
                            onNavigateToSettings = { navigateSingleTop(AerisDestinations.SETTINGS) },
                            onNavigateToTracker = { navController.navigate(AerisDestinations.TRACKER) },
                            onExecuteQuickFeatureAction = { target ->
                                when (target) {
                                    FeatureRouteTarget.TOGGLE_FLASHLIGHT -> viewModel.toggleFlashlight()
                                    FeatureRouteTarget.CHECK_BATTERY -> viewModel.runQuickDeviceCheck(ActionType.BATTERY_CHECK)
                                    FeatureRouteTarget.CHECK_STORAGE -> viewModel.runQuickDeviceCheck(ActionType.STORAGE_INFO)
                                    FeatureRouteTarget.CHECK_DATETIME -> viewModel.runQuickDeviceCheck(ActionType.DATE_TIME)
                                    FeatureRouteTarget.SET_TIMER_60S -> viewModel.setQuickTimer(60)
                                    FeatureRouteTarget.SET_ALARM_7AM -> viewModel.setQuickAlarm(7, 0)
                                    FeatureRouteTarget.FLOATING_ASSISTANT -> navController.navigate(AerisDestinations.PERMISSIONS)
                                    else -> Unit
                                }
                            },
                            featureUsageCounts = featureUsageCounts,
                            onRecordFeatureUsage = { featureId -> viewModel.recordFeatureUsage(featureId) }
                        )
                    }

                    composable(AerisDestinations.MEMORY) {
                        val cameFromWorkMode =
                            navController.previousBackStackEntry?.destination?.route == AerisDestinations.WORK_MODE
                        BackHandler {
                            if (cameFromWorkMode) {
                                navController.popBackStack()
                            } else {
                                navController.navigate(AerisDestinations.HOME) {
                                    popUpTo(AerisDestinations.HOME) { inclusive = true }
                                }
                            }
                        }
                        MemoryScreen(
                            viewModel = viewModel,
                            showBackButton = cameFromWorkMode || isLandscape,
                            onBack = {
                                if (cameFromWorkMode) navController.popBackStack()
                                else navController.navigate(AerisDestinations.HOME) {
                                    popUpTo(AerisDestinations.HOME) { inclusive = true }
                                }
                            },
                            onResumeConversation = { convId, title ->
                                viewModel.selectConversation(convId, title)
                                navigateSingleTop(AerisDestinations.CHAT)
                            },
                            onNavigateToProjects = { navController.navigate(AerisDestinations.PROJECTS) }
                        )
                    }

                    composable(AerisDestinations.SETTINGS) {
                        val cameFromWorkMode =
                            navController.previousBackStackEntry?.destination?.route == AerisDestinations.WORK_MODE
                        val returnAction = {
                            if (cameFromWorkMode) {
                                navController.popBackStack()
                            } else {
                                navController.navigate(AerisDestinations.HOME) {
                                    popUpTo(AerisDestinations.HOME) { inclusive = true }
                                }
                            }
                            Unit
                        }
                        BackHandler(onBack = returnAction)
                        SettingsScreen(
                            viewModel = viewModel,
                            showBackButton = cameFromWorkMode || isLandscape,
                            onBack = returnAction,
                            onNavigateToTracker = { navController.navigate(AerisDestinations.TRACKER) },
                            onNavigateToMemory = { navigateSingleTop(AerisDestinations.MEMORY) },
                            onNavigateToModels = { navController.navigate(AerisDestinations.MODELS) },
                            onNavigateToAiProviders = { navController.navigate(AerisDestinations.AI_PROVIDERS) },
                            onNavigateToNotifications = { navController.navigate(AerisDestinations.NOTIFICATIONS) },
                            onNavigateToVoiceSettings = { navController.navigate(AerisDestinations.VOICE_SETTINGS) },
                            onNavigateToPermissions = { navController.navigate(AerisDestinations.PERMISSIONS) },
                            onNavigateToPrivacyCenter = { navController.navigate(AerisDestinations.PRIVACY_CENTER) },
                            onNavigateToCustomize = { navController.navigate(AerisDestinations.CUSTOMIZE) }
                        )
                    }

                    composable(AerisDestinations.WORK_MODE) {
                        BackHandler { viewModel.exitWorkMode(returnToHome = false) }
                        WorkModeScreen(
                            viewModel = viewModel,
                            onNavigateToNormalMode = { viewModel.exitWorkMode(returnToHome = false) },
                            onNavigateToHistory = { navController.navigate(AerisDestinations.HISTORY) },
                            onNavigateToMemory = { navController.navigate(AerisDestinations.MEMORY) },
                            onNavigateToSettings = { navController.navigate(AerisDestinations.SETTINGS) },
                            onNavigateToProjects = { navController.navigate(AerisDestinations.PROJECTS) },
                            onNavigateToImageStudio = { navController.navigate(AerisDestinations.IMAGE_STUDIO) },
                            onNavigateToModels = { navController.navigate(AerisDestinations.MODELS) },
                            onNavigateToGameCoach = { navController.navigate(AerisDestinations.GAME_COACH) },
                            onNavigateToPermissions = { navController.navigate(AerisDestinations.PERMISSIONS) },
                            onNavigateToVoiceAssistant = { navController.navigate(AerisDestinations.VOICE_ASSISTANT) }
                        )
                    }

                    composable(AerisDestinations.HISTORY) {
                        BackHandler { navController.popBackStack() }
                        ChatHistoryScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onSelectConversation = { navigateSingleTop(AerisDestinations.CHAT) }
                        )
                    }

                    composable(AerisDestinations.MODELS) {
                        BackHandler { navController.popBackStack() }
                        ModelManagerScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToAiProviders = { navController.navigate(AerisDestinations.AI_PROVIDERS) }
                        )
                    }

                    composable(AerisDestinations.AI_PROVIDERS) {
                        BackHandler { navController.popBackStack() }
                        com.example.ui.settings.AiProvidersScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onOpenApiKeyEntry = { providerType ->
                                navController.navigate(AerisDestinations.apiKeyEntryRoute(providerType))
                            },
                            onNavigateToLocalModels = { navController.navigate(AerisDestinations.MODELS) }
                        )
                    }

                    composable(AerisDestinations.API_KEY_ENTRY) { entry ->
                        BackHandler { navController.popBackStack() }
                        val rawProvider = entry.arguments?.getString("provider").orEmpty()
                        val providerType = try {
                            com.example.ai.AiProviderType.valueOf(rawProvider)
                        } catch (_: Exception) {
                            com.example.ai.AiProviderType.OPENROUTER
                        }
                        com.example.ui.settings.ApiKeyEntryScreen(
                            viewModel = viewModel,
                            providerType = providerType,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(AerisDestinations.PROJECTS) {
                        BackHandler { navController.popBackStack() }
                        com.example.ui.projects.ProjectWorkspaceScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onOpenChat = { navigateSingleTop(AerisDestinations.CHAT) }
                        )
                    }

                    composable(AerisDestinations.PERMISSIONS) {
                        BackHandler { navController.popBackStack() }
                        PermissionManagerScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(AerisDestinations.PRIVACY_CENTER) {
                        BackHandler { navController.popBackStack() }
                        PrivacyCenterScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onOpenPermissions = { navController.navigate(AerisDestinations.PERMISSIONS) }
                        )
                    }

                    composable(AerisDestinations.LIBRARY) {
                        BackHandler { navController.popBackStack() }
                        LibraryScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onOpenChat = { navigateSingleTop(AerisDestinations.CHAT) },
                            onOpenImageStudio = { navController.navigate(AerisDestinations.IMAGE_STUDIO) }
                        )
                    }

                    composable(AerisDestinations.CUSTOMIZE) {
                        BackHandler { navController.popBackStack() }
                        CustomizeScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToVoiceSettings = { navController.navigate(AerisDestinations.VOICE_SETTINGS) }
                        )
                    }

                    composable(AerisDestinations.TRACKER) {
                        BackHandler { navController.popBackStack() }
                        FeatureTrackerScreen(onBack = { navController.popBackStack() })
                    }

                    composable(AerisDestinations.WEB_ASSISTANT) {
                        BackHandler { navController.popBackStack() }
                        com.example.ui.web.WebAssistantScreen(onBack = { navController.popBackStack() })
                    }

                    composable(AerisDestinations.GAME_COACH) {
                        BackHandler { navController.popBackStack() }
                        com.example.ui.coach.GameCoachScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                    }

                    composable(AerisDestinations.IMAGE_STUDIO) {
                        BackHandler { navController.popBackStack() }
                        ImageStudioScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                    }

                    composable(AerisDestinations.VOICE_ASSISTANT) {
                        BackHandler { navController.popBackStack() }
                        com.example.ui.voice.VoiceAssistantScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToSettings = { navController.navigate(AerisDestinations.VOICE_SETTINGS) },
                            onNavigateToVoiceSelection = { navController.navigate(AerisDestinations.VOICE_SELECTION) }
                        )
                    }

                    composable(AerisDestinations.VOICE_SELECTION) {
                        BackHandler { navController.popBackStack() }
                        com.example.ui.voice.VoiceSelectionScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToVoiceSettings = { navController.navigate(AerisDestinations.VOICE_SETTINGS) }
                        )
                    }

                    composable(AerisDestinations.VOICE_SETTINGS) {
                        BackHandler { navController.popBackStack() }
                        com.example.ui.voice.VoiceSettingsScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToVoiceSelection = { navController.navigate(AerisDestinations.VOICE_SELECTION) }
                        )
                    }

                    composable(AerisDestinations.NOTIFICATIONS) {
                        BackHandler { navController.popBackStack() }
                        com.example.ui.notifications.NotificationCenterScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToModels = { navController.navigate(AerisDestinations.MODELS) },
                            onNavigateToWebAssistant = { navController.navigate(AerisDestinations.WEB_ASSISTANT) }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = modeSwitchBanner != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = HoloSurface.copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, CyanNeon),
                    modifier = Modifier
                        .clickable { viewModel.clearModeSwitchBanner() }
                        .testTag("mode_switch_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.ScreenRotation, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = modeSwitchBanner.orEmpty(),
                            style = Typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            color = TextWhite
                        )
                    }
                }
            }

            pendingConfirmation?.let { pending ->
                AlertDialog(
                    onDismissRequest = { viewModel.dismissPendingModeSwitch() },
                    title = {
                        Text(
                            text = "CONFIRM MODE SWITCH",
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = CyanNeon
                        )
                    },
                    text = { Text(pending.promptMessage, style = Typography.bodyMedium, color = TextWhite) },
                    confirmButton = {
                        TextButton(
                            onClick = { viewModel.confirmPendingModeSwitch() },
                            modifier = Modifier.testTag("confirm_mode_switch_button")
                        ) {
                            Text("Switch to ${pending.targetMode.title}", color = CyanNeon)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.dismissPendingModeSwitch() },
                            modifier = Modifier.testTag("cancel_mode_switch_button")
                        ) {
                            Text("Cancel", color = TextCoolGrey)
                        }
                    },
                    containerColor = HoloSurface,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}
