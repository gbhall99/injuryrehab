# RecoverWell: customer thinking audit (v3.10)

Date: 28 Sep 2026 · Branch: `claude/ux-achilles-content-audit-cz7xdo` (restarted from `main` after PR #32 merged)

The simplicity audit counted **taps**. This audit counts **thinking**: how long a customer spends working out *which* control to press, at every step of every journey. The same process was used: define it, measure it, fix everything that wasn't perfect, measure again.

## The metric: Customer Thinking Score (CTS)

**CTS is the seconds from looking at a screen to knowing which control to press.** It is calculated per step, summed per journey, and graded out of 10.

```
step seconds = Choose + Find + Words + Scroll
```

| Part | Cost | What it captures | Basis |
|---|---|---|---|
| **Choose** | 0.15 s × log₂(controls on screen + 1) | More visible choices take longer to decide between. A rating scale (a row of numbered buttons) counts as one control. | Hick–Hyman law (Hick 1952; Hyman 1953) |
| **Find** | 0.2 s per place looked | The places passed before the right control: section headings skimmed, plus rows read inside its section. A screen's only primary button costs 0, because the eye lands on it first. | One fixation is roughly 200–300 ms (Rayner 1998). Pop-out follows Treisman & Gelade (1980). |
| **Words** | +1.35 s | The control and its heading don't use the customer's own words, or it's an icon with no words beside it. The customer has to translate. | One Keystroke-Level Model "mental operator" (Card, Moran & Newell 1980) |
| **Scroll** | +1.35 s | The control is off-screen, so the customer has to guess it's further down. | One KLM mental operator |

**At a glance** means ≤ 1.5 s. That's a normal phone screen of about 24 controls (0.7 s), with the right one within the first four places (0.8 s), in the customer's words, and no scroll-guess.

- **Step mark:** min(1, 1.5 ÷ step seconds).
- **Journey score:** 10 × the average step mark.
- **Perfect:** a journey is 10/10 only when every step is at a glance.

### How it was measured

- **Harness.** `ThinkingScoreTest` drives each journey through the **real Activity**, laid out as a typical phone window (360 × 740 dp; Robolectric's default is 320 × 470). At each step it reads what's on screen:
  - the controls
  - their positions and headings
  - whether the right control is visible
  - the words on it, its card title and its section heading

  It then presses the control, exactly as a customer would.
- **Realistic text sizes.** Robolectric doesn't measure text in the JVM: TextViews come out with no text height or width. That would make every screen look shorter than it is, and it did flatter the first "after" run, which I caught and corrected. So before each measurement, the harness gives every text a realistic size from Roboto metrics:
  - line height 1.17 × text size, plus font padding
  - average glyph 0.5 × text size (0.55 when bold)
  - wrapped to its real available width

  It then lays the window out again. Text at the default font size is assumed.
- **Customer words.** Each step's words are written in the customer's vocabulary, not the app's labels, and live in the test file where they can be audited. For example, "blood thinner" and "medication", not "anticoagulant". For settings tasks, "settings" counts as a customer word.
- **Starting state.** Unless a journey needs something else, every journey starts at **day 23 in a VACOped boot (phase 2), morning, nothing logged yet**. That's an ordinary day, not a boot-change day. Exceptions:
  - "Record today's boot change" uses a change day.
  - "Tick off leg elevation" starts after the morning dose and pain check-in, which is when the first elevation of the day usually happens.
- **Before vs after.** "Before" is the untouched `main` app (a git worktree) run with the same goals, words, states and rules, using the routes it offered.
- **Honest limits.**
  - This is a **model with published constants**, not a stopwatch with real patients.
  - It describes a customer who hasn't memorised the screens yet, i.e. the first weeks. Thinking time falls as people learn a layout.
  - It hasn't been validated on a device, at larger font sizes, or with users.

---

## Headline

Across **20 journeys**, measured on the untouched `main` app and on this branch:

| | Before | After |
|---|:-:|:-:|
| Average journey score | **7.5 / 10** | **10 / 10** |
| Journeys at 10/10 | 5 of 20 | **20 of 20** |
| Steps where the right control is found at a glance (≤ 1.5 s) | 19 of 45 | **43 of 43** |
| Total thinking time across all 20 journeys | **86.3 s** | **39.3 s (−54%)** |

Two journeys lost a step, which is why there are 45 steps before and 43 after:
- A different boot angle is now set straight from Today's hero.
- An appointment is now added straight from Today.

## Journey scores

Each cell shows total thinking time and score. The last column says what slowed the "before" version down.

| Journey | Before | After | What made the customer think (before) |
|---|:-:|:-:|---|
| Log my morning blood thinner | 1.0 s · **10.0** | 1.0 s · **10.0** | - |
| Log today's pain | 3.1 s · **4.8** | 1.4 s · **10.0** | Tap my pain score 3.1s (off-screen, 6 places in) |
| Tick off leg elevation | 2.9 s · **5.1** | 1.2 s · **10.0** | Tick elevation 2.9s (off-screen, 5 places in) |
| Note that my ankle is more swollen | 6.0 s · **5.2** | 1.7 s · **10.0** | Find where to note it 3.5s (off-screen, 8 places in); Save it 2.4s (off-screen) |
| Undo a dose I ticked by mistake | 1.5 s · **10.0** | 1.5 s · **10.0** | - |
| Do today's exercises | 3.9 s · **8.7** | 2.0 s · **10.0** | Finish a set 2.5s (off-screen) |
| Watch how to do my knee extensions | 3.1 s · **10.0** | 3.2 s · **10.0** | - |
| Physio says: 15 reps now | 8.7 s · **8.3** | 5.1 s · **10.0** | Find where to change reps 2.3s (not in their words); Save 3.0s (off-screen, 5 places in) |
| My calf is hot and swollen | 3.0 s · **8.1** | 2.0 s · **10.0** | Call for same-day advice 2.5s (off-screen) |
| Record today's boot change | 1.2 s · **10.0** | 1.2 s · **10.0** | - |
| Clinic set a different boot angle | 5.1 s · **8.0** | 1.6 s · **10.0** | Find the boot 2.5s (not in their words); Change the setting 1.9s (not in their words) |
| What should I expect at this stage? | 3.1 s · **4.8** | 0.6 s · **10.0** | Find guidance 3.1s (off-screen, 6 places in) |
| Can I drive yet? | 2.9 s · **8.9** | 1.6 s · **10.0** | Find somewhere to ask 1.9s (not in their words) |
| How is my pain trending? | 1.0 s · **10.0** | 1.0 s · **10.0** | - |
| Add my next physio appointment | 5.5 s · **8.0** | 1.6 s · **10.0** | Find appointments 2.7s (not in their words); Add it 1.8s (6 places in) |
| Send my progress to my physio | 6.9 s · **4.6** | 1.8 s · **10.0** | Find sharing 2.7s (not in their words); Make the report 4.1s (off-screen, 11 places in) |
| Move my evening dose reminder | 6.4 s · **7.0** | 3.3 s · **10.0** | Find reminders 2.7s (not in their words); Open the medication 2.7s (off-screen) |
| Back up my data | 6.5 s · **4.8** | 2.6 s · **10.0** | Find backup 2.7s (not in their words); Back up 3.7s (off-screen, 9 places in) |
| Change my sport to tennis | 6.8 s · **7.0** | 3.4 s · **10.0** | Find my goal 2.7s (not in their words); Open injury & goal 2.5s (off-screen); Change the sport 1.6s (5 places in) |
| First-time setup | 7.8 s · **7.1** | 1.6 s · **10.0** | Confirm my injury 2.6s (off-screen); Confirm my medication 2.0s (off-screen); Finish 2.8s (off-screen, 5 places in) |

**Starting states.** Unless noted, every journey starts at day 23 in a VACOped boot, phase 2, in the morning with nothing logged. The exceptions:
- **Record today's boot change:** starts on a boot-change day.
- **Tick off leg elevation:** starts after the morning dose and pain check-in.

**The one cost of the fixes.** Today now carries three hero chips, so steps that start there take about 0.04 s longer to choose from; "Go to exercises", for example, went from 0.77 s to 0.81 s. That's why "Watch how to do my knee extensions" is flat at about 3.1–3.2 s. Every such step stays well inside the at-a-glance line.

## What made customers think, and the fixes

Every slow step had one of three causes. In order of cost:

### 1. The right control was off-screen

The customer had to guess it was further down. The fixes:

| Where | Fix |
|---|---|
| **Today: pain check-in** (was below meds and all of daily care) | Its own section, **"How's your pain today?"**, now sits directly under Medication until it's logged. After that it becomes a done row in Daily care. The hero is more compact: 68 dp ring, 24 sp "Week", and the "N of M done" line folded into the ring's spoken label. Together these put both rows of the 0–10 scale on the first screen. |
| **Today: swelling, mood, note** | "Log swelling, mood or a note" sits under the question instead of below the scale. |
| **Today: what to expect** (a tile at the very bottom) | A **"What to expect"** chip in the hero. The full phase guide opens from that screen. |
| **Settings: backup, medications** | Sections are now ordered by what people come for: **Your plan** (Injury & goal, Medications, Configure) → **Reminders** → **Data** (Full backup first, with the explanation moved under the rows) → Safety & info → Appearance → Videos → AI → Recovery tools. |
| **Long forms and the session player** | The main action is **pinned below the scrolling content**, where the thumb is. This covers: the session player's "Set N done" and its timer; Save check-in (Today, and past days in Progress and History); Save changes (sets and reps); Save / Confirm & continue on Injury & goal and on every setup step. The medication setup step's intro now scrolls with the list, so it can't push the button off-screen. |
| **Red flags: "Call 111" for a hot, swollen calf** (below the whole PE section) | A **"Which one is it?"** block at the top lists each sign's title and first symptom next to its call button. The full signs follow below. |
| **Injury & goal: the sport picker** | It gets a **"Sport & goal"** heading. The read-only "Your plan" line moved to the end of the form. |

### 2. The words weren't the customer's

The customer had to translate. The fixes:

| Before | After |
|---|---|
| Tab **"More"** (said nothing about plan, medicines, reminders, backups) | Tab **"Settings"** |
| Boot angle: "My leg" tab, then "Adjust setting" | A **"Boot 20°"** chip in the hero opens the same dialog. On My leg the control now reads **"Change boot angle"** (or "Change boot wedges"). |
| Reps: "Adjust dose or video" | **"Change sets, reps or video"** |
| Coach: an unlabelled icon | An **"Ask"** button. The journal icon, an AI feature that's off by default, now shows only when AI is on. |
| Report: More → bottom of Data → "PDF report" | **"Share with your physio"** on the **Progress** tab, where people look for their progress. The PDF also stays in Settings › Data. |
| Appointments: More → Physio visits | A **"Physio"** chip in the hero. The next visit's date rides on the hero's date line ("Mon 28 Sep · physio Sat 10 Oct"). |

### 3. Too many places before it

- **Daily care is ordered by frequency.** Elevation (3× a day) comes first and the once-a-day boot check last: what people look for most sits where the eye lands first.
- **"Add appointment" is the Physio visits screen's primary button**, so the eye lands on it.

### Kept consistent while fixing

- **No duplication on Today.** The jump grid no longer repeats destinations that are now in the hero or the app bar (coach, what to expect, physio visits).
- **Disclaimer on one line.** It now reads **"Supports - never replaces - your clinical team"**, so on a phone it no longer eats into every first screen.
- **Section headings** are slightly tighter (16 dp top padding instead of 22).
- **Deliberate duplicates.** Red flags shows call buttons twice, in the triage block and in each detailed card, because on an emergency screen the call sits beside both the summary and the full signs. Sharing also has two homes: Progress, where people look first, and Settings › Data, with the other exports.
- **The earlier audits' guards still pass:** tap budgets, screen-load budgets, accessibility, the video and clinical tests.


---

## Verification

**Test suite.** `gradle test` + `gradle :app:assembleApk` ran **213 tests, 0 failures**: the 193 from before, plus 20 new journey tests. The signed APK (3.10, code 30) builds and verifies.

**`ThinkingScoreTest`** is 20 Robolectric classes, one per journey. Each one drives its journey on a 360 × 740 dp window with realistic text. At every step it records choices, places, words and on-screen, then **fails if any step takes longer than 1.5 s** to find. A future change that buries a control, drops a customer's word or pushes a button below the fold now breaks the build. Running with `THINKING_BASELINE=1` records without asserting, which is how "before" was measured on `main`.

**Earlier guards, re-checked:**
- **Effort:** one tap per dose, one tap per check-in, 12 taps for a guided session. The boot change is now **3 taps from Today** (was 4, via My leg).
- **Screen load:** the plan editor and Progress budgets still pass.
- **Everything else:** accessibility labels, the red-flag call buttons, onboarding and the video tests all pass.

## Residual risks

- **It's a model, not a stopwatch.** The constants are published averages, and the customer words are my representation of how patients talk; both are in the test file to audit and adjust. What would validate it: timing 5–8 real patients on the same 20 tasks.
- **Default font size only.** At the larger system font sizes many older users choose, more falls below the fold. The pinned actions and the compact Today still help, but the pain scale's second row, for example, would need a scroll. Measuring 1.3× text is a sensible next round.
- **Text metrics are estimated.** Robolectric can't measure text, so the probe uses Roboto averages. Real wrapping may differ by a line here and there.
- **A new-ish customer is assumed.** Thinking time falls as people learn a layout. These scores describe the first weeks, which is when it matters most.
- **Not validated on a physical device.**
