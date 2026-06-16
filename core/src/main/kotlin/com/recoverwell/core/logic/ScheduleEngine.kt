package com.recoverwell.core.logic

import com.recoverwell.core.model.*
import com.recoverwell.core.protocol.ProtocolRegistry
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Builds the unified daily checklist (medications, rehab tasks, exercise
 * sessions, dated wedge changes) and computes upcoming reminder times.
 * Pure logic: the Android layer only renders items and sets alarms.
 */
object ScheduleEngine {

    enum class ItemKind { MEDICATION, TASK, WEDGE_CHANGE, EXERCISE, CHECKIN }

    /** Uniform number of daily exercise sessions; each session is the full routine. */
    const val EXERCISE_SESSIONS_PER_DAY = 3

    data class ChecklistItem(
        val kind: ItemKind,
        val refId: String,
        val slotKey: String,
        val title: String,
        val subtitle: String,
        val time: LocalTime?,
        val status: EventStatus?
    ) {
        val isDone: Boolean get() = status == EventStatus.TAKEN || status == EventStatus.DONE
    }

    data class Reminder(
        val at: LocalDateTime,
        val kind: ItemKind,
        val refId: String,
        val slotKey: String,
        val title: String,
        val message: String
    )

    fun mergedExercises(specs: List<ExerciseSpec>, overrides: Map<String, ExerciseOverride>): List<ExerciseSpec> =
        specs.mapNotNull { spec ->
            val o = overrides[spec.id] ?: return@mapNotNull spec
            if (!o.enabled) return@mapNotNull null
            spec.copy(
                sets = o.sets ?: spec.sets,
                reps = o.reps ?: spec.reps,
                holdSeconds = o.holdSeconds ?: spec.holdSeconds,
                sessionsPerDay = o.sessionsPerDay ?: spec.sessionsPerDay
            )
        }

    /** Locale-independent: slot keys are stored identifiers, never display text. */
    fun slotKey(time: LocalTime): String =
        String.format(java.util.Locale.ROOT, "%02d:%02d", time.hour, time.minute)

    /**
     * Consecutive days on which EVERY active medication dose was taken,
     * ending today (if today is already complete) or yesterday. Judged
     * against the current schedule - good enough for a motivation streak.
     */
    fun medicationStreak(meds: List<Medication>, events: List<EventLog>, today: LocalDate): Int {
        val slots = meds.filter { it.active }
            .flatMap { m -> m.times.map { m.id to slotKey(it) } }
        if (slots.isEmpty()) return 0
        val taken = events.filter { it.type == EventType.MEDICATION && it.status == EventStatus.TAKEN }
            .groupBy { it.date }
        fun complete(d: LocalDate) = slots.all { (id, slot) ->
            taken[d]?.any { it.refId == id && it.slotKey == slot } == true
        }
        var day = if (complete(today)) today else today.minusDays(1)
        var n = 0
        while (complete(day)) {
            n++
            day = day.minusDays(1)
        }
        return n
    }

    /** Device-reduction items due on [date] per the editable plan (boot wedges etc.). */
    fun wedgeChangesOn(profile: Profile, date: LocalDate): List<ChecklistItem> {
        val device = ProtocolRegistry.forProfile(profile).supportDevice ?: return emptyList()
        return profile.wedgePlan.removalSchedule(profile.injuryDate, profile.wedgeDateOverrides)
            .filter { it.first == date }
            .map { (d, after) ->
                ChecklistItem(
                    kind = ItemKind.WEDGE_CHANGE,
                    refId = "wedge_$after",
                    slotKey = d.toString(),
                    title = "Boot change due: ${device.reductionVerb} to ${device.format(after)}",
                    subtitle = ProtocolRegistry.forProfile(profile).placeholderNote +
                        " Only adjust the ${device.name.lowercase()} if your clinic has agreed this step.",
                    time = LocalTime.of(9, 0),
                    status = null
                )
            }
    }

    fun dailyChecklist(
        profile: Profile,
        meds: List<Medication>,
        tasks: List<RehabTask>,
        overrides: Map<String, ExerciseOverride>,
        events: List<EventLog>,
        date: LocalDate
    ): List<ChecklistItem> {
        val phase = PhaseEngine.currentPhase(profile, date)
        val statusOf = { type: EventType, refId: String, slot: String ->
            events.lastOrNull {
                it.date == date && it.type == type && it.refId == refId && it.slotKey == slot
            }?.status
        }

        val items = ArrayList<ChecklistItem>()

        for (med in meds.filter { it.active }) {
            for (t in med.times.sorted()) {
                val slot = slotKey(t)
                items.add(
                    ChecklistItem(
                        ItemKind.MEDICATION, med.id, slot,
                        "${med.name} ${med.dose}",
                        "Scheduled $slot - clot prevention matters: take on time",
                        t, statusOf(EventType.MEDICATION, med.id, slot)
                    )
                )
            }
        }

        for (task in tasks.filter { it.active && it.dueDate == null }) {
            if (phase.number < task.fromPhase || phase.number > task.toPhase) continue
            for (t in task.times.sorted()) {
                val slot = slotKey(t)
                items.add(
                    ChecklistItem(
                        ItemKind.TASK, task.id, slot,
                        task.title, task.detail, t,
                        statusOf(EventType.TASK, task.id, slot)
                    )
                )
            }
        }

        for (task in tasks.filter { it.active && it.dueDate == date }) {
            val slot = task.dueDate.toString()
            items.add(
                ChecklistItem(
                    ItemKind.TASK, task.id, slot, task.title, task.detail,
                    task.times.firstOrNull(), statusOf(EventType.TASK, task.id, slot)
                )
            )
        }

        items.addAll(wedgeChangesOn(profile, date).map { item ->
            item.copy(status = statusOf(EventType.TASK, item.refId, item.slotKey))
        })

        // Exercises are grouped into a uniform number of daily SESSIONS; each
        // session is the same routine (all of the phase's exercises once), rather
        // than each exercise carrying its own 2x/3x/4x count.
        val exercises = mergedExercises(phase.exercises, overrides)
        if (exercises.isNotEmpty()) {
            for (session in 1..EXERCISE_SESSIONS_PER_DAY) {
                val slot = "session$session"
                for (ex in exercises) {
                    items.add(
                        ChecklistItem(
                            ItemKind.EXERCISE, ex.id, slot, ex.name,
                            exercisePrescription(ex), null,
                            statusOf(EventType.EXERCISE, ex.id, slot)
                        )
                    )
                }
            }
        }

        return items.sortedWith(compareBy({ it.time == null }, { it.time }, { it.title }))
    }

