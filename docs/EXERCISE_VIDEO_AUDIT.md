# RecoverWell: exercise-video quality audit (v3.9)

Date: 26 Sep 2026 · Branch: `claude/ux-achilles-content-audit-cz7xdo` (restarted from `main` after PR #31 merged)

This audit used the same process as `docs/UX_CLINICAL_AUDIT.md` and `docs/SIMPLICITY_EFFORT_AUDIT.md`:

1. Measure what the user actually gets.
2. Score every exercise and every video journey out of 10.
3. Fix it.
4. Re-measure.

Clinical judgement came from a physio's viewpoint specialising in **conservative (non-surgical) Achilles rupture** rehabilitation.

> **Honest scope. Read this first.**
> - **No video was watched.** This environment's network policy blocks youtube.com. Every judgement below comes from **titles, channels and descriptions**, never from the footage itself.
> - **Search ranking is a proxy.** It was measured with a web search restricted to YouTube (`site:youtube.com …`, US index). That approximates YouTube's own ranking but cannot replicate it: YouTube personalises results, and runs vary.
> - **What this means in the app.** Nothing is presented as physio-checked. The "physio-checked" tier is deliberately empty (a test enforces it) until a clinician has watched each pick. See the review sheet at the end.
> - **Not validated on a device or with patients.**

---

## Headline

| | Before | After |
|---|:-:|:-:|
| Top-5 results that show the right movement safely, all 28 exercises | **15 / 140 (11%)** | **123 / 135 (91%)**, with ankle pumps withheld |
| Top-5 results unsafe for this pathway, phases 1–3 | **53 / 80 (66%)** | **3 / 75 (4%)** |
| Exercises where "Watch video" plays a specific, pre-screened clinical demo | 0 / 28 | **24 / 28** |
| Average per-exercise score (rubric below) | **1.1 / 10** | **8.0 / 10** |
| Video journeys at 10/10 | 0 / 8 | **8 / 8** |

**Why the old results were so poor.** Every exercise searched for "*name* + Achilles rupture rehab physiotherapy". The generic tail swamped the exercise name, so nearly every exercise returned the same videos:

- Achilles overviews
- **stretching** (e.g. "Achilles Tendon Rupture Stretches & Exercises")
- **post-surgical protocols** ("Seated heel raise - INITIAL ACHILLES REPAIR", "Achilles Rehab after Surgery")
- week-14 programmes that are far beyond phase 1–3

For someone in a boot on the conservative pathway, most of these were the wrong advice.

---

## Rubric

### Per exercise (10 = a clinician-verified demonstration of exactly this exercise, safe for this phase)

| Part | Points | 10/10 needs |
|---|:-:|---|
| Relevance of what plays first | 0–4 | A single-movement demo of exactly this exercise, as prescribed (boot on where the plan says). **3:** right movement, but no boot or only part of the exercise. **2:** search where ≥4 of the top 5 match. **1:** 2–3 of 5. **0:** ≤1 of 5. |
| Safety fit | 0–3 | Nothing shown first conflicts with the plan, and the "your plan comes first" strip is visible. **2:** no unsafe hits, no strip. **1:** 1–2 unsafe hits. **0:** ≥3 unsafe hits. |
| Source shown | 0–2 | A named clinical source, shown to the user. **1:** search where clinical channels rank top. |
| Verified | 0–1 | Watched and confirmed by a clinician. |

**What counts as unsafe for this pathway:**

- surgical-repair protocols
- calf **stretching**, or anything pulling the foot up past neutral before the physio allows it
- tendinopathy loading below step level
- work beyond the phase, such as week-14 programmes in the boot phase, or single-leg bridges while in the boot

### Journeys

Journeys use the rubric from the earlier audits: task success, effort, clarity, error-proneness, and safety and trust. **Safety and trust is weighted most.**

---

## Per-exercise results

"Top-5" counts the first five YouTube results that show the right movement safely (M+S). "Unsafe" counts the top-5 results that are unsafe for this pathway.

| Exercise | Top-5 M+S before → after | Unsafe before → after | What "Watch video" does now | Score |
|---|:-:|:-:|---|:-:|
| Toe wiggles & scrunches | 0 → 4 | 4 → 1 | Suggests a physio toe-curl demo | 0 → 8 |
| Seated knee bends | 0 → 4 | 4 → 0 | Suggests an NHS GGC demo | 0 → 8 |
| Straight-leg raise | 0 → 5 | 3 → 0 | Suggests an NUH physiotherapy demo | 0 → 8 |
| Side-lying hip raises | 0 → 5 | 4 → 0 | Suggests a MedBridge demo | 0 → 8 |
| Glute squeezes & gentle bridges | 0 → 4 | 4 → 1 | Suggests a PT glute-set demo | 0 → 8 |
| Weight-bearing in boot | 1 → 5 | 2 → 0 | Suggests a physio partial-weight-bearing crutch demo | 1 → 8 |
| Seated knee extensions | 0 → 5 | 4 → 0 | Suggests a DPT demo | 0 → 8 |
| Two-leg bridges | 0 → 5 | 4 → 0 | Suggests a PT demo | 0 → 8 |
| Clamshells | 0 → 5 | 3 → 0 | Suggests a sports-PT demo | 0 → 8 |
| Seated core & upper body | 0 → 5 | 4 → 0 | Suggests band and trunk demos | 0 → 8 |
| **Ankle pumps (to neutral)** | 0 → 1 (best of 4 queries) | 3 → 2 | **No video, and the app says why.** The animation stops at neutral. | 0 → 7 |
| Ankle in/out | 0 → 5 | 4 → 0 | Suggests a PT demo | 0 → 9 |
| Seated heel raises | 0 → 4 | 4 → 1 | Suggests an NHS trust (NHFT) demo | 0 → 9 |
| Gait practice in shoes | 2 → 2 | 1 → 0 | Tuned search. The top two are rupture-specific gait-retraining videos. | 2 → 4 |
| Stationary bike | 2 → 4 | 1 → 0 | Suggests a PT saddle-height demo | 2 → 8 |
| Towel scrunches | 0 → 5 | 4 → 0 | Suggests a Pocket Physio demo | 0 → 9 |
| Heel-raise progression | 1 → 4 | 0 → 0 | Suggests an NUH demo, then a progression demo | 2 → 8 |
| Seated raises with weight | 2 → 5 | 0 → 0* | Suggests a physio soleus tutorial | 2 → 8 |
| Single-leg balance | 0 → 5 | 0 → 0 | Suggests a MedBridge demo | 2 → 9 |
| Band ankle pushes | 0 → 4 | 0 → 0 | Suggests a DPT demo | 2 → 8 |
| Step-ups | 0 → 4 | 0 → 0 | Suggests a physiotherapy demo | 2 → 9 |
| Bodyweight squats | 2 → 5 | 0 → 0 | Suggests a PT demo | 3 → 9 |
| Swim / bike conditioning | 1 → 5 | 0 → 0 | Tuned search | 2 → 6 |
| Single-leg heel raises | 1 → 5 | 0 → 0 | Suggests a DPT demo | 2 → 9 |
| Walk-jog | 0 → 5 | 0 → 0 | Suggests a physio explainer | 2 → 8 |
| Hop progression | 3 → 5 | 0 → 0 | Suggests a PT progression demo | 3 → 9 |
| Direction-change drills | 0 → 4 | 0 → 0 | Suggests a sports-medicine PT demo | 2 → 8 |
| Sport drills | 0 → 5 (padel) | 0 → 0 | A search for **your** sport: footwork (court sports), pedalling (cycling), kick drills (swimming), plyometrics (gym) | 2 → 6 |

\* Two of the old seated-raise results were Achilles-**tendinopathy** eccentric videos. Their titles pass the rule, but those programmes lower the heel below step level. The new query and pick avoid them.

**Why unsafe counts differ between phases.** Phases 4–5 show 0 unsafe hits before because week-14-type content is no longer premature by then, so those results weren't counted as unsafe. The phase-1–3 counts are strict about that.

### Clinical calls made

1. **Ankle pumps get no YouTube video at all.** Four queries were tried. Nearly every ankle-pump or active-range video pulls the foot up past neutral, which is the one movement this plan forbids before about 12 weeks. The best query found 1 safe match in 5, with 2 unsafe. Showing those results would undo the app's own advice.
   - The exercise now says **"No YouTube video for this one"** and gives the reason.
   - There's no Watch button, in the exercise screen or the session player.
   - The animation stays as the demo; a test locks it to never going past neutral.
   - The user's own clip, or a link from their physio, still plays.
2. **Every video is labelled for what it is.** The labels are "Your chosen video", "Physio-checked" (empty for now), "Suggested … not yet checked by a physio", or YouTube search results with the exact query shown.
3. **A permanent safety strip sits under every video:** *"Your plan comes first: boot on where it says, and no calf stretching or pulling the foot up past neutral unless your physio said so."* This matters because most picks for boot-phase exercises show bare legs.
4. **Screening rules for a pick.** A pick needs:
   - a title naming the exact movement
   - a clinical source: NHS trusts (NUH, NHS GGC, NHFT), MedBridge, Bupa, or named physios and DPTs
   - no framing from a conflicting pathway (surgery, stretching, tendinopathy loading below step)

   A generic movement from another condition's series is allowed, because the movement is the same (a step-up from an osteoarthritis series, for example). Titles are kept verbatim, so the banner matches what plays.
5. **Animations fixed** (these are the offline floor):
   - **Clamshells** were drawn without the boot in a boot phase. The boot is now drawn and the exercise is renamed **"Clamshells (boot on)"**, cueing lying on the uninjured side.
   - **Heel-raise progression** only animated two-leg raises (stage 1 of 3). It now shows up on two, down on one.
   - **Weighted seated raises** reused the unweighted phase-3 animation. They now have their own, with the weight on the knee.
   - Two guard tests stop these recurring: the boot is shown exactly in boot phases, and there are no orphan animations.

---

## Journey scores

| # | Journey | Before | After | What changed |
|---|---|:-:|:-:|---|
| V1 | Watch a demo from the exercise screen | **2** | 10 | 24/28 exercises play a specific pre-screened clinical demo straight away. The rest get a tuned search, and ankle pumps get an explanation. Every video is labelled honestly and shows the safety strip. |
| V2 | Check form mid-session | 4 | 10 | The session player had no video. The user had to leave the session and find the exercise. Now there's **one tap** ("Watch video demonstration") and back to the same set. A running hold timer is stopped first, so it can't rebuild the video behind you. |
| V3 | Keep a video you trust | 5 | 10 | Leave the app, find and copy a link, come back, go to Adjust dose or video, then paste. Now it's **one tap: "Use this video"**. The banner updates in place without restarting playback. Pasting a physio's link still works. |
| V4 | Watching with no connection | 3 | 10 | The WebView showed a browser error page. Now it says **"You're offline"** and points to the animation, which works without internet. "Search results" retries. The API fallback wait is 12 s instead of 6 s for slow mobile data. |
| V5 | Getting proper clips into the app | **1** | 10 | The production brief was stale: 25 clips for 29 exercises, a removed demo, clamshells without the boot, and both new phase-4 exercises missing. There was also nowhere to put finished clips. Now: **More › Your own demo clips** takes a folder. A clip named by demo or exercise id replaces the animation in the exercise screen and the session player. It plays offline, looped and muted, and falls back to the animation if it can't play. Each exercise shows its expected file name. The brief is synced to **26 clips / 28 exercises**, and a test fails if they drift apart. |
| V6 | Accessibility | 6 | 10 | Captions are on by default (`cc_load_policy`, English). The Watch buttons have spoken labels. |
| V7 | Safety and honesty | **2** | 10 | Before, nothing said a video might contradict the plan. Now there's the strip, the tier banners and the ankle-pump withholding, and nothing can be labelled physio-checked unless it's been watched (enforced by a test). |
| V8 | Offline animations | 6 | 10 | The three mismatches above are fixed and guarded. |

**Smaller fixes along the way:**

- The player's old caption pointed to "Pin a favourite on the exercise screen". That option had moved in the simplicity audit, so the caption now points to the right place.
- "Use best match" was a dead button when nothing was pinned. It's now "Remove my video", shown only when there's a video to remove.
- Each sport has its own drill search. "Cycling footwork drills" meant nothing, so cycling now searches for pedalling drills.
- The search for a single-suggestion exercise now stays in the app instead of jumping to YouTube.

---

## What 10/10 per exercise still needs

The journeys are at 10. **Per-exercise scores top out at 9 because nobody has watched the videos yet.** That's the one thing this environment couldn't do, and it isn't worth faking. Three concrete routes remain:

1. **A physio watches the picks** in the review sheet below: 41 picks, 39 distinct videos, about a minute each. Each confirmed pick moves from `VideoSuggestions` into `ExerciseVideo.curated`, a one-line change. It then shows as "Physio-checked" (+1).
2. **Bespoke clips** for the boot phases and the gaps. That covers:
   - the 10 boot-phase exercises (+1 relevance, because stock videos show bare legs)
   - ankle pumps
   - gait practice
   - swim / bike conditioning
   - sport drills

   The brief (`docs/exercise-demo-video-prompts*.md`) is ready to hand to a video-generation AI or a physio with a camera. Drop the files into the clips folder; no app release is needed.
3. **The user's own check.** For one person, watching a suggestion and tapping "Use this video" already makes it their verified choice.

---

## Verification

- `gradle test` + `gradle :app:assembleApk`: **193 tests, 0 failures** (179 before this round, so 14 new). The signed APK (3.9, code 29) builds and verifies with v2 and v3 signatures.
- **New core tests:**
  - every exercise has its own tuned query
  - queries avoid surgical, stretching and tendinopathy terms, in every sport
  - each sport's drill search is its own
  - ankle pumps are never sent to a search
  - tier order: pinned → curated → suggested → search → none
  - nothing is labelled physio-checked
  - suggestions are well-formed, with 11-character ids and safe titles
  - the production briefs list exactly the app's 26 clips
- **New app tests:**
  - `SuggestedVideoTest`: an honest banner and safety strip, the exact video embedded with captions, and "Use this video" pins it **without rebuilding the player**
  - `SearchVideoTest`: the tuned, sport-aware search, shown to the user
  - `OfflineVideoTest`: plain words and no load
  - `OwnClipsFallbackTest`: a vanished clip folder keeps the animation
  - `NoSafeVideoTest`: ankle pumps give a reason, never a search, and a physio's pin still works
  - `DemoLibraryTest`: the boot is shown exactly in boot phases, and there are no orphan animations

## Residual risks

- **Unwatched picks.** A video's content can differ from its title. The banner and strip say so, and the review sheet is the fix.
- **YouTube changes.** Videos get deleted or made private, and embedding gets blocked. The player falls back to the tuned search automatically; if the API fails to load it waits 12 s first. A dead pick costs one extra second of the user's time, not a broken screen.
- **Own clips** are indexed once per folder change. A very large cloud folder may pause the first exercise screen briefly.
- **Ranking drift.** Queries were tuned against a proxy of YouTube's ranking. Re-run the measurement every few months.

---

## Review sheet for a physio (all 41 picks)

Each link opens the video. Confirm that it shows the movement the app prescribes, at this phase, with nothing that contradicts a conservative plan. Pay particular attention to calf stretching, dorsiflexion past neutral in phases 1–3, and heels dropping off a step.

| Exercise | Video | Source | Check when watching |
|---|---|---|---|
| Toe wiggles & scrunches | [Toe Curl Exercise](https://youtu.be/FxHeokSwEes) | Rehab My Patient physiotherapy exercise library | Curls a towel. In the boot, only curl and spread the toes; the heel stays grounded. |
| Seated knee bends | [Knee Exercises in Sitting](https://youtu.be/TQyUvwnWdVs) | NHS Greater Glasgow and Clyde | Seated knee bend and straighten only, with nothing pushing through the foot. |
| Seated knee bends | [Seated Knee Bending Exercise](https://youtu.be/gd7Y9gmDBOE) | Concord Hospital rehabilitation services | From a joint-replacement series, but it shows a generic seated knee bend. |
| Straight-leg raise | [Straight Leg Raise](https://youtu.be/ie-tyGqon0w) | Nottingham University Hospitals NHS physiotherapy | Standard straight-leg raise. The app adds "boot on". |
| Straight-leg raise | [Straight Leg Raise Exercise • How To Do Properly](https://youtu.be/EWGR5mTPzsU) | Margaret Martin, physical therapist | Standard straight-leg raise. The app adds "boot on". |
| Side-lying hip raises | [How to Do a Sidelying Hip Abduction](https://youtu.be/UmmBtOG2N_s) | MedBridge clinical exercise library | Standard technique. The app adds "boot on". |
| Side-lying hip raises | [Strengthen Your “Gluteus Medius” with Side-Lying Hip Abduction](https://youtu.be/dBQXWsdrnfo) | Pain Science Physical Therapy | Standard technique. The app adds "boot on". |
| Glute squeezes & gentle bridges | [Glute Sets](https://youtu.be/TPUcaCNKwnY) | VNA Health Group physical therapy | Glute squeeze. Exactly the phase-1 start. |
| Glute squeezes & gentle bridges | [How to Do a Glute Bridge Exercise: A Guide from Physical Therapists](https://youtu.be/PhTDzR0TpZs) | Physical therapists | A two-leg bridge only. Phase 1 pushes mainly through the good foot. |
| Weight-bearing in boot | [Learn Crutches, Partial Weight Bearing (PWB), in 45sec](https://youtu.be/TdIESUPLSMw) | Physiotherapist | Crutch technique for partial weight-bearing. The boot stays on. |
| Seated knee extensions | [Seated Knee Extension (LAQ)](https://youtu.be/VuJZ6dqMf8M) | Ask Doctor Jo, physical therapist | A long-arc quad. The app adds "boot on". |
| Seated knee extensions | [Seated Knee Extension - PT Exercise](https://youtu.be/v_R4c04GuKE) | OneStep Digital Physical Therapy | A long-arc quad. The app adds "boot on". |
| Two-leg bridges | [How to Do a Glute Bridge Exercise: A Guide from Physical Therapists](https://youtu.be/PhTDzR0TpZs) | Physical therapists | A two-leg bridge only. Phase 1 pushes mainly through the good foot. |
| Two-leg bridges | [How to do a glute bridge](https://youtu.be/XLXGydU5DdU) | Bupa Health | A two-leg bridge only. Single-leg progressions are not for the boot phase. |
| Clamshells | [How To Do The Clamshell Exercise](https://youtu.be/EG5_gXcfozw) | Kinetic Sports Rehab physical therapy | Standard technique. The app adds "boot on". |
| Clamshells | [Clam Shell Exercise: Strengthen Your Hip & Knees by Physical Therapist](https://youtu.be/2c5xiz4q7ow) | Physical therapist | Standard technique. The app adds "boot on". |
| Seated core & upper body | [Upper Body Seated Resistance Band Exercises](https://youtu.be/sbzR-daGhag) | Intermountain Healthcare exercise physiologist | Band part of the circuit. The foot rests flat. |
| Seated core & upper body | [Seated Trunk Rotation](https://youtu.be/esNSztn8OWQ) | Ask Doctor Jo, physical therapist | Trunk-rotation part of the circuit. |
| Ankle in/out | [Seated Inversion Eversion Exercise for Foot and Ankle](https://youtu.be/9CJkHt7Cbag) | Congruency Therapy & Wellness physical therapy | A small, unforced range. No "as far as you can". |
| Ankle in/out | [Seated ankle inversion & eversion ROM](https://youtu.be/sd8WSPKMIYs) | Doctor of Physical Therapy | A small, unforced range. No "as far as you can". |
| Seated heel raises | [Seated heel raise](https://youtu.be/M5j_CfIobHE) | Northamptonshire Healthcare NHS physiotherapy (NHFT) | Bodyweight only. The heel does not drop below neutral. |
| Stationary bike | [Bicycle Seat Height: Do It Right For Comfort & Speed (Stop Knee Pain)](https://youtu.be/B5jBa94dNZ4) | Bob & Brad, physical therapists | Covers saddle height only. The app covers easy, heel-first pedalling. |
| Towel scrunches | [Towel scrunch \| Foot exercises](https://youtu.be/ztgcEsuqves) | Pocket Physio | The heel stays grounded. |
| Towel scrunches | [Toe Curl Exercise](https://youtu.be/FxHeokSwEes) | Rehab My Patient physiotherapy exercise library | Curls a towel. In the boot, only curl and spread the toes; the heel stays grounded. |
| Heel-raise progression | [Heel Raises](https://youtu.be/Y_R1CICW6Rw) | Nottingham University Hospitals NHS physiotherapy | Stage 1 (two legs) of the three-stage progression. |
| Heel-raise progression | [The Calf Raise - Exercise Progression](https://youtu.be/HmgXnST4Mdw) | Tim Keeley, physiotherapist (Physio REHAB) | Two legs to one. Its later loaded or off-a-step stages go beyond phase 4. |
| Seated raises with weight | [Seated Soleus Muscle Strengthening Exercise Tutorial (Level 2)](https://youtu.be/KogqCeFHhbE) | Online Physio Exercises | Check that the weight sits on the knee. |
| Seated raises with weight | [Seated Soleus Muscle Strengthening Exercise Tutorial (Level 1)](https://youtu.be/u45C0NByqF8) | Online Physio Exercises | The unloaded lead-in (level 1). |
| Single-leg balance | [How to Do a Single Leg Balance Exercise](https://youtu.be/IF1PymZNN0k) | MedBridge clinical exercise library | A 30 s single-leg stance. The eyes-closed and cushion progressions are not shown. |
| Band ankle pushes | [Ankle Plantar Flexion with Resistive Band](https://youtu.be/xgW2hhgjUz0) | Ask Doctor Jo, physical therapist | The return stops at neutral. The band must never pull the foot up. |
| Step-ups | [Step Up Exercise \| Osteoarthritis Physiotherapy](https://youtu.be/wfhXnLILqdk) | Physiotherapy demonstration | A low step, affected foot first. |
| Step-ups | [Physio Hip and Knee Exercises: Step Ups](https://youtu.be/j_FG0quhQMQ) | Physiotherapy exercise series | A low step, affected foot first. |
| Bodyweight squats | [How to Do Squats: A Guide from Physical Therapists](https://youtu.be/L61HQqjYFdQ) | Physical therapists | Heels stay down; depth only as comfort allows. |
| Bodyweight squats | [Physiotherapy: Squats](https://youtu.be/59AOEyi7STU) | Physiotherapy demonstration | Heels stay down; depth only as comfort allows. |
| Single-leg heel raises | [Single Leg Heel Raises](https://youtu.be/-IqeI-mMQLQ) | Ask Doctor Jo, physical therapist | On a flat floor, full height, not dropping off a step. |
| Single-leg heel raises | [Physio Led Pilates for Calf Strength: How to do a single leg heel raise](https://youtu.be/8gR4Lfp5dBw) | Physio-led Pilates | On a flat floor, full height. |
| Walk-jog | [Return to Running after Knee and Ankle Injuries](https://youtu.be/qjZOX0L8PaM) | Tim Keeley, physiotherapist (Physio REHAB) | An explainer, not a demo. Check it covers walk-jog intervals. |
| Walk-jog | [How To Return To Running Safely - Guidance from Physical Therapists](https://youtu.be/vPEOoE64rD8) | Physical therapists | An explainer, not a demo. Check it covers walk-jog intervals. |
| Hop progression | [Achilles Tendon Physical Therapy Treatment \| Plyometrics, Impact, Jumping Progressions](https://youtu.be/3oFMGHVbTBk) | Physical therapy clinic | Check it stays at low-level hops first. |
| Hop progression | [Single Leg Step Hops](https://youtu.be/ldlVqjdlpSo) | POGO Physio | The single-leg stage, after two-leg pogo hops. |
| Direction-change drills | [Agility Exercise for ACL: Lateral Shuffle](https://youtu.be/iBmvPEWt5og) | MedStar Sports Medicine physical therapist | Shuffles only (no diagonal cuts). Framed for ACL rehab, but the drill is generic. |

**No pick, by design:**
- Ankle pumps: withheld, as explained above.
- Gait practice and swim / bike conditioning: no clinical-source demo was found. The tuned search ranks the best first.
- Sport drills: a per-sport search.
