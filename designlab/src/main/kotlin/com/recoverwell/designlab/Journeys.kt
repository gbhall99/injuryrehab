package com.recoverwell.designlab

import com.recoverwell.draw.*
import java.io.File

/** Renders one faithful preview per user-journey screen, in both themes. */
object Journeys {

    fun renderAll(out: File) {
        for (dark in listOf(false, true)) {
            Palette.dark = dark
            val suffix = if (dark) "_dark" else ""
            onboardingWelcome(out, suffix)
            today(out, suffix)
            exerciseDetail(out, suffix)
            guidedSession(out, suffix)
            tracker(out, suffix)
            twin(out, suffix)
            redFlags(out, suffix)
            settings(out, suffix)
            smart(out, suffix)
        }
        Palette.dark = false
    }

    // ---- Smart layer: insights + pace + ask ----
    private fun smart(out: File, sfx: String) {
        val m = ScreenMock()
        m.fill(); m.statusBar(); m.appBar("Progress", withAlert = false)
        m.section("Your pace")
        val pt = m.card(96f)
        m.s.text("~1 week ahead", m.pad + 16f, pt + 30f, 16f, Palette.ON_SURFACE, medium = true)
        m.s.text("You're tracking about 1 week ahead of the typical", m.pad + 16f, pt + 54f, 13.5f, Palette.ON_SURFACE)
        m.s.text("timeline. Keep letting your physio set the pace.", m.pad + 16f, pt + 74f, 13.5f, Palette.ON_SURFACE)
        m.section("Insights")
        fun insight(bg: Int, fg: Int, icon: String, title: String, body: List<String>) {
            val h = 46f + body.size * 19f
            val t = m.card(h, bg)
            MaterialIcons.draw(m.s, icon, m.pad + 14f, t + 16f, 20f, fg)
            m.s.text(title, m.pad + 44f, t + 30f, 15f, fg, medium = true)
            body.forEachIndexed { i, line -> m.s.text(line, m.pad + 44f, t + 52f + i * 19f, 13f, Palette.ON_SURFACE) }
        }
        insight(Palette.DONE_BG, Palette.DONE, "ic_check", "Pain is easing",
            listOf("Average pain is down 2 points vs last week", "(6 → 4). A good sign the tendon is settling."))
        insight(Palette.DONE_BG, Palette.DONE, "ic_check", "Elevation seems to help your swelling",
            listOf("On days you logged elevation, swelling tended", "to be lower (1.1 vs 2.0). Worth keeping up."))
        insight(Palette.INFO_CONTAINER, Palette.ON_INFO_CONTAINER, "ic_ask", "Recovery coach",
            listOf("Can I drive yet? What's next? - answered offline"))
        m.disclaimerStrip(); m.bottomNav(2)
        m.save(out, "journey_smart$sfx")
    }

