package com.recoverwell.core.protocol

/**
 * Pre-screened YouTube demonstrations per exercise (see docs/EXERCISE_VIDEO_AUDIT.md).
 *
 * Screened from title and source only. Each names the exact movement, comes
 * from a clinical source, and has no framing from a CONFLICTING pathway:
 * surgical repair, tendinopathy loading below step level, or stretching. A
 * generic movement (a squat, a step-up, a shuffle) may come from another
 * condition's series; the movement is the same. Titles are the videos' own
 * (channel suffixes trimmed), so the banner matches what plays.
 *
 * None has been watched by a clinician. The app shows them with a "does this
 * match your plan?" banner, never as confirmed. The user's one-tap "Use this
 * video" pins one; a physio-watched pick belongs in [ExerciseVideo.curated].
 */
object VideoSuggestions {

    private fun pick(id: String, title: String, source: String) = VideoPick(id, title, source)

    val byExercise: Map<String, List<VideoPick>> = linkedMapOf(
        // P1_P3_PICKS

        // -- Phase 4: strength & balance ----------------------------------
        "p4_double_raise" to listOf(
            pick("Y_R1CICW6Rw", "Heel Raises", "Nottingham University Hospitals NHS physiotherapy"),
            // shows the two-legs-to-one progression; its later loaded/on-a-step stages go beyond phase 4
            pick("HmgXnST4Mdw", "The Calf Raise - Exercise Progression", "Tim Keeley, physiotherapist (Physio REHAB)")
        ),
        "p4_soleus_raise" to listOf(
            pick("KogqCeFHhbE", "Seated Soleus Muscle Strengthening Exercise Tutorial (Level 2)", "Online Physio Exercises"),
            pick("u45C0NByqF8", "Seated Soleus Muscle Strengthening Exercise Tutorial (Level 1)", "Online Physio Exercises")
        ),
        "p4_balance" to listOf(
            pick("IF1PymZNN0k", "How to Do a Single Leg Balance Exercise", "MedBridge clinical exercise library")
        ),
        "p4_band_pf" to listOf(
            // exact exercise; the app's own rule (band never pulls past neutral) is in the cues and safety strip
            pick("xgW2hhgjUz0", "Ankle Plantar Flexion with Resistive Band", "Ask Doctor Jo, physical therapist")
        ),
        "p4_step_up" to listOf(
            pick("wfhXnLILqdk", "Step Up Exercise | Osteoarthritis Physiotherapy", "Physiotherapy demonstration"),
            pick("j_FG0quhQMQ", "Physio Hip and Knee Exercises: Step Ups", "Physiotherapy exercise series")
        ),
        "p4_squat" to listOf(
            pick("L61HQqjYFdQ", "How to Do Squats: A Guide from Physical Therapists", "Physical therapists"),
            pick("59AOEyi7STU", "Physiotherapy: Squats", "Physiotherapy demonstration")
        ),

        // -- Phase 5: impact & return to sport ----------------------------
        "p5_single_raise" to listOf(
            pick("-IqeI-mMQLQ", "Single Leg Heel Raises", "Ask Doctor Jo, physical therapist"),
            pick("8gR4Lfp5dBw", "Physio Led Pilates for Calf Strength: How to do a single leg heel raise", "Physio-led Pilates")
        ),
        "p5_jog" to listOf(
            // explainers rather than demos: the exercise is itself a programme
            pick("qjZOX0L8PaM", "Return to Running after Knee and Ankle Injuries", "Tim Keeley, physiotherapist (Physio REHAB)"),
            pick("vPEOoE64rD8", "How To Return To Running Safely - Guidance from Physical Therapists", "Physical therapists")
        ),
        "p5_hop" to listOf(
            pick("3oFMGHVbTBk", "Achilles Tendon Physical Therapy Treatment | Plyometrics, Impact, Jumping Progressions", "Physical therapy clinic"),
            pick("ldlVqjdlpSo", "Single Leg Step Hops", "POGO Physio")
        ),
        "p5_agility" to listOf(
            // the ACL framing doesn't matter for a generic shuffle; presenter is a sports-medicine PT
            pick("iBmvPEWt5og", "Agility Exercise for ACL: Lateral Shuffle", "MedStar Sports Medicine physical therapist")
        )
        // p4_swim: no clinical-source demo found; p5_padel: sport-specific, so a tuned per-sport search
    )
}
