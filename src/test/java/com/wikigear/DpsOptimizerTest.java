package com.wikigear;

import com.google.gson.Gson;
import com.wikigear.dps.EquipmentPiece;
import com.wikigear.dps.GameData;
import com.wikigear.dps.Monster;
import com.wikigear.dps.PlayerSkills;
import com.wikigear.dps.Potion;
import com.wikigear.dps.Prayer;
import com.wikigear.dps.Spells;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class DpsOptimizerTest
{
	private static GameData data;

	@BeforeClass
	public static void loadData() throws IOException {
		data = GameData.loadBundled(new Gson());
		data.install();
	}

	private static EquipmentPiece item(String name) {
		for (var e : data.equipment)
			if (e.name.equals(name))
				return e;
		throw new AssertionError("No item named " + name);
	}

	private static Monster monster(String name) {
		for (var m : data.monsters)
			if (m.name.equals(name))
				return m;
		throw new AssertionError("No monster named " + name);
	}

	private static DpsOptimizer.Context context(String monster, String... owned) {
		var ctx = new DpsOptimizer.Context();
		ctx.monster = monster(monster);
		ctx.skills = new PlayerSkills();
		ctx.skills.atk = ctx.skills.str = ctx.skills.def = ctx.skills.ranged = ctx.skills.magic = 99;
		ctx.skills.hp = ctx.skills.prayer = 99;
		for (String name : owned) {
			var piece = item(name);
			ctx.candidates.computeIfAbsent(piece.slot, k -> new ArrayList<>()).add(piece);
		}
		return ctx;
	}

	@Test
	public void picksTheStrongestMeleeGear() {
		var ctx = context("Abyssal demon", "Bronze sword", "Abyssal whip", "Rune scimitar", "Iron platebody", "Bandos chestplate",
			"Amulet of power", "Amulet of fury", "Bronze kiteshield", "Dragon defender", "Leather gloves", "Barrows gloves");
		var result = DpsOptimizer.optimise(ctx, CombatStyle.MELEE);
		assertNotNull(result);
		assertEquals("Abyssal whip", result.equipment.weapon.name);
		assertEquals("Bandos chestplate", result.equipment.body.name);
		assertEquals("Amulet of fury", result.equipment.neck.name);
		assertEquals("Dragon defender", result.equipment.shield.name);
		assertEquals("Barrows gloves", result.equipment.hands.name);
		assertTrue(result.dps > 0);
	}

	@Test
	public void usesBestPrayersAndPotions() {
		var ctx = context("Abyssal demon", "Abyssal whip");
		var noBoosts = DpsOptimizer.optimise(ctx, CombatStyle.MELEE);

		ctx.prayers = EnumSet.of(Prayer.PIETY, Prayer.ULTIMATE_STRENGTH, Prayer.INCREDIBLE_REFLEXES);
		ctx.potions = EnumSet.of(Potion.SUPER_COMBAT, Potion.ATTACK, Potion.RANGING);
		var boosted = DpsOptimizer.optimise(ctx, CombatStyle.MELEE);

		assertEquals(List.of(Prayer.PIETY), boosted.prayers);
		assertEquals(List.of(Potion.SUPER_COMBAT), boosted.potions);
		assertTrue(boosted.dps > noBoosts.dps);
	}

	@Test
	public void stacksStrengthAndAccuracyPrayersWithoutPiety() {
		var prayers = DpsOptimizer.bestPrayers(CombatStyle.MELEE,
			EnumSet.of(Prayer.ULTIMATE_STRENGTH, Prayer.SUPERHUMAN_STRENGTH, Prayer.IMPROVED_REFLEXES));
		assertEquals(List.of(Prayer.ULTIMATE_STRENGTH, Prayer.IMPROVED_REFLEXES), prayers);
		assertEquals(List.of(Prayer.EAGLE_EYE), DpsOptimizer.bestPrayers(CombatStyle.RANGED, EnumSet.of(Prayer.HAWK_EYE, Prayer.EAGLE_EYE)));
		assertTrue(DpsOptimizer.bestPrayers(CombatStyle.MAGIC, EnumSet.noneOf(Prayer.class)).isEmpty());
	}

	@Test
	public void completesTheVoidSet() {
		var ctx = context("Abyssal demon", "Abyssal whip", "Void melee helm", "Void knight top", "Void knight robe",
			"Void knight gloves", "Leather body", "Leather chaps", "Leather gloves", "Leather cowl");
		var result = DpsOptimizer.optimise(ctx, CombatStyle.MELEE);
		assertEquals("Void melee helm", result.equipment.head.name);
		assertEquals("Void knight top", result.equipment.body.name);
		assertEquals("Void knight robe", result.equipment.legs.name);
		assertEquals("Void knight gloves", result.equipment.hands.name);
	}

	@Test
	public void needsAmmoTheWeaponCanFire() {
		// No bolts for the crossbow, so it has to be the shortbow with arrows
		var ctx = context("Abyssal demon", "Rune crossbow", "Magic shortbow", "Rune arrow", "Bronze arrow");
		var result = DpsOptimizer.optimise(ctx, CombatStyle.RANGED);
		assertEquals("Magic shortbow", result.equipment.weapon.name);
		assertEquals("Rune arrow", result.equipment.ammo.name);
		assertEquals("Rapid", result.style.stance);
	}

	@Test
	public void choosesAStaffAndSpell() {
		var ctx = context("Abyssal demon", "Staff of fire", "Mystic robe top", "Abyssal whip");
		ctx.spells = List.of(Spells.spellByName("Fire Strike"), Spells.spellByName("Fire Bolt"), Spells.spellByName("Fire Blast"));
		var result = DpsOptimizer.optimise(ctx, CombatStyle.MAGIC);
		assertEquals("Staff of fire", result.equipment.weapon.name);
		assertEquals("Fire Blast", result.spell.name);
		assertEquals("Mystic robe top", result.equipment.body.name);
	}

	@Test
	public void keepsTheSeedWhenNothingIsBetter() {
		// Two rings with no combat stats: the wiki's choice stays
		var ctx = context("Abyssal demon", "Abyssal whip", "Ring of wealth", "Ring of dueling");
		ctx.seed.put("ring", item("Ring of dueling"));
		var result = DpsOptimizer.optimise(ctx, CombatStyle.MELEE);
		assertEquals("Ring of dueling", result.equipment.ring.name);
	}

	@Test
	public void returnsNullWithoutAnyWayToAttack() {
		var ctx = context("Abyssal demon", "Rune crossbow");
		assertNull(DpsOptimizer.optimise(ctx, CombatStyle.RANGED));
	}
}
