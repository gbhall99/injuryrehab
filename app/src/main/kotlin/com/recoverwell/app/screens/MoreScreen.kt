package com.recoverwell.app.screens

import android.view.View
import com.recoverwell.app.MainActivity
import com.recoverwell.app.notify.Reminders
import com.recoverwell.app.ui.Forms
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.PhaseEngine
import com.recoverwell.core.model.*
import com.recoverwell.core.protocol.ProtocolContent
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/** Settings hub: every personal field is editable from here. */
object MoreScreen {

    fun build(a: MainActivity): View {
        val col = Ui.column(a)
        col.addView(Ui.title(a, "Settings & info"))

        fun navCard(title: String, subtitle: String, onClick: () -> Unit) {
            val card = Ui.card(a)
            card.isClickable = true
            card.setOnClickListener { onClick() }
            card.contentDescription = title
            card.addView(Ui.text(a, title, 17f, Ui.TEXT, bold = true))
            card.addView(Ui.text(a, subtitle, 14f, Ui.TEXT_DIM))
            col.addView(card)
        }

        navCard("My injury & goal", "Injury date, side, pathway, consultant visits, goal, wedge plan") {
            a.pushOverlay { profileEditor(a) }
        }
        navCard("Phase dates", "Adjust when each phase starts, as agreed with your physio") {
            a.pushOverlay { phaseDatesEditor(a) }
        }
        navCard("Medications", "Doses, times and reminders") {
            a.pushOverlay { medsEditor(a) }
        }
        navCard("Rehab task reminders", "Elevation, boot checks, circulation checks and custom tasks") {
            a.pushOverlay { tasksEditor(a) }
        }
        navCard("Red flags", "DVT, re-rupture, bleeding - know them cold") {
            a.pushOverlay { RedFlagsScreen.build(a) }
        }
        navCard("About & protocol sources", "What this app is based on, and what it is not") {
            a.pushOverlay { about(a) }
        }

        col.addView(Ui.section(a, "Data"))
        val data = Ui.card(a)
        data.addView(Ui.text(
            a, "All data lives only on this phone (no account, no network access at all). " +
                "Export regularly so you have a backup.", 14f, Ui.TEXT_DIM
        ))
        data.addView(Ui.fullWidth(Ui.secondaryButton(a, "Full backup (JSON)") { a.exportBackup() }, a))
        data.addView(Ui.fullWidth(Ui.secondaryButton(a, "Restore from backup") { a.importBackup() }, a))
        col.addView(data)

        col.addView(Ui.spacer(a, 20))
        return Ui.scroll(a, col)
    }

    // ------------------------------------------------------------------

