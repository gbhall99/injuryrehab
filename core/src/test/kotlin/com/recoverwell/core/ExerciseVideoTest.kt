package com.recoverwell.core

import com.recoverwell.core.protocol.ExerciseVideo
import com.recoverwell.core.protocol.ProtocolRegistry
import com.recoverwell.core.protocol.VideoTier
import org.junit.Assert.*
import org.junit.Test

class ExerciseVideoTest {

    private val protocol = ProtocolRegistry.default

    private val allExercises = ProtocolRegistry.all.flatMap { p -> p.phases.flatMap { it.exercises } }

    @Test
    fun fallbackQueryDropsParentheticalsAndAddsContext() {
        val ankle = protocol.phases.flatMap { it.exercises }.first { it.id == "p3_ankle_pump" }.copy(videoQuery = "")
        // name is "Active ankle pumps (to neutral only)"
        val q = ExerciseVideo.query(ankle, protocol.videoContext)
        assertFalse("parenthetical removed", q.contains("("))
        assertTrue(q.startsWith("Active ankle pumps"))
        assertTrue(q.contains("Achilles"))
    }

    /** Audit finding: "<name> Achilles rupture rehab physiotherapy" surfaced surgical
     *  protocols, stretching and whole programmes. Every exercise now has its own query. */
    @Test
    fun everyExerciseHasItsOwnTunedQuery() {
        for (ex in allExercises) assertTrue("${ex.id} needs a tuned videoQuery", ex.videoQuery.isNotBlank())
        val dupes = allExercises.groupBy { it.videoQuery.lowercase() }.filterValues { it.size > 1 }
        assertTrue("each exercise searches for itself: $dupes", dupes.isEmpty())
    }

    /** A conservative (non-surgical) patient must never be steered to surgical,
     *  stretching or tendinopathy content by the app's own words. */
    @Test
    fun tunedQueriesAvoidTermsThatSurfaceTheWrongVideos() {
        val banned = listOf("surg", "post-op", "post op", "postop", "repair", "stretch", "tendinopathy",
            "tendonitis", "tendinitis", "→", "{", "}")
        for (sport in com.recoverwell.core.protocol.SportRegistry.all) {
            val p = com.recoverwell.core.protocol.SportText.resolveProtocol(protocol, sport.name, sport.drillVideoQuery)
            for (ex in p.phases.flatMap { it.exercises }) {
                val q = ExerciseVideo.query(ex, p.videoContext).lowercase()
                for (b in banned) assertFalse("${ex.id} query \"$q\" contains \"$b\"", q.contains(b))
                assertTrue("${ex.id} query is specific enough", q.split(" ").size >= 3)
            }
        }
    }

    @Test
    fun sportDrillsSearchForTheUsersOwnSport() {
        for (sport in com.recoverwell.core.protocol.SportRegistry.all) {
            val p = com.recoverwell.core.protocol.SportText.resolveProtocol(protocol, sport.name, sport.drillVideoQuery)
            val drills = p.phases.flatMap { it.exercises }.first { it.id == "p5_padel" }
            // "<sport> footwork drills" is meaningless for cycling or swimming: each sport names its own
            assertTrue("${sport.id} needs a drill query", sport.drillVideoQuery.isNotBlank())
            assertEquals(sport.drillVideoQuery, ExerciseVideo.query(drills, p.videoContext))
            // gym's drills are plyometrics; every other sport's query names the sport
            if (sport.id != "gym") assertTrue(sport.drillVideoQuery.contains(sport.name, ignoreCase = true))
        }
    }

    @Test
    fun everyExerciseProducesAValidYoutubeSearchUrl() {
        for (ex in protocol.phases.flatMap { it.exercises }) {
            val url = ExerciseVideo.youtubeSearchUrl(ex, protocol.videoContext)
            assertTrue(url, url.startsWith("https://www.youtube.com/results?search_query="))
            assertFalse("query must be URL-encoded (no spaces)", url.contains(" "))
            assertTrue("query non-empty", url.length > "https://www.youtube.com/results?search_query=".length)
        }
    }

    @Test
    fun tunedQueryIsUsedVerbatim() {
        // appending the generic context is what dragged in programmes and surgical protocols
        val spec = protocol.phases.first().exercises.first().copy(videoQuery = "  my custom phrase ")
        assertEquals("my custom phrase", ExerciseVideo.query(spec, protocol.videoContext))
    }

    @Test
    fun parsesVideoIdFromEveryCommonLinkForm() {
        val id = "dQw4w9WgXcQ"
        assertEquals(id, ExerciseVideo.parseVideoId("https://www.youtube.com/watch?v=$id"))
        assertEquals(id, ExerciseVideo.parseVideoId("https://m.youtube.com/watch?v=$id&t=30s"))
        assertEquals(id, ExerciseVideo.parseVideoId("https://youtu.be/$id"))
        assertEquals(id, ExerciseVideo.parseVideoId("https://www.youtube.com/embed/$id"))
        assertEquals(id, ExerciseVideo.parseVideoId("https://youtube.com/shorts/$id"))
        assertEquals(id, ExerciseVideo.parseVideoId("  $id  ")) // bare id, trimmed
    }

