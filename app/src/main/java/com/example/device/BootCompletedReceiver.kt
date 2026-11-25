package com.example.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.repository.SettingsRepository
import com.example.notifications.AerisNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Android 14+ compliant opt-in boot receiver:
 * Never starts FOREGROUND_SERVICE_TYPE_MICROPHONE directly from BOOT_COMPLETED (which Android 14+ restricts).
 * Instead, if user explicitly opted into boot standby + background listening, posts a gentle standby notification.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = SettingsRepository(appContext)
                val bootOptIn = repo.startOnBootOptInFlow.first()
                val bgPref = repo.backgroundListeningEnabledFlow.first()
                if (bootOptIn && bgPref) {
                    AerisNotificationManager(appContext).showBootStandbyNotification()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
