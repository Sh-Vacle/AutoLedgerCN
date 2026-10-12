package com.autoledgercn.domain.stats

import com.autoledgercn.domain.time.DateRange
import com.autoledgercn.domain.time.DateRangeCalculator
import com.autoledgercn.domain.time.TimeRangeType

data class StatsRangeDefaultState(
    val selectedRange: TimeRangeType,
    val customStart: String,
    val customEnd: String,
    val customRange: DateRange
)

object StatsRangeDefaults {
    fun initial(nowMillis: Long = System.currentTimeMillis()): StatsRangeDefaultState {
        val customStart = DateRangeCalculator.dateString(DateRangeCalculator.month(nowMillis).startMillis)
        val customEnd = DateRangeCalculator.dateString(nowMillis)
        val customRange = DateRangeCalculator.custom(customStart, customEnd).range
            ?: DateRangeCalculator.month(nowMillis)
        return StatsRangeDefaultState(
            selectedRange = TimeRangeType.CUSTOM,
            customStart = customStart,
            customEnd = customEnd,
            customRange = customRange
        )
    }
}
