package com.recoverwell.app

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
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
import com.recoverwell.core.protocol.RehabFramework

class MainActivity : Activity() {

    enum class Tab(val label: String, val iconName: String) {
        TODAY("Today", "ic_today"),
        EXERCISES("Exercises", "ic_exercises"),
        TRACKER("Progress", "ic_progress"),
        TWIN("My leg", "ic_leg"),
        MORE("More", "ic_more")
    }

    lateinit var store: Store
    private lateinit var content: FrameLayout
    private lateinit var tabBar: LinearLayout
    private lateinit var appBar: LinearLayout
    private lateinit var disclaimer: android.widget.TextView
    private lateinit var appBarTitle: android.widget.TextView
    var currentTab = Tab.TODAY
    private val overlays = ArrayList<() -> View>()
    // each overlay's own title for the app bar (null falls back to the tab name,
    // so the bar never shows a generic "RecoverWell" over a named screen)
    private val overlayTitles = ArrayList<String?>()

    private var pendingExport: ByteArray? = null
    private var pendingExportToast = ""
    private val REQ_EXPORT = 41
    private val REQ_IMPORT = 42
    private val REQ_AUTOBACKUP = 43
    private val REQ_MIC = 44
    private var pendingMicResult: ((Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashGuard.install(applicationContext)
        store = Store.get(this)

        // resolve theme before any view is built: tokens are read at build time
        val appearance = store.setting("appearance", "system")
        val systemDark = (resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        com.recoverwell.draw.Palette.dark = when (appearance) {
            "dark" -> true
            "light" -> false
            else -> systemDark
        }

        window.statusBarColor = Ui.BG
        window.navigationBarColor = Ui.CARD
        window.decorView.systemUiVisibility = if (com.recoverwell.draw.Palette.dark) 0
        else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.BG)
        }

        // app bar: large title + always-one-tap red flags
        appBar = Ui.row(this).apply {
            setPadding(Ui.dp(this@MainActivity, 20), Ui.dp(this@MainActivity, 14),
                Ui.dp(this@MainActivity, 12), Ui.dp(this@MainActivity, 6))
        }
        appBarTitle = Ui.display(this, Tab.TODAY.label)
        appBar.addView(Ui.weight(appBarTitle, 1f))
        appBar.addView(Ui.iconButton(this, "ic_alert", Ui.DANGER, Ui.DANGER_BG, desc = "Red flags - urgent symptoms") {
            pushOverlay("Red flags") { RedFlagsScreen.build(this) }
        })
        root.addView(appBar)

        content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        // persistent disclaimer: quiet but always present and tappable
        disclaimer = Ui.caption(this, "Supports - never replaces - your physio and consultant")
        disclaimer.gravity = Gravity.CENTER
        disclaimer.setPadding(Ui.dp(this, 12), Ui.dp(this, 6), Ui.dp(this, 12), Ui.dp(this, 6))
        disclaimer.setOnClickListener { Forms.info(this, "Medical disclaimer", RehabFramework.DISCLAIMER) }
        root.addView(disclaimer)

        tabBar = Ui.row(this).apply {
            setBackgroundColor(Ui.CARD)
            elevation = Ui.dpF(this@MainActivity, 8f)
            setPadding(Ui.dp(this@MainActivity, 4), Ui.dp(this@MainActivity, 6), Ui.dp(this@MainActivity, 4), Ui.dp(this@MainActivity, 8))
        }
        root.addView(tabBar)
        rebuildTabBar()

        // edge-to-edge (enforced from targetSdk 35): keep content clear of bars
        root.setOnApplyWindowInsetsListener { v, insets ->
            val top: Int
            val bottom: Int
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars())
                top = bars.top; bottom = bars.bottom
            } else {
                @Suppress("DEPRECATION")
                top = insets.systemWindowInsetTop
                @Suppress("DEPRECATION")
                bottom = insets.systemWindowInsetBottom
            }
            v.setPadding(0, top, 0, bottom)
            insets
        }

        setContentView(root)

        // notifications need a runtime grant from Android 13
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission("android.permission.POST_NOTIFICATIONS") !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 7)
        }

        Reminders.reschedule(this)
        CrashGuard.offerReport(this)

        if (!store.profile().onboardingComplete) {
            pushOverlay { Onboarding.build(this) }
        } else {
            show(Tab.TODAY)
        }
    }

    /** Onboarding is a modal flow: hide the bottom nav and disclaimer during it. */
    private fun onboardingActive(): Boolean =
        overlays.isNotEmpty() && !store.profile().onboardingComplete

    private fun rebuildTabBar() {
        tabBar.removeAllViews()
        for (tab in Tab.values()) {
            // keep the current tab highlighted even under an overlay, so the nav
            // never looks "dead" - the overlay is a child of this tab
            val active = tab == currentTab
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                isClickable = true
                isFocusable = true
                contentDescription = tab.label + if (active) ", selected" else ""
                background = Ui.ripple(this@MainActivity, Ui.rounded(0x00000000, 20f))
                minimumHeight = Ui.dp(this@MainActivity, Ui.MIN_TOUCH_DP + 8)
                setPadding(0, Ui.dp(this@MainActivity, 4), 0, 0)
                setOnClickListener { show(tab) }
            }
            // icon inside a pill that lights up when active, centred in the slot
            val pill = FrameLayout(this).apply {
                background = GradientDrawable().apply {
                    cornerRadius = Ui.dpF(this@MainActivity, 16f)
                    setColor(if (active) Ui.PRIMARY_CONTAINER else 0x00000000)
                }
                layoutParams = LinearLayout.LayoutParams(
                    Ui.dp(this@MainActivity, 56), Ui.dp(this@MainActivity, 30)
                ).apply { gravity = Gravity.CENTER_HORIZONTAL }
            }
            val iv = ImageView(this).apply {
                setImageResource(Ui.drawableId(this@MainActivity, tab.iconName))
                imageTintList = ColorStateList.valueOf(if (active) Ui.ON_PRIMARY_CONTAINER else Ui.TEXT_DIM)
            }
            val ivLp = FrameLayout.LayoutParams(Ui.dp(this, 22), Ui.dp(this, 22))
            ivLp.gravity = Gravity.CENTER
            pill.addView(iv, ivLp)
            item.addView(pill)
            // label fills the slot width and centres its own text, so it always
            // lines up under the icon regardless of label length
            val label = Ui.text(this, tab.label, 11.5f,
                if (active) Ui.TEXT else Ui.TEXT_DIM, bold = active)
            label.gravity = Gravity.CENTER
            label.setPadding(Ui.dp(this, 2), Ui.dp(this, 3), Ui.dp(this, 2), 0)
            label.maxLines = 1
            item.addView(label, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            tabBar.addView(item, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    fun show(tab: Tab) {
        currentTab = tab
        overlays.clear()
        overlayTitles.clear()
        render(animated = true)
    }

    fun refresh() = render(animated = false)

    fun pushOverlay(title: String? = null, factory: () -> View) {
        overlays.add(factory)
        overlayTitles.add(title)
        render(animated = true)
    }

    fun popOverlay() {
        if (overlays.isNotEmpty()) {
            overlays.removeAt(overlays.size - 1)
            overlayTitles.removeAt(overlayTitles.size - 1)
        }
        render(animated = true)
    }

    private fun render(animated: Boolean) {
        rebuildTabBar()
        val onboarding = onboardingActive()
        // onboarding is a clean, full-screen modal flow: hide all the app chrome
        tabBar.visibility = if (onboarding) View.GONE else View.VISIBLE
        disclaimer.visibility = if (onboarding) View.GONE else View.VISIBLE
        appBar.visibility = if (onboarding) View.GONE else View.VISIBLE
        appBarTitle.text = if (overlays.isNotEmpty())
            (overlayTitles.lastOrNull() ?: currentTab.label) else currentTab.label
        // an in-place refresh (e.g. checking an item off) rebuilds the screen, which
        // would otherwise jump the scroll to the top - capture and restore it so the
        // user's attention stays where they acted. Navigation (animated) resets to top.
        val priorScrollY = if (!animated)
            (if (content.childCount > 0) findScroll(content.getChildAt(0))?.scrollY ?: 0 else 0) else 0
        for (i in 0 until content.childCount) content.getChildAt(i).animate().cancel()
        content.removeAllViews()
        val view = try {
            if (overlays.isNotEmpty()) overlays.last()() else when (currentTab) {
                Tab.TODAY -> TodayScreen.build(this)
                Tab.EXERCISES -> ExercisesScreen.build(this)
                Tab.TRACKER -> TrackerScreen.build(this)
                Tab.TWIN -> TwinScreen.build(this)
                Tab.MORE -> MoreScreen.build(this)
            }
        } catch (t: Throwable) {
            CrashGuard.record(this, t)
            errorScreen(t)
        }
        content.addView(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        if (animated) {
            // gentle fade-and-rise on navigation; refreshes stay still
            view.alpha = 0f
            view.translationY = Ui.dpF(this, 12f)
            view.animate().alpha(1f).translationY(0f)
                .setDuration(220L)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        } else if (priorScrollY > 0) {
            // restore the scroll position so an in-place action keeps its place
            findScroll(view)?.let { sv -> sv.post { sv.scrollTo(0, priorScrollY) } }
        }
    }

    /** First ScrollView in a subtree, used to preserve scroll across refreshes. */
    private fun findScroll(v: View?): android.widget.ScrollView? {
        if (v is android.widget.ScrollView) return v
        if (v is ViewGroup) for (i in 0 until v.childCount) {
            findScroll(v.getChildAt(i))?.let { return it }
        }
        return null
    }

    private fun errorScreen(t: Throwable): View {
        val col = Ui.column(this)
        col.addView(Ui.headline(this, "Something went wrong"))
        col.addView(Ui.spacer(this, 4))
        col.addView(Ui.body(this, "This screen hit a problem. Your data is safe. " +
            "Copy the details below and share them so it can be fixed."))
        col.addView(Ui.spacer(this, 8))
        val traceCard = Ui.card(this)
        val trace = Ui.caption(this, CrashGuard.describe(t))
        trace.typeface = android.graphics.Typeface.MONOSPACE
        trace.textSize = 11f
        traceCard.addView(trace)
        col.addView(traceCard)
        col.addView(Ui.fullWidth(Ui.button(this, "Copy details") {
            val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText(
                "RecoverWell error", CrashGuard.describe(t)))
        }, this))
        col.addView(Ui.fullWidth(Ui.tonalButton(this, "Back to Today") { show(Tab.TODAY) }, this))
        return Ui.scroll(this, col)
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
        runAutoBackupIfDue()
    }

    // ------------------------------------------------------------------
    // Automatic backup: a once-a-day silent overwrite to a user-chosen file.
    // Uses a persisted SAF document grant - no account, no network, no extra
    // permission. The user picks the destination once; we keep writing to it.
    // ------------------------------------------------------------------

    /** Let the user pick (create) the file that auto-backups overwrite. */
    fun chooseAutoBackupFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "recoverwell-auto-backup.json")
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
        startActivityForResult(intent, REQ_AUTOBACKUP)
    }

    fun autoBackupEnabled(): Boolean =
        store.setting("auto_backup_uri", "").isNotBlank()

    fun disableAutoBackup() {
        val uriStr = store.setting("auto_backup_uri", "")
        if (uriStr.isNotBlank()) {
            try {
                contentResolver.releasePersistableUriPermission(
                    android.net.Uri.parse(uriStr),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) { /* grant may already be gone */ }
        }
        store.saveSetting("auto_backup_uri", "")
        store.saveSetting("auto_backup_name", "")
        store.saveSetting("auto_backup_error", "")
        refresh()
    }

    /** Overwrite the chosen file with the current snapshot. Returns true on success. */
    fun writeAutoBackup(): Boolean {
        val uriStr = store.setting("auto_backup_uri", "")
        if (uriStr.isBlank()) return false
        return try {
            val bytes = BackupCodec.encode(store.snapshot()).toByteArray()
            // "wt" truncates, so the file is replaced rather than appended to
            contentResolver.openOutputStream(android.net.Uri.parse(uriStr), "wt")?.use {
                it.write(bytes)
            } ?: return false
            val now = java.time.LocalDate.now().toString()
            store.saveSetting("auto_backup_last", now)
            store.saveSetting("last_backup", now)
            store.saveSetting("auto_backup_error", "")
            true
        } catch (e: Exception) {
            store.saveSetting("auto_backup_error", e.message ?: "Could not write the backup file")
            false
        }
    }

    private fun runAutoBackupIfDue() {
        if (!autoBackupEnabled()) return
        if (store.setting("auto_backup_last", "") == java.time.LocalDate.now().toString()) return
        writeAutoBackup()
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

    /** Open an external link (e.g. a YouTube demonstration) in the system handler. */
    fun openUrl(url: String) {
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Toast.makeText(this, "No app found to open the link", Toast.LENGTH_LONG).show()
        }
    }

    /** Human-readable name for a picked document, for the settings summary. */
    private fun displayNameOf(uri: android.net.Uri): String {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { c ->
                val i = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (i >= 0 && c.moveToFirst()) c.getString(i) else null
            } ?: uri.lastPathSegment ?: "Backup file"
        } catch (e: Exception) {
            uri.lastPathSegment ?: "Backup file"
        }
    }

    fun importBackup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        startActivityForResult(intent, REQ_IMPORT)
    }

    @Deprecated("Deprecated in Java")
    /** Ensure microphone access, prompting once if needed, then call [onResult]. */
    fun requestMic(onResult: (Boolean) -> Unit) {
        if (checkSelfPermission("android.permission.RECORD_AUDIO") ==
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            onResult(true)
            return
        }
        pendingMicResult = onResult
        requestPermissions(arrayOf("android.permission.RECORD_AUDIO"), REQ_MIC)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_MIC) {
            val granted = grantResults.isNotEmpty() &&
                grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
            pendingMicResult?.invoke(granted)
            pendingMicResult = null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data ?: return
        if (resultCode != RESULT_OK) return
        try {
            when (requestCode) {
                REQ_EXPORT -> {
                    val bytes = pendingExport ?: return
                    contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    if (pendingExportToast == "Backup exported") {
                        store.saveSetting("last_backup", java.time.LocalDate.now().toString())
                    }
                    Toast.makeText(this, pendingExportToast, Toast.LENGTH_LONG).show()
                    pendingExport = null
                    refresh()
                }
                REQ_AUTOBACKUP -> {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    store.saveSetting("auto_backup_uri", uri.toString())
                    store.saveSetting("auto_backup_name", displayNameOf(uri))
                    store.saveSetting("auto_backup_last", "")
                    if (writeAutoBackup()) {
                        Toast.makeText(this, "Automatic backup is on", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Couldn't write to that file - try another location", Toast.LENGTH_LONG).show()
                    }
                    refresh()
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
