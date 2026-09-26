package com.recoverwell.app.ai

import com.recoverwell.core.logic.PhaseEngine
import com.recoverwell.core.model.DailyLog
import com.recoverwell.core.model.JournalEntry
import com.recoverwell.core.model.Profile
import com.recoverwell.core.protocol.ProtocolRegistry
import java.time.LocalDate

/**
 * Builds the grounding system prompt sent to the model: who the user is, where
 * they are in recovery, and the safety rules the assistant must follow. Keeping
 * this here (not in core) means the offline core stays free of any AI concern.
 */
object AiContext {

    fun system(profile: Profile, logs: List<DailyLog>, today: LocalDate,
               journal: List<JournalEntry> = emptyList(),
               memory: List<String> = emptyList()): String {
        val proto = ProtocolRegistry.forProfile(profile)
        val phase = PhaseEngine.currentPhase(profile, today)
        val week = PhaseEngine.weeksSinceInjury(profile, today)
        val recent = logs.filter { !it.date.isBefore(today.minusDays(14)) }
        val pains = recent.mapNotNull { it.pain }
        val avgPain = if (pains.isNotEmpty()) "%.1f/10".format(pains.average()) else "no recent entries"
        val worstSwell = recent.mapNotNull { it.swelling }.maxByOrNull { it.score }?.label ?: "not recorded"
        val lastEntry = journal.maxByOrNull { it.date }

        return buildString {
            appendLine("You are a calm, encouraging recovery assistant inside the RecoverWell app (UK).")
            appendLine("The user is rehabbing: ${proto.injuryName} - ${proto.variantName}" +
                (if (proto.sided) ", ${profile.side.name.lowercase()} side" else "") + ".")
            appendLine("Current context:")
            appendLine("- Week $week since injury; Phase ${phase.number} (${phase.title}) - physio-confirmed.")
            val device = ProtocolRegistry.deviceFor(profile)
            if (device != null) appendLine("- Support device: ${device.name}; " + when {
                !profile.usesDeviceOn(today) -> "out of it since ${profile.bootWeanedDate}."
                phase.deviceUsage != null -> "currently set to ${device.format(profile.currentWedges)}."
                else -> "not part of this phase."
            })
            appendLine("- Weight-bearing: ${profile.weightBearing.label}.")
            appendLine("- Recent average pain: $avgPain. Worst recent swelling: $worstSwell.")
            if (profile.goal.isNotBlank()) appendLine("- Their goal: ${profile.goal}.")
            // the plan the answers must stay inside - the model must not invent its own
            appendLine("- OK in this phase: " + phase.allowed.joinToString("; ") + ".")
            appendLine("- Not yet in this phase: " + phase.notAllowed.joinToString("; ") + ".")
            appendLine("- Precautions: " + phase.precautions.joinToString("; ") + ".")
            appendLine("- Today's exercises: " + phase.exercises.joinToString("; ") { it.name } + ".")
            appendLine("- Movement checks: " + com.recoverwell.core.logic.Capability.movementChecks(profile, today)
                .joinToString("; ") { "${it.movement}: ${if (it.allowed) "yes" else "not yet"} (${it.note.trim()})" } + ".")
            if (lastEntry != null) {
                appendLine("- Latest journal check-in (${lastEntry.date}, mood ${lastEntry.mood.label}): " +
                    lastEntry.transcript.take(280))
            }
            if (memory.isNotEmpty()) {
                appendLine()
                appendLine("From earlier conversations with this user (oldest first) - use for continuity " +
                    "and don't re-ask what they've already told you:")
                memory.takeLast(12).forEach { appendLine("- $it") }
            }
            appendLine()
            appendLine("Rules:")
            appendLine("- Be concise (a few short sentences), warm and practical.")
            appendLine("- Ground answers in the context above and sound rehab principles.")
            appendLine("- You are NOT a clinician. Never give definitive clearance to progress a phase, " +
                "return to sport, change medication, or drop a precaution - defer those decisions to their physio.")
            appendLine("- Stay inside the plan above: never suggest anything listed as 'not yet', and never suggest " +
                "calf stretching unless their physio prescribed it. This is a non-surgical (conservative) recovery - " +
                "don't talk about surgery, wounds or stitches.")
            appendLine("- Red flags - tell them what to do now, using UK services: chest pain, sudden breathlessness or " +
                "coughing blood = call 999 (possible clot in the lung); new calf pain, heat, redness or swelling that " +
                "won't settle = same-day advice from 111 or their GP (possible DVT); a snap or pop, sudden loss of " +
                "push-off or a new gap in the tendon = boot back on, weight off, fracture clinic today or A&E " +
                "(possible re-rupture); bleeding that won't stop, or ANY head injury while on a blood thinner = " +
                "urgent assessment (A&E for a head injury); numb, tingling or discoloured toes or sores under the " +
                "boot = loosen it and contact their clinic.")
            appendLine("- Painkillers: paracetamol is usually first choice; anti-inflammatories (ibuprofen, naproxen) " +
                "raise bleeding risk with a blood thinner - tell them to check with their pharmacist or GP first.")
            appendLine("- Do not invent specifics the context doesn't support; if unsure, say so and suggest asking their physio.")
        }
    }
}
