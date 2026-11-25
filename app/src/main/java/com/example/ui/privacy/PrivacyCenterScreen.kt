package com.example.ui.privacy

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDeep
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.HoloBlack
import com.example.ui.theme.HoloCardBorder
import com.example.ui.theme.HoloSurface
import com.example.ui.theme.HoloSurfaceVariant
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextCoolGrey
import com.example.ui.theme.TextWhite
import com.example.ui.theme.Typography

@Composable
fun PrivacyCenterScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenPermissions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val conversations by viewModel.conversations.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val wakePrivacy by viewModel.wakeWordPrivacyMode.collectAsState()

    var confirmClearMemories by remember { mutableStateOf(false) }
    var confirmClearChats by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val json = viewModel.buildPrivacyExportJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                } ?: error("Output stream unavailable")
            }.onSuccess {
                Toast.makeText(context, "Exported AERIS JSON backup, Sir ji.", Toast.LENGTH_LONG).show()
            }.onFailure { err ->
                Toast.makeText(context, "Export failed: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HoloBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
            .testTag("privacy_center_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                Text("Privacy & Data Control Center", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextWhite)
                Text("Zero Ads • Zero Hidden Telemetry • Local SQLite Control", style = Typography.labelSmall.copy(fontSize = 10.sp), color = CyanBright)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = HoloSurface,
            border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("What AERIS Stores On-Device", style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextWhite)
                }
                Text("• Conversations: ${conversations.size}", style = Typography.bodySmall, color = TextCoolGrey)
                Text("• Long-Term Memories: ${memories.size}", style = Typography.bodySmall, color = TextCoolGrey)
                Text("• Project Workspaces: ${projects.size}", style = Typography.bodySmall, color = TextCoolGrey)
                Text("• Scheduled Reminders: ${reminders.size}", style = Typography.bodySmall, color = TextCoolGrey)
                Text("• API Credentials: Encrypted in Android KeyStore AES-GCM (never exported)", style = Typography.bodySmall, color = CyanBright)
                Text(
                    text = "• Wake-Word Privacy Buffer Purge: ${if (wakePrivacy) "Active (no rolling recordings)" else "Standard"}",
                    style = Typography.bodySmall,
                    color = TextCoolGrey
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { exportLauncher.launch("aeris_privacy_export_${System.currentTimeMillis()}.json") },
            modifier = Modifier.fillMaxWidth().testTag("privacy_export_json_button"),
            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Download, contentDescription = null, tint = HoloBlack, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Export Stored Data (JSON)", color = HoloBlack, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onOpenPermissions,
            modifier = Modifier.fillMaxWidth().testTag("privacy_open_permissions_button"),
            border = BorderStroke(1.dp, CyanGlow),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Android Permission Manager", color = CyanNeon, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = HoloSurface,
            border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Data Erasure Controls", style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = AlertRed)
                OutlinedButton(
                    onClick = { confirmClearMemories = true },
                    modifier = Modifier.fillMaxWidth().testTag("privacy_clear_memories_button"),
                    border = BorderStroke(1.dp, AlertRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = AlertRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete All Saved Memories", color = AlertRed, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { confirmClearChats = true },
                    modifier = Modifier.fillMaxWidth().testTag("privacy_clear_chats_button"),
                    border = BorderStroke(1.dp, AlertRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = AlertRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete All Conversations", color = AlertRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (confirmClearMemories) {
        AlertDialog(
            onDismissRequest = { confirmClearMemories = false },
            title = { Text("DELETE ALL MEMORIES", color = AlertRed, style = Typography.titleMedium) },
            text = { Text("This permanently deletes all ${memories.size} saved memory items on this device. Continue?", color = TextWhite) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllMemories()
                        confirmClearMemories = false
                        Toast.makeText(context, "All memories deleted", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("confirm_privacy_clear_memories")
                ) { Text("DELETE", color = AlertRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearMemories = false }) { Text("CANCEL", color = CyanNeon) }
            },
            containerColor = HoloSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (confirmClearChats) {
        AlertDialog(
            onDismissRequest = { confirmClearChats = false },
            title = { Text("DELETE ALL CONVERSATIONS", color = AlertRed, style = Typography.titleMedium) },
            text = { Text("This deletes all ${conversations.size} local conversations and messages. Continue?", color = TextWhite) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllConversations()
                        confirmClearChats = false
                        Toast.makeText(context, "All conversations cleared", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("confirm_privacy_clear_chats")
                ) { Text("DELETE", color = AlertRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearChats = false }) { Text("CANCEL", color = CyanNeon) }
            },
            containerColor = HoloSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
