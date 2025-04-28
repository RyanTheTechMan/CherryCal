package com.ryanthetechman.cherrycal.calendar_interaction

import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable

@Serializable
data class Calendar(
    val id: String, // Calendar ID
    val name: String, // Calendar name
    val description: String?, // Calendar description
    val timeZone: TimeZone, // Time zone of the calendar (e.g., "America/Los_Angeles")
)