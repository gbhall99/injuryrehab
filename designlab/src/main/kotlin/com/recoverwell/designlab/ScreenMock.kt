package com.recoverwell.designlab

import com.recoverwell.draw.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File

/**
 * Faithful full-screen previews of every user-journey screen, painted with
 * the same Palette tokens, radii, icon set and proportions the Android app
 * uses. These are the surfaces the UI/journey review scores against - so a
 * fix in the review can be seen here before it ships.
 */
class ScreenMock(val w: Int = 412, val h: Int = 892) {
    val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val s = Java2DSketch(img)
    var y = 0f
    val pad = 20f
    val W = w.toFloat()

    fun save(out: File, name: String) {
        ImageIO.write(img, "png", File(out, "$name.png"))
        println("rendered $name.png")
    }

    fun fill(bg: Int = Palette.SURFACE) = s.clear(bg)

    fun statusBar() {
        // subtle time + indicators line, like a real device
        s.text("9:41", pad, 30f, 14f, Palette.ON_SURFACE, medium = true)
        // signal/wifi/battery as small rounded marks
        s.roundRect(W - 64f, 19f, W - 50f, 30f, 2f, Palette.ON_SURFACE)
        s.roundRect(W - 46f, 19f, W - 32f, 30f, 2f, Palette.ON_SURFACE)
        s.roundRect(W - 28f, 18f, W - 12f, 30f, 3f, Palette.ON_SURFACE)
    }

    fun appBar(title: String, withAlert: Boolean = true) {
        s.text(title, pad, 76f, 27f, Palette.ON_SURFACE, medium = true)
        if (withAlert) {
            s.circle(W - 36f, 66f, 22f, Palette.ERROR_CONTAINER)
            MaterialIcons.draw(s, "ic_alert", W - 47f, 55f, 22f, Palette.ERROR)
        }
        y = 100f
    }

    fun backBar(title: String) {
        s.circle(pad + 22f, 70f, 22f, Palette.SURFACE_HIGH)
        MaterialIcons.draw(s, "ic_back", pad + 11f, 59f, 22f, Palette.ON_SURFACE)
        s.text(title, pad + 54f, 78f, 21f, Palette.ON_SURFACE, medium = true)
        y = 104f
    }

    fun section(label: String) {
        y += 18f
        s.text(label.uppercase(), pad + 4f, y + 12f, 12f, Palette.ON_SURFACE_VARIANT, medium = true)
        y += 24f
    }

    fun card(height: Float, bg: Int = Palette.SURFACE_CARD): Float {
        val top = y + 6f
        // soft shadow
        s.roundRect(pad + 2f, top + 3f, W - pad + 2f, top + height + 3f, 18f,
            Palette.withAlpha(0xFF1A1C1A.toInt(), 0x0E))
        s.roundRect(pad, top, W - pad, top + height, 18f, bg)
        y = top + height + 6f
        return top
    }

    fun bottomNav(active: Int) {
        val navT = h - 86f
        s.roundRect(0f, navT, W, h.toFloat(), 0f, Palette.SURFACE_CARD)
        s.stroke(PathSpec.line(0f, navT, W, navT), Palette.OUTLINE, 1.2f)
        val tabs = listOf("Today" to "ic_today", "Exercises" to "ic_exercises",
            "Progress" to "ic_progress", "My leg" to "ic_leg", "Settings" to "ic_more")
        tabs.forEachIndexed { i, (label, icon) ->
            val cx = W * (i + 0.5f) / 5f
            val on = i == active
            if (on) s.roundRect(cx - 28f, navT + 12f, cx + 28f, navT + 42f, 16f, Palette.PRIMARY_CONTAINER)
            MaterialIcons.draw(s, icon, cx - 11f, navT + 16f, 22f,
                if (on) Palette.ON_PRIMARY_CONTAINER else Palette.ON_SURFACE_VARIANT)
            s.text(label, cx, navT + 62f, 11f,
                if (on) Palette.ON_SURFACE else Palette.ON_SURFACE_VARIANT, medium = on, centered = true)
        }
    }

    fun disclaimerStrip() {
        val t = h - 86f - 26f
        s.text("Supports - never replaces - your physio and consultant",
            W / 2f, t + 16f, 12f, Palette.ON_SURFACE_VARIANT, centered = true)
    }

