package com.recoverwell.draw

import kotlin.math.cos
import kotlin.math.sin

/**
 * Exercise demonstration engine: a side-view figure driven by joint-angle
 * keyframes. Pure logic + Sketch drawing, so the same demonstrations run
 * inside the Android app and render to PNG in designlab for design review.
 */
data class Pose(
    val torso: Float = 0f,   // lean from vertical, + = forward
    val thighA: Float = 0f,  // injured-side leg; 0 = straight down, + = hip flexion
    val kneeA: Float = 0f,   // 0 = straight, + = flexion
    val ankleA: Float = 0f,  // + = plantarflexion (toes down), - = dorsiflexion
    val toesA: Float = 0f,   // + = toe curl
    val thighB: Float = 0f,
    val kneeB: Float = 0f,
    val ankleB: Float = 0f,
    val hipX: Float = 0f,    // offsets in leg-length units
    val hipY: Float = 0f,    // + = down (squat), - = up (raise/jump)
    val lying: Boolean = false,
    val arm: Float = 35f     // arm angle from vertical-down, + = forward
)

enum class Prop { NONE, BOOT, CRUTCHES, BAND, STEP, WALL, TOWEL, BIKE, CONES, RACQUET, CHAIR }

/**
 * Which camera the demo is drawn from. SIDE is the default sagittal figure.
 * The FRONT_* views draw a coronal (face-on) figure of someone side-lying, so
 * a genuinely sideways movement - abduction (FRONT_ABD) or a clamshell knee
 * opening (FRONT_CLAM) - actually reads as sideways instead of a leg lift. The
 * moving (top) leg's opening angle is driven by the pose's [Pose.thighA].
 */
enum class DemoView { SIDE, FRONT_ABD, FRONT_CLAM }

data class Demo(
    val props: Set<Prop>,
    val caption: String,
    /** keyframe + milliseconds to morph from the previous frame */
    val frames: List<Pair<Pose, Long>>,
    val view: DemoView = DemoView.SIDE
)

object DemoScene {

    fun pose(demo: Demo, elapsed: Long): Pose {
        val total = demo.frames.sumOf { it.second }
        if (total <= 0) return demo.frames.first().first
        var t = elapsed % total
        var prev = demo.frames.last().first
        for ((pose, dur) in demo.frames) {
            if (t < dur) {
                val f = (t.toFloat() / dur).coerceIn(0f, 1f)
                val eased = f * f * (3 - 2 * f)
                return lerp(prev, pose, eased)
            }
            t -= dur
            prev = pose
        }
        return prev
    }

    private fun lerp(a: Pose, b: Pose, f: Float) = Pose(
        torso = a.torso + (b.torso - a.torso) * f,
        thighA = a.thighA + (b.thighA - a.thighA) * f,
        kneeA = a.kneeA + (b.kneeA - a.kneeA) * f,
        ankleA = a.ankleA + (b.ankleA - a.ankleA) * f,
        toesA = a.toesA + (b.toesA - a.toesA) * f,
        thighB = a.thighB + (b.thighB - a.thighB) * f,
        kneeB = a.kneeB + (b.kneeB - a.kneeB) * f,
        ankleB = a.ankleB + (b.ankleB - a.ankleB) * f,
        hipX = a.hipX + (b.hipX - a.hipX) * f,
        hipY = a.hipY + (b.hipY - a.hipY) * f,
        lying = b.lying,
        arm = a.arm + (b.arm - a.arm) * f
    )

    private fun dir(deg: Float): Pair<Float, Float> {
        val r = Math.toRadians(deg.toDouble())
        return sin(r).toFloat() to cos(r).toFloat()
    }

    /**
     * Foot direction angle. The figure faces +x, so a neutral foot points
     * FORWARD: the shank rotated +90deg. Plantarflexion (ankle+) rotates the
     * toes down toward the shank line. Its x-component (sin) must stay positive
     * for realistic poses - the foot must never point behind the figure.
     */
    fun footAngleOf(shankAngle: Float, ankle: Float): Float = shankAngle + 90f - ankle

