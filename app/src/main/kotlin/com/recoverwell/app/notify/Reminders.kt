package com.recoverwell.app.notify

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.recoverwell.app.MainActivity
import com.recoverwell.app.data.Store
import com.recoverwell.core.logic.ScheduleEngine
import com.recoverwell.core.model.EventLog
import com.recoverwell.core.model.EventStatus
import com.recoverwell.core.model.EventType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

object Reminders {

    const val CHANNEL_MEDS = "meds"
    const val CHANNEL_TASKS = "tasks"
    private const val MAX_SCHEDULED = 32

    // All reminders share one bundle so the shade shows a single collapsed
    // RecoverWell group instead of a stack of separate cards.
    private const val GROUP_KEY = "com.recoverwell.reminders"
    private val SUMMARY_ID = "recoverwell_group_summary".hashCode()

    // A reminder is only useful around its time; if it goes unactioned it
    // auto-dismisses rather than lingering for hours. Medication windows are
    // longer-lived than rehab tasks, so they get a more forgiving timeout.
    private const val TIMEOUT_MED_MS = 8L * 60 * 60_000
    private const val TIMEOUT_TASK_MS = 3L * 60 * 60_000

    /** Setting value "off" or "HH:mm" -> a time, or null when disabled. */
    fun parseTime(value: String): LocalTime? =
        if (value.isBlank() || value == "off") null
        else runCatching {
            val (h, m) = value.split(":").map { it.toInt() }
            LocalTime.of(h, m)
        }.getOrNull()

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MEDS, "Medication reminders", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Anticoagulant and other medication doses" }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TASKS, "Rehab task reminders", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Elevation, boot checks, circulation checks, wedge changes" }
        )
    }

    /**
     * (Re)schedules the next reminders computed by the core ScheduleEngine.
     * Called on app start, after any data edit, after each fired reminder and
     * after device reboot - so the alarm set is always derived from current data.
     */
    fun reschedule(context: Context) {
        ensureChannels(context)
        val store = Store.get(context)
        val exerciseTime = parseTime(store.setting("exercise_reminder", "10:00"))
        val checkInTime = parseTime(store.setting("checkin_reminder", "off"))
        val reminders = ScheduleEngine.upcomingReminders(
            store.profile(), store.medications(), store.tasks(), LocalDateTime.now(),
            exerciseReminderTime = exerciseTime,
            overrides = store.exerciseOverrides(),
            checkInTime = checkInTime,
            sessionsPerDay = store.exerciseSessions()
        ).take(MAX_SCHEDULED)

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Clear the previous alarm slots, then arm the new set.
        for (slot in 0 until MAX_SCHEDULED) {
            am.cancel(firePendingIntent(context, slot, null))
        }
        val exactAllowed = android.os.Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        reminders.forEachIndexed { slot, r ->
            val at = r.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val pi = firePendingIntent(context, slot, r)
            if (exactAllowed) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                // exact-alarm permission revoked: fire within a 10-minute window
                am.setWindow(AlarmManager.RTC_WAKEUP, at, 10 * 60_000L, pi)
            }
        }
        com.recoverwell.app.widget.TodayWidget.update(context)
    }

    private fun firePendingIntent(
        context: Context,
        slot: Int,
        r: ScheduleEngine.Reminder?
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.recoverwell.REMINDER_$slot"
            if (r != null) {
                putExtra("kind", r.kind.name)
                putExtra("refId", r.refId)
                putExtra("slotKey", r.slotKey)
                putExtra("title", r.title)
                putExtra("message", r.message)
            }
        }
        return PendingIntent.getBroadcast(
            context, slot, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showNotification(
        context: Context,
        kind: ScheduleEngine.ItemKind,
        refId: String,
        slotKey: String,
        title: String,
        message: String
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = if (kind == ScheduleEngine.ItemKind.MEDICATION) CHANNEL_MEDS else CHANNEL_TASKS
        val notifId = (kind.name + refId + slotKey).hashCode()

        val openApp = PendingIntent.getActivity(
            context, notifId,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun action(label: String, status: EventStatus): Notification.Action {
            val intent = Intent(context, ActionReceiver::class.java).apply {
                action = "com.recoverwell.ACTION_${status.name}_$notifId"
                putExtra("kind", kind.name)
                putExtra("refId", refId)
                putExtra("slotKey", slotKey)
                putExtra("status", status.name)
                putExtra("notifId", notifId)
            }
            val pi = PendingIntent.getBroadcast(
                context, notifId + status.ordinal, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            return Notification.Action.Builder(null, label, pi).build()
        }

        val builder = Notification.Builder(context, channel)
            .setSmallIcon(context.resources.getIdentifier("ic_bell", "drawable", context.packageName))
            .setColor(0xFF2F6B4F.toInt())
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(Notification.BigTextStyle().bigText(message))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY)
            .setTimeoutAfter(
                if (kind == ScheduleEngine.ItemKind.MEDICATION) TIMEOUT_MED_MS else TIMEOUT_TASK_MS
            )

        // one-tap pain logging straight from the daily check-in notification
        fun painAction(label: String, pain: Int): Notification.Action {
            val intent = Intent(context, ActionReceiver::class.java).apply {
                action = "com.recoverwell.CHECKIN_${pain}_$notifId"
                putExtra("status", "CHECKIN")
                putExtra("pain", pain)
                putExtra("notifId", notifId)
            }
            val pi = PendingIntent.getBroadcast(
                context, notifId + 20 + pain, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            return Notification.Action.Builder(null, label, pi).build()
        }

        if (kind == ScheduleEngine.ItemKind.CHECKIN) {
            builder.addAction(painAction("Good (low)", 2))
            builder.addAction(painAction("Manageable", 5))
            builder.addAction(painAction("Sore (high)", 8))
            nm.notify(notifId, builder.build())
            refreshGroupSummary(context, nm)
            return
        }

        if (kind == ScheduleEngine.ItemKind.MEDICATION) {
            builder.addAction(action("Taken", EventStatus.TAKEN))
            builder.addAction(action("Missed", EventStatus.MISSED))
        } else {
            builder.addAction(action("Done", EventStatus.DONE))
        }
        // review-mined essential: a snooze that actually works
        run {
            val intent = Intent(context, ActionReceiver::class.java).apply {
                action = "com.recoverwell.SNOOZE_$notifId"
                putExtra("kind", kind.name)
                putExtra("refId", refId)
                putExtra("slotKey", slotKey)
                putExtra("title", title)
                putExtra("message", message)
                putExtra("status", "SNOOZE")
                putExtra("notifId", notifId)
            }
            val pi = PendingIntent.getBroadcast(
                context, notifId + 7, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(Notification.Action.Builder(null, "Snooze 15m", pi).build())
        }

        nm.notify(notifId, builder.build())
        refreshGroupSummary(context, nm)
    }

    /**
     * Posts/updates a single group-summary notification so 2+ active reminders
     * collapse into one RecoverWell bundle (InboxStyle lists the pending ones).
     * With fewer than two children the summary is removed so a lone reminder
     * shows on its own. Safe if the platform can't enumerate active
     * notifications (e.g. under test) - it simply skips the summary.
     */
    private fun refreshGroupSummary(context: Context, nm: NotificationManager) {
        val children = runCatching {
            nm.activeNotifications.filter {
                it.id != SUMMARY_ID && it.notification.group == GROUP_KEY
            }
        }.getOrDefault(emptyList())

        if (children.size < 2) {
            nm.cancel(SUMMARY_ID)
            return
        }

        val titles = children
            .sortedByDescending { it.postTime }
            .mapNotNull { it.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() }
        val inbox = Notification.InboxStyle().setBigContentTitle("RecoverWell reminders")
        titles.take(6).forEach { inbox.addLine(it) }
        if (titles.size > 6) inbox.addLine("+${titles.size - 6} more")

        val openApp = PendingIntent.getActivity(
            context, SUMMARY_ID,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val summary = Notification.Builder(context, CHANNEL_TASKS)
            .setSmallIcon(context.resources.getIdentifier("ic_bell", "drawable", context.packageName))
            .setColor(0xFF2F6B4F.toInt())
            .setContentTitle("RecoverWell reminders")
            .setContentText("${children.size} reminders need attention")
            .setStyle(inbox)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        nm.notify(SUMMARY_ID, summary)
    }

    /** Re-evaluate the group summary after a reminder is dismissed/actioned. */
    fun refreshSummaryAfterDismiss(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        refreshGroupSummary(context, nm)
    }

    /** One-off re-delivery of a snoozed reminder, 15 minutes out. */
    fun scheduleSnooze(
        context: Context,
        kind: ScheduleEngine.ItemKind,
        refId: String,
        slotKey: String,
        title: String,
        message: String,
        notifId: Int
    ) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.recoverwell.SNOOZED_$notifId"
            putExtra("kind", kind.name)
            putExtra("refId", refId)
            putExtra("slotKey", slotKey)
            putExtra("title", title)
            putExtra("message", message)
        }
        val pi = PendingIntent.getBroadcast(
            context, notifId + 13, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val at = System.currentTimeMillis() + 15 * 60_000L
        val exactAllowed = android.os.Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (exactAllowed) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.setWindow(AlarmManager.RTC_WAKEUP, at, 5 * 60_000L, pi)
    }

    /** Fires a reminder right now so the user can confirm notifications actually arrive. */
    fun sendTestNotification(context: Context) {
        ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openApp = PendingIntent.getActivity(
            context, 99,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = Notification.Builder(context, CHANNEL_TASKS)
            .setSmallIcon(context.resources.getIdentifier("ic_bell", "drawable", context.packageName))
            .setColor(0xFF2F6B4F.toInt())
            .setContentTitle("Reminders are working")
            .setContentText("This is a test reminder from RecoverWell. If you can see it, your medication and rehab reminders will arrive too.")
            .setStyle(Notification.BigTextStyle().bigText(
                "This is a test reminder from RecoverWell. If you can see it, your medication and rehab reminders will arrive too."))
            .setContentIntent(openApp)
            .setAutoCancel(true)
        nm.notify("test".hashCode(), builder.build())
    }

    /** One-tap pain log from the check-in notification; carries forward boot/weight-bearing. */
    fun recordCheckIn(context: Context, pain: Int) {
        val store = Store.get(context)
        val today = LocalDate.now()
        val log = store.dailyLog(today)
        val p = store.profile()
        store.saveDailyLog(
            log.copy(
                pain = pain,
                wedges = log.wedges ?: p.currentWedges,
                weightBearing = log.weightBearing ?: p.weightBearing
            )
        )
        com.recoverwell.app.widget.TodayWidget.update(context)
    }

    fun recordEvent(
        context: Context,
        kind: ScheduleEngine.ItemKind,
        refId: String,
        slotKey: String,
        status: EventStatus
    ) {
        val type = when (kind) {
            ScheduleEngine.ItemKind.MEDICATION -> EventType.MEDICATION
            ScheduleEngine.ItemKind.EXERCISE -> EventType.EXERCISE
            else -> EventType.TASK
        }
        val now = LocalTime.now()
        Store.get(context).addEvent(
            EventLog(
                id = UUID.randomUUID().toString(),
                date = LocalDate.now(),
                type = type,
                refId = refId,
                slotKey = slotKey,
                status = status,
                recordedAtMinuteOfDay = now.hour * 60 + now.minute
            )
        )
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra("kind")?.let { ScheduleEngine.ItemKind.valueOf(it) } ?: return
        Reminders.showNotification(
            context, kind,
            intent.getStringExtra("refId") ?: return,
            intent.getStringExtra("slotKey") ?: "",
            intent.getStringExtra("title") ?: "RecoverWell reminder",
            intent.getStringExtra("message") ?: ""
        )
        Reminders.reschedule(context)
    }
}

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra("notifId", 0)
        // one-tap pain log from the check-in notification (no "kind" needed)
        if (intent.getStringExtra("status") == "CHECKIN") {
            Reminders.recordCheckIn(context, intent.getIntExtra("pain", 5))
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(notifId)
            Reminders.refreshSummaryAfterDismiss(context)
            return
        }
        val kind = intent.getStringExtra("kind")?.let { ScheduleEngine.ItemKind.valueOf(it) } ?: return
        if (intent.getStringExtra("status") == "SNOOZE") {
            Reminders.scheduleSnooze(
                context, kind,
                intent.getStringExtra("refId") ?: return,
                intent.getStringExtra("slotKey") ?: "",
                intent.getStringExtra("title") ?: "RecoverWell reminder",
                intent.getStringExtra("message") ?: "",
                notifId
            )
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(notifId)
            Reminders.refreshSummaryAfterDismiss(context)
            return
        }
        val status = intent.getStringExtra("status")?.let { EventStatus.valueOf(it) } ?: return
        Reminders.recordEvent(
            context, kind,
            intent.getStringExtra("refId") ?: return,
            intent.getStringExtra("slotKey") ?: "",
            status
        )
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(intent.getIntExtra("notifId", 0))
        Reminders.refreshSummaryAfterDismiss(context)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Reminders.reschedule(context)
        }
    }
}
