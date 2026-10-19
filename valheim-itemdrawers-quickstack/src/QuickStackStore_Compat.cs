using System;
using System.Collections.Generic;
using System.Reflection;
using BepInEx.Configuration;
using HarmonyLib;
using UnityEngine;

namespace ItemDrawers;

// Quick Stack - Store - Sort - Trash - Restock (goldenrevolver.quick_stack_store) quick stacks only into vanilla
// Containers, and a drawer is not one: its stock lives in its ZDO. These patches make its "quick stack to nearby"
// (the button and the hotkey) also fill the drawers already set to an item. Everything in that mod is looked up by
// name: without it, or with a version that changed these methods, nothing here is patched.
internal static class QuickStackCompat
{
	internal const string Guid = "goldenrevolver.quick_stack_store";

	internal static ConfigEntry<bool> Enabled;

	internal static ConfigEntry<bool> DrawersFirst;

	internal static MethodInfo DoQuickStack;

	internal static MethodInfo ReportResult;

	private static MethodInfo _shouldQuickStackItem;

	private static MethodInfo _shouldAreaQuickStack;

	private static MethodInfo _getPlayerConfig;

	// That mod creates its config entries in its Start, after this one is patched: read when used.
	private static FieldInfo _range;

	private static FieldInfo _hotbarOverride;

	private static FieldInfo _includeHotbar;

	private static bool _resolved;

	private static bool _available;

	// While an area quick stack runs, its result message is held back so the drawers' stacks can be added to it.
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
			return false;
		}
		Type module = assembly.GetType("QuickStackStore.QuickStackModule");
		Type userConfig = assembly.GetType("QuickStackStore.UserConfig");
		Type quickStackConfig = assembly.GetType("QuickStackStore.QSSConfig+QuickStackConfig");
		Type generalConfig = assembly.GetType("QuickStackStore.QSSConfig+GeneralConfig");
		if (module != null)
		{
			DoQuickStack = AccessTools.Method(module, "DoQuickStack");
			ReportResult = AccessTools.Method(module, "ReportQuickStackResult");
			_shouldQuickStackItem = AccessTools.Method(module, "ShouldQuickStackItem");
			_shouldAreaQuickStack = AccessTools.Method(module, "ShouldAreaQuickStack");
		}
		if (userConfig != null)
		{
			_getPlayerConfig = AccessTools.Method(userConfig, "GetPlayerConfig");
		}
		if (quickStackConfig != null)
		{
			_range = AccessTools.Field(quickStackConfig, "QuickStackToNearbyRange");
			_includeHotbar = AccessTools.Field(quickStackConfig, "QuickStackIncludesHotkeyBar");
		}
		if (generalConfig != null)
		{
			_hotbarOverride = AccessTools.Field(generalConfig, "OverrideHotkeyBarBehavior");
		}
		if (DoQuickStack == null || ReportResult == null || _shouldQuickStackItem == null || _shouldAreaQuickStack == null || _getPlayerConfig == null || _range == null || _includeHotbar == null || _hotbarOverride == null)
		{
			ItemDrawersPlugin.Log.LogWarning("Quick Stack - Store - Sort - Trash - Restock is installed, but not in a version this was made for (1.4.16): its quick stacking leaves the drawers alone.");
			return false;
		}
		Enabled = ModConfig.File.Bind("8 - Quick Stack", "Enabled", true, "Quick stacking to nearby containers (Quick Stack - Store - Sort - Trash - Restock, its button or its hotkey) also fills the drawers already set to an item, within that mod's QuickStackToNearbyRange. Drawers inside a ward you cannot open are left alone. A drawer never takes an item none of its compartments was set to.");
		DrawersFirst = ModConfig.File.Bind("8 - Quick Stack", "DrawersFirst", true, "true = the drawers fill up before the chests; false = the chests that already hold an item get it first, the drawers take the rest.");
		return true;
	}

	private static object Value(FieldInfo entry)
	{
		return (entry.GetValue(null) as ConfigEntryBase)?.BoxedValue;
	}

	internal static void Begin(Player player, bool onlyCurrentContainer, Container containerOverride)
	{
		_active = false;
		_chestStacks = -1;
		_drawerStacks = 0;
		if (!Enabled.Value || onlyCurrentContainer || player == null || player != Player.m_localPlayer || player.IsTeleporting() || InventoryGui.instance == null || Value(_range) == null || Value(_includeHotbar) == null || Value(_hotbarOverride) == null)
		{
			return;
		}
		Container current = (containerOverride != null) ? containerOverride : InventoryGui.instance.m_currentContainer;
		if ((bool)_shouldAreaQuickStack.Invoke(null, new object[1] { current }))
		{
			_active = true;
			if (DrawersFirst.Value)
			{
				_drawerStacks = StoreInDrawers(player);
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

	internal static void End(Player player, bool completed)
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
			_drawerStacks += StoreInDrawers(player);
		}
		// No report from the chests and nothing in the drawers: it stopped early and said why itself.
		if (_chestStacks < 0 && _drawerStacks == 0)
		{
			return;
		}
		ReportResult.Invoke(null, new object[2] { player, Mathf.Max(0, _chestStacks) + _drawerStacks });
	}

	// Same items as the chests would get (no favorites, the hotbar only if that mod's config says so), each kind
	// in one deposit so the room left in the drawers is counted once for all its stacks. Returns the stacks moved.
	private static int StoreInDrawers(Player player)
	{
		Inventory inventory = player.GetInventory();
		object playerConfig = _getPlayerConfig.Invoke(null, new object[1] { player.GetPlayerID() });
		bool includeHotbar = Convert.ToInt32(Value(_hotbarOverride)) != 0 && (bool)Value(_includeHotbar);
		int height = inventory.GetHeight();
		int width = inventory.GetWidth();
		Dictionary<string, List<ItemDrop.ItemData>> kinds = new Dictionary<string, List<ItemDrop.ItemData>>();
		foreach (ItemDrop.ItemData item in inventory.GetAllItems())
		{
			if (ItemInfo.CanStore(item) && (bool)_shouldQuickStackItem.Invoke(null, new object[5] { item, playerConfig, height, width, includeHotbar }))
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
		float range = (float)Value(_range);
		int moved = 0;
		foreach (List<ItemDrop.ItemData> stacks in kinds.Values)
		{
			int total = 0;
			foreach (ItemDrop.ItemData stack in stacks)
			{
				total += stack.m_stack;
			}
			int stored = DrawerNetwork.Deposit(stacks[0].m_dropPrefab.name, ItemInfo.DataKey(stacks[0]), total, center, range, useFree: false, Delivery.Return);
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

	private static void Prefix(Player player, bool onlyQuickStackToCurrentContainer, Container currentContainerOverride)
	{
		try
		{
			QuickStackCompat.Begin(player, onlyQuickStackToCurrentContainer, currentContainerOverride);
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
			QuickStackCompat.End(player, __exception == null);
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