    fun profileEditor(a: MainActivity, onDone: (() -> Unit)? = null): View {
        var p = a.store.profile()
        val col = Ui.column(a)
        if (onDone == null) col.addView(Ui.secondaryButton(a, "← Back") { a.popOverlay() })
        col.addView(Ui.title(a, "My injury & goal"))
        col.addView(Ui.text(a, "Everything here is editable - the app adapts around it.", 14f, Ui.TEXT_DIM))

        val card = Ui.card(a)
        card.addView(Forms.label(a, "Name (optional)"))
        val nameEdit = Forms.editText(a, p.name, "How should the app greet you?")
        card.addView(nameEdit)

        card.addView(Forms.label(a, "Injury"))
        val descEdit = Forms.editText(a, p.injuryDescription, "What happened", multiline = true)
        card.addView(descEdit)

        card.addView(Forms.dateRow(a, "Injury date", p.injuryDate) { p = p.copy(injuryDate = it) })

        card.addView(Forms.label(a, "Side"))
        card.addView(Forms.choiceRow(a, Side.values().toList(), { it.name.lowercase() }, p.side) {
            p = p.copy(side = it)
        })

        card.addView(Forms.label(a, "Pathway"))
        card.addView(Ui.text(a, "Conservative (non-surgical) - this app only contains the conservative protocol", 15f, Ui.TEXT, bold = true))

        card.addView(Forms.label(a, "Goal"))
        val goalEdit = Forms.editText(a, p.goal, "What are you working back to?")
        card.addView(goalEdit)
        col.addView(card)

        col.addView(Ui.section(a, "Boot & weight-bearing"))
        val bootCard = Ui.card(a)
        bootCard.addView(Forms.stepper(a, "Wedges in boot now", p.currentWedges, 0, 8) { p = p.copy(currentWedges = it) })
        bootCard.addView(Forms.stepper(a, "Wedges fitted at start", p.wedgePlan.initialWedges, 1, 8) {
            p = p.copy(wedgePlan = p.wedgePlan.copy(initialWedges = it))
        })
        bootCard.addView(Forms.stepper(a, "First removal (week of injury +)", p.wedgePlan.removalStartWeek, 1, 12) {
            p = p.copy(wedgePlan = p.wedgePlan.copy(removalStartWeek = it))
        })
        bootCard.addView(Forms.stepper(a, "Days between removals", p.wedgePlan.removalIntervalDays, 3, 28) {
            p = p.copy(wedgePlan = p.wedgePlan.copy(removalIntervalDays = it))
        })
        bootCard.addView(Ui.text(a, "Default: one wedge weekly from week 3 (typical NHS plan). Match whatever YOUR clinic prescribed.", 13f, Ui.WARN))
        bootCard.addView(Forms.label(a, "Weight-bearing status"))
        bootCard.addView(Forms.choiceRow(a, WeightBearing.values().toList(), {
            it.label.replace("weight-bearing", "WB").replace("Weight-bearing", "WB")
        }, p.weightBearing) { p = p.copy(weightBearing = it) })
        col.addView(bootCard)

        col.addView(Ui.section(a, "Appointments"))
        val apptCard = Ui.card(a)
        p.appointments.forEachIndexed { i, appt ->
            val row = Ui.row(a)
            row.addView(Ui.weight(Ui.text(a, "${appt.date} · ${appt.label}" + if (appt.completed) " ✓" else "", 15f), 1f))
            row.addView(Ui.secondaryButton(a, if (appt.completed) "Reopen" else "Done") {
                val list = p.appointments.toMutableList()
                list[i] = appt.copy(completed = !appt.completed)
                p = p.copy(appointments = list)
                a.store.saveProfile(p)
                a.refresh()
            })
            row.addView(Ui.secondaryButton(a, "✕") {
                p = p.copy(appointments = p.appointments.filterIndexed { j, _ -> j != i })
                a.store.saveProfile(p)
                a.refresh()
            })
            apptCard.addView(row)
        }
        var newDate = LocalDate.now()
        apptCard.addView(Forms.dateRow(a, "New appointment date", newDate) { newDate = it })
        val newLabel = Forms.editText(a, "", "e.g. Physio review")
        apptCard.addView(newLabel)
        apptCard.addView(Ui.fullWidth(Ui.secondaryButton(a, "Add appointment") {
            val label = newLabel.text.toString().ifBlank { "Appointment" }
            p = p.copy(appointments = p.appointments + Appointment(newDate, label, false))
            a.store.saveProfile(p)
            a.refresh()
        }, a))
        col.addView(apptCard)

        col.addView(Ui.spacer(a, 10))
        col.addView(Ui.fullWidth(Ui.button(a, if (onDone == null) "Save" else "Confirm & continue") {
            a.store.saveProfile(p.copy(
                name = nameEdit.text.toString().trim(),
                injuryDescription = descEdit.text.toString().trim(),
                goal = goalEdit.text.toString().trim()
            ))
            Reminders.reschedule(a)
            if (onDone != null) onDone() else a.popOverlay()
        }, a))
        col.addView(Ui.spacer(a, 20))
        return Ui.scroll(a, col)
    }

    // ------------------------------------------------------------------

