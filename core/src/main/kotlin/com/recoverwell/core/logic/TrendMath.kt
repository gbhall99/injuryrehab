package com.recoverwell.core.logic

import com.recoverwell.core.model.DailyLog
import com.recoverwell.core.model.JournalEntry
import java.time.LocalDate

/** Chart-ready series extraction from daily logs. Pure math, UI-agnostic. */
object TrendMath {

    data class Point(val date: LocalDate, val value: Double)

    data class Series(val label: String, val points: List<Point>, val min: Double, val max: Double)

    fun pain(logs: List<DailyLog>): Series =
        series("Pain (0-10)", logs.mapNotNull { l -> l.pain?.let { Point(l.date, it.toDouble()) } }, 0.0, 10.0)

    fun swelling(logs: List<DailyLog>): Series =
        series("Swelling (0-3)", logs.mapNotNull { l -> l.swelling?.let { Point(l.date, it.score.toDouble()) } }, 0.0, 3.0)

    fun mood(logs: List<DailyLog>): Series =
        series("Mood (1-5)", logs.mapNotNull { l -> l.mood?.let { Point(l.date, it.toDouble()) } }, 1.0, 5.0)

    fun energy(logs: List<DailyLog>): Series =
        series("Energy (1-5)", logs.mapNotNull { l -> l.energy?.let { Point(l.date, it.toDouble()) } }, 1.0, 5.0)

    fun journalMood(entries: List<JournalEntry>): Series =
        series("Journal mood (1-5)", entries.map { Point(it.date, it.mood.score.toDouble()) }, 1.0, 5.0)

    private fun series(label: String, points: List<Point>, min: Double, max: Double) =
        Series(label, points.sortedBy { it.date }, min, max)

    /** Trailing moving average over [window] calendar entries (not days). */
    fun movingAverage(points: List<Point>, window: Int): List<Point> {
        if (points.isEmpty() || window <= 1) return points
        return points.indices.map { i ->
            val from = (i - window + 1).coerceAtLeast(0)
            val slice = points.subList(from, i + 1)
            Point(points[i].date, slice.sumOf { it.value } / slice.size)
        }
    }

    /** Percentage of scheduled occurrences marked done/taken, for adherence stats. */
    fun adherence(done: Int, total: Int): Int =
        if (total <= 0) 0 else ((done * 100.0) / total).toInt()

    /** At/above this percentage, adherence is "strong". Shared so every feature agrees. */
    const val ADHERENCE_STRONG = 90
    /** Below this percentage (but above 0), adherence is "slipping". */
    const val ADHERENCE_SLIPPING = 80

    /**
     * Split [items] into the trailing [windowDays] window and the equal window
     * just before it, both bounded so future-dated items never leak into the
     * "recent" side. Shared by every recent-vs-prior comparison so the windows
     * (and the future-date guard) can't drift between features. Pure.
     */
    fun <T> twoWindows(
        items: List<T>, today: LocalDate, windowDays: Int = 7, date: (T) -> LocalDate
    ): Pair<List<T>, List<T>> {
        val recentStart = today.minusDays((windowDays - 1).toLong())
        val priorStart = today.minusDays((2L * windowDays - 1))
        val recent = items.filter { !date(it).isBefore(recentStart) && !date(it).isAfter(today) }
        val prior = items.filter { !date(it).isBefore(priorStart) && date(it).isBefore(recentStart) }
        return recent to prior
    }
}
