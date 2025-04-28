package com.ryanthetechman.cherrycal.ui.view.day

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.ryanthetechman.cherrycal.calendar_interaction.CalendarEvent
import com.ryanthetechman.cherrycal.calendar_interaction.EventTime
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun DayCell(
    date: LocalDate,
    events: List<CalendarEvent>,
    onDayClick: (LocalDate) -> Unit,
    isToday: Boolean,
    isInCurrentMonth: Boolean,
    isCurrentWeek: Boolean,
    transitionProgress: Float,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    onDayDoubleClick: ((LocalDate) -> Unit)? = null,
    onTimedTap: ((date: LocalDate, hour: Int, minute: Int) -> Unit)? = null,
    timelineAlpha: Float = 0f
) {
    val baseMonthColor = if (isInCurrentMonth) Color(0xFFE6E6E6) else Color(0xFFCCCCCC)
    val weekTintBase   = Color(0xFFFCEBEB)

    val monthHighlight = when {
        !isCurrentWeek       -> baseMonthColor                      // not current-week but in current month
        isInCurrentMonth     -> weekTintBase                        // current-week and in current month
        else                 -> lerp(baseMonthColor, weekTintBase, 0.333f)  // Current week but not in current month
    }

    val blendedBg   = lerp(monthHighlight, baseMonthColor, transitionProgress)
    val todayWeekBg = Color(0xFFFFEBEE)

    val cellBackground = if (isToday)
        lerp(blendedBg, todayWeekBg, transitionProgress)
    else
        blendedBg

    val dayBorderWidth  = 2.dp
    val weekBorderWidth = 0.dp

    val cellBorderWidth = if (isToday)
        lerp(dayBorderWidth, weekBorderWidth, transitionProgress)
    else
        0.dp

    val cellBorderColor = if (isToday)
        lerp(Color.Red, Color(0xFF999999), transitionProgress)
    else
        Color(0xFF999999)

    var cellHeightPx by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .onSizeChanged { cellHeightPx = it.height }
            .fillMaxSize()
            .background(cellBackground)
            .border(width = cellBorderWidth, color = cellBorderColor)
            .pointerInput(onDayDoubleClick, onTimedTap) {
                detectTapGestures(
                    onTap = { offset ->
                        onTimedTap?.let { cb ->
                            if (cellHeightPx > 0) {
                                val fraction    = offset.y / cellHeightPx.toFloat()
                                val minuteOfDay = (fraction * 24f * 60f)
                                    .toInt()
                                    .coerceIn(0, 24 * 60 - 1)
                                val hour   = minuteOfDay / 60
                                val minute = if ((minuteOfDay % 60) < 30) 0 else 30
                                cb(date, hour, minute)
                            }
                        }
                        onDayClick(date)
                    },
                    onDoubleTap = { onDayDoubleClick?.invoke(date) }
                )
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MonthEventsLayout(
                events = events,
                timeZone = timeZone,
                modifier = Modifier.fillMaxSize().alpha(1f - transitionProgress)
            )
            WeekEventsLayout(
                date = date,
                events = events,
                timeZone = timeZone,
                modifier = Modifier.fillMaxSize().alpha(transitionProgress),
                timelineAlpha = timelineAlpha
            )
        }

        /* date badge */
        Box (
            modifier = Modifier
                .align(Alignment.TopEnd)
                .alpha(1 - transitionProgress)
        ){
            if (isToday) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(bottomStart = 8.dp))
                        .background(Color.Red)
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.caption,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            } else {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(2.dp)
                )
            }
        }
    }
}

@Composable
fun MonthEventsLayout(
    events: List<CalendarEvent>,
    timeZone: TimeZone,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(top = 28.dp, start = 4.dp, end = 4.dp, bottom = 4.dp)) {
        events.filter { it.time is EventTime.AllDay }
            .forEach { event ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFBBDEFB))
                        .padding(2.dp)
                ) {
                    Text(
                        text = event.title ?: "No title",
                        style = MaterialTheme.typography.caption,
                        color = Color.Black
                    )
                }
            }
        Spacer(modifier = Modifier.height(2.dp))
        events.filter { it.time is EventTime.Timed }.sortedBy { (it.time as EventTime.Timed).start }
            .forEach { event ->
                val eventTimeText: String = (event.time as? EventTime.Timed)
                    ?.start
                    ?.toLocalDateTime(timeZone)
                    ?.let { localDt -> formatTime(localDt.hour, localDt.minute) }
                    ?: ""
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFC8E6C9))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = eventTimeText,
                            style = MaterialTheme.typography.caption
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.title ?: "No title",
                        style = MaterialTheme.typography.caption
                    )
                }
            }
    }
}

