package com.example.ui.normal

import android.Manifest
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ChatMessage
import com.example.domain.model.MessageSender
import com.example.domain.model.VoiceState
import com.example.ui.MainViewModel
import com.example.ui.components.HoloCard
import com.example.ui.components.HolographicCore
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDeep
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanTeal
import com.example.ui.theme.HoloBlack
import com.example.ui.theme.HoloCardBorder
import com.example.ui.theme.HoloDark
import com.example.ui.theme.HoloSurface
import com.example.ui.theme.HoloSurfaceVariant
import com.example.ui.theme.StatusError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.Typography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NormalModeScreen(
    viewModel: MainViewModel,
    onNavigateToWorkMode: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToModels: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToTracker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isFlashOn by viewModel.flashState.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Audio recording permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceInput()
        } else {
            Toast.makeText(context, "Microphone permission required for voice input", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(messages.size, uiState.streamingText) {
        if (messages.isNotEmpty() || uiState.streamingText != null) {
            val target = (messages.size + (if (uiState.streamingText != null) 1 else 0) - 1).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HoloBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // 1. Top HUD Header
        NormalHudHeader(
            uiState = uiState,
            onToggleMode = onNavigateToWorkMode,
            onOpenHistory = onNavigateToHistory,
            onNewConversation = {
                viewModel.createNewConversation("Session ${System.currentTimeMillis() % 1000}")
                Toast.makeText(context, "New conversation started", Toast.LENGTH_SHORT).show()
            },
            onOpenMemory = onNavigateToMemory,
            onOpenSettings = onNavigateToSettings
        )

        // 2. Central AI Core Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            HolographicCore(
                voiceState = uiState.voiceState,
                rmsLevel = uiState.speechRms,
                size = 160.dp
            )
        }

        // 3. Quick Action Telemetry Strip
        QuickActionStrip(
            isFlashOn = isFlashOn,
            onToggleFlash = { viewModel.toggleFlashlight() },
            onSetTimer = { viewModel.setQuickTimer(60) },
            onSetAlarm = { viewModel.setQuickAlarm(7, 30) },
            onLaunchCamera = { viewModel.launchApp("camera") },
            onOpenModels = onNavigateToModels
        )

        // Action Status Toast if recent action executed
        AnimatedVisibility(visible = uiState.lastAction != null) {
            uiState.lastAction?.let { action ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp)
                        .background(HoloSurfaceVariant, RoundedCornerShape(8.dp))
                        .border(1.dp, CyanGlow, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "▶ ${action.label}: ${action.resultMessage ?: action.detail}",
                        style = Typography.labelSmall,
                        color = CyanBright
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 4. Conversation Feed
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty() && uiState.streamingText == null) {
                item {
                    HoloCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AERIS HUD • ONLINE",
                                    style = Typography.labelLarge,
                                    color = CyanNeon
                                )
                                Text(
                                    text = "SESSION #1",
                                    style = Typography.labelSmall.copy(fontSize = 9.sp),
                                    color = CyanBright
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Strict NO-API local assistant. Type or use Push-to-Talk to execute actions, queries, or long-term memories offline.",
                                style = Typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                ChatMessageItem(
                    message = msg,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(msg.text))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onSpeak = {
                        viewModel.speakMessage(msg.text)
                    }
                )
            }

            if (uiState.streamingText != null) {
                item {
                    StreamingMessageBubble(
                        text = uiState.streamingText ?: "",
                        onStop = { viewModel.stopGeneration() }
                    )
                }
            }
        }

        // 5. Bottom Push-To-Talk & Input Bar
        BottomInputBar(
            inputText = inputText,
            onTextChange = { inputText = it },
            voiceState = uiState.voiceState,
            isStreaming = uiState.streamingText != null,
            onSend = {
                if (inputText.isNotBlank()) {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                }
            },
            onVoiceClick = {
                if (uiState.voiceState == VoiceState.LISTENING) {
                    viewModel.stopVoiceInput()
                } else {
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onStopGeneration = {
                viewModel.stopGeneration()
            },
            onStopPlayback = {
                viewModel.stopSpeechPlayback()
            }
        )
    }
}

@Composable
private fun NormalHudHeader(
    uiState: com.example.ui.MainUiState,
    onToggleMode: () -> Unit,
    onOpenHistory: () -> Unit,
    onNewConversation: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "AERIS AI",
                style = Typography.titleLarge.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold),
                color = CyanNeon
            )
            StatusBadge(voiceState = uiState.voiceState)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Work mode pill
            Surface(
                modifier = Modifier
                    .clickable { onToggleMode() }
                    .border(1.dp, CyanGlow, RoundedCornerShape(12.dp))
                    .testTag("switch_work_mode_button"),
                color = HoloSurface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewQuilt,
                        contentDescription = "Switch to Work Mode",
                        tint = CyanBright,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "WORK",
                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                        color = CyanBright
                    )
                }
            }

            IconButton(onClick = onNewConversation, modifier = Modifier.size(36.dp).testTag("new_chat_button")) {
                Icon(
                    imageVector = Icons.Default.AddComment,
                    contentDescription = "New Session",
                    tint = CyanBright,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onOpenHistory, modifier = Modifier.size(36.dp).testTag("history_button")) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Chat Archive",
                    tint = CyanBright,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onOpenMemory, modifier = Modifier.size(36.dp).testTag("memory_button")) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = "Memory Bank",
                    tint = CyanBright,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onOpenSettings, modifier = Modifier.size(36.dp).testTag("settings_button")) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickActionStrip(
    isFlashOn: Boolean,
    onToggleFlash: () -> Unit,
    onSetTimer: () -> Unit,
    onSetAlarm: () -> Unit,
    onLaunchCamera: () -> Unit,
    onOpenModels: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ActionChip(
                icon = if (isFlashOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                label = if (isFlashOn) "Torch ON" else "Torch",
                isActive = isFlashOn,
                onClick = onToggleFlash
            )
        }
        item {
            ActionChip(
                icon = Icons.Default.Timer,
                label = "60s Timer",
                onClick = onSetTimer
            )
        }
        item {
            ActionChip(
                icon = Icons.Default.Alarm,
                label = "Alarm",
                onClick = onSetAlarm
            )
        }
        item {
            ActionChip(
                icon = Icons.Default.CameraAlt,
                label = "Camera",
                onClick = onLaunchCamera
            )
        }
        item {
            ActionChip(
                icon = Icons.Default.Memory,
                label = "GGUF Models",
                onClick = onOpenModels
            )
        }
    }
}