    // figure palette, theme-aware (read at render time)
    private val figureBase get() = if (Palette.dark) 0xFFBFE3CC.toInt() else 0xFF24433A.toInt()
    private val FIGURE get() = Palette.withAlpha(figureBase, 0xE6)
    private val FIGURE_BACK get() = Palette.withAlpha(figureBase, 0x52)
    private val ARM_FRONT get() = Palette.withAlpha(figureBase, 0xC2)
    private val PROP get() = if (Palette.dark) 0xFF77837B.toInt() else 0xFF9AA8A0.toInt()
    private val PROP_SOFT get() = Palette.withAlpha(PROP, 0x80)
    private val BAND get() = 0xFFE07A65.toInt()
    private val SHADOW get() = Palette.withAlpha(if (Palette.dark) 0xFF000000.toInt() else 0xFF1A1C1A.toInt(), 0x1C)

    /** 0..1 position within the demo's loop, for player-style progress bars. */
    fun cycleFraction(demoId: String, elapsed: Long): Float {
        val demo = DemoLibrary.demos[demoId] ?: return 0f
        val total = demo.frames.sumOf { it.second }
        if (total <= 0) return 0f
        return (elapsed % total).toFloat() / total
    }

    fun render(s: Sketch, demoId: String, elapsed: Long) {
        val demo = DemoLibrary.demos[demoId] ?: DemoLibrary.demos.getValue("ankle_pump")
        if (demo.view != DemoView.SIDE) { renderFront(s, demo, elapsed); return }
        val p = pose(demo, elapsed)

        val L = s.height * 0.20f
        val groundY = s.height * 0.82f
        val limbW = L * 0.30f

        // ground - or, for a lying figure, a clear mat/bed band at their back so
        // the movement reads against a surface (and nothing appears to sink below it)
        if (p.lying) {
            val cy = groundY - 1.05f * L
            s.roundRect(s.width * 0.05f, cy + 0.34f * L, s.width * 0.95f, cy + 0.72f * L, 12f,
                Palette.withAlpha(Palette.ON_SURFACE_VARIANT, 0x33))
        } else {
            s.stroke(PathSpec.line(s.width * 0.06f, groundY, s.width * 0.94f, groundY),
                Palette.OUTLINE, 3f)
        }

        s.save()
        if (p.lying) {
            s.rotate(-90f, s.width * 0.5f, groundY - L * 1.05f)
            s.translate(s.width * 0.10f, L * 0.35f)
        }

        // hip sits legs-plus-ankle-height above the ground so feet touch down
        val hipX = s.width * 0.42f + p.hipX * L
        val hipY = groundY - 2.12f * L + p.hipY * L

        // soft contact shadow (skip when lying: rotation would misplace it)
        if (!p.lying) {
            val sh = PathSpec.of {
                moveTo(hipX - L * 0.9f, groundY + 6f)
                cubicTo(hipX - L * 0.4f, groundY - 8f, hipX + L * 0.7f, groundY - 8f, hipX + L * 1.2f, groundY + 6f)
                close()
            }
            s.fill(sh, SHADOW)
        }

        // torso: pelvis -> chest (tapered) -> neck -> head
        val (tx, ty) = dir(p.torso)
        val chestX = hipX + tx * L * 0.90f
        val chestY = hipY - ty * L * 0.90f
        val neckX = hipX + tx * L * 1.14f
        val neckY = hipY - ty * L * 1.14f
        s.stroke(PathSpec.line(hipX, hipY, chestX, chestY), FIGURE, limbW * 1.32f)
        s.stroke(PathSpec.line(chestX, chestY, neckX, neckY), FIGURE, limbW * 0.95f)
        s.circle(neckX + tx * L * 0.30f, neckY - ty * L * 0.30f, L * 0.185f, FIGURE)
        val shX = chestX
        val shY = chestY

        // props behind figure
        if (Prop.WALL in demo.props) {
            s.stroke(PathSpec.line(s.width * 0.80f, groundY - 2.7f * L, s.width * 0.80f, groundY), PROP, 6f)
        }
        if (Prop.CHAIR in demo.props) {
            s.stroke(PathSpec.line(hipX - L * 0.6f, hipY + limbW * 0.55f, hipX + L * 0.38f, hipY + limbW * 0.55f), PROP, 7f)
            s.stroke(PathSpec.line(hipX - L * 0.48f, hipY + limbW * 0.55f, hipX - L * 0.48f, groundY), PROP, 7f)
            s.stroke(PathSpec.line(hipX + L * 0.26f, hipY + limbW * 0.55f, hipX + L * 0.26f, groundY), PROP, 7f)
            s.stroke(PathSpec.line(hipX - L * 0.6f, hipY + limbW * 0.55f, hipX - L * 0.6f, hipY - L * 0.75f), PROP, 7f)
        }
        if (Prop.STEP in demo.props) {
            s.roundRect(hipX + L * 0.32f, groundY - L * 0.52f, hipX + L * 1.55f, groundY, 6f, Palette.SURFACE_HIGH)
            s.roundRect(hipX + L * 0.32f, groundY - L * 0.52f, hipX + L * 1.55f, groundY - L * 0.40f, 6f, PROP_SOFT)
        }
        if (Prop.BIKE in demo.props) {
            s.circleStroke(hipX + L * 0.72f, groundY - L * 0.56f, L * 0.55f, PROP, 6f)
            s.circle(hipX + L * 0.72f, groundY - L * 0.56f, 6f, PROP)
        }
        if (Prop.CONES in demo.props) {
            for (i in 0..1) {
                val cx = s.width * (0.16f + 0.64f * i)
                s.fill(PathSpec.of {
                    moveTo(cx, groundY - L * 0.38f)
                    lineTo(cx - L * 0.17f, groundY)
                    lineTo(cx + L * 0.17f, groundY)
                    close()
                }, Palette.withAlpha(Palette.WEDGE, 0xAA))
            }
        }

        // arms: two segments with a relaxed elbow; back arm dimmed
        val armW = limbW * 0.60f
        fun drawArm(angle: Float, color: Int, w: Float): Pair<Float, Float> {
            val (uax, uay) = dir(angle)
            val elbowX = shX + uax * L * 0.50f
            val elbowY = shY + uay * L * 0.50f
            val (fax, fay) = dir(angle - 26f)
            val hx = elbowX + fax * L * 0.45f
            val hy = elbowY + fay * L * 0.45f
            s.stroke(PathSpec.line(shX, shY, elbowX, elbowY), color, w)
            s.stroke(PathSpec.line(elbowX, elbowY, hx, hy), color, w * 0.86f)
            return hx to hy
        }
        drawArm(p.arm - 18f, FIGURE_BACK, armW * 0.92f)
        val hand = drawArm(p.arm, ARM_FRONT, armW)
        val handX = hand.first
        val handY = hand.second
        if (Prop.WALL in demo.props) {
            // fingertips resting on the wall - balance support cue
            s.stroke(PathSpec.line(handX, handY, s.width * 0.80f, handY), FIGURE_BACK, armW * 0.7f)
        }
        if (Prop.RACQUET in demo.props) {
            val (ax, ay) = dir(p.arm - 26f)
            val rx = handX + ax * L * 0.42f
            val ry = handY + ay * L * 0.42f
            s.stroke(PathSpec.line(handX, handY, rx, ry), PROP, 5f)
            s.circleStroke(rx + ax * L * 0.24f, ry + ay * L * 0.24f, L * 0.26f, PROP, 5f)
        }
        if (Prop.CRUTCHES in demo.props) {
            s.stroke(PathSpec.line(shX + L * 0.22f, shY - L * 0.1f, shX + L * 0.40f, groundY), PROP, 6f)
            s.stroke(PathSpec.line(shX - L * 0.16f, shY - L * 0.1f, shX - L * 0.34f, groundY), PROP, 6f)
        }

        // legs: back leg first
        drawLeg(s, hipX, hipY, L, limbW * 0.92f, p.thighB, p.kneeB, p.ankleB, 0f, FIGURE_BACK, boot = false)
        val toe = drawLeg(s, hipX, hipY, L, limbW, p.thighA, p.kneeA, p.ankleA, p.toesA, FIGURE,
            boot = Prop.BOOT in demo.props)

        if (Prop.BAND in demo.props) {
            s.stroke(PathSpec.line(toe.first, toe.second, s.width * 0.92f, toe.second + L * 0.08f), BAND, 6f)
            s.circle(s.width * 0.92f, toe.second + L * 0.08f, 7f, BAND)
        }
        if (Prop.TOWEL in demo.props) {
            s.stroke(PathSpec.line(toe.first - L * 0.34f, groundY - 5f, toe.first + L * 0.85f, groundY - 5f),
                Palette.withAlpha(0xFF6B9BC3.toInt(), 0xCC), 8f)
        }

        s.restore()
    }

