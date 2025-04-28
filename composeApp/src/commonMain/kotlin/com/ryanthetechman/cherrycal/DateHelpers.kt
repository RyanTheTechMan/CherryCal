package com.ryanthetechman.cherrycal

import com.ryanthetechman.cherrycal.calendar_interaction.CalendarEvent
import com.ryanthetechman.cherrycal.calendar_interaction.EventTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.until

// Instead of using epoch days, we use the ISO day number.
// In ISO-8601, Monday = 1, Tuesday = 2, … Sunday = 7.
// We want Sunday in column 0, Monday in column 1, … Saturday in column 6.
fun getWeekdayOffset(date: LocalDate): Int {
    val iso = date.dayOfWeek.isoDayNumber
    return if (iso == 7) 0 else iso
}

// Returns the number of days in the given month.
fun daysInMonth(year: Int, month: Int): Int {
    val start = LocalDate(year, month, 1)
    val end = start.plus(1, kotlinx.datetime.DateTimeUnit.MONTH)
    return start.until(end, kotlinx.datetime.DateTimeUnit.DAY)
}

// Helper function for getting previous month
fun getPreviousMonth(year: Int, month: Int): Pair<Int, Int> =
    if (month == 1) Pair(year - 1, 12) else Pair(year, month - 1)

// Helper function for getting next month
fun getNextMonth(year: Int, month: Int): Pair<Int, Int> =
    if (month == 12) Pair(year + 1, 1) else Pair(year, month + 1)

fun calculateYearMonth(initialYear: Int, initialMonth: Int, offset: Int): Pair<Int, Int> {
    val startMonthIndex = (initialYear * 12) + (initialMonth - 1)
    val newMonthIndex = startMonthIndex + offset

    val year = newMonthIndex.floorDiv(12)
    val month = (newMonthIndex % 12).let { if (it < 0) it + 12 else it } + 1

    return Pair(year, month)
}

fun CalendarEvent.occursOn(date: LocalDate, timeZone: TimeZone = TimeZone.currentSystemDefault()): Boolean {
    return when (val t = this.time) {
        is EventTime.Timed -> {
            // For a timed event, use the start instant’s date.
            t.start.toLocalDateTime(timeZone).date == date
        }
        is EventTime.AllDay -> {
            // For an all-day event, check if the date is in the event’s range.
            // (Assumes endDateExclusive is exclusive.)
            date >= t.startDate && date < t.endDateExclusive
        }

        null -> TODO()
    }
}