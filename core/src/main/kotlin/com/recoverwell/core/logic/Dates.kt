package com.recoverwell.core.logic

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Patient-facing dates: "Wed 12 Aug" (plus the year when it isn't this year), never "2026-08-12". */
object Dates {
    private val DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.UK)
    private val DAY_YEAR = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.UK)

    fun friendly(date: LocalDate, today: LocalDate = LocalDate.now()): String =
        date.format(if (date.year == today.year) DAY else DAY_YEAR)
}
