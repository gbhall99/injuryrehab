package com.recoverwell.core

import com.recoverwell.core.protocol.Defaults
import com.recoverwell.core.protocol.ProtocolRegistry
import org.junit.Assert.*
import org.junit.Test

/** The {sport} token resolution: protocol copy speaks the user's chosen sport. */
class SportTextTest {

    @Test
    fun defaultResolvesToPadel() {
        val p = ProtocolRegistry.forProfile(Defaults.profile())
        assertTrue(p.movementChecks.any { it.movement == "Play padel" })
        assertTrue(p.milestones.any { it.title == "Return to padel" })
    }

    @Test
    fun chosenSportReshapesTheCopy() {
        val p = ProtocolRegistry.forProfile(Defaults.profile().copy(sportId = "running"))
        assertTrue(p.movementChecks.any { it.movement == "Play running" })
        assertTrue(p.milestones.any { it.title == "Return to running" })
        assertFalse(p.milestones.any { it.title.contains("padel", ignoreCase = true) })

        val phase5 = p.phase(5)
        val joined = (phase5.goals + phase5.allowed + phase5.notAllowed +
            phase5.exercises.map { it.name + " " + it.whyItMatters }).joinToString(" ").lowercase()
        assertFalse("phase 5 copy should not mention padel for a runner", joined.contains("padel"))
        assertTrue(joined.contains("running"))
    }

    @Test
    fun noSportTokenSurvivesResolutionForAnySupportedSport() {
        for (sportId in ProtocolRegistry.default.supportedSportIds) {
            val p = ProtocolRegistry.forProfile(Defaults.profile().copy(sportId = sportId))
            val all = buildString {
                p.phases.forEach { ph ->
                    append(ph.subtitle).append(' ')
                    (ph.goals + ph.allowed + ph.notAllowed).forEach { append(it).append(' ') }
                    ph.exercises.forEach { append(it.name).append(' ').append(it.whyItMatters).append(' ') }
                }
                p.milestones.forEach { append(it.title).append(' ').append(it.detail).append(' ') }
                p.movementChecks.forEach { append(it.movement).append(' ') }
                p.mindset.forEach { m -> m.normalToFeel.forEach { append(it).append(' ') } }
            }
            assertFalse("unresolved {sport} token for $sportId", all.contains("{sport}"))
            assertFalse("unresolved {Sport} token for $sportId", all.contains("{Sport}"))
        }
    }
}
