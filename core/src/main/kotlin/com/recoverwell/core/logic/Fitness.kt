package com.recoverwell.core.logic

import com.recoverwell.core.model.EventLog
import com.recoverwell.core.model.EventStatus
import com.recoverwell.core.model.EventType
import com.recoverwell.core.model.Profile
import com.recoverwell.core.protocol.FitnessActivity
import com.recoverwell.core.protocol.ProtocolRegistry
import java.time.LocalDate

/**
 * "Stay fit": general conditioning the user can do during recovery, gated by
 * what's appropriate for their current phase, plus a simple weekly session goal.
 * Conditioning sessions are logged as exercise events with a dedicated ref so
 * they never inflate the rehab-exercise counts.
 */
object Fitness {

    const val SESSION_REF = "fitness_session"
    const val DEFAULT_WEEKLY_GOAL = 3

    /** Activities appropriate to do now (current phase >= their minPhase). */
    fun available(profile: Profile, today: LocalDate): List<FitnessActivity> {
        val phase = PhaseEngine.currentPhase(profile, today).number
        return ProtocolRegistry.forProfile(profile).fitness.filter { phase >= it.minPhase }
    }

    fun all(profile: Profile): List<FitnessActivity> =
        ProtocolRegistry.forProfile(profile).fitness

    /** Conditioning sessions logged in the trailing 7 days. */
    fun sessionsThisWeek(events: List<EventLog>, today: LocalDate): Int =
        events.count {
            it.type == EventType.EXERCISE && it.refId == SESSION_REF && it.status == EventStatus.DONE &&
                !it.date.isBefore(today.minusDays(6)) && !it.date.isAfter(today)
        }
}
