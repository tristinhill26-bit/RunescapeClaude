package com.wikigear;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * One recommended equipment table from a wiki strategy page, e.g. the ranged setup for Vorkath.
 */
@Getter
@RequiredArgsConstructor
public class GearSetup
{
	/**
	 * Name shown to the user, e.g. "Ranged" or "Melee (Phase 2)"
	 */
	private final String label;
	/**
	 * For each slot, the recommended items from best to worst. Each rank can list several equally good items.
	 */
	private final Map<GearSlot, List<List<String>>> slots;

	public GearSetup withLabel(String label) {
		return new GearSetup(label, slots);
	}

	public List<List<String>> getRanks(GearSlot slot) {
		return slots.getOrDefault(slot, Collections.emptyList());
	}

	static Map<GearSlot, List<List<String>>> emptySlots() {
		return new EnumMap<>(GearSlot.class);
	}
}
