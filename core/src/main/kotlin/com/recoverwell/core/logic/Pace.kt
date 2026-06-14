package com.recoverwell.core.logic

import com.recoverwell.core.model.Milestone
import com.recoverwell.core.model.Profile
import com.recoverwell.core.protocol.ProtocolRegistry
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Personalised pacing: compares the dates the user actually had each phase
 * physio-confirmed against the protocol baseline, and projects the upcoming
 * milestone dates accordingly. Honest and caveated - it never promises a date,
 * only a tracking read against the typical timeline.
 */
object Pace {

    data class Projection(
        /** Positive = ahead of the typical timeline (weeks), negative = behind, 0 = on track. */
        val deltaWeeks: Int,
        val summary: String,
        /** Upcoming milestones with their projected dates (baseline shifted by pace). */
        val projectedMilestones: List<Pair<Milestone, LocalDate>>,
        /** True when there isn't enough confirmation history to judge pace yet. */
        val earlyDays: Boolean
    )

    fun project(profile: Profile, today: LocalDate): Projection {
        val protocol = ProtocolRegistry.forProfile(profile)

        // For each confirmed phase (>1), how many weeks early/late was the
        // confirmation vs that phase's baseline start week?
        val leads = ArrayList<Double>()
        for (phase in protocol.phases.filter { it.number in 2..profile.physioConfirmedPhase }) {
            val confirmedOn = profile.phaseConfirmedDates[phase.number] ?: continue
            val baselineDate = profile.injuryDate.plusWeeks(phase.startWeek.toLong())
            val leadDays = ChronoUnit.DAYS.between(confirmedOn, baselineDate) // +ve = confirmed early
            leads.add(leadDays / 7.0)
        }

        val earlyDays = leads.isEmpty()
        val avgLeadWeeks = if (earlyDays) 0.0 else leads.average()
        val deltaWeeks = Math.round(avgLeadWeeks).toInt()

        val summary = when {
            earlyDays -> "Not enough confirmed phases yet to gauge your pace - this fills in as you progress."
            deltaWeeks >= 1 -> "You're tracking about $deltaWeeks week${plural(deltaWeeks)} ahead of the typical timeline. Keep letting your physio set the pace."
            deltaWeeks <= -1 -> "You're tracking about ${-deltaWeeks} week${plural(-deltaWeeks)} behind the typical timeline - that is common and not a setback. Your physio guides the timing."
            else -> "You're tracking close to the typical timeline."
        }

        // Project upcoming milestones: baseline date shifted earlier by the pace.
        val projected = protocol.milestones
            .map { m ->
                val baseline = profile.injuryDate.plusWeeks(m.week.toLong())
                m to baseline.minusDays(Math.round(avgLeadWeeks * 7).toLong())
            }
            .filter { it.second.isAfter(today) }
            .take(3)

        return Projection(deltaWeeks, summary, projected, earlyDays)
    }

    private fun plural(n: Int) = if (n == 1) "" else "s"
}
