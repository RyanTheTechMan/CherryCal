package com.ryanthetechman.cherrycal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ryanthetechman.cherrycal.calendar_interaction.CalendarEvent
import com.ryanthetechman.cherrycal.calendar_interaction.EventTime
import com.ryanthetechman.cherrycal.occursOn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber

private val DAY_NAMES = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
val MONTH_NAMES = listOf(
    "January","February","March","April","May","June",
    "July","August","September","October","November","December"
)

@Composable
fun MonthWeekdayHeader(
    year: Int,
    month: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // month-year label
        Text(
            text      = "${MONTH_NAMES[month - 1]}, $year",
            style     = MaterialTheme.typography.subtitle1,
            modifier  = Modifier.padding(start = 4.dp)
        )

        Spacer(Modifier.height(2.dp))

        // weekday abbreviations
        Row(Modifier.fillMaxWidth()) {
            DAY_NAMES.forEach { label ->
                Text(
                    text      = label,
                    modifier  = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style     = MaterialTheme.typography.caption
                )
            }
        }
    }
}

@Composable
fun WeekViewHeader(
    weekDates: List<LocalDate>,
    events: List<CalendarEvent>,
    timeZone: TimeZone,
    modifier: Modifier = Modifier
) {
    val monthLabel =
        "${MONTH_NAMES[weekDates.first().monthNumber - 1]}, ${weekDates.first().year}"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        // month-year label
        Text(
            text = monthLabel,
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )

        // day columns
        Row(Modifier.fillMaxWidth()) {
            weekDates.forEach { date ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                ) {
                    Text(
                        text      = DAY_NAMES[date.dayOfWeek.isoDayNumber % 7],
                        style     = MaterialTheme.typography.caption,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text      = date.dayOfMonth.toString(),
                        style     = MaterialTheme.typography.subtitle2,
                        textAlign = TextAlign.Center
                    )

                    /* all-day events */
                    val allDay = events.filter { ev ->
                        ev.time is EventTime.AllDay && ev.occursOn(date, timeZone)
                    }
                    allDay.take(3).forEach { ev ->
                        Text(
                            text = ev.title ?: "No title",
                            style = MaterialTheme.typography.overline,
                            maxLines = 1
                        )
                    }
                    if (allDay.size > 3) {
                        Text("…", style = MaterialTheme.typography.overline, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun DayViewHeader(
    date: LocalDate,
    events: List<CalendarEvent>,
    timeZone: TimeZone,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        val header = "${DAY_NAMES[date.dayOfWeek.isoDayNumber % 7]}, " +
                "${date.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${date.dayOfMonth}"
        Text(header, style = MaterialTheme.typography.h6)

        events.filter { it.time is EventTime.AllDay && it.occursOn(date, timeZone) }
            .forEach { ev ->
                Text("• ${ev.title ?: "No title"}", style = MaterialTheme.typography.body2)
            }
    }
}