    fun exercisePrescription(ex: ExerciseSpec): String {
        val hold = when {
            ex.holdSeconds >= 60 -> " · hold ${ex.holdSeconds / 60} min"
            ex.holdSeconds > 0 -> " · hold ${ex.holdSeconds}s"
            else -> ""
        }
        return "${ex.sets} set${if (ex.sets > 1) "s" else ""} × ${ex.reps}$hold"
    }

    /** Stable id used for the once-daily "do your exercises" engagement nudge. */
    const val EXERCISE_SESSION_REF = "session_reminder"

    /** Stable id used for the once-daily "how's it feeling?" check-in nudge. */
    const val DAILY_CHECKIN_REF = "daily_checkin"

    /**
     * All reminders in (now, now+horizonDays]: medication times, task times,
     * dated wedge changes and - when [exerciseReminderTime] is set - one daily
     * exercise-session nudge per day that has exercises in the active phase.
     * The Android layer schedules the earliest ones.
     */
    fun upcomingReminders(
        profile: Profile,
        meds: List<Medication>,
        tasks: List<RehabTask>,
        now: LocalDateTime,
        horizonDays: Int = 3,
        exerciseReminderTime: LocalTime? = null,
        overrides: Map<String, ExerciseOverride> = emptyMap(),
        checkInTime: LocalTime? = null
    ): List<Reminder> {
        val out = ArrayList<Reminder>()
        for (offset in 0..horizonDays) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            val phase = PhaseEngine.currentPhase(profile, date)

            if (checkInTime != null) {
                val at = LocalDateTime.of(date, checkInTime)
                if (at.isAfter(now)) out.add(
                    Reminder(
                        at, ItemKind.CHECKIN, DAILY_CHECKIN_REF, DAILY_CHECKIN_REF,
                        "How's it feeling today?",
                        "A 10-second check-in keeps your recovery trends accurate. How's the pain right now?"
                    )
                )
            }

            if (exerciseReminderTime != null) {
                val exercises = mergedExercises(phase.exercises, overrides)
                val at = LocalDateTime.of(date, exerciseReminderTime)
                if (exercises.isNotEmpty() && at.isAfter(now)) out.add(
                    Reminder(
                        at, ItemKind.EXERCISE, EXERCISE_SESSION_REF, EXERCISE_SESSION_REF,
                        "Rehab exercises",
                        "${exercises.size} exercise${if (exercises.size == 1) "" else "s"} in today's plan. " +
                            "A few focused minutes keeps your recovery on track."
                    )
                )
            }

            for (med in meds.filter { it.active }) {
                for (t in med.times) {
                    val at = LocalDateTime.of(date, t)
                    if (at.isAfter(now)) out.add(
                        Reminder(
                            at, ItemKind.MEDICATION, med.id, slotKey(t),
                            "Medication: ${med.name} ${med.dose}",
                            "Time for your ${slotKey(t)} dose. Clot prevention is clinically important."
                        )
                    )
                }
            }
            for (task in tasks.filter { it.active && it.dueDate == null }) {
                if (phase.number < task.fromPhase || phase.number > task.toPhase) continue
                for (t in task.times) {
                    val at = LocalDateTime.of(date, t)
                    if (at.isAfter(now)) out.add(
                        Reminder(at, ItemKind.TASK, task.id, slotKey(t), task.title, task.detail)
                    )
                }
            }
            for (task in tasks.filter { it.active && it.dueDate == date }) {
                val t = task.times.firstOrNull() ?: LocalTime.of(9, 0)
                val at = LocalDateTime.of(date, t)
                if (at.isAfter(now)) out.add(
                    Reminder(at, ItemKind.TASK, task.id, date.toString(), task.title, task.detail)
                )
            }
            for (item in wedgeChangesOn(profile, date)) {
                val at = LocalDateTime.of(date, item.time ?: LocalTime.of(9, 0))
                if (at.isAfter(now)) out.add(
                    Reminder(at, ItemKind.WEDGE_CHANGE, item.refId, item.slotKey, item.title, item.subtitle)
                )
            }
        }
        return out.sortedBy { it.at }
    }
}
