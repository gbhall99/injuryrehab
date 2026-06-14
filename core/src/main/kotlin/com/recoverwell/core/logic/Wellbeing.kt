package com.recoverwell.core.logic

import com.recoverwell.core.model.Milestone
import com.recoverwell.core.model.Profile
import com.recoverwell.core.protocol.PhaseMindset
import com.recoverwell.core.protocol.ProtocolRegistry
import com.recoverwell.core.protocol.WeekExpectation
import java.time.LocalDate

/**
 * The emotional side of recovery: what's normal to feel in the current phase,
 * and gentle celebration when a milestone is reached. Pure derivation over data
 * the app already holds.
 */
object Wellbeing {

    /** "What's normal to feel" for the phase the user is actually in. */
    fun currentMindset(profile: Profile, today: LocalDate): PhaseMindset? {
        val phase = PhaseEngine.currentPhase(profile, today).number
        return ProtocolRegistry.forProfile(profile).mindset.firstOrNull { it.phase == phase }
    }

    /** The "what to expect" band covering the current week since injury. */
    fun expectationFor(profile: Profile, today: LocalDate): WeekExpectation? {
        val week = PhaseEngine.weeksSinceInjury(profile, today).toInt()
        return ProtocolRegistry.forProfile(profile).expectations
            .firstOrNull { week >= it.weekFrom && week < it.weekTo }
    }

    /**
     * A milestone whose typical date fell within the last [withinDays] days -
     * the moment worth celebrating. Most recent one wins.
     */
    fun recentlyReachedMilestone(profile: Profile, today: LocalDate, withinDays: Long = 7): Milestone? =
        ProtocolRegistry.forProfile(profile).milestones
            .map { it to profile.injuryDate.plusWeeks(it.week.toLong()) }
            .filter { (_, date) -> !date.isAfter(today) && date.isAfter(today.minusDays(withinDays + 1)) }
            .maxByOrNull { (_, date) -> date }
            ?.first
}
