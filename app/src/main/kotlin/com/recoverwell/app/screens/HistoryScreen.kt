package com.recoverwell.app.screens

import android.app.AlertDialog
import android.view.View
import android.widget.Toast
import com.recoverwell.app.MainActivity
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.PhaseEngine
import com.recoverwell.core.logic.ScheduleEngine
import com.recoverwell.core.model.EventLog
import com.recoverwell.core.model.EventStatus
import com.recoverwell.core.model.EventType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Per-stat history views, reachable by tapping the stats on Today. Each lists
 * past days and lets the record be corrected:
 *  - check-ins  -> edit that day's pain/mood log (shared check-in form)
 *  - medication -> set each dose Taken / Missed for a past day
 *  - exercises  -> mark each session done / not done for a past day
 *
 * Editing a past day appends a new dated [EventLog]; the store reads
 * "latest-by-rowid wins", so the newest correction is what counts.
 */
object HistoryScreen {

    private const val DAYS = 45
    private val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM")

    // ---- check-in (daily log) history --------------------------------------

    fun checkins(a: MainActivity): View {
        val today = LocalDate.now()
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Check-in history") { a.popOverlay() })
        col.addView(Ui.caption(a, "Every check-in you've logged. Tap any day to edit it, or " +
            "backfill a day you missed."))

        col.addView(Ui.listRow(a, "ic_edit", "Add a day you missed", "Pick any past date") {
            android.app.DatePickerDialog(a, { _, y, m, d ->
                val date = LocalDate.of(y, m + 1, d).coerceAtMost(today)
                a.pushOverlay(date.format(dayFmt)) { editLog(a, date) }
            }, today.year, today.monthValue - 1, today.dayOfMonth).apply {
                datePicker.maxDate = System.currentTimeMillis()
            }.show()
        })

        val logs = a.store.allLogs().filter { it.pain != null }.sortedByDescending { it.date }
        if (logs.isEmpty()) {
            col.addView(Ui.spacer(a, 8))
            col.addView(Ui.caption(a, "No check-ins logged yet."))
        } else {
            col.addView(Ui.section(a, "Logged days"))
            for (log in logs) {
                val extras = buildList {
                    log.mood?.let { add("mood $it/5") }
                    log.swelling?.let { add("swelling ${it.label.lowercase()}") }
                    if (!log.notes.isNullOrBlank()) add("note")
                }.joinToString(" · ")
                val sub = "Pain ${log.pain}/10" + if (extras.isBlank()) "" else " · $extras"
                col.addView(Ui.listRow(a, "ic_progress", log.date.format(dayFmt), sub) {
                    a.pushOverlay(log.date.format(dayFmt)) { editLog(a, log.date) }
                })
            }
        }
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    private fun editLog(a: MainActivity, date: LocalDate): View {
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, date.format(dayFmt)) { a.popOverlay() })
        col.addView(TodayScreen.checkInCard(a, date, expanded = true) {
            Toast.makeText(a, "Log saved", Toast.LENGTH_SHORT).show()
            a.popOverlay()
        })
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    // ---- medication history -------------------------------------------------

