package com.recoverwell.core.protocol

import com.recoverwell.core.model.*

/**
 * The framework: a rehab protocol is pure data. Everything injury-specific -
 * phases, milestones, red flags, the support device and its vocabulary,
 * movement checks, body visual, prefills - lives in one InjuryProtocol value.
 * Engines and screens only ever read through this type, so supporting a new
 * injury (or a variant of an existing one) is a new data file plus one
 * ProtocolRegistry entry; no engine or UI changes.
 */
data class InjuryProtocol(
    val id: String,
    /** e.g. "Achilles tendon rupture" */
    val injuryName: String,
    /** e.g. "Conservative (non-surgical) · walking boot" */
    val variantName: String,
    /** Citation-style name of the source protocol(s). */
    val protocolName: String,
    /** Shown wherever typical timings appear. */
    val placeholderNote: String,
    /** Does left/right apply to this injury? */
    val sided: Boolean,
    /**
     * Adjustable support device (boot/brace/sling) with a stepped reduction
     * plan, or null when the protocol has none. The unit vocabulary drives
     * every related label ("wedge", "brace degree", ...).
     */
    val supportDevice: SupportDevice?,
    val phases: List<PhaseSpec>,
    val milestones: List<Milestone>,
    val redFlags: List<RedFlagSection>,
    /** Digital-twin "Can I...?" checks, unlocked by phase. */
    val movementChecks: List<MovementCheckSpec>,
    /** Which drawn body visual the twin screen uses (registry key). */
    val bodySceneId: String,
    // ---- onboarding prefills (all editable in-app) ----
    val prefillDescription: String,
    val prefillGoal: String,
    val prefillAppointments: List<Appointment>,
    val prefillMedications: List<Medication>,
    val prefillTasks: List<RehabTask>
) {
    fun phase(number: Int): PhaseSpec = phases.first { it.number == number }
}

data class SupportDevice(
    /** e.g. "Walking boot" */
    val name: String,
    /** e.g. "wedge" / "wedges" */
    val unitName: String,
    val unitNamePlural: String,
    /** What a scheduled reduction means, e.g. "remove 1 wedge". */
    val reductionVerb: String,
    val plan: WedgePlan
)

data class MovementCheckSpec(
    val movement: String,
    /** First phase in which this is allowed. */
    val unlockPhase: Int,
    val noteWhenLocked: String,
    val noteWhenUnlocked: String
)

data class RedFlagSection(
    val id: String,
    val title: String,
    val urgency: String,
    val symptoms: List<String>,
    val action: String
)

/** App-wide framing that is true for every protocol. */
object RehabFramework {
    const val APP_NAME = "RecoverWell"

    const val DISCLAIMER =
        "RecoverWell supports - but never replaces - the advice of your physiotherapist " +
            "and consultant. All timelines are typical-protocol placeholders: confirm every " +
            "progression with your own clinical team before acting on it."
}

/**
 * All protocols this build ships. Adding an injury = add its data file and
 * list it here. Exactly one entry today: the conservative Achilles pathway.
 */
object ProtocolRegistry {
    val all: List<InjuryProtocol> = listOf(
        AchillesConservative.protocol
    )

    val default: InjuryProtocol = all.first()

    fun byId(id: String): InjuryProtocol = all.firstOrNull { it.id == id } ?: default

    fun forProfile(profile: Profile): InjuryProtocol = byId(profile.protocolId)
}
