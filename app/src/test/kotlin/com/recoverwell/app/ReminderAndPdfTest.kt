package com.recoverwell.app

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/** Proves the reminder pipeline end-to-end: schedule -> fire -> notify -> action -> log. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class ReminderPipelineTest {

    @Test
    fun reminderFiresNotifiesAndActionLogsEvent() {
        val activity = Robolectric.setupActivity(MainActivity::class.java)
        val context = RuntimeEnvironment.application

        // 1. alarms scheduled from data (done in onCreate via reschedule)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val scheduled = Shadows.shadowOf(alarmManager).scheduledAlarms
        assertTrue("expected alarms scheduled for meds/tasks", scheduled.isNotEmpty())

        // 2. simulate the alarm firing: receiver shows an actionable notification
        val fire = Intent(context, com.recoverwell.app.notify.ReminderReceiver::class.java).apply {
            putExtra("kind", "MEDICATION")
            putExtra("refId", "med_anticoagulant")
            putExtra("slotKey", "08:00")
            putExtra("title", "Medication: Anticoagulant 2.5 mg")
            putExtra("message", "Time for your 08:00 dose.")
        }
        com.recoverwell.app.notify.ReminderReceiver().onReceive(context, fire)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifications = Shadows.shadowOf(nm).allNotifications
        assertEquals(1, notifications.size)
        assertEquals(3, notifications[0].actions.size) // Taken / Missed / Snooze 15m
        assertEquals("Snooze 15m", notifications[0].actions[2].title.toString())

        // 3. the "Taken" action records a TAKEN event for today
        val act = Intent(context, com.recoverwell.app.notify.ActionReceiver::class.java).apply {
            putExtra("kind", "MEDICATION")
            putExtra("refId", "med_anticoagulant")
            putExtra("slotKey", "08:00")
            putExtra("status", "TAKEN")
            putExtra("notifId", 1)
        }
        com.recoverwell.app.notify.ActionReceiver().onReceive(context, act)
        val events = activity.store.eventsOn(java.time.LocalDate.now())
        assertTrue(events.any {
            it.refId == "med_anticoagulant" && it.slotKey == "08:00" &&
                it.status == com.recoverwell.core.model.EventStatus.TAKEN
        })
    }
}

/**
 * Verifies the PDF report's content assembly against live store data.
 * (The final native rendering via android.graphics.pdf.PdfDocument has no
 * Robolectric 3.8 shadow; it is a thin 30-line platform call exercised
 * on-device.)
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class PdfReportTest {

    @Test
    fun reportContainsProfileMedsMilestonesAndLogs() {
        val activity = Robolectric.setupActivity(MainActivity::class.java)
        activity.store.saveDailyLog(
            com.recoverwell.core.model.DailyLog.empty(java.time.LocalDate.now())
                .copy(pain = 3, notes = "first padel-free week done")
        )
        val lines = com.recoverwell.app.export.PdfReport.composeLines(activity.store)
        val text = lines.joinToString("\n") { it.second }
        assertTrue(text.contains("Recovery Report"))
        assertTrue(text.contains("Achilles tendon rupture"))
        assertTrue(text.contains("2026-06-02"))
        assertTrue(text.contains("Conservative (non-surgical)"))
        assertTrue(text.contains("Anticoagulant 2.5 mg at 08:00, 20:00"))
        assertTrue(text.contains("Week 8"))   // milestone table present
        assertTrue(text.contains("Pain: 1 entries, latest 3/10"))
        assertTrue(text.contains("first padel-free week done"))
        assertTrue(text.contains("supports - but never replaces"))
        assertFalse(text.lowercase().contains("paddle"))
    }
}
