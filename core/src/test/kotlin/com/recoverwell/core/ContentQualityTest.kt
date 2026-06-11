package com.recoverwell.core

import com.recoverwell.core.export.AppState
import com.recoverwell.core.export.BackupCodec
import com.recoverwell.core.protocol.ProtocolContent
import org.junit.Assert.*
import org.junit.Test

/**
 * Guards the clinical and editorial constraints of the app content:
 * conservative-pathway-only language and correct "padel" spelling.
 */
class ContentQualityTest {

    /** Every user-visible string in the protocol content, concatenated. */
    private fun allContent(): String {
        val sb = StringBuilder()
        sb.append(ProtocolContent.PROTOCOL_NAME).append('\n')
        sb.append(ProtocolContent.DISCLAIMER).append('\n')
        sb.append(ProtocolContent.PLACEHOLDER_NOTE).append('\n')
        for (p in ProtocolContent.phases) {
            sb.append(p.title).append('\n').append(p.subtitle).append('\n')
            (p.entryCriteria + p.goals + p.precautions + p.allowed + p.notAllowed).forEach { sb.append(it).append('\n') }
            for (e in p.exercises) {
                sb.append(e.name).append('\n').append(e.whyItMatters).append('\n').append(e.precaution).append('\n')
                e.cues.forEach { sb.append(it).append('\n') }
            }
        }
        ProtocolContent.milestones.forEach { sb.append(it.title).append('\n').append(it.detail).append('\n') }
        ProtocolContent.redFlags.forEach { rf ->
            sb.append(rf.title).append('\n').append(rf.action).append('\n')
            rf.symptoms.forEach { sb.append(it).append('\n') }
        }
        val state = AppState(
            ProtocolContent.defaultProfile(), ProtocolContent.defaultMedications(),
            ProtocolContent.defaultTasks(), emptyMap(), emptyList(), emptyList()
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
        assertTrue(ProtocolContent.PLACEHOLDER_NOTE.contains("confirm with your physio"))
        // Every phase's entry criteria mention clinician involvement.
        for (p in ProtocolContent.phases) {
            val text = (p.entryCriteria + p.precautions).joinToString(" ").lowercase()
            assertTrue(
                "Phase ${p.number} must reference physio/clinic confirmation",
                text.contains("physio") || text.contains("clinic")
            )
        }
    }

    @Test
    fun redFlagsCoverDvtPeRerupture() {
        val ids = ProtocolContent.redFlags.map { it.id }
        assertTrue(ids.containsAll(listOf("dvt", "pe", "rerupture", "bleeding")))
        val dvt = ProtocolContent.redFlags.first { it.id == "dvt" }
        assertTrue(dvt.symptoms.any { it.lowercase().contains("calf") })
        assertTrue(dvt.symptoms.any { it.lowercase().contains("hot") || it.lowercase().contains("heat") })
    }

    @Test
    fun defaultsMatchPersonalData() {
        val p = ProtocolContent.defaultProfile()
        assertEquals("2026-06-02", p.injuryDate.toString())
        assertEquals("LEFT", p.side.name)
        assertEquals("CONSERVATIVE_NON_SURGICAL", p.pathway.name)
        assertEquals("2026-06-07", p.appointments.first().date.toString())
        assertTrue(p.appointments.first().completed)
        assertTrue(p.goal.contains("padel"))

        val med = ProtocolContent.defaultMedications().single()
        assertEquals("2.5 mg", med.dose)
        assertEquals(2, med.times.size)
        assertEquals(listOf("08:00", "20:00"), med.times.map { it.toString() })
    }

    @Test
    fun everyExerciseFullySpecified() {
        for (p in ProtocolContent.phases) {
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
        assertEquals(5, ProtocolContent.phases.size)
        assertEquals(listOf(1, 2, 3, 4, 5), ProtocolContent.phases.map { it.number })
    }
}
