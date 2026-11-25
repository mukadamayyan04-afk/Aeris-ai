package com.example.ui.chat

import android.Manifest
import android.content.res.Configuration
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.MessageSender
import com.example.domain.model.VoiceState
import com.example.ui.MainViewModel
import com.example.ui.components.AerisRoundedComposer
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDeep
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanTeal
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.HoloBlack
import com.example.ui.theme.HoloCardBorder
import com.example.ui.theme.HoloDark
import com.example.ui.theme.HoloSurface
import com.example.ui.theme.HoloSurfaceVariant
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextCoolGrey
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextWhite
import com.example.ui.theme.Typography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToVoiceAssistant: () -> Unit = {},
    onOpenDrawer: (() -> Unit)? = null,
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToFeatures: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val aiEngineState by viewModel.aiEngineState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val liveTranscript by viewModel.liveTranscript.collectAsState()
    val voiceInputEnabled by viewModel.voiceInputEnabled.collectAsState()
    val pushToTalkEnabled by viewModel.pushToTalkEnabled.collectAsState()
    val voiceError by viewModel.voiceError.collectAsState()
    val catalogModels by viewModel.catalogModels.collectAsState()
    val freeOnlyMode by viewModel.freeOnlyMode.collectAsState()
    val preferredModelId by viewModel.preferredModelId.collectAsState()
    val attachedDoc by viewModel.attachedDocument.collectAsState()
    val attachedImageName by viewModel.attachedImageName.collectAsState()
    val handoffBanner by viewModel.modelHandoffBanner.collectAsState()
    val pendingDeviceAction by viewModel.pendingDeviceAction.collectAsState()
    val activeConversation = conversations.find { it.id == uiState.currentConversationId }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val density = LocalDensity.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0

    var inputText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            } ?: uri.lastPathSegment ?: "photo.jpg"
            viewModel.attachImageFromPicker(uri, fileName)
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            } ?: uri.lastPathSegment ?: "document.txt"
            viewModel.attachDocumentFromUri(uri, fileName)
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onMicrophonePermissionResult(isGranted)
        if (isGranted) {
            viewModel.startListeningForComposer(
                onPartial = {},
                onResult = { recognized ->
                    inputText = if (inputText.isBlank()) recognized else "$inputText $recognized"
                }
            )
        } else {
            Toast.makeText(context, "Microphone access required for speech input", Toast.LENGTH_SHORT).show()
        }
    }

    val streamBucket = (uiState.streamingText?.length ?: 0) / 140
    LaunchedEffect(messages.size, uiState.streamingText != null, isImeVisible) {
        if (messages.isNotEmpty() || uiState.streamingText != null) {
            val target = (messages.size + (if (uiState.streamingText != null) 1 else 0) - 1).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }
    LaunchedEffect(streamBucket) {
        if (uiState.streamingText != null && streamBucket > 0) {
            val target = (messages.size + 1 - 1).coerceAtLeast(0)
            listState.scrollToItem(target)
        }
    }

    val lastAssistantId = messages.lastOrNull { it.sender == MessageSender.AERIS && !it.isError }?.id

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HoloBlack)
            .statusBarsPadding()
            .displayCutoutPadding()
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            .testTag("chat_screen_root")
    ) {
        // 1. TOP BAR with Active Model Pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HoloDark)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (onOpenDrawer != null) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("chat_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Menu",
                            tint = CyanNeon,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(HoloSurface)
                        .border(1.5.dp, CyanNeon, CircleShape)
                        .clickable { onNavigateToVoiceAssistant() },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.aeris_avatar),
                        contentDescription = "AERIS AI Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "AERIS AI",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                        color = CyanNeon
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(StatusSuccess, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (uiState.voiceState) {
                                VoiceState.IDLE -> "Online"
                                VoiceState.LISTENING -> "Listening..."
                                VoiceState.PROCESSING -> "Recognizing..."
                                VoiceState.THINKING -> "Thinking..."
                                VoiceState.RESPONDING -> "Responding..."
                                VoiceState.SPEAKING -> "Speaking..."
                                VoiceState.OFFLINE -> "Offline"
                                VoiceState.ERROR -> "Notice"
                            },
                            style = Typography.labelSmall.copy(fontSize = 10.sp),
                            color = StatusSuccess
                        )
                    }
                }
            }

            // Active Model Selector Pill
            Box {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = HoloSurfaceVariant,
                    border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .clickable { showModelMenu = true }
                        .testTag("chat_model_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = aiEngineState.modelId ?: "gemini-3.8-flash",
                            style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                            color = CyanBright,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Switch Model",
                            tint = CyanNeon,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showModelMenu,
                    onDismissRequest = { showModelMenu = false },
                    modifier = Modifier.background(HoloSurface)
                ) {
                    catalogModels.filter { !freeOnlyMode || (!it.requiresBilling && it.isFreeTierEligible) }
                        .forEach { desc ->
                            val isSelected = desc.id == (preferredModelId ?: aiEngineState.modelId)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (isSelected) "* ${desc.displayName}" else desc.displayName,
                                        color = if (isSelected) CyanNeon else TextWhite,
                                        fontSize = 12.sp
                                    )
                                },
                                onClick = {
                                    showModelMenu = false
                                    viewModel.setPreferredModelId(desc.id)
                                    Toast.makeText(context, "Preferred model: ${desc.displayName}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CyanDeep.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, CyanNeon),
                    modifier = Modifier
                        .clickable { onNavigateToVoiceAssistant() }
                        .testTag("chat_voice_mode_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice Mode", tint = CyanNeon, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Voice", style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = CyanNeon)
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onNavigateToHistory, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Search, contentDescription = "Search Chat", tint = CyanBright, modifier = Modifier.size(19.dp))
                }

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = TextCoolGrey, modifier = Modifier.size(19.dp))
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(HoloSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Voice Assistant HUD", color = TextWhite) },
                            leadingIcon = { Icon(Icons.Default.Mic, contentDescription = null, tint = CyanNeon) },
                            onClick = {
                                showMenu = false
                                onNavigateToVoiceAssistant()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("New Session", color = TextWhite) },
                            leadingIcon = { Icon(Icons.Default.AddComment, contentDescription = null, tint = CyanNeon) },
                            onClick = {
                                showMenu = false
                                viewModel.createNewConversation("AERIS Session")
                                Toast.makeText(context, "New conversation started", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Chat History", color = TextWhite) },
                            leadingIcon = { Icon(Icons.Default.History, contentDescription = null, tint = CyanBright) },
                            onClick = {
                                showMenu = false
                                onNavigateToHistory()
                            }
                        )
                    }
                }
            }
        }

        // Active Project Workspace badge when session belongs to a project
        if (activeConversation != null && (activeConversation.projectId != null || activeConversation.projectContext.isNotBlank())) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .testTag("chat_project_badge"),
                shape = RoundedCornerShape(10.dp),
                color = CyanDeep.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.7f))
            ) {
                Text(
                    text = "Project Scope: ${activeConversation.projectContext.ifBlank { "Project #${activeConversation.projectId}" }}",
                    style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = CyanNeon,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        // Continuity Handoff Banner when router switches models
        AnimatedVisibility(visible = !handoffBanner.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clickable { viewModel.clearModelHandoffBanner() },
                shape = RoundedCornerShape(10.dp),
                color = HoloSurfaceVariant,
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.7f))
            ) {
                Text(
                    text = handoffBanner.orEmpty(),
                    style = Typography.labelSmall.copy(fontSize = 10.sp),
                    color = GoldAccent,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        // 2. CHAT MESSAGES THREAD
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }

            if (messages.isEmpty() && uiState.streamingText == null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(HoloSurfaceVariant)
                                .border(1.5.dp, CyanNeon, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.aeris_avatar),
                                contentDescription = "AERIS",
                                modifier = Modifier.size(60.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Hello Sir ji! How can I assist you today?",
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Type below, attach a file, or tap Hold to Talk for voice input.",
                            style = Typography.bodyMedium.copy(fontSize = 12.sp),
                            color = TextCoolGrey
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickPromptChip(
                                text = "Explain Kotlin Flow",
                                onClick = { viewModel.sendMessage("Explain Kotlin StateFlow with a clean code example.") }
                            )
                            QuickPromptChip(
                                text = "Check System RAM",
                                onClick = { viewModel.sendMessage("Check hardware diagnostics and available RAM.") }
                            )
                        }
                    }
                }
            }

            items(items = messages, key = { it.id }) { msg ->
                val timeStr = formatTimestamp(msg.timestamp)
                if (msg.sender == MessageSender.USER) {
                    UserMessageRow(
                        message = msg,
                        formattedTime = timeStr,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(msg.text))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    AerisMessageRow(
                        message = msg,
                        formattedTime = timeStr,
                        isLatestAssistantMessage = msg.id == lastAssistantId,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(msg.text))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        onSpeak = { viewModel.speakMessage(msg.text) },
                        onRegenerate = { viewModel.regenerateLastResponse() },
                        onRetry = { viewModel.retryFailedMessage(msg) },
                        onFeedback = { rating -> viewModel.submitMessageFeedback(msg, rating) }
                    )
                }
            }

            if (uiState.streamingText != null) {
                item {
                    val streamTime = formatTimestamp(System.currentTimeMillis())
                    StreamingMessageRow(
                        text = uiState.streamingText ?: "",
                        modelLabel = aiEngineState.modelId ?: "Inferring",
                        formattedTime = streamTime
                    )
                }
            } else if (uiState.voiceState == VoiceState.THINKING) {
                item { ThinkingIndicatorBubble() }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // 3. ERROR RECOVERY BANNER
        val activeError = voiceError ?: uiState.errorMessage
        AnimatedVisibility(visible = activeError != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                color = AlertRed.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = activeError.orEmpty(),
                        style = Typography.labelSmall.copy(fontSize = 11.sp),
                        color = TextWhite,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Dismiss",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = CyanNeon,
                        modifier = Modifier
                            .clickable { viewModel.clearVoiceError() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // 3b. WAVEFORM & LIVE TRANSCRIPT VISUALIZER
        AnimatedVisibility(
            visible = uiState.voiceState == VoiceState.LISTENING ||
                uiState.voiceState == VoiceState.SPEAKING ||
                uiState.streamingText != null
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                color = HoloSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CyanGlow)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WaveformVisualizer(
                            isActive = true,
                            amplitudeMultiplier = (uiState.speechRms.coerceIn(0f, 10f) / 10f) + 0.6f,
                            modifier = Modifier.weight(1f).height(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (uiState.voiceState) {
                                VoiceState.LISTENING -> "Listening..."
                                VoiceState.SPEAKING -> "Speaking..."
                                else -> "Inferring..."
                            },
                            style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = CyanBright
                        )
                        if (uiState.voiceState == VoiceState.LISTENING) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cancel",
                                style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = AlertRed,
                                modifier = Modifier
                                    .clickable { viewModel.cancelVoiceInput() }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .testTag("chat_cancel_listening_button")
                            )
                        }
                    }
                    if (uiState.voiceState == VoiceState.LISTENING && liveTranscript.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Live: $liveTranscript",
                            style = Typography.bodySmall.copy(fontSize = 11.sp),
                            color = CyanNeon
                        )
                    }
                }
            }
        }

        // 4. FIXED ROUNDED PILL COMPOSER & VOICE CONTROLS
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = HoloDark,
            border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = if (isLandscape) 6.dp else 8.dp
                )
            ) {
                AerisRoundedComposer(
                    inputText = inputText,
                    onInputChange = { inputText = it },
                    voiceState = uiState.voiceState,
                    isStreaming = uiState.streamingText != null,
                    onSend = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    onStopGeneration = { viewModel.stopGeneration() },
                    onMicClick = {
                        if (!voiceInputEnabled) {
                            Toast.makeText(context, "Voice Input is disabled in Voice Settings", Toast.LENGTH_SHORT).show()
                        } else if (uiState.voiceState == VoiceState.LISTENING) {
                            viewModel.stopVoiceInput()
                        } else {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onVoiceModeClick = onNavigateToVoiceAssistant,
                    onAttachImageClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onAttachDocumentClick = {
                        documentPickerLauncher.launch(arrayOf("text/*", "application/json", "*/*"))
                    },
                    onOpenLibraryClick = onNavigateToLibrary,
                    onOpenToolsClick = onNavigateToFeatures,
                    attachedDoc = attachedDoc,
                    onClearDoc = { viewModel.clearAttachedDocument() },
                    attachedImageName = attachedImageName,
                    onClearImage = { viewModel.clearAttachedImage() },
                    placeholderText = "Ask AERIS...",
                    containerTag = "chat_composer_container",
                    plusTag = "composer_plus_button",
                    inputTag = "chat_input_field",
                    micTag = "chat_composer_mic_button",
                    voiceModeTag = "composer_voice_mode_button",
                    sendTag = "send_button",
                    maxLines = if (isLandscape) 2 else 4
                )

                Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val holdLabel = when {
                        uiState.voiceState == VoiceState.LISTENING -> "Listening..."
                        pushToTalkEnabled -> "Hold to Talk"
                        else -> "Tap to Talk"
                    }
                    Row(
                        modifier = Modifier
                            .then(
                                if (pushToTalkEnabled) {
                                    Modifier.pointerInput(voiceInputEnabled) {
                                        detectTapGestures(
                                            onPress = {
                                                if (!voiceInputEnabled) {
                                                    Toast.makeText(context, "Voice Input is disabled in Voice Settings", Toast.LENGTH_SHORT).show()
                                                    return@detectTapGestures
                                                }
                                                if (viewModel.audioSessionManager.hasAudioRecordPermission) {
                                                    viewModel.startListeningForComposer(
                                                        onResult = { recognized ->
                                                            inputText = if (inputText.isBlank()) recognized else "$inputText $recognized"
                                                        }
                                                    )
                                                    tryAwaitRelease()
                                                    viewModel.stopVoiceInput()
                                                } else {
                                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                }
                                            }
                                        )
                                    }
                                } else {
                                    Modifier.clickable {
                                        if (!voiceInputEnabled) {
                                            Toast.makeText(context, "Voice Input is disabled in Voice Settings", Toast.LENGTH_SHORT).show()
                                        } else if (uiState.voiceState == VoiceState.LISTENING) {
                                            viewModel.stopVoiceInput()
                                        } else {
                                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("chat_hold_to_talk_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (uiState.voiceState == VoiceState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = holdLabel,
                            tint = if (uiState.voiceState == VoiceState.LISTENING) CyanNeon else TextCoolGrey,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = holdLabel,
                            style = Typography.labelSmall.copy(fontSize = 11.sp),
                            color = if (uiState.voiceState == VoiceState.LISTENING) CyanNeon else TextCoolGrey
                        )
                    }

                    VoiceBarButton(
                        icon = Icons.Default.Stop,
                        label = "Stop",
                        isActive = false,
                        onClick = {
                            viewModel.stopGeneration()
                            viewModel.stopSpeechPlayback()
                            viewModel.cancelVoiceInput()
                        }
                    )

                    VoiceBarButton(
                        icon = Icons.Default.PlayArrow,
                        label = "Play",
                        isActive = uiState.voiceState == VoiceState.SPEAKING,
                        onClick = {
                            val lastAiMsg = messages.lastOrNull { it.sender != MessageSender.USER && !it.isError }
                            if (lastAiMsg != null) {
                                viewModel.speakMessage(lastAiMsg.text)
                            } else {
                                Toast.makeText(context, "No response to read aloud", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    // Sensitive Device Action Confirmation Dialog (Stage 5)
    pendingDeviceAction?.let { pending ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeviceAction() },
            title = { Text("CONFIRM DEVICE ACTION", style = Typography.titleMedium, color = CyanNeon) },
            text = {
                Text(
                    text = "Sir ji, confirm Android action: ${pending.request.rationale} (${pending.request.detail})?",
                    style = Typography.bodyMedium,
                    color = TextWhite
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDeviceAction() },
                    modifier = Modifier.testTag("confirm_device_action_button")
                ) {
                    Text("EXECUTE", color = CyanNeon)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeviceAction() }) {
                    Text("CANCEL", color = AlertRed)
                }
            },
            containerColor = HoloSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun VoiceBarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) CyanNeon else TextCoolGrey,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = Typography.labelSmall.copy(fontSize = 11.sp),
            color = if (isActive) CyanNeon else TextCoolGrey
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