@Composable
fun WeekEventsLayout(
    date: LocalDate,
    events: List<CalendarEvent>,
    timeZone: TimeZone,
    modifier: Modifier = Modifier,
    timelineAlpha: Float = 0f
) {
    val MIN_DURATION = 60 // week minimum duration in minutes

    BoxWithConstraints(modifier = modifier.then(
        Modifier.drawBehind {
            for (h in 0..24) {
                val y = size.height * (h / 24f)
                drawLine(
                    color       = Color.LightGray.copy(alpha = 0.30f),
                    start       = Offset(0f, y),
                    end         = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (timelineAlpha > 0f) {
                val now = Clock.System.now().toLocalDateTime(timeZone)
                val fraction = ((now.hour * 60 + now.minute) / (24f * 60f))
                val yNow = size.height * fraction
                drawLine(
                    color = Color.Red.copy(alpha = timelineAlpha),
                    start = Offset(0f, yNow),
                    end   = Offset(size.width, yNow),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }))
    {
        val totalHeight = maxHeight
        val totalWidth = maxWidth

        val timedEvents = events.filter { it.time is EventTime.Timed }
            .map { event -> event to (event.time as EventTime.Timed) }
            .sortedBy { it.second.start }

        // Layout info for each event
        data class LayoutInfo(
            val event: CalendarEvent,
            val timed: EventTime.Timed,
            val startFraction: Float,
            val endFraction: Float,
            var columnIndex: Int = 0,
            var totalColumns: Int = 1
        )

        val layoutInfos = timedEvents.map { (event, timed) ->
            val startDt = timed.start.toLocalDateTime(timeZone)
            val endDt = timed.end.toLocalDateTime(timeZone)
            val startMinutes = startDt.hour * 60 + startDt.minute
            val realEndMinutes = endDt.hour * 60 + endDt.minute
            val duration = realEndMinutes - startMinutes
            val endMinutes = if (duration < MIN_DURATION) {
                startMinutes + MIN_DURATION
            } else {
                realEndMinutes
            }

            val startFraction = startMinutes / (24f * 60f)
            val endFraction = endMinutes.coerceAtMost(24 * 60) / (24f * 60f)
            LayoutInfo(event, timed, startFraction, endFraction)
        }

        // Assign column indices (simple overlap algorithm)
        val active = mutableListOf<LayoutInfo>()
        layoutInfos.forEach { info ->
            // Remove events that have finished (do not overlap)
            active.removeAll { it.endFraction <= info.startFraction }
            // Determine the lowest unused column index
            val usedColumns = active.map { it.columnIndex }.toSet()
            var col = 0
            while (col in usedColumns) { col++ }
            info.columnIndex = col
            active.add(info)
            // Update total columns for all active events
            val currentColumns = active.maxOfOrNull { it.columnIndex }?.plus(1) ?: 1
            active.forEach { it.totalColumns = currentColumns }
        }

        // Render each event with its computed position and width
        layoutInfos.forEach { info ->
            val eventLeft = totalWidth * (info.columnIndex.toFloat() / info.totalColumns)
            val eventWidth = totalWidth / info.totalColumns
            val eventTop = totalHeight * info.startFraction
            val eventHeight = totalHeight * (info.endFraction - info.startFraction)
            Box(
                modifier = Modifier
                    .absoluteOffset(x = eventLeft, y = eventTop)
                    .width(eventWidth)
                    .height(eventHeight)
                    .background(Color(0xFFBBDEFB))
                    .padding(4.dp)
            ) {
                Text(
                    text = info.event.title ?: "No title",
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}

fun formatTime(hour: Int, minute: Int): String {
    val formattedHour = hour.toString().padStart(2, '0')
    val formattedMinute = minute.toString().padStart(2, '0')
    return "$formattedHour:$formattedMinute"
}
