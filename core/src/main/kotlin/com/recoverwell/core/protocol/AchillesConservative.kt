package com.recoverwell.core.protocol

import com.recoverwell.core.model.*
import java.time.LocalDate
import java.time.LocalTime

/**
 * Default content for CONSERVATIVE (non-surgical) Achilles tendon rupture
 * rehabilitation, modelled on established UK functional rehabilitation
 * pathways: immediate/early weight-bearing in a boot fitted in full equinus,
 * progressive wedge reduction to neutral by ~week 8, boot weaning from
 * ~week 8-10, no calf stretching before week 12, graded strengthening, and
 * return to sport from ~6 months with racquet/court sports typically 9-12
 * months (see README for the cited sources: UKSTAR trial, NHS trust
 * non-operative pathways).
 *
 * EVERY date and week number here is a typical-protocol placeholder, NOT a
 * prescription. The app marks them as physio-confirmable and they are
 * editable in Settings.
 */
object AchillesConservative {

    const val ID = "achilles_rupture_conservative"

    private const val PLACEHOLDER_NOTE =
        "Typical conservative-protocol timing - confirm with your physio. Your own plan may differ."

    // ------------------------------------------------------------------
    // Phases
    // ------------------------------------------------------------------

    val phases: List<PhaseSpec> = listOf(
        PhaseSpec(
            number = 1,
            title = "Immobilisation & protection",
            subtitle = "Weeks 0-2 · boot at full equinus, let the tendon ends knit",
            tissueState = "Tendon ends knitting together - maximum protection",
            deviceUsage = "Boot on at all times · heel angle {n}°",
            startWeek = 0,
            endWeek = 2,
            entryCriteria = listOf(
                "Achilles rupture confirmed and conservative pathway chosen by your clinical team",
                "Walking boot set to full heel angle (foot pointed down / full equinus)"
            ),
            goals = listOf(
                "Protect the healing tendon - boot on at all times, including in bed unless told otherwise",
                "Control swelling with elevation and keep circulation moving",
                "Take clot-prevention medication exactly as prescribed",
                "Keep the rest of the body moving: hips, knees, core, upper body"
            ),
            precautions = listOf(
                "Never walk without the boot, even for one step (e.g. night-time bathroom trips)",
                "Do not move the ankle up towards you (dorsiflexion) - the boot angle protects the tendon",
                "Do not change the boot's heel angle yourself unless your clinic has told you to",
                "Watch daily for DVT warning signs - calf pain, heat, swelling, redness"
            ),
            allowed = listOf(
                "Walking short distances in the boot with crutches, putting weight through as comfort allows",
                "Wiggling and scrunching toes inside the boot",
                "Knee, hip and core exercises with the boot on",
                "Sitting with the leg elevated above heart level",
                "Washing with the boot off ONLY if seated, foot pointed down, no weight through it (if your clinic allows)"
            ),
            notAllowed = listOf(
                "Any step without the boot",
                "Pulling the foot/toes up towards you (dorsiflexion past the boot angle)",
                "Calf stretching of any kind",
                "Driving",
                "Running, jumping, sport of any kind - padel comes much later"
            ),
            exercises = phase1Exercises()
        ),
        PhaseSpec(
            number = 2,
            title = "Progressive weight-bearing & heel-angle reduction",
            subtitle = "Weeks 2-8 · step the heel down gradually, build to full weight",
            tissueState = "Early healing tissue forming - protected loading helps it organise",
            deviceUsage = "Boot on at all times · heel angle {n}°",
            startWeek = 2,
            endWeek = 8,
            entryCriteria = listOf(
                "Around 2 weeks since injury (typical protocol - confirm with your physio)",
                "Comfortable in the boot with pain and swelling settling",
                "Clinic happy for the heel-angle reduction plan to start"
            ),
            goals = listOf(
                "Lower the heel angle on schedule so the boot reaches neutral (0°) by ~week 8",
                "Progress from crutches to confident full weight-bearing in the boot",
                "Keep swelling controlled; continue clot-prevention medication if still prescribed",
                "Maintain strength everywhere else so phase 4 starts from a good base"
            ),
            precautions = listOf(
                "Only lower the heel angle on the planned dates and only if your clinic agrees",
                "Still no steps without the boot",
                "No dorsiflexion past the current boot angle, no calf stretching",
                "If lowering the heel angle causes sharp pain, set it back and call your clinic",
                "Keep watching for DVT signs - risk persists while immobilised"
            ),
            allowed = listOf(
                "Full weight-bearing in the boot as comfort allows (wean off crutches)",
                "Longer walks in the boot as tolerated",
                "Gym work that keeps the boot on and does not load the ankle (seated upper body, core)",
                "Toe, knee, hip and core exercises"
            ),
            notAllowed = listOf(
                "Walking without the boot",
                "Calf stretching or forcing the ankle upwards",
                "Removing more wedges than planned to “speed things up”",
                "Driving (most people cannot drive safely in a boot - ask your clinic and insurer)",
                "Impact activity: running, jumping, padel"
            ),
            exercises = phase2Exercises()
        ),
        PhaseSpec(
            number = 3,
            title = "Early mobilisation out of the boot",
            subtitle = "Weeks 8-12 · wean off the boot, wake the ankle up gently",
            tissueState = "Tendon consolidating - gentle movement, no stretch",
            deviceUsage = "Weaning out of the boot, physio-guided",
            startWeek = 8,
            endWeek = 12,
            entryCriteria = listOf(
                "Around 8 weeks since injury and all wedges out (boot at neutral) - confirm with your physio",
                "Comfortable fully weight-bearing in the neutral boot",
                "Physiotherapist has confirmed you can begin weaning out of the boot"
            ),
            goals = listOf(
                "Gradually wean out of the boot indoors, then outdoors, as your physio directs",
                "Restore gentle active ankle movement - up to neutral only, no stretch",
                "Re-learn a normal walking pattern in supportive shoes (a small heel raise insert helps)",
                "Begin gentle, physio-guided calf activation"
            ),
            precautions = listOf(
                "No calf stretching until at least 12 weeks from injury - the tendon is still remodelling",
                "Dorsiflexion (foot up) only to neutral; never push into stretch",
                "Avoid slopes, stairs without rails, and uneven ground early in the wean",
                "Re-rupture risk is highest in this transition out of the boot - progress only as your physio directs",
                "Keep wearing the boot in crowded or unpredictable places until cleared"
            ),
            allowed = listOf(
                "Walking indoors in supportive shoes with heel raise (as physio directs)",
                "Gentle active ankle movement: down fully, up to neutral only",
                "Stationary cycling with low resistance once your physio approves",
                "Swimming/pool walking once any wounds are healed and physio approves"
            ),
            notAllowed = listOf(
                "Calf stretches (before week 12, and after only when physio says)",
                "Barefoot or flat-shoe walking",
                "Single-leg heel raises - far too early",
                "Running, hopping, jumping",
                "Padel, even “just a gentle rally”"
            ),
            exercises = phase3Exercises()
        ),
        PhaseSpec(
            number = 4,
            title = "Strengthening",
            subtitle = "Weeks 12-24 · rebuild the calf, balance and gait",
            tissueState = "Tendon remodelling - progressive load makes it stronger",
            deviceUsage = null,
            startWeek = 12,
            endWeek = 24,
            entryCriteria = listOf(
                "Around 12 weeks since injury (typical protocol - confirm with your physio)",
                "Out of the boot and walking in normal supportive shoes",
                "No night pain or persistent swelling flare-ups",
                "Physiotherapist has confirmed progression to strengthening"
            ),
            goals = listOf(
                "Rebuild calf strength: seated raises, double-leg raises, then towards single-leg",
                "Restore balance and proprioception on the injured side",
                "Walk 30+ minutes comfortably with a symmetrical pattern",
                "Build general leg strength: squats, step-ups, bridges"
            ),
            precautions = listOf(
                "Strengthen before you stretch - aggressive stretching can still over-lengthen the tendon",
                "Expect mild ache after sessions; sharp pain in the tendon means stop and tell your physio",
                "No impact work (running/jumping) until your physio clears it - usually phase 5",
                "Progress one variable at a time: range, then reps, then load"
            ),
            allowed = listOf(
                "Progressive calf strengthening as prescribed",
                "Stationary bike with increasing resistance",
                "Swimming and deep-water running",
                "Leg press and gym strength work within physio guidance",
                "Longer daily walks on even ground"
            ),
            notAllowed = listOf(
                "Running and jumping (until physio clears - typically phase 5)",
                "Ballistic or forced calf stretching",
                "Sports with sudden direction changes - padel still waits",
                "Maximal single-leg hopping or sprinting"
            ),
            exercises = phase4Exercises()
        ),
        PhaseSpec(
            number = 5,
            title = "Return to sport",
            subtitle = "Week 24 onwards · earn your way back to the padel court",
            tissueState = "Tendon maturing - building sport-level capacity",
            deviceUsage = null,
            startWeek = 24,
            endWeek = null,
            entryCriteria = listOf(
                "Around 6 months since injury (typical protocol - confirm with your physio)",
                "Can do 20-25 good single-leg heel raises on the injured side",
                "Walking unlimited distances without pain or limp",
                "Physiotherapist has explicitly cleared the start of impact work"
            ),
            goals = listOf(
                "Build a graded running programme: walk-jog intervals first",
                "Add hopping and plyometric capacity, then direction changes",
                "Padel-specific drills: court movement, shadow play, controlled rallies",
                "Return to competitive padel when cleared - typically 9-12 months after injury"
            ),
            precautions = listOf(
                "Each step up (jog, hop, agility, rally, match) needs physio sign-off",
                "Warm up thoroughly; fatigue is when re-injuries happen",
                "The repaired tendon often stays slightly thicker - that is normal",
                "Morning tendon stiffness that worsens week-on-week means back off and ask your physio"
            ),
            allowed = listOf(
                "Graded running once cleared",
                "Plyometric progressions once cleared",
                "Padel drills and controlled rallies once cleared",
                "Full competitive padel typically from 9-12 months, with physio sign-off"
            ),
            notAllowed = listOf(
                "Competitive matches before your physio explicitly signs them off",
                "Skipping progression steps after a good week",
                "Playing through sharp tendon pain"
            ),
            exercises = phase5Exercises()
        )
    )

