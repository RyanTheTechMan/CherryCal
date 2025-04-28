package com.ryanthetechman.cherrycal.ui.view.month

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.times
import com.ryanthetechman.cherrycal.calendar_interaction.CalendarEvent
import com.ryanthetechman.cherrycal.daysInMonth
import com.ryanthetechman.cherrycal.getNextMonth
import com.ryanthetechman.cherrycal.getPreviousMonth
import com.ryanthetechman.cherrycal.getWeekdayOffset
import com.ryanthetechman.cherrycal.ui.view.week.WeekPager
import com.ryanthetechman.cherrycal.ui.view.week.WeekRow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs

@Composable
fun MonthView(
    events: List<CalendarEvent>,
    year: Int,
    month: Int,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    onDayClick: (LocalDate) -> Unit = {},
    onDayDoubleClick: (LocalDate) -> Unit = {},
    onWeekExpandComplete: (List<LocalDate>) -> Unit = {},
    onTimeSlotTap: (LocalDate, Int, Int) -> Unit = { _, _, _ -> },
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val firstDayOfMonth = LocalDate(year, month, 1)
        val totalDays = daysInMonth(year, month)
        val blankDays = getWeekdayOffset(firstDayOfMonth)
        val (prevYear, prevMonth) = getPreviousMonth(year, month)
        val prevMonthTotalDays = daysInMonth(prevYear, prevMonth)
        val gridDays = mutableListOf<LocalDate>().apply {
            for (i in 0 until blankDays) {
                add(LocalDate(prevYear, prevMonth, prevMonthTotalDays - blankDays + i + 1))
            }
            for (day in 1..totalDays) {
                add(LocalDate(year, month, day))
            }
            val remainder = size % 7
            if (remainder != 0) {
                val (nextYear, nextMonth) = getNextMonth(year, month)
                val daysToAdd = 7 - remainder
                for (day in 1..daysToAdd) {
                    add(LocalDate(nextYear, nextMonth, day))
                }
            }
        }
        val rowCount = gridDays.size / 7
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        val currentWeekIndex = gridDays.indexOf(today).let { if (it >= 0) it / 7 else null }

        var expandedWeekIndex by remember { mutableStateOf<Int?>(null) }
        var dragDeltaX by remember { mutableStateOf(0f) }
        var dragStartX by remember { mutableStateOf<Float?>(null) }
        val deadZonePx = 25f
        val thresholdPx = 200f
        val targetProgress = if (expandedWeekIndex != null && dragDeltaX > deadZonePx)
            (dragDeltaX / thresholdPx).coerceIn(0f, 1f)
        else 0f
        val progress by animateFloatAsState(targetValue = targetProgress)

        var expandedWeekDates by remember { mutableStateOf<List<LocalDate>?>(null) }

        // Lock vertical scrolling once the horizontal drag delta exceeds deadZonePx OR when week view is active.
        val isTransitioningToWeek = (dragDeltaX > deadZonePx) || (expandedWeekDates != null)

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val containerHeight = maxHeight
            val containerWidth = maxWidth
            val normalRowHeight = containerHeight / rowCount
            val expandedRowHeight = lerp(normalRowHeight, containerHeight, progress)
            val columnHeight = if (expandedWeekIndex != null)
                (rowCount - 1) * normalRowHeight + expandedRowHeight
            else rowCount * normalRowHeight
            val translationY = if (expandedWeekIndex != null)
                -(expandedWeekIndex!! * normalRowHeight * progress)
            else 0.dp

            // Vertical scrolling is disabled while transitioning to week view.
            Box(modifier = Modifier.verticalScroll(rememberScrollState(), enabled = !isTransitioningToWeek)) {
                Column(
                    modifier = Modifier
                        .height(columnHeight)
                        .offset(y = translationY)
                ) {
                    for (week in 0 until rowCount) {
                        val weekDates = gridDays.subList(week * 7, week * 7 + 7)
                        val isExpandedRow = (week == expandedWeekIndex)
                        val isThisCurrentWeek = (currentWeekIndex != null && week == currentWeekIndex)
                        val rowAlpha = if (isExpandedRow) 1f else 1f - progress
                        val rowHeight = if (isExpandedRow) expandedRowHeight else normalRowHeight

                        WeekRow(
                            weekDates = weekDates,
                            rowHeight = rowHeight,
                            cellWidth = containerWidth / 7,
                            events = events,
                            currentMonth = month,
                            timeZone = timeZone,
                            onDayClick = onDayClick,
                            onDayDoubleClick   = onDayDoubleClick,
                            onDrag = { change, dragAmount ->
                                // Only consider horizontal drags.
                                if (abs(dragAmount.x) > abs(dragAmount.y)) {
                                    if (dragStartX == null) {
                                        dragStartX = change.position.x
                                        expandedWeekIndex = week
                                    }
                                    val currentX = change.position.x
                                    dragDeltaX = abs(currentX - (dragStartX ?: currentX))
                                }
                            },
                            onDragEnd = {
                                if (progress >= 0.5f) {
                                    expandedWeekDates = gridDays.subList(expandedWeekIndex!! * 7, expandedWeekIndex!! * 7 + 7)
                                    onWeekExpandComplete(expandedWeekDates!!)
                                } else {
                                    dragDeltaX = 0f
                                    expandedWeekIndex = null
                                }
                                dragStartX = null
                            },
                            transitionProgress = if (isExpandedRow) progress else 0f,
                            isCurrentWeek = isThisCurrentWeek,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(rowHeight)
                                .alpha(rowAlpha),
                            onTimeSlotTap = onTimeSlotTap
                        )
                    }
                }
            }
        }

        if (expandedWeekDates != null) {
            WeekPager(
                events               = events,
                initialWeekDates     = expandedWeekDates!!,
                timeZone             = timeZone,
                animateDayTransition = false,
                onDayDoubleClick     = onDayDoubleClick,
                onWeekChange         = { newWeekDates -> expandedWeekDates = newWeekDates },
                onExitWeekView       = {
                    expandedWeekDates = null
                    dragDeltaX        = 0f
                    expandedWeekIndex = null
                },
                onTimeSlotTap = onTimeSlotTap
            )
        }
    }
}