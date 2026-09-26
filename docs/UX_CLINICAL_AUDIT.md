# RecoverWell: user-journey and specialist-content audit (v3.7)

Date: 26 Sep 2026 · Branch: `claude/ux-achilles-content-audit-cz7xdo`

There were two parts to this audit:

1. **UX and user-journey audit.** Each end-to-end journey was scored out of 10, its friction points were listed, and the fixes were verified. Nothing counted as fixed unless it was backed by a code change and a test.
2. **Content, knowledge, instruction and advice audit.** Every piece of protocol content was reviewed from the position of a physio who specialises in Achilles rupture rehabilitation. The review applied to the app's scope, which is the conservative (non-surgical) functional pathway.

> **Honest scope.** I walked the scores through the real code paths and the JVM tests (Robolectric boots the real Activity). **They have not been validated on a physical device or with real patients.** No emulator exists in this build environment. The clinical review is written in a specialist physiotherapist's voice against published UK pathways and common specialist practice, but it does not replace sign-off by a registered physio. The treating team should confirm every change, exactly as the app's own disclaimer asks.

---

## Method

**Personas walked through every journey:**

- **A.** Day 2, in a VACOped boot at 30°, anxious, on apixaban.
- **B.** Installs at week 14, already out of the boot. This is the "late joiner".
- **C.** Week 26, working back to padel.

**Rubric per journey (10 = no friction found):**

- Task success
- Taps and cognitive load
- Clarity of language
- Error-proneness
- Safety and trust (the most heavily weighted criterion for a medical app)

A journey reaches 10 only when every friction point found is fixed or deliberately accepted, and the fix is covered by a test.

---

## 1. User-journey scores

| # | Journey | Before | After |
|---|---|:-:|:-:|
| J1 | First launch and onboarding (fresh injury) | 7 | 10 |
| J2 | Joining mid-recovery (late joiner) | **3** | 10 |
| J3 | Daily routine on Today | 6 | 10 |
| J4 | Doing the exercises (session, guided, logging) | 6 | 10 |
| J5 | Medication and reminders | 8 | 10 |
| J6 | "Can I…?" (Recovery coach) | **3** | 10 |
| J7 | "I'm worried" (red flags) | 7 | 10 |
| J8 | Phase progression (the two-key gate) | 5 | 10 |
| J9 | Boot changes and coming out of the boot | 6 | 10 |
| J10 | Physio appointment loop | 7 | 10 |
| J11 | Progress and insights | 7 | 10 |
| J12 | Return to sport | 8 | 10 |
| J13 | Reassurance and "what to expect" | 8 | 10 |
| J14 | Data, privacy and backup | 7 | 10 |

### J1: First launch and onboarding (7 → 10)

| Friction found | Fix |
|---|---|
| Step 1 showed a single-option "Injury & protocol" chip, captioned "more protocols can be added to the registry". That is developer language shown to a patient. | The picker only appears when there is a real choice. Otherwise the step states the plan in plain words. |
| A day-one patient had to face a target return date and an "out of the boot?" toggle. | Both are hidden during setup. They keep their defaults and stay editable later. |
| There was nowhere to record the clinic's phone number, although the red flags say "contact your clinic". | A "Your clinic" phone field was added to setup and to Injury & goal. It is carried in the backup. |

### J2: Joining mid-recovery (3 → 10)

| Friction found | Fix |
|---|---|
| The physio-confirmed phase always started at 1. A user joining at week 14 got **week-one boot exercises, boot checks and boot-angle warnings**. They then had to confirm phases 2, 3 and 4 one by one, and the prompt for that was hidden for the first 3 days. | A **"Where are you now?"** step appears when the injury date is past phase 1. The user picks the stage the physio actually confirmed (the typical stage is flagged), and enters their current boot setting or the date they came out of it. |
| Every phase confirmed at install was stamped with the install date. "Your pace" then read the user as **10+ weeks behind**. | Confirmation dates that aren't known are left out, so pace reads "not enough data" instead of a false figure. |
| "Add a blood-thinner reminder" seeded a course end date **in the past** for anyone past week 10. The reminder then **silently never fired**. | If the typical course has already ended, no end date is seeded. A review prompt is set for 7 days' time instead. |

### J3: Daily routine on Today (6 → 10)

