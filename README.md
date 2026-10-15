<p align="center"><img width=60% src="https://user-images.githubusercontent.com/831317/223843536-9aa497c7-37ed-4d5f-98fd-1ba8aaa99d40.png"></p>

<p align="center">
  <img src="https://img.shields.io/discord/886733267284398130.svg?label=Discord&logo=Discord&colorB=7289da&style=for-the-badge">
</p>


# 117 HD Enhanced

A fork of [117 HD](https://github.com/117HD/RLHD) which adds **interaction lighting**: the things you click, hover and
search for are lit up by real dynamic lights in the 117 HD renderer, so they stand out, especially at night.

It ships two plugins:

- **117 HD Enhanced** is 117 HD with a small hook in its light manager (`ExternalLightProvider`), so other code can add
  lights each frame, plus scene light strength and radius multipliers. It uses its own config group (`hdenhanced`)
  and conflicts with the original 117 HD, so only one of them runs at a time.
- **117 HD Interaction Lights** is the settings panel with the new features:
  - **Click marker**: a light beam, glowing tile outline and lit ground on whatever you last clicked, with dots
    tracing the path your character will walk, plus a flash when you click.
  - **Mouse-over**: outlines and softly lights the tile, object, NPC or item under your cursor.
  - **Chevron**: an optional bouncing arrow above the marker.
  - **Long range**: a taller beam and an arrow at the edge of the screen for far away or off-screen markers,
    and a beam (plus light, if you like) on the game's own hint arrow target.
  - **Behaviour**: clear walk markers on arrival, forget markers after a delay, or keep them after the target is gone.
  - **Scan pulse**: press a hotkey to send out a ring of light. As it passes, it lights up agility shortcuts
    (red if you're under the level), ladders, stairs and exits, valuable, highlighted or your own ground items,
    Object Markers, and other resources like the one you last interacted with (e.g. every Evergreen tree).
  - **Shortcut glint**: usable agility shortcuts nearby give off a faint twinkling light.
  - **Scene lights**: make torches, fires and other 117 HD lights brighter or reach further.
  - **High visibility**: a key that switches the whole game to easier to see settings (higher contrast and
    brightness, no fog, no shadows, an optional colour filter) and back to your own settings on the second press.

It also includes **Fairy Ring Labels**, which adds the destination next to fairy ring codes in right-click menus,
such as the Favorites list and Last-destination, e.g. `CIR - Farming Guild`. Names can be short or RuneLite's full
descriptions, and you can set your own names per code.

**Wiki Gear Loadouts** adds a side panel where you search for a monster or activity, such as Barrows, a raid or a
minigame (with its wiki picture), tick the combat
styles you want, and it creates a bank tag per style, e.g. `gear vorkath ranged`. Each tag holds the best gear you own
for every slot, as ranked by the wiki's Recommended equipment tables, laid out like the equipment screen, with the
page's recommended inventory in a 4 by 7 grid beside it. Inventory items you don't own are swapped for the closest
thing you do: your best food (by heal amount, from RuneLite's Item Stats data), or a potion that does the same job.
Gear that only works as a full set, like Void, is only used when you own every piece. Blighted items are left out unless the
monster or activity is in the Wilderness, since they can't be used anywhere else. Slots the wiki leaves empty
are filled with your best item for the setup's combat style, judged by its stats.
Open your bank once per session first, so the plugin can see what you own.
New tags are added to your bank's tag tabs automatically (they appear the next time you open the bank), so this
works even with plugins that hide the tag tab + button. Each tag also gets a **Copy** button for Bank Tags'
*Import tag tab*, or for sharing.

**Shadow Tiles** highlights the tiles an attack is about to hit, so telegraphs are easy to see:
the ground shadows before rocks and debris land, where projectiles will land (with a tick countdown),
acid pools and poison clouds, dangerous NPCs like rolling boulders, and the area around a boss while
it winds up a sweep or slam. It comes with Ba-Ba, Mad Angel, the shared falling debris shadows,
Great Olm, Vorkath, Alchemical Hydra, Zulrah, Cerberus, Verzik and Phantom Muspah, each with its own
toggle. To add another boss, turn on **ID finder > Log nearby IDs**, fight it, and add the IDs it
prints in the chatbox to **Custom attacks**, one per line:

```
# type, id, radius, label, color, ticks
graphic, 2250, 1, Rock
projectile, 4015, 2, Bomb, #B000FF
anim, 14429, 1, Sweep, , 3
```

`type` is `graphic`, `projectile`, `object`, `npc` or `anim`, and `radius` is extra tiles around it
(0 = one tile, 1 = 3x3, 2 = 5x5). Only `type` and `id` are required, and a line with the same type and
id as a built-in replaces it, which is how to fix a built-in's size or colour.
The built-in IDs are from RuneLite's `gameval` lists, but the sizes are estimates, so check them
against the real attacks.

The overlays (beam, outline, dots, arrows) work on their own. The lighting needs 117 HD Enhanced running with
*Dynamic lights* enabled.

## Development

Run `rs117.hd.HdPluginTest` (or `./gradlew run`) to start RuneLite with all of the plugins loaded.
The interaction lighting lives in [`src/main/java/rs117/hd/interaction`](src/main/java/rs117/hd/interaction),
Shadow Tiles in [`src/main/java/com/shadowtiles`](src/main/java/com/shadowtiles).

To pull in upstream 117 HD changes:

```
git remote add upstream https://github.com/117HD/RLHD.git
git fetch upstream
git merge upstream/master
```

---

*The rest of this README is from upstream 117 HD.*

# 117 HD

GPU renderer with a suite of graphical enhancements.

# How to install it

- Download RuneLite from their website: https://runelite.net
- Launch RuneLite
- Click the Wrench icon on the top right of the RuneLite window
- Click the Plugin-Hub button on the right side near the top
- Search for "117 HD"
- Click Install

![Installation intruction gif](https://user-images.githubusercontent.com/72366279/223843005-6175c825-7f35-4bcb-84eb-882321a017f7.gif)

# Links
![discord](https://user-images.githubusercontent.com/5789682/173276137-8ea82e88-4ec1-444f-baf0-4b0dc171901f.png)
[Discord](https://discord.gg/U4p6ChjgSE) - Our Discord server.

🔌 [117HD](https://github.com/117HD/RLHD) - The plugin's source code.

🤖 [Discord-Bot](https://github.com/117HD/discord-bot) - Our Discord bot's source code.

# Links to the original author
![twitter](https://user-images.githubusercontent.com/5789682/173276125-347af1a5-e866-4770-97b2-7ebd3ab1aaa6.png)
[117's Twitter](https://twitter.com/117scape) - The original author's Twitter handle.

![patreon](https://user-images.githubusercontent.com/5789682/173276176-22dd9dcc-40c8-472b-9da2-455eebec296a.png)
[117's Patreon](https://www.patreon.com/RS_117) - If you wish to support the original author of the plugin.
