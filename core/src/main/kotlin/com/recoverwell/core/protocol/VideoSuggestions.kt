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
        // -- Phase 1: protect & activate (boot on - the videos show bare legs) --
        "p1_toe_scrunch" to listOf(
            // curls a towel; in the boot, just curl and spread the toes
            pick("FxHeokSwEes", "Toe Curl Exercise", "Rehab My Patient physiotherapy exercise library")
        ),
        "p1_knee_flex" to listOf(
            pick("TQyUvwnWdVs", "Knee Exercises in Sitting", "NHS Greater Glasgow and Clyde"),
            pick("gd7Y9gmDBOE", "Seated Knee Bending Exercise", "Concord Hospital rehabilitation services")
        ),
        "p1_slr" to listOf(
            pick("ie-tyGqon0w", "Straight Leg Raise", "Nottingham University Hospitals NHS physiotherapy"),
            pick("EWGR5mTPzsU", "Straight Leg Raise Exercise • How To Do Properly", "Margaret Martin, physical therapist")
        ),
        "p1_hip_abd" to listOf(
            pick("UmmBtOG2N_s", "How to Do a Sidelying Hip Abduction", "MedBridge clinical exercise library"),
            pick("dBQXWsdrnfo", "Strengthen Your “Gluteus Medius” with Side-Lying Hip Abduction",
                "Pain Science Physical Therapy")
        ),
        "p1_glute_squeeze" to listOf(
            pick("TPUcaCNKwnY", "Glute Sets", "VNA Health Group physical therapy"),
            pick("PhTDzR0TpZs", "How to Do a Glute Bridge Exercise: A Guide from Physical Therapists", "Physical therapists")
        ),

        // -- Phase 2: controlled loading (still in the boot) ---------------
        "p2_boot_walk" to listOf(
            // crutch technique for partial / as-tolerated weight bearing; the boot stays on
            pick("TdIESUPLSMw", "Learn Crutches, Partial Weight Bearing (PWB), in 45sec", "Physiotherapist")
        ),
        "p2_leg_ext" to listOf(
            pick("VuJZ6dqMf8M", "Seated Knee Extension (LAQ)", "Ask Doctor Jo, physical therapist"),
            pick("v_R4c04GuKE", "Seated Knee Extension - PT Exercise", "OneStep Digital Physical Therapy")
        ),
        "p2_bridge" to listOf(
            pick("PhTDzR0TpZs", "How to Do a Glute Bridge Exercise: A Guide from Physical Therapists", "Physical therapists"),
            pick("XLXGydU5DdU", "How to do a glute bridge", "Bupa Health")
        ),
        "p2_clamshell" to listOf(
            pick("EG5_gXcfozw", "How To Do The Clamshell Exercise", "Kinetic Sports Rehab physical therapy"),
            pick("2c5xiz4q7ow", "Clam Shell Exercise: Strengthen Your Hip & Knees by Physical Therapist",
                "Physical therapist")
        ),
        "p2_core" to listOf(
            // each covers part of the circuit: band work, then trunk rotation
            pick("sbzR-daGhag", "Upper Body Seated Resistance Band Exercises", "Intermountain Healthcare exercise physiologist"),
            pick("esNSztn8OWQ", "Seated Trunk Rotation", "Ask Doctor Jo, physical therapist")
        ),

        // -- Phase 3: motion & gait -----------------------------------------
        // p3_ankle_pump: deliberately none - see its noVideoSearchReason
        "p3_inv_ev" to listOf(
            pick("9CJkHt7Cbag", "Seated Inversion Eversion Exercise for Foot and Ankle",
                "Congruency Therapy & Wellness physical therapy"),
            pick("sd8WSPKMIYs", "Seated ankle inversion & eversion ROM", "Doctor of Physical Therapy")
        ),
        "p3_seated_raise" to listOf(
            pick("M5j_CfIobHE", "Seated heel raise", "Northamptonshire Healthcare NHS physiotherapy (NHFT)")
        ),
        // p3_gait: no clinical-source demo found - the tuned search ranks two rupture-specific ones first
        "p3_bike" to listOf(
            // covers the set-up cue (saddle height); the easy-pedalling cues are the app's own
            pick("B5jBa94dNZ4", "Bicycle Seat Height: Do It Right For Comfort & Speed (Stop Knee Pain)",
                "Bob & Brad, physical therapists")
        ),
        "p3_towel" to listOf(
            pick("ztgcEsuqves", "Towel scrunch | Foot exercises", "Pocket Physio"),
            pick("FxHeokSwEes", "Toe Curl Exercise", "Rehab My Patient physiotherapy exercise library")
        ),

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
