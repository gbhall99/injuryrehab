package com.recoverwell.app.screens

import android.app.AlertDialog
import android.view.View
import com.recoverwell.app.MainActivity
import com.recoverwell.app.notify.Reminders
import com.recoverwell.app.ui.Forms
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.Capability
import com.recoverwell.core.logic.PhaseEngine
import com.recoverwell.core.logic.ScheduleEngine
import com.recoverwell.core.model.EventStatus
import com.recoverwell.core.protocol.ProtocolContent
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Unified daily view: phase banner, risk warnings, gate prompt, full checklist. */
object TodayScreen {

    fun build(a: MainActivity): View {
        val today = LocalDate.now()
        val profile = a.store.profile()
        val col = Ui.column(a)

        val phase = PhaseEngine.currentPhase(profile, today)
        val week = PhaseEngine.weeksSinceInjury(profile, today)
        val day = PhaseEngine.daysSinceInjury(profile, today)

        col.addView(Ui.title(a, today.format(DateTimeFormatter.ofPattern("EEEE d MMMM"))))
        col.addView(Ui.text(a, "Week $week · day $day since injury", 16f, Ui.TEXT_DIM))

        // urgent/warning cards first
        val recentLogs = a.store.allLogs().filter { !it.date.isBefore(today.minusDays(7)) }
        val warnings = Capability.warnings(profile, recentLogs, today)
        for (w in warnings.filter { it.severity != Capability.Severity.INFO }) {
            val card = Ui.card(a, if (w.severity == Capability.Severity.URGENT) Ui.DANGER_BG else Ui.WARN_BG)
            card.addView(Ui.text(a, w.title, 17f, if (w.severity == Capability.Severity.URGENT) Ui.DANGER else Ui.WARN, bold = true))
            card.addView(Ui.text(a, w.detail, 15f))
            if (w.severity == Capability.Severity.URGENT) {
                card.addView(Ui.fullWidth(Ui.dangerButton(a, "Open red flag guidance") {
                    a.pushOverlay { RedFlagsScreen.build(a) }
                }, a))
            }
            col.addView(card)
        }

        // phase banner
        val phaseCard = Ui.card(a)
        phaseCard.addView(Ui.text(a, "PHASE ${phase.number} OF ${ProtocolContent.phases.size}", 13f, Ui.PRIMARY, bold = true))
        phaseCard.addView(Ui.text(a, phase.title, 20f, Ui.TEXT, bold = true))
        phaseCard.addView(Ui.text(a, phase.subtitle, 15f, Ui.TEXT_DIM))
        phaseCard.addView(Ui.text(a, ProtocolContent.PLACEHOLDER_NOTE, 13f, Ui.WARN))
        phaseCard.addView(Ui.fullWidth(Ui.secondaryButton(a, "Phase details, goals & precautions") {
            a.pushOverlay { phaseDetail(a, phase.number) }
        }, a))
        col.addView(phaseCard)

        // progression gate
        val gate = PhaseEngine.nextPhaseGate(profile, today)
        if (gate.nextPhase != null) {
            val gateCard = Ui.card(a, if (gate.readyToConfirm) Ui.INFO_BG else Ui.CARD)
            if (gate.readyToConfirm) {
                gateCard.addView(Ui.text(a, "Phase ${gate.nextPhase!!.number} dates reached", 17f, Ui.TEXT, bold = true))
                gateCard.addView(Ui.text(
                    a,
                    "The typical timeline says \"${gate.nextPhase!!.title}\" could begin " +
                        "(${gate.startDate}). Speak to your physio first - only they can " +
                        "confirm you are ready.",
                    15f
                ))
                gateCard.addView(Ui.fullWidth(Ui.button(a, "My physio confirmed phase ${gate.nextPhase!!.number}") {
                    Forms.confirm(
                        a, "Confirm progression",
                        "Has your physiotherapist explicitly confirmed you should start " +
                            "phase ${gate.nextPhase!!.number} (${gate.nextPhase!!.title})?"
                    ) {
                        a.store.saveProfile(a.store.profile().copy(physioConfirmedPhase = gate.nextPhase!!.number))
                        Reminders.reschedule(a)
                        a.refresh()
                    }
                }, a))
            } else if (!gate.dateEligible) {
                gateCard.addView(Ui.text(
                    a,
                    "Next: phase ${gate.nextPhase!!.number} - ${gate.nextPhase!!.title} · " +
                        "typically from ${gate.startDate} (${gate.daysUntilEligible} days). " +
                        "Your physio may adjust this.",
                    14f, Ui.TEXT_DIM
                ))
            }
            if (gateCard.childCount > 0) col.addView(gateCard)
        }

        // checklist
        val items = ScheduleEngine.dailyChecklist(
            profile, a.store.medications(), a.store.tasks(),
            a.store.exerciseOverrides(), a.store.eventsOn(today), today
        )
        val doneCount = items.count { it.isDone }
        col.addView(Ui.section(a, "Today's checklist · $doneCount of ${items.size} done"))

        fun addGroup(label: String, kinds: Set<ScheduleEngine.ItemKind>) {
            val group = items.filter { it.kind in kinds }
            if (group.isEmpty()) return
            col.addView(Ui.text(a, label, 15f, Ui.TEXT_DIM, bold = true).apply {
                setPadding(0, Ui.dp(a, 10), 0, Ui.dp(a, 2))
            })
            for (item in group) {
                val timeLabel = item.time?.let { "%02d:%02d · ".format(it.hour, it.minute) } ?: ""
                val statusLabel = when (item.status) {
                    EventStatus.TAKEN -> "Taken"
                    EventStatus.DONE -> "Done"
                    EventStatus.MISSED -> "Marked missed"
                    EventStatus.SKIPPED -> "Skipped"
                    null -> null
                }
                col.addView(Ui.checkRow(a, item.title, timeLabel + item.subtitle, item.isDone, statusLabel) {
                    onItemTapped(a, item)
                })
            }
        }
        addGroup("Medication", setOf(ScheduleEngine.ItemKind.MEDICATION))
        addGroup("Rehab tasks", setOf(ScheduleEngine.ItemKind.TASK, ScheduleEngine.ItemKind.WEDGE_CHANGE))
        addGroup("Exercise sessions (tap for demonstration)", setOf(ScheduleEngine.ItemKind.EXERCISE))

        col.addView(Ui.spacer(a, 20))
        return Ui.scroll(a, col)
    }

