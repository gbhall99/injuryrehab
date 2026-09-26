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
                "Watch daily for DVT warning signs - calf pain, heat, swelling, redness",
                "Pain relief: paracetamol is usually first choice - avoid anti-inflammatories (ibuprofen, naproxen) " +
                    "while on a blood thinner unless your doctor agrees"
            ),
            allowed = listOf(
                "Walking short distances in the boot with crutches, putting weight through as comfort allows",
                "Wiggling and scrunching toes inside the boot",
                "Knee, hip and core exercises with the boot on",
                "Sitting with the leg elevated above heart level",
                "Stairs on crutches: up with the good leg first, down with the booted leg first - or sit and " +
                    "shuffle on your bottom",
                "Washing with the boot off ONLY if seated, foot pointed down, no weight through it (if your clinic allows)"
            ),
            notAllowed = listOf(
                "Any step without the boot",
                "Pulling the foot/toes up towards you (dorsiflexion past the boot angle)",
                "Calf stretching of any kind",
                "Driving",
                "Running, jumping, sport of any kind - {sport} comes much later"
            ),
            exercises = phase1Exercises(),
            physioQuestions = listOf(
                "Can the boot come off to wash or sleep - and how do I keep my foot pointed down if it does?",
                "When is my first heel-angle reduction, and will the steps be weekly or fortnightly?",
                "How long will I need the blood thinner?",
                "How much weight can I put through the boot, and when can I stop using crutches?"
            )
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
                "Lowering the heel angle faster than planned to “speed things up”",
                "Driving (most people cannot drive safely in a boot - ask your clinic and insurer)",
                "Impact activity: running, jumping, {sport}"
            ),
            exercises = phase2Exercises(),
            physioQuestions = listOf(
                "Is my heel angle on track to reach neutral (foot flat) by around week 8?",
                "When can I come off crutches completely?",
                "Do I still need to sleep in the boot?",
                "Does the blood thinner stop when the boot comes off?"
            )
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
                "Around 8 weeks since injury with the boot at neutral (foot flat) - confirm with your physio",
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
                "Use the heel raise your clinic advises (often in both shoes, so you stay level) and lower it only " +
                    "as directed",
                "Re-rupture risk is highest in this transition out of the boot - progress only as your physio directs",
                "Keep wearing the boot in crowded or unpredictable places until cleared"
            ),
            allowed = listOf(
                "Walking indoors in supportive shoes with heel raise (as physio directs)",
                "Gentle active ankle movement: down fully, up to neutral only",
                "Stationary cycling with low resistance once your physio approves",
                "Pool walking and gentle swimming once your physio approves - no pushing off the wall with the " +
                    "injured foot"
            ),
            notAllowed = listOf(
                "Calf stretches (before week 12, and after only when physio says)",
                "Barefoot or flat-shoe walking",
                "Single-leg heel raises - far too early",
                "Running, hopping, jumping",
                "{Sport}, even a gentle session"
            ),
            exercises = phase3Exercises(),
            physioQuestions = listOf(
                "How should I wean out of the boot - indoors first, then outdoors? For how long each day?",
                "Is the tendon healing at the right length? (Your physio can compare the resting angle of " +
                    "each ankle.)",
                "Do I need a heel raise in my shoes - in one or both, and for how long?",
                "When can I stop wearing the boot at night and outdoors?"
            )
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
                "Rebuild calf strength step by step: two legs, then up-on-two-down-on-one, then single-leg - " +
                    "adding load as your physio directs",
                "Restore balance and proprioception on the injured side",
                "Walk 30+ minutes comfortably with a symmetrical pattern",
                "Build general leg strength: squats, step-ups, bridges"
            ),
            precautions = listOf(
                "Don't stretch the calf unless your physio prescribes it - most people regain movement by walking " +
                    "and strengthening, and over-stretching can leave the tendon long and push-off weak",
                "Use the pain rule: up to about 3-4/10 during exercise is OK if it settles by the next morning; " +
                    "sharp pain, or worse the next day, means drop back a step and tell your physio",
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
                "Returning to {sport} - that is phase 5 work",
                "Maximal single-leg hopping or sprinting"
            ),
            exercises = phase4Exercises(),
            physioQuestions = listOf(
                "When can I progress from two-leg to single-leg heel raises?",
                "How much weight should I add to calf raises, and how often should I load them?",
                "Is my heel-rise height on the injured side close to the other side?",
                "What do I need to achieve before I can start jogging?"
            )
        ),
        PhaseSpec(
            number = 5,
            title = "Return to sport",
            subtitle = "Week 24 onwards · earn your way back to {sport}",
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
                "{Sport}-specific drills: graded, physio-approved practice",
                "Return to competitive {sport} when cleared - typically 9-12 months after injury"
            ),
            precautions = listOf(
                "Each step up (jog, hop, agility, rally, match) needs physio sign-off",
                "Warm up thoroughly; fatigue is when re-injuries happen",
                "The healed tendon often stays slightly thicker - that is normal",
                "Keep calf strengthening going 2-3 times a week, even once you're back - it's the best " +
                    "protection against re-injury",
                "Morning tendon stiffness that worsens week-on-week means back off and ask your physio"
            ),
            allowed = listOf(
                "Graded running once cleared",
                "Plyometric progressions once cleared",
                "{Sport} drills once cleared",
                "Full competitive {sport} typically from 9-12 months, with physio sign-off"
            ),
            notAllowed = listOf(
                "Competitive matches before your physio explicitly signs them off",
                "Skipping progression steps after a good week",
                "Playing through sharp tendon pain"
            ),
            exercises = phase5Exercises(),
            physioQuestions = listOf(
                "What heel-raise and hop numbers do you want to see before I run, hop or play?",
                "How should I build my running and hopping from week to week?",
                "What should the steps back to {sport} look like - drills, practice, then matches?",
                "What should I keep doing long-term to protect the tendon?"
            )
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
            id = "p4_double_raise", phase = 4, name = "Heel-raise progression (two legs → one)",
            demoId = "double_heel_raise",
            cues = listOf(
                "Stand by a wall or counter, fingertips on it for balance only",
                "Stage 1 - both legs: 3 seconds up, pause, 3 seconds down, weight shared 50/50",
                "Stage 2 - up on two, down on one: rise on both feet, lift the good foot at the top, lower slowly on the injured leg",
                "Stage 3 - single-leg: rise and lower on the injured leg alone, full height",
                "Move up a stage only when the current one is easy, full height and no worse the next morning - and your physio agrees"
            ),
            sets = 3, reps = 15, holdSeconds = 1, sessionsPerDay = 1,
            whyItMatters = "The cornerstone of Achilles rehab: progressive calf-raise load is what turns scar tissue into a strong, organised tendon. Working from two legs to one builds the single-leg strength every later stage depends on.",
            precaution = "Height matters as much as reps - a rep that doesn't reach full height doesn't count. Lower slowly; never bounce."
        ),
        ExerciseSpec(
            id = "p4_soleus_raise", phase = 4, name = "Seated calf raises with weight",
            demoId = "seated_heel_raise",
            cues = listOf(
                "Sit with the knee bent at 90 degrees, foot flat",
                "Rest a weight on the injured knee - a heavy bag or dumbbell",
                "Push up through the ball of the foot, pause at the top, lower over 3 seconds"
            ),
            sets = 3, reps = 12, holdSeconds = 1, sessionsPerDay = 1,
            whyItMatters = "Bent-knee raises load the soleus, the deep calf muscle that takes the most force when you walk and run. Weight on the knee strengthens it without any balance demand.",
            precaution = "Start light (around 5 kg) and add weight gradually as your physio directs - it should feel hard by the last few reps, never sharp."
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
            whyItMatters = "Builds the leg drive and confidence needed for stairs, slopes and eventually the footwork {sport} demands.",
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
            whyItMatters = "Restores symmetrical leg strength and ankle confidence under bend - the positions {sport} demands.",
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
            precaution = "No push-off turns in the pool off the injured foot yet.",
            intervalDays = 2
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
            whyItMatters = "20-25 strong single-leg raises - reaching the same height as the other side - is the classic benchmark that the calf-tendon unit is ready for running and court work.",
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
            precaution = "Only after physio clearance and the single-leg raise benchmark. Next-morning stiffness that settles within an hour is fine; if it's worse, repeat the last level.",
            intervalDays = 2
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
            whyItMatters = "Sport is built on springs: plyometric capacity is the last physical quality the tendon needs before returning to {sport}.",
            precaution = "Each new hop variation needs physio sign-off.",
            intervalDays = 2
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
            precaution = "Fatigue ruins technique: stop while movements still feel crisp.",
            intervalDays = 2
        ),
        ExerciseSpec(
            id = "p5_padel", phase = 5, name = "{Sport}-specific drills",
            demoId = "padel_drill",
            cues = listOf(
                "Stage 1: sport-specific movement patterns at low intensity, no resistance",
                "Stage 2: light, controlled practice - skills before speed",
                "Stage 3: progressive return to full play, then competition - each stage physio-approved"
            ),
            sets = 1, reps = 1, holdSeconds = 1200, sessionsPerDay = 1,
            whyItMatters = "Staged exposure to {sport} rebuilds timing and confidence while keeping loads predictable - the final bridge back to the game you are doing all this for.",
            precaution = "Full competitive {sport} typically returns 9-12 months after injury, only with explicit physio sign-off.",
            intervalDays = 2
        )
    )

    // ------------------------------------------------------------------
    // Milestones (weeks from injury; typical conservative pathway)
    // ------------------------------------------------------------------

    val milestones: List<Milestone> = listOf(
        Milestone(0, "Injury & boot fitted", "Rupture confirmed; boot on in full equinus; clot-prevention plan started.", phase = 1),
        Milestone(1, "Specialist review", "Consultant confirms the conservative pathway and the boot plan.", phase = 1),
        Milestone(2, "Settled in the boot", "Pain and swelling settling; weight-bearing as tolerated becoming comfortable.", phase = 1),
        Milestone(3, "First heel-angle reduction", "Heel-angle reduction typically begins (clinic-dependent: weekly or fortnightly).", phase = 2),
        Milestone(6, "Walking confidently in boot", "Full weight-bearing without crutches for most people.", phase = 2),
        Milestone(8, "Boot at neutral", "Heel angle typically at neutral (0°); foot flat in the boot.", phase = 3),
        Milestone(10, "Boot weaning", "Transition to supportive shoes with a heel raise, guided by your physio.", phase = 3),
        Milestone(12, "Out of the boot", "Normal supportive shoes. No calf stretching unless your physio prescribes it.", phase = 4),
        Milestone(16, "Strength building", "Two-leg heel raises strong and moving towards single-leg; balance work progressing.", phase = 4),
        Milestone(24, "Single-leg strength & jogging", "Single-leg raise benchmark approaching; walk-jog may begin once cleared.", phase = 5),
        Milestone(39, "{Sport} drills", "Sport-specific drills and graded practice, physio-approved (~9 months).", phase = 5),
        Milestone(52, "Return to {sport}", "Typical window for full competitive return is 9-12 months with sign-off.", phase = 5)
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
            urgency = "Urgent - contact your fracture clinic today (A&E if it's closed)",
            symptoms = listOf(
                "A new snap, pop or sudden sharp pain at the back of the ankle",
                "Sudden loss of push-off power",
                "A new gap or dip you can feel in the tendon",
                "Sudden new swelling around the heel cord"
            ),
            action = "Stop, put the boot back on at the last setting you used (or keep the foot pointed down if you no " +
                "longer have it), keep weight off the leg and contact your fracture clinic today - if it's closed, go to " +
                "A&E or an urgent treatment centre. Re-rupture is most common between about weeks 6 and 12 and when " +
                "coming out of the boot."
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
                "Severe headache or sudden confusion",
                "Any knock to the head - e.g. from a fall on crutches - even if you feel fine"
            ),
            action = "Clot-prevention medication slightly raises bleeding risk. Severe or persistent bleeding needs urgent " +
                "assessment, and any head injury while taking it needs same-day assessment at A&E even if you feel well - " +
                "bleeding inside the head can show up hours later. Never stop the medication on your own without medical advice."
        ),
        RedFlagSection(
            id = "boot",
            title = "Boot & skin problems",
            urgency = "Contact your clinic promptly",
            symptoms = listOf(
                "Numbness, tingling or colour change in the toes",
                "Pressure sores, blisters or broken skin under the boot",
                "Pain from the boot that adjustment does not fix",
                "Severe or increasing pain in the foot or calf that rest, elevation and loosening the straps don't ease"
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
                "Clinically important: do not stop or skip without medical advice. " +
                "This is usually a time-limited course (often while the boot is on) - " +
                "the end date below is an editable placeholder, so confirm with your " +
                "clinician when to stop. Avoid anti-inflammatory painkillers (ibuprofen, " +
                "naproxen) unless your doctor agrees, and get any head injury checked the " +
                "same day. Missed a dose? Check the leaflet or ask a pharmacist - never double up.",
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
            detail = "Straps snug, boot set as your plan expects, no rubbing or pressure points on the skin.",
            // with the morning dose: fewer separate interruptions
            times = listOf(LocalTime.of(8, 0)),
            fromPhase = 1, toPhase = 3, dueDate = null, active = true
        ),
        RehabTask(
            id = "task_circulation",
            kind = TaskKind.CIRCULATION_CHECK,
            title = "Circulation & calf check",
            detail = "Toes warm and pink? Any new calf pain, heat, swelling or redness? " +
                "If yes - open Red Flags now.",
            // paired with the blood-thinner doses: one "clot prevention" moment, morning and evening
            times = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
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
                "Stand on one leg facing a wall, fingertips on it for balance only - no pushing",
                "Rise as high as you can and lower with control, at a steady pace of one rep every 2 seconds",
                "Only count reps that reach close to the height of your first few; stop when you can't, or at sharp pain",
                "Test the other side the same way and record both"
            ),
            precaution = "Calf endurance symmetry is one of the best markers before impact. Stop at sharp tendon pain.",
            earliestPhase = 4
        ),
        SelfTest(
            id = "heel_rise_height", name = "Single-leg heel-rise height", unit = "cm",
            symmetry = true, passThreshold = 90.0, requirePainFree = true, lowerIsBetter = false,
            howTo = listOf(
                "Stand side-on to a wall with a ruler or tape measure held upright beside your heel",
                "Rise onto your toes on one leg as high as you can and hold for a moment",
                "Have someone read how high the back of your heel lifts (or mark the wall) - best of 3",
                "Repeat on the other leg and record both"
            ),
            precaution = "Height matters as much as reps: a lower heel-rise on the injured side can mean the tendon has " +
                "healed a little long - worth showing your physio.",
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
        ),
        SelfTest(
            id = "run_long", name = "Longer continuous run", unit = "minutes",
            symmetry = false, passThreshold = 40.0, requirePainFree = true, lowerIsBetter = false,
            howTo = listOf(
                "Only once you're comfortable with continuous easy jogging",
                "On flat, even ground, run easily and record continuous pain-free minutes",
                "Build distance gradually - no more than about 10% more per week"
            ),
            precaution = "Endurance running is calf-endurance work; increase volume slowly.",
            earliestPhase = 5
        )
    )

    // The SHARED foundation only; sport-specific tail stages live in SportRegistry
    // so the same rehab scales from padel to running to cycling.
    private val returnToSport: List<RtsRung> = listOf(
        RtsRung(
            id = "rts_strength", order = 1, title = "Single-leg strength base", phase = 4,
            summary = "Rebuild calf strength, balance and walking tolerance before any impact.",
            testIds = listOf("heel_rise_sym", "heel_rise_height", "balance_eo", "calf_girth_sym", "walk_tol"),
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
                "Excitement and nerves about impact and, finally, {sport}",
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
            "Twinges that settle within a day" to "New calf pain, heat, swelling or redness - think DVT",
            "Ankle swelling by evening that's gone by morning" to "Swelling that won't settle with elevation"
        )
    )

    // ------------------------------------------------------------------
    // What to expect (week-banded; proactive reassurance)
    // ------------------------------------------------------------------

    private val expectations: List<WeekExpectation> = listOf(
        WeekExpectation(0, 2, "The first two weeks",
            "Protect the tendon and settle the swelling. This is the most cautious stretch.",
            listOf(
                "Bruising and swelling down the calf, ankle and foot - normal as it settles",
                "The boot feeling heavy and awkward; sleep can be disrupted",
                "Throbbing when the leg hangs down - elevate above heart level to ease it",
                "Needing regular simple pain relief for the first week or so - paracetamol is usually preferred on a blood thinner"
            ),
            "You're not being overcautious - early protection is exactly what gives the tendon the best result."),
        WeekExpectation(2, 8, "Weeks 2-8: weight-bearing & heel-angle reduction",
            "Build confidence on the leg as the heel angle steps down toward neutral.",
            listOf(
                "Walking further in the boot - many people are off crutches somewhere between weeks 2 and 6",
                "Each heel-angle reduction feeling odd for a day or two - that settles",
                "Calf looking thinner than the other side - muscle wasting is expected and reversible"
            ),
            "Slow, steady weeks are good weeks; the tendon is healing on schedule even when nothing feels dramatic."),
        WeekExpectation(8, 12, "Weeks 8-12: out of the boot",
            "Wean out of the boot and re-learn a normal, even walking pattern.",
            listOf(
                "First steps out of the boot feeling wobbly and the ankle very stiff",
                "A limp at first - a heel raise in the shoe helps while strength returns",
                "Re-rupture anxiety peaking around now; that's normal as protection comes off",
                "The tendon feeling thick or lumpy where it healed - normal scar tissue that remodels over months",
                "The ankle swelling by the end of the day - common for 6-12 months and it settles overnight"
            ),
            "Nerves out of the boot are universal. Progress at your physio's pace and confidence follows strength."),
        WeekExpectation(12, 24, "Weeks 12-24: strengthening",
            "The rebuild: calf strength, balance and gait, working toward single-leg strength.",
            listOf(
                "Steady strength gains - double-leg, then eventually single-leg heel raises",
                "Mild ache after sessions is fine; sharp tendon pain is not",
                "Frustration that strength lags expectations - it takes months, not weeks",
                "Calf still visibly smaller than the other side - it catches up slowly and may never fully match"
            ),
            "This phase does the real work. Consistency now is what gets you back to {sport}."),
        WeekExpectation(24, 52, "6-12 months: return to {sport}",
            "Earn impact back in stages - jogging, hopping, change of direction, then your sport.",
            listOf(
                "Returning to jogging, then graded running and hopping once cleared",
                "The tendon often staying slightly thicker than the other side - that's normal",
                "Morning stiffness that eases as you warm up"
            ),
            "Most people return to {sport} around 9-12 months. Respect each step-up and the court will still be there."),
        WeekExpectation(52, 520, "Beyond a year",
            "Most function is back; keep the calf strong to protect against re-injury.",
            listOf(
                "Near-symmetrical strength and confidence in most activities",
                "Occasional stiffness after a hard session - usually nothing to worry about",
                "Keeping up calf strengthening long-term lowers re-injury risk"
            ),
            "You've done the hard part. Maintenance strength work keeps the gains for good.")
    )

    // ------------------------------------------------------------------
    // Stay-fit: general conditioning that doesn't load the tendon
    // ------------------------------------------------------------------

    private val fitness: List<FitnessActivity> = listOf(
        FitnessActivity("f_upper", "Seated press & row", "Upper body", 1,
            "Dumbbells or a resistance band: shoulder press, rows, biceps and triceps. Sit tall with the boot " +
                "resting - your upper body stays strong with zero load on the tendon."),
        FitnessActivity("f_pushups", "Incline or knee push-ups", "Upper body", 1,
            "Hands on a sofa or wall (or knees down) so the foot takes no weight - solid chest and arm work."),
        FitnessActivity("f_core", "Core circuit", "Core & trunk", 1,
            "Dead bugs, bird-dogs, side planks and gentle crunches. Keep the boot on and the ankle completely still."),
        FitnessActivity("f_cardio", "No-impact cardio", "Cardio (no impact)", 1,
            "Get your heart rate up without loading the leg: fast banded punches, seated 'boxing' rounds, an arm " +
                "bike (ergometer) at the gym, or brisk upper-body circuits."),
        FitnessActivity("f_glutes", "Hip & glute work", "Hips & glutes", 1,
            "Clamshells, side-lying leg lifts and bridges driven through the good side - keeps the hips strong " +
                "for when you're walking again."),
        FitnessActivity("f_bike", "Stationary bike (light)", "Once out of the boot", 3,
            "Once your physio approves: easy spinning at low resistance, pedalling through the heel and midfoot."),
        FitnessActivity("f_swim", "Swimming & pool", "Once out of the boot", 3,
            "Once your physio approves (and any skin sores from the boot have healed): pool walking and easy " +
                "swimming are excellent low-impact cardio. No pushing off the wall with the injured foot at first."),
        FitnessActivity("f_gym", "Good-side & gym strength", "Once out of the boot", 4,
            "Leg press, squats and step-ups within physio guidance; train the uninjured leg hard to limit overall " +
                "strength loss while the injured side catches up.")
    )

    // ------------------------------------------------------------------
    // Everyday questions for the offline Recovery coach (phase-aware)
    // ------------------------------------------------------------------

    private const val EVERYDAY = "Everyday life"
    private const val SYMPTOMS = "Pain, swelling & medicines"
    private const val ACTIVITY = "Exercise & activity"
    private const val SAFETY = "Safety"

    private val faqs: List<Faq> = listOf(
        Faq("sleep", "Can I take the boot off to sleep?", EVERYDAY,
            keywords = listOf("sleep", "sleeping", "bed", "bedtime", "night", "nights", "overnight"),
            answers = mapOf(
                1 to "Not yet. Most UK pathways keep the boot on day and night for the first 6-8 weeks - an " +
                    "unguarded movement in your sleep, like pulling the toes up, can stretch the healing tendon. " +
                    "Loosen the straps slightly for comfort and rest the leg on a pillow. Some clinics allow nights " +
                    "out of the boot from around week 6 - only if yours has said so.",
                3 to "Often yes by now, if your physio has agreed - many clinics stop night-time wear during the " +
                    "wean. If you're not sure, keep it on until you've asked.",
                4 to "Yes - the boot isn't needed at night at this stage."),
            listToPhase = 3),
        Faq("wash", "Can I shower or have a bath?", EVERYDAY,
            keywords = listOf("shower", "showering", "bath", "baths", "bathe", "bathing", "wash", "washing"),
            answers = mapOf(
                1 to "Ask your clinic whether the boot can come off to wash. If it can: sit down to do it, keep the " +
                    "foot pointed down (never pulled up), put no weight through it, and put the boot back on before " +
                    "you stand. A shower stool and a waterproof cover help. Never stand in the shower without the " +
                    "boot in these weeks.",
                3 to "Usually yes, normally, once you're weaning out of the boot. Take care on wet floors - a " +
                    "non-slip mat helps while balance and confidence come back."),
            listToPhase = 2),
        Faq("stairs", "How do I manage stairs?", EVERYDAY,
            keywords = listOf("stairs", "stair", "staircase", "upstairs", "downstairs"),
            answers = mapOf(
                1 to "On crutches: going UP, lead with the good leg, then bring the booted leg and crutches. Going " +
                    "DOWN, crutches and booted leg first, then the good leg ('up with the good, down with the bad'). " +
                    "Use a rail if there is one. If you feel unsteady, sit and shuffle up or down on your bottom - " +
                    "it's safe and very common.",
                3 to "One step at a time with the rail: good leg leads going up, injured leg leads coming down. Your " +
                    "physio will tell you when to try normal step-over-step.",
                4 to "Build back to normal stairs as strength returns - step-ups in your exercises train exactly " +
                    "this. Keep a hand on the rail coming down until it feels solid."),
            listToPhase = 3),
        Faq("crutches", "When can I stop using crutches?", EVERYDAY,
            keywords = listOf("crutch", "crutches", "walking stick"),
            answers = mapOf(
                1 to "Use them whenever you walk for now. Most UK pathways let you put as much weight through the " +
                    "boot as is comfortable, so you'll lean on them less and less.",
                2 to "Many people come off crutches between weeks 2 and 6, once they can walk in the boot without " +
                    "pain or a limp. Wean from two to one (held in the hand opposite the injured leg), then none.",
                3 to "You should be walking in the boot without crutches by now. A single crutch or stick can help " +
                    "for the first days out of the boot - hold it in the hand opposite the injured leg."),
            listToPhase = 3),
        Faq("work", "When can I go back to work?", EVERYDAY,
            keywords = listOf("work", "job", "office", "desk", "back to work"),
            answers = mapOf(
                1 to "It depends on your job. Desk work: often from about 2 weeks, if you can get there safely and " +
                    "keep the leg up. Standing or walking jobs: usually once you're out of the boot (around 8-12 " +
                    "weeks). Heavy manual work, ladders or rough ground: often 3-6 months. Your GP can give a fit " +
                    "note with adjustments such as phased hours or seated work.",
                4 to "Most jobs are manageable now. Heavy manual work, ladders and uneven ground are the last to " +
                    "return - often 4-6 months. Ask your physio about any specific demands of your job."),
            listToPhase = 4),
        Faq("travel", "Can I fly or take a long journey?", EVERYDAY,
            keywords = listOf("fly", "flying", "flight", "flights", "plane", "holiday", "travel", "travelling",
                "traveling", "journey", "abroad"),
            answers = mapOf(
                1 to "Talk to your clinic before any flight or journey over about 4 hours while you're in the boot " +
                    "or on a blood thinner - immobility plus travel raises clot risk, and you may need clot " +
                    "prevention for the trip. If you do travel: an aisle seat, toe and knee exercises every 30 " +
                    "minutes, walk when you can and stay well hydrated.",
                4 to "Usually fine once you're out of the boot and walking normally. Keep moving on long journeys - " +
                    "walk the aisle, do calf and toe exercises - and drink plenty of water. Check with your clinic " +
                    "if you're still on a blood thinner."),
            listToPhase = 3),
        Faq("painkillers", "What painkillers can I take?", SYMPTOMS,
            keywords = listOf("painkiller", "painkillers", "pain killer", "pain killers", "ibuprofen", "nurofen",
                "naproxen", "paracetamol", "nsaid", "nsaids", "anti inflammatory", "anti inflammatories",
                "antiinflammatory", "co codamol", "codeine", "pain relief", "tablets for pain"),
            answers = mapOf(
                1 to "Paracetamol is usually the first choice. Avoid anti-inflammatories such as ibuprofen, " +
                    "naproxen or high-dose aspirin while you're on a blood thinner unless your doctor says otherwise " +
                    "- together they raise bleeding risk, and some clinicians prefer to avoid them while a tendon " +
                    "heals. Check with your pharmacist or GP and follow the leaflets with your medicines."),
            listToPhase = 3),
        Faq("blood_thinner", "How long do I take the blood thinner for?", SYMPTOMS,
            keywords = listOf("blood thinner", "blood thinners", "anticoagulant", "anticoagulants", "injection",
                "injections", "clexane", "enoxaparin", "dalteparin", "fragmin", "apixaban", "eliquis",
                "rivaroxaban", "xarelto", "tinzaparin"),
            answers = mapOf(
                1 to "Usually while your leg is immobilised in the boot or cast - often around 6-10 weeks - but it " +
                    "varies, so follow your prescriber. Don't stop early or skip doses without medical advice. Set " +
                    "the course end date under Medications so the reminders stop on time. Missed a dose? Check the " +
                    "leaflet or ask a pharmacist - never double up."),
            link = FaqLink.MEDICATIONS, listToPhase = 3),
        Faq("swelling", "Is my swelling normal?", SYMPTOMS,
            keywords = listOf("swelling", "swollen", "swell", "swells", "puffy", "fluid"),
            answers = mapOf(
                1 to "Some swelling and bruising down into the foot is normal, and it's worse when the leg hangs " +
                    "down. Elevate above heart level for 20-30 minutes a few times a day and keep the toes moving. " +
                    "But swelling that won't settle with elevation - especially with calf pain, heat or redness - " +
                    "needs same-day advice in case it's a clot.",
                3 to "Ankle swelling by the end of the day is very common for 6-12 months and usually settles " +
                    "overnight with the leg up. New swelling with calf pain, heat or redness is different - treat " +
                    "it as a possible clot and get same-day advice."),
            link = FaqLink.RED_FLAGS),
        Faq("elevate", "How long should I keep my leg up?", SYMPTOMS,
            keywords = listOf("elevate", "elevating", "elevation", "leg up", "keep my leg up", "raise my leg"),
            answers = mapOf(
                1 to "Aim for 20-30 minutes a few times a day with the foot above heart level - lying down with " +
                    "the leg on pillows. More if swelling is bad. A footstool while sitting isn't high enough to " +
                    "drain swelling.",
                4 to "Only as needed now - if the ankle swells after a busy day, 20-30 minutes with the leg up in " +
                    "the evening helps."),
            listToPhase = 3),
        Faq("lump", "Is the lump on my tendon normal?", SYMPTOMS,
            keywords = listOf("lump", "lumpy", "thick", "thicker", "thickened", "thickening", "bump", "knot"),
            answers = mapOf(
                1 to "Yes - a thicker, firmer area where the tendon healed is normal scar tissue. It remodels over " +
                    "many months but often stays a little thicker than the other side for good. What's NOT normal " +
                    "is a new gap or dip in the tendon, or a sudden snap with loss of push-off - that needs urgent " +
                    "review."),
            listFromPhase = 3),
        Faq("exercise_pain", "How much pain is OK when I exercise?", SYMPTOMS,
            keywords = listOf("hurt", "hurts", "painful", "ache", "aches", "aching", "sore", "soreness",
                "how much pain", "pain during", "pain after"),
            answers = mapOf(
                1 to "A little discomfort is fine: up to about 3-4 out of 10 during exercise, settling within an " +
                    "hour and no worse the next morning. If pain is sharp, goes above that, or the tendon is " +
                    "stiffer and sorer the next day, drop back a step and tell your physio. A sudden sharp pain " +
                    "with a snap is different - check the red flags."),
            listFromPhase = 2),
        Faq("shoes", "What shoes should I wear?", ACTIVITY,
            keywords = listOf("shoe", "shoes", "trainer", "trainers", "footwear", "heel lift", "heel insert",
                "insole", "insoles", "barefoot", "flip flops", "slippers", "heels"),
            answers = mapOf(
                1 to "The boot is your shoe for now. On the other foot, wear a supportive trainer with a similar " +
                    "sole height to the boot (a thicker sole or a shoe 'leveller') so your hips stay level - it " +
                    "prevents knee, hip and back aches.",
                3 to "Supportive trainers with the heel raise your clinic advises - often in both shoes so you " +
                    "stay level. Avoid flat shoes, slippers and barefoot walking for now.",
                4 to "Normal supportive trainers. Your physio will wean you off any heel raise. Build up to barefoot " +
                    "and flatter shoes gradually - they ask more of the calf."),
            listFromPhase = 2),
        Faq("gym", "Can I go to the gym?", ACTIVITY,
            keywords = listOf("gym", "weights", "workout", "work out", "training", "fitness", "cardio"),
            answers = mapOf(
                1 to "Yes, for anything that doesn't load the ankle: seated upper-body work, core, and exercises for " +
                    "the good leg. Keep the boot on and never push through the injured foot. Stay fit has ideas.",
                3 to "Upper body, core and good-leg work, plus the stationary bike if your physio agrees. No jumping " +
                    "and no machines that load the calf yet.",
                4 to "Yes - leg press, squats, step-ups and calf work as prescribed. No jumping, running or heavy " +
                    "single-leg calf loading until your physio clears it.",
                5 to "Yes - build towards jumping and heavy calf loading as your physio progresses you."),
            link = FaqLink.STAY_FIT),
        Faq("rerupture_fear", "I'm scared of re-rupturing it", SAFETY,
            keywords = listOf("re rupture", "rerupture", "re rupturing", "rerupturing", "rupture again",
                "tear again", "snap again", "tear it again", "re tear", "retear"),
            contextKeywords = listOf("scared", "afraid", "fear", "worried", "worry", "anxious", "nervous", "confidence"),
            answers = mapOf(
                1 to "That fear is completely normal - almost everyone feels a jolt at every twinge. Re-rupture is " +
                    "uncommon when you follow the plan: the boot, the gradual steps and your physio's pace are " +
                    "exactly what protect you. Ordinary healing sensations (stiffness, a thicker tendon, a mild " +
                    "ache after a harder day) are not warning signs. A sudden snap, loss of push-off or a new gap " +
                    "in the tendon is - that needs your clinic today."),
            link = FaqLink.WELLBEING),
        Faq("fall", "I've tripped or fallen - what should I do?", SAFETY,
            keywords = listOf("fell", "fall", "fallen", "falling", "tripped", "trip", "slipped", "slip", "stumbled",
                "twisted", "went over"),
            answers = mapOf(
                1 to "If you felt a snap or pop, can't push off, or can feel a new gap in the tendon, treat it as a " +
                    "possible re-rupture: boot on, weight off, and contact your fracture clinic today (A&E if it's " +
                    "closed). If you hit your head while on a blood thinner, go to A&E today even if you feel fine. " +
                    "Otherwise rest, elevate, and mention it to your physio."),
            link = FaqLink.RED_FLAGS)
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
        // Default to the OPED VACOped (ROM dial, degrees); the user can switch to
        // an Aircast walker (wedges) or a cast journey in Settings.
        supportDevice = DeviceRegistry.VACOPED,
        supportedDeviceIds = listOf("vacoped", "aircast_wedges", "cast"),
        defaultDeviceId = "vacoped",
        phases = phases,
        milestones = milestones,
        redFlags = redFlags,
        movementChecks = listOf(
            MovementCheckSpec("Walk without the boot", 3,
                "Not before ~week 8-10, physio-confirmed", "Physio-guided weaning only - indoors first",
                keywords = listOf("walk", "walking", "stop wearing", "come out of the boot", "out of the boot",
                    "wean", "weaning", "without the boot", "without my boot", "without a boot"),
                contextKeywords = listOf("boot off", "take off the boot", "take the boot off", "take my boot off",
                    "remove the boot", "remove my boot")),
            MovementCheckSpec("Pull foot up past neutral / calf stretch", 4,
                "Not before week 12 - tendon over-lengthening risk",
                "Only if your physio prescribes it - many people never need to stretch the calf",
                keywords = listOf("stretch", "stretching", "stretches", "dorsiflex", "dorsiflexion", "past neutral",
                    "pull my foot up", "pull the foot up", "foot up"),
                question = "Can I stretch my calf?"),
            MovementCheckSpec("Drive a car", 4,
                "Not in the boot on the right leg - you can't brake safely. Most people are ready once out of the " +
                    "boot and able to do an emergency stop, often around the time the boot comes off. With the left " +
                    "leg injured and an automatic car you may manage sooner - check with your clinic and insurer.",
                "Only once you can brake hard and comfortably - the DVLA puts the responsibility on you to stay in " +
                    "full control. Check with your insurer before restarting (many won't cover driving in a boot) " +
                    "and confirm timing with your physio.",
                keywords = listOf("drive", "driving", "car", "drove"),
                question = "Can I drive yet?"),
            MovementCheckSpec("Standing heel raises", 4,
                "Phase 4 work - too early now",
                "Two legs first, then up on two and down on one, then single-leg - as your physio progresses you",
                keywords = listOf("heel raises", "calf raise", "calf raises", "tiptoe", "tiptoes", "tip toe",
                    "on my toes", "do a heel raise"),
                question = "Can I do heel raises?"),
            MovementCheckSpec("Swim or pool walk", 3,
                "Not while the boot is on full-time - keep fit on land for now (see Stay fit)",
                "Once your physio approves - pool walking first, no pushing off the wall with the injured foot",
                keywords = listOf("swim", "swimming", "pool", "aqua")),
            MovementCheckSpec("Ride a stationary bike", 3,
                "Wait until you're out of the boot",
                "Low resistance, pedalling through the heel or midfoot, once your physio approves",
                keywords = listOf("bike", "biking", "cycle", "cycling", "exercise bike", "spin", "spinning")),
            MovementCheckSpec("Run / jump", 5,
                "Phase 5 work after strength benchmarks", "Graded walk-jog programme once physio clears it",
                keywords = listOf("run", "running", "jog", "jogging", "jump", "jumping", "hop", "hopping", "sprint"),
                question = "Can I run or jump?"),
            MovementCheckSpec("Get back to {sport}", 5,
                "The end goal - but not yet",
                "Drills first; competitive play typically 9-12 months with sign-off",
                keywords = listOf("{sport}", "sport", "sports", "court", "match", "matches"),
                contextKeywords = listOf("play", "playing"),
                question = "Can I get back to {sport}?")
        ),
        faqs = faqs,
        exercisePainRule = "How it should feel: mild discomfort up to about 3-4/10 is fine if it settles within an " +
            "hour and isn't worse the next morning. Sharp pain, or a stiffer, sorer tendon the next day, means drop " +
            "back a step and tell your physio.",
        selfTests = selfTests,
        returnToSport = returnToSport,
        supportedSportIds = listOf("padel", "tennis", "football", "running", "hiking", "cycling", "swimming", "gym"),
        defaultSportId = "padel",
        mindset = mindset,
        reassurance = reassurance,
        expectations = expectations,
        fitness = fitness,
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
 * Neutral first-run seed. Personal facts (injury date, side, goal, medications,
 * appointments) are NOT assumed - they start blank/today and are captured in
 * onboarding, so a fresh install is correct for any user. Clinical scaffolding
 * that genuinely generalises (boot plan, phases, care tasks) still comes from
 * the selected protocol, anchored to the user's own injury date.
 */
object Defaults {
    fun profile(): Profile {
        val p = ProtocolRegistry.default
        return Profile(
            protocolId = p.id,
            name = "",
            injuryDate = LocalDate.now(),
            side = Side.LEFT,
            pathway = Pathway.CONSERVATIVE_NON_SURGICAL,
            injuryDescription = "",
            goal = "",
            appointments = emptyList(),
            wedgePlan = p.supportDevice?.plan ?: WedgePlan(0, 1, 7),
            currentWedges = p.supportDevice?.plan?.initialWedges ?: 0,
            weightBearing = WeightBearing.AS_TOLERATED,
            physioConfirmedPhase = 1,
            phaseStartOverrides = emptyMap(),
            onboardingComplete = false,
            disclaimerAcknowledged = false,
            sportId = p.defaultSportId ?: "",
            deviceId = p.defaultDeviceId ?: ""
        )
    }

    // Medications are personal/prescribed: none are assumed. Onboarding offers the
    // protocol's typical medication (e.g. an anticoagulant) as an explicit opt-in.
    fun medications(): List<Medication> = emptyList()
    fun tasks(): List<RehabTask> = ProtocolRegistry.default.prefillTasks
}
