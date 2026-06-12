package com.recoverwell.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import com.recoverwell.core.logic.TrendMath
import java.time.format.DateTimeFormatter

/** Offline line chart for recovery trends: series dots + line + 7-entry moving average. */
class ChartView(context: Context) : View(context) {

    var series: TrendMath.Series? = null
        set(value) {
            field = value
            contentDescription = value?.let {
                "Trend chart for ${it.label} with ${it.points.size} entries"
            } ?: "Trend chart, no data"
            invalidate()
        }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Ui.PRIMARY; strokeWidth = 5f; style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val avgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Ui.WARN; strokeWidth = 4f; style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(12f, 10f), 0f)
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Ui.PRIMARY }
    private val gridPaint = Paint().apply { color = Ui.BORDER; strokeWidth = 2f }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Ui.TEXT_DIM; textSize = 28f
    }

    private val dateFmt = DateTimeFormatter.ofPattern("d MMM")

    override fun onDraw(canvas: Canvas) {
        val s = series
        canvas.drawColor(Ui.CARD)
        val padL = 70f
        val padR = 24f
        val padT = 30f
        val padB = 60f
        val w = width - padL - padR
        val h = height - padT - padB

        if (s == null || s.points.isEmpty()) {
            canvas.drawText("No entries yet - add a daily log", padL, height / 2f, textPaint)
            return
        }

        val span = (s.max - s.min).takeIf { it > 0 } ?: 1.0
        // horizontal gridlines + y labels
        val steps = 4
        for (i in 0..steps) {
            val value = s.min + span * i / steps
            val y = padT + h - (h * i / steps)
            canvas.drawLine(padL, y, padL + w, y, gridPaint)
            canvas.drawText(trimNum(value), 8f, y + 9f, textPaint)
        }

        val first = s.points.first().date.toEpochDay()
        val last = s.points.last().date.toEpochDay()
        val daySpan = (last - first).coerceAtLeast(1).toFloat()
        fun x(epochDay: Long) = padL + w * (epochDay - first) / daySpan
        fun y(v: Double) = (padT + h - h * ((v - s.min) / span)).toFloat()

        fun pathOf(points: List<TrendMath.Point>): Path {
            val p = Path()
            points.forEachIndexed { i, pt ->
                if (i == 0) p.moveTo(x(pt.date.toEpochDay()), y(pt.value))
                else p.lineTo(x(pt.date.toEpochDay()), y(pt.value))
            }
            return p
        }

        if (s.points.size > 1) {
            canvas.drawPath(pathOf(s.points), linePaint)
            canvas.drawPath(pathOf(TrendMath.movingAverage(s.points, 7)), avgPaint)
        }
        for (pt in s.points) {
            canvas.drawCircle(x(pt.date.toEpochDay()), y(pt.value), 7f, dotPaint)
        }

        canvas.drawText(s.points.first().date.format(dateFmt), padL, height - 18f, textPaint)
        val endLabel = s.points.last().date.format(dateFmt)
        canvas.drawText(endLabel, padL + w - textPaint.measureText(endLabel), height - 18f, textPaint)
    }

    private fun trimNum(v: Double): String =
        if (v == Math.floor(v)) v.toInt().toString() else "%.1f".format(v)
}
