package com.autoledgercn.domain.model

data class ParsedTransaction(
    val amount: Double,
    val type: TransactionType,
    val merchant: String,
    val account: String,
    val sourceApp: String,
    val sourcePackage: String,
    val rawTitle: String,
    val rawText: String,
    val rawBigText: String,
    val transactionTime: Long,
    val confidence: Double,
    val categoryPrimary: String = "其他",
    val categorySecondary: String = "未分类",
    val rawFingerprint: String = "",
    val transactionFingerprint: String = ""
)

data class ClassifiedTransaction(
    val categoryPrimary: String,
    val categorySecondary: String,
    val confidenceDelta: Double
)