    private fun phaseDatesEditor(a: MainActivity): View {
        val col = Ui.column(a)
        col.addView(Ui.secondaryButton(a, "← Back") { a.popOverlay() })
        col.addView(Ui.title(a, "Phase start dates"))
        col.addView(Ui.text(
            a, "Defaults follow the typical conservative protocol, anchored to your " +
                "injury date. Override them only to match what your physio has agreed.",
            14f, Ui.WARN
        ))
        val profile = a.store.profile()
        for (phase in ProtocolContent.phases) {
            val card = Ui.card(a)
            card.addView(Ui.text(a, "Phase ${phase.number}: ${phase.title}", 16f, Ui.TEXT, bold = true))
            val defaultDate = profile.injuryDate.plusWeeks(phase.startWeek.toLong())
            val overridden = profile.phaseStartOverrides[phase.number]
            card.addView(Ui.text(
                a, "Default: $defaultDate (week ${phase.startWeek})" +
                    if (overridden != null) " · overridden to $overridden" else "",
                14f, Ui.TEXT_DIM
            ))
            card.addView(Forms.dateRow(a, "Starts", overridden ?: defaultDate) { newDate ->
                val p2 = a.store.profile()
                a.store.saveProfile(p2.copy(phaseStartOverrides = p2.phaseStartOverrides + (phase.number to newDate)))
                Reminders.reschedule(a)
            })
            if (overridden != null) {
                card.addView(Ui.fullWidth(Ui.secondaryButton(a, "Clear override") {
                    val p2 = a.store.profile()
                    a.store.saveProfile(p2.copy(phaseStartOverrides = p2.phaseStartOverrides - phase.number))
                    Reminders.reschedule(a)
                    a.refresh()
                }, a))
            }
            col.addView(card)
        }
        val confirmCard = Ui.card(a)
        val current = a.store.profile().physioConfirmedPhase
        confirmCard.addView(Ui.text(a, "Physio-confirmed phase: $current", 16f, Ui.TEXT, bold = true))
        confirmCard.addView(Ui.text(
            a, "If you progressed by mistake, you can wind this back.", 14f, Ui.TEXT_DIM
        ))
        confirmCard.addView(Forms.stepper(a, "Confirmed up to phase", current, 1, 5) { v ->
            a.store.saveProfile(a.store.profile().copy(physioConfirmedPhase = v))
            Reminders.reschedule(a)
        })
        col.addView(confirmCard)
        col.addView(Ui.spacer(a, 20))
        return Ui.scroll(a, col)
    }

    // ------------------------------------------------------------------

    fun medsEditor(a: MainActivity, onDone: (() -> Unit)? = null): View {
        val col = Ui.column(a)
        if (onDone == null) col.addView(Ui.secondaryButton(a, "← Back") { a.popOverlay() })
        col.addView(Ui.title(a, "Medications"))
        col.addView(Ui.text(
            a, "Each time gets its own reminder with taken/missed logging. " +
                "Change doses only with your prescriber.", 14f, Ui.TEXT_DIM
        ))

        for (med in a.store.medications()) {
            val card = Ui.card(a)
            card.addView(Ui.text(a, "${med.name} ${med.dose}" + if (!med.active) " (paused)" else "", 17f, Ui.TEXT, bold = true))
            card.addView(Ui.text(a, "Reminders: ${med.times.joinToString(", ")}", 14f, Ui.TEXT_DIM))
            if (med.notes.isNotBlank()) card.addView(Ui.text(a, med.notes, 13f, Ui.TEXT_DIM))
            val row = Ui.row(a)
            row.addView(Ui.weight(Ui.secondaryButton(a, "Edit") {
                a.pushOverlay { medEditor(a, med) }
            }, 1f))
            row.addView(Ui.weight(Ui.secondaryButton(a, if (med.active) "Pause" else "Resume") {
                a.store.saveMedications(a.store.medications().map {
                    if (it.id == med.id) it.copy(active = !it.active) else it
                })
                Reminders.reschedule(a)
                a.refresh()
            }, 1f))
            row.addView(Ui.weight(Ui.secondaryButton(a, "Delete") {
                Forms.confirm(a, "Delete ${med.name}?", "Past taken/missed history is kept.") {
                    a.store.saveMedications(a.store.medications().filter { it.id != med.id })
                    Reminders.reschedule(a)
                    a.refresh()
                }
            }, 1f))
            card.addView(row)
            col.addView(card)
        }

        col.addView(Ui.fullWidth(Ui.button(a, "Add medication") {
            a.pushOverlay {
                medEditor(a, Medication(UUID.randomUUID().toString(), "", "", listOf(LocalTime.of(9, 0)), "", true))
            }
        }, a))
        if (onDone != null) {
            col.addView(Ui.fullWidth(Ui.button(a, "Confirm & continue") { onDone() }, a))
        }
        col.addView(Ui.spacer(a, 20))
        return Ui.scroll(a, col)
    }