    fun primaryButton(label: String, top: Float = y + 8f): Float {
        s.roundRect(pad, top, W - pad, top + 50f, 25f, Palette.PRIMARY)
        s.text(label, W / 2f, top + 31f, 15.5f, Palette.ON_PRIMARY, medium = true, centered = true)
        y = top + 50f + 8f
        return top
    }

    fun tonalButton(label: String, top: Float = y + 8f): Float {
        s.roundRect(pad, top, W - pad, top + 50f, 25f, Palette.PRIMARY_CONTAINER)
        s.text(label, W / 2f, top + 31f, 15.5f, Palette.ON_PRIMARY_CONTAINER, medium = true, centered = true)
        y = top + 50f + 8f
        return top
    }

    fun checkRow(title: String, sub: String, time: String?, done: Boolean) {
        val top = y + 4f; val ht = if (sub.isEmpty()) 56f else 64f
        s.roundRect(pad, top, W - pad, top + ht, 18f, if (done) Palette.DONE_BG else Palette.SURFACE_CARD)
        val cy = top + ht / 2f
        if (done) {
            s.circle(pad + 28f, cy, 13f, Palette.DONE)
            MaterialIcons.draw(s, "ic_check", pad + 20f, cy - 8f, 16f, Palette.ON_PRIMARY)
        } else {
            s.circleStroke(pad + 28f, cy, 13f, Palette.withAlpha(Palette.ON_SURFACE_VARIANT, 0x66), 2.4f)
        }
        val tx = pad + 52f
        if (sub.isEmpty()) {
            s.text(title, tx, cy + 5f, 15f, if (done) Palette.ON_SURFACE_VARIANT else Palette.ON_SURFACE, medium = true)
        } else {
            s.text(title, tx, cy - 4f, 15f, if (done) Palette.ON_SURFACE_VARIANT else Palette.ON_SURFACE, medium = true)
            s.text(sub, tx, cy + 16f, 12.5f, Palette.ON_SURFACE_VARIANT)
        }
        if (time != null) {
            val tw = s.measureText(time, 12.5f, medium = true)
            s.roundRect(W - pad - tw - 22f, cy - 13f, W - pad - 8f, cy + 13f, 12f, Palette.SURFACE_HIGH)
            s.text(time, W - pad - tw - 11f, cy + 4.5f, 12.5f, Palette.ON_SURFACE_VARIANT, medium = true)
        }
        y = top + ht + 8f
    }

    fun listRow(icon: String, title: String, sub: String, chevron: Boolean = true,
                tint: Int = Palette.PRIMARY, badgeBg: Int = Palette.PRIMARY_CONTAINER) {
        val top = y + 5f; val ht = 64f
        s.roundRect(pad + 2f, top + 3f, W - pad + 2f, top + ht + 3f, 18f, Palette.withAlpha(0xFF1A1C1A.toInt(), 0x0C))
        s.roundRect(pad, top, W - pad, top + ht, 18f, Palette.SURFACE_CARD)
        val cy = top + ht / 2f
        s.circle(pad + 34f, cy, 20f, badgeBg)
        MaterialIcons.draw(s, icon, pad + 23f, cy - 11f, 22f, tint)
        s.text(title, pad + 66f, cy - 3f, 15f, Palette.ON_SURFACE, medium = true)
        s.text(sub, pad + 66f, cy + 16f, 12.5f, Palette.ON_SURFACE_VARIANT)
        if (chevron) MaterialIcons.draw(s, "ic_chevron", W - pad - 26f, cy - 9f, 18f, Palette.ON_SURFACE_VARIANT)
        y = top + ht + 10f
    }

    fun chips(options: List<String>, sel: Int) {
        val top = y + 4f
        val n = options.size
        val gap = 6f
        val cw = (W - 2 * pad - (n - 1) * gap) / n
        options.forEachIndexed { i, o ->
            val l = pad + i * (cw + gap)
            val on = i == sel
            s.roundRect(l, top, l + cw, top + 44f, 16f, if (on) Palette.PRIMARY_CONTAINER else Palette.SURFACE_HIGH)
            s.text(o, l + cw / 2f, top + 28f, 13f,
                if (on) Palette.ON_PRIMARY_CONTAINER else Palette.ON_SURFACE_VARIANT, medium = true, centered = true)
        }
        y = top + 44f + 6f
    }

    fun textLine(t: String, size: Float = 14.5f, color: Int = Palette.ON_SURFACE, bold: Boolean = false, gap: Float = 6f) {
        y += size + 2f
        s.text(t, pad, y, size, color, medium = bold)
        y += gap
    }
}
