using System;
using System.Collections.Generic;
using System.Reflection;
using BepInEx.Configuration;
using HarmonyLib;
using UnityEngine;

namespace ItemDrawers;

// Quick Stack - Store - Sort - Trash - Restock (goldenrevolver.quick_stack_store) moves items only into vanilla
// Containers, and a drawer is not one: its stock lives in its ZDO. These patches make its quick stack (to the open
// chest or to the nearby ones, button or hotkey) and its Store all also fill the drawers already set to an item.
// The drawers do not follow that mod's rules for nearby chests (off in multiplayer, off while a chest is open):
// those protect chests, while every change to a drawer is a request to its owner. Everything in that mod is looked
// up by name: without it, or with a version that changed these methods, nothing here is patched.
internal static class QuickStackCompat
{
	private const string Section = "8 - Quick Stack";

	internal static ConfigEntry<bool> Enabled;

	internal static ConfigEntry<bool> StoreAll;

	internal static ConfigEntry<bool> DrawersFirst;

	internal static ConfigEntry<float> Range;

	internal static MethodInfo DoQuickStack;

	internal static MethodInfo ReportResult;

	internal static MethodInfo StoreAllItems;

	private static MethodInfo _shouldQuickStackItem;

	private static MethodInfo _shouldStoreItem;

	private static MethodInfo _getPlayerConfig;

	// That mod creates its config entries in its Start, after this one is patched: read when used.
	private static FieldInfo _hotbarOverride;

	private static FieldInfo _quickStackHotbar;

	private static FieldInfo _storeAllHotbar;

	private static bool _resolved;

	private static bool _available;

	// While a quick stack runs, its result message is held back so the drawers' stacks can be added to it.
	private static bool _active;

	private static int _chestStacks;

	private static int _drawerStacks;

	internal static bool Available
	{
		get
		{
			if (!_resolved)
			{
				_resolved = true;
				_available = Resolve();
			}
			return _available;
		}
	}

	private static bool Resolve()
	{
		Assembly assembly = null;
		foreach (Assembly loaded in AppDomain.CurrentDomain.GetAssemblies())
		{
			if (loaded.GetName().Name == "QuickStackStore")
			{
				assembly = loaded;
				break;
			}
		}
		if (assembly == null)
		{
			ItemDrawersPlugin.Log.LogInfo("Quick Stack - Store - Sort - Trash - Restock not loaded: no quick stacking into the drawers.");
			return false;
		}
		Type quickStack = assembly.GetType("QuickStackStore.QuickStackModule");
		Type storeAll = assembly.GetType("QuickStackStore.StoreTakeAllModule");
		Type userConfig = assembly.GetType("QuickStackStore.UserConfig");
		Type generalConfig = assembly.GetType("QuickStackStore.QSSConfig+GeneralConfig");
		Type quickStackConfig = assembly.GetType("QuickStackStore.QSSConfig+QuickStackConfig");
		Type storeAllConfig = assembly.GetType("QuickStackStore.QSSConfig+StoreTakeAllConfig");
		if (quickStack != null)
		{
			DoQuickStack = AccessTools.Method(quickStack, "DoQuickStack");
			ReportResult = AccessTools.Method(quickStack, "ReportQuickStackResult");
			_shouldQuickStackItem = AccessTools.Method(quickStack, "ShouldQuickStackItem");
		}
		if (storeAll != null)
		{
			StoreAllItems = AccessTools.Method(storeAll, "StoreAllItemsInOrder");
			_shouldStoreItem = AccessTools.Method(storeAll, "ShouldStoreItem");
		}
		if (userConfig != null)
		{
			_getPlayerConfig = AccessTools.Method(userConfig, "GetPlayerConfig");
		}
		if (generalConfig != null)
		{
			_hotbarOverride = AccessTools.Field(generalConfig, "OverrideHotkeyBarBehavior");
		}
		if (quickStackConfig != null)
		{
			_quickStackHotbar = AccessTools.Field(quickStackConfig, "QuickStackIncludesHotkeyBar");
		}
		if (storeAllConfig != null)
		{
			_storeAllHotbar = AccessTools.Field(storeAllConfig, "StoreAllIncludesHotkeyBar");
		}
		if (DoQuickStack == null || ReportResult == null || StoreAllItems == null || _shouldQuickStackItem == null || _shouldStoreItem == null || _getPlayerConfig == null || _hotbarOverride == null || _quickStackHotbar == null || _storeAllHotbar == null)
		{
			ItemDrawersPlugin.Log.LogWarning("Quick Stack - Store - Sort - Trash - Restock is installed, but not in a version this was made for (1.4.16): its quick stacking leaves the drawers alone.");
			return false;
		}
		Enabled = ModConfig.File.Bind(Section, "Enabled", true, "Quick stacking (Quick Stack - Store - Sort - Trash - Restock: to the open chest or to the nearby ones, button or hotkey) also fills the drawers already set to an item. Works in multiplayer too. Drawers inside a ward you cannot open are left alone. A drawer never takes an item none of its compartments was set to.");
		StoreAll = ModConfig.File.Bind(Section, "StoreAll", true, "That mod's Store all (with a chest open) first puts into the drawers what they already hold, the rest goes into the chest.");
		DrawersFirst = ModConfig.File.Bind(Section, "DrawersFirst", true, "Quick stack: true = the drawers fill up before the chests; false = the chests that already hold an item get it first, the drawers take the rest.");
		Range = ModConfig.File.Bind(Section, "Range", 20f, new ConfigDescription("Drawers within this many meters of you are filled by quick stacking and Store all.", new AcceptableValueRange<float>(2f, 100f)));
		ItemDrawersPlugin.Log.LogInfo("Quick Stack - Store - Sort - Trash - Restock found: its quick stack and Store all also fill the drawers.");
		return true;
	}

