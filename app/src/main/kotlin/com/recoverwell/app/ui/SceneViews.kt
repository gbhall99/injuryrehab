package com.recoverwell.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.View
import com.recoverwell.draw.*

/** android.graphics.Canvas backend for the shared Sketch drawing abstraction. */
class AndroidSketch(private val canvas: Canvas) : Sketch {

    override val width: Float get() = canvas.width.toFloat()
    override val height: Float get() = canvas.height.toFloat()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val medium = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    private fun toPath(p: PathSpec): Path {
        val out = Path()
        for (c in p.cmds) when (c) {
            is PathSpec.Cmd.Move -> out.moveTo(c.x, c.y)
            is PathSpec.Cmd.Line -> out.lineTo(c.x, c.y)
            is PathSpec.Cmd.Quad -> out.quadTo(c.cx, c.cy, c.x, c.y)
            is PathSpec.Cmd.Cubic -> out.cubicTo(c.c1x, c.c1y, c.c2x, c.c2y, c.x, c.y)
            is PathSpec.Cmd.Close -> out.close()
        }
        return out
    }

    override fun save() { canvas.save() }
    override fun restore() { canvas.restore() }
    override fun translate(dx: Float, dy: Float) = canvas.translate(dx, dy)
    override fun rotate(degrees: Float, px: Float, py: Float) = canvas.rotate(degrees, px, py)

    override fun clear(color: Int) = canvas.drawColor(color)

    override fun fill(path: PathSpec, color: Int) {
        paint.reset(); paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawPath(toPath(path), paint)
    }

    override fun fillGradient(path: PathSpec, x0: Float, y0: Float, x1: Float, y1: Float, c0: Int, c1: Int) {
        paint.reset(); paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(x0, y0, x1, y1, c0, c1, Shader.TileMode.CLAMP)
        canvas.drawPath(toPath(path), paint)
        paint.shader = null
    }

    override fun stroke(path: PathSpec, color: Int, strokeWidth: Float, roundCaps: Boolean, dash: FloatArray?) {
        paint.reset(); paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.color = color
        paint.strokeWidth = strokeWidth
        paint.strokeCap = if (roundCaps) Paint.Cap.ROUND else Paint.Cap.BUTT
        paint.strokeJoin = Paint.Join.ROUND
        if (dash != null) paint.pathEffect = DashPathEffect(dash, 0f)
        canvas.drawPath(toPath(path), paint)
        paint.pathEffect = null
    }

    override fun circle(cx: Float, cy: Float, r: Float, color: Int) {
        paint.reset(); paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawCircle(cx, cy, r, paint)
    }

    override fun circleStroke(cx: Float, cy: Float, r: Float, color: Int, strokeWidth: Float) {
        paint.reset(); paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.color = color
        paint.strokeWidth = strokeWidth
        canvas.drawCircle(cx, cy, r, paint)
    }

    override fun roundRect(l: Float, t: Float, r: Float, b: Float, radius: Float, color: Int) {
        paint.reset(); paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawRoundRect(RectF(l, t, r, b), radius, radius, paint)
    }

    override fun arc(cx: Float, cy: Float, radius: Float, startDeg: Float, sweepDeg: Float,
                     color: Int, strokeWidth: Float, roundCaps: Boolean) {
        paint.reset(); paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.color = color
        paint.strokeWidth = strokeWidth
        paint.strokeCap = if (roundCaps) Paint.Cap.ROUND else Paint.Cap.BUTT
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(rect, startDeg, sweepDeg, false, paint)
    }

    override fun text(s: String, x: Float, y: Float, sizePx: Float, color: Int, medium: Boolean, centered: Boolean) {
        paint.reset(); paint.isAntiAlias = true
        paint.color = color
        paint.textSize = sizePx
        paint.typeface = if (medium) this.medium else Typeface.SANS_SERIF
        paint.textAlign = if (centered) Paint.Align.CENTER else Paint.Align.LEFT
        canvas.drawText(s, x, y, paint)
    }

    override fun measureText(s: String, sizePx: Float, medium: Boolean): Float {
        paint.reset()
        paint.textSize = sizePx
        paint.typeface = if (medium) this.medium else Typeface.SANS_SERIF
        return paint.measureText(s)
    }
}

/** Static scene host: renders a Sketch lambda once per draw. */
class SceneView(context: Context, var scene: (Sketch) -> Unit) : View(context) {
    override fun onDraw(canvas: Canvas) {
        scene(AndroidSketch(canvas))
    }
}

/**
 * Animated exercise demonstration host. The keyframe engine and figure
 * rendering live in the shared draw module; this view adds the 60fps loop
 * and tap-to-pause. A test hook disables the loop under JVM schedulers.
 */
class ExerciseDemoView(context: Context) : View(context) {

    companion object {
        /** Test hook: JVM test schedulers would spin the frame loop forever. */
        @JvmStatic
        var frameLoopEnabled = true
    }

    var demoId: String = "ankle_pump"
        set(value) {
            field = value
            elapsed = 0L
            val caption = DemoLibrary.demos[value]?.caption ?: ""
            contentDescription = "Demonstration: $caption. Tap to pause or play."
            invalidate()
        }

    private var playing = true
    private var elapsed = 0L
    private var lastTick = 0L
    private val overlay = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        isClickable = true
        setOnClickListener {
            playing = !playing
            if (playing && frameLoopEnabled) { lastTick = 0L; postOnAnimation(ticker) }
            invalidate()
        }
    }

    private val ticker = object : Runnable {
        override fun run() {
            if (!isAttachedToWindow || !playing || !frameLoopEnabled) return
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
        if (frameLoopEnabled) postOnAnimation(ticker)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE && playing && frameLoopEnabled) {
            lastTick = 0L
            postOnAnimation(ticker)
        }
    }

    override fun onDraw(canvas: Canvas) {
        DemoScene.render(AndroidSketch(canvas), demoId, elapsed)
        if (!playing) {
            // dim + centred play glyph
            overlay.color = 0x59FFFFFF
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlay)
            overlay.color = Palette.PRIMARY
            val cx = width / 2f
            val cy = height / 2f
            val r = Ui.dpF(context, 26f)
            canvas.drawCircle(cx, cy, r, overlay)
            overlay.color = 0xFFFFFFFF.toInt()
            val p = Path()
            p.moveTo(cx - r * 0.28f, cy - r * 0.42f)
            p.lineTo(cx + r * 0.50f, cy)
            p.lineTo(cx - r * 0.28f, cy + r * 0.42f)
            p.close()
            canvas.drawPath(p, overlay)
        }
    }
}