    private fun medEditor(a: MainActivity, med: Medication): View {
        var times = med.times.toMutableList()
        val col = Ui.column(a)
        col.addView(Ui.secondaryButton(a, "← Back") { a.popOverlay() })
        col.addView(Ui.title(a, if (med.name.isBlank()) "New medication" else "Edit ${med.name}"))

        val card = Ui.card(a)
        card.addView(Forms.label(a, "Name"))
        val nameEdit = Forms.editText(a, med.name, "e.g. Anticoagulant")
        card.addView(nameEdit)
        card.addView(Forms.label(a, "Dose"))
        val doseEdit = Forms.editText(a, med.dose, "e.g. 2.5 mg")
        card.addView(doseEdit)
        card.addView(Forms.label(a, "Notes"))
        val notesEdit = Forms.editText(a, med.notes, "Anything to remember", multiline = true)
        card.addView(notesEdit)
        col.addView(card)

        col.addView(Ui.section(a, "Reminder times"))
        val timesCard = Ui.card(a)
        fun rebuildTimes() {
            timesCard.removeAllViews()
            times.sorted().forEachIndexed { i, t ->
                val row = Ui.row(a)
                row.addView(Ui.weight(Forms.timeButton(a, t) { new ->
                    times[i] = new
                }, 1f))
                row.addView(Ui.secondaryButton(a, "Remove") {
                    times.removeAt(i)
                    rebuildTimes()
                })
                timesCard.addView(row)
            }
            timesCard.addView(Ui.fullWidth(Ui.secondaryButton(a, "Add a time") {
                times.add(LocalTime.of(12, 0))
                rebuildTimes()
            }, a))
        }
        rebuildTimes()
        col.addView(timesCard)

        col.addView(Ui.fullWidth(Ui.button(a, "Save") {
            val name = nameEdit.text.toString().trim().ifBlank { "Medication" }
            val updated = med.copy(
                name = name,
                dose = doseEdit.text.toString().trim(),
                notes = notesEdit.text.toString().trim(),
                times = times.sorted().ifEmpty { listOf(LocalTime.of(9, 0)) }
            )
            val all = a.store.medications()
            a.store.saveMedications(
                if (all.any { it.id == med.id }) all.map { if (it.id == med.id) updated else it }
                else all + updated
            )
            Reminders.reschedule(a)
            a.popOverlay()
        }, a))
        col.addView(Ui.spacer(a, 20))
        return Ui.scroll(a, col)
    }

    // ------------------------------------------------------------------

    private fun tasksEditor(a: MainActivity): View {
        val col = Ui.column(a)
        col.addView(Ui.secondaryButton(a, "← Back") { a.popOverlay() })
        col.addView(Ui.title(a, "Rehab task reminders"))
        col.addView(Ui.text(
            a, "Wedge-change reminders come from your wedge plan automatically " +
                "(edit it under My injury & goal).", 14f, Ui.TEXT_DIM
        ))

        for (task in a.store.tasks()) {
            val card = Ui.card(a)
            card.addView(Ui.text(a, task.title + if (!task.active) " (off)" else "", 17f, Ui.TEXT, bold = true))
            card.addView(Ui.text(a, task.detail, 14f, Ui.TEXT_DIM))
            card.addView(Ui.text(
                a, "Times: ${task.times.joinToString(", ")} · phases ${task.fromPhase}-${task.toPhase}",
                13f, Ui.TEXT_DIM
            ))
            val row = Ui.row(a)
            row.addView(Ui.weight(Ui.secondaryButton(a, "Edit") { a.pushOverlay { taskEditor(a, task) } }, 1f))
            row.addView(Ui.weight(Ui.secondaryButton(a, if (task.active) "Turn off" else "Turn on") {
                a.store.saveTasks(a.store.tasks().map {
                    if (it.id == task.id) it.copy(active = !it.active) else it
                })
                Reminders.reschedule(a)
                a.refresh()
            }, 1f))
            row.addView(Ui.weight(Ui.secondaryButton(a, "Delete") {
                Forms.confirm(a, "Delete task?", "\"${task.title}\" will stop appearing and reminding.") {
                    a.store.saveTasks(a.store.tasks().filter { it.id != task.id })
                    Reminders.reschedule(a)
                    a.refresh()
                }
            }, 1f))
            card.addView(row)
            col.addView(card)
        }

        col.addView(Ui.fullWidth(Ui.button(a, "Add custom task") {
            a.pushOverlay {
                taskEditor(a, RehabTask(
                    UUID.randomUUID().toString(), TaskKind.CUSTOM, "", "",
                    listOf(LocalTime.of(9, 0)), 1, 5, null, true
                ))
            }
        }, a))
        col.addView(Ui.spacer(a, 20))
        return Ui.scroll(a, col)
    }

