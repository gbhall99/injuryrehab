package com.recoverwell.core.model

import java.time.LocalDate
import java.time.LocalTime

enum class Side { LEFT, RIGHT }

/**
 * This app deliberately models only the conservative (non-surgical) pathway.
 * The enum exists so the stored profile is explicit about it and so a future
 * version could add a surgical pathway with its own protocol content.
 */
enum class Pathway { CONSERVATIVE_NON_SURGICAL }

enum class WeightBearing(val label: String, val shortLabel: String) {
    NON_WEIGHT_BEARING("Non weight-bearing", "None"),
    PARTIAL("Partial weight-bearing", "Partial"),
    AS_TOLERATED("Weight-bearing as tolerated", "As tolerated"),
    FULL("Full weight-bearing", "Full")
}

enum class Swelling(val score: Int, val label: String) {
    NONE(0, "None"), MILD(1, "Mild"), MODERATE(2, "Moderate"), SEVERE(3, "Severe")
}

data class Appointment(
    val date: LocalDate,
    val label: String,
    val completed: Boolean,
    /** Stable identity so completing/removing one targets exactly that entry.
     *  Blank on legacy backups predating the field; matched by date+label then. */
    val id: String = "",
    /** Who the appointment is with, e.g. "Mr Patel (consultant)". Optional. */
    val withWhom: String = ""
)

/**
 * Plan for stepping a support device down from its starting setting to neutral
 * (0). The value is expressed in the device's own unit - boot wedges (step 1)
 * or heel-angle degrees (e.g. step 5) - and [stepSize] is how much each
 * scheduled change reduces it. Every value is a physio-confirmable placeholder,
 * editable in the app. Field names keep the historical "wedge" spelling for
 * backup compatibility; the unit is defined by the protocol's SupportDevice.
 */
data class WedgePlan(
    val initialWedges: Int,
    val removalStartWeek: Int,
    val removalIntervalDays: Int,
    val stepSize: Int = 1
) {
    private val step get() = stepSize.coerceAtLeast(1)

    /**
     * Dates on which a reduction is due, with the device value after it.
     *
     * Dates default to the formula (injury date + start week + interval), but any
     * step can be pinned to an exact clinic-agreed date via [overrides], keyed by
     * the 0-based step index. A pinned step keeps its date even when the injury
     * date or plan parameters change, so the two date concepts stay independent.
     */
    fun removalSchedule(
        injuryDate: LocalDate,
        overrides: Map<Int, LocalDate> = emptyMap()
    ): List<Pair<LocalDate, Int>> {
        val out = ArrayList<Pair<LocalDate, Int>>()
        var date = injuryDate.plusDays((removalStartWeek - 1) * 7L)
        var remaining = initialWedges
        var index = 0
        while (remaining > 0) {
            remaining = (remaining - step).coerceAtLeast(0)
            out.add((overrides[index] ?: date) to remaining)
            date = date.plusDays(removalIntervalDays.toLong())
            index++
        }
        return out
    }

    /** Expected device value on a given date. Honours any pinned step dates. */
    fun expectedWedges(
        injuryDate: LocalDate,
        on: LocalDate,
        overrides: Map<Int, LocalDate> = emptyMap()
    ): Int {
        var expected = initialWedges
        // sort by date so a pin that moves a step out of formula order still
        // resolves to the last reduction that has actually fallen due by [on]
        for ((date, after) in removalSchedule(injuryDate, overrides).sortedBy { it.first }) {
            if (!on.isBefore(date)) expected = after
        }
        return expected
    }
}

