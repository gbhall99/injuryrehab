package com.recoverwell.core.logic

import com.recoverwell.core.model.*
import com.recoverwell.core.protocol.ProtocolRegistry
import java.time.LocalDate

/**
 * A once-a-week plain-language summary of the last 7 days, built entirely from
 * on-device data. Advisory, never prescriptive.
 */
object WeeklyDigest {

    enum class Trend { DOWN, UP, STEADY, NONE }

    data class Digest(
        val adherencePct: Int,
        val painTrend: Trend,
        val painDetail: String,
        val exercisesDone: Int,
        val milestonesThisWeek: List<String>,
        val focus: String
    )

    fun generate(
        profile: Profile,
        logs: List<DailyLog>,
        events: List<EventLog>,
        meds: List<Medication>,
        tasks: List<RehabTask>,
        today: LocalDate
    ): Digest {
        val weekAgo = today.minusDays(6)

        // adherence
        val scheduledPerDay = meds.filter { it.active }.sumOf { it.times.size }
        val total = scheduledPerDay * 7
        val taken = events.count {
            it.type == EventType.MEDICATION && it.status == EventStatus.TAKEN &&
                !it.date.isBefore(weekAgo) && !it.date.isAfter(today)
        }
        val adherence = if (total <= 0) 0 else (taken * 100) / total

        // pain trend
        val recent = logs.filter { !it.date.isBefore(weekAgo) }.mapNotNull { it.pain }
        val prior = logs.filter { !it.date.isBefore(today.minusDays(13)) && it.date.isBefore(weekAgo) }
            .mapNotNull { it.pain }
        val (trend, painDetail) = when {
            recent.isEmpty() -> Trend.NONE to "No pain entries this week."
            prior.size < 2 -> Trend.STEADY to "Average pain ${avg(recent)}/10 this week."
            else -> {
                val d = recent.average() - prior.average()
                when {
                    d <= -0.8 -> Trend.DOWN to "Pain down to ${avg(recent)}/10 (was ${avg(prior)}/10)."
                    d >= 0.8 -> Trend.UP to "Pain up to ${avg(recent)}/10 (was ${avg(prior)}/10)."
                    else -> Trend.STEADY to "Pain steady around ${avg(recent)}/10."
                }
            }
        }

        // exercises completed
        val exercisesDone = events.count {
            it.type == EventType.EXERCISE && it.status == EventStatus.DONE &&
                !it.date.isBefore(weekAgo) && !it.date.isAfter(today) && it.refId != "session_reminder"
        }

        // milestones whose typical date fell this week
        val protocol = ProtocolRegistry.forProfile(profile)
        val milestones = protocol.milestones.filter {
            val d = profile.injuryDate.plusWeeks(it.week.toLong())
            !d.isBefore(weekAgo) && !d.isAfter(today)
        }.map { it.title }

        // one focus for next week
        val gate = PhaseEngine.nextPhaseGate(profile, today)
        val phase = PhaseEngine.currentPhase(profile, today)
        val focus = when {
            adherence in 1..79 -> "Tighten up your medication routine - consistency protects against clots."
            trend == Trend.UP -> "Ease off a little; if pain keeps rising, tell your physio."
            gate.readyToConfirm -> "Ask your physio whether you're ready for phase ${gate.nextPhase!!.number}."
            else -> phase.goals.firstOrNull() ?: "Keep following your plan."
        }

        return Digest(adherence, trend, painDetail, exercisesDone, milestones, focus)
    }

    private fun avg(v: List<Int>): String {
        val a = v.average()
        return if (a == Math.floor(a)) a.toInt().toString() else "%.1f".format(a)
    }
}