    fun phase(number: Int): PhaseSpec = phases.first { it.number == number }

    // ------------------------------------------------------------------
    // Exercises
    // ------------------------------------------------------------------

    private fun phase1Exercises() = listOf(
        ExerciseSpec(
            id = "p1_toe_scrunch", phase = 1, name = "Toe wiggles & scrunches",
            demoId = "toe_scrunch",
            cues = listOf(
                "Keep the boot on and the ankle completely still",
                "Spread and wiggle all five toes, then scrunch them gently",
                "Slow and rhythmic - think of it as a circulation pump"
            ),
            sets = 1, reps = 20, holdSeconds = 0, sessionsPerDay = 4,
            whyItMatters = "Moving the toes pumps blood through the lower leg while you are immobilised, which helps control swelling and lowers DVT risk without loading the tendon.",
            precaution = "Ankle stays still inside the boot - only the toes move."
        ),
        ExerciseSpec(
            id = "p1_knee_flex", phase = 1, name = "Seated knee bends (boot on)",
            demoId = "knee_flex",
            cues = listOf(
                "Sit on a chair or bed edge with the boot on",
                "Slowly bend and straighten the knee through comfortable range",
                "Let the boot swing - do not push through the foot"
            ),
            sets = 2, reps = 10, holdSeconds = 0, sessionsPerDay = 3,
            whyItMatters = "Keeps the knee joint mobile and the hamstrings/quads active so the whole leg does not stiffen up around the protected ankle.",
            precaution = "No weight through the foot while bending."
        ),
        ExerciseSpec(
            id = "p1_slr", phase = 1, name = "Straight-leg raise (boot on)",
            demoId = "slr",
            cues = listOf(
                "Lie on your back, uninjured knee bent, injured leg straight in the boot",
                "Tighten the thigh, lift the whole leg about 30 cm",
                "Lower slowly with control"
            ),
            sets = 3, reps = 10, holdSeconds = 2, sessionsPerDay = 2,
            whyItMatters = "Preserves quadriceps and hip-flexor strength, which makes crutch walking safer and speeds the return to normal walking later.",
            precaution = "Stop if it pulls at the back of the leg near the tendon."
        ),
        ExerciseSpec(
            id = "p1_hip_abd", phase = 1, name = "Side-lying hip raises (boot on)",
            demoId = "hip_abd",
            cues = listOf(
                "Lie on your uninjured side, legs stacked",
                "Lift the booted leg up sideways, keeping it straight",
                "Pause, then lower slowly"
            ),
            sets = 2, reps = 10, holdSeconds = 2, sessionsPerDay = 2,
            whyItMatters = "Strong hip abductors keep your pelvis level on crutches and prevent the limp pattern that otherwise lingers after the boot comes off.",
            precaution = "Keep the movement smooth; the boot adds weight, so fewer good reps beat many sloppy ones."
        ),
        ExerciseSpec(
            id = "p1_glute_squeeze", phase = 1, name = "Glute squeezes & gentle bridges",
            demoId = "bridge",
            cues = listOf(
                "Lie on your back, both knees bent, boot flat on the bed",
                "Squeeze your buttocks and lift hips a few centimetres",
                "Push mainly through the uninjured foot; the booted side just rests"
            ),
            sets = 2, reps = 10, holdSeconds = 3, sessionsPerDay = 2,
            whyItMatters = "Keeps the glutes - the engine of walking - switched on while you are less active, protecting your back and hips.",
            precaution = "Hips only as high as comfortable; no pushing hard through the booted foot."
        )
    )

