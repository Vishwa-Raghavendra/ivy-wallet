package com.ivy.legacy.legacy.ui.theme.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.utils.convertLocalToUTC
import com.ivy.legacy.utils.convertUTCToLocal
import com.ivy.legacy.utils.convertUTCtoLocal
import com.ivy.legacy.utils.formatLocalTime
import com.ivy.legacy.utils.formatNicely
import com.ivy.legacy.utils.timeNowUTC
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.IvyOutlinedButton
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Composable
fun DateTimeRow(
    dateTime: LocalDateTime,
    onSetDateTime: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier
) {
    val ivyContext = ivyWalletCtx()

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        IvyOutlinedButton(
            text = dateTime.formatNicely(),
            iconStart = R.drawable.ic_date
        ) {
            ivyContext.datePicker(
                initialDate = dateTime.convertUTCtoLocal().toLocalDate()
            ) {
                onSetDateTime(getTrueDate(it, dateTime.toLocalTime()))
            }
        }

        Spacer(Modifier.weight(1f))

        IvyOutlinedButton(
            text = dateTime.formatLocalTime(),
            iconStart = R.drawable.ic_date
        ) {
            ivyContext.timePicker(
                initialTime = dateTime.convertUTCtoLocal().toLocalTime()
            ) {
                onSetDateTime(getTrueDate(dateTime.convertUTCtoLocal().toLocalDate(), it))
            }
        }

        Spacer(Modifier.width(24.dp))
    }
}

// The timepicker returns time in UTC, but the date picker returns date in LocalTimeZone
// hence use this method to get both date & time in UTC
@Deprecated("Rework this to use the TimeConverter API")
fun getTrueDate(
    date: LocalDate,
    time: LocalTime,
    convert: Boolean = true
): LocalDateTime {
    val timeLocal = if (convert) time.convertUTCToLocal() else time

    return timeNowUTC()
        .withYear(date.year)
        .withMonth(date.monthValue)
        .withDayOfMonth(date.dayOfMonth)
        .withHour(timeLocal.hour)
        .withMinute(timeLocal.minute)
        .withSecond(0)
        .withNano(0)
        .convertLocalToUTC()
}