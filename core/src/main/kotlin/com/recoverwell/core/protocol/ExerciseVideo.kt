package com.recoverwell.core.protocol

import com.recoverwell.core.model.ExerciseSpec
import java.net.URLEncoder

/**
 * Real demonstrations are linked, not bundled: the app opens a YouTube search
 * scoped to the exact movement and rehab context, which the device's YouTube
 * app or browser plays. This keeps the app itself fully offline and
 * permission-free (it sends nothing; the hand-off is user-initiated), while
 * giving live, reputable video that never rots into a dead hard-coded link.
 * The bundled animation remains the offline quick reference.
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
}
