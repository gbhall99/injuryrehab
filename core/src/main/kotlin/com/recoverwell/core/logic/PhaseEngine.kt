package com.recoverwell.core.logic

import com.recoverwell.core.model.PhaseSpec
import com.recoverwell.core.model.Profile
import com.recoverwell.core.protocol.ProtocolRegistry
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Phase progression with a two-key gate: a phase only becomes the active one
 * when (a) its (editable) start date has been reached AND (b) the user has
 * recorded that their physio confirmed the progression. Date eligibility on
 * its own never advances the phase.
 */
object PhaseEngine {

    data class Gate(
        val nextPhase: PhaseSpec?,
        val dateEligible: Boolean,
        val physioConfirmed: Boolean,
        val startDate: LocalDate?,
        val daysUntilEligible: Long
    ) {
        val readyToConfirm: Boolean get() = nextPhase != null && dateEligible && !physioConfirmed
    }

    fun phaseStartDate(profile: Profile, phase: PhaseSpec): LocalDate =
        profile.phaseStartOverrides[phase.number]
            ?: profile.injuryDate.plusWeeks(phase.startWeek.toLong())

    /** Highest phase whose start date has been reached. */
    fun dateEligiblePhase(profile: Profile, today: LocalDate): Int =
        ProtocolRegistry.forProfile(profile).phases
            .filter { !phaseStartDate(profile, it).isAfter(today) }
            .maxOfOrNull { it.number } ?: 1

    /** The phase the app treats as active: date-eligible, capped by physio confirmation. */
    fun currentPhase(profile: Profile, today: LocalDate): PhaseSpec {
        val protocol = ProtocolRegistry.forProfile(profile)
        val byDate = dateEligiblePhase(profile, today)
        val active = minOf(byDate, profile.physioConfirmedPhase.coerceAtLeast(1))
        return protocol.phase(active.coerceIn(1, protocol.phases.size))
    }

    fun nextPhaseGate(profile: Profile, today: LocalDate): Gate {
        val current = currentPhase(profile, today)
        val next = ProtocolRegistry.forProfile(profile).phases.firstOrNull { it.number == current.number + 1 }
            ?: return Gate(null, dateEligible = false, physioConfirmed = false, startDate = null, daysUntilEligible = 0)
        val start = phaseStartDate(profile, next)
        val eligible = !start.isAfter(today)
        val confirmed = profile.physioConfirmedPhase >= next.number
        val days = if (eligible) 0 else ChronoUnit.DAYS.between(today, start)
        return Gate(next, eligible, confirmed, start, days)
    }

    fun weeksSinceInjury(profile: Profile, today: LocalDate): Long =
        ChronoUnit.WEEKS.between(profile.injuryDate, today)

    fun daysSinceInjury(profile: Profile, today: LocalDate): Long =
        ChronoUnit.DAYS.between(profile.injuryDate, today)
}
