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

/**
 * Boots the real MainActivity on the JVM via Robolectric: proves the APK's
 * entry point, navigation, store and screens actually run, not just compile.
 */
abstract class SmokeBase {
    protected fun allText(root: View, out: MutableList<String> = ArrayList()): List<String> {
        if (root is TextView) out.add(root.text.toString())
        if (root is ViewGroup) for (i in 0 until root.childCount) allText(root.getChildAt(i), out)
        return out
    }

    /** case-insensitive: section headers render uppercased */
    protected fun String.has(needle: String): Boolean =
        this.lowercase().contains(needle.lowercase())
}

/**
 * One test class per scenario: Robolectric 3.8 sandboxes are per-test but the
 * sqlite4java native layer is per-process, so each class runs in a fresh JVM
 * (forkEvery = 1 in build.gradle.kts).
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class OnboardingSmokeTest : SmokeBase() {
    @Test
    fun launchesIntoOnboardingWithDisclaimer() {
        val activity = Robolectric.setupActivity(MainActivity::class.java)
        val texts = allText(activity.window.decorView).joinToString("\n")
        assertTrue(texts.has("Welcome to RecoverWell"))
        assertTrue(texts.has("supports"))
        assertTrue(texts.has("Red flags"))
    }

}

@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class ScreensSmokeTest : SmokeBase() {
    @Test
    fun mainScreensRenderAfterOnboarding() {
        val activity = Robolectric.setupActivity(MainActivity::class.java)
        // complete onboarding directly through the store
        activity.store.saveProfile(
            activity.store.profile().copy(onboardingComplete = true, disclaimerAcknowledged = true)
        )
        activity.show(MainActivity.Tab.TODAY)
        var texts = allText(activity.window.decorView).joinToString("\n")
        assertTrue(texts.has("checklist"))
        assertTrue(texts.has("Anticoagulant 2.5 mg"))
        assertTrue(texts.has("phase"))

        activity.show(MainActivity.Tab.EXERCISES)
        texts = allText(activity.window.decorView).joinToString("\n")
        assertTrue(texts.has("Exercise library"))

        activity.show(MainActivity.Tab.TRACKER)
        texts = allText(activity.window.decorView).joinToString("\n")
        assertTrue(texts.has("Recovery tracker"))
        assertTrue(texts.has("Milestones"))

        activity.show(MainActivity.Tab.TWIN)
        texts = allText(activity.window.decorView).joinToString("\n")
        assertTrue(texts.has("My leg right now"))
        assertTrue(texts.has("Not yet"))

        activity.show(MainActivity.Tab.MORE)
        texts = allText(activity.window.decorView).joinToString("\n")
        assertTrue(texts.has("Medications"))
    }

}

@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class RedFlagsSmokeTest : SmokeBase() {
    @Test
    fun redFlagsOneTapFromHeader() {
        val activity = Robolectric.setupActivity(MainActivity::class.java)
        activity.store.saveProfile(
            activity.store.profile().copy(onboardingComplete = true, disclaimerAcknowledged = true)
        )
        activity.show(MainActivity.Tab.TODAY)
        activity.pushOverlay { com.recoverwell.app.screens.RedFlagsScreen.build(activity) }
        val texts = allText(activity.window.decorView).joinToString("\n")
        assertTrue(texts.has("DVT"))
        assertTrue(texts.has("re-rupture"))
        assertTrue(texts.has("999"))
    }

}

@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class StoreSmokeTest : SmokeBase() {
    @Test
    fun storePersistsLogsAndEvents() {
        val activity = Robolectric.setupActivity(MainActivity::class.java)
        val store = activity.store
        val log = com.recoverwell.core.model.DailyLog.empty(java.time.LocalDate.now())
            .copy(pain = 4, mood = 3)
        store.saveDailyLog(log)
        assertEquals(4, store.dailyLog(java.time.LocalDate.now()).pain)
        val snapshot = store.snapshot()
        assertEquals(1, snapshot.dailyLogs.size)
        // round-trip through backup codec and restore
        val decoded = com.recoverwell.core.export.BackupCodec.decode(
            com.recoverwell.core.export.BackupCodec.encode(snapshot)
        )
        store.restore(decoded)
        assertEquals(4, store.dailyLog(java.time.LocalDate.now()).pain)
    }
}
