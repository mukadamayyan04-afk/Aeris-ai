package com.example.ui.notifications

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.notifications.AerisNotificationListener
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationCenterScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToModels: () -> Unit = {},
    onNavigateToWebAssistant: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val tasks by viewModel.tasks.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val announcerEnabled by viewModel.notificationAnnouncerEnabled.collectAsState()
    val hideSensitive by viewModel.notificationHideSensitive.collectAsState()
    val allowedApps by viewModel.notificationAllowedApps.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val recentNotifications by AerisNotificationListener.recentNotifications.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Announcer, 1 = Tasks & Reminders
    var listenerGranted by remember { mutableStateOf(AerisNotificationListener.isPermissionGranted(context)) }
    var appSearchQuery by remember { mutableStateOf("") }
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.US)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                listenerGranted = AerisNotificationListener.isPermissionGranted(context)
                viewModel.refreshInstalledApps()
                viewModel.refreshPermissionStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val filteredApps = remember(installedApps, appSearchQuery) {
        val q = appSearchQuery.trim()
        if (q.isEmpty()) installedApps else installedApps.filter {
            it.appName.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HoloBlack)
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("notification_center_screen")
    ) {
        // 1. Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .background(HoloSurfaceVariant, CircleShape)
                        .border(1.dp, CyanGlow, CircleShape)
                        .testTag("notif_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "NOTIFICATION CENTER",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = TextWhite
                    )
                    Text(
                        text = "${if (listenerGranted) "Listener Ready" else "Access Off"} • ${tasks.size} Tasks • ${reminders.size} Reminders",
                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                        color = CyanBright
                    )
                }
            }

            IconButton(
                onClick = {
                    if (selectedTab == 0) {
                        AerisNotificationListener.clearRecent()
                    } else {
                        viewModel.taskManager.clearCompleted()
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .background(HoloSurfaceVariant, CircleShape)
                    .border(1.dp, CyanGlow, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ClearAll,
                    contentDescription = "Clear",
                    tint = CyanNeon,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Segmented Tab Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HoloSurface, RoundedCornerShape(12.dp))
                .border(1.dp, HoloCardBorder, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TabSelectorButton(
                label = "Notification Announcer",
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f)
            )
            TabSelectorButton(
                label = "Tasks & Reminders",
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedTab == 1) {
            NotificationTasksAndRemindersTab(
                viewModel = viewModel,
                tasks = tasks,
                reminders = reminders,
                onNavigateToModels = onNavigateToModels,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Permission Disclosure & Grant Card
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = HoloSurface,
                        border = BorderStroke(1.dp, if (listenerGranted) StatusSuccess else StatusWarning)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = if (listenerGranted) StatusSuccess else StatusWarning,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (listenerGranted) "Notification Access Granted" else "Notification Access Required",
                                        style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextWhite
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "AERIS only reads notifications from apps you approve below to read them aloud. No notification data ever leaves your device.",
                                style = Typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextCoolGrey
                            )
                            if (!listenerGranted) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("notification_listener_permission_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Open Notification Access Settings", color = HoloBlack, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Master Announcer Controls + Sensitive Filter + Quick Disable
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = HoloSurface,
                        border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Announce Notifications Aloud", style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextWhite)
                                    Text("Speaks incoming messages from approved apps", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextCoolGrey)
                                }
                                Switch(
                                    checked = announcerEnabled,
                                    onCheckedChange = { viewModel.setNotificationAnnouncerEnabled(it) },
                                    enabled = listenerGranted,
                                    colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanDeep)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("Hide Sensitive Content (OTP/Bank)", style = Typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = TextWhite)
                                        Text("Redacts OTP, PIN, UPI, password & bank alerts", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextMuted)
                                    }
                                }
                                Switch(
                                    checked = hideSensitive,
                                    onCheckedChange = { viewModel.setNotificationHideSensitive(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanDeep)
                                )
                            }

                            if (announcerEnabled) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.setNotificationAnnouncerEnabled(false)
                                        viewModel.stopSpeechPlayback()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("notification_quick_disable_button"),
                                    border = BorderStroke(1.dp, AlertRed)
                                ) {
                                    Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = AlertRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Quick Disable Announcer", color = AlertRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Recent Captured Notifications
                if (recentNotifications.isNotEmpty()) {
                    item {
                        Text(
                            text = "RECENT NOTIFICATIONS (${recentNotifications.size})",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = CyanBright
                        )
                    }
                    items(recentNotifications, key = { "notif_${it.id}" }) { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = HoloSurfaceVariant,
                            border = BorderStroke(1.dp, if (item.isSensitive && hideSensitive) StatusWarning else HoloCardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.appName} • ${timeFormatter.format(Date(item.timestamp))}",
                                        style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = CyanNeon
                                    )
                                    Text(
                                        text = if (item.isSensitive && hideSensitive) {
                                            "Sensitive content hidden for privacy"
                                        } else {
                                            "${item.title}: ${item.text}".trim()
                                        },
                                        style = Typography.bodySmall.copy(fontSize = 11.sp),
                                        color = TextWhite,
                                        maxLines = 2
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        viewModel.speakMessage(
                                            AerisNotificationListener.formatAnnouncementText(item, hideSensitive)
                                        )
                                    }
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Replay", tint = CyanNeon, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Allowed Apps Selector
                item {
                    Text(
                        text = "ALLOWED APPS (${allowedApps.size} SELECTED)",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = CyanBright
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = appSearchQuery,
                        onValueChange = { appSearchQuery = it },
                        placeholder = { Text("Filter installed apps...", color = TextMuted, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyanNeon) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = HoloCardBorder
                        )
                    )
                }

                if (filteredApps.isEmpty()) {
                    item {
                        Text(
                            text = "No launchable apps matched.",
                            style = Typography.bodySmall,
                            color = TextMuted
                        )
                    }
                } else {
                    items(filteredApps.take(40), key = { "app_${it.packageName}" }) { app ->
                        val isAllowed = app.packageName in allowedApps
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleNotificationAllowedApp(app.packageName) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isAllowed) CyanDeep.copy(alpha = 0.35f) else HoloSurface,
                            border = BorderStroke(1.dp, if (isAllowed) CyanNeon else HoloCardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(app.appName, style = Typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = TextWhite)
                                    Text(app.packageName, style = Typography.labelSmall.copy(fontSize = 9.sp), color = TextMuted)
                                }
                                Checkbox(
                                    checked = isAllowed,
                                    onCheckedChange = { viewModel.toggleNotificationAllowedApp(app.packageName) },
                                    colors = CheckboxDefaults.colors(checkedColor = CyanNeon, checkmarkColor = HoloBlack)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabSelectorButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(36.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(9.dp),
        color = if (selected) CyanNeon else HoloSurfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = if (selected) HoloBlack else TextCoolGrey
            )
        }
    }
}
