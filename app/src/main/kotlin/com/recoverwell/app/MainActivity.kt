package com.recoverwell.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import com.recoverwell.app.data.Store
import com.recoverwell.app.export.PdfReport
import com.recoverwell.app.notify.Reminders
import com.recoverwell.app.screens.*
import com.recoverwell.app.ui.Forms
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.export.BackupCodec
import com.recoverwell.core.export.CsvExporter
import com.recoverwell.core.protocol.ProtocolContent

class MainActivity : Activity() {

    enum class Tab(val label: String, val icon: String) {
        TODAY("Today", "☀"),
        EXERCISES("Exercises", "💪"),
        TRACKER("Tracker", "📈"),
        TWIN("My Leg", "🦵"),
        MORE("More", "⚙")
    }

    lateinit var store: Store
    private lateinit var content: FrameLayout
    private lateinit var tabBar: LinearLayout
    var currentTab = Tab.TODAY
    private val overlays = ArrayList<() -> View>()

    private var pendingExport: ByteArray? = null
    private var pendingExportToast = ""
    private val REQ_EXPORT = 41
    private val REQ_IMPORT = 42

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store.get(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.BG)
        }

        // header: app name + always-visible red flags button
        val header = Ui.row(this).apply {
            setBackgroundColor(Ui.PRIMARY)
            setPadding(Ui.dp(this@MainActivity, 16), Ui.dp(this@MainActivity, 10), Ui.dp(this@MainActivity, 10), Ui.dp(this@MainActivity, 10))
        }
        header.addView(Ui.weight(Ui.text(this, "RecoverWell", 21f, 0xFFFFFFFF.toInt(), bold = true), 1f))
        val redFlagBtn = Ui.button(this, "⚠ Red flags", bg = Ui.DANGER) { pushOverlay { RedFlagsScreen.build(this) } }
        redFlagBtn.textSize = 15f
        header.addView(redFlagBtn)
        root.addView(header)

        content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        // persistent disclaimer strip
        val disclaimer = Ui.text(
            this,
            "Supports - never replaces - your physio & consultant. Tap for details.",
            13f, Ui.TEXT_DIM
        ).apply {
            gravity = Gravity.CENTER
            setBackgroundColor(0xFFEFF3EF.toInt())
            setPadding(Ui.dp(this@MainActivity, 8), Ui.dp(this@MainActivity, 6), Ui.dp(this@MainActivity, 8), Ui.dp(this@MainActivity, 6))
            setOnClickListener { Forms.info(this@MainActivity, "Medical disclaimer", ProtocolContent.DISCLAIMER) }
        }
        root.addView(disclaimer)

        tabBar = Ui.row(this).apply {
            setBackgroundColor(Ui.CARD)
            setPadding(0, Ui.dp(this@MainActivity, 2), 0, Ui.dp(this@MainActivity, 2))
        }
        root.addView(tabBar)
        rebuildTabBar()

        setContentView(root)

        Reminders.reschedule(this)

        if (!store.profile().onboardingComplete) {
            pushOverlay { Onboarding.build(this) }
        } else {
            show(Tab.TODAY)
        }
    }

    private fun rebuildTabBar() {
        tabBar.removeAllViews()
        for (tab in Tab.values()) {
            val active = tab == currentTab && overlays.isEmpty()
            val btn = Ui.button(
                this, "${tab.icon}\n${tab.label}",
                bg = Ui.CARD, fg = if (active) Ui.PRIMARY else Ui.TEXT_DIM
            ) { show(tab) }
            btn.textSize = 12f
            btn.background = null
            btn.minHeight = Ui.dp(this, Ui.MIN_TOUCH_DP + 6)
            btn.contentDescription = tab.label + if (active) ", selected" else ""
            tabBar.addView(btn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    fun show(tab: Tab) {
        currentTab = tab
        overlays.clear()
        render()
    }

    fun refresh() = render()

    fun pushOverlay(factory: () -> View) {
        overlays.add(factory)
        render()
    }

    fun popOverlay() {
        if (overlays.isNotEmpty()) overlays.removeAt(overlays.size - 1)
        render()
    }

    private fun render() {
        rebuildTabBar()
        content.removeAllViews()
        val view = if (overlays.isNotEmpty()) overlays.last()() else when (currentTab) {
            Tab.TODAY -> TodayScreen.build(this)
            Tab.EXERCISES -> ExercisesScreen.build(this)
            Tab.TRACKER -> TrackerScreen.build(this)
            Tab.TWIN -> TwinScreen.build(this)
            Tab.MORE -> MoreScreen.build(this)
        }
        content.addView(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (overlays.isNotEmpty()) {
            // onboarding stays modal as the root overlay, but screens stacked
            // on top of it (e.g. red flags) can still be backed out of
            if (!store.profile().onboardingComplete && overlays.size == 1) return
            popOverlay()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        Reminders.reschedule(this)
    }

    // ------------------------------------------------------------------
    // Export / import via the system document picker (no permissions needed)
    // ------------------------------------------------------------------

    fun exportFile(fileName: String, mime: String, bytes: ByteArray, toast: String) {
        pendingExport = bytes
        pendingExportToast = toast
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mime
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        startActivityForResult(intent, REQ_EXPORT)
    }

    fun exportLogsCsv() = exportFile(
        "recoverwell-daily-logs.csv", "text/csv",
        CsvExporter.dailyLogsCsv(store.allLogs()).toByteArray(), "Daily logs exported"
    )

    fun exportEventsCsv() = exportFile(
        "recoverwell-medication-task-log.csv", "text/csv",
        CsvExporter.eventsCsv(store.allEvents()).toByteArray(), "Medication & task log exported"
    )

    fun exportBackup() = exportFile(
        "recoverwell-backup.json", "application/json",
        BackupCodec.encode(store.snapshot()).toByteArray(), "Backup exported"
    )

    fun exportPdf() = exportFile(
        "recoverwell-report.pdf", "application/pdf",
        PdfReport.build(store), "PDF report exported"
    )

    fun importBackup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        startActivityForResult(intent, REQ_IMPORT)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data ?: return
        if (resultCode != RESULT_OK) return
        try {
            when (requestCode) {
                REQ_EXPORT -> {
                    val bytes = pendingExport ?: return
                    contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    Toast.makeText(this, pendingExportToast, Toast.LENGTH_LONG).show()
                    pendingExport = null
                }
                REQ_IMPORT -> {
                    val text = contentResolver.openInputStream(uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    } ?: return
                    val state = BackupCodec.decode(text)
                    Forms.confirm(
                        this, "Restore backup?",
                        "This replaces ALL current data with the backup " +
                            "(${state.dailyLogs.size} daily logs, ${state.events.size} events)."
                    ) {
                        store.restore(state)
                        Reminders.reschedule(this)
                        refresh()
                        Toast.makeText(this, "Backup restored", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            Forms.info(this, "Operation failed", e.message ?: "Unknown error")
        }
    }
}
