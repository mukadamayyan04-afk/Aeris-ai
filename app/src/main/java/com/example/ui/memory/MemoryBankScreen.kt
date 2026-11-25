package com.example.ui.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.MemoryItem
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

@Composable
fun MemoryBankScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val memories by viewModel.memories.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var newKey by remember { mutableStateOf("") }
    var newContent by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("Preferences") }

    val filtered = if (searchQuery.isBlank()) {
        memories
    } else {
        memories.filter {
            it.key.contains(searchQuery, ignoreCase = true) ||
            it.content.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true)
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
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = CyanNeon
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "LOCAL MEMORY BANK",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = CyanNeon
                    )
                    Text(
                        text = "User-controlled Room database • Shared across modes",
                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextSecondary
                    )
                }
            }

            Row {
                IconButton(onClick = { viewModel.clearAllMemories() }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All Memories", tint = StatusError)
                }
                IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("add_memory_button")) {
                    Icon(Icons.Default.Add, contentDescription = "Add Memory", tint = CyanNeon)
                }
            }
        }

        // Privacy Banner
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            color = HoloSurface,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyanGlow)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Shield, contentDescription = "Privacy", tint = CyanNeon, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Zero Silent Logging: AERIS only retains memories you explicitly approve or store. All items stay locally on your device in encrypted Room storage.",
                    style = Typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextSecondary
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = CyanTeal)
            },
            placeholder = { Text("Search memories by key or content...", style = Typography.bodyMedium, color = TextMuted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanNeon,
                unfocusedBorderColor = HoloCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = CyanNeon,
                focusedContainerColor = HoloSurface,
                unfocusedContainerColor = HoloSurface
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // Memories List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    HoloCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("MEMORY BANK EMPTY", style = Typography.labelLarge, color = CyanTeal)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "No memories recorded yet. Tap + to teach AERIS facts, personal preferences, or task reminders.",
                                style = Typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            items(filtered, key = { it.id }) { item ->
                MemoryItemRow(
                    memory = item,
                    onDelete = { viewModel.deleteMemory(item.id) }
                )
            }
        }
    }

    // Add Memory Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text("ADD NEW MEMORY", style = Typography.titleMedium, color = CyanNeon)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        label = { Text("Topic / Key (e.g., User Name, Coffee)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanNeon, unfocusedBorderColor = HoloCardBorder)
                    )
                    OutlinedTextField(
                        value = newContent,
                        onValueChange = { newContent = it },
                        label = { Text("Fact / Memory Content") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanNeon, unfocusedBorderColor = HoloCardBorder)
                    )
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        label = { Text("Category (Preferences, Work, Routine)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanNeon, unfocusedBorderColor = HoloCardBorder)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newKey.isNotBlank() && newContent.isNotBlank()) {
                            viewModel.saveMemory(newKey.trim(), newContent.trim(), newCategory.trim())
                            newKey = ""
                            newContent = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("SAVE TO ROOM", color = CyanNeon)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = HoloSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun MemoryItemRow(
    memory: MemoryItem,
    onDelete: () -> Unit
) {
    HoloCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = memory.key.uppercase(),
                        style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = CyanBright
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = HoloSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanTeal.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = memory.category,
                            style = Typography.labelSmall.copy(fontSize = 9.sp),
                            color = CyanTeal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = memory.content,
                    style = Typography.bodyMedium,
                    color = TextPrimary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Memory",
                    tint = StatusError.copy(alpha = 0.8f)
                )
            }
        }
    }
}
