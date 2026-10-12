package com.autoledgercn.domain.time

import com.autoledgercn.data.entity.TransactionEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.time.temporal.TemporalAdjusters

enum class TimeRangeType(val label: String) {
    TODAY("今日"),
    WEEK("本周"),
    MONTH("本月"),
    YEAR("本年"),
    ALL("全部"),
    CUSTOM("自定义")
}

data class DateRange(
    val startMillis: Long,
    val endMillis: Long,
    val label: String
)

data class DateRangeResult(
    val range: DateRange? = null,
    val error: String? = null
)

data class DateTimeParseResult(
    val millis: Long? = null,
    val error: String? = null
)

object DateRangeCalculator {
    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd")
        .withResolverStyle(ResolverStyle.STRICT)
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        .withResolverStyle(ResolverStyle.STRICT)

    fun range(type: TimeRangeType, nowMillis: Long = System.currentTimeMillis(), custom: DateRange? = null): DateRange =
        when (type) {
            TimeRangeType.TODAY -> today(nowMillis)
            TimeRangeType.WEEK -> week(nowMillis)
            TimeRangeType.MONTH -> month(nowMillis)
            TimeRangeType.YEAR -> year(nowMillis)
            TimeRangeType.ALL -> all()
            TimeRangeType.CUSTOM -> custom ?: today(nowMillis)
        }

    fun today(nowMillis: Long = System.currentTimeMillis()): DateRange {
        val day = localDate(nowMillis)
        return fromDates(day, day, "今日")
    }

    fun week(nowMillis: Long = System.currentTimeMillis()): DateRange {
        val day = localDate(nowMillis)
        val start = day.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        val end = start.plusDays(6)
        return fromDates(start, end, "本周")
    }

    fun month(nowMillis: Long = System.currentTimeMillis()): DateRange {
        val day = localDate(nowMillis)
        return fromDates(day.withDayOfMonth(1), day.withDayOfMonth(day.lengthOfMonth()), "本月")
    }

    fun year(nowMillis: Long = System.currentTimeMillis()): DateRange {
        val day = localDate(nowMillis)
        return fromDates(day.withDayOfYear(1), day.withDayOfYear(day.lengthOfYear()), "本年")
    }

    fun all(): DateRange = DateRange(Long.MIN_VALUE, Long.MAX_VALUE, "全部")

    fun custom(startDateString: String, endDateString: String): DateRangeResult {
        val start = parseDate(startDateString) ?: return DateRangeResult(error = "请输入正确日期，例如 2026-06-25")
        val end = parseDate(endDateString) ?: return DateRangeResult(error = "请输入正确日期，例如 2026-06-25")
        if (end.isBefore(start)) return DateRangeResult(error = "结束日期不能早于开始日期")
        return DateRangeResult(range = fromDates(start, end, "${dateFormatter.format(start)} ~ ${dateFormatter.format(end)}"))
    }

    fun parseDateTime(dateString: String, timeString: String): DateTimeParseResult {
        val date = parseDate(dateString) ?: return DateTimeParseResult(error = "请输入正确日期，例如 2026-06-25")
        val time = parseTime(timeString) ?: return DateTimeParseResult(error = "请输入正确时间，例如 14:30")
        val millis = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
        return DateTimeParseResult(millis = millis)
    }

    fun filter(transactions: List<TransactionEntity>, range: DateRange): List<TransactionEntity> =
        transactions.filter { it.transactionTime in range.startMillis..range.endMillis }

    fun dateString(millis: Long): String = dateFormatter.format(localDate(millis))

    fun timeString(millis: Long): String =
        timeFormatter.format(java.time.Instant.ofEpochMilli(millis).atZone(zone).toLocalTime())

    private fun fromDates(start: LocalDate, end: LocalDate, label: String): DateRange =
        DateRange(
            startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli(),
            endMillis = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1,
            label = label
        )

    private fun localDate(millis: Long): LocalDate =
        java.time.Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    private fun parseDate(value: String): LocalDate? =
        try {
            LocalDate.parse(value.trim(), dateFormatter)
        } catch (_: DateTimeParseException) {
            null
        }

    private fun parseTime(value: String): LocalTime? =
        try {
            LocalTime.parse(value.trim(), timeFormatter)
        } catch (_: DateTimeParseException) {
            null
        }
}