    // ---- Journey 1: first launch ----
    private fun onboardingWelcome(out: File, sfx: String) {
        val m = ScreenMock()
        m.fill(); m.statusBar()
        // brand hero badge
        m.s.circle(m.pad + 36f, 110f, 36f, Palette.PRIMARY_CONTAINER)
        MaterialIcons.draw(m.s, "ic_leg", m.pad + 18f, 92f, 36f, Palette.ON_PRIMARY_CONTAINER)
        m.s.text("Welcome to", m.pad, 184f, 16f, Palette.ON_SURFACE_VARIANT)
        m.s.text("RecoverWell", m.pad, 218f, 30f, Palette.ON_SURFACE, medium = true)
        m.y = 234f
        m.textLine("Your daily coach through a conservative (non-surgical)", 15f, Palette.ON_SURFACE)
        m.textLine("Achilles rupture - exercises, reminders and", 15f, Palette.ON_SURFACE)
        m.textLine("progress tracking.", 15f, Palette.ON_SURFACE)
        m.y += 8f
        // disclaimer card
        val t1 = m.card(96f, Palette.WARN_CONTAINER)
        m.s.text("Before you start", m.pad + 16f, t1 + 28f, 16f, Palette.WARN, medium = true)
        m.s.text("Supports - never replaces - your physio and", m.pad + 16f, t1 + 52f, 13.5f, Palette.ON_SURFACE)
        m.s.text("consultant. Timelines are physio-confirmable.", m.pad + 16f, t1 + 72f, 13.5f, Palette.ON_SURFACE)
        // safety card
        val t2 = m.card(150f, Palette.ERROR_CONTAINER)
        m.s.circle(m.pad + 34f, t2 + 30f, 18f, Palette.withAlpha(Palette.ERROR, 0x22))
        MaterialIcons.draw(m.s, "ic_alert", m.pad + 23f, t2 + 19f, 22f, Palette.ERROR)
        m.s.text("Safety first", m.pad + 60f, t2 + 36f, 16f, Palette.ON_ERROR_CONTAINER, medium = true)
        m.s.text("Achilles rupture carries a real risk of blood", m.pad + 16f, t2 + 64f, 13.5f, Palette.ON_ERROR_CONTAINER)
        m.s.text("clots - that is why you take a clot-prevention med.", m.pad + 16f, t2 + 84f, 13.5f, Palette.ON_ERROR_CONTAINER)
        m.s.roundRect(m.pad + 16f, t2 + 100f, m.W - m.pad - 16f, t2 + 138f, 19f, Palette.ERROR)
        m.s.text("Read the red flags", m.W / 2f, t2 + 124f, 14f, Palette.ON_ERROR, medium = true, centered = true)
        m.primaryButton("I understand - continue")
        m.save(out, "journey_onboarding$sfx")
    }

    // ---- Journey 2: daily check-in ----
    private fun today(out: File, sfx: String) {
        val m = ScreenMock()
        m.fill(); m.statusBar(); m.appBar("Today")
        // hero
        val ht = 168f
        val top = m.y + 6f
        m.s.roundRect(m.pad, top, m.W - m.pad, top + ht, 22f, Palette.HERO_BG)
        val oh = Palette.ON_HERO
        m.s.text("SATURDAY 13 JUNE", m.pad + 20f, top + 30f, 11.5f, Palette.withAlpha(oh, 0xCC), medium = true)
        m.s.text("Week 1", m.pad + 20f, top + 62f, 30f, oh, medium = true)
        m.s.text("Phase 1 · Immobilisation & protection", m.pad + 20f, top + 86f, 13f, Palette.withAlpha(oh, 0xE6))
        m.s.text("4 of 21 done today", m.pad + 20f, top + 108f, 12.5f, Palette.withAlpha(oh, 0xCC))
        // streak chip (width fits its text, as in the real wrap_content chip)
        run {
            val label = "3-day medication streak"
            val tw = m.s.measureText(label, 12f, medium = true)
            m.s.roundRect(m.pad + 20f, top + 120f, m.pad + 20f + tw + 36f, top + 146f, 13f, Palette.withAlpha(oh, 0x28))
            MaterialIcons.draw(m.s, "ic_flag", m.pad + 28f, top + 126f, 14f, oh)
            m.s.text(label, m.pad + 48f, top + 138f, 12f, oh, medium = true)
        }
        // ring
        val cx = m.W - 70f; val cy = top + 66f; val r = 36f
        m.s.arc(cx, cy, r, -90f, 360f, Palette.withAlpha(oh, 0x59), 8f)
        m.s.arc(cx, cy, r, -90f, 68f, oh, 8f)
        m.s.text("19%", cx, cy + 6f, 16f, oh, medium = true, centered = true)
        m.y = top + ht + 6f
        m.section("Medication")
        m.checkRow("Anticoagulant 2.5 mg", "", "08:00", true)
        m.checkRow("Anticoagulant 2.5 mg", "", "20:00", false)
        m.section("Daily care")
        m.checkRow("Boot check", "", "09:00", false)
        m.checkRow("Elevate the leg", "", "10:00", false)
        m.section("Exercises · tap for demo")
        m.checkRow("Toe wiggles & scrunches", "1 set × 20", null, false)
        m.disclaimerStrip(); m.bottomNav(0)
        m.save(out, "journey_today$sfx")
    }

