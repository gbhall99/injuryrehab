package com.recoverwell.draw

/** Progress ring with a soft track - the hero element of the Today screen. */
object RingScene {
    fun render(s: Sketch, fraction: Float, strokeWidth: Float, trackColor: Int = Palette.withAlpha(0xFFFFFFFF.toInt(), 0x59), color: Int = 0xFFFFFFFF.toInt()) {
        val cx = s.width / 2f
        val cy = s.height / 2f
        val r = minOf(cx, cy) - strokeWidth / 2f - 1f
        s.arc(cx, cy, r, -90f, 360f, trackColor, strokeWidth)
        val sweep = (fraction.coerceIn(0f, 1f)) * 360f
        if (sweep > 0.5f) s.arc(cx, cy, r, -90f, sweep, color, strokeWidth)
    }
}

/** Trend chart: soft grid, gradient area fill, emphasised latest point. */
object ChartScene {

    data class Data(
        val points: List<Pair<Float, Float>>,   // x = day index, y = value
        val avg: List<Pair<Float, Float>>,
        val min: Float,
        val max: Float,
        val startLabel: String,
        val endLabel: String,
        val emptyHint: String
    )

    fun render(s: Sketch, d: Data) {
        val padL = 44f
        val padR = 30f
        val padT = 26f
        val padB = 46f
        val w = s.width - padL - padR
        val h = s.height - padT - padB

        if (d.points.isEmpty()) {
            s.text(d.emptyHint, s.width / 2f, s.height / 2f, 15f * (s.height / 220f),
                Palette.ON_SURFACE_VARIANT, centered = true)
            return
        }

        val span = (d.max - d.min).takeIf { it > 0f } ?: 1f
        val xMin = d.points.first().first
        val xSpan = (d.points.last().first - xMin).coerceAtLeast(1f)
        fun X(x: Float) = padL + w * (x - xMin) / xSpan
        fun Y(v: Float) = padT + h - h * ((v - d.min) / span)

        // grid + labels
        val labelSize = 11f * (s.height / 220f)
        for (i in 0..2) {
            val v = d.min + span * i / 2f
            val y = Y(v)
            s.stroke(PathSpec.line(padL, y, padL + w, y), Palette.OUTLINE, 1.6f)
            s.text(trim(v), padL - 8f - s.measureText(trim(v), labelSize), y + labelSize * 0.35f,
                labelSize, Palette.ON_SURFACE_VARIANT)
        }

        fun polyline(pts: List<Pair<Float, Float>>): PathSpec {
            val p = PathSpec()
            pts.forEachIndexed { i, (x, v) -> if (i == 0) p.moveTo(X(x), Y(v)) else p.lineTo(X(x), Y(v)) }
            return p
        }

        if (d.points.size > 1) {
            // area fill
            val area = PathSpec()
            d.points.forEachIndexed { i, (x, v) ->
                if (i == 0) area.moveTo(X(x), Y(v)) else area.lineTo(X(x), Y(v))
            }
            area.lineTo(X(d.points.last().first), padT + h)
            area.lineTo(X(d.points.first().first), padT + h)
            area.close()
            s.fillGradient(area, 0f, padT, 0f, padT + h,
                Palette.withAlpha(Palette.PRIMARY, 0x33), Palette.withAlpha(Palette.PRIMARY, 0x00))

            s.stroke(polyline(d.avg), Palette.withAlpha(Palette.PRIMARY_DIM, 0x99), 2.6f,
                dash = floatArrayOf(10f, 9f))
            s.stroke(polyline(d.points), Palette.PRIMARY, 4f)
        }

        for ((x, v) in d.points) s.circle(X(x), Y(v), 4.4f, Palette.PRIMARY)
        // emphasise latest
        val (lx, lv) = d.points.last()
        s.circle(X(lx), Y(lv), 8.6f, Palette.SURFACE_CARD)
        s.circleStroke(X(lx), Y(lv), 8.6f, Palette.PRIMARY, 3.4f)

        s.text(d.startLabel, padL, s.height - 14f, labelSize, Palette.ON_SURFACE_VARIANT)
        val ew = s.measureText(d.endLabel, labelSize)
        s.text(d.endLabel, padL + w - ew, s.height - 14f, labelSize, Palette.ON_SURFACE_VARIANT)
    }

