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

import java.util.ArrayList;
import java.util.List;
import static com.wikigear.dps.Constants.*;
import static com.wikigear.dps.MathUtil.trunc;

/**
 * Base class which the other calculators extend
 */
public abstract class BaseCalc
{
	public static class CalcOpts
	{
		public boolean disableMonsterScaling;
		public boolean usingSpecialAttack;
		/**
		 * Internal-only flag for when the calc is spawned as a sub-calc of the current one, and the caller guarantees
		 * that all required properties are already sanitized and valid
		 */
		public boolean noInit;
		public Double overrideAccuracy;
		public Double overrideAttackRoll;
		public Double overrideDefenceRoll;

		public CalcOpts copy() {
			var o = new CalcOpts();
			o.disableMonsterScaling = disableMonsterScaling;
			o.usingSpecialAttack = usingSpecialAttack;
			o.noInit = noInit;
			o.overrideAccuracy = overrideAccuracy;
			o.overrideAttackRoll = overrideAttackRoll;
			o.overrideDefenceRoll = overrideDefenceRoll;
			return o;
		}
	}

	protected final CalcOpts opts;
	protected Player player;
	protected Monster monster;
	/**
	 * The monster passed in to the calculator before scaling was applied
	 */
	protected Monster baseMonster;
	/**
	 * Names of all equipped items, for quick checks
	 */
	protected List<String> allEquippedItems = new ArrayList<>();

	public final List<String> userIssues = new ArrayList<>();

	protected BaseCalc(Player player, Monster monster, CalcOpts opts) {
		this.opts = opts == null ? new CalcOpts() : opts;
		this.player = player;
		this.baseMonster = monster;
		this.monster = (this.opts.disableMonsterScaling || this.opts.noInit) ? monster : MonsterScaling.scaleMonster(monster);

		if (!this.opts.noInit) {
			canonicalizeEquipment();
			for (EquipmentPiece piece : this.player.equipment.values())
				allEquippedItems.add(piece.name == null ? "" : piece.name);
			sanitizeInputs();
		}
	}

	protected static double trackFactor(double base, double factor, double divisor) {
		return trunc(base * factor / divisor);
	}

	protected static double trackFactor(double base, double[] factor) {
		return trunc(base * factor[0] / factor[1]);
	}

	protected static double trackMaxHitFromEffective(double effectiveLevel, double gearBonus) {
		return trunc((effectiveLevel * gearBonus + 320) / 640);
	}

	protected static double trackAdd(double base, double addend) {
		return trunc(base + addend);
	}

	protected static double trackAddFactor(double base, double[] factor) {
		double addend = trunc(base * factor[0] / factor[1]);
		return trunc(base + addend);
	}

	protected void canonicalizeEquipment() {
		player = player.copy();
		player.equipment = EquipmentCalc.getCanonicalEquipment(player.equipment);
	}

	public static double getNormalAccuracyRoll(double atk, double def) {
		if (atk < 0)
			atk = Math.min(0, atk + 2);
		if (def < 0)
			def = Math.min(0, def + 2);

		if (atk >= 0 && def >= 0)
			return stdRoll(atk, def);
		if (atk >= 0 && def < 0)
			return 1 - 1 / (-def + 1) / (atk + 1);
		if (atk < 0 && def >= 0)
			return 0;
		if (atk < 0 && def < 0)
			return stdRoll(-def, -atk);
		return 0;
	}

	private static double stdRoll(double attack, double defence) {
		return attack > defence
			? 1 - ((defence + 2) / (2 * (attack + 1)))
			: attack / (2 * (defence + 1));
	}

	public static double getFangAccuracyRoll(double atk, double def) {
		if (atk < 0)
			atk = Math.min(0, atk + 2);
		if (def < 0)
			def = Math.min(0, def + 2);
		// the calculator's inner functions compare against the adjusted outer def
		final double outerDef = def;
		if (atk >= 0 && def >= 0) {
			double attack = atk, defence = def;
			return attack > outerDef
				? 1 - (defence + 2) * (2 * defence + 3) / (attack + 1) / (attack + 1) / 6
				: attack * (4 * attack + 5) / 6 / (attack + 1) / (defence + 1);
		}
		if (atk >= 0 && def < 0)
			return 1 - 1 / (-def + 1) / (atk + 1);
		if (atk < 0 && def >= 0)
			return 0;
		if (atk < 0 && def < 0) {
			double attack = -def, defence = -atk;
			return attack < outerDef
				? attack * (defence * 6 - 2 * attack + 5) / 6 / (defence + 1) / (defence + 1)
				: 1 - (defence + 2) * (2 * defence + 3) / 6 / (defence + 1) / (attack + 1);
		}
		return 0;
	}

