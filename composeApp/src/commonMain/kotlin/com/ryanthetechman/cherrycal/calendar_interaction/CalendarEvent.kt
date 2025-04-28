package com.ryanthetechman.cherrycal.calendar_interaction

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.Duration


/**
 * Follows the iCal standard for calendar events.
 * https://icalendar.org/iCalendar-RFC-5545/3-8-5-3-calendar-object.html
 */
@Serializable
data class CalendarEvent(
    val id: String? = null, // UID: Unique identifier
    val createdAt: Instant? = null, // DTSTAMP: When the event was created
    val time: EventTime? = null, // DTSTART & DTEND: Start & end date/time
    val duration: Duration? = null, // DURATION: Duration of the event (alternative to endTime)
    val title: String? = null, // SUMMARY: Event title
    val description: String? = null, // DESCRIPTION: Optional event description
    val location: String? = null, // LOCATION: Event location
    val status: EventStatus? = null, // STATUS: Event status (Confirmed, Tentative, etc.)
    val categories: List<String>? = null, // CATEGORIES: List of categories (e.g., "Meeting", "Birthday")
    val recurrence: RecurrenceRule? = null, // RRULE: Recurring event rules
    val exceptionDates: List<Instant>? = null, // EXDATE: Dates to exclude from recurrence
    val extraDates: List<Instant>? = null, // RDATE: Additional specific occurrences
    val organizer: Contact? = null, // ORGANIZER: Organizer of the event
    val attendees: List<Contact>? = null, // ATTENDEE: List of attendees
    val reminder: Alarm? = null // VALARM: Notification reminder
)

/**
 * Represents either a timed event or an all-day event.
 */
@Serializable
sealed class EventTime {
    @Serializable
    data class Timed(val start: Instant, val end: Instant) : EventTime()

    @Serializable
    data class AllDay(val startDate: LocalDate, val endDateExclusive: LocalDate) : EventTime()
}

/**
 * An event's status.
 */
@Serializable
enum class EventStatus {
    CONFIRMED, TENTATIVE, CANCELLED, NONE
}

/**
 * A contact (used for organizers and attendees).
 */
@Serializable
data class Contact(
    val name: String?,
    val email: String
)

/**
 * Recurrence rules for an event.
 */
@Serializable
data class RecurrenceRule(
    val frequency: Frequency, // FREQ: Daily, Weekly, Monthly, etc.
    val interval: Int, // INTERVAL: How often the event repeats
    val daysOfWeek: List<DayOfWeek>?, // BYDAY: Specific days (e.g., "Monday, Friday")
    val endDate: Instant? // UNTIL: When the recurrence ends
)

/**
 * Recurrence frequency.
 */
@Serializable
enum class Frequency {
    DAILY, WEEKLY, MONTHLY, YEARLY
}

/**
 * An alarm/notification for an event.
 */
@Serializable
data class Alarm(
    val triggerBefore: Duration, // TRIGGER: Time before event when alarm goes off
    val message: String // DESCRIPTION: Message displayed for the reminder
)