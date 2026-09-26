package com.recoverwell.core.protocol

/**
 * Pre-screened YouTube demonstrations per exercise (see docs/EXERCISE_VIDEO_AUDIT.md).
 *
 * Screened from title and source only: each names the exact movement, comes
 * from a clinical channel, and carries none of the red-flag framings for this
 * pathway (surgery/repair, stretching, a different condition, work beyond the
 * phase). They have NOT been watched by a clinician, so the app shows them
 * with a "does this match your plan?" banner and never as confirmed - a user's
 * one-tap "Use this video" pins it; a physio-watched pick belongs in
 * [ExerciseVideo.curated].
 */
object VideoSuggestions {
    val byExercise: Map<String, List<VideoPick>> = emptyMap()
}
