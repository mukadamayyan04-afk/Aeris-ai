package com.example.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.runtime.collectAsState
import com.example.ai.AerisPersonalityStyle
import com.example.ai.AerisResponseLength
import com.example.domain.model.OperatingMode
import com.example.domain.model.PermissionStateItem
import com.example.domain.model.PermissionStateStatus
import com.example.domain.model.UserFeedbackRecord
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
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextCoolGrey
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.theme.Typography

enum class SettingsDialogType {
    OPERATING_MODE,
    APPEARANCE,
    PERSONALITY,
    SAFETY_ACTION_CONFIRM,
    FEEDBACK_RULES,
    PERMISSIONS,
    ABOUT
}

@Composable
fun SettingsDialogHost(
    activeDialog: SettingsDialogType?,
    onDismiss: () -> Unit,
    viewModel: MainViewModel,
    currentMode: OperatingMode,
    animationsEnabled: Boolean,
    visualEffectsEnabled: Boolean,
    batterySaverHolo: Boolean,
    deviceActionConfirmSensitive: Boolean,
    feedbackRules: List<UserFeedbackRecord>,
    permissionItems: List<PermissionStateItem>
) {
    val context = LocalContext.current
    var pendingRequestedPermission by remember { mutableStateOf<String?>(null) }

    val runtimePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        pendingRequestedPermission?.let { perm ->
            viewModel.markPermissionRequested(perm)
        }
        viewModel.refreshPermissionStates()
    }

    when (activeDialog) {
        SettingsDialogType.OPERATING_MODE -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("DISPLAY & OPERATING MODE", style = Typography.titleMedium, color = CyanNeon) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OperatingMode.entries.forEach { mode ->
                            val isSelected = currentMode == mode
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setOperatingMode(mode)
                                        onDismiss()
                                    }
                                    .testTag("mode_option_${mode.name.lowercase()}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) CyanDeep.copy(alpha = 0.45f) else HoloSurfaceVariant,
                                border = BorderStroke(1.dp, if (isSelected) CyanNeon else HoloCardBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mode.title,
                                            style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSelected) CyanNeon else TextWhite
                                        )
                                        Text(
                                            text = mode.subtitle,
                                            style = Typography.labelSmall.copy(fontSize = 10.sp),
                                            color = TextCoolGrey
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = CyanNeon,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE", color = CyanNeon) } },
                containerColor = HoloSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        SettingsDialogType.APPEARANCE -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("APPEARANCE & PERFORMANCE", style = Typography.titleMedium, color = CyanNeon) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Active Theme: Black + Cyan / Purple Holographic", style = Typography.bodySmall, color = TextCoolGrey)
                        SettingSwitchRow(
                            label = "Holographic Ring Animations",
                            checked = animationsEnabled,
                            onCheckedChange = { viewModel.setAnimationsEnabled(it) }
                        )
                        SettingSwitchRow(
                            label = "Audio Visualizer Glow",
                            checked = visualEffectsEnabled,
                            onCheckedChange = { viewModel.setVisualEffectsEnabled(it) }
                        )
                        SettingSwitchRow(
                            label = "Battery-Saver Hologram (Reduced Motion)",
                            checked = batterySaverHolo,
                            onCheckedChange = { viewModel.setBatterySaverHolo(it) }
                        )
                    }
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text("DONE", color = CyanNeon) } },
                containerColor = HoloSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        SettingsDialogType.PERSONALITY -> {
            val currentStyle by viewModel.personalityStyle.collectAsState()
            val currentLength by viewModel.responseLength.collectAsState()
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("AERIS PERSONALITY & STYLE", style = Typography.titleMedium, color = CyanNeon) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Tone & Demeanor", style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = CyanBright)
                        AerisPersonalityStyle.entries.forEach { style ->
                            val selected = currentStyle == style
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setPersonalityStyle(style) }
                                    .testTag("personality_style_${style.name.lowercase()}"),
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) CyanDeep.copy(alpha = 0.45f) else HoloSurfaceVariant,
                                border = BorderStroke(1.dp, if (selected) CyanNeon else HoloCardBorder)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(style.title, style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = if (selected) CyanNeon else TextWhite)
                                    Text(style.directive, style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextCoolGrey)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Response Length", style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = CyanBright)
                        AerisResponseLength.entries.forEach { len ->
                            val selected = currentLength == len
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setResponseLength(len) }
                                    .testTag("response_length_${len.name.lowercase()}"),
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) CyanDeep.copy(alpha = 0.45f) else HoloSurfaceVariant,
                                border = BorderStroke(1.dp, if (selected) CyanNeon else HoloCardBorder)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(len.title, style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = if (selected) CyanNeon else TextWhite)
                                    Text(len.directive, style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextCoolGrey)
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text("DONE", color = CyanNeon) } },
                containerColor = HoloSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        SettingsDialogType.SAFETY_ACTION_CONFIRM -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("DEVICE ACTION SAFETY", style = Typography.titleMedium, color = CyanNeon) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Require explicit confirmation before AERIS launches another app or opens Android settings pages from chat/voice prompts.",
                            style = Typography.bodySmall,
                            color = TextCoolGrey
                        )
                        SettingSwitchRow(
                            label = "Confirm Sensitive Device Actions",
                            checked = deviceActionConfirmSensitive,
                            onCheckedChange = { viewModel.setDeviceActionConfirmSensitive(it) }
                        )
                    }
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text("DONE", color = CyanNeon) } },
                containerColor = HoloSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        SettingsDialogType.FEEDBACK_RULES -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("LEARNED FEEDBACK RULES", style = Typography.titleMedium, color = CyanNeon) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Approved rules adapt AERIS's prompt style and continuity instructions cleanly. Underlying model weights are never altered.",
                            style = Typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextMuted
                        )
                        if (feedbackRules.isEmpty()) {
                            Text(
                                text = "No feedback logged yet. Use Thumbs Up or Thumbs Down on AERIS replies in Chat.",
                                style = Typography.bodySmall,
                                color = TextCoolGrey
                            )
                        } else {
                            feedbackRules.forEach { rule ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = HoloSurfaceVariant,
                                    border = BorderStroke(1.dp, if (rule.isApproved) CyanGlow else HoloCardBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${rule.category} • ${if (rule.rating > 0) "Thumbs Up" else "Thumbs Down"}",
                                                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                                color = if (rule.isApproved) StatusSuccess else CyanBright
                                            )
                                            Text(
                                                text = rule.ruleText,
                                                style = Typography.bodySmall.copy(fontSize = 11.sp),
                                                color = TextWhite
                                            )
                                        }
                                        Switch(
                                            checked = rule.isApproved,
                                            onCheckedChange = { viewModel.approveFeedbackRule(rule.id, it) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanDeep)
                                        )
                                        IconButton(onClick = { viewModel.deleteFeedbackRule(rule.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Rule", tint = AlertRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE", color = CyanNeon) } },
                containerColor = HoloSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        SettingsDialogType.PERMISSIONS -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("PERMISSION MANAGER", style = Typography.titleMedium, color = CyanNeon) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        permissionItems.forEach { item ->
                            val statusColor = when (item.status) {
                                PermissionStateStatus.GRANTED -> StatusSuccess
                                PermissionStateStatus.DENIED -> AlertRed
                                PermissionStateStatus.RESTRICTED -> StatusWarning
                                PermissionStateStatus.NOT_REQUESTED -> CyanBright
                            }
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = HoloSurfaceVariant,
                                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.title,
                                            style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = TextWhite,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = item.status.label,
                                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                            color = statusColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = item.explanation,
                                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                                        color = TextCoolGrey
                                    )
                                    if (item.isInstallTimeOnly) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Install-time (Manifest) — no runtime prompt needed",
                                            style = Typography.labelSmall.copy(fontSize = 9.sp),
                                            color = TextMuted
                                        )
                                    } else if (item.status != PermissionStateStatus.GRANTED) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (item.isRuntimeRequestable) {
                                                Button(
                                                    onClick = {
                                                        pendingRequestedPermission = item.androidPermission
                                                        runtimePermLauncher.launch(item.androidPermission)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Request", color = HoloBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            item.settingsAction?.let { actionStr ->
                                                Button(
                                                    onClick = {
                                                        val intent = Intent(actionStr).apply {
                                                            if (actionStr == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                                                                data = Uri.fromParts("package", context.packageName, null)
                                                            }
                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        }
                                                        runCatching { context.startActivity(intent) }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = CyanDeep),
                                                    border = BorderStroke(1.dp, CyanNeon),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Open Settings", color = TextWhite, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE", color = CyanNeon) } },
                containerColor = HoloSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        SettingsDialogType.ABOUT -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("ABOUT AERIS AI", style = Typography.titleMedium, color = CyanNeon) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("AERIS AI v1.0.0", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = TextWhite)
                        Text(
                            text = "Personal AI Assistant for Redmi 13 5G / Android 8+. Built with an 11-engine AI Brain, Firebase AI Logic + App Check, OpenAI-compatible SSE support, offline fallback, continuous Work Mode, Notification Announcer, Game Coach, and Image Studio.",
                            style = Typography.bodySmall,
                            color = TextCoolGrey
                        )
                        Text(
                            text = "Free-First • Ad-Free • Privacy Protected",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = CyanNeon
                        )
                    }
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE", color = CyanNeon) } },
                containerColor = HoloSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        null -> Unit
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = Typography.bodySmall.copy(color = TextWhite),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanDeep)
        )
    }
}