    private fun phase2Exercises() = listOf(
        ExerciseSpec(
            id = "p2_boot_walk", phase = 2, name = "Weight-bearing practice in boot",
            demoId = "boot_walk",
            cues = listOf(
                "Stand tall between crutches, boot flat on the floor",
                "Shift weight onto the booted leg as comfort allows",
                "Progress: two crutches, one crutch, then none - heel-to-toe rolling steps"
            ),
            sets = 1, reps = 10, holdSeconds = 5, sessionsPerDay = 3,
            whyItMatters = "Controlled load through the boot stimulates the tendon to heal strong and in the right alignment - this is the core of functional conservative rehab.",
            precaution = "Increase load gradually; sharp tendon pain means ease off and tell your physio."
        ),
        ExerciseSpec(
            id = "p2_leg_ext", phase = 2, name = "Seated knee extensions (boot on)",
            demoId = "leg_ext",
            cues = listOf(
                "Sit tall on a chair",
                "Straighten the injured-side knee until the boot is level",
                "Hold, then lower slowly"
            ),
            sets = 3, reps = 10, holdSeconds = 3, sessionsPerDay = 2,
            whyItMatters = "The boot's weight turns this into useful quad strengthening, keeping the thigh from wasting during the immobilisation weeks.",
            precaution = "Move only the knee; the ankle stays protected in the boot."
        ),
        ExerciseSpec(
            id = "p2_bridge", phase = 2, name = "Two-leg bridges (boot on)",
            demoId = "bridge",
            cues = listOf(
                "Lie on your back, knees bent, feet hip-width",
                "Lift hips until body forms a straight line shoulders-to-knees",
                "Share weight between both feet now that comfort allows"
            ),
            sets = 3, reps = 10, holdSeconds = 3, sessionsPerDay = 2,
            whyItMatters = "Builds glute and hamstring strength you will lean on heavily when gait retraining starts in phase 3.",
            precaution = "Keep the booted foot flat; no pushing up onto the toes."
        ),
        ExerciseSpec(
            id = "p2_clamshell", phase = 2, name = "Clamshells",
            demoId = "clamshell",
            cues = listOf(
                "Lie on your side, knees bent, feet together",
                "Open the top knee like a clamshell without rolling your pelvis back",
                "Slow up, slow down"
            ),
            sets = 3, reps = 12, holdSeconds = 1, sessionsPerDay = 2,
            whyItMatters = "Targets the deep hip stabilisers that keep your knee and ankle aligned once you start walking out of the boot.",
            precaution = "Keep it pain-free; this should burn in the hip, not pull anywhere near the ankle."
        ),
        ExerciseSpec(
            id = "p2_core", phase = 2, name = "Seated core & upper body circuit",
            demoId = "seated_core",
            cues = listOf(
                "Sit tall: shoulder presses, rows with a band, gentle trunk rotations",
                "Keep the booted foot resting flat",
                "Breathe steadily; quality over speed"
            ),
            sets = 2, reps = 12, holdSeconds = 0, sessionsPerDay = 1,
            whyItMatters = "General conditioning keeps energy, mood and circulation up, and means your fitness does not start from zero when sport-specific work returns.",
            precaution = "Nothing that requires pushing through the injured foot."
        )
    )

