package com.recoverwell.core.logic

import com.recoverwell.core.model.Appointment
import java.time.LocalDate

/**
 * Pure derivation over the user's appointment list: what's next, what slipped
 * past without being captured, and whether it's time to nudge them to book the
 * next visit. The UI renders these; it never decides them here.
 */
object Appointments {

    data class Outlook(
        /** Soonest still-to-attend appointment (today or later), if any. */
        val next: Appointment?,
        /** Past, not-yet-captured appointments, oldest first - prompt to record them. */
        val overdue: List<Appointment>,
        /** Nothing on the horizon, but care is clearly ongoing - prompt to re-book. */
        val needsRebooking: Boolean
    )

    fun outlook(appointments: List<Appointment>, today: LocalDate): Outlook {
        val pending = appointments.filter { !it.completed }
        val next = pending.filter { !it.date.isBefore(today) }.minByOrNull { it.date }
        val overdue = pending.filter { it.date.isBefore(today) }.sortedBy { it.date }
        // Only nudge to re-book once the user is clearly in active care (has attended
        // at least one visit) and there's nothing ahead to attend or capture - so we
        // never nag a brand-new user, and we don't compete with the "capture it" prompt.
        val needsRebooking = next == null && overdue.isEmpty() && appointments.any { it.completed }
        return Outlook(next, overdue, needsRebooking)
    }
}
