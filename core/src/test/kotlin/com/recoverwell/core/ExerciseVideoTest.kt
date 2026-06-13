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
}
