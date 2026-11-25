package com.example.ui.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AerisReminder
import com.example.tasks.AerisTask
import com.example.tasks.TaskStatus
import com.example.tasks.TaskType
import com.example.ui.MainViewModel
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDeep
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.HoloCardBorder
import com.example.ui.theme.HoloSurface
import com.example.ui.theme.HoloSurfaceVariant
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextCoolGrey
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.theme.Typography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationTasksAndRemindersTab(
    viewModel: MainViewModel,
    tasks: List<AerisTask>,
    reminders: List<AerisReminder>,
    onNavigateToModels: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormatter = SimpleDateFormat("dd MMM, HH:mm", Locale.US)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Quick Actions Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickTaskChip(
                    icon = Icons.Default.Speed,
                    label = "Run Sweep",
                    onClick = { viewModel.taskManager.runHardwareSweep() },
                    modifier = Modifier.weight(1f)
                )
                QuickTaskChip(
                    icon = Icons.Default.Memory,
                    label = "Index Memory",
                    onClick = { viewModel.taskManager.runMemoryIndexing() },
                    modifier = Modifier.weight(1f)
                )
                QuickTaskChip(
                    icon = Icons.Default.Refresh,
                    label = "Purge Cache",
                    onClick = { viewModel.taskManager.runCacheCleanup() },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Scheduled Reminders Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SCHEDULED REMINDERS (${reminders.size})",
                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = CyanBright
                )
                OutlinedButton(
                    onClick = { viewModel.createReminder("AERIS Quick Reminder", 15) },
                    border = BorderStroke(1.dp, CyanGlow)
                ) {
                    Icon(Icons.Default.AddAlarm, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+15m Reminder", color = CyanNeon, fontSize = 11.sp)
                }
            }
        }

        if (reminders.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = HoloSurfaceVariant,
                    border = BorderStroke(1.dp, HoloCardBorder)
                ) {
                    Text(
                        text = "No scheduled reminders yet. Say 'Remind me in 15 minutes' or tap +15m Reminder.",
                        style = Typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextMuted,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        } else {
            items(reminders, key = { "rem_${it.id}" }) { reminder ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = HoloSurface,
                    border = BorderStroke(1.dp, if (reminder.isCompleted) HoloCardBorder else CyanGlow.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.markReminderCompleted(reminder.id, !reminder.isCompleted) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (reminder.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = "Toggle Reminder",
                                    tint = if (reminder.isCompleted) StatusSuccess else CyanNeon,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = reminder.title,
                                    style = Typography.titleSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                                    color = if (reminder.isCompleted) TextMuted else TextWhite
                                )
                                Text(
                                    text = "${timeFormatter.format(Date(reminder.triggerAtMillis))} • ${reminder.source}",
                                    style = Typography.labelSmall.copy(fontSize = 10.sp),
                                    color = TextCoolGrey
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.deleteReminder(reminder.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Reminder", tint = AlertRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Background Tasks Section
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "BACKGROUND TASKS (${tasks.size})",
                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = CyanBright
            )
        }

        if (tasks.isEmpty()) {
            item {
                Text(
                    text = "No background sweeps or model downloads queued.",
                    style = Typography.bodySmall,
                    color = TextCoolGrey,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        } else {
            items(tasks, key = { "task_${it.id}" }) { task ->
                NotificationTaskCard(
                    task = task,
                    onPause = { viewModel.taskManager.pauseTask(task.id) },
                    onRetry = { viewModel.taskManager.retryTask(task.id) },
                    onCancel = { viewModel.taskManager.cancelTask(task.id) },
                    onOpen = { if (task.type == TaskType.MODEL_DOWNLOAD) onNavigateToModels() }
                )
            }
        }
    }
}

@Composable
private fun QuickTaskChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(36.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = HoloSurface,
        border = BorderStroke(1.dp, CyanGlow)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = TextWhite)
        }
    }
}

@Composable
private fun NotificationTaskCard(
    task: AerisTask,
    onPause: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onOpen: () -> Unit
) {
    val statusColor = when (task.status) {
        TaskStatus.COMPLETED -> StatusSuccess
        TaskStatus.RUNNING -> CyanNeon
        TaskStatus.PAUSED -> StatusWarning
        TaskStatus.FAILED -> AlertRed
        else -> TextCoolGrey
    }

    val icon = when (task.type) {
        TaskType.MODEL_DOWNLOAD -> Icons.Default.CloudDownload
        TaskType.MEMORY_INDEXING -> Icons.Default.Memory
        TaskType.HARDWARE_SWEEP -> Icons.Default.Speed
        TaskType.CACHE_CLEANUP -> Icons.Default.Refresh
        TaskType.BACKUP -> Icons.Default.Alarm
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = HoloSurface,
        border = BorderStroke(1.dp, HoloCardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(CyanDeep.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(task.name, style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp), color = TextWhite)
                        Text(task.statusMessage, style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextSecondary)
                    }
                }

                Text(
                    text = "${task.status} (${task.progress}%)",
                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                    color = statusColor
                )
            }

            if (task.status == TaskStatus.RUNNING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { task.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = CyanNeon,
                    trackColor = HoloSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (task.status == TaskStatus.RUNNING) {
                    TextButton(onClick = onPause) {
                        Icon(Icons.Default.Pause, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Pause", color = StatusWarning, fontSize = 10.sp)
                    }
                }

                if (task.status == TaskStatus.FAILED || task.status == TaskStatus.PAUSED) {
                    TextButton(onClick = onRetry) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Retry", color = CyanNeon, fontSize = 10.sp)
                    }
                }

                TextButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = AlertRed, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Dismiss", color = AlertRed, fontSize = 10.sp)
                }

                if (task.type == TaskType.MODEL_DOWNLOAD) {
                    TextButton(onClick = onOpen) {
                        Text("Open", color = CyanBright, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
