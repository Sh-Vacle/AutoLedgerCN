package com.autoledgercn.domain.stats

import com.autoledgercn.data.entity.TransactionEntity
import com.autoledgercn.domain.model.TransactionType
import java.util.Calendar
import java.util.Locale

data class RangeTotals(
    val income: Double,
    val expense: Double,
    val balance: Double
)

enum class StatsPeriod {
    TODAY, WEEK, MONTH, HALF_YEAR, YEAR, ALL
}

object LedgerStatistics {
    fun totals(transactions: List<TransactionEntity>, period: StatsPeriod, now: Long = System.currentTimeMillis()): RangeTotals {
        val start = startMillis(period, now)
        val rows = transactions.filter { it.transactionTime >= start }
        val income = rows.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = rows.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        return RangeTotals(income = income, expense = expense, balance = income - expense)
    }

    fun startMillis(period: StatsPeriod, now: Long): Long = when (period) {
        StatsPeriod.TODAY -> calendar(now).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        StatsPeriod.WEEK -> calendar(now).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        StatsPeriod.MONTH -> calendar(now).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        StatsPeriod.HALF_YEAR -> calendar(now).apply {
            add(Calendar.MONTH, -6)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        StatsPeriod.YEAR -> calendar(now).apply {
            set(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        StatsPeriod.ALL -> Long.MIN_VALUE
    }

    private fun calendar(now: Long): Calendar =
        Calendar.getInstance(Locale.CHINA).apply { timeInMillis = now }
}
