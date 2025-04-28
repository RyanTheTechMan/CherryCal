package com.ryanthetechman.cherrycal.ui

import CalendarSelectorDrawer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ryanthetechman.cherrycal.calendar_interaction.Calendar
import com.ryanthetechman.cherrycal.calendar_interaction.CalendarDataManager
import com.ryanthetechman.cherrycal.calendar_interaction.CalendarEvent
import com.ryanthetechman.cherrycal.calendar_interaction.Contact
import com.ryanthetechman.cherrycal.calendar_interaction.EventStatus
import com.ryanthetechman.cherrycal.calendar_interaction.EventTime
import com.ryanthetechman.cherrycal.calendar_interaction.provider.AccountProvider
import com.ryanthetechman.cherrycal.calendar_interaction.provider.ProviderAccount
import com.ryanthetechman.cherrycal.calculateYearMonth
import com.ryanthetechman.cherrycal.ui.view.day.DayPager
import com.ryanthetechman.cherrycal.ui.view.month.MonthPager
import com.ryanthetechman.cherrycal.ui.view.week.WeekPager
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@Composable
fun CalendarScreen(
    selectedAccount: ProviderAccount?,
    currentProvider: AccountProvider?,
    selectedCalendar: Calendar?
) {
    if (selectedAccount == null || currentProvider == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Please sign in and select an account before viewing your calendar.")
        }
        return
    }

    val timeZone     = TimeZone.currentSystemDefault()
    val now          = Clock.System.now().toLocalDateTime(timeZone).date

    var selectedView   by remember { mutableStateOf(CalendarViewMode.MONTH) }

    // month / week
    var showWeekView   by remember { mutableStateOf(false) }
    var baseWeekDates  by remember { mutableStateOf(currentWeekDates(timeZone)) }

    val initialYear    = remember { now.year }
    val initialMonth   = remember { now.monthNumber }
    var currentYear    by remember { mutableStateOf(initialYear) }
    var currentMonth   by remember { mutableStateOf(initialMonth) }

    // month
    var monthEventCache by remember { mutableStateOf<Map<Pair<Int, Int>, List<CalendarEvent>>>(emptyMap()) }
    fun eventsForMonth(y: Int, m: Int): List<CalendarEvent> = monthEventCache[y to m] ?: emptyList()
    val weekEvents by remember(baseWeekDates, monthEventCache) {
        derivedStateOf {
            baseWeekDates.flatMap { d ->
                eventsForMonth(d.year, d.monthNumber)
            }.distinct()
        }
    }

    // day
    var selectedDay    by remember { mutableStateOf(now) }
    val dayEvents by remember(selectedDay, monthEventCache) {
        derivedStateOf {
            eventsForMonth(selectedDay.year, selectedDay.monthNumber)
        }
    }

    /* event-creation UI state */
    var showEventDialog by remember { mutableStateOf(false) }
    var draftDate       by remember { mutableStateOf<LocalDate?>(null) }
    var draftHour       by remember { mutableStateOf(0) }
    var draftMinute     by remember { mutableStateOf(0) }

    // data / scope
    val coroutineScope = rememberCoroutineScope()
    var calendars      by remember { mutableStateOf<List<Calendar>>(emptyList()) }
    var localSelectedCalendar by remember { mutableStateOf<Calendar?>(selectedCalendar) }

    LaunchedEffect(currentProvider, selectedAccount) {
        val result = currentProvider.getCalendars(selectedAccount)
        calendars = result.getOrElse { emptyList() }
        localSelectedCalendar = calendars.firstOrNull()
    }

    suspend fun loadAndCacheSurroundingMonths(year: Int, month: Int) {
        CalendarDataManager.loadSurroundingMonths(
            year, month, selectedAccount, currentProvider, localSelectedCalendar!!
        )
        (-1..1).forEach { delta ->
            val (y, m) = calculateYearMonth(year, month, delta)

            CalendarDataManager.getCachedMonth(y, m)?.let { fresh ->
                val existing = monthEventCache[y to m].orEmpty()

                // merge and drop duplicates (by your own chosen key)
                val merged = (existing + fresh)
                    .distinctBy { it.id ?: it.hashCode() }   // use ‘id’ if you have one

                monthEventCache = monthEventCache + ((y to m) to merged)
            }
        }
    }

    LaunchedEffect(localSelectedCalendar) {
        CalendarDataManager.clearCache()
        monthEventCache = emptyMap()
        localSelectedCalendar?.let {
            loadAndCacheSurroundingMonths(initialYear, initialMonth)
        }
    }

    LaunchedEffect(selectedDay) {
        if (eventsForMonth(selectedDay.year, selectedDay.monthNumber).isEmpty()) {
            loadAndCacheSurroundingMonths(selectedDay.year, selectedDay.monthNumber)
        }
    }

    if (localSelectedCalendar == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No calendars found for this account. Please add or select a different account.")
        }
        return
    }

    val openEventCreator: (LocalDate, Int, Int) -> Unit = { d, h, m ->
        draftDate   = d
        draftHour   = h
        draftMinute = m
        showEventDialog = true
    }

    fun addEventToCache(e: CalendarEvent) {
        val date = when (val t = e.time) {
            is EventTime.Timed -> t.start.toLocalDateTime(timeZone).date
            is EventTime.AllDay -> t.startDate
            else -> return
        }
        val key = date.year to date.monthNumber
        monthEventCache = monthEventCache + (key to (
                eventsForMonth(key.first, key.second) + e
                ))
    }

    fun createEvent(
        title: String,
        location: String?,
        description: String?,
        attendees: List<Contact>,
        start: Instant,
        end: Instant
    ) {
        coroutineScope.launch {
            val newEvent = CalendarEvent(
                createdAt = Clock.System.now(),
                time      = EventTime.Timed(start, end),
                title     = title,
                location  = location,
                description = description,
                attendees = attendees,
                status    = EventStatus.CONFIRMED
            )
            currentProvider.writeEvent(selectedAccount, localSelectedCalendar!!, newEvent)
                .onSuccess { saved ->
                    addEventToCache(saved)
                }
        }
    }

    val openDayView: (LocalDate) -> Unit = { dayDate ->
        selectedDay   = dayDate
        selectedView  = CalendarViewMode.DAY
        showWeekView  = false
    }

    Row(modifier = Modifier.fillMaxSize()) {
        CalendarSelectorDrawer(
            calendars         = calendars,
            selectedCalendar  = localSelectedCalendar,
            onCalendarSelected = { newCal ->
                localSelectedCalendar = newCal
                CalendarDataManager.clearCache()
                monthEventCache = emptyMap()
                coroutineScope.launch {
                    loadAndCacheSurroundingMonths(currentYear, currentMonth)
                }
            }
        )

        Column(modifier = Modifier.weight(1f)) {
            CalendarHeader(
                selectedView = selectedView,
                onViewSelected = { newMode ->
                    when (newMode) {
                        CalendarViewMode.MONTH -> {
                            if (selectedView == CalendarViewMode.DAY) {
                                currentYear  = selectedDay.year
                                currentMonth = selectedDay.monthNumber
                            } else {            // existing week→month logic
                                val firstDay  = baseWeekDates.firstOrNull() ?: now
                                currentYear   = firstDay.year
                                currentMonth  = firstDay.monthNumber
                            }
                            selectedView  = newMode
                            showWeekView  = false
                        }

                        CalendarViewMode.WEEK -> {
                            baseWeekDates =
                                if (selectedView == CalendarViewMode.DAY)
                                    weekDatesFor(selectedDay)
                                else
                                    currentWeekDates(timeZone)
                            selectedView   = newMode
                            showWeekView   = true
                            coroutineScope.launch {
                                val first = baseWeekDates.first()
                                if (eventsForMonth(first.year, first.monthNumber).isEmpty())
                                    loadAndCacheSurroundingMonths(first.year, first.monthNumber)
                            }
                        }

                        CalendarViewMode.DAY  -> {
                            selectedDay   = now
                            selectedView  = newMode
                            showWeekView  = false
                        }
                        CalendarViewMode.YEAR -> { println("Not Yet Implemented") }
                    }
                }
            )

            /* BODY */
            when (selectedView) {
                CalendarViewMode.MONTH -> {
                    MonthWeekdayHeader(
                        year = currentYear,
                        month = currentMonth,
                    )
                    MonthPager(
                        initialYear = initialYear,
                        initialMonth = initialMonth,
                        currentYear = currentYear,
                        currentMonth = currentMonth,
                        timeZone = timeZone,
                        onMonthChange = { y, m ->
                            currentYear = y
                            currentMonth = m
                            coroutineScope.launch {
                                loadAndCacheSurroundingMonths(y, m)
                            }
                        },
                        onDayClick = { },
                        onDayDoubleClick = openDayView,
                        eventsCache = monthEventCache,
                        onWeekExpandComplete = { weekDates ->
                            baseWeekDates = weekDates
                            selectedView = CalendarViewMode.WEEK
                            showWeekView = true
                            coroutineScope.launch {
                                val first = weekDates.first()
                                if (eventsForMonth(first.year, first.monthNumber).isEmpty())
                                    loadAndCacheSurroundingMonths(first.year, first.monthNumber)
                            }
                        }
                    )

                }

                CalendarViewMode.WEEK -> {
                    AnimatedVisibility(
                        visible = showWeekView,
                        exit    = fadeOut(animationSpec = tween(250))
                    ) {
                        WeekPager(
                            events               = weekEvents,
                            initialWeekDates     = baseWeekDates,
                            timeZone             = timeZone,
                            onDayDoubleClick     = openDayView,
                            onWeekChange         = { weekDates ->
                                baseWeekDates = weekDates
                                 coroutineScope.launch {
                                     val first = weekDates.first()
                                     if (eventsForMonth(first.year, first.monthNumber).isEmpty())
                                         loadAndCacheSurroundingMonths(
                                             first.year,
                                             first.monthNumber
                                         )
                                 }
                            },
                            onExitWeekView       = {
                                selectedView = CalendarViewMode.MONTH
                                showWeekView  = false
                                val first     = baseWeekDates.firstOrNull() ?: now
                                currentYear   = first.year
                                currentMonth  = first.monthNumber
                            },
                            animateDayTransition = false,
                            onTimeSlotTap = openEventCreator
                        )
                    }
                }

                CalendarViewMode.DAY -> {
                    DayPager(
                        initialDate   = selectedDay,
                        events        = dayEvents,
                        timeZone      = timeZone,
                        onDateChange  = { newDate ->
                            selectedDay = newDate
                        },
                        onTimeSlotTap = openEventCreator
                    )
                }

                else -> { println("Unknown view: $selectedView") }
            }
        }
    }

    if (showEventDialog && draftDate != null) {
        EventEditorDialog(
            timeZone      = timeZone,
            initialDate   = draftDate!!,
            initialHour   = draftHour,
            initialMinute = draftMinute,
            onDismiss     = { showEventDialog = false },
            onCreateEvent = { title, loc, desc, attendees, start, end ->
                createEvent(title, loc, desc, attendees, start, end)
            }
        )
    }
}

fun currentWeekDates(timeZone: TimeZone): List<LocalDate> {
    val today = Clock.System.now().toLocalDateTime(timeZone).date
    val daysSinceSunday = today.dayOfWeek.isoDayNumber % 7
    val sunday = today.minus(daysSinceSunday.toLong(), DateTimeUnit.DAY)
    return (0 until 7).map { sunday.plus(it.toLong(), DateTimeUnit.DAY) }
}

private fun weekDatesFor(date: LocalDate): List<LocalDate> {
    val daysSinceSunday = date.dayOfWeek.isoDayNumber % 7
    val weekStart = date.minus(daysSinceSunday.toLong(), DateTimeUnit.DAY)
    return List(7) { i -> weekStart.plus(i, DateTimeUnit.DAY) }
}