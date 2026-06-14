package com.recoverwell.core

import com.recoverwell.core.export.AppState
import com.recoverwell.core.export.BackupCodec
import com.recoverwell.core.protocol.AchillesConservative
import com.recoverwell.core.protocol.Defaults
import com.recoverwell.core.protocol.ProtocolRegistry
import com.recoverwell.core.protocol.RehabFramework
import org.junit.Assert.*
import org.junit.Test

/**
 * Guards the clinical and editorial constraints of the app content:
 * conservative-pathway-only language and correct "padel" spelling.
 */
class ContentQualityTest {

    private val achilles = AchillesConservative.protocol

    /** Every user-visible string in the protocol content, concatenated. */
    private fun allContent(): String {
        val sb = StringBuilder()
        sb.append(achilles.injuryName).append('\n').append(achilles.variantName).append('\n')
        sb.append(achilles.protocolName).append('\n')
        sb.append(RehabFramework.DISCLAIMER).append('\n')
        sb.append(achilles.placeholderNote).append('\n')
        achilles.movementChecks.forEach {
            sb.append(it.movement).append('\n').append(it.noteWhenLocked).append('\n')
                .append(it.noteWhenUnlocked).append('\n')
        }
        for (p in achilles.phases) {
            sb.append(p.title).append('\n').append(p.subtitle).append('\n')
            (p.entryCriteria + p.goals + p.precautions + p.allowed + p.notAllowed).forEach { sb.append(it).append('\n') }
            for (e in p.exercises) {
                sb.append(e.name).append('\n').append(e.whyItMatters).append('\n').append(e.precaution).append('\n')
                e.cues.forEach { sb.append(it).append('\n') }
            }
        }
        achilles.milestones.forEach { sb.append(it.title).append('\n').append(it.detail).append('\n') }
        achilles.selfTests.forEach { t ->
            sb.append(t.name).append('\n').append(t.precaution).append('\n')
            t.howTo.forEach { sb.append(it).append('\n') }
        }
        (achilles.returnToSport + com.recoverwell.core.protocol.SportRegistry.all.flatMap { it.tailRungs })
            .forEach { r ->
                sb.append(r.title).append('\n').append(r.summary).append('\n')
                r.guidance.forEach { sb.append(it).append('\n') }
            }
        com.recoverwell.core.protocol.SportRegistry.all.forEach {
            sb.append(it.name).append('\n').append(it.returnPhrase).append('\n').append(it.demands).append('\n')
        }
        achilles.mindset.forEach { m ->
            m.normalToFeel.forEach { sb.append(it).append('\n') }
            sb.append(m.encouragement).append('\n')
        }
        achilles.expectations.forEach { e ->
            sb.append(e.title).append('\n').append(e.summary).append('\n').append(e.reassure).append('\n')
            e.likely.forEach { sb.append(it).append('\n') }
        }
        achilles.fitness.forEach { sb.append(it.name).append('\n').append(it.detail).append('\n') }
        com.recoverwell.core.protocol.DeviceRegistry.all.forEach { d ->
            sb.append(d.name).append('\n').append(d.operation).append('\n')
            d.setupNotes.forEach { sb.append(it).append('\n') }
        }
        achilles.redFlags.forEach { rf ->
            sb.append(rf.title).append('\n').append(rf.action).append('\n')
            rf.symptoms.forEach { sb.append(it).append('\n') }
        }
        val state = AppState(
            Fixtures.profile(), Fixtures.medications(), Fixtures.tasks(),
            emptyMap(), emptyList(), emptyList()
        )
        sb.append(BackupCodec.encode(state))
        return sb.toString()
    }

    @Test
    fun padelSpelledCorrectlyEverywhere() {
        val content = allContent().lowercase()
        assertFalse("The sport is padel, never 'paddle'", content.contains("paddle"))
        assertTrue(content.contains("padel"))
    }