    /** A short thick dark stroke standing in for the boot along a leg segment. */
    private fun drawBootSeg(s: Sketch, x0: Float, y0: Float, x1: Float, y1: Float, w: Float) {
        s.stroke(PathSpec.line(x0, y0, x1, y1), Palette.withAlpha(Palette.BOOT_DARK, 0xE6), w * 1.85f)
    }

    /**
     * Front-facing (coronal) view of someone side-lying on a mat, head to the
     * left. Both legs point right, stacked; the moving (top) leg opens UPWARD by
     * [Pose.thighA] degrees, so abduction / a clamshell reads as a true sideways
     * opening. FRONT_CLAM bends the knees with the feet kept together (the
     * clamshell hinge); FRONT_ABD keeps the legs straight (hip abduction).
     */
    private fun renderFront(s: Sketch, demo: Demo, elapsed: Long) {
        val p = pose(demo, elapsed)
        val L = s.height * 0.21f
        val limbW = L * 0.30f
        val matY = s.height * 0.72f

        // mat/bed the person lies on
        s.roundRect(s.width * 0.05f, matY, s.width * 0.95f, matY + L * 0.18f, 12f,
            Palette.withAlpha(Palette.ON_SURFACE_VARIANT, 0x33))

        val bent = demo.view == DemoView.FRONT_CLAM
        val open = p.thighA
        val hipX = s.width * 0.46f
        val hipY = matY - limbW * 1.05f
        val headX = hipX - L * 1.20f

        // torso, head, and a resting supporting forearm
        s.stroke(PathSpec.line(hipX, hipY, headX + L * 0.20f, hipY), FIGURE, limbW * 1.28f)
        s.circle(headX, hipY - limbW * 0.05f, L * 0.17f, FIGURE)
        s.stroke(PathSpec.line(headX + L * 0.55f, hipY, headX + L * 0.48f, matY - limbW * 0.2f),
            FIGURE_BACK, limbW * 0.5f)

        // feet kept together for the clamshell, resting near the mat
        val feetX = hipX + L * 0.98f
        val feetY = matY - limbW * 0.4f

        fun leg(angleUp: Float, color: Int, boot: Boolean) {
            val (dx, dy) = dir(90f + angleUp)   // angleUp=0 -> right; grows -> up
            if (bent) {
                val kneeX = hipX + dx * L * 0.95f
                val kneeY = hipY + dy * L * 0.95f
                if (boot) drawBootSeg(s, kneeX, kneeY, feetX, feetY, limbW)
                s.stroke(PathSpec.line(hipX, hipY, kneeX, kneeY), color, limbW)
                s.stroke(PathSpec.line(kneeX, kneeY, feetX, feetY), color, limbW * 0.88f)
            } else {
                val kneeX = hipX + dx * L
                val kneeY = hipY + dy * L
                val footX = kneeX + dx * L
                val footY = kneeY + dy * L
                if (boot) drawBootSeg(s, kneeX, kneeY, footX, footY, limbW)
                s.stroke(PathSpec.line(hipX, hipY, kneeX, kneeY), color, limbW)
                s.stroke(PathSpec.line(kneeX, kneeY, footX, footY), color, limbW * 0.88f)
                // short foot at the ankle, pointing down toward the toes
                s.stroke(PathSpec.line(footX, footY, footX + dy * L * 0.22f, footY - dx * L * 0.22f),
                    color, limbW * 0.6f)
            }
        }

        // bottom (resting) leg first and dimmer, then the moving top leg
        leg(-5f, FIGURE_BACK, boot = false)
        leg(open, FIGURE, boot = Prop.BOOT in demo.props)
    }

