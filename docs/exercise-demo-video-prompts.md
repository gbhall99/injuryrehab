# Exercise demonstration videos — production brief / AI prompt

**Purpose.** RecoverWell currently ships lightweight 2-D "stick-figure" animations as exercise references. They read as crude and undersell the clinical quality of the content. This document is a complete brief you can hand to a video-generation AI (or a physio + videographer) to produce **proper demonstration clips** — one per movement — that drop straight back into the app.

**App context.** RecoverWell coaches a patient through a **conservative (non-surgical) Achilles tendon rupture** on a UK NHS-style pathway. Patients are often in a **walking boot** (OPED VACOped or an Aircast-style walker) for the early phases, progress through five rehab phases over ~6–9 months, and many are working back toward a sport (padel/tennis/running/football/etc.). The audience is anxious, non-athletic-by-default adults who need to *trust* the movement and copy it safely.

---

## 1. Global style & technical spec (applies to every clip)

- **Format:** short silent loop, **12–25 seconds**, seamless loop point. MP4 (H.264) + WebM; also export a 3-second poster frame (still) per clip.
- **Orientation & size:** **portrait 1080×1350** (4:5) primary, plus a 1:1 square crop. It plays inside a phone card ~230 dp tall.
- **No reliance on audio.** All guidance is **on-screen text**: a short title and 2–3 rotating form-cue captions (large, high-contrast, lower third). Assume captions are the only narration.
- **One person, full body in frame**, neutral athletic clothing, bare lower legs so the ankle/calf are visible — **except where the script says "boot on,"** in which case a realistic walking boot must be worn on the affected leg.
- **Plain, calm background** (light studio or clean room), soft even lighting, no clutter, no music branding. Consistent subject, wardrobe and set across all clips so the library feels uniform.
- **Demonstrate perfect, controlled form at the prescribed tempo** — slow eccentric ("lowering is the medicine"). Show **2–3 clean reps**, not a full set. Never show fatigue, grimacing, or end-range stretching.
- **Affected side:** film the affected leg as the **left** by default; the app also has right-affected users, so keep framing **mirror-safe** (no left/right text baked into the video; the app supplies side wording).
- **Accessibility:** captions ≥ 24 px equivalent, WCAG-AA contrast, no fast flashing, motion smooth.
- **Safety overlays:** where an exercise has a hard limit (e.g. "to neutral only — no stretch," "band must not pull the foot up"), bake that as a persistent caution caption.

### Deliverable naming (critical for app integration)
Name each file by its **`demoId`** exactly, e.g. `toe_scrunch.mp4`, `single_heel_raise.mp4`. The app maps videos to exercises by this id. There are **25 distinct clips** below covering 29 exercises (a few exercises intentionally share a clip — noted as "Used by").

---

## 2. Per-clip scripts

Each block: **Equipment · Setup · Action · Form cues to caption · Tempo/reps to depict · Avoid (show the *right* way) · Camera.**

### Phase 1 — Protect & activate (boot on, non-/partial weight-bearing)

**`toe_scrunch`** — *Toe wiggles & scrunches*
- Equipment: walking **boot on**. Setup: seated, leg supported, ankle still inside boot. Action: spread/wiggle all five toes, then scrunch gently, rhythmic. Cues: "Boot stays on, ankle completely still"; "Spread and wiggle, then scrunch"; "Slow, rhythmic — a circulation pump". Depict: ~20 slow reps (show 4–5). Avoid: any ankle/boot movement. Camera: side-on close on foot/boot.

**`knee_flex`** — *Seated knee bends (boot on)*
- Equipment: boot on. Setup: sit on chair/bed edge, boot on. Action: slowly bend and straighten the knee, letting the boot hang/swing. Cues: "Let the boot swing — no push through the foot"; "Comfortable range only". Depict: 2×10, slow. Avoid: loading the foot. Camera: side-on, full leg.

**`slr`** — *Straight-leg raise (boot on)*
- Equipment: boot on. Setup: lie on back, other knee bent, affected leg straight in boot. Action: tighten thigh, lift whole leg ~30 cm, lower slowly. Cues: "Tighten the thigh first"; "Lift to ~30 cm, lower with control". Depict: 3×10, 2 s hold. Avoid: jerking, lifting too high. Camera: side-on at floor level.

**`hip_abd`** — *Side-lying hip raises (boot on)*
- Equipment: boot on. Setup: lie on the **un-affected** side, legs stacked. Action: lift the booted leg straight up sideways, pause, lower slowly. Cues: "Keep the leg straight"; "Smooth up, slow down". Depict: 2×10, 2 s hold. Avoid: rolling the pelvis, sloppy swings. Camera: front-on to the back of the subject, full body.

