/*
 * Translated to Java from the OSRS Wiki DPS calculator, https://github.com/weirdgloop/osrs-dps-calc
 * Copyright (C) Jayden Bailey (Weird Gloop) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License along with this program, see
 * licenses/osrs-dps-calc-GPL-3.0.txt, or https://www.gnu.org/licenses/.
 */
package com.wikigear.dps;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Equipment bonuses, ammo rules and attack speed
 */
public final class EquipmentCalc
{
	public enum AmmoApplicability
	{
		/**
		 * Include the ammo slot bonuses in the equipment stats
		 */
		INCLUDED,
		/**
		 * Allow the ammo but do not include its ranged accuracy and strength bonuses
		 */
		ALLOWED,
		/**
		 * The ammo is incompatible with the weapon
		 */
		INVALID,
	}

	private static final int[] BOW_T1 = {
		882, 883, 5616, 5622, 598, 942, 33553, // Bronze arrow + variants
		884, 885, 5617, 5623, 2532, 2533, 33559, // Iron arrow + variants
		22227, 22228, 22229, 22230, // barb assault
	};
	private static final int[] CB_T1 = { 877, 878, 6061, 6062, 879, 9236 };
	private static final int[] JAVELIN = {
		825, 831, 5642, 5648, 826, 832, 5643, 5649, 827, 833, 5644, 5650, 828, 834, 5645, 5651,
		829, 835, 5646, 5652, 830, 836, 5647, 5653, 21318, 21320, 21322, 21324, 19484, 19486, 19488, 19490,
	};
	private static final int[] BOW_T5 = concat(BOW_T1, 886, 887, 5618, 5624, 2534, 2535, 33565);
	private static final int[] BOW_T20 = concat(BOW_T5, 888, 889, 5619, 5625, 2536, 2537, 33571);
	private static final int[] BOW_T30 = concat(BOW_T20, 890, 891, 5620, 5626, 2538, 2539, 33577);
	private static final int[] BOW_T40 = concat(BOW_T30, 892, 893, 5621, 5627, 78, 2540, 2541, 33583);
	private static final int[] BOW_T50 = concat(BOW_T40, 21326, 21332, 21334, 21336, 4160, 21328, 21330, 33589, 33601);
	private static final int[] BOW_T60 = concat(BOW_T50, 11212, 11227, 11228, 11229, 11217, 11222, 33595);
	private static final int[] CB_T16 = concat(CB_T1, 9139, 9286, 9293, 9300, 9335, 9237);
	private static final int[] CB_T26 = concat(CB_T16, 9140, 9287, 9294, 9301, 880, 9238, 9145, 9292, 9299, 9306);
	private static final int[] CB_T31 = concat(CB_T26, 9141, 9288, 9295, 9302, 9336, 9239);
	private static final int[] CB_T36 = concat(CB_T31, 9142, 9289, 9296, 9303, 9337, 9240, 9338, 9241);
	private static final int[] CB_T46 = concat(CB_T36, 9143, 9290, 9297, 9304, 9339, 9242, 9340, 9243);
	private static final int[] CB_T61 = concat(CB_T46, 9144, 9291, 9298, 9305, 11875, 21316, 9341, 9244, 9342, 9245);
	private static final int[] CB_T64 = concat(CB_T61, 21905, 21924, 21926, 21928, 21955, 21932, 21957, 21934, 21959, 21936,
		21961, 21938, 21963, 21940, 21965, 21942, 21967, 21944, 21969, 21946, 21971, 21948, 21973, 21950);

	/**
	 * Ranged weapon ID to the ammo it can use. Empty arrays mean the weapon takes no ammo in the ammo slot.
	 */
	private static final Map<Integer, int[]> AMMO_FOR_RANGED_WEAPONS = new HashMap<>();