    @Test
    fun rejectsNonYoutubeText() {
        assertNull(ExerciseVideo.parseVideoId(""))
        assertNull(ExerciseVideo.parseVideoId("not a link"))
        assertNull(ExerciseVideo.parseVideoId("https://example.com/watch?v=abc"))
    }

    @Test
    fun pinnedIdBeatsCuratedDefault() {
        // user's pin always wins; with no pin and no curated entry, returns null -> search
        assertEquals("PINNED12345", ExerciseVideo.resolveVideoId("p1_toe_scrunch", "PINNED12345"))
        assertNull(ExerciseVideo.resolveVideoId("p1_toe_scrunch", null))
    }

    @Test
    fun resolveGoesPinnedThenCuratedThenSuggestedThenSearch() {
        val (id, picks) = ExerciseVideo.suggested.entries.first()
        assertEquals(VideoTier.PINNED, ExerciseVideo.resolve(id, "PINNED12345").tier)
        assertEquals("PINNED12345", ExerciseVideo.resolve(id, "PINNED12345").videoId)
        val r = ExerciseVideo.resolve(id, null)
        assertEquals(VideoTier.SUGGESTED, r.tier)
        assertEquals(picks.first(), r.pick)
        // "Next suggestion" cycles and wraps, never out of range
        assertEquals(picks[1 % picks.size], ExerciseVideo.resolve(id, null, 1).pick)
        assertEquals(picks.first(), ExerciseVideo.resolve(id, null, picks.size).pick)
        assertEquals(picks.last(), ExerciseVideo.resolve(id, " ", -1).pick)
        val unsuggested = allExercises.map { it.id }.first { it !in ExerciseVideo.suggested }
        assertEquals(VideoTier.SEARCH, ExerciseVideo.resolve(unsuggested, null).tier)
        assertNull(ExerciseVideo.resolve(unsuggested, "").pick)
    }

    /** Nothing is labelled physio-checked until a clinician has actually watched it. */
    @Test
    fun noVideoIsPresentedAsPhysioCheckedWithoutBeingWatched() {
        assertTrue(ExerciseVideo.curated.isEmpty())
    }

    @Test
    fun suggestionsAreWellFormedAndSafeByTitle() {
        val ids = allExercises.map { it.id }.toSet()
        assertTrue("the audit shipped suggestions", ExerciseVideo.suggested.isNotEmpty())
        val unsafe = listOf("surg", "post-op", "post op", "postop", "repair", "stretch", "tendinopathy",
            "tendonitis", "tendinitis", "sprain")
        for ((exId, picks) in ExerciseVideo.suggested) {
            assertTrue("$exId is a real exercise", exId in ids)
            assertTrue("$exId has picks", picks.isNotEmpty())
            assertEquals("$exId has no duplicate picks", picks.size, picks.map { it.videoId }.toSet().size)
            for (pick in picks) {
                assertTrue("${pick.videoId} is an 11-char id", Regex("[A-Za-z0-9_-]{11}").matches(pick.videoId))
                assertEquals(pick.videoId, ExerciseVideo.parseVideoId(pick.videoId))
                assertTrue(pick.title.isNotBlank() && pick.source.isNotBlank())
                assertTrue(pick.startSeconds >= 0)
                val title = pick.title.lowercase().replace("non-surgical", "").replace("non surgical", "")
                    .replace("nonsurgical", "")
                for (b in unsafe) assertFalse("$exId pick \"${pick.title}\" has \"$b\"", title.contains(b))
            }
        }
    }

    /** The video brief must describe exactly the clips the app can use - it had drifted
     *  (a removed demo, the wrong count) and a producer would have filmed the wrong list. */
    @Test
    fun productionBriefsListExactlyTheAppsDemoClips() {
        val demoIds = allExercises.map { it.demoId }.toSortedSet()
        for (path in listOf("../docs/exercise-demo-video-prompts.md", "../docs/exercise-demo-video-prompts-standalone.md")) {
            val text = java.io.File(path).readText()
            val listed = Regex("(?m)^(?:## |\\*\\*)`([a-z_]+)`").findAll(text).map { it.groupValues[1] }.toList()
            assertEquals("$path lists each clip once", listed.size, listed.toSet().size)
            assertEquals(path, demoIds, listed.toSortedSet())
            assertTrue("$path states the clip count", text.contains("${demoIds.size}"))
        }
    }

    @Test
    fun pinnedVideoSurvivesBackupRoundTrip() {
        val o = com.recoverwell.core.model.ExerciseOverride("p4_double_raise", 3, 12, 5, 2, true, "abcDEF12345")
        val decoded = com.recoverwell.core.export.BackupCodec.overrideFrom(
            com.recoverwell.core.export.BackupCodec.overrideJson(o))
        assertEquals("abcDEF12345", decoded.videoId)
    }
}