    private fun phase3Exercises() = listOf(
        ExerciseSpec(
            id = "p3_ankle_pump", phase = 3, name = "Active ankle pumps (to neutral only)",
            demoId = "ankle_pump",
            cues = listOf(
                "Sit with the leg supported, boot off for the exercise",
                "Point the foot down as far as comfortable",
                "Bring it back up ONLY to flat/neutral - never pull into stretch"
            ),
            sets = 3, reps = 10, holdSeconds = 0, sessionsPerDay = 3,
            whyItMatters = "Re-awakens active control of the ankle and feeds the tendon the gentle movement it needs to remodel, without stretching it.",
            precaution = "Up to neutral only until 12 weeks - your physio will say when more range is safe."
        ),
        ExerciseSpec(
            id = "p3_inv_ev", phase = 3, name = "Gentle ankle in/out movements",
            demoId = "ankle_inv_ev",
            cues = listOf(
                "Foot relaxed, ankle in a comfortable mid position",
                "Slowly turn the sole inwards, then outwards",
                "Small, controlled range - no forcing"
            ),
            sets = 2, reps = 10, holdSeconds = 0, sessionsPerDay = 2,
            whyItMatters = "Restores the side-to-side ankle control needed for balance and for walking on anything that is not perfectly flat.",
            precaution = "Stays comfortable; sharp pulls near the heel mean shrink the range."
        ),
        ExerciseSpec(
            id = "p3_seated_raise", phase = 3, name = "Seated heel raises",
            demoId = "seated_heel_raise",
            cues = listOf(
                "Sit with feet flat, knees at 90 degrees",
                "Push through the ball of the injured foot to lift the heel",
                "Lower slowly - the lowering is the medicine"
            ),
            sets = 3, reps = 12, holdSeconds = 1, sessionsPerDay = 2,
            whyItMatters = "First direct calf work: bent-knee raises load the healing tendon lightly and start rebuilding the soleus muscle that walking depends on.",
            precaution = "Body weight only; add load only when your physio prescribes it."
        ),
        ExerciseSpec(
            id = "p3_gait", phase = 3, name = "Gait practice in shoes (heel raise insert)",
            demoId = "gait_walk",
            cues = listOf(
                "Supportive shoes with the heel-raise insert your clinic provided",
                "Short indoor walks: heel down, roll through, push off gently",
                "Even step lengths - a mirror or phone video helps"
            ),
            sets = 1, reps = 5, holdSeconds = 60, sessionsPerDay = 2,
            whyItMatters = "Re-learning a symmetrical walking pattern now prevents the protective limp from becoming a habit that takes months to undo.",
            precaution = "Boot back on for crowds, uneven ground and tiredness, until your physio says otherwise."
        ),
        ExerciseSpec(
            id = "p3_bike", phase = 3, name = "Stationary bike (easy)",
            demoId = "bike",
            cues = listOf(
                "Saddle slightly higher than usual; minimal resistance",
                "Pedal through the heel/midfoot rather than the toes at first",
                "10-15 relaxed minutes"
            ),
            sets = 1, reps = 1, holdSeconds = 600, sessionsPerDay = 1,
            whyItMatters = "Pain-free cardio that gently cycles the ankle through safe range and rebuilds fitness without impact.",
            precaution = "Only once your physio approves; stop if the tendon aches sharply."
        ),
        ExerciseSpec(
            id = "p3_towel", phase = 3, name = "Towel scrunches",
            demoId = "towel_scrunch",
            cues = listOf(
                "Sit with the foot flat on a towel on a smooth floor",
                "Scrunch the towel towards you with your toes",
                "Re-spread and repeat"
            ),
            sets = 2, reps = 10, holdSeconds = 0, sessionsPerDay = 1,
            whyItMatters = "Strengthens the small foot muscles that support the arch and take load off the Achilles with every step.",
            precaution = "Keep the heel grounded throughout."
        )
    )

    private fun phase4Exercises() = listOf(
        ExerciseSpec(
            id = "p4_double_raise", phase = 4, name = "Double-leg heel raises",
            demoId = "double_heel_raise",
            cues = listOf(
                "Stand by a wall or counter for balance",
                "Push up through the balls of both feet",
                "3 seconds up, pause, 3 seconds down"
            ),
            sets = 3, reps = 12, holdSeconds = 1, sessionsPerDay = 2,
            whyItMatters = "The cornerstone of Achilles rehab: progressive calf-raise load is what turns scar tissue into a strong, organised tendon.",
            precaution = "Start sharing weight 50/50; shift weight toward the injured side only as your physio progresses you."
        ),
        ExerciseSpec(
            id = "p4_balance", phase = 4, name = "Single-leg balance",
            demoId = "single_balance",
            cues = listOf(
                "Stand on the injured leg next to support",
                "Soft knee, tall posture, eyes ahead",
                "Progress: eyes closed, then cushion underfoot"
            ),
            sets = 3, reps = 1, holdSeconds = 30, sessionsPerDay = 2,
            whyItMatters = "The rupture also damaged position-sense nerves; retraining balance is what prevents ankle sprains and awkward landings on court later.",
            precaution = "Always have support within reach."
        ),
        ExerciseSpec(
            id = "p4_band_pf", phase = 4, name = "Resistance-band ankle pushes",
            demoId = "band_pf",
            cues = listOf(
                "Long sitting, band looped around the ball of the foot",
                "Push the foot down against the band like a slow gas pedal",
                "Control the return - do not let the band yank the foot up"
            ),
            sets = 3, reps = 15, holdSeconds = 1, sessionsPerDay = 1,
            whyItMatters = "Trains the calf through range with adjustable load, bridging the gap between seated raises and full standing work.",
            precaution = "The band must never pull the foot up past neutral."
        ),
        ExerciseSpec(
            id = "p4_step_up", phase = 4, name = "Step-ups",
            demoId = "step_up",
            cues = listOf(
                "Low step to start; injured foot goes up first",
                "Drive up through the heel, control the step down",
                "Increase step height before adding speed"
            ),
            sets = 3, reps = 10, holdSeconds = 0, sessionsPerDay = 1,
            whyItMatters = "Builds the leg drive and confidence needed for stairs, slopes and eventually the lunging footwork padel demands.",
            precaution = "Use the rail until balance is solid."
        ),
        ExerciseSpec(
            id = "p4_squat", phase = 4, name = "Bodyweight squats",
            demoId = "squat",
            cues = listOf(
                "Feet shoulder-width, weight even between sides",
                "Sit back and down as far as comfortable, heels down",
                "Drive up evenly through both legs"
            ),
            sets = 3, reps = 12, holdSeconds = 0, sessionsPerDay = 1,
            whyItMatters = "Restores symmetrical leg strength and ankle confidence under bend - the position every padel shot starts from.",
            precaution = "Heels stay down; depth only as ankle comfort allows."
        ),
        ExerciseSpec(
            id = "p4_swim", phase = 4, name = "Swimming / bike conditioning",
            demoId = "bike",
            cues = listOf(
                "Bike: build resistance gradually; Swim: gentle kick only",
                "20-30 minutes, conversational effort",
                "Schedule on alternate days to strength work"
            ),
            sets = 1, reps = 1, holdSeconds = 1500, sessionsPerDay = 1,
            whyItMatters = "Rebuilds the aerobic engine so that returning to running in phase 5 is limited by the tendon plan, not by fitness.",
            precaution = "No push-off turns in the pool off the injured foot yet."
        )
    )