data class Profile(
    /** Which InjuryProtocol in the registry this recovery follows. */
    val protocolId: String,
    val name: String,
    val injuryDate: LocalDate,
    val side: Side,
    val pathway: Pathway,
    val injuryDescription: String,
    val goal: String,
    val appointments: List<Appointment>,
    val wedgePlan: WedgePlan,
    /** Per-step pinned boot-change dates (key = 0-based reduction step), overriding
     *  the formula. Lets the user fix exact clinic dates independently of the
     *  injury date and physio appointments. */
    val wedgeDateOverrides: Map<Int, LocalDate> = emptyMap(),
    val currentWedges: Int,
    val weightBearing: WeightBearing,
    /** Highest phase number the user has recorded physio confirmation for. */
    val physioConfirmedPhase: Int,
    /** Optional per-phase start date overrides set by the user/physio. Key = phase number. */
    val phaseStartOverrides: Map<Int, LocalDate>,
    /** Date each phase was physio-confirmed, for pace/forecasting. Key = phase number. */
    val phaseConfirmedDates: Map<Int, LocalDate> = emptyMap(),
    val onboardingComplete: Boolean,
    val disclaimerAcknowledged: Boolean,
    /** Target sport for the return-to-sport program; blank = the protocol default. */
    val sportId: String = "",
    /** Chosen support device (boot/cast); blank = the protocol default. */
    val deviceId: String = "",
    /**
     * Estimated date the user is aiming to be back to sport, set at first setup
     * and editable later. Drives the overall recovery-days timeline ("day X of
     * N"). Null = use the default estimate ([effectiveReturnDate]); always an
     * estimate the physio's real timeline overrides.
     */
    val targetReturnDate: LocalDate? = null,
    /**
     * Date the user stopped using the support device entirely (physio-agreed
     * "out of the boot"). Null = still using it as the phase directs. Going
     * boot-free is a real event that need not line up with phase timing, so it
     * is profile state: from this date boot checks and boot-change reminders
     * stop and the digital twin shows the leg without the device.
     */
    val bootWeanedDate: LocalDate? = null
) {
    /** Aimed return-to-sport date, defaulting to ~12 months post-injury. */
    fun effectiveReturnDate(): LocalDate =
        targetReturnDate ?: injuryDate.plusDays(DEFAULT_RETURN_DAYS)

    /** Whether the support device is still in use on [date] (see [bootWeanedDate]). */
    fun usesDeviceOn(date: LocalDate): Boolean =
        bootWeanedDate == null || date.isBefore(bootWeanedDate)

    companion object {
        /** Default estimated recovery span when no return date is set (~1 year). */
        const val DEFAULT_RETURN_DAYS = 365L
    }
}

data class Medication(
    val id: String,
    val name: String,
    val dose: String,
    val times: List<LocalTime>,
    val notes: String,
    val active: Boolean,
    /**
     * Optional last day the medicine is taken (inclusive). VTE prophylaxis after
     * lower-limb immobilisation is time-limited - NICE NG89 continues it only
     * while the limb is immobilised - so a course can carry a clinician-set end
     * after which reminders stop. Null = ongoing / no end set. A placeholder set
     * by the app is always editable and should be confirmed with the prescriber.
     */
    val courseEndDate: LocalDate? = null,
    /**
     * Optional date to surface a "confirm whether to continue or stop" prompt,
     * usually a few days before [courseEndDate]. Null = no review prompt.
     */
    val reviewDate: LocalDate? = null
) {
    /**
     * Whether this medicine should still be scheduled on [date]: it must be
     * active and not past its course end (the end date itself is inclusive).
     */
    fun activeOn(date: LocalDate): Boolean =
        active && (courseEndDate == null || !date.isAfter(courseEndDate))

    companion object {
        /**
         * Typical length (weeks from injury) of a clot-prevention course after
         * lower-limb immobilisation - roughly the boot period (NICE NG89 keeps
         * prophylaxis only while the limb is immobilised). Used to seed the
         * editable course-end placeholder, and to prompt a review when a course
         * has no end date at all. A placeholder, never a prescription.
         */
        const val TYPICAL_COURSE_WEEKS = 10L

        /** When to prompt "continue or stop?" - shortly before the typical end. */
        const val TYPICAL_REVIEW_WEEKS = 9L
    }
}

enum class TaskKind(val label: String) {
    ELEVATION("Elevation"),
    BOOT_CHECK("Boot check"),
    CIRCULATION_CHECK("Circulation / calf check"),
    WEDGE_CHANGE("Wedge change"),
    CUSTOM("Custom task")
}

/**
 * A recurring rehab task (daily at fixed times while the phase range applies)
 * or, when [dueDate] is set, a one-off dated task such as a wedge removal.
 */
data class RehabTask(
    val id: String,
    val kind: TaskKind,
    val title: String,
    val detail: String,
    val times: List<LocalTime>,
    val fromPhase: Int,
    val toPhase: Int,
    val dueDate: LocalDate?,
    val active: Boolean
)

