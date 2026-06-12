# RecoverWell — Achilles Rehab Coaching App

A native Android app that acts as a daily rehab coach for a **conservative
(non-surgical) Achilles tendon rupture**, managed in a walking boot with
progressive wedge reduction — built around a real recovery that started on
**2 June 2026** with the goal of returning to **padel**.

> **RecoverWell supports — but never replaces — the advice of your
> physiotherapist and consultant.** Every timeline in the app is a
> typical-protocol placeholder that must be confirmed with your own clinical
> team. This disclaimer is baked into the app: it gates onboarding, sits in a
> persistent strip on every screen, and every phase progression requires an
> explicit "my physio confirmed" step.

---

## Features

- **Exercise engine** — five-phase library with animated demonstrations
  (offline, see below), written cues, sets/reps/holds, frequency and a
  "why this matters" for every exercise. Daily session view with tick-off
  logging. Progression is gated by *date AND physio confirmation*: the next
  phase only activates when its (editable) start date has been reached *and*
  you record that your physio approved it.
- **Medication & task reminders** — anticoagulant 2.5 mg pre-loaded with
  08:00/20:00 reminders and taken/missed logging (actions directly on the
  notification). Rehab tasks (elevation, boot checks, circulation/calf
  checks) ride the same engine, and wedge-change reminders are generated
  automatically from the editable wedge plan. Everything lands in one unified
  daily checklist.
- **Recovery tracker** — daily log (pain 0–10, swelling, ROM note, boot
  compliance, wedges, weight-bearing, mood, energy, notes), trend charts with
  7-entry moving average, milestone timeline anchored to 2 June 2026 vs
  typical conservative-protocol expectations, and export to **PDF, CSV and
  JSON backup** (with restore).
- **Digital twin** — Canvas-drawn lower-leg model showing boot, wedge stack,
  and tendon healing state; current capability panel; phase-based do/don't
  lists; "Can I…?" movement checks; and off-plan risk warnings (wedges ahead
  of plan, boot not worn in the protection phase, pain/swelling patterns that
  deserve a DVT check).
- **Safety first** — DVT, pulmonary embolism, re-rupture, anticoagulant
  bleeding and boot/skin red flags are one tap away from **every** screen via
  the persistent header button, written as symptoms + concrete actions
  (999 / 111 / clinic).

## Rehab protocol (conservative / non-surgical only)

The app contains **no post-surgical content**. Phases, timelines, precautions
and exercises are modelled on established UK conservative functional
rehabilitation pathways:

- **UKSTAR trial** — Costa ML et al., *Plaster cast versus functional brace
  for non-surgical treatment of Achilles tendon rupture (UKSTAR): a
  multicentre randomised controlled trial*, The Lancet 2020. Demonstrated
  early weight-bearing in a functional brace is as good as casting; basis for
  the immediate weight-bearing-as-tolerated approach.
  <https://pubmed.ncbi.nlm.nih.gov/32035553/>
