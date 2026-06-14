package com.recoverwell.core.logic

import com.recoverwell.core.model.*
import com.recoverwell.core.protocol.ProtocolRegistry
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The "bring this to your appointment" pack: turns everything the app already
 * knows into a short list of questions worth raising and a snapshot of current
 * numbers a physio would want. Pure derivation - it suggests, never decides.
 */
object PhysioPrep {

    data class Pack(
        /** Auto-suggested questions/points, most clinically useful first. */
        val discussionPoints: List<String>,
        /** Objective current-state lines to show the physio. */
        val summaryLines: List<String>
    )

    fun build(
        profile: Profile,
        logs: List<DailyLog>,
        events: List<EventLog>,
        meds: List<Medication>,
        tasks: List<RehabTask>,
        selfTests: List<SelfTestResult>,
        rtsSignoffs: Set<String>,
        today: LocalDate
    ): Pack {
        val protocol = ProtocolRegistry.forProfile(profile)
        val points = ArrayList<String>()
        val summary = ArrayList<String>()

        val phase = PhaseEngine.currentPhase(profile, today)
        val week = PhaseEngine.weeksSinceInjury(profile, today)
        summary.add("Week $week · Phase ${phase.number} (${phase.title})")

        // progression gate
        val gate = PhaseEngine.nextPhaseGate(profile, today)
        if (gate.nextPhase != null && gate.readyToConfirm) {
            points.add("Am I ready to progress to phase ${gate.nextPhase!!.number} (${gate.nextPhase!!.title})?")
        }

        // return-to-sport
        val rts = ReturnToSport.progress(profile, selfTests, rtsSignoffs, today, protocol)
        if (rts.available) {
            rts.currentRung?.let { rung ->
                val status = rts.rungs.first { it.rung.id == rung.id }
                if (rung.requiresPhysioSignoff && status.testsMet) {
                    points.add("I've passed the self-tests for \"${rung.title}\" - can you clear me for it?")
                } else {
                    val pending = status.tests.filter { it.state != ReturnToSport.TestState.PASS }
                        .joinToString(", ") { it.test.name }
                    if (pending.isNotBlank()) {
                        points.add("Working toward \"${rung.title}\"; still to pass: $pending. Anything to adjust?")
                    }
                    Unit
                }
            }
            rts.nextLockedByPhase?.let {
                points.add("What do I need to hit before starting \"${it.title}\"?")
            }
        }

        // pace vs the typical timeline
        val pace = Pace.project(profile, today)
        if (!pace.earlyDays && kotlin.math.abs(pace.deltaWeeks) >= 1) {
            val dir = if (pace.deltaWeeks > 0) "ahead of" else "behind"
            points.add("I'm tracking about ${kotlin.math.abs(pace.deltaWeeks)} week(s) $dir the typical timeline - does that match what you see?")
        }

        // caution-tone insights worth raising
        val recentLogs = logs.filter { !it.date.isBefore(today.minusDays(14)) }
        Insights.generate(profile, logs, events, meds, tasks, today)
            .filter { it.tone == Insights.Tone.CAUTION }
            .forEach { points.add("Mention: ${it.title} - ${it.detail}") }

        // standing-out warnings (non-urgent get raised; urgent are handled elsewhere)
        Capability.warnings(profile, recentLogs, today)
            .filter { it.severity == Capability.Severity.WARNING }
            .forEach { points.add("Flag: ${it.title}") }

        // ---- summary numbers ----
        val digest = WeeklyDigest.generate(profile, logs, events, meds, tasks, today)
        summary.add("Medication adherence (7 days): ${digest.adherencePct}%")

        val recentPain = recentLogs.mapNotNull { it.pain }
        if (recentPain.isNotEmpty())
            summary.add("Pain (recent avg): ${"%.1f".format(recentPain.average())}/10")
        recentLogs.mapNotNull { it.swelling }.maxByOrNull { it.score }?.let {
            summary.add("Worst recent swelling: ${it.label}")
        }

        // latest self-test results
        for (test in protocol.selfTests) {
            val latest = ReturnToSport.latestFor(test.id, selfTests) ?: continue
            val days = ChronoUnit.DAYS.between(latest.date, today)
            val age = if (days <= 0) "today" else "${days}d ago"
            summary.add("${test.name}: ${test.valueLabel(latest)} ($age)")
        }

        if (points.isEmpty())
            points.add("No flags from the app this period - is my plan still on track?")

        return Pack(points, summary)
    }
}
