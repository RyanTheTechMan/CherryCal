package com.ryanthetechman.cherrycal.calendar_interaction.provider.apple

import com.ryanthetechman.cherrycal.calendar_interaction.DateRange
import com.ryanthetechman.cherrycal.calendar_interaction.provider.AccountProvider
import com.ryanthetechman.cherrycal.calendar_interaction.Calendar
import com.ryanthetechman.cherrycal.calendar_interaction.CalendarEvent
import com.ryanthetechman.cherrycal.calendar_interaction.provider.ProviderAccount

object AppleAccountProvider : AccountProvider {
    override val providerName: String = "Apple"
    override suspend fun configureAccount(): ProviderAccount? {
        TODO("Not yet implemented")
    }

    override suspend fun writeEvent(account: ProviderAccount, calendar: Calendar, event: CalendarEvent): Result<CalendarEvent> {
        TODO("Not yet implemented")
    }

    override suspend fun readEvents(account: ProviderAccount, calendar: Calendar, dateRange: DateRange): Result<List<CalendarEvent>> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteEvent(account: ProviderAccount, calendar: Calendar, eventId: String): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override suspend fun getCalendars(account: ProviderAccount): Result<List<Calendar>> {
        TODO("Not yet implemented")
    }
}