	public static double getConflictionGauntletsAccuracyRoll(double atk, double def) {
		double singleRoll = getNormalAccuracyRoll(atk, def);
		double doubleRoll = getFangAccuracyRoll(atk, def);
		return doubleRoll / (1 + doubleRoll - singleRoll);
	}

	/**
	 * Hit chance for mechanics that force a specific attack roll, but still use a random defence roll
	 */
	public static double getFixedAttackHitChance(double atk, double def) {
		if (atk < 0)
			atk = Math.min(0, atk + 2);
		if (def < 0)
			def = Math.min(0, def + 2);

		if (atk >= 0 && def >= 0)
			return atk > def ? 1 : atk / (def + 1);
		if (atk >= 0 && def < 0)
			return 1;
		if (atk < 0 && def >= 0)
			return 0;
		if (atk < 0 && def < 0) {
			double attack = -def, defence = -atk;
			return attack > defence ? 1 : attack / (defence + 1);
		}
		return 0;
	}

	/**
	 * Whether any of the items are equipped
	 */
	protected boolean wearing(String... items) {
		for (String i : items)
			if (allEquippedItems.contains(i))
				return true;
		return false;
	}

	/**
	 * Whether all of the items are equipped
	 */
	protected boolean wearingAll(String... items) {
		for (String i : items)
			if (!allEquippedItems.contains(i))
				return false;
		return true;
	}

	protected boolean anyEquippedNameContains(String part) {
		for (String i : allEquippedItems)
			if (i.contains(part))
				return true;
		return false;
	}

	protected boolean isUsingMeleeStyle() {
		String t = player.style.type;
		return "slash".equals(t) || "crush".equals(t) || "stab".equals(t);
	}

	protected boolean isWearingVoidRobes() {
		return wearing("Void knight top", "Void knight top (or)", "Elite void top", "Elite void top (or)")
			&& wearing("Void knight robe", "Void knight robe (or)", "Elite void robe", "Elite void robe (or)")
			&& wearing("Void knight gloves");
	}

	protected boolean isWearingEliteVoidRobes() {
		return wearing("Elite void top", "Elite void top (or)")
			&& wearing("Elite void robe", "Elite void robe (or)")
			&& wearing("Void knight gloves");
	}

	protected boolean isWearingMeleeVoid() {
		return isWearingVoidRobes() && wearing("Void melee helm", "Void melee helm (or)");
	}

	protected boolean isWearingEliteRangedVoid() {
		return isWearingEliteVoidRobes() && wearing("Void ranger helm", "Void ranger helm (or)");
	}

	protected boolean isWearingEliteMagicVoid() {
		return isWearingEliteVoidRobes() && wearing("Void mage helm", "Void mage helm (or)");
	}

	protected boolean isWearingRangedVoid() {
		return isWearingVoidRobes() && wearing("Void ranger helm", "Void ranger helm (or)");
	}

	protected boolean isWearingMagicVoid() {
		return isWearingVoidRobes() && wearing("Void mage helm", "Void mage helm (or)");
	}

	protected boolean isWearingSlayerHelmet() {
		return wearing("Slayer helmet", "Slayer helmet (i)");
	}

	protected boolean isWearingBlackMask() {
		return isWearingImbuedBlackMask() || wearing("Black mask", "Slayer helmet");
	}

	protected boolean isWearingImbuedBlackMask() {
		return wearing("Black mask (i)", "Slayer helmet (i)", "V's helm");
	}

	protected boolean isWearingSmokeStaff() {
		return wearing("Smoke battlestaff", "Mystic smoke staff", "Twinflame staff");
	}

	protected boolean isWearingTzhaarWeapon() {
		return wearing("Tzhaar-ket-em", "Tzhaar-ket-om", "Tzhaar-ket-om (t)", "Toktz-xil-ak", "Toktz-xil-ek", "Toktz-mej-tal");
	}

	protected boolean isWearingObsidian() {
		return wearingAll("Obsidian helmet", "Obsidian platelegs", "Obsidian platebody");
	}

	protected boolean isWearingBerserkerNecklace() {
		return wearing("Berserker necklace", "Berserker necklace (or)");
	}

	protected boolean isWearingCrystalBow() {
		return wearing("Crystal bow") || anyEquippedNameContains("Bow of Faerdhinen");
	}

	protected boolean isWearingFang() {
		return wearing("Osmumten's fang", "Osmumten's fang (or)");
	}

