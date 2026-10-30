package com.wikigear;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;

public class HybridLoadoutTest
{
	private static final int TRIDENT = 1, ANCESTRAL_HAT = 2, ANCESTRAL_TOP = 3, OCCULT = 4, RING = 5;
	private static final int BLOWPIPE = 11, MASORI_MASK = 12, MASORI_BODY = 13, ANGUISH = 14;
	private static final int SHARK = 21, PRAYER_POTION = 22, VOID_RANGER_HELM = 23, RANGING_POTION = 24;

	private static GearPicker.OwnedItems owned() {
		var owned = new GearPicker.OwnedItems();
		owned.add(TRIDENT, "Trident of the swamp", null, bonuses(3));
		owned.add(ANCESTRAL_HAT, "Ancestral hat", null, bonuses(0));
		owned.add(ANCESTRAL_TOP, "Ancestral robe top", null, bonuses(4));
		owned.add(OCCULT, "Occult necklace", null, bonuses(2));
		owned.add(RING, "Ring of suffering (i)", null, bonuses(12));
		owned.add(BLOWPIPE, "Toxic blowpipe", null, bonuses(3));
		owned.add(MASORI_MASK, "Masori mask", null, bonuses(0));
		owned.add(MASORI_BODY, "Masori body", null, bonuses(4));
		owned.add(ANGUISH, "Necklace of anguish", null, bonuses(2));
		owned.add(SHARK, "Shark", null);
		owned.add(PRAYER_POTION, "Prayer potion(4)", null);
		owned.add(VOID_RANGER_HELM, "Void ranger helm", null, bonuses(0));
		owned.add(RANGING_POTION, "Ranging potion(4)", null);
		return owned;
	}

	private static ItemBonuses bonuses(int slot) {
		return ItemBonuses.builder().slot(slot).build();
	}

	private static GearPicker.Result result(int[] inventory, Object... picks) {
		var map = new EnumMap<GearSlot, GearPicker.Pick>(GearSlot.class);
		for (int i = 0; i < picks.length; i += 3)
			map.put((GearSlot) picks[i], new GearPicker.Pick((GearSlot) picks[i], (int) picks[i + 1], (String) picks[i + 2], 1, 1, GearPicker.Source.WIKI));
		int[] inv = Arrays.copyOf(inventory, GearSetup.INVENTORY_SIZE);
		for (int i = inventory.length; i < inv.length; i++)
			inv[i] = -1;
		return new GearPicker.Result(map, new ArrayList<>(), CombatStyle.MAGIC, inv, new ArrayList<>(), new LinkedHashMap<>());
	}

	@Test
	public void wearsTheFirstStyleAndCarriesTheOthersAsSwitches() {
		int[] wikiInventory = { VOID_RANGER_HELM, BLOWPIPE, PRAYER_POTION, PRAYER_POTION, SHARK, SHARK };
		var magic = result(wikiInventory,
			GearSlot.WEAPON, TRIDENT, "Trident of the swamp",
			GearSlot.HEAD, ANCESTRAL_HAT, "Ancestral hat",
			GearSlot.BODY, ANCESTRAL_TOP, "Ancestral robe top",
			GearSlot.NECK, OCCULT, "Occult necklace",
			GearSlot.RING, RING, "Ring of suffering (i)");
		var ranged = result(new int[] { RANGING_POTION, SHARK },
			GearSlot.WEAPON, BLOWPIPE, "Toxic blowpipe",
			GearSlot.HEAD, MASORI_MASK, "Masori mask",
			GearSlot.BODY, MASORI_BODY, "Masori body",
			GearSlot.NECK, ANGUISH, "Necklace of anguish",
			GearSlot.RING, RING, "Ring of suffering (i)");

		var merged = HybridLoadout.merge(List.of(magic, ranged), List.of("Magic", "Ranged"), owned());

		assertEquals(TRIDENT, merged.picks.get(GearSlot.WEAPON).itemId);
		assertEquals(ANCESTRAL_HAT, merged.picks.get(GearSlot.HEAD).itemId);
		// Switches first, in equipment order: head, neck, weapon, body. The shared ring isn't a switch.
		assertArrayEquals(new int[] { MASORI_MASK, ANGUISH, BLOWPIPE, MASORI_BODY }, Arrays.copyOf(merged.inventory, 4));
		// Then the wiki inventory, without the Void helm the Masori mask replaces, then the other style's extras
		assertArrayEquals(new int[] { PRAYER_POTION, PRAYER_POTION, SHARK, SHARK, RANGING_POTION, -1 }, Arrays.copyOfRange(merged.inventory, 4, 10));
		assertTrue(merged.notes.stream().anyMatch(n -> n.startsWith("Switches for Ranged: Masori mask")));
	}

