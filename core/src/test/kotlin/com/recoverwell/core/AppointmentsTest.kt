package com.recoverwell.core

import com.recoverwell.core.export.AppState
import com.recoverwell.core.export.BackupCodec
import com.recoverwell.core.logic.Appointments
import com.recoverwell.core.model.Appointment
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AppointmentsTest {

    private val today = LocalDate.of(2026, 6, 14)

    @Test
    fun newUserWithNothingBookedIsNotNagged() {
        val o = Appointments.outlook(emptyList(), today)
        assertNull(o.next)
        assertTrue(o.overdue.isEmpty())
        assertFalse("brand-new users shouldn't be told to re-book", o.needsRebooking)
    }

    @Test
    fun addingAnAppointmentSurfacesItAsNext() {
        val appts = listOf(
            Appointment(today.plusWeeks(2), "Physio review", completed = false, id = "a"),
            Appointment(today.plusWeeks(5), "Consultant", completed = false, id = "b")
        )
        val o = Appointments.outlook(appts, today)
        assertEquals("a", o.next?.id) // soonest upcoming wins
        assertTrue(o.overdue.isEmpty())
        assertFalse(o.needsRebooking)
    }

    @Test
    fun todayCountsAsUpcomingNotOverdue() {
        val appts = listOf(Appointment(today, "Physio review", completed = false, id = "a"))
        val o = Appointments.outlook(appts, today)
        assertEquals("a", o.next?.id)
        assertTrue(o.overdue.isEmpty())
    }

    @Test
    fun pastUncapturedAppointmentIsOverdueNotRebooking() {
        val appts = listOf(Appointment(today.minusDays(3), "Physio review", completed = false, id = "a"))
        val o = Appointments.outlook(appts, today)
        assertNull(o.next)
        assertEquals(listOf("a"), o.overdue.map { it.id })
        assertFalse("capture comes before re-book", o.needsRebooking)
    }

    @Test
    fun overdueListIsOldestFirst() {
        val appts = listOf(
            Appointment(today.minusDays(1), "recent", completed = false, id = "new"),
            Appointment(today.minusDays(9), "older", completed = false, id = "old")
        )
        val o = Appointments.outlook(appts, today)
        assertEquals(listOf("old", "new"), o.overdue.map { it.id })
    }

    @Test
    fun completingTheLastAppointmentTriggersRebooking() {
        // a single past appointment, now captured (completed) -> nothing ahead -> re-book
        val appts = listOf(Appointment(today.minusDays(3), "Physio review", completed = true, id = "a"))
        val o = Appointments.outlook(appts, today)
        assertNull(o.next)
        assertTrue(o.overdue.isEmpty())
        assertTrue(o.needsRebooking)
    }

    @Test
    fun bookingAgainClearsTheRebookingNudge() {
        val appts = listOf(
            Appointment(today.minusDays(3), "Past visit", completed = true, id = "done"),
            Appointment(today.plusWeeks(2), "Next visit", completed = false, id = "next")
        )
        val o = Appointments.outlook(appts, today)
        assertEquals("next", o.next?.id)
        assertFalse(o.needsRebooking)
    }

    @Test
    fun appointmentIdAndWithSurviveBackupRoundTrip() {
        val profile = Fixtures.profile().copy(
            appointments = listOf(Appointment(today.plusWeeks(1), "Physio review",
                completed = false, id = "stable-id", withWhom = "Mr Patel"))
        )
        val state = AppState(
            profile = profile,
            medications = emptyList(),
            tasks = emptyList(),
            exerciseOverrides = emptyMap(),
            dailyLogs = emptyList(),
            events = emptyList()
        )
        val decoded = BackupCodec.decode(BackupCodec.encode(state))
        val appt = decoded.profile.appointments.single()
        assertEquals("stable-id", appt.id)
        assertEquals("Mr Patel", appt.withWhom)
    }
}
