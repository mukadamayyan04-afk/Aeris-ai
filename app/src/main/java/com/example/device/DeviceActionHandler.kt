package com.example.device

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import com.example.data.database.ReminderDao
import com.example.data.database.entities.ReminderEntity
import com.example.domain.model.ActionExecutionStatus
import com.example.domain.model.ActionType
import com.example.domain.model.DeviceAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Locale

data class InstalledAppSummary(
    val appName: String,
    val packageName: String
)

class DeviceActionHandler(private val context: Context) {

    private val _isFlashlightOn = MutableStateFlow(false)
    val isFlashlightOn: Flow<Boolean> = _isFlashlightOn.asStateFlow()

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var torchCameraId: String? = null

    init {
        detectFlashlight()
    }

    private fun detectFlashlight() {
        try {
            cameraManager?.cameraIdList?.forEach { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    torchCameraId = id
                    return@forEach
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun toggleFlashlight(explicitState: Boolean? = null): DeviceAction = withContext(Dispatchers.IO) {
        val cid = torchCameraId
        if (cid == null) {
            return@withContext DeviceAction(
                id = "torch-${System.currentTimeMillis()}",
                type = ActionType.FLASHLIGHT,
                label = "Flashlight Toggle",
                detail = "Flash hardware not found on device",
                status = ActionExecutionStatus.FAILED,
                resultMessage = "Flashlight hardware is unavailable on this device."
            )
        }

        try {
            val newState = explicitState ?: !_isFlashlightOn.value
            cameraManager?.setTorchMode(cid, newState)
            _isFlashlightOn.value = newState
            DeviceAction(
                id = "torch-${System.currentTimeMillis()}",
                type = ActionType.FLASHLIGHT,
                label = "Flashlight ${if (newState) "ON" else "OFF"}",
                detail = "Hardware torch mode set to $newState",
                status = ActionExecutionStatus.SUCCESS,
                resultMessage = "Flashlight turned ${if (newState) "ON" else "OFF"}."
            )
        } catch (e: CameraAccessException) {
            DeviceAction(
                id = "torch-${System.currentTimeMillis()}",
                type = ActionType.FLASHLIGHT,
                label = "Flashlight Toggle",
                detail = e.localizedMessage ?: "Camera hardware error",
                status = ActionExecutionStatus.FAILED,
                resultMessage = "Camera access error: ${e.message}"
            )
        }
    }

    fun setTimer(seconds: Int, label: String = "AERIS Timer"): DeviceAction {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            DeviceAction(
                id = "timer-${System.currentTimeMillis()}",
                type = ActionType.TIMER,
                label = "Timer ($seconds sec)",
                detail = "Dispatched native timer intent",
                status = ActionExecutionStatus.SUCCESS,
                resultMessage = "Dispatched $seconds second timer to Android Clock."
            )
        } catch (e: Exception) {
            DeviceAction(
                id = "timer-${System.currentTimeMillis()}",
                type = ActionType.TIMER,
                label = "Set Timer",
                detail = e.localizedMessage ?: "Intent failed",
                status = ActionExecutionStatus.FAILED,
                resultMessage = "No compatible clock application found for timer."
            )
        }
    }

    fun setAlarm(hour: Int, minutes: Int, message: String = "AERIS Alarm"): DeviceAction {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minutes)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            DeviceAction(
                id = "alarm-${System.currentTimeMillis()}",
                type = ActionType.ALARM,
                label = "Alarm (${String.format(Locale.US, "%02d:%02d", hour, minutes)})",
                detail = "Dispatched native alarm intent",
                status = ActionExecutionStatus.SUCCESS,
                resultMessage = "Alarm set for ${String.format(Locale.US, "%02d:%02d", hour, minutes)}."
            )
        } catch (e: Exception) {
            DeviceAction(
                id = "alarm-${System.currentTimeMillis()}",
                type = ActionType.ALARM,
                label = "Set Alarm",
                detail = e.localizedMessage ?: "Intent failed",
                status = ActionExecutionStatus.FAILED,
                resultMessage = "No clock application available to set alarm."
            )
        }
    }