	protected boolean isWearingAccursedSceptre() {
		return wearing("Accursed sceptre", "Accursed sceptre (a)");
	}

	protected boolean isWearingBlowpipe() {
		return wearing("Toxic blowpipe", "Blazing blowpipe");
	}

	protected boolean isWearingGodsword() {
		return wearing("Ancient godsword", "Armadyl godsword", "Bandos godsword", "Saradomin godsword", "Zamorak godsword");
	}

	protected boolean isWearingScythe() {
		return wearing("Scythe of Vitur") || anyEquippedNameContains("of vitur") || anyEquippedNameContains("of Vitur");
	}

	protected boolean isWearingTwoHitWeapon() {
		return wearing("Torag's hammers", "Sulphur blades", "Glacial temotli", "Earthbound tecpatl");
	}

	protected boolean isWearingKeris() {
		return anyEquippedNameContains("Keris");
	}

	protected boolean isWearingDharok() {
		return wearingAll("Dharok's helm", "Dharok's platebody", "Dharok's platelegs", "Dharok's greataxe");
	}

	protected boolean isWearingVeracs() {
		return wearingAll("Verac's helm", "Verac's brassard", "Verac's plateskirt", "Verac's flail");
	}

	protected boolean isWearingKarils() {
		return wearingAll("Karil's coif", "Karil's leathertop", "Karil's leatherskirt", "Karil's crossbow", "Amulet of the damned");
	}

	protected boolean isWearingAhrims() {
		return wearingAll("Ahrim's staff", "Ahrim's hood", "Ahrim's robetop", "Ahrim's robeskirt", "Amulet of the Damned");
	}

	protected boolean isWearingTorags() {
		return wearingAll("Torag's helm", "Torag's platebody", "Torag's platelegs", "Torag's hammers", "Amulet of the Damned");
	}

	protected boolean isWearingBloodMoonSet() {
		return wearingAll("Dual macuahuitl", "Blood moon helm", "Blood moon chestplate", "Blood moon tassets");
	}

	protected boolean isWearingSilverWeapon() {
		if ("ranged".equals(player.style.type) && wearing(
			"Silver bolts#Unpoisoned", "Silver bolts#Poison", "Silver bolts#Poison+", "Silver bolts#Poison++", "Blisterwood stake"))
			return true;

		return isUsingMeleeStyle() && wearing(
			"Blessed axe", "Ivandis flail", "Blisterwood flail", "Hallowed flail", "Silver sickle", "Silver sickle (b)",
			"Emerald sickle", "Emerald sickle (b)", "Enchanted emerald sickle (b)", "Ruby sickle (b)", "Enchanted ruby sickle (b)",
			"Blisterwood sickle", "Silverlight", "Darklight", "Arclight", "Rod of Ivandis", "Wolfbane");
	}

	/**
	 * Whether the player is using a vampyrebane weapon capable of full damage against t2 or t3 vampyres
	 */
	protected boolean wearingVampyrebane(String tier) {
		boolean t2 = MonsterAttribute.VAMPYRE_2.equals(tier);
		var weapon = player.equipment.weapon;
		if ("ranged".equals(player.style.type) && weapon != null && "Blisterwood stake".equals(weapon.name))
			return true;
		if (!(t2 || isUsingMeleeStyle()))
			return false;
		if (t2 && wearing("Rod of Ivandis"))
			return true;
		return wearing("Ivandis flail", "Blisterwood sickle", "Blisterwood flail", "Hallowed flail", "Sunspear");
	}

	protected boolean isWearingMsb() {
		return wearing("Magic shortbow", "Magic shortbow (i)");
	}

	protected boolean isWearingMlb() {
		return wearing("Magic longbow", "Magic comp bow");
	}

	protected boolean isWearingLeafBladedWeapon() {
		if (isUsingMeleeStyle() && wearing("Leaf-bladed battleaxe", "Leaf-bladed spear", "Leaf-bladed sword"))
			return true;
		if (player.spell != null && "Magic Dart".equals(player.spell.name))
			return true;
		return wearing("Broad arrows", "Broad bolts", "Amethyst broad bolts", "Seeking broad arrows")
			&& "ranged".equals(player.style.type);
	}

	protected boolean isWearingCorpbaneWeapon() {
		var weapon = player.equipment.weapon;
		boolean isStab = "stab".equals(player.style.type);
		if (weapon == null)
			return false;
		if (isWearingFang())
			return isStab;
		if (weapon.name.endsWith("halberd"))
			return isStab;
		if (weapon.name.contains("spear") && !weapon.name.equals("Blue moon spear"))
			return isStab;
		return "magic".equals(player.style.type);
	}

