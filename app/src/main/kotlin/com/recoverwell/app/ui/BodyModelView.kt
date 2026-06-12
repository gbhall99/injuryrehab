package com.recoverwell.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import com.recoverwell.core.logic.Capability
import com.recoverwell.core.model.Side

/**
 * Digital-twin body model: side view of the lower leg and foot, showing the
 * boot, wedge stack, tendon state colour and weight-bearing status for the
 * current phase. Pure Canvas drawing - no assets, works offline.
 */
class BodyModelView(context: Context) : View(context) {

    var snapshot: Capability.Snapshot? = null
        set(value) {
            field = value
            contentDescription = value?.let {
                "Body model: ${it.bootStatus}. Tendon: ${it.tendonState}. ${it.weightBearing}."
            }
            invalidate()
        }
    var side: Side = Side.LEFT
        set(value) { field = value; invalidate() }

    private val skin = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE8C5A0.toInt() }
    private val skinEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF8D6E63.toInt(); style = Paint.Style.STROKE; strokeWidth = 4f
    }
    private val bootPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF37474F.toInt() }
    private val bootStrap = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF78909C.toInt() }
    private val wedgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFB8C00.toInt() }
    private val tendonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 14f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Ui.TEXT; textSize = 30f }
    private val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Ui.TEXT_DIM; textSize = 26f }
    private val groundPaint = Paint().apply { color = Ui.BORDER; strokeWidth = 5f }
    private val calloutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Ui.TEXT_DIM; strokeWidth = 3f; style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas) {
        val snap = snapshot ?: return
        canvas.drawColor(Ui.CARD)
        val cx = width * 0.40f
        val ground = height * 0.88f
        val scale = height / 420f

        // tendon colour by healing stage
        tendonPaint.color = when (snap.phaseNumber) {
            1 -> 0xFFD32F2F.toInt()
            2 -> 0xFFF57C00.toInt()
            3 -> 0xFFFBC02D.toInt()
            4 -> 0xFF7CB342.toInt()
            else -> 0xFF2E7D32.toInt()
        }

        val inBoot = snap.phaseNumber <= 2
        // equinus angle: more wedges = more pointed foot
        val wedgeRatio = if (snap.expectedWedges > 0 && snap.wedges > 0) snap.wedges.toFloat() / 5f else 0f
        val plantarDeg = if (inBoot) 12f + 18f * wedgeRatio else 0f

        val kneeY = ground - 290f * scale
        val ankleY = ground - 80f * scale
        val ankleX = cx

        // shank
        val shank = Path().apply {
            moveTo(cx - 34f * scale, kneeY)
            lineTo(cx + 34f * scale, kneeY)
            lineTo(ankleX + 26f * scale, ankleY)
            lineTo(ankleX - 26f * scale, ankleY)
            close()
        }
        canvas.drawPath(shank, skin)
        canvas.drawPath(shank, skinEdge)

        // foot, rotated by plantarflexion angle around the ankle
        canvas.save()
        canvas.rotate(plantarDeg, ankleX, ankleY)
        val foot = Path().apply {
            moveTo(ankleX - 30f * scale, ankleY - 6f * scale)
            lineTo(ankleX + 120f * scale, ankleY + 28f * scale)
            lineTo(ankleX + 116f * scale, ankleY + 58f * scale)
            lineTo(ankleX - 36f * scale, ankleY + 52f * scale)
            close()
        }
        canvas.drawPath(foot, skin)
        canvas.drawPath(foot, skinEdge)

        // Achilles tendon down the back of the heel
        canvas.drawLine(
            ankleX - 28f * scale, ankleY - 60f * scale,
            ankleX - 30f * scale, ankleY + 40f * scale, tendonPaint
        )

        if (inBoot) {
            // boot shell
            val boot = Path().apply {
                moveTo(ankleX - 48f * scale, kneeY + 60f * scale)
                lineTo(ankleX + 44f * scale, kneeY + 60f * scale)
                lineTo(ankleX + 130f * scale, ankleY + 50f * scale)
                lineTo(ankleX + 128f * scale, ankleY + 74f * scale)
                lineTo(ankleX - 52f * scale, ankleY + 74f * scale)
                close()
            }
            bootPaint.alpha = 120
            canvas.drawPath(boot, bootPaint)
            for (i in 0..2) {
                val y = kneeY + 80f * scale + i * 55f * scale
                canvas.drawRect(
                    ankleX - 50f * scale, y, ankleX + 40f * scale, y + 14f * scale, bootStrap
                )
            }
            // wedge stack under the heel
            for (w in 0 until snap.wedges) {
                val top = ankleY + 74f * scale - (w + 1) * 12f * scale
                canvas.drawRect(
                    ankleX - 50f * scale, top + 60f * scale,
                    ankleX + 10f * scale, top + 70f * scale, wedgePaint
                )
            }
        }
        canvas.restore()

        canvas.drawLine(0f, ground + 40f * scale, width.toFloat(), ground + 40f * scale, groundPaint)

        // callout to the tendon
        val tx = ankleX - 30f * scale
        val ty = ankleY - 10f * scale
        canvas.drawLine(tx, ty, width * 0.62f, height * 0.30f, calloutPaint)
        labelPaint.color = tendonPaint.color
        canvas.drawText("Achilles (${side.name.lowercase()})", width * 0.55f, height * 0.27f, labelPaint)
        labelPaint.color = Ui.TEXT

        // status text block
        var y = height * 0.36f
        fun line(t: String, p: Paint) { canvas.drawText(t, width * 0.55f, y, p); y += p.textSize + 12f }
        line("Week ${snap.weeksSinceInjury}", labelPaint)
        line("Phase ${snap.phaseNumber}", labelPaint)
        if (inBoot) line("Wedges: ${snap.wedges}", smallPaint)
        for (part in wrap(snap.weightBearing, 18)) line(part, smallPaint)
    }

    private fun wrap(text: String, max: Int): List<String> {
        val words = text.split(" ")
        val lines = ArrayList<String>()
        var cur = StringBuilder()
        for (w in words) {
            if (cur.isNotEmpty() && cur.length + w.length + 1 > max) {
                lines.add(cur.toString()); cur = StringBuilder()
            }
            if (cur.isNotEmpty()) cur.append(' ')
            cur.append(w)
        }
        if (cur.isNotEmpty()) lines.add(cur.toString())
        return lines
    }
}
