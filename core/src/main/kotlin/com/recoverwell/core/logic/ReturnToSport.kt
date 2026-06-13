package com.recoverwell.core.logic

import com.recoverwell.core.model.Profile
import com.recoverwell.core.model.SelfTestResult
import com.recoverwell.core.protocol.InjuryProtocol
import com.recoverwell.core.protocol.ProtocolRegistry
import com.recoverwell.core.protocol.RtsRung
import com.recoverwell.core.protocol.SelfTest
import com.recoverwell.core.protocol.Sport
import com.recoverwell.core.protocol.SportRegistry
import java.time.LocalDate

/**
 * Turns logged self-tests into a criteria-based return-to-sport picture: which
 * rung you are on, what each test needs, and an overall readiness toward the
 * goal sport. Pure data in, pure data out - advisory, never a clearance.
 */
object ReturnToSport {

    enum class TestState { PASS, FAIL, UNTESTED }
    enum class RungState { CLEARED, CURRENT, LOCKED }

    data class TestStatus(
        val test: SelfTest,
        val latest: SelfTestResult?,
        val state: TestState
    )

    data class RungStatus(
        val rung: RtsRung,
        val state: RungState,
        val tests: List<TestStatus>,
        val physioSignedOff: Boolean
    ) {
        /** Every test passes (sign-off considered separately). */
        val testsMet: Boolean get() = tests.isNotEmpty() && tests.all { it.state == TestState.PASS }
        val cleared: Boolean get() = state == RungState.CLEARED
    }

    data class Progress(
        val rungs: List<RungStatus>,
        val currentRung: RtsRung?,
        /** First rung blocked only because its phase isn't active yet, if any. */
        val nextLockedByPhase: RtsRung?,
        val readinessPct: Int,
        /** The program is relevant once the earliest rung's phase is reached. */
        val available: Boolean,
        val startPhase: Int,
        /** Resolved target sport (null = generic), and its display labels. */
        val sport: Sport?,
        val returnPhrase: String
    )

    /** Resolve the user's target sport (their choice, else the protocol default). */
    fun resolveSport(profile: Profile, protocol: InjuryProtocol): Sport? {
        val id = profile.sportId.ifBlank { protocol.defaultSportId ?: "" }
        return if (id.isBlank()) null else SportRegistry.byId(id)
    }

    /** Foundation rungs the sport keeps, then the sport's tail - renumbered 1..N. */
    fun effectiveLadder(protocol: InjuryProtocol, sport: Sport?): List<RtsRung> {
        val foundation = protocol.returnToSport.sortedBy { it.order }
        val kept = if (sport == null) foundation
            else foundation.filter { it.id in sport.foundationRungIds }.ifEmpty { foundation }
        val tail = sport?.tailRungs.orEmpty().sortedBy { it.order }
        return (kept + tail).mapIndexed { i, r -> r.copy(order = i + 1) }
    }

    fun latestFor(testId: String, results: List<SelfTestResult>): SelfTestResult? =
        results.filter { it.testId == testId }.maxByOrNull { it.date }

    fun passes(test: SelfTest, r: SelfTestResult): Boolean {
        if (test.requirePainFree && !r.painFree) return false
        return if (test.symmetry) {
            val sym = r.symmetryPct ?: return false
            sym >= test.passThreshold
        } else {
            if (test.lowerIsBetter) r.injuredValue <= test.passThreshold
            else r.injuredValue >= test.passThreshold
        }
    }

    private fun testStatus(test: SelfTest, results: List<SelfTestResult>): TestStatus {
        val latest = latestFor(test.id, results)
        val state = when {
            latest == null -> TestState.UNTESTED
            passes(test, latest) -> TestState.PASS
            else -> TestState.FAIL
        }
        return TestStatus(test, latest, state)
    }

    fun progress(
        profile: Profile,
        results: List<SelfTestResult>,
        signoffs: Set<String>,
        today: LocalDate,
        protocol: InjuryProtocol = ProtocolRegistry.forProfile(profile)
    ): Progress {
        val sport = resolveSport(profile, protocol)
        val ladder = effectiveLadder(protocol, sport)
        val testsById = protocol.selfTests.associateBy { it.id }
        val currentPhase = PhaseEngine.currentPhase(profile, today).number
        val startPhase = ladder.minOfOrNull { it.phase } ?: Int.MAX_VALUE

        // First pass: per-rung test status + whether criteria (tests + sign-off) are met.
        data class Raw(val rung: RtsRung, val tests: List<TestStatus>, val signed: Boolean, val criteriaMet: Boolean)
        val raw = ladder.map { rung ->
            val tests = rung.testIds.mapNotNull { testsById[it] }.map { testStatus(it, results) }
            val signed = rung.id in signoffs
            val testsMet = tests.isNotEmpty() && tests.all { it.state == TestState.PASS }
            val criteriaMet = testsMet && (!rung.requiresPhysioSignoff || signed)
            Raw(rung, tests, signed, criteriaMet)
        }

        // The current rung is the first not-yet-cleared rung whose phase is active.
        val firstUnclearedReachable = raw.firstOrNull { !it.criteriaMet && it.rung.phase <= currentPhase }
        val nextLockedByPhase = raw.firstOrNull { !it.criteriaMet && it.rung.phase > currentPhase }?.rung

        val statuses = raw.map { r ->
            val state = when {
                r.criteriaMet -> RungState.CLEARED
                firstUnclearedReachable != null && r.rung.id == firstUnclearedReachable.rung.id -> RungState.CURRENT
                else -> RungState.LOCKED
            }
            RungStatus(r.rung, state, r.tests, r.signed)
        }

        // Readiness: each rung equal weight; cleared = full, current = fraction of tests passed.
        val readiness = if (ladder.isEmpty()) 0 else {
            val per = 1.0 / ladder.size
            var sum = 0.0
            for (s in statuses) {
                sum += when (s.state) {
                    RungState.CLEARED -> per
                    RungState.CURRENT -> per * (s.tests.count { it.state == TestState.PASS }.toDouble() /
                        s.tests.size.coerceAtLeast(1))
                    RungState.LOCKED -> 0.0
                }
            }
            (sum * 100).toInt().coerceIn(0, 100)
        }

        return Progress(
            rungs = statuses,
            currentRung = firstUnclearedReachable?.rung,
            nextLockedByPhase = nextLockedByPhase,
            readinessPct = readiness,
            available = currentPhase >= startPhase,
            startPhase = if (startPhase == Int.MAX_VALUE) 99 else startPhase,
            sport = sport,
            returnPhrase = sport?.returnPhrase ?: "Return to sport"
        )
    }
}
