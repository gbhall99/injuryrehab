package com.recoverwell.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.recoverwell.app.MainActivity
import com.recoverwell.app.data.Store
import com.recoverwell.core.logic.ScheduleEngine
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Home-screen glance: today's checklist progress and the next reminder.
 * Refreshed whenever reminders are rescheduled (app opens, items ticked,
 * alarms fire) plus the standard half-hour widget cycle.
 */
class TodayWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        update(context)
    }

    companion object {
        fun update(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context) ?: return
                val ids = manager.getAppWidgetIds(ComponentName(context, TodayWidget::class.java))
                if (ids.isEmpty()) return

                val store = Store.get(context)
                val today = LocalDate.now()
                val items = ScheduleEngine.dailyChecklist(
                    store.profile(), store.medications(), store.tasks(),
                    store.exerciseOverrides(), store.eventsOn(today), today
                )
                val done = items.count { it.isDone }
                val next = ScheduleEngine.upcomingReminders(
                    store.profile(), store.medications(), store.tasks(), LocalDateTime.now()
                ).firstOrNull()

                val pkg = context.packageName
                fun id(name: String, type: String) = context.resources.getIdentifier(name, type, pkg)
                val views = RemoteViews(pkg, id("widget_today", "layout"))
                views.setTextViewText(id("widget_progress", "id"), "$done of ${items.size} done")
                views.setProgressBar(id("widget_bar", "id"), 100,
                    if (items.isEmpty()) 0 else done * 100 / items.size, false)
                views.setTextViewText(id("widget_next", "id"),
                    if (next != null)
                        "Next: %02d:%02d · %s".format(next.at.hour, next.at.minute,
                            next.title.removePrefix("Medication: "))
                    else "All set for now")
                views.setOnClickPendingIntent(id("widget_root", "id"), PendingIntent.getActivity(
                    context, 99,
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                ))
                manager.updateAppWidget(ids, views)
            } catch (_: Exception) {
                // a widget refresh must never take the app down with it
            }
        }
    }
}
