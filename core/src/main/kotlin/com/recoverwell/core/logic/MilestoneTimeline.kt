package com.recoverwell.core.logic

import com.recoverwell.core.model.Milestone
import com.recoverwell.core.model.Profile
import com.recoverwell.core.protocol.ProtocolRegistry
import java.time.LocalDate

/** Milestone timeline anchored to the injury date vs typical conservative-protocol expectations. */
object MilestoneTimeline {

    enum class Status { REACHED, DUE_NOW, UPCOMING }

    data class Entry(
        val milestone: Milestone,
        val expectedDate: LocalDate,
        val status: Status,
        val weeksFromNow: Long
    )

    fun build(profile: Profile, today: LocalDate): List<Entry> =
        ProtocolRegistry.forProfile(profile).milestones.map { m ->
            val expected = profile.injuryDate.plusWeeks(m.week.toLong())
            val status = when {
                expected.isBefore(today.minusDays(6)) -> Status.REACHED
                !expected.isAfter(today.plusDays(6)) -> Status.DUE_NOW
                else -> Status.UPCOMING
            }
            Entry(m, expected, status, java.time.temporal.ChronoUnit.WEEKS.between(today, expected))
        }
}