    private fun phase5Exercises() = listOf(
        ExerciseSpec(
            id = "p5_single_raise", phase = 5, name = "Single-leg heel raises",
            demoId = "single_heel_raise",
            cues = listOf(
                "Fingertips on a wall for balance only",
                "Rise on the injured leg alone, full height",
                "Slow, controlled lowering every rep"
            ),
            sets = 3, reps = 15, holdSeconds = 1, sessionsPerDay = 1,
            whyItMatters = "20-25 strong single-leg raises is the classic benchmark that the calf-tendon unit is ready for running and court work.",
            precaution = "Quality first: a shaky half-height rep does not count."
        ),
        ExerciseSpec(
            id = "p5_jog", phase = 5, name = "Walk-jog programme",
            demoId = "jog",
            cues = listOf(
                "Flat, even ground; cushioned shoes",
                "Start 1 min jog / 2 min walk x 8, build gradually",
                "No two running days back-to-back at first"
            ),
            sets = 1, reps = 8, holdSeconds = 180, sessionsPerDay = 1,
            whyItMatters = "Graded exposure to impact lets the tendon adapt to running loads without spikes - the safe road back to court speed.",
            precaution = "Only after physio clearance and the single-leg raise benchmark."
        ),
        ExerciseSpec(
            id = "p5_hop", phase = 5, name = "Hop & plyometric progression",
            demoId = "hop",
            cues = listOf(
                "Start: two-leg mini hops on the spot",
                "Progress: single-leg hops, then forward/sideways",
                "Land softly - quiet feet"
            ),
            sets = 3, reps = 10, holdSeconds = 0, sessionsPerDay = 1,
            whyItMatters = "Padel is a game of springs: plyometric capacity is the last physical quality the tendon needs before real rallies.",
            precaution = "Each new hop variation needs physio sign-off."
        ),
        ExerciseSpec(
            id = "p5_agility", phase = 5, name = "Direction-change drills",
            demoId = "agility",
            cues = listOf(
                "Cone shuffles: side-to-side, then diagonal cuts",
                "Start at 50% speed, build over weeks",
                "Stay low and balanced through each turn"
            ),
            sets = 3, reps = 6, holdSeconds = 0, sessionsPerDay = 1,
            whyItMatters = "Sharp direction changes are exactly the load that ruptured the tendon - rehearse them progressively before they happen at match pace.",
            precaution = "Fatigue ruins technique: stop while movements still feel crisp."
        ),
        ExerciseSpec(
            id = "p5_padel", phase = 5, name = "Padel-specific drills",
            demoId = "padel_drill",
            cues = listOf(
                "Stage 1: shadow swings and court movement, no ball",
                "Stage 2: cooperative rallies, no competitive points",
                "Stage 3: friendly matches, then competition - each stage physio-approved"
            ),
            sets = 1, reps = 1, holdSeconds = 1200, sessionsPerDay = 1,
            whyItMatters = "Staged court exposure rebuilds timing and confidence while keeping loads predictable - the final bridge back to the game you are doing all this for.",
            precaution = "Full competitive padel typically returns 9-12 months after injury, only with explicit physio sign-off."
        )
    )

    // ------------------------------------------------------------------
    // Milestones (weeks from injury; typical conservative pathway)
    // ------------------------------------------------------------------

    val milestones: List<Milestone> = listOf(
        Milestone(0, "Injury & boot fitted", "Rupture confirmed; boot on in full equinus; clot-prevention plan started."),
        Milestone(1, "Specialist review", "Consultant confirms the conservative pathway and the boot/wedge plan."),
        Milestone(2, "Settled in the boot", "Pain and swelling settling; weight-bearing as tolerated becoming comfortable."),
        Milestone(3, "First heel-angle reduction", "Heel-angle reduction typically begins (clinic-dependent: weekly or fortnightly)."),
        Milestone(6, "Walking confidently in boot", "Full weight-bearing without crutches for most people."),
        Milestone(8, "Boot at neutral", "Heel angle typically at neutral (0°); foot flat in the boot."),
        Milestone(10, "Boot weaning", "Transition to supportive shoes with a heel raise, guided by your physio."),
        Milestone(12, "Out of the boot", "Normal shoes; gentle stretching may begin ONLY if your physio approves."),
        Milestone(16, "Strength building", "Double-leg heel raises strong; balance work progressing."),
        Milestone(24, "Single-leg strength & jogging", "Single-leg raise benchmark approaching; walk-jog may begin once cleared."),
        Milestone(39, "Padel drills", "Court movement and controlled rallies, physio-approved (~9 months)."),
        Milestone(52, "Return to padel", "Typical window for full competitive return is 9-12 months with sign-off.")
    )

    // ------------------------------------------------------------------
    // Red flags - kept one tap away throughout the app
    // ------------------------------------------------------------------

