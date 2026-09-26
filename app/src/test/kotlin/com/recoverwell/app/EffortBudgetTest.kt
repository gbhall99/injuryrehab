package com.recoverwell.app

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.recoverwell.core.model.DailyLog
import com.recoverwell.core.model.EventStatus
import com.recoverwell.core.model.EventType
import com.recoverwell.core.model.Swelling
import com.recoverwell.core.protocol.ProtocolRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import java.time.LocalDate

/**
 * Customer-effort budgets from the simplicity audit: each daily journey is
 * driven through the real Activity and its taps are COUNTED, so the effort
 * can't silently creep back. One scenario per class (fresh JVM each).
 */
abstract class EffortBase : JourneyBase() {
    protected var taps = 0

    protected fun find(root: View, match: (View) -> Boolean): View? {
        if (root.visibility != View.VISIBLE) return null
        if (match(root)) return root
        if (root is ViewGroup) for (i in 0 until root.childCount) find(root.getChildAt(i), match)?.let { return it }
        return null
    }

    /** Tap the first clickable view whose content description contains [desc]. */
    protected fun tapDesc(root: View, desc: String) {
        val v = find(root) { it.isClickable && it.contentDescription?.toString()?.contains(desc) == true }
        assertNotNull("nothing labelled \"$desc\"", v)
        v!!.performClick(); taps++
    }

    /** Tap the first clickable text matching [re]. */
    protected fun tapText(root: View, re: Regex): Boolean {
        val v = find(root) { it is TextView && it.isClickable && re.matches(it.text.toString()) } ?: return false
        v.performClick(); taps++
        return true
    }

    protected fun tap(root: View, needle: String) {
        assertTrue("no \"$needle\"", clickByText(root, needle)); taps++
    }

    protected fun load(v: View, acc: IntArray = IntArray(3)): IntArray {
        if (v.visibility != View.VISIBLE) return acc
        if (v is TextView && v.text.isNotBlank()) { acc[0]++; acc[2] += v.text.split(Regex("\\s+")).size }
        if (v.isClickable) acc[1]++
        if (v is ViewGroup) for (i in 0 until v.childCount) load(v.getChildAt(i), acc)
        return acc
    }

    protected fun ready(a: MainActivity, weeks: Long, phase: Int) {
        onboarded(a, weeks, phase)
        a.store.saveSetting("checklist_legend_seen", "1")
    }
}

/** A dose is logged with ONE tap on Today (was: tap + read a dialog + choose). */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class OneTapDoseTest : EffortBase() {
    @Test
    fun doseInOneTap() {
        val a = Robolectric.setupActivity(MainActivity::class.java)
        ready(a, 1, 1)
        a.store.saveMedications(ProtocolRegistry.default.prefillMedications)
        a.show(MainActivity.Tab.TODAY)
        tapDesc(a.window.decorView, "To do: Anticoagulant")
        assertEquals(1, taps)
        val taken = a.store.eventsOn(LocalDate.now()).filter { it.type == EventType.MEDICATION && it.status == EventStatus.TAKEN }
        assertEquals(1, taken.size)
        assertTrue("reassures with the time taken", screen(a).has("Taken at"))
        // the second dose of the day: one more tap, then the finished group folds away
        tapDesc(a.window.decorView, "To do: Anticoagulant")
        assertEquals(2, taps)
        assertTrue(screen(a).has("All 2 doses taken today"))
        assertFalse("folded rows stay out of the way", screen(a).has("Taken at"))
    }
}

