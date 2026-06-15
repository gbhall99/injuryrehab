# RecoverWell — User Test Kit
*Validate the navigation before changing it. ~1 week, 5 users, no budget needed.*

Goal: find out whether the right features are in the right place and reachable in the
right order — specifically testing two hypotheses:

- **H1.** High-value features (Recovery journal, Ask my recovery, Physio visits) are
  *hidden* in the "More" tab and users won't find/return to them.
- **H2.** The "My leg" tab mixes one-time learning (boot setup, do/don't) with live
  status, so it loses ongoing value after the first few weeks.

Nielsen rule of thumb: **5 users surface ~85% of usability problems.** Aim for 5–7,
spread across recovery stages (see screener).

---

## 1. Recruiting blurb (post to r/AchillesRupture, a local physio clinic noticeboard, or a running club)

> **Recovering from an Achilles (or similar) injury? Help test a free recovery app — 25 mins, video call.**
> I'm building a phone app that helps people through rehab (daily check-ins, exercises,
> tracking, prepping physio visits). I'd love 25 minutes of your time to try it and tell
> me what's confusing — no prep needed, no right answers, you're testing the app not
> yourself. Happy to share the app with you afterwards. If you're in the first few weeks,
> mid-rehab, or returning to sport, all stages welcome. Comment or DM and I'll send a time.

**Screener (pick a spread):**
- Which best describes you? ☐ First 0–3 weeks ☐ ~1–3 months ☐ 3+ months / returning to sport
- Are you using any rehab app today? ☐ Yes ☐ No
- Phone: ☐ Android ☐ iPhone *(note: current build is Android)*
- Comfort with apps: ☐ Low ☐ Medium ☐ High *(deliberately include 1–2 "Low")*

---

## 2. Before you start (read aloud)
> "Thanks for helping. This is a test of the **app, not you** — if something's confusing,
> that's exactly what I need to learn. Please **think out loud**: say what you're looking
> at, what you expect, and what you'd tap. I'll mostly stay quiet. Okay if I record the
> screen and audio for my notes only?" ☐ Consent given

Hand over the phone on the **home screen**, nothing open.

---

## 3. Tasks (don't show them how — watch where they go)
For each: note **first tap**, whether they **succeed**, **# taps**, **time**, and any
quote. Read the scenario, not the feature name.

| # | Say this | Tests | Success = |
|---|----------|-------|-----------|
| 1 | "Show me how you'd record how your recovery felt today." | Daily loop / journal discoverability (H1) | Reaches check-in or voice journal |
| 2 | "You're not sure if you're allowed to drive yet. Find out." | Ask / capability discoverability (H1) | Reaches Ask my recovery or "Can I…" |
| 3 | "You've got a physio appointment next week. Get ready for it." | Physio visits / pack (H1) | Reaches Physio visits pack |
| 4 | "Something doesn't feel right and you're worried. What do you do?" | Red flags reachability (safety) | Reaches Red flags |
| 5 | "What should you expect over the next week of recovery?" | What to expect placement | Reaches What to expect |
| 6 | "Find what your leg can and can't do right now." | My leg value (H2) | Reaches My leg / capability |
| 7 | *(stage 1–3mo only)* "It's been a few weeks. Open My leg — is anything here new/useful to you today?" | H2 — ongoing value | Honest read on staleness |

After each task ask one question — **SEQ**: "How easy or hard was that? (1 very hard – 7 very easy)." Write the number.

---

## 4. Wrap-up questions (5 mins)
1. "What were the 3 most useful things in the app?" *(do journal/ask/physio surface unprompted?)*
2. "Was there anything you didn't realise was in here?" *(reveal the buried features; watch reaction)*
3. "If you opened this every day, what's the one thing you'd want first?"
4. "Anything that felt like it belonged somewhere else?"
5. "After the first couple of weeks, what would you stop using?" *(probes H2)*

---

## 5. Score sheet (one row per participant)

| P# | Stage | T1 journal | T2 ask | T3 physio | T4 redflags | T5 expect | T6 myleg | Mean SEQ | Top friction quote |
|----|-------|-----------|--------|-----------|-------------|-----------|----------|----------|--------------------|
| 1  |       | ✓/✗ taps  |        |           |             |           |          |          |                    |
| 2  |       |           |        |           |             |           |          |          |                    |
| 3  |       |           |        |           |             |           |          |          |                    |
| 4  |       |           |        |           |             |           |          |          |                    |
| 5  |       |           |        |           |             |           |          |          |                    |

**Read the results:**
- T1/T2/T3 success < ~70% or high taps → **H1 confirmed**: promote those features out of "More".
- T6/T7 feedback that My leg is "stuff I already know" → **H2 confirmed**: slim it to live-only.
- Any task where they tap **More first** = a sign that feature should be primary nav.

---

## 6. No time for calls? Unmoderated alternative
Ship via **Google Play internal testing**, then send testers this 6-question form:
1. Which screen do you open most? (Today / Exercises / Progress / My leg / More)
2. Did you find the daily voice check-in? (Yes easily / Yes eventually / No)
3. Would you use the voice check-in daily? Why / why not? *(free text)*
4. When you had a question ("can I drive?"), where did you look first?
5. After a few weeks, which screens do you still open? Which do you skip?
6. What's missing, or buried? *(free text)*

---

## 7. What I'll do with the results
Bring the score sheet back and I'll turn it into a concrete reorg PR (the three options
we discussed: *Journal replaces My leg*, *Keep My leg but slim it*, or *promote-only*) —
chosen by what the data actually shows, not a guess.
