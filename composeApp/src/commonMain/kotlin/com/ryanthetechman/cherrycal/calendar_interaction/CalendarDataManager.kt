package com.ryanthetechman.cherrycal.calendar_interaction

import androidx.compose.runtime.mutableStateMapOf
import com.ryanthetechman.cherrycal.calendar_interaction.provider.AccountProvider
import com.ryanthetechman.cherrycal.calendar_interaction.provider.ProviderAccount
import com.ryanthetechman.cherrycal.daysInMonth
import com.ryanthetechman.cherrycal.getNextMonth
import com.ryanthetechman.cherrycal.getPreviousMonth
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus

/**
 * Simple in-memory cache & manager for calendar events, keyed by (year, month).
 */
object CalendarDataManager {
    // For each (year, month), store the loaded events.
    private val monthlyEventsCache = mutableStateMapOf<Pair<Int, Int>, List<CalendarEvent>>()


    /**
     * Returns the events for [year, month] if already cached, else null.
     */
    fun getCachedMonth(year: Int, month: Int): List<CalendarEvent>? {
        return monthlyEventsCache[year to month]
    }

    /**
     * Loads events for the specified [year, month] from the current provider, if not in cache.
     * Returns the final list of events for that month (cached or newly loaded).
     */
    suspend fun ensureMonthIsLoaded(
        year: Int,
        month: Int,
        account: ProviderAccount,
        provider: AccountProvider,
        calendar: Calendar
    ): List<CalendarEvent> {
        // If we already have them cached, return immediately.
        val key = year to month
        monthlyEventsCache[key]?.let {
            println("CalendarDataManager: Month $year-$month is already in cache. Returning cached data.")
            return it
        }

        // Not in cache, so we read from the provider's API
        println("CalendarDataManager: Loading events for $year-$month from provider for calendar: ${calendar.name}")
        val startOfMonth = LocalDate(year, month, 1).atStartOfDayIn(TimeZone.currentSystemDefault())
        val endOfMonth = LocalDate(year, month, daysInMonth(year, month)).plus(1, DateTimeUnit.DAY)
            .atStartOfDayIn(TimeZone.currentSystemDefault())
        val dateRange = DateRange(
            start = startOfMonth,
            end = endOfMonth
        )

        // Provider call
        val result: Result<List<CalendarEvent>> = provider.readEvents(account, calendar, dateRange)
        if (result.isFailure) {
            println("CalendarDataManager: Error loading events for $year-$month: ${result.exceptionOrNull()}")
            return emptyList()
        }
        val loadedEvents = result.getOrElse { emptyList() }
        println("CalendarDataManager: Loaded ${loadedEvents.size} events for $year-$month")

        // Store in cache
        monthlyEventsCache[key] = loadedEvents
        return loadedEvents
    }

    /**
     * Loads the specified [year, month], plus [year,month-1], plus [year,month+1].
     */
    suspend fun loadSurroundingMonths(
        year: Int,
        month: Int,
        account: ProviderAccount,
        provider: AccountProvider,
        calendar: Calendar
    ) {
        println("CalendarDataManager: loadSurroundingMonths for $year-$month, plus neighbors.")

        // Current
        ensureMonthIsLoaded(year, month, account, provider, calendar)
        // Previous
        val (prevYear, prevMonth) = getPreviousMonth(year, month)
        ensureMonthIsLoaded(prevYear, prevMonth, account, provider, calendar)
        // Next
        val (nextYear, nextMonth) = getNextMonth(year, month)
        ensureMonthIsLoaded(nextYear, nextMonth, account, provider, calendar)
    }

    /**
     * Handy function to clear out the cache if needed.
     */
    fun clearCache() {
        println("CalendarDataManager: Clearing entire monthlyEventsCache.")
        monthlyEventsCache.clear()
    }
}