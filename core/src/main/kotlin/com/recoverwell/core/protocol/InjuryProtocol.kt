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
    /** Ids (into DeviceRegistry) of the boots/casts this injury supports. */
    val supportedDeviceIds: List<String> = emptyList(),
    /** Device used when the user hasn't chosen one; null = use [supportDevice]. */
    val defaultDeviceId: String? = null,
    /** Per-phase "what's normal to feel" + encouragement (the emotional side). */
    val mindset: List<PhaseMindset> = emptyList(),
    /** Reassurance about the injury's signature fear (e.g. re-rupture). */
    val reassurance: Reassurance? = null,
    /** Week-banded "what to expect" content, surfaced at the right time. */
    val expectations: List<WeekExpectation> = emptyList(),
    /** General fitness / conditioning that's safe to do during recovery. */
    val fitness: List<FitnessActivity> = emptyList(),
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

/** How a support device is adjusted - drives vocabulary and the twin visual. */
enum class DeviceKind {
    /** A ROM dial sets the ankle angle in degrees (e.g. OPED VACOped). */
    BOOT_DIAL,
    /** Stacked heel wedges set the angle; removed one at a time (e.g. Aircast). */
    BOOT_WEDGES,
    /** A rigid cast in fixed equinus; only a clinic adjusts it. */
    CAST
}

data class SupportDevice(
    /** Stable id used for selection/backup, e.g. "vacoped". */
    val id: String,
    /** e.g. "VACOped boot" */
    val name: String,
    val kind: DeviceKind,
    /** e.g. "wedge" / "wedges", or "degree" / "degrees" */
    val unitName: String,
    val unitNamePlural: String,
    /** Compact symbol for the unit, e.g. "°"; empty for counted units. */
    val unitSymbol: String,
    /** What a scheduled reduction means, e.g. "remove a wedge" / "lower the ROM dial". */
    val reductionVerb: String,
    /** Upper bound for the in-app stepper editing this device's value. */
    val maxValue: Int,
    val plan: WedgePlan,
    /** One line on how this device works. */
    val operation: String = "",
    /** How to set up / wear / operate it safely (device-specific). */
    val setupNotes: List<String> = emptyList()
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

/**
 * General conditioning the user can do to stay fit *during* recovery, over and
 * above rehabbing the injury - chosen so it doesn't load the healing tissue.
 * [minPhase] gates when it becomes appropriate (e.g. out-of-boot work).
 */
data class FitnessActivity(
    val id: String,
    val name: String,
    /** Grouping label, e.g. "Upper body", "Core & trunk", "Cardio (no impact)". */
    val category: String,
    val minPhase: Int,
    val detail: String
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

/**
 * Plain-language "what to expect" for a span of weeks - the proactive answers to
 * the anxious questions people otherwise Google at 2am. Advisory, not a forecast.
 */
data class WeekExpectation(
    val weekFrom: Int,
    /** Exclusive upper bound; the last period can use a large number. */
    val weekTo: Int,
    val title: String,
    val summary: String,
    /** What's commonly happening around now. */
    val likely: List<String>,
    /** One reassuring line. */
    val reassure: String
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

    /** Resolved per (protocol, sport, device) so tokens/device are applied once. */
    private val resolvedCache = java.util.concurrent.ConcurrentHashMap<String, InjuryProtocol>()

    /** The user's chosen support device (boot/cast), or the protocol's default. */
    fun deviceFor(profile: Profile): SupportDevice? {
        val base = byId(profile.protocolId)
        val id = profile.deviceId.ifBlank { base.defaultDeviceId ?: "" }
        return if (id.isBlank()) base.supportDevice else DeviceRegistry.byId(id) ?: base.supportDevice
    }

    /**
     * The protocol for this profile, with {sport} tokens resolved and the chosen
     * boot/cast swapped in. Every screen and engine reads through here, so
     * picking a sport or device reshapes the plan and copy at once.
     */
    fun forProfile(profile: Profile): InjuryProtocol {
        val base = byId(profile.protocolId)
        val sportId = profile.sportId.ifBlank { base.defaultSportId ?: "" }
        val sport = SportRegistry.byId(sportId)
        val device = deviceFor(profile)
        val key = base.id + "|" + (sport?.id ?: "-") + "|" + (device?.id ?: "-")
        return resolvedCache.getOrPut(key) {
            var p = if (sport != null) SportText.resolveProtocol(base, sport.name) else base
            if (device != null && device.id != base.supportDevice?.id) p = p.copy(supportDevice = device)
            p
        }
    }
}
