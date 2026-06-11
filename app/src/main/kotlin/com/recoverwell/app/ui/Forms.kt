package com.recoverwell.app.ui

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import java.time.LocalDate
import java.time.LocalTime

/** Form building blocks sized for one-handed use with the leg up. */
object Forms {

    fun label(ctx: Activity, text: String): TextView =
        Ui.text(ctx, text, 15f, Ui.TEXT_DIM, bold = true).apply {
            setPadding(0, Ui.dp(ctx, 10), 0, Ui.dp(ctx, 2))
        }

    fun editText(ctx: Activity, initial: String, hint: String, multiline: Boolean = false): EditText =
        EditText(ctx).apply {
            setText(initial)
            this.hint = hint
            textSize = 17f
            setTextColor(Ui.TEXT)
            background = Ui.roundedBg(Ui.CARD, strokeColor = Ui.BORDER)
            val p = Ui.dp(ctx, 12)
            setPadding(p, p, p, p)
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
        onChange: (Int) -> Unit
    ): LinearLayout {
        var value = initial
        val row = Ui.row(ctx)
        row.minimumHeight = Ui.dp(ctx, Ui.MIN_TOUCH_DP)
        val valueView = Ui.text(ctx, value.toString(), 20f, Ui.TEXT, bold = true).apply {
            gravity = Gravity.CENTER
            minWidth = Ui.dp(ctx, 56)
        }
        fun set(v: Int) {
            value = v.coerceIn(min, max)
            valueView.text = value.toString()
            onChange(value)
        }
        val minus = Ui.secondaryButton(ctx, "−") { set(value - 1) }
        val plus = Ui.secondaryButton(ctx, "+") { set(value + 1) }
        minus.contentDescription = "Decrease $title"
        plus.contentDescription = "Increase $title"
        row.addView(Ui.weight(Ui.text(ctx, title, 16f), 1f))
        row.addView(minus)
        row.addView(valueView)
        row.addView(plus)
        return row
    }

    fun dateRow(ctx: Activity, title: String, initial: LocalDate, onChange: (LocalDate) -> Unit): LinearLayout {
        val row = Ui.row(ctx)
        var current = initial
        val btn = Ui.secondaryButton(ctx, current.toString()) {}
        btn.setOnClickListener {
            DatePickerDialog(ctx, { _, y, m, d ->
                current = LocalDate.of(y, m + 1, d)
                btn.text = current.toString()
                onChange(current)
            }, current.year, current.monthValue - 1, current.dayOfMonth).show()
        }
        row.addView(Ui.weight(Ui.text(ctx, title, 16f), 1f))
        row.addView(btn)
        return row
    }

    fun timeButton(ctx: Activity, initial: LocalTime, onChange: (LocalTime) -> Unit): Button {
        var current = initial
        val btn = Ui.secondaryButton(ctx, "%02d:%02d".format(current.hour, current.minute)) {}
        btn.setOnClickListener {
            TimePickerDialog(ctx, { _, h, m ->
                current = LocalTime.of(h, m)
                btn.text = "%02d:%02d".format(h, m)
                onChange(current)
            }, current.hour, current.minute, true).show()
        }
        return btn
    }

    /** Single-select row of large toggle chips. */
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
                val chip = Ui.button(
                    ctx, labelOf(opt),
                    bg = if (isSel) Ui.PRIMARY else Ui.CARD,
                    fg = if (isSel) 0xFFFFFFFF.toInt() else Ui.TEXT
                ) {
                    current = opt
                    onSelect(opt)
                    rebuild()
                }
                if (!isSel) chip.background = Ui.roundedBg(Ui.CARD, strokeColor = Ui.BORDER)
                chip.textSize = 15f
                val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                lp.setMargins(Ui.dp(ctx, 2), 0, Ui.dp(ctx, 2), 0)
                chip.layoutParams = lp
                row.addView(chip)
            }
        }
        rebuild()
        return row
    }

    /** 0-10 slider with a live value readout, for the pain scale. */
    fun scaleSlider(ctx: Activity, max: Int, initial: Int?, onChange: (Int) -> Unit): LinearLayout {
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val readout = Ui.text(ctx, initial?.toString() ?: "-", 22f, Ui.PRIMARY, bold = true)
        readout.gravity = Gravity.CENTER
        val bar = SeekBar(ctx).apply {
            this.max = max
            progress = initial ?: 0
            minimumHeight = Ui.dp(ctx, Ui.MIN_TOUCH_DP)
            setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 12), Ui.dp(ctx, 16), Ui.dp(ctx, 12))
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
