package com.recoverwell.app.ui

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import java.time.LocalDate
import java.time.LocalTime

/** Form building blocks sized for one-handed use with the leg up. */
object Forms {

    fun label(ctx: Activity, text: String): TextView =
        Ui.text(ctx, text, 13.5f, Ui.TEXT_DIM, bold = true).apply {
            setPadding(Ui.dp(ctx, 2), Ui.dp(ctx, 14), 0, Ui.dp(ctx, 6))
        }

    fun editText(ctx: Activity, initial: String, hint: String, multiline: Boolean = false): EditText =
        EditText(ctx).apply {
            setText(initial)
            this.hint = hint
            textSize = 15.5f
            setTextColor(Ui.TEXT)
            setHintTextColor(0xFF9AA39C.toInt())
            background = Ui.rounded(Ui.SURFACE_HIGH, Ui.RADIUS_SMALL)
            val p = Ui.dp(ctx, 14)
            setPadding(p, Ui.dp(ctx, 12), p, Ui.dp(ctx, 12))
            minHeight = Ui.dp(ctx, Ui.MIN_TOUCH_DP)
            if (multiline) {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                minLines = 2
            }
        }

    /** Large +/- stepper, much easier than typing numbers one-handed. */
    fun stepper(
        ctx: Activity,
        title: String,
        initial: Int,
        min: Int,
        max: Int,
        step: Int = 1,
        onChange: (Int) -> Unit
    ): LinearLayout {
        var value = initial
        val row = Ui.row(ctx)
        row.minimumHeight = Ui.dp(ctx, Ui.MIN_TOUCH_DP)
        val valueView = Ui.text(ctx, value.toString(), 17f, Ui.TEXT, bold = true).apply {
            gravity = Gravity.CENTER
            minWidth = Ui.dp(ctx, 48)
        }
        fun set(v: Int) {
            value = v.coerceIn(min, max)
            valueView.text = value.toString()
            onChange(value)
        }
        val minus = Ui.iconButton(ctx, "ic_minus", Ui.ON_PRIMARY_CONTAINER, Ui.PRIMARY_CONTAINER,
            desc = "Decrease $title") { set(value - step) }
        val plus = Ui.iconButton(ctx, "ic_plus", Ui.ON_PRIMARY_CONTAINER, Ui.PRIMARY_CONTAINER,
            desc = "Increase $title") { set(value + step) }
        row.addView(Ui.weight(Ui.text(ctx, title, 15f), 1f))
        row.addView(minus)
        row.addView(valueView)
        row.addView(plus)
        return row
    }

    fun dateRow(ctx: Activity, title: String, initial: LocalDate, onChange: (LocalDate) -> Unit): LinearLayout {
        val row = Ui.row(ctx)
        row.minimumHeight = Ui.dp(ctx, Ui.MIN_TOUCH_DP)
        var current = initial
        var btn: TextView? = null
        btn = Ui.tonalButton(ctx, current.toString()) {
            DatePickerDialog(ctx, { _, y, m, d ->
                current = LocalDate.of(y, m + 1, d)
                btn?.text = current.toString()
                onChange(current)
            }, current.year, current.monthValue - 1, current.dayOfMonth).show()
        }
        row.addView(Ui.weight(Ui.text(ctx, title, 15f), 1f))
        row.addView(btn)
        return row
    }

    fun timeButton(ctx: Activity, initial: LocalTime, onChange: (LocalTime) -> Unit): TextView {
        var current = initial
        var btn: TextView? = null
        btn = Ui.tonalButton(ctx, "%02d:%02d".format(current.hour, current.minute)) {
            TimePickerDialog(ctx, { _, h, m ->
                current = LocalTime.of(h, m)
                btn?.text = "%02d:%02d".format(h, m)
                onChange(current)
            }, current.hour, current.minute, true).show()
        }
        return btn
    }

    /** Single-select row of segmented chips. */
    fun <T> choiceRow(
        ctx: Activity,
        options: List<T>,
        labelOf: (T) -> String,
        selected: T?,
        onSelect: (T) -> Unit
    ): LinearLayout {
        val row = Ui.row(ctx)
        lateinit var rebuild: () -> Unit
        var current = selected
        rebuild = {
            row.removeAllViews()
            for (opt in options) {
                val isSel = opt == current
                val chip = Ui.text(ctx, labelOf(opt), 13.5f,
                    if (isSel) Ui.ON_PRIMARY_CONTAINER else Ui.TEXT_DIM, bold = true).apply {
                    gravity = Gravity.CENTER
                    background = Ui.ripple(ctx, Ui.rounded(
                        if (isSel) Ui.PRIMARY_CONTAINER else Ui.SURFACE_HIGH, 16f))
                    minHeight = Ui.dp(ctx, Ui.MIN_TOUCH_DP)
                    setPadding(Ui.dp(ctx, 6), Ui.dp(ctx, 12), Ui.dp(ctx, 6), Ui.dp(ctx, 12))
                    isClickable = true
                    isFocusable = true
                    contentDescription = labelOf(opt) + if (isSel) ", selected" else ""
                    setOnClickListener {
                        current = opt
                        onSelect(opt)
                        rebuild()
                    }
                }
                val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                lp.setMargins(Ui.dp(ctx, 3), 0, Ui.dp(ctx, 3), 0)
                chip.layoutParams = lp
                row.addView(chip)
            }
        }
        rebuild()
        return row
    }

    /** 0-10 slider with a live value readout and scale anchors, for the pain scale. */
    fun scaleSlider(
        ctx: Activity, max: Int, initial: Int?,
        minLabel: String? = null, maxLabel: String? = null,
        onChange: (Int) -> Unit
    ): LinearLayout {
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val readout = Ui.text(ctx, initial?.toString() ?: "–", 22f, Ui.PRIMARY, bold = true)
        readout.gravity = Gravity.CENTER
        val bar = SeekBar(ctx).apply {
            this.max = max
            progress = initial ?: 0
            minimumHeight = Ui.dp(ctx, Ui.MIN_TOUCH_DP)
            progressTintList = ColorStateList.valueOf(Ui.PRIMARY)
            thumbTintList = ColorStateList.valueOf(Ui.PRIMARY)
            setPadding(Ui.dp(ctx, 18), Ui.dp(ctx, 10), Ui.dp(ctx, 18), Ui.dp(ctx, 10))
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    readout.text = progress.toString()
                    onChange(progress)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        col.addView(readout)
        col.addView(bar)
        if (minLabel != null || maxLabel != null) {
            val anchors = Ui.row(ctx)
            anchors.setPadding(Ui.dp(ctx, 18), 0, Ui.dp(ctx, 18), 0)
            anchors.addView(Ui.weight(Ui.text(ctx, minLabel ?: "", 12f, Ui.TEXT_DIM), 1f))
            val mx = Ui.text(ctx, maxLabel ?: "", 12f, Ui.TEXT_DIM)
            mx.gravity = Gravity.END
            anchors.addView(mx)
            col.addView(anchors)
        }
        return col
    }

    fun confirm(ctx: Activity, title: String, message: String, onYes: () -> Unit) {
        AlertDialog.Builder(ctx)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Confirm") { _, _ -> onYes() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun info(ctx: Activity, title: String, message: String) {
        AlertDialog.Builder(ctx)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
