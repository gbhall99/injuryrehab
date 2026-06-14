package com.recoverwell.core

import com.recoverwell.core.logic.PhysioPrep
import com.recoverwell.core.model.SelfTestResult
import com.recoverwell.core.protocol.Defaults
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

class PhysioPrepTest {

    private val injury = LocalDate.of(2026, 1, 1)
    private val meds = Fixtures.medications()
    private val tasks = Fixtures.tasks()

    @Test
    fun packRaisesGateQuestionAndShowsNumbers() {
        // ~3 weeks in: phase 2 is date-eligible but not physio-confirmed -> a gate question
        val today = injury.plusDays(20)
        val profile = Fixtures.profile().copy(injuryDate = injury, physioConfirmedPhase = 1)
        val pack = PhysioPrep.build(profile, emptyList(), emptyList(), meds, tasks, emptyList(), emptySet(), today)
        assertTrue(pack.discussionPoints.any { it.contains("phase 2") })
        assertTrue(pack.summaryLines.any { it.startsWith("Week") })
        assertTrue(pack.summaryLines.any { it.contains("adherence") })
    }

    @Test
    fun packAsksForSignoffWhenSelfTestsArePassed() {
        val today = injury.plusWeeks(30)
        val profile = Fixtures.profile().copy(injuryDate = injury, physioConfirmedPhase = 5)
        // clear the whole strength stage so the jogging stage (needs sign-off) is current
        val results = listOf(
            SelfTestResult(UUID.randomUUID().toString(), "heel_rise_sym", today, 18.0, 20.0, true, ""),
            SelfTestResult(UUID.randomUUID().toString(), "balance_eo", today, 35.0, null, true, ""),
            SelfTestResult(UUID.randomUUID().toString(), "calf_girth_sym", today, 38.0, 40.0, true, ""),
            SelfTestResult(UUID.randomUUID().toString(), "walk_tol", today, 35.0, null, true, "")
        )
        val pack = PhysioPrep.build(profile, emptyList(), emptyList(), meds, tasks, results, emptySet(), today)
        assertTrue(pack.discussionPoints.any { it.contains("clear me") })
        // a logged self-test appears in the numbers
        assertTrue(pack.summaryLines.any { it.contains("symmetry") })
    }
}