	private static bool Usable(Player player)
	{
		if (player != null && player == Player.m_localPlayer && !player.IsTeleporting())
		{
			return InventoryGui.instance != null;
		}
		return false;
	}

	private static bool IncludesHotbar(FieldInfo feature)
	{
		object hotbarOverride = (_hotbarOverride.GetValue(null) as ConfigEntryBase)?.BoxedValue;
		object includes = (feature.GetValue(null) as ConfigEntryBase)?.BoxedValue;
		if (hotbarOverride != null && includes != null && Convert.ToInt32(hotbarOverride) != 0)
		{
			return (bool)includes;
		}
		return false;
	}

	internal static void BeginQuickStack(Player player)
	{
		_active = false;
		_chestStacks = -1;
		_drawerStacks = 0;
		if (Enabled.Value && Usable(player))
		{
			_active = true;
			if (DrawersFirst.Value)
			{
				_drawerStacks = StoreInDrawers(player, _shouldQuickStackItem, IncludesHotbar(_quickStackHotbar), "Quick stack");
			}
		}
	}

	// Returns false to hold back the message of the chests while the drawers have not had their turn yet.
	internal static bool OnReport(int movedCount)
	{
		if (!_active)
		{
			return true;
		}
		_chestStacks = movedCount;
		return false;
	}

	internal static void EndQuickStack(Player player, bool completed)
	{
		if (!_active)
		{
			return;
		}
		_active = false;
		if (!completed)
		{
			return;
		}
		if (!DrawersFirst.Value)
		{
			_drawerStacks += StoreInDrawers(player, _shouldQuickStackItem, IncludesHotbar(_quickStackHotbar), "Quick stack");
		}
		// No report from the chests and nothing in the drawers: it stopped early and said why itself.
		if (_chestStacks < 0 && _drawerStacks == 0)
		{
			return;
		}
		ReportResult.Invoke(null, new object[2] { player, Mathf.Max(0, _chestStacks) + _drawerStacks });
	}

	internal static void BeforeStoreAll(Player player)
	{
		if (Enabled.Value && StoreAll.Value && Usable(player) && InventoryGui.instance.m_currentContainer != null)
		{
			int stacks = StoreInDrawers(player, _shouldStoreItem, IncludesHotbar(_storeAllHotbar), "Store all");
			if (stacks > 0)
			{
				player.Message(MessageHud.MessageType.Center, $"{stacks} stacks into the drawers", 0, null, false);
			}
		}
	}

