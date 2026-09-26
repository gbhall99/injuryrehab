# RecoverWell: simplicity and customer-effort audit (v3.8)

Date: 26 Sep 2026 · Branch: `claude/ux-achilles-content-audit-cz7xdo` (restarted from `main` after PR #30 merged)

This audit used the same process as `docs/UX_CLINICAL_AUDIT.md`. The same three personas walked every journey:

- Day 2 in the boot
- A week-14 late joiner
- Week 26, returning to padel

This time the scoring question was different: **how much work does the app ask of someone recovering from an Achilles rupture?** That person is usually seated, leg up, phone in one hand. Every journey was measured, scored out of 10, fixed and re-measured. **Frequency-weighted effort** counts most: a tap saved on something done twice a day is worth far more than one saved during setup.

> **How it was measured.**
> - **Taps:** counted by driving the real Activity in Robolectric tests (`EffortBudgetTest`). Where a "before" count couldn't be driven (the old rep-by-rep guided mode), it was counted from the code path, and the arithmetic is shown.
> - **Reading and choice load:** a probe counts visible text elements, tap targets and words per screen. For Today, the probe ran on **the old code (a worktree of `main`) and the new code with identical data**.
> - **Limits:** none of this was validated on a physical device or with patients. There's no emulator here.

## Rubric (10 = effortless)

| Measure | 10/10 means |
|---|---|
| Taps | The minimum a task can possibly take (1 for a log; 1 per set for exercise) |
| Decisions | No avoidable choice or dialog on the common path |
| Reading | Nothing must be read to act; repeated facts appear once |
| Findability | Every setting has exactly one obvious home |
| Interruptions | The fewest reminder moments that still deliver the care |
| Honesty | Low effort never produces fake data |

---

## Headline: effort to get through a day

A phase-2 day with the anticoagulant, default care tasks, 3 sessions, one of them guided:

| | Before | After |
|---|:-:|:-:|
| 2 doses | 4 taps (row + dialog choice, ×2) | **2** |
| Care tasks (elevation ×3, boot, calf ×2) | 6 | 6 |
| Check-in | 3 (open, slide, save) | **1** |
| Guided session 1 (5 exercises) | **~151** (a tap per rep: 130, plus opening, starting, finishing and backing out of each exercise) | **14** (one tap per set) |
| Sessions 2 and 3, quick-logged | 4 | 4 |
| **Total** | **~168 taps** | **~27 taps (−84%)** |
| Reminder moments per day (new installs) | 8 | **5** |

---

## Journey scores

| # | Journey (frequency) | Before | After | Measured change |
|---|---|:-:|:-:|---|
| E1 | Log a dose (2×/day) | 7 | 10 | 2 taps + a dialog → **1 tap**. The row shows "Taken at 08:12". |
| E2 | Daily check-in (1×/day) | 5 | 10 | 3 steps → **1 tap**. Optional metrics are no longer saved as fake data. |
| E3 | Guided exercise session (1–3×/day) | **2** | 10 | ~130 taps (phase 1, session 1) → **12**. The session plays through. |
| E4 | Quick-log a session done | 9 | 10 | Still 2 taps; now also one tap to start from the Exercises tab. |
| E5 | Reminder interruptions (daily) | 6 | 10 | 8 → **5** moments a day, with the same care delivered. |
| E6 | Scanning Today (daily) | 5 | 10 | Evening: 84 → **62** texts (−26%), 294 → **212** words. Duplicates removed. |
| E7 | Change the boot setting after a clinic visit | 5 | 10 | 4–5 taps via a label that didn't mention the boot → **3 taps** on My leg. |
| E8 | Find and change a plan setting | **4** | 10 | Plan editor: 129 → **54** texts, 76 → **32** targets. One home per setting. |
| E9 | Read an exercise | 7 | 10 | Rarely used video-pin controls moved off the daily screen. |
| E10 | Onboarding | 8 | 10 | Weight-bearing question removed from setup (sensible default, one home later). |
| E11 | Weekly review on Progress | 7 | 10 | 393 → **219** words (−44%). Milestones shown as reached plus the next three. |
| E12 | Navigating More | 6 | 10 | 5 overlapping plan rows → 3. Two spreadsheet rows → 1. |
| E13 | Asking the coach | 8 | 10 | Question box first. The answer is scrolled into view (it was off-screen). |

### E1: Log a dose (7 → 10)

| Friction found | Fix |
|---|---|
| The app's most frequent in-app action cost a tap, then reading a dialog, then choosing "Taken", every time. The notification path was already one tap. | **Tap = taken.** A dialog appears only when tapping an already-logged dose, to mark it missed or undo it, and it still carries the "never double up" advice. |
| An undone dose read "Skipped". | An undone dose returns to "not done". |
| Nothing answered the anxious "did I take it?". | A logged dose shows **"Taken at 08:12"**. |

### E2: Daily check-in (5 → 10)

| Friction found | Fix |
|---|---|
| Checking in meant navigating away, facing four sliders plus notes, and pressing Save. | **Today has a one-tap 0–10 pain scale**: two rows of 48 dp number buttons. The tap logs pain and carries the boot setting and weight-bearing forward. |
| **Untouched mood and energy saved as 3, and swelling as "None".** Mood insights and trends were partly built from values nobody entered. | Mood, energy and swelling start as "–" (not logged) and are **saved only if the user sets them**. |

### E3: Guided exercise session (2 → 10)

| Friction found | Fix |
|---|---|
| Guided mode needed **a tap for every rep**: 3 × 12 = 36 taps for one exercise. It worked one exercise at a time. Each exercise had to be opened, started and finished, and "finish" dropped the user back on that exercise's detail rather than the next exercise. Phase 1, session 1 (5 exercises, 10 sets) took about 130 taps, all while trying to exercise. | A new **session player** plays the whole session through. There is **one tap per set**, a real countdown for single holds and timed rounds, "Skip this exercise" (it suggests telling the physio if something hurt), and automatic advance to the next exercise. Each exercise is logged the moment its last set is done, so leaving mid-session keeps progress. It resumes if the app is rebuilt, and "Done" returns to the tab it was started from. |
| The Exercises tab offered a library, not today's session. | The Exercises tab now leads with **"Today · session N of M · Start"**. |

### E5: Reminder interruptions (6 → 10)

Default reminders fired at 8 separate times: 08, 09, 10, 12, 14, 18, 20 and 21.

**Fix:** the boot check moved to 08:00, and the calf and circulation checks now pair with the blood-thinner doses (08:00 and 20:00). That gives **5 moments**: 08 · 10 · 14 · 18 · 20. Clinically, the calf check sits in a natural "clot prevention" moment with the dose. Reminders due together are bundled by the existing group summary.

> Existing installs keep their saved times, which remain editable. For you, at phase 4, the care tasks have already ended.

### E6: Scanning Today (5 → 10)

**Measured** on identical data, old code against new:

| State | Texts | Tap targets | Words |
|---|:-:|:-:|:-:|
| Morning (nothing done yet): old | 81 | 32 | 284 |
| Morning: new | 90 | 41 | 296 |
| Evening (all done): old | 84 | 33 | 294 |
| Evening: new | **62** | **25** | **212** |

- **Morning is higher by design.** The one-tap check-in puts eleven number buttons on the page, and they replace a whole navigate-slide-save round trip. Excluding them, morning texts are 79 against 81.
- **Evening is 26% lighter.** As each group finishes it folds into one line ("All 2 doses taken today", "Daily care and check-in done", "All 3 exercise sessions done"). What's left always stands out, and a tap re-opens a group to undo something.
- **Duplicates removed:**
  - Streak chips on the hero repeated the streak tiles.
  - "Phase 2 of 5" plus dots in the stats card repeated the hero.
  - Jump tiles for Exercises (a tab), Medication (the section right above) and Journal (an app-bar icon).

### E7: Change the boot setting (5 → 10)

**Before:** More › "Injury & goal", whose subtitle still listed "appointments" and whose label never mentioned the boot, then scroll, stepper and Save. On My leg the device's "Change" link was hidden behind "Show phase reference".

**Fix:** My leg shows **"Adjust setting"** right under the boot status. It opens a one-stepper dialog, then Save: 3 taps, where the value is displayed.

### E8: Plan settings (4 → 10)

**Before:** four screens overlapped.

| Setting | Where it could be changed |
|---|---|
| Phase start dates | Configure my plan and Phase dates |
| Physio-confirmed phase | Configure my plan and Phase dates |
| Weight-bearing | Configure my plan and Injury & goal |
| Boot change dates | Top-level More row and Physio visits |

Configure my plan itself was the heaviest screen in the app (129 texts, 76 targets).

**Fix:**
- **One home per setting.** Configure my plan holds phases, exercises, boot schedule and pinned dates, weight-bearing and the confirmed phase. Injury & goal holds date, side, sport, boot type and setting, and the clinic number.
- The "Phase dates" screen is removed, and "Boot change dates" moves inside the boot card.
- Each phase is a **one-line summary** ("From Tue 16 Jun · 5 of 5 exercises on"). The current phase starts open, and headers announce open or closed to TalkBack.

### E9–E13

- **E9 Exercise detail:** "Pin a video" and "Reset" moved into "Adjust dose or video". The daily screen keeps "Watch video".
- **E10 Onboarding:** weight-bearing is no longer asked at setup. The default is weight-bearing as tolerated, which is right for the UK functional pathway, and it's editable in the plan. The earlier audit had already removed the target date and the out-of-boot question.
- **E11 Progress:** milestones show "✓ N milestones reached" plus the next three, with the full list on "Show all". The redundant export caption is removed.
- **E12 More:** "Your plan" is 3 rows instead of 5. The two CSV rows become "Spreadsheets (CSV)" with a choice.
- **E13 Coach:** the free-text question box now comes first, above the starter questions. Tapping a starter question used to put the answer at the top while the screen stayed scrolled down, so the answer was off-screen. It now scrolls to the answer, using a new `MainActivity.refresh(keepScroll = false)`.

---

## Verification

`gradle test` + `gradle :app:assembleApk`: **179 tests, 0 failures** (174 before; +5 effort-budget tests). The signed APK builds and verifies.

**New `EffortBudgetTest`** drives the real Activity and counts taps:

| Test | Asserts |
|---|---|
| `OneTapDoseTest` | 1 tap per dose, "Taken at" shown, finished group folds |
| `OneTapCheckInTest` | 1 tap, and mood, energy and swelling stay null (not invented) |
| `GuidedSessionEffortTest` | Phase-1 session 1: **exactly 12 taps** from Today, all 5 exercises logged |
| `BootAdjustEffortTest` | 3 taps from My leg, setting stepped 20° → 15° |
| `ScreenLoadBudgetTest` | Plan editor ≤ 60 texts and ≤ 36 targets; Progress ≤ 240 words; no duplicate Today tiles or chips; no second plan editor |

## Residual risks

- **On the phone:** try a guided session, especially holding the phone between sets, and the evening fold-away on Today. Check that the 0–10 buttons feel right one-handed.
- **Existing installs** keep their old reminder times, which is correct because they may be customised. New defaults apply to new setups.
- **Morning Today** carries the one-tap check-in buttons until you've checked in. I judged one tap to be worth eleven small buttons for a few hours each day, but that's a judgement call to test with real use.
