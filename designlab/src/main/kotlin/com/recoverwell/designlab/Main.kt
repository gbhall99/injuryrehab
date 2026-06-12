package com.recoverwell.designlab

import com.recoverwell.draw.*
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Design proof renderer: outputs PNGs of every drawn surface for review, and
 * regenerates the app's vector drawable XMLs from the icon source of truth.
 *
 *   gradle :designlab:render
 */
fun main(args: Array<String>) {
    val root = File(args.firstOrNull() ?: ".")
    val out = File(root, "designlab/out").apply { mkdirs() }

    fun png(name: String, w: Int, h: Int, bg: Int = Palette.SURFACE, draw: (Sketch) -> Unit) {
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val s = Java2DSketch(img)
        s.clear(bg)
        draw(s)
        ImageIO.write(img, "png", File(out, "$name.png"))
        println("rendered $name.png")
    }

    // ---- icon contact sheet -------------------------------------------------
    run {
        val cell = 96
        val cols = 7
        val rows = (Icons.all.size + cols - 1) / cols
        png("icons", cols * cell, rows * (cell + 26), Palette.SURFACE_CARD) { s ->
            Icons.all.forEachIndexed { i, icon ->
                val cx = (i % cols) * cell
                val cy = (i / cols) * (cell + 26)
                Icons.draw(s, icon, cx + 24f, cy + 24f, 48f, Palette.ON_SURFACE)
                s.text(icon.name.removePrefix("ic_"), cx + cell / 2f, cy + cell + 8f, 12f,
                    Palette.ON_SURFACE_VARIANT, centered = true)
            }
        }
    }

    // ---- progress ring ------------------------------------------------------
    png("ring", 240, 240, Palette.PRIMARY) { s ->
        RingScene.render(s, 0.34f, 16f)
    }

    // ---- chart with sample data --------------------------------------------
    run {
        val pts = listOf(0f to 7f, 1f to 6f, 2f to 6f, 3f to 5f, 4f to 5.5f, 5f to 4f, 6f to 4f,
            7f to 3.5f, 8f to 3f, 9f to 3.5f, 10f to 2.5f)
        val avg = pts.mapIndexed { i, (x, _) ->
            val from = (i - 6).coerceAtLeast(0)
            x to pts.subList(from, i + 1).map { it.second }.average().toFloat()
        }
        png("chart", 720, 320, Palette.SURFACE_CARD) { s ->
            ChartScene.render(s, ChartScene.Data(pts, avg, 0f, 10f, "2 Jun", "12 Jun", "No entries yet"))
        }
        png("chart_empty", 720, 320, Palette.SURFACE_CARD) { s ->
            ChartScene.render(s, ChartScene.Data(emptyList(), emptyList(), 0f, 10f, "", "", "No entries yet - add today's log"))
        }
    }

    // ---- body model across phases -------------------------------------------
    for (phase in 1..5) {
        val wedges = when (phase) { 1 -> 5; 2 -> 2; else -> 0 }
        png("body_p$phase", 360, 420, Palette.SURFACE_CARD) { s ->
            BodyScene.render(s, phase, wedges, mirrored = false)
        }
    }

    // ---- demo figures: every demo at 3 moments in its cycle ------------------
    run {
        val ids = DemoLibrary.demos.keys.sorted()
        val cw = 300
        val ch = 240
        val cols = 3
        for (id in ids) {
            png("demo_$id", cw * cols, ch, Palette.SURFACE_HIGH) { s ->
                val demo = DemoLibrary.demos.getValue(id)
                val total = demo.frames.sumOf { it.second }
                for (i in 0 until cols) {
                    val sub = object : Sketch by s {
                        override val width = cw.toFloat()
                        override val height = ch.toFloat()
                    }
                    s.save()
                    s.translate(i * cw.toFloat(), 0f)
                    DemoScene.render(sub, id, total * i / cols)
                    s.restore()
                }
            }
        }
    }

    // ---- regenerate app vector drawables -------------------------------------
    val resDir = File(root, "app/src/main/res/drawable")
    resDir.mkdirs()
    for (icon in Icons.all) {
        File(resDir, "${icon.name}.xml").writeText(Icons.vectorXml(icon))
    }
    println("wrote ${Icons.all.size} vector drawables to ${resDir.relativeTo(root)}")

    renderTodayMock(out, "screen_today")
    Palette.dark = true
    renderTodayMock(out, "screen_today_dark")
    Palette.dark = false
}