	// Same items as that mod would move (no favorites, the hotbar only if its config says so), each kind in one
	// deposit so the room left in the drawers is counted once for all its stacks. Returns the stacks moved.
	private static int StoreInDrawers(Player player, MethodInfo shouldMove, bool includeHotbar, string feature)
	{
		Inventory inventory = player.GetInventory();
		object playerConfig = _getPlayerConfig.Invoke(null, new object[1] { player.GetPlayerID() });
		int height = inventory.GetHeight();
		int width = inventory.GetWidth();
		Dictionary<string, List<ItemDrop.ItemData>> kinds = new Dictionary<string, List<ItemDrop.ItemData>>();
		foreach (ItemDrop.ItemData item in inventory.GetAllItems())
		{
			if (ItemInfo.CanStore(item) && (bool)shouldMove.Invoke(null, new object[5] { item, playerConfig, height, width, includeHotbar }))
			{
				string key = item.m_dropPrefab.name + "\n" + ItemInfo.DataKey(item);
				if (!kinds.TryGetValue(key, out var stacks))
				{
					stacks = new List<ItemDrop.ItemData>();
					kinds[key] = stacks;
				}
				stacks.Add(item);
			}
		}
		Vector3 center = player.transform.position;
		float range = Range.Value;
		int moved = 0;
		int items = 0;
		foreach (List<ItemDrop.ItemData> stacks in kinds.Values)
		{
			int total = 0;
			foreach (ItemDrop.ItemData stack in stacks)
			{
				total += stack.m_stack;
			}
			int stored = DrawerNetwork.Deposit(stacks[0].m_dropPrefab.name, ItemInfo.DataKey(stacks[0]), total, center, range, useFree: false, Delivery.Return);
			items += stored;
			foreach (ItemDrop.ItemData stack in stacks)
			{
				if (stored <= 0)
				{
					break;
				}
				int amount = Mathf.Min(stored, stack.m_stack);
				inventory.RemoveItem(stack, amount);
				stored -= amount;
				moved++;
			}
		}
		DrawerNetwork.Stock(center, range, out var drawers, out var _);
		ItemDrawersPlugin.Log.LogInfo($"{feature}: {drawers} drawers within {range} m, {kinds.Count} kinds of item to put away, {items} items went into the drawers.");
		return moved;
	}
}

[HarmonyPatch]
internal static class QuickStackModule_DoQuickStack_Patch
{
	private static bool Prepare()
	{
		return QuickStackCompat.Available;
	}

	private static MethodBase TargetMethod()
	{
		return QuickStackCompat.DoQuickStack;
	}

	private static void Prefix(Player player)
	{
		try
		{
			QuickStackCompat.BeginQuickStack(player);
		}
		catch (Exception ex)
		{
			ItemDrawersPlugin.Log.LogError("Quick stacking into the drawers failed: " + ex);
		}
	}

	private static void Finalizer(Player player, Exception __exception)
	{
		try
		{
			QuickStackCompat.EndQuickStack(player, __exception == null);
		}
		catch (Exception ex)
		{
			ItemDrawersPlugin.Log.LogError("Quick stacking into the drawers failed: " + ex);
		}
	}
}

[HarmonyPatch]
internal static class QuickStackModule_ReportQuickStackResult_Patch
{
	private static bool Prepare()
	{
		return QuickStackCompat.Available;
	}

	private static MethodBase TargetMethod()
	{
		return QuickStackCompat.ReportResult;
	}

	private static bool Prefix(int movedCount)
	{
		return QuickStackCompat.OnReport(movedCount);
	}
}

[HarmonyPatch]
internal static class StoreTakeAllModule_StoreAllItemsInOrder_Patch
{
	private static bool Prepare()
	{
		return QuickStackCompat.Available;
	}

	private static MethodBase TargetMethod()
	{
		return QuickStackCompat.StoreAllItems;
	}

	private static void Prefix(Player player)
	{
		try
		{
			QuickStackCompat.BeforeStoreAll(player);
		}
		catch (Exception ex)
		{
			ItemDrawersPlugin.Log.LogError("Storing into the drawers failed: " + ex);
		}
	}
}