    val redFlags: List<RedFlagSection> = listOf(
        RedFlagSection(
            id = "pe",
            title = "Possible pulmonary embolism - EMERGENCY",
            urgency = "Call 999 now",
            symptoms = listOf(
                "Sudden breathlessness",
                "Chest pain, especially when breathing in",
                "Coughing up blood",
                "Feeling faint, rapid heartbeat"
            ),
            action = "A clot can travel from the leg to the lungs. This is life-threatening: call 999 immediately."
        ),
        RedFlagSection(
            id = "dvt",
            title = "Possible DVT (blood clot in the leg)",
            urgency = "Same-day medical review - call 111 or your GP now",
            symptoms = listOf(
                "New or worsening calf pain or tenderness (either leg)",
                "Swelling that does not settle with elevation",
                "The calf feels hot to touch",
                "Redness or darkening of the skin on the leg",
                "Vein looks swollen or feels hard"
            ),
            action = "Achilles rupture plus a boot is a high-risk setting for DVT even on clot-prevention medication. Do not wait to see if it settles: get same-day medical advice."
        ),
        RedFlagSection(
            id = "rerupture",
            title = "Possible re-rupture",
            urgency = "Urgent - contact your clinic / fracture clinic today",
            symptoms = listOf(
                "A new snap, pop or sudden sharp pain at the back of the ankle",
                "Sudden loss of push-off power",
                "A new gap or dip you can feel in the tendon",
                "Sudden new swelling around the heel cord"
            ),
            action = "Put the boot back on (with the wedges you last used), avoid weight-bearing, and contact your clinic urgently. Re-rupture risk is highest from week 6 to week 12 and when transitioning out of the boot."
        ),
        RedFlagSection(
            id = "bleeding",
            title = "Bleeding problems on anticoagulant medication",
            urgency = "Urgent medical advice - 111, GP, or 999 if severe",
            symptoms = listOf(
                "Bleeding that will not stop",
                "Unexplained or spreading bruising",
                "Blood in urine, or black/tarry stools",
                "Coughing or vomiting blood",
                "Severe headache or sudden confusion"
            ),
            action = "Clot-prevention medication slightly raises bleeding risk. Severe or persistent bleeding needs urgent assessment - and never stop the medication on your own without medical advice."
        ),
        RedFlagSection(
            id = "boot",
            title = "Boot & skin problems",
            urgency = "Contact your clinic promptly",
            symptoms = listOf(
                "Numbness, tingling or colour change in the toes",
                "Pressure sores, blisters or broken skin under the boot",
                "Pain from the boot that adjustment does not fix"
            ),
            action = "A boot that fits badly can damage skin and nerves. Loosen the straps, recheck the padding, and contact your clinic if it does not settle quickly."
        )
    )

    // ------------------------------------------------------------------
    // Prefills (all editable in-app)
    // ------------------------------------------------------------------

    private val prefillMedications: List<Medication> = listOf(
        Medication(
            id = "med_anticoagulant",
            name = "Anticoagulant",
            dose = "2.5 mg",
            times = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
            notes = "Clot-prevention medication prescribed after the rupture. " +
                "Clinically important: do not stop or skip without medical advice.",
            active = true
        )
    )

    private val prefillTasks: List<RehabTask> = listOf(
        RehabTask(
            id = "task_elevation",
            kind = TaskKind.ELEVATION,
            title = "Elevate the leg",
            detail = "Leg up above heart level for 20-30 minutes to drain swelling.",
            times = listOf(LocalTime.of(10, 0), LocalTime.of(14, 0), LocalTime.of(18, 0)),
            fromPhase = 1, toPhase = 3, dueDate = null, active = true
        ),
        RehabTask(
            id = "task_boot_check",
            kind = TaskKind.BOOT_CHECK,
            title = "Boot check",
            detail = "Straps snug, wedges seated correctly, no rubbing or pressure points on the skin.",
            times = listOf(LocalTime.of(9, 0)),
            fromPhase = 1, toPhase = 3, dueDate = null, active = true
        ),
        RehabTask(
            id = "task_circulation",
            kind = TaskKind.CIRCULATION_CHECK,
            title = "Circulation & calf check",
            detail = "Toes warm and pink? Any new calf pain, heat, swelling or redness? " +
                "If yes - open Red Flags now.",
            times = listOf(LocalTime.of(12, 0), LocalTime.of(21, 0)),
            fromPhase = 1, toPhase = 3, dueDate = null, active = true
        )
    )

    // ------------------------------------------------------------------
    // Return-to-sport self-tests and ladder (criteria, not dates)
    // ------------------------------------------------------------------