	@Test
	public void notesItemsThatDontFit() {
		int[] full = new int[GearSetup.INVENTORY_SIZE];
		Arrays.fill(full, SHARK);
		var magic = result(full, GearSlot.WEAPON, TRIDENT, "Trident of the swamp");
		var ranged = result(new int[0], GearSlot.WEAPON, BLOWPIPE, "Toxic blowpipe");
		var merged = HybridLoadout.merge(List.of(magic, ranged), List.of("Magic", "Ranged"), owned());
		assertEquals(BLOWPIPE, merged.inventory[0]);
		assertTrue(merged.notes.stream().anyMatch(n -> n.contains("No room left for Shark")));
	}

	@Test
	public void startsSwitchesFromTheOwnedGearInTheInventory() {
		int[] inventory = { VOID_RANGER_HELM, BLOWPIPE, SHARK };
		var magic = result(inventory,
			GearSlot.WEAPON, TRIDENT, "Trident of the swamp",
			GearSlot.HEAD, ANCESTRAL_HAT, "Ancestral hat",
			GearSlot.BODY, ANCESTRAL_TOP, "Ancestral robe top",
			GearSlot.SPECIAL, TRIDENT, "Trident of the swamp");
		var picks = HybridLoadout.switchStartingPicks(magic, owned());
		assertEquals(VOID_RANGER_HELM, picks.get(GearSlot.HEAD).itemId);
		assertEquals(BLOWPIPE, picks.get(GearSlot.WEAPON).itemId);
		assertEquals("The body stays", ANCESTRAL_TOP, picks.get(GearSlot.BODY).itemId);
		assertNull(picks.get(GearSlot.SPECIAL));
	}

	@Test
	public void dropsMissingSwitchesWhenThereIsAReplacement() {
		var magic = result(new int[] { SHARK }, GearSlot.WEAPON, TRIDENT, "Trident of the swamp");
		magic.missingInventory.add("Toxic blowpipe");
		magic.missingInventory.add("Necklace of anguish");
		magic.missingInventory.add("Extended anti-venom+(4)");
		var ranged = result(new int[0], GearSlot.WEAPON, MASORI_BODY, "Some bow");
		var merged = HybridLoadout.merge(List.of(magic, ranged), List.of("Magic", "Ranged"), owned(),
			name -> name.equals("Toxic blowpipe") ? Integer.valueOf(3) : name.equals("Necklace of anguish") ? Integer.valueOf(2) : null);
		// There's a weapon switch now, but no necklace one
		assertEquals(List.of("Necklace of anguish", "Extended anti-venom+(4)"), merged.missingInventory);
	}

	@Test
	public void findsTheStylesALabelNames() {
		assertEquals(List.of(CombatStyle.MAGIC, CombatStyle.RANGED), CombatStyle.mentioned("Void Mage and Ranged"));
		assertEquals(List.of(CombatStyle.MAGIC, CombatStyle.RANGED), CombatStyle.mentioned("Max Magic & Ranged"));
		assertEquals(List.of(CombatStyle.MAGIC), CombatStyle.mentioned("Magic (Diary kill)"));
	}
}
