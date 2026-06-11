package com.recoverwell.core.logic

import com.recoverwell.core.model.DailyLog
import com.recoverwell.core.model.Profile
import com.recoverwell.core.model.Swelling
import com.recoverwell.core.protocol.ProtocolContent
import java.time.LocalDate

/**
 * Digital-twin logic: a snapshot of what the leg can currently do, plus
 * off-plan risk warnings derived from the profile and recent logs.
 */
object Capability {

    data class Snapshot(
        val phaseNumber: Int,
        val phaseTitle: String,
        val weeksSinceInjury: Long,
        val bootStatus: String,
        val wedges: Int,
        val expectedWedges: Int,
        val weightBearing: String,
        val tendonState: String,
        val allowed: List<String>,
        val notAllowed: List<String>
    )

    enum class Severity { URGENT, WARNING, INFO }

    data class Warning(
        val severity: Severity,
        val title: String,
        val detail: String
    )

    fun snapshot(profile: Profile, today: LocalDate): Snapshot {
        val phase = PhaseEngine.currentPhase(profile, today)
        val weeks = PhaseEngine.weeksSinceInjury(profile, today)
        val expected = profile.wedgePlan.expectedWedges(profile.injuryDate, today)
        val boot = when {
            phase.number <= 2 && profile.currentWedges > 0 ->
                "Boot on at all times · ${profile.currentWedges} wedge(s), heel raised"
            phase.number <= 2 -> "Boot on at all times · neutral (no wedges)"
            phase.number == 3 -> "Weaning out of boot, physio-guided"
            else -> "Out of boot · supportive shoes"
        }
        val tendon = when (phase.number) {
            1 -> "Tendon ends knitting together - maximum protection"
            2 -> "Early healing tissue forming - protected loading helps it organise"
            3 -> "Tendon consolidating - gentle movement, no stretch"
            4 -> "Tendon remodelling - progressive load makes it stronger"
            else -> "Tendon maturing - building sport-level capacity"
        }
        return Snapshot(
            phaseNumber = phase.number,
            phaseTitle = phase.title,
            weeksSinceInjury = weeks,
            bootStatus = boot,
            wedges = profile.currentWedges,
            expectedWedges = expected,
            weightBearing = profile.weightBearing.label,
            tendonState = tendon,
            allowed = phase.allowed,
            notAllowed = phase.notAllowed
        )
    }

    /**
     * Off-plan and symptom-based warnings. DVT-suggestive symptom patterns are
     * surfaced as URGENT because the user is anticoagulated post-rupture.
     */
    fun warnings(profile: Profile, recentLogs: List<DailyLog>, today: LocalDate): List<Warning> {
        val out = ArrayList<Warning>()
        val phase = PhaseEngine.currentPhase(profile, today)
        val expected = profile.wedgePlan.expectedWedges(profile.injuryDate, today)
        val latest = recentLogs.maxByOrNull { it.date }

        if (latest?.swelling == Swelling.SEVERE) {
            out.add(
                Warning(
                    Severity.URGENT,
                    "Severe swelling logged",
                    "Severe swelling can be a DVT sign, especially with calf pain, heat or redness. " +
                        "Open Red Flags and get same-day advice if any of those are present."
                )
            )
        }
        if (latest?.pain != null && latest.pain >= 8) {
            out.add(
                Warning(
                    Severity.URGENT,
                    "Very high pain logged (${latest.pain}/10)",
                    "Pain this high is not expected on the conservative pathway. If it came on suddenly, " +
                        "check the re-rupture red flags; otherwise contact your clinic today."
                )
            )
        }

        if (profile.currentWedges < expected) {
            out.add(
                Warning(
                    Severity.WARNING,
                    "Wedges ahead of plan",
                    "Boot has ${profile.currentWedges} wedge(s) but the plan expects $expected today. " +
                        "Reducing heel raise early increases re-rupture risk - check with your physio."
                )
            )
        }
        if (profile.currentWedges > expected) {
            out.add(
                Warning(
                    Severity.INFO,
                    "Wedges behind plan",
                    "Boot has ${profile.currentWedges} wedge(s); the plan expects $expected. " +
                        "Fine if your clinic chose this - otherwise a wedge change may be overdue."
                )
            )
        }

        if (phase.number <= 2 && latest?.bootWornAsPlanned == false) {
            out.add(
                Warning(
                    Severity.WARNING,
                    "Boot not worn as planned",
                    "In phase ${phase.number} the tendon has little of its own strength - even one " +
                        "unbooted step risks re-rupture. Keep the boot on and tell your physio if it is a comfort problem."
                )
            )
        }

        val dateEligible = PhaseEngine.dateEligiblePhase(profile, today)
        if (dateEligible > profile.physioConfirmedPhase) {
            out.add(
                Warning(
                    Severity.INFO,
                    "Next phase awaiting physio confirmation",
                    "The typical timeline says phase $dateEligible could start, but progress only " +
                        "after your physio confirms it. Record their confirmation in the Today tab."
                )
            )
        }

        val sustainedHighPain = recentLogs.filter { !it.date.isBefore(today.minusDays(6)) }
            .mapNotNull { it.pain }.let { it.size >= 3 && it.all { p -> p >= 6 } }
        if (sustainedHighPain) {
            out.add(
                Warning(
                    Severity.WARNING,
                    "Pain staying high this week",
                    "Several recent days at 6/10 or more. Persistent pain deserves a physio review " +
                        "rather than pushing on."
                )
            )
        }

        return out
    }

    /** Phase-appropriate movement checks used by the digital twin screen. */
    data class MovementCheck(val movement: String, val allowed: Boolean, val note: String)

    fun movementChecks(profile: Profile, today: LocalDate): List<MovementCheck> {
        val n = PhaseEngine.currentPhase(profile, today).number
        return listOf(
            MovementCheck(
                "Walk without the boot", n >= 3,
                if (n >= 3) "Physio-guided weaning only" else "Not before ~week 8-10, physio-confirmed"
            ),
            MovementCheck(
                "Pull foot up past neutral / calf stretch", n >= 4,
                if (n >= 4) "Gentle and physio-guided only" else "Not before week 12 - tendon over-lengthening risk"
            ),
            MovementCheck(
                "Drive a car", n >= 4,
                "Generally only out of the boot, off strong painkillers, and able to emergency-brake - confirm with clinic and insurer"
            ),
            MovementCheck(
                "Standing heel raises", n >= 4,
                if (n >= 4) "Progress as prescribed" else "Phase 4 work - too early now"
            ),
            MovementCheck(
                "Run / jump", n >= 5,
                if (n >= 5) "Graded programme once physio clears it" else "Phase 5 work after strength benchmarks"
            ),
            MovementCheck(
                "Play padel", n >= 5,
                if (n >= 5) "Drills first; competitive play typically 9-12 months with sign-off"
                else "The end goal - but not yet"
            )
        )
    }
}
