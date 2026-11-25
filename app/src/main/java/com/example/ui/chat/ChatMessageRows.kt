package com.example.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.ChatMessage
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDeep
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.HoloSurface
import com.example.ui.theme.HoloSurfaceVariant
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextCoolGrey
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextWhite
import com.example.ui.theme.Typography

@Composable
fun QuickPromptChip(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = HoloSurface,
        border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.5f)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = Typography.labelSmall.copy(fontSize = 11.sp),
            color = CyanBright,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun UserMessageRow(
    message: ChatMessage,
    formattedTime: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Bottom
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp),
            color = CyanDeep.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, CyanBright.copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .clickable(onClick = onCopy)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (!message.attachmentName.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = message.attachmentName,
                            style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = CyanNeon
                        )
                    }
                }
                Text(
                    text = message.text,
                    style = Typography.bodyMedium.copy(fontSize = 14.sp),
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = formattedTime,
                        style = Typography.labelSmall.copy(fontSize = 9.sp),
                        color = CyanBright.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun AerisMessageRow(
    message: ChatMessage,
    formattedTime: String,
    isLatestAssistantMessage: Boolean,
    onCopy: () -> Unit,
    onSpeak: () -> Unit,
    onRegenerate: () -> Unit,
    onRetry: () -> Unit,
    onFeedback: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(HoloSurface)
                .border(1.dp, if (message.isError) AlertRed else CyanNeon, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.aeris_avatar),
                contentDescription = "AERIS",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp),
            color = if (message.isError) AlertRed.copy(alpha = 0.12f) else HoloSurface,
            border = BorderStroke(
                1.dp,
                if (message.isError) AlertRed.copy(alpha = 0.8f) else CyanGlow.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth(0.89f)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (message.isError) {
                    Text(
                        text = message.text,
                        style = Typography.bodyMedium.copy(fontSize = 13.sp),
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = onRetry,
                        modifier = Modifier.testTag("retry_message_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = CyanNeon, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry Request", color = CyanNeon, style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                } else {
                    MarkdownText(markdown = message.text, textColor = TextPrimary)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val modelLabel = message.modelId?.let { " • $it" }.orEmpty()
                    Text(
                        text = "$formattedTime$modelLabel",
                        style = Typography.labelSmall.copy(fontSize = 9.sp),
                        color = TextMuted,
                        modifier = Modifier.weight(1f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!message.isError) {
                            IconButton(
                                onClick = { onFeedback(1) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ThumbUp,
                                    contentDescription = "Helpful response",
                                    tint = if (message.feedbackRating > 0) StatusSuccess else TextCoolGrey,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            IconButton(
                                onClick = { onFeedback(-1) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ThumbDown,
                                    contentDescription = "Needs improvement",
                                    tint = if (message.feedbackRating < 0) AlertRed else TextCoolGrey,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        if (isLatestAssistantMessage && !message.isError) {
                            IconButton(
                                onClick = onRegenerate,
                                modifier = Modifier.size(24.dp).testTag("regenerate_message_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Regenerate response",
                                    tint = CyanBright,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        IconButton(onClick = onSpeak, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Read aloud",
                                tint = CyanBright,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy message",
                                tint = TextCoolGrey,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThinkingIndicatorBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(HoloSurface)
                .border(1.dp, CyanNeon, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.aeris_avatar),
                contentDescription = "AERIS",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = HoloSurface,
            border = BorderStroke(1.dp, CyanGlow),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(6.dp).background(CyanNeon, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Thinking...",
                    style = Typography.labelSmall.copy(fontSize = 11.sp),
                    color = CyanBright
                )
            }
        }
    }
}

@Composable
fun StreamingMessageRow(
    text: String,
    modelLabel: String,
    formattedTime: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(HoloSurface)
                .border(1.dp, CyanBright, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.aeris_avatar),
                contentDescription = "AERIS",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp),
            color = HoloSurfaceVariant,
            border = BorderStroke(1.dp, CyanNeon),
            modifier = Modifier.fillMaxWidth(0.89f)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (text.isEmpty()) {
                    Text(
                        text = "Thinking...",
                        style = Typography.bodyMedium.copy(fontSize = 14.sp),
                        color = CyanBright
                    )
                } else {
                    MarkdownText(markdown = text, textColor = TextPrimary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$formattedTime • $modelLabel",
                    style = Typography.labelSmall.copy(fontSize = 9.sp),
                    color = CyanBright
                )
            }
        }
    }
}
