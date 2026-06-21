package com.recoverwell.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.recoverwell.core.export.AppState
import com.recoverwell.core.export.BackupCodec
import com.recoverwell.core.json.Json
import com.recoverwell.core.model.*
import com.recoverwell.core.protocol.Defaults
import java.time.LocalDate

/**
 * Single-user offline store. All data lives in a private SQLite database in
 * app-internal storage: private by default, no network, no cloud. Volumes are
 * tiny (one row per day plus a few config docs), so access is synchronous.
 *
 * Domain (de)serialisation is delegated to the core module's BackupCodec so
 * the database rows, the backup file and the in-memory model can never drift
 * apart.
 */
class Store private constructor(context: Context) :
    SQLiteOpenHelper(context, "recoverwell.db", null, 1) {

    companion object {
        @Volatile private var instance: Store? = null

        fun get(context: Context): Store =
            instance ?: synchronized(this) {
                instance ?: Store(context.applicationContext).also { instance = it }
            }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE kv (k TEXT PRIMARY KEY, v TEXT NOT NULL)")
        db.execSQL("CREATE TABLE logs (date TEXT PRIMARY KEY, json TEXT NOT NULL)")
        db.execSQL("CREATE TABLE events (id TEXT PRIMARY KEY, date TEXT NOT NULL, json TEXT NOT NULL)")
        db.execSQL("CREATE INDEX idx_events_date ON events(date)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    // -- kv helpers -------------------------------------------------------

    private fun putKv(key: String, value: String) {
        writableDatabase.execSQL("INSERT OR REPLACE INTO kv (k, v) VALUES (?, ?)", arrayOf(key, value))
    }

    private fun getKv(key: String): String? =
        readableDatabase.rawQuery("SELECT v FROM kv WHERE k = ?", arrayOf(key)).use {
            if (it.moveToFirst()) it.getString(0) else null
        }

    // -- app settings (theme etc.) ------------------------------------------

    fun setting(key: String, default: String): String = getKv("setting_$key") ?: default

    fun saveSetting(key: String, value: String) = putKv("setting_$key", value)

    /** Daily exercise sessions the user chose at setup (1-3); each is the full routine. */
    fun exerciseSessions(): Int =
        com.recoverwell.core.logic.ScheduleEngine.clampSessions(
            setting("exercise_sessions", "3").toIntOrNull()
                ?: com.recoverwell.core.logic.ScheduleEngine.EXERCISE_SESSIONS_PER_DAY
        )

    fun saveExerciseSessions(n: Int) =
        saveSetting("exercise_sessions",
            com.recoverwell.core.logic.ScheduleEngine.clampSessions(n).toString())

    // -- profile ----------------------------------------------------------

    fun profile(): Profile =
        getKv("profile")?.let { BackupCodec.profileFrom(Json.parse(it)) }
            ?: Defaults.profile()

    fun saveProfile(p: Profile) = putKv("profile", Json.write(BackupCodec.profileJson(p)))

    // -- medications --------------------------------------------------------

    fun medications(): List<Medication> =
        getKv("medications")?.let { Json.parse(it).asArr().map(BackupCodec::medFrom) }
            ?: Defaults.medications()

    fun saveMedications(meds: List<Medication>) =
        putKv("medications", Json.write(Json.arr(meds.map(BackupCodec::medJson))))

    // -- tasks ---------------------------------------------------------------

    fun tasks(): List<RehabTask> =
        getKv("tasks")?.let { Json.parse(it).asArr().map(BackupCodec::taskFrom) }
            ?: Defaults.tasks()

    fun saveTasks(tasks: List<RehabTask>) =
        putKv("tasks", Json.write(Json.arr(tasks.map(BackupCodec::taskJson))))

    // -- exercise overrides ----------------------------------------------------

    fun exerciseOverrides(): Map<String, ExerciseOverride> =
        getKv("exerciseOverrides")
            ?.let { Json.parse(it).asArr().map(BackupCodec::overrideFrom).associateBy { o -> o.exerciseId } }
            ?: emptyMap()

    fun saveExerciseOverride(o: ExerciseOverride) {
        val all = exerciseOverrides().toMutableMap()
        all[o.exerciseId] = o
        putKv("exerciseOverrides", Json.write(Json.arr(all.values.map(BackupCodec::overrideJson))))
    }

    // -- daily logs ----------------------------------------------------------

    fun dailyLog(date: LocalDate): DailyLog =
        readableDatabase.rawQuery("SELECT json FROM logs WHERE date = ?", arrayOf(date.toString())).use {
            if (it.moveToFirst()) BackupCodec.logFrom(Json.parse(it.getString(0))) else DailyLog.empty(date)
        }

    fun saveDailyLog(log: DailyLog) {
        writableDatabase.execSQL(
            "INSERT OR REPLACE INTO logs (date, json) VALUES (?, ?)",
            arrayOf(log.date.toString(), Json.write(BackupCodec.logJson(log)))
        )
    }

    fun allLogs(): List<DailyLog> =
        readableDatabase.rawQuery("SELECT json FROM logs ORDER BY date", emptyArray()).use { c ->
            val out = ArrayList<DailyLog>()
            while (c.moveToNext()) out.add(BackupCodec.logFrom(Json.parse(c.getString(0))))
            out
        }

    // -- events -----------------------------------------------------------------

    fun addEvent(e: EventLog) {
        writableDatabase.execSQL(
            "INSERT OR REPLACE INTO events (id, date, json) VALUES (?, ?, ?)",
            arrayOf(e.id, e.date.toString(), Json.write(BackupCodec.eventJson(e)))
        )
    }

    // ordered by insertion so "the last event for a slot wins" is well-defined
    fun eventsOn(date: LocalDate): List<EventLog> =
        readableDatabase.rawQuery(
            "SELECT json FROM events WHERE date = ? ORDER BY rowid", arrayOf(date.toString())
        ).use { c ->
            val out = ArrayList<EventLog>()
            while (c.moveToNext()) out.add(BackupCodec.eventFrom(Json.parse(c.getString(0))))
            out
        }

    fun allEvents(): List<EventLog> =
        readableDatabase.rawQuery("SELECT json FROM events ORDER BY rowid", emptyArray()).use { c ->
            val out = ArrayList<EventLog>()
            while (c.moveToNext()) out.add(BackupCodec.eventFrom(Json.parse(c.getString(0))))
            out
        }

    // -- self-test results (return-to-sport) ---------------------------------

    fun selfTestResults(): List<SelfTestResult> =
        getKv("selfTestResults")?.let { Json.parse(it).asArr().map(BackupCodec::selfTestFrom) }
            ?: emptyList()

    fun saveSelfTestResult(r: SelfTestResult) {
        val all = selfTestResults() + r
        putKv("selfTestResults", Json.write(Json.arr(all.map(BackupCodec::selfTestJson))))
    }

    fun deleteSelfTestResult(id: String) {
        val all = selfTestResults().filter { it.id != id }
        putKv("selfTestResults", Json.write(Json.arr(all.map(BackupCodec::selfTestJson))))
    }

    fun rtsSignoffs(): Set<String> =
        getKv("rtsSignoffs")?.let { Json.parse(it).asArr().map { v -> v.asString() }.toSet() }
            ?: emptySet()

    fun setRtsSignoff(rungId: String, on: Boolean) {
        val all = if (on) rtsSignoffs() + rungId else rtsSignoffs() - rungId
        putKv("rtsSignoffs", Json.write(Json.strings(all.toList())))
    }

    // -- physio loop ----------------------------------------------------------

    fun physioNotes(): List<PhysioNote> =
        getKv("physioNotes")?.let { Json.parse(it).asArr().map(BackupCodec::physioNoteFrom) }
            ?: emptyList()

    private fun savePhysioNotes(notes: List<PhysioNote>) =
        putKv("physioNotes", Json.write(Json.arr(notes.map(BackupCodec::physioNoteJson))))

    fun addPhysioNote(n: PhysioNote) = savePhysioNotes(physioNotes() + n)

    fun deletePhysioNote(id: String) = savePhysioNotes(physioNotes().filter { it.id != id })

    /** User's own questions to raise next visit (prep notes, not clinical record). */
    fun physioQuestions(): List<String> =
        getKv("physioQuestions")?.let { Json.parse(it).asArr().map { v -> v.asString() } } ?: emptyList()

    fun savePhysioQuestions(qs: List<String>) =
        putKv("physioQuestions", Json.write(Json.strings(qs)))

    // -- recovery journal -----------------------------------------------------

    fun journalEntries(): List<JournalEntry> =
        getKv("journalEntries")?.let { Json.parse(it).asArr().map(BackupCodec::journalEntryFrom) }
            ?: emptyList()

    private fun saveJournal(entries: List<JournalEntry>) =
        putKv("journalEntries", Json.write(Json.arr(entries.map(BackupCodec::journalEntryJson))))

    fun addJournalEntry(e: JournalEntry) = saveJournal(journalEntries() + e)

    /** One entry per day: replace any existing entry on the same date. */
    fun upsertJournalEntry(e: JournalEntry) = saveJournal(journalEntries().filter { it.date != e.date } + e)

    /** Edit an existing entry in place (matched by id). */
    fun updateJournalEntry(e: JournalEntry) = saveJournal(journalEntries().map { if (it.id == e.id) e else it })

    fun deleteJournalEntry(id: String) = saveJournal(journalEntries().filter { it.id != id })

    // -- Ask my recovery: persisted conversation + cross-chat memory -----------
    // The active transcript survives leaving the screen so a conversation resumes
    // where it left off. The memory bank is a compact, longer-lived record of past
    // exchanges that persists across "Clear" so the assistant keeps continuity
    // between separate chats. Both live in kv (device-only), like other AI caches.

    fun askTurns(): List<Pair<String, String>> =
        getKv("ask_turns")?.let {
            Json.parse(it).asArr().map { o -> o.get("role").asString() to o.get("content").asString() }
        } ?: emptyList()

    fun saveAskTurns(turns: List<Pair<String, String>>) =
        putKv("ask_turns", Json.write(Json.arr(turns.takeLast(40).map {
            Json.obj("role" to Json.of(it.first), "content" to Json.of(it.second))
        })))

    fun clearAskTurns() = saveAskTurns(emptyList())

    fun askMemory(): List<String> =
        getKv("ask_memory")?.let { Json.parse(it).asArr().map { v -> v.asString() } } ?: emptyList()

    fun saveAskMemory(notes: List<String>) =
        putKv("ask_memory", Json.write(Json.strings(notes.takeLast(24))))

    fun clearAskMemory() = saveAskMemory(emptyList())

    // -- AI weekly summary cache (keyed by week-start date) --------------------

    fun cachedWeeklySummary(weekStart: LocalDate): String = setting("weekly_summary_$weekStart", "")

    fun saveWeeklySummary(weekStart: LocalDate, text: String) =
        saveSetting("weekly_summary_$weekStart", text)

    // -- journal red-flag alert (set when a check-in mentions urgent symptoms) -

    fun redFlagAlert(): Pair<LocalDate, String>? {
        val d = setting("journal_redflag_date", "")
        if (d.isBlank()) return null
        return try { LocalDate.parse(d) to setting("journal_redflag_note", "") } catch (e: Exception) { null }
    }

    fun setRedFlagAlert(date: LocalDate, note: String) {
        saveSetting("journal_redflag_date", date.toString())
        saveSetting("journal_redflag_note", note)
    }

    fun clearRedFlagAlert() {
        saveSetting("journal_redflag_date", "")
        saveSetting("journal_redflag_note", "")
    }

    // -- backup / restore ----------------------------------------------------------

    fun snapshot(): AppState = AppState(
        profile = profile(),
        medications = medications(),
        tasks = tasks(),
        exerciseOverrides = exerciseOverrides(),
        dailyLogs = allLogs(),
        events = allEvents(),
        selfTestResults = selfTestResults(),
        rtsSignoffs = rtsSignoffs().toList(),
        physioNotes = physioNotes(),
        journalEntries = journalEntries()
    )

    fun restore(state: AppState) {
        // One transaction for the whole wipe-and-repopulate: every putKv/save* below
        // runs on this same connection and enlists in it, so a crash (or a malformed
        // row) mid-restore rolls back to the prior data instead of leaving the DB
        // half-wiped. For an offline-only app with no cloud copy, atomicity is vital.
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM kv")
            db.execSQL("DELETE FROM logs")
            db.execSQL("DELETE FROM events")
            saveProfile(state.profile)
            saveMedications(state.medications)
            saveTasks(state.tasks)
            if (state.exerciseOverrides.isNotEmpty())
                putKv("exerciseOverrides", Json.write(Json.arr(state.exerciseOverrides.values.map(BackupCodec::overrideJson))))
            state.dailyLogs.forEach { saveDailyLog(it) }
            state.events.forEach { addEvent(it) }
            if (state.selfTestResults.isNotEmpty())
                putKv("selfTestResults", Json.write(Json.arr(state.selfTestResults.map(BackupCodec::selfTestJson))))
            if (state.rtsSignoffs.isNotEmpty())
                putKv("rtsSignoffs", Json.write(Json.strings(state.rtsSignoffs)))
            if (state.physioNotes.isNotEmpty())
                putKv("physioNotes", Json.write(Json.arr(state.physioNotes.map(BackupCodec::physioNoteJson))))
            if (state.journalEntries.isNotEmpty())
                putKv("journalEntries", Json.write(Json.arr(state.journalEntries.map(BackupCodec::journalEntryJson))))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
