# Shadow Tiles

A RuneLite plugin that highlights the tiles a boss attack is about to hit: the ground
shadows shown before rocks and debris land, projectile landing areas, acid pools and
poison clouds, and the area around a boss while it plays a slam animation.

It comes with OSRS bosses built in. Your server's custom bosses are added from the config
panel without recompiling.

## What it watches

| Type         | What it is                                  | Area highlighted                           |
|--------------|---------------------------------------------|--------------------------------------------|
| `graphic`    | Graphics object / spot anim on the ground   | Around the graphic until it finishes       |
| `projectile` | Projectile in flight                        | Around its landing tile until it lands (with a tick countdown) |
| `object`     | Game or ground object (pools, clouds)       | Around the object while it exists          |
| `npc`        | An NPC that is itself dangerous (boulders)  | Its footprint plus radius                  |
| `anim`       | An NPC animation (slams, stomps)            | Around the NPC for `ticks` game ticks      |

`radius` is how many extra tiles around the source: `0` = one tile, `1` = 3x3, `2` = 5x5.

## Built-in attacks

Each group can be toggled under **Built-in attacks**.

- **Falling debris shadows:** the shared `GARGBOSS_DEBRIS_SHADOW_*` graphics used by Grotesque
  Guardians, Olm, the DT2 bosses and others
- **Ba-Ba:** falling rocks (graphics 2250–2252) and rolling boulders (NPCs 11782, 11783)
- **Great Olm:** falling crystals, crystal bombs, acid pools
- **Vorkath:** firebomb and acid-phase projectiles
- **Alchemical Hydra:** poison splats
- **Zulrah:** toxic clouds
- **Cerberus:** lava pools
- **Verzik:** P2 ranged bombs
- **Phantom Muspah:** spike warnings

The IDs come from the OSRS cache and match the names in RuneLite's `net.runelite.api.gameval`.
The radii are best guesses at each mechanic's hit area. If your server's cache or mechanics
differ, override the entry from the custom box (see below).

## Adding your own bosses (Mad Angel etc.)

1. Open the plugin config. Under **ID finder**, turn on **Log nearby IDs**.
2. Fight the boss. Each new graphic, projectile, object, NPC and NPC animation near you is
   printed to the chatbox once, e.g. `[Shadow Tiles] anim 9123 - Mad Angel (npc 15001)`.
3. Add a line for each attack to **Custom attacks**:

```
# type, id, radius, label, color, ticks
graphic, 3301, 1, Mad Angel, #B000FF
projectile, 3302, 2, Holy bomb
anim, 9123, 2, Slam, #B000FF, 4
npc, 15002, 0, Orb
```

Only `type` and `id` are required. A custom line with the same type and id as a built-in
replaces the built-in, which is also how you change a built-in's radius or colour. Lines
that don't parse are reported in the chatbox.

## Building and running

The build uses the same setup as RuneLite's
[example-plugin](https://github.com/runelite/example-plugin) template.

```sh
cd shadow-tiles
./gradlew build   # compile and run the unit tests
./gradlew run     # launch RuneLite in developer mode with the plugin loaded
```

### Putting it in your RSPS client

**Bundled in a RuneLite fork (most RSPS clients):** copy
`src/main/java/net/runelite/client/plugins/shadowtiles/` into your client's
`runelite-client/src/main/java/net/runelite/client/plugins/`. The package already matches
the built-in plugin layout, so it is picked up like any other core plugin.

**As an external plugin:** build the jar with `./gradlew jar` and load it however your
client sideloads plugins.

The code targets the current RuneLite API. Older forks may be missing the
`LocalPoint(x, y, worldView)` constructor and `LocalPoint.getWorldView()`. On those, switch to
`new LocalPoint(x, y)` and drop the world-view part of the tile key in `ShadowTilesOverlay`.