    @Test
    fun noSurgicalProtocolContent() {
        val content = allContent().lowercase()
        for (term in listOf("incision", "suture", "stitch", "post-op", "postoperative", "wound healing after surgery", "repair site")) {
            assertFalse("Surgical term found: $term", content.contains(term))
        }
        assertTrue(content.contains("conservative"))
        assertTrue(content.contains("non-surgical"))
    }

    @Test
    fun timelinesMarkedAsPhysioConfirmable() {
        assertTrue(achilles.placeholderNote.contains("confirm with your physio"))
        // Every phase's entry criteria mention clinician involvement.
        for (p in achilles.phases) {
            val text = (p.entryCriteria + p.precautions).joinToString(" ").lowercase()
            assertTrue(
                "Phase ${p.number} must reference physio/clinic confirmation",
                text.contains("physio") || text.contains("clinic")
            )
        }
    }

    @Test
    fun redFlagsCoverDvtPeRerupture() {
        val ids = achilles.redFlags.map { it.id }
        assertTrue(ids.containsAll(listOf("dvt", "pe", "rerupture", "bleeding")))
        val dvt = achilles.redFlags.first { it.id == "dvt" }
        assertTrue(dvt.symptoms.any { it.lowercase().contains("calf") })
        assertTrue(dvt.symptoms.any { it.lowercase().contains("hot") || it.lowercase().contains("heat") })
    }

    @Test
    fun defaultsMatchPersonalData() {
        val p = Fixtures.profile()
        assertEquals("2026-06-02", p.injuryDate.toString())
        assertEquals("LEFT", p.side.name)
        assertEquals("CONSERVATIVE_NON_SURGICAL", p.pathway.name)
        assertEquals("2026-06-07", p.appointments.first().date.toString())
        assertTrue(p.appointments.first().completed)
        assertTrue(p.goal.contains("padel"))

        val med = Fixtures.medications().single()
        assertEquals("2.5 mg", med.dose)
        assertEquals(2, med.times.size)
        assertEquals(listOf("08:00", "20:00"), med.times.map { it.toString() })
    }

    @Test
    fun everyExerciseFullySpecified() {
        for (protocol in ProtocolRegistry.all) for (p in protocol.phases) {
            assertTrue("Phase ${p.number} needs exercises", p.exercises.isNotEmpty())
            for (e in p.exercises) {
                assertTrue(e.cues.size >= 2)
                assertTrue(e.whyItMatters.length > 30)
                assertTrue(e.precaution.isNotBlank())
                assertTrue(e.sets > 0 && e.reps > 0 && e.sessionsPerDay > 0)
                assertTrue(e.demoId.isNotBlank())
                assertEquals(p.number, e.phase)
            }
        }
    }

    @Test
    fun fivePhasesCoveringProtocol() {
        assertEquals(5, achilles.phases.size)
        assertEquals(listOf(1, 2, 3, 4, 5), achilles.phases.map { it.number })
    }

    @Test
    fun everyRegistryProtocolIsWellFormed() {
        assertTrue(ProtocolRegistry.all.isNotEmpty())
        for (protocol in ProtocolRegistry.all) {
            assertTrue(protocol.id.isNotBlank())
            assertTrue(protocol.phases.isNotEmpty())
            assertTrue(protocol.milestones.isNotEmpty())
            assertTrue(protocol.redFlags.isNotEmpty())
            assertTrue(protocol.movementChecks.isNotEmpty())
            // phase windows contiguous from week 0, last open-ended
            val phases = protocol.phases.sortedBy { it.number }
            assertEquals(0, phases.first().startWeek)
            for (i in 0 until phases.size - 1) {
                assertEquals(phases[i].endWeek, phases[i + 1].startWeek)
            }
            assertEquals(null, phases.last().endWeek)
            // every phase carries the generalized framework fields
            phases.forEach { assertTrue(it.tissueState.isNotBlank()) }
            // ids unique across the registry
        }
        assertEquals(ProtocolRegistry.all.size, ProtocolRegistry.all.map { it.id }.toSet().size)
    }
}