@Composable
private fun ActionChip(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .border(
                1.dp,
                if (isActive) CyanNeon else HoloCardBorder.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp)
            ),
        color = if (isActive) CyanDeep.copy(alpha = 0.35f) else HoloSurface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) CyanNeon else CyanBright,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = Typography.labelSmall.copy(fontSize = 11.sp),
                color = if (isActive) CyanNeon else TextPrimary
            )
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    onCopy: () -> Unit,
    onSpeak: () -> Unit
) {
    val isUser = message.sender == MessageSender.USER
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 14.dp
            ),
            color = if (isUser) HoloSurfaceVariant else HoloSurface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isUser) CyanTeal.copy(alpha = 0.5f) else HoloCardBorder.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth(0.88f)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "OPERATOR" else "AERIS CORE",
                        style = Typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = if (isUser) CyanTeal else CyanNeon
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formattedTime,
                            style = Typography.labelSmall.copy(fontSize = 9.sp),
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = onCopy, modifier = Modifier.size(22.dp)) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Message",
                                tint = TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        if (!isUser) {
                            IconButton(onClick = onSpeak, modifier = Modifier.size(22.dp)) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Speak Aloud",
                                    tint = CyanTeal,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = message.text,
                    style = Typography.bodyMedium,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun StreamingMessageBubble(
    text: String,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = HoloSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth(0.88f)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AERIS INFERRING",
                            style = Typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = CyanNeon
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(CyanNeon, CircleShape)
                        )
                    }

                    IconButton(onClick = onStop, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.StopCircle,
                            contentDescription = "Stop Generation",
                            tint = StatusError,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "$text ▋",
                    style = Typography.bodyMedium,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun BottomInputBar(
    inputText: String,
    onTextChange: (String) -> Unit,
    voiceState: VoiceState,
    isStreaming: Boolean,
    onSend: () -> Unit,
    onVoiceClick: () -> Unit,
    onStopGeneration: () -> Unit,
    onStopPlayback: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = HoloDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, CyanGlow)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Push-to-talk voice button
            IconButton(
                onClick = onVoiceClick,
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if (voiceState == VoiceState.LISTENING) StatusError else CyanDeep,
                        shape = CircleShape
                    )
                    .border(1.5.dp, CyanNeon, CircleShape)
                    .testTag("push_to_talk_button")
            ) {
                Icon(
                    imageVector = if (voiceState == VoiceState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Push to Talk",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                placeholder = {
                    Text(
                        text = if (voiceState == VoiceState.LISTENING) "Listening..." else "Message AERIS...",
                        style = Typography.bodyMedium,
                        color = TextMuted
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = HoloCardBorder.copy(alpha = 0.5f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = CyanNeon,
                    focusedContainerColor = HoloSurface,
                    unfocusedContainerColor = HoloSurface
                ),
                shape = RoundedCornerShape(20.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(6.dp))

            if (isStreaming) {
                IconButton(
                    onClick = onStopGeneration,
                    modifier = Modifier
                        .size(40.dp)
                        .background(StatusError.copy(alpha = 0.25f), CircleShape)
                        .border(1.dp, StatusError, CircleShape)
                        .testTag("stop_generation_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Generation",
                        tint = StatusError,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (voiceState == VoiceState.SPEAKING) {
                IconButton(
                    onClick = onStopPlayback,
                    modifier = Modifier
                        .size(40.dp)
                        .background(StatusError.copy(alpha = 0.25f), CircleShape)
                        .border(1.dp, StatusError, CircleShape)
                        .testTag("stop_speech_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Spoken Response",
                        tint = StatusError,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .size(40.dp)
                        .background(CyanDeep, CircleShape)
                        .border(1.dp, CyanNeon, CircleShape)
                        .testTag("send_message_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Message",
                        tint = CyanNeon,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