	static {
		var m = AMMO_FOR_RANGED_WEAPONS;
		m.put(11708, BOW_T1); // Cursed goblin bow
		m.put(23357, BOW_T1); // Rain bow
		m.put(9705, new int[] { 9706 }); // Training bow
		m.put(841, BOW_T1); // Shortbow
		m.put(839, BOW_T1); // Longbow
		m.put(843, BOW_T5); // Oak shortbow
		m.put(845, BOW_T5); // Oak longbow
		m.put(4236, BOW_T5); // Signed oak bow
		m.put(849, BOW_T20); // Willow shortbow
		m.put(847, BOW_T20); // Willow longbow
		m.put(10280, BOW_T20); // Willow comp bow
		m.put(853, BOW_T30); // Maple shortbow
		m.put(851, BOW_T30); // Maple longbow
		m.put(2883, new int[] { 2866, 4773, 4778, 4783, 4788, 4793 }); // Ogre bow
		m.put(4827, new int[] { 2866, 4773, 4778, 4783, 4788, 4793, 4798, 4803 }); // Comp ogre bow
		m.put(857, BOW_T40); // Yew shortbow
		m.put(855, BOW_T40); // Yew longbow
		m.put(10282, BOW_T40); // Yew comp bow
		m.put(28794, BOW_T50); // Bone shortbow
		m.put(6724, BOW_T50); // Seercull
		m.put(861, BOW_T50); // Magic shortbow
		m.put(12788, BOW_T50); // Magic shortbow (i)
		m.put(859, BOW_T50); // Magic longbow
		m.put(10284, BOW_T50); // Magic comp bow
		m.put(11235, BOW_T60); // Dark bow
		m.put(27853, BOW_T60); // Dark bow (bh)
		m.put(12424, BOW_T60); // 3rd age bow
		m.put(27610, BOW_T60); // Venator bow
		m.put(27612, BOW_T60); // Venator bow (uncharged)
		m.put(20997, BOW_T60); // Twisted bow
		m.put(29591, BOW_T60); // Scorching bow
		m.put(837, CB_T1); // Crossbow
		m.put(767, CB_T1); // Phoenix crossbow
		m.put(9174, CB_T1); // Bronze crossbow
		m.put(9176, CB_T16); // Blurite crossbow
		m.put(9177, CB_T26); // Iron crossbow
		m.put(9179, CB_T31); // Steel crossbow
		m.put(9181, CB_T36); // Mithril crossbow
		m.put(9183, CB_T46); // Adamant crossbow
		m.put(9185, CB_T61); // Rune crossbow
		m.put(21902, CB_T64); // Dragon crossbow
		m.put(19478, JAVELIN); // Light ballista
		m.put(19481, JAVELIN); // Heavy ballista
		m.put(8880, concat(CB_T16, 9140, 9287, 9294, 9301, 8882)); // Dorgeshuun crossbow
		m.put(10156, new int[] { 10158, 10159 }); // Hunters' crossbow
		m.put(4734, new int[] { 4740 }); // Karil's crossbow
		m.put(21012, CB_T64); // Dragon hunter crossbow
		m.put(11785, CB_T64); // Armadyl crossbow
		m.put(26374, CB_T64); // Zaryte crossbow
		m.put(33251, CB_T64); // King's barrage
		for (int id : new int[] { 12924, 12926, 22547, 22550, 23983, 23985, 24123, 27652, 27655, 25862, 25865 })
			m.put(id, new int[0]); // Blowpipe, Craw's bow, crystal bows, Webweaver bow, Bow of Faerdhinen
		m.put(10149, new int[] { 10142 }); // Swamp lizard, Guam tar
		m.put(10146, new int[] { 10143 }); // Orange salamander, Marrentill tar
		m.put(10147, new int[] { 10144 }); // Red salamander, Tarromin tar
		m.put(10148, new int[] { 10145 }); // Black salamander, Harralander tar
		m.put(28834, new int[] { 28837 }); // Tecu salamander, Irit tar
		m.put(28869, new int[] { 28872, 28878 }); // Hunters' sunlight crossbow
		m.put(29000, new int[] { 28991 }); // Eclipse atlatl
		m.put(33245, BOW_T60); // Nature's recurve
	}

	public static final Map<String, Integer> WEAPON_SPEC_COSTS = new HashMap<>();

	static {
		var m = WEAPON_SPEC_COSTS;
		for (String n : new String[] {
			"Abyssal dagger", "Dragon dagger", "Dragon longsword", "Dragon mace", "Osmumten's fang", "Osmumten's fang (or)",
			"Dual macuahuitl", "Scorching bow", "Dragon knife", "Purging staff", "Rosewood blowpipe" })
			m.put(n, 25);
		for (String n : new String[] { "Dawnbringer", "Dragon halberd", "Crystal halberd", "Arkan blade" })
			m.put(n, 30);
		for (String n : new String[] { "Burning claws", "Magic longbow", "Magic comp bow" })
			m.put(n, 35);
		m.put("Dragon sword", 40);
		for (String n : new String[] {
			"Elder maul", "Dragon warhammer", "Bandos godsword", "Saradomin godsword", "Accursed sceptre", "Accursed sceptre (a)",
			"Arclight", "Emberlight", "Tonalztics of Ralos", "Dragon claws", "Voidwaker", "Toxic blowpipe", "Blazing blowpipe",
			"Webweaver bow", "Magic shortbow (i)", "Ancient godsword", "Armadyl godsword", "Zamorak godsword", "Abyssal bludgeon",
			"Abyssal whip", "Barrelchest anchor", "Eye of Ayak", "Crimson kisten", "Sunspear" })
			m.put(n, 50);
		for (String n : new String[] { "Magic shortbow", "Dark bow", "Eldritch Nightmare staff", "Volatile Nightmare staff", "Dragon scimitar" })
			m.put(n, 55);
		m.put("Granite hammer", 60);
		for (String n : new String[] { "Heavy ballista", "Light ballista", "Saradomin's blessed sword" })
			m.put(n, 65);
		m.put("Brine sabre", 75);
		m.put("Zaryte crossbow", 75);
		m.put("Saradomin sword", 100);
		m.put("Seercull", 100);
	}

