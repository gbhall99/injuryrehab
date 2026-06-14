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

/**
 * Shared palette: single source of truth for app chrome and drawn scenes,
 * theme-switchable. Every consumer reads tokens at render time, so flipping
 * [dark] re-skins the entire app and all drawn scenes.
 */
object Palette {
    var dark: Boolean = false

    private fun c(light: Int, darkValue: Int) = if (dark) darkValue else light

    val PRIMARY get() = c(0xFF2F6B4F.toInt(), 0xFF8FCBA8.toInt())
    val ON_PRIMARY get() = c(0xFFFFFFFF.toInt(), 0xFF10301F.toInt())
    val PRIMARY_CONTAINER get() = c(0xFFD6EBDD.toInt(), 0xFF2C4A39.toInt())
    val ON_PRIMARY_CONTAINER get() = c(0xFF123524.toInt(), 0xFFC9E8D4.toInt())
    val PRIMARY_DIM get() = c(0xFF4E8B6C.toInt(), 0xFF6FA486.toInt())

    val SURFACE get() = c(0xFFF7FAF7.toInt(), 0xFF111412.toInt())
    val SURFACE_CARD get() = c(0xFFFFFFFF.toInt(), 0xFF1B201C.toInt())
    val SURFACE_HIGH get() = c(0xFFEDF3EE.toInt(), 0xFF262C27.toInt())
    val ON_SURFACE get() = c(0xFF191C1A.toInt(), 0xFFE1E5E0.toInt())
    val ON_SURFACE_VARIANT get() = c(0xFF5F6660.toInt(), 0xFFA3ACA4.toInt())
    val OUTLINE get() = c(0xFFE3E9E3.toInt(), 0xFF343B35.toInt())

    val ERROR get() = c(0xFFB3261E.toInt(), 0xFFF2B8B5.toInt())
    val ON_ERROR get() = c(0xFFFFFFFF.toInt(), 0xFF5C1410.toInt())
    val ERROR_CONTAINER get() = c(0xFFFCE8E6.toInt(), 0xFF4A211E.toInt())
    val ON_ERROR_CONTAINER get() = c(0xFF5F1410.toInt(), 0xFFF9DEDC.toInt())
    val WARN get() = c(0xFF95660C.toInt(), 0xFFE7C078.toInt())
    val WARN_CONTAINER get() = c(0xFFFBF0DA.toInt(), 0xFF3E3322.toInt())
    val INFO_CONTAINER get() = c(0xFFE4EEF9.toInt(), 0xFF253647.toInt())
    val ON_INFO_CONTAINER get() = c(0xFF1D3A57.toInt(), 0xFFCFE2F5.toInt())

    val DONE get() = c(0xFF2F6B4F.toInt(), 0xFF8FCBA8.toInt())
    val DONE_BG get() = c(0xFFE2F1E7.toInt(), 0xFF24402F.toInt())

    /** Hero card keeps a deep green in both themes; text tokens adapt. */
    val HERO_BG get() = c(0xFF2F6B4F.toInt(), 0xFF26473A.toInt())
    val ON_HERO get() = c(0xFFFFFFFF.toInt(), 0xFFE8F2EC.toInt())

    // scene materials: identical in both themes
    val SKIN get() = 0xFFEAC9A8.toInt()
    val SKIN_DEEP get() = 0xFFD9B591.toInt()
    val BOOT_DARK get() = c(0xFF3A4750.toInt(), 0xFF8997A1.toInt())
    val BOOT_MID get() = c(0xFF55636D.toInt(), 0xFFA8B5BE.toInt())
    val WEDGE get() = 0xFFEF8E2E.toInt()

    fun withAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha shl 24)
}
