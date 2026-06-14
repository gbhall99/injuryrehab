package com.recoverwell.app

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.recoverwell.app.screens.PhysioScreen
import com.recoverwell.app.screens.ReturnToSportScreen
import com.recoverwell.app.screens.WellbeingScreen
import com.recoverwell.app.ui.SceneView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Accessibility guards. One scenario per class: Robolectric's SQLite layer is
 * per-process, so a fresh JVM per class (forkEvery = 1) avoids stale handles.
 */
private fun collect(root: View, out: MutableList<View> = ArrayList()): List<View> {
    out.add(root)
    if (root is ViewGroup) for (i in 0 until root.childCount) collect(root.getChildAt(i), out)
    return out
}

private fun MainActivity.makeReady() {
    store.saveProfile(store.profile().copy(
        onboardingComplete = true, disclaimerAcknowledged = true,
        injuryDate = LocalDate.now().minusWeeks(30), physioConfirmedPhase = 4))
}

@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class DecorativeSceneAccessibilityTest {
    @Test
    fun decorativeScenesAreSkippedButLabelledOnesAreAnnounced() {
        val a = Robolectric.setupActivity(MainActivity::class.java)
        val plain = SceneView(a) {}
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, plain.importantForAccessibility)
        plain.contentDescription = "Pain trend chart"
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_YES, plain.importantForAccessibility)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class IconLabelAccessibilityTest {
    @Test
    fun clickableIconControlsAllHaveContentDescriptions() {
        val a = Robolectric.setupActivity(MainActivity::class.java)
        a.makeReady()
        a.show(MainActivity.Tab.TODAY)
        val offenders = collect(a.window.decorView).filter {
            it is ImageView && it.isClickable && it.contentDescription.isNullOrBlank()
        }
        assertTrue("clickable icons must be labelled for TalkBack: $offenders", offenders.isEmpty())
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class NewScreensAccessibilityTest {
    @Test
    fun returnToSportPhysioAndWellbeingBuildWithoutCrashing() {
        val a = Robolectric.setupActivity(MainActivity::class.java)
        a.makeReady()
        assertTrue(collect(ReturnToSportScreen.build(a)).size > 5)
        assertTrue(collect(PhysioScreen.build(a)).size > 5)
        assertTrue(collect(WellbeingScreen.build(a)).size > 5)
    }
}
