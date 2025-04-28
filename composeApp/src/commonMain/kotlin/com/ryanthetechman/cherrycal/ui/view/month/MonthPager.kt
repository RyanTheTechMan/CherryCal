package com.ryanthetechman.cherrycal.ui.view.month

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.ryanthetechman.cherrycal.calendar_interaction.CalendarEvent
import com.ryanthetechman.cherrycal.calculateYearMonth
import com.ryanthetechman.cherrycal.ui.view.PagerOrientation
import com.ryanthetechman.cherrycal.ui.view.ParallelText
import com.ryanthetechman.cherrycal.ui.view.PrettyPager
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

@Composable
fun MonthPager(
    initialYear: Int,
    initialMonth: Int,
    currentYear: Int,
    currentMonth: Int,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    eventsCache: Map<Pair<Int, Int>, List<CalendarEvent>>,
    onMonthChange: (Int, Int) -> Unit = { _, _ -> },
    onDayClick: (LocalDate) -> Unit = {},
    onDayDoubleClick: (LocalDate) -> Unit = {},
    onWeekExpandComplete: (List<LocalDate>) -> Unit = {}
) {
    data class YearMonth(val year: Int, val month: Int)
    val baseMonth by remember { mutableStateOf(YearMonth(initialYear, initialMonth)) }

    val initialPage = Int.MAX_VALUE / 2
    val monthNames  = listOf(
        "January","February","March","April","May","June",
        "July","August","September","October","November","December"
    )

    val targetPage = remember(currentYear, currentMonth) {
        val delta = (currentYear - baseMonth.year) * 12 +
                (currentMonth - baseMonth.month)
        initialPage + delta
    }

    PrettyPager(
        orientation          = PagerOrientation.Vertical,
        initialPage          = initialPage,
        externalPage         = targetPage,
        scrollForwardIsNext  = false,
        reversePageDirection = false,
        onPageChanged = { page ->
            val offset     = page - initialPage
            val (y, m)     = calculateYearMonth(baseMonth.year, baseMonth.month, offset)
            onMonthChange(y, m)
        }
    ) { page, pageOffset ->
        val offset        = page - initialPage
        val (year, month) = calculateYearMonth(baseMonth.year, baseMonth.month, offset)

        val events = (
                eventsCache[year to month].orEmpty()
                + eventsCache[year to month - 1].orEmpty()
                + eventsCache[year to month + 1].orEmpty()
                )

        BoxWithConstraints(Modifier.fillMaxSize()) {
            MonthView(
                events               = events,
                year                 = year,
                month                = month,
                timeZone             = timeZone,
                onDayClick           = onDayClick,
                onDayDoubleClick     = onDayDoubleClick,
                onWeekExpandComplete = onWeekExpandComplete
            )

            ParallelText(
                text        = "${monthNames[month - 1]} $year",
                orientation = PagerOrientation.Vertical,
                pageOffset  = pageOffset
            )
        }
    }
}