    private fun drawLeg(
        s: Sketch, hipX: Float, hipY: Float, L: Float, w: Float,
        thigh: Float, knee: Float, ankle: Float, toes: Float,
        color: Int, boot: Boolean
    ): Pair<Float, Float> {
        val (txd, tyd) = dir(thigh)
        val kneeX = hipX + txd * L
        val kneeY = hipY + tyd * L
        val shankAngle = thigh - knee
        val (sxd, syd) = dir(shankAngle)
        val ankleX = kneeX + sxd * L
        val ankleY = kneeY + syd * L
        val footAngle = footAngleOf(shankAngle, ankle)
        val (fxd, fyd) = dir(footAngle)
        val footLen = L * 0.42f
        val midX = ankleX + fxd * footLen
        val midY = ankleY + fyd * footLen
        // toe curl bends the toes down/under (towards the sole)
        val toesAngle = footAngle - toes
        val (toxd, toyd) = dir(toesAngle)
        val toeX = midX + toxd * L * 0.18f
        val toeY = midY + toyd * L * 0.18f

        if (boot) {
            // Boot drawn as thick strokes that FOLLOW the shank and foot segments,
            // so it wraps correctly in any pose (seated, lying, standing) instead
            // of a fixed shape that deforms. The lighter leg strokes draw on top,
            // giving a cuff/outline read.
            val shaftFrom = 0.34f  // start the shaft a third down from the knee
            val sx0 = kneeX + sxd * L * shaftFrom
            val sy0 = kneeY + syd * L * shaftFrom
            // shaft up the shank
            s.stroke(PathSpec.line(sx0, sy0, ankleX, ankleY), Palette.withAlpha(Palette.BOOT_DARK, 0xE6), w * 1.95f)
            // foot box: ankle -> mid -> toe
            s.stroke(PathSpec.of {
                moveTo(ankleX, ankleY); lineTo(midX, midY); lineTo(toeX, toeY)
            }, Palette.withAlpha(Palette.BOOT_DARK, 0xE6), w * 1.7f)
            // rocker sole: a darker line just outside the foot underside
            val pfx = -fyd; val pfy = fxd            // perpendicular to the foot
            val soleOff = w * 0.95f
            s.stroke(PathSpec.of {
                moveTo(ankleX + pfx * soleOff, ankleY + pfy * soleOff)
                lineTo(midX + pfx * soleOff, midY + pfy * soleOff)
                lineTo(toeX + pfx * soleOff * 0.8f, toeY + pfy * soleOff * 0.8f)
            }, Palette.BOOT_DARK, w * 0.5f)
            // straps: two short bands perpendicular to the shank
            val pnx = -syd; val pny = sxd
            for (f in listOf(0.55f, 0.80f)) {
                val cxp = kneeX + sxd * L * f
                val cyp = kneeY + syd * L * f
                s.stroke(PathSpec.line(cxp - pnx * w * 1.0f, cyp - pny * w * 1.0f,
                    cxp + pnx * w * 1.0f, cyp + pny * w * 1.0f),
                    Palette.BOOT_MID, w * 0.28f, roundCaps = false)
            }
        }

        s.stroke(PathSpec.line(hipX, hipY, kneeX, kneeY), color, w)
        s.stroke(PathSpec.line(kneeX, kneeY, ankleX, ankleY), color, w * 0.88f)
        s.stroke(PathSpec.of {
            moveTo(ankleX, ankleY); lineTo(midX, midY); lineTo(toeX, toeY)
        }, color, w * 0.62f)

        return toeX to toeY
    }
}

