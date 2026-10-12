package com.autoledgercn

import com.autoledgercn.data.entity.TransactionEntity
import com.autoledgercn.domain.model.SourceType
import com.autoledgercn.domain.model.TransactionType
import com.autoledgercn.domain.stats.StatsRangeDefaults
import com.autoledgercn.domain.time.DateRangeCalculator
import com.autoledgercn.domain.time.TimeRangeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class DateRangeCalculatorTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun fixedRangesOnlyCountTheirOwnWindow() {
        val now = millis(2026, 6, 25, 12, 0)
        val txs = listOf(
            tx(100.0, TransactionType.INCOME, millis(2026, 6, 25, 10, 0)),
            tx(30.0, TransactionType.EXPENSE, millis(2026, 6, 25, 14, 0)),
            tx(20.0, TransactionType.EXPENSE, millis(2026, 6, 24, 10, 0)),
            tx(200.0, TransactionType.INCOME, millis(2026, 6, 30, 10, 0)),
            tx(70.0, TransactionType.EXPENSE, millis(2026, 5, 30, 10, 0)),
            tx(500.0, TransactionType.INCOME, millis(2025, 12, 30, 10, 0))
        )

        assertEquals(2, DateRangeCalculator.filter(txs, DateRangeCalculator.today(now)).size)
        assertEquals(3, DateRangeCalculator.filter(txs, DateRangeCalculator.week(now)).size)
        assertEquals(4, DateRangeCalculator.filter(txs, DateRangeCalculator.month(now)).size)
        assertEquals(5, DateRangeCalculator.filter(txs, DateRangeCalculator.year(now)).size)
        assertEquals(6, DateRangeCalculator.filter(txs, DateRangeCalculator.all()).size)
    }

    @Test
    fun customRangeIncludesOnlyStartToEndDates() {
        val txs = listOf(
            tx(1.0, TransactionType.EXPENSE, millis(2026, 6, 1, 0, 0)),
            tx(2.0, TransactionType.EXPENSE, millis(2026, 6, 15, 12, 0)),
            tx(3.0, TransactionType.EXPENSE, millis(2026, 7, 1, 0, 0))
        )

        val range = DateRangeCalculator.custom("2026-06-01", "2026-06-30").range!!

        assertEquals(listOf(txs[0], txs[1]), DateRangeCalculator.filter(txs, range))
    }

    @Test
    fun parseManualTransactionDateTime() {
        val parsed = DateRangeCalculator.parseDateTime("2026-06-01", "14:30")

        assertEquals(millis(2026, 6, 1, 14, 30), parsed.millis)
        assertNull(parsed.error)
    }

    @Test
    fun customRangeTypeUsesDateRangeCalculatorResult() {
        val custom = DateRangeCalculator.custom("2026-06-01", "2026-06-30").range!!
        val active = DateRangeCalculator.range(TimeRangeType.CUSTOM, millis(2026, 6, 25, 12, 0), custom)

        assertEquals(custom, active)
    }

    @Test
    fun statsDefaultRangeIsCustomFromMonthStartToToday() {
        val defaults = StatsRangeDefaults.initial(millis(2026, 6, 25, 12, 0))

        assertEquals(TimeRangeType.CUSTOM, defaults.selectedRange)
        assertEquals("2026-06-01", defaults.customStart)
        assertEquals("2026-06-25", defaults.customEnd)
        assertEquals(DateRangeCalculator.custom("2026-06-01", "2026-06-25").range, defaults.customRange)
    }

    @Test
    fun invalidDateAndTimeReturnErrors() {
        assertNotNull(DateRangeCalculator.parseDateTime("2026-99-99", "14:30").error)
        assertNotNull(DateRangeCalculator.parseDateTime("2026-06-01", "25:99").error)
        assertNotNull(DateRangeCalculator.custom("2026-06-30", "2026-06-01").error)
    }

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun tx(amount: Double, type: TransactionType, time: Long) = TransactionEntity(
        amount = amount,
        type = type,
        categoryPrimary = if (type == TransactionType.INCOME) "收入" else "餐饮",
        categorySecondary = if (type == TransactionType.INCOME) "工资" else "外卖",
        merchant = "测试",
        account = "测试账户",
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
