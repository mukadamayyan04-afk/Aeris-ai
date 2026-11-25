package com.example.ui.work

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.MessageSender
import com.example.domain.model.VoiceState
import com.example.tasks.TaskStatus
import com.example.ui.MainViewModel
import com.example.ui.components.HoloCard
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
import com.example.ui.theme.TextWhite
import com.example.ui.theme.Typography
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class WorkModeActiveModal {
    AI_ASSISTANT,
    TASK_MANAGER,
    SYSTEM_MONITOR,
    DEVICE_CONTROL,
    MEMORY,
    PROJECTS_TOOLS
}

@Composable
fun WorkModeScreen(
    viewModel: MainViewModel,
    onNavigateToNormalMode: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToMemory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProjects: () -> Unit = {},
    onNavigateToImageStudio: () -> Unit = {},
    onNavigateToModels: () -> Unit = {},
    onNavigateToGameCoach: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToVoiceAssistant: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val aiEngineState by viewModel.aiEngineState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isFlashOn by viewModel.flashState.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val liveTranscript by viewModel.liveTranscript.collectAsState()
    val voiceInputEnabled by viewModel.voiceInputEnabled.collectAsState()
    val healthStates by viewModel.healthStates.collectAsState()
    val freeOnlyMode by viewModel.freeOnlyMode.collectAsState()
    val aiOperatingMode by viewModel.aiOperatingMode.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val activeProjectId by viewModel.activeProjectId.collectAsState()
    val activeProjectTasks by viewModel.activeProjectTasks.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val activeConversation = remember(conversations, uiState.currentConversationId) {
        conversations.find { it.id == uiState.currentConversationId }
    }
    val context = LocalContext.current

    var consoleInput by remember { mutableStateOf("") }
    var activeModal by remember { mutableStateOf<WorkModeActiveModal?>(null) }
    var currentTime by remember {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
    }
    val consoleListState = rememberLazyListState()

    BackHandler {
        onNavigateToNormalMode()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshDiagnostics()
        while (true) {
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            delay(30_000L)
        }
    }

    LaunchedEffect(messages.size, uiState.streamingText, activeModal) {
        if (activeModal == WorkModeActiveModal.AI_ASSISTANT && (messages.isNotEmpty() || uiState.streamingText != null)) {
            val count = messages.takeLast(12).size + (if (uiState.streamingText != null) 1 else 0)
            if (count > 0) {
                consoleListState.animateScrollToItem(count - 1)
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onMicrophonePermissionResult(isGranted)
        if (isGranted) {
            viewModel.startVoiceInput()
        } else {
            Toast.makeText(context, "Microphone access required for speech input", Toast.LENGTH_SHORT).show()
        }
    }

    val submitConsolePrompt: () -> Unit = {
        val text = consoleInput.trim()
        if (text.isNotEmpty()) {
            viewModel.sendMessage(text)
            consoleInput = ""
            activeModal = WorkModeActiveModal.AI_ASSISTANT
        }
    }

    // Animation for holographic rotation rings
    val infiniteTransition = rememberInfiniteTransition(label = "work_mode_holo")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val counterRotationAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_rotation"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HoloBlack)
            .statusBarsPadding()
            .displayCutoutPadding()
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(listOf(CyanNeon.copy(alpha = 0.7f), CyanDeep)),
                shape = RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .testTag("work_mode_screen")
    ) {
        // 1. TOP COMMAND BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HoloDark.copy(alpha = 0.92f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onNavigateToNormalMode,
                    modifier = Modifier
                        .size(34.dp)
                        .background(HoloSurfaceVariant, CircleShape)
                        .border(1.dp, CyanGlow, CircleShape)
                        .testTag("back_to_normal_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Exit Work Mode",
                        tint = CyanNeon,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "AERIS AI — Work Mode",
                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(8.dp))

                val statusDotColor = when (uiState.voiceState) {
                    VoiceState.OFFLINE -> TextMuted
                    VoiceState.ERROR -> AlertRed
                    else -> StatusSuccess
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(statusDotColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = uiState.voiceState.label,
                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                        color = statusDotColor,
                        maxLines = 1
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Explicit visible exit button to switch cleanly back to Portrait Normal Mode
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = HoloSurfaceVariant,
                    border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .clickable(onClick = onNavigateToNormalMode)
                        .testTag("work_mode_exit_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = "Normal Mode",
                            tint = CyanNeon,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Normal Mode",
                            style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = CyanNeon
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "System Ready",
                        tint = CyanBright,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    val isCharging = uiState.diagnostics?.isCharging == true
                    Icon(
                        imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                        contentDescription = "Battery",
                        tint = StatusSuccess,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = currentTime,
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = TextWhite
                    )
                }
            }
        }

        // 2. MAIN HORIZONTAL 3-COLUMN COMMAND CENTER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // LEFT COLUMN: Navigation & Command Categories (scrollable for compact landscape devices)
            Column(
                modifier = Modifier
                    .weight(0.24f)
                    .widthIn(min = 104.dp, max = 185.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WorkNavButton(
                    icon = Icons.Default.Psychology,
                    label = "AI Assistant",
                    isActive = activeModal == WorkModeActiveModal.AI_ASSISTANT,
                    onClick = {
                        activeModal = if (activeModal == WorkModeActiveModal.AI_ASSISTANT) null else WorkModeActiveModal.AI_ASSISTANT
                    }
                )

                WorkNavButton(
                    icon = Icons.Default.Task,
                    label = "Task Manager",
                    isActive = activeModal == WorkModeActiveModal.TASK_MANAGER,
                    onClick = {
                        activeModal = if (activeModal == WorkModeActiveModal.TASK_MANAGER) null else WorkModeActiveModal.TASK_MANAGER
                    }
                )

                WorkNavButton(
                    icon = Icons.Default.Speed,
                    label = "System Monitor",
                    isActive = activeModal == WorkModeActiveModal.SYSTEM_MONITOR,
                    onClick = {
                        viewModel.refreshDiagnostics()
                        activeModal = if (activeModal == WorkModeActiveModal.SYSTEM_MONITOR) null else WorkModeActiveModal.SYSTEM_MONITOR
                    }
                )

                WorkNavButton(
                    icon = Icons.Default.Smartphone,
                    label = "Device Control",
                    isActive = activeModal == WorkModeActiveModal.DEVICE_CONTROL,
                    onClick = {
                        activeModal = if (activeModal == WorkModeActiveModal.DEVICE_CONTROL) null else WorkModeActiveModal.DEVICE_CONTROL
                    }
                )

                WorkNavButton(
                    icon = Icons.Default.Memory,
                    label = "Memory",
                    isActive = activeModal == WorkModeActiveModal.MEMORY,
                    onClick = {
                        activeModal = if (activeModal == WorkModeActiveModal.MEMORY) null else WorkModeActiveModal.MEMORY
                    }
                )

                WorkNavButton(
                    icon = Icons.Default.Task,
                    label = "Projects & Tools",
                    isActive = activeModal == WorkModeActiveModal.PROJECTS_TOOLS,
                    onClick = {
                        activeModal = if (activeModal == WorkModeActiveModal.PROJECTS_TOOLS) null else WorkModeActiveModal.PROJECTS_TOOLS
                    }
                )

                WorkNavButton(
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    isActive = false,
                    onClick = onNavigateToSettings
                )
            }

            // CENTER COLUMN: Responsive Holographic AERIS Core & Active Workspace
            BoxWithConstraints(
                modifier = Modifier
                    .weight(0.49f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                if (activeModal == null) {
                    val coreDiameter = minOf(maxWidth, maxHeight).coerceAtLeast(100.dp) * 0.86f
                    val outerRingSize = coreDiameter
                    val innerRingSize = coreDiameter * 0.82f
                    val globeSize = coreDiameter * 0.68f
                    val avatarSize = (coreDiameter * 0.24f).coerceIn(32.dp, 54.dp)

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(outerRingSize)
                                .rotate(rotationAngle)
                                .border(
                                    1.2.dp,
                                    Brush.sweepGradient(listOf(CyanNeon, Color.Transparent, CyanBright, Color.Transparent)),
                                    CircleShape
                                )
                        )

                        Box(
                            modifier = Modifier
                                .size(innerRingSize)
                                .rotate(counterRotationAngle)
                                .border(
                                    1.dp,
                                    Brush.sweepGradient(listOf(ElectricBlue, Color.Transparent, CyanTeal, Color.Transparent)),
                                    CircleShape
                                )
                        )

                        Image(
                            painter = painterResource(id = R.drawable.holo_globe),
                            contentDescription = "AERIS Holographic Core Workspace",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(globeSize)
                                .clip(CircleShape)
                        )

                        // AERIS Holographic Core Identity Badge
                        Image(
                            painter = painterResource(id = R.drawable.aeris_avatar),
                            contentDescription = "AERIS Core Identity",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 6.dp)
                                .size(avatarSize)
                                .clip(CircleShape)
                                .border(1.5.dp, CyanNeon, CircleShape)
                        )

                        // Pedestal glow
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 2.dp)
                                .width(globeSize * 0.9f)
                                .height(12.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(CyanNeon.copy(alpha = 0.55f), Color.Transparent)
                                    ),
                                    shape = CircleShape
                                )
                        )
                    }
                } else {
                    HoloCard(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (activeModal) {
                                        WorkModeActiveModal.AI_ASSISTANT -> "AI ASSISTANT CONSOLE"
                                        WorkModeActiveModal.TASK_MANAGER -> "QUICK TASK MANAGER"
                                        WorkModeActiveModal.SYSTEM_MONITOR -> "HARDWARE TELEMETRY"
                                        WorkModeActiveModal.DEVICE_CONTROL -> "DEVICE CONTROLS"
                                        WorkModeActiveModal.MEMORY -> "SAVED MEMORY BANK"
                                        WorkModeActiveModal.PROJECTS_TOOLS -> "PROJECTS & TOOLS LAUNCHER"
                                        null -> ""
                                    },
                                    style = Typography.titleMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                    color = CyanNeon
                                )
                                IconButton(
                                    onClick = { activeModal = null },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Panel",
                                        tint = TextCoolGrey,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            when (activeModal) {
                                WorkModeActiveModal.AI_ASSISTANT -> {
                                    LazyColumn(
                                        state = consoleListState,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        if (messages.isEmpty() && uiState.streamingText == null) {
                                            item {
                                                Text(
                                                    text = "Work Mode AI Console ready, Sir ji. Type or speak below.",
                                                    style = Typography.bodySmall.copy(fontSize = 11.sp),
                                                    color = TextCoolGrey
                                                )
                                            }
                                        }
                                        items(messages.takeLast(12), key = { it.id }) { msg ->
                                            val isUser = msg.sender == MessageSender.USER
                                            Text(
                                                text = "${if (isUser) "> You: " else "# AERIS: "}${msg.text}",
                                                style = Typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                                                color = if (isUser) CyanTeal else TextWhite
                                            )
                                        }
                                        if (uiState.streamingText != null) {
                                            item {
                                                Text(
                                                    text = "# AERIS: ${uiState.streamingText} ▋",
                                                    style = Typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                                                    color = CyanNeon
                                                )
                                            }
                                        }
                                    }
                                }

                                WorkModeActiveModal.TASK_MANAGER -> {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { viewModel.taskManager.runHardwareSweep() },
                                                modifier = Modifier.weight(1f),
                                                border = BorderStroke(1.dp, CyanGlow)
                                            ) {
                                                Text("HW Sweep", style = Typography.labelSmall.copy(fontSize = 10.sp), color = CyanNeon, maxLines = 1)
                                            }
                                            OutlinedButton(
                                                onClick = { viewModel.taskManager.runMemoryIndexing() },
                                                modifier = Modifier.weight(1f),
                                                border = BorderStroke(1.dp, CyanGlow)
                                            ) {
                                                Text("Index Mem", style = Typography.labelSmall.copy(fontSize = 10.sp), color = CyanNeon, maxLines = 1)
                                            }
                                            OutlinedButton(
                                                onClick = { viewModel.taskManager.runCacheCleanup() },
                                                modifier = Modifier.weight(1f),
                                                border = BorderStroke(1.dp, CyanGlow)
                                            ) {
                                                Text("Purge Cache", style = Typography.labelSmall.copy(fontSize = 10.sp), color = CyanNeon, maxLines = 1)
                                            }
                                        }

                                        LazyColumn(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (tasks.isEmpty()) {
                                                item {
                                                    Text(
                                                        text = "No background sweeps queued yet.",
                                                        style = Typography.labelSmall,
                                                        color = TextMuted
                                                    )
                                                }
                                            }
                                            items(tasks) { task ->
                                                Surface(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = HoloSurface,
                                                    border = BorderStroke(0.5.dp, HoloCardBorder)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(task.name, style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold), color = TextWhite)
                                                            Text(task.statusMessage, style = Typography.labelSmall.copy(fontSize = 9.sp), color = TextCoolGrey)
                                                        }
                                                        Text(
                                                            text = "${task.status} (${task.progress}%)",
                                                            style = Typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                            color = when (task.status) {
                                                                TaskStatus.COMPLETED -> StatusSuccess
                                                                TaskStatus.RUNNING -> CyanNeon
                                                                TaskStatus.FAILED -> AlertRed
                                                                else -> TextCoolGrey
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { viewModel.setQuickTimer(60) },
                                                modifier = Modifier.weight(1f),
                                                border = BorderStroke(1.dp, CyanGlow)
                                            ) {
                                                Icon(Icons.Default.Timer, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("60s Timer", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextWhite)
                                            }
                                            OutlinedButton(
                                                onClick = { viewModel.setQuickAlarm(7, 0) },
                                                modifier = Modifier.weight(1f),
                                                border = BorderStroke(1.dp, CyanGlow)
                                            ) {
                                                Icon(Icons.Default.Alarm, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("7 AM Alarm", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextWhite)
                                            }
                                        }
                                    }
                                }

                                WorkModeActiveModal.SYSTEM_MONITOR -> {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        uiState.diagnostics?.let { diag ->
                                            Text("Device: ${diag.deviceModel} (Android ${diag.androidVersion})", style = Typography.labelSmall.copy(fontSize = 11.sp), color = CyanBright)
                                            Text("RAM: ${diag.availableRamMb} MB Free / ${diag.totalRamMb} MB Total", style = Typography.labelSmall.copy(fontSize = 11.sp), color = TextWhite)
                                            Text("Storage: ${String.format(Locale.US, "%.2f", diag.freeStorageGb)} GB Free / ${String.format(Locale.US, "%.1f", diag.totalStorageGb)} GB", style = Typography.labelSmall.copy(fontSize = 11.sp), color = TextWhite)
                                            Text("Battery: ${diag.batteryPercent}% (${if (diag.isCharging) "Charging" else "Discharging"})", style = Typography.labelSmall.copy(fontSize = 11.sp), color = TextWhite)
                                            Text("Architecture: ${diag.supportedAbis.joinToString()}", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextCoolGrey)
                                        } ?: Text("Reading hardware sensors...", style = Typography.labelSmall, color = TextMuted)
                                    }
                                }

                                WorkModeActiveModal.DEVICE_CONTROL -> {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { viewModel.toggleFlashlight() },
                                                modifier = Modifier.weight(1f),
                                                border = BorderStroke(1.dp, if (isFlashOn) CyanNeon else HoloCardBorder)
                                            ) {
                                                Icon(if (isFlashOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (isFlashOn) "Torch ON" else "Torch", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextWhite)
                                            }

                                            OutlinedButton(
                                                onClick = { viewModel.launchApp("camera") },
                                                modifier = Modifier.weight(1f),
                                                border = BorderStroke(1.dp, HoloCardBorder)
                                            ) {
                                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = CyanBright, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Camera", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextWhite)
                                            }

                                            OutlinedButton(
                                                onClick = { viewModel.adjustVolume(true) },
                                                modifier = Modifier.weight(1f),
                                                border = BorderStroke(1.dp, HoloCardBorder)
                                            ) {
                                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = CyanBright, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Vol +", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextWhite)
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { viewModel.createReminder("Work Mode Reminder", 15) },
                                                modifier = Modifier.weight(1f),
                                                border = BorderStroke(1.dp, CyanGlow)
                                            ) {
                                                Icon(Icons.Default.Alarm, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("15m Remind", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextWhite)
                                            }

                                            OutlinedButton(
                                                onClick = { viewModel.openSettingsPage("settings") },
                                                modifier = Modifier.weight(1f),
                                                border = BorderStroke(1.dp, HoloCardBorder)
                                            ) {
                                                Icon(Icons.Default.Settings, contentDescription = null, tint = CyanBright, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Android Settings", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextWhite)
                                            }
                                        }
                                        uiState.lastAction?.let { last ->
                                            Text(
                                                text = "Last action: ${last.label} [${last.lifecycleState.label} • ${last.status}] — ${last.resultMessage.orEmpty()}",
                                                style = Typography.labelSmall.copy(fontSize = 10.sp),
                                                color = CyanBright
                                            )
                                        }
                                        if (uiState.actionHistory.isNotEmpty()) {
                                            Text(
                                                text = "Recent Actions:",
                                                style = Typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                color = TextCoolGrey
                                            )
                                            uiState.actionHistory.take(4).forEach { act ->
                                                Text(
                                                    text = "• ${act.label} — ${act.lifecycleState.label} (${act.status})",
                                                    style = Typography.labelSmall.copy(fontSize = 9.sp),
                                                    color = TextMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                WorkModeActiveModal.MEMORY -> {
                                    LazyColumn(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        items(memories.take(10), key = { it.id }) { mem ->
                                            Text(
                                                text = "• [${mem.category}] ${mem.key}: ${mem.content}",
                                                style = Typography.bodySmall.copy(fontSize = 11.sp),
                                                color = TextWhite
                                            )
                                        }
                                        if (memories.isEmpty()) {
                                            item {
                                                Text("No memories saved yet. Use the Memory tab to add items.", style = Typography.labelSmall, color = TextMuted)
                                            }
                                        }
                                    }
                                }

                                WorkModeActiveModal.PROJECTS_TOOLS -> {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
                                            .testTag("work_mode_projects_tools_panel"),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val activeProj = projects.find { it.id == activeProjectId } ?: projects.firstOrNull()
                                        if (activeProj != null) {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp),
                                                color = HoloSurface,
                                                border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.6f))
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Project: ${activeProj.name}",
                                                            style = Typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                                            color = CyanNeon,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "${activeProj.progressPercent}%",
                                                            style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                            color = StatusSuccess
                                                        )
                                                    }
                                                    if (projects.size > 1) {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            projects.take(3).forEach { proj ->
                                                                val selected = proj.id == activeProj.id
                                                                Surface(
                                                                    modifier = Modifier.clickable { viewModel.selectProject(proj.id) },
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    color = if (selected) CyanDeep.copy(alpha = 0.5f) else HoloDark,
                                                                    border = BorderStroke(1.dp, if (selected) CyanNeon else HoloCardBorder)
                                                                ) {
                                                                    Text(
                                                                        text = proj.name.take(12),
                                                                        style = Typography.labelSmall.copy(fontSize = 9.sp),
                                                                        color = if (selected) CyanNeon else TextCoolGrey,
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                    if (activeProjectTasks.isNotEmpty()) {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        activeProjectTasks.take(3).forEach { task ->
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clickable { viewModel.toggleProjectTask(task.id, !task.isDone, task.projectId) }
                                                                    .padding(vertical = 2.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Icon(
                                                                    imageVector = if (task.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                                    contentDescription = null,
                                                                    tint = if (task.isDone) StatusSuccess else CyanBright,
                                                                    modifier = Modifier.size(13.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(5.dp))
                                                                Text(
                                                                    text = task.title,
                                                                    style = Typography.labelSmall.copy(fontSize = 10.sp),
                                                                    color = if (task.isDone) TextMuted else TextWhite,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = "No active project selected. Open Projects Hub to create one.",
                                                style = Typography.labelSmall.copy(fontSize = 10.sp),
                                                color = TextCoolGrey
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = onNavigateToProjects,
                                                modifier = Modifier.weight(1f),
                                                border = BorderStroke(1.dp, CyanGlow)
                                            ) {
                                                Icon(Icons.Default.Folder, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Projects", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextWhite, maxLines = 1)
                                            }
                                            OutlinedButton(
                                                onClick = onNavigateToImageStudio,
                                                modifier = Modifier.weight(1f),
                                                border = BorderStroke(1.dp, HoloCardBorder)
                                            ) {
                                                Icon(Icons.Default.Image, contentDescription = null, tint = CyanBright, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Studio", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextWhite, maxLines = 1)
                                            }
                                            OutlinedButton(
                                                onClick = onNavigateToModels,
                                                modifier = Modifier.weight(1f),
                                                border = BorderStroke(1.dp, HoloCardBorder)
                                            ) {
                                                Icon(Icons.Default.DeveloperBoard, contentDescription = null, tint = CyanBright, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Models", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextWhite, maxLines = 1)
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = onNavigateToGameCoach,
                                                modifier = Modifier.weight(1f),
                                                border = BorderStroke(1.dp, HoloCardBorder)
                                            ) {
                                                Icon(Icons.Default.SportsEsports, contentDescription = null, tint = CyanBright, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Coach", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextWhite, maxLines = 1)
                                            }
                                            OutlinedButton(
                                                onClick = onNavigateToPermissions,
                                                modifier = Modifier.weight(1f),
                                                border = BorderStroke(1.dp, HoloCardBorder)
                                            ) {
                                                Icon(Icons.Default.Security, contentDescription = null, tint = CyanBright, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Perms", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextWhite, maxLines = 1)
                                            }
                                            OutlinedButton(
                                                onClick = onNavigateToVoiceAssistant,
                                                modifier = Modifier.weight(1f),
                                                border = BorderStroke(1.dp, CyanGlow)
                                            ) {
                                                Icon(Icons.Default.Mic, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Voice", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextWhite, maxLines = 1)
                                            }
                                        }

                                        if (uiState.actionHistory.isNotEmpty()) {
                                            Text(
                                                text = "Action Audit Log:",
                                                style = Typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                color = CyanBright
                                            )
                                            uiState.actionHistory.take(5).forEach { act ->
                                                Text(
                                                    text = "• ${act.label} — ${act.lifecycleState.label} (${act.status})",
                                                    style = Typography.labelSmall.copy(fontSize = 9.sp),
                                                    color = TextCoolGrey,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                null -> {}
                            }
                        }
                    }
                }
            }

            // RIGHT COLUMN: Real AI Status, Real System Info, Active Tasks (scrollable)
            Column(
                modifier = Modifier
                    .weight(0.27f)
                    .widthIn(min = 112.dp, max = 210.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 1: AI Status (real engine state)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = HoloSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CyanGlow)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AI Status",
                                style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = CyanBright
                            )
                            Text(
                                text = "${aiOperatingMode.title} • ${if (freeOnlyMode) "Free" else "Custom"}",
                                style = Typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = if (freeOnlyMode) StatusSuccess else CyanNeon,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(if (aiEngineState.isLoaded) StatusSuccess else AlertRed, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (uiState.voiceState) {
                                    VoiceState.LISTENING -> "Listening..."
                                    VoiceState.PROCESSING -> "Recognizing..."
                                    VoiceState.THINKING -> "Inferring..."
                                    VoiceState.RESPONDING -> "Responding..."
                                    VoiceState.SPEAKING -> "Speaking..."
                                    VoiceState.OFFLINE -> "Offline Core"
                                    VoiceState.ERROR -> "Fault"
                                    VoiceState.IDLE -> if (aiEngineState.isLoaded) "Brain Ready" else "Standby"
                                },
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = if (aiEngineState.isLoaded) StatusSuccess else AlertRed,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        val activeHealth = healthStates[aiEngineState.modelId]?.status?.label ?: "Ready"
                        Text(
                            text = "${aiEngineState.modelName ?: aiEngineState.engineType} • $activeHealth",
                            style = Typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextCoolGrey,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        val summarySnippet = activeConversation?.summary?.takeIf { it.isNotBlank() }
                        if (summarySnippet != null) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Context: ${summarySnippet.take(60)}",
                                style = Typography.labelSmall.copy(fontSize = 9.sp),
                                color = TextMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Card 2: System Info (real hardware diagnostics — zero fake fallbacks)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.refreshDiagnostics() },
                    color = HoloSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CyanGlow)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "System Info",
                            style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = CyanBright
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        val diag = uiState.diagnostics
                        val ramText = diag?.let {
                            String.format(Locale.US, "RAM: %.1f / %.1f GB", it.availableRamMb / 1024.0, it.totalRamMb / 1024.0)
                        } ?: "RAM: Reading..."
                        val storageText = diag?.let {
                            String.format(Locale.US, "Storage: %.1f GB free", it.freeStorageGb)
                        } ?: "Storage: Reading..."
                        val batteryText = diag?.let {
                            "Battery: ${it.batteryPercent}%${if (it.isCharging) " ⚡" else ""}"
                        } ?: "Battery: Reading..."

                        Text(text = ramText, style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextWhite)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = storageText, style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextWhite)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = batteryText, style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextWhite)
                    }
                }

                // Card 3: Task & Session Panel
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeModal = WorkModeActiveModal.TASK_MANAGER },
                    color = HoloSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, HoloCardBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Task Panel",
                            style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = CyanBright
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        val runningCount = tasks.count { it.status == TaskStatus.RUNNING }
                        Text(
                            text = if (runningCount > 0) "$runningCount task(s) running" else "${tasks.size} tracked task(s)",
                            style = Typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (runningCount > 0) CyanNeon else TextCoolGrey
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${messages.size} session msgs • ${memories.size} memories",
                            style = Typography.labelSmall.copy(fontSize = 9.sp),
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Live voice transcript pill while listening in Work Mode
        AnimatedVisibility(visible = uiState.voiceState == VoiceState.LISTENING) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (liveTranscript.isNotBlank()) "Listening: $liveTranscript" else "Listening for command or query, Sir ji...",
                    style = Typography.labelSmall.copy(fontSize = 11.sp),
                    color = CyanNeon,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cancel",
                    style = Typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = AlertRed,
                    modifier = Modifier.clickable { viewModel.cancelVoiceInput() }
                )
            }
        }

        // 3. BOTTOM WORK MODE COMPOSER (Properly centered BasicTextField pill with zero clipping)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HoloDark.copy(alpha = 0.85f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (uiState.voiceState == VoiceState.LISTENING) CyanNeon.copy(alpha = 0.3f) else HoloSurface,
                        CircleShape
                    )
                    .border(1.dp, CyanNeon, CircleShape)
                    .clickable {
                        if (!voiceInputEnabled) {
                            Toast.makeText(context, "Voice Input is disabled in Voice Settings", Toast.LENGTH_SHORT).show()
                        } else if (uiState.voiceState == VoiceState.LISTENING) {
                            viewModel.stopVoiceInput()
                        } else {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                    .testTag("work_mode_mic_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (uiState.voiceState == VoiceState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Work Mode Voice Input",
                    tint = CyanNeon,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp, max = 80.dp),
                shape = RoundedCornerShape(22.dp),
                color = HoloSurface,
                border = BorderStroke(1.dp, if (consoleInput.isNotEmpty()) CyanNeon else CyanGlow)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (consoleInput.isEmpty()) {
                        Text(
                            text = "Ask anything in Work Mode...",
                            style = Typography.bodyMedium.copy(fontSize = 13.sp),
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = consoleInput,
                        onValueChange = { consoleInput = it },
                        textStyle = Typography.bodyMedium.copy(
                            color = TextWhite,
                            fontSize = 13.sp
                        ),
                        cursorBrush = SolidColor(CyanNeon),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { submitConsolePrompt() }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("work_mode_input_field")
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(CyanNeon, CircleShape)
                    .clickable(onClick = submitConsolePrompt)
                    .testTag("work_mode_send_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send in Work Mode",
                    tint = HoloBlack,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun WorkNavButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 38.dp)
            .clickable(onClick = onClick),
        color = if (isActive) CyanDeep.copy(alpha = 0.55f) else HoloSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (isActive) CyanNeon else HoloCardBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) CyanNeon else CyanBright,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = Typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isActive) CyanNeon else TextWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
