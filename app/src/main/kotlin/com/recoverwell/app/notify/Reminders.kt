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
        val reminders = ScheduleEngine.upcomingReminders(
            store.profile(), store.medications(), store.tasks(), LocalDateTime.now()
        ).take(MAX_SCHEDULED)

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Clear the previous alarm slots, then arm the new set.
        for (slot in 0 until MAX_SCHEDULED) {
            am.cancel(firePendingIntent(context, slot, null))
        }
        reminders.forEachIndexed { slot, r ->
            val at = r.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            am.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, at, firePendingIntent(context, slot, r)
            )
        }
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
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(Notification.BigTextStyle().bigText(message))
            .setContentIntent(openApp)
            .setAutoCancel(true)

        if (kind == ScheduleEngine.ItemKind.MEDICATION) {
            builder.addAction(action("Taken", EventStatus.TAKEN))
            builder.addAction(action("Missed", EventStatus.MISSED))
        } else {
            builder.addAction(action("Done", EventStatus.DONE))
        }

        nm.notify(notifId, builder.build())
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
        val kind = intent.getStringExtra("kind")?.let { ScheduleEngine.ItemKind.valueOf(it) } ?: return
        val status = intent.getStringExtra("status")?.let { EventStatus.valueOf(it) } ?: return
        Reminders.recordEvent(
            context, kind,
            intent.getStringExtra("refId") ?: return,
            intent.getStringExtra("slotKey") ?: "",
            status
        )
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(intent.getIntExtra("notifId", 0))
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Reminders.reschedule(context)
        }
    }
}