- **NHS trust non-operative pathways**, e.g. University Hospitals Coventry &
  Warwickshire ("Achilles tendon injury (rupture) — Aircast boot and wedges")
  and Cambridge University Hospitals ("Achilles tendon rupture: management
  and rehabilitation"): boot in full equinus with ~5 wedges, **one wedge
  removed weekly from ~week 3 so the boot is neutral by ~week 8**, boot
  weaned from ~weeks 8–10, **no calf stretching before week 12**, supportive
  shoes with a heel raise from ~weeks 12–14.
  <https://www.uhcw.nhs.uk/> · <https://www.cuh.nhs.uk/>
- **Return to sport** consistent with published return-to-play reviews
  (e.g. Zellers et al., systematic review of return to play post-Achilles
  rupture): graded running from ~6 months once single-leg heel-raise
  benchmarks are met; racquet/court sports such as padel typically
  **9–12 months** with explicit physio sign-off.

The five in-app phases:

| Phase | Typical window* | Focus |
|---|---|---|
| 1 | Weeks 0–2 | Immobilisation & protection (boot full equinus, WBAT, clot prevention) |
| 2 | Weeks 2–8 | Progressive weight-bearing & wedge reduction (to neutral by ~wk 8) |
| 3 | Weeks 8–12 | Early mobilisation out of the boot (ROM to neutral only, gait) |
| 4 | Weeks 12–24 | Strengthening (calf raise progression, balance, conditioning) |
| 5 | Week 24+ | Return to sport (run → plyometrics → padel drills → competition) |

\* Every window is an editable, physio-confirmable placeholder — both the
dates (Settings → Phase dates) and the content (sets/reps/hold/frequency per
exercise, plus enable/disable) can be changed in-app. DVT risk after Achilles
rupture is among the highest of any sports injury, which is why the
anticoagulant reminders and calf-check tasks are treated as first-class
clinical features.

## Engineering decisions

### Stack: Kotlin + Android platform APIs, four-module clean split

- **`core/`** — pure Kotlin (zero Android imports): protocol content, phase
  gating, checklist/reminder scheduling, wedge planning, digital-twin logic,
  trend math, CSV export, versioned JSON backup codec (hand-rolled, zero
  dependencies). 39 unit tests.
- **`draw/`** — pure Kotlin rendering layer: a small `Sketch` 2D abstraction
  plus everything the app draws (icon set, charts, the digital-twin leg, the
  exercise demonstration engine and its keyframes, palette). Platform-free,
  so the same visuals render on Android, in design tooling, and on a future
  iOS/web port.
- **`designlab/`** — JVM design tool: renders every drawn surface to PNG for
  visual review (`gradle :designlab:render`), including the exact icon set
  the app ships. The UI was iterated against these proofs.
- **`app/`** — Android shell: programmatic Views over a token-based design
  system (tonal surfaces, ripples, elevation, 48dp+ targets), SQLite store,
  AlarmManager reminders, notifications, SAF export/import, PDF report.

**Iconography:** all 28 icons (and the launcher glyph) are the official
**Google Material Symbols** — the established Android icon set — shipped
verbatim as their published vector drawables (Apache License 2.0,
<https://github.com/google/material-design-icons>; attribution headers in
each `res/drawable/ic_*.xml`). Nothing hand-drawn: `podiatry` for the leg
tab, `footprint` for boot checks, `ecg_heart` for circulation, etc. Charts,
the digital-twin leg and the exercise demonstration figures are functional
data visualisations rendered from the `draw/` module.

**Path to iOS/web:** all business rules live in `core/`, which is plain
Kotlin with no Android types — it compiles unchanged as the common module of
a Kotlin Multiplatform project (the one porting cost is swapping `java.time`
for `kotlinx-datetime`, a mechanical change). The Android layer is a thin
renderer over `core` interfaces; an iOS (SwiftUI) or web (Compose for Web /
React) front-end re-implements only screens, storage adapters and platform
notifications. The UI itself is deliberately decoupled: screens are pure
functions from store state to a View tree, re-rendered on every change —
the same unidirectional pattern those platforms use.

**Why no AndroidX/Compose?** This was built and verified in a sandboxed
environment where Google's Maven repository (AndroidX, AGP) and SDK download
hosts are unreachable. Rather than ship unverified code, the app targets the
Android platform APIs directly (everything needed — notifications, SQLite,
Canvas, PdfDocument, SAF — is in the platform) and is built with a
transparent five-step pipeline using Debian-packaged AOSP tools:

```
kotlinc (JVM 1.8, no invokedynamic) → dx → aapt → zipalign → apksigner
```

`gradle assembleApk` produces a **signed, verified APK**
(`app/build/apk/recoverwell-debug.apk`, minSdk 26 / Android 8.0+,
targetSdk 30). `aapt dump badging`, `apksigner verify` and dex inspection all
pass, and Robolectric boots the real `MainActivity` in JVM tests as a
runtime smoke check. In a normal environment the same two modules drop into
a standard AGP build without code changes (`core` is build-system agnostic;
`app` is plain Kotlin + resources).

### Demonstrations: procedural Canvas animation, not video files

Each exercise has an **animated demonstration rendered procedurally**
(`ExerciseDemoView`): a side-view figure driven by named joint-angle
keyframes (hip/knee/ankle/toes), interpolated at 60 fps, with props (boot,
crutches, band, step, wall, bike, cones, racquet) and play/pause on tap.

Why this over video:

- **Offline & reliable by construction** — no streaming, no caching step, no
  third-party links that rot or get taken down. The "video" is code in this
  repo, owned outright, with no licensing risk.
- **Tiny** — the entire demonstration library adds ~20 KB to the APK vs
  ~5–15 MB *per clip* for bundled video.
- **Clinically reviewable & editable** — a physio-suggested tweak (e.g.
  "dorsiflexion only to neutral") is a one-line keyframe change with a code
  review trail, not a re-shoot.
- The demo is always paired with written cues, prescription and a
  precaution line, which carry the clinical detail.

### Storage: app-private SQLite, offline-only, export-first

- **Private by default:** a single SQLite database in app-internal storage.
  No account, no analytics, and **no `INTERNET` permission** — the OS itself
  guarantees the app cannot transmit anything.
- **Fully offline:** every feature works with radios off.
- **Export/backup:** PDF report, two CSVs, and a versioned full-fidelity JSON
  backup (with in-app restore) via the system document picker — no storage
  permissions needed. Serialisation is shared with the database layer
  (`BackupCodec`), so DB rows, backup files and in-memory models cannot
  drift apart.
- **Cloud sync:** intentionally absent rather than opt-in — for a
  single-user medical journal, the most private sync is the JSON backup you
  choose to put in your own cloud drive. The codec is the sync-ready
  serialisation layer if true sync is ever wanted.

### Accessibility / one-handed use

56 dp minimum touch targets, 15–24 sp text, whole-row tap targets on
checklist items, bottom tab bar in thumb reach, steppers instead of
keyboards for numbers, content descriptions on interactive elements, and
high-contrast colours — designed for someone seated with a leg elevated,
phone in one hand.

## Building & testing

```bash
# Signed APK → app/build/apk/recoverwell-debug.apk
gradle assembleApk

# Pure-logic tests (39) + Robolectric app/runtime tests (10)
gradle :core:test :app:test
```

Toolchain expectations (all from Ubuntu/Maven Central):
`gradle 8.x`, JDK 21 (compile) + JDK 8 (Robolectric tests only), and the
Debian-packaged Android tools `aapt`, `zipalign`, `apksigner`,
`dalvik-exchange`, plus `android-sdk-platform-23` for resource linking.
Install on the target phone via `adb install` or any file transfer
(sideload); Android 8.0 or newer.

## Personal data pre-fill (all editable in-app)

| Field | Pre-filled value |
|---|---|
| Injury | Full Achilles rupture, **left**, playing padel |
| Injury date | **2 June 2026** |
| Pathway | Conservative / non-surgical, walking boot |
| Consultant review | **7 June 2026** (completed) |
| Medication | Anticoagulant **2.5 mg, twice daily** (08:00 / 20:00) |
| Goal | Full recovery and return to playing **padel** |

## Repository layout

```
core/      pure-Kotlin domain: model/ protocol/ logic/ export/ json/   + tests
draw/      pure-Kotlin rendering: Sketch, icons, scenes, demo engine
designlab/ JVM proof renderer + vector-resource generator
app/       Android shell: data/ notify/ screens/ ui/ export/           + smoke tests
```
