package com.wikigear;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Getter;

/**
 * One recommended setup from a wiki strategy page, e.g. the ranged setup for Vorkath.
 */
@Getter
public class GearSetup
{
	public static final int INVENTORY_SIZE = 28;

	/**
	 * Name shown to the user, e.g. "Ranged" or "Melee (Phase 2)"
	 */
	private final String label;
	/**
	 * For each slot, the recommended items from best to worst. Each rank can list several equally good items.
	 */
	private final Map<GearSlot, List<List<String>>> slots;
	/**
	 * The recommended inventory, by inventory slot. Empty slots are null, and the list is empty if there's none.
	 */
	private final List<String> inventory;

	public GearSetup(String label, Map<GearSlot, List<List<String>>> slots, List<String> inventory) {
		this.label = label;
		this.slots = slots;
		this.inventory = inventory;
	}

	public GearSetup(String label, Map<GearSlot, List<List<String>>> slots) {
		this(label, slots, Collections.emptyList());
	}

	public GearSetup withLabel(String label) {
		return new GearSetup(label, slots, inventory);
	}

	public GearSetup withInventory(List<String> inventory) {
		return new GearSetup(label, slots, inventory);
	}

	public List<List<String>> getRanks(GearSlot slot) {
		return slots.getOrDefault(slot, Collections.emptyList());
	}

	public boolean hasInventory() {
		return inventory.stream().anyMatch(item -> item != null);
	}

	@Nullable
	public String getInventoryItem(int slot) {
		return slot < inventory.size() ? inventory.get(slot) : null;
	}

	static Map<GearSlot, List<List<String>>> emptySlots() {
		return new EnumMap<>(GearSlot.class);
	}
}
