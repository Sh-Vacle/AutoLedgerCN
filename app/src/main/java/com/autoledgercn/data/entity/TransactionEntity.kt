package com.autoledgercn.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.autoledgercn.domain.model.SourceType
import com.autoledgercn.domain.model.TransactionType

@Entity(
    tableName = "transactions",
    indices = [
        Index("transactionFingerprint"),
        Index("rawFingerprint"),
        Index(value = ["amount", "type", "transactionTime"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val categoryPrimary: String,
    val categorySecondary: String,
    val merchant: String,
    val account: String,
    val sourceApp: String,
    val sourcePackage: String,
    val sourceType: SourceType,
    val rawTitle: String,
    val rawText: String,
    val rawBigText: String,
    val rawFingerprint: String,
    val transactionFingerprint: String,
    val transactionTime: Long,
    val createdAt: Long,
    val confidence: Double,
    val duplicateGroupId: String? = null,
    val relatedSourceCount: Int = 1,
    val note: String = ""
)
