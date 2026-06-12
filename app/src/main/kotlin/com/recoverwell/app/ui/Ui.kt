package com.recoverwell.app.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Tiny programmatic-view toolkit. Accessibility defaults baked in:
 * minimum 56dp touch targets, 16sp+ text, high-contrast palette, and
 * layouts that keep primary actions in thumb reach for one-handed,
 * leg-elevated use.
 */
object Ui {

    // Palette: calm clinical green with high-contrast text.
    const val BG = 0xFFF6F8F6.toInt()
    const val CARD = 0xFFFFFFFF.toInt()
    const val PRIMARY = 0xFF1B5E20.toInt()
    const val PRIMARY_LIGHT = 0xFFA5D6A7.toInt()
    const val TEXT = 0xFF1A1A1A.toInt()
    const val TEXT_DIM = 0xFF555555.toInt()
    const val DANGER = 0xFFB71C1C.toInt()
    const val DANGER_BG = 0xFFFFEBEE.toInt()
    const val WARN = 0xFFB26A00.toInt()
    const val WARN_BG = 0xFFFFF3E0.toInt()
    const val INFO_BG = 0xFFE3F2FD.toInt()
    const val DONE = 0xFF2E7D32.toInt()
    const val BORDER = 0xFFDDDDDD.toInt()

    const val MIN_TOUCH_DP = 56

    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()

    fun column(context: Context, padding: Int = 16): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val p = dp(context, padding)
            setPadding(p, p, p, p)
        }

    fun row(context: Context): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

    fun scroll(context: Context, content: View): ScrollView =
        ScrollView(context).apply {
            isFillViewport = true
            setBackgroundColor(BG)
            addView(content, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

    fun text(
        context: Context,
        value: CharSequence,
        sizeSp: Float = 16f,
        color: Int = TEXT,
        bold: Boolean = false
    ): TextView = TextView(context).apply {
        text = value
        textSize = sizeSp
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    fun title(context: Context, value: String): TextView =
        text(context, value, 24f, TEXT, bold = true).apply {
            setPadding(0, dp(context, 8), 0, dp(context, 8))
        }

    fun section(context: Context, value: String): TextView =
        text(context, value.uppercase(), 14f, PRIMARY, bold = true).apply {
            setPadding(0, dp(context, 16), 0, dp(context, 6))
            letterSpacing = 0.06f
        }

    fun roundedBg(color: Int, radiusDp: Float = 12f, strokeColor: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radiusDp * 3
            if (strokeColor != null) setStroke(2, strokeColor)
        }

    fun card(context: Context, bg: Int = CARD): LinearLayout =
        column(context, 14).apply {
            background = roundedBg(bg, strokeColor = BORDER)
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, dp(context, 6), 0, dp(context, 6))
            layoutParams = lp
        }

    fun button(
        context: Context,
        label: String,
        bg: Int = PRIMARY,
        fg: Int = Color.WHITE,
        onClick: () -> Unit
    ): Button = Button(context).apply {
        text = label
        textSize = 17f
        isAllCaps = false
        setTextColor(fg)
        background = roundedBg(bg)
        minHeight = dp(context, MIN_TOUCH_DP)
        minimumHeight = dp(context, MIN_TOUCH_DP)
        setPadding(dp(context, 18), 0, dp(context, 18), 0)
        stateListAnimator = null
        contentDescription = label
        setOnClickListener { onClick() }
    }

    fun secondaryButton(context: Context, label: String, onClick: () -> Unit): Button =
        button(context, label, bg = CARD, fg = PRIMARY, onClick = onClick).apply {
            background = roundedBg(CARD, strokeColor = PRIMARY)
        }

    fun dangerButton(context: Context, label: String, onClick: () -> Unit): Button =
        button(context, label, bg = DANGER, onClick = onClick)

    fun spacer(context: Context, heightDp: Int): View =
        View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, heightDp)
            )
        }

    fun divider(context: Context): View =
        View(context).apply {
            setBackgroundColor(BORDER)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 1))
        }

    fun weight(view: View, w: Float): View {
        val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w)
        lp.gravity = Gravity.CENTER_VERTICAL
        view.layoutParams = lp
        return view
    }

    fun fullWidth(view: View, context: Context, marginTopDp: Int = 8): View {
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.topMargin = dp(context, marginTopDp)
        view.layoutParams = lp
        return view
    }

    fun badge(context: Context, label: String, bg: Int, fg: Int): TextView =
        text(context, label, 13f, fg, bold = true).apply {
            background = roundedBg(bg, radiusDp = 16f)
            setPadding(dp(context, 10), dp(context, 4), dp(context, 10), dp(context, 4))
        }

    /** A big tappable checklist row: whole row is the touch target. */
    fun checkRow(
        context: Context,
        titleText: String,
        subtitleText: String,
        done: Boolean,
        statusLabel: String?,
        onToggle: () -> Unit
    ): View {
        val row = row(context).apply {
            background = roundedBg(if (done) 0xFFE8F5E9.toInt() else CARD, strokeColor = BORDER)
            minimumHeight = dp(context, MIN_TOUCH_DP + 8)
            setPadding(dp(context, 14), dp(context, 10), dp(context, 14), dp(context, 10))
            isClickable = true
            isFocusable = true
            contentDescription = (if (done) "Done: " else "To do: ") + titleText
            setOnClickListener { onToggle() }
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, dp(context, 4), 0, dp(context, 4))
            layoutParams = lp
        }
        val tick = text(context, if (done) "✓" else "○", 26f, if (done) DONE else TEXT_DIM, bold = true)
        tick.setPadding(0, 0, dp(context, 14), 0)
        row.addView(tick)
        val texts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(text(context, titleText, 17f, TEXT, bold = true))
        if (subtitleText.isNotBlank()) {
            texts.addView(text(context, subtitleText, 14f, TEXT_DIM))
        }
        if (statusLabel != null) {
            texts.addView(text(context, statusLabel, 13f, if (done) DONE else WARN, bold = true))
        }
        row.addView(weight(texts, 1f))
        return row
    }

    fun frame(context: Context): FrameLayout = FrameLayout(context)
}
