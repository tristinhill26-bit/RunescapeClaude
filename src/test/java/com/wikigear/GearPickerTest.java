package com.wikigear;

import java.util.List;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.*;

public class GearPickerTest {
	private static GearSetup setup(Map<GearSlot, List<List<String>>> slots) {
		var map = GearSetup.emptySlots();
		map.putAll(slots);
		return new GearSetup("Test", map);
	}

	@Test
	public void picksHighestRankedOwnedItem() {
		var owned = new GearPicker.OwnedItems();
		owned.add(2, "Blessed coif", null);
		owned.add(3, "Void ranger helm", null);

		var picks = GearPicker.pick(setup(Map.of(GearSlot.HEAD, List.of(
			List.of("Armadyl helmet"),
			List.of("Void ranger helm", "Blessed coif")
		))), owned);

		var head = picks.get(GearSlot.HEAD);
		assertEquals(3, head.itemId);
		assertEquals(2, head.rank);
		assertEquals(2, head.rankCount);
	}

	@Test
	public void matchesChargedAndDegradedVariants() {
		var owned = new GearPicker.OwnedItems();
		owned.add(10, "Amulet of glory(4)", null);
		owned.add(11, "Dharok's helm 100", null);
		owned.add(12, "Slayer helmet (i)", "Slayer helmet");

		assertEquals(Integer.valueOf(10), owned.find("Amulet of glory"));
		assertEquals(Integer.valueOf(11), owned.find("Dharok's helm"));
		assertEquals(Integer.valueOf(12), owned.find("Slayer helmet"));
		assertEquals(Integer.valueOf(12), owned.find("slayer helmet (i)"));
	}

	@Test
	public void doesNotTreatUnimbuedItemsAsImbued() {
		var owned = new GearPicker.OwnedItems();
		owned.add(20, "Berserker ring", null);
		assertNull(owned.find("Berserker ring (i)"));
		assertEquals(Integer.valueOf(20), owned.find("Berserker ring"));
	}

	@Test
	public void leavesSlotEmptyWhenNothingIsOwned() {
		var picks = GearPicker.pick(setup(Map.of(GearSlot.BODY, List.of(List.of("Bandos chestplate")))), new GearPicker.OwnedItems());
		assertTrue(picks.isEmpty());
	}
}
