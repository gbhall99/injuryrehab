package com.recoverwell.core.logic

import com.recoverwell.core.model.Profile
import com.recoverwell.core.protocol.Faq
import com.recoverwell.core.protocol.FaqLink
import com.recoverwell.core.protocol.InjuryProtocol
import com.recoverwell.core.protocol.MovementCheckSpec
import com.recoverwell.core.protocol.ProtocolRegistry
import java.time.LocalDate

/**
 * "Recovery coach": a fully offline, deterministic question-answerer. It maps
 * a free-text question (or a suggested one) onto the active protocol's own data
 * - red flags, movement checks, everyday FAQs, phase info and the support
 * device - and never invents anything. Safe because every answer is the
 * protocol's own content.
 *
 * Matching is whole-word (so "speed" never reads as "PE" and "cardio" never as
 * "car"), safety-first (a described red-flag symptom always wins), and prefers
 * the topic the question is actually about: strong topic words beat weak
 * context words ("take the boot off to sleep" is about sleeping), and among
 * strong matches the one mentioned first wins ("drive to work" is about
 * driving).
 */
object Ask {

    enum class Action {
        NONE, OPEN_RED_FLAGS, OPEN_PHASE_GUIDE, OPEN_MEDICATIONS, OPEN_WELLBEING, OPEN_STAY_FIT,
        OPEN_EXERCISES, OPEN_WHAT_TO_EXPECT
    }

    /**
     * [dial] is a number the answer card should offer to call ("999", "111"),
     * or [DIAL_CLINIC] for the user's own clinic.
     */
    data class Answer(
        val title: String,
        val body: String,
        val action: Action = Action.NONE,
        val dial: String? = null
    )

    const val DIAL_CLINIC = "clinic"

    /** A labelled group of starter questions, so the coach reads as structured
     *  topics rather than one open-ended box. */
    data class Topic(val title: String, val icon: String, val questions: List<String>)

    /**
     * Suggested questions grouped into topics, tailored to the current phase
     * (bathing and stairs early on; gym and running later). Every question
     * resolves back to its own deterministic answer (and seeds the AI chat
     * when that is enabled).
     */
    fun topics(profile: Profile, today: LocalDate = LocalDate.now()): List<Topic> {
        val protocol = ProtocolRegistry.forProfile(profile)
        val phase = PhaseEngine.currentPhase(profile, today).number
        val checks = protocol.movementChecks
        fun faqsFor(topic: String) = protocol.faqs
            .filter { it.topic == topic && phase in it.listFromPhase..it.listToPhase }
            .map { it.question }
        fun check(match: (MovementCheckSpec) -> Boolean) = checks.firstOrNull(match)?.questionText()

        val out = ArrayList<Topic>()
        out.add(Topic("Where I'm at", "ic_today", listOf("What can I do right now?", "What's next?")))

        val everyday = ArrayList<String>()
        check { it.keywords.contains("drive") }?.let { everyday.add(it) }
        if (phase <= 3) check { it.keywords.contains("walk") }?.let { everyday.add(it) }
        everyday.addAll(faqsFor("Everyday life"))
        if (everyday.isNotEmpty()) out.add(Topic("Everyday life", "ic_leg", everyday.distinct()))

        val activity = ArrayList<String>()
        if (phase >= 3) check { it.keywords.contains("heel raises") }?.let { activity.add(it) }
        check { it.keywords.contains("stretch") }?.let { activity.add(it) }
        if (phase in 2..4) {
            check { it.keywords.contains("swim") }?.let { activity.add(it) }
            check { it.keywords.contains("bike") }?.let { activity.add(it) }
        }
        if (phase >= 4) check { it.keywords.contains("run") }?.let { activity.add(it) }
        activity.addAll(faqsFor("Exercise & activity"))
        check { it.keywords.contains("sport") }?.let { activity.add(it) }
        if (activity.isNotEmpty()) out.add(Topic("Exercise & activity", "ic_exercises", activity.distinct()))

        val symptoms = faqsFor("Pain, swelling & medicines")
        if (symptoms.isNotEmpty()) out.add(Topic("Pain, swelling & medicines", "ic_pulse", symptoms))

        out.add(Topic("Safety", "ic_alert",
            listOf("What are the red flags?", "I'm worried about a clot") + faqsFor("Safety")))
        return out
    }

