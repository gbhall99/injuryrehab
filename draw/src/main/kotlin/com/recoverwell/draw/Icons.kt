package com.recoverwell.draw

import kotlin.math.sqrt

/**
 * The app's icon set: stroked geometric icons on a 24x24 grid (2px stroke,
 * round caps/joins - contemporary "line icon" style). Authored as code so the
 * same definitions generate Android vector drawables, render inside drawn
 * scenes, and produce PNG proofs in designlab.
 */
data class IconLayer(
    val path: PathSpec,
    /** Stroke width in 24-grid units, or null for a filled layer. */
    val stroke: Float?
)

data class Icon(val name: String, val layers: List<IconLayer>)

object Icons {

    private const val SW = 1.9f

    private fun stroke(build: PathSpec.() -> Unit) = IconLayer(PathSpec.of(build), SW)
    private fun fill(build: PathSpec.() -> Unit) = IconLayer(PathSpec.of(build), null)
    private fun dot(x: Float, y: Float, r: Float = 1.1f) = IconLayer(PathSpec.of { circle(x, y, r) }, null)

    /** Capsule outline along a segment - used for the pill icon. */
    private fun PathSpec.capsule(x0: Float, y0: Float, x1: Float, y1: Float, r: Float) {
        val dx = x1 - x0; val dy = y1 - y0
        val len = sqrt(dx * dx + dy * dy)
        val ux = dx / len; val uy = dy / len
        val nx = -uy; val ny = ux
        val k = r * 4f / 3f
        moveTo(x0 + nx * r, y0 + ny * r)
        lineTo(x1 + nx * r, y1 + ny * r)
        cubicTo(x1 + nx * r + ux * k, y1 + ny * r + uy * k,
                x1 - nx * r + ux * k, y1 - ny * r + uy * k,
                x1 - nx * r, y1 - ny * r)
        lineTo(x0 - nx * r, y0 - ny * r)
        cubicTo(x0 - nx * r - ux * k, y0 - ny * r - uy * k,
                x0 + nx * r - ux * k, y0 + ny * r - uy * k,
                x0 + nx * r, y0 + ny * r)
        close()
    }

    val today = Icon("ic_today", listOf(
        stroke { roundRect(3.5f, 5f, 20.5f, 20.5f, 3f) },
        stroke { moveTo(8f, 3f); lineTo(8f, 7f) },
        stroke { moveTo(16f, 3f); lineTo(16f, 7f) },
        stroke { moveTo(3.5f, 10f); lineTo(20.5f, 10f) },
        stroke { moveTo(8.6f, 14.8f); lineTo(11.2f, 17.2f); lineTo(15.6f, 12.8f) }
    ))

    val exercises = Icon("ic_exercises", listOf(
        stroke { moveTo(8.4f, 12f); lineTo(15.6f, 12f) },
        stroke { roundRect(5f, 7.6f, 8.4f, 16.4f, 1.4f) },
        stroke { roundRect(15.6f, 7.6f, 19f, 16.4f, 1.4f) },
        stroke { moveTo(2.6f, 9.8f); lineTo(2.6f, 14.2f) },
        stroke { moveTo(21.4f, 9.8f); lineTo(21.4f, 14.2f) }
    ))

    val progress = Icon("ic_progress", listOf(
        stroke { moveTo(2.6f, 17f); lineTo(8.6f, 10.8f); lineTo(13.2f, 15.2f); lineTo(21.4f, 7f) },
        stroke { moveTo(15.6f, 7f); lineTo(21.4f, 7f); lineTo(21.4f, 12.8f) }
    ))

    val leg = Icon("ic_leg", listOf(
        stroke {
            moveTo(10f, 3f); lineTo(10f, 9.6f)
            quadTo(10f, 11f, 11f, 12f)
            lineTo(13f, 14f)
            quadTo(13.8f, 14.8f, 13.8f, 16f)
            lineTo(13.8f, 18.6f)
        },
        stroke { moveTo(13.8f, 18.6f); lineTo(19.6f, 18.6f) },
        stroke { moveTo(13.8f, 18.6f); lineTo(8.4f, 18.6f) }
    ))

