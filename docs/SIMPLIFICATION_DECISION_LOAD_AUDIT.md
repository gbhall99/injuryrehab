# RecoverWell: simplicity and decision-load audit (v3.11)

**The question:** the app has so many buttons, menus, sections and scrolling that it feels hard to use. Does the Customer Thinking Score measure that? If not, extend it, measure again and simplify the app a lot, without losing features.

**Short answer:** the Customer Thinking Score (v3.10) didn't measure it. That score times how long it takes to find the one control a journey needs. Everything else on the screen only counts as a small "number of choices" cost, and screens a journey never visits don't count at all. So a screen could hold 25 questions, 8 sections and 5 screens of scrolling and still score 10/10.

This round adds a second metric, **Decision Load**: everything each screen puts in front of you, whether or not you need it today. It measures 22 screens before and after, and restructures the app until every everyday screen is within budget. All 20 journeys stay at a glance.

## The metric: Decision Load

For every screen, on an ordinary day (day 23, phase 2, a few days of check-ins, one medicine), measured on the real app at phone size (360 × 740 dp) with realistic text sizes:

| Measure | What it counts | Budget per screen | Why that budget |
|---|---|:-:|---|
| **Controls** | Every button, row, chip, field and link on the whole screen, not just the visible part. A 0-10 rating scale counts as one. | **15** | Choice time grows with log₂(n + 1) (Hick–Hyman). 15 choices is 4 bits, the practical ceiling for a screen you scan rather than study. |
| **Sections** | Headed groups you have to skim past. | **4** | Working memory holds about four chunks (Cowan 2001). |
| **Scrolling** | Screens of scroll: content height ÷ visible height. | **2** for screens where you decide or do something<br>**3** for pages you open to read | Most attention lands in the first two screenfuls (Nielsen Norman Group eye-tracking). Reading pages are opened on purpose, so they get more room. |
| **Redundancy** | Duplicate entry points: controls on different screens that lead to the same place. The app bar's Ask and Red flags don't count, because they're global by design. | as few as possible | Every duplicate is one more choice that looks different but isn't. |
| **Words** | Words on screen (reported, not budgeted). | - | Reading effort. |

**Score:** each budget met scores 1, and an overrun scores budget ÷ actual. A screen's score is the mean of the three, and the app's score is the mean over screens, out of 10.

**One deliberate exception: Red flags.** It lists every sign of five emergencies and folds none of them away, because on a safety page completeness beats brevity. Its budget is 4.2 screens. It was 5.7 with everything listed twice.

`DecisionLoadTest` crawls the screens and fails the build if any screen goes over budget, or if the app-wide totals creep back up.

## Headline

Same 22 screens, adapted to the new structure (three overlapping guides became one page, and Settings gained two small sub-screens):

| | Before (v3.10) | After (v3.11) |
|---|:-:|:-:|
| Screens within budget | 7 of 22 | **21 of 22** (Red flags is the documented exception) |
| Controls, all screens | 215 | **147 (−32%)** |
| Sections, all screens | 83 | **44 (−47%)** |
| Screens of scrolling, all screens | 60.4 | **38.9 (−36%)** |
| Words on screen | 4,585 | **3,263 (−29%)** |
| Most controls on any one screen | 28 (coach) | **13** |
| Most sections on any one screen | 8 (Settings) | **4** |
| Duplicate entry points | 21 | **5** |
| Decision-load score | 8.66 / 10 | **9.96 / 10** (Red flags keeps it off 10) |
| The five tabs: controls · sections · scrolling | 67 · 21 · 13.8 | **47 · 11 · 8.9** |
| Journeys found at a glance (Customer Thinking Score) | 20 of 20 | **20 of 20** |

Three screens were added to the crawl this round: the new appointment form, *Your boot*, and Check-in history. With them, the 25-screen totals are 168 controls, 48 sections and 42.0 screens of scrolling. All 25 are within budget apart from Red flags.

## Screen by screen

Each cell is controls · sections · screens of scrolling. Budgets: 15 · 4 · 2 (3 for reading pages).

