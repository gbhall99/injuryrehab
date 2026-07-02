package com.recoverwell.app.ui

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.os.Build
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Form building blocks sized for one-handed use with the leg up. */
object Forms {

    private val DATE_FMT = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
    private val TIME_FMT = DateTimeFormatter.ofPattern("h:mm a")

    /** Human-friendly date: Today/Tomorrow/Yesterday, else "Tue, 1 Jul 2026". */
    fun friendlyDate(d: LocalDate): String {
        val today = LocalDate.now()
        return when (d) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            today.minusDays(1) -> "Yesterday"
            else -> d.format(DATE_FMT)
        }
    }

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
            setHintTextColor(Ui.TEXT_DIM)
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
        btn = Ui.tonalButton(ctx, friendlyDate(current)) {
            DatePickerDialog(ctx, { _, y, m, d ->
                current = LocalDate.of(y, m + 1, d)
                btn?.text = friendlyDate(current)
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
        btn = Ui.tonalButton(ctx, current.format(TIME_FMT)) {
            TimePickerDialog(ctx, { _, h, m ->
                current = LocalTime.of(h, m)
                btn?.text = current.format(TIME_FMT)
                onChange(current)
            }, current.hour, current.minute, false).show()
        }
        return btn
    }

    /**
     * Editable list of reminder times rendered into [card]: each existing time is a
     * picker plus a remove button, followed by an "Add a time" button. Mutates
     * [times] in place. Shared by the medication and task editors.
     */
    fun timeListEditor(ctx: Activity, card: LinearLayout, times: MutableList<LocalTime>) {
        fun rebuild() {
            card.removeAllViews()
            times.sorted().forEachIndexed { i, t ->
                val row = Ui.row(ctx)
                // moderate corner radius (not a full stadium) so stacked time
                // fields read as separate rows instead of pinching together
                val tb = timeButton(ctx, t) { new -> times[i] = new }
                tb.background = Ui.ripple(ctx, Ui.rounded(Ui.PRIMARY_CONTAINER, Ui.RADIUS_SMALL))
                row.addView(Ui.weight(tb, 1f))
                row.addView(Ui.iconButton(ctx, "ic_close", Ui.TEXT_DIM, desc = "Remove time") {
                    times.removeAt(i); rebuild()
                })
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                if (i > 0) lp.topMargin = Ui.dp(ctx, 8)
                card.addView(row, lp)
            }
            card.addView(Ui.fullWidth(Ui.textButton(ctx, "Add a time") {
                times.add(LocalTime.of(12, 0)); rebuild()
            }, ctx, 4))
        }
        rebuild()
    }

    /** On/off segmented row - the common boolean case of [choiceRow]. */
    fun toggle(
        ctx: Activity,
        current: Boolean,
        onLabel: String = "On",
        offLabel: String = "Off",
        onSelect: (Boolean) -> Unit
    ): LinearLayout =
        choiceRow(ctx, listOf(true, false), { if (it) onLabel else offLabel }, current, onSelect)

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
                val chip = Ui.text(ctx, labelOf(opt), 14f,
                    if (isSel) Ui.ON_PRIMARY_CONTAINER else Ui.TEXT, bold = true).apply {
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
            // screen-reader: a stable label plus a live value announcement
            contentDescription = buildString {
                append("Rating from 0")
                minLabel?.let { append(" (").append(it).append(")") }
                append(" to ").append(max)
                maxLabel?.let { append(" (").append(it).append(")") }
            }
            if (Build.VERSION.SDK_INT >= 30) stateDescription = "${initial ?: 0} of $max"
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    readout.text = progress.toString()
                    if (Build.VERSION.SDK_INT >= 30) seekBar?.stateDescription = "$progress of $max"
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

    fun confirm(
        ctx: Activity, title: String, message: String,
        confirmLabel: String = "Confirm", destructive: Boolean = false, onYes: () -> Unit
    ) {
        val dialog = AlertDialog.Builder(ctx)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(confirmLabel) { _, _ -> onYes() }
            .setNegativeButton("Cancel", null)
            .create()
        // tint the confirm action red when it deletes/undoes something
        if (destructive) dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Ui.DANGER)
        }
        dialog.show()
    }

    fun info(ctx: Activity, title: String, message: String) {
        AlertDialog.Builder(ctx)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
