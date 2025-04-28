package com.ryanthetechman.cherrycal.ui.view.day

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ryanthetechman.cherrycal.calendar_interaction.CalendarEvent
import com.ryanthetechman.cherrycal.occursOn
import com.ryanthetechman.cherrycal.ui.DayViewHeader
import com.ryanthetechman.cherrycal.ui.view.PagerOrientation
import com.ryanthetechman.cherrycal.ui.view.ParallelText
import com.ryanthetechman.cherrycal.ui.view.PrettyPager
import com.ryanthetechman.cherrycal.ui.view.week.TimeColumn
import com.ryanthetechman.cherrycal.ui.view.week.formatReadableDate
import kotlinx.datetime.*

@Composable
fun DayPager(
    initialDate: LocalDate,
    events: List<CalendarEvent>,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    onDateChange: (LocalDate) -> Unit = {},
    onTimeSlotTap: (LocalDate, Int, Int) -> Unit = { _, _, _ -> }
) {
    val initialPage          = Int.MAX_VALUE / 2
    val baseDate by remember { mutableStateOf(initialDate) }
    val scrollForwardIsNext  = false
    val reversePageDirection = false

    var showCol by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showCol = true }
    val timeColWidth by animateDpAsState(if (showCol) 50.dp else 0.dp, tween(250))

    BoxWithConstraints(
        Modifier.fillMaxSize().zIndex(1f)
    ) {
        val H = maxHeight
        val W = maxWidth
        val headerHeight = 70.dp
        val today = Clock.System.now().toLocalDateTime(timeZone).date

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
                        val raw  = if (reversePageDirection) initialPage - p else p - initialPage
                        val date = baseDate.plus(raw, DateTimeUnit.DAY)
                        onDateChange(date)
                    }
                ) { page, pageOffset ->
                    val raw       = if (reversePageDirection) initialPage - page else page - initialPage
                    val date      = baseDate.plus(raw, DateTimeUnit.DAY)
                    val dayEvents = events.filter { it.occursOn(date, timeZone) }

                    /* content */
                    Column(Modifier.fillMaxSize()) {
                        DayViewHeader(
                            date = date,
                            events = dayEvents,
                            timeZone = timeZone,
                            modifier = Modifier.height(headerHeight)
                        )
                        Box(
                            Modifier
                                .fillMaxSize()
                                .pointerInput(date) {
                                    detectTapGestures { offset ->
                                        val minuteOfDay = ((offset.y / size.height) * 24f * 60f).toInt().coerceIn(0, 24 * 60 - 1)
                                        val hour = minuteOfDay / 60
                                        val minute = (minuteOfDay % 60).let { if (it < 30) 0 else 30 }

                                        onTimeSlotTap(date, hour, minute)
                                    }
                                }
                        ) {
                            WeekEventsLayout(
                                date = date,
                                events = dayEvents,
                                timeZone = timeZone,
                                modifier = Modifier.fillMaxSize(),
                                timelineAlpha = if (date == today) 0.4f else 0f // Now Line
                            )
                        }
                    }

                    ParallelText(
                        text        = formatReadableDate(date),
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