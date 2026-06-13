package com.recoverwell.app.screens

import android.view.View
import com.recoverwell.app.MainActivity
import com.recoverwell.app.notify.Reminders
import com.recoverwell.app.ui.Forms
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.model.*
import com.recoverwell.core.protocol.Defaults
import com.recoverwell.core.protocol.ProtocolRegistry
import com.recoverwell.core.protocol.RehabFramework
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/** Settings hub: every personal field is editable from here. */
object MoreScreen {

    fun build(a: MainActivity): View {
        val col = Ui.column(a)

        col.addView(Ui.section(a, "My recovery"))
        col.addView(Ui.listRow(a, "ic_heart", "Injury & goal",
            "Dates, side, boot plan, appointments") { a.pushOverlay { profileEditor(a) } })
        col.addView(Ui.listRow(a, "ic_heart", "How you're doing",
            "What's normal to feel, reassurance, milestones") { a.pushOverlay { WellbeingScreen.build(a) } })
        col.addView(Ui.listRow(a, "ic_calendar", "Phase dates",
            "Adjust timings agreed with your physio") { a.pushOverlay { phaseDatesEditor(a) } })
        col.addView(Ui.listRow(a, "ic_edit", "Physio visits",
            "Appointment pack, sign-offs and visit notes") { a.pushOverlay { PhysioScreen.build(a) } })
        col.addView(Ui.listRow(a, "ic_pill", "Medications",
            "Doses, times and reminders") { a.pushOverlay { medsEditor(a) } })
        col.addView(Ui.listRow(a, "ic_bell", "Daily care reminders",
            "Elevation, boot checks, circulation checks") { a.pushOverlay { tasksEditor(a) } })
        val exTime = Reminders.parseTime(a.store.setting("exercise_reminder", "10:00"))
        col.addView(Ui.listRow(a, "ic_exercises", "Exercise reminders",
            if (exTime == null) "Off"
            else "Daily nudge at %02d:%02d".format(exTime.hour, exTime.minute)) {
            a.pushOverlay { exerciseReminderEditor(a) }
        })

        col.addView(Ui.section(a, "Appearance"))
        val themeCard = Ui.card(a)
        val current = a.store.setting("appearance", "system")
        themeCard.addView(Forms.choiceRow(
            a, listOf("system", "light", "dark"),
            { it.replaceFirstChar { c -> c.uppercase() } }, current
        ) { choice ->
            a.store.saveSetting("appearance", choice)
            a.recreate()
        })
        col.addView(themeCard)

        col.addView(Ui.section(a, "Notifications"))
        val blocked = com.recoverwell.app.notify.ReminderHealth.deliveryBlocked(a)
        val anyIssue = com.recoverwell.app.notify.ReminderHealth.hasIssue(a)
        col.addView(Ui.listRow(a, "ic_bell", "Reminder reliability",
            if (blocked) "Action needed - reminders may not arrive"
            else if (anyIssue) "Mostly fine - one setting could be improved"
            else "All clear - test it any time",
            iconTint = if (blocked) Ui.DANGER else Ui.PRIMARY,
            iconBg = if (blocked) Ui.DANGER_BG else Ui.PRIMARY_CONTAINER) {
            a.pushOverlay { reminderHealth(a) }
        })

        col.addView(Ui.section(a, "Safety & info"))
        col.addView(Ui.listRow(a, "ic_alert", "Red flags",
            "DVT, re-rupture, bleeding - know them cold",
            iconTint = Ui.DANGER, iconBg = Ui.DANGER_BG) { a.pushOverlay { RedFlagsScreen.build(a) } })
        col.addView(Ui.listRow(a, "ic_info", "About & protocol sources",
            "What this app is based on") { a.pushOverlay { about(a) } })

        col.addView(Ui.section(a, "Data"))
        val lastBackup = a.store.setting("last_backup", "")
        col.addView(Ui.caption(a, "All data lives only on this phone - no account, no network. " +
            if (lastBackup.isBlank()) "No backup yet - turn on automatic backup or export one below."
            else "Last full backup: $lastBackup."))
        col.addView(Ui.spacer(a, 6))
        val autoOn = a.autoBackupEnabled()
        col.addView(Ui.listRow(a, "ic_restore", "Automatic backup",
            if (autoOn) "On · saves once a day to ${a.store.setting("auto_backup_name", "your file")}"
            else "Off · save a fresh copy daily, no action needed",
            iconTint = if (autoOn) Ui.PRIMARY else Ui.TEXT_DIM,
            iconBg = if (autoOn) Ui.PRIMARY_CONTAINER else Ui.SURFACE_HIGH) {
            a.pushOverlay { autoBackupEditor(a) }
        })
        col.addView(Ui.listRow(a, "ic_export", "Full backup · JSON", "Everything, restorable") { a.exportBackup() })
        col.addView(Ui.listRow(a, "ic_restore", "Restore from backup", "Replaces all current data") { a.importBackup() })

        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    // ------------------------------------------------------------------

    fun profileEditor(a: MainActivity, onDone: (() -> Unit)? = null): View {
        var p = a.store.profile()
        val col = Ui.column(a)
        if (onDone == null) col.addView(Ui.backRow(a, "Injury & goal") { a.popOverlay() })

        val card = Ui.card(a)
        card.addView(Forms.label(a, "Name · optional"))
        val nameEdit = Forms.editText(a, p.name, "How should the app greet you?")
        card.addView(nameEdit)

        card.addView(Forms.label(a, "Injury"))
        val descEdit = Forms.editText(a, p.injuryDescription, "What happened", multiline = true)
        card.addView(descEdit)

        card.addView(Ui.spacer(a, 6))
        card.addView(Forms.dateRow(a, "Injury date", p.injuryDate) { p = p.copy(injuryDate = it) })

        card.addView(Forms.label(a, "Side"))
        card.addView(Forms.choiceRow(a, Side.values().toList(), {
            it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
        }, p.side) { p = p.copy(side = it) })

        card.addView(Forms.label(a, "Injury & protocol"))
        card.addView(Forms.choiceRow(a, ProtocolRegistry.all.map { it.id }, { id ->
            ProtocolRegistry.byId(id).injuryName
        }, p.protocolId) { id ->
            val proto = ProtocolRegistry.byId(id)
            p = p.copy(
                protocolId = id,
                wedgePlan = proto.supportDevice?.plan ?: p.wedgePlan,
                currentWedges = proto.supportDevice?.plan?.initialWedges ?: 0
            )
        })
        card.addView(Ui.caption(a, ProtocolRegistry.byId(p.protocolId).variantName +
            " · more protocols can be added to the registry"))

        card.addView(Forms.label(a, "Goal"))
        val goalEdit = Forms.editText(a, p.goal, "What are you working back to?")
        card.addView(goalEdit)
        col.addView(card)

        val device = ProtocolRegistry.byId(p.protocolId).supportDevice
        col.addView(Ui.section(a, (device?.name ?: "Support") + " & weight-bearing"))
        val bootCard = Ui.card(a)
        if (device != null) {
            val units = device.unitNamePlural
            val step = device.plan.stepSize.coerceAtLeast(1)
            bootCard.addView(Forms.stepper(a, "Setting now ($units)",
                p.currentWedges, 0, device.maxValue, step = step) { p = p.copy(currentWedges = it) })
            bootCard.addView(Forms.stepper(a, "Setting at start ($units)",
                p.wedgePlan.initialWedges, 0, device.maxValue, step = step) {
                p = p.copy(wedgePlan = p.wedgePlan.copy(initialWedges = it))
            })
            bootCard.addView(Forms.stepper(a, "Reduce by each change ($units)",
                p.wedgePlan.stepSize, 1, device.maxValue) {
                p = p.copy(wedgePlan = p.wedgePlan.copy(stepSize = it))
            })
            bootCard.addView(Forms.stepper(a, "First change · week", p.wedgePlan.removalStartWeek, 1, 12) {
                p = p.copy(wedgePlan = p.wedgePlan.copy(removalStartWeek = it))
            })
            bootCard.addView(Forms.stepper(a, "Days between changes", p.wedgePlan.removalIntervalDays, 3, 28) {
                p = p.copy(wedgePlan = p.wedgePlan.copy(removalIntervalDays = it))
            })
            bootCard.addView(Ui.spacer(a, 4))
            bootCard.addView(Ui.caption(a, "Default: lower by ${device.plan.stepSize} $units every " +
                "${device.plan.removalIntervalDays} days from week ${device.plan.removalStartWeek}. " +
                "Match whatever your clinic prescribed."))
        }
        bootCard.addView(Forms.label(a, "Weight-bearing"))
        bootCard.addView(Forms.choiceRow(a, WeightBearing.values().toList(), { it.shortLabel }, p.weightBearing) {
            p = p.copy(weightBearing = it)
        })
        col.addView(bootCard)

        col.addView(Ui.section(a, "Appointments"))
        val apptCard = Ui.card(a)
        // appointment edits rebuild the screen, so persist any typed-but-unsaved
        // text fields first or they would be lost
        fun saveWithTexts() {
            a.store.saveProfile(p.copy(
                name = nameEdit.text.toString().trim(),
                injuryDescription = descEdit.text.toString().trim(),
                goal = goalEdit.text.toString().trim()
            ))
            a.refresh()
        }
        p.appointments.forEachIndexed { i, appt ->
            val row = Ui.row(a)
            val texts = Ui.text(a, "${appt.date} · ${appt.label}" + if (appt.completed) "  ✓" else "", 14.5f)
            row.addView(Ui.weight(texts, 1f))
            row.addView(Ui.textButton(a, if (appt.completed) "Reopen" else "Done") {
                val list = p.appointments.toMutableList()
                list[i] = appt.copy(completed = !appt.completed)
                p = p.copy(appointments = list)
                saveWithTexts()
            })
            row.addView(Ui.iconButton(a, "ic_close", Ui.TEXT_DIM, desc = "Remove appointment") {
                p = p.copy(appointments = p.appointments.filterIndexed { j, _ -> j != i })
                saveWithTexts()
            })
            apptCard.addView(row)
        }
        var newDate = LocalDate.now()
        apptCard.addView(Forms.dateRow(a, "New appointment", newDate) { newDate = it })
        val newLabel = Forms.editText(a, "", "e.g. Physio review")
        apptCard.addView(newLabel)
        apptCard.addView(Ui.fullWidth(Ui.tonalButton(a, "Add appointment") {
            val label = newLabel.text.toString().ifBlank { "Appointment" }
            p = p.copy(appointments = p.appointments + Appointment(newDate, label, false))
            saveWithTexts()
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
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    // ------------------------------------------------------------------

    fun phaseDatesEditor(a: MainActivity): View {
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Phase dates") { a.popOverlay() })
        col.addView(Ui.caption(a, "Defaults follow the typical conservative protocol, anchored to " +
            "your injury date. Override them only to match what your physio agreed."))
        val profile = a.store.profile()
        for (phase in ProtocolRegistry.forProfile(profile).phases) {
            val card = Ui.card(a)
            card.addView(Ui.text(a, "Phase ${phase.number} · ${phase.title}", 15.5f, Ui.TEXT, bold = true))
            val defaultDate = profile.injuryDate.plusWeeks(phase.startWeek.toLong())
            val overridden = profile.phaseStartOverrides[phase.number]
            card.addView(Ui.caption(a, "Default $defaultDate (week ${phase.startWeek})" +
                if (overridden != null) " · overridden" else ""))
            card.addView(Ui.spacer(a, 4))
            card.addView(Forms.dateRow(a, "Starts", overridden ?: defaultDate) { newDate ->
                val p2 = a.store.profile()
                a.store.saveProfile(p2.copy(phaseStartOverrides = p2.phaseStartOverrides + (phase.number to newDate)))
                Reminders.reschedule(a)
            })
            if (overridden != null) {
                card.addView(Ui.fullWidth(Ui.textButton(a, "Clear override") {
                    val p2 = a.store.profile()
                    a.store.saveProfile(p2.copy(phaseStartOverrides = p2.phaseStartOverrides - phase.number))
                    Reminders.reschedule(a)
                    a.refresh()
                }, a, 2))
            }
            col.addView(card)
        }
        val confirmCard = Ui.card(a)
        val current = a.store.profile().physioConfirmedPhase
        confirmCard.addView(Ui.text(a, "Physio-confirmed phase", 15.5f, Ui.TEXT, bold = true))
        confirmCard.addView(Ui.caption(a, "If you progressed by mistake, wind this back."))
        confirmCard.addView(Forms.stepper(a, "Confirmed up to", current, 1, 5) { v ->
            val pp = a.store.profile()
            val dates = if (v > pp.physioConfirmedPhase)
                pp.phaseConfirmedDates + (v to LocalDate.now()) else pp.phaseConfirmedDates
            a.store.saveProfile(pp.copy(physioConfirmedPhase = v, phaseConfirmedDates = dates))
            Reminders.reschedule(a)
        })
        col.addView(confirmCard)
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    // ------------------------------------------------------------------

    fun medsEditor(a: MainActivity, onDone: (() -> Unit)? = null): View {
        val col = Ui.column(a)
        if (onDone == null) col.addView(Ui.backRow(a, "Medications") { a.popOverlay() })
        col.addView(Ui.caption(a, "Each time gets its own reminder with taken/missed logging. " +
            "Change doses only with your prescriber."))
        col.addView(Ui.spacer(a, 4))

        for (med in a.store.medications()) {
            val card = Ui.card(a)
            val head = Ui.row(a)
            head.addView(Ui.iconBadge(a, "ic_pill", boxDp = 36))
            val texts = android.widget.LinearLayout(a).apply { orientation = android.widget.LinearLayout.VERTICAL }
            texts.setPadding(Ui.dp(a, 12), 0, 0, 0)
            texts.addView(Ui.text(a, "${med.name} ${med.dose}" + if (!med.active) " · paused" else "",
                15.5f, Ui.TEXT, bold = true))
            texts.addView(Ui.caption(a, "Reminders ${med.times.joinToString(", ")}"))
            head.addView(Ui.weight(texts, 1f))
            card.addView(head)
            if (med.notes.isNotBlank()) {
                card.addView(Ui.spacer(a, 6))
                card.addView(Ui.caption(a, med.notes))
            }
            card.addView(Ui.spacer(a, 4))
            val row = Ui.row(a)
            row.addView(Ui.weight(Ui.textButton(a, "Edit") { a.pushOverlay { medEditor(a, med) } }, 1f))
            row.addView(Ui.weight(Ui.textButton(a, if (med.active) "Pause" else "Resume") {
                a.store.saveMedications(a.store.medications().map {
                    if (it.id == med.id) it.copy(active = !it.active) else it
                })
                Reminders.reschedule(a)
                a.refresh()
            }, 1f))
            row.addView(Ui.weight(Ui.textButton(a, "Delete", Ui.DANGER) {
                Forms.confirm(a, "Delete ${med.name}?", "Past taken/missed history is kept.") {
                    a.store.saveMedications(a.store.medications().filter { it.id != med.id })
                    Reminders.reschedule(a)
                    a.refresh()
                }
            }, 1f))
            card.addView(row)
            col.addView(card)
        }

        col.addView(Ui.fullWidth(Ui.tonalButton(a, "Add medication") {
            a.pushOverlay {
                medEditor(a, Medication(UUID.randomUUID().toString(), "", "", listOf(LocalTime.of(9, 0)), "", true))
            }
        }, a))
        if (onDone != null) {
            col.addView(Ui.fullWidth(Ui.button(a, "Confirm & continue") { onDone() }, a))
        }
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    private fun medEditor(a: MainActivity, med: Medication): View {
        var times = med.times.toMutableList()
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, if (med.name.isBlank()) "New medication" else med.name) { a.popOverlay() })

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
                row.addView(Ui.weight(Forms.timeButton(a, t) { new -> times[i] = new }, 1f))
                row.addView(Ui.iconButton(a, "ic_close", Ui.TEXT_DIM, desc = "Remove time") {
                    times.removeAt(i)
                    rebuildTimes()
                })
                timesCard.addView(row)
            }
            timesCard.addView(Ui.fullWidth(Ui.textButton(a, "Add a time") {
                times.add(LocalTime.of(12, 0))
                rebuildTimes()
            }, a, 4))
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
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    // ------------------------------------------------------------------

    private fun tasksEditor(a: MainActivity): View {
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Daily care") { a.popOverlay() })
        col.addView(Ui.caption(a, "Boot-change reminders come from your boot plan automatically " +
            "(edit it under Injury & goal)."))
        col.addView(Ui.spacer(a, 4))

        for (task in a.store.tasks()) {
            val iconName = when (task.kind) {
                TaskKind.ELEVATION -> "ic_elevate"
                TaskKind.BOOT_CHECK -> "ic_boot"
                TaskKind.CIRCULATION_CHECK -> "ic_pulse"
                else -> "ic_bell"
            }
            val card = Ui.card(a)
            val head = Ui.row(a)
            head.addView(Ui.iconBadge(a, iconName, boxDp = 36))
            val texts = android.widget.LinearLayout(a).apply { orientation = android.widget.LinearLayout.VERTICAL }
            texts.setPadding(Ui.dp(a, 12), 0, 0, 0)
            texts.addView(Ui.text(a, task.title + if (!task.active) " · off" else "", 15.5f, Ui.TEXT, bold = true))
            texts.addView(Ui.caption(a, "${task.times.joinToString(", ")} · phases ${task.fromPhase}-${task.toPhase}"))
            head.addView(Ui.weight(texts, 1f))
            card.addView(head)
            card.addView(Ui.spacer(a, 4))
            card.addView(Ui.caption(a, task.detail))
            card.addView(Ui.spacer(a, 4))
            val row = Ui.row(a)
            row.addView(Ui.weight(Ui.textButton(a, "Edit") { a.pushOverlay { taskEditor(a, task) } }, 1f))
            row.addView(Ui.weight(Ui.textButton(a, if (task.active) "Turn off" else "Turn on") {
                a.store.saveTasks(a.store.tasks().map {
                    if (it.id == task.id) it.copy(active = !it.active) else it
                })
                Reminders.reschedule(a)
                a.refresh()
            }, 1f))
            row.addView(Ui.weight(Ui.textButton(a, "Delete", Ui.DANGER) {
                Forms.confirm(a, "Delete task?", "\"${task.title}\" will stop appearing and reminding.") {
                    a.store.saveTasks(a.store.tasks().filter { it.id != task.id })
                    Reminders.reschedule(a)
                    a.refresh()
                }
            }, 1f))
            card.addView(row)
            col.addView(card)
        }

        col.addView(Ui.fullWidth(Ui.tonalButton(a, "Add custom task") {
            a.pushOverlay {
                taskEditor(a, RehabTask(
                    UUID.randomUUID().toString(), TaskKind.CUSTOM, "", "",
                    listOf(LocalTime.of(9, 0)), 1, 5, null, true
                ))
            }
        }, a))
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    private fun taskEditor(a: MainActivity, task: RehabTask): View {
        var times = task.times.toMutableList()
        var fromPhase = task.fromPhase
        var toPhase = task.toPhase
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, if (task.title.isBlank()) "New task" else task.title) { a.popOverlay() })

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
                row.addView(Ui.iconButton(a, "ic_close", Ui.TEXT_DIM, desc = "Remove time") {
                    times.removeAt(i); rebuildTimes()
                })
                timesCard.addView(row)
            }
            timesCard.addView(Ui.fullWidth(Ui.textButton(a, "Add a time") {
                times.add(LocalTime.of(12, 0)); rebuildTimes()
            }, a, 4))
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
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    // ------------------------------------------------------------------

    // ------------------------------------------------------------------

    private fun exerciseReminderEditor(a: MainActivity): View {
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Exercise reminders") { a.popOverlay() })
        col.addView(Ui.caption(a, "A single gentle nudge each day to do your rehab exercises. " +
            "Doing them little and often is what rebuilds the tendon - the reminder just helps the habit stick."))
        col.addView(Ui.spacer(a, 4))

        var time = Reminders.parseTime(a.store.setting("exercise_reminder", "10:00"))
            ?: LocalTime.of(10, 0)
        var enabled = Reminders.parseTime(a.store.setting("exercise_reminder", "10:00")) != null

        val card = Ui.card(a)
        card.addView(Forms.label(a, "Daily exercise reminder"))
        val timeCard = Ui.card(a)
        fun persist() {
            a.store.saveSetting("exercise_reminder", if (enabled)
                "%02d:%02d".format(time.hour, time.minute) else "off")
            Reminders.reschedule(a)
        }
        fun rebuild() {
            timeCard.removeAllViews()
            if (enabled) {
                timeCard.addView(Forms.timeButton(a, time) { t -> time = t; persist() })
            } else {
                timeCard.addView(Ui.caption(a, "Reminder is off. Turn it on to pick a time."))
            }
        }
        card.addView(Forms.choiceRow(a, listOf(true, false), { if (it) "On" else "Off" }, enabled) {
            enabled = it; persist(); rebuild()
        })
        rebuild()
        card.addView(timeCard)
        col.addView(card)

        col.addView(Ui.caption(a, "The nudge appears only on days your current phase has exercises, " +
            "and never replaces medication reminders."))
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    // ------------------------------------------------------------------

    private fun autoBackupEditor(a: MainActivity): View {
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Automatic backup") { a.popOverlay() })
        col.addView(Ui.caption(a, "Pick a file once - in Drive, Files, an SD card, anywhere your phone " +
            "can save to - and RecoverWell quietly overwrites it with a fresh copy once a day. " +
            "No account, no network from the app itself; the file is yours."))
        col.addView(Ui.spacer(a, 4))

        if (a.autoBackupEnabled()) {
            val card = Ui.card(a)
            card.addView(Ui.text(a, "On", 16f, Ui.DONE, bold = true))
            card.addView(Ui.spacer(a, 2))
            card.addView(Ui.text(a, "Saving to: ${a.store.setting("auto_backup_name", "your chosen file")}", 14f))
            val last = a.store.setting("auto_backup_last", "")
            card.addView(Ui.caption(a, if (last.isBlank()) "Not written yet." else "Last saved: $last."))
            val err = a.store.setting("auto_backup_error", "")
            if (err.isNotBlank()) {
                card.addView(Ui.spacer(a, 4))
                card.addView(Ui.text(a, "Last attempt failed: $err. The file may have been moved or " +
                    "deleted - choose it again.", 13.5f, Ui.DANGER))
            }
            col.addView(card)

            col.addView(Ui.fullWidth(Ui.button(a, "Back up now") {
                if (a.writeAutoBackup()) Forms.info(a, "Done", "Your backup file is up to date.")
                else Forms.info(a, "Couldn't save", "The file may have been moved or deleted. " +
                    "Choose a new location below.")
                a.refresh()
            }, a))
            col.addView(Ui.fullWidth(Ui.tonalButton(a, "Choose a different file") { a.chooseAutoBackupFile() }, a))
            col.addView(Ui.fullWidth(Ui.textButton(a, "Turn off automatic backup", Ui.DANGER) {
                Forms.confirm(a, "Turn off automatic backup?",
                    "Your existing backup file is kept, but no new copies will be saved automatically.") {
                    a.disableAutoBackup()
                }
            }, a))
        } else {
            val card = Ui.card(a, Ui.INFO_BG)
            card.addView(Ui.text(a, "Losing your recovery log is the one thing you can't undo.", 14.5f, Ui.ON_INFO_BG))
            col.addView(card)
            col.addView(Ui.fullWidth(Ui.button(a, "Choose backup file") { a.chooseAutoBackupFile() }, a))
            col.addView(Ui.caption(a, "Tip: saving into a cloud-synced folder (Google Drive, OneDrive) means " +
                "your data also survives losing the phone."))
        }
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    // ------------------------------------------------------------------

    private fun reminderHealth(a: MainActivity): View {
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Reminder reliability") { a.popOverlay() })
        col.addView(Ui.caption(a, "Missed reminders are almost always an Android setting, not the app. " +
            "Here's the status of each, with a one-tap fix where there is one."))
        col.addView(Ui.spacer(a, 4))

        for (check in com.recoverwell.app.notify.ReminderHealth.checks(a)) {
            val card = Ui.card(a, if (check.ok) Ui.DONE_BG else if (check.critical) Ui.DANGER_BG else Ui.WARN_BG)
            val head = Ui.row(a)
            head.addView(Ui.icon(a, if (check.ok) "ic_check" else "ic_alert", 20,
                if (check.ok) Ui.DONE else if (check.critical) Ui.DANGER else Ui.WARN))
            val t = Ui.text(a, check.label, 15.5f,
                if (check.ok) Ui.DONE else if (check.critical) Ui.ON_DANGER_BG else Ui.WARN, bold = true)
            t.setPadding(Ui.dp(a, 10), 0, 0, 0)
            head.addView(Ui.weight(t, 1f))
            head.addView(Ui.text(a, if (check.ok) "OK" else "Action", 13f,
                if (check.ok) Ui.DONE else Ui.DANGER, bold = true))
            card.addView(head)
            card.addView(Ui.spacer(a, 4))
            card.addView(Ui.text(a, check.detail, 14f, Ui.TEXT))
            if (!check.ok && check.fixable) {
                card.addView(Ui.fullWidth(Ui.tonalButton(a, "Fix this") {
                    when (check.id) {
                        "notifications" -> com.recoverwell.app.notify.ReminderHealth.openNotificationSettings(a)
                        "exact" -> com.recoverwell.app.notify.ReminderHealth.requestExactAlarm(a)
                        "battery" -> com.recoverwell.app.notify.ReminderHealth.requestIgnoreBattery(a)
                    }
                }, a))
            }
            col.addView(card)
        }

        col.addView(Ui.section(a, "Check it works"))
        col.addView(Ui.fullWidth(Ui.button(a, "Send a test reminder now") {
            Reminders.sendTestNotification(a)
            Forms.info(a, "Test sent",
                "Pull down your notification shade. If you don't see a RecoverWell test reminder, " +
                    "fix the items above and try again.")
        }, a))
        col.addView(Ui.caption(a, "Tap above, then swipe down from the top of your screen to confirm it arrived."))
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    // ------------------------------------------------------------------

    private fun about(a: MainActivity): View {
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "About") { a.popOverlay() })

        val disc = Ui.card(a, Ui.WARN_BG)
        disc.addView(Ui.text(a, "What this app is - and is not", 16f, Ui.WARN, bold = true))
        disc.addView(Ui.spacer(a, 4))
        disc.addView(Ui.text(a, RehabFramework.DISCLAIMER, 14.5f))
        col.addView(disc)

        col.addView(Ui.section(a, "Protocol basis"))
        val proto = Ui.card(a)
        val protocol = ProtocolRegistry.forProfile(a.store.profile())
        proto.addView(Ui.text(a, "${protocol.injuryName} · ${protocol.variantName}", 15f, Ui.TEXT, bold = true))
        proto.addView(Ui.caption(a, protocol.protocolName))
        proto.addView(Ui.spacer(a, 6))
        proto.addView(Ui.text(
            a,
            "Modelled on established UK conservative (non-surgical) functional " +
                "rehabilitation pathways for Achilles rupture:\n\n" +
                "UKSTAR trial (Costa et al., The Lancet 2020) - early functional bracing " +
                "with immediate weight-bearing as tolerated.\n\n" +
                "NHS trust non-operative pathways (e.g. University Hospitals Coventry & " +
                "Warwickshire; Cambridge University Hospitals): boot in full equinus, wedge " +
                "removal from ~week 3, neutral by ~week 8, boot off ~weeks 8-10, no calf " +
                "stretching before week 12.\n\n" +
                "Return to sport typically from 6 months; court sports such as padel " +
                "usually 9-12 months with physio sign-off.\n\n" +
                "No post-surgical content is included. Every timeline in the app is a " +
                "typical-protocol placeholder for your own clinic's plan.",
            14f
        ))
        col.addView(proto)

        col.addView(Ui.section(a, "Privacy"))
        val priv = Ui.card(a)
        priv.addView(Ui.text(
            a,
            "No account, no analytics, no network permission - the operating system " +
                "enforces that the app cannot reach the internet. Your data leaves this " +
                "phone only when you export it. Tapping \"Watch video demonstration\" hands " +
                "off to YouTube or your browser (which have their own network); the app " +
                "itself still sends nothing.",
            14f
        ))
        col.addView(priv)

        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }
}