data class ExerciseSpec(
    val id: String,
    val phase: Int,
    val name: String,
    /** Identifier of the bundled animated demonstration. */
    val demoId: String,
    val cues: List<String>,
    val sets: Int,
    val reps: Int,
    val holdSeconds: Int,
    val sessionsPerDay: Int,
    val whyItMatters: String,
    val precaution: String,
    /** Optional override search phrase for the "watch on YouTube" link;
     *  blank = derive from the exercise name + protocol video context. */
    val videoQuery: String = ""
)

/** User edits applied on top of the protocol defaults; null = keep default. */
data class ExerciseOverride(
    val exerciseId: String,
    val sets: Int?,
    val reps: Int?,
    val holdSeconds: Int?,
    val sessionsPerDay: Int?,
    val enabled: Boolean,
    /** User-pinned YouTube video id for this exercise's demonstration. */
    val videoId: String? = null
)

data class PhaseSpec(
    val number: Int,
    val title: String,
    val subtitle: String,
    /** One line on what the healing tissue is doing in this phase. */
    val tissueState: String,
    /** Support-device state this phase; "{n}" = current unit count; null = no device. */
    val deviceUsage: String?,
    /** Default start, in completed weeks since injury (phase 1 = 0). */
    val startWeek: Int,
    /** Default end week (exclusive); null for the final open-ended phase. */
    val endWeek: Int?,
    val entryCriteria: List<String>,
    val goals: List<String>,
    val precautions: List<String>,
    val allowed: List<String>,
    val notAllowed: List<String>,
    val exercises: List<ExerciseSpec>
)

data class DailyLog(
    val date: LocalDate,
    val pain: Int?,
    val swelling: Swelling?,
    val romNote: String?,
    val bootWornAsPlanned: Boolean?,
    val wedges: Int?,
    val weightBearing: WeightBearing?,
    val mood: Int?,
    val energy: Int?,
    val notes: String?
) {
    companion object {
        fun empty(date: LocalDate) = DailyLog(date, null, null, null, null, null, null, null, null, null)
    }
}

enum class EventType { MEDICATION, TASK, EXERCISE }

enum class EventStatus { TAKEN, MISSED, DONE, SKIPPED }

/**
 * One taken/missed/done record. [slotKey] identifies which occurrence on the
 * day it belongs to (e.g. the "08:00" dose vs the "20:00" dose).
 */
data class EventLog(
    val id: String,
    val date: LocalDate,
    val type: EventType,
    val refId: String,
    val slotKey: String,
    val status: EventStatus,
    val recordedAtMinuteOfDay: Int
)

data class Milestone(
    val week: Int,
    val title: String,
    val detail: String
)

/** A dated record of what a clinician said - the durable half of the physio loop. */
data class PhysioNote(
    val id: String,
    val date: LocalDate,
    val text: String
)

/** Overall tone the AI inferred from a spoken journal entry, ordered low->high. */
enum class JournalMood(val score: Int, val label: String) {
    LOW(1, "Low"), MIXED(2, "Mixed"), STEADY(3, "Steady"), POSITIVE(4, "Positive"), GREAT(5, "Great");

    companion object {
        /** Tolerant lookup by enum name or display label; defaults to [MIXED]. */
        fun from(s: String?): JournalMood {
            val t = s?.trim().orEmpty()
            return values().firstOrNull { it.name.equals(t, true) || it.label.equals(t, true) } ?: MIXED
        }
    }
}

/**
 * One daily "Rosebud-style" spoken check-in: the transcript of what the user
 * said, plus the AI's reflection, observed insights, suggested tips and an
 * inferred mood. Audio is transcribed then discarded - only text is kept.
 */
data class JournalEntry(
    val id: String,
    val date: LocalDate,
    val transcript: String,
    val reflection: String,
    val insights: List<String>,
    val tips: List<String>,
    val mood: JournalMood
)

/**
 * One logged objective self-test (e.g. single-leg heel-rise count). Symmetry
 * tests carry both sides so a limb-symmetry index can be computed; single-value
 * tests leave [otherValue] null.
 */
data class SelfTestResult(
    val id: String,
    val testId: String,
    val date: LocalDate,
    val injuredValue: Double,
    val otherValue: Double?,
    val painFree: Boolean,
    val note: String
) {
    /** Limb-symmetry index as a percentage (injured / other x 100), or null. */
    val symmetryPct: Int?
        get() = otherValue?.takeIf { it > 0.0 }?.let { ((injuredValue / it) * 100).toInt() }
}
