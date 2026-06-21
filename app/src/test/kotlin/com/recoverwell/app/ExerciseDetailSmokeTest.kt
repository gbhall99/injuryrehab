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

/** Opens an exercise detail (demo view, cues, why, precaution, session logging) for every phase. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class ExerciseDetailSmokeTest {

    private fun allText(root: View, out: MutableList<String> = ArrayList()): List<String> {
        if (root is TextView) out.add(root.text.toString())
        if (root is ViewGroup) for (i in 0 until root.childCount) allText(root.getChildAt(i), out)
        return out
    }

    @Test
    fun detailRendersForFirstExerciseOfEveryPhase() {
        // keep Robolectric's scheduler from spinning the 60fps demo loop
        com.recoverwell.app.ui.ExerciseDemoView.frameLoopEnabled = false
        val activity = Robolectric.setupActivity(MainActivity::class.java)
        activity.store.saveProfile(
            activity.store.profile().copy(onboardingComplete = true, disclaimerAcknowledged = true)
        )
        for (phase in ProtocolRegistry.default.phases) {
            val spec = phase.exercises.first()
            activity.show(MainActivity.Tab.EXERCISES)
            activity.pushOverlay {
                com.recoverwell.app.screens.ExercisesScreen.exerciseDetail(activity, spec)
            }
            val texts = allText(activity.window.decorView).joinToString("\n").lowercase()
            assertTrue("name for ${spec.id}", texts.contains(spec.name.lowercase()))
            assertTrue("why for ${spec.id}", texts.contains("why this matters"))
            assertTrue("cue for ${spec.id}", texts.contains(spec.cues.first().lowercase()))
            assertTrue("precaution for ${spec.id}", texts.contains(spec.precaution.lowercase().take(30)))
            if (spec.phase == 1) {
                assertTrue("session logging for current phase", texts.contains("mark session 1 done"))
            } else {
                assertTrue("lock warning for future phase", texts.contains("not unlocked yet"))
            }
        }
    }
}
