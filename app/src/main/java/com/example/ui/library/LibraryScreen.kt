package com.example.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.ImageGenResult
import com.example.ui.MainViewModel
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDeep
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.HoloBlack
import com.example.ui.theme.HoloCardBorder
import com.example.ui.theme.HoloSurface
import com.example.ui.theme.HoloSurfaceVariant
import com.example.ui.theme.TextCoolGrey
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.theme.Typography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class LibraryFilter(val label: String) {
    ALL("All"),
    CHATS("Chats"),
    FILES("Project Files"),
    IMAGES("Images")
}

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenImageStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.conversations.collectAsState()
    val projectFiles by viewModel.allProjectFiles.collectAsState()
    val imageGenResult by viewModel.imageGenResult.collectAsState()

    var filter by remember { mutableStateOf(LibraryFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val dateFormatter = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    val filteredConversations = remember(conversations, searchQuery) {
        conversations.filter {
            searchQuery.isBlank() ||
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.summary.contains(searchQuery, ignoreCase = true) ||
                (it.lastMessagePreview?.contains(searchQuery, ignoreCase = true) == true)
        }
    }
    val filteredFiles = remember(projectFiles, searchQuery) {
        projectFiles.filter {
            searchQuery.isBlank() ||
                it.fileName.contains(searchQuery, ignoreCase = true) ||
                it.previewText.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HoloBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("library_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .background(HoloSurfaceVariant, CircleShape)
                        .border(1.dp, CyanGlow, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CyanNeon, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("AERIS Library", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextWhite)
                    Text("Local Chats, Project Files & Studio Media", style = Typography.labelSmall.copy(fontSize = 10.sp), color = CyanBright)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().testTag("library_search_field"),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyanNeon) },
            placeholder = { Text("Search chats, summaries, or files...", color = TextMuted, fontSize = 13.sp) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanNeon,
                unfocusedBorderColor = HoloCardBorder,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedContainerColor = HoloSurface,
                unfocusedContainerColor = HoloSurface
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(LibraryFilter.entries) { item ->
                val selected = filter == item
                FilterChip(
                    selected = selected,
                    onClick = { filter = item },
                    label = { Text(item.label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanDeep,
                        selectedLabelColor = CyanNeon,
                        containerColor = HoloSurface,
                        labelColor = TextCoolGrey
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = if (selected) CyanNeon else HoloCardBorder
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filter == LibraryFilter.ALL || filter == LibraryFilter.IMAGES) {
                item {
                    Text("Studio Artwork", style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = CyanBright)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = HoloSurface,
                        border = BorderStroke(1.dp, HoloCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val successImage = imageGenResult as? ImageGenResult.Success
                            if (successImage != null) {
                                Image(
                                    bitmap = successImage.bitmap.asImageBitmap(),
                                    contentDescription = "Generated artwork (${successImage.modelUsed})",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Text("Model: ${successImage.modelUsed}", style = Typography.labelSmall, color = TextWhite, maxLines = 2)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "No generated artwork in this session yet. Saved PNGs appear in Pictures/AERIS.",
                                        style = Typography.bodySmall.copy(fontSize = 11.sp),
                                        color = TextCoolGrey
                                    )
                                }
                            }
                            Button(
                                onClick = onOpenImageStudio,
                                colors = ButtonDefaults.buttonColors(containerColor = CyanDeep),
                                border = BorderStroke(1.dp, CyanNeon),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Open Image Studio", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (filter == LibraryFilter.ALL || filter == LibraryFilter.FILES) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Project & Attached Files (${filteredFiles.size})", style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = CyanBright)
                }
                if (filteredFiles.isEmpty()) {
                    item {
                        Text("No project files saved yet.", style = Typography.bodySmall, color = TextMuted)
                    }
                } else {
                    items(filteredFiles, key = { "file_${it.id}" }) { ref ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = HoloSurface,
                            border = BorderStroke(1.dp, HoloCardBorder)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ref.fileName, style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextWhite)
                                    Text(
                                        text = "${ref.sizeBytes} bytes • ${ref.previewText.take(64).replace('\n', ' ')}",
                                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                                        color = TextCoolGrey,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (filter == LibraryFilter.ALL || filter == LibraryFilter.CHATS) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Conversations (${filteredConversations.size})", style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = CyanBright)
                }
                items(filteredConversations, key = { "conv_${it.id}" }) { conv ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectConversation(conv.id, conv.title)
                                onOpenChat()
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = HoloSurface,
                        border = BorderStroke(1.dp, HoloCardBorder)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(conv.title, style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = conv.lastMessagePreview?.take(80) ?: conv.summary.ifBlank { "${conv.messageCount} messages" },
                                    style = Typography.labelSmall.copy(fontSize = 10.sp),
                                    color = TextCoolGrey,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(dateFormatter.format(Date(conv.updatedAt)), style = Typography.labelSmall.copy(fontSize = 9.sp), color = TextMuted)
                        }
                    }
                }
            }
        }
    }
}
