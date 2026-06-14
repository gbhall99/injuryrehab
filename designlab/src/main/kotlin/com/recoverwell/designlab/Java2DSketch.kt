package com.recoverwell.designlab

import com.recoverwell.draw.PathSpec
import com.recoverwell.draw.Sketch
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.GradientPaint
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Arc2D
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.util.ArrayDeque

/** Java2D backend for Sketch: pixel-accurate PNG proofs of every drawn surface. */
class Java2DSketch(val image: BufferedImage) : Sketch {

    private val g: Graphics2D = image.createGraphics().apply {
        setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    }
    private val stack = ArrayDeque<AffineTransform>()

    override val width: Float get() = image.width.toFloat()
    override val height: Float get() = image.height.toFloat()

    private fun color(c: Int) = Color(c, true)

    private fun toPath(p: PathSpec): Path2D.Float {
        val out = Path2D.Float()
        for (c in p.cmds) when (c) {
            is PathSpec.Cmd.Move -> out.moveTo(c.x, c.y)
            is PathSpec.Cmd.Line -> out.lineTo(c.x, c.y)
            is PathSpec.Cmd.Quad -> out.quadTo(c.cx, c.cy, c.x, c.y)
            is PathSpec.Cmd.Cubic -> out.curveTo(c.c1x, c.c1y, c.c2x, c.c2y, c.x, c.y)
            is PathSpec.Cmd.Close -> out.closePath()
        }
        return out
    }

    override fun save() { stack.push(g.transform) }

    /** designlab-only hook: fill an arbitrary Java2D shape (parsed icon glyphs). */
    fun fillPath2D(shape: java.awt.Shape, color: Int) {
        g.color = color(color)
        g.fill(shape)
    }
    override fun restore() { if (stack.isNotEmpty()) g.transform = stack.pop() }
    override fun translate(dx: Float, dy: Float) { g.translate(dx.toDouble(), dy.toDouble()) }
    override fun rotate(degrees: Float, px: Float, py: Float) {
        g.rotate(Math.toRadians(degrees.toDouble()), px.toDouble(), py.toDouble())
    }

    override fun clear(color: Int) {
        val t = g.transform
        g.transform = AffineTransform()
        g.color = color(color)
        g.fillRect(0, 0, image.width, image.height)
        g.transform = t
    }

    override fun fill(path: PathSpec, color: Int) {
        g.color = color(color)
        g.fill(toPath(path))
    }

    override fun fillGradient(path: PathSpec, x0: Float, y0: Float, x1: Float, y1: Float, c0: Int, c1: Int) {
        g.paint = GradientPaint(x0, y0, color(c0), x1, y1, color(c1))
        g.fill(toPath(path))
        g.paint = null
        g.color = Color.BLACK
    }

    override fun stroke(path: PathSpec, color: Int, strokeWidth: Float, roundCaps: Boolean, dash: FloatArray?) {
        g.color = color(color)
        val cap = if (roundCaps) BasicStroke.CAP_ROUND else BasicStroke.CAP_BUTT
        g.stroke = if (dash != null)
            BasicStroke(strokeWidth, cap, BasicStroke.JOIN_ROUND, 4f, dash, 0f)
        else BasicStroke(strokeWidth, cap, BasicStroke.JOIN_ROUND)
        g.draw(toPath(path))
    }

    override fun circle(cx: Float, cy: Float, r: Float, color: Int) {
        g.color = color(color)
        g.fillOval((cx - r).toInt(), (cy - r).toInt(), (r * 2).toInt(), (r * 2).toInt())
    }

    override fun circleStroke(cx: Float, cy: Float, r: Float, color: Int, strokeWidth: Float) {
        g.color = color(color)
        g.stroke = BasicStroke(strokeWidth)
        g.drawOval((cx - r).toInt(), (cy - r).toInt(), (r * 2).toInt(), (r * 2).toInt())
    }

    override fun roundRect(l: Float, t: Float, r: Float, b: Float, radius: Float, color: Int) {
        g.color = color(color)
        g.fill(RoundRectangle2D.Float(l, t, r - l, b - t, radius * 2, radius * 2))
    }

    override fun arc(cx: Float, cy: Float, radius: Float, startDeg: Float, sweepDeg: Float,
                     color: Int, strokeWidth: Float, roundCaps: Boolean) {
        g.color = color(color)
        g.stroke = BasicStroke(strokeWidth,
            if (roundCaps) BasicStroke.CAP_ROUND else BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND)
        // Java2D angles are counter-clockwise; Sketch contract is clockwise
        g.draw(Arc2D.Float(cx - radius, cy - radius, radius * 2, radius * 2,
            -startDeg, -sweepDeg, Arc2D.OPEN))
    }

    override fun text(s: String, x: Float, y: Float, sizePx: Float, color: Int, medium: Boolean, centered: Boolean) {
        g.color = color(color)
        g.font = Font("DejaVu Sans", if (medium) Font.BOLD else Font.PLAIN, sizePx.toInt())
        val tx = if (centered) x - g.fontMetrics.stringWidth(s) / 2f else x
        g.drawString(s, tx, y)
    }

    override fun measureText(s: String, sizePx: Float, medium: Boolean): Float {
        g.font = Font("DejaVu Sans", if (medium) Font.BOLD else Font.PLAIN, sizePx.toInt())
        return g.fontMetrics.stringWidth(s).toFloat()
    }
}
