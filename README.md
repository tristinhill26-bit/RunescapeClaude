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
it winds up a slam. It comes with Ba-Ba, Mad Angel's smite, the shared falling debris shadows,
Great Olm, Vorkath, Alchemical Hydra, Zulrah, Cerberus, Verzik and Phantom Muspah, each with its own
toggle. To add another boss, turn on **ID finder > Log nearby IDs**, fight it, and add the IDs it
prints in the chatbox to **Custom attacks**, one per line:

```
# type, id, radius, label, color, ticks
graphic, 2250, 1, Rock
projectile, 1481, 2, Firebomb, #B000FF
anim, 12345, 2, Slam, , 3
```

`type` is `graphic`, `projectile`, `object`, `npc` or `anim`, and `radius` is extra tiles around it
(0 = one tile, 1 = 3x3, 2 = 5x5). Only `type` and `id` are required, and a line with the same type and
id as a built-in replaces it, which is how to fix a built-in's size or colour.
The built-in IDs are from RuneLite's `gameval` lists, but the sizes are estimates, so check them
against the real attacks.
**Falling rock size** sets every falling rock and debris shadow (Ba-Ba, Olm, Zebak and the shared
shadows) to just the tile it lands on, or a 3x3 around it. **Per attack** keeps each one's own size.

**Mad Angel Helper** shows where to go during Mad Angel's two "move here" attacks:

- **Sweep:** when she draws her sword, the tile to click is marked green (**CLICK**), beside her on the
  side away from the sword. Dodging there gives the bonus (next attack 100% accurate, raised minimum hit).
  The tile behind her, also safe but with no bonus, is yellow (**BEHIND**), and the cleave area is shaded
  red. The marks move each time the sword swaps sides. If the dodge tile is a wall, her own edge tile on
  that side is marked instead, which still walks you to the dodge side.
- **Exploding Light:** the tile the ball will land on is marked (**STAND**). Standing on it bounces the
  ball back at her.

Which side counts as "left" and "right" comes from the animation names, worked out from the wiki's
description rather than tested in a fight. If the **CLICK** tile ever shows up on the sword's side, turn
on **Flip left/right**.

**ToA Helper** is for the Tombs of Amascut. It shows up only inside the raid, and follows the wiki's
[Tombs of Amascut/Strategies](https://oldschool.runescape.wiki/w/Tombs_of_Amascut/Strategies) page.

- **Prayers:** the protection prayer to click is highlighted in your prayer book (a thin green frame
  once it's on). A panel shows what to pray and how many ticks until the attack lands, plus what's
  coming after it: Zebak's rocks and orbs, the Wardens' skulls and Divine Projectiles, and the
  baboons' and scarabs' attacks. Between attacks it advises from the standing threat, most urgent
  first:
  1. An Arcane Scarab charging: Magic.
  2. Akkha's form. His damage is decided when he fires, so this is the prayer to be on before he
     attacks, not a countdown.
  3. Ba-Ba: Melee (it's her only attack).
  4. Tumeken's Warden in phase 2 (it closes in), or any phase 2 Warden next to you: Melee.
  5. A Spitting Scarab alive: Missiles.
  6. A Soldier Scarab or Brawler next to you: Melee.
  7. Kephri's fireballs: Magic (blocks about 66%).
  8. The Scabaras puzzle scarabs: Missiles.
  9. Anywhere in Apmeken: Melee, the wiki's easy strategy.
- **Warnings:**
  - Step out of Zebak's melee, which hits through prayer.
  - A Dung Strike is aimed at you.
  - Ba-Ba is nearly at 66% or 33%, where she knocks everyone to the bottom (deadly with Mind the Gap!).
  - Akkha's special countdown (every 7 regular attacks, 6 with **Stay Vigilant on**), and what to do
    in each special: stand still for Trailing Orbs, follow the markers for Memory Blast, spread out
    for Detonate.
  - How long the Warden's core stays out (21, 29 or 37 ticks, depending on the Warden's health).
- **Damage:** for whatever you're attacking, it shows:
  - what it's praying (from its overhead icon, or Akkha's form, or always Melee for the P2 Wardens),
  - which style gets through, and the style it's weak to: Kephri stab, each baboon its counter style,
    the obelisk ranged, the core and siphons melee, Akkha's final stand melee only,
  - its points multiplier and a wiki tip.

  On screen it marks Ba-Ba's cracked boulder, Het's open seal, the Arcane Scarab, Kephri's eggs (you
  always max-hit them), the Warden's core (5x damage) and the Energy Siphons (melee only). Apmeken's
  baboons are numbered in the wiki's kill order: mages and shamans, then throwers, cursed, and
  brawlers and thralls last.
- **Where to stand:**
  - The tiles behind Zebak's rocks that block his roar, and the 5x5 a jug's water covers when it
    breaks (3x3 with **Upset Stomach on**), which clears acid pools.
  - After Ba-Ba's rockfall, the tiles next to debris, sarcophagi and her baboons. Her thrown boulder
    can't hit you there; it lands about 6 ticks later.
  - The nearest safe tile on your row during the Wardens' phase 3 floor slams. If it ever shows up
    on the raised side, turn on **Flip slam sides**.
  - The Wardens' Lightning Skulls: the 7x7 they hit, and the safe tiles two out in each straight
    direction. These need their graphic ID in **Lightning Skull IDs**; get it with Shadow Tiles'
    ID finder.
- **Points:** your estimated room and raid points, using the wiki's per-NPC multipliers and room caps,
  what a death would cost, and your purple chance at the current raid level. The MVP bonus can't be
  seen from the client, so it isn't counted, and in a team the purple chance is based on your own
  points only.

Shadow Tiles' **Tombs of Amascut** toggle marks the tiles to step off: Wardens lightning and
entomb, Zebak's rockfall, blood clouds and waves, Akkha's unstable orbs, Het's Orbs of Darkness, and
the 3x3 explosions of Volatile Baboons and Kephri's eggs. Ba-Ba's rockfall is one tile, since the
wiki says only the player directly under a shadow is hurt.

The overlays (beam, outline, dots, arrows) work on their own. The lighting needs 117 HD Enhanced running with
*Dynamic lights* enabled.

## Development

Run `rs117.hd.HdPluginTest` (or `./gradlew run`) to start RuneLite with all of the plugins loaded.
On Windows, `Launch-RuneLite.bat` updates to the latest version (`git pull`) and then starts it. To get a
desktop shortcut for it, run this once from the repo folder:

```powershell
powershell -ExecutionPolicy Bypass -File .\Create-Desktop-Shortcut.ps1
```
The interaction lighting lives in [`src/main/java/rs117/hd/interaction`](src/main/java/rs117/hd/interaction),
Shadow Tiles in [`src/main/java/com/shadowtiles`](src/main/java/com/shadowtiles), and
Mad Angel Helper in [`src/main/java/com/madangel`](src/main/java/com/madangel), and
ToA Helper in [`src/main/java/com/toahelper`](src/main/java/com/toahelper).

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
