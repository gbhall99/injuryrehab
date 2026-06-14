package com.recoverwell.app

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import java.io.File

/**
 * Last-resort diagnostics: any uncaught exception is written to app storage
 * and surfaced on the next launch (and screen-build failures render an
 * in-app error view instead of killing the process). Sideloaded apps have
 * no Play crash reporting, so this is how device-specific crashes become
 * fixable bug reports.
 */
object CrashGuard {

    private fun crashFile(context: Context) = File(context.filesDir, "last-crash.txt")

    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                crashFile(context).writeText(describe(throwable))
            } catch (_: Exception) {
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun describe(t: Throwable): String =
        "RecoverWell crash report\n" +
            "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT}) · " +
            "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n\n" +
            android.util.Log.getStackTraceString(t)

    /** Persist a handled failure so it also shows on next launch. */
    fun record(context: Context, t: Throwable) {
        try {
            crashFile(context).writeText(describe(t))
        } catch (_: Exception) {
        }
    }

    /** If the previous session crashed, offer the report; consumes it. */
    fun offerReport(activity: Activity) {
        val f = crashFile(activity)
        if (!f.exists()) return
        val report = f.readText()
        f.delete()
        AlertDialog.Builder(activity)
            .setTitle("The app hit a problem")
            .setMessage(
                "Sorry about that - a report was saved so it can be fixed.\n\n" +
                    report.lineSequence().take(8).joinToString("\n")
            )
            .setPositiveButton("Copy full report") { _, _ ->
                val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("RecoverWell crash report", report))
            }
            .setNegativeButton("Dismiss", null)
            .show()
    }
}
