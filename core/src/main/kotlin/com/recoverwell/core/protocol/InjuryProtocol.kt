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
    /** Objective self-tests the user can perform and log (heel-rise, hop, ...). */
    val selfTests: List<SelfTest> = emptyList(),
    /**
     * The SHARED foundation of the return-to-sport ladder (strength, jogging,
     * hopping) - sport-agnostic rungs every supported sport builds on. Each
     * sport then contributes its own tail (see [supportedSportIds]).
     */
    val returnToSport: List<RtsRung> = emptyList(),
    /** Ids (into SportRegistry) of the sports this injury's program supports. */
    val supportedSportIds: List<String> = emptyList(),
    /** Sport selected when the user hasn't chosen one yet; null = generic. */
    val defaultSportId: String? = null,
    /** Per-phase "what's normal to feel" + encouragement (the emotional side). */
    val mindset: List<PhaseMindset> = emptyList(),
    /** Reassurance about the injury's signature fear (e.g. re-rupture). */
    val reassurance: Reassurance? = null,
    /** Which drawn body visual the twin screen uses (registry key). */
    val bodySceneId: String,
    // ---- copy that would otherwise be hard-coded in screens (scalable) ----
    /** One-line welcome blurb on the onboarding screen. */
    val welcomeBlurb: String,
    /** Headline + body of the onboarding "safety first" card. */
    val safetyTitle: String,
    val safetyBlurb: String,
    /** Intro paragraph at the top of the red-flags screen. */
    val redFlagIntro: String,
    /** Label of the red-flags shortcut button on the digital-twin screen. */
    val redFlagButtonLabel: String,
    /** Appended to exercise names when searching YouTube for a demonstration. */
    val videoContext: String,
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
    /** e.g. "wedge" / "wedges", or "degree" / "degrees" */
    val unitName: String,
    val unitNamePlural: String,
    /** Compact symbol for the unit, e.g. "°"; empty for counted units. */
    val unitSymbol: String,
    /** What a scheduled reduction means, e.g. "remove a wedge" / "lower the heel angle". */
    val reductionVerb: String,
    /** Upper bound for the in-app stepper editing this device's value. */
    val maxValue: Int,
    val plan: WedgePlan
) {
    /** Display a value with its unit: "20°" or "3 wedges". */
    fun format(value: Int): String =
        if (unitSymbol.isNotEmpty()) "$value$unitSymbol" else "$value $unitNamePlural"
}

/**
 * An objective self-test the user performs at home. A [symmetry] test compares
 * the injured side against the other side (pass = limb-symmetry index >=
 * [passThreshold]%); a single-value test passes when the injured-side value
 * meets [passThreshold] in the test's [unit] (>=, or <= when [lowerIsBetter]).
 */
data class SelfTest(
    val id: String,
    val name: String,
    val unit: String,
    val symmetry: Boolean,
    val passThreshold: Double,
    val requirePainFree: Boolean,
    val lowerIsBetter: Boolean,
    val howTo: List<String>,
    val precaution: String,
    /** Earliest rehab phase in which it is appropriate to attempt this test. */
    val earliestPhase: Int
) {
    /** "90% symmetry" or "20 reps" etc. - the bar to clear. */
    fun targetLabel(): String =
        if (symmetry) "${passThreshold.toInt()}% symmetry"
        else (if (lowerIsBetter) "<= " else "") + "${trim(passThreshold)} $unit" +
            (if (lowerIsBetter) "" else "+")

    fun valueLabel(r: com.recoverwell.core.model.SelfTestResult): String =
        if (symmetry) (r.symmetryPct?.let { "$it% symmetry" } ?: "needs both sides")
        else "${trim(r.injuredValue)} $unit"

    private fun trim(v: Double): String =
        if (v == Math.floor(v)) v.toInt().toString() else v.toString()
}

/**
 * One rung of the return-to-sport ladder. Cleared when every test in [testIds]
 * passes and (if [requiresPhysioSignoff]) the user records physio clearance.
 */
data class RtsRung(
    val id: String,
    val order: Int,
    val title: String,
    val summary: String,
    /** Rehab phase this rung belongs to; it stays locked until that phase is active. */
    val phase: Int,
    val testIds: List<String>,
    val guidance: List<String>,
    val requiresPhysioSignoff: Boolean
)

/** The emotional reality of a phase: what's normal to feel, and a nudge forward. */
data class PhaseMindset(
    val phase: Int,
    val normalToFeel: List<String>,
    val encouragement: String
)

/** Addresses the injury's signature anxiety (for Achilles: fear of re-rupture). */
data class Reassurance(
    val title: String,
    val body: String,
    /** How to tell ordinary recovery sensations from genuine warning signs. */
    val normalVsFlag: List<Pair<String, String>>
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
