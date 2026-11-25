package com.example.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.domain.model.PermissionStateItem
import com.example.domain.model.PermissionStateStatus
import com.example.notifications.AerisNotificationListener

/**
 * Inspects real Android permission states (Stage 8 Permission Manager):
 * Distinguishes GRANTED, DENIED, RESTRICTED, and NOT_REQUESTED cleanly,
 * and marks install-time permissions like INTERNET so UI never sends bogus runtime popups.
 */
object PermissionStatusInspector {

    private const val PREF_ASKED = "aeris_permission_asked_flags"

    fun markRequested(context: Context, permission: String) {
        context.getSharedPreferences(PREF_ASKED, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(permission, true)
            .apply()
    }

    fun inspectAll(context: Context): List<PermissionStateItem> {
        val prefs = context.getSharedPreferences(PREF_ASKED, Context.MODE_PRIVATE)
        val items = mutableListOf<PermissionStateItem>()

        fun runtimeStatus(permission: String): PermissionStateStatus {
            val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            if (granted) return PermissionStateStatus.GRANTED
            val wasRequested = prefs.getBoolean(permission, false)
            return if (wasRequested) PermissionStateStatus.DENIED else PermissionStateStatus.NOT_REQUESTED
        }

        items += PermissionStateItem(
            id = "perm_mic",
            title = "Microphone (Voice & Wake Word)",
            androidPermission = Manifest.permission.RECORD_AUDIO,
            status = runtimeStatus(Manifest.permission.RECORD_AUDIO),
            explanation = "Required for Push-to-Talk speech input and optional wake-word listening. Audio is processed only when active.",
            isRuntimeRequestable = true,
            settingsAction = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        )

        items += PermissionStateItem(
            id = "perm_camera",
            title = "Camera (Selfie Optical Scanner)",
            androidPermission = Manifest.permission.CAMERA,
            status = runtimeStatus(Manifest.permission.CAMERA),
            explanation = "Requested only when opening Selfie Camera; stops immediately when closed.",
            isRuntimeRequestable = true,
            settingsAction = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            items += PermissionStateItem(
                id = "perm_notifications",
                title = "Post Notifications (Android 13+)",
                androidPermission = Manifest.permission.POST_NOTIFICATIONS,
                status = runtimeStatus(Manifest.permission.POST_NOTIFICATIONS),
                explanation = "Required for foreground service status (Wake Word / Game Coach) and scheduled Reminders.",
                isRuntimeRequestable = true,
                settingsAction = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            )
        }

        val listenerGranted = AerisNotificationListener.isPermissionGranted(context)
        items += PermissionStateItem(
            id = "perm_notif_listener",
            title = "Notification Listener Access",
            androidPermission = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
            status = if (listenerGranted) PermissionStateStatus.GRANTED else PermissionStateStatus.RESTRICTED,
            explanation = "Allows AERIS to announce selected app notifications aloud with sensitive OTP/bank privacy filters.",
            isRuntimeRequestable = false,
            settingsAction = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
        )

        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val ignoringBattery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        } else {
            true
        }
        items += PermissionStateItem(
            id = "perm_battery",
            title = "Unrestricted Background Battery",
            androidPermission = "android.settings.IGNORE_BATTERY_OPTIMIZATION",
            status = if (ignoringBattery) PermissionStateStatus.GRANTED else PermissionStateStatus.RESTRICTED,
            explanation = "Prevents MIUI / Android from force-stopping optional Wake Word or Reminder alerts when idle.",
            isRuntimeRequestable = false,
            settingsAction = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
        )

        val overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
        items += PermissionStateItem(
            id = "perm_overlay",
            title = "Display Over Other Apps (Floating Assistant)",
            androidPermission = Manifest.permission.SYSTEM_ALERT_WINDOW,
            status = if (overlayGranted) PermissionStateStatus.GRANTED else PermissionStateStatus.RESTRICTED,
            explanation = "Optional permission used only when you turn on AERIS Floating Assistant. Zero hidden screen reading.",
            isRuntimeRequestable = false,
            settingsAction = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
        )

        val screenCaptureActive = com.example.ui.coach.GameCoachService.isObserving.value
        items += PermissionStateItem(
            id = "perm_screen_capture",
            title = "Screen Capture (Per-Session Consent)",
            androidPermission = "android.media.projection.MediaProjection",
            status = if (screenCaptureActive) PermissionStateStatus.GRANTED else PermissionStateStatus.NOT_REQUESTED,
            explanation = "Prompted by Android every time you explicitly start Game Coach observation. Stops immediately on close.",
            isRuntimeRequestable = false,
            settingsAction = null
        )

        val locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        items += PermissionStateItem(
            id = "perm_location",
            title = "Location (Optional Context Only)",
            androidPermission = Manifest.permission.ACCESS_FINE_LOCATION,
            status = if (locationGranted) PermissionStateStatus.GRANTED else PermissionStateStatus.NOT_REQUESTED,
            explanation = "Not requested by default to preserve privacy. AERIS uses Location Settings only if you explicitly ask.",
            isRuntimeRequestable = false,
            settingsAction = Settings.ACTION_LOCATION_SOURCE_SETTINGS
        )

        val internetGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET) ==
            PackageManager.PERMISSION_GRANTED
        items += PermissionStateItem(
            id = "perm_internet",
            title = "Network Access (Install-Time)",
            androidPermission = Manifest.permission.INTERNET,
            status = if (internetGranted) PermissionStateStatus.GRANTED else PermissionStateStatus.RESTRICTED,
            explanation = "Install-time permission declared in AndroidManifest.xml (no runtime prompt needed) for Gemini AI & HTTPS tools.",
            isRuntimeRequestable = false,
            isInstallTimeOnly = true
        )

        return items
    }
}
