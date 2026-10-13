package com.wikigear;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.*;

public class InventorySubstitutesTest {
	private static final Map<String, Integer> HEALS = Map.of(
		"Shark", 20,
		"Cooked karambwan", 18,
		"Potato with cheese", 16,
		"Lobster", 12
	);

	private static final InventorySubstitutes SUBSTITUTES = new InventorySubstitutes(
		name -> HEALS.getOrDefault(name, 0),
		item -> HEALS.getOrDefault(item.name, 0)
	);

	private static GearSetup withInventory(String... items) {
		var inventory = new ArrayList<String>(Collections.nCopies(GearSetup.INVENTORY_SIZE, (String) null));
		for (int i = 0; i < items.length; i++)
			inventory.set(i, items[i]);
		return new GearSetup("Magic", GearSetup.emptySlots(), inventory);
	}

	@Test
	public void swapsMissingFoodForTheBestFoodOwned() {
		var owned = new GearPicker.OwnedItems();
		owned.add(1, "Potato with cheese", null, null, 100);
		owned.add(2, "Cooked karambwan", null, null, 100);
		owned.add(3, "Lobster", null, null, 100);
		owned.add(4, "Prayer potion(4)", null, null, 10);

		var result = GearPicker.pick(withInventory("Shark", "Shark", "Prayer potion(4)"), owned, false, SUBSTITUTES);
		assertEquals(2, result.inventory[0]);
		assertEquals(2, result.inventory[1]);
		assertEquals(4, result.inventory[2]);
		assertEquals(Map.of("Shark", "Cooked karambwan"), result.substitutions);
		assertTrue(result.missingInventory.isEmpty());
	}

	@Test
	public void prefersFoodThereIsEnoughOf() {
		var owned = new GearPicker.OwnedItems();
		owned.add(2, "Cooked karambwan", null, null, 1);
		owned.add(1, "Potato with cheese", null, null, 50);

		var result = GearPicker.pick(withInventory("Shark", "Shark", "Shark"), owned, false, SUBSTITUTES);
		assertEquals(1, result.inventory[0]);
	}

	@Test
	public void swapsPotionsWithinTheirGroup() {
		var owned = new GearPicker.OwnedItems();
		owned.add(5, "Prayer potion(3)", null, null, 5);
		owned.add(6, "Antidote++(4)", null, null, 5);
		owned.add(7, "Super energy(2)", null, null, 5);

		var result = GearPicker.pick(withInventory("Super restore(4)", "Anti-venom+(4)", "Stamina potion(4)", "Ranging potion(4)"),
			owned, false, SUBSTITUTES);
		assertEquals(5, result.inventory[0]);
		assertEquals(6, result.inventory[1]);
		assertEquals(7, result.inventory[2]);
		assertEquals(-1, result.inventory[3]);
		assertEquals(List.of("Ranging potion(4)"), result.missingInventory);
	}

	@Test
	public void leavesOtherItemsMissing() {
		var owned = new GearPicker.OwnedItems();
		owned.add(1, "Lobster", null, null, 100);
		var result = GearPicker.pick(withInventory("Spade"), owned, false, SUBSTITUTES);
		assertEquals(-1, result.inventory[0]);
		assertEquals(List.of("Spade"), result.missingInventory);
	}

	@Test
	public void addsUpQuantitiesAcrossContainers() {
		var owned = new GearPicker.OwnedItems();
		owned.add(1, "Shark", null, null, 3);
		owned.add(1, "Shark", null, null, 4);
		assertEquals(7, owned.findItem("Shark").quantity);
		assertEquals(1, owned.getAll().size());
	}

	@Test
	public void fillsGenericFoodWithTheBestFoodOwned() {
		var owned = new GearPicker.OwnedItems();
		owned.add(1, "Lobster", null, null, 500);
		owned.add(2, "Cooked karambwan", null, null, 300);

		var food = InventorySubstitutes.ANY_FOOD;
		var result = GearPicker.pick(withInventory(food, food, food), owned, false, SUBSTITUTES);
		assertEquals(2, result.inventory[0]);
		assertEquals(2, result.inventory[2]);
		assertEquals(Map.of(food, "Cooked karambwan"), result.substitutions);
	}

	@Test
	public void ignoresNotesInWikiPageNames() {
		var owned = new GearPicker.OwnedItems();
		owned.add(8013, "Teleport to house", null, null, 50);
		var result = GearPicker.pick(withInventory("Teleport to house (tablet)"), owned, false, SUBSTITUTES);
		assertEquals(8013, result.inventory[0]);
		assertTrue(result.missingInventory.isEmpty());
	}
}
