package com.wikigear;

import com.google.gson.Gson;
import com.wikigear.dps.GameData;
import com.wikigear.dps.Potion;
import com.wikigear.dps.Prayer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class DpsPlannerTest
{
	private static GameData data;

	@BeforeClass
	public static void loadData() throws IOException {
		data = GameData.loadBundled(new Gson());
		data.install();
	}

	private static DpsPlanner.PlayerState state(int prayer, int defence, int magic) {
		var state = new DpsPlanner.PlayerState();
		state.skills.prayer = prayer;
		state.skills.def = defence;
		state.skills.magic = magic;
		return state;
	}

	@Test
	public void unlocksPrayersByLevelAndQuest() {
		var state = state(77, 70, 99);
		var unlocked = DpsPlanner.unlockedPrayers(state);
		assertTrue(unlocked.contains(Prayer.MYSTIC_MIGHT));
		assertFalse("Piety needs Knight Waves", unlocked.contains(Prayer.PIETY));
		assertFalse("Rigour needs its scroll", unlocked.contains(Prayer.RIGOUR));

		state.knightWaves = true;
		state.rigour = true;
		unlocked = DpsPlanner.unlockedPrayers(state);
		assertTrue(unlocked.contains(Prayer.PIETY));
		assertTrue(unlocked.contains(Prayer.RIGOUR));

		assertFalse("Piety needs 70 Defence", DpsPlanner.unlockedPrayers(stateWithWaves(70, 60)).contains(Prayer.PIETY));
	}

	private static DpsPlanner.PlayerState stateWithWaves(int prayer, int defence) {
		var state = state(prayer, defence, 1);
		state.knightWaves = true;
		return state;
	}

	@Test
	public void onlyCastsSpellsThePlayerHas() {
		var names = DpsPlanner.castableSpells(data, state(1, 1, 60)).stream().map(s -> s.name).collect(Collectors.toList());
		assertTrue(names.contains("Fire Blast"));
		assertFalse(names.contains("Fire Wave"));
		assertFalse("Ancients need Desert Treasure", names.contains("Smoke Rush"));
		assertFalse("No damage", names.contains("Bind"));

		var state = state(1, 1, 94);
		state.desertTreasure = true;
		names = DpsPlanner.castableSpells(data, state).stream().map(s -> s.name).collect(Collectors.toList());
		assertTrue(names.contains("Ice Barrage"));
		assertEquals(58, DpsPlanner.spellLevel("Ice Rush"));
		assertEquals(88, DpsPlanner.spellLevel("Shadow Barrage"));
	}

	@Test
	public void findsOwnedPotionsAndRaidPotions() {
		var owned = new GearPicker.OwnedItems();
		owned.add(12695, "Super combat potion(4)", null);
		owned.add(2444, "Ranging potion(3)", null);
		owned.add(9739, "Combat potion(4)", null);
		var notes = new ArrayList<String>();
		var vorkath = DpsPlanner.findMonsters(data, "Vorkath", 5).get(0);
		var potions = DpsPlanner.potions(owned, vorkath, false, notes);
		assertTrue(potions.containsAll(List.of(Potion.SUPER_COMBAT, Potion.RANGING, Potion.ATTACK, Potion.STRENGTH)));
		assertFalse(potions.contains(Potion.MAGIC));
		assertTrue(notes.isEmpty());

		var tekton = DpsPlanner.findMonsters(data, "Tekton", 5).get(0);
		assertTrue(DpsPlanner.potions(new GearPicker.OwnedItems(), tekton, false, notes).contains(Potion.OVERLOAD_PLUS));
		assertEquals(1, notes.size());
	}

	@Test
	public void skipsUnchargedWeapons() {
		var owned = new GearPicker.OwnedItems();
		for (var e : data.equipment)
			if (e.name.equals("Tumeken's shadow"))
				owned.add(e.id, e.name + ("Uncharged".equals(e.version) ? " (uncharged)" : ""), null);
		var weapons = DpsPlanner.candidates(data, owned).get("weapon");
		assertFalse(weapons.isEmpty());
		assertTrue(weapons.stream().noneMatch(w -> "Uncharged".equals(w.version)));
	}

	@Test
	public void writesTheResultIntoTheTagPicks() {
		var owned = new GearPicker.OwnedItems();
		var whip = data.equipment.stream().filter(e -> e.name.equals("Abyssal whip")).findFirst().get();
		var sgs = data.equipment.stream().filter(e -> e.name.equals("Saradomin godsword")).findFirst().get();
		owned.add(whip.id, "Abyssal whip", null);
		owned.add(sgs.id, "Saradomin godsword", null);

		var picks = new EnumMap<GearSlot, GearPicker.Pick>(GearSlot.class);
		picks.put(GearSlot.TWO_HANDED, new GearPicker.Pick(GearSlot.TWO_HANDED, sgs.id, "Saradomin godsword", 1, 2, GearPicker.Source.WIKI));
		picks.put(GearSlot.SPECIAL, new GearPicker.Pick(GearSlot.SPECIAL, sgs.id, "Saradomin godsword", 1, 1, GearPicker.Source.WIKI));

		var result = new DpsOptimizer.Result();
		result.equipment = new com.wikigear.dps.PlayerEquipment();
		result.equipment.weapon = whip;
		DpsPlanner.applyResult(picks, result, owned);

		assertNull(picks.get(GearSlot.TWO_HANDED));
		assertEquals(whip.id, picks.get(GearSlot.WEAPON).itemId);
		assertEquals(GearPicker.Source.DPS, picks.get(GearSlot.WEAPON).source);
		assertEquals("The spec weapon stays", sgs.id, picks.get(GearSlot.SPECIAL).itemId);
	}

	@Test
	public void matchesMonstersByName() {
		assertTrue(DpsPlanner.findMonsters(data, "vorkath", 30).stream().allMatch(m -> m.name.equals("Vorkath")));
		assertFalse(DpsPlanner.findMonsters(data, "Abyssal demon", 30).isEmpty());
	}
}
