package com.recoverwell.core

import com.recoverwell.core.protocol.ExerciseVideo
import com.recoverwell.core.protocol.ProtocolRegistry
import org.junit.Assert.*
import org.junit.Test

class ExerciseVideoTest {

    private val protocol = ProtocolRegistry.default

    @Test
    fun queryDropsParentheticalsAndAddsContext() {
        val ankle = protocol.phases.flatMap { it.exercises }.first { it.id == "p3_ankle_pump" }
        // name is "Active ankle pumps (to neutral only)"
        val q = ExerciseVideo.query(ankle, protocol.videoContext)
        assertFalse("parenthetical removed", q.contains("("))
        assertTrue(q.startsWith("Active ankle pumps"))
        assertTrue(q.contains("Achilles"))
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
    fun explicitOverrideWins() {
        val spec = protocol.phases.first().exercises.first().copy(videoQuery = "my custom phrase")
        assertEquals("my custom phrase Achilles rupture rehab physiotherapy",
            ExerciseVideo.query(spec, protocol.videoContext))
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
    fun pinnedVideoSurvivesBackupRoundTrip() {
        val o = com.recoverwell.core.model.ExerciseOverride("p4_double_raise", 3, 12, 5, 2, true, "abcDEF12345")
        val decoded = com.recoverwell.core.export.BackupCodec.overrideFrom(
            com.recoverwell.core.export.BackupCodec.overrideJson(o))
        assertEquals("abcDEF12345", decoded.videoId)
    }
}
