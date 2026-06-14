package com.recoverwell.app.ui

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.recoverwell.draw.Palette

/**
 * RecoverWell design system. Tonal surfaces, no hard borders, 18dp corner
 * radii, ripple feedback on everything interactive, vector iconography
 * (generated from the draw module - no emoji anywhere), and a type scale on
 * Roboto regular/medium. Accessibility: >=48dp touch targets, whole-row taps,
 * high contrast, content descriptions.
 */
object Ui {

    // palette bridged from the shared draw module so scenes and chrome match;
    // read-through getters keep everything theme-aware
    val BG get() = Palette.SURFACE
    val CARD get() = Palette.SURFACE_CARD
    val SURFACE_HIGH get() = Palette.SURFACE_HIGH
    val PRIMARY get() = Palette.PRIMARY
    val PRIMARY_CONTAINER get() = Palette.PRIMARY_CONTAINER
    val ON_PRIMARY_CONTAINER get() = Palette.ON_PRIMARY_CONTAINER
    val TEXT get() = Palette.ON_SURFACE
    val TEXT_DIM get() = Palette.ON_SURFACE_VARIANT
    val OUTLINE get() = Palette.OUTLINE
    val DANGER get() = Palette.ERROR
    val ON_DANGER get() = Palette.ON_ERROR
    val DANGER_BG get() = Palette.ERROR_CONTAINER
    val ON_DANGER_BG get() = Palette.ON_ERROR_CONTAINER
    val WARN get() = Palette.WARN
    val WARN_BG get() = Palette.WARN_CONTAINER
    val INFO_BG get() = Palette.INFO_CONTAINER
    val ON_INFO_BG get() = Palette.ON_INFO_CONTAINER
    val DONE get() = Palette.DONE
    val DONE_BG get() = Palette.DONE_BG
    val HERO_BG get() = Palette.HERO_BG
    val ON_HERO get() = Palette.ON_HERO

    const val MIN_TOUCH_DP = 48
    const val RADIUS = 18f
    const val RADIUS_SMALL = 12f

    private val medium: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()

    fun dpF(context: Context, value: Float): Float =
        value * context.resources.displayMetrics.density

    // ------------------------------------------------------------------ layout

