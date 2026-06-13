package com.recoverwell.core.protocol

import com.recoverwell.core.model.ExerciseSpec
import java.net.URLEncoder

/**
 * Demonstrations are resolved through a self-healing chain so a video is always
 * available:
 *   1. a user-pinned YouTube id for this exercise (survives in the backup), else
 *   2. a curated default id for the movement (verified, may be empty), else
 *   3. a YouTube *search* scoped to the movement + rehab context.
 * The in-app player embeds an id when there is one and falls back to the search
 * (in-app, then external) on any embed error; the bundled animation is the
 * offline floor. Searches never rot into a dead hard-coded link.
 */
object ExerciseVideo {

    /** Search phrase for an exercise: explicit override, else name + context. */
    fun query(spec: ExerciseSpec, context: String): String {
        val base = spec.videoQuery.ifBlank {
            // drop parentheticals like "(to neutral only)" for a cleaner search
            spec.name.replace(Regex("\\(.*?\\)"), "").trim()
        }
        return listOf(base, context).filter { it.isNotBlank() }.joinToString(" ").trim()
    }

    fun youtubeSearchUrl(spec: ExerciseSpec, context: String): String =
        "https://www.youtube.com/results?search_query=" +
            URLEncoder.encode(query(spec, context), "UTF-8")

    /** Curated, verified demonstration ids keyed by exercise id. Empty until
     *  confirmed - the search fallback covers everything in the meantime. */
    val curated: Map<String, String> = emptyMap()

    /** Resolved id to embed, preferring the user's pin over any curated default. */
    fun resolveVideoId(exerciseId: String, pinnedId: String?): String? =
        pinnedId?.takeIf { it.isNotBlank() } ?: curated[exerciseId]

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
        return Regex(id).find(s)?.value
    }
}

