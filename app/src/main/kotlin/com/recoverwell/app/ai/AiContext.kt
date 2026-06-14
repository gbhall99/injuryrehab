package com.recoverwell.app.ai

import com.recoverwell.core.logic.PhaseEngine
import com.recoverwell.core.model.DailyLog
import com.recoverwell.core.model.Profile
import com.recoverwell.core.protocol.ProtocolRegistry
import java.time.LocalDate

/**
 * Builds the grounding system prompt sent to the model: who the user is, where
 * they are in recovery, and the safety rules the assistant must follow. Keeping
 * this here (not in core) means the offline core stays free of any AI concern.
 */
object AiContext {

    fun system(profile: Profile, logs: List<DailyLog>, today: LocalDate): String {
        val proto = ProtocolRegistry.forProfile(profile)
        val phase = PhaseEngine.currentPhase(profile, today)
        val week = PhaseEngine.weeksSinceInjury(profile, today)
        val recent = logs.filter { !it.date.isBefore(today.minusDays(14)) }
        val pains = recent.mapNotNull { it.pain }
        val avgPain = if (pains.isNotEmpty()) "%.1f/10".format(pains.average()) else "no recent entries"
        val worstSwell = recent.mapNotNull { it.swelling }.maxByOrNull { it.score }?.label ?: "not recorded"

        return buildString {
            appendLine("You are a calm, encouraging recovery assistant inside the RecoverWell app.")
            appendLine("The user is rehabbing: ${proto.injuryName} - ${proto.variantName}.")
            appendLine("Current context:")
            appendLine("- Week $week since injury; Phase ${phase.number} (${phase.title}).")
            appendLine("- Recent average pain: $avgPain. Worst recent swelling: $worstSwell.")
            if (profile.goal.isNotBlank()) appendLine("- Their goal: ${profile.goal}.")
            appendLine()
            appendLine("Rules:")
            appendLine("- Be concise (a few short sentences), warm and practical.")
            appendLine("- Ground answers in the context above and sound rehab principles.")
            appendLine("- You are NOT a clinician. Never give definitive clearance to progress a phase, " +
                "return to sport, change medication, or drop a precaution - defer those decisions to their physio.")
            appendLine("- If they describe red-flag symptoms (sudden calf pain/swelling/redness/warmth, " +
                "chest pain, breathlessness, fever, or a wound that looks infected), tell them to seek urgent medical advice now.")
            appendLine("- Do not invent specifics the context doesn't support; if unsure, say so and suggest asking their physio.")
        }
    }
}
