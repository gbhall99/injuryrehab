package com.recoverwell.core

import com.recoverwell.core.model.Medication
import com.recoverwell.core.model.Profile
import com.recoverwell.core.model.RehabTask
import com.recoverwell.core.protocol.Defaults
import com.recoverwell.core.protocol.ProtocolRegistry
import java.time.LocalDate

/**
 * Deterministic test seed. Production [Defaults] is now neutral (today's date,
 * blank goal, no medications) so first-run is correct for any user; the engine
 * tests still need a fully-specified case, so they use this fixture - which
 * reproduces the original worked example from the protocol's own prefill fields.
 */
object Fixtures {
    private val proto = ProtocolRegistry.default

    fun profile(): Profile = Defaults.profile().copy(
        injuryDate = LocalDate.of(2026, 6, 2),
        injuryDescription = proto.prefillDescription,
        goal = proto.prefillGoal,
        appointments = proto.prefillAppointments
    )

    fun medications(): List<Medication> = proto.prefillMedications
    fun tasks(): List<RehabTask> = proto.prefillTasks
}
