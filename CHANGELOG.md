# 0.0.7

## # YOU CAN CONFIG ENTITIES' BEHAVIOURS WITH EITHER THE COMMAND /abnorm_config OR WITH A CONFIG MOD!

### new entity: it "the condition"
- flat 2D black silhouette that spawns at night
- stare at it to win (stand within 5.5 blocks, unbroken eye contact for 4 seconds)
- it freezes when you look at it, advances when you don't
- steals inventory items when it passes through you (1 stack, 2 if you're close and looking away)
- 3 steals = punish (KICK/CRASH/NONE configurable)
- 12fps choppy walking animation (quantized limb swing)
- billboard-style rendering facing camera, 6-part flat model matching Blockbench export
- spawns at high reputation only (rep >= 1200)
- unkillable (only bypasses_invulnerability/projectile/explosion damage)
- win gives +40 rep and broadcasts `<it> you chose to see me.` to all players

### new entity: him "the nightmare you should not have found"
- player-like survivor that bridges and towers at you exactly like a player
- pain_collapse death animation with dialogue lines
- boss variant every 10 kills (60hp, bow attacks, fog, boss bar)
- boss does a random "boss call" on spawn (nurs / it / 3 extra hims / the tally event)
- spawns at low reputation only (rep <= 999)
- config: spawn weight, rep max, punish mode

### new system: v1s1t "the in-law"
- home invasion gift giver, never renders
- stages 0-3, advances every 3 visits
- stage 0: flowers in chests/near bed
- stage 1: food gifts (bread, cookies, berries)
- stage 2: rotate stairs, move torches, better food
- stage 3: break torches near bed, low rep = silent nur spawn
- config: enabled, check interval, visit days, stage up

### new system: hush "the hush"
- all loaded mobs within range freeze and stare at you
- new mobs entering range during active event also freeze
- rotation set directly (bypasses noAI)
- config: enabled, weight, duration, range, cooldown

### new system: w4k3 "wake displacement"
- wake up 60-70 blocks from bed with no notification
- 1 in 3 chance per sleep
- one-tick deferred teleport
- config: enabled, distance, chance

### new system: m1n3r "the miner"
- underground tunnel digger, spawns when player at Y<=0 in dark
- carves 1x2 tunnel toward player with dig sounds
- torches every 4 blocks, stops on bedrock/chests
- 5% dummy nur at tunnel end
- config: enabled, weight, max tunnel length, dummy nur chance

### new system: l3ns "the lens"
- screenshot figure appears in F2 screenshots
- draws dark humanoid silhouette + white eyes directly into NativeImage
- night or dark only, 1 in 8 chance
- config: enabled, chance, cooldown

### new system: 1ull "the lure"
- music box that draws you in (80-block audible range)
- spawns 40-80 blocks away at night
- approach within 12 blocks = relocate twice, 3rd time = hunt then punish
- config: enabled, weight, cooldown, punish mode

### new system: m1sl4y "misplace"
- inventory gaslighting, swaps slots or shrinks stacks
- every 200 ticks, 1 in 600 chance
- never during vr9p
- config: enabled, weight

### new system: 0th3r "other"
- fake player in tab list with yellow join/leave message
- name one character off from real player
- persists 100-240 ticks
- config: enabled, weight, cooldown

### new system: fake achievements
- fake black achievements (toast only, never in real menu)
- 15 spooky names, pure black concrete icon
- config: enabled, cooldown

### new system: chat disabled
- curse locks chat with fake Forge error
- rep <= 900 only, 300-600 ticks random
- config: pool cooldown

### new system: sister "the warning spirit"
- tab list entry with void skin face icon
- joins during days 0-6, stays for configured days
- warns before kicks

### new command: /abnorm_config
- lists all config values (recursive flatten)
- `/abnorm_config <key>` shows one value
- `/abnorm_config <key> <value>` sets live + saves to disk

### nur improvements
- distance speed scaling: 1.8 * min(maxMult, 1 + dist/ramp)
- at 10 blocks = 2x, 50 blocks = 6x, 90+ = 10x
- block-breaking sweep scales with speed too
- NBT save/load for chase state (persists across restarts)
- safe teleport using heightmap

### k3w improvements
- crash sequence uses tick counter (sounds + kick deferred properly)
- door opening (scans 3x3x3, opens doors and trapdoors)
- flying activates when target is >2 blocks above
- crash guard prevents re-trigger on subsequent hits

### skinwalker improvements
- approach speed is now a multiplier (default 1.0, was 0.25 = too slow)
- transform rep drain moved after nur null check

### whisper improvements
- all whisper messages now YELLOW+ITALIC (were DARK_GRAY)

### vr9p improvements
- stargazed variant: rare, fast (10 tick switches), strict (instant punish on look/swing/slot)
- normal semantics: STOP = must not move, CONTINUE = free
- nur sound plays on punish, kick deferred 10 ticks so it's audible
- all end paths call stopAmbience
- cooldown off-by-one fixed

### config changes
- `/abnorm_config` command for live editing
- vr9p and vr9pStargazed onPunish both default to NONE
- all new systems have their own config sections
- hush, w4k3, m1n3r, l3ns, 1ull, m1sl4y, 0th3r, fakeAch all configurable

### bug fixes
- it renderer UV mapping fixed (6-part Blockbench model, correct UV regions)
- it upside-down rendering fixed (Blockbench Y-down to Minecraft Y-up conversion)
- it dual spawn fixed (bossCall + natural spawn deduplication)
- it dialogue color changed from GRAY to white
- 1ull bell sound volume fixed (was 0.8 = 13 blocks, now 5.0 = 80 blocks)
- k3w crash sequence timing fixed (was all same tick, now properly deferred)
- skinwalker approach speed was 25% of animal speed (now 100% default)
- rep drain on failed skinwalker transform fixed
- whisper color updated to yellow italic
- old mod jar versions cleaned from modpack instances
- vr9p ambience stops on all end paths
- connection null checks in all deferred tasks
- rep saves throttled to once per 5 seconds

## known caveats
- CRASH mode uses `mc.stop()`. game closes after saving. keep at KICK unless you want hardcore.
- singleplayer/LAN/e4mc/essential and 2-4 player focus, didn't test for 20+ players
- grace period gates natural spawns only (admin commands bypass it)
- horror events also gated by grace period
- "it" texture may need touch-up (was designed for billboard, now mapped to 6-part model)
- stargazed vr9p is intentionally brutal (rare variant, instant punish)