| Friction found | Fix |
|---|---|
| The ring counted every exercise × every session: "4 of 23 done". The day felt unwinnable and the number was dominated by exercises. | The ring counts real actions. Each dose, each care task, each **session** and the check-in count once each. The home-screen widget uses the same maths. |
| The exercise-adherence tile assumed 3 sessions a day. A user on 1 session a day could never exceed 33%. | Adherence is now calculated in core against the user's own daily plan. |
| A boot change due today (a key weekly event) was rendered **last**, below every exercise. | It moved to directly under Medication, headed "Boot change due today". |
| The phase gate only appeared after 3 check-ins, and even then only if nothing else outranked it. | The gate is pinned and always shows as a full card. |
| Only the single top prompt was shown. Appointment prep, backup and insight prompts were **silently dropped**, although the README described a "More for you" list. | "More for you" is implemented as compact rows, so nothing is lost. |
| A focus card sat below the stats block, far down the page. | It now comes directly after the day's actions. |

### J4: Doing the exercises (6 → 10)

| Friction found | Fix |
|---|---|
| Every session repeated **every** exercise. Examples: a 25-minute swim or bike session three times a day, and in phase 5, jogging, hopping and padel drills three times a day. This is clinically inappropriate and made the list long. | Sessions are **dose-aware**. An exercise sits in as many sessions as its own prescription asks for. |
| There was no alternate-day scheduling. | Impact work now runs on **alternate days**, which fixes the contradiction with "no two running days back-to-back". |
| Timed activities showed "1 set × 1 · hold 10 min". The guided mode said "Start 60s hold" for walking and had **no timer** for anything over 299 s. | Timed work reads "10 min", "5 × 1 min" or "8 × 3 min · alternate days". Guided mode runs a real mm:ss timer and has a "Done this round" button. |
| The "per day" tile showed the global session count, not the exercise's own frequency. | The tile shows the exercise's own frequency, or "Alt days". |
| "Today's sessions" offered buttons for sessions the exercise isn't in. | Only the sessions the exercise is actually in are offered, with an explanation on its rest days. |
| There was no guidance on how much pain is acceptable. | A **pain-monitoring rule** appears on every exercise. |
| The per-exercise "times a day" was stored but ignored, and couldn't be edited. | "Times a day" is now editable and honoured. |
| The session overlay showed protocol doses, not the user's edited doses. | The overlay shows the user's own dose. |

### J5: Medication and reminders (8 → 10)

| Friction found | Fix |
|---|---|
| Every medicine, including a painkiller the user added, said "clot prevention matters" on the checklist and in notifications. | A new `Medication.isClotPrevention()` recognises generic and brand names, so the clot copy only appears for blood thinners. |
| There was no advice about NSAIDs, missed doses or head injury. | These were added to the medication notes, the coach and the red flags. |
| The course end date was shown as a raw ISO date. | It is shown as a friendly date. |

### J6: "Can I…?" Recovery coach (3 → 10)

| Friction found | Fix |
|---|---|
| **Matching bugs.** Substring matching sent "speed", "type", "open", "slope" and "pedal" to the **red flags**, because each contains "pe". "Cardio" and "care" answered **driving**. "Can I drive in my boot?" answered *walking without the boot*. "Foot" questions answered calf stretching. "Am I allowed to drive?" returned a generic phase summary. Answers ended in "..". | The matcher was rewritten. It uses whole words and checks **safety first**. Strong topic words beat weak context words ("boot off to sleep" is about sleep), and among strong matches the one mentioned first wins ("drive to work" is about driving). The punctuation is fixed. |
| The coach couldn't answer the questions patients ask most: sleeping, showering, stairs, crutches, work, flying, painkillers, swelling, a lump, shoes, gym, falls or fear of re-rupture. | A **16-entry, phase-aware FAQ library** was added. It lives in protocol data, so it scales per injury. |
| A described symptom got a generic "red flags" reply. | Symptom-specific answers now come with **one-tap Call 999, 111 or my clinic**. In AI mode the deterministic safety answer shows even if the model's reply doesn't cover it. |
| The AI prompt knew the phase name but not the plan. It also used surgical red flags ("infected wound"). | The AI is grounded in the phase's do / don't / precaution lists, the movement verdicts, the device state and UK services. The model is told never to suggest anything the plan marks "not yet". |
| For running, cycling and similar sports the sport check read "Can I play running?". | It now reads "Can I get back to running?". |

### J7: "I'm worried" (7 → 10)

| Friction found | Fix |
|---|---|
| There was no way to call from the red-flag screen. The user had to leave the app and dial. | **Call 999**, **Call 111** and **Call my clinic** buttons were added, or a prompt to save the clinic's number. These use `ACTION_DIAL`, so no new permission is needed. |
| Re-rupture advice said "with the wedges you last used", which is wrong for the default VACOped. There was no out-of-hours route. | The advice is device-neutral and adds "A&E if the clinic is closed". |
| There was no warning about a head injury while on a blood thinner, although a fall on crutches is common. | Added to the red flags, as NICE CG176 requires same-day assessment. |

### J8: Phase progression (5 → 10)

