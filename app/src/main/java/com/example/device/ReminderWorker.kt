package com.example.device

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.notifications.AerisNotificationManager
import java.util.concurrent.TimeUnit

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getLong(KEY_REMINDER_ID, System.currentTimeMillis())
        val title = inputData.getString(KEY_TITLE) ?: "Scheduled AERIS Reminder"
        AerisNotificationManager(applicationContext).showReminderNotification(reminderId, title)
        return Result.success()
    }

    companion object {
        const val KEY_REMINDER_ID = "reminder_id"
        const val KEY_TITLE = "reminder_title"

        fun schedule(context: Context, reminderId: Long, title: String, delayMillis: Long) {
            val safeDelay = delayMillis.coerceAtLeast(1_000L)
            val data = Data.Builder()
                .putLong(KEY_REMINDER_ID, reminderId)
                .putString(KEY_TITLE, title)
                .build()
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(safeDelay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag("aeris_reminder_$reminderId")
                .build()
            WorkManager.getInstance(context.applicationContext).enqueue(request)
        }
    }
}
