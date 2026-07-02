package com.recoverwell.core.logic

import com.recoverwell.core.model.Profile
import com.recoverwell.core.protocol.ProtocolRegistry
import java.time.LocalDate

/**
 * "Recovery coach": a fully offline, deterministic question-answerer. It maps
 * a free-text question (or a suggested one) onto the active protocol's own data
 * - movement checks, red flags, phase info and the support device - and never
 * invents anything. Safe because every answer is the protocol's own content.
 */
object Ask {

    enum class Action { NONE, OPEN_RED_FLAGS, OPEN_PHASE_GUIDE }

    data class Answer(val title: String, val body: String, val action: Action = Action.NONE)

    /** Suggested questions to seed the screen, drawn from the active protocol. */
    fun suggestions(profile: Profile): List<String> {
        val checks = ProtocolRegistry.forProfile(profile).movementChecks
        val out = ArrayList<String>()
        out.add("What can I do right now?")
        checks.firstOrNull { it.movement.contains("drive", true) }?.let { out.add("Can I drive yet?") }
        checks.lastOrNull()?.let { out.add("Can I play ${it.movement.lowercase().removePrefix("play ")}?") }
        out.add("What are the red flags?")
        out.add("What's next?")
        return out.distinct()
    }

    /** A labelled group of starter questions, so the coach reads as structured
     *  topics rather than one open-ended box. */
    data class Topic(val title: String, val icon: String, val questions: List<String>)

    /**
     * Suggested questions grouped into a few topics. Every question resolves to
     * a deterministic offline answer (and seeds the AI chat when enabled), so the
     * coach offers clear starting points instead of a single blank prompt.
     */
    fun topics(profile: Profile): List<Topic> {
        val checks = ProtocolRegistry.forProfile(profile).movementChecks
        val moves = LinkedHashSet<String>()
        checks.firstOrNull { it.movement.contains("drive", true) }?.let { moves.add("Can I drive yet?") }
        checks.firstOrNull { it.movement.contains("walk", true) }?.let { moves.add("Can I walk without the boot?") }
        checks.firstOrNull { it.movement.contains("raise", true) }?.let { moves.add("Can I do heel raises?") }
        checks.firstOrNull { it.movement.contains("stretch", true) || it.movement.contains("neutral", true) }
            ?.let { moves.add("Can I stretch my ankle?") }
        checks.lastOrNull { it.movement.startsWith("Play", true) }
            ?.let { moves.add("Can I play ${it.movement.lowercase().removePrefix("play ")}?") }
        val out = ArrayList<Topic>()
        out.add(Topic("Where I'm at", "ic_today", listOf("What can I do right now?", "What's next?")))
        if (moves.isNotEmpty()) out.add(Topic("Everyday movement", "ic_leg", moves.toList()))
        out.add(Topic("Safety", "ic_alert", listOf("What are the red flags?", "I'm worried about a clot")))
        return out
    }

    private val redFlagWords = listOf("red flag", "clot", "dvt", "pe ", "embolism", "bleed",
        "emergency", "999", "111", "worried", "symptom", "danger", "warning")
    private val nextWords = listOf("next", "progress", "phase", "when can", "move on", "advance")
    private val currentWords = listOf("right now", "today", "currently", "what can i do", "allowed")

    /** Per-movement keyword synonyms so natural phrasing matches the checks. */
    private fun keywordsFor(movement: String): List<String> {
        val m = movement.lowercase()
        val base = m.split(Regex("[^a-z]+")).filter { it.length >= 4 }
        val extra = when {
            m.contains("drive") -> listOf("drive", "driving", "car")
            m.contains("run") || m.contains("jump") -> listOf("run", "running", "jog", "jogging", "jump", "hop")
            m.contains("stretch") || m.contains("neutral") -> listOf("stretch", "stretching", "calf", "dorsiflex")
            m.contains("heel raise") || m.contains("raises") -> listOf("heel raise", "calf raise", "raises")
            m.contains("walk") -> listOf("walk", "walking", "boot off", "without the boot")
            // the sport-return check is "Play {sport}" - match it for any sport
            m.startsWith("play ") -> listOf("sport", "play", "court", m.removePrefix("play ").trim())
            else -> emptyList()
        }
        return (base + extra).distinct()
    }

    fun answer(rawQuery: String, profile: Profile, today: LocalDate): Answer {
        val q = rawQuery.lowercase().trim()
        val protocol = ProtocolRegistry.forProfile(profile)
        val phase = PhaseEngine.currentPhase(profile, today)

        // red flags
        if (redFlagWords.any { q.contains(it.trim()) }) {
            return Answer("Red flags",
                protocol.redFlagIntro + " Tap below to see every warning sign and what to do.",
                Action.OPEN_RED_FLAGS)
        }

        // current capability
        if (currentWords.any { q.contains(it) }) {
            val snap = Capability.snapshot(profile, today)
            return Answer("Right now - phase ${phase.number}",
                "You're in \"${phase.title}\". OK in this phase:\n• " +
                    snap.allowed.take(4).joinToString("\n• ") +
                    "\n\nNot yet:\n• " + snap.notAllowed.take(3).joinToString("\n• "))
        }

        // movement checks ("can I ...")
        Capability.movementChecks(profile, today).forEach { check ->
            if (keywordsFor(check.movement).any { q.contains(it) }) {
                val verdict = if (check.allowed) "Yes - allowed in your current phase." else "Not yet."
                return Answer("Can I ${check.movement.lowercase()}?",
                    "$verdict ${check.note}.\n\nYou're in phase ${phase.number} (${phase.title}). " +
                        "Always confirm progressions with your physio.")
            }
        }

        // next phase
        if (nextWords.any { q.contains(it) }) {
            val gate = PhaseEngine.nextPhaseGate(profile, today)
            return if (gate.nextPhase == null) {
                Answer("What's next", "You're in the final phase: ${phase.title}. " +
                    "Keep progressing your return-to-sport work with your physio.", Action.OPEN_PHASE_GUIDE)
            } else if (gate.readyToConfirm) {
                Answer("What's next", "Phase ${gate.nextPhase.number} (\"${gate.nextPhase.title}\") is due by " +
                    "date (${gate.startDate}). Ask your physio whether you can start - only they can confirm it.",
                    Action.OPEN_PHASE_GUIDE)
            } else {
                Answer("What's next", "Next is phase ${gate.nextPhase.number} (\"${gate.nextPhase.title}\"), " +
                    "typically from ${gate.startDate} - about ${gate.daysUntilEligible} days away. " +
                    "Your physio may adjust this.", Action.OPEN_PHASE_GUIDE)
            }
        }

        // support device
        protocol.supportDevice?.let { dev ->
            if (q.contains(dev.unitName) || q.contains("boot") || q.contains("heel angle") || q.contains("wedge")) {
                val expected = profile.wedgePlan.expectedWedges(profile.injuryDate, today, profile.wedgeDateOverrides)
                return Answer("Your ${dev.name.lowercase()}",
                    "It's set to ${dev.format(profile.currentWedges)}; the plan expects " +
                        "${dev.format(expected)} around now. Only change it if your clinic has agreed.")
            }
        }

        return Answer("I can help with your recovery",
            "Ask me what you can do yet (driving, walking, sport), what phase you're in, " +
                "what's next, your boot setting, or the red flags. Try a suggestion above.")
    }
}