    val more = Icon("ic_more", listOf(
        stroke { moveTo(3.4f, 6.4f); lineTo(11.6f, 6.4f) },
        stroke { moveTo(17.4f, 6.4f); lineTo(20.6f, 6.4f) },
        stroke { circle(14.5f, 6.4f, 2.5f) },
        stroke { moveTo(3.4f, 12f); lineTo(5.6f, 12f) },
        stroke { moveTo(11.4f, 12f); lineTo(20.6f, 12f) },
        stroke { circle(8.5f, 12f, 2.5f) },
        stroke { moveTo(3.4f, 17.6f); lineTo(13.6f, 17.6f) },
        stroke { moveTo(19.4f, 17.6f); lineTo(20.6f, 17.6f) },
        stroke { circle(16.5f, 17.6f, 2.5f) }
    ))

    val alert = Icon("ic_alert", listOf(
        stroke {
            moveTo(12f, 3.6f)
            lineTo(21.2f, 19.4f)
            lineTo(2.8f, 19.4f)
            close()
        },
        stroke { moveTo(12f, 9.6f); lineTo(12f, 13.8f) },
        dot(12f, 16.7f)
    ))

    val pill = Icon("ic_pill", listOf(
        stroke { capsule(7.2f, 7.2f, 16.8f, 16.8f, 4.4f) },
        stroke { moveTo(8.9f, 15.1f); lineTo(15.1f, 8.9f) }
    ))

    val clock = Icon("ic_clock", listOf(
        stroke { circle(12f, 12f, 8.6f) },
        stroke { moveTo(12f, 7.2f); lineTo(12f, 12.4f); lineTo(15.4f, 14.2f) }
    ))

    val check = Icon("ic_check", listOf(
        stroke { moveTo(4.8f, 12.6f); lineTo(9.8f, 17.6f); lineTo(19.2f, 7f) }
    ))

    val chevron = Icon("ic_chevron", listOf(
        stroke { moveTo(9.6f, 5.8f); lineTo(15.8f, 12f); lineTo(9.6f, 18.2f) }
    ))

    val back = Icon("ic_back", listOf(
        stroke { moveTo(20f, 12f); lineTo(4.6f, 12f) },
        stroke { moveTo(10.8f, 5.6f); lineTo(4.4f, 12f); lineTo(10.8f, 18.4f) }
    ))

    val plus = Icon("ic_plus", listOf(
        stroke { moveTo(12f, 5.2f); lineTo(12f, 18.8f) },
        stroke { moveTo(5.2f, 12f); lineTo(18.8f, 12f) }
    ))

    val minus = Icon("ic_minus", listOf(
        stroke { moveTo(5.2f, 12f); lineTo(18.8f, 12f) }
    ))

    val play = Icon("ic_play", listOf(
        fill {
            moveTo(9f, 6.2f)
            quadTo(9f, 5.2f, 9.9f, 5.7f)
            lineTo(18.3f, 11.1f)
            quadTo(19.2f, 11.7f, 18.3f, 12.4f)
            lineTo(9.9f, 18.3f)
            quadTo(9f, 18.8f, 9f, 17.8f)
            close()
        }
    ))

    val pause = Icon("ic_pause", listOf(
        fill { roundRect(7.4f, 5.8f, 10.4f, 18.2f, 1.2f) },
        fill { roundRect(13.6f, 5.8f, 16.6f, 18.2f, 1.2f) }
    ))

    val export = Icon("ic_export", listOf(
        stroke { moveTo(12f, 3.8f); lineTo(12f, 14.4f) },
        stroke { moveTo(7.2f, 10f); lineTo(12f, 14.8f); lineTo(16.8f, 10f) },
        stroke { moveTo(4.6f, 19.4f); lineTo(19.4f, 19.4f) }
    ))

    val restore = Icon("ic_restore", listOf(
        stroke { moveTo(12f, 15.2f); lineTo(12f, 4.6f) },
        stroke { moveTo(7.2f, 9.2f); lineTo(12f, 4.4f); lineTo(16.8f, 9.2f) },
        stroke { moveTo(4.6f, 19.4f); lineTo(19.4f, 19.4f) }
    ))