    // ---- Journey 3a: exercise detail ----
    private fun exerciseDetail(out: File, sfx: String) {
        val m = ScreenMock()
        m.fill(); m.statusBar(); m.backBar("Toe wiggles & scrunches")
        // demo panel
        val top = m.y + 6f; val dh = 200f
        m.s.roundRect(m.pad, top, m.W - m.pad, top + dh, 18f, Palette.SURFACE_HIGH)
        // draw the actual demo figure inside
        m.s.save()
        m.s.translate(m.pad, top)
        val sub = object : Sketch by m.s {
            override val width = (m.W - 2 * m.pad); override val height = dh
        }
        DemoScene.render(sub, "toe_scrunch", 900L)
        m.s.restore()
        // "Quick reference" tag on the animation
        m.s.roundRect(m.pad + 10f, top + 10f, m.pad + 122f, top + 34f, 10f, Palette.withAlpha(Palette.SURFACE_CARD, 0xE6))
        m.s.text("Quick reference", m.pad + 18f, top + 26f, 11.5f, Palette.ON_SURFACE_VARIANT, medium = true)
        m.y = top + dh + 6f
        // primary CTA: watch real video on YouTube
        val by = m.y + 10f
        m.s.roundRect(m.pad, by, m.W - m.pad, by + 50f, 25f, Palette.PRIMARY)
        MaterialIcons.draw(m.s, "ic_play", m.pad + 18f, by + 15f, 20f, Palette.ON_PRIMARY)
        m.s.text("Watch video demonstration", m.pad + 48f, by + 31f, 15.5f, Palette.ON_PRIMARY, medium = true)
        m.s.text("YouTube", m.W - m.pad - 60f, by + 31f, 12f, Palette.withAlpha(Palette.ON_PRIMARY, 0xCC))
        m.y = by + 50f + 6f
        m.s.text("Opens YouTube · the animation above works offline", m.pad + 4f, m.y + 12f, 12f, Palette.ON_SURFACE_VARIANT)
        m.y += 20f
        m.section("Prescription")
        // stat tiles
        val tt = m.y + 4f
        val tiles = listOf("1" to "set", "20" to "reps", "4×" to "per day")
        val tw = (m.W - 2 * m.pad - 2 * 6f) / 3f
        tiles.forEachIndexed { i, (v, l) ->
            val lx = m.pad + i * (tw + 6f)
            m.s.roundRect(lx, tt, lx + tw, tt + 56f, 12f, Palette.SURFACE_HIGH)
            m.s.text(v, lx + 12f, tt + 28f, 17f, Palette.ON_SURFACE, medium = true)
            m.s.text(l, lx + 12f, tt + 46f, 12.5f, Palette.ON_SURFACE_VARIANT)
        }
        m.y = tt + 56f + 6f
        m.save(out, "journey_exercise$sfx")
    }

