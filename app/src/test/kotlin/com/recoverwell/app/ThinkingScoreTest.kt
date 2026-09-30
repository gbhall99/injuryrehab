package com.recoverwell.app

import android.app.AlertDialog
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.protocol.ProtocolRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowDisplay
import java.io.File

/**
 * Customer Thinking Score (CTS): the seconds a customer spends working out WHICH
 * control to press, for every step of every journey. Measured on the real
 * Activity laid out as a typical phone (360 x 740 dp), using published
 * human-performance constants (see docs/CUSTOMER_THINKING_AUDIT.md):
 *
 *   step seconds = Choose + Find + Words + Scroll
 *   Choose  0.15 s x log2(choices + 1)   Hick-Hyman: more visible controls, longer to decide
 *                                        (a rating scale - a row of numbered buttons - is one control)
 *   Find    0.2 s per place looked       headings/rows passed before the right one;
 *                                        0 if it is the screen's only primary button
 *   Words   +1.35 s if the control (and its heading) doesn't use the customer's words,
 *                   or is an unlabelled icon (one KLM "mental operator" to translate)
 *   Scroll  +1.35 s if it is off-screen (the customer has to guess it's further down)
 *
 * A step is AT A GLANCE at <= 1.5 s: a normal phone screen (~24 controls = 0.7 s) with
 * the right control within the first four places (0.8 s), in the customer's words,
 * without a scroll-guess. Journey score = 10 x the average of min(1, 1.5 / step seconds).
 */
object Thinking {
    const val PER_BIT = 0.15
    const val PER_PLACE = 0.2
    const val MENTAL = 1.35
    const val GLANCE = 1.5
    const val WIDTH = 360
    const val HEIGHT = 740

    data class Step(
        val journey: String, val step: String, val choices: Int, val places: Int,
        val words: Boolean, val onScreen: Boolean
    ) {
        val choose get() = PER_BIT * Math.log(choices + 1.0) / Math.log(2.0)
        val seconds get() = choose + PER_PLACE * places + (if (words) 0.0 else MENTAL) + (if (onScreen) 0.0 else MENTAL)
        val mark get() = Math.min(1.0, GLANCE / seconds)
        fun tsv() = listOf(journey, step, choices, places, words, onScreen, "%.2f".format(seconds)).joinToString("\t")
    }

    /** A realistic phone window - Robolectric's default is a tiny 320 x 470. */
    fun phone() {
        Shadows.shadowOf(ShadowDisplay.getDefaultDisplay()).apply { setWidth(WIDTH); setHeight(HEIGHT) }
    }

    // ------------------------------------------------------------------ probing

