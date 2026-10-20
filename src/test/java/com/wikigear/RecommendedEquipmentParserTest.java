package com.wikigear;

import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;

public class RecommendedEquipmentParserTest {
	private static final String STRATEGY_PAGE = String.join("\n",
		"==Equipment==",
		"<tabber>",
		"Ranged=",
		"{{Recommended equipment",
		"|style = Ranged",
		"|head1 = {{plink|Armadyl helmet}}",
		"|head2 = {{plink|Blessed coif}} / {{plink|Void ranger helm}}{{efn|Only with the [[Elite void top]].}}",
		"|weapon1 = {{plink|Dragon hunter crossbow}}",
		"|2h1 = {{plink|Twisted bow}}",
		"|ammo1 = {{plink|Ruby dragon bolts (e)|pic=Ruby dragon bolts (e) 5}}",
		"|ring1 = [[Venator ring]]",
		"|ring2 = [[Ring of suffering (i)|Suffering (i)]]",
		"|special1 = {{plink|Dragon warhammer}}",
		"}}",
		"|-|",
		"Melee=",
		"{{Recommended equipment",
		"|style = Melee",
		"|head1 = {{plink|Neitiznot faceguard}}",
		"|weapon1 = {{plink|Dragon hunter lance}}",
		"}}",
		"</tabber>",
		"===Phase 2===",
		"{{Recommended equipment",
		"|style = Melee",
		"|head1 = {{plink|Torva full helm}}",
		"}}",
		"{{Recommended equipment|style=Magic}}"
	);

	@Test
	public void findsEachSetup() {
		List<GearSetup> setups = RecommendedEquipmentParser.parse(STRATEGY_PAGE);
		// The empty magic table has no items, so it's skipped
		assertEquals(3, setups.size());
		assertEquals("Ranged", setups.get(0).getLabel());
		assertEquals("Melee", setups.get(1).getLabel());
		assertEquals("Melee (Phase 2)", setups.get(2).getLabel());
	}

	@Test
	public void ranksItemsBestFirst() {
		GearSetup ranged = RecommendedEquipmentParser.parse(STRATEGY_PAGE).get(0);
		assertEquals(List.of(List.of("Armadyl helmet"), List.of("Blessed coif", "Void ranger helm")), ranged.getRanks(GearSlot.HEAD));
		assertEquals(List.of(List.of("Twisted bow")), ranged.getRanks(GearSlot.TWO_HANDED));
		assertEquals(List.of(List.of("Ruby dragon bolts (e)")), ranged.getRanks(GearSlot.AMMO));
		assertEquals(List.of(List.of("Venator ring"), List.of("Ring of suffering (i)")), ranged.getRanks(GearSlot.RING));
		assertEquals(List.of(List.of("Dragon warhammer")), ranged.getRanks(GearSlot.SPECIAL));
	}

	@Test
	public void ignoresLinksInsideFootnotes() {
		GearSetup ranged = RecommendedEquipmentParser.parse(STRATEGY_PAGE).get(0);
		assertFalse(ranged.getRanks(GearSlot.HEAD).stream().anyMatch(rank -> rank.contains("Elite void top")));
	}

	@Test
	public void usesHeadingsWhenThereIsNoStyle() {
		var setups = RecommendedEquipmentParser.parse("==Budget setup==\n{{Recommended equipment|body1={{plink|Bandos chestplate}}}}");
		assertEquals(1, setups.size());
		assertEquals("Budget setup", setups.get(0).getLabel());
	}

	@Test
	public void handlesPagesWithoutTables() {
		assertTrue(RecommendedEquipmentParser.parse("Vorkath is a dragon.").isEmpty());
		assertTrue(RecommendedEquipmentParser.parse(null).isEmpty());
	}

	@Test
	public void readsInventorySlotsAmountsAndNotes() {
		var inventory = RecommendedEquipmentParser.parseInventory(
			"Inventory|align=right|Prayer potion(4)|Shark\\3||Blood rune\\500;n|28=Teleport to house");
		assertEquals(GearSetup.INVENTORY_SIZE, inventory.size());
		assertEquals("Prayer potion(4)", inventory.get(0));
		assertEquals("Shark", inventory.get(1));
		assertNull(inventory.get(2));
		assertEquals("Blood rune", inventory.get(3));
		assertEquals("Teleport to house", inventory.get(27));
	}

	@Test
	public void attachesInventoriesToTheirSetups() {
		String page = String.join("\n",
			"<tabber>",
			"Magic=",
			"{{Recommended equipment|style=Magic|head1={{plink|Ahrim's hood}}}}",
			"{{Inventory|Trident of the seas|Prayer potion(4)}}",
			"|-|",
			"Melee=",
			"{{Recommended equipment|style=Melee|head1={{plink|Neitiznot faceguard}}}}",
			"{{Inventory|Super combat potion(4)}}",
			"</tabber>",
			"{{Inventory license}}"
		);
		var setups = RecommendedEquipmentParser.parse(page);
		assertEquals("Trident of the seas", setups.get(0).getInventoryItem(0));
		assertEquals("Prayer potion(4)", setups.get(0).getInventoryItem(1));
		assertEquals("Super combat potion(4)", setups.get(1).getInventoryItem(0));
		assertNull(setups.get(1).getInventoryItem(1));
	}

