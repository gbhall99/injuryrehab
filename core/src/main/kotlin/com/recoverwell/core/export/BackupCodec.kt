package com.recoverwell.core.export

import com.recoverwell.core.json.Json
import com.recoverwell.core.json.JsonValue
import com.recoverwell.core.model.*
import java.time.LocalDate
import java.time.LocalTime

/** Aggregate of everything the app persists; the unit of backup/restore. */
data class AppState(
    val profile: Profile,
    val medications: List<Medication>,
    val tasks: List<RehabTask>,
    val exerciseOverrides: Map<String, ExerciseOverride>,
    val dailyLogs: List<DailyLog>,
    val events: List<EventLog>,
    val selfTestResults: List<SelfTestResult> = emptyList(),
    /** Ids of return-to-sport rungs the user recorded physio clearance for. */
    val rtsSignoffs: List<String> = emptyList(),
    val physioNotes: List<PhysioNote> = emptyList(),
    val journalEntries: List<JournalEntry> = emptyList()
)

/**
 * Full-fidelity JSON backup/restore. Versioned so future schema changes can
 * migrate old backups instead of rejecting them.
 */
object BackupCodec {

    const val VERSION = 5

    fun encode(state: AppState): String = Json.write(
        Json.obj(
            "app" to Json.of("RecoverWell"),
            "version" to Json.of(VERSION),
            "profile" to profileJson(state.profile),
            "medications" to Json.arr(state.medications.map { medJson(it) }),
            "tasks" to Json.arr(state.tasks.map { taskJson(it) }),
            "exerciseOverrides" to Json.arr(state.exerciseOverrides.values.map { overrideJson(it) }),
            "dailyLogs" to Json.arr(state.dailyLogs.map { logJson(it) }),
            "events" to Json.arr(state.events.map { eventJson(it) }),
            "selfTestResults" to Json.arr(state.selfTestResults.map { selfTestJson(it) }),
            "rtsSignoffs" to Json.strings(state.rtsSignoffs),
            "physioNotes" to Json.arr(state.physioNotes.map { physioNoteJson(it) }),
            "journalEntries" to Json.arr(state.journalEntries.map { journalEntryJson(it) })
        )
    )

    fun decode(text: String): AppState {
        val root = Json.parse(text)
        // treat a missing version as the oldest schema (v1) rather than rejecting it
        val version = root.opt("version")?.asInt() ?: 1
        require(version in 1..VERSION) { "Unsupported backup version: $version" }
        return AppState(
            profile = profileFrom(root.get("profile")),
            medications = root.get("medications").asArr().map { medFrom(it) },
            tasks = root.get("tasks").asArr().map { taskFrom(it) },
            exerciseOverrides = root.get("exerciseOverrides").asArr()
                .map { overrideFrom(it) }.associateBy { it.exerciseId },
            dailyLogs = root.get("dailyLogs").asArr().map { logFrom(it) },
            events = root.get("events").asArr().map { eventFrom(it) },
            // v<3 backups predate the return-to-sport program
            selfTestResults = (root.opt("selfTestResults")?.asArr() ?: emptyList()).map { selfTestFrom(it) },
            rtsSignoffs = (root.opt("rtsSignoffs")?.asArr() ?: emptyList()).map { it.asString() },
            // v<4 backups predate the physio loop
            physioNotes = (root.opt("physioNotes")?.asArr() ?: emptyList()).map { physioNoteFrom(it) },
            // the AI recovery journal is newer still; absent in older backups
            journalEntries = (root.opt("journalEntries")?.asArr() ?: emptyList()).map { journalEntryFrom(it) }
        )
    }

    // -- journal entry ------------------------------------------------------

    fun journalEntryJson(e: JournalEntry): JsonValue = Json.obj(
        "id" to Json.of(e.id),
        "date" to Json.of(e.date.toString()),
        "transcript" to Json.of(e.transcript),
        "reflection" to Json.of(e.reflection),
        "insights" to Json.strings(e.insights),
        "tips" to Json.strings(e.tips),
        "mood" to Json.of(e.mood.name)
    )