    private fun walk(v: View, out: MutableList<View>) {
        if (v.visibility != View.VISIBLE) return
        out.add(v)
        if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i), out)
    }

    fun all(root: View): List<View> = ArrayList<View>().also { walk(root, it) }

    fun y(v: View): Int = IntArray(2).also { v.getLocationOnScreen(it) }[1]
    fun x(v: View): Int = IntArray(2).also { v.getLocationOnScreen(it) }[0]

    fun onScreen(v: View, dialog: Boolean): Boolean {
        if (dialog) return true
        val r = Rect()
        return v.getGlobalVisibleRect(r) && r.height() * 2 >= v.height
    }

    fun inScroll(v: View): Boolean {
        var p: android.view.ViewParent? = v.parent
        while (p != null) { if (p is ScrollView) return true; p = p.parent }
        return false
    }

    /** The words a sighted customer can read on the control itself. */
    fun ownText(v: View): String = when (v) {
        is EditText -> (v.text?.toString().orEmpty() + " " + (v.hint ?: "")).trim()
        is TextView -> v.text.toString()
        is ViewGroup -> all(v).filterIsInstance<TextView>().joinToString(" ") { it.text.toString() }.trim()
        else -> ""
    }

    /** The card title right above/around the control (e.g. "How's your pain today?"). */
    fun cardTitle(v: View): String {
        var child: View = v
        var p = v.parent
        var depth = 0
        while (p is ViewGroup && depth < 4) {
            val first = p.getChildAt(0)
            if (first !== child && first is TextView && !first.isClickable && first.text.isNotBlank()) return first.text.toString()
            child = p; p = p.parent; depth++
        }
        return ""
    }

    /**
     * Robolectric doesn't measure text (TextViews have no text width or height in the
     * JVM), which would make every screen look shorter than on a phone. Give each text
     * its real size - Roboto metrics: line height 1.17 x text size (+ font padding),
     * average glyph 0.5 x text size (0.55 bold), wrapped to its real width - then lay
     * the window out again, so "on screen" means what it would on a device.
     */
    fun realistic(root: View) {
        for (v in all(root)) {
            if (v !is TextView) continue
            val text = if (v is EditText) (v.text?.toString().takeUnless { it.isNullOrEmpty() } ?: v.hint?.toString() ?: "")
                else v.text.toString()
            if (text.isEmpty()) continue
            val size = v.textSize
            val glyph = size * (if (v.typeface?.isBold == true) 0.55f else 0.5f)
            val padH = v.paddingLeft + v.paddingRight
            // room to grow into: the nearest ancestor with a real (not wrap-content) width
            var box: View? = v.parent as? View
            while (box != null && box.layoutParams?.width == ViewGroup.LayoutParams.WRAP_CONTENT) box = box.parent as? View
            val parentW = (box?.width ?: WIDTH) - (box?.let { it.paddingLeft + it.paddingRight } ?: 0)
            val wrap = v.layoutParams?.width == ViewGroup.LayoutParams.WRAP_CONTENT
            val longest = text.split("\n").maxOf { it.length } * glyph
            if (wrap) v.minWidth = maxOf(v.minWidth, minOf(longest.toInt() + padH, parentW))
            val avail = ((if (wrap) parentW else v.width) - padH).coerceAtLeast(1)
            var lines = text.split("\n").sumOf { Math.ceil((it.length * glyph / avail).toDouble()).toInt().coerceAtLeast(1) }
            if (v.maxLines in 1 until lines) lines = v.maxLines
            v.minHeight = maxOf(v.minHeight, (lines * size * 1.17f + size * 0.16f).toInt() + v.paddingTop + v.paddingBottom)
        }
        root.measure(View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(HEIGHT, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, WIDTH, HEIGHT)
    }

    fun measure(journey: String, step: String, root: View, target: View, words: List<String>, dialog: Boolean): Step {
        if (!dialog) realistic(root)
        val views = all(root)
        val clickables = views.filter { it.isClickable }
        val visible = clickables.filter { onScreen(it, dialog) }
        // a rating scale (a row of numbered buttons) is ONE control when deciding where to go
        val scaleExtras = visible.filter { ownText(it).trim().toIntOrNull() != null }
            .groupBy { it.parent }.values.filter { it.size >= 3 }.sumOf { it.size - 1 }
        val content = views.filter { dialog || inScroll(it) }
        val chrome = !dialog && !inScroll(target)
        val places: Int = if (chrome) {
            // a bar (tabs, app-bar tools) is read left to right
            val bar = target.parent as ViewGroup
            all(bar).filter { it.isClickable && it.parent === bar }.sortedBy { x(it) }.indexOf(target).coerceAtLeast(0)
        } else {
            val primaries = visible.filter { it.tag == Ui.TAG_PRIMARY && content.contains(it) }
            if (target.tag == Ui.TAG_PRIMARY && primaries.size == 1 && primaries[0] === target) 0
            else findPlaces(content, target)
        }
        val section = content.filter { it.tag == Ui.TAG_HEADING && y(it) < y(target) }.maxByOrNull { y(it) }
        val own = ownText(target)
        val label = (own + " " + cardTitle(target) + " " + ((section as? TextView)?.text ?: "")).lowercase()
        // a "+" beside "Reps" is labelled; an icon with no words beside it is not
        val match = (own.isNotBlank() || cardTitle(target).isNotBlank()) && words.any { label.contains(it.lowercase()) }
        return Step(journey, step, visible.size - scaleExtras, places, match, onScreen(target, dialog))
    }

    /** Places looked before the target: headings skimmed + rows read in its section. */
    private fun findPlaces(content: List<View>, target: View): Int {
        val ty = y(target)
        val headings = content.filter { it.tag == Ui.TAG_HEADING }.map { y(it) }.sorted()
        val rows = content.filter { it.isClickable }.map { y(it) }.distinct().sorted()
        fun rowsBetween(from: Int, to: Int) = rows.filter { it > from && it < to }.map { it / 6 }.distinct().size
        val above = headings.filter { it < ty }
        var places = if (above.isEmpty()) rowsBetween(Int.MIN_VALUE, ty - 3) else {
            val top = if (rows.any { it < headings.first() }) 1 else 0
            top + above.size + rowsBetween(above.last(), ty - 3)
        }
        // within its own row, read left to right - unless it's a number on a scale
        val row = content.filter { it.isClickable && Math.abs(y(it) - ty) < 4 && it.parent === target.parent }.sortedBy { x(it) }
        if (row.size > 1 && ownText(target).trim().toIntOrNull() == null) places += row.indexOf(target).coerceAtLeast(0)
        return places
    }

    fun record(steps: List<Step>) {
        val dir = File("build/thinking").apply { mkdirs() }
        File(dir, steps.first().journey.replace(Regex("[^A-Za-z0-9]+"), "_") + ".tsv")
            .writeText(steps.joinToString("\n") { it.tsv() } + "\n")
    }
}