    fun medication(a: MainActivity): View {
        val today = LocalDate.now()
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Medication history") { a.popOverlay() })
        val meds = a.store.medications().filter { it.active }
        if (meds.isEmpty()) {
            col.addView(Ui.caption(a, "No medications are being tracked. Add one under " +
                "More - Medications to log doses."))
            col.addView(Ui.spacer(a, 24))
            return Ui.scroll(a, col)
        }
        col.addView(Ui.caption(a, "Each day's doses. Tap a day to correct what was taken or missed."))
        col.addView(Ui.section(a, "Recent days"))
        val events = a.store.allEvents()
        for (i in 0 until DAYS) {
            val d = today.minusDays(i.toLong())
            val slots = meds.filter { it.activeOn(d) }.flatMap { m -> m.times.map { m.id to ScheduleEngine.slotKey(it) } }
            if (slots.isEmpty()) continue
            val taken = slots.count { (id, slot) ->
                latestStatus(events, d, EventType.MEDICATION, id, slot) == EventStatus.TAKEN
            }
            col.addView(Ui.listRow(a, "ic_pill", d.format(dayFmt), "$taken/${slots.size} doses taken") {
                a.pushOverlay("Doses · ${d.format(dayFmt)}") { medDay(a, d) }
            })
        }
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    private fun medDay(a: MainActivity, date: LocalDate): View {
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, date.format(dayFmt)) { a.popOverlay() })
        col.addView(Ui.caption(a, "Tap a dose to set whether it was taken or missed."))
        val events = a.store.allEvents()
        for (med in a.store.medications().filter { it.activeOn(date) }) {
            for (t in med.times.sorted()) {
                val slot = ScheduleEngine.slotKey(t)
                val status = latestStatus(events, date, EventType.MEDICATION, med.id, slot)
                val label = when (status) {
                    EventStatus.TAKEN -> "Taken"
                    EventStatus.MISSED -> "Missed"
                    else -> "Not logged"
                }
                col.addView(Ui.checkRow(a, "${med.name} ${med.dose}".trim(), "$slot · $label",
                    null, status == EventStatus.TAKEN, if (status == EventStatus.MISSED) "Marked missed" else null) {
                    AlertDialog.Builder(a)
                        .setTitle("${med.name} · $slot")
                        .setItems(arrayOf("Taken", "Missed")) { _, which ->
                            writeEvent(a, date, EventType.MEDICATION, med.id, slot,
                                if (which == 0) EventStatus.TAKEN else EventStatus.MISSED)
                            a.refresh()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                })
            }
        }
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    // ---- exercise history ---------------------------------------------------

    fun exercises(a: MainActivity): View {
        val today = LocalDate.now()
        val profile = a.store.profile()
        val overrides = a.store.exerciseOverrides()
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Exercise history") { a.popOverlay() })
        col.addView(Ui.caption(a, "Each day's exercise sessions. Tap a day to mark sessions done or not."))
        col.addView(Ui.section(a, "Recent days"))
        val events = a.store.allEvents()
        val sessions = ScheduleEngine.clampSessions(ScheduleEngine.EXERCISE_SESSIONS_PER_DAY)
        for (i in 0 until DAYS) {
            val d = today.minusDays(i.toLong())
            val exs = ScheduleEngine.mergedExercises(PhaseEngine.currentPhase(profile, d).exercises, overrides)
            if (exs.isEmpty()) continue
            val done = (1..sessions).count { s ->
                val slot = "session$s"
                exs.all { ex -> latestStatus(events, d, EventType.EXERCISE, ex.id, slot) == EventStatus.DONE }
            }
            col.addView(Ui.listRow(a, "ic_exercises", d.format(dayFmt), "$done/$sessions sessions done") {
                a.pushOverlay("Sessions · ${d.format(dayFmt)}") { exDay(a, d) }
            })
        }
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    private fun exDay(a: MainActivity, date: LocalDate): View {
        val profile = a.store.profile()
        val overrides = a.store.exerciseOverrides()
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, date.format(dayFmt)) { a.popOverlay() })
        col.addView(Ui.caption(a, "Tap a session to mark every exercise in it done, or undo it."))
        val events = a.store.allEvents()
        val exs = ScheduleEngine.mergedExercises(PhaseEngine.currentPhase(profile, date).exercises, overrides)
        val sessions = ScheduleEngine.clampSessions(ScheduleEngine.EXERCISE_SESSIONS_PER_DAY)
        for (s in 1..sessions) {
            val slot = "session$s"
            val doneCount = exs.count { ex -> latestStatus(events, date, EventType.EXERCISE, ex.id, slot) == EventStatus.DONE }
            val allDone = doneCount == exs.size
            col.addView(Ui.progressRow(a, "Exercise session $s",
                "$doneCount/${exs.size} exercises", doneCount, exs.size) {
                val newStatus = if (allDone) EventStatus.SKIPPED else EventStatus.DONE
                for (ex in exs) writeEvent(a, date, EventType.EXERCISE, ex.id, slot, newStatus)
                a.refresh()
            })
        }
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    // ---- shared -------------------------------------------------------------

    /** Latest recorded status for a (day, type, ref, slot), or null if unlogged. */
    private fun latestStatus(
        events: List<EventLog>, date: LocalDate, type: EventType, refId: String, slot: String
    ): EventStatus? = events.lastOrNull {
        it.date == date && it.type == type && it.refId == refId && it.slotKey == slot
    }?.status

    /** Append a dated correction; latest-by-rowid wins on read. */
    private fun writeEvent(
        a: MainActivity, date: LocalDate, type: EventType, refId: String, slot: String, status: EventStatus
    ) {
        a.store.addEvent(EventLog(UUID.randomUUID().toString(), date, type, refId, slot, status, 0))
    }
}