	/**
	 * Whether the ammo is used by the weapon. Passing null for either means there's nothing in that slot.
	 */
	public static AmmoApplicability ammoApplicability(Integer weaponId, Integer ammoId) {
		int[] validAmmo = AMMO_FOR_RANGED_WEAPONS.get(weaponId == null || weaponId == 0 ? -1 : weaponId);

		// The weapon does not use ammo
		if (validAmmo == null || validAmmo.length == 0)
			return AmmoApplicability.ALLOWED;

		// weapon requires ammo, and we have one that matches the list
		if (ammoId != null && Constants.has(validAmmo, ammoId))
			return AmmoApplicability.INCLUDED;

		return AmmoApplicability.INVALID;
	}

	/**
	 * Whether the weapon takes ammo in the ammo slot at all
	 */
	public static boolean usesAmmo(int weaponId) {
		int[] validAmmo = AMMO_FOR_RANGED_WEAPONS.get(weaponId);
		return validAmmo != null && validAmmo.length > 0;
	}

	private static Integer id(EquipmentPiece piece) {
		return piece == null ? null : piece.id;
	}

	public static EquipmentPiece getCanonicalItem(EquipmentPiece piece) {
		var data = GameData.get();
		int canonicalId = data.canonicalItemId(piece.id);
		if (piece.id == canonicalId)
			return piece;

		var canonicalItem = data.equipmentById(canonicalId);
		if (canonicalItem == null)
			return piece;

		var copy = canonicalItem.copy();
		copy.itemVars = piece.itemVars == null ? new EquipmentPiece.ItemVars() : piece.itemVars.copy();
		return copy;
	}

	public static PlayerEquipment getCanonicalEquipment(PlayerEquipment input) {
		PlayerEquipment canonicalized = input;
		for (String slot : PlayerEquipment.SLOTS) {
			var v = input.get(slot);
			if (v == null)
				continue;
			var canonical = getCanonicalItem(v);
			if (v.id != canonical.id) {
				if (canonicalized == input)
					canonicalized = input.copy();
				canonicalized.set(slot, canonical);
			}
		}
		return canonicalized;
	}

	/**
	 * The player's attack speed, from their stance and equipment
	 */
	public static double calculateAttackSpeed(Player player, Monster monster) {
		var weapon = player.equipment.weapon;
		double attackSpeed = weapon != null && weapon.speed != 0 ? weapon.speed : Constants.DEFAULT_ATTACK_SPEED;
		String weaponName = weapon == null ? null : weapon.name;

		if ("ranged".equals(player.style.type) && "Rapid".equals(player.style.stance)) {
			attackSpeed -= 1;
		} else if (Constants.CAST_STANCES.contains(player.style.stance)) {
			if ("Harmonised Nightmare staff".equals(weaponName)
				&& player.spell != null && "standard".equals(player.spell.spellbook)
				&& !"Manual Cast".equals(player.style.stance)) {
				attackSpeed = 4;
			} else if ("Twinflame staff".equals(weaponName)) {
				attackSpeed = 6;
			} else {
				attackSpeed = 5;
			}
		}

		// Giant rat (Scurrius)
		if (monster.id == 7223 && !"Manual Cast".equals(player.style.stance)) {
			if ("Bone mace".equals(weaponName) || "Bone shortbow".equals(weaponName) || "Bone staff".equals(weaponName))
				attackSpeed = 1;
		}

		return Math.max(attackSpeed, 1);
	}