| Screen | Before (v3.10) | After (v3.11) |
|---|:-:|:-:|
| Today (tab) | 21 · 5 · 3.30 | 13 · 3 · 1.97 |
| Exercises (tab) | 11 · 1 · 1.45 | 8 · 1 · 1.53 |
| Progress (tab) | 9 · 5 · 2.94 | 7 · 3 · 1.98 |
| My leg (tab) | 3 · 2 · 2.44 | 10 · 2 · 1.96 |
| Settings (tab) | 23 · 8 · 3.63 | 9 · 2 · 1.45 |
| Exercise detail | 8 · 5 · 2.51 | 6 · 3 · 1.98 |
| Today's session list | 8 · 2 · 1.14 | 8 · 2 · 1.14 |
| Session player | 5 · 2 · 1.41 | 5 · 2 · 1.41 |
| Check-in form | 3 · 1 · 1.55 | 3 · 1 · 1.55 |
| Phase guide, What to expect, How you're doing → **Your plan** *(reading page)* | 1 · 7 · 2.52<br>4 · 5 · 3.11<br>2 · 5 · 2.39 | 8 · 1 · 2.97 |
| Physio visits | 15 · 6 · 3.98 | 8 · 4 · 1.72 |
| Stay fit | 4 · 6 · 2.93 | 3 · 3 · 1.93 |
| Return to sport *(reading page)* | 3 · 1 · 3.23 | 2 · 1 · 2.49 |
| Red flags *(safety exception)* | 13 · 3 · 5.69 | 7 · 1 · 4.16 |
| Recovery coach | 28 · 6 · 3.96 | 10 · 2 · 1.35 |
| Medications | 5 · 1 · 1.00 | 5 · 1 · 1.00 |
| Injury & goal | 17 · 4 · 2.68 | 9 · 3 · 1.84 |
| Reminders | 6 · 1 · 1.00 | 6 · 1 · 1.00 |
| Configure my plan | 25 · 4 · 5.61 | 9 · 3 · 1.54 |
| About *(reading page)* | 1 · 3 · 1.95 | 1 · 3 · 1.95 |
| Backup, restore & export *(new sub-screen)* | - | 6 · 1 · 1.00 |
| Exercise videos *(new sub-screen)* | - | 4 · 1 · 1.00 |
| New appointment *(new form)* | - | 5 · 1 · 1.00 |
| Your boot *(new: every boot setting in one place)* | - | 10 · 1 · 1.09 |
| Check-in history *(existed, now crawled)* | - | 6 · 2 · 1.00 |

*My leg* went from 3 to 10 controls on purpose. Each "Can I…?" row is now tappable to show the why and the when, which keeps the tab short: a verdict at a glance, and the detail one tap away. Scrolling there fell from 2.4 to 2.0 screens.

## What changed

### 1. One home for each thing (21 duplicate entry points → 5)