/** Today's check-in is ONE tap, and optional metrics are never invented. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class OneTapCheckInTest : EffortBase() {
    @Test
    fun checkInInOneTapWithHonestData() {
        val a = Robolectric.setupActivity(MainActivity::class.java)
        ready(a, 2, 1)
        a.show(MainActivity.Tab.TODAY)
        tapDesc(a.window.decorView, "Pain 3 out of 10")
        assertEquals(1, taps)
        val log = a.store.dailyLog(LocalDate.now())
        assertEquals(3, log.pain)
        assertNull("mood not invented", log.mood)
        assertNull("energy not invented", log.energy)
        assertNull("swelling not invented", log.swelling)
        assertTrue(screen(a).has("Pain 3/10 logged"))
    }
}

/**
 * A whole guided session is one tap per SET: phase-1 session 1 (5 exercises,
 * 10 sets) takes 12 taps from Today - it took ~130 when every rep was a tap
 * and each exercise was opened, started, finished and backed out of alone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class GuidedSessionEffortTest : EffortBase() {
    @Test
    fun wholeSessionInTwelveTaps() {
        com.recoverwell.app.ui.ExerciseDemoView.frameLoopEnabled = false
        val a = Robolectric.setupActivity(MainActivity::class.java)
        ready(a, 1, 1)
        a.show(MainActivity.Tab.TODAY)
        tap(a.window.decorView, "Exercise session 1")
        tap(a.window.decorView, "Start guided session")
        var guard = 0
        while (tapText(a.window.decorView, Regex("(Set|Round) \\d+ done")) && guard++ < 40) Unit
        assertTrue(screen(a).has("Session complete"))
        assertEquals(12, taps)
        val sets = ProtocolRegistry.default.phase(1).exercises.sumOf { it.sets }
        assertEquals("one tap per set", 10, sets)
        val logged = a.store.eventsOn(LocalDate.now())
            .filter { it.type == EventType.EXERCISE && it.slotKey == "session1" && it.status == EventStatus.DONE }
        assertEquals(5, logged.map { it.refId }.toSet().size)
    }
}

/** The clinic changed the boot angle: three taps from My leg, where the setting is shown. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class BootAdjustEffortTest : EffortBase() {
    @Test
    fun adjustBootInThreeTaps() {
        val a = Robolectric.setupActivity(MainActivity::class.java)
        ready(a, 4, 2)
        a.store.saveProfile(a.store.profile().copy(currentWedges = 20))
        a.show(MainActivity.Tab.TWIN)
        tapDesc(a.window.decorView, "Adjust VACOped boot setting")
        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        tapDesc(dialog.window.decorView, "Decrease")
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick(); taps++
        assertEquals(3, taps)
        assertEquals(15, a.store.profile().currentWedges)
    }
}

/** Screen-load budgets: the plan editor and Progress stay as lean as the audit left them. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class ScreenLoadBudgetTest : EffortBase() {
    @Test
    fun screensStayLean() {
        val a = Robolectric.setupActivity(MainActivity::class.java)
        ready(a, 4, 2)
        val today = LocalDate.now()
        for (i in 1..4) a.store.saveDailyLog(DailyLog(today.minusDays(i.toLong()), 3, Swelling.MILD, null, true,
            null, null, 3, 3, null))
        a.pushOverlay("Configure my plan") { com.recoverwell.app.screens.MoreScreen.planEditor(a) }
        val plan = load(a.window.decorView)
        assertTrue("plan editor texts ${plan[0]} (was 129)", plan[0] <= 60)
        assertTrue("plan editor targets ${plan[1]} (was 76)", plan[1] <= 36)
        a.show(MainActivity.Tab.TRACKER)
        val progress = load(a.window.decorView)
        assertTrue("progress words ${progress[2]} (was 393)", progress[2] <= 240)
        a.show(MainActivity.Tab.TODAY)
        val texts = screen(a)
        // one home per fact: streaks in the tiles (not also hero chips), no tiles for tabs/sections
        assertFalse(texts.has("-day meds"))
        assertFalse(texts.has("Today's routine"))
        assertFalse(texts.has("Doses & reminders"))
        a.show(MainActivity.Tab.MORE)
        assertFalse("one plan editor, not three", screen(a).has("Phase dates"))
    }
}
