package com.ryanthetechman.cherrycal.calendar_interaction.provider

import com.ryanthetechman.cherrycal.calendar_interaction.Calendar
import com.ryanthetechman.cherrycal.calendar_interaction.CalendarEvent
import com.ryanthetechman.cherrycal.calendar_interaction.DateRange
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

interface AccountProvider {
    val providerName: String

    /**
     * Initializes a new account for this provider.
     */
    suspend fun configureAccount(): ProviderAccount?

    /**
     * Creates or Updates an event on a specific calendar.
     * Returns the event ID or the updated event.
     */
    suspend fun writeEvent(account: ProviderAccount, calendar: Calendar, event: CalendarEvent): Result<CalendarEvent>

    /**
     * Reads events from a specific calendar.
     */
    suspend fun readEvents(account: ProviderAccount, calendar: Calendar, dateRange: DateRange): Result<List<CalendarEvent>>

    /**
     * Deletes an event from a specific calendar.
     */
    suspend fun deleteEvent(account: ProviderAccount, calendar: Calendar, eventId: String): Result<Boolean>

    /**
     * Gets all calendars available for the provider.
     */
    suspend fun getCalendars(account: ProviderAccount): Result<List<Calendar>>
}

object AccountProviderRefSerializer : KSerializer<AccountProvider> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AccountProvider", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: AccountProvider) = encoder.encodeString(value.providerName)

    override fun deserialize(decoder: Decoder): AccountProvider {
        val providerName = decoder.decodeString()
        return ProviderRegistry.all().find { it.providerName == providerName } ?: throw SerializationException("Unknown provider: $providerName")
    }
}