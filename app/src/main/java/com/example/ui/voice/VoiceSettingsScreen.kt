package com.example.ui.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.os.Build
import androidx.compose.foundation.horizontalScroll
import com.example.domain.model.WakeWordEngineStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDeep
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanTeal
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HoloBlack
import com.example.ui.theme.HoloCardBorder
import com.example.ui.theme.HoloDark
import com.example.ui.theme.HoloSurface
import com.example.ui.theme.HoloSurfaceVariant
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextCoolGrey
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.theme.Typography

@Composable
fun VoiceSettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToVoiceSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedVoice by viewModel.selectedVoice.collectAsState()
    val availableTtsEngines by viewModel.availableTtsEngines.collectAsState()
    val activeTtsEnginePackage by viewModel.activeTtsEnginePackage.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()
    val speechPitch by viewModel.speechPitch.collectAsState()
    val voiceResponsesEnabled by viewModel.voiceResponsesEnabled.collectAsState()
    val autoPlayResponses by viewModel.autoPlayResponses.collectAsState()
    val voiceInputEnabled by viewModel.voiceInputEnabled.collectAsState()
    val pushToTalkEnabled by viewModel.pushToTalkEnabled.collectAsState()
    val recognitionLanguage by viewModel.recognitionLanguage.collectAsState()
    val wakeWordEnabled by viewModel.wakeWordEnabled.collectAsState()
    val wakeWordPhrase by viewModel.wakeWordPhrase.collectAsState()
    val backgroundListeningEnabled by viewModel.backgroundListeningEnabled.collectAsState()
    val wakeWordStatus by viewModel.wakeWordStatus.collectAsState()
    val wakeWordStatusDetail by viewModel.wakeWordStatusDetail.collectAsState()
    val wakeWordServiceRunning by viewModel.wakeWordServiceRunning.collectAsState()
    val wakeWordPrivacyMode by viewModel.wakeWordPrivacyMode.collectAsState()
    val startOnBootOptIn by viewModel.startOnBootOptIn.collectAsState()
    val capabilityReport by viewModel.voiceCapabilityReport.collectAsState()
    val voiceTaskCount by viewModel.voiceTaskCount.collectAsState()

    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.refreshVoiceCapabilities()
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        viewModel.onMicrophonePermissionResult(granted)
        if (granted) {
            Toast.makeText(context, "Microphone permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice input", Toast.LENGTH_LONG).show()
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        if (granted) {
            Toast.makeText(context, "Notification permission granted for foreground status", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission is recommended for background service status", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HoloBlack)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .verticalScroll(rememberScrollState())
            .testTag("voice_settings_screen")
    ) {
        // 1. TOP HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(38.dp)
                    .background(HoloSurfaceVariant, CircleShape)
                    .border(1.dp, CyanGlow, CircleShape)
                    .testTag("voice_settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = CyanNeon,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "VOICE & SPEECH SETTINGS",
                    style = Typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        fontSize = 15.sp
                    ),
                    color = CyanNeon
                )
                Text(
                    text = "Neural Speech & Acoustic Controls",
                    style = Typography.labelSmall.copy(fontSize = 10.sp),
                    color = CyanBright
                )
            }

            IconButton(
                onClick = {
                    viewModel.resetVoiceSettingsToDefaults()
                    Toast.makeText(context, "Voice settings reset to defaults", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .size(38.dp)
                    .background(HoloSurfaceVariant, CircleShape)
                    .border(1.dp, CyanGlow.copy(alpha = 0.5f), CircleShape)
                    .testTag("reset_voice_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Reset Defaults",
                    tint = CyanNeon,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ==========================================
        // SECTION 1: VOICE OUTPUT (TTS)
        // ==========================================
        SettingsSectionHeader(title = "VOICE OUTPUT (TEXT-TO-SPEECH)")

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = HoloSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, HoloCardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Enable Voice Responses Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Voice Responses",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = TextWhite
                        )
                        Text(
                            text = "AERIS will vocalize responses aloud",
                            style = Typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextCoolGrey
                        )
                    }
                    Switch(
                        checked = voiceResponsesEnabled,
                        onCheckedChange = { viewModel.setVoiceResponsesEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanNeon,
                            checkedTrackColor = CyanDeep
                        )
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = HoloCardBorder.copy(alpha = 0.5f))

                // Current Selected Voice Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Selected Voice",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = TextWhite
                        )
                        Text(
                            text = selectedVoice?.let { "${it.name} (${it.gender.label})" } ?: "AERIS System Default Voice",
                            style = Typography.labelSmall.copy(fontSize = 11.sp),
                            color = CyanNeon
                        )
                        Text(
                            text = selectedVoice?.let { "${it.languageDisplayName} • ${it.engineName.takeLast(16)}" } ?: "Device Local Engine",
                            style = Typography.labelSmall.copy(fontSize = 9.sp),
                            color = TextCoolGrey
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .clickable(onClick = onNavigateToVoiceSelection)
                            .testTag("change_voice_button"),
                        shape = RoundedCornerShape(8.dp),
                        color = CyanNeon,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanBright)
                    ) {
                        Text(
                            text = "CHANGE",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp),
                            color = HoloBlack,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                if (availableTtsEngines.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = HoloCardBorder.copy(alpha = 0.5f))
                    Text(
                        text = "Installed TTS Engine",
                        style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableTtsEngines.forEach { engineInfo ->
                            val isSelected = engineInfo.packageName == activeTtsEnginePackage
                            Surface(
                                modifier = Modifier
                                    .clickable { viewModel.switchTtsEngine(engineInfo.packageName) }
                                    .testTag("tts_engine_chip_${engineInfo.packageName}"),
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) CyanNeon else HoloDark,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) CyanBright else HoloCardBorder
                                )
                            ) {
                                Text(
                                    text = engineInfo.label,
                                    style = Typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) HoloBlack else TextWhite,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = HoloCardBorder.copy(alpha = 0.5f))

                // Speech Speed Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Speech Rate (Speed)",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                            color = TextWhite
                        )
                        Text(
                            text = String.format("%.2fx", speechRate),
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                            color = CyanNeon
                        )
                    }
                    Slider(
                        value = speechRate,
                        onValueChange = { viewModel.setSpeechRate(it) },
                        valueRange = 0.5f..2.0f,
                        steps = 14,
                        colors = SliderDefaults.colors(thumbColor = CyanNeon, activeTrackColor = CyanBright)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Speech Pitch Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Speech Pitch",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                            color = TextWhite
                        )
                        Text(
                            text = String.format("%.2fx", speechPitch),
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                            color = CyanNeon
                        )
                    }
                    Slider(
                        value = speechPitch,
                        onValueChange = { viewModel.setSpeechPitch(it) },
                        valueRange = 0.5f..1.8f,
                        steps = 12,
                        colors = SliderDefaults.colors(thumbColor = CyanNeon, activeTrackColor = CyanBright)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = HoloCardBorder.copy(alpha = 0.5f))

                // Auto-Play Responses Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Play Spoken Responses",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = TextWhite
                        )
                        Text(
                            text = "Automatically read out text replies as they arrive",
                            style = Typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextCoolGrey
                        )
                    }
                    Switch(
                        checked = autoPlayResponses,
                        onCheckedChange = { viewModel.setAutoPlayResponses(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanNeon,
                            checkedTrackColor = CyanDeep
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // SECTION 2: VOICE INPUT (STT)
        // ==========================================
        SettingsSectionHeader(title = "VOICE INPUT (SPEECH RECOGNITION)")

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = HoloSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, HoloCardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Enable Voice Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Voice Input",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = TextWhite
                        )
                        Text(
                            text = "Allow speaking to AERIS via microphone",
                            style = Typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextCoolGrey
                        )
                    }
                    Switch(
                        checked = voiceInputEnabled,
                        onCheckedChange = { viewModel.setVoiceInputEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanNeon,
                            checkedTrackColor = CyanDeep
                        )
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = HoloCardBorder.copy(alpha = 0.5f))

                // Push-To-Talk Preference
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Push-to-Talk Mode",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = TextWhite
                        )
                        Text(
                            text = if (pushToTalkEnabled) "Hold microphone while speaking" else "Tap once to start listening, tap to stop",
                            style = Typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextCoolGrey
                        )
                    }
                    Switch(
                        checked = pushToTalkEnabled,
                        onCheckedChange = { viewModel.setPushToTalkEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanNeon,
                            checkedTrackColor = CyanDeep
                        )
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = HoloCardBorder.copy(alpha = 0.5f))

                // Recognition Language
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Recognition Language",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = TextWhite
                        )
                        Text(
                            text = when (recognitionLanguage) {
                                "en-IN" -> "English (India / Hinglish)"
                                "en-US" -> "English (United States)"
                                "en-GB" -> "English (United Kingdom)"
                                else -> "System Default (Locale.getDefault)"
                            },
                            style = Typography.labelSmall.copy(fontSize = 11.sp),
                            color = CyanBright
                        )
                    }

                    // Selector chips
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("en-US", "en-IN").forEach { tag ->
                            val isSelected = recognitionLanguage == tag
                            Surface(
                                modifier = Modifier.clickable { viewModel.setRecognitionLanguage(tag) },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) CyanNeon else HoloSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CyanBright else HoloCardBorder)
                            ) {
                                Text(
                                    text = if (tag == "en-IN") "IN" else "US",
                                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                    color = if (isSelected) HoloBlack else TextWhite,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = HoloCardBorder.copy(alpha = 0.5f))

                // Microphone Permission Status Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (hasMicPermission) StatusSuccess else AlertRed, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Microphone Permission",
                                style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                color = TextWhite
                            )
                            Text(
                                text = if (hasMicPermission) "Hardware Access Granted" else "Permission Required for STT",
                                style = Typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (hasMicPermission) StatusSuccess else AlertRed
                            )
                        }
                    }

                    if (!hasMicPermission) {
                        Surface(
                            modifier = Modifier.clickable {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = AlertRed,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed)
                        ) {
                            Text(
                                text = "GRANT",
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = TextWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    } else {
                        Surface(
                            modifier = Modifier.clickable {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = HoloSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, HoloCardBorder)
                        ) {
                            Text(
                                text = "SETTINGS",
                                style = Typography.labelSmall.copy(fontSize = 9.sp),
                                color = TextCoolGrey,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // SECTION 3: WAKE WORD DETECTION
        // ==========================================
        SettingsSectionHeader(title = "WAKE WORD DETECTION")

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = HoloSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, HoloCardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Wake word toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Background Wake Word Detection",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = TextWhite
                        )
                        Text(
                            text = "Opt-in local foreground keyword listener (zero audio recorded)",
                            style = Typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextCoolGrey
                        )
                    }
                    Switch(
                        checked = wakeWordEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !hasMicPermission) {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                return@Switch
                            }
                            if (enabled && !hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            viewModel.setWakeWordEnabled(enabled)
                            if (enabled && (wakeWordStatus == WakeWordEngineStatus.MISSING_DEPENDENCY)) {
                                Toast.makeText(
                                    context,
                                    wakeWordStatusDetail ?: "Local KWS model/native library not bundled yet.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanNeon,
                            checkedTrackColor = CyanDeep
                        )
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = HoloCardBorder.copy(alpha = 0.5f))

                // Background listening persistence toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Allow Background Service Preference",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                            color = TextWhite
                        )
                        Text(
                            text = "Battery-conscious opt-in for foreground microphone service",
                            style = Typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextCoolGrey
                        )
                    }
                    Switch(
                        checked = backgroundListeningEnabled,
                        onCheckedChange = { viewModel.setBackgroundListeningEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanNeon,
                            checkedTrackColor = CyanDeep
                        )
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = HoloCardBorder.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Privacy Mode (Immediate Buffer Purge)",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                            color = TextWhite
                        )
                        Text(
                            text = "Discards audio frames immediately; never records audio files",
                            style = Typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextCoolGrey
                        )
                    }
                    Switch(
                        checked = wakeWordPrivacyMode,
                        onCheckedChange = { viewModel.setWakeWordPrivacyMode(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanDeep),
                        modifier = Modifier.testTag("wake_privacy_mode_switch")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = HoloCardBorder.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Standby Prompt on Device Boot",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                            color = TextWhite
                        )
                        Text(
                            text = "Android 14+ compliant: posts a tap-to-resume standby notification on reboot",
                            style = Typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextCoolGrey
                        )
                    }
                    Switch(
                        checked = startOnBootOptIn,
                        onCheckedChange = { viewModel.setStartOnBootOptIn(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanDeep),
                        modifier = Modifier.testTag("wake_boot_opt_in_switch")
                    )
                }

                if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = HoloCardBorder.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notification Permission (Android 13+)",
                                style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                color = TextWhite
                            )
                            Text(
                                text = "Required to display foreground microphone status & Stop button",
                                style = Typography.labelSmall.copy(fontSize = 10.sp),
                                color = AlertRed
                            )
                        }
                        Surface(
                            modifier = Modifier.clickable {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = AlertRed
                        ) {
                            Text(
                                text = "ALLOW",
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = TextWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = HoloCardBorder.copy(alpha = 0.5f))

                // Wake Word Phrase Selection
                Text(
                    text = "Activation Phrase",
                    style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Hey AERIS", "Wake up AERIS").forEach { phrase ->
                        val isSelected = wakeWordPhrase == phrase ||
                            (phrase == "Wake up AERIS" && wakeWordPhrase == "AERIS, wake up")
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clickable { viewModel.setWakeWordPhrase(phrase) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) CyanNeon else HoloDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CyanBright else HoloCardBorder)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "\"$phrase\"",
                                    style = Typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isSelected) HoloBlack else TextWhite
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Honest Background listening status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Background Service Status",
                        style = Typography.labelSmall.copy(fontSize = 11.sp),
                        color = TextCoolGrey
                    )
                    Text(
                        text = if (wakeWordServiceRunning) "ACTIVE (Foreground Service)" else wakeWordStatus.label.uppercase(),
                        style = Typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = when {
                            wakeWordServiceRunning -> StatusSuccess
                            wakeWordStatus == WakeWordEngineStatus.MISSING_DEPENDENCY ||
                                wakeWordStatus == WakeWordEngineStatus.PERMISSION_REQUIRED ||
                                wakeWordStatus == WakeWordEngineStatus.ERROR -> AlertRed
                            else -> TextMuted
                        }
                    )
                }
                if (!wakeWordStatusDetail.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = wakeWordStatusDetail.orEmpty(),
                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (wakeWordServiceRunning) CyanBright else TextCoolGrey
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Battery & OS notice: Android / MIUI battery savers may pause or force-stop background services when the screen locks. Set Battery Saver to \"No restrictions\" in App Settings if you rely on continuous listening.",
                    style = Typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextCoolGrey
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // SECTION 4: ENGINE & ARCHITECTURE INFO
        // ==========================================
        SettingsSectionHeader(title = "VOICE ENGINE & OFFLINE ARCHITECTURE")

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = HoloSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, HoloCardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                EngineInfoRow(
                    engineType = "Speech-to-Text (STT)",
                    engineName = if (capabilityReport.isAndroidOnDeviceSttAvailable) {
                        "Android On-Device SpeechRecognizer"
                    } else if (capabilityReport.isAndroidSttAvailable) {
                        "Android SpeechRecognizer (PREFER_OFFLINE)"
                    } else {
                        "No System STT Service Detected"
                    },
                    status = if (capabilityReport.isAndroidOnDeviceSttAvailable) {
                        "On-Device Ready"
                    } else if (capabilityReport.isAndroidSttAvailable) {
                        "System Service Ready"
                    } else {
                        "Unavailable"
                    },
                    isReady = capabilityReport.isAndroidSttAvailable || capabilityReport.isAndroidOnDeviceSttAvailable
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = HoloCardBorder.copy(alpha = 0.5f))

                EngineInfoRow(
                    engineType = "Text-to-Speech (TTS)",
                    engineName = selectedVoice?.engineName ?: "Android System TTS",
                    status = "${availableTtsEngines.size} Engine(s) Detected",
                    isReady = true
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = HoloCardBorder.copy(alpha = 0.5f))

                EngineInfoRow(
                    engineType = "Local KWS / Sherpa-ONNX",
                    engineName = if (capabilityReport.isSherpaNativeLibPresent) "libsherpa-onnx-jni.so Loaded" else "Sherpa-ONNX Native / KWS Models",
                    status = if (capabilityReport.isSherpaNativeLibPresent && capabilityReport.isSherpaKwsModelPresent) {
                        "Local Models Ready"
                    } else {
                        "Not Bundled in APK"
                    },
                    isReady = capabilityReport.isSherpaNativeLibPresent && capabilityReport.isSherpaKwsModelPresent
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = HoloCardBorder.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Room Voice Task Log",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = CyanBright
                        )
                        Text(
                            text = "$voiceTaskCount recorded voice interaction(s)",
                            style = Typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextWhite
                        )
                    }
                    if (voiceTaskCount > 0) {
                        Text(
                            text = "CLEAR LOG",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = AlertRed,
                            modifier = Modifier
                                .clickable {
                                    viewModel.clearVoiceTasks()
                                    Toast.makeText(context, "Voice task log cleared", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = Typography.labelSmall.copy(
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            letterSpacing = 1.sp
        ),
        color = CyanNeon,
        modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
    )
}

@Composable
private fun EngineInfoRow(
    engineType: String,
    engineName: String,
    status: String,
    isReady: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = engineType,
                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = CyanBright
            )
            Text(
                text = engineName,
                style = Typography.bodySmall.copy(fontSize = 12.sp),
                color = TextWhite
            )
        }

        Box(
            modifier = Modifier
                .background(
                    if (isReady) StatusSuccess.copy(alpha = 0.15f) else HoloSurfaceVariant,
                    RoundedCornerShape(6.dp)
                )
                .border(
                    0.5.dp,
                    if (isReady) StatusSuccess else HoloCardBorder,
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = status,
                style = Typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.sp
                ),
                color = if (isReady) StatusSuccess else TextCoolGrey
            )
        }
    }
}
