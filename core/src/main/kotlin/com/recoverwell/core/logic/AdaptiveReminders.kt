package com.recoverwell.core.logic

import com.recoverwell.core.model.*
import java.time.LocalDate
import kotlin.math.abs

/**
 * Learns from when doses are *actually* logged and proposes better reminder
 * times, and flags slots that are routinely missed. Pure functions over the
 * event log - no behaviour is changed automatically; the user applies a
 * suggestion explicitly.
 */
object AdaptiveReminders {

    data class TimeSuggestion(
        val medId: String,
        val medName: String,
        val slotKey: String,
        val scheduledMinute: Int,
        val typicalMinute: Int
    )

    data class MissPattern(
        val medId: String,
        val medName: String,
        val slotKey: String,
        val missedRate: Double
    )

    private fun parseSlotMinute(slot: String): Int? {
        val parts = slot.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return h * 60 + m
    }

    /** Slots whose typical taken-time differs from the schedule by >= 30 min, with >= 4 samples. */
    fun timeSuggestions(
        meds: List<Medication>, events: List<EventLog>, today: LocalDate, days: Int = 21
    ): List<TimeSuggestion> {
        val out = ArrayList<TimeSuggestion>()
        val since = today.minusDays((days - 1).toLong())
        for (med in meds.filter { it.active }) {
            for (t in med.times) {
                val slot = ScheduleEngine.slotKey(t)
                val scheduled = t.hour * 60 + t.minute
                val takenMinutes = events.filter {
                    it.type == EventType.MEDICATION && it.refId == med.id && it.slotKey == slot &&
                        it.status == EventStatus.TAKEN && !it.date.isBefore(since)
                }.map { it.recordedAtMinuteOfDay }.sorted()
                if (takenMinutes.size < 4) continue
                val typical = median(takenMinutes)
                if (abs(typical - scheduled) >= 30) {
                    out.add(TimeSuggestion(med.id, "${med.name} ${med.dose}".trim(), slot, scheduled, typical))
                }
            }
        }
        return out
    }

    /** Slots missed (or never logged) on >= 40% of scheduled days, with enough history. */
    fun missPatterns(
        meds: List<Medication>, events: List<EventLog>, today: LocalDate, days: Int = 14
    ): List<MissPattern> {
        val out = ArrayList<MissPattern>()
        val dates = (0 until days).map { today.minusDays(it.toLong()) }
        for (med in meds.filter { it.active }) {
            for (t in med.times) {
                val slot = ScheduleEngine.slotKey(t)
                var missed = 0
                for (d in dates) {
                    val taken = events.any {
                        it.type == EventType.MEDICATION && it.refId == med.id &&
                            it.slotKey == slot && it.date == d && it.status == EventStatus.TAKEN
                    }
                    if (!taken) missed++
                }
                val rate = missed.toDouble() / days
                if (rate >= 0.4) out.add(MissPattern(med.id, "${med.name} ${med.dose}".trim(), slot, rate))
            }
        }
        return out
    }

    /** Apply a time suggestion to the medication list, returning the updated list. */
    fun applySuggestion(meds: List<Medication>, suggestion: TimeSuggestion): List<Medication> =
        meds.map { med ->
            if (med.id != suggestion.medId) med
            else med.copy(times = med.times.map { t ->
                if (ScheduleEngine.slotKey(t) == suggestion.slotKey)
                    java.time.LocalTime.of(suggestion.typicalMinute / 60, suggestion.typicalMinute % 60)
                else t
            })
        }

    private fun median(sorted: List<Int>): Int {
        val n = sorted.size
        return if (n % 2 == 1) sorted[n / 2] else (sorted[n / 2 - 1] + sorted[n / 2]) / 2
    }
}
