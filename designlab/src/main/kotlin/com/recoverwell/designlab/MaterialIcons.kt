package com.recoverwell.designlab

import com.recoverwell.draw.Sketch
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import java.io.File

/**
 * Loads the official Material Symbols vector drawables that the app ships
 * (app/src/main/res/drawable/ic_*.xml) so previews render the exact same
 * established icon set - no hand-authored geometry anywhere.
 */
object MaterialIcons {

    data class Glyph(val name: String, val paths: List<Path2D.Float>, val viewport: Float)

    private val cache = HashMap<String, Glyph>()
    lateinit var resDir: File

    fun glyph(name: String): Glyph = cache.getOrPut(name) {
        val xml = File(resDir, "$name.xml").readText()
        val viewport = Regex("android:viewportWidth=\"([0-9.]+)\"").find(xml)!!.groupValues[1].toFloat()
        val paths = Regex("android:pathData=\"([^\"]+)\"").findAll(xml)
            .map { parsePath(it.groupValues[1]) }.toList()
        Glyph(name, paths, viewport)
    }

    fun names(): List<String> =
        resDir.listFiles { f -> f.name.startsWith("ic_") && !f.name.contains("launcher") }!!
            .map { it.name.removeSuffix(".xml") }.sorted()

    /** Draw into a Sketch backed by Java2D (designlab previews only). */
    fun draw(s: Java2DSketch, name: String, x: Float, y: Float, sizePx: Float, color: Int) {
        val g = glyph(name)
        val k = sizePx / g.viewport
        for (p in g.paths) {
            val t = AffineTransform()
            t.translate(x.toDouble(), y.toDouble())
            t.scale(k.toDouble(), k.toDouble())
            s.fillPath2D(p.createTransformedShape(t), color)
        }
    }

    /** Minimal SVG/VectorDrawable path-data parser (no arcs in Material Symbols). */
    fun parsePath(d: String): Path2D.Float {
        val out = Path2D.Float(Path2D.WIND_NON_ZERO)
        val tokens = Regex("[MmLlHhVvCcSsQqTtZz]|-?\\d*\\.?\\d+(?:[eE]-?\\d+)?").findAll(d)
            .map { it.value }.toList()
        var i = 0
        var cmd = ' '
        var cx = 0f; var cy = 0f          // current point
        var lqx = 0f; var lqy = 0f        // last quad control (for T)
        var lcx = 0f; var lcy = 0f        // last cubic control (for S)
        var sx = 0f; var sy = 0f          // subpath start
        fun num(): Float = tokens[i++].toFloat()
        while (i < tokens.size) {
            val t = tokens[i]
            if (t.length == 1 && t[0].isLetter()) { cmd = t[0]; i++ }
            val rel = cmd.isLowerCase()
            when (cmd.uppercaseChar()) {
                'M' -> {
                    val x = num() + if (rel) cx else 0f
                    val y = num() + if (rel) cy else 0f
                    out.moveTo(x, y); cx = x; cy = y; sx = x; sy = y
                    cmd = if (rel) 'l' else 'L'  // subsequent pairs are implicit lineto
                }
                'L' -> {
                    val x = num() + if (rel) cx else 0f
                    val y = num() + if (rel) cy else 0f
                    out.lineTo(x, y); cx = x; cy = y
                }
                'H' -> { val x = num() + if (rel) cx else 0f; out.lineTo(x, cy); cx = x }
                'V' -> { val y = num() + if (rel) cy else 0f; out.lineTo(cx, y); cy = y }
                'Q' -> {
                    val qx = num() + if (rel) cx else 0f
                    val qy = num() + if (rel) cy else 0f
                    val x = num() + if (rel) cx else 0f
                    val y = num() + if (rel) cy else 0f
                    out.quadTo(qx, qy, x, y); lqx = qx; lqy = qy; cx = x; cy = y
                }
                'T' -> {
                    val qx = 2 * cx - lqx
                    val qy = 2 * cy - lqy
                    val x = num() + if (rel) cx else 0f
                    val y = num() + if (rel) cy else 0f
                    out.quadTo(qx, qy, x, y); lqx = qx; lqy = qy; cx = x; cy = y
                }
                'C' -> {
                    val c1x = num() + if (rel) cx else 0f
                    val c1y = num() + if (rel) cy else 0f
                    val c2x = num() + if (rel) cx else 0f
                    val c2y = num() + if (rel) cy else 0f
                    val x = num() + if (rel) cx else 0f
                    val y = num() + if (rel) cy else 0f
                    out.curveTo(c1x, c1y, c2x, c2y, x, y); lcx = c2x; lcy = c2y; cx = x; cy = y
                }
                'S' -> {
                    val c1x = 2 * cx - lcx
                    val c1y = 2 * cy - lcy
                    val c2x = num() + if (rel) cx else 0f
                    val c2y = num() + if (rel) cy else 0f
                    val x = num() + if (rel) cx else 0f
                    val y = num() + if (rel) cy else 0f
                    out.curveTo(c1x, c1y, c2x, c2y, x, y); lcx = c2x; lcy = c2y; cx = x; cy = y
                }
                'Z' -> { out.closePath(); cx = sx; cy = sy }
                else -> error("Unsupported path command '$cmd' (arcs are not used by Material Symbols)")
            }
        }
        return out
    }
}