    private val selfTests: List<SelfTest> = listOf(
        SelfTest(
            id = "heel_rise_sym", name = "Single-leg heel-rise count", unit = "reps",
            symmetry = true, passThreshold = 90.0, requirePainFree = true, lowerIsBetter = false,
            howTo = listOf(
                "Stand by a wall or counter for light balance support only",
                "On one leg, rise fully onto your toes and lower with control - that's one rep",
                "Count good-quality reps until form drops or you fatigue, then test the other side",
                "Record reps for each side"
            ),
            precaution = "Calf strength symmetry is the single best marker before impact. Stop at sharp tendon pain.",
            earliestPhase = 4
        ),
        SelfTest(
            id = "balance_eo", name = "Single-leg balance (eyes open)", unit = "seconds",
            symmetry = false, passThreshold = 30.0, requirePainFree = false, lowerIsBetter = false,
            howTo = listOf(
                "Stand on the injured leg only, hands on hips, near support",
                "Time how long you hold steady before you touch down or grab support",
                "Cap the timing at 45 seconds"
            ),
            precaution = "Tests the ankle's control and confidence, not just strength.",
            earliestPhase = 4
        ),
        SelfTest(
            id = "calf_girth_sym", name = "Calf circumference", unit = "cm",
            symmetry = true, passThreshold = 95.0, requirePainFree = false, lowerIsBetter = false,
            howTo = listOf(
                "Sit with the leg relaxed; find the widest part of the calf",
                "Measure around it with a tape, same spot on each leg",
                "Record both sides - the injured calf is usually a little smaller at first"
            ),
            precaution = "A shrinking gap shows the calf muscle is rebuilding. Not a strength test on its own.",
            earliestPhase = 4
        ),
        SelfTest(
            id = "walk_tol", name = "Pain-free brisk walk", unit = "minutes",
            symmetry = false, passThreshold = 30.0, requirePainFree = true, lowerIsBetter = false,
            howTo = listOf(
                "Walk briskly on even ground at a comfortable pace",
                "Record how many minutes you manage with no tendon pain and no limp",
                "Stop the count the moment pain or a limp appears"
            ),
            precaution = "Walking tolerance is the floor you build running on.",
            earliestPhase = 4
        ),
        SelfTest(
            id = "jog_tol", name = "Continuous easy jog", unit = "minutes",
            symmetry = false, passThreshold = 20.0, requirePainFree = true, lowerIsBetter = false,
            howTo = listOf(
                "Only once your physio has cleared jogging",
                "On a flat, even surface, jog easily and record continuous pain-free minutes",
                "Next-morning stiffness that settles within an hour is acceptable; worsening week-on-week is not"
            ),
            precaution = "Build by a few minutes per session, never in big jumps.",
            earliestPhase = 5
        ),
        SelfTest(
            id = "hop_count", name = "Single-leg hops in a row", unit = "hops",
            symmetry = false, passThreshold = 20.0, requirePainFree = true, lowerIsBetter = false,
            howTo = listOf(
                "Only once your physio has cleared hopping",
                "On the injured leg, do small controlled pogo hops on the spot",
                "Count consecutive springy, pain-free hops before form drops"
            ),
            precaution = "Land softly through the forefoot; this is springiness, not height.",
            earliestPhase = 5
        ),
        SelfTest(
            id = "hop_sym", name = "Single-leg hop for distance", unit = "cm",
            symmetry = true, passThreshold = 90.0, requirePainFree = true, lowerIsBetter = false,
            howTo = listOf(
                "Only once your physio has cleared hopping",
                "From standing on one leg, hop forward as far as you can land cleanly and hold it",
                "Measure the distance for each leg (best of 3) and record both"
            ),
            precaution = "Limb symmetry on hopping is a key gate before cutting and court sport.",
            earliestPhase = 5
        )
    )

    private val returnToSport: List<RtsRung> = listOf(
        RtsRung(
            id = "rts_strength", order = 1, title = "Single-leg strength base", phase = 4,
            summary = "Rebuild calf strength, balance and walking tolerance before any impact.",
            testIds = listOf("heel_rise_sym", "balance_eo", "calf_girth_sym", "walk_tol"),
            guidance = listOf(
                "This is the foundation - skipping it is how tendons get re-injured later.",
                "Re-test every week or two; expect steady, not overnight, gains."
            ),
            requiresPhysioSignoff = false
        ),
        RtsRung(
            id = "rts_jog", order = 2, title = "Cleared to start jogging", phase = 5,
            summary = "Strength and walking are there; impact can begin - with physio sign-off.",
            testIds = listOf("heel_rise_sym", "walk_tol"),
            guidance = listOf(
                "Starting impact too early is the classic setback. Your physio confirms this step.",
                "Begin with walk-jog intervals, not a continuous run."
            ),
            requiresPhysioSignoff = true
        ),
        RtsRung(
            id = "rts_run_hop", order = 3, title = "Running & hopping", phase = 5,
            summary = "Build continuous jogging and basic plyometric capacity.",
            testIds = listOf("jog_tol", "hop_count", "heel_rise_sym"),
            guidance = listOf(
                "Add hopping work alongside steady jogging volume.",
                "Keep one easy day between impact sessions early on."
            ),
            requiresPhysioSignoff = true
        ),
        RtsRung(
            id = "rts_agility", order = 4, title = "Change of direction", phase = 5,
            summary = "Add the cutting and lateral movement padel actually demands.",
            testIds = listOf("hop_sym", "hop_count"),
            guidance = listOf(
                "Side shuffles, then diagonal cuts, building from 50% speed.",
                "Sharp direction changes are exactly what ruptured the tendon - rehearse them progressively."
            ),
            requiresPhysioSignoff = true
        ),
        RtsRung(
            id = "rts_padel", order = 5, title = "Return to padel", phase = 5,
            summary = "Court movement, then controlled rallies, then friendly matches, then competition.",
            testIds = listOf("hop_sym"),
            guidance = listOf(
                "Stage it: shadow movement, cooperative rallies, friendly games, competition.",
                "Each stage needs physio sign-off; full competitive padel is typically 9-12 months after injury."
            ),
            requiresPhysioSignoff = true
        )
    )

    // ------------------------------------------------------------------
    // The emotional side: what's normal to feel, and reassurance
    // ------------------------------------------------------------------

    private val mindset: List<PhaseMindset> = listOf(
        PhaseMindset(
            phase = 1,
            normalToFeel = listOf(
                "Shock and frustration that one wrong step changed your routine - that's normal",
                "Feeling clumsy and dependent on crutches and the boot",
                "Anxiety about the clot risk - the medication and your daily checks are exactly how you manage it"
            ),
            encouragement = "Right now your only job is to protect the tendon and rest. Doing little is doing the work."
        ),
        PhaseMindset(
            phase = 2,
            normalToFeel = listOf(
                "Impatience as the days blur together in the boot",
                "Small wins - a longer walk, fewer crutches - feeling surprisingly big",
                "Worry every time you lower the heel angle; a bit of unfamiliarity is expected"
            ),
            encouragement = "Steady, boring weeks are good weeks. The tendon is knitting on schedule."
        ),
        PhaseMindset(
            phase = 3,
            normalToFeel = listOf(
                "Nervousness about those first steps out of the boot - almost everyone feels it",
                "The ankle feeling stiff, weak and strangely unfamiliar",
                "A wobble of confidence on uneven ground"
            ),
            encouragement = "Confidence comes back one careful step at a time. Trust the wean, not the calendar."
        ),
        PhaseMindset(
            phase = 4,
            normalToFeel = listOf(
                "Motivation returning as you can finally train and feel stronger",
                "Frustration that the calf is weaker than you expected",
                "Comparing yourself to where you 'should' be - try not to"
            ),
            encouragement = "This is where the real rebuilding happens. Consistent strength work now is what gets you back on court."
        ),
        PhaseMindset(
            phase = 5,
            normalToFeel = listOf(
                "Excitement and nerves about impact and, finally, padel",
                "Fear of re-rupture the first time you jog, hop or change direction",
                "Wanting to rush the last stretch - the hardest patience of all"
            ),
            encouragement = "You've earned this stage. Respect each step-up and the court will still be there."
        )
    )

