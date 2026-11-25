package com.example.ui.history

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Conversation
import com.example.ui.MainViewModel
import com.example.ui.components.HoloCard
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
fun ChatHistoryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onSelectConversation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.conversations.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var conversationToRename by remember { mutableStateOf<Conversation?>(null) }
    var renameTitleInput by remember { mutableStateOf("") }
    var conversationToDelete by remember { mutableStateOf<Conversation?>(null) }

    val filteredList = if (searchQuery.isBlank()) {
        conversations
    } else {
        conversations.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            (it.lastMessagePreview?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HoloBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HoloDark)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("history_back_button")) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = CyanNeon
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "CONVERSATION ARCHIVE",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = CyanNeon
                    )
                    Text(
                        text = "${conversations.size} Sessions in Room Database",
                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextSecondary
                    )
                }
            }

            IconButton(
                onClick = {
                    viewModel.createNewConversation("Session ${System.currentTimeMillis() % 1000}")
                    onSelectConversation()
                },
                modifier = Modifier
                    .size(38.dp)
                    .background(CyanDeep, CircleShape)
                    .border(1.dp, CyanNeon, CircleShape)
                    .testTag("new_conversation_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Session",
                    tint = CyanNeon,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("search_conversations_field"),
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = CyanTeal)
            },
            placeholder = {
                Text("Search by session title or text snippet...", style = Typography.bodyMedium, color = TextMuted)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanNeon,
                unfocusedBorderColor = HoloCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = CyanNeon,
                focusedContainerColor = HoloSurface,
                unfocusedContainerColor = HoloSurface
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Conversation List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filteredList.isEmpty()) {
                item {
                    HoloCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        Column {
                            Text("NO SESSIONS FOUND", style = Typography.labelLarge, color = CyanTeal)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (searchQuery.isNotBlank()) "No conversation matches '$searchQuery'." else "No conversations recorded yet. Start a new session using the + button.",
                                style = Typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            items(filteredList, key = { it.id }) { item ->
                val isCurrent = item.id == uiState.currentConversationId
                ConversationItemRow(
                    conversation = item,
                    isActive = isCurrent,
                    onClick = {
                        viewModel.selectConversation(item.id, item.title)
                        onSelectConversation()
                    },
                    onRename = {
                        conversationToRename = item
                        renameTitleInput = item.title
                    },
                    onDelete = {
                        conversationToDelete = item
                    }
                )
            }
        }
    }

    // Rename Dialog
    conversationToRename?.let { target ->
        AlertDialog(
            onDismissRequest = { conversationToRename = null },
            title = {
                Text("RENAME SESSION", style = Typography.titleMedium, color = CyanNeon)
            },
            text = {
                OutlinedTextField(
                    value = renameTitleInput,
                    onValueChange = { renameTitleInput = it },
                    label = { Text("Session Title") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = HoloCardBorder
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameTitleInput.isNotBlank()) {
                            viewModel.renameConversation(target.id, renameTitleInput.trim())
                            conversationToRename = null
                        }
                    }
                ) {
                    Text("RENAME", color = CyanNeon)
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToRename = null }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = HoloSurface,
            shape = RoundedCornerShape(14.dp)
        )
    }

    // Delete Confirmation Dialog
    conversationToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = {
                Text("DELETE CONVERSATION?", style = Typography.titleMedium, color = StatusError)
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${target.title}'? All messages in this session will be permanently erased from local Room database storage.",
                    style = Typography.bodyMedium,
                    color = TextPrimary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteConversation(target.id)
                        conversationToDelete = null
                    }
                ) {
                    Text("DELETE", color = StatusError)
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = HoloSurface,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
private fun ConversationItemRow(
    conversation: Conversation,
    isActive: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val formattedTime = remember(conversation.updatedAt) { dateFormat.format(Date(conversation.updatedAt)) }

    HoloCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        borderColor = if (isActive) CyanNeon else HoloCardBorder.copy(alpha = 0.35f),
        backgroundColor = if (isActive) HoloSurfaceVariant else HoloSurface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Session",
                        tint = if (isActive) CyanNeon else CyanTeal,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = conversation.title,
                        style = Typography.titleMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        color = if (isActive) CyanNeon else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = CyanDeep.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon)
                        ) {
                            Text(
                                text = "ACTIVE",
                                style = Typography.labelSmall.copy(fontSize = 8.sp),
                                color = CyanNeon,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = conversation.lastMessagePreview ?: "No messages in session",
                    style = Typography.bodyMedium.copy(fontSize = 12.sp),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "$formattedTime • ${conversation.messageCount} messages",
                    style = Typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextMuted
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRename, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename Session",
                        tint = CyanBright,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Session",
                        tint = StatusError.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
