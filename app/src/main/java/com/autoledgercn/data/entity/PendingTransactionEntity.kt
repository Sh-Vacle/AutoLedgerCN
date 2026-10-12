package com.autoledgercn.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.autoledgercn.domain.model.TransactionType

@Entity(tableName = "pending_transactions")
data class PendingTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val categoryPrimary: String,
    val categorySecondary: String,
    val merchant: String,
    val account: String,
    val sourceApp: String,
    val sourcePackage: String,
    val rawTitle: String,
    val rawText: String,
    val rawBigText: String,
    val transactionTime: Long,
    val confidence: Double,
    val reason: String,
    val possibleDuplicateTransactionId: Long? = null,
    val createdAt: Long
)
