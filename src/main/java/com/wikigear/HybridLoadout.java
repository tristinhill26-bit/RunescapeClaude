package com.wikigear;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Combines several setups into one, for fights where you switch styles, like Magic and Ranged at Zulrah.
 * The first setup is worn, and the gear from the others that differs goes at the top of the inventory as switches.
 */
public class HybridLoadout
{
	private HybridLoadout() {
	}

	/**
	 * @param results the picks for each setup, the one to wear first
	 * @param labels  each setup's name, e.g. "Ranged", for the notes
	 */
	public static GearPicker.Result merge(List<GearPicker.Result> results, List<String> labels, GearPicker.OwnedItems owned) {
		var primary = results.get(0);
		var picks = new EnumMap<GearSlot, GearPicker.Pick>(GearSlot.class);
		picks.putAll(primary.picks);
		var notes = new ArrayList<>(primary.notes);

		var worn = new HashSet<Integer>();
		for (var pick : picks.values())
			worn.add(pick.itemId);

		// The other styles' gear that isn't already worn, in equipment order
		var switches = new LinkedHashSet<Integer>();
		var switchSlots = new HashSet<Integer>();
		for (int i = 1; i < results.size(); i++) {
			var names = new ArrayList<String>();
			for (var slot : GearSlot.values()) {
				var pick = results.get(i).picks.get(slot);
				if (pick == null || slot == GearSlot.SPECIAL || worn.contains(pick.itemId))
					continue;
				if (switches.add(pick.itemId))
					names.add(pick.name);
				switchSlots.add(slot.getEquipmentSlot());
			}
			if (!names.isEmpty())
				notes.add("Switches for " + labels.get(i) + ": " + String.join(", ", names) + ".");
			for (String note : results.get(i).notes)
				if (!notes.contains(note))
					notes.add(note);
		}

		// Then the recommended inventories, leaving out gear the switches replace
		var items = new ArrayList<Integer>(switches);
		var seen = new HashSet<>(switches);
		for (int i = 0; i < results.size(); i++) {
			for (int itemId : results.get(i).inventory) {
				if (itemId == -1 || worn.contains(itemId) || switches.contains(itemId) || replacedBySwitch(itemId, switchSlots, owned))
					continue;
				// The worn setup's inventory is kept as it is, including repeats like food; others only add what's new
				if (i > 0 && !seen.add(itemId))
					continue;
				seen.add(itemId);
				items.add(itemId);
			}
		}

		int[] inventory = new int[GearSetup.INVENTORY_SIZE];
		Arrays.fill(inventory, -1);
		var leftOut = new LinkedHashSet<String>();
		for (int i = 0; i < items.size(); i++) {
			if (i < inventory.length) {
				inventory[i] = items.get(i);
			} else {
				var item = owned.findById(items.get(i));
				leftOut.add(item != null ? item.name : String.valueOf(items.get(i)));
			}
		}
		if (!leftOut.isEmpty())
			notes.add("No room left for " + String.join(", ", leftOut) + ".");

		var missing = new ArrayList<String>();
		var substitutions = new LinkedHashMap<String, String>();
		for (var result : results) {
			for (String name : result.missingInventory)
				if (!missing.contains(name))
					missing.add(name);
			result.substitutions.forEach(substitutions::putIfAbsent);
		}

		return new GearPicker.Result(picks, notes, primary.style, inventory, missing, substitutions);
	}

	/**
	 * Whether a recommended inventory item is gear for a slot that already has a switch, like the wiki's Void ranger
	 * helm when the switches include a better ranged helmet
	 */
	private static boolean replacedBySwitch(int itemId, Set<Integer> switchSlots, GearPicker.OwnedItems owned) {
		var item = owned.findById(itemId);
		return item != null && item.bonuses != null && switchSlots.contains(item.bonuses.getSlot());
	}
}
