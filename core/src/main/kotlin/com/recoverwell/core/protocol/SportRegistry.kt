package com.recoverwell.core.protocol

/**
 * A target sport for the return-to-sport program. The injury protocol owns the
 * shared foundation (calf strength, jogging, hopping); a Sport adds the demands
 * specific to getting back to *that* game on top - so the same Achilles rehab
 * scales from padel to running to cycling without touching the engine.
 *
 * Sports reference the injury's self-test pool by id, plus pick which
 * foundation rungs apply (a low-impact sport can skip the impact stages).
 */
data class Sport(
    val id: String,
    val name: String,
    /** Headline phrase, e.g. "Return to padel". */
    val returnPhrase: String,
    /** One line on what this sport asks of the healing tendon. */
    val demands: String,
    /** Which foundation rung ids (from InjuryProtocol.returnToSport) apply. */
    val foundationRungIds: List<String>,
    /** Sport-specific stages appended after the foundation. */
    val tailRungs: List<RtsRung>
)

/**
 * Every sport the framework knows. Adding a sport is a data entry here plus
 * listing its id in the relevant injury protocol's supportedSportIds. These
 * are lower-limb oriented (they reference hop/jog self-tests); an upper-limb
 * injury would define its own sports or its own test pool.
 */
object SportRegistry {

    // -- court sports (cutting + multidirectional) --------------------------

    private val PADEL = Sport(
        id = "padel", name = "Padel", returnPhrase = "Return to padel",
        demands = "Repeated sprints, sudden stops and lateral cuts to the glass - the exact loads that rupture tendons.",
        foundationRungIds = listOf("rts_strength", "rts_jog", "rts_run_hop"),
        tailRungs = listOf(
            RtsRung("rts_agility", 10, "Change of direction", "Add the cutting and lateral movement padel demands.",
                phase = 5, testIds = listOf("hop_sym", "hop_count"),
                guidance = listOf(
                    "Side shuffles, then diagonal cuts, building from 50% speed.",
                    "Sharp direction changes are exactly what ruptured the tendon - rehearse them progressively."),
                requiresPhysioSignoff = true),
            RtsRung("rts_padel", 11, "Return to padel", "Court movement, then rallies, then friendly matches, then competition.",
                phase = 5, testIds = listOf("hop_sym"),
                guidance = listOf(
                    "Stage it: shadow movement, cooperative rallies, friendly games, competition.",
                    "Each stage needs physio sign-off; full competitive padel is typically 9-12 months after injury."),
                requiresPhysioSignoff = true)
        )
    )

    private val TENNIS = Sport(
        id = "tennis", name = "Tennis", returnPhrase = "Return to tennis",
        demands = "Explosive court movement, serving and lunging - high, sudden tendon loads.",
        foundationRungIds = listOf("rts_strength", "rts_jog", "rts_run_hop"),
        tailRungs = listOf(
            RtsRung("rts_tennis_court", 10, "Court movement & serve", "Multidirectional movement and serving load.",
                phase = 5, testIds = listOf("hop_sym", "hop_count"),
                guidance = listOf(
                    "Shadow footwork and split-steps before live hitting.",
                    "Add serving volume gradually - it loads the calf hard."),
                requiresPhysioSignoff = true),
            RtsRung("rts_tennis_return", 11, "Return to tennis", "Rallies, then sets, then competitive play.",
                phase = 5, testIds = listOf("hop_sym"),
                guidance = listOf("Cooperative rallies first, then points, then matches - each physio-approved."),
                requiresPhysioSignoff = true)
        )
    )

    private val FOOTBALL = Sport(
        id = "football", name = "Football", returnPhrase = "Return to football",
        demands = "Sprinting, cutting, kicking and contact - the most demanding return of all.",
        foundationRungIds = listOf("rts_strength", "rts_jog", "rts_run_hop"),
        tailRungs = listOf(
            RtsRung("rts_fb_sprint", 10, "Sprinting & cutting", "Build top-speed running and sharp direction changes.",
                phase = 5, testIds = listOf("hop_sym", "hop_count"),
                guidance = listOf(
                    "Progress striders to sprints, and gentle cuts to full agility.",
                    "Fatigue ruins technique - quality over volume early."),
                requiresPhysioSignoff = true),
            RtsRung("rts_fb_ball", 11, "Ball work & contact", "Kicking volume and non-contact, then contact, training.",
                phase = 5, testIds = listOf("hop_sym"),
                guidance = listOf(
                    "Passing and shadow play before full-intensity drills.",
                    "Join non-contact team training before any contact."),
                requiresPhysioSignoff = true),
            RtsRung("rts_fb_return", 12, "Return to football", "Full training, then a substitute role, then full matches.",
                phase = 5, testIds = listOf("hop_sym"),
                guidance = listOf("Build match minutes gradually; first matches are often as a substitute."),
                requiresPhysioSignoff = true)
        )
    )