    // ---- Journey 3b: guided session ----
    private fun guidedSession(out: File, sfx: String) {
        val m = ScreenMock()
        m.fill(); m.statusBar(); m.backBar("Toe wiggles & scrunches")
        val top = m.y + 6f; val dh = 150f
        m.s.roundRect(m.pad, top, m.W - m.pad, top + dh, 18f, Palette.SURFACE_HIGH)
        m.s.save(); m.s.translate(m.pad, top)
        val sub = object : Sketch by m.s { override val width = (m.W - 2 * m.pad); override val height = dh }
        DemoScene.render(sub, "toe_scrunch", 700L)
        m.s.restore()
        m.y = top + dh + 6f
        // big counter card with set-progress dots
        val ct = m.card(250f)
        // dots: 3 sets, on set 2
        listOf(0, 1, 2).forEach { i ->
            val on = i < 1
            val cx = m.W / 2f - 16f + i * 16f
            m.s.circle(cx, ct + 26f, if (on) 5f else 4f, if (on) Palette.PRIMARY else Palette.PRIMARY_CONTAINER)
        }
        m.s.roundRect(m.W / 2f - 52f, ct + 42f, m.W / 2f + 52f, ct + 70f, 14f, Palette.PRIMARY_CONTAINER)
        m.s.text("Set 2 of 3", m.W / 2f, ct + 61f, 13f, Palette.ON_PRIMARY_CONTAINER, medium = true, centered = true)
        m.s.text("12", m.W / 2f, ct + 138f, 56f, Palette.PRIMARY, medium = true, centered = true)
        m.s.text("of 15 reps", m.W / 2f, ct + 164f, 13.5f, Palette.ON_SURFACE_VARIANT, centered = true)
        m.s.roundRect(m.pad + 16f, ct + 184f, m.W - m.pad - 16f, ct + 224f, 20f, Palette.PRIMARY)
        m.s.text("Rep done", m.W / 2f, ct + 208f, 15.5f, Palette.ON_PRIMARY, medium = true, centered = true)
        m.s.text("End early & log anyway", m.W / 2f, m.y + 24f, 14f, Palette.PRIMARY, medium = true, centered = true)
        m.save(out, "journey_guided$sfx")
    }

    // ---- Journey 4: progress tracker ----
    private fun tracker(out: File, sfx: String) {
        val m = ScreenMock()
        m.fill(); m.statusBar(); m.appBar("Progress")
        m.section("Today's log")
        // day nav
        val nt = m.y + 4f
        m.s.circle(m.pad + 22f, nt + 22f, 22f, Palette.SURFACE_HIGH)
        MaterialIcons.draw(m.s, "ic_back", m.pad + 11f, nt + 11f, 22f, Palette.ON_SURFACE)
        m.s.roundRect(m.pad + 52f, nt, m.W - m.pad - 52f, nt + 44f, 22f, Palette.PRIMARY_CONTAINER)
        m.s.text("Today", m.W / 2f, nt + 28f, 14f, Palette.ON_PRIMARY_CONTAINER, medium = true, centered = true)
        m.s.circle(m.W - m.pad - 22f, nt + 22f, 22f, Palette.SURFACE_HIGH)
        MaterialIcons.draw(m.s, "ic_chevron", m.W - m.pad - 33f, nt + 11f, 22f, Palette.OUTLINE)
        m.y = nt + 44f + 6f
        val ct = m.card(166f)
        m.s.text("Pain right now", m.pad + 16f, ct + 26f, 13f, Palette.ON_SURFACE_VARIANT, medium = true)
        m.s.text("3", m.W / 2f, ct + 58f, 22f, Palette.PRIMARY, medium = true, centered = true)
        // slider
        m.s.roundRect(m.pad + 20f, ct + 76f, m.W - m.pad - 20f, ct + 82f, 3f, Palette.OUTLINE)
        m.s.roundRect(m.pad + 20f, ct + 76f, m.pad + 20f + (m.W - 2 * m.pad - 40f) * 0.3f, ct + 82f, 3f, Palette.PRIMARY)
        m.s.circle(m.pad + 20f + (m.W - 2 * m.pad - 40f) * 0.3f, ct + 79f, 11f, Palette.PRIMARY)
        // scale anchors
        m.s.text("0 · None", m.pad + 18f, ct + 102f, 12f, Palette.ON_SURFACE_VARIANT)
        val mxw = m.s.measureText("10 · Worst", 12f)
        m.s.text("10 · Worst", m.W - m.pad - 18f - mxw, ct + 102f, 12f, Palette.ON_SURFACE_VARIANT)
        m.s.text("Swelling", m.pad + 16f, ct + 126f, 13f, Palette.ON_SURFACE_VARIANT, medium = true)
        // chips
        listOf("None", "Mild", "Moderate", "Severe").forEachIndexed { i, o ->
            val cw = (m.W - 2 * m.pad - 32f - 18f) / 4f
            val lx = m.pad + 16f + i * (cw + 6f)
            val on = i == 1
            m.s.roundRect(lx, ct + 136f, lx + cw, ct + 158f, 11f, if (on) Palette.PRIMARY_CONTAINER else Palette.SURFACE_HIGH)
            m.s.text(o, lx + cw / 2f, ct + 151f, 11f, if (on) Palette.ON_PRIMARY_CONTAINER else Palette.ON_SURFACE_VARIANT, centered = true)
        }
        m.section("Trends")
        m.chips(listOf("Pain", "Swelling", "Mood", "Energy"), 0)
        val cht = m.card(190f)
        m.s.save(); m.s.translate(m.pad, cht)
        val sub = object : Sketch by m.s { override val width = (m.W - 2 * m.pad); override val height = 190f }
        val pts = (0..10).map { it.toFloat() to (7f - it * 0.45f) }
        val avg = pts.mapIndexed { i, (x, _) -> x to pts.take(i + 1).map { it.second }.average().toFloat() }
        ChartScene.render(sub, ChartScene.Data(pts, avg, 0f, 10f, "2 Jun", "13 Jun", "No entries yet"))
        m.s.restore()
        m.y = cht + 190f + 6f
        m.disclaimerStrip(); m.bottomNav(2)
        m.save(out, "journey_tracker$sfx")
    }