/** A journey: steps measured, then pressed, exactly as a customer would. */
abstract class ThinkingJourney : JourneyBase() {
    protected lateinit var a: MainActivity
    private val steps = ArrayList<Thinking.Step>()
    abstract val journey: String

    /** Day 23 in a VACOped boot (phase 2), morning, nothing logged yet - an ordinary day,
     *  not a boot-change day (those are [bootChangeDay]). */
    protected fun start(days: Long = 23, phase: Int = 2, meds: Boolean = true): MainActivity {
        Thinking.phone()
        com.recoverwell.app.ui.ExerciseDemoView.frameLoopEnabled = false
        a = Robolectric.setupActivity(MainActivity::class.java)
        a.store.saveProfile(a.store.profile().copy(onboardingComplete = true, disclaimerAcknowledged = true,
            injuryDate = java.time.LocalDate.now().minusDays(days), physioConfirmedPhase = phase))
        if (meds) a.store.saveMedications(ProtocolRegistry.default.prefillMedications)
        a.store.saveSetting("checklist_legend_seen", "1")
        a.store.saveSetting("video_notice_ack", "1")
        a.show(MainActivity.Tab.TODAY)
        return a
    }

    private fun find(root: View, match: (View) -> Boolean): View? =
        Thinking.all(root).firstOrNull { it.isClickable && match(it) }

    /** Measure the step whose right control is the first clickable matching [match], then press it
     *  ([press] = false for a last step that hands off to the system, e.g. a file picker). */
    protected fun step(name: String, words: List<String>, match: (View) -> Boolean, press: Boolean = true) {
        val root = a.window.decorView
        val target = find(root, match) ?: error("$journey / $name: control not found")
        steps.add(Thinking.measure(journey, name, root, target, words, dialog = false))
        if (press) target.performClick()
    }

    /** A bottom-bar tab. */
    protected fun tab(label: String): (View) -> Boolean = { v ->
        !Thinking.inScroll(v) && (v.contentDescription?.toString() == label || v.contentDescription?.toString() == "$label, selected")
    }

    protected fun text(t: String): (View) -> Boolean = { v ->
        val own = Thinking.ownText(v)
        own.contains(t, ignoreCase = true) || v.contentDescription?.toString()?.contains(t, ignoreCase = true) == true
    }

    protected fun desc(t: String): (View) -> Boolean = { v -> v.contentDescription?.toString()?.startsWith(t) == true }

    /** A step in the dialog that's showing. */
    protected fun dialogStep(name: String, words: List<String>, button: String) {
        val d: AlertDialog = ShadowAlertDialog.getLatestAlertDialog() ?: error("$journey / $name: no dialog")
        val root = d.window!!.decorView
        val target = find(root) { it is TextView && it.text.toString().equals(button, ignoreCase = true) }
            ?: error("$journey / $name: no \"$button\"")
        steps.add(Thinking.measure(journey, name, root, target, words, dialog = true))
        target.performClick()
    }

    protected fun finish() {
        Thinking.record(steps)
        if (System.getenv("THINKING_BASELINE") == null) for (s in steps) assertTrue(
            "${s.journey} / ${s.step}: %.2f s to find the right control (choices ${s.choices}, places ${s.places}, words ${s.words}, on screen ${s.onScreen})"
                .format(s.seconds), s.seconds <= Thinking.GLANCE)
    }
}

// ---------------------------------------------------------------------- journeys
// Words are the customer's own: what they have in mind, not the app's labels.

private val DOSE = listOf("blood thinner", "blood-thinning", "dose", "tablet", "injection", "medication", "medicine",
    "pill", "apixaban", "rivaroxaban", "dalteparin", "clexane", "heparin")

@RunWith(RobolectricTestRunner::class) @Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class ThinkDoseTest : ThinkingJourney() {
    override val journey = "Log my morning blood thinner"
    @Test fun run() { start(); step("Tick the dose", DOSE, desc("To do: Anticoagulant")); finish() }
}

@RunWith(RobolectricTestRunner::class) @Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class ThinkPainTest : ThinkingJourney() {
    override val journey = "Log today's pain"
    @Test fun run() { start(); step("Tap my pain score", listOf("pain", "hurt", "sore"), desc("Pain 3 out of 10")); finish() }
}