	/**
	 * Totals the bonuses of the player's gear, and works out their attack speed
	 */
	public static void applyEquipmentBonuses(Player player, Monster monster) {
		var bonuses = new Bonuses();
		var offensive = new StyleBonuses();
		var defensive = new StyleBonuses();

		// canonicalize all items first, otherwise ammoApplicability etc calls may return incorrect results later
		var eq = getCanonicalEquipment(player.equipment);

		for (String slot : PlayerEquipment.SLOTS) {
			var piece = eq.get(slot);
			if (piece == null)
				continue;

			// skip over ammo slot's ranged bonuses if it is not used by the bow
			boolean applyRangedStats = !"ammo".equals(piece.slot)
				|| ammoApplicability(id(eq.weapon), piece.id) == AmmoApplicability.INCLUDED;

			bonuses.str += piece.bonuses.str;
			if (applyRangedStats)
				bonuses.ranged_str += piece.bonuses.ranged_str;
			bonuses.magic_str += piece.bonuses.magic_str;
			bonuses.prayer += piece.bonuses.prayer;

			offensive.stab += piece.offensive.stab;
			offensive.slash += piece.offensive.slash;
			offensive.crush += piece.offensive.crush;
			offensive.magic += piece.offensive.magic;
			if (applyRangedStats)
				offensive.ranged += piece.offensive.ranged;

			defensive.stab += piece.defensive.stab;
			defensive.slash += piece.defensive.slash;
			defensive.crush += piece.defensive.crush;
			defensive.magic += piece.defensive.magic;
			defensive.ranged += piece.defensive.ranged;
		}

		String weaponName = eq.weapon == null ? null : eq.weapon.name;

		if (eq.weapon != null && Constants.has(Constants.BLOWPIPE_IDS, eq.weapon.id)) {
			var vars = eq.weapon.itemVars;
			var dart = vars == null || vars.blowpipeDartId == null ? null : GameData.get().equipmentById(vars.blowpipeDartId);
			if (dart != null)
				bonuses.ranged_str += dart.bonuses.ranged_str;
		}

		if ("Tumeken's shadow".equals(weaponName) && !"Manual Cast".equals(player.style.stance)) {
			double factor = Constants.has(Constants.TOMBS_OF_AMASCUT_MONSTER_IDS, monster.id) ? 4 : 3;
			bonuses.magic_str = Math.min(1000, bonuses.magic_str * factor);
			offensive.magic *= factor;
		}

		if ("Keris partisan of amascut".equals(weaponName) && !Constants.has(Constants.TOMBS_OF_AMASCUT_MONSTER_IDS, monster.id)) {
			bonuses.str -= 22;
			offensive.stab -= 50;
		}

		if ("Dinh's bulwark".equals(weaponName) || "Dinh's blazing bulwark".equals(weaponName)) {
			double defenceSum = defensive.stab + defensive.slash + defensive.crush + defensive.ranged;
			bonuses.str += Math.max(0, MathUtil.trunc((defenceSum - 800) / 12) - 38);
		}

		if (player.spell != null && "ancient".equals(player.spell.spellbook) && Constants.CAST_STANCES.contains(player.style.stance)) {
			int virtusPieces = 0;
			for (var piece : new EquipmentPiece[] { eq.head, eq.body, eq.legs })
				if (piece != null && piece.name.contains("Virtus"))
					virtusPieces++;
			bonuses.magic_str += 30 * virtusPieces;
		}

		// void mage is a visible bonus of 5%
		if (eq.head != null && "Void mage helm".equals(eq.head.name)
			&& eq.body != null && "Elite void top".equals(eq.body.name)
			&& eq.legs != null && "Elite void robe".equals(eq.legs.name)
			&& eq.hands != null && "Void knight gloves".equals(eq.hands.name)) {
			bonuses.magic_str += 50;
		}

		var cape = eq.cape;
		boolean dizanasQuiverCharged = cape != null && ("Dizana's max cape".equals(cape.name)
			|| "Blessed Dizana's quiver".equals(cape.name)
			|| ("Dizana's quiver".equals(cape.name) && "Charged".equals(cape.version)));
		if (dizanasQuiverCharged
			&& ammoApplicability(id(player.equipment.weapon), id(player.equipment.ammo)) == AmmoApplicability.INCLUDED) {
			offensive.ranged += 10;
			bonuses.ranged_str += 1;
		}

		player.bonuses = bonuses;
		player.offensive = offensive;
		player.defensive = defensive;
		player.attackSpeed = calculateAttackSpeed(player, monster);
	}

	private static int[] concat(int[] base, int... extra) {
		return IntStream.concat(IntStream.of(base), IntStream.of(extra)).toArray();
	}

	private EquipmentCalc() {
	}
}
