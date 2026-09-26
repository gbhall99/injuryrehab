package com.recoverwell.core.protocol

import com.recoverwell.core.model.ExerciseSpec
import java.net.URLEncoder

/** A specific YouTube demonstration for an exercise, with who made it. */
data class VideoPick(
    val videoId: String,
    val title: String,
    /** Who made it, e.g. a physio or clinic channel. */
    val source: String,
    /** Seconds of intro to skip; 0 = play from the start. */
    val startSeconds: Int = 0
)

/**
 * Where "Watch video" plays from, best first:
 *  - [PINNED]: the video the user chose for this exercise (kept in the backup);
 *  - [CURATED]: a video a physio has WATCHED and confirmed matches the plan;
 *  - [SUGGESTED]: pre-screened from its title and source only - shown with an
 *    honest "not yet checked, does it match your plan?" banner and a one-tap
 *    "Use this video" that pins it;
 *  - [SEARCH]: a YouTube search, tuned per exercise to find a single-movement
 *    demo rather than whole programmes, post-operative protocols or stretching.
 * The bundled animation (or the user's own offline clip) is always the floor.
 */
enum class VideoTier { PINNED, CURATED, SUGGESTED, SEARCH }

data class VideoResolution(val tier: VideoTier, val pick: VideoPick?) {
    val videoId: String? get() = pick?.videoId
}

object ExerciseVideo {

    /**
     * Search phrase for an exercise. A per-exercise [ExerciseSpec.videoQuery] is
     * complete on its own and used verbatim - appending the generic context is
     * what pulled in whole programmes, post-operative protocols and stretching.
     * Without one: the name (parentheticals dropped) plus the protocol context.
     */
    fun query(spec: ExerciseSpec, context: String): String {
        if (spec.videoQuery.isNotBlank()) return spec.videoQuery.trim()
        val base = spec.name.replace(Regex("\\(.*?\\)"), "").trim()
        return listOf(base, context).filter { it.isNotBlank() }.joinToString(" ").trim()
    }

    fun youtubeSearchUrl(spec: ExerciseSpec, context: String): String =
        "https://www.youtube.com/results?search_query=" +
            URLEncoder.encode(query(spec, context), "UTF-8")

    /** Physio-watched, confirmed demonstrations keyed by exercise id. Empty until
     *  a clinician has watched each one - see docs/EXERCISE_VIDEO_AUDIT.md. */
    val curated: Map<String, VideoPick> = emptyMap()

    /** Pre-screened candidates keyed by exercise id (title + source checks only). */
    val suggested: Map<String, List<VideoPick>> = VideoSuggestions.byExercise

    /** The best available demonstration, by tier. [suggestionIndex] cycles candidates. */
    fun resolve(exerciseId: String, pinnedId: String?, suggestionIndex: Int = 0): VideoResolution {
        pinnedId?.takeIf { it.isNotBlank() }?.let {
            return VideoResolution(VideoTier.PINNED, VideoPick(it, "", ""))
        }
        curated[exerciseId]?.let { return VideoResolution(VideoTier.CURATED, it) }
        val picks = suggested[exerciseId].orEmpty()
        if (picks.isNotEmpty()) {
            return VideoResolution(VideoTier.SUGGESTED, picks[Math.floorMod(suggestionIndex, picks.size)])
        }
        return VideoResolution(VideoTier.SEARCH, null)
    }

    /** Resolved id to embed, preferring the user's pin over any curated default. */
    fun resolveVideoId(exerciseId: String, pinnedId: String?): String? =
        pinnedId?.takeIf { it.isNotBlank() } ?: curated[exerciseId]?.videoId

    fun embedUrl(id: String): String = "https://www.youtube-nocookie.com/embed/$id"

    /**
     * Extracts an 11-character YouTube video id from a pasted link or a bare id.
     * Handles watch?v=, youtu.be/, /embed/, /shorts/, /live/ forms; returns null
     * if the text isn't a YouTube link or id.
     */
    fun parseVideoId(input: String): String? {
        val s = input.trim()
        if (s.isEmpty()) return null
        val id = "[A-Za-z0-9_-]{11}"
        if (Regex("^$id$").matches(s)) return s
        if (!s.contains("youtu", ignoreCase = true)) return null
        for (p in listOf(
            Regex("[?&]v=($id)"),
            Regex("youtu\\.be/($id)"),
            Regex("/embed/($id)"),
            Regex("/shorts/($id)"),
            Regex("/live/($id)")
        )) p.find(s)?.let { return it.groupValues[1] }
        // last resort: an id that sits as a whole path/query token (delimiter-bounded),
        // so we don't grab an arbitrary 11-char slice of some unrelated parameter.
        return Regex("[/=]($id)(?:[?&#/]|$)").find(s)?.groupValues?.get(1)
    }
}
