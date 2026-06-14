package com.recoverwell.core

import com.recoverwell.core.logic.ScheduleEngine
import com.recoverwell.core.protocol.Defaults
import com.recoverwell.core.protocol.DeviceKind
import com.recoverwell.core.protocol.DeviceRegistry
import com.recoverwell.core.protocol.ProtocolRegistry
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/** The selectable support-device framework: VACOped, Aircast wedges, cast. */
class DeviceTest {

    private val injury = LocalDate.of(2026, 6, 2)

    @Test
    fun defaultDeviceIsTheVacoped() {
        val device = ProtocolRegistry.forProfile(Defaults.profile()).supportDevice
        assertEquals("vacoped", device?.id)
        assertEquals(DeviceKind.BOOT_DIAL, device?.kind)
        assertEquals("°", device?.unitSymbol)
        assertTrue(device!!.setupNotes.isNotEmpty())
    }

    @Test
    fun switchingToAircastChangesUnitsAndPlan() {
        val p = Defaults.profile().copy(deviceId = "aircast_wedges")
        val device = ProtocolRegistry.forProfile(p).supportDevice
        assertEquals("Aircast walker", device?.name)
        assertEquals(DeviceKind.BOOT_WEDGES, device?.kind)
        assertEquals("", device?.unitSymbol) // counted wedges
        // a counted device formats without a symbol
        assertEquals("3 wedges", device?.format(3))
    }

    @Test
    fun castHasNoSelfAdjustSchedule() {
        // settings resets the plan to the device's when switching; mirror that here
        val p = Defaults.profile().copy(
            injuryDate = injury, deviceId = "cast", wedgePlan = DeviceRegistry.CAST.plan)
        // no scheduled boot changes anywhere in the first months
        for (w in 0..12) {
            assertTrue("week $w should have no cast change",
                ScheduleEngine.wedgeChangesOn(p, injury.plusWeeks(w.toLong())).isEmpty())
        }
    }

    @Test
    fun deviceIdSurvivesBackupRoundTrip() {
        val p = Defaults.profile().copy(deviceId = "aircast_wedges")
        val state = com.recoverwell.core.export.AppState(
            p, Defaults.medications(), Defaults.tasks(), emptyMap(), emptyList(), emptyList())
        val decoded = com.recoverwell.core.export.BackupCodec.decode(
            com.recoverwell.core.export.BackupCodec.encode(state))
        assertEquals("aircast_wedges", decoded.profile.deviceId)
    }
}
