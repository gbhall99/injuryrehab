package com.recoverwell.core.export

import com.recoverwell.core.model.DailyLog
import com.recoverwell.core.model.EventLog

/** CSV export of recovery data. Plain RFC-4180-style quoting, no dependencies. */
object CsvExporter {

    fun dailyLogsCsv(logs: List<DailyLog>): String {
        val sb = StringBuilder()
        sb.appendLine("date,pain_0_10,swelling,rom_note,boot_worn_as_planned,wedges,weight_bearing,mood_1_5,energy_1_5,notes")
        for (l in logs.sortedBy { it.date }) {
            sb.appendLine(
                listOf(
                    l.date.toString(),
                    l.pain?.toString() ?: "",
                    l.swelling?.name ?: "",
                    l.romNote ?: "",
                    l.bootWornAsPlanned?.toString() ?: "",
                    l.wedges?.toString() ?: "",
                    l.weightBearing?.name ?: "",
                    l.mood?.toString() ?: "",
                    l.energy?.toString() ?: "",
                    l.notes ?: ""
                ).joinToString(",") { quote(it) }
            )
        }
        return sb.toString()
    }

    fun eventsCsv(events: List<EventLog>): String {
        val sb = StringBuilder()
        sb.appendLine("date,type,item,slot,status,recorded_at")
        for (e in events.sortedWith(compareBy({ it.date }, { it.recordedAtMinuteOfDay }))) {
            val hh = e.recordedAtMinuteOfDay / 60
            val mm = e.recordedAtMinuteOfDay % 60
            sb.appendLine(
                listOf(
                    e.date.toString(), e.type.name, e.refId, e.slotKey, e.status.name,
                    "%02d:%02d".format(hh, mm)
                ).joinToString(",") { quote(it) }
            )
        }
        return sb.toString()
    }

    fun quote(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else field
}
