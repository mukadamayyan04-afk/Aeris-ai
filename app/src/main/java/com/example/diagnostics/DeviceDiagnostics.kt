package com.example.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.example.domain.model.DeviceDiagnosticsInfo
import java.io.File

class DeviceDiagnostics(private val context: Context) {

    fun getDiagnostics(): DeviceDiagnosticsInfo {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availRamMb = memInfo.availMem / (1024 * 1024)

        var totalStorageGb = 0.0
        var freeStorageGb = 0.0
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            totalStorageGb = (stat.blockCountLong * stat.blockSizeLong) / (1024.0 * 1024.0 * 1024.0)
            freeStorageGb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024.0 * 1024.0 * 1024.0)
        } catch (_: Exception) {}

        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else 100
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val supportedAbis = Build.SUPPORTED_ABIS.toList()
        val isArm64 = supportedAbis.any { it.contains("arm64-v8a", ignoreCase = true) }

        val model = "${Build.MANUFACTURER} ${Build.MODEL}"
        val isRedmi135G = model.contains("Redmi", ignoreCase = true) || model.contains("Xiaomi", ignoreCase = true) || isArm64

        return DeviceDiagnosticsInfo(
            totalRamMb = totalRamMb,
            availableRamMb = availRamMb,
            totalStorageGb = totalStorageGb,
            freeStorageGb = freeStorageGb,
            supportedAbis = supportedAbis,
            isArm64V8a = isArm64,
            batteryPercent = batteryPct,
            isCharging = isCharging,
            deviceModel = model,
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            redmiOptimized = isRedmi135G
        )
    }
}