	protected boolean isRevWeaponBuffApplicable() {
		var weapon = player.equipment.weapon;
		if (!player.buffs.inWilderness || weapon == null || !"Charged".equals(weapon.version))
			return false;

		String type = player.style.type == null ? "" : player.style.type;
		switch (type) {
			case "magic":
				return wearing("Accursed sceptre", "Accursed sceptre (a)", "Thammaron's sceptre", "Thammaron's sceptre (a)");
			case "ranged":
				return wearing("Craw's bow", "Webweaver bow");
			default:
				return wearing("Ursine chainmace", "Viggora's chainmace");
		}
	}

	protected boolean isWearingRatBoneWeapon() {
		return wearing("Bone mace", "Bone shortbow", "Bone staff");
	}

	protected boolean isChargeSpellApplicable() {
		if (!player.buffs.chargeSpell || player.spell == null)
			return false;

		switch (player.spell.name) {
			case "Saradomin Strike":
				return wearing("Saradomin cape", "Imbued Saradomin cape", "Saradomin max cape", "Imbued Saradomin max cape");
			case "Claws of Guthix":
				return wearing("Guthix cape", "Imbued Guthix cape", "Guthix max cape", "Imbued Guthix max cape");
			case "Flames of Zamorak":
				return wearing("Zamorak cape", "Imbued Zamorak cape", "Zamorak max cape", "Imbued Zamorak max cape");
			default:
				return false;
		}
	}

	/**
	 * air, water, earth, fire or null
	 */
	protected String getSpellement() {
		return player.spell == null || player.spell.element == null || player.spell.element.isEmpty() ? null : player.spell.element;
	}

	protected boolean hasMatchingElementalAmulet() {
		String spellement = getSpellement();
		if (spellement == null)
			return false;
		if (wearing("Elemental amulet"))
			return true;
		switch (spellement) {
			case "air":
				return wearing("Amulet of air");
			case "water":
				return wearing("Amulet of water");
			case "earth":
				return wearing("Amulet of earth");
			case "fire":
				return wearing("Amulet of fire");
			default:
				return false;
		}
	}

	protected boolean isWearingJusticiarArmour() {
		return wearingAll("Justiciar faceguard", "Justiciar chestguard", "Justiciar legguards");
	}

	protected boolean isUsingDemonbane() {
		String type = player.style.type == null ? "" : player.style.type;
		switch (type) {
			case "magic":
				return player.spell != null && player.spell.name.contains("Demonbane");
			case "ranged":
				return wearing("Scorching bow");
			default:
				return wearing("Silverlight", "Darklight", "Arclight", "Emberlight", "Bone claws", "Burning claws");
		}
	}

	protected boolean isUsingAbyssal() {
		return isUsingMeleeStyle() && wearing("Abyssal bludgeon", "Abyssal dagger", "Abyssal whip", "Abyssal tentacle");
	}

	protected boolean isWearingOgreBow() {
		return wearing("Ogre bow", "Comp ogre bow");
	}

	protected boolean tdUnshieldedBonusApplies() {
		if (!"Tormented Demon".equals(monster.name) || !"Unshielded".equals(monster.inputs.phase))
			return false;

		String type = player.style.type == null ? "" : player.style.type;
		switch (type) {
			case "magic":
				return player.spell != null;
			case "ranged":
				return CombatStyles.getRangedDamageType(player.equipment.weapon.category).equals("heavy");
			case "crush":
				return true;
			default:
				return false;
		}
	}

	protected boolean isAmmoInvalid() {
		var eq = player.equipment;
		return EquipmentCalc.ammoApplicability(eq.weapon == null ? null : eq.weapon.id, eq.ammo == null ? null : eq.ammo.id)
			== EquipmentCalc.AmmoApplicability.INVALID;
	}

	protected boolean isImmuneToNormalBurns() {
		return "Normal".equals(monster.immunities.burn) || isImmuneToStrongBurns();
	}

	protected boolean isImmuneToStrongBurns() {
		return "Strong".equals(monster.immunities.burn) || has(IMMUNE_TO_BURN_DAMAGE_NPC_IDS, monster.id);
	}

	protected boolean isSlayerMonster() {
		return monster.is_slayer_monster || monster.id == -1;
	}

	protected void addIssue(String message) {
		userIssues.add(message);
	}

