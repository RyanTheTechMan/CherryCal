import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

fun Instant.startOfDay(timeZone: TimeZone = TimeZone.currentSystemDefault()): Instant {
    val localDate = this.toLocalDateTime(timeZone).date
    return localDate.atStartOfDayIn(timeZone)
}

fun Instant.endOfDay(timeZone: TimeZone = TimeZone.currentSystemDefault()): Instant {
    val localDate = this.toLocalDateTime(timeZone).date
    val localDateTime = localDate.atTime(23, 59)
    return localDateTime.toInstant(timeZone)
}