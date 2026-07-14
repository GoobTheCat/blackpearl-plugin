# Black Pearl - Plugin + Resource Pack (Minecraft 26.1.2)

Converted from the original `pirate_sword` datapack into a standalone Paper
plugin, paired with a resource pack built from your Blockbench model.

## IMPORTANT - read this first

This sandbox has no Java compiler at all (only a JRE) and no internet access,
so this code has not been compiled or run here.

## What's in here

- `blackpearl-plugin/` - Maven project, `mvn clean package`, drop the jar in
  `plugins/`. Needs internet (pulls `paper-api` from repo.papermc.io) and
  Java 25 to run the server; the plugin itself compiles at bytecode level 21.
- `blackpearl-resourcepack/` - zip the folder's contents and apply as your
  server's resource pack. `pack_format` 84, matching 26.1.2. Updated this
  round (item frame size fix) - re-download and reapply this one.

## Commands

`/blackpearl [player]` (permission `blackpearl.give`, default op). Or craft
it: Heart of the Sea, 2x Diamond, Gold Block, Iron Sword.

## About that recurring IllegalArgumentException

I still can't reproduce it directly (no compiler/runtime in this sandbox) or
confirm the exact line from a collapsed log entry, but the task-number
pattern in your screenshot (307, 314, 328 - different IDs each time, not the
same task erroring repeatedly) tells us something useful: it's coming from a
task that gets freshly *scheduled* each time you do something (sneaking for
Vigilance, or right-clicking for Execution), not from the one steady
background task. That's exactly the kind of failure that's easy to make
harmless even without knowing the root cause: I wrapped the bodies of both
Vigilance's channel tick and each Execution strike (plus the ambient task and
Endurance's knockback-resistance attribute handling, which was my top
suspect - see below) in try/catch blocks that log a clear one-line message
instead of a bare stack trace.

**If it happens again**, check the console for a line starting with
`[BlackPearl]` and containing "Vigilance tick failed", "Execution strike ...
failed", or "Ambient tick failed" - that'll say exactly what broke and why,
and I can fix it for real on the next pass instead of guessing.

## This round's changes

**Base material switched to diamond sword** (from netherite) - same idea as
last round (a completely plain, unmodified sword, no custom attribute
modifiers), just a tier down since you don't need the fire/lava immunity for
an early-game-friendly weapon.

**Recipe changed accordingly:** Heart of the Sea, 2x Diamond (was 2x
Netherite Ingot), Gold Block, Iron Sword (was Diamond Sword) - noticeably
more obtainable early on, matching a diamond-tier weapon instead of a
netherite-tier one.

**Item frame display fixed.** Last round I shrank both the dropped-item and
item-frame scale together to fix the oversized ground look, but the item
frame version was fine before that - it's now reverted to its original scale
`[0.9, 1, 0.8]`. The dropped-item fix stays as-is (`[0.405, 0.45, 0.36]`),
since that one was actually needed.

## The four abilities, current behavior

### Execution (8s cooldown)
Hit up to 3 things to mark them (each locked up to 12 blocks away, fading
warning particle 7-12 blocks). Right-click with any target marked: 6
strikes hit all of them at once - first 5 land every 0.5s for 1 heart each
as normal damage, then a longer pause before a finishing 6th strike for 2
hearts of true damage (bypasses armor) per target, +1 heart vs
drowned/guardians/elder guardians.

### Endurance
Sword in main hand + in water or rain: Resistance II, Strength I, knockback
resistance. Raining also adds Speed III; in water also adds Dolphin's Grace
III.

### Vigilance (originally "Seer")
Sneak while holding the sword - starts instantly. ~0.3s: sensing cue. ~0.7s:
20-block reveal (Glowing, shows through walls). Up to ~2.3s: pushes out to
40 blocks. No debuff. Lingers 5s after you release sneak.

### Hydra's Will
Every 5th successful hit heals 2 hearts (Instant Health I).

## Removed

Sentinel (parrot glow), the gold-nugget easter egg, ground-snap-on-drop and
its bug, the Lucentbind hook, and Escape Plan.

## Tuning cheat-sheet

| What | Where | Value |
|---|---|---|
| Execution cooldown | `ExecutionAbility.CHARGE_MS` | 8000ms |
| Max simultaneous targets | `TargetManager.MAX_TARGETS` | 3 |
| Target lock range | `TargetManager.MAX_RANGE` / `WARN_RANGE` | 12 / 7 blocks |
| Knockback resistance bonus | `EnduranceAbility.KNOCKBACK_RESISTANCE_BONUS` | 0.6 |
| Vigilance radius steps | `SeerAbility.STEP_RADIUS` / `STEP_TICKS` | 10 to 40 over ~2.3s |
| Dropped item scale | resourcepack `display.ground.scale` | [0.405, 0.45, 0.36] |
| Item frame scale | resourcepack `display.fixed.scale` | [0.9, 1, 0.8] (reverted) |