    fun journalEntryFrom(j: JsonValue): JournalEntry = JournalEntry(
        id = j.get("id").asString(),
        date = LocalDate.parse(j.get("date").asString()),
        transcript = j.opt("transcript")?.asString() ?: "",
        reflection = j.opt("reflection")?.asString() ?: "",
        insights = (j.opt("insights")?.asArr() ?: emptyList()).map { it.asString() },
        tips = (j.opt("tips")?.asArr() ?: emptyList()).map { it.asString() },
        mood = JournalMood.from(j.opt("mood")?.asString())
    )

    // -- physio note --------------------------------------------------------

    fun physioNoteJson(n: PhysioNote): JsonValue = Json.obj(
        "id" to Json.of(n.id),
        "date" to Json.of(n.date.toString()),
        "text" to Json.of(n.text)
    )

    fun physioNoteFrom(j: JsonValue): PhysioNote = PhysioNote(
        id = j.get("id").asString(),
        date = LocalDate.parse(j.get("date").asString()),
        text = j.opt("text")?.asString() ?: ""
    )

    // -- self-test result ---------------------------------------------------

    fun selfTestJson(s: SelfTestResult): JsonValue = Json.obj(
        "id" to Json.of(s.id),
        "testId" to Json.of(s.testId),
        "date" to Json.of(s.date.toString()),
        "injuredValue" to Json.of(s.injuredValue),
        "otherValue" to Json.of(s.otherValue),
        "painFree" to Json.of(s.painFree),
        "note" to Json.of(s.note)
    )

    fun selfTestFrom(j: JsonValue): SelfTestResult = SelfTestResult(
        id = j.get("id").asString(),
        testId = j.get("testId").asString(),
        date = LocalDate.parse(j.get("date").asString()),
        injuredValue = j.get("injuredValue").asDouble(),
        otherValue = j.opt("otherValue")?.asDouble(),
        painFree = j.opt("painFree")?.asBool() ?: true,
        note = j.opt("note")?.asString() ?: ""
    )

    // -- profile --------------------------------------------------------

    fun profileJson(p: Profile): JsonValue = Json.obj(
        "protocolId" to Json.of(p.protocolId),
        "name" to Json.of(p.name),
        "injuryDate" to Json.of(p.injuryDate.toString()),
        "side" to Json.of(p.side.name),
        "pathway" to Json.of(p.pathway.name),
        "injuryDescription" to Json.of(p.injuryDescription),
        "goal" to Json.of(p.goal),
        "appointments" to Json.arr(p.appointments.map {
            Json.obj(
                "date" to Json.of(it.date.toString()),
                "label" to Json.of(it.label),
                "completed" to Json.of(it.completed),
                "id" to Json.of(it.id),
                "with" to Json.of(it.withWhom)
            )
        }),
        "wedgePlan" to Json.obj(
            "initialWedges" to Json.of(p.wedgePlan.initialWedges),
            "removalStartWeek" to Json.of(p.wedgePlan.removalStartWeek),
            "removalIntervalDays" to Json.of(p.wedgePlan.removalIntervalDays),
            "stepSize" to Json.of(p.wedgePlan.stepSize)
        ),
        "wedgeDateOverrides" to JsonValue.Obj(
            p.wedgeDateOverrides.entries.associate { (k, v) -> k.toString() to Json.of(v.toString()) }
        ),
        "currentWedges" to Json.of(p.currentWedges),
        "weightBearing" to Json.of(p.weightBearing.name),
        "physioConfirmedPhase" to Json.of(p.physioConfirmedPhase),
        "phaseStartOverrides" to JsonValue.Obj(
            p.phaseStartOverrides.entries.associate { (k, v) -> k.toString() to Json.of(v.toString()) }
        ),
        "phaseConfirmedDates" to JsonValue.Obj(
            p.phaseConfirmedDates.entries.associate { (k, v) -> k.toString() to Json.of(v.toString()) }
        ),
        "onboardingComplete" to Json.of(p.onboardingComplete),
        "disclaimerAcknowledged" to Json.of(p.disclaimerAcknowledged),
        "sportId" to Json.of(p.sportId),
        "deviceId" to Json.of(p.deviceId)
    )