    private fun taskEditor(a: MainActivity, task: RehabTask): View {
        var times = task.times.toMutableList()
        var fromPhase = task.fromPhase
        var toPhase = task.toPhase
        val col = Ui.column(a)
        col.addView(Ui.secondaryButton(a, "← Back") { a.popOverlay() })
        col.addView(Ui.title(a, if (task.title.isBlank()) "New task" else "Edit task"))

        val card = Ui.card(a)
        card.addView(Forms.label(a, "Title"))
        val titleEdit = Forms.editText(a, task.title, "e.g. Ice / compression check")
        card.addView(titleEdit)
        card.addView(Forms.label(a, "Detail"))
        val detailEdit = Forms.editText(a, task.detail, "What exactly to do", multiline = true)
        card.addView(detailEdit)
        card.addView(Forms.stepper(a, "From phase", fromPhase, 1, 5) { fromPhase = it })
        card.addView(Forms.stepper(a, "To phase", toPhase, 1, 5) { toPhase = it })
        col.addView(card)

        col.addView(Ui.section(a, "Reminder times"))
        val timesCard = Ui.card(a)
        fun rebuildTimes() {
            timesCard.removeAllViews()
            times.sorted().forEachIndexed { i, t ->
                val row = Ui.row(a)
                row.addView(Ui.weight(Forms.timeButton(a, t) { new -> times[i] = new }, 1f))
                row.addView(Ui.secondaryButton(a, "Remove") { times.removeAt(i); rebuildTimes() })
                timesCard.addView(row)
            }
            timesCard.addView(Ui.fullWidth(Ui.secondaryButton(a, "Add a time") {
                times.add(LocalTime.of(12, 0)); rebuildTimes()
            }, a))
        }
        rebuildTimes()
        col.addView(timesCard)

        col.addView(Ui.fullWidth(Ui.button(a, "Save") {
            val updated = task.copy(
                title = titleEdit.text.toString().trim().ifBlank { "Task" },
                detail = detailEdit.text.toString().trim(),
                times = times.sorted().ifEmpty { listOf(LocalTime.of(9, 0)) },
                fromPhase = minOf(fromPhase, toPhase),
                toPhase = maxOf(fromPhase, toPhase)
            )
            val all = a.store.tasks()
            a.store.saveTasks(
                if (all.any { it.id == task.id }) all.map { if (it.id == task.id) updated else it }
                else all + updated
            )
            Reminders.reschedule(a)
            a.popOverlay()
        }, a))
        col.addView(Ui.spacer(a, 20))
        return Ui.scroll(a, col)
    }

    // ------------------------------------------------------------------

    private fun about(a: MainActivity): View {
        val col = Ui.column(a)
        col.addView(Ui.secondaryButton(a, "← Back") { a.popOverlay() })
        col.addView(Ui.title(a, "About RecoverWell"))

        val disc = Ui.card(a, Ui.WARN_BG)
        disc.addView(Ui.text(a, "What this app is - and is not", 17f, Ui.WARN, bold = true))
        disc.addView(Ui.text(a, ProtocolContent.DISCLAIMER, 15f))
        col.addView(disc)

        col.addView(Ui.section(a, "Protocol basis"))
        val proto = Ui.card(a)
        proto.addView(Ui.text(a, ProtocolContent.PROTOCOL_NAME, 16f, Ui.TEXT, bold = true))
        proto.addView(Ui.text(
            a,
            "Content is modelled on established UK conservative (non-surgical) functional " +
                "rehabilitation pathways for Achilles tendon rupture:\n\n" +
                "•  UKSTAR trial (Costa et al., The Lancet 2020) - early functional bracing " +
                "with immediate weight-bearing as tolerated\n" +
                "•  NHS trust non-operative pathways (e.g. University Hospitals Coventry & " +
                "Warwickshire; Cambridge University Hospitals): boot in full equinus, wedge " +
                "removal from ~week 3, neutral by ~week 8, boot off ~weeks 8-10, no calf " +
                "stretching before week 12\n" +
                "•  Return to sport typically from 6 months; racquet/court sports such as " +
                "padel usually 9-12 months with physio sign-off\n\n" +
                "No post-surgical content is included. Every timeline in the app is a " +
                "typical-protocol placeholder for your own clinic's plan.",
            14f
        ))
        col.addView(proto)

        col.addView(Ui.section(a, "Privacy"))
        val priv = Ui.card(a)
        priv.addView(Ui.text(
            a,
            "RecoverWell holds no account, sends nothing anywhere and requests no " +
                "network permission - the operating system enforces that it cannot reach " +
                "the internet. Your data leaves the phone only when you export it.",
            14f
        ))
        col.addView(priv)

        col.addView(Ui.spacer(a, 20))
        return Ui.scroll(a, col)
    }
}
