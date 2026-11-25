package com.example.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.device.FloatingAssistantService
import com.example.domain.model.PermissionStateStatus
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

@Composable
fun PermissionManagerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionItems by viewModel.permissionItems.collectAsState()
    val floatingEnabled by viewModel.floatingAssistantEnabled.collectAsState()
    val floatingRunning by viewModel.isFloatingAssistantRunning.collectAsState()
    var pendingPerm by remember { mutableStateOf<String?>(null) }

    val runtimeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        pendingPerm?.let { viewModel.markPermissionRequested(it) }
        viewModel.refreshPermissionStates()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HoloBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("permission_manager_screen")
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
                        .testTag("permission_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CyanNeon, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Permission Manager", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextWhite)
                    Text("Live Android 8–15+ Capability Audit", style = Typography.labelSmall.copy(fontSize = 10.sp), color = CyanBright)
                }
            }
            IconButton(
                onClick = {
                    viewModel.refreshPermissionStates()
                    Toast.makeText(context, "Permissions re-checked", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .size(36.dp)
                    .background(HoloSurfaceVariant, CircleShape)
                    .border(1.dp, HoloCardBorder, CircleShape)
                    .testTag("permission_refresh_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Retry Check", tint = CyanNeon, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Floating Assistant User Control Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = HoloSurface,
            border = BorderStroke(1.dp, if (floatingRunning) CyanNeon else CyanGlow.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PictureInPictureAlt, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Floating Assistant Orb", style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextWhite)
                            Text(
                                text = when {
                                    floatingRunning -> "Active on screen • Tap orb for Chat/Voice"
                                    FloatingAssistantService.canDrawOverlays(context) -> "Overlay ready • Opt-in only, zero hidden tracking"
                                    else -> "Overlay permission restricted • Tap Settings or use in-app HUD"
                                },
                                style = Typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (floatingRunning) StatusSuccess else TextCoolGrey
                            )
                        }
                    }
                    Switch(
                        checked = floatingEnabled && floatingRunning,
                        onCheckedChange = { enable ->
                            val ok = viewModel.setFloatingAssistantEnabled(enable)
                            if (enable && !ok) {
                                Toast.makeText(
                                    context,
                                    "Allow 'Display over other apps' in Settings first, Sir ji.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanDeep),
                        modifier = Modifier.testTag("floating_assistant_switch")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val grantedCount = permissionItems.count { it.status == PermissionStateStatus.GRANTED }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = CyanBright, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$grantedCount of ${permissionItems.size} Permissions Active",
                    style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextWhite
                )
            }
            Text("Least-Privilege Policy", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextMuted)
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(permissionItems, key = { it.id }) { item ->
                val statusColor = when (item.status) {
                    PermissionStateStatus.GRANTED -> StatusSuccess
                    PermissionStateStatus.DENIED -> AlertRed
                    PermissionStateStatus.RESTRICTED -> StatusWarning
                    PermissionStateStatus.NOT_REQUESTED -> CyanBright
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("perm_card_${item.id}"),
                    shape = RoundedCornerShape(14.dp),
                    color = HoloSurface,
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                color = TextWhite,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = statusColor.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = item.status.label,
                                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(item.explanation, style = Typography.bodySmall.copy(fontSize = 11.sp), color = TextCoolGrey)
                        if (item.isInstallTimeOnly) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Install-time permission declared in AndroidManifest.xml.",
                                style = Typography.labelSmall.copy(fontSize = 9.sp),
                                color = TextMuted
                            )
                        } else if (item.status != PermissionStateStatus.GRANTED) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (item.isRuntimeRequestable) {
                                    Button(
                                        onClick = {
                                            pendingPerm = item.androidPermission
                                            runtimeLauncher.launch(item.androidPermission)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Request Permission", color = HoloBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                item.settingsAction?.let { actionStr ->
                                    Button(
                                        onClick = {
                                            val intent = Intent(actionStr).apply {
                                                if (actionStr == Settings.ACTION_APPLICATION_DETAILS_SETTINGS ||
                                                    actionStr == Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                                                ) {
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
                                        Text("Android Settings", color = TextWhite, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
