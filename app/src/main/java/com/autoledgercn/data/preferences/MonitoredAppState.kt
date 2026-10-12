package com.autoledgercn.data.preferences

object MonitoredAppState {
    fun updated(apps: List<MonitoredApp>, name: String, packageName: String, enabled: Boolean): List<MonitoredApp> {
        val existing = apps.firstOrNull { it.packageName == packageName }
        return if (existing == null) {
            apps + MonitoredApp(packageName = packageName, name = name.ifBlank { packageName }, enabled = enabled, isDefault = false)
        } else {
            apps.map { if (it.packageName == packageName) it.copy(enabled = enabled) else it }
        }
    }
}
