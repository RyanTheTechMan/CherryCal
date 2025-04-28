package com.ryanthetechman.cherrycal.calendar_interaction.provider.google

import com.ryanthetechman.cherrycal.calendar_interaction.Calendar
import com.ryanthetechman.cherrycal.calendar_interaction.CalendarEvent
import com.ryanthetechman.cherrycal.calendar_interaction.Contact
import com.ryanthetechman.cherrycal.calendar_interaction.EventStatus
import com.ryanthetechman.cherrycal.calendar_interaction.EventTime
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GoogleCalendarResponse(
    val items: List<GoogleCalendarEvent>
)

@Serializable
data class GoogleCalendarEvent(
    val id: String,
    val summary: String? = null, // Required, but is null when event is cancelled
    val description: String? = null,
    val location: String? = null,
    val status: String? = null,
    val start: GoogleCalendarDateTime? = null, // Required, but is null when event is cancelled
    val end: GoogleCalendarDateTime? = null, // Required, but is null when event is cancelled
    val attendees: List<GoogleCalendarAttendee>? = null,
    val organizer: GoogleCalendarOrganizer? = null,
    val created: Instant? = null, // Required, but is null when event is cancelled
    val sendUpdates: String? = null // "none", "externalOnly", "all" -- Default is "all"
)

@Serializable
data class GoogleCalendarDateTime(
    val dateTime: String? = null, // If it's a date-time event
    val date: String? = null      // If it's an all-day event
)

@Serializable
data class GoogleCalendarAttendee(
    val email: String,
    val displayName: String? = null
)

@Serializable
data class GoogleCalendarOrganizer(
    val email: String,
    val displayName: String? = null
)

@Serializable
data class GoogleCalendarListResponse(
    @SerialName("items") val items: List<GoogleCalendarListEntry>
)

@Serializable
data class GoogleCalendarListEntry(
    @SerialName("id") val id: String, // Calendar ID
    @SerialName("summary") val summary: String, // Calendar name
    @SerialName("description") val description: String? = null, // Calendar description
    @SerialName("timeZone") val timeZone: String, // Time zone of the calendar (e.g., "America/Los_Angeles")
)

fun GoogleCalendarListEntry.asCalendar(): Calendar {
    return Calendar(
        id = id,
        name = summary,
        description = description,
        timeZone = TimeZone.of(timeZone)
    )
}

fun GoogleCalendarEvent.asCalendarEvent(): CalendarEvent {
    return CalendarEvent(
        id = id,
        time = if (start?.date != null) { // Google Calendar uses exclusive end dates for all-day events
            EventTime.AllDay(
                startDate = LocalDate.parse(start.date),
                endDateExclusive = end?.date!!.let { LocalDate.parse(it) }
            )
        } else {
            EventTime.Timed(
                start = Instant.parse(start?.dateTime!!), // Ensure it's not null
                end = end?.dateTime!!.let { Instant.parse(it) }
            )
        },
        title = summary,
        description = description,
        location = location,
        status = when (status) {
            "confirmed" -> EventStatus.CONFIRMED
            "tentative" -> EventStatus.TENTATIVE
            "cancelled" -> EventStatus.CANCELLED
            else -> EventStatus.NONE
        },
        categories = null, // Handle categories later
        recurrence = null, // Handle recurrence later
        exceptionDates = null, // Handle exceptions later
        extraDates = null, // Handle extra dates later
        organizer = organizer?.let {
            Contact(name = it.displayName, email = it.email)
        },
        attendees = attendees?.map {
            Contact(name = it.displayName ?: "", email = it.email)
        },
        createdAt = created,
    )
}

fun CalendarEvent.asGoogleCalendarEvent(): GoogleCalendarEvent {
    val startDateTime: GoogleCalendarDateTime
    val endDateTime: GoogleCalendarDateTime?

    when (this.time) {
        is EventTime.Timed -> {
            startDateTime = GoogleCalendarDateTime(
                dateTime = this.time.start.toString()
            )
            endDateTime = this.time.end.let {
                GoogleCalendarDateTime(
                    dateTime = it.toString()
                )
            }
        }
        is EventTime.AllDay -> {
            startDateTime = GoogleCalendarDateTime(
                date = this.time.startDate.toString() // Use only the date for all-day events
            )
            endDateTime = this.time.endDateExclusive.let {
                GoogleCalendarDateTime(
                    date = it.toString() // Google Calendar expects an exclusive end date
                )
            }
        }

        null -> TODO()
    }

    return GoogleCalendarEvent(
        id = this.id ?: "", // For new events, Google can generate an ID.
        summary = this.title.orEmpty(), // TODO: This is temporary
        description = this.description,
        location = this.location,
        status = when(this.status) {
            EventStatus.CONFIRMED   -> "confirmed"
            EventStatus.TENTATIVE   -> "tentative"
            EventStatus.CANCELLED   -> "cancelled"
            else                    -> "confirmed"
        },
        start = startDateTime,
        end = endDateTime,
        attendees = this.attendees?.map {
            GoogleCalendarAttendee(email = it.email, displayName = it.name)
        },
        organizer = this.organizer?.let {
            GoogleCalendarOrganizer(email = it.email, displayName = it.name)
        },
        created = this.createdAt
    )
}