@RunWith(RobolectricTestRunner::class) @Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class ThinkElevateTest : ThinkingJourney() {
    override val journey = "Tick off leg elevation"
    @Test fun run() {
        // the first elevation usually follows the morning routine: dose taken, pain logged
        start()
        a.findViewByDesc("To do: Anticoagulant")?.performClick()
        com.recoverwell.app.notify.Reminders.recordCheckIn(a, 3)
        a.show(MainActivity.Tab.TODAY)
        step("Tick elevation", listOf("elevat", "leg up", "raise", "feet up"), text("Elevate the leg"))
        finish()
    }
}

@RunWith(RobolectricTestRunner::class) @Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class ThinkSessionTest : ThinkingJourney() {
    override val journey = "Do today's exercises"
    @Test fun run() {
        start()
        step("Go to exercises", listOf("exercise", "workout", "rehab", "session"), tab("Exercises"))
        step("Start today's session", listOf("start", "begin", "session"), text("Start session 1"))
        step("Finish a set", listOf("done", "finished", "next", "complete"), text("Set 1 done"))
        finish()
    }
}

@RunWith(RobolectricTestRunner::class) @Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class ThinkUndoDoseTest : ThinkingJourney() {
    override val journey = "Undo a dose I ticked by mistake"
    @Test fun run() {
        start()
        a.findViewByDesc("To do: Anticoagulant")?.performClick()
        step("Tap the ticked dose", DOSE, desc("Done: Anticoagulant"))
        dialogStep("Undo it", listOf("undo", "not taken", "mistake", "untick"), "Undo")
        finish()
    }
}

@RunWith(RobolectricTestRunner::class) @Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class ThinkSwellingTest : ThinkingJourney() {
    override val journey = "Note that my ankle is more swollen"
    @Test fun run() {
        start()
        step("Find where to note it", listOf("swell", "swollen", "note"), { v ->
            Thinking.ownText(v).let { it.contains("swelling") && it.contains("note") } })
        step("Save it", listOf("save", "done", "log"), text("Save check-in"))
        finish()
    }
}

@RunWith(RobolectricTestRunner::class) @Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class ThinkWatchVideoTest : ThinkingJourney() {
    override val journey = "Watch how to do my knee extensions"
    @Test fun run() {
        start()
        step("Go to exercises", listOf("exercise", "video", "how to"), tab("Exercises"))
        step("Pick the exercise", listOf("knee extension", "knee"), text("Seated knee extensions"))
        step("Play the video", listOf("video", "watch", "demo"), text("Watch video demonstration"))
        finish()
    }
}

@RunWith(RobolectricTestRunner::class) @Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class ThinkRepsTest : ThinkingJourney() {
    override val journey = "Physio says: 15 reps now"
    @Test fun run() {
        start()
        step("Go to exercises", listOf("exercise", "reps"), tab("Exercises"))
        step("Pick the exercise", listOf("knee extension", "knee"), text("Seated knee extensions"))
        step("Find where to change reps", listOf("reps", "repetitions", "sets"), text("or video"))
        step("Add reps", listOf("reps", "repetitions"), desc("Increase Reps"))
        step("Save", listOf("save", "done"), text("Save changes"))
        finish()
    }
}

@RunWith(RobolectricTestRunner::class) @Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class ThinkCalfTest : ThinkingJourney() {
    override val journey = "My calf is hot and swollen"
    @Test fun run() {
        start()
        step("Find urgent help", listOf("calf", "clot", "dvt", "swell", "urgent", "help", "emergency", "warning", "red flag"),
            desc("Red flags"))
        step("Call for same-day advice", listOf("calf", "clot", "dvt", "111", "doctor", "gp"),
            { v -> Thinking.ownText(v) == "Call 111" })
        finish()
    }
}

@RunWith(RobolectricTestRunner::class) @Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class ThinkBootScheduledTest : ThinkingJourney() {
    override val journey = "Record today's boot change"
    @Test fun run() {
        start(days = 21) // a boot-change day
        step("Find the boot change", listOf("boot", "angle", "wedge", "heel", "dial"), desc("To do: Boot change due"))
        finish()
    }
}

@RunWith(RobolectricTestRunner::class) @Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class ThinkBootAdjustTest : ThinkingJourney() {
    override val journey = "Clinic set a different boot angle"
    @Test fun run() {
        start()
        step("Find the boot setting", listOf("boot", "angle", "wedge", "heel", "dial"), { v ->
            v.contentDescription?.toString()?.startsWith("Change boot angle") == true })
        dialogStep("Save", listOf("save", "done"), "Save")
        finish()
    }
}

