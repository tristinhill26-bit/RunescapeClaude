# ItemDrawers × Quick Stack: quick stacking into the drawers

Patch for **ItemDrawers-Ymiron 0.5.0** so that the quick stack and Store all of **Quick Stack - Store - Sort -
Trash - Restock 1.4.16** (goldenrevolver) also put items into the drawers, not only into the chests.

## Why it did not work

The two mods store items in completely different ways:

- **Quick Stack** only knows vanilla `Container` objects (chests, carts, ships...). It keeps a list of every
  `Container` that spawns (`ContainerFinder.AllContainers`, filled by a patch on `Container.Awake`). "Quick stack
  to nearby" goes through that list and calls `container.m_inventory.AddItem(item)` on the ones in range that
  already hold the item.
- **A drawer is not a `Container`.** It is its own `Drawer` component with no `Inventory`: each compartment keeps an
  item name and a count in the drawer's network data (`ymiron_drawer_item0`, `ymiron_drawer_count0`...). Changes
  go through the drawer's own RPC (`ymiron_drawer_change`) so two players can't duplicate items.

So Quick Stack never finds the drawers, and even if it did, it could not put anything into them with
`Inventory.AddItem`. **Sort** and **Take all** can't apply to drawers: they work on the chest you have open, and a
drawer has no window to open.

## What changed in the drawer mod

Two changes, both in the ItemDrawers project:

1. **New file [`src/QuickStackStore_Compat.cs`](src/QuickStackStore_Compat.cs).** Harmony patches on Quick Stack's
   own methods, found by name, so the drawer mod still has no hard dependency on it:
   - `QuickStackModule.DoQuickStack`: on **every** quick stack (to the open chest or to the nearby ones, button or
     hotkey), the items it would quick stack also go into the drawers within `Range` that are already set to that
     item. It uses the mod's existing `DrawerNetwork.Deposit(..., useFree: false, Delivery.Return)`, the same call
     as the Ledger's *Store all* and the ground pickup. So it uses the same network-safe path, never starts a new
     compartment, and sends back whatever doesn't fit.
   - `QuickStackModule.ReportQuickStackResult`: holds back Quick Stack's "Quick stacked N items" message until the
     drawers have had their turn, then shows one message with the chests and drawers added together.
   - `StoreTakeAllModule.StoreAllItemsInOrder`: before *Store all* fills the open chest, the drawers take the items
     they already hold. The message says how many stacks went into the drawers.

   It uses Quick Stack's own `ShouldQuickStackItem` / `ShouldStoreItem`, so **favorited items and slots stay put**,
   and the hotbar follows Quick Stack's settings. Drawers inside a ward you can't open are skipped, the same as for
   crafting.

   The drawers deliberately do **not** follow Quick Stack's rules for nearby chests. Quick Stack turns nearby
   stacking off in multiplayer (`AllowAreaStackingInMultiplayerWithoutMUC = false`, which also counts a world
   started with "Start server") and, by default, while a chest is open. Those rules protect chests; every change to
   a drawer is already a request to its owner. The first version of this patch followed them, which is why it did
   nothing in those cases.

2. **One attribute on `ItemDrawersPlugin`** (`ItemDrawersPlugin.cs`), so BepInEx loads Quick Stack first when it is
   installed. Otherwise its types might not be loaded yet when the patches are looked up:

   ```csharp
   [BepInDependency("goldenrevolver.quick_stack_store", BepInDependency.DependencyFlags.SoftDependency)]
   ```

It is a soft dependency: without Quick Stack, nothing is patched and the drawers work as before. If a future
Quick Stack version renames these methods, the log says so once and quick stacking just leaves the drawers alone.

## Drawers that sat a quarter metre off (skewed walls)