/** Keyframe definitions for every demonstration referenced by the protocol content. */
object DemoLibrary {

    private fun seated(kneeA: Float, ankleA: Float = 0f, toesA: Float = 0f, kneeB: Float = 85f) =
        Pose(
            torso = 4f, thighA = 85f, kneeA = kneeA, ankleA = ankleA, toesA = toesA,
            thighB = 80f, kneeB = kneeB, ankleB = 0f, hipY = 0.95f
        )

    val demos: Map<String, Demo> = buildMap {
        put("toe_scrunch", Demo(
            setOf(Prop.BOOT, Prop.CHAIR),
            "Ankle still in the boot - only the toes curl and release",
            listOf(seated(85f, 0f, 0f) to 700L, seated(85f, 0f, 38f) to 700L, seated(85f, 0f, 0f) to 700L)
        ))
        put("knee_flex", Demo(
            setOf(Prop.BOOT, Prop.CHAIR),
            "Bend and straighten the knee - the boot just goes along",
            listOf(seated(95f) to 900L, seated(25f) to 900L, seated(95f) to 900L)
        ))
        put("slr", Demo(
            setOf(Prop.BOOT),
            "Leg straight in the boot - lift from the hip, lower slowly",
            listOf(
                Pose(lying = true, thighA = 2f, kneeA = 0f, thighB = 55f, kneeB = 75f) to 900L,
                Pose(lying = true, thighA = 28f, kneeA = 0f, thighB = 55f, kneeB = 75f) to 900L,
                Pose(lying = true, thighA = 2f, kneeA = 0f, thighB = 55f, kneeB = 75f) to 1100L
            )
        ))
        put("hip_abd", Demo(
            setOf(Prop.BOOT),
            "Side-lying: lift the booted leg straight up and out, then lower slowly",
            listOf(
                Pose(thighA = 2f) to 900L,
                Pose(thighA = 34f) to 900L,
                Pose(thighA = 2f) to 1100L
            ),
            view = DemoView.FRONT_ABD
        ))
        put("bridge", Demo(
            setOf(Prop.BOOT),
            "Knees bent, feet flat, squeeze and lift the hips, lower with control",
            listOf(
                // knees up with feet planted on the mat, hips resting, then the
                // pelvis lifts (hipY up) while the feet stay put, then lower
                Pose(lying = true, thighA = 62f, kneeA = 120f, thighB = 62f, kneeB = 120f, hipY = 0.06f) to 900L,
                Pose(lying = true, thighA = 40f, kneeA = 96f, thighB = 40f, kneeB = 96f, hipY = -0.26f) to 900L,
                Pose(lying = true, thighA = 62f, kneeA = 120f, thighB = 62f, kneeB = 120f, hipY = 0.06f) to 1100L
            )
        ))
        put("boot_walk", Demo(
            setOf(Prop.BOOT, Prop.CRUTCHES),
            "Roll through the boot: heel down, weight on, step through",
            listOf(
                Pose(thighA = 22f, kneeA = 6f, thighB = -16f, kneeB = 22f, torso = 4f) to 600L,
                Pose(thighA = 0f, kneeA = 8f, thighB = 0f, kneeB = 30f, torso = 4f) to 600L,
                Pose(thighA = -16f, kneeA = 10f, thighB = 24f, kneeB = 12f, torso = 4f) to 600L,
                Pose(thighA = 0f, kneeA = 18f, thighB = 0f, kneeB = 6f, torso = 4f) to 600L
            )
        ))
        put("leg_ext", Demo(
            setOf(Prop.BOOT, Prop.CHAIR),
            "Straighten the knee until the boot is level, hold, lower",
            listOf(seated(88f) to 900L, seated(6f) to 900L, seated(88f) to 1000L)
        ))
        put("clamshell", Demo(
            setOf(),
            "Side-lying, knees bent, feet together: open the top knee, pelvis still",
            listOf(
                Pose(thighA = 6f) to 800L,
                Pose(thighA = 44f) to 800L,
                Pose(thighA = 6f) to 900L
            ),
            view = DemoView.FRONT_CLAM
        ))
        put("seated_core", Demo(
            setOf(Prop.BOOT, Prop.CHAIR, Prop.BAND),
            "Sit tall - rows and presses against the band, foot resting",
            listOf(
                seated(85f).copy(arm = 80f) to 700L,
                seated(85f).copy(arm = 10f, torso = -4f) to 700L,
                seated(85f).copy(arm = 80f) to 800L
            )
        ))
        put("ankle_pump", Demo(
            setOf(Prop.CHAIR),
            "Point down fully - return ONLY to flat, never pull up further",
            listOf(seated(25f, 0f) to 900L, seated(25f, 38f) to 900L, seated(25f, 0f) to 1100L)
        ))
        put("ankle_inv_ev", Demo(
            setOf(Prop.CHAIR),
            "Sole turns gently in, then out - small and controlled",
            listOf(seated(25f, 8f, -8f) to 800L, seated(25f, 8f, 12f) to 800L, seated(25f, 8f, -8f) to 900L)
        ))
        put("seated_heel_raise", Demo(
            setOf(Prop.CHAIR),
            "Knee at 90 degrees - push through the ball of the foot, heel up",
            listOf(seated(85f, 0f) to 800L, seated(85f, 32f) to 800L, seated(85f, 0f) to 1000L)
        ))
        put("gait_walk", Demo(
            setOf(),
            "Heel strikes, roll through, gentle push-off - even steps",
            listOf(
                Pose(thighA = 24f, kneeA = 4f, ankleA = -4f, thighB = -14f, kneeB = 18f, ankleB = 18f) to 550L,
                Pose(thighA = 2f, kneeA = 10f, ankleA = 0f, thighB = 0f, kneeB = 34f, ankleB = 6f) to 550L,
                Pose(thighA = -14f, kneeA = 14f, ankleA = 16f, thighB = 24f, kneeB = 6f, ankleB = -4f) to 550L,
                Pose(thighA = 0f, kneeA = 32f, ankleA = 6f, thighB = 2f, kneeB = 8f, ankleB = 0f) to 550L
            )
        ))
        put("bike", Demo(
            setOf(Prop.BIKE, Prop.CHAIR),
            "Easy spinning - pedal through heel and midfoot",
            listOf(
                Pose(torso = 18f, thighA = 70f, kneeA = 70f, thighB = 30f, kneeB = 25f, hipY = 0.6f) to 500L,
                Pose(torso = 18f, thighA = 50f, kneeA = 35f, thighB = 50f, kneeB = 60f, hipY = 0.6f) to 500L,
                Pose(torso = 18f, thighA = 30f, kneeA = 25f, thighB = 70f, kneeB = 70f, hipY = 0.6f) to 500L,
                Pose(torso = 18f, thighA = 50f, kneeA = 60f, thighB = 50f, kneeB = 35f, hipY = 0.6f) to 500L
            )
        ))
        put("towel_scrunch", Demo(
            setOf(Prop.CHAIR, Prop.TOWEL),
            "Heel stays down - drag the towel in with the toes",
            listOf(seated(60f, 6f, 0f) to 700L, seated(60f, 6f, 34f) to 700L, seated(60f, 6f, 0f) to 800L)
        ))
        put("double_heel_raise", Demo(
            setOf(Prop.WALL),
            "Both heels rise together - slow up, pause, slower down",
            listOf(
                Pose() to 900L,
                Pose(ankleA = 30f, ankleB = 30f, hipY = -0.22f) to 900L,
                Pose() to 1100L
            )
        ))
        put("single_balance", Demo(
            setOf(Prop.WALL),
            "Stand tall on the injured leg - quiet foot, soft knee",
            listOf(
                Pose(kneeA = 4f, thighB = 25f, kneeB = 70f, hipX = 0.02f) to 1200L,
                Pose(kneeA = 6f, thighB = 25f, kneeB = 70f, hipX = -0.02f) to 1200L,
                Pose(kneeA = 4f, thighB = 25f, kneeB = 70f, hipX = 0.02f) to 1200L
            )
        ))
        put("band_pf", Demo(
            // seated with the leg extended and a band looped round the ball of the
            // foot (held ahead) - the foot presses down against it. Rendered seated
            // rather than lying so the pose reads clearly instead of collapsing.
            setOf(Prop.CHAIR, Prop.BAND),
            "Push the foot down against the band like a slow gas pedal",
            listOf(
                seated(20f, -2f) to 800L,
                seated(20f, 34f) to 800L,
                seated(20f, -2f) to 1000L
            )
        ))
        put("step_up", Demo(
            setOf(Prop.STEP),
            "Injured foot on the step - drive up through the heel",
            listOf(
                Pose(thighA = 50f, kneeA = 70f, thighB = 0f, kneeB = 0f, torso = 8f) to 900L,
                Pose(thighA = 8f, kneeA = 6f, thighB = 12f, kneeB = 30f, torso = 2f, hipY = -0.5f, hipX = 0.45f) to 900L,
                Pose(thighA = 50f, kneeA = 70f, thighB = 0f, kneeB = 0f, torso = 8f) to 1100L
            )
        ))
        put("squat", Demo(
            setOf(),
            "Sit back and down, heels planted, drive up evenly",
            listOf(
                Pose() to 900L,
                Pose(torso = 28f, thighA = 52f, kneeA = 95f, ankleA = -12f, thighB = 52f, kneeB = 95f, ankleB = -12f, hipY = 0.55f, arm = 80f) to 900L,
                Pose() to 1100L
            )
        ))
        put("single_heel_raise", Demo(
            setOf(Prop.WALL),
            "All bodyweight on the injured leg - full height, slow down",
            listOf(
                Pose(thighB = 18f, kneeB = 50f) to 900L,
                Pose(ankleA = 32f, hipY = -0.24f, thighB = 18f, kneeB = 50f) to 900L,
                Pose(thighB = 18f, kneeB = 50f) to 1100L
            )
        ))
        put("jog", Demo(
            setOf(),
            "Relaxed walk-jog intervals - flat ground, easy pace",
            listOf(
                Pose(thighA = 35f, kneeA = 25f, ankleA = -5f, thighB = -20f, kneeB = 50f, ankleB = 22f, hipY = -0.06f, torso = 6f, arm = 60f) to 380L,
                Pose(thighA = 0f, kneeA = 20f, thighB = 0f, kneeB = 60f, hipY = 0.02f, torso = 6f, arm = 20f) to 380L,
                Pose(thighA = -20f, kneeA = 50f, ankleA = 22f, thighB = 35f, kneeB = 25f, ankleB = -5f, hipY = -0.06f, torso = 6f, arm = 60f) to 380L,
                Pose(thighA = 0f, kneeA = 60f, thighB = 0f, kneeB = 20f, hipY = 0.02f, torso = 6f, arm = 20f) to 380L
            )
        ))
        put("hop", Demo(
            setOf(),
            "Small spring, quiet landing - soft knees and ankles",
            listOf(
                Pose() to 500L,
                Pose(kneeA = 35f, kneeB = 35f, hipY = 0.22f, torso = 12f) to 400L,
                Pose(ankleA = 30f, ankleB = 30f, hipY = -0.45f) to 350L,
                Pose(kneeA = 35f, kneeB = 35f, hipY = 0.22f, torso = 12f) to 350L,
                Pose() to 500L
            )
        ))
        put("agility", Demo(
            setOf(Prop.CONES),
            "Shuffle between cones - stay low, controlled turns",
            listOf(
                Pose(kneeA = 30f, kneeB = 30f, hipY = 0.18f, hipX = -0.7f, torso = 14f) to 500L,
                Pose(kneeA = 22f, kneeB = 38f, hipY = 0.12f, hipX = 0f, torso = 14f) to 500L,
                Pose(kneeA = 30f, kneeB = 30f, hipY = 0.18f, hipX = 0.7f, torso = 14f) to 500L,
                Pose(kneeA = 38f, kneeB = 22f, hipY = 0.12f, hipX = 0f, torso = 14f) to 500L
            )
        ))
        put("padel_drill", Demo(
            setOf(Prop.RACQUET),
            "Shadow swings and court movement - build up in stages",
            listOf(
                Pose(kneeA = 18f, kneeB = 18f, hipY = 0.1f, arm = -30f, torso = 10f) to 600L,
                Pose(kneeA = 30f, kneeB = 12f, hipY = 0.14f, hipX = 0.2f, arm = 75f, torso = 16f) to 500L,
                Pose(kneeA = 18f, kneeB = 18f, hipY = 0.1f, arm = 20f, torso = 10f) to 600L
            )
        ))
    }
}