    // ---- matching ------------------------------------------------------------

    /** Lower-case words only, apostrophes dropped ("what's" -> "whats"). */
    private fun normalize(raw: String): List<String> =
        raw.lowercase().replace("'", "").replace("’", "")
            .split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }

    /** Token index of the first whole-word occurrence of [phrase] in [words], or -1. */
    private fun indexOf(words: List<String>, phrase: String): Int {
        val p = normalize(phrase)
        if (p.isEmpty() || p.size > words.size) return -1
        for (i in 0..words.size - p.size) {
            var ok = true
            for (j in p.indices) if (words[i + j] != p[j]) { ok = false; break }
            if (ok) return i
        }
        return -1
    }

    /** Earliest match of any phrase: (position, phrase length in words), or null. */
    private fun firstHit(words: List<String>, phrases: List<String>): Pair<Int, Int>? =
        phrases.mapNotNull { ph -> indexOf(words, ph).takeIf { it >= 0 }?.let { it to normalize(ph).size } }
            .minWithOrNull(compareBy<Pair<Int, Int>> { it.first }.thenByDescending { it.second })

    private fun any(words: List<String>, phrases: List<String>) = phrases.any { indexOf(words, it) >= 0 }

    // Symptom vocabulary, checked before anything else. Most specific first.
    private val emergencyWords = listOf("chest pain", "chest pains", "tight chest", "chest tightness",
        "pain in my chest", "breathless", "breathlessness", "short of breath",
        "shortness of breath", "cant breathe", "cannot breathe", "struggling to breathe", "coughing blood",
        "coughing up blood", "cough up blood", "embolism", "pe", "collapsed", "collapse", "fainted", "faint")
    private val clotWords = listOf("dvt", "clot", "clots", "blood clot", "calf pain", "pain in my calf",
        "calf is hot", "hot calf", "calf feels hot", "warm calf", "calf is warm", "calf is red", "red calf",
        "calf is swollen", "swollen calf", "calf is tender", "tender calf")
    private val ruptureWords = listOf("snap", "snapped", "a pop", "heard a pop", "felt a pop", "popping",
        "gap", "dip in the tendon", "cant push off", "cannot push off", "lost push off")
    private val bleedWords = listOf("bleeding", "bleed", "bleeds", "blood in", "black stool", "black stools",
        "black poo", "hit my head", "banged my head", "bumped my head", "head injury", "knocked my head")
    private val skinWords = listOf("numb", "numbness", "tingling", "pins and needles", "pressure sore",
        "blister", "blisters", "broken skin", "toes are blue", "toes are white", "toes are cold")
    private val redFlagWords = listOf("red flag", "red flags", "emergency", "999", "111", "danger", "warning signs",
        "warning sign", "symptoms", "symptom", "when to get help", "urgent")

    private val currentWords = listOf("right now", "what can i do", "what am i allowed", "allowed to do",
        "at the moment", "currently", "this phase", "this stage", "can i do now")
    private val currentContext = listOf("allowed", "today", "now")
    private val nextWords = listOf("whats next", "what is next", "next phase", "next stage", "next steps",
        "next step", "move on", "progress to", "advance", "when does phase")
    private val nextContext = listOf("next", "progress", "phase")
    private val deviceWords = listOf("heel angle", "boot angle", "angle", "dial", "rom dial", "wedge", "wedges",
        "boot setting", "degrees", "degree", "equinus")
    private val deviceContext = listOf("boot", "cast")

    /** One candidate reading of the question. [order] breaks exact ties. */
    private class Candidate(
        val order: Int,
        val primary: List<String>,
        val context: List<String>,
        val answer: () -> Answer
    )

    fun answer(rawQuery: String, profile: Profile, today: LocalDate): Answer {
        val words = normalize(rawQuery)
        val protocol = ProtocolRegistry.forProfile(profile)
        val phase = PhaseEngine.currentPhase(profile, today)

        // 1. safety first: a described symptom always wins
        redFlagAnswer(words, protocol)?.let { return it }

        // 2. everything else competes on how clearly the question is about it
        val candidates = ArrayList<Candidate>()
        var order = 0
        for (faq in protocol.faqs) {
            // safety FAQs (a fall, re-rupture fears) outrank everyday ones on ties
            val o = if (faq.topic == "Safety") -100 + order else order
            candidates.add(Candidate(o, faq.keywords, faq.contextKeywords) { faqAnswer(faq, phase.number) })
            order++
        }
        val checks = Capability.movementChecks(profile, today)
        protocol.movementChecks.forEachIndexed { i, spec ->
            val check = checks[i]
            candidates.add(Candidate(order++, spec.keywords, spec.contextKeywords) {
                val verdict = if (check.allowed) "Yes - allowed in your current phase." else "Not yet."
                Answer(spec.questionText(),
                    "$verdict ${check.note.trim().removeSuffix(".")}.\n\nYou're in phase ${phase.number} " +
                        "(${phase.title}). Always confirm progressions with your physio.",
                    Action.OPEN_PHASE_GUIDE)
            })
        }
        protocol.supportDevice?.let { dev ->
            candidates.add(Candidate(order++, deviceWords + dev.unitName + dev.unitNamePlural, deviceContext) {
                deviceAnswer(profile, today, protocol)
            })
        }
        candidates.add(Candidate(order++, nextWords, nextContext) { nextAnswer(profile, today) })
        candidates.add(Candidate(order++, currentWords, currentContext) { currentAnswer(profile, today) })

        val best = pick(words, candidates) { it.primary } ?: pick(words, candidates) { it.context }
        return best?.answer?.invoke() ?: fallback()
    }

    /** Earliest mention wins; then the longer (more specific) phrase; then [Candidate.order]. */
    private fun pick(words: List<String>, cs: List<Candidate>, phrases: (Candidate) -> List<String>): Candidate? =
        cs.mapNotNull { c -> firstHit(words, phrases(c))?.let { c to it } }
            .minWithOrNull(compareBy<Pair<Candidate, Pair<Int, Int>>> { it.second.first }
                .thenByDescending { it.second.second }
                .thenBy { it.first.order })
            ?.first

    private fun redFlagAnswer(words: List<String>, protocol: InjuryProtocol): Answer? = when {
        any(words, emergencyWords) -> Answer("Call 999 now",
            "Sudden breathlessness, chest pain, coughing up blood or collapsing can mean a blood clot has " +
                "reached the lungs. This is an emergency - call 999 now. Don't drive yourself.",
            Action.OPEN_RED_FLAGS, dial = "999")
        any(words, ruptureWords) -> Answer("Possible re-rupture - act today",
            "A snap or pop, a sudden loss of push-off or a new gap in the tendon can mean it has re-torn. " +
                "Put the boot back on at your last setting (or keep the foot pointed down), keep weight off the " +
                "leg and contact your fracture clinic today - A&E if it's closed.",
            Action.OPEN_RED_FLAGS, dial = DIAL_CLINIC)
        any(words, bleedWords) -> Answer("Bleeding or a head injury on a blood thinner",
            "Bleeding that won't stop, blood in your urine or black stools needs urgent advice - call 111, or " +
                "999 if it's severe. Any knock to the head while on a blood thinner needs same-day assessment at " +
                "A&E, even if you feel fine.",
            Action.OPEN_RED_FLAGS, dial = "111")
        any(words, clotWords) -> Answer("Possible blood clot - get advice today",
            "New calf pain or tenderness, a calf that's hot, red or swollen, or swelling that won't settle with " +
                "elevation can be a DVT - even on clot-prevention medication. Don't wait to see if it settles: call " +
                "111 or your GP for same-day advice. Chest pain or breathlessness means 999.",
            Action.OPEN_RED_FLAGS, dial = "111")
        any(words, skinWords) -> Answer("Boot or skin problem",
            "Numbness, tingling or colour change in the toes, or sores under the boot, mean it may be too tight " +
                "or rubbing. Loosen the straps, check the padding and contact your clinic if it doesn't settle " +
                "quickly.",
            Action.OPEN_RED_FLAGS, dial = DIAL_CLINIC)
        any(words, redFlagWords) -> Answer("Red flags",
            protocol.redFlagIntro + " Tap below to see every warning sign and exactly what to do.",
            Action.OPEN_RED_FLAGS)
        else -> null
    }

    private fun faqAnswer(faq: Faq, phase: Int): Answer = Answer(faq.question, faq.answerFor(phase),
        when (faq.link) {
            FaqLink.RED_FLAGS -> Action.OPEN_RED_FLAGS
            FaqLink.PHASE_GUIDE -> Action.OPEN_PHASE_GUIDE
            FaqLink.MEDICATIONS -> Action.OPEN_MEDICATIONS
            FaqLink.WELLBEING -> Action.OPEN_WELLBEING
            FaqLink.STAY_FIT -> Action.OPEN_STAY_FIT
            FaqLink.EXERCISES -> Action.OPEN_EXERCISES
            FaqLink.WHAT_TO_EXPECT -> Action.OPEN_WHAT_TO_EXPECT
            FaqLink.NONE -> Action.NONE
        })

    private fun currentAnswer(profile: Profile, today: LocalDate): Answer {
        val phase = PhaseEngine.currentPhase(profile, today)
        val snap = Capability.snapshot(profile, today)
        return Answer("Right now - phase ${phase.number}",
            "You're in \"${phase.title}\". OK in this phase:\n• " +
                snap.allowed.take(4).joinToString("\n• ") +
                "\n\nNot yet:\n• " + snap.notAllowed.take(3).joinToString("\n• "),
            Action.OPEN_PHASE_GUIDE)
    }

    private fun nextAnswer(profile: Profile, today: LocalDate): Answer {
        val phase = PhaseEngine.currentPhase(profile, today)
        val gate = PhaseEngine.nextPhaseGate(profile, today)
        return when {
            gate.nextPhase == null -> Answer("What's next", "You're in the final phase: ${phase.title}. " +
                "Keep progressing your return-to-sport work with your physio.", Action.OPEN_PHASE_GUIDE)
            gate.readyToConfirm -> Answer("What's next", "Phase ${gate.nextPhase.number} " +
                "(\"${gate.nextPhase.title}\") is due by date (${gate.startDate?.let { Dates.friendly(it, today) }}). " +
                "Ask your physio whether you " +
                "can start - only they can confirm it.", Action.OPEN_PHASE_GUIDE)
            else -> Answer("What's next", "Next is phase ${gate.nextPhase.number} " +
                "(\"${gate.nextPhase.title}\"), typically from ${gate.startDate?.let { Dates.friendly(it, today) }} - about " +
                "${gate.daysUntilEligible} days away. Your physio may adjust this.", Action.OPEN_PHASE_GUIDE)
        }
    }

    private fun deviceAnswer(profile: Profile, today: LocalDate, protocol: InjuryProtocol): Answer {
        val dev = protocol.supportDevice!!
        if (!profile.usesDeviceOn(today)) {
            return Answer("Your ${dev.name.lowercase()}",
                "You recorded being out of the ${dev.name.lowercase()} since " +
                    "${profile.bootWeanedDate?.let { Dates.friendly(it, today) }}, so there's " +
                    "nothing left to adjust. If your physio puts you back in it, update that under Injury & goal.")
        }
        val expected = profile.wedgePlan.expectedWedges(profile.injuryDate, today, profile.wedgeDateOverrides)
        val next = profile.wedgePlan.removalSchedule(profile.injuryDate, profile.wedgeDateOverrides)
            .filter { it.first.isAfter(today) }.minByOrNull { it.first }
        return Answer("Your ${dev.name.lowercase()}",
            "It's set to ${dev.format(profile.currentWedges)}; the plan expects ${dev.format(expected)} around " +
                "now." + (next?.let { " Next change: ${dev.reductionVerb} to ${dev.format(it.second)} on " +
                    "${Dates.friendly(it.first, today)}." }
                ?: "") + " Only change it when your clinic has agreed the step.")
    }

    private fun fallback() = Answer("I can help with your recovery",
        "Ask me what you can do yet (driving, walking, sleeping, showering, stairs, the gym, sport), how long " +
            "things take, painkillers and swelling, what's next, your boot setting, or the red flags. Or pick " +
            "one of the questions below.")
}
