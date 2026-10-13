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
}
