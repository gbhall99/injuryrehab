package com.recoverwell.app

import android.view.View
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.model.DailyLog
import com.recoverwell.core.protocol.ProtocolRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

/**
 * Decision load: what each screen puts in front of the customer, whether or not
 * they need it today - every control (the whole screen, not just what's visible),
 * every section, and how many screens of scrolling. Measured on a regular user's
 * ordinary day (day 23, phase 2, a few days of check-ins), realistic text sizes.
 */
object Load {
    data class Screen(val name: String, val controls: Int, val sections: Int, val screens: Double,
                      val words: Int, val labels: List<String>, val blocks: List<String> = emptyList()) {
        fun tsv() = listOf(name, controls, sections, "%.2f".format(screens), words, labels.joinToString(" | ")).joinToString("\t")
    }

    /** Where the scrolling comes from: each top-level block's height (px at the probe's density) and first words. */
    private fun blocks(scroll: ScrollView?): List<String> {
        val column = (scroll?.getChildAt(0) as? android.view.ViewGroup) ?: return emptyList()
        return (0 until column.childCount).map { column.getChildAt(it) }.filter { it.height > 0 }.map { v ->
            val first = Thinking.all(v).filterIsInstance<TextView>().firstOrNull { it.text.isNotBlank() }?.text
            "${v.height}:${first.toString().replace(Regex("\\s+"), " ").take(28)}"
        }
    }

    // Budgets per screen (see docs/SIMPLIFICATION_DECISION_LOAD_AUDIT.md):
    //  - controls: 15 (Hick's law: choice time grows with log2(n+1); 15 is 4 bits)
    //  - sections: 4 (working memory holds about four chunks - Cowan)
    //  - scroll: 2 screens where you decide or do something (most attention lands in the first
    //    two screenfuls); 3 for pages opened on purpose to read
    const val CONTROLS = 15
    const val SECTIONS = 4
    const val SCROLL = 2.0
    const val READING_SCROLL = 3.0
    /** Pages opened to read (the old and new names, so a baseline run scores the same way). */
    val READING = setOf("Your plan", "Phase guide", "What to expect", "How you're doing",
        "Return to sport", "Red flags", "About")
    /**
     * The one deliberate exception: Red flags lists every sign of five emergencies and folds
     * none of them away (safety over brevity). It was 5.7 screens with everything listed twice.
     */
    const val RED_FLAGS_SCROLL = 4.2

    fun scrollBudget(name: String) = when {
        name == "Red flags" -> RED_FLAGS_SCROLL
        name in READING -> READING_SCROLL
        else -> SCROLL
    }

    /** 0-10: each budget met scores 1, an overrun scores budget/actual; the mean of the three. */
    fun score(sc: Screen): Double {
        val b = if (sc.name in READING) READING_SCROLL else SCROLL
        return 10 * listOf(minOf(1.0, CONTROLS.toDouble() / sc.controls.coerceAtLeast(1)),
            minOf(1.0, SECTIONS.toDouble() / sc.sections.coerceAtLeast(1)),
            minOf(1.0, b / sc.screens)).average()
    }

