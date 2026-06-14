package com.recoverwell.core.protocol

/**
 * Substitutes the chosen sport into protocol content. Protocol strings use
 * {sport} (lower-case, mid-sentence) and {Sport} (as a label) placeholders;
 * this resolves them to the user's target sport so the same Achilles content
 * reads as "Return to running" or "Play tennis" without per-sport copies.
 *
 * Resolution is idempotent on token-free strings, so it is applied broadly.
 */
object SportText {

    fun resolve(text: String, sportName: String): String =
        text.replace("{Sport}", sportName).replace("{sport}", sportName.lowercase())

    private fun List<String>.resolved(name: String) = map { resolve(it, name) }

    fun resolveProtocol(p: InjuryProtocol, sportName: String): InjuryProtocol {
        val n = sportName
        return p.copy(
            placeholderNote = resolve(p.placeholderNote, n),
            phases = p.phases.map { ph ->
                ph.copy(
                    title = resolve(ph.title, n),
                    subtitle = resolve(ph.subtitle, n),
                    tissueState = resolve(ph.tissueState, n),
                    deviceUsage = ph.deviceUsage?.let { resolve(it, n) },
                    entryCriteria = ph.entryCriteria.resolved(n),
                    goals = ph.goals.resolved(n),
                    precautions = ph.precautions.resolved(n),
                    allowed = ph.allowed.resolved(n),
                    notAllowed = ph.notAllowed.resolved(n),
                    exercises = ph.exercises.map { e ->
                        e.copy(
                            name = resolve(e.name, n),
                            cues = e.cues.resolved(n),
                            whyItMatters = resolve(e.whyItMatters, n),
                            precaution = resolve(e.precaution, n),
                            videoQuery = resolve(e.videoQuery, n)
                        )
                    }
                )
            },
            milestones = p.milestones.map { it.copy(title = resolve(it.title, n), detail = resolve(it.detail, n)) },
            movementChecks = p.movementChecks.map {
                it.copy(
                    movement = resolve(it.movement, n),
                    noteWhenLocked = resolve(it.noteWhenLocked, n),
                    noteWhenUnlocked = resolve(it.noteWhenUnlocked, n)
                )
            },
            selfTests = p.selfTests.map {
                it.copy(name = resolve(it.name, n), howTo = it.howTo.resolved(n), precaution = resolve(it.precaution, n))
            },
            returnToSport = p.returnToSport.map {
                it.copy(title = resolve(it.title, n), summary = resolve(it.summary, n), guidance = it.guidance.resolved(n))
            },
            expectations = p.expectations.map {
                it.copy(title = resolve(it.title, n), summary = resolve(it.summary, n),
                    likely = it.likely.resolved(n), reassure = resolve(it.reassure, n))
            },
            mindset = p.mindset.map { it.copy(normalToFeel = it.normalToFeel.resolved(n), encouragement = resolve(it.encouragement, n)) },
            reassurance = p.reassurance?.let { r ->
                r.copy(
                    title = resolve(r.title, n),
                    body = resolve(r.body, n),
                    normalVsFlag = r.normalVsFlag.map { resolve(it.first, n) to resolve(it.second, n) }
                )
            },
            welcomeBlurb = resolve(p.welcomeBlurb, n),
            safetyBlurb = resolve(p.safetyBlurb, n),
            redFlagIntro = resolve(p.redFlagIntro, n)
        )
    }
}
