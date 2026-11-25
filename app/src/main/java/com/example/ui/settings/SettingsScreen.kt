package com.example.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.MainViewModel
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.HoloBlack
import com.example.ui.theme.HoloCardBorder
import com.example.ui.theme.HoloSurface
import com.example.ui.theme.HoloSurfaceVariant
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextCoolGrey
import com.example.ui.theme.TextWhite
import com.example.ui.theme.Typography

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
    onNavigateToTracker: () -> Unit = {},
    onNavigateToMemory: () -> Unit = {},
    onNavigateToModels: () -> Unit = {},
    onNavigateToAiProviders: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToVoiceSettings: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToPrivacyCenter: () -> Unit = {},
    onNavigateToCustomize: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val aiEngineState by viewModel.aiEngineState.collectAsState()
    val freeOnlyMode by viewModel.freeOnlyMode.collectAsState()
    val aiOperatingMode by viewModel.aiOperatingMode.collectAsState()
    val personalityStyle by viewModel.personalityStyle.collectAsState()
    val responseLength by viewModel.responseLength.collectAsState()
    val animationsEnabled by viewModel.animationsEnabled.collectAsState()
    val visualEffectsEnabled by viewModel.visualEffectsEnabled.collectAsState()
    val batterySaverHolo by viewModel.batterySaverHolo.collectAsState()
    val deviceActionConfirmSensitive by viewModel.deviceActionConfirmSensitive.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val allFeedback by viewModel.allFeedback.collectAsState()
    val permissionItems by viewModel.permissionItems.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var activeDialog by remember { mutableStateOf<SettingsDialogType?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val userName = memories.find {
        it.key.equals("username", ignoreCase = true) || it.key.equals("name", ignoreCase = true)
    }?.content ?: "Ayyan Mukadam"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HoloBlack)
            .statusBarsPadding()
            .displayCutoutPadding()
            .then(if (isLandscape) Modifier.navigationBarsPadding() else Modifier)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("settings_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showBackButton) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(34.dp)
                            .background(HoloSurfaceVariant, CircleShape)
                            .border(1.dp, CyanGlow, CircleShape)
                            .testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CyanNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(HoloSurfaceVariant, CircleShape)
                            .border(1.dp, CyanGlow, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = CyanNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = "Settings",
                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    color = TextWhite
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Profile Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = HoloSurface,
            border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(HoloSurfaceVariant, CircleShape)
                        .border(1.5.dp, CyanNeon, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = "User Profile", tint = CyanBright, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = userName,
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(StatusSuccess, CircleShape))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${aiOperatingMode.title} • ${aiEngineState.modelId}",
                            style = Typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            color = StatusSuccess
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Settings Category List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                val orientationLabel = if (isLandscape) "Landscape" else "Portrait"
                SettingsListItem(
                    icon = Icons.Default.ScreenRotation,
                    title = "Display & Work Mode",
                    subtitle = "${uiState.operatingMode.title} • $orientationLabel",
                    onClick = { activeDialog = SettingsDialogType.OPERATING_MODE },
                    modifier = Modifier.testTag("settings_mode_switch_item")
                )
            }

            item {
                SettingsListItem(
                    icon = Icons.Default.ColorLens,
                    title = "Appearance & Performance",
                    subtitle = if (batterySaverHolo) "Cyan+Purple Theme • Battery Saver" else "Cyan+Purple Holographic Theme",
                    onClick = { activeDialog = SettingsDialogType.APPEARANCE }
                )
            }

            item {
                SettingsListItem(
                    icon = Icons.Default.Key,
                    title = "AI Providers & API Keys",
                    subtitle = "${aiOperatingMode.title} • Official Key Setup & Routing",
                    onClick = onNavigateToAiProviders,
                    modifier = Modifier.testTag("settings_ai_providers_item")
                )
            }

            item {
                SettingsListItem(
                    icon = Icons.Default.Psychology,
                    title = "AI Brain & Model Manager",
                    subtitle = "${aiEngineState.modelId} • ${if (freeOnlyMode) "Free-Only ON" else "Blaze Opt-In"}",
                    onClick = onNavigateToModels,
                    modifier = Modifier.testTag("settings_ai_models_item")
                )
            }

            item {
                SettingsListItem(
                    icon = Icons.Default.RecordVoiceOver,
                    title = "AERIS Personality & Style",
                    subtitle = "${personalityStyle.title} • ${responseLength.title}",
                    onClick = { activeDialog = SettingsDialogType.PERSONALITY },
                    modifier = Modifier.testTag("settings_personality_item")
                )
            }

            item {
                SettingsListItem(
                    icon = Icons.Default.VolumeUp,
                    title = "Voice Settings",
                    subtitle = "STT, TTS, Voices & Wake Word",
                    onClick = onNavigateToVoiceSettings
                )
            }

            item {
                SettingsListItem(
                    icon = Icons.Default.Memory,
                    title = "Memory Settings",
                    subtitle = "${memories.size} Saved Memories",
                    onClick = onNavigateToMemory
                )
            }

            item {
                val approvedCount = allFeedback.count { it.isApproved }
                SettingsListItem(
                    icon = Icons.Default.Rule,
                    title = "Learned Feedback Rules",
                    subtitle = "$approvedCount approved / ${allFeedback.size} logged",
                    onClick = { activeDialog = SettingsDialogType.FEEDBACK_RULES }
                )
            }

            item {
                SettingsListItem(
                    icon = Icons.Default.VerifiedUser,
                    title = "Device Action Safety",
                    subtitle = if (deviceActionConfirmSensitive) "Confirm before opening apps/settings" else "Direct execution enabled",
                    onClick = { activeDialog = SettingsDialogType.SAFETY_ACTION_CONFIRM }
                )
            }

            item {
                SettingsListItem(
                    icon = Icons.Default.Tune,
                    title = "Customize & Custom Instructions",
                    subtitle = "Personality, Response Style & System Instructions",
                    onClick = onNavigateToCustomize,
                    modifier = Modifier.testTag("settings_customize_item")
                )
            }

            item {
                SettingsListItem(
                    icon = Icons.Default.Security,
                    title = "Permissions Manager",
                    subtitle = "${permissionItems.count { it.status.name == "GRANTED" }} / ${permissionItems.size} granted • Floating Orb",
                    onClick = {
                        viewModel.refreshPermissionStates()
                        onNavigateToPermissions()
                    },
                    modifier = Modifier.testTag("settings_permissions_item")
                )
            }

            item {
                SettingsListItem(
                    icon = Icons.Default.Lock,
                    title = "Privacy & Data Control Center",
                    subtitle = "Export JSON Backup, Wipe Memory & Chat History",
                    onClick = onNavigateToPrivacyCenter,
                    modifier = Modifier.testTag("settings_privacy_item")
                )
            }

            item {
                SettingsListItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications & Reminders",
                    subtitle = "App Announcer, OTP Filter & Alerts",
                    onClick = onNavigateToNotifications
                )
            }

            item {
                SettingsListItem(
                    icon = Icons.Default.Build,
                    title = "Diagnostics & Feature Tracker",
                    subtitle = "Module Readiness & Redmi Telemetry",
                    onClick = onNavigateToTracker
                )
            }

            item {
                SettingsListItem(
                    icon = Icons.Default.Info,
                    title = "About AERIS AI",
                    subtitle = "Version 1.0.0",
                    onClick = { activeDialog = SettingsDialogType.ABOUT }
                )
            }
        }
    }

    SettingsDialogHost(
        activeDialog = activeDialog,
        onDismiss = { activeDialog = null },
        viewModel = viewModel,
        currentMode = uiState.operatingMode,
        animationsEnabled = animationsEnabled,
        visualEffectsEnabled = visualEffectsEnabled,
        batterySaverHolo = batterySaverHolo,
        deviceActionConfirmSensitive = deviceActionConfirmSensitive,
        feedbackRules = allFeedback,
        permissionItems = permissionItems
    )
}

@Composable
private fun SettingsListItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = HoloSurface,
        border = BorderStroke(1.dp, HoloCardBorder.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(HoloSurfaceVariant, CircleShape)
                        .border(1.dp, CyanGlow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = CyanBright,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextWhite
                    )
                    Text(
                        text = subtitle,
                        style = Typography.bodyMedium.copy(fontSize = 11.sp),
                        color = TextCoolGrey
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = CyanBright,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