    fun profileFrom(j: JsonValue): Profile = Profile(
        // v1 backups predate multi-protocol support: they are Achilles ones
        protocolId = j.opt("protocolId")?.asString()
            ?: com.recoverwell.core.protocol.ProtocolRegistry.default.id,
        name = j.opt("name")?.asString() ?: "",
        injuryDate = LocalDate.parse(j.get("injuryDate").asString()),
        side = Side.valueOf(j.get("side").asString()),
        pathway = Pathway.valueOf(j.get("pathway").asString()),
        injuryDescription = j.opt("injuryDescription")?.asString() ?: "",
        goal = j.opt("goal")?.asString() ?: "",
        appointments = (j.opt("appointments")?.asArr() ?: emptyList()).map {
            Appointment(
                LocalDate.parse(it.get("date").asString()),
                it.get("label").asString(),
                it.opt("completed")?.asBool() ?: false,
                it.opt("id")?.asString() ?: "",
                it.opt("with")?.asString() ?: ""
            )
        },
        wedgePlan = j.get("wedgePlan").let {
            WedgePlan(
                it.get("initialWedges").asInt(),
                it.get("removalStartWeek").asInt(),
                it.get("removalIntervalDays").asInt(),
                it.opt("stepSize")?.asInt() ?: 1
            )
        },
        // v<5 backups predate per-step pinned boot-change dates
        wedgeDateOverrides = (j.opt("wedgeDateOverrides")?.asObj() ?: emptyMap())
            .entries.associate { (k, v) -> k.toInt() to LocalDate.parse(v.asString()) },
        currentWedges = j.get("currentWedges").asInt(),
        weightBearing = WeightBearing.valueOf(j.get("weightBearing").asString()),
        physioConfirmedPhase = j.get("physioConfirmedPhase").asInt(),
        phaseStartOverrides = (j.opt("phaseStartOverrides")?.asObj() ?: emptyMap())
            .entries.associate { (k, v) -> k.toInt() to LocalDate.parse(v.asString()) },
        phaseConfirmedDates = (j.opt("phaseConfirmedDates")?.asObj() ?: emptyMap())
            .entries.associate { (k, v) -> k.toInt() to LocalDate.parse(v.asString()) },
        onboardingComplete = j.opt("onboardingComplete")?.asBool() ?: false,
        disclaimerAcknowledged = j.opt("disclaimerAcknowledged")?.asBool() ?: false,
        sportId = j.opt("sportId")?.asString() ?: "",
        deviceId = j.opt("deviceId")?.asString() ?: ""
    )

    // -- medication -----------------------------------------------------

    fun medJson(m: Medication): JsonValue = Json.obj(
        "id" to Json.of(m.id),
        "name" to Json.of(m.name),
        "dose" to Json.of(m.dose),
        "times" to Json.strings(m.times.map { it.toString() }),
        "notes" to Json.of(m.notes),
        "active" to Json.of(m.active)
    )

    fun medFrom(j: JsonValue): Medication = Medication(
        id = j.get("id").asString(),
        name = j.get("name").asString(),
        dose = j.get("dose").asString(),
        times = j.get("times").asArr().map { LocalTime.parse(it.asString()) },
        notes = j.opt("notes")?.asString() ?: "",
        active = j.opt("active")?.asBool() ?: true
    )

    // -- task -------------------------------------------------------------

    fun taskJson(t: RehabTask): JsonValue = Json.obj(
        "id" to Json.of(t.id),
        "kind" to Json.of(t.kind.name),
        "title" to Json.of(t.title),
        "detail" to Json.of(t.detail),
        "times" to Json.strings(t.times.map { it.toString() }),
        "fromPhase" to Json.of(t.fromPhase),
        "toPhase" to Json.of(t.toPhase),
        "dueDate" to Json.of(t.dueDate?.toString()),
        "active" to Json.of(t.active)
    )

