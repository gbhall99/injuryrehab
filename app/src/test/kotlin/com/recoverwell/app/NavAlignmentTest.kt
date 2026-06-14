package com.recoverwell.app

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards the bottom-nav label alignment: every tab label must be a full-width,
 * centre-gravity TextView so it lines up under its icon (regression from a
 * device report where labels were left-shifted).
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class NavAlignmentTest {

    private fun collect(root: View, out: MutableList<TextView> = ArrayList()): List<TextView> {
        if (root is TextView) out.add(root)
        if (root is ViewGroup) for (i in 0 until root.childCount) collect(root.getChildAt(i), out)
        return out
    }

    @Test
    fun bottomNavLabelsAreCentred() {
        val activity = Robolectric.setupActivity(MainActivity::class.java)
        activity.store.saveProfile(
            activity.store.profile().copy(onboardingComplete = true, disclaimerAcknowledged = true)
        )
        activity.show(MainActivity.Tab.TODAY)

        val tabLabels = MainActivity.Tab.values().map { it.label }.toSet()
        // nav labels are the centre-gravity, full-width TextViews carrying a tab name
        val navLabels = collect(activity.window.decorView).filter {
            it.text.toString() in tabLabels &&
                (it.gravity and Gravity.CENTER_HORIZONTAL) != 0 &&
                it.layoutParams?.width == ViewGroup.LayoutParams.MATCH_PARENT
        }
        assertEquals("all 5 tab labels centred under their icons", 5, navLabels.size)
    }
}
