package com.recoverwell.core.logic

import com.recoverwell.core.model.DailyLog
import com.recoverwell.core.model.Profile
import com.recoverwell.core.model.Swelling
import com.recoverwell.core.protocol.ProtocolRegistry
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
        val protocol = ProtocolRegistry.forProfile(profile)
        val phase = PhaseEngine.currentPhase(profile, today)
        val weeks = PhaseEngine.weeksSinceInjury(profile, today)
        val expected = profile.wedgePlan.expectedWedges(profile.injuryDate, today)
        val boot = phase.deviceUsage?.replace("{n}", profile.currentWedges.toString())
            ?: protocol.supportDevice?.let { "No ${it.name.lowercase()} needed in this phase" }
            ?: "No support device for this protocol"
        val tendon = phase.tissueState
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

        val device = ProtocolRegistry.forProfile(profile).supportDevice
        if (device != null && profile.currentWedges < expected) {
            out.add(
                Warning(
                    Severity.WARNING,
                    "${device.name} ahead of plan",
                    "Your ${device.name.lowercase()} is set to ${device.format(profile.currentWedges)} " +
                        "but the plan expects ${device.format(expected)} today. Reducing support early " +
                        "increases re-injury risk - check with your physio."
                )
            )
        }
        if (device != null && profile.currentWedges > expected) {
            out.add(
                Warning(
                    Severity.INFO,
                    "${device.name} behind plan",
                    "Your ${device.name.lowercase()} is set to ${device.format(profile.currentWedges)}; " +
                        "the plan expects ${device.format(expected)}. Fine if your clinic chose this - " +
                        "otherwise a change may be overdue."
                )
            )
        }

        if (device != null && phase.deviceUsage != null && latest?.bootWornAsPlanned == false) {
            out.add(
                Warning(
                    Severity.WARNING,
                    "${device.name} not worn as planned",
                    "In phase ${phase.number} the healing tissue has little of its own strength - " +
                        "going without the ${device.name.lowercase()} risks re-injury. Tell your " +
                        "physio if it is a comfort problem."
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

        val sustainedHighPain = recentLogs
            .filter { !it.date.isBefore(today.minusDays(6)) && !it.date.isAfter(today) }
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

    /** Phase-appropriate movement checks, defined by the active protocol. */
    data class MovementCheck(val movement: String, val allowed: Boolean, val note: String)

    fun movementChecks(profile: Profile, today: LocalDate): List<MovementCheck> {
        val n = PhaseEngine.currentPhase(profile, today).number
        return ProtocolRegistry.forProfile(profile).movementChecks.map { spec ->
            val allowed = n >= spec.unlockPhase
            MovementCheck(spec.movement, allowed,
                if (allowed) spec.noteWhenUnlocked else spec.noteWhenLocked)
        }
    }
}