@RunWith(RobolectricTestRunner::class) @Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class ThinkAppointmentTest : ThinkingJourney() {
    override val journey = "Add my next physio appointment"
    @Test fun run() {
        start()
        step("Find physio visits", listOf("physio", "appointment", "visit", "clinic"), desc("Physio visits"))
        step("Add it", listOf("add", "new", "appointment"), text("Add appointment"))
        finish()
    }
}

@RunWith(RobolectricTestRunner::class) @Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class ThinkReportTest : ThinkingJourney() {
    override val journey = "Send my progress to my physio"
    @Test fun run() {
        start()
        step("Find sharing", listOf("share", "send", "report", "export", "pdf", "physio", "progress"), tab("Progress"))
        step("Make the report", listOf("share", "send", "report", "export", "pdf", "physio"), press = false,
            match = text("Share with your physio"))
        finish()
    }
}

@RunWith(RobolectricTestRunner::class) @Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class ThinkDriveTest : ThinkingJourney() {
    override val journey = "Can I drive yet?"
    @Test fun run() {
        start()
        step("Find somewhere to ask", listOf("drive", "driving", "car", "ask", "question", "coach"),
            desc("Ask - recovery coach"))
        step("Ask it", listOf("drive", "driving", "car"), text("Can I drive yet?"))
        finish()
    }
}

@RunWith(RobolectricTestRunner::class) @Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class ThinkReminderTest : ThinkingJourney() {
    override val journey = "Move my evening dose reminder"
    @Test fun run() {
        start()
        step("Find reminders", listOf("reminder", "medication", "dose", "alarm", "notification", "time", "settings"), tab("Settings"))
        step("Open the medication", listOf("reminder", "medication", "dose", "time", "blood thinner"),
            { v -> v.contentDescription?.toString() == "Medications" })
        step("Change it", listOf("time", "edit", "change", "reminder"), desc("Edit"))
        finish()
    }
}

@RunWith(RobolectricTestRunner::class) @Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class ThinkBackupTest : ThinkingJourney() {
    override val journey = "Back up my data"
    @Test fun run() {
        start()
        step("Find backup", listOf("backup", "back up", "save", "data", "copy", "settings"), tab("Settings"))
        step("Open backup", listOf("backup", "back up", "save", "data", "copy"),
            { v -> v.contentDescription?.toString() == "Backup, restore & export" })
        step("Back up", listOf("backup", "back up"), press = false, match = text("Back up now"))
        finish()
    }
}

@RunWith(RobolectricTestRunner::class) @Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class ThinkTrendTest : ThinkingJourney() {
    override val journey = "How is my pain trending?"
    @Test fun run() { start(); step("Find trends", listOf("progress", "trend", "chart", "history", "pain"), tab("Progress")); finish() }
}

@RunWith(RobolectricTestRunner::class) @Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class ThinkExpectTest : ThinkingJourney() {
    override val journey = "What should I expect at this stage?"
    @Test fun run() {
        start()
        step("Find guidance", listOf("expect", "normal", "stage", "this week"), press = false, match = text("What to expect"))
        finish()
    }
}

@RunWith(RobolectricTestRunner::class) @Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class ThinkSportTest : ThinkingJourney() {
    override val journey = "Change my sport to tennis"
    @Test fun run() {
        start()
        step("Find my goal", listOf("sport", "padel", "goal", "tennis", "settings", "profile"), tab("Settings"))
        step("Open injury & goal", listOf("sport", "padel", "goal", "tennis"), { v -> v.contentDescription?.toString() == "Injury & goal" })
        step("Change the sport", listOf("sport", "padel", "tennis"), text("Padel"))
        finish()
    }
}

@RunWith(RobolectricTestRunner::class) @Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class ThinkSetupTest : ThinkingJourney() {
    override val journey = "First-time setup"
    @Test fun run() {
        Thinking.phone()
        a = Robolectric.setupActivity(MainActivity::class.java)
        a.store.saveProfile(a.store.profile().copy(injuryDate = java.time.LocalDate.now().minusWeeks(1)))
        val next = listOf("continue", "next", "agree", "understand", "ok", "finish", "start", "done")
        step("Accept the safety note", next, text("I understand"))
        step("Confirm my injury", next, text("Confirm & continue"))
        step("Confirm my medication", next, text("Confirm & continue"))
        step("Finish", next, text("Finish setup"))
        finish()
    }
}

private fun MainActivity.findViewByDesc(prefix: String): View? =
    Thinking.all(window.decorView).firstOrNull { it.isClickable && it.contentDescription?.toString()?.startsWith(prefix) == true }
