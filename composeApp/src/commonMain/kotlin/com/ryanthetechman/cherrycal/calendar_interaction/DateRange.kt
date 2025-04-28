package com.ryanthetechman.cherrycal.calendar_interaction

import kotlinx.datetime.Instant

data class DateRange(
    val start: Instant,
    val end: Instant
) {
    /**
     * Checks if a given Instant falls within the range (inclusive).
     * Note: It compares the underlying Instants.
     */
    fun contains(instant: Instant): Boolean {
        return instant in start..end
    }
}