package com.recoverwell.app

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.recoverwell.core.protocol.ProtocolRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Reproductions for "different exercises all open the same detail". Covers both
 * the Exercises tab list and the Today exercise sessions (where tapping a session
 * used to jump straight into the first exercise, making every session identical).
 *
 * One activity / one test method: creating a second MainActivity in the same
 * class trips Robolectric's reused SQLite connection.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class ExerciseListTapTest {

    private fun allText(root: View, out: MutableList<String> = ArrayList()): List<String> {
        if (root is TextView) out.add(root.text.toString())
        if (root is ViewGroup) for (i in 0 until root.childCount) allText(root.getChildAt(i), out)
        return out
    }

    private fun decorText(activity: MainActivity) =
        allText(activity.window.decorView).joinToString("\n").lowercase()

    private fun findClickable(root: View, descContains: String): View? {
        val d = root.contentDescription?.toString()
        if (d != null && root.isClickable && d.contains(descContains)) return root
        if (root is ViewGroup) for (i in 0 until root.childCount) {
            findClickable(root.getChildAt(i), descContains)?.let { return it }
        }
        return null
    }

    @Test
    fun differentExercisesOpenDifferentDetails() {
        com.recoverwell.app.ui.ExerciseDemoView.frameLoopEnabled = false
        val activity = Robolectric.setupActivity(MainActivity::class.java)
        activity.store.saveProfile(
            activity.store.profile().copy(onboardingComplete = true, disclaimerAcknowledged = true)
        )
        val phase1 = ProtocolRegistry.default.phase(1).exercises
        assertTrue("phase 1 has multiple exercises to distinguish", phase1.size >= 2)
        val first = phase1[0]
        val second = phase1[1]

        // --- Exercises tab: each row opens its own exercise ---
        activity.show(MainActivity.Tab.EXERCISES)
        findClickable(activity.window.decorView, first.name)!!.performClick()
        assertTrue("first detail shows its own cue",
            decorText(activity).contains(first.cues.first().lowercase()))
        activity.popOverlay()

        activity.show(MainActivity.Tab.EXERCISES)
        findClickable(activity.window.decorView, second.name)!!.performClick()
        val secondDetail = decorText(activity)
        assertTrue("second detail shows its own cue",
            secondDetail.contains(second.cues.first().lowercase()))
        assertFalse("second detail must not still show the first exercise's cue",
            secondDetail.contains(first.cues.first().lowercase()))
        activity.popOverlay()

        // --- Today: a session lists ALL its exercises, not just the first ---
        activity.show(MainActivity.Tab.TODAY)
        val sessionRow = findClickable(activity.window.decorView, "Exercise session 1")
        assertNotNull("Today shows an exercise session row", sessionRow)
        sessionRow!!.performClick()

        val overview = decorText(activity)
        for (ex in phase1) {
            assertTrue("session overview lists ${ex.name}", overview.contains(ex.name.lowercase()))
        }

        // opening a specific exercise from the session opens THAT exercise, scoped
        // to the session for logging
        findClickable(activity.window.decorView, second.name)!!.performClick()
        val detail = decorText(activity)
        assertTrue("opens the tapped exercise's cue",
            detail.contains(second.cues.first().lowercase()))
        assertTrue("logging is scoped to the session", detail.contains("mark done for session 1"))
    }
}