    // ---- Journey 5: digital twin ----
    private fun twin(out: File, sfx: String) {
        val m = ScreenMock()
        m.fill(); m.statusBar(); m.appBar("My leg")
        val ht = 230f; val top = m.card(ht)
        m.s.save(); m.s.translate(m.pad + 8f, top + 8f)
        val sub = object : Sketch by m.s { override val width = 150f; override val height = ht - 16f }
        BodyScene.render(sub, 1, 1.0f, 0, false)
        m.s.restore()
        val fx = m.pad + 168f
        m.s.roundRect(fx, top + 16f, fx + 150f, top + 42f, 13f, Palette.PRIMARY_CONTAINER)
        m.s.text("Week 1 · Phase 1", fx + 12f, top + 33f, 12.5f, Palette.ON_PRIMARY_CONTAINER, medium = true)
        m.s.text("Left · Achilles tendon", fx, top + 70f, 15f, Palette.ON_SURFACE, medium = true)
        m.s.text("rupture", fx, top + 90f, 15f, Palette.ON_SURFACE, medium = true)
        m.s.text("Tendon ends knitting", fx, top + 116f, 12.5f, Palette.ON_SURFACE_VARIANT)
        m.s.text("together", fx, top + 132f, 12.5f, Palette.ON_SURFACE_VARIANT)
        m.s.text("Boot on at all times", fx, top + 162f, 13f, Palette.ON_SURFACE)
        m.s.text("Heel angle 30°", fx, top + 182f, 13f, Palette.ON_SURFACE)
        m.section("Can I...")
        val cc = m.card(186f)
        val rows = listOf("Walk without the boot" to false, "Drive a car" to false, "Play padel" to false)
        rows.forEachIndexed { i, (mv, ok) ->
            val ly = cc + 36f + i * 56f
            m.s.circle(m.pad + 30f, ly, 17f, if (ok) Palette.DONE_BG else Palette.ERROR_CONTAINER)
            MaterialIcons.draw(m.s, if (ok) "ic_check" else "ic_close", m.pad + 20f, ly - 10f, 20f, if (ok) Palette.DONE else Palette.ERROR)
            m.s.text(mv, m.pad + 58f, ly - 4f, 14.5f, Palette.ON_SURFACE, medium = true)
            m.s.text("Not yet - your physio confirms each step", m.pad + 58f, ly + 15f, 12f, Palette.ON_SURFACE_VARIANT)
            if (i < rows.size - 1) m.s.stroke(PathSpec.line(m.pad + 16f, ly + 28f, m.W - m.pad - 16f, ly + 28f), Palette.OUTLINE, 1f)
        }
        m.disclaimerStrip(); m.bottomNav(3)
        m.save(out, "journey_twin$sfx")
    }

