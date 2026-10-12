package com.autoledgercn.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

data class PermissionStatus(
    val notificationListenerEnabled: Boolean = false,
    val postNotificationsGranted: Boolean = true,
    val bootKeepAliveEnabled: Boolean = false,
    val batteryOptimizationIgnored: Boolean? = null
) {
    val keyPermissionsReady: Boolean
        get() = notificationListenerEnabled && postNotificationsGranted

    val allReady: Boolean
        get() = keyPermissionsReady && bootKeepAliveEnabled
}

object PermissionStatusReader {
    fun read(context: Context, bootKeepAliveEnabled: Boolean): PermissionStatus =
        PermissionStatus(
            notificationListenerEnabled = notificationListenerEnabled(context),
            postNotificationsGranted = postNotificationsGranted(context),
            bootKeepAliveEnabled = bootKeepAliveEnabled,
            batteryOptimizationIgnored = batteryOptimizationIgnored(context)
        )

    fun notificationListenerEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ).orEmpty()
        return enabled.split(":").any { component ->
            component.substringBefore('/').equals(context.packageName, ignoreCase = true)
        }
    }

    fun postNotificationsGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun batteryOptimizationIgnored(context: Context): Boolean? {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return null
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
}
