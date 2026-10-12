package com.autoledgercn

import com.autoledgercn.data.entity.TransactionEntity
import com.autoledgercn.domain.model.SourceType
import com.autoledgercn.domain.model.TransactionType
import com.autoledgercn.domain.stats.LedgerStatistics
import com.autoledgercn.domain.stats.StatsPeriod
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class LedgerStatisticsTest {
    @Test
    fun calculatesCommonRangeTotals() {
        val now = millis(2026, Calendar.JUNE, 24, 12)
        val txs = listOf(
            tx(100.0, TransactionType.INCOME, now),
            tx(30.0, TransactionType.EXPENSE, now),
            tx(20.0, TransactionType.EXPENSE, millis(2026, Calendar.JUNE, 23, 12)),
            tx(500.0, TransactionType.INCOME, millis(2026, Calendar.MAY, 1, 12)),
            tx(70.0, TransactionType.EXPENSE, millis(2025, Calendar.DECEMBER, 1, 12))
        )

        assertEquals(100.0, LedgerStatistics.totals(txs, StatsPeriod.TODAY, now).income, 0.001)
        assertEquals(30.0, LedgerStatistics.totals(txs, StatsPeriod.TODAY, now).expense, 0.001)
        assertEquals(50.0, LedgerStatistics.totals(txs, StatsPeriod.WEEK, now).expense, 0.001)
        assertEquals(50.0, LedgerStatistics.totals(txs, StatsPeriod.MONTH, now).expense, 0.001)
        assertEquals(600.0, LedgerStatistics.totals(txs, StatsPeriod.YEAR, now).income, 0.001)
        assertEquals(120.0, LedgerStatistics.totals(txs, StatsPeriod.ALL, now).expense, 0.001)
    }

    private fun millis(year: Int, month: Int, day: Int, hour: Int): Long =
        Calendar.getInstance(Locale.CHINA).apply {
            set(year, month, day, hour, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun tx(amount: Double, type: TransactionType, time: Long) = TransactionEntity(
        amount = amount,
        type = type,
        categoryPrimary = if (type == TransactionType.INCOME) "工资收入" else "餐饮",
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
