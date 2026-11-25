package com.example.ui.navigation

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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Conversation
import com.example.ui.MainViewModel
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
import com.example.ui.theme.TextCoolGrey
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.theme.Typography

@Composable
fun AerisSideDrawerContent(
    viewModel: MainViewModel,
    onCloseDrawer: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    onNavigateToWorkMode: () -> Unit
) {
    val context = LocalContext.current
    val conversations by viewModel.conversations.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val aiOperatingMode by viewModel.aiOperatingMode.collectAsState()
    val memories by viewModel.memories.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<Conversation?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Conversation?>(null) }

    val userName = memories.find {
        it.key.equals("username", ignoreCase = true) || it.key.equals("name", ignoreCase = true)
    }?.content ?: "Sir ji"

    val filteredConversations = remember(conversations, searchQuery) {
        if (searchQuery.isBlank()) conversations.take(12)
        else conversations.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                (it.lastMessagePreview?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    ModalDrawerSheet(
        drawerContainerColor = HoloDark,
        drawerContentColor = TextWhite,
        modifier = Modifier
            .width(310.dp)
            .testTag("aeris_side_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Top AERIS Branding
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(CyanDeep.copy(alpha = 0.5f), CircleShape)
                            .border(1.5.dp, CyanNeon, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("AERIS AI", style = Typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.5.sp), color = CyanNeon)
                        Text("${aiOperatingMode.title} • Local First", style = Typography.labelSmall.copy(fontSize = 10.sp), color = CyanBright)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().testTag("drawer_search_field"),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Chats", tint = CyanNeon, modifier = Modifier.size(18.dp)) },
                placeholder = { Text("Search chats...", color = TextMuted, fontSize = 12.sp) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = HoloCardBorder,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = HoloSurface,
                    unfocusedContainerColor = HoloSurface
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Main scrollable items + Recent Conversations
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    DrawerNavItem(Icons.Default.Image, "Images", "drawer_item_images") {
                        onCloseDrawer()
                        onNavigateToRoute(AerisDestinations.IMAGE_STUDIO)
                    }
                    DrawerNavItem(Icons.Default.FolderOpen, "Library", "drawer_item_library") {
                        onCloseDrawer()
                        onNavigateToRoute(AerisDestinations.LIBRARY)
                    }
                    DrawerNavItem(Icons.Default.Tune, "Customize", "drawer_item_customize") {
                        onCloseDrawer()
                        onNavigateToRoute(AerisDestinations.CUSTOMIZE)
                    }
                    DrawerNavItem(Icons.Default.Folder, "Projects", "drawer_item_projects") {
                        onCloseDrawer()
                        onNavigateToRoute(AerisDestinations.PROJECTS)
                    }
                    DrawerNavItem(Icons.Default.ViewQuilt, "Remote / Work Mode", "drawer_item_work_mode") {
                        onCloseDrawer()
                        onNavigateToWorkMode()
                    }
                    DrawerNavItem(Icons.Default.Explore, "Explore Features (112)", "drawer_item_features") {
                        onCloseDrawer()
                        onNavigateToRoute(AerisDestinations.FEATURES)
                    }
                    DrawerNavItem(Icons.Default.Security, "Privacy & Data Center", "drawer_item_privacy") {
                        onCloseDrawer()
                        onNavigateToRoute(AerisDestinations.PRIVACY_CENTER)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = HoloCardBorder.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Conversations", style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = CyanBright)
                        Text(
                            text = "See All",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = CyanNeon,
                            modifier = Modifier
                                .clickable {
                                    onCloseDrawer()
                                    onNavigateToRoute(AerisDestinations.HISTORY)
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .testTag("drawer_see_all_chats")
                        )
                    }
                }

                items(filteredConversations, key = { it.id }) { conv ->
                    val isActive = conv.id == uiState.currentConversationId
                    var menuExpanded by remember(conv.id) { mutableStateOf(false) }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectConversation(conv.id, conv.title)
                                onCloseDrawer()
                                onNavigateToRoute(AerisDestinations.CHAT)
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isActive) CyanDeep.copy(alpha = 0.35f) else HoloSurface.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, if (isActive) CyanNeon.copy(alpha = 0.7f) else HoloCardBorder.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = if (isActive) CyanNeon else TextCoolGrey, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = conv.title,
                                style = Typography.bodySmall.copy(fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal),
                                color = if (isActive) CyanNeon else TextWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Box {
                                IconButton(
                                    onClick = { menuExpanded = true },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Conversation Options", tint = TextCoolGrey, modifier = Modifier.size(16.dp))
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                    modifier = Modifier.background(HoloSurface)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Rename", color = TextWhite) },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = CyanNeon) },
                                        onClick = {
                                            menuExpanded = false
                                            renameInput = conv.title
                                            renameTarget = conv
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = AlertRed) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AlertRed) },
                                        onClick = {
                                            menuExpanded = false
                                            deleteTarget = conv
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = HoloCardBorder.copy(alpha = 0.6f))

            // Bottom anchored actions: New Chat + Profile + Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            viewModel.createNewConversation("New AERIS Chat")
                            onCloseDrawer()
                            onNavigateToRoute(AerisDestinations.CHAT)
                        }
                        .testTag("drawer_new_chat_button"),
                    shape = RoundedCornerShape(12.dp),
                    color = CyanNeon
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AddComment, contentDescription = null, tint = HoloBlack, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Chat", style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = HoloBlack)
                    }
                }

                IconButton(
                    onClick = {
                        onCloseDrawer()
                        onNavigateToRoute(AerisDestinations.SETTINGS)
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .background(HoloSurfaceVariant, CircleShape)
                        .border(1.dp, CyanGlow, CircleShape)
                        .testTag("drawer_settings_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = CyanNeon, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = CyanBright, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Operator: $userName • Free-First Ad-Free", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextCoolGrey)
            }
        }
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Chat", color = CyanNeon, style = Typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = HoloCardBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val clean = renameInput.trim()
                    if (clean.isNotEmpty()) {
                        viewModel.renameConversation(target.id, clean)
                        Toast.makeText(context, "Renamed conversation", Toast.LENGTH_SHORT).show()
                    }
                    renameTarget = null
                }) { Text("SAVE", color = CyanNeon, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("CANCEL", color = TextCoolGrey) }
            },
            containerColor = HoloSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Conversation", color = AlertRed, style = Typography.titleMedium) },
            text = { Text("Delete \"${target.title}\" permanently from device storage?", color = TextWhite) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteConversation(target.id)
                    deleteTarget = null
                    Toast.makeText(context, "Conversation deleted", Toast.LENGTH_SHORT).show()
                }) { Text("DELETE", color = AlertRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("CANCEL", color = CyanNeon) }
            },
            containerColor = HoloSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun DrawerNavItem(
    icon: ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = CyanBright, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = Typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium), color = TextWhite)
    }
}
