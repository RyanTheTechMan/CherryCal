package com.ryanthetechman.cherrycal.ui.view.week

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import com.ryanthetechman.cherrycal.calendar_interaction.CalendarEvent
import com.ryanthetechman.cherrycal.occursOn
import com.ryanthetechman.cherrycal.ui.view.day.DayCell
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

@Composable
fun WeekRow(
    weekDates: List<LocalDate>,
    rowHeight: Dp,
    cellWidth: Dp,
    events: List<CalendarEvent>,
    currentMonth: Int,
    timeZone: TimeZone,
    onDayClick: (LocalDate) -> Unit,
    onDayDoubleClick: (LocalDate) -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onTimeSlotTap: (LocalDate, Int, Int) -> Unit = { _, _, _ -> },
    transitionProgress: Float = 0f,
    isCurrentWeek: Boolean = false,
    modifier: Modifier = Modifier
) {
    val today         = Clock.System.now().toLocalDateTime(timeZone).date
    val weekHasToday  = weekDates.contains(today)

    Row(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures(
                onDrag    = onDrag,
                onDragEnd = onDragEnd
            )
        }
    ) {
        weekDates.forEach { date ->
            Box(
                modifier = Modifier
                    .width(cellWidth)
                    .fillMaxHeight()
            ) {
                val isToday          = date == today
                val isInCurrentMonth = date.month.number == currentMonth
                val alphaNowLine     = when { // Now Line
                    isToday      -> 0.4f
                    weekHasToday -> 0.15f
                    else         -> 0.0f
                }

                DayCell(
                    date               = date,
                    events             = events.filter { it.occursOn(date, timeZone) },
                    onDayClick         = { onDayClick(date) },
                    onDayDoubleClick   = onDayDoubleClick,
                    isToday            = isToday,
                    isInCurrentMonth   = isInCurrentMonth,
                    isCurrentWeek      = isCurrentWeek,
                    transitionProgress = transitionProgress,
                    timeZone           = timeZone,
                    onTimedTap         = onTimeSlotTap,
                    timelineAlpha      = alphaNowLine
                )
            }
        }
    }
}

fun formatDateTime(date: LocalDate, hour: Int, minute: Int): String {
    return "${date.dayOfMonth.toString().padStart(2, '0')}/${date.month.name.lowercase()}/" +
            "${date.year} ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}