    private fun trim(v: Float): String =
        if (v == Math.round(v).toFloat()) Math.round(v).toString() else "%.1f".format(v)
}

/**
 * Digital-twin lower leg, side view. The boot stands upright on its rocker
 * sole (as a real wedged boot does); plantarflexion shows as the heel riding
 * on the orange wedge stack inside. The Achilles is highlighted with a
 * healing-stage colour and a marker at the typical rupture site (lower third).
 * Labels live in the surrounding layout, not in the scene.
 */
object BodyScene {

    fun tendonColor(phase: Int): Int = when (phase) {
        1 -> 0xFFD25C50.toInt()
        2 -> 0xFFE08A3C.toInt()
        3 -> 0xFFD9B13B.toInt()
        4 -> 0xFF7FA84C.toInt()
        else -> 0xFF3E8E5E.toInt()
    }

    /**
     * @param heelFraction 0..1, how raised the heel is (1 = full equinus) -
     *   works for any unit (wedges or degrees), computed by the caller.
     * @param wedgeStackCount orange wedges to draw under the heel; 0 for
     *   degree-based boots (which lift the heel without a wedge stack).
     */
    fun render(s: Sketch, phase: Int, heelFraction: Float, wedgeStackCount: Int, mirrored: Boolean) {
        val w = s.width
        val h = s.height
        val groundY = h * 0.90f
        val inBoot = phase <= 2
        val nWedges = if (inBoot) wedgeStackCount.coerceIn(0, 6) else 0

        val cx = w * 0.42f               // shank centreline
        val kneeY = h * 0.06f
        // barefoot rests on the ground; in the boot the foot sits on the sole
        val soleTopY = if (inBoot) groundY - h * 0.040f else groundY
        val wedgeH = h * 0.016f
        val maxLift = h * 0.096f
        val heelLift = if (inBoot) heelFraction.coerceIn(0f, 1f) * maxLift else 0f
        val ankleY = (if (inBoot) soleTopY - heelLift else soleTopY) - h * 0.092f

        // contact shadow
        s.fill(PathSpec.of {
            moveTo(cx - w * 0.24f, groundY + 6f)
            cubicTo(cx - w * 0.06f, groundY - 8f, cx + w * 0.34f, groundY - 8f, cx + w * 0.46f, groundY + 6f)
            close()
        }, Palette.withAlpha(0xFF1A1C1A.toInt(), 0x10))

        // ---- leg silhouette: shin fairly straight, calf bulge behind ----
        val frontX = cx + w * 0.055f
        val leg = PathSpec.of {
            moveTo(frontX + w * 0.01f, kneeY)
            cubicTo(frontX + w * 0.005f, h * 0.30f, frontX - w * 0.018f, h * 0.48f, frontX - w * 0.022f, ankleY)
            lineTo(cx - w * 0.052f, ankleY)
            cubicTo(cx - w * 0.082f, h * 0.50f, cx - w * 0.118f, h * 0.38f, cx - w * 0.112f, h * 0.26f)
            cubicTo(cx - w * 0.108f, h * 0.15f, cx - w * 0.094f, kneeY, cx - w * 0.085f, kneeY)
            close()
        }
        s.fillGradient(leg, cx - w * 0.12f, 0f, frontX, 0f, Palette.SKIN_DEEP, Palette.SKIN)

        // ---- foot: toes on the sole, heel riding the wedge stack (or flat) ----
        val heelX = cx - w * 0.055f
        val footTopY = ankleY
        val heelUnderY = soleTopY - heelLift
        val foot = PathSpec.of {
            moveTo(frontX - w * 0.022f, footTopY + h * 0.018f)
            cubicTo(cx + w * 0.10f, footTopY + h * 0.046f, cx + w * 0.20f, soleTopY - h * 0.040f,
                cx + w * 0.290f, soleTopY - h * 0.026f)         // instep hugging the foot
            quadTo(cx + w * 0.322f, soleTopY - h * 0.014f, cx + w * 0.308f, soleTopY)  // toe cap
            lineTo(cx + w * 0.05f, soleTopY)                     // ball of foot on the sole
            lineTo(heelX + w * 0.035f, heelUnderY)               // arch back to the heel
            quadTo(heelX - w * 0.034f, heelUnderY + h * 0.002f, heelX - w * 0.028f, heelUnderY - h * 0.045f) // heel curve
            lineTo(heelX - w * 0.020f, footTopY + h * 0.008f)
            close()
        }
        s.fillGradient(foot, 0f, footTopY, 0f, soleTopY, Palette.SKIN, Palette.SKIN_DEEP)

        // ---- Achilles: glow + cord; rupture marker in the lower third ----
        val tColor = tendonColor(phase)
        val tendon = PathSpec.of {
            moveTo(cx - w * 0.092f, h * 0.34f)
            cubicTo(cx - w * 0.072f, h * 0.50f, cx - w * 0.062f, h * 0.60f, heelX - w * 0.014f, footTopY + h * 0.030f)
        }
        s.stroke(tendon, Palette.withAlpha(tColor, 0x3D), w * 0.050f)
        s.stroke(tendon, tColor, w * 0.020f)
        val siteY = footTopY - h * 0.055f
        val siteX = cx - w * 0.066f
        s.circle(siteX, siteY, w * 0.030f, Palette.withAlpha(tColor, 0x4D))
        s.circle(siteX, siteY, w * 0.015f, tColor)

        if (inBoot) {
            // wedge stack under the heel (inside the boot)
            for (i in 0 until nWedges) {
                val t = soleTopY - (i + 1) * wedgeH
                s.roundRect(heelX - w * 0.045f, t + 1.5f, heelX + w * 0.105f, t + wedgeH, 2.5f, Palette.WEDGE)
            }
            // upright shell: shaft hugging the shank + foot box to the toes
            val shaftL = cx - w * 0.155f
            val shaftR = frontX + w * 0.035f
            val shell = PathSpec.of {
                moveTo(shaftL, h * 0.22f)
                lineTo(shaftR, h * 0.22f)
                lineTo(shaftR, soleTopY - h * 0.085f)
                quadTo(shaftR + w * 0.01f, soleTopY - h * 0.055f, cx + w * 0.20f, soleTopY - h * 0.050f) // over instep
                lineTo(cx + w * 0.335f, soleTopY - h * 0.042f)
                quadTo(cx + w * 0.385f, soleTopY - h * 0.030f, cx + w * 0.385f, soleTopY)               // toe box
                lineTo(shaftL, soleTopY)
                close()
            }
            s.fill(shell, Palette.withAlpha(Palette.BOOT_DARK, 0x38))
            s.stroke(shell, Palette.withAlpha(Palette.BOOT_DARK, 0xC6), 3.2f)
            // straps
            for (i in 0..3) {
                val y = h * (0.26f + 0.105f * i)
                s.roundRect(shaftL - w * 0.012f, y, shaftR + w * 0.012f, y + h * 0.020f, 4f,
                    Palette.withAlpha(Palette.BOOT_MID, 0xD9))
            }
            // instep strap
            s.roundRect(cx + w * 0.04f, soleTopY - h * 0.075f, cx + w * 0.21f, soleTopY - h * 0.057f, 4f,
                Palette.withAlpha(Palette.BOOT_MID, 0xD9))
            // rocker sole
            val sole = PathSpec.of {
                moveTo(shaftL, soleTopY)
                lineTo(cx + w * 0.385f, soleTopY)
                quadTo(cx + w * 0.395f, soleTopY, cx + w * 0.385f, soleTopY + h * 0.016f)
                quadTo(cx + w * 0.30f, groundY, cx + w * 0.10f, groundY)
                lineTo(cx - w * 0.06f, groundY)
                quadTo(shaftL - w * 0.012f, groundY, shaftL, soleTopY + h * 0.012f)
                close()
            }
            s.fill(sole, Palette.BOOT_DARK)
        }

        // ground line
        s.stroke(PathSpec.line(w * 0.05f, groundY + h * 0.030f, w * 0.95f, groundY + h * 0.030f),
            Palette.OUTLINE, 3f)
    }
}