| Friction found | Fix |
|---|---|
| The gate prompt could stay hidden, and it went missing entirely for late joiners (see J2 and J3). | The gate is pinned, and late joiners set their stage at onboarding. |
| Confirming a phase was a silent state flip. The user didn't learn what changed. | A **"Phase N unlocked"** summary appears, covering focus, what is newly OK and the exercise count. The same flow is used from Physio visits. |

### J9: Boot changes and coming out of the boot (6 → 10)

| Friction found | Fix |
|---|---|
| Nothing prompted the key "I'm out of the boot" event. Boot checks kept firing, which is the complaint from the previous round. | A pinned phase-3 prompt asks "Out of the boot yet?", with a confirm button and a 7-day snooze. |
| "Coming up" in Physio visits still listed boot changes after the user had come out of the boot. | These are now filtered out. |
| Boot-change placement on Today. | Covered in J3. |

### J10: Physio appointment loop (7 → 10)

| Friction found | Fix |
|---|---|
| The pack only held data-driven points. It often read just "No flags from the app this period". | **"Worth asking at this stage"** adds four specialist questions per phase. One tap adds them to "my questions", and they are included in the copied pack. |
| The numbers left out the boot setting and weight-bearing. | Both are added. |

### J11: Progress and insights (7 → 10)

| Friction found | Fix |
|---|---|
| Stale copy: "save today's log above" on a screen with no log form, and "log your mood on the Progress tab" when mood is logged on Today. | Both are corrected, and a **"Log today's check-in"** row appears when today hasn't been logged. |
| "Medication: 0% of doses taken" appeared for users with **no** medication. | The line is hidden when no medication is scheduled. |
| **Milestones were "reached" by calendar alone.** A user their physio had kept in the boot was told "Milestone reached: Out of the boot". | A milestone only counts once its **phase is actually reached**. A passed date shows "typically ~date · waiting on your physio's go-ahead". |

### J12: Return to sport (8 → 10)

| Friction found | Fix |
|---|---|
| The heel-rise test had no pace and no height standard. | The test is standardised: one rep every 2 s, and a rep counts only at near-first-rep height. |
| There was no heel-rise **height** test, the key marker of an elongated tendon. | A **single-leg heel-rise height** symmetry test was added to the strength stage. |
| Result history showed ISO dates, and the log screen's app-bar title was blank. | Both are fixed. |

### J13: Reassurance and "what to expect" (8 → 10)

| Friction found | Fix |
|---|---|
| Common worries weren't pre-empted: evening swelling lasting 6–12 months, a lumpy or thick tendon at 8–12 weeks, calf size, pain relief in the first fortnight. | Each is added to the matching week band, and there is a new normal-versus-flag pair on swelling. |

### J14: Data, privacy and backup (7 → 10)

| Friction found | Fix |
|---|---|
| **About said video was "the only feature that uses the network", and Data said "no network". The optional AI sends questions and a summary to Groq.** | Both statements now accurately describe the two opt-in exceptions, and show when AI is on. |

**Cross-journey:** patient-facing ISO dates ("typically from 2026-09-01") are now friendly dates ("Tue 1 Sep"). Overlays that fell back to the tab name as their title now have proper titles.

---

## 2. Specialist-physio content audit

Scope: conservative functional rehabilitation (VACOped, Aircast or cast), UK NHS pathway (UKSTAR, trust leaflets). There is still no surgical content, by design.

| Area | Before | After | What changed |
|---|:-:|:-:|---|
| Protocol structure and timelines | 8 | 9.5 | These were already sound and cited. The wording is now device-neutral: "boot at neutral (foot flat)" instead of "all wedges out", and "lowering the heel angle faster than planned" instead of "removing wedges". |
| Exercise prescription and progression | **5** | 9.5 | See *Critical findings* below. |
| Load and pain monitoring | **3** | 9.5 | Pain-monitoring rule added: ≤3–4/10 during, settling within an hour and no worse the next morning. It appears on every exercise, in the phase 4 precautions and in the coach. |
| Protecting tendon length | 6 | 9.5 | "No calf stretching unless prescribed" replaces "strengthen before you stretch". The week-12 milestone no longer implies stretching begins. A heel-rise height test was added. A resting-angle (ATRA) check was added as a physio question. |
| Red flags and safety | 8 | 10 | Added: head injury on an anticoagulant, out-of-hours route for re-rupture, severe unremitting pain in the boot, device-neutral re-rupture first aid, one-tap calls. |
| Medication and VTE advice | 7 | 9.5 | Added: NSAID interaction, "never double up" after a missed dose, head-injury advice. The clot copy is limited to blood thinners. A course end date is never seeded in the past. |
| Activities of daily living | **4** | 9.5 | Phase-aware answers now cover sleeping in the boot, washing, stairs on crutches ("up with the good, down with the bad", or bumping on the bottom), weaning off crutches, work, flying/long travel (VTE), footwear (leveller for the other shoe, heel raise in both shoes), elevation dose, swimming, cycling and gym. |
| Return-to-sport testing | 7.5 | 9.5 | The heel-rise endurance protocol is standardised and a height-symmetry test was added. Thresholds are unchanged (LSI ≥ 90%). |
| Psychological support and expectations | 8 | 9.5 | New expectations cover evening swelling, a thickened tendon and calf size. A fear-of-re-rupture answer was added to the coach. |
| Language accuracy | 7 | 10 | "Repaired tendon" becomes "healed tendon". The surgical "once any wounds are healed" becomes "skin sores from the boot" in 3 places. "Wedges" advice no longer goes to VACOped users. "Play running" becomes "Get back to running". A test guards these terms. |
| AI grounding (optional feature) | 5 | 9 | The AI gets the plan, device, side, weight-bearing and UK red-flag routes. The journal red-flag detector covers re-rupture, bleeding or head injury, and boot or skin problems. |

