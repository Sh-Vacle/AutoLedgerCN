package com.autoledgercn.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "raw_notifications",
    indices = [Index(value = ["rawFingerprint"], unique = true), Index("parsedTransactionId")]
)
data class RawNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val bigText: String,
    val postTime: Long,
    val rawFingerprint: String,
    val parsedTransactionId: Long? = null,
    val createdAt: Long
)
