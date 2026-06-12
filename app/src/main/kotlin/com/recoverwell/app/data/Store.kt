package com.recoverwell.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.recoverwell.core.export.AppState
import com.recoverwell.core.export.BackupCodec
import com.recoverwell.core.json.Json
import com.recoverwell.core.model.*
import com.recoverwell.core.protocol.ProtocolContent
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

    // -- profile ----------------------------------------------------------

    fun profile(): Profile =
        getKv("profile")?.let { BackupCodec.profileFrom(Json.parse(it)) }
            ?: ProtocolContent.defaultProfile()

    fun saveProfile(p: Profile) = putKv("profile", Json.write(BackupCodec.profileJson(p)))

    // -- medications --------------------------------------------------------

    fun medications(): List<Medication> =
        getKv("medications")?.let { Json.parse(it).asArr().map(BackupCodec::medFrom) }
            ?: ProtocolContent.defaultMedications()

    fun saveMedications(meds: List<Medication>) =
        putKv("medications", Json.write(Json.arr(meds.map(BackupCodec::medJson))))

    // -- tasks ---------------------------------------------------------------

    fun tasks(): List<RehabTask> =
        getKv("tasks")?.let { Json.parse(it).asArr().map(BackupCodec::taskFrom) }
            ?: ProtocolContent.defaultTasks()

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

    // -- backup / restore ----------------------------------------------------------

    fun snapshot(): AppState = AppState(
        profile = profile(),
        medications = medications(),
        tasks = tasks(),
        exerciseOverrides = exerciseOverrides(),
        dailyLogs = allLogs(),
        events = allEvents()
    )

    fun restore(state: AppState) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM kv")
            db.execSQL("DELETE FROM logs")
            db.execSQL("DELETE FROM events")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        saveProfile(state.profile)
        saveMedications(state.medications)
        saveTasks(state.tasks)
        state.exerciseOverrides.values.forEach { saveExerciseOverride(it) }
        state.dailyLogs.forEach { saveDailyLog(it) }
        state.events.forEach { addEvent(it) }
    }
}