    // ---- Journey 6: red flags ----
    private fun redFlags(out: File, sfx: String) {
        val m = ScreenMock()
        m.fill(); m.statusBar(); m.backBar("Red flags")
        m.y = 110f
        m.textLine("After an Achilles rupture you are at raised risk", 14f, Palette.ON_SURFACE)
        m.textLine("of a blood clot, and the healing tendon can", 14f, Palette.ON_SURFACE)
        m.textLine("re-tear. Take these signs seriously.", 14f, Palette.ON_SURFACE)
        m.y += 8f
        // PE card (urgent)
        val pe = m.card(150f, Palette.ERROR_CONTAINER)
        m.s.circle(m.pad + 32f, pe + 30f, 17f, Palette.withAlpha(Palette.ERROR, 0x22))
        MaterialIcons.draw(m.s, "ic_alert", m.pad + 22f, pe + 20f, 20f, Palette.ERROR)
        m.s.text("Possible pulmonary embolism", m.pad + 58f, pe + 36f, 15f, Palette.ON_ERROR_CONTAINER, medium = true)
        m.s.roundRect(m.pad + 16f, pe + 52f, m.pad + 120f, pe + 76f, 12f, Palette.ERROR)
        m.s.text("Call 999 now", m.pad + 68f, pe + 68f, 12f, Palette.ON_ERROR, medium = true, centered = true)
        m.s.text("· Sudden breathlessness", m.pad + 18f, pe + 96f, 13.5f, Palette.ON_ERROR_CONTAINER)
        m.s.text("· Chest pain when breathing in", m.pad + 18f, pe + 116f, 13.5f, Palette.ON_ERROR_CONTAINER)
        m.s.text("A clot can travel to the lungs. Call 999.", m.pad + 18f, pe + 138f, 13f, Palette.ON_ERROR_CONTAINER, medium = true)
        // DVT card
        val dvt = m.card(130f)
        m.s.circle(m.pad + 32f, dvt + 30f, 17f, Palette.ERROR_CONTAINER)
        MaterialIcons.draw(m.s, "ic_alert", m.pad + 22f, dvt + 20f, 20f, Palette.ERROR)
        m.s.text("Possible DVT (clot in the leg)", m.pad + 58f, dvt + 36f, 15f, Palette.ON_SURFACE, medium = true)
        m.s.roundRect(m.pad + 16f, dvt + 52f, m.pad + 200f, dvt + 76f, 12f, Palette.ERROR)
        m.s.text("Same-day review · 111", m.pad + 108f, dvt + 68f, 12f, Palette.ON_ERROR, medium = true, centered = true)
        m.s.text("· New calf pain, tenderness or swelling", m.pad + 18f, dvt + 96f, 13.5f, Palette.ON_SURFACE)
        m.s.text("· Calf hot to touch, skin red", m.pad + 18f, dvt + 116f, 13.5f, Palette.ON_SURFACE)
        m.save(out, "journey_redflags$sfx")
    }

    // ---- Journey 7: settings ----
    private fun settings(out: File, sfx: String) {
        val m = ScreenMock()
        m.fill(); m.statusBar(); m.appBar("Settings", withAlert = false)
        m.section("Recovery tools")
        m.listRow("ic_ask", "Recovery coach", "Can I drive yet? What's next?")
        m.listRow("ic_edit", "Recovery journal", "Speak a daily check-in")
        m.listRow("ic_calendar", "Physio visits", "Appointment pack and visit notes")
        m.section("Your plan")
        m.listRow("ic_heart", "Injury & goal", "Dates, side, wedge plan, appointments")
        m.listRow("ic_calendar", "Phase dates", "Adjust timings agreed with your physio")
        m.section("Appearance")
        m.chips(listOf("System", "Light", "Dark"), 0)
        m.section("Safety & info")
        m.listRow("ic_alert", "Red flags", "DVT, re-rupture, bleeding", tint = Palette.ERROR, badgeBg = Palette.ERROR_CONTAINER)
        m.disclaimerStrip(); m.bottomNav(4)
        m.save(out, "journey_settings$sfx")
    }
}
