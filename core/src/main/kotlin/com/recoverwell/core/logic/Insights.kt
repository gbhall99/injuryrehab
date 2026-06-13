package com.recoverwell.core.logic

import com.recoverwell.core.model.*
import java.time.LocalDate
import kotlin.math.abs

/**
 * On-device "smart" layer: turns the user's own logs and event history into
 * plain-language observations. Everything is computed locally from data the
 * app already holds, framed as gentle, physio-advisory guidance - never a
 * diagnosis. Pure functions, fully testable.
 */
object Insights {

    enum class Tone { POSITIVE, NEUTRAL, CAUTION }

    data class Insight(val tone: Tone, val title: String, val detail: String)

    /** All insights, most actionable first. */
    fun generate(
        profile: Profile,
        logs: List<DailyLog>,
        events: List<EventLog>,
        meds: List<Medication>,
        tasks: List<RehabTask>,
        today: LocalDate
    ): List<Insight> {
        val out = ArrayList<Insight>()

        // ---- pain trend: last 7 days vs the 7 before ----
        trend(logs, today, { it.pain?.toDouble() })?.let { (recent, prior) ->
            val delta = recent - prior
            when {
                delta <= -1.0 -> out.add(Insight(Tone.POSITIVE, "Pain is easing",
                    "Your average pain is down ${fmt(-delta)} points vs the previous week " +
                        "(${fmt(prior)} → ${fmt(recent)}). A good sign the tendon is settling."))
                delta >= 1.0 -> out.add(Insight(Tone.CAUTION, "Pain is creeping up",
                    "Your average pain is up ${fmt(delta)} points vs the previous week " +
                        "(${fmt(prior)} → ${fmt(recent)}). If it keeps climbing, mention it to your physio."))
                else -> {}
            }
        }

        // ---- swelling trend ----
        trend(logs, today, { it.swelling?.score?.toDouble() })?.let { (recent, prior) ->
            val delta = recent - prior
            when {
                delta >= 0.7 -> out.add(Insight(Tone.CAUTION, "Swelling is up this week",
                    "Average swelling has risen vs last week. Keep elevating, and watch the DVT red flags."))
                delta <= -0.7 -> out.add(Insight(Tone.POSITIVE, "Swelling is settling",
                    "Average swelling is lower than last week - elevation and circulation work are paying off."))
                else -> {}
            }
        }

        // ---- habit -> outcome: elevation days vs swelling ----
        elevationVsSwelling(tasks, events, logs, today)?.let { out.add(it) }

        // ---- medication adherence (last 7 days) ----
        adherenceInsight(meds, events, today)?.let { out.add(it) }

        // ---- readiness for the next phase ----
        readiness(profile, logs, events, meds, today)?.let { out.add(it) }

        return out
    }

    /** (recentAvg, priorAvg) over the two trailing 7-day windows, or null if sparse. */
    private fun trend(logs: List<DailyLog>, today: LocalDate, value: (DailyLog) -> Double?): Pair<Double, Double>? {
        val recent = logs.filter { !it.date.isBefore(today.minusDays(6)) && !it.date.isAfter(today) }
            .mapNotNull(value)
        val prior = logs.filter { !it.date.isBefore(today.minusDays(13)) && it.date.isBefore(today.minusDays(6)) }
            .mapNotNull(value)
        if (recent.size < 3 || prior.size < 3) return null
        return recent.average() to prior.average()
    }

    private fun elevationVsSwelling(
        tasks: List<RehabTask>, events: List<EventLog>, logs: List<DailyLog>, today: LocalDate
    ): Insight? {
        val elevation = tasks.firstOrNull { it.kind == TaskKind.ELEVATION } ?: return null
        val window = logs.filter { it.swelling != null && !it.date.isBefore(today.minusDays(20)) }
        if (window.size < 6) return null
        val doneDates = events.filter { it.type == EventType.TASK && it.refId == elevation.id && it.status == EventStatus.DONE }
            .map { it.date }.toSet()
        val withElev = window.filter { it.date in doneDates }.mapNotNull { it.swelling?.score }
        val without = window.filter { it.date !in doneDates }.mapNotNull { it.swelling?.score }
        if (withElev.size < 3 || without.size < 3) return null
        val diff = without.average() - withElev.average()
        if (diff < 0.7) return null
        return Insight(Tone.POSITIVE, "Elevation seems to help your swelling",
            "On days you logged elevation as done, your swelling tended to be lower " +
                "(${fmt(withElev.average())} vs ${fmt(without.average())} on days you didn't). " +
                "Worth keeping up.")
    }

    private fun adherenceInsight(meds: List<Medication>, events: List<EventLog>, today: LocalDate): Insight? {
        val active = meds.filter { it.active }
        val scheduledPerDay = active.sumOf { it.times.size }
        if (scheduledPerDay == 0) return null
        val days = 7
        val total = scheduledPerDay * days
        val taken = events.count {
            it.type == EventType.MEDICATION && it.status == EventStatus.TAKEN &&
                !it.date.isBefore(today.minusDays((days - 1).toLong())) && !it.date.isAfter(today)
        }
        val pct = (taken * 100) / total
        return when {
            pct >= 90 -> Insight(Tone.POSITIVE, "Strong medication adherence",
                "You've taken about $pct% of your doses this week. Clot prevention works best taken consistently - nicely done.")
            pct in 1..59 -> Insight(Tone.CAUTION, "Medication adherence is slipping",
                "Only about $pct% of this week's doses are logged as taken. Consistency matters for clot prevention - consider adjusting your reminders.")
            else -> null
        }
    }

    private fun readiness(
        profile: Profile, logs: List<DailyLog>, events: List<EventLog>,
        meds: List<Medication>, today: LocalDate
    ): Insight? {
        val gate = PhaseEngine.nextPhaseGate(profile, today)
        if (gate.nextPhase == null || !gate.readyToConfirm) return null
        val recentPain = logs.filter { !it.date.isBefore(today.minusDays(4)) }.mapNotNull { it.pain }
        val painOk = recentPain.size >= 2 && recentPain.average() <= 3.0
        val streak = ScheduleEngine.medicationStreak(meds, events, today)
        if (!painOk) return null
        return Insight(Tone.POSITIVE, "You may be ready for the next phase",
            "Phase ${gate.nextPhase.number} is due by date, your recent pain is low" +
                (if (streak >= 3) ", and you're on a $streak-day medication streak" else "") +
                ". Worth asking your physio whether you can progress - only they can confirm it.")
    }

    fun minuteLabel(m: Int): String = "%02d:%02d".format(m / 60, m % 60)
    private fun fmt(v: Double): String = if (v == Math.floor(v)) v.toInt().toString() else "%.1f".format(v)
}