    private fun onItemTapped(a: MainActivity, item: ScheduleEngine.ChecklistItem) {
        when (item.kind) {
            ScheduleEngine.ItemKind.MEDICATION -> {
                AlertDialog.Builder(a)
                    .setTitle(item.title)
                    .setMessage("Record this ${item.slotKey} dose. Clot prevention is clinically important - if in doubt about a missed dose, ask your pharmacist or 111; never double up.")
                    .setPositiveButton("Taken") { _, _ -> record(a, item, EventStatus.TAKEN) }
                    .setNegativeButton("Missed") { _, _ -> record(a, item, EventStatus.MISSED) }
                    .setNeutralButton("Cancel", null)
                    .show()
            }
            ScheduleEngine.ItemKind.EXERCISE -> {
                val spec = ProtocolContent.phases.flatMap { it.exercises }.find { it.id == item.refId }
                if (spec != null) {
                    a.pushOverlay { ExercisesScreen.exerciseDetail(a, spec, item.slotKey) }
                } else {
                    record(a, item, EventStatus.DONE)
                }
            }
            else -> {
                if (item.isDone) {
                    Forms.confirm(a, "Undo", "Mark \"${item.title}\" as not done?") {
                        record(a, item, EventStatus.SKIPPED)
                    }
                } else record(a, item, EventStatus.DONE)
            }
        }
    }

    fun record(a: MainActivity, item: ScheduleEngine.ChecklistItem, status: EventStatus) {
        Reminders.recordEvent(a, item.kind, item.refId, item.slotKey, status)
        a.refresh()
    }

    fun phaseDetail(a: MainActivity, phaseNumber: Int): View {
        val phase = ProtocolContent.phase(phaseNumber)
        val col = Ui.column(a)
        col.addView(Ui.secondaryButton(a, "← Back") { a.popOverlay() })
        col.addView(Ui.title(a, "Phase ${phase.number}: ${phase.title}"))
        col.addView(Ui.text(a, phase.subtitle, 16f, Ui.TEXT_DIM))
        col.addView(Ui.text(a, ProtocolContent.PLACEHOLDER_NOTE, 13f, Ui.WARN))

        fun bullets(heading: String, lines: List<String>, color: Int = Ui.TEXT) {
            col.addView(Ui.section(a, heading))
            val card = Ui.card(a)
            for (l in lines) card.addView(Ui.text(a, "•  $l", 15f, color))
            col.addView(card)
        }
        bullets("Entry criteria", phase.entryCriteria)
        bullets("Goals", phase.goals)
        bullets("Precautions", phase.precautions, Ui.WARN)
        bullets("OK in this phase", phase.allowed, Ui.DONE)
        bullets("Not yet", phase.notAllowed, Ui.DANGER)
        col.addView(Ui.spacer(a, 20))
        return Ui.scroll(a, col)
    }
}