    // -- endurance / linear -------------------------------------------------

    private val RUNNING = Sport(
        id = "running", name = "Running", returnPhrase = "Return to running",
        demands = "Repetitive impact and calf endurance over distance.",
        foundationRungIds = listOf("rts_strength", "rts_jog", "rts_run_hop"),
        tailRungs = listOf(
            RtsRung("rts_run_build", 10, "Build your distance", "Grow continuous running time on flat ground.",
                phase = 5, testIds = listOf("run_long"),
                guidance = listOf(
                    "Increase weekly volume by no more than ~10%.",
                    "Keep most runs easy; add pace only once distance is comfortable."),
                requiresPhysioSignoff = true),
            RtsRung("rts_run_return", 11, "Return to running", "Hills, tempo and, if you race, a graded return to racing.",
                phase = 5, testIds = listOf("run_long"),
                guidance = listOf("Add hills and speed last; they load the tendon most."),
                requiresPhysioSignoff = true)
        )
    )

    private val HIKING = Sport(
        id = "hiking", name = "Hiking", returnPhrase = "Return to hiking",
        demands = "Long time on feet, inclines and uneven ground - endurance more than impact.",
        foundationRungIds = listOf("rts_strength"),
        tailRungs = listOf(
            RtsRung("rts_hike_terrain", 10, "Uneven ground & inclines", "Add hills, descents and uneven trails.",
                phase = 4, testIds = listOf("balance_eo", "walk_tol"),
                guidance = listOf(
                    "Start on gentle, even paths; add gradient and rough ground gradually.",
                    "Descending and ankle balance on uneven ground are the real challenge - and poles help."),
                requiresPhysioSignoff = false),
            RtsRung("rts_hike_return", 11, "Return to hiking", "Build up to your usual distance and pack weight.",
                phase = 4, testIds = listOf("heel_rise_sym"),
                guidance = listOf("Add distance first, then carry weight - one change at a time."),
                requiresPhysioSignoff = true)
        )
    )

    // -- low impact ---------------------------------------------------------

    private val CYCLING = Sport(
        id = "cycling", name = "Cycling", returnPhrase = "Return to cycling",
        demands = "Low-impact endurance; mostly ankle range, control and calf endurance.",
        foundationRungIds = listOf("rts_strength"),
        tailRungs = listOf(
            RtsRung("rts_cycle_return", 10, "Return to cycling", "Stationary, then flat outdoor riding, then hills and longer rides.",
                phase = 4, testIds = listOf("balance_eo"),
                guidance = listOf(
                    "Stationary bike first, then flat roads, then gradient and distance.",
                    "Clipless pedals load the calf more - reintroduce them last."),
                requiresPhysioSignoff = true)
        )
    )

    private val SWIMMING = Sport(
        id = "swimming", name = "Swimming", returnPhrase = "Return to swimming",
        demands = "Minimal impact; gentle ankle range and push-off through the water.",
        foundationRungIds = listOf("rts_strength"),
        tailRungs = listOf(
            RtsRung("rts_swim_return", 10, "Return to swimming", "Pool walking, then easy strokes, then push-off and kick.",
                phase = 4, testIds = listOf("balance_eo"),
                guidance = listOf(
                    "Wait until any wounds are fully healed and your physio approves the pool.",
                    "Reintroduce strong kicking and wall push-offs last."),
                requiresPhysioSignoff = true)
        )
    )

    // -- general ------------------------------------------------------------

    private val GYM = Sport(
        id = "gym", name = "Gym & fitness", returnPhrase = "Return to full training",
        demands = "Mixed strength and conditioning, including some impact work.",
        foundationRungIds = listOf("rts_strength", "rts_jog"),
        tailRungs = listOf(
            RtsRung("rts_gym_return", 10, "Return to full training", "Add jumping, plyometrics and heavy calf loading.",
                phase = 5, testIds = listOf("heel_rise_sym", "hop_count"),
                guidance = listOf(
                    "Reintroduce jumping and plyometrics gradually.",
                    "Heavy single-leg calf work is the goal - build load slowly."),
                requiresPhysioSignoff = true)
        )
    )

    val all: List<Sport> = listOf(
        PADEL, TENNIS, FOOTBALL, RUNNING, HIKING, CYCLING, SWIMMING, GYM
    )

    fun byId(id: String): Sport? = all.firstOrNull { it.id == id }
}