**`bridge`** — *Glute squeeze & gentle bridge* — **Used by:** `p1_glute_squeeze`, `p2_bridge`
- Equipment: boot on (phase 1 light version; phase 2 shares weight). Setup: on back, knees bent, foot/boot flat. Action: squeeze glutes, lift hips a few cm to a straight shoulder-to-knee line, lower. Cues: "Squeeze the glutes"; "Push through the un-affected foot early on"; "Hips only as high as comfortable". Depict: 2–3×10, 3 s hold. Avoid: pushing up onto the toes of the booted foot. Camera: side-on.

### Phase 2 — Controlled loading (boot, increasing weight-bearing)

**`boot_walk`** — *Weight-bearing practice in boot*
- Equipment: boot on, crutches. Setup: stand tall between crutches, boot flat. Action: shift weight onto the booted leg; show progression two crutches → one → none, heel-to-toe rolling steps. Cues: "Shift weight as comfort allows"; "Heel-to-toe rolling steps"; "Sharp tendon pain → ease off". Depict: slow controlled steps. Avoid: lurching, hard heel strike. Camera: side-on tracking a few steps.

**`leg_ext`** — *Seated knee extensions (boot on)*
- Equipment: boot on. Setup: sit tall on a chair. Action: straighten the affected knee until the boot is level, hold, lower slowly. Cues: "Move only the knee"; "Ankle stays protected in the boot"; "Hold, then lower slow". Depict: 3×10, 3 s hold. Avoid: ankle motion. Camera: side-on, full leg.

**`clamshell`** — *Clamshells*
- Equipment: none (boot may be off-camera/resting). Setup: side-lying, knees bent, feet together. Action: open the top knee like a clamshell without rolling the pelvis back. Cues: "Don't roll the pelvis back"; "Slow up, slow down"; "Feel it in the hip, not the ankle". Depict: 3×12, 1 s hold. Avoid: pelvis rocking. Camera: front-on to subject's back, hips in frame.

**`seated_core`** — *Seated core & upper-body circuit*
- Equipment: light resistance band, chair. Setup: sit tall, booted foot resting flat. Action: montage of shoulder presses, band rows, gentle trunk rotations. Cues: "Sit tall, booted foot rests flat"; "Quality over speed"; "Nothing pushing through the foot". Depict: a few reps of each. Avoid: loading the foot. Camera: front-on, upper body.

### Phase 3 — Restore motion & gait (boot weaning; **no stretch before ~12 weeks**)

**`ankle_pump`** — *Active ankle pumps (to neutral only)*
- Equipment: **boot off** for the exercise, leg supported. Action: point the foot down as far as comfortable, return **only to flat/neutral**. Cues (one as persistent caution): "Up to NEUTRAL only — never pull into a stretch"; "Slow and controlled". Depict: 3×10. Avoid: dorsiflexing past neutral. Camera: side-on close on foot/ankle.

**`ankle_inv_ev`** — *Gentle ankle in/out movements*
- Equipment: boot off, foot relaxed mid-position. Action: slowly turn the sole inward, then outward, small range. Cues: "Small, controlled range"; "No forcing"; "Sharp pulls near the heel → shrink the range". Depict: 2×10. Avoid: large/forced range. Camera: side-on/front-on close on foot.

**`seated_heel_raise`** — *Seated heel raises*
- Equipment: none; knees at 90°, feet flat. Action: push through the ball of the affected foot to lift the heel, lower slowly. Cues: "Push through the ball of the foot"; "The lowering is the medicine"; "Body weight only". Depict: 3×12, 1 s hold. Avoid: bouncing, added load. Camera: side-on, foot + heel.

**`gait_walk`** — *Gait practice in shoes (heel-raise insert)*
- Equipment: supportive shoes with heel-raise inserts. Action: short indoor walk — heel down, roll through, gentle push-off, even step lengths. Cues: "Heel down, roll through, push off gently"; "Even step lengths"; "Boot back on for crowds/uneven ground". Depict: a few symmetrical strides. Avoid: limp, uneven steps. Camera: side-on tracking + a front-on for symmetry.

**`bike`** — *Stationary bike / cross-training* — **Used by:** `p3_bike`, `p4_swim`
- Equipment: stationary bike (and a brief pool/gentle-kick insert for the phase-4 swim variant). Setup: saddle slightly higher than usual, minimal resistance. Action: smooth relaxed pedalling through heel/midfoot. Cues: "Saddle slightly high, light resistance"; "Pedal through heel/midfoot at first"; "Relaxed, conversational effort". Depict: steady spinning. Avoid: hard toe-pushing, high resistance. Camera: side-on.

**`towel_scrunch`** — *Towel scrunches*
- Equipment: hand towel on a smooth floor. Setup: seated, foot flat on towel, heel grounded. Action: scrunch the towel toward you with the toes, re-spread, repeat. Cues: "Keep the heel grounded"; "Scrunch, then re-spread". Depict: 2×10. Avoid: lifting the heel. Camera: top-down/side on foot + towel.