	@Test
	public void givesEveryTableAnInventoryWhenTheyAllComeFirst() {
		// Like Zulrah: the equipment tables, then an inventory section with a few options
		String page = String.join("\n",
			"==Equipment==",
			"{{Recommended equipment|style=Magic|head1={{plink|Ahrim's hood}}}}",
			"{{Recommended equipment|style=Ranged|head1={{plink|Karil's coif}}}}",
			"==Inventory==",
			"<tabber>",
			"Budget=",
			"{{Inventory|Toxic blowpipe|Shark}}",
			"|-|",
			"Max=",
			"{{Inventory|Twisted bow|Anglerfish}}",
			"</tabber>"
		);
		var setups = RecommendedEquipmentParser.parse(page);
		assertEquals("Toxic blowpipe", setups.get(0).getInventoryItem(0));
		assertEquals("Toxic blowpipe", setups.get(1).getInventoryItem(0));
	}

	@Test
	public void matchesInventoriesToTabsOfTheSameName() {
		String page = String.join("\n",
			"{{Recommended equipment|style=Magic|head1={{plink|Ahrim's hood}}}}",
			"{{Recommended equipment|style=Ranged|head1={{plink|Karil's coif}}}}",
			"<tabber>",
			"Magic=",
			"{{Inventory|Trident of the seas}}",
			"|-|",
			"Ranged=",
			"{{Inventory|Toxic blowpipe}}",
			"</tabber>"
		);
		var setups = RecommendedEquipmentParser.parse(page);
		assertEquals("Trident of the seas", setups.get(0).getInventoryItem(0));
		assertEquals("Toxic blowpipe", setups.get(1).getInventoryItem(0));
	}

	@Test
	public void readsLoadoutsLikeZulrahs() {
		String page = String.join("\n",
			"==Equipment==",
			"<tabber>",
			"Magic=",
			"{{Recommended equipment|style = Magic|head1={{plink|Ancestral hat}}}}",
			"|-|",
			"Ranged=",
			"{{Recommended equipment|style = Ranged|head1={{plink|Masori mask (f)}}}}",
			"</tabber>",
			"==Inventory Setups==",
			"<tabber>",
			"Max Magic & Ranged=",
			"{{Loadout",
			"|loadoutname = Zulrah (max hybrid)",
			"|head = Ancestral hat",
			"|weapon = Eye of Ayak",
			"|torso = Ancestral robe top",
			"|gloves = Confliction gauntlets",
			"|1=Twisted bow",
			"|2=Masori body (f)",
			"|3={{Cheap food}}",
			"|rune1 = Fire rune",
			"}}",
			"|-|",
			"Magic Only=",
			"{{Loadout",
			"|head = Ahrim's hood",
			"|1=Shark",
			"|2=Cooked karambwan",
			"}}",
			"|-|",
			"Ranged Only=",
			"{{Loadout",
			"|head = Karil's coif",
			"|1=Toxic blowpipe",
			"}}",
			"</tabber>"
		);
		var setups = RecommendedEquipmentParser.parse(page);
		assertEquals(3, setups.size());
		assertEquals("Shark", setups.get(0).getInventoryItem(0));
		assertEquals("Toxic blowpipe", setups.get(1).getInventoryItem(0));

		// The hybrid loadout becomes a setup of its own; the single-style ones match the tables instead
		var hybrid = setups.get(2);
		assertEquals("Max Magic & Ranged", hybrid.getLabel());
		assertEquals(List.of(List.of("Ancestral hat")), hybrid.getRanks(GearSlot.HEAD));
		assertEquals(List.of(List.of("Ancestral robe top")), hybrid.getRanks(GearSlot.BODY));
		assertEquals(List.of(List.of("Confliction gauntlets")), hybrid.getRanks(GearSlot.HANDS));
		assertEquals("Twisted bow", hybrid.getInventoryItem(0));
		assertEquals(InventorySubstitutes.ANY_FOOD, hybrid.getInventoryItem(2));
	}

	@Test
	public void sharesASingleInventoryWithEverySetup() {
		String page = String.join("\n",
			"{{Recommended equipment|style=Magic|head1={{plink|Ahrim's hood}}}}",
			"{{Recommended equipment|style=Ranged|head1={{plink|Karil's coif}}}}",
			"==Inventory==",
			"{{Inventory|Spade|Barrows teleport}}"
		);
		var setups = RecommendedEquipmentParser.parse(page);
		assertEquals("Spade", setups.get(0).getInventoryItem(0));
		assertEquals("Spade", setups.get(1).getInventoryItem(0));
	}

	@Test
	public void resolvesTemplatesInInventorySlots() {
		var inventory = RecommendedEquipmentParser.parseInventory(
			"Inventory|Crimson kisten|{{Cheap prayer}}|{{Cheap food}}|{{plinkp|Divine rune pouch}}|Teleport to house (tablet)|{{Unknown thing}}");
		assertEquals("Crimson kisten", inventory.get(0));
		assertEquals("Prayer potion(4)", inventory.get(1));
		assertEquals(InventorySubstitutes.ANY_FOOD, inventory.get(2));
		assertEquals("Divine rune pouch", inventory.get(3));
		assertEquals("Teleport to house (tablet)", inventory.get(4));
		assertNull(inventory.get(5));
	}
}
