package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class AerisNotificationManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_TASKS = "aeris_tasks"
        const val CHANNEL_MODELS = "aeris_models"
        const val CHANNEL_SERVICE = "aeris_service"
        const val CHANNEL_ALERTS = "aeris_alerts"

        const val NOTIFICATION_ID_WAKE_WORD = 1001
        const val NOTIFICATION_ID_GAME_COACH = 1002
        const val NOTIFICATION_ID_MODEL_DOWNLOAD = 2001
        const val NOTIFICATION_ID_ACCOUNT_PROMPT = 3001
        const val NOTIFICATION_ID_TASK_BASE = 4000
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val tasksChannel = NotificationChannel(
                CHANNEL_TASKS,
                "AERIS Background Tasks",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Status and progress of background tasks, indexing, and backups"
            }

            val modelsChannel = NotificationChannel(
                CHANNEL_MODELS,
                "AERIS Model Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Download progress and verification for local GGUF AI models"
            }

            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "AERIS Foreground Services",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing notification for wake word detection and Game Coach projection"
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "AERIS Alerts & Login Prompts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Prompts for Gmail account selection, alarms, and important errors"
            }

            notificationManager.createNotificationChannels(
                listOf(tasksChannel, modelsChannel, serviceChannel, alertsChannel)
            )
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun showModelDownloadProgress(modelName: String, progressPercent: Int, isCompleted: Boolean) {
        if (!hasNotificationPermission()) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_MODELS)
            .setSmallIcon(R.drawable.aeris_avatar_thumb)
            .setContentTitle(if (isCompleted) "Model Ready: $modelName" else "Downloading: $modelName")
            .setContentText(if (isCompleted) "GGUF weights installed and verified on-device." else "$progressPercent% downloaded")
            .setContentIntent(pendingIntent)
            .setOngoing(!isCompleted)
            .setOnlyAlertOnce(true)

        if (!isCompleted) {
            builder.setProgress(100, progressPercent, progressPercent == 0)
        } else {
            builder.setProgress(0, 0, false)
            builder.setAutoCancel(true)
        }

        notificationManager.notify(NOTIFICATION_ID_MODEL_DOWNLOAD, builder.build())
    }

    fun showAccountSelectionPrompt(accountPrompt: String) {
        if (!hasNotificationPermission()) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.aeris_avatar_thumb)
            .setContentTitle("AERIS AI — Web Assistant")
            .setContentText(accountPrompt)
            .setStyle(NotificationCompat.BigTextStyle().bigText(accountPrompt))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_ACCOUNT_PROMPT, notification)
    }

    fun showTaskNotification(
        taskId: Int,
        taskName: String,
        statusText: String,
        progress: Int,
        isOngoing: Boolean
    ) {
        if (!hasNotificationPermission()) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_TASKS)
            .setSmallIcon(R.drawable.aeris_avatar_thumb)
            .setContentTitle(taskName)
            .setContentText(statusText)
            .setContentIntent(pendingIntent)
            .setOngoing(isOngoing)

        if (isOngoing && progress in 0..100) {
            builder.setProgress(100, progress, progress == 0)
        } else {
            builder.setProgress(0, 0, false)
            builder.setAutoCancel(true)
        }

        notificationManager.notify(NOTIFICATION_ID_TASK_BASE + taskId, builder.build())
    }

    fun showImportantError(errorTitle: String, errorMessage: String) {
        if (!hasNotificationPermission()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.aeris_avatar_thumb)
            .setContentTitle(errorTitle)
            .setContentText(errorMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(errorMessage))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun showReminderNotification(reminderId: Long, title: String) {
        if (!hasNotificationPermission()) return
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.aeris_avatar_thumb)
            .setContentTitle("AERIS Reminder")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(4500 + (reminderId.toInt() % 1000), notification)
    }

    fun showBootStandbyNotification() {
        if (!hasNotificationPermission()) return
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            5101,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.aeris_avatar_thumb)
            .setContentTitle("AERIS Assistant Standby")
            .setContentText("Opt-in boot standby ready. Tap to resume AERIS voice or chat, Sir ji.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(5101, notification)
    }

    fun cancelNotification(id: Int) {
        notificationManager.cancel(id)
    }
}
