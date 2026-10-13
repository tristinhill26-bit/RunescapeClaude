package com.wikigear;

import java.util.List;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.*;

public class GearPickerTest {
	private static final int HEAD = 0, NECK = 2, WEAPON = 3, BODY = 4, LEGS = 7, HANDS = 9;

	private static GearSetup setup(String label, Map<GearSlot, List<List<String>>> slots) {
		var map = GearSetup.emptySlots();
		map.putAll(slots);
		return new GearSetup(label, map);
	}

	private static ItemBonuses.ItemBonusesBuilder bonuses(int slot) {
		return ItemBonuses.builder().slot(slot);
	}

	@Test
	public void picksHighestRankedOwnedItem() {
		var owned = new GearPicker.OwnedItems();
		owned.add(2, "Blessed coif", null);
		owned.add(3, "Void ranger helm", null);

		var result = GearPicker.pick(setup("Test", Map.of(GearSlot.HEAD, List.of(
			List.of("Armadyl helmet"),
			List.of("Blessed coif", "Snakeskin bandana")
		))), owned, false);

		var head = result.picks.get(GearSlot.HEAD);
		assertEquals(2, head.itemId);
		assertEquals(2, head.rank);
		assertEquals(2, head.rankCount);
		assertEquals(GearPicker.Source.WIKI, head.source);
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
		var result = GearPicker.pick(setup("Melee", Map.of(GearSlot.BODY, List.of(List.of("Bandos chestplate")))), new GearPicker.OwnedItems(), true);
		assertTrue(result.picks.isEmpty());
	}

	private static final GearSetup VOID_RANGED = setup("Ranged", Map.of(
		GearSlot.HEAD, List.of(List.of("Void ranger helm"), List.of("Armadyl helmet")),
		GearSlot.BODY, List.of(List.of("Elite void top", "Void knight top"), List.of("Armadyl chestplate")),
		GearSlot.LEGS, List.of(List.of("Elite void robe", "Void knight robe"), List.of("Armadyl chainskirt")),
		GearSlot.HANDS, List.of(List.of("Barrows gloves"))
	));

	@Test
	public void completesVoidWhenTheWholeSetIsOwned() {
		var owned = new GearPicker.OwnedItems();
		owned.add(1, "Void ranger helm", null);
		owned.add(2, "Void knight top", null);
		owned.add(3, "Elite void robe (or)", null);
		owned.add(4, "Void knight gloves", null);
		owned.add(5, "Barrows gloves", null);

		var result = GearPicker.pick(VOID_RANGED, owned, false);
		assertEquals(1, result.picks.get(GearSlot.HEAD).itemId);
		assertEquals(2, result.picks.get(GearSlot.BODY).itemId);
		assertEquals(3, result.picks.get(GearSlot.LEGS).itemId);
		// Barrows gloves are swapped for Void gloves, since Void needs all four pieces
		assertEquals(4, result.picks.get(GearSlot.HANDS).itemId);
		assertEquals(GearPicker.Source.SET, result.picks.get(GearSlot.HANDS).source);
		assertEquals(1, result.notes.size());
	}

	@Test
	public void dropsVoidWhenAPieceIsMissing() {
		var owned = new GearPicker.OwnedItems();
		owned.add(1, "Void ranger helm", null);
		owned.add(2, "Void knight top", null);
		owned.add(3, "Void knight robe", null);
		owned.add(6, "Armadyl helmet", null);
		owned.add(7, "Armadyl chestplate", null);

		var result = GearPicker.pick(VOID_RANGED, owned, false);
		assertEquals(6, result.picks.get(GearSlot.HEAD).itemId);
		assertEquals(7, result.picks.get(GearSlot.BODY).itemId);
		assertNull(result.picks.get(GearSlot.LEGS));
		assertTrue(result.notes.get(0).contains("Void"));
	}

	@Test
	public void picksTheStylesHelmForVoid() {
		var owned = new GearPicker.OwnedItems();
		owned.add(1, "Void melee helm", null);
		owned.add(8, "Void mage helm", null);
		owned.add(2, "Void knight top", null);
		owned.add(3, "Void knight robe", null);
		owned.add(4, "Void knight gloves", null);

		var mage = setup("Magic", Map.of(GearSlot.BODY, List.of(List.of("Void knight top"))));
		var result = GearPicker.pick(mage, owned, false);
		assertEquals(8, result.picks.get(GearSlot.HEAD).itemId);
	}

