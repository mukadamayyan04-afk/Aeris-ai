package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.VoiceState
import com.example.ui.AttachedDocument
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDeep
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.HoloBlack
import com.example.ui.theme.HoloCardBorder
import com.example.ui.theme.HoloDark
import com.example.ui.theme.HoloSurface
import com.example.ui.theme.HoloSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.theme.Typography

/**
 * Shared Stage 8 Dark Rounded Pill Composer ("Ask AERIS") for both HomeScreen & ChatScreen:
 * - Left + button -> Image Picker (zero storage permission), File Attach, Open Library, Tools
 * - Multiline Ask AERIS input
 * - Mic, Voice Mode Orb, and Send / Stop buttons
 */
@Composable
fun AerisRoundedComposer(
    inputText: String,
    onInputChange: (String) -> Unit,
    voiceState: VoiceState,
    isStreaming: Boolean,
    onSend: () -> Unit,
    onStopGeneration: () -> Unit,
    onMicClick: () -> Unit,
    onVoiceModeClick: () -> Unit,
    onAttachImageClick: () -> Unit,
    onAttachDocumentClick: () -> Unit,
    onOpenLibraryClick: () -> Unit,
    onOpenToolsClick: () -> Unit,
    attachedDoc: AttachedDocument? = null,
    onClearDoc: () -> Unit = {},
    attachedImageName: String? = null,
    onClearImage: () -> Unit = {},
    placeholderText: String = "Ask AERIS...",
    containerTag: String = "chat_composer_container",
    plusTag: String = "composer_plus_button",
    inputTag: String = "chat_input_field",
    micTag: String = "chat_composer_mic_button",
    voiceModeTag: String = "composer_voice_mode_button",
    sendTag: String = "send_button",
    maxLines: Int = 4,
    modifier: Modifier = Modifier
) {
    var showPlusMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(containerTag)
    ) {
        if (attachedDoc != null || attachedImageName != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                attachedDoc?.let { doc ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = HoloSurfaceVariant,
                        border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("${doc.fileName} (${doc.totalChars}ch)", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextWhite)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.Close, contentDescription = "Remove File", tint = AlertRed, modifier = Modifier.size(15.dp).clickable { onClearDoc() })
                        }
                    }
                }
                attachedImageName?.let { img ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = HoloSurfaceVariant,
                        border = BorderStroke(1.dp, CyanBright.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = CyanBright, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(img.take(24), style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextWhite)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.Close, contentDescription = "Remove Image", tint = AlertRed, modifier = Modifier.size(15.dp).clickable { onClearImage() })
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = HoloSurface,
            border = BorderStroke(1.dp, if (inputText.isNotBlank()) CyanNeon else CyanGlow.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    IconButton(
                        onClick = { showPlusMenu = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(HoloDark, CircleShape)
                            .border(1.dp, HoloCardBorder, CircleShape)
                            .testTag(plusTag)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Attach or Tools", tint = CyanNeon, modifier = Modifier.size(20.dp))
                    }

                    DropdownMenu(
                        expanded = showPlusMenu,
                        onDismissRequest = { showPlusMenu = false },
                        modifier = Modifier.background(HoloSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Attach Photo (Photo Picker)", color = TextWhite) },
                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = CyanNeon) },
                            onClick = {
                                showPlusMenu = false
                                onAttachImageClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Attach Document", color = TextWhite) },
                            leadingIcon = { Icon(Icons.Default.AttachFile, contentDescription = null, tint = CyanBright) },
                            onClick = {
                                showPlusMenu = false
                                onAttachDocumentClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Open Library", color = TextWhite) },
                            leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null, tint = CyanNeon) },
                            onClick = {
                                showPlusMenu = false
                                onOpenLibraryClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Explore Tools (100+)", color = TextWhite) },
                            leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, tint = CyanBright) },
                            onClick = {
                                showPlusMenu = false
                                onOpenToolsClick()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 38.dp, max = 108.dp)
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (inputText.isEmpty()) {
                        Text(
                            text = placeholderText,
                            style = Typography.bodyMedium.copy(fontSize = 14.sp),
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = inputText,
                        onValueChange = onInputChange,
                        textStyle = Typography.bodyMedium.copy(color = TextWhite, fontSize = 14.sp),
                        cursorBrush = SolidColor(CyanNeon),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank()) onSend()
                        }),
                        maxLines = maxLines,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(inputTag)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            if (voiceState == VoiceState.LISTENING) CyanNeon.copy(alpha = 0.3f) else HoloDark,
                            CircleShape
                        )
                        .border(
                            1.dp,
                            if (voiceState == VoiceState.LISTENING) CyanNeon else HoloCardBorder,
                            CircleShape
                        )
                        .testTag(micTag)
                ) {
                    Icon(
                        imageVector = if (voiceState == VoiceState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (voiceState == VoiceState.LISTENING) "Stop Listening" else "Voice Dictation",
                        tint = if (voiceState == VoiceState.LISTENING) CyanNeon else CyanBright,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onVoiceModeClick,
                    modifier = Modifier
                        .size(38.dp)
                        .background(CyanDeep.copy(alpha = 0.45f), CircleShape)
                        .border(1.dp, CyanGlow, CircleShape)
                        .testTag(voiceModeTag)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Voice HUD",
                        tint = CyanNeon,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(if (isStreaming) AlertRed else CyanNeon, CircleShape)
                        .clickable {
                            if (isStreaming && inputText.isBlank()) {
                                onStopGeneration()
                            } else if (inputText.isNotBlank()) {
                                onSend()
                            }
                        }
                        .testTag(sendTag),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isStreaming && inputText.isBlank()) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (isStreaming && inputText.isBlank()) "Stop Generation" else "Send Message",
                        tint = HoloBlack,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