    val edit = Icon("ic_edit", listOf(
        stroke {
            moveTo(4.6f, 19.4f)
            lineTo(8.2f, 18.8f)
            lineTo(18.8f, 8.2f)
            quadTo(20f, 7f, 18.8f, 5.8f)
            lineTo(18.2f, 5.2f)
            quadTo(17f, 4f, 15.8f, 5.2f)
            lineTo(5.2f, 15.8f)
            close()
        }
    ))

    val shield = Icon("ic_shield", listOf(
        stroke {
            moveTo(12f, 3.4f)
            lineTo(19f, 6.2f)
            lineTo(19f, 11.4f)
            quadTo(19f, 16.8f, 12f, 20.6f)
            quadTo(5f, 16.8f, 5f, 11.4f)
            lineTo(5f, 6.2f)
            close()
        },
        stroke { moveTo(12f, 8f); lineTo(12f, 12f) },
        dot(12f, 14.9f)
    ))

    val heart = Icon("ic_heart", listOf(
        stroke {
            moveTo(12f, 19.4f)
            cubicTo(7.2f, 15.6f, 3.4f, 12.6f, 3.4f, 8.8f)
            cubicTo(3.4f, 5.9f, 5.7f, 4f, 8f, 4f)
            cubicTo(9.7f, 4f, 11.2f, 4.9f, 12f, 6.3f)
            cubicTo(12.8f, 4.9f, 14.3f, 4f, 16f, 4f)
            cubicTo(18.3f, 4f, 20.6f, 5.9f, 20.6f, 8.8f)
            cubicTo(20.6f, 12.6f, 16.8f, 15.6f, 12f, 19.4f)
            close()
        }
    ))

    val pulse = Icon("ic_pulse", listOf(
        stroke {
            moveTo(12f, 19.4f)
            cubicTo(7.2f, 15.6f, 3.4f, 12.6f, 3.4f, 8.8f)
            cubicTo(3.4f, 5.9f, 5.7f, 4f, 8f, 4f)
            cubicTo(9.7f, 4f, 11.2f, 4.9f, 12f, 6.3f)
            cubicTo(12.8f, 4.9f, 14.3f, 4f, 16f, 4f)
            cubicTo(18.3f, 4f, 20.6f, 5.9f, 20.6f, 8.8f)
            cubicTo(20.6f, 12.6f, 16.8f, 15.6f, 12f, 19.4f)
            close()
        },
        stroke { moveTo(7.6f, 11.4f); lineTo(10f, 11.4f); lineTo(11.2f, 9.2f); lineTo(13f, 13.4f); lineTo(14.1f, 11.4f); lineTo(16.4f, 11.4f) }
    ))

    val calendar = Icon("ic_calendar", listOf(
        stroke { roundRect(3.5f, 5f, 20.5f, 20.5f, 3f) },
        stroke { moveTo(8f, 3f); lineTo(8f, 7f) },
        stroke { moveTo(16f, 3f); lineTo(16f, 7f) },
        stroke { moveTo(3.5f, 10f); lineTo(20.5f, 10f) }
    ))

    val info = Icon("ic_info", listOf(
        stroke { circle(12f, 12f, 8.6f) },
        stroke { moveTo(12f, 11.2f); lineTo(12f, 16f) },
        dot(12f, 8.2f)
    ))

    val close = Icon("ic_close", listOf(
        stroke { moveTo(6f, 6f); lineTo(18f, 18f) },
        stroke { moveTo(18f, 6f); lineTo(6f, 18f) }
    ))

    val flag = Icon("ic_flag", listOf(
        stroke { moveTo(6f, 21f); lineTo(6f, 3.6f) },
        fill {
            moveTo(6f, 4.2f)
            lineTo(17.6f, 7.6f)
            lineTo(6f, 11f)
            close()
        }
    ))

