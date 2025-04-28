package com.ryanthetechman.cherrycal.ui.view.week

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ryanthetechman.cherrycal.calendar_interaction.CalendarEvent
import com.ryanthetechman.cherrycal.ui.view.PagerOrientation
import com.ryanthetechman.cherrycal.ui.view.ParallelText
import com.ryanthetechman.cherrycal.ui.view.PrettyPager
import com.ryanthetechman.cherrycal.ui.WeekViewHeader
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.plus

@Composable
fun WeekPager(
    events: List<CalendarEvent>,
    initialWeekDates: List<LocalDate>,
    timeZone: TimeZone,
    animateDayTransition: Boolean = true,
    onDayDoubleClick: (LocalDate) -> Unit = {},
    onWeekChange: (List<LocalDate>) -> Unit = {},
    onExitWeekView: () -> Unit = {},
    onTimeSlotTap: (LocalDate, Int, Int) -> Unit = { _, _, _ -> },
) {
    val scrollForwardIsNext   = false
    val reversePageDirection  = false
    val initialPage           = Int.MAX_VALUE / 2
    val baseWeek by remember { mutableStateOf(initialWeekDates) }

    var progress by remember { mutableStateOf(if (animateDayTransition) 0f else 1f) }
    LaunchedEffect(animateDayTransition) {
        if (animateDayTransition)
            animate(0f, 1f, 0f, tween(300)) { v, _ -> progress = v }
    }

    var showCol by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showCol = true }
    val timeColWidth by animateDpAsState(if (showCol) 50.dp else 0.dp, tween(250))

    BoxWithConstraints(
        Modifier.fillMaxSize().zIndex(1f)
    ) {
        val H = maxHeight
        val W = maxWidth
        val headerHeight = 70.dp

        Row(Modifier.fillMaxSize()) {
            TimeColumn(
                modifier = Modifier
                    .width(timeColWidth)
                    .height(H - headerHeight)
                    .offset(y = headerHeight)
            )

            Box(Modifier.fillMaxSize()) {
                PrettyPager(
                    orientation          = PagerOrientation.Horizontal,
                    initialPage          = initialPage,
                    scrollForwardIsNext  = scrollForwardIsNext,
                    reversePageDirection = reversePageDirection,
                    onPageChanged = { p ->
                        val raw = if (reversePageDirection) initialPage - p else p - initialPage
                        onWeekChange(baseWeek.map { it.plus(raw * 7, DateTimeUnit.DAY) })
                    }
                ) { page, pageOffset ->
                    val raw       = if (reversePageDirection) initialPage - page else page - initialPage
                    val weekDates = baseWeek.map { it.plus(raw * 7, DateTimeUnit.DAY) }
                    val cellW     = (W - timeColWidth) / 7
                    val weekLabel = formatWeekRangeShort(weekDates)

                    Column(Modifier.fillMaxSize()) {
                        WeekViewHeader(
                            weekDates = weekDates,
                            events = events,
                            timeZone = timeZone,
                            modifier = Modifier.height(headerHeight)
                        )
                        WeekRow(
                            events              = events,
                            weekDates           = weekDates,
                            rowHeight           = H,
                            cellWidth           = cellW,
                            currentMonth        = weekDates.first().month.number,
                            timeZone            = timeZone,
                            onDayClick          = {},
                            onDayDoubleClick    = onDayDoubleClick,
                            onDrag              = { _, _ -> },
                            onDragEnd           = {},
                            transitionProgress  = progress,
                            onTimeSlotTap       = onTimeSlotTap
                        )
                    }

                    ParallelText(
                        text        = weekLabel,
                        orientation = PagerOrientation.Horizontal,
                        pageOffset  = pageOffset,
                        insetStart  = with(LocalDensity.current) { timeColWidth.toPx() },
                        reversePageDirection = reversePageDirection
                    )
                }
            }
        }
    }
}

@Composable
fun TimeColumn(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val hourHeight = maxHeight / 24
        Column(modifier = Modifier.fillMaxSize()) {
            for (hour in 0 until 24) {
                Box(
                    modifier = Modifier
                        .height(hourHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopStart
                ) {
                    Text(
                        text = formatTime(hour),
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }
        }
    }
}

fun formatTime(hour: Int): String {
    val suffix = if (hour < 12) "AM" else "PM"
    val hour12 = when (hour % 12) {
        0    -> 12
        else -> hour % 12
    }
    val formattedHour = hour12.toString().padStart(2, '0')
    return "$formattedHour $suffix"
}

fun formatReadableDate(date: LocalDate): String {
    val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$month ${date.dayOfMonth}, ${date.year}"
}

fun formatWeekRangeShort(dates: List<LocalDate>): String {
    val start = dates.first()
    val end = dates.last()
    val monthAbbrev = start.month.name.take(3).replaceFirstChar { it.uppercase() }  // “Apr”
    return if (start.month == end.month) {
        "$monthAbbrev ${start.dayOfMonth}–${end.dayOfMonth}, ${start.year}"
    } else {
        val endMonthAbbrev = end.month.name.take(3).replaceFirstChar { it.uppercase() }
        "$monthAbbrev ${start.dayOfMonth} – $endMonthAbbrev ${end.dayOfMonth}, ${start.year}"
    }
}