package com.example.notifications

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class AnnouncedNotification(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSensitive: Boolean = false
)

class AerisNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isListenerConnected.value = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isListenerConnected.value = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        if (pkg == packageName) return

        val extras = sbn.notification.extras ?: return
        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val rawText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        if (rawTitle.isBlank() && rawText.isBlank()) return

        val pm = packageManager
        val appName = try {
            val appInfo = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            pkg
        }

        val notification = AnnouncedNotification(
            id = "${sbn.id}_${sbn.postTime}",
            packageName = pkg,
            appName = appName,
            title = rawTitle,
            text = rawText,
            timestamp = sbn.postTime,
            isSensitive = containsSensitiveContent(rawTitle, rawText)
        )

        _latestNotification.value = notification
        _recentNotifications.value = (listOf(notification) + _recentNotifications.value).take(25)
        _notificationEvents.tryEmit(notification)
    }

    companion object {
        private val _isListenerConnected = MutableStateFlow(false)
        val isListenerConnected: StateFlow<Boolean> = _isListenerConnected.asStateFlow()

        private val _latestNotification = MutableStateFlow<AnnouncedNotification?>(null)
        val latestNotification: StateFlow<AnnouncedNotification?> = _latestNotification.asStateFlow()

        private val _recentNotifications = MutableStateFlow<List<AnnouncedNotification>>(emptyList())
        val recentNotifications: StateFlow<List<AnnouncedNotification>> = _recentNotifications.asStateFlow()

        private val _notificationEvents = MutableSharedFlow<AnnouncedNotification>(replay = 5, extraBufferCapacity = 20)
        val notificationEvents: SharedFlow<AnnouncedNotification> = _notificationEvents.asSharedFlow()

        fun isPermissionGranted(context: Context): Boolean {
            val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
            return enabledListeners.contains(context.packageName)
        }

        fun containsSensitiveContent(title: String, text: String): Boolean {
            val lowerContent = "$title $text".lowercase()
            return listOf(
                "otp", "one time password", "verification code", "pin",
                "password", "passcode", "bank", "debit", "credit",
                "rupees", "rs.", "inr", "cvv", "upi", "transaction"
            ).any { lowerContent.contains(it) }
        }

        fun formatAnnouncementText(
            notification: AnnouncedNotification,
            hideSensitive: Boolean
        ): String {
            return if (notification.isSensitive && hideSensitive) {
                "Sir ji, you received a sensitive notification from ${notification.appName}. Details hidden for privacy."
            } else {
                val bodySnippet = notification.text.take(120)
                "Sir ji, ${notification.appName} says: ${notification.title}. $bodySnippet".trim()
            }
        }

        fun clearRecent() {
            _recentNotifications.value = emptyList()
            _latestNotification.value = null
        }
    }
}

typealias HairaNotificationListener = AerisNotificationListener
