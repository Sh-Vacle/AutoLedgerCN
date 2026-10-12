package com.autoledgercn

import com.autoledgercn.data.entity.TransactionEntity
import com.autoledgercn.domain.model.SourceType
import com.autoledgercn.domain.model.TransactionType
import com.autoledgercn.domain.stats.SelectableStatsCalculator
import com.autoledgercn.domain.stats.SelectableStatsRange
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class SelectableStatsRangeTest {
    @Test
    fun selectedRangesOnlyCountTheirOwnWindow() {
        val txs = listOf(
            tx(100.0, TransactionType.INCOME, millis(2026, Calendar.JUNE, 25, 10)),
            tx(30.0, TransactionType.EXPENSE, millis(2026, Calendar.JUNE, 25, 14)),
            tx(20.0, TransactionType.EXPENSE, millis(2026, Calendar.JUNE, 24, 10)),
            tx(200.0, TransactionType.INCOME, millis(2026, Calendar.JUNE, 30, 10)),
            tx(70.0, TransactionType.EXPENSE, millis(2026, Calendar.MAY, 30, 10)),
            tx(500.0, TransactionType.INCOME, millis(2025, Calendar.DECEMBER, 30, 10))
        )
        val selected = millis(2026, Calendar.JUNE, 25, 12)

        assertEquals(100.0, SelectableStatsCalculator.totals(txs, SelectableStatsRange.DAY, selected).income, 0.001)
        assertEquals(30.0, SelectableStatsCalculator.totals(txs, SelectableStatsRange.DAY, selected).expense, 0.001)
        assertEquals(50.0, SelectableStatsCalculator.totals(txs, SelectableStatsRange.WEEK, selected).expense, 0.001)
        assertEquals(300.0, SelectableStatsCalculator.totals(txs, SelectableStatsRange.MONTH, selected).income, 0.001)
        assertEquals(120.0, SelectableStatsCalculator.totals(txs, SelectableStatsRange.YEAR, selected).expense, 0.001)
    }

    private fun millis(year: Int, month: Int, day: Int, hour: Int): Long =
        Calendar.getInstance(Locale.CHINA).apply {
            set(year, month, day, hour, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun tx(amount: Double, type: TransactionType, time: Long) = TransactionEntity(
        amount = amount,
        type = type,
        categoryPrimary = if (type == TransactionType.INCOME) "收入" else "餐饮",
        categorySecondary = if (type == TransactionType.INCOME) "工资" else "外卖",
        merchant = "测试",
        account = "测试",
        sourceApp = "测试",
        sourcePackage = "test",
        sourceType = SourceType.MANUAL,
        rawTitle = "",
        rawText = "",
        rawBigText = "",
        rawFingerprint = "raw-$time-$amount",
        transactionFingerprint = "tx-$time-$amount",
        transactionTime = time,
        createdAt = time,
        confidence = 1.0
    )
}
