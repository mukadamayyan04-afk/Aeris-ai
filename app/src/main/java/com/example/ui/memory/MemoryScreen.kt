package com.example.ui.memory

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.MemoryItem
import com.example.ui.MainViewModel
import com.example.ui.components.HoloCard
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDeep
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanTeal
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.HoloBlack
import com.example.ui.theme.HoloCardBorder
import com.example.ui.theme.HoloSurface
import com.example.ui.theme.HoloSurfaceVariant
import com.example.ui.theme.TextCoolGrey
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.theme.Typography

@Composable
fun MemoryScreen(
    viewModel: MainViewModel,
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
    onResumeConversation: (Long, String) -> Unit = { _, _ -> },
    onNavigateToProjects: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val memories by viewModel.memories.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val memoryEnabled by viewModel.memoryEnabled.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) } // 0: Saved Memories, 1: Summaries, 2: Search
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var newKey by remember { mutableStateOf("") }
    var newContent by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("My Interests") }
    var newProjectTag by remember { mutableStateOf("") }

    var sensitiveConsentPending by remember { mutableStateOf<MemoryItem?>(null) }
    var memoryToEdit by remember { mutableStateOf<MemoryItem?>(null) }
    var editContentInput by remember { mutableStateOf("") }
    var editTagInput by remember { mutableStateOf("") }

    val defaultCategories = listOf("My Interests", "Study Notes", "Life Goals", "Personal Info")
    val memoriesByCategory = memories.groupBy { it.category }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HoloBlack)
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("memory_screen")
    ) {
        // 1. Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showBackButton) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(34.dp)
                            .background(HoloSurfaceVariant, CircleShape)
                            .border(1.dp, CyanGlow, CircleShape)
                            .testTag("memory_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CyanNeon, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(HoloSurfaceVariant, CircleShape)
                            .border(1.dp, CyanGlow, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Memory, contentDescription = "Memory", tint = CyanNeon, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Column {
                    Text("Memory", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp), color = TextWhite)
                    Text(
                        text = if (memoryEnabled) "Active Recall ON" else "Recall Paused",
                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (memoryEnabled) CyanBright else GoldAccent
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = memoryEnabled,
                    onCheckedChange = { viewModel.setMemoryEnabled(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanDeep)
                )
                IconButton(onClick = { selectedTab = 2 }) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = CyanBright, modifier = Modifier.size(20.dp))
                }
                if (memories.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearAllDialog = true },
                        modifier = Modifier.testTag("clear_all_memories_button")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear All Memories", tint = AlertRed, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedButton(
            onClick = onNavigateToProjects,
            border = BorderStroke(1.dp, CyanGlow),
            modifier = Modifier.fillMaxWidth().testTag("open_project_workspaces_button")
        ) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Open Project Workspaces (Chats • Notes • Tasks • Files)", style = Typography.labelSmall, color = CyanNeon)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Segmented Tabs: Saved Memories | Summaries | Search
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = HoloSurface,
            border = BorderStroke(1.dp, HoloCardBorder.copy(alpha = 0.5f))
        ) {
            Row(modifier = Modifier.padding(3.dp)) {
                listOf("Saved Memories", "Summaries", "Search").forEachIndexed { idx, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedTab = idx
                                if (idx == 0) selectedCategoryFilter = null
                            }
                            .background(if (selectedTab == idx) CyanDeep else Color.Transparent, RoundedCornerShape(18.dp))
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = Typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (selectedTab == idx) CyanNeon else TextCoolGrey
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            0 -> {
                if (selectedCategoryFilter != null) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { selectedCategoryFilter = null }) {
                            Text("<- All Folders", color = CyanNeon, style = Typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedCategoryFilter.orEmpty(),
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                            color = TextWhite
                        )
                    }

                    val folderItems = memories.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (folderItems.isEmpty()) {
                            item {
                                HoloCard(modifier = Modifier.fillMaxWidth()) {
                                    Text("No memories saved in this folder yet.", style = Typography.bodyMedium, color = TextMuted)
                                }
                            }
                        }
                        items(folderItems, key = { it.id }) { item ->
                            MemoryItemCard(
                                item = item,
                                onEdit = {
                                    memoryToEdit = item
                                    editContentInput = item.content
                                    editTagInput = item.projectTag
                                },
                                onDelete = {
                                    viewModel.deleteMemory(item.id)
                                    Toast.makeText(context, "Memory deleted", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(defaultCategories) { category ->
                            FolderCategoryCard(
                                category = category,
                                count = memoriesByCategory[category]?.size ?: 0,
                                onClick = { selectedCategoryFilter = category }
                            )
                        }
                        val customCats = memoriesByCategory.keys.filter { !defaultCategories.contains(it) }
                        items(customCats) { category ->
                            FolderCategoryCard(
                                category = category,
                                count = memoriesByCategory[category]?.size ?: 0,
                                onClick = { selectedCategoryFilter = category }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAddDialog = true }
                        .testTag("add_memory_button"),
                    shape = RoundedCornerShape(18.dp),
                    color = HoloSurface,
                    border = BorderStroke(1.dp, CyanNeon)
                ) {
                    Row(
                        modifier = Modifier
                            .background(Brush.horizontalGradient(listOf(CyanDeep.copy(alpha = 0.4f), HoloSurface)))
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Memory", tint = CyanNeon, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Add Memory", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp), color = CyanNeon)
                    }
                }
            }

            1 -> {
                // CONVERSATION SUMMARIES & CONTINUITY CONTEXT TAB
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (conversations.isEmpty()) {
                        item {
                            HoloCard(modifier = Modifier.fillMaxWidth()) {
                                Text("No conversation summaries recorded yet.", style = Typography.bodyMedium, color = TextMuted)
                            }
                        }
                    }
                    items(conversations, key = { it.id }) { conv ->
                        HoloCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(conv.title, style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = CyanNeon)
                                    Text("${conv.messageCount} msgs", style = Typography.labelSmall, color = CyanBright)
                                }
                                if (conv.projectContext.isNotBlank()) {
                                    Text("Project: ${conv.projectContext}", style = Typography.labelSmall, color = GoldAccent)
                                }
                                if (!conv.activeModelId.isNullOrBlank()) {
                                    Text("Last Model: ${conv.activeModelId}", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextSecondary)
                                }
                                Text(
                                    text = conv.summary.ifBlank { conv.lastMessagePreview ?: "Session started; no rolling summary yet." },
                                    style = Typography.bodyMedium.copy(fontSize = 12.sp),
                                    color = TextWhite
                                )
                                OutlinedButton(
                                    onClick = { onResumeConversation(conv.id, conv.title) },
                                    border = BorderStroke(1.dp, CyanGlow)
                                ) {
                                    Text("Resume Session", style = Typography.labelSmall, color = CyanNeon)
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                // SEARCH TAB
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().testTag("search_memory_input"),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = CyanTeal) },
                    placeholder = { Text("Search memories or project tags...", style = Typography.bodyMedium, color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = HoloCardBorder.copy(alpha = 0.4f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = CyanNeon,
                        focusedContainerColor = HoloSurface,
                        unfocusedContainerColor = HoloSurface
                    ),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                val searchResults = if (searchQuery.isBlank()) {
                    memories
                } else {
                    memories.filter {
                        it.key.contains(searchQuery, ignoreCase = true) ||
                            it.content.contains(searchQuery, ignoreCase = true) ||
                            it.category.contains(searchQuery, ignoreCase = true) ||
                            it.projectTag.contains(searchQuery, ignoreCase = true)
                    }
                }

                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (searchResults.isEmpty()) {
                        item {
                            HoloCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No entries match '$searchQuery'." else "No memories saved yet.",
                                    style = Typography.bodyMedium,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                    items(searchResults, key = { it.id }) { item ->
                        MemoryItemCard(
                            item = item,
                            onEdit = {
                                memoryToEdit = item
                                editContentInput = item.content
                                editTagInput = item.projectTag
                            },
                            onDelete = {
                                viewModel.deleteMemory(item.id)
                                Toast.makeText(context, "Memory deleted", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("+ ADD NEW MEMORY", style = Typography.titleMedium, color = CyanNeon) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        label = { Text("Category (e.g. My Interests, Study Notes)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanNeon, unfocusedBorderColor = HoloCardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        label = { Text("Topic / Title") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanNeon, unfocusedBorderColor = HoloCardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newContent,
                        onValueChange = { newContent = it },
                        label = { Text("Fact / Memory Content") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanNeon, unfocusedBorderColor = HoloCardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                    )
                    OutlinedTextField(
                        value = newProjectTag,
                        onValueChange = { newProjectTag = it },
                        label = { Text("Project Tag (optional)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanNeon, unfocusedBorderColor = HoloCardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newKey.isNotBlank() && newContent.isNotBlank()) {
                            val k = newKey.trim()
                            val c = newContent.trim()
                            val cat = newCategory.trim().ifBlank { "General" }
                            val tag = newProjectTag.trim()
                            if (viewModel.aerisAiEngine.longTermMemoryEngine.looksSensitive(k, c)) {
                                sensitiveConsentPending = MemoryItem(key = k, content = c, category = cat, projectTag = tag, isSensitive = true)
                                showAddDialog = false
                            } else {
                                viewModel.saveMemory(k, c, cat, tag)
                                newKey = ""; newContent = ""; newProjectTag = ""
                                showAddDialog = false
                                Toast.makeText(context, "Memory saved to Room", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) { Text("SAVE", color = CyanNeon) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("CANCEL", color = TextSecondary) }
            },
            containerColor = HoloSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    sensitiveConsentPending?.let { pending ->
        AlertDialog(
            onDismissRequest = { sensitiveConsentPending = null },
            title = { Text("SENSITIVE INFO CONSENT", style = Typography.titleMedium, color = GoldAccent) },
            text = {
                Text(
                    text = "This memory appears to contain sensitive details (e.g., password, card, PIN, or secret token). Confirm you want AERIS to store this locally in Room memory?",
                    style = Typography.bodyMedium,
                    color = TextWhite
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveMemory(pending.key, pending.content, pending.category, pending.projectTag)
                    newKey = ""; newContent = ""; newProjectTag = ""
                    sensitiveConsentPending = null
                    Toast.makeText(context, "Memory saved with explicit consent", Toast.LENGTH_SHORT).show()
                }) { Text("CONFIRM SAVE", color = GoldAccent) }
            },
            dismissButton = {
                TextButton(onClick = { sensitiveConsentPending = null }) { Text("CANCEL", color = AlertRed) }
            },
            containerColor = HoloSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    memoryToEdit?.let { target ->
        AlertDialog(
            onDismissRequest = { memoryToEdit = null },
            title = { Text("EDIT MEMORY", style = Typography.titleMedium, color = CyanNeon) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editContentInput,
                        onValueChange = { editContentInput = it },
                        label = { Text("Content (${target.key})") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanNeon, unfocusedBorderColor = HoloCardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                    )
                    OutlinedTextField(
                        value = editTagInput,
                        onValueChange = { editTagInput = it },
                        label = { Text("Project Tag") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanNeon, unfocusedBorderColor = HoloCardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateMemory(
                            target.copy(
                                content = editContentInput.trim(),
                                projectTag = editTagInput.trim()
                            )
                        )
                        memoryToEdit = null
                        Toast.makeText(context, "Memory updated", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("UPDATE", color = CyanNeon) }
            },
            dismissButton = {
                TextButton(onClick = { memoryToEdit = null }) { Text("CANCEL", color = TextSecondary) }
            },
            containerColor = HoloSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("CLEAR ALL MEMORIES?", style = Typography.titleMedium, color = AlertRed) },
            text = {
                Text(
                    text = "This permanently deletes all ${memories.size} stored local memories from Room. Continue, Sir ji?",
                    style = Typography.bodyMedium,
                    color = TextWhite
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllMemories()
                        showClearAllDialog = false
                        Toast.makeText(context, "All memories cleared", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("confirm_clear_all_memories_button")
                ) { Text("CLEAR ALL", color = AlertRed) }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) { Text("CANCEL", color = CyanNeon) }
            },
            containerColor = HoloSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun FolderCategoryCard(category: String, count: Int, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = HoloSurface,
        border = BorderStroke(1.dp, HoloCardBorder.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(CyanDeep.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .border(1.dp, CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Folder, contentDescription = "Folder", tint = CyanNeon, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(category, style = Typography.titleMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = TextWhite)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("$count memories", style = Typography.labelSmall.copy(fontSize = 11.sp), color = TextSecondary)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = CyanBright, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun MemoryItemCard(item: MemoryItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    HoloCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.key.uppercase(), style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = CyanBright)
                    if (item.projectTag.isNotBlank()) {
                        Text("[${item.projectTag}]", style = Typography.labelSmall.copy(fontSize = 10.sp), color = GoldAccent)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(item.content, style = Typography.bodyMedium.copy(fontSize = 13.sp), color = TextWhite)
            }
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = CyanTeal, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AlertRed, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
