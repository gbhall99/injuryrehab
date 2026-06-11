package com.recoverwell.core

import com.recoverwell.core.export.AppState
import com.recoverwell.core.export.BackupCodec
import com.recoverwell.core.export.CsvExporter
import com.recoverwell.core.model.*
import com.recoverwell.core.protocol.ProtocolContent
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class BackupAndCsvTest {

    private fun sampleState(): AppState {
        val injury = LocalDate.of(2026, 6, 2)
        return AppState(
            profile = ProtocolContent.defaultProfile().copy(
                name = "G",
                physioConfirmedPhase = 2,
                phaseStartOverrides = mapOf(3 to injury.plusWeeks(9)),
                onboardingComplete = true,
                disclaimerAcknowledged = true
            ),
            medications = ProtocolContent.defaultMedications(),
            tasks = ProtocolContent.defaultTasks(),
            exerciseOverrides = mapOf(
                "p1_slr" to ExerciseOverride("p1_slr", 4, null, 3, null, true)
            ),
            dailyLogs = listOf(
                DailyLog(injury.plusDays(1), 6, Swelling.MODERATE, "n/a, in boot", true, 5,
                    WeightBearing.AS_TOLERATED, 3, 2, "Rough night, leg \"throbbing\", elevated"),
                DailyLog.empty(injury.plusDays(2))
            ),
            events = listOf(
                EventLog("e1", injury.plusDays(1), EventType.MEDICATION, "med_anticoagulant", "08:00", EventStatus.TAKEN, 485),
                EventLog("e2", injury.plusDays(1), EventType.TASK, "task_elevation", "10:00", EventStatus.DONE, 612)
            )
        )
    }

    @Test
    fun backupRoundTripIsLossless() {
        val state = sampleState()
        val decoded = BackupCodec.decode(BackupCodec.encode(state))
        assertEquals(state, decoded)
    }

    @Test
    fun backupRejectsUnsupportedVersion() {
        val bad = BackupCodec.encode(sampleState()).replaceFirst("\"version\":1", "\"version\":99")
        try {
            BackupCodec.decode(bad)
            fail("Expected rejection of unknown backup version")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("version"))
        }
    }

    @Test
    fun csvEscapesQuotesCommasNewlines() {
        val csv = CsvExporter.dailyLogsCsv(sampleState().dailyLogs)
        val lines = csv.trim().lines()
        assertEquals(3, lines.size) // header + 2 logs
        assertTrue(lines[0].startsWith("date,pain_0_10,swelling"))
        assertTrue(lines[1].contains("\"Rough night, leg \"\"throbbing\"\", elevated\""))
        assertTrue(lines[1].contains("\"n/a, in boot\""))
    }

    @Test
    fun eventsCsvFormatsTime() {
        val csv = CsvExporter.eventsCsv(sampleState().events)
        assertTrue(csv.contains("08:05"))
        assertTrue(csv.contains("MEDICATION"))
        assertTrue(csv.contains("TAKEN"))
    }
}