### Critical findings (fixed)

1. **Phase 5's entry criterion couldn't be met through the programme.** Phase 5 requires "20–25 good single-leg heel raises", but no single-leg calf work was prescribed before phase 5. Phase 4's "Double-leg heel raises" is now a **heel-raise progression**:
   - Two legs, then up on two and down on one, then single-leg.
   - A move up a stage happens only when the current one is easy, at full height and no worse the next morning, and the physio agrees.

   A new **seated calf raise with weight** loads the soleus, the deep calf muscle most loaded in gait and running, which the programme had skipped.
2. **Impact dosing was unsafe.** Running, hopping, agility and sport drills were scheduled daily and repeated in every session. They now run once a day on **alternate days**. Phase 4 conditioning also moves to alternate days, which its own cue already asked for.
3. **Every phase is now paired with evergreen physio questions.** Examples:
   - Phase 1: "Can the boot come off to wash or sleep?"
   - Phase 3: "Is the tendon healing at the right length?" (resting angle)
   - Phase 4: "When can I progress to single-leg raises? How much load?"

   The questions reach the appointment pack.

### Deliberately unchanged (with rationale)

- **Timeline pace.** Some accelerated protocols (for example Willits 2010) begin controlled ankle ROM out of the boot at 2–4 weeks and jogging from about 12–16 weeks. The app keeps the more conservative UK trust pathway. Every date stays a physio-editable placeholder behind the two-key gate, because this choice belongs to the treating team, not an app.
- **ATRA self-measurement.** It needs a second person and clinical interpretation, so it is framed as a question for the physio rather than a self-test.
- **Demonstrations.** The renamed heel-raise progression reuses the double-heel-raise animation, and the loaded seated raise reuses the seated-raise animation. The "Watch video" search covers the exact movement. New keyframe animations are a possible follow-up.
- **No surgical pathway.** This is out of scope by design (review criterion 1).

---

## 3. Verification

`gradle test` + `gradle :app:assembleApk` (CI-equivalent):

| Suite | Before | After |
|---|:-:|:-:|
| core (pure logic + content guards) | 106 | 128 |
| app (Robolectric, real Activity) | 36 | 42 |
| draw | 4 | 4 |
| **Total** | **146** | **174** — 0 failures |

**New tests:**

- `AskCoachTest` covers 50+ real phrasings. It checks whole-word matching, symptom routing and dial targets, phase-aware answers and punctuation, and that every suggested question returns a real answer in every phase.
- `ClinicalAuditTest` covers alternate-day impact work, dose-aware sessions, timed prescriptions, adherence with 1 session a day, clot-only copy, phase-gated milestones, no-medication digest and pack, clinic phone backup round-trip, the phase 4 → 5 strength bridge, specialist safety content, conservative-only language, and physio questions per phase.
- `JourneyAuditTest` covers the late-joiner onboarding end to end, the 999 dialler intent and clinic call, the pinned gate plus "Phase 2 unlocked" summary, coach safety answers, the guided bike timer, and the out-of-boot prompt with its snooze.

Signed APK: v2+v3 signatures verified, invoke-dynamic guard clean, permissions unchanged.

## 4. Residual risks and next steps

- **On-device check.** This is the one thing the environment can't do. Walk J2 (late joiner), J3 (Today order) and J7 (tap "Call 999": it should open the dialler, not call) on the phone.
- **Clinical sign-off.** Show the physio the phase 4 heel-raise progression, the alternate-day impact schedule and the pain rule. These are the three changes that most affect what you physically do.
- **Free-text understanding.** The offline coach is keyword-based by design (deterministic and safe). Unusual phrasings fall back to a list of topics, and AI mode handles open conversation.