    fun taskFrom(j: JsonValue): RehabTask = RehabTask(
        id = j.get("id").asString(),
        kind = TaskKind.valueOf(j.get("kind").asString()),
        title = j.get("title").asString(),
        detail = j.opt("detail")?.asString() ?: "",
        times = j.get("times").asArr().map { LocalTime.parse(it.asString()) },
        fromPhase = j.get("fromPhase").asInt(),
        toPhase = j.get("toPhase").asInt(),
        dueDate = j.opt("dueDate")?.asString()?.let { LocalDate.parse(it) },
        active = j.opt("active")?.asBool() ?: true
    )

    // -- exercise override ------------------------------------------------

    fun overrideJson(o: ExerciseOverride): JsonValue = Json.obj(
        "exerciseId" to Json.of(o.exerciseId),
        "sets" to Json.of(o.sets),
        "reps" to Json.of(o.reps),
        "holdSeconds" to Json.of(o.holdSeconds),
        "sessionsPerDay" to Json.of(o.sessionsPerDay),
        "enabled" to Json.of(o.enabled),
        "videoId" to Json.of(o.videoId)
    )

    fun overrideFrom(j: JsonValue): ExerciseOverride = ExerciseOverride(
        exerciseId = j.get("exerciseId").asString(),
        sets = j.opt("sets")?.asInt(),
        reps = j.opt("reps")?.asInt(),
        holdSeconds = j.opt("holdSeconds")?.asInt(),
        sessionsPerDay = j.opt("sessionsPerDay")?.asInt(),
        enabled = j.opt("enabled")?.asBool() ?: true,
        videoId = j.opt("videoId")?.asString()
    )

    // -- daily log ----------------------------------------------------------

    fun logJson(l: DailyLog): JsonValue = Json.obj(
        "date" to Json.of(l.date.toString()),
        "pain" to Json.of(l.pain),
        "swelling" to Json.of(l.swelling?.name),
        "romNote" to Json.of(l.romNote),
        "bootWornAsPlanned" to Json.of(l.bootWornAsPlanned),
        "wedges" to Json.of(l.wedges),
        "weightBearing" to Json.of(l.weightBearing?.name),
        "mood" to Json.of(l.mood),
        "energy" to Json.of(l.energy),
        "notes" to Json.of(l.notes)
    )

    fun logFrom(j: JsonValue): DailyLog = DailyLog(
        date = LocalDate.parse(j.get("date").asString()),
        pain = j.opt("pain")?.asInt(),
        swelling = j.opt("swelling")?.asString()?.let { Swelling.valueOf(it) },
        romNote = j.opt("romNote")?.asString(),
        bootWornAsPlanned = j.opt("bootWornAsPlanned")?.asBool(),
        wedges = j.opt("wedges")?.asInt(),
        weightBearing = j.opt("weightBearing")?.asString()?.let { WeightBearing.valueOf(it) },
        mood = j.opt("mood")?.asInt(),
        energy = j.opt("energy")?.asInt(),
        notes = j.opt("notes")?.asString()
    )

    // -- event ---------------------------------------------------------------

    fun eventJson(e: EventLog): JsonValue = Json.obj(
        "id" to Json.of(e.id),
        "date" to Json.of(e.date.toString()),
        "type" to Json.of(e.type.name),
        "refId" to Json.of(e.refId),
        "slotKey" to Json.of(e.slotKey),
        "status" to Json.of(e.status.name),
        "recordedAtMinuteOfDay" to Json.of(e.recordedAtMinuteOfDay)
    )

    fun eventFrom(j: JsonValue): EventLog = EventLog(
        id = j.get("id").asString(),
        date = LocalDate.parse(j.get("date").asString()),
        type = EventType.valueOf(j.get("type").asString()),
        refId = j.get("refId").asString(),
        slotKey = j.get("slotKey").asString(),
        status = EventStatus.valueOf(j.get("status").asString()),
        recordedAtMinuteOfDay = j.get("recordedAtMinuteOfDay").asInt()
    )
}