    fun measure(a: MainActivity, name: String): Screen {
        Thinking.realistic(a.window.decorView)
        val views = Thinking.all(a.screenView)
        val clicks = views.filter { it.isClickable }
        // a rating scale (a row of numbered buttons) is one control
        val scaleExtras = clicks.filter { Thinking.ownText(it).trim().toIntOrNull() != null }
            .groupBy { it.parent }.values.filter { it.size >= 3 }.sumOf { it.size - 1 }
        val sections = views.count { it.tag == Ui.TAG_HEADING }
        val scroll = views.filterIsInstance<ScrollView>().firstOrNull()
        val screens = if (scroll == null || scroll.height == 0 || scroll.childCount == 0) 1.0
            else scroll.getChildAt(0).height.toDouble() / scroll.height
        val words = views.filter { it is TextView && it !is EditText }
            .sumOf { (it as TextView).text.toString().split(Regex("\\s+")).count { w -> w.any(Char::isLetterOrDigit) } }
        val labels = clicks.map { (it.contentDescription?.toString() ?: Thinking.ownText(it)).trim().take(48) }
        return Screen(name, clicks.size - scaleExtras, sections, screens, words, labels, blocks(scroll))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class DecisionLoadTest : JourneyBase() {
    private val out = ArrayList<Load.Screen>()
    private lateinit var a: MainActivity

    private fun m(name: String) { out.add(Load.measure(a, name)) }
    private fun overlay(name: String, open: () -> Unit) { open(); m(name); repeat(3) { a.popOverlay() } }
    private fun tap(desc: String) {
        Thinking.all(a.window.decorView).first { it.isClickable &&
            (it.contentDescription?.toString() == desc || Thinking.ownText(it) == desc) }.performClick()
    }

    @Test fun crawl() {
        Thinking.phone()
        com.recoverwell.app.ui.ExerciseDemoView.frameLoopEnabled = false
        a = Robolectric.setupActivity(MainActivity::class.java)
        val today = LocalDate.now()
        a.store.saveProfile(a.store.profile().copy(onboardingComplete = true, disclaimerAcknowledged = true,
            injuryDate = today.minusDays(23), physioConfirmedPhase = 2))
        a.store.saveMedications(ProtocolRegistry.default.prefillMedications)
        a.store.saveSetting("checklist_legend_seen", "1")
        a.store.saveSetting("video_notice_ack", "1")
        // a regular user: a few days of check-ins already logged
        for (d in 1..4) a.store.saveDailyLog(DailyLog(today.minusDays(d.toLong()), 3, null, null, null, null, null, null, null, null))

        for (t in MainActivity.Tab.values()) { a.show(t); m("Tab: " + t.label) }
        val spec = ProtocolRegistry.forProfile(a.store.profile()).phases.first { it.number == 2 }.exercises.first()
        a.show(MainActivity.Tab.TODAY)
        overlay("Exercise detail") { a.pushOverlay(spec.name) { com.recoverwell.app.screens.ExercisesScreen.exerciseDetail(a, spec) } }
        overlay("Today's session list") { tap("0 of 5 done: Exercise session 1") }
        a.show(MainActivity.Tab.EXERCISES)
        overlay("Session player") { tap("Start session 1") }
        a.show(MainActivity.Tab.TODAY)
        overlay("Check-in form") { a.pushOverlay("Daily check-in") { com.recoverwell.app.screens.TodayScreen.checkInOverlay(a, today) } }
        overlay("Your plan") { a.pushOverlay("Your plan") { com.recoverwell.app.screens.PlanGuideScreen.build(a) } }
        overlay("Physio visits") { a.pushOverlay("Physio visits") { com.recoverwell.app.screens.PhysioScreen.build(a) } }
        overlay("New appointment") {
            a.pushOverlay("Physio visits") { com.recoverwell.app.screens.PhysioScreen.build(a) }
            tap("Add appointment")
        }
        overlay("Check-in history") { a.pushOverlay("Check-in history") { com.recoverwell.app.screens.HistoryScreen.checkins(a) } }
        overlay("Stay fit") { a.pushOverlay("Stay fit") { com.recoverwell.app.screens.StayFitScreen.build(a) } }
        overlay("Return to sport") { a.pushOverlay("Return to sport") { com.recoverwell.app.screens.ReturnToSportScreen.build(a) } }
        overlay("Red flags") { a.pushOverlay("Red flags") { com.recoverwell.app.screens.RedFlagsScreen.build(a) } }
        overlay("Recovery coach") { a.openAsk() }
        overlay("Medications") { a.pushOverlay("Medications") { com.recoverwell.app.screens.MoreScreen.medsEditor(a) } }
        overlay("Injury & goal") { a.pushOverlay("Injury & goal") { com.recoverwell.app.screens.MoreScreen.profileEditor(a) } }
        a.show(MainActivity.Tab.MORE)
        overlay("Reminders") { tap("Reminders") }
        a.show(MainActivity.Tab.MORE)
        overlay("Configure my plan") { tap("Configure my plan") }
        a.show(MainActivity.Tab.MORE)
        overlay("Your boot") { tap("Configure my plan"); tap("Your vacoped boot") }
        a.show(MainActivity.Tab.MORE)
        overlay("Backup, restore & export") { tap("Backup, restore & export") }
        a.show(MainActivity.Tab.MORE)
        overlay("Exercise videos") { tap("Exercise videos") }
        a.show(MainActivity.Tab.MORE)
        overlay("About") { tap("About & protocol sources") }

        val dir = File("build/decision-load").apply { mkdirs() }
        File(dir, "screens.tsv").writeText(out.joinToString("\n") { it.tsv() } + "\n")
        File(dir, "blocks.tsv").writeText(out.joinToString("\n") { it.name + "\t" + it.blocks.joinToString(" | ") } + "\n")
        val appScore = out.map { Load.score(it) }.average()
        File(dir, "summary.txt").writeText("screens ${out.size}, controls ${out.sumOf { it.controls }}, " +
            "sections ${out.sumOf { it.sections }}, screens of scroll ${"%.1f".format(out.sumOf { it.screens })}, " +
            "words ${out.sumOf { it.words }}, decision-load score ${"%.1f".format(appScore)}/10\n")

        if (System.getenv("DECISION_BASELINE") != null) return
        for (sc in out) {
            assertTrue("${sc.name}: ${sc.controls} controls (budget ${Load.CONTROLS})", sc.controls <= Load.CONTROLS)
            assertTrue("${sc.name}: ${sc.sections} sections (budget ${Load.SECTIONS})", sc.sections <= Load.SECTIONS)
            assertTrue("${sc.name}: %.2f screens of scroll (budget ${Load.scrollBudget(sc.name)})".format(sc.screens),
                sc.screens <= Load.scrollBudget(sc.name))
        }
        // the whole app, so the simplification can't quietly creep back. The 22 screens crawled
        // on 3.10 had 215 controls, 83 sections and 60.4 screens of scroll; the three screens
        // added since (new appointment, check-in history, your boot) are in these totals too.
        assertTrue("controls ${out.sumOf { it.controls }}", out.sumOf { it.controls } <= 180)
        assertTrue("sections ${out.sumOf { it.sections }}", out.sumOf { it.sections } <= 52)
        assertTrue("scroll ${out.sumOf { it.screens }}", out.sumOf { it.screens } <= 46.0)
    }
}