    private val reassurance = Reassurance(
        title = "Worried about re-rupture?",
        body = "Almost everyone recovering from an Achilles rupture feels a jolt of fear at every twinge. " +
            "That fear is normal and it fades as strength and trust return. Knowing the difference between " +
            "ordinary healing sensations and a genuine warning sign is what turns anxiety into confidence.",
        normalVsFlag = listOf(
            "Morning stiffness that eases as you move" to "A sudden snap or pop with loss of push-off power",
            "A tendon that looks/feels a little thicker than the other side" to "A new gap or dip you can feel in the tendon",
            "Mild ache for a day after a harder session" to "Sharp tendon pain that stops you mid-step",
            "Twinges that settle within a day" to "New calf pain, heat, swelling or redness - think DVT"
        )
    )

    // ------------------------------------------------------------------
    // The registry entry: everything above, wired as framework data
    // ------------------------------------------------------------------

    val protocol = InjuryProtocol(
        id = ID,
        injuryName = "Achilles tendon rupture",
        variantName = "Conservative (non-surgical) · walking boot",
        protocolName = "Conservative (non-surgical) functional rehabilitation - UK NHS-style pathway",
        placeholderNote = PLACEHOLDER_NOTE,
        sided = true,
        supportDevice = SupportDevice(
            name = "Walking boot",
            unitName = "degree",
            unitNamePlural = "degrees",
            unitSymbol = "°",
            reductionVerb = "lower the heel angle",
            maxValue = 40,
            // heel angle stepped from 30° to neutral, 5° a week from week 3 -> 0° by ~week 8
            plan = WedgePlan(initialWedges = 30, removalStartWeek = 3, removalIntervalDays = 7, stepSize = 5)
        ),
        phases = phases,
        milestones = milestones,
        redFlags = redFlags,
        movementChecks = listOf(
            MovementCheckSpec("Walk without the boot", 3,
                "Not before ~week 8-10, physio-confirmed", "Physio-guided weaning only"),
            MovementCheckSpec("Pull foot up past neutral / calf stretch", 4,
                "Not before week 12 - tendon over-lengthening risk", "Gentle and physio-guided only"),
            MovementCheckSpec("Drive a car", 4,
                "Generally only out of the boot and able to emergency-brake - confirm with clinic and insurer",
                "Confirm with clinic and insurer"),
            MovementCheckSpec("Standing heel raises", 4,
                "Phase 4 work - too early now", "Progress as prescribed"),
            MovementCheckSpec("Run / jump", 5,
                "Phase 5 work after strength benchmarks", "Graded programme once physio clears it"),
            MovementCheckSpec("Play padel", 5,
                "The end goal - but not yet",
                "Drills first; competitive play typically 9-12 months with sign-off")
        ),
        selfTests = selfTests,
        returnToSport = returnToSport,
        mindset = mindset,
        reassurance = reassurance,
        bodySceneId = "lower_leg",
        welcomeBlurb = "Your daily coach through a conservative (non-surgical) Achilles " +
            "rupture - exercises, reminders and progress tracking.",
        safetyTitle = "Safety first",
        safetyBlurb = "Achilles rupture carries a real risk of blood clots - that is why " +
            "you take a clot-prevention medication. The red-flag button stays at the top " +
            "of every screen. Read it once now so you know what to watch for.",
        redFlagIntro = "After an Achilles rupture you are at raised risk of a blood clot, and " +
            "the healing tendon can re-tear. Take these signs seriously even if they seem mild.",
        redFlagButtonLabel = "DVT & re-rupture red flags",
        videoContext = "Achilles rupture rehab physiotherapy",
        prefillDescription = "Full Achilles tendon rupture (left), injured playing padel; " +
            "managed conservatively in a walking boot.",
        prefillGoal = "Full recovery and return to playing padel",
        prefillAppointments = listOf(
            Appointment(LocalDate.of(2026, 6, 7), "Consultant review", completed = true)
        ),
        prefillMedications = prefillMedications,
        prefillTasks = prefillTasks
    )
}

/**
 * Personal first-run seed for THIS build's user, layered over the protocol
 * prefills. Every field is editable in-app.
 */
object Defaults {
    fun profile(): Profile {
        val p = ProtocolRegistry.default
        return Profile(
            protocolId = p.id,
            name = "",
            injuryDate = LocalDate.of(2026, 6, 2),
            side = Side.LEFT,
            pathway = Pathway.CONSERVATIVE_NON_SURGICAL,
            injuryDescription = p.prefillDescription,
            goal = p.prefillGoal,
            appointments = p.prefillAppointments,
            wedgePlan = p.supportDevice?.plan ?: WedgePlan(0, 1, 7),
            currentWedges = p.supportDevice?.plan?.initialWedges ?: 0,
            weightBearing = WeightBearing.AS_TOLERATED,
            physioConfirmedPhase = 1,
            phaseStartOverrides = emptyMap(),
            onboardingComplete = false,
            disclaimerAcknowledged = false
        )
    }

    fun medications(): List<Medication> = ProtocolRegistry.default.prefillMedications
    fun tasks(): List<RehabTask> = ProtocolRegistry.default.prefillTasks
}