### Phase 4 — Build strength & balance (out of boot)

**`double_heel_raise`** — *Double-leg heel raises*
- Equipment: wall/counter for balance. Action: push up through the balls of both feet, 3 s up, pause, 3 s down. Cues: "3 s up, pause, 3 s down"; "Share weight 50/50 at first". Depict: 3×12, slow. Avoid: fast bouncing, uneven weight. Camera: side-on + rear for heel height.

**`single_balance`** — *Single-leg balance*
- Equipment: support within reach. Action: balance on the affected leg, soft knee, tall posture; show progression eyes-closed then cushion underfoot. Cues: "Soft knee, eyes ahead"; "Support always within reach"; "Progress: eyes closed, then cushion". Depict: 30 s holds (excerpt). Avoid: locked knee, no support nearby. Camera: front-on full body.

**`band_pf`** — *Resistance-band ankle pushes (plantarflexion)*
- Equipment: resistance band. Setup: long sitting, band looped around the ball of the foot. Action: push the foot down against the band like a slow gas pedal, control the return. Cues (one persistent caution): "Slow gas-pedal push"; "Control the return"; "Band must NEVER pull the foot up past neutral". Depict: 3×15, 1 s hold. Avoid: the band yanking the foot into dorsiflexion. Camera: side-on, foot + band.

**`step_up`** — *Step-ups*
- Equipment: low step + handrail. Action: affected foot up first, drive through the heel, controlled step down; note "raise height before speed". Cues: "Affected foot up first"; "Drive through the heel"; "Use the rail until steady". Depict: 3×10. Avoid: pushing off the trailing toe, rushing. Camera: side-on.

**`squat`** — *Bodyweight squats*
- Equipment: none. Action: feet shoulder-width, sit back and down with heels down, drive up evenly. Cues: "Heels stay down"; "Weight even between sides"; "Depth only as ankle allows". Depict: 3×12. Avoid: heels lifting, weight shifting to the good side. Camera: front-on + side-on.

### Phase 5 — Return to impact & sport (physio-cleared)

**`single_heel_raise`** — *Single-leg heel raises*
- Equipment: wall for fingertip balance. Action: rise on the affected leg alone to **full height**, slow controlled lowering. Cues: "Full height, fingertips for balance only"; "Slow lowering every rep"; "A shaky half-rep doesn't count". Depict: 3×15. Avoid: partial-height or jerky reps. Camera: side-on + rear for height.

**`jog`** — *Walk-jog programme*
- Equipment: flat even ground, cushioned shoes. Action: relaxed easy jogging intervals. Cues: "Flat ground, cushioned shoes"; "Jog/walk intervals, build gradually"; "Only after physio clearance + heel-raise benchmark". Depict: smooth easy jog. Avoid: heavy heel strike, sprinting. Camera: side-on tracking.

**`hop`** — *Hop & plyometric progression*
- Equipment: none. Action: two-leg mini hops on the spot → single-leg → forward/sideways, landing softly. Cues: "Land softly — quiet feet"; "Two legs first, then one"; "Each variation needs physio sign-off". Depict: a few controlled hops. Avoid: stiff/loud landings. Camera: front-on + side-on.

**`agility`** — *Direction-change drills*
- Equipment: a few cones. Action: side-to-side cone shuffles then diagonal cuts, staying low and balanced, ~50% speed. Cues: "Stay low and balanced"; "Start at 50% speed, build over weeks"; "Stop while movements feel crisp". Depict: a short shuffle + cut sequence. Avoid: high-speed sloppy cuts. Camera: front-on wide.

**`padel_drill`** — *Sport-specific drills* (the app substitutes the user's sport name)
- Equipment: generic court/sport props; keep it **sport-neutral** where possible (the app overlays the sport name). Action: staged sport movement patterns — low-intensity patterning → light controlled practice → progressive return. Cues: "Skills before speed"; "Each stage physio-approved"; "Full competition typically 9–12 months, with sign-off". Depict: light controlled movement. Avoid: max-effort match play. Camera: wide, full body.

---

## 3. Acceptance checklist
- [ ] 25 clips, each named exactly by `demoId`, portrait 4:5 + 1:1, silent, looping, 12–25 s, with poster stills.
- [ ] On-screen captions carry all guidance; hard-limit cautions persistent where noted.
- [ ] Boot worn for the phase-1/2 "boot on" clips; bare lower leg elsewhere; affected side mirror-safe.
- [ ] Form is slow and controlled; no fatigue, no end-range stretching; phase-3 ankle work never passes neutral; band clip never dorsiflexes past neutral.
- [ ] Consistent subject/wardrobe/set across the library; calm, clinical, reassuring tone.
