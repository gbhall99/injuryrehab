package com.recoverwell.app

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Journey-level guards for the UX audit fixes. One scenario per class: each
 * runs in a fresh JVM (see SmokeBase).
 */
abstract class JourneyBase : SmokeBase() {
    protected fun clickByText(root: View, needle: String): Boolean {
        if (root is TextView && root.text.toString().contains(needle, ignoreCase = true) && root.isClickable) {
            root.performClick(); return true
        }
        // whole-row tap targets carry their label on a child TextView
        if (root is ViewGroup && root.isClickable && allText(root).any { it.contains(needle, ignoreCase = true) } &&
            root.childCount > 0 && root !is android.widget.ScrollView) {
            root.performClick(); return true
        }
        if (root is ViewGroup) for (i in 0 until root.childCount) if (clickByText(root.getChildAt(i), needle)) return true
        return false
    }

    protected fun screen(a: MainActivity) = allText(a.window.decorView).joinToString("\n")

    protected fun onboarded(a: MainActivity, weeksAgo: Long, phase: Int) {
        a.store.saveProfile(a.store.profile().copy(
            onboardingComplete = true, disclaimerAcknowledged = true,
            injuryDate = LocalDate.now().minusWeeks(weeksAgo), physioConfirmedPhase = phase))
    }
}

/** Joining at week 14 must not restart the plan on week-one boot exercises. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class LateJoinerOnboardingTest : JourneyBase() {
    @Test
    fun midRecoveryJoinerPicksTheirRealStage() {
        val a = Robolectric.setupActivity(MainActivity::class.java)
        a.store.saveProfile(a.store.profile().copy(injuryDate = LocalDate.now().minusWeeks(14)))
        val decor = a.window.decorView
        assertTrue(clickByText(decor, "I understand"))
        assertTrue(clickByText(decor, "Confirm & continue"))
        assertTrue("stage step shows", screen(a).has("Where are you now?"))
        assertTrue("typical stage is flagged", screen(a).has("typical for your date"))
        assertTrue(clickByText(decor, "Confirm & continue"))
        assertTrue(screen(a).has("medication reminders"))
        assertTrue(clickByText(decor, "Confirm & continue"))
        assertTrue(clickByText(decor, "Finish setup"))
        val p = a.store.profile()
        assertEquals(4, p.physioConfirmedPhase)
        assertNotNull("out of the boot by phase 4", p.bootWeanedDate)
        assertTrue("unknown confirmation dates aren't invented", p.phaseConfirmedDates.isEmpty())
        assertTrue(screen(a).has("Phase 4"))
        assertFalse("no week-one boot checks", screen(a).has("Boot check"))
    }
}

/** Red flags: reading a sign to dialling the right service is one tap. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class RedFlagCallButtonsTest : JourneyBase() {
    @Test
    fun redFlagsOfferTheRightCalls() {
        val a = Robolectric.setupActivity(MainActivity::class.java)
        onboarded(a, 3, 2)
        a.pushOverlay("Red flags") { com.recoverwell.app.screens.RedFlagsScreen.build(a) }
        val texts = screen(a)
        assertTrue(texts.has("Call 999"))
        assertTrue(texts.has("Call 111"))
        assertTrue("prompts to save the clinic number", texts.has("Save your clinic's number"))
        assertTrue(texts.has("head"))
        assertTrue(clickByText(a.window.decorView, "Call 999"))
        val dial = Shadows.shadowOf(a).nextStartedActivity
        assertEquals(Intent.ACTION_DIAL, dial.action)
        assertEquals("tel:999", dial.data.toString())

        a.store.saveProfile(a.store.profile().copy(clinicPhone = "024 7696 5000"))
        a.popOverlay()
        a.pushOverlay("Red flags") { com.recoverwell.app.screens.RedFlagsScreen.build(a) }
        assertTrue(screen(a).has("Call my clinic"))
    }
}

/** The phase gate is the app's core mechanic: never hidden behind first-run calm. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class PhaseGatePinnedTest : JourneyBase() {
    @Test
    fun gateShowsWithNoCheckInsAndConfirmsWithASummary() {
        val a = Robolectric.setupActivity(MainActivity::class.java)
        onboarded(a, 3, 1)
        a.show(MainActivity.Tab.TODAY)
        assertTrue(screen(a).has("Ready for phase 2?"))
        assertTrue(clickByText(a.window.decorView, "My physio confirmed it"))
        val confirm = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
        confirm.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        assertEquals(2, a.store.profile().physioConfirmedPhase)
        val summary = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
        assertTrue(Shadows.shadowOf(summary).title.toString().contains("Phase 2 unlocked"))
    }
}

/** Offline coach: a described symptom gets the safety answer and the right call. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class CoachSafetyTest : JourneyBase() {
    @Test
    fun coachAnswersEverydayAndSymptomQuestions() {
        val a = Robolectric.setupActivity(MainActivity::class.java)
        onboarded(a, 1, 1)
        a.openAsk()
        // a short list first (six, one from each topic in turn), the rest one tap away
        assertTrue(screen(a).has("Can I drive yet?"))
        assertFalse(screen(a).has("Can I take the boot off to sleep?"))
        assertTrue(clickByText(a.window.decorView, "more questions"))
        assertTrue(screen(a).has("Can I take the boot off to sleep?"))
        assertTrue(clickByText(a.window.decorView, "Can I take the boot off to sleep?"))
        assertTrue(screen(a).has("unguarded movement"))
        assertTrue(clickByText(a.window.decorView, "I'm worried about a clot"))
        assertTrue(screen(a).has("Call 111"))
    }
}

/** Timed activities get a real timer, not a "hold". */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class GuidedTimerTest : JourneyBase() {
    @Test
    fun bikeSessionIsTimed() {
        com.recoverwell.app.ui.ExerciseDemoView.frameLoopEnabled = false
        val a = Robolectric.setupActivity(MainActivity::class.java)
        onboarded(a, 9, 3)
        val bike = com.recoverwell.core.protocol.ProtocolRegistry.default.phase(3).exercises.first { it.id == "p3_bike" }
        a.pushOverlay(bike.name) { com.recoverwell.app.screens.ExercisesScreen.exerciseDetail(a, bike) }
        assertTrue(screen(a).has("rounds") || screen(a).has("round"))
        assertTrue(screen(a).has("How it should feel"))
        assertTrue(clickByText(a.window.decorView, "Start guided session"))
        assertTrue(screen(a).has("Start 10 min timer"))
        assertFalse(screen(a).has("hold"))
    }
}

/** Phase 3: the app asks about coming out of the boot, and "Not yet" snoozes it. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class OutOfBootPromptTest : JourneyBase() {
    @Test
    fun outOfBootPromptAndSnooze() {
        val a = Robolectric.setupActivity(MainActivity::class.java)
        onboarded(a, 9, 3)
        a.show(MainActivity.Tab.TODAY)
        assertTrue(screen(a).has("Out of the vacoped boot yet?"))
        assertTrue(clickByText(a.window.decorView, "Not yet"))
        assertFalse(screen(a).has("Out of the vacoped boot yet?"))
    }
}
