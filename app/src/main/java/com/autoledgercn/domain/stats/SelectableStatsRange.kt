package com.autoledgercn.domain.stats

import com.autoledgercn.data.entity.TransactionEntity
import com.autoledgercn.domain.model.TransactionType
import java.util.Calendar
import java.util.Locale

enum class SelectableStatsRange(val label: String) {
    DAY("日"),
    WEEK("周"),
    MONTH("月"),
    YEAR("年"),
    ALL("全部")
}

data class TimeBounds(val startMillis: Long, val endMillis: Long)

object SelectableStatsCalculator {
    fun bounds(range: SelectableStatsRange, selectedDateMillis: Long): TimeBounds {
        if (range == SelectableStatsRange.ALL) return TimeBounds(Long.MIN_VALUE, Long.MAX_VALUE)
        val start = calendar(selectedDateMillis)
        when (range) {
            SelectableStatsRange.DAY -> Unit
            SelectableStatsRange.WEEK -> {
                start.firstDayOfWeek = Calendar.MONDAY
                start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            SelectableStatsRange.MONTH -> start.set(Calendar.DAY_OF_MONTH, 1)
            SelectableStatsRange.YEAR -> start.set(Calendar.DAY_OF_YEAR, 1)
            SelectableStatsRange.ALL -> Unit
        }
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)

        val end = start.clone() as Calendar
        when (range) {
            SelectableStatsRange.DAY -> end.add(Calendar.DAY_OF_MONTH, 1)
            SelectableStatsRange.WEEK -> end.add(Calendar.DAY_OF_MONTH, 7)
            SelectableStatsRange.MONTH -> end.add(Calendar.MONTH, 1)
            SelectableStatsRange.YEAR -> end.add(Calendar.YEAR, 1)
            SelectableStatsRange.ALL -> Unit
        }
        end.add(Calendar.MILLISECOND, -1)
        return TimeBounds(start.timeInMillis, end.timeInMillis)
    }

    fun filter(transactions: List<TransactionEntity>, range: SelectableStatsRange, selectedDateMillis: Long): List<TransactionEntity> {
        val bounds = bounds(range, selectedDateMillis)
        return transactions.filter { it.transactionTime in bounds.startMillis..bounds.endMillis }
    }

    fun totals(transactions: List<TransactionEntity>, range: SelectableStatsRange, selectedDateMillis: Long): RangeTotals {
        val rows = filter(transactions, range, selectedDateMillis)
        val income = rows.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = rows.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        return RangeTotals(income = income, expense = expense, balance = income - expense)
    }

    private fun calendar(value: Long): Calendar =
        Calendar.getInstance(Locale.CHINA).apply { timeInMillis = value }
}
