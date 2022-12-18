package com.ivy.core.ui.time.picker.date

import com.ivy.core.ui.time.picker.date.data.PickerDay
import com.ivy.core.ui.time.picker.date.data.PickerMonth
import com.ivy.core.ui.time.picker.date.data.PickerYear

sealed interface DatePickerEvent {
    data class DayChange(val day: PickerDay) : DatePickerEvent
    data class MonthChange(val month: PickerMonth) : DatePickerEvent
    data class YearChange(val year: PickerYear) : DatePickerEvent
}