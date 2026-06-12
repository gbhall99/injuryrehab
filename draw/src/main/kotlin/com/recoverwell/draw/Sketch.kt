package com.recoverwell.draw

/**
 * Minimal platform-agnostic 2D drawing abstraction. The Android app backs it
 * with android.graphics.Canvas; the designlab tool backs it with Java2D so
 * every visual surface can be rendered to PNG and reviewed during design.
 * A future iOS/web port supplies its own backend.
 */
interface Sketch {
    val width: Float
    val height: Float

    fun save()
    fun restore()
    fun translate(dx: Float, dy: Float)
    fun rotate(degrees: Float, px: Float, py: Float)

    fun clear(color: Int)
    fun fill(path: PathSpec, color: Int)
    /** Vertical/diagonal linear gradient fill. */
    fun fillGradient(path: PathSpec, x0: Float, y0: Float, x1: Float, y1: Float, c0: Int, c1: Int)
    fun stroke(
        path: PathSpec,
        color: Int,
        strokeWidth: Float,
        roundCaps: Boolean = true,
        dash: FloatArray? = null
    )

    fun circle(cx: Float, cy: Float, r: Float, color: Int)
    fun circleStroke(cx: Float, cy: Float, r: Float, color: Int, strokeWidth: Float)
    fun roundRect(l: Float, t: Float, r: Float, b: Float, radius: Float, color: Int)
    /** Arc stroke for progress rings; angles in degrees, 0 = 3 o'clock, clockwise. */
    fun arc(cx: Float, cy: Float, radius: Float, startDeg: Float, sweepDeg: Float,
            color: Int, strokeWidth: Float, roundCaps: Boolean = true)

    fun text(s: String, x: Float, y: Float, sizePx: Float, color: Int,
             medium: Boolean = false, centered: Boolean = false)
    fun measureText(s: String, sizePx: Float, medium: Boolean = false): Float
}

/** Recorded vector path: M/L/Q/C/Z only, so it maps 1:1 to Android pathData. */
class PathSpec {
    sealed class Cmd {
        data class Move(val x: Float, val y: Float) : Cmd()
        data class Line(val x: Float, val y: Float) : Cmd()
        data class Quad(val cx: Float, val cy: Float, val x: Float, val y: Float) : Cmd()
        data class Cubic(val c1x: Float, val c1y: Float, val c2x: Float, val c2y: Float,
                         val x: Float, val y: Float) : Cmd()
        object Close : Cmd()
    }

    val cmds = ArrayList<Cmd>()

    fun moveTo(x: Float, y: Float) = apply { cmds.add(Cmd.Move(x, y)) }
    fun lineTo(x: Float, y: Float) = apply { cmds.add(Cmd.Line(x, y)) }
    fun quadTo(cx: Float, cy: Float, x: Float, y: Float) = apply { cmds.add(Cmd.Quad(cx, cy, x, y)) }
    fun cubicTo(c1x: Float, c1y: Float, c2x: Float, c2y: Float, x: Float, y: Float) =
        apply { cmds.add(Cmd.Cubic(c1x, c1y, c2x, c2y, x, y)) }
    fun close() = apply { cmds.add(Cmd.Close) }

    /** Axis-aligned rounded rectangle approximated with cubics (k = circle constant). */
    fun roundRect(l: Float, t: Float, r: Float, b: Float, rad: Float): PathSpec {
        val k = 0.5523f * rad
        moveTo(l + rad, t)
        lineTo(r - rad, t)
        cubicTo(r - rad + k, t, r, t + rad - k, r, t + rad)
        lineTo(r, b - rad)
        cubicTo(r, b - rad + k, r - rad + k, b, r - rad, b)
        lineTo(l + rad, b)
        cubicTo(l + rad - k, b, l, b - rad + k, l, b - rad)
        lineTo(l, t + rad)
        cubicTo(l, t + rad - k, l + rad - k, t, l + rad, t)
        close()
        return this
    }

    fun circle(cx: Float, cy: Float, r: Float): PathSpec {
        val k = 0.5523f * r
        moveTo(cx, cy - r)
        cubicTo(cx + k, cy - r, cx + r, cy - k, cx + r, cy)
        cubicTo(cx + r, cy + k, cx + k, cy + r, cx, cy + r)
        cubicTo(cx - k, cy + r, cx - r, cy + k, cx - r, cy)
        cubicTo(cx - r, cy - k, cx - k, cy - r, cx, cy - r)
        close()
        return this
    }

    /** Android VectorDrawable pathData serialisation. */
    fun toPathData(): String {
        val sb = StringBuilder()
        fun f(v: Float) = if (v == Math.round(v).toFloat()) Math.round(v).toString() else "%.2f".format(v)
        for (c in cmds) when (c) {
            is Cmd.Move -> sb.append("M${f(c.x)},${f(c.y)}")
            is Cmd.Line -> sb.append("L${f(c.x)},${f(c.y)}")
            is Cmd.Quad -> sb.append("Q${f(c.cx)},${f(c.cy)} ${f(c.x)},${f(c.y)}")
            is Cmd.Cubic -> sb.append("C${f(c.c1x)},${f(c.c1y)} ${f(c.c2x)},${f(c.c2y)} ${f(c.x)},${f(c.y)}")
            is Cmd.Close -> sb.append("Z")
        }
        return sb.toString()
    }

    companion object {
        fun of(build: PathSpec.() -> Unit): PathSpec = PathSpec().apply(build)
        fun line(x0: Float, y0: Float, x1: Float, y1: Float): PathSpec =
            PathSpec().moveTo(x0, y0).lineTo(x1, y1)
    }
}

/** Shared palette: single source of truth for app chrome and drawn scenes. */
object Palette {
    const val PRIMARY = 0xFF2F6B4F.toInt()
    const val ON_PRIMARY = 0xFFFFFFFF.toInt()
    const val PRIMARY_CONTAINER = 0xFFD6EBDD.toInt()
    const val ON_PRIMARY_CONTAINER = 0xFF123524.toInt()
    const val PRIMARY_DIM = 0xFF4E8B6C.toInt()

    const val SURFACE = 0xFFF7FAF7.toInt()
    const val SURFACE_CARD = 0xFFFFFFFF.toInt()
    const val SURFACE_HIGH = 0xFFEDF3EE.toInt()
    const val ON_SURFACE = 0xFF191C1A.toInt()
    const val ON_SURFACE_VARIANT = 0xFF5F6660.toInt()
    const val OUTLINE = 0xFFE3E9E3.toInt()

    const val ERROR = 0xFFB3261E.toInt()
    const val ERROR_CONTAINER = 0xFFFCE8E6.toInt()
    const val ON_ERROR_CONTAINER = 0xFF5F1410.toInt()
    const val WARN = 0xFF95660C.toInt()
    const val WARN_CONTAINER = 0xFFFBF0DA.toInt()
    const val INFO_CONTAINER = 0xFFE4EEF9.toInt()
    const val ON_INFO_CONTAINER = 0xFF1D3A57.toInt()

    const val SKIN = 0xFFEAC9A8.toInt()
    const val SKIN_DEEP = 0xFFD9B591.toInt()
    const val BOOT_DARK = 0xFF3A4750.toInt()
    const val BOOT_MID = 0xFF55636D.toInt()
    const val WEDGE = 0xFFEF8E2E.toInt()

    fun withAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha shl 24)
}
