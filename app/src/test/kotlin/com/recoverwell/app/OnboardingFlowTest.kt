package com.recoverwell.app

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Clicks through the real onboarding flow - the path a fresh install takes. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class OnboardingFlowTest {

    private fun allText(root: View, out: MutableList<String> = ArrayList()): List<String> {
        if (root is TextView) out.add(root.text.toString())
        if (root is ViewGroup) for (i in 0 until root.childCount) allText(root.getChildAt(i), out)
        return out
    }

    private fun clickByText(root: View, needle: String): Boolean {
        if (root is TextView && root.text.toString().contains(needle, ignoreCase = true)
            && root.isClickable) {
            root.performClick()
            return true
        }
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                if (clickByText(root.getChildAt(i), needle)) return true
            }
        }
        return false
    }

    @Test
    fun fullOnboardingClickThrough() {
        val activity = Robolectric.setupActivity(MainActivity::class.java)
        val decor = activity.window.decorView

        // splash -> details
        assertTrue("continue button present", clickByText(decor, "I understand"))
        var texts = allText(decor).joinToString("\n").lowercase()
        assertTrue("step 1 shows", texts.contains("check your details"))

        // details -> medication
        assertTrue(clickByText(decor, "Confirm & continue"))
        texts = allText(decor).joinToString("\n").lowercase()
        assertTrue("step 2 shows", texts.contains("medication reminders"))

        // medication -> Today
        assertTrue(clickByText(decor, "Confirm & continue"))
        texts = allText(decor).joinToString("\n").lowercase()
        assertTrue("today shows", texts.contains("done today"))
        assertTrue(activity.store.profile().onboardingComplete)
    }
}
