# Changelog

## 0.0.7

### new events

- v1s1t: home invasion, leaves gifts in your chests, stages up with every visit
- hush: every mob freezes and stares at you, configurable chance and range
- w4k3: sleep displacement, you wake up somewhere else
- m1n3r: a tunnel gets carved toward you underground, sometimes leaving a dummy nur waiting at the end
- 1ull: a music box lures you toward it
- m1sl4y: your inventory gets shuffled
- l3ns: a figure appears in your screenshot
- wr0ng: your crafted item starts talking
- s1gn: signs appear written about you
- st1ll: the world freezes and you must not move
- br34th: phantom drowning on dry land
- h01d: staying still attracts attention
- c1rcl: a circle of torches appears around your bed
- f4ded: faint grey figure flickers in the world
- tOXIC: time rewinds you, everything unhappens
- g0n3: the light thief, snuffs out all torches and campfires near you and steals light items from your inventory
- fakeAch: fake black achievement toasts pop up with 15 different spooky names

### sister rework

- 0th3r removed entirely, replaced by the Sister, a warning spirit that talks to you before events
- sister warns about every event, vr9p gets a 10s / 5s heads-up
- 3x dialogue pools, xYz warnings, cocky kick taunts
- sister warnings all get a 2s reaction delay except the vr9p heads-up
- kicked players receive their taunt on rejoin instead of before the disconnect
- sister now says "joined the game" and "left the game", and its tab icon shows a pure black head
- sister whispers in yellow italic now

### vr9p

- vr9p spawn check now runs every 20 ticks like other spawners instead of every tick
- stargazed variant, always shows the vr9p-hit face, no stop/continue faces
- stargazed gradient fix, grace ticks 5 -> 15, strict STOP checks for all modes
- CONTINUE must-move applies to every mode now, not just stargazed
- stargazed punishment defaults to NONE

### k3w

- k3w is now a true copy: takes real damage, mirrors your health with a 2 second hold
- toxic rewind can never softlock anymore
- k3w crash sequence fixes

### xYz

- xYz no longer requests silk-touch or unobtainable blocks
- the demand broadcasts to all players, nurs chase whoever hit her
- entities can no longer be boated, pushed around, or pushed by fluids

### nur

- nur chase speed scales with distance, new speedRamp and maxSpeedMult config
- looking at a claimed nur no longer hijacks it, no more double kills
- nur keeps its sound and is visible after relog, restores chase state properly
- nur texture stays black and white after relog

### chat lock

- chat can get locked with a fake forge error, lower reputation players get it naturally
- draws from a pool of 34 fake forge errors
- the sister comments on the fake errors, 30 new lines

### misc

- /abnorm_callevent and /abnorm_config commands
- /abnorm_config lists all 111 config values recursively and can set them
- fixed ConcurrentModificationException crashes on player disconnect
- fixed crash caused by duplicate entries in the xYz silk-touch block set

### config changes

- VR9P onPunish default changed from KICK to NONE
- VR9P stargazed onPunish default is NONE
- nur speedRamp and maxSpeedMult added for chase scaling
- many new event toggles, spawn weights, intervals, and radii for all the new events
- w4k3 chance configurable, default 1/3
- fakeAch cooldown configurable