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
    val completed: Boolean
)

/**
 * Plan for stepping the boot down from full equinus to neutral.
 * Defaults follow a typical NHS conservative functional protocol
 * (one wedge removed per week from the start of week 3) but every value
 * is a physio-confirmable placeholder, editable in the app.
 */
data class WedgePlan(
    val initialWedges: Int,
    val removalStartWeek: Int,
    val removalIntervalDays: Int
) {
    /** Dates on which one wedge is due to be removed, with the wedge count after removal. */
    fun removalSchedule(injuryDate: LocalDate): List<Pair<LocalDate, Int>> {
        val out = ArrayList<Pair<LocalDate, Int>>()
        var date = injuryDate.plusDays((removalStartWeek - 1) * 7L)
        var remaining = initialWedges
        while (remaining > 0) {
            remaining -= 1
            out.add(date to remaining)
            date = date.plusDays(removalIntervalDays.toLong())
        }
        return out
    }

    /** Expected wedge count in the boot on a given date. */
    fun expectedWedges(injuryDate: LocalDate, on: LocalDate): Int {
        var expected = initialWedges
        for ((date, after) in removalSchedule(injuryDate)) {
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
    val currentWedges: Int,
    val weightBearing: WeightBearing,
    /** Highest phase number the user has recorded physio confirmation for. */
    val physioConfirmedPhase: Int,
    /** Optional per-phase start date overrides set by the user/physio. Key = phase number. */
    val phaseStartOverrides: Map<Int, LocalDate>,
    val onboardingComplete: Boolean,
    val disclaimerAcknowledged: Boolean
)

data class Medication(
    val id: String,
    val name: String,
    val dose: String,
    val times: List<LocalTime>,
    val notes: String,
    val active: Boolean
)

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
    val precaution: String
)

/** User edits applied on top of the protocol defaults; null = keep default. */
data class ExerciseOverride(
    val exerciseId: String,
    val sets: Int?,
    val reps: Int?,
    val holdSeconds: Int?,
    val sessionsPerDay: Int?,
    val enabled: Boolean
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