The original drawer had snap points on its four front corners and its four back corners. Because the drawer is
0.5 m deep, a new drawer aimed at an existing one found front-to-front, front-to-back and back-to-back pairs at the
same distance, so it often snapped half a depth in front of or behind its neighbour. A row that sticks out like
that shows its top or underside and makes the wall look skewed. [`src/SnapPoints_Fix.cs`](src/SnapPoints_Fix.cs)
removes the front snap points when the drawer pieces are built, so drawers can only snap back to back and always
sit flush. Drawers that are already placed keep their position; remove and re-place the ones that stick out.

## New config (`ymiron.valheim.itemdrawers.cfg`, section `8 - Quick Stack`)

This section only appears when Quick Stack is installed.

| Key | Default | Meaning |
|---|---|---|
| `Enabled` | `true` | Quick stacking also fills the drawers. |
| `StoreAll` | `true` | *Store all* (with a chest open) first puts into the drawers what they already hold; the rest goes into the chest. |
| `DrawersFirst` | `true` | Quick stack: `true` = drawers fill up before the chests; `false` = chests that already hold an item get it first, and the drawers take the rest. |
| `Range` | `20` | Drawers within this many meters of you are filled (2 to 100). |

## Checking that it works

`BepInEx/LogOutput.log` says at startup either `Quick Stack - Store - Sort - Trash - Restock found: its quick
stack and Store all also fill the drawers.` or why not. Each quick stack or Store all then writes a line such as
`Quick stack: 3 drawers within 20 m, 5 kinds of item to put away, 240 items went into the drawers.`

## Install

> **Warning:** never load a world while the drawer mod is missing or failed to load. Valheim permanently
> deletes every drawer (and everything inside it) in the area you load, and the ledger with them
> (`Destroyed invalid prefab ZDO` in the log). Back up your `worlds_local` folder first, and check the log for
> the `ItemDrawers-Ymiron 0.5.0 loaded.` line before you load a world.

Install it wherever your game actually loads mods from. The first lines of `BepInEx/LogOutput.log` show which
BepInEx folder that is:

- **BepInEx in the game folder** (e.g. `C:\Program Files (x86)\Steam\steamapps\common\Valheim\BepInEx`):
  replace `BepInEx\plugins\ItemDrawers.dll` with [`dist/ItemDrawers.dll`](dist/ItemDrawers.dll). Overwrite
  the file; don't delete the old one first and launch.
- **r2modman / Thunderstore Mod Manager** (the game is started from the manager): *Settings > Import local mod*
  with [`dist/ItemDrawers_Ymiron-0.5.0-quickstack.zip`](dist/ItemDrawers_Ymiron-0.5.0-quickstack.zip), in the
  profile you play with, then check that it is listed and enabled before you remove the old one.

Quick stacking runs on your own game, so each player who wants it needs the patched dll. The server doesn't
need it. The plugin version stays **0.5.0** on purpose: players with the unpatched 0.5.0 get no version warning,
and nothing about the drawers' network data changed.

## How the dll was built

There is no game install here, so `tools/build.sh` compiles the files in `src/` against small stand-ins
for `assembly_valheim` and `BepInEx` (`tools/stubs`) and against the original `ItemDrawers.dll`. Then it copies
the new classes and the attribute into the original dll with Mono.Cecil (`tools/patcher`). Nothing else in the
dll changes: decompiling the result gives the original code, the five new classes and the extra
`BepInDependency`.

The stand-ins prove nothing on their own, so the last step checks that **every game, BepInEx and Harmony member
the new code calls is one the original ItemDrawers.dll or Quick Stack Store already calls**. Those two dlls are
known to work in the game. Result: 777 references, 0 problems, no new assembly references. It also checks that every non-virtual call into the game or BepInEx is one those dlls make too: a stand-in that got a member's virtual-ness wrong once produced a call the game rejected ("Bad IL format").

If you have the ItemDrawers source, the simpler route is to add the files in `src/` to the project,
add the attribute above and build it normally.

**Not yet tested in game.** The first version did nothing in game (see above); if this one does not work either, the log lines above show where it stops.
