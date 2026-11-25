package com.example.ui.voice

import android.Manifest
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.HoloPurpleBright
import com.example.R
import com.example.domain.model.VoiceState
import com.example.ui.MainViewModel
import com.example.ui.components.WaveformVisualizer
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
fun VoiceAssistantScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToVoiceSelection: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val selectedVoice by viewModel.selectedVoice.collectAsState()
    val liveTranscript by viewModel.liveTranscript.collectAsState()
    val activeSttEngineName by viewModel.activeSttEngineName.collectAsState()
    val activeTtsEngineName by viewModel.activeTtsEngineName.collectAsState()
    val voiceError by viewModel.voiceError.collectAsState()
    val pushToTalkEnabled by viewModel.pushToTalkEnabled.collectAsState()
    val voiceInputEnabled by viewModel.voiceInputEnabled.collectAsState()
    val isSpeechPaused by viewModel.isSpeechPaused.collectAsState()
    val isSttOffline by viewModel.isSttOffline.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var editBeforeSend by remember { mutableStateOf(false) }
    var transcriptDraft by remember { mutableStateOf("") }

    val lastAerisMessage = remember(messages) {
        messages.lastOrNull { it.sender == com.example.domain.model.MessageSender.AERIS }?.text
    }
    val lastUserMessage = remember(messages) {
        messages.lastOrNull { it.sender == com.example.domain.model.MessageSender.USER }?.text
    }

    val startMicCapture: () -> Unit = {
        if (editBeforeSend) {
            viewModel.startListeningForComposer(
                onPartial = { partial ->
                    if (partial.isNotBlank()) transcriptDraft = partial
                },
                onResult = { finalText ->
                    transcriptDraft = if (transcriptDraft.isBlank() || transcriptDraft == finalText) {
                        finalText
                    } else {
                        "$transcriptDraft $finalText"
                    }
                }
            )
        } else {
            viewModel.startVoiceInput()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onMicrophonePermissionResult(isGranted)
        if (isGranted) {
            startMicCapture()
        } else {
            Toast.makeText(context, "Microphone permission is required for Voice Assistant", Toast.LENGTH_SHORT).show()
        }
    }

    // Concentric rings animation for central mic button
    val transition = rememberInfiniteTransition(label = "voice_mic_pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (uiState.voiceState == VoiceState.LISTENING) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    // Status label and color mapping
    val statusText = when (uiState.voiceState) {
        VoiceState.IDLE -> "IDLE • READY"
        VoiceState.LISTENING -> "LISTENING TO SIR JI..."
        VoiceState.PROCESSING -> "PROCESSING SPEECH..."
        VoiceState.THINKING -> "THINKING • INFERRING..."
        VoiceState.RESPONDING -> "RESPONDING TO SIR JI..."
        VoiceState.SPEAKING -> "SPEAKING RESPONSE..."
        VoiceState.OFFLINE -> "OFFLINE MODE"
        VoiceState.ERROR -> "SYSTEM ERROR"
    }

    val statusColor = when (uiState.voiceState) {
        VoiceState.LISTENING -> CyanNeon
        VoiceState.PROCESSING -> CyanBright
        VoiceState.THINKING -> CyanBright
        VoiceState.RESPONDING -> CyanNeon
        VoiceState.SPEAKING -> ElectricBlue
        VoiceState.ERROR -> AlertRed
        VoiceState.OFFLINE -> TextMuted
        else -> StatusSuccess
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HoloBlack)
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("voice_assistant_screen")
    ) {
        // ==========================================
        // 1. TOP SECTION (Header & Engine status)
        // ==========================================
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
                    .testTag("voice_back_button")
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
                    text = "AERIS VOICE ASSISTANT",
                    style = Typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        fontSize = 15.sp
                    ),
                    color = CyanNeon
                )
                Text(
                    text = "${activeSttEngineName.take(22)} • ${activeTtsEngineName.take(18)}",
                    style = Typography.labelSmall.copy(fontSize = 9.sp),
                    color = CyanBright,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .size(38.dp)
                    .background(HoloSurfaceVariant, CircleShape)
                    .border(1.dp, CyanGlow, CircleShape)
                    .testTag("voice_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Voice Settings",
                    tint = CyanNeon,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Current Engine Status Badge + Mic Privacy Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = HoloSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = statusText,
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = statusColor
                    )
                }
            }

            if (uiState.voiceState == VoiceState.LISTENING) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = HoloPurpleBright.copy(alpha = 0.16f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HoloPurpleBright),
                    modifier = Modifier.testTag("voice_privacy_pill")
                ) {
                    Text(
                        text = if (isSttOffline) "MIC ACTIVE • ON-DEVICE" else "MIC ACTIVE • NETWORK STT",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = if (isSttOffline) StatusSuccess else GoldAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Honest Voice Error / Permission Recovery Banner
        val activeVoiceError = voiceError ?: uiState.errorMessage
        AnimatedVisibility(visible = activeVoiceError != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(10.dp),
                color = AlertRed.copy(alpha = 0.14f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.8f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = activeVoiceError.orEmpty(),
                        style = Typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Retry",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = CyanNeon,
                            modifier = Modifier.clickable {
                                viewModel.clearVoiceError()
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        )
                        if (!viewModel.audioSessionManager.hasAudioRecordPermission) {
                            Text(
                                text = "Open App Settings",
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = CyanBright,
                                modifier = Modifier.clickable {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }
                        Text(
                            text = "Dismiss",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = TextCoolGrey,
                            modifier = Modifier.clickable { viewModel.clearVoiceError() }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Responsive Body: Portrait (Single Column Scrollable) vs Landscape (Two Columns)
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Reactor + Waveform
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CentralMicrophoneReactor(
                        voiceState = uiState.voiceState,
                        pulseScale = pulseScale,
                        pushToTalkEnabled = pushToTalkEnabled,
                        onPressHoldStart = {
                            if (!voiceInputEnabled) {
                                Toast.makeText(context, "Voice Input is disabled in Voice Settings", Toast.LENGTH_SHORT).show()
                            } else if (viewModel.audioSessionManager.hasAudioRecordPermission) {
                                startMicCapture()
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onPressHoldEnd = { viewModel.stopVoiceInput() },
                        onMicClick = {
                            if (!voiceInputEnabled) {
                                Toast.makeText(context, "Voice Input is disabled in Voice Settings", Toast.LENGTH_SHORT).show()
                            } else if (uiState.voiceState == VoiceState.LISTENING) {
                                viewModel.stopVoiceInput()
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when {
                            uiState.voiceState == VoiceState.LISTENING -> "Listening... Speak now, Sir ji."
                            pushToTalkEnabled -> "Hold microphone to speak, Sir ji."
                            else -> "Speak now, Sir ji."
                        },
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                        color = if (uiState.voiceState == VoiceState.LISTENING) CyanNeon else TextWhite
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    WaveformVisualizer(
                        isActive = uiState.voiceState == VoiceState.LISTENING || uiState.voiceState == VoiceState.SPEAKING,
                        amplitudeMultiplier = (uiState.speechRms.coerceIn(0f, 10f) / 10f) + 0.6f,
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(30.dp)
                    )
                }

                // Right Column: Transcripts & Action Buttons
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EditBeforeSendBar(
                        editBeforeSend = editBeforeSend,
                        onToggle = { editBeforeSend = it },
                        draft = transcriptDraft,
                        onDraftChange = { transcriptDraft = it },
                        onSend = {
                            if (transcriptDraft.isNotBlank()) {
                                viewModel.sendEditedVoiceTranscript(transcriptDraft)
                                transcriptDraft = ""
                            }
                        },
                        onClear = { transcriptDraft = "" }
                    )

                    TranscriptsSection(
                        lastUserMessage = lastUserMessage,
                        liveTranscript = if (uiState.voiceState == VoiceState.LISTENING) liveTranscript else "",
                        lastAerisMessage = lastAerisMessage,
                        streamingText = uiState.streamingText,
                        onSpeakMessage = { viewModel.speakMessage(it) }
                    )

                    ActionButtonsSection(
                        voiceState = uiState.voiceState,
                        isSpeechPaused = isSpeechPaused,
                        lastAerisMessage = lastAerisMessage,
                        onStopListening = { viewModel.cancelVoiceInput() },
                        onPlayResponse = {
                            val textToSpeak = lastAerisMessage ?: "Hello Sir ji, AERIS voice system is active."
                            viewModel.speakMessage(textToSpeak)
                        },
                        onPauseOrResume = {
                            if (isSpeechPaused) viewModel.resumeSpeechPlayback() else viewModel.pauseSpeechPlayback()
                        },
                        onStopSpeaking = { viewModel.stopSpeechPlayback() },
                        onReplayResponse = {
                            if (!viewModel.voiceEngineManager.replayLastResponse()) {
                                val textToReplay = lastAerisMessage ?: "Hello Sir ji, AERIS voice assistant is ready."
                                viewModel.speakMessage(textToReplay)
                            }
                        }
                    )

                    ShortcutsSection(
                        selectedVoiceName = selectedVoice?.name ?: "System Default",
                        onNavigateToVoiceSelection = onNavigateToVoiceSelection,
                        onNavigateToSettings = onNavigateToSettings
                    )
                }
            }
        } else {
            // Portrait Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Avatar & "Speak now, Sir ji" Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.aeris_avatar),
                            contentDescription = "AERIS Avatar",
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .border(1.dp, CyanNeon, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.voiceState == VoiceState.LISTENING) "Listening... Speak now, Sir ji." else "Speak now, Sir ji.",
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                            color = if (uiState.voiceState == VoiceState.LISTENING) CyanNeon else TextWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ==========================================
                    // 2. CENTER SECTION: LARGE GLOWING MICROPHONE & RINGS
                    // ==========================================
                    CentralMicrophoneReactor(
                        voiceState = uiState.voiceState,
                        pulseScale = pulseScale,
                        pushToTalkEnabled = pushToTalkEnabled,
                        onPressHoldStart = {
                            if (!voiceInputEnabled) {
                                Toast.makeText(context, "Voice Input is disabled in Voice Settings", Toast.LENGTH_SHORT).show()
                            } else if (viewModel.audioSessionManager.hasAudioRecordPermission) {
                                startMicCapture()
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onPressHoldEnd = { viewModel.stopVoiceInput() },
                        onMicClick = {
                            if (!voiceInputEnabled) {
                                Toast.makeText(context, "Voice Input is disabled in Voice Settings", Toast.LENGTH_SHORT).show()
                            } else if (uiState.voiceState == VoiceState.LISTENING) {
                                viewModel.stopVoiceInput()
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Audio Waveform
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        color = HoloSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanGlow.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            WaveformVisualizer(
                                isActive = uiState.voiceState == VoiceState.LISTENING || uiState.voiceState == VoiceState.SPEAKING,
                                amplitudeMultiplier = (uiState.speechRms.coerceIn(0f, 10f) / 10f) + 0.6f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when {
                                    uiState.voiceState == VoiceState.LISTENING && liveTranscript.isNotBlank() -> liveTranscript
                                    uiState.voiceState == VoiceState.LISTENING -> "Live audio waveform active"
                                    pushToTalkEnabled -> "Hold glowing mic to speak"
                                    else -> "Audio waveform idle"
                                },
                                style = Typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (uiState.voiceState == VoiceState.LISTENING && liveTranscript.isNotBlank()) CyanNeon else TextCoolGrey,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    EditBeforeSendBar(
                        editBeforeSend = editBeforeSend,
                        onToggle = { editBeforeSend = it },
                        draft = transcriptDraft,
                        onDraftChange = { transcriptDraft = it },
                        onSend = {
                            if (transcriptDraft.isNotBlank()) {
                                viewModel.sendEditedVoiceTranscript(transcriptDraft)
                                transcriptDraft = ""
                            }
                        },
                        onClear = { transcriptDraft = "" }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Transcripts
                    TranscriptsSection(
                        lastUserMessage = lastUserMessage,
                        liveTranscript = if (uiState.voiceState == VoiceState.LISTENING) liveTranscript else "",
                        lastAerisMessage = lastAerisMessage,
                        streamingText = uiState.streamingText,
                        onSpeakMessage = { viewModel.speakMessage(it) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ==========================================
                // 3. BOTTOM SECTION: CONTROLS & SHORTCUTS
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionButtonsSection(
                        voiceState = uiState.voiceState,
                        isSpeechPaused = isSpeechPaused,
                        lastAerisMessage = lastAerisMessage,
                        onStopListening = { viewModel.cancelVoiceInput() },
                        onPlayResponse = {
                            val textToSpeak = lastAerisMessage ?: "Hello Sir ji, AERIS voice system is active."
                            viewModel.speakMessage(textToSpeak)
                        },
                        onPauseOrResume = {
                            if (isSpeechPaused) viewModel.resumeSpeechPlayback() else viewModel.pauseSpeechPlayback()
                        },
                        onStopSpeaking = { viewModel.stopSpeechPlayback() },
                        onReplayResponse = {
                            if (!viewModel.voiceEngineManager.replayLastResponse()) {
                                val textToReplay = lastAerisMessage ?: "Hello Sir ji, AERIS voice assistant is ready."
                                viewModel.speakMessage(textToReplay)
                            }
                        }
                    )

                    ShortcutsSection(
                        selectedVoiceName = selectedVoice?.name ?: "System Default",
                        onNavigateToVoiceSelection = onNavigateToVoiceSelection,
                        onNavigateToSettings = onNavigateToSettings
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun CentralMicrophoneReactor(
    voiceState: VoiceState,
    pulseScale: Float,
    pushToTalkEnabled: Boolean = false,
    onPressHoldStart: () -> Unit = {},
    onPressHoldEnd: () -> Unit = {},
    onMicClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer concentric ring 2
        Box(
            modifier = Modifier
                .size(190.dp)
                .scale(if (voiceState == VoiceState.LISTENING) pulseScale else 1f)
                .border(1.5.dp, CyanGlow.copy(alpha = 0.35f), CircleShape)
        )

        // Outer concentric ring 1
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(if (voiceState == VoiceState.LISTENING) pulseScale * 0.95f else 1f)
                .border(1.5.dp, CyanBright.copy(alpha = 0.5f), CircleShape)
        )

        // Central glowing sphere reactor
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            CyanNeon,
                            ElectricBlue,
                            HoloDark
                        )
                    ),
                    CircleShape
                )
                .border(2.dp, CyanBright, CircleShape)
                .then(
                    if (pushToTalkEnabled) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onPressHoldStart()
                                    tryAwaitRelease()
                                    onPressHoldEnd()
                                }
                            )
                        }
                    } else {
                        Modifier.clickable(onClick = onMicClick)
                    }
                )
                .testTag("voice_central_mic_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (voiceState == VoiceState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = "Voice Input Toggle",
                tint = HoloBlack,
                modifier = Modifier.size(50.dp)
            )
        }
    }
}

@Composable
private fun TranscriptsSection(
    lastUserMessage: String?,
    liveTranscript: String = "",
    lastAerisMessage: String?,
    streamingText: String?,
    onSpeakMessage: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Operator input card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = HoloSurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, HoloCardBorder)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = if (liveTranscript.isNotBlank()) "LIVE TRANSCRIPT:" else "LAST OPERATOR INPUT:",
                    style = Typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = CyanBright
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = liveTranscript.ifBlank { lastUserMessage ?: "Tap or hold the mic to speak..." },
                    style = Typography.bodyMedium.copy(fontSize = 12.sp),
                    color = if (liveTranscript.isNotBlank()) CyanNeon else TextWhite,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // AERIS response card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = HoloSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyanGlow.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AERIS RESPONSE:",
                        style = Typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = CyanNeon
                    )
                    if (lastAerisMessage != null) {
                        IconButton(
                            onClick = { onSpeakMessage(lastAerisMessage) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speak Response",
                                tint = CyanNeon,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (streamingText != null) {
                        "$streamingText ▋"
                    } else {
                        lastAerisMessage ?: "I am ready to assist you Sir ji. Press the glowing microphone or give an Android voice command."
                    },
                    style = Typography.bodyMedium.copy(fontSize = 12.sp),
                    color = TextWhite,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EditBeforeSendBar(
    editBeforeSend: Boolean,
    onToggle: (Boolean) -> Unit,
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = HoloSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (editBeforeSend) CyanNeon.copy(alpha = 0.7f) else HoloCardBorder)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Edit Transcript Before Send",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = TextWhite
                    )
                    Text(
                        text = if (editBeforeSend) "Speech fills editable draft below" else "Auto-sends speech immediately",
                        style = Typography.labelSmall.copy(fontSize = 9.sp),
                        color = TextCoolGrey
                    )
                }
                Switch(
                    checked = editBeforeSend,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanDeep),
                    modifier = Modifier.testTag("voice_edit_before_send_toggle")
                )
            }

            if (editBeforeSend) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    placeholder = { Text("Recognized transcript appears here...", color = TextMuted, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("voice_editable_transcript_input"),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = HoloCardBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    OutlinedButton(
                        onClick = onClear,
                        border = androidx.compose.foundation.BorderStroke(1.dp, HoloCardBorder)
                    ) {
                        Text("Clear", style = Typography.labelSmall, color = TextCoolGrey)
                    }
                    OutlinedButton(
                        onClick = onSend,
                        enabled = draft.isNotBlank(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon),
                        modifier = Modifier.testTag("voice_send_transcript_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send", style = Typography.labelSmall, color = CyanNeon)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButtonsSection(
    voiceState: VoiceState,
    isSpeechPaused: Boolean,
    lastAerisMessage: String?,
    onStopListening: () -> Unit,
    onPlayResponse: () -> Unit,
    onPauseOrResume: () -> Unit,
    onStopSpeaking: () -> Unit,
    onReplayResponse: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Button 1: Stop Listening
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clickable(onClick = onStopListening)
                    .testTag("action_stop_listening"),
                shape = RoundedCornerShape(10.dp),
                color = HoloSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (voiceState == VoiceState.LISTENING) AlertRed else HoloCardBorder
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MicOff, contentDescription = null, tint = if (voiceState == VoiceState.LISTENING) AlertRed else TextCoolGrey, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop Listening", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = TextWhite)
                }
            }

            // Button 2: Play / Pause / Resume Response
            val showPauseOrResume = voiceState == VoiceState.SPEAKING || isSpeechPaused
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clickable(onClick = if (showPauseOrResume) onPauseOrResume else onPlayResponse)
                    .testTag("action_pause_resume_speech"),
                shape = RoundedCornerShape(10.dp),
                color = HoloSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSpeechPaused) GoldAccent else CyanGlow)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (voiceState == VoiceState.SPEAKING && !isSpeechPaused) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isSpeechPaused) GoldAccent else CyanNeon,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            isSpeechPaused -> "Resume Voice"
                            voiceState == VoiceState.SPEAKING -> "Pause Voice"
                            else -> "Play Response"
                        },
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = TextWhite
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Button 3: Stop Speaking
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clickable(onClick = onStopSpeaking)
                    .testTag("action_stop_speaking"),
                shape = RoundedCornerShape(10.dp),
                color = HoloSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = AlertRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop Speaking", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = TextWhite)
                }
            }

            // Button 4: Replay Response
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clickable(onClick = onReplayResponse)
                    .testTag("action_replay_response"),
                shape = RoundedCornerShape(10.dp),
                color = HoloSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanBright)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Replay, contentDescription = null, tint = CyanBright, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Replay Response", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = TextWhite)
                }
            }
        }
    }
}

@Composable
private fun ShortcutsSection(
    selectedVoiceName: String,
    onNavigateToVoiceSelection: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Shortcut 1: Dedicated Voice Selection Screen
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onNavigateToVoiceSelection)
                .testTag("shortcut_voice_selection"),
            shape = RoundedCornerShape(10.dp),
            color = HoloSurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyanGlow)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Voice Selection",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = TextWhite
                    )
                    Text(
                        text = selectedVoiceName.take(14),
                        style = Typography.labelSmall.copy(fontSize = 9.sp),
                        color = CyanBright,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Shortcut 2: Dedicated Voice Settings Screen
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onNavigateToSettings)
                .testTag("shortcut_voice_settings"),
            shape = RoundedCornerShape(10.dp),
            color = HoloSurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, HoloCardBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = CyanBright,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Voice Settings",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = TextWhite
                    )
                    Text(
                        text = "Pitch, Speed, Input",
                        style = Typography.labelSmall.copy(fontSize = 9.sp),
                        color = TextCoolGrey
                    )
                }
            }
        }
    }
}
