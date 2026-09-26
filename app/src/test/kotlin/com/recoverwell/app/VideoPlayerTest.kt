package com.recoverwell.app

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import com.recoverwell.app.screens.ExercisesScreen
import com.recoverwell.app.screens.VideoScreen
import com.recoverwell.app.ui.ExerciseDemoView
import com.recoverwell.app.ui.OwnClips
import com.recoverwell.core.model.ExerciseSpec
import com.recoverwell.core.protocol.ExerciseVideo
import com.recoverwell.core.protocol.ProtocolRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The exercise-video audit's guarantees, driven through the real Activity:
 * every video says honestly where it came from, keeps "your plan comes first"
 * in view, plays with captions on, and degrades gracefully (offline, or a
 * broken clip folder). One scenario per class (fresh JVM each).
 */
abstract class VideoBase : JourneyBase() {
    protected fun <T : View> first(root: View, type: Class<T>): T? {
        if (type.isInstance(root)) return type.cast(root)
        if (root is ViewGroup) for (i in 0 until root.childCount) first(root.getChildAt(i), type)?.let { return it }
        return null
    }

    protected fun exercise(a: MainActivity, id: String): ExerciseSpec =
        ProtocolRegistry.forProfile(a.store.profile()).phases.flatMap { it.exercises }.first { it.id == id }

    protected fun ready(a: MainActivity): MainActivity {
        ExerciseDemoView.frameLoopEnabled = false
        onboarded(a, 16, 4)
        a.store.saveSetting("video_notice_ack", "1")
        return a
    }
}

/** A pre-screened suggestion plays inline, labelled as unchecked, and one tap keeps it. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class SuggestedVideoTest : VideoBase() {
    @Test
    fun suggestionIsHonestSafeAndPinnableWithoutRestarting() {
        val a = ready(Robolectric.setupActivity(MainActivity::class.java))
        val (id, picks) = ExerciseVideo.suggested.entries.first()
        val pick = picks.first()
        VideoScreen.open(a, exercise(a, id))
        val texts = screen(a)
        assertTrue(texts.has("Suggested: ${pick.title}"))
        assertTrue("never passed off as physio-checked", texts.has("not yet checked by a physio"))
        assertTrue("safety strip", texts.has("Your plan comes first"))
        assertTrue(texts.has("no calf stretching"))
        val web = first(a.window.decorView, WebView::class.java)!!
        val page = shadowOf(web).lastLoadDataWithBaseURL
        assertNotNull("the exact video is embedded", page)
        assertTrue(page.data.contains("videoId:\"${pick.videoId}\""))
        assertTrue("captions on by default", page.data.contains("cc_load_policy:1"))
        assertTrue("falls back to the tuned search if it can't embed", page.data.contains("search_query="))

        assertTrue(clickByText(a.window.decorView, "Use this video"))
        assertEquals(pick.videoId, a.store.exerciseOverrides()[id]?.videoId)
        assertTrue(screen(a).has("Your chosen video"))
        assertSame("pinning must not rebuild (and restart) the playing video",
            web, first(a.window.decorView, WebView::class.java))

        // next time it opens as the user's own pick
        a.popOverlay()
        VideoScreen.open(a, exercise(a, id))
        assertTrue(screen(a).has("Your chosen video"))
        assertFalse(screen(a).has("Suggested:"))
    }
}

/** No suggestion: the tuned, sport-aware search shows, with the query visible. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class SearchVideoTest : VideoBase() {
    @Test
    fun searchUsesTheTunedQuery() {
        val a = ready(Robolectric.setupActivity(MainActivity::class.java))
        val spec = exercise(a, "p5_padel")
        assertFalse(spec.id in ExerciseVideo.suggested)
        VideoScreen.open(a, spec)
        val context = ProtocolRegistry.forProfile(a.store.profile()).videoContext
        assertTrue(screen(a).has("YouTube results"))
        assertTrue(screen(a).has(ExerciseVideo.query(spec, context)))
        assertTrue(ExerciseVideo.query(spec, context).contains("padel", ignoreCase = true))
        val web = first(a.window.decorView, WebView::class.java)!!
        assertEquals(ExerciseVideo.youtubeSearchUrl(spec, context), shadowOf(web).lastLoadedUrl)
        assertTrue(screen(a).has("Your plan comes first"))
    }
}

/** Offline: a plain message, not a browser error page. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class OfflineVideoTest : VideoBase() {
    @Test
    fun offlineSaysSoInsteadOfLoading() {
        val a = ready(Robolectric.setupActivity(MainActivity::class.java))
        val cm = a.getSystemService(android.net.ConnectivityManager::class.java)
        shadowOf(cm).setDefaultNetworkActive(false)
        VideoScreen.open(a, exercise(a, "p4_squat"))
        assertTrue(screen(a).has("You're offline"))
        assertTrue(screen(a).has("The animation on the exercise works without one"))
        val web = first(a.window.decorView, WebView::class.java)!!
        assertNull("nothing loaded while offline", shadowOf(web).lastLoadedUrl)
        assertNull(shadowOf(web).lastLoadDataWithBaseURL)
    }
}

/** A clip folder that's gone (revoked, deleted) never blanks the demo: the animation stays. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [26])
class OwnClipsFallbackTest : VideoBase() {
    @Test
    fun brokenClipFolderFallsBackToTheAnimation() {
        val a = ready(Robolectric.setupActivity(MainActivity::class.java))
        a.store.saveSetting(OwnClips.KEY_FOLDER,
            "content://com.android.externalstorage.documents/tree/primary%3AGone")
        OwnClips.rescan()
        val spec = exercise(a, "p4_squat")
        a.pushOverlay(spec.name) { ExercisesScreen.exerciseDetail(a, spec) }
        assertNotNull(first(a.window.decorView, ExerciseDemoView::class.java))
        assertTrue(screen(a).has("Quick reference"))
        assertFalse(screen(a).has("Your clip"))
        assertEquals(0, OwnClips.count(a))
    }
}
