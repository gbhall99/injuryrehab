package com.recoverwell.app.notify

import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Why reminders fail in the real world is almost never the alarm code - it is an
 * OS setting the user (or the manufacturer) switched off: notifications blocked,
 * exact alarms revoked, or aggressive battery optimisation killing the app. This
 * checks each of those and offers a one-tap route to the right system screen.
 */
object ReminderHealth {

    data class Check(
        val id: String,
        val label: String,
        val ok: Boolean,
        val detail: String,
        /** Critical checks block delivery entirely; non-critical only risk delay. */
        val critical: Boolean,
        /** True if there is a system screen we can send the user to. */
        val fixable: Boolean
    )

    fun notificationsEnabled(ctx: Context): Boolean {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.areNotificationsEnabled()
    }

    fun exactAlarmsAllowed(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return am.canScheduleExactAlarms()
    }

    fun batteryUnrestricted(ctx: Context): Boolean {
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(ctx.packageName)
    }

    fun checks(ctx: Context): List<Check> {
        val out = ArrayList<Check>()
        out.add(
            Check(
                "notifications", "Notifications allowed", notificationsEnabled(ctx),
                if (notificationsEnabled(ctx)) "RecoverWell can post reminders."
                else "Reminders are silently dropped until notifications are turned back on.",
                critical = true, fixable = true
            )
        )
        out.add(
            Check(
                "exact", "Exact alarm timing", exactAlarmsAllowed(ctx),
                if (exactAlarmsAllowed(ctx)) "Doses fire at the minute you set."
                else "Without this, Android may delay a dose reminder by several minutes.",
                critical = false, fixable = Build.VERSION.SDK_INT >= 31
            )
        )
        out.add(
            Check(
                "battery", "Battery not restricted", batteryUnrestricted(ctx),
                if (batteryUnrestricted(ctx)) "The system won't sleep the app's alarms."
                else "Battery optimisation can stop reminders firing while the phone is idle - " +
                    "important for an anticoagulant.",
                critical = false, fixable = true
            )
        )
        return out
    }

    /** Every critical check passes (delivery is not actively blocked). */
    fun deliveryBlocked(ctx: Context): Boolean = checks(ctx).any { it.critical && !it.ok }

    /** Any check at all is failing (worth nudging about). */
    fun hasIssue(ctx: Context): Boolean = checks(ctx).any { !it.ok }

    // ---- fix-it routes -------------------------------------------------------

    fun openNotificationSettings(a: Activity) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, a.packageName)
        safeStart(a, intent, appDetailsFallback(a))
    }

    fun requestExactAlarm(a: Activity) {
        if (Build.VERSION.SDK_INT < 31) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            .setData(Uri.parse("package:${a.packageName}"))
        safeStart(a, intent, appDetailsFallback(a))
    }

    fun requestIgnoreBattery(a: Activity) {
        @Suppress("BatteryLife")
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:${a.packageName}"))
        safeStart(a, intent, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }

    private fun appDetailsFallback(a: Activity): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:${a.packageName}"))

    private fun safeStart(a: Activity, primary: Intent, fallback: Intent) {
        try {
            a.startActivity(primary)
        } catch (e: Exception) {
            try {
                a.startActivity(fallback)
            } catch (e2: Exception) {
                android.widget.Toast.makeText(
                    a, "Open Settings > Apps > RecoverWell to fix this.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
