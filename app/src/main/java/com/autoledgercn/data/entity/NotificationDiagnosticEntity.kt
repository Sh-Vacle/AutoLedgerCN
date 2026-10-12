package com.autoledgercn.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_diagnostics")
data class NotificationDiagnosticEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timeMillis: Long,
    val appName: String,
    val packageName: String,
    val title: String,
    val text: String,
    val isMonitoredApp: Boolean,
    val parserResult: String,
    val finalAction: String,
    val ignoreReason: String
)
