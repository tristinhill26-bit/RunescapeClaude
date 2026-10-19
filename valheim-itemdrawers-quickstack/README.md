# ItemDrawers × Quick Stack: quick stacking into the drawers

Patch for **ItemDrawers-Ymiron 0.5.0** so that the quick stack of **Quick Stack - Store - Sort - Trash - Restock
1.4.16** (goldenrevolver) also puts items into the drawers, not only into the chests.

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
`Inventory.AddItem`. The other buttons can't apply to drawers either: **Store all**, **Sort** and **Take all**
only work on the chest you have open, and a drawer has no window to open.

## What changed in the drawer mod

Two changes, both in the ItemDrawers project:

1. **New file [`src/QuickStackStore_Compat.cs`](src/QuickStackStore_Compat.cs).** It has two Harmony patches on
   Quick Stack's own methods, found by name, so the drawer mod still has no hard dependency on it:
   - `QuickStackModule.DoQuickStack`: when Quick Stack does an **area** quick stack (the same check it uses itself,
     `ShouldAreaQuickStack`), every item it would quick stack goes into the drawers within its
     `QuickStackToNearbyRange` that are already set to that item. It uses the mod's existing
     `DrawerNetwork.Deposit(..., useFree: false, Delivery.Return)`, the same call as the Ledger's *Store all* and the
     ground pickup. So it uses the same network-safe path, never starts a new compartment, and sends back whatever
     doesn't fit.
   - `QuickStackModule.ReportQuickStackResult`: holds back Quick Stack's "Quick stacked N items" message until the
     drawers have had their turn, then shows one message with the chests and drawers added together.

   It uses Quick Stack's own `ShouldQuickStackItem`, so **favorited items and slots stay put**, and the hotbar
   follows Quick Stack's `QuickStackIncludesHotkeyBar` / `OverrideHotkeyBarBehavior` settings. Drawers inside a ward
   you can't open are skipped, the same as for crafting.

2. **One attribute on `ItemDrawersPlugin`** (`ItemDrawersPlugin.cs`), so BepInEx loads Quick Stack first when it is
   installed. Otherwise its types might not be loaded yet when the patches are looked up:

   ```csharp
   [BepInDependency("goldenrevolver.quick_stack_store", BepInDependency.DependencyFlags.SoftDependency)]
   ```

It is a soft dependency: without Quick Stack, nothing is patched and the drawers work as before. If a future
Quick Stack version renames these methods, the log says so once and quick stacking just leaves the drawers alone.

## New config (`ymiron.valheim.itemdrawers.cfg`, section `8 - Quick Stack`)

This section only appears when Quick Stack is installed.

| Key | Default | Meaning |
|---|---|---|
| `Enabled` | `true` | Quick stacking to nearby containers also fills the drawers. |
| `DrawersFirst` | `true` | `true`: drawers fill up before the chests. `false`: chests that already hold an item get it first, and the drawers take the rest. |

The range is Quick Stack's own `QuickStackToNearbyRange` (default 10 m). If that is 0, or Quick Stack is set
to only stack into the open chest (`QuickStackHotkeyBehaviorWhenContainerOpen`), the drawers are left alone, the
same as nearby chests.

## Install

- **r2modman / Thunderstore Mod Manager:** remove the old ItemDrawers_Ymiron, then *Settings > Import local mod*
  and pick [`dist/ItemDrawers_Ymiron-0.5.0-quickstack.zip`](dist/ItemDrawers_Ymiron-0.5.0-quickstack.zip).
- **By hand:** replace `BepInEx/plugins/ItemDrawers.dll` with [`dist/ItemDrawers.dll`](dist/ItemDrawers.dll).

Quick stacking runs on your own game, so each player who wants it needs the patched dll. The server doesn't
need it. The plugin version stays **0.5.0** on purpose: players with the unpatched 0.5.0 get no version warning,
and nothing about the drawers' network data changed.

## How the dll was built

There is no game install here, so `tools/build.sh` compiles `src/QuickStackStore_Compat.cs` against small stand-ins
for `assembly_valheim` and `BepInEx` (`tools/stubs`) and against the original `ItemDrawers.dll`. Then it copies
the new classes and the attribute into the original dll with Mono.Cecil (`tools/patcher`). Nothing else in the
dll changes: decompiling the result gives the original code, the three new classes and the extra
`BepInDependency`.

The stand-ins prove nothing on their own, so the last step checks that **every game, BepInEx and Harmony member
the new code calls is one the original ItemDrawers.dll or Quick Stack Store already calls**. Those two dlls are
known to work in the game. Result: 772 references, 0 problems, no new assembly references.

If you have the ItemDrawers source, the simpler route is to add `src/QuickStackStore_Compat.cs` to the project,
add the attribute above and build it normally.

**Not yet tested in game.** Check that it works before you rely on it.