    fun column(context: Context, padding: Int = 20): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val p = dp(context, padding)
            setPadding(p, dp(context, 12), p, dp(context, 12))
            clipChildren = false
            clipToPadding = false
        }

    fun row(context: Context): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

    fun scroll(context: Context, content: View): ScrollView =
        ScrollView(context).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            setBackgroundColor(BG)
            clipChildren = false
            addView(content, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

    fun spacer(context: Context, heightDp: Int): View =
        View(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, heightDp))
        }

    fun weight(view: View, w: Float): View {
        val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w)
        lp.gravity = Gravity.CENTER_VERTICAL
        view.layoutParams = lp
        return view
    }

    fun fullWidth(view: View, context: Context, marginTopDp: Int = 10): View {
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.topMargin = dp(context, marginTopDp)
        view.layoutParams = lp
        return view
    }

    // ------------------------------------------------------------------ type

    fun text(
        context: Context,
        value: CharSequence,
        sizeSp: Float = 15f,
        color: Int = TEXT,
        bold: Boolean = false
    ): TextView = TextView(context).apply {
        text = value
        textSize = sizeSp
        setTextColor(color)
        if (bold) typeface = medium
        setLineSpacing(0f, 1.22f)
    }

    fun display(context: Context, value: String): TextView =
        text(context, value, 26f, TEXT, bold = true).apply { asHeading(this) }

    fun headline(context: Context, value: String): TextView =
        text(context, value, 21f, TEXT, bold = true).apply { asHeading(this) }

    /** Flag a view as a screen-reader heading so TalkBack users can jump by heading. */
    fun asHeading(view: View) {
        if (android.os.Build.VERSION.SDK_INT >= 28) view.isAccessibilityHeading = true
    }

    fun title(context: Context, value: String): TextView =
        text(context, value, 17f, TEXT, bold = true)

    fun body(context: Context, value: String): TextView = text(context, value, 15f, TEXT)

    fun caption(context: Context, value: String): TextView = text(context, value, 13f, TEXT_DIM)

    fun section(context: Context, value: String): TextView =
        text(context, value, 13f, TEXT_DIM, bold = true).apply {
            letterSpacing = 0.08f
            isAllCaps = true
            setPadding(dp(context, 4), dp(context, 22), 0, dp(context, 8))
            // keep the spoken label normal-case (not letter-by-letter) and jumpable
            contentDescription = value
            asHeading(this)
        }

    // ------------------------------------------------------------------ shape

    fun rounded(color: Int, radius: Float = RADIUS): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius * 3
        }

    fun ripple(context: Context, content: GradientDrawable, rippleColor: Int = 0x1F2F6B4F): RippleDrawable =
        RippleDrawable(ColorStateList.valueOf(rippleColor), content, rounded(0xFF000000.toInt(), RADIUS))

    fun elevate(view: View, context: Context, elevationDp: Float = 2.5f) {
        view.elevation = dpF(context, elevationDp)
    }

    /** Elevated white card - the default content container. */
    fun card(context: Context, bg: Int = CARD, padding: Int = 16): LinearLayout =
        column(context, padding).apply {
            setPadding(dp(context, padding), dp(context, padding), dp(context, padding), dp(context, padding))
            background = rounded(bg)
            if (bg == CARD) elevate(this, context)
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, dp(context, 6), 0, dp(context, 6))
            layoutParams = lp
        }

    /** Card that responds to taps with a ripple. */
    fun tapCard(context: Context, bg: Int = CARD, onClick: () -> Unit): LinearLayout =
        card(context, bg).apply {
            isClickable = true
            isFocusable = true
            background = ripple(context, rounded(bg))
            setOnClickListener { onClick() }
        }

    // ------------------------------------------------------------------ icons

    fun drawableId(context: Context, name: String): Int =
        context.resources.getIdentifier(name, "drawable", context.packageName)

    fun icon(context: Context, name: String, sizeDp: Int = 22, tint: Int = TEXT): ImageView =
        ImageView(context).apply {
            setImageResource(drawableId(context, name))
            imageTintList = ColorStateList.valueOf(tint)
            layoutParams = LinearLayout.LayoutParams(dp(context, sizeDp), dp(context, sizeDp))
        }

    /** Circular tonal icon container, the visual anchor of list rows. */
    fun iconBadge(context: Context, name: String, tint: Int = PRIMARY, bg: Int = PRIMARY_CONTAINER, boxDp: Int = 40): FrameLayout =
        FrameLayout(context).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(bg) }
            layoutParams = LinearLayout.LayoutParams(dp(context, boxDp), dp(context, boxDp))
            val iv = icon(context, name, (boxDp * 0.55f).toInt(), tint)
            val lp = FrameLayout.LayoutParams(dp(context, (boxDp * 0.55f).toInt()), dp(context, (boxDp * 0.55f).toInt()))
            lp.gravity = Gravity.CENTER
            addView(iv, lp)
        }

    /** 44dp circular icon button with ripple. */
    fun iconButton(context: Context, name: String, tint: Int = TEXT, bg: Int = 0, desc: String = name, onClick: () -> Unit): FrameLayout =
        FrameLayout(context).apply {
            val bgShape = GradientDrawable()
            bgShape.shape = GradientDrawable.OVAL
            bgShape.setColor(bg)
            val mask = GradientDrawable()
            mask.shape = GradientDrawable.OVAL
            mask.setColor(0xFF000000.toInt())
            background = RippleDrawable(ColorStateList.valueOf(0x33000000), if (bg != 0) bgShape else null, mask)
            layoutParams = LinearLayout.LayoutParams(dp(context, 44), dp(context, 44))
            isClickable = true
            isFocusable = true
            contentDescription = desc
            setOnClickListener { onClick() }
            val iv = icon(context, name, 22, tint)
            val lp = FrameLayout.LayoutParams(dp(context, 22), dp(context, 22))
            lp.gravity = Gravity.CENTER
            addView(iv, lp)
        }

    // ------------------------------------------------------------------ buttons

    private fun baseButton(context: Context, label: String, fg: Int, bgDrawable: RippleDrawable): TextView =
        text(context, label, 15.5f, fg, bold = true).apply {
            background = bgDrawable
            gravity = Gravity.CENTER
            minHeight = dp(context, 50)
            minimumHeight = dp(context, 50)
            setPadding(dp(context, 22), dp(context, 13), dp(context, 22), dp(context, 13))
            isClickable = true
            isFocusable = true
            contentDescription = label
        }

    fun button(context: Context, label: String, onClick: () -> Unit): TextView =
        baseButton(context, label, Palette.ON_PRIMARY,
            ripple(context, rounded(PRIMARY, 25f), 0x33FFFFFF)).apply {
            setOnClickListener { onClick() }
        }

    fun tonalButton(context: Context, label: String, onClick: () -> Unit): TextView =
        baseButton(context, label, ON_PRIMARY_CONTAINER,
            ripple(context, rounded(PRIMARY_CONTAINER, 25f))).apply {
            setOnClickListener { onClick() }
        }

    fun textButton(context: Context, label: String, color: Int = PRIMARY, onClick: () -> Unit): TextView =
        baseButton(context, label, color, ripple(context, rounded(0x00000000, 25f))).apply {
            setOnClickListener { onClick() }
        }

    fun dangerButton(context: Context, label: String, onClick: () -> Unit): TextView =
        baseButton(context, label, ON_DANGER,
            ripple(context, rounded(DANGER, 25f), 0x33FFFFFF)).apply {
            setOnClickListener { onClick() }
        }

    /** Small "back" affordance used at the top of overlay screens. */
    fun backRow(context: Context, title: String, onBack: () -> Unit): LinearLayout =
        row(context).apply {
            setPadding(0, dp(context, 4), 0, dp(context, 8))
            addView(iconButton(context, "ic_back", TEXT, desc = "Back") { onBack() })
            val t = headline(context, title)
            t.setPadding(dp(context, 8), 0, 0, 0)
            addView(weight(t, 1f))
        }

    // ------------------------------------------------------------------ rows & chips

    /** Standard settings/list row: icon badge, title+subtitle, chevron. */
    fun listRow(
        context: Context,
        iconName: String,
        titleText: String,
        subtitleText: String?,
        iconTint: Int = PRIMARY,
        iconBg: Int = PRIMARY_CONTAINER,
        chevron: Boolean = true,
        onClick: (() -> Unit)? = null
    ): LinearLayout = row(context).apply {
        minimumHeight = dp(context, 64)
        setPadding(dp(context, 14), dp(context, 10), dp(context, 12), dp(context, 10))
        background = ripple(context, rounded(CARD))
        elevate(this, context)
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.setMargins(0, dp(context, 5), 0, dp(context, 5))
        layoutParams = lp
        if (onClick != null) {
            isClickable = true; isFocusable = true
            contentDescription = titleText
            setOnClickListener { onClick() }
        }
        addView(iconBadge(context, iconName, iconTint, iconBg))
        val texts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        texts.setPadding(dp(context, 14), 0, dp(context, 8), 0)
        texts.addView(text(context, titleText, 15.5f, TEXT, bold = true))
        if (!subtitleText.isNullOrBlank()) texts.addView(caption(context, subtitleText))
        addView(weight(texts, 1f))
        if (chevron && onClick != null) addView(icon(context, "ic_chevron", 18, TEXT_DIM))
    }

    /** Hairline divider for separating list items inside a card. */
    fun divider(context: Context): View = View(context).apply {
        setBackgroundColor(OUTLINE)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 1)
        ).apply { setMargins(0, dp(context, 10), 0, dp(context, 10)) }
    }

    /** Row of progress dots (filled up to [current] of [total]) - guided sessions. */
    fun setDots(context: Context, total: Int, current: Int): LinearLayout =
        row(context).apply {
            gravity = Gravity.CENTER
            for (i in 0 until total) {
                val on = i < current
                val dot = View(context).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(if (on) PRIMARY else PRIMARY_CONTAINER)
                    }
                }
                val sz = dp(context, if (on) 10 else 8)
                val lp = LinearLayout.LayoutParams(sz, sz)
                lp.setMargins(dp(context, 4), 0, dp(context, 4), 0)
                addView(dot, lp)
            }
        }

    /** A circular brand/illustration badge - the visual anchor of intro screens. */
    fun heroBadge(context: Context, iconName: String, boxDp: Int = 72): FrameLayout =
        FrameLayout(context).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(PRIMARY_CONTAINER) }
            layoutParams = LinearLayout.LayoutParams(dp(context, boxDp), dp(context, boxDp))
            val iv = icon(context, iconName, (boxDp * 0.5f).toInt(), ON_PRIMARY_CONTAINER)
            val lp = FrameLayout.LayoutParams(dp(context, (boxDp * 0.5f).toInt()), dp(context, (boxDp * 0.5f).toInt()))
            lp.gravity = Gravity.CENTER
            addView(iv, lp)
        }

    /** Checklist row: drawn circular check, whole row toggles, time on the right. */
    fun checkRow(
        context: Context,
        titleText: String,
        subtitleText: String,
        timeLabel: String?,
        done: Boolean,
        statusLabel: String?,
        onToggle: () -> Unit
    ): View {
        val rowView = row(context).apply {
            background = ripple(context, rounded(if (done) DONE_BG else CARD))
            if (!done) elevate(this, context)
            minimumHeight = dp(context, 60)
            setPadding(dp(context, 14), dp(context, 10), dp(context, 14), dp(context, 10))
            isClickable = true
            isFocusable = true
            contentDescription = (if (done) "Done: " else "To do: ") + titleText
            setOnClickListener {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                onToggle()
            }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, dp(context, 4), 0, dp(context, 4))
            layoutParams = lp
        }
        val ring = FrameLayout(context).apply {
            val d = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (done) setColor(DONE) else { setColor(CARD); setStroke(dp(context, 2), Palette.withAlpha(TEXT_DIM, 0x66)) }
            }
            background = d
            layoutParams = LinearLayout.LayoutParams(dp(context, 26), dp(context, 26))
            if (done) {
                val iv = icon(context, "ic_check", 16, 0xFFFFFFFF.toInt())
                val lp = FrameLayout.LayoutParams(dp(context, 16), dp(context, 16))
                lp.gravity = Gravity.CENTER
                addView(iv, lp)
            }
        }
        rowView.addView(ring)
        val texts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        texts.setPadding(dp(context, 13), 0, dp(context, 8), 0)
        val titleView = text(context, titleText, 15.5f, if (done) TEXT_DIM else TEXT, bold = true)
        texts.addView(titleView)
        if (subtitleText.isNotBlank()) {
            val sub = caption(context, subtitleText)
            sub.maxLines = 2
            texts.addView(sub)
        }
        if (statusLabel != null) {
            texts.addView(text(context, statusLabel, 12.5f, if (done) DONE else WARN, bold = true))
        }
        rowView.addView(weight(texts, 1f))
        if (timeLabel != null) {
            rowView.addView(text(context, timeLabel, 13f, TEXT_DIM, bold = true).apply {
                background = rounded(SURFACE_HIGH, RADIUS_SMALL)
                setPadding(dp(context, 9), dp(context, 4), dp(context, 9), dp(context, 4))
            })
        }
        return rowView
    }

    /**
     * Progressive checklist row drawn as a bullet chart: the pill is shaded from
     * the left to [done]/[total] complete, with the count on the right. Tapping
     * advances the next step. Used for repeated tasks / exercise sessions.
     */
    fun progressRow(
        context: Context,
        titleText: String,
        subtitleText: String,
        done: Int,
        total: Int,
        onClick: () -> Unit
    ): View {
        val frac = if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
        val complete = done >= total && total > 0
        val pill = FrameLayout(context)
        pill.background = rounded(if (complete) DONE_BG else CARD)
        if (!complete) elevate(pill, context)
        pill.clipToOutline = true
        pill.isClickable = true
        pill.isFocusable = true
        pill.contentDescription = (if (complete) "Done: " else "$done of $total done: ") + titleText
        pill.minimumHeight = dp(context, 60)
        val plp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        plp.setMargins(0, dp(context, 4), 0, dp(context, 4))
        pill.layoutParams = plp
        pill.foreground = RippleDrawable(ColorStateList.valueOf(0x1F2F6B4F), null,
            rounded(0xFF000000.toInt(), RADIUS))
        pill.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
            onClick()
        }

        // bullet-chart fill behind the content: left portion = fraction complete
        if (!complete && frac > 0f) {
            val fillRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            fillRow.addView(View(context).apply { setBackgroundColor(Palette.withAlpha(DONE, 0x30)) },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, frac))
            fillRow.addView(View(context),
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f - frac))
            pill.addView(fillRow, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }

        val rowView = row(context)
        rowView.setPadding(dp(context, 14), dp(context, 10), dp(context, 14), dp(context, 10))
        val ring = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (complete) setColor(DONE) else {
                    setColor(CARD); setStroke(dp(context, 2), Palette.withAlpha(TEXT_DIM, 0x66))
                }
            }
            layoutParams = LinearLayout.LayoutParams(dp(context, 26), dp(context, 26))
            if (complete) {
                val iv = icon(context, "ic_check", 16, 0xFFFFFFFF.toInt())
                val lp = FrameLayout.LayoutParams(dp(context, 16), dp(context, 16))
                lp.gravity = Gravity.CENTER
                addView(iv, lp)
            }
        }
        rowView.addView(ring)
        val texts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        texts.setPadding(dp(context, 13), 0, dp(context, 8), 0)
        texts.addView(text(context, titleText, 15.5f, if (complete) TEXT_DIM else TEXT, bold = true))
        if (subtitleText.isNotBlank()) {
            val sub = caption(context, subtitleText)
            sub.maxLines = 2
            texts.addView(sub)
        }
        rowView.addView(weight(texts, 1f))
        rowView.addView(pillBadge(context, "$done/$total",
            if (complete) DONE else ON_PRIMARY_CONTAINER,
            if (complete) DONE_BG else PRIMARY_CONTAINER))
        pill.addView(rowView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return pill
    }

    fun pillBadge(context: Context, label: String, fg: Int, bg: Int): TextView =
        text(context, label, 12.5f, fg, bold = true).apply {
            background = rounded(bg, 14f)
            setPadding(dp(context, 10), dp(context, 5), dp(context, 10), dp(context, 5))
        }

    /** Two-line stat tile used in grids (value on top, label under). */
    fun statTile(context: Context, value: String, label: String): LinearLayout =
        column(context, 0).apply {
            background = rounded(SURFACE_HIGH, RADIUS_SMALL)
            setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10))
            addView(text(context, value, 17f, TEXT, bold = true))
            addView(caption(context, label))
        }

    fun frame(context: Context): FrameLayout = FrameLayout(context)

    fun activity(view: View): Activity = view.context as Activity
}
