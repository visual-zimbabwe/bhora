package com.bhora.app.domain.logic

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object FixtureTimeFormatter {
    fun formatDateHeader(
        date: LocalDate,
        locale: Locale = Locale.getDefault()
    ): String {
        val formatter = DateTimeFormatter.ofPattern("EEEE d MMMM", locale)
        return date.format(formatter).uppercase(locale)
    }

    fun formatKickoffTime(
        kickoffUtcMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault()
    ): String {
        val zoned = Instant.ofEpochMilli(kickoffUtcMillis).atZone(zoneId)
        val formatter = DateTimeFormatter.ofPattern("HH:mm", locale)
        return "${zoned.format(formatter)} LOCAL"
    }
}