	@Test
	public void fillsEmptySlotsWithTheBestItemForTheStyle() {
		var owned = new GearPicker.OwnedItems();
		owned.add(10, "Amulet of glory(4)", null, bonuses(NECK).stab(10).slash(10).crush(10).magic(10).ranged(10).strength(6).prayer(3).build());
		owned.add(11, "Amulet of magic", null, bonuses(NECK).magic(10).build());
		owned.add(12, "Mystic hat", null, bonuses(HEAD).magic(4).build());
		owned.add(13, "Bronze full helm", null, bonuses(HEAD).magic(-6).ranged(-3).defence(15).build());
		owned.add(14, "Trident of the seas", null, bonuses(WEAPON).magic(25).build());

		var magic = setup("Magic", Map.of(GearSlot.WEAPON, List.of(List.of("Trident of the seas"))));
		var result = GearPicker.pick(magic, owned, true);

		assertEquals(14, result.picks.get(GearSlot.WEAPON).itemId);
		assertEquals(CombatStyle.MAGIC, result.style);
		// The glory has the higher magic score once prayer is counted
		assertEquals(10, result.picks.get(GearSlot.NECK).itemId);
		assertEquals(GearPicker.Source.FILLED, result.picks.get(GearSlot.NECK).source);
		// Bronze hurts magic, so the mystic hat is used
		assertEquals(12, result.picks.get(GearSlot.HEAD).itemId);
	}

	@Test
	public void infersStyleFromTheWeaponWhenTheLabelDoesNotSay() {
		var owned = new GearPicker.OwnedItems();
		owned.add(20, "Rune crossbow", null, bonuses(WEAPON).ranged(90).build());
		owned.add(21, "Coif", null, bonuses(HEAD).ranged(2).defence(8).build());
		owned.add(22, "Neitiznot helm", null, bonuses(HEAD).strength(3).defence(100).build());

		var result = GearPicker.pick(setup("Phase 1", Map.of(GearSlot.WEAPON, List.of(List.of("Rune crossbow")))), owned, true);
		assertEquals(CombatStyle.RANGED, result.style);
		assertEquals(21, result.picks.get(GearSlot.HEAD).itemId);
	}

	@Test
	public void doesNotFillWhenDisabled() {
		var owned = new GearPicker.OwnedItems();
		owned.add(10, "Amulet of glory(4)", null, bonuses(NECK).strength(6).build());
		var result = GearPicker.pick(setup("Melee", Map.of()), owned, false);
		assertTrue(result.picks.isEmpty());
	}

	@Test
	public void doesNotAddAShieldToATwoHandedWeapon() {
		var owned = new GearPicker.OwnedItems();
		owned.add(30, "Dharok's greataxe", null, bonuses(WEAPON).twoHanded(true).crush(105).strength(105).build());
		owned.add(31, "Rune kiteshield", null, bonuses(5).defence(150).build());

		var result = GearPicker.pick(setup("Melee", Map.of(GearSlot.TWO_HANDED, List.of(List.of("Dharok's greataxe")))), owned, true);
		assertNull(result.picks.get(GearSlot.SHIELD));
		assertNull(result.picks.get(GearSlot.WEAPON));
	}

	@Test
	public void matchesInventoryItemsAtAnyDose() {
		var owned = new GearPicker.OwnedItems();
		owned.add(40, "Prayer potion(3)", null);
		owned.add(41, "Shark", null);

		var inventory = new java.util.ArrayList<String>(java.util.Collections.nCopies(GearSetup.INVENTORY_SIZE, (String) null));
		inventory.set(0, "Prayer potion(4)");
		inventory.set(1, "Prayer potion(4)");
		inventory.set(2, "Shark");
		inventory.set(3, "Saradomin brew(4)");
		var result = GearPicker.pick(new GearSetup("Magic", GearSetup.emptySlots(), inventory), owned, false);

		assertEquals(40, result.inventory[0]);
		assertEquals(40, result.inventory[1]);
		assertEquals(41, result.inventory[2]);
		assertEquals(-1, result.inventory[3]);
		assertEquals(-1, result.inventory[4]);
		assertEquals(List.of("Saradomin brew(4)"), result.missingInventory);
	}

	@Test
	public void layoutKeepsTheInventoryGridBesideTheGear() {
		assertEquals(4, GearSlot.inventoryLayoutPosition(0));
		assertEquals(7, GearSlot.inventoryLayoutPosition(3));
		assertEquals(12, GearSlot.inventoryLayoutPosition(4));
		assertEquals(6 * 8 + 7, GearSlot.inventoryLayoutPosition(27));
		for (var slot : GearSlot.values())
			assertTrue(slot.getColumn() < GearSlot.INVENTORY_COLUMN);
	}
}