    val bell = Icon("ic_bell", listOf(
        stroke {
            moveTo(12f, 3.8f)
            cubicTo(8.9f, 3.8f, 6.8f, 6.2f, 6.8f, 9.4f)
            lineTo(6.8f, 13.2f)
            lineTo(5.2f, 16.4f)
            lineTo(18.8f, 16.4f)
            lineTo(17.2f, 13.2f)
            lineTo(17.2f, 9.4f)
            cubicTo(17.2f, 6.2f, 15.1f, 3.8f, 12f, 3.8f)
            close()
        },
        stroke { moveTo(10.2f, 19.6f); quadTo(12f, 21f, 13.8f, 19.6f) }
    ))

    val elevate = Icon("ic_elevate", listOf(
        stroke { circle(12f, 12f, 8.6f) },
        stroke { moveTo(8.2f, 12.6f); lineTo(12f, 8.8f); lineTo(15.8f, 12.6f) },
        stroke { moveTo(12f, 8.8f); lineTo(12f, 15.8f) }
    ))

    val boot = Icon("ic_boot", listOf(
        stroke {
            moveTo(8.6f, 3.6f)
            lineTo(14.2f, 3.6f)
            lineTo(14.2f, 11.4f)
            quadTo(14.2f, 12.8f, 15.5f, 13.4f)
            lineTo(18.6f, 14.9f)
            quadTo(20f, 15.6f, 20f, 17f)
            lineTo(20f, 18.4f)
            lineTo(8.6f, 18.4f)
            close()
        },
        stroke { moveTo(8.6f, 7.4f); lineTo(14.2f, 7.4f) },
        stroke { moveTo(8.6f, 11.2f); lineTo(14.2f, 11.2f) },
        stroke { moveTo(4f, 18.4f); lineTo(8.6f, 18.4f) }
    ))

    val all: List<Icon> = listOf(
        today, exercises, progress, leg, more, alert, pill, clock, check,
        chevron, back, plus, minus, play, pause, export, restore, edit,
        shield, heart, pulse, calendar, info, close, flag, bell, elevate, boot
    )

    fun byName(name: String): Icon = all.first { it.name == name }

    /** Draws an icon into any Sketch at the given box. */
    fun draw(s: Sketch, icon: Icon, x: Float, y: Float, sizePx: Float, color: Int) {
        val scale = sizePx / 24f
        s.save()
        s.translate(x, y)
        for (layer in icon.layers) {
            val scaled = scalePath(layer.path, scale)
            if (layer.stroke != null) s.stroke(scaled, color, layer.stroke * scale)
            else s.fill(scaled, color)
        }
        s.restore()
    }

    private fun scalePath(p: PathSpec, k: Float): PathSpec {
        val out = PathSpec()
        for (c in p.cmds) when (c) {
            is PathSpec.Cmd.Move -> out.moveTo(c.x * k, c.y * k)
            is PathSpec.Cmd.Line -> out.lineTo(c.x * k, c.y * k)
            is PathSpec.Cmd.Quad -> out.quadTo(c.cx * k, c.cy * k, c.x * k, c.y * k)
            is PathSpec.Cmd.Cubic -> out.cubicTo(c.c1x * k, c.c1y * k, c.c2x * k, c.c2y * k, c.x * k, c.y * k)
            is PathSpec.Cmd.Close -> out.close()
        }
        return out
    }

    /** Android VectorDrawable XML for one icon (black; tinted at the use site). */
    fun vectorXml(icon: Icon): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        sb.append("<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n")
        sb.append("    android:width=\"24dp\"\n    android:height=\"24dp\"\n")
        sb.append("    android:viewportWidth=\"24\"\n    android:viewportHeight=\"24\">\n")
        for (layer in icon.layers) {
            sb.append("    <path\n")
            sb.append("        android:pathData=\"${layer.path.toPathData()}\"\n")
            if (layer.stroke != null) {
                sb.append("        android:strokeColor=\"#FF000000\"\n")
                sb.append("        android:strokeWidth=\"${layer.stroke}\"\n")
                sb.append("        android:strokeLineCap=\"round\"\n")
                sb.append("        android:strokeLineJoin=\"round\"\n")
                sb.append("        android:fillColor=\"#00000000\" />\n")
            } else {
                sb.append("        android:fillColor=\"#FF000000\" />\n")
            }
        }
        sb.append("</vector>\n")
        return sb.toString()
    }
}
