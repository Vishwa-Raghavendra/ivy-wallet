package com.ivy.wallet.core.utils

import com.ivy.wallet.ui.onboarding.model.FromToTimeRange
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

fun LocalDateTime?.atLocalDateTimeToMilliSeconds(): Long {
    return this?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: -1L
}

fun LocalDateTime.fromUtcToLocalTimeNew() = this.atZone(ZoneId.systemDefault()).toLocalDateTime()

fun FromToTimeRange?.getDateTimeComparator(): DateTimeComparator {
    return DateTimeComparator(this)
}

internal fun LocalDateTime?.atEndOfDay(): LocalDateTime? {
    return this?.let {
        LocalDateTime.of(it.toLocalDate(), LocalTime.MAX)
    }
}


class DateTimeComparator internal constructor(fromToTimeRange: FromToTimeRange?) {
    private val lowerLimit: Long
    private val upperLimit: Long

    init {
        lowerLimit = fromToTimeRange?.from().atLocalDateTimeToMilliSeconds()
        upperLimit = fromToTimeRange?.to().atEndOfDay().atLocalDateTimeToMilliSeconds()
    }

    fun isDateInRange(time: LocalDateTime?): Boolean {
        val timeInMilli = time.atLocalDateTimeToMilliSeconds()
        return when {
            (timeInMilli == -1L || lowerLimit == -1L || upperLimit == -1L) -> false
            timeInMilli in lowerLimit..upperLimit -> true
            else -> false
        }
    }

    fun getStartTimeInMilli() = lowerLimit
    fun getEndTimeInMilli() = upperLimit
}