	protected double demonbaneVulnerability() {
		if (monster.id == -1 && monster.inputs.demonbaneVulnerability != null)
			return monster.inputs.demonbaneVulnerability;
		if ("Duke Sucellus".equals(monster.name))
			return 70;
		if (has(YAMA_IDS, monster.id))
			return 120;
		if (has(YAMA_VOID_FLARE_IDS, monster.id))
			return 200;
		if (has(ICE_DEMON_IDS, monster.id))
			return 115;
		return 100;
	}

	private void setMonsterInputs(Monster.Inputs inputs) {
		monster = monster.copy();
		monster.inputs = inputs;
	}

	private void setPlayerStyle(CombatStyle style, Spell spell) {
		player = player.copy();
		player.style = style;
		player.spell = spell;
	}

	private void clearSpell() {
		player = player.copy();
		player.spell = null;
	}

	private void sanitizeInputs() {
		var eq = player.equipment;
		String weaponName = eq.weapon == null ? "" : eq.weapon.name;

		if (monster.attributes.contains(MonsterAttribute.DEMON)) {
			// make sure demonbane effectiveness is set and uses the right value
			var inputs = monster.inputs.copy();
			inputs.demonbaneVulnerability = demonbaneVulnerability();
			setMonsterInputs(inputs);
		}

		// make sure monsterCurrentHp is set and valid
		double hp = monster.inputs.monsterCurrentHp;
		if (hp == 0 || Double.isNaN(hp) || hp > monster.skills.hp) {
			var inputs = monster.inputs.copy();
			inputs.monsterCurrentHp = monster.skills.hp;
			setMonsterInputs(inputs);
		}

		// specs are never manual cast, although the base loadout can be at the same time
		if (opts.usingSpecialAttack) {
			if ("Manual Cast".equals(player.style.stance)) {
				String category = eq.weapon == null || eq.weapon.category == null || eq.weapon.category.isEmpty()
					? EquipmentCategory.UNARMED : eq.weapon.category;
				var styles = CombatStyles.getCombatStylesForCategory(category);
				setPlayerStyle(styles.get(0), null);
			}

			// these staves use a built-in spell for their spec
			if (weaponName.equals("Accursed sceptre (a)") || weaponName.equals("Eldritch Nightmare staff") || weaponName.equals("Volatile Nightmare staff"))
				setPlayerStyle(CombatStyles.getCombatStylesForCategory(EquipmentCategory.POWERED_STAFF).get(0), null);
		}

		if (!CAST_STANCES.contains(player.style.stance))
			clearSpell();

		if (!"Manual Cast".equals(player.style.stance) && isAmmoInvalid()) {
			if (eq.ammo != null && eq.ammo.name != null && !eq.ammo.name.isEmpty())
				addIssue("This ammo does not work with your current weapon.");
			else
				addIssue("Your weapon requires ammo to use.");
		}

		// Certain spells require specific weapons to be equipped
		String spellName = player.spell == null ? null : player.spell.name;
		if (spellName != null && (
			(spellName.equals("Iban Blast") && !wearing("Iban's staff", "Iban's staff (u)"))
				|| (spellName.equals("Saradomin Strike") && !wearing("Saradomin staff", "Staff of Light"))
				|| (spellName.equals("Claws of Guthix") && !wearing("Guthix staff", "Void knight mace", "Staff of balance"))
				|| (spellName.equals("Flames of Zamarok") && !wearing("Zamorak staff", "Staff of the dead", "Toxic staff of the dead", "Thammaron's sceptre (a)", "Accursed sceptre (a)"))
				|| (spellName.equals("Magic Dart") && !wearing("Slayer's staff", "Slayer's staff (e)", "Staff of the Dead", "Toxic staff of the Dead", "Staff of Light", "Staff of Balance"))
		)) {
			clearSpell();
			addIssue("This spell needs a specific weapon equipped to cast.");
		}

		// Certain spells can only be cast on specific monsters
		if (spellName != null && (
			(spellName.contains("Demonbane") && !monster.attributes.contains(MonsterAttribute.DEMON))
				|| (spellName.equals("Crumble Undead") && !monster.attributes.contains(MonsterAttribute.UNDEAD))
		)) {
			clearSpell();
			addIssue("This spell cannot be cast on the selected monster.");
		}

		// some weapons are only available to use against certain monsters
		if ((wearing("Dawnbringer") && (!"Verzik Vitur".equals(monster.name) || monster.version == null || !monster.version.contains("Phase 1")))
			|| (wearing("Holy water") && !monster.attributes.contains(MonsterAttribute.DEMON))) {
			addIssue("This weapon cannot be used against the selected monster.");
		}
	}
}
