package com.recoverwell.core.protocol

import com.recoverwell.core.model.WedgePlan

/**
 * The boots and casts an injury's recovery can run on. The support device is
 * data, so the same Achilles protocol scales across an OPED VACOped boot (ROM
 * dial, degrees), an Aircast walker (heel wedges), or a rigid cast - and the
 * schedule, digital twin and copy all follow the chosen device.
 *
 * Adding a device is a data entry here plus listing its id in the relevant
 * injury protocol's supportedDeviceIds.
 */
object DeviceRegistry {

    /**
     * OPED VACOped: a vacuum-cushion boot whose ROM dial sets the ankle angle
     * in degrees (no heel wedges). Start locked in equinus, step the dial down
     * to neutral, then move to a controlled range as the tendon heals.
     */
    val VACOPED = SupportDevice(
        id = "vacoped",
        name = "VACOped boot",
        kind = DeviceKind.BOOT_DIAL,
        unitName = "degree", unitNamePlural = "degrees", unitSymbol = "°",
        reductionVerb = "lower the ROM dial",
        maxValue = 40,
        // dial stepped from 30 deg equinus to neutral, 5 deg a week from week 3
        plan = WedgePlan(initialWedges = 30, removalStartWeek = 3, removalIntervalDays = 7, stepSize = 5),
        operation = "A ROM dial sets the ankle angle in degrees - there are no separate heel wedges. " +
            "It starts locked in equinus and steps down to neutral, then unlocks a controlled range of motion.",
        setupNotes = listOf(
            "Set the equinus angle on the ROM dial (often 30° to start) - this replaces stacked heel wedges.",
            "Worn day and night at first; only change the dial when your clinic tells you to.",
            "Re-pump the vacuum cushion if it loosens - it should feel firmly supportive, not painful.",
            "Keep the rocker sole on for walking; some clinics remove it for sleeping - follow your clinic's advice.",
            "Daily skin check under the cushion; numbness, rubbing or colour change in the toes means loosen it and contact your clinic.",
            "Later your clinic switches the hinge from a fixed angle to a controlled range (e.g. free movement within 0-30°)."
        )
    )

    /** Aircast/walker boot whose equinus is set by stacked heel wedges, removed one at a time. */
    val AIRCAST = SupportDevice(
        id = "aircast_wedges",
        name = "Aircast walker",
        kind = DeviceKind.BOOT_WEDGES,
        unitName = "wedge", unitNamePlural = "wedges", unitSymbol = "",
        reductionVerb = "remove a wedge",
        maxValue = 6,
        plan = WedgePlan(initialWedges = 5, removalStartWeek = 3, removalIntervalDays = 7, stepSize = 1),
        operation = "Heel wedges stacked inside the boot set the equinus angle. You remove one wedge at a " +
            "time on your clinic's schedule until the foot is flat (neutral).",
        setupNotes = listOf(
            "Stack the starting number of heel wedges your clinic set (often ~5).",
            "Remove one wedge at a time only on the agreed schedule - never more to 'speed things up'.",
            "Worn day and night at first; rocker sole on for walking.",
            "Daily skin check; loosen the straps if the toes tingle or change colour."
        )
    )

    /** Rigid below-knee cast in fixed equinus; only a clinic changes it. */
    val CAST = SupportDevice(
        id = "cast",
        name = "Below-knee cast",
        kind = DeviceKind.CAST,
        unitName = "degree", unitNamePlural = "degrees", unitSymbol = "°",
        reductionVerb = "have the cast re-set",
        maxValue = 40,
        // no self-adjust: clinic re-casts at visits, so no scheduled reductions
        plan = WedgePlan(initialWedges = 30, removalStartWeek = 999, removalIntervalDays = 28, stepSize = 5),
        operation = "A rigid cast holds the ankle in equinus. You can't adjust it yourself - the angle is " +
            "stepped toward neutral at clinic visits, where the cast is changed.",
        setupNotes = listOf(
            "The cast is set in equinus by your clinic; there is nothing to adjust at home.",
            "Angle changes happen at clinic appointments, where the cast is re-applied closer to neutral.",
            "Keep it completely dry; never push anything down inside it to scratch.",
            "Watch the toes for colour, warmth and feeling - numbness, severe swelling or pain needs urgent advice."
        )
    )

    val all: List<SupportDevice> = listOf(VACOPED, AIRCAST, CAST)

    fun byId(id: String): SupportDevice? = all.firstOrNull { it.id == id }
}