    suspend fun createReminder(
        title: String,
        delayMinutes: Int = 15,
        reminderDao: ReminderDao? = null,
        dispatchCalendarIntent: Boolean = true
    ): DeviceAction = withContext(Dispatchers.IO) {
        val safeTitle = title.ifBlank { "AERIS Reminder" }
        val triggerAt = System.currentTimeMillis() + (delayMinutes.coerceAtLeast(1) * 60_000L)
        var calendarLaunched = false

        if (dispatchCalendarIntent) {
            val calIntent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, safeTitle)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, triggerAt)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, triggerAt + 30 * 60_000L)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            calendarLaunched = try {
                if (calIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(calIntent)
                    true
                } else {
                    false
                }
            } catch (_: Exception) {
                false
            }
        }

        val sourceLabel = if (calendarLaunched) "Calendar + WorkManager" else "Local WorkManager Alert"
        val insertedId = try {
            reminderDao?.insert(
                ReminderEntity(
                    title = safeTitle,
                    triggerAtMillis = triggerAt,
                    source = sourceLabel
                )
            ) ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }

        try {
            ReminderWorker.schedule(
                context = context,
                reminderId = insertedId,
                title = safeTitle,
                delayMillis = delayMinutes.coerceAtLeast(1) * 60_000L
            )
        } catch (_: Exception) {
            // WorkManager may not be initialized in unit-test context
        }

        DeviceAction(
            id = "reminder-${System.currentTimeMillis()}",
            type = ActionType.REMINDER,
            label = "Reminder ($delayMinutes min)",
            detail = "$safeTitle • $sourceLabel",
            status = ActionExecutionStatus.SUCCESS,
            resultMessage = if (calendarLaunched) {
                "Opened Calendar event draft & scheduled local AERIS reminder in $delayMinutes min."
            } else {
                "Scheduled local AERIS reminder notification in $delayMinutes min ($safeTitle)."
            }
        )
    }

    fun openSettingsPage(target: String): DeviceAction {
        val lower = target.lowercase(Locale.ROOT)
        val (action, label) = when {
            lower.contains("overlay") || lower.contains("floating") ->
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION to "Display Over Other Apps"
            lower.contains("location") || lower.contains("gps") ->
                Settings.ACTION_LOCATION_SOURCE_SETTINGS to "Location Settings"
            lower.contains("date") || lower.contains("time") ->
                Settings.ACTION_DATE_SETTINGS to "Date & Time Settings"
            lower.contains("storage") ->
                Settings.ACTION_INTERNAL_STORAGE_SETTINGS to "Storage Settings"
            lower.contains("wifi") || lower.contains("wi-fi") ->
                Settings.ACTION_WIFI_SETTINGS to "Wi-Fi Settings"
            lower.contains("bluetooth") ->
                Settings.ACTION_BLUETOOTH_SETTINGS to "Bluetooth Settings"
            lower.contains("battery") ->
                Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS to "Battery Optimization Settings"
            lower.contains("notification listener") || lower.contains("notification access") ->
                Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS to "Notification Access Settings"
            lower.contains("app info") || lower.contains("app details") ->
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS to "AERIS App Details"
            else ->
                Settings.ACTION_SETTINGS to "Android Settings"
        }
        val intent = Intent(action).apply {
            if (action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS ||
                action == Settings.ACTION_MANAGE_OVERLAY_PERMISSION
            ) {
                data = Uri.fromParts("package", context.packageName, null)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            DeviceAction(
                id = "settings-${System.currentTimeMillis()}",
                type = ActionType.SETTINGS_PAGE,
                label = label,
                detail = "Opened Android $label",
                status = ActionExecutionStatus.SUCCESS,
                resultMessage = "Opened $label."
            )
        } catch (e: Exception) {
            DeviceAction(
                id = "settings-${System.currentTimeMillis()}",
                type = ActionType.SETTINGS_PAGE,
                label = label,
                detail = e.localizedMessage ?: "Settings unavailable",
                status = ActionExecutionStatus.FAILED,
                resultMessage = "Could not open $label: ${e.message}"
            )
        }
    }

    fun readBatteryInfo(): DeviceAction {
        return try {
            val intent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100)?.takeIf { it > 0 } ?: 100
            val status = intent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
            val tempTenths = intent?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                status == android.os.BatteryManager.BATTERY_STATUS_FULL
            val pct = if (level >= 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else -1
            val tempC = tempTenths / 10.0
            val summary = if (pct >= 0) {
                "Battery: $pct% (${if (isCharging) "Charging" else "Discharging"}), Temp: ${String.format(Locale.US, "%.1f", tempC)}°C"
            } else {
                "Battery telemetry unavailable in this environment."
            }
            DeviceAction(
                id = "battery-${System.currentTimeMillis()}",
                type = ActionType.BATTERY_CHECK,
                label = if (pct >= 0) "Battery $pct%" else "Battery Status",
                detail = summary,
                status = ActionExecutionStatus.SUCCESS,
                resultMessage = summary
            )
        } catch (e: Exception) {
            DeviceAction(
                id = "battery-${System.currentTimeMillis()}",
                type = ActionType.BATTERY_CHECK,
                label = "Battery Status",
                detail = e.localizedMessage ?: "Battery service error",
                status = ActionExecutionStatus.FAILED,
                resultMessage = "Unable to read battery telemetry: ${e.message}"
            )
        }
    }

    fun readStorageInfo(): DeviceAction {
        return try {
            val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
            val bytesTotal = stat.blockCountLong * stat.blockSizeLong
            val bytesFree = stat.availableBlocksLong * stat.blockSizeLong
            val gbTotal = bytesTotal / (1024.0 * 1024.0 * 1024.0)
            val gbFree = bytesFree / (1024.0 * 1024.0 * 1024.0)
            val summary = String.format(Locale.US, "Internal Storage: %.2f GB free / %.1f GB total", gbFree, gbTotal)
            DeviceAction(
                id = "storage-${System.currentTimeMillis()}",
                type = ActionType.STORAGE_INFO,
                label = String.format(Locale.US, "Storage: %.1f GB Free", gbFree),
                detail = summary,
                status = ActionExecutionStatus.SUCCESS,
                resultMessage = summary
            )
        } catch (e: Exception) {
            DeviceAction(
                id = "storage-${System.currentTimeMillis()}",
                type = ActionType.STORAGE_INFO,
                label = "Storage Info",
                detail = e.localizedMessage ?: "StatFs error",
                status = ActionExecutionStatus.FAILED,
                resultMessage = "Could not inspect device storage: ${e.message}"
            )
        }
    }

    fun readDateTime(): DeviceAction {
        val now = java.util.Date()
        val formatted = java.text.SimpleDateFormat("EEE, dd MMM yyyy • HH:mm:ss", Locale.getDefault()).format(now)
        val tz = java.util.TimeZone.getDefault().id
        return DeviceAction(
            id = "datetime-${System.currentTimeMillis()}",
            type = ActionType.DATE_TIME,
            label = "Date & Time",
            detail = "$formatted ($tz)",
            status = ActionExecutionStatus.SUCCESS,
            resultMessage = "$formatted ($tz)"
        )
    }

    fun listInstalledLaunchableApps(): List<InstalledAppSummary> {
        return try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            pm.queryIntentActivities(mainIntent, 0)
                .mapNotNull { info ->
                    val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                    if (pkg == context.packageName) return@mapNotNull null
                    val label = info.loadLabel(pm)?.toString().orEmpty().ifBlank { pkg }
                    InstalledAppSummary(appName = label, packageName = pkg)
                }
                .distinctBy { it.packageName }
                .sortedBy { it.appName.lowercase(Locale.ROOT) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun launchApp(appNameOrPackage: String): DeviceAction {
        val pm = context.packageManager
        val normalized = appNameOrPackage.lowercase(Locale.ROOT).trim()
        val targetPackage = when (normalized) {
            "camera" -> {
                val camIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return try {
                    context.startActivity(camIntent)
                    DeviceAction("app-cam", ActionType.OPEN_APP, "Launch Camera", "Opened Camera", ActionExecutionStatus.SUCCESS, "Camera launched.")
                } catch (e: Exception) {
                    DeviceAction("app-cam", ActionType.OPEN_APP, "Launch Camera", e.message ?: "", ActionExecutionStatus.FAILED, "Cannot open camera.")
                }
            }
            "settings" -> return openSettingsPage("settings")
            "calculator" -> "com.google.android.calculator"
            "chrome" -> "com.android.chrome"
            "youtube" -> "com.google.android.youtube"
            "whatsapp" -> "com.whatsapp"
            else -> {
                listInstalledLaunchableApps()
                    .firstOrNull {
                        it.packageName.equals(appNameOrPackage, ignoreCase = true) ||
                            it.appName.equals(appNameOrPackage, ignoreCase = true) ||
                            it.appName.lowercase(Locale.ROOT).contains(normalized)
                    }?.packageName ?: appNameOrPackage
            }
        }

        val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(launchIntent)
                DeviceAction(
                    id = "launch-${System.currentTimeMillis()}",
                    type = ActionType.OPEN_APP,
                    label = "Launch $appNameOrPackage",
                    detail = "Opened package: $targetPackage",
                    status = ActionExecutionStatus.SUCCESS,
                    resultMessage = "Opened $appNameOrPackage successfully."
                )
            } catch (e: Exception) {
                DeviceAction(
                    id = "launch-${System.currentTimeMillis()}",
                    type = ActionType.OPEN_APP,
                    label = "Launch $appNameOrPackage",
                    detail = e.localizedMessage ?: "Launch error",
                    status = ActionExecutionStatus.FAILED,
                    resultMessage = "Failed to launch $appNameOrPackage: ${e.message}"
                )
            }
        } else {
            DeviceAction(
                id = "launch-${System.currentTimeMillis()}",
                type = ActionType.OPEN_APP,
                label = "Launch $appNameOrPackage",
                detail = "Package '$targetPackage' not installed on this device",
                status = ActionExecutionStatus.FAILED,
                resultMessage = "Application '$appNameOrPackage' ($targetPackage) is not installed."
            )
        }
    }

    fun adjustMediaVolume(raise: Boolean): DeviceAction {
        val am = audioManager ?: return DeviceAction("vol", ActionType.VOLUME, "Volume Control", "Audio service unavailable", ActionExecutionStatus.FAILED, "Audio service unavailable")
        val direction = if (raise) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return DeviceAction(
            id = "vol-${System.currentTimeMillis()}",
            type = ActionType.VOLUME,
            label = "Media Volume: $current / $max",
            detail = if (raise) "Increased volume" else "Decreased volume",
            status = ActionExecutionStatus.SUCCESS,
            resultMessage = "Media volume adjusted to $current / $max."
        )
    }

    fun vibrateHaptic(durationMs: Long = 40) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {}
    }
}
