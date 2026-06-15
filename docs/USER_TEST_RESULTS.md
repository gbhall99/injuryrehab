# RecoverWell — Usability Study Results (SIMULATED)

> ⚠️ **Method caveat.** This run was conducted with **AI-simulated personas**, not real
> injury sufferers. It reliably surfaces *structural* IA problems (wrong place, too deep,
> mislabeled, wrong first-click) but cannot stand in for real emotion, motivation, or
> edge behaviour. Treat success/taps as directional and **confirm the two starred calls
> with ≥3 real users** before the boldest changes.

Date: 2026-06-15 · Moderator: simulated · Build: `claude/ai-groq-assistant`
Hypotheses under test: **H1** high-value features buried in *More*; **H2** *My leg* goes
stale after the early weeks.

## Panel
| P# | Persona | Stage | Tech comfort |
|----|---------|-------|--------------|
| 1 | Dan, 34 — acute, in boot, anxious | Week 1 | Med-low |
| 2 | Eileen, 67 — cautious, phone-wary | Week 2 | Low |
| 3 | Tom, 38 — settling into routine | Week 6 | Medium |
| 4 | Priya, 41 — diligent tracker | Week 10 | High |
| 5 | Marcus, 52 — return-to-sport | Week 22 | High |

## Score sheet
Legend: ✓ success · ◑ partial/slow · ✗ failed · *(n)* taps · first-click in **bold**

| Task (scenario) | Dan | Eileen | Tom | Priya | Marcus | Success |
|---|---|---|---|---|---|---|
| **T1** record today's recovery | ✓ (2) **Today** | ✓ (2) **Today** | ✓ (2) **Today** | ✓ (2) **Today**, found voice | ✓ (1) **Today** | 5/5 |
| **T2** "can I drive yet?" | ◑ (4) **My leg** can-I, unsure | ✗ (5) **More**, lost | ◑ (4) **My leg**→More | ✓ (3) **More**→Ask "why's it buried?" | ✓ (3) **My leg** can-I | 2/5 ✓ |
| **T3** prep physio visit | ◑ (4) **More**, slow | ✗ **More**, "I'd ring the clinic" | ✓ (3) **More**→Physio | ✓ (3) **More**→Physio "took a while" | ✓ (3) **More**→Physio | 3/5 ✓ |
| **T4** worried, what to do | ✓ (3) **My leg**→Red flags | ◑ "I'd call 111" | ✓ (3) **My leg**→Red flags | ✓ (2) **More**→Red flags | ✓ (2) **My leg** | 4/5 ✓ |
| **T5** what to expect next wk | ◑ (4) **More** | ✗ **Progress**, gave up | ✓ (3) **Today**→More-for-you | ✓ (3) **More** | ✓ (3) **More** | 3/5 ✓ |
| **T6** what leg can/can't do | ✓ (1) **My leg** | ✓ (1) **My leg** | ✓ (1) **My leg** | ✓ (1) **My leg** | ✓ (1) **My leg** | 5/5 |
| Mean SEQ (1–7) | 4.5 | 2.7 | 5.0 | 5.3 | 5.7 | — |

### T7 — "Open My leg; anything new/useful today?" (stage 6wk+)
- **Tom (wk6):** "Boot setup I learned in week one. The do/don't I basically know now.
  The *watch-outs* are the only live bit." → mostly stale.
- **Priya (wk10):** "Honestly I don't open this tab anymore — nothing changes." → stale.
- **Marcus (wk22):** "Haven't touched it in months." → stale.

## What happened, task by task
- **T1 (record today): 5/5, easy.** The daily check-in on Today works for everyone; Priya
  discovered the 🎙 voice button and loved it ("this is the bit I'd actually do"). But
  only 1/5 found the **journal as a place** (history/insights) — it's reached *through*
  Today, not as a destination.
- **T2 (can I drive): 2/5.** Strongest H1 signal. Nobody's first tap was *Ask my recovery*
  — they went to **My leg** ("can-I lives there") or **More** and floundered. The natural-
  language answer engine is invisible.
- **T3 (physio prep): 3/5, slow.** *Physio visits* is findable in More but never the first
  guess; lower-confidence users abandoned. It only auto-surfaces on Today within 4 days of
  an appointment, so "next week" had no hint.
- **T4 (worried): 4/5.** Red flags is reasonably reachable (My leg button + More). Eileen
  defaulting to "call 111" is arguably the *right* real-world instinct, not a failure.
- **T5 (what to expect): 3/5.** Placement ambiguous — split between More, Progress, and the
  Today "More for you" row. Low-tech users missed it.
- **T6 (leg can/can't): 5/5, instant.** *My leg* nails the **live "what can I do"** need —
  this is the part worth protecting.

## Hypothesis verdicts
- **H1 — buried features: CONFIRMED (strong).** Ask my recovery (2/5) and Physio visits
  (3/5, slow) fail the first-click test; both are top features sitting in a settings-shaped
  tab. The voice journal is half-promoted (reachable via Today) but not a destination.
- **H2 — My leg goes stale: CONFIRMED, with nuance.** Its *static* content (boot setup,
  do/don't) is dead weight after ~3 weeks, **but** its *live* content (capability "Can I…",
  watch-outs) scored the best discoverability in the whole study (T6 5/5). So: **slim, don't
  delete.**

## Recommendation (data-driven)
Pick **"Keep My leg, slim it" + promote** — it matches the evidence better than replacing
the tab, because T6 shows the live capability view is the app's *most* discoverable feature.

Concretely:
1. **★ Promote Ask** — add a persistent "Ask anything about your recovery" entry on Today
   (the #1 failure, and exactly the anxious early-stage need). *Confirm with real users.*
2. **Promote the journal** to a first-class Today card (not only via the check-in button),
   so it's a place you go, not just an action.
3. **Give Physio visits a stable home** + show its prep prompt earlier than 4 days out.
4. **★ Slim My leg** — keep capability + watch-outs; collapse boot-setup & do/don't behind
   "Show phase reference". *Confirm the collapse with real users before shipping.*
5. Split *More* into **Features** vs **Settings** so it stops masquerading as the hub.

This is essentially **Option 2** from our discussion, which the simulated data supports over
the bolder "Journal replaces My leg" (that would sacrifice the strongest-performing view).

## Confidence
- High confidence (structural, low-risk): #1, #2, #3, #5.
- Medium confidence (validate with ≥3 real users): #4 (slimming My leg), and the exact form
  of the Today Ask entry.