| Was in several places | Now lives in one |
|---|---|
| What to expect (screen), How you're doing (screen), the phase guide, My leg's phase reference, plus Today tiles and Settings rows to them | **My leg** has *What to expect now* (this week, what's normal, what's next). **Your plan** has every phase, the boot, and "worried about re-rupture?". |
| Boot settings in five places: Injury & goal, Configure my plan, Physio visits (twice), boot-date editors | **Settings › Configure my plan › Your boot**: type, today's setting, out-of-boot date, reduction schedule and the clinic's change dates. The quick "Change boot angle" stays on Today and My leg. |
| Red flags from six places (app bar, Settings, My leg, What to expect, How you're doing, Return to sport) | The **app bar pill**, on every screen, plus contextual links where a warning is raised |
| Red flags page: a "Which one is it?" block, then every warning again | **One card per warning**: what to do and the right call first, then "if you have:" the signs, then why |
| Coach from the app bar and Settings | **Ask** in the app bar |
| Stay fit from Today and Settings | **Exercises › Stay fit** |
| Physio visits from Today, Settings and My leg | Today's **Physio** chip, plus prompts before and after a visit |
| Stats, streaks and the recovery timeline on Today *and* Progress | **Progress** only (*This week*, *Your road back*) |
| "Log today's check-in" on Progress | Today (where the check-in is) |
| "Add a check-in for a day you missed" on Progress and in Check-in history | **Check-in history** (open it from the Pain tile on Progress) |
| Physio visits: "Adjust phases & exercises", "Adjust boot / injury plan", "Boot change dates" | One **Update my plan** row |

The five that remain are deliberate: Today's *What to expect* and *Boot* chips (shortcuts from home, and the reason those journeys are found at a glance), the PDF in three places (Progress, the physio pack, and Settings' export screen serve three different moments), and *Update my plan* in Physio visits (it's where people are when their physio changes something).

### 2. Fewer choices at once

- **Recovery coach:** 25 starter questions became **6**, one from each topic in turn ("Can I drive yet?" among them), plus "19 more questions".
- **Configure my plan:** 25 → 9 controls. Weight-bearing (four chips) and the confirmed phase (a stepper) are now one row each that opens a short list. The boot is one row. Each exercise's On/Off chip pair is one switch, and phases stay folded until you tap one.
- **Settings:** 23 controls in 8 sections → **9 rows in 2 sections** (*Your plan*, *App*). Backups and exports, the theme, and video options each open their own small screen.
- **Physio visits:** 15 → 8. The always-open four-field add form is now an **Add appointment** button that opens a short form. *Edit* and *Remove* moved into that form. Your questions and the stage's questions are one list with **Add a question**, and the pack shares as **Copy pack** or **Share PDF**.
- **Progress:** the four chart chips are one **Pain ▾** selector.
- **Exercises:** the five phase chips are one **Other phases ▾** selector.
- **Exercise detail:** three "Mark session N done" buttons became one ("Mark session 1 done · 0 of 3 today").
- **Stay fit:** the goal stepper became a "Goal: 3 a week" button.

### 3. Fewer sections

Settings 8 → 2, Progress 5 → 3 (*This week*, *Trends*, *Your road back*), Stay fit 6 → 3, Physio visits 6 → 4, coach 6 → 2, Red flags 3 → 1, and three guides with 17 sections between them became *Your plan*, with 1. Insights joined *This week*, and milestones joined *Your road back*.

### 4. Less scrolling

- **Today** (3.30 → 1.97): stats moved to Progress. The three exercise-session rows became one *next session* row inside Daily care, with all sessions on the Exercises tab. Suggestions are one tappable card.
- **My leg** (2.44 → 1.96): the watch-outs sit inside the leg card they're about. "Can I…?" shows the verdict, with the why one tap away. The tendon's state moved to each phase in *Your plan*.
- **Configure my plan** (5.61 → 1.54): phases are one folded list, not open cards.
- **Physio visits** (3.98 → 1.72): the six-row *Coming up* schedule is covered by My leg's "Next: …" lines and Today on the day. *Your current numbers* travel in the copied pack and the PDF.
- **Return to sport** (3.23 → 2.49): before phase 4 it opens with one "Opens in phase 4" card, not a 0 % ring, and locked stages show their targets in one line.
- **Red flags** (5.69 → 4.16): nothing is listed twice any more, and the explanatory intro now closes the page, so a worried person lands straight on the warnings and their call buttons.

### 5. Things in the right place

Stay fit sits with the exercises. Stats sit with progress. Every boot setting sits with the plan your physio changes. The tendon's state sits with each phase. The missed-day check-in sits with your check-in history. And "update my plan" is one door from a physio visit.

## Journeys: still at a glance

The Customer Thinking Score was re-run on both builds: untouched v3.10 in a worktree, and this branch. It uses the same 20 journeys, words and states.

| | v3.10 | v3.11 |
|---|:-:|:-:|
| Journeys scoring 10/10 | 20 / 20 | **20 / 20** |
| Steps at a glance (≤ 1.5 s) | 43 / 43 | **45 / 45** |
| Total thinking time | 39.3 s | 40.6 s |

Total thinking time is up by 1.3 s because two rare journeys gained a step, and each new step is found at a glance:

- **Back up my data:** Settings, then *Backup, restore & export*, then *Back up now*. That's +0.8 s in exchange for a Settings tab with 9 controls instead of 23.
- **Add my next physio appointment:** the new form's *Save appointment* (+0.5 s), where it used to be four always-open fields.

"Physio says: 15 reps now" is 0.4 s slower, because "Change sets, reps or video" lost the *Prescription* heading above it. "My calf is hot and swollen" got faster (2.0 → 1.7 s).

## Nothing lost: every feature still works, and some take one more tap

Daily tasks are untouched: a dose, the pain check-in, care tasks and exercise sessions take the same taps as before.

These occasional tasks now take one tap more, by design:

- Opening a form to add an appointment.
- Opening the backup screen.
- Picking weight-bearing, the confirmed phase, the chart's measure or another phase's exercises from a short list.
- Opening a phase before switching an exercise off.
- Tapping "more questions" in the coach.
- Tapping a "Can I…?" row to read the why.
- Reaching a missed day through Check-in history.

## Safety review of the changes

- **Red flags** still shows every sign of every warning. Each call button now sits directly under its warning, NHS style ("Call 111 … if you have:"). The earlier triage block was dropped because it repeated all five warnings, not because triage stopped mattering. The calf/DVT call is on the first screen.
- Urgent and warning-level alerts still appear as full cards on Today, and in the My leg leg card.
- The anticoagulant, head-injury, re-rupture and boot guidance is unchanged. No clinical content was removed. Where content moved, it's listed above.

## Limits

- **This is a model.** The budgets are grounded in the literature, but they are heuristics. Nobody has used v3.11 yet. The simulated usability study in `USER_TEST_RESULTS.md` is why the tab kept its name *My leg*: it was the most findable place for "what can I do?".
- **One ordinary day.** Screens are measured with the default font size on one day (day 23, phase 2). Other days show other prompts, and larger system fonts mean more scrolling.
- **Estimated text size, and no screenshots.** Robolectric doesn't lay out text, so the probe estimates it (the same estimator for before and after). I haven't seen these screens rendered on a device.

## Tests

- **`DecisionLoadTest`** (new) crawls 25 screens and asserts each one's budgets and the app-wide totals: ≤ 180 controls, ≤ 52 sections, ≤ 46 screens of scrolling. It also writes the tables to `app/build/decision-load/`.
- **`ThinkingScoreTest`:** the 20 journeys, with *Back up* and *Add appointment* updated for the new screens.
- **Updated tests:**
  - The coach test opens "more questions".
  - The smoke test checks My leg's *What to expect now* and *Your plan*.
  - The accessibility test covers *Your plan*.
- **Result:** 214 tests pass (213 on v3.10, plus the new crawl).
