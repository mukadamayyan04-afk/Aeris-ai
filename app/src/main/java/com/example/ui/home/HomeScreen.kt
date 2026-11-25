package com.example.ui.home

import android.Manifest
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.VoiceState
import com.example.ui.MainViewModel
import com.example.ui.components.AerisRoundedComposer
import com.example.ui.components.HolographicAvatar
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDeep
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.HoloBlack
import com.example.ui.theme.HoloCardBorder
import com.example.ui.theme.HoloSurface
import com.example.ui.theme.HoloSurfaceVariant
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.theme.Typography

/**
 * AERIS AI Mobile Home Screen (Stage 8 Shell + Canonical Holographic HUD):
 * - Top bar: Drawer menu button, "AERIS AI" + Operating Mode pill, Notification Bell.
 * - Scrollable Body: Holographic Avatar, Greeting Card, Voice Assistant LIVE pill,
 *   Chat / Work Mode primary cards, Quick prompt chips (Image Studio / Write / Web / Features / Library),
 *   and Quick Actions.
 * - Docked Bottom Composer: Shared dark Cyan/Purple rounded "Ask AERIS" composer.
 */
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToWorkMode: () -> Unit,
    onNavigateToFeatures: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToVoiceAssistant: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onOpenDrawer: () -> Unit = onNavigateToFeatures,
    onSubmitPromptToChat: (String) -> Unit = { prompt ->
        viewModel.sendMessage(prompt)
        onNavigateToChat()
    },
    onNavigateToImageStudio: () -> Unit = {},
    onNavigateToWebAssistant: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val aiEngineState by viewModel.aiEngineState.collectAsState()
    val aiOperatingMode by viewModel.aiOperatingMode.collectAsState()
    val freeOnlyMode by viewModel.freeOnlyMode.collectAsState()
    val isFlashOn by viewModel.flashState.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val animationsEnabled by viewModel.animationsEnabled.collectAsState()
    val batterySaverHolo by viewModel.batterySaverHolo.collectAsState()
    val attachedDoc by viewModel.attachedDocument.collectAsState()
    val attachedImageName by viewModel.attachedImageName.collectAsState()
    val voiceInputEnabled by viewModel.voiceInputEnabled.collectAsState()
    val context = LocalContext.current

    var homeInput by remember { mutableStateOf("") }

    val userName = memories.find {
        it.key.equals("username", ignoreCase = true) || it.key.equals("name", ignoreCase = true)
    }?.content ?: "Ayyan"

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
                    homeInput = if (homeInput.isBlank()) recognized else "$homeInput $recognized"
                }
            )
        } else {
            Toast.makeText(context, "Microphone permission needed for dictation", Toast.LENGTH_SHORT).show()
        }
    }

    val submitHomePrompt: () -> Unit = {
        val text = homeInput.trim()
        if (text.isNotEmpty()) {
            homeInput = ""
            onSubmitPromptToChat(text)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(HoloBlack)
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("aeris_home_screen")
    ) {
        val isCompactHeight = maxHeight < 620.dp
        val avatarSize = if (isCompactHeight) 128.dp else 172.dp

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. TOP BAR: Side Drawer Menu, "AERIS AI" Title + Mode Status, Notification Bell
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier
                        .size(40.dp)
                        .background(HoloSurfaceVariant, CircleShape)
                        .border(1.dp, CyanGlow.copy(alpha = 0.55f), CircleShape)
                        .testTag("menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Menu",
                        tint = CyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "AERIS AI",
                        style = Typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            fontSize = 19.sp
                        ),
                        color = CyanNeon
                    )
                    Text(
                        text = "${aiOperatingMode.title} • ${if (freeOnlyMode) "Free-First" else "Custom"}",
                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                        color = CyanBright
                    )
                }

                IconButton(
                    onClick = onNavigateToNotifications,
                    modifier = Modifier
                        .size(40.dp)
                        .background(HoloSurfaceVariant, CircleShape)
                        .border(1.dp, CyanGlow.copy(alpha = 0.55f), CircleShape)
                        .testTag("notification_bell_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = CyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. SCROLLABLE BODY
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToVoiceAssistant() },
                    contentAlignment = Alignment.Center
                ) {
                    HolographicAvatar(
                        voiceState = uiState.voiceState,
                        rmsLevel = uiState.speechRms,
                        animationsEnabled = animationsEnabled && !batterySaverHolo,
                        size = avatarSize
                    )
                }

                // Greeting Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = HoloSurface,
                    border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Hello Sir ji! ($userName)",
                            style = Typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "How can I help you today?",
                            style = Typography.bodyMedium.copy(fontSize = 12.sp),
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(StatusSuccess, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Online • ${aiEngineState.modelId ?: "Core Ready"}",
                                style = Typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = StatusSuccess
                            )
                        }
                    }
                }

                // Prominent Voice Assistant Quick-Action Button
                val voicePulseTransition = rememberInfiniteTransition(label = "voice_home_pulse")
                val voiceBorderAlpha by voicePulseTransition.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "voice_border"
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .clickable { onNavigateToVoiceAssistant() }
                        .testTag("home_voice_assistant_prominent_button"),
                    shape = RoundedCornerShape(16.dp),
                    color = HoloSurface,
                    border = BorderStroke(1.5.dp, CyanNeon.copy(alpha = voiceBorderAlpha))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(CyanDeep.copy(alpha = 0.4f), HoloSurface, CyanDeep.copy(alpha = 0.25f))
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(CyanNeon, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Assistant",
                                    tint = HoloBlack,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Voice Assistant",
                                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                    color = TextWhite
                                )
                                Text(
                                    text = "Tap to speak • Neural Voice",
                                    style = Typography.labelSmall.copy(fontSize = 11.sp),
                                    color = CyanBright
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .background(CyanDeep.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .border(1.dp, CyanNeon.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "LIVE",
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = CyanNeon
                            )
                        }
                    }
                }

                // Dual Primary Action Cards: Chat & Work Mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PrimaryActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ChatBubbleOutline,
                        title = "Chat",
                        subtitle = "Start Conversation",
                        onClick = onNavigateToChat,
                        testTag = "home_chat_button"
                    )

                    PrimaryActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Bolt,
                        title = "Work Mode",
                        subtitle = "Landscape Workspace",
                        onClick = onNavigateToWorkMode,
                        testTag = "home_work_mode_button"
                    )
                }

                // Shortcut Prompt Chips (Image, Write, Web Search, Library, 112 Features)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HomeShortcutChip(Icons.Default.Image, "Create Image", "chip_create_image", onNavigateToImageStudio)
                    HomeShortcutChip(Icons.Default.Edit, "Write or Edit", "chip_write_edit", onNavigateToChat)
                    HomeShortcutChip(Icons.Default.Language, "Search Web", "chip_search_web", onNavigateToWebAssistant)
                    HomeShortcutChip(Icons.Default.FolderOpen, "Library", "chip_library", onNavigateToLibrary)
                    HomeShortcutChip(Icons.Default.Explore, "112 Tools", "chip_all_tools", onNavigateToFeatures)
                }

                // Quick Actions section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Actions",
                            style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = TextWhite
                        )

                        Row(
                            modifier = Modifier
                                .clickable { onNavigateToFeatures() }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "See All (112)",
                                style = Typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                color = CyanBright
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "See All Features",
                                tint = CyanBright,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QuickActionItem(
                            icon = Icons.Default.Search,
                            label = "Features",
                            onClick = onNavigateToFeatures,
                            testTag = "quick_search_features"
                        )

                        QuickActionItem(
                            icon = Icons.Default.Memory,
                            label = "Memory",
                            onClick = onNavigateToMemory,
                            testTag = "quick_memory"
                        )

                        QuickActionItem(
                            icon = Icons.Default.Smartphone,
                            label = if (isFlashOn) "Torch ON" else "Torch",
                            isActive = isFlashOn,
                            onClick = {
                                viewModel.toggleFlashlight()
                                Toast.makeText(context, if (isFlashOn) "Flashlight Off" else "Flashlight On", Toast.LENGTH_SHORT).show()
                            },
                            testTag = "quick_device_actions"
                        )

                        QuickActionItem(
                            icon = Icons.Default.Settings,
                            label = "Settings",
                            onClick = onNavigateToSettings,
                            testTag = "quick_settings"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. DOCKED DARK ROUNDED PILL COMPOSER ("Ask AERIS")
            AerisRoundedComposer(
                inputText = homeInput,
                onInputChange = { homeInput = it },
                voiceState = uiState.voiceState,
                isStreaming = uiState.streamingText != null,
                onSend = submitHomePrompt,
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
                containerTag = "home_composer_container",
                plusTag = "home_composer_plus_button",
                inputTag = "home_input_field",
                micTag = "home_composer_mic_button",
                voiceModeTag = "home_voice_mode_button",
                sendTag = "home_send_button",
                maxLines = 3
            )
        }
    }
}

@Composable
private fun HomeShortcutChip(
    icon: ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = RoundedCornerShape(18.dp),
        color = HoloSurfaceVariant,
        border = BorderStroke(1.dp, HoloCardBorder.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(label, style = Typography.labelSmall.copy(fontSize = 11.sp), color = TextWhite)
        }
    }
}

@Composable
private fun PrimaryActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = HoloSurface,
        border = BorderStroke(1.dp, CyanGlow)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(CyanDeep.copy(alpha = 0.35f), HoloSurface)
                    )
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = CyanNeon,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = title,
                    style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    color = TextWhite
                )
                Text(
                    text = subtitle,
                    style = Typography.labelSmall.copy(fontSize = 10.sp),
                    color = CyanBright
                )
            }
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    if (isActive) CyanNeon.copy(alpha = 0.3f) else HoloSurfaceVariant,
                    CircleShape
                )
                .border(
                    1.dp,
                    if (isActive) CyanNeon else HoloCardBorder.copy(alpha = 0.6f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) CyanNeon else CyanBright,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = Typography.labelSmall.copy(fontSize = 10.sp),
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}
