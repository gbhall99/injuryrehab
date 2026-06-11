package com.recoverwell.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated exercise demonstrations rendered procedurally on Canvas.
 *
 * Chosen over bundled video files because: zero APK bloat, always available
 * offline, no third-party hosting or licensing, and every demonstration is
 * editable in code review like any other logic. Each demo is a set of named
 * joint-angle keyframes interpolated at 60fps - effectively a looping,
 * pausable "video" the app owns outright.
 */
class ExerciseDemoView(context: Context) : View(context) {

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

    data class Demo(
        val props: Set<Prop>,
        val caption: String,
        /** keyframe + milliseconds to morph from the previous frame */
        val frames: List<Pair<Pose, Long>>
    )

    var demoId: String = "ankle_pump"
        set(value) {
            field = value
            elapsed = 0L
            contentDescription = "Animated demonstration: ${demo().caption}. Tap to pause or play."
            invalidate()
        }

    private var playing = true
    private var elapsed = 0L
    private var lastTick = 0L

    private val limbA = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1B5E20.toInt(); strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE
    }
    private val limbB = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x701B5E20; strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE
    }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF33691E.toInt(); strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE
    }
    private val bootPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCC37474F.toInt() }
    private val propPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF607D8B.toInt(); strokeWidth = 8f; style = Paint.Style.STROKE
    }
    private val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFD32F2F.toInt(); strokeWidth = 10f; style = Paint.Style.STROKE
    }
    private val groundPaint = Paint().apply { color = Ui.BORDER; strokeWidth = 6f }
    private val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Ui.TEXT_DIM; textSize = 30f
    }
    private val pausePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Ui.PRIMARY; textSize = 44f; isFakeBoldText = true
    }

    init {
        isClickable = true
        setOnClickListener {
            playing = !playing
            if (playing) { lastTick = 0L; postInvalidateOnAnimation() }
            invalidate()
        }
    }

    private val ticker = object : Runnable {
        override fun run() {
            if (!isAttachedToWindow || !playing) return
            val now = System.nanoTime() / 1_000_000
            if (lastTick != 0L) elapsed += (now - lastTick).coerceAtMost(64)
            lastTick = now
            invalidate()
            postOnAnimation(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lastTick = 0L
        postOnAnimation(ticker)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE && playing) { lastTick = 0L; postOnAnimation(ticker) }
    }

    private fun demo(): Demo = DemoLibrary.demos[demoId] ?: DemoLibrary.demos.getValue("ankle_pump")

    private fun currentPose(): Pose {
        val d = demo()
        val total = d.frames.sumOf { it.second }
        if (total <= 0) return d.frames.first().first
        var t = elapsed % total
        var prev = d.frames.last().first
        for ((pose, dur) in d.frames) {
            if (t < dur) {
                val f = (t.toFloat() / dur).coerceIn(0f, 1f)
                val eased = f * f * (3 - 2 * f) // smoothstep
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

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(0xFFF0F4F0.toInt())
        val d = demo()
        val pose = currentPose()

        val L = height * 0.21f // leg segment length
        limbA.strokeWidth = L * 0.16f
        limbB.strokeWidth = L * 0.16f
        bodyPaint.strokeWidth = L * 0.19f

        val groundY = height * 0.80f
        canvas.drawLine(0f, groundY, width.toFloat(), groundY, groundPaint)

        canvas.save()
        if (pose.lying) {
            // rotate the standing rig so the figure lies on the ground line
            canvas.rotate(-90f, width * 0.5f, groundY - L * 1.1f)
            canvas.translate(width * 0.5f - width * 0.40f, L * 0.4f)
        }

        val hipX = width * 0.40f + pose.hipX * L
        val hipY = groundY - 2.40f * L + pose.hipY * L

        // torso + head
        val (tx, ty) = dir(pose.torso)
        val shoulderX = hipX + tx * L * 1.15f
        val shoulderY = hipY - ty * L * 1.15f
        canvas.drawLine(hipX, hipY, shoulderX, shoulderY, bodyPaint)
        canvas.drawCircle(shoulderX + tx * L * 0.32f, shoulderY - ty * L * 0.32f, L * 0.22f, bodyPaint)

        // arm (used by racquet and balance demos)
        val (ax, ay) = dir(pose.arm)
        val handX = shoulderX + ax * L * 0.95f
        val handY = shoulderY + ay * L * 0.95f
        canvas.drawLine(shoulderX, shoulderY, handX, handY, limbB)
        if (Prop.RACQUET in d.props) {
            canvas.drawLine(handX, handY, handX + ax * L * 0.5f, handY + ay * L * 0.5f, propPaint)
            canvas.drawOval(
                handX + ax * L * 0.5f - L * 0.22f, handY + ay * L * 0.5f - L * 0.30f,
                handX + ax * L * 0.5f + L * 0.22f, handY + ay * L * 0.5f + L * 0.30f, propPaint
            )
        }
        if (Prop.CRUTCHES in d.props) {
            canvas.drawLine(shoulderX + L * 0.25f, shoulderY, shoulderX + L * 0.45f, groundY, propPaint)
            canvas.drawLine(shoulderX - L * 0.15f, shoulderY, shoulderX - L * 0.35f, groundY, propPaint)
        }
        if (Prop.WALL in d.props) {
            canvas.drawLine(width * 0.78f, groundY - 2.6f * L, width * 0.78f, groundY, propPaint)
            canvas.drawLine(shoulderX, shoulderY, width * 0.78f, shoulderY, limbB)
        }
        if (Prop.CHAIR in d.props) {
            canvas.drawLine(hipX - L * 0.55f, hipY + L * 0.05f, hipX + L * 0.35f, hipY + L * 0.05f, propPaint)
            canvas.drawLine(hipX - L * 0.45f, hipY + L * 0.05f, hipX - L * 0.45f, groundY, propPaint)
            canvas.drawLine(hipX + L * 0.25f, hipY + L * 0.05f, hipX + L * 0.25f, groundY, propPaint)
        }
        if (Prop.STEP in d.props) {
            canvas.drawRect(hipX + L * 0.35f, groundY - L * 0.55f, hipX + L * 1.6f, groundY, propPaint)
        }
        if (Prop.CONES in d.props) {
            for (i in 0..1) {
                val cxn = width * (0.18f + 0.6f * i)
                val cone = Path().apply {
                    moveTo(cxn, groundY - L * 0.4f); lineTo(cxn - L * 0.2f, groundY)
                    lineTo(cxn + L * 0.2f, groundY); close()
                }
                canvas.drawPath(cone, propPaint)
            }
        }
        if (Prop.BIKE in d.props) {
            canvas.drawCircle(hipX + L * 0.7f, groundY - L * 0.55f, L * 0.55f, propPaint)
        }

        // back leg (B) first so the injured-side leg (A) draws on top
        drawLeg(canvas, hipX, hipY, L, pose.thighB, pose.kneeB, pose.ankleB, 0f, limbB, boot = false)
        val footAnkle = drawLeg(canvas, hipX, hipY, L, pose.thighA, pose.kneeA, pose.ankleA, pose.toesA, limbA, boot = Prop.BOOT in d.props)

        if (Prop.BAND in d.props) {
            canvas.drawLine(footAnkle.first, footAnkle.second, width * 0.92f, footAnkle.second + L * 0.1f, bandPaint)
        }
        if (Prop.TOWEL in d.props) {
            canvas.drawLine(footAnkle.first - L * 0.3f, groundY - 4f, footAnkle.first + L * 0.9f, groundY - 4f, bandPaint)
        }

        canvas.restore()

        canvas.drawText(d.caption, 24f, height - 20f, captionPaint)
        if (!playing) canvas.drawText("▶ tap to play", width / 2f - 100f, height / 2f, pausePaint)
    }

    /** Draws one leg; returns the toe-end coordinates of the foot. */
    private fun drawLeg(
        canvas: Canvas, hipX: Float, hipY: Float, L: Float,
        thigh: Float, knee: Float, ankle: Float, toes: Float,
        paint: Paint, boot: Boolean
    ): Pair<Float, Float> {
        val (txd, tyd) = dir(thigh)
        val kneeX = hipX + txd * L
        val kneeY = hipY + tyd * L
        val shankAngle = thigh - knee
        val (sxd, syd) = dir(shankAngle)
        val ankleX = kneeX + sxd * L
        val ankleY = kneeY + syd * L
        val footAngle = shankAngle - 90f + ankle
        val (fxd, fyd) = dir(footAngle)
        val footLen = L * 0.45f
        val midFootX = ankleX + fxd * footLen
        val midFootY = ankleY + fyd * footLen
        val toesAngle = footAngle + toes
        val (toxd, toyd) = dir(toesAngle)
        val toeX = midFootX + toxd * L * 0.20f
        val toeY = midFootY + toyd * L * 0.20f

        canvas.drawLine(hipX, hipY, kneeX, kneeY, paint)
        canvas.drawLine(kneeX, kneeY, ankleX, ankleY, paint)
        canvas.drawLine(ankleX, ankleY, midFootX, midFootY, paint)
        canvas.drawLine(midFootX, midFootY, toeX, toeY, paint)

        if (boot) {
            val bootPath = Path().apply {
                moveTo(kneeX + sxd * L * 0.35f - L * 0.16f, kneeY + syd * L * 0.35f)
                lineTo(kneeX + sxd * L * 0.35f + L * 0.16f, kneeY + syd * L * 0.35f)
                lineTo(toeX + L * 0.10f, toeY + L * 0.16f)
                lineTo(ankleX - fxd * L * 0.25f - L * 0.12f, ankleY + L * 0.20f)
                close()
            }
            canvas.drawPath(bootPath, bootPaint)
        }
        return toeX to toeY
    }
}

private typealias P = ExerciseDemoView.Pose

/** Keyframe definitions for every demonstration referenced by the protocol content. */
object DemoLibrary {

    private fun seated(kneeA: Float, ankleA: Float = 0f, toesA: Float = 0f, kneeB: Float = 85f) =
        ExerciseDemoView.Pose(
            torso = 4f, thighA = 85f, kneeA = kneeA, ankleA = ankleA, toesA = toesA,
            thighB = 80f, kneeB = kneeB, ankleB = 0f, hipY = 0.95f
        )

    val demos: Map<String, ExerciseDemoView.Demo> = buildMap {
        put("toe_scrunch", ExerciseDemoView.Demo(
            setOf(ExerciseDemoView.Prop.BOOT, ExerciseDemoView.Prop.CHAIR),
            "Ankle still in the boot - only the toes curl and release",
            listOf(seated(85f, 0f, 0f) to 700L, seated(85f, 0f, 38f) to 700L, seated(85f, 0f, 0f) to 700L)
        ))
        put("knee_flex", ExerciseDemoView.Demo(
            setOf(ExerciseDemoView.Prop.BOOT, ExerciseDemoView.Prop.CHAIR),
            "Bend and straighten the knee - the boot just goes along",
            listOf(seated(95f) to 900L, seated(25f) to 900L, seated(95f) to 900L)
        ))
        put("slr", ExerciseDemoView.Demo(
            setOf(ExerciseDemoView.Prop.BOOT),
            "Leg straight in the boot - lift from the hip, lower slowly",
            listOf(
                P(lying = true, thighA = 2f, kneeA = 0f, thighB = 55f, kneeB = 75f) to 900L,
                P(lying = true, thighA = 28f, kneeA = 0f, thighB = 55f, kneeB = 75f) to 900L,
                P(lying = true, thighA = 2f, kneeA = 0f, thighB = 55f, kneeB = 75f) to 1100L
            )
        ))
        put("hip_abd", ExerciseDemoView.Demo(
            setOf(ExerciseDemoView.Prop.BOOT),
            "Side-lying: lift the booted leg sideways, keep it straight",
            listOf(
                P(lying = true, thighA = 0f, thighB = 8f, kneeB = 10f) to 900L,
                P(lying = true, thighA = 32f, thighB = 8f, kneeB = 10f) to 900L,
                P(lying = true, thighA = 0f, thighB = 8f, kneeB = 10f) to 1100L
            )
        ))
        put("bridge", ExerciseDemoView.Demo(
            setOf(ExerciseDemoView.Prop.BOOT),
            "Knees bent, squeeze and lift the hips, lower with control",
            listOf(
                P(lying = true, thighA = 50f, kneeA = 80f, thighB = 50f, kneeB = 80f) to 900L,
                P(lying = true, thighA = 22f, kneeA = 80f, thighB = 22f, kneeB = 80f, hipY = -0.30f) to 900L,
                P(lying = true, thighA = 50f, kneeA = 80f, thighB = 50f, kneeB = 80f) to 1100L
            )
        ))
        put("boot_walk", ExerciseDemoView.Demo(
            setOf(ExerciseDemoView.Prop.BOOT, ExerciseDemoView.Prop.CRUTCHES),
            "Roll through the boot: heel down, weight on, step through",
            listOf(
                P(thighA = 22f, kneeA = 6f, thighB = -16f, kneeB = 22f, torso = 4f) to 600L,
                P(thighA = 0f, kneeA = 8f, thighB = 0f, kneeB = 30f, torso = 4f) to 600L,
                P(thighA = -16f, kneeA = 10f, thighB = 24f, kneeB = 12f, torso = 4f) to 600L,
                P(thighA = 0f, kneeA = 18f, thighB = 0f, kneeB = 6f, torso = 4f) to 600L
            )
        ))
        put("leg_ext", ExerciseDemoView.Demo(
            setOf(ExerciseDemoView.Prop.BOOT, ExerciseDemoView.Prop.CHAIR),
            "Straighten the knee until the boot is level, hold, lower",
            listOf(seated(88f) to 900L, seated(6f) to 900L, seated(88f) to 1000L)
        ))
        put("clamshell", ExerciseDemoView.Demo(
            setOf(),
            "Side-lying, knees bent: open the top knee, pelvis still",
            listOf(
                P(lying = true, thighA = 55f, kneeA = 85f, thighB = 55f, kneeB = 85f) to 800L,
                P(lying = true, thighA = 28f, kneeA = 85f, thighB = 55f, kneeB = 85f) to 800L,
                P(lying = true, thighA = 55f, kneeA = 85f, thighB = 55f, kneeB = 85f) to 900L
            )
        ))
        put("seated_core", ExerciseDemoView.Demo(
            setOf(ExerciseDemoView.Prop.BOOT, ExerciseDemoView.Prop.CHAIR, ExerciseDemoView.Prop.BAND),
            "Sit tall - rows and presses against the band, foot resting",
            listOf(
                seated(85f).copy(arm = 80f) to 700L,
                seated(85f).copy(arm = 10f, torso = -4f) to 700L,
                seated(85f).copy(arm = 80f) to 800L
            )
        ))
        put("ankle_pump", ExerciseDemoView.Demo(
            setOf(ExerciseDemoView.Prop.CHAIR),
            "Point down fully - return ONLY to flat, never pull up further",
            listOf(seated(25f, 0f) to 900L, seated(25f, 38f) to 900L, seated(25f, 0f) to 1100L)
        ))
        put("ankle_inv_ev", ExerciseDemoView.Demo(
            setOf(ExerciseDemoView.Prop.CHAIR),
            "Sole turns gently in, then out - small and controlled",
            listOf(seated(25f, 8f, -8f) to 800L, seated(25f, 8f, 12f) to 800L, seated(25f, 8f, -8f) to 900L)
        ))
        put("seated_heel_raise", ExerciseDemoView.Demo(
            setOf(ExerciseDemoView.Prop.CHAIR),
            "Knee at 90° - push through the ball of the foot, heel up",
            listOf(seated(85f, 0f) to 800L, seated(85f, 32f) to 800L, seated(85f, 0f) to 1000L)
        ))
        put("gait_walk", ExerciseDemoView.Demo(
            setOf(),
            "Heel strikes, roll through, gentle push-off - even steps",
            listOf(
                P(thighA = 24f, kneeA = 4f, ankleA = -4f, thighB = -14f, kneeB = 18f, ankleB = 18f) to 550L,
                P(thighA = 2f, kneeA = 10f, ankleA = 0f, thighB = 0f, kneeB = 34f, ankleB = 6f) to 550L,
                P(thighA = -14f, kneeA = 14f, ankleA = 16f, thighB = 24f, kneeB = 6f, ankleB = -4f) to 550L,
                P(thighA = 0f, kneeA = 32f, ankleA = 6f, thighB = 2f, kneeB = 8f, ankleB = 0f) to 550L
            )
        ))
        put("bike", ExerciseDemoView.Demo(
            setOf(ExerciseDemoView.Prop.BIKE, ExerciseDemoView.Prop.CHAIR),
            "Easy spinning - pedal through heel and midfoot",
            listOf(
                P(torso = 18f, thighA = 70f, kneeA = 70f, thighB = 30f, kneeB = 25f, hipY = 0.6f) to 500L,
                P(torso = 18f, thighA = 50f, kneeA = 35f, thighB = 50f, kneeB = 60f, hipY = 0.6f) to 500L,
                P(torso = 18f, thighA = 30f, kneeA = 25f, thighB = 70f, kneeB = 70f, hipY = 0.6f) to 500L,
                P(torso = 18f, thighA = 50f, kneeA = 60f, thighB = 50f, kneeB = 35f, hipY = 0.6f) to 500L
            )
        ))
        put("towel_scrunch", ExerciseDemoView.Demo(
            setOf(ExerciseDemoView.Prop.CHAIR, ExerciseDemoView.Prop.TOWEL),
            "Heel stays down - drag the towel in with the toes",
            listOf(seated(60f, 6f, 0f) to 700L, seated(60f, 6f, 34f) to 700L, seated(60f, 6f, 0f) to 800L)
        ))
        put("double_heel_raise", ExerciseDemoView.Demo(
            setOf(ExerciseDemoView.Prop.WALL),
            "Both heels rise together - slow up, pause, slower down",
            listOf(
                P() to 900L,
                P(ankleA = 30f, ankleB = 30f, hipY = -0.22f) to 900L,
                P() to 1100L
            )
        ))
        put("single_balance", ExerciseDemoView.Demo(
            setOf(ExerciseDemoView.Prop.WALL),
            "Stand tall on the injured leg - quiet foot, soft knee",
            listOf(
                P(kneeA = 4f, thighB = 25f, kneeB = 70f, hipX = 0.02f) to 1200L,
                P(kneeA = 6f, thighB = 25f, kneeB = 70f, hipX = -0.02f) to 1200L,
                P(kneeA = 4f, thighB = 25f, kneeB = 70f, hipX = 0.02f) to 1200L
            )
        ))
        put("band_pf", ExerciseDemoView.Demo(
            setOf(ExerciseDemoView.Prop.BAND),
            "Push the foot down against the band like a slow gas pedal",
            listOf(
                P(lying = true, torso = -65f, thighA = 12f, kneeA = 8f, ankleA = -2f, thighB = 20f, kneeB = 35f) to 800L,
                P(lying = true, torso = -65f, thighA = 12f, kneeA = 8f, ankleA = 34f, thighB = 20f, kneeB = 35f) to 800L,
                P(lying = true, torso = -65f, thighA = 12f, kneeA = 8f, ankleA = -2f, thighB = 20f, kneeB = 35f) to 1000L
            )
        ))
        put("step_up", ExerciseDemoView.Demo(
            setOf(ExerciseDemoView.Prop.STEP),
            "Injured foot on the step - drive up through the heel",
            listOf(
                P(thighA = 50f, kneeA = 70f, thighB = 0f, kneeB = 0f, torso = 8f) to 900L,
                P(thighA = 8f, kneeA = 6f, thighB = 12f, kneeB = 30f, torso = 2f, hipY = -0.5f, hipX = 0.45f) to 900L,
                P(thighA = 50f, kneeA = 70f, thighB = 0f, kneeB = 0f, torso = 8f) to 1100L
            )
        ))
        put("squat", ExerciseDemoView.Demo(
            setOf(),
            "Sit back and down, heels planted, drive up evenly",
            listOf(
                P() to 900L,
                P(torso = 28f, thighA = 52f, kneeA = 95f, ankleA = -12f, thighB = 52f, kneeB = 95f, ankleB = -12f, hipY = 0.55f, arm = 80f) to 900L,
                P() to 1100L
            )
        ))
        put("single_heel_raise", ExerciseDemoView.Demo(
            setOf(ExerciseDemoView.Prop.WALL),
            "All bodyweight on the injured leg - full height, slow down",
            listOf(
                P(thighB = 18f, kneeB = 50f) to 900L,
                P(ankleA = 32f, hipY = -0.24f, thighB = 18f, kneeB = 50f) to 900L,
                P(thighB = 18f, kneeB = 50f) to 1100L
            )
        ))
        put("jog", ExerciseDemoView.Demo(
            setOf(),
            "Relaxed walk-jog intervals - flat ground, easy pace",
            listOf(
                P(thighA = 35f, kneeA = 25f, ankleA = -5f, thighB = -20f, kneeB = 50f, ankleB = 22f, hipY = -0.06f, torso = 6f, arm = 60f) to 380L,
                P(thighA = 0f, kneeA = 20f, thighB = 0f, kneeB = 60f, hipY = 0.02f, torso = 6f, arm = 20f) to 380L,
                P(thighA = -20f, kneeA = 50f, ankleA = 22f, thighB = 35f, kneeB = 25f, ankleB = -5f, hipY = -0.06f, torso = 6f, arm = 60f) to 380L,
                P(thighA = 0f, kneeA = 60f, thighB = 0f, kneeB = 20f, hipY = 0.02f, torso = 6f, arm = 20f) to 380L
            )
        ))
        put("hop", ExerciseDemoView.Demo(
            setOf(),
            "Small spring, quiet landing - soft knees and ankles",
            listOf(
                P() to 500L,
                P(kneeA = 35f, kneeB = 35f, hipY = 0.22f, torso = 12f) to 400L,
                P(ankleA = 30f, ankleB = 30f, hipY = -0.45f) to 350L,
                P(kneeA = 35f, kneeB = 35f, hipY = 0.22f, torso = 12f) to 350L,
                P() to 500L
            )
        ))
        put("agility", ExerciseDemoView.Demo(
            setOf(ExerciseDemoView.Prop.CONES),
            "Shuffle between cones - stay low, controlled turns",
            listOf(
                P(kneeA = 30f, kneeB = 30f, hipY = 0.18f, hipX = -0.7f, torso = 14f) to 500L,
                P(kneeA = 22f, kneeB = 38f, hipY = 0.12f, hipX = 0f, torso = 14f) to 500L,
                P(kneeA = 30f, kneeB = 30f, hipY = 0.18f, hipX = 0.7f, torso = 14f) to 500L,
                P(kneeA = 38f, kneeB = 22f, hipY = 0.12f, hipX = 0f, torso = 14f) to 500L
            )
        ))
        put("padel_drill", ExerciseDemoView.Demo(
            setOf(ExerciseDemoView.Prop.RACQUET),
            "Shadow swings and court movement - build up in stages",
            listOf(
                P(kneeA = 18f, kneeB = 18f, hipY = 0.1f, arm = -30f, torso = 10f) to 600L,
                P(kneeA = 30f, kneeB = 12f, hipY = 0.14f, hipX = 0.2f, arm = 75f, torso = 16f) to 500L,
                P(kneeA = 18f, kneeB = 18f, hipY = 0.1f, arm = 20f, torso = 10f) to 600L
            )
        ))
    }
}
