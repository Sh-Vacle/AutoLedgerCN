package com.autoledgercn.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.autoledgercn.domain.model.TransactionType

@Entity(tableName = "category_rules")
data class CategoryRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keyword: String,
    val categoryPrimary: String,
    val categorySecondary: String,
    val transactionType: TransactionType,
    val priority: Int,
    val enabled: Boolean,
    val createdAt: Long
)