/** Faithful tone-preview of the redesigned Today screen (Roboto on device). */
fun renderTodayMock(out: java.io.File, name: String) {
    val w = 412; val h = 880
    val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val s = Java2DSketch(img)
    s.clear(Palette.SURFACE)
    val W = w.toFloat()

    // app bar
    s.text("Today", 20f, 46f, 27f, Palette.ON_SURFACE, medium = true)
    s.circle(W - 34f, 38f, 21f, Palette.ERROR_CONTAINER)
    Icons.draw(s, Icons.alert, W - 45f, 27f, 22f, Palette.ERROR)

    // hero card
    val heroT = 70f; val heroB = heroT + 158f
    s.roundRect(20f, heroT, W - 20f, heroB, 27f, Palette.HERO_BG)
    s.text("THURSDAY 12 JUNE", 40f, heroT + 34f, 11.5f, Palette.withAlpha(Palette.ON_HERO, 0xCC), medium = true)
    s.text("Week 1", 40f, heroT + 66f, 29f, Palette.ON_HERO, medium = true)
    s.text("Phase 1 · Immobilisation & protection", 40f, heroT + 88f, 13f, Palette.withAlpha(Palette.ON_HERO, 0xE6))
    s.text("9 of 21 done today", 40f, heroT + 108f, 12.5f, Palette.withAlpha(Palette.ON_HERO, 0xCC))
    // ring
    run {
        val cx = W - 70f; val cy = heroT + 64f; val r = 36f
        s.arc(cx, cy, r, -90f, 360f, Palette.withAlpha(Palette.ON_HERO, 0x59), 8f)
        s.arc(cx, cy, r, -90f, 154f, Palette.ON_HERO, 8f)
        s.text("43%", cx, cy + 6f, 16f, Palette.ON_HERO, medium = true, centered = true)
    }
    s.roundRect(40f, heroB - 38f, 40f + 118f, heroB - 10f, 14f, Palette.withAlpha(Palette.ON_HERO, 0x28))
    s.text("Phase guide", 40f + 59f, heroB - 19f, 12.5f, Palette.ON_HERO, medium = true, centered = true)

    // section + med row
    var y = heroB + 44f
    s.text("MEDICATION", 24f, y, 11.5f, Palette.ON_SURFACE_VARIANT, medium = true)
    y += 12f
    fun checkItem(title: String, sub: String, time: String?, done: Boolean) {
        val t = y; val b = y + 64f
        s.roundRect(20f, t, W - 20f, b, 24f, if (done) Palette.DONE_BG else Palette.SURFACE_CARD)
        val cy = (t + b) / 2f
        if (done) {
            s.circle(48f, cy, 13f, Palette.DONE)
            Icons.draw(s, Icons.check, 40f, cy - 8f, 16f, Palette.ON_PRIMARY)
        } else {
            s.circleStroke(48f, cy, 13f, Palette.withAlpha(Palette.ON_SURFACE_VARIANT, 0x66), 2.4f)
        }
        s.text(title, 72f, cy - 2f, 14.5f, if (done) Palette.ON_SURFACE_VARIANT else Palette.ON_SURFACE, medium = true)
        if (sub.isNotEmpty()) s.text(sub, 72f, cy + 16f, 12f, Palette.ON_SURFACE_VARIANT)
        if (time != null) {
            val tw = s.measureText(time, 12.5f, medium = true)
            s.roundRect(W - 40f - tw - 18f, cy - 13f, W - 36f, cy + 13f, 12f, Palette.SURFACE_HIGH)
            s.text(time, W - 49f - tw + 4f, cy + 4.5f, 12.5f, Palette.ON_SURFACE_VARIANT, medium = true)
        }
        y = b + 8f
    }
    checkItem("Anticoagulant 2.5 mg", "", "08:00", true)
    checkItem("Anticoagulant 2.5 mg", "", "20:00", false)
    y += 18f
    s.text("DAILY CARE", 24f, y, 11.5f, Palette.ON_SURFACE_VARIANT, medium = true)
    y += 12f
    checkItem("Boot check", "", "09:00", true)
    checkItem("Elevate the leg", "", "10:00", false)
    y += 18f
    s.text("EXERCISES · TAP FOR DEMO", 24f, y, 11.5f, Palette.ON_SURFACE_VARIANT, medium = true)
    y += 12f
    checkItem("Toe wiggles & scrunches (1/4)", "1 set × 20", null, false)

    // bottom nav
    val navT = h - 84f
    s.roundRect(0f, navT, W, h.toFloat(), 0f, Palette.SURFACE_CARD)
    val tabs = listOf("Today" to Icons.today, "Exercises" to Icons.exercises,
        "Progress" to Icons.progress, "My leg" to Icons.leg, "Settings" to Icons.more)
    tabs.forEachIndexed { i, (label, icon) ->
        val cx = W * (i + 0.5f) / 5f
        val active = i == 0
        if (active) s.roundRect(cx - 28f, navT + 10f, cx + 28f, navT + 40f, 16f, Palette.PRIMARY_CONTAINER)
        Icons.draw(s, icon, cx - 11f, navT + 14f, 22f,
            if (active) Palette.ON_PRIMARY_CONTAINER else Palette.ON_SURFACE_VARIANT)
        s.text(label, cx, navT + 60f, 11f, if (active) Palette.ON_SURFACE else Palette.ON_SURFACE_VARIANT, medium = active, centered = true)
    }
    javax.imageio.ImageIO.write(img, "png", java.io.File(out, "$name.png"))
    println("rendered $name.png")
}
