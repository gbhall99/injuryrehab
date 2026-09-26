package com.recoverwell.core.logic

import com.recoverwell.core.model.Milestone
import com.recoverwell.core.model.Profile
import com.recoverwell.core.protocol.ProtocolRegistry
import java.time.LocalDate

/**
 * Milestone timeline anchored to the injury date vs typical conservative-protocol
 * expectations. A milestone only counts as REACHED once its typical date has
 * passed AND the user has actually reached its phase (physio-confirmed); a
 * passed date without the phase reads as DUE_NOW - "around now, waiting on
 * your physio" - never as an achievement the user hasn't had.
 */
object MilestoneTimeline {

    enum class Status { REACHED, DUE_NOW, UPCOMING }

    data class Entry(
        val milestone: Milestone,
        val expectedDate: LocalDate,
        val status: Status,
        val weeksFromNow: Long
    )

    fun build(profile: Profile, today: LocalDate): List<Entry> {
        val phase = PhaseEngine.currentPhase(profile, today).number
        return ProtocolRegistry.forProfile(profile).milestones.map { m ->
            val expected = profile.injuryDate.plusWeeks(m.week.toLong())
            val status = when {
                expected.isBefore(today.minusDays(6)) && phase >= m.phase -> Status.REACHED
                !expected.isAfter(today.plusDays(6)) -> Status.DUE_NOW
                else -> Status.UPCOMING
            }
            Entry(m, expected, status, java.time.temporal.ChronoUnit.WEEKS.between(today, expected))
        }
    }
}
