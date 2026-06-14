package com.recoverwell.draw

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

/**
 * Guards the demonstration figure's foot orientation: the figure faces +x, so
 * the foot/boot must point forward (positive x-component) - never backward.
 * This regressed once (boot drawn the wrong way around), so it is locked here.
 */
class DemoFootTest {

    private fun forwardX(shankAngle: Float, ankle: Float): Float =
        sin(Math.toRadians(DemoScene.footAngleOf(shankAngle, ankle).toDouble())).toFloat()

    @Test
    fun neutralStandingFootPointsForward() {
        assertTrue("standing neutral foot must point forward", forwardX(0f, 0f) > 0.5f)
    }

    @Test
    fun seatedFootPointsForward() {
        // seated: thigh 85, knee 85 -> shank vertical (shankAngle 0)
        assertTrue("seated foot must point forward", forwardX(0f, 0f) > 0.5f)
    }

    @Test
    fun footStaysForwardThroughPlantarAndDorsiflexion() {
        // a realistic ankle range must never flip the foot behind the figure
        for (ankle in -20..35 step 5) {
            assertTrue("ankle=$ankle foot must stay forward", forwardX(0f, ankle.toFloat()) > 0f)
        }
    }

    @Test
    fun walkingSwingFootPointsForward() {
        // mid-stride shank angles from the gait demo still point the foot ahead
        assertTrue(forwardX(20f, -4f) > 0f)
        assertTrue(forwardX(-14f, 16f) > 0f)
    }
}
