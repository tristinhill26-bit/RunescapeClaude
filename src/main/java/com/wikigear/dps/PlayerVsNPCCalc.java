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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static com.wikigear.dps.Constants.*;
import static com.wikigear.dps.MathUtil.trunc;

/**
 * Computes player-vs-NPC metrics, such as max hit, accuracy, DPS and time to kill
 */
public class PlayerVsNPCCalc extends BaseCalc
{
	public enum FeatureStatus
	{
		IMPLEMENTED,
		PARTIALLY_IMPLEMENTED,
		UNIMPLEMENTED,
		NOT_APPLICABLE,
	}

	private static final List<String> PARTIALLY_IMPLEMENTED_SPECS = List.of("Ancient godsword");

	private static final List<String> UNIMPLEMENTED_SPECS = List.of(
		"Abyssal tentacle", "Ancient mace", "Armadyl crossbow", "Blue moon spear", "Bone dagger", "Brine sabre", "Darklight",
		"Dinh's bulwark", "Dorgeshuun crossbow", "Dragon 2h sword", "Dragon crossbow", "Dragon hasta", "Dragon spear",
		"Dragon thrownaxe", "Eclipse atlatl", "Excalibur", "Granite maul", "Rune claws", "Staff of balance", "Staff of light",
		"Staff of the dead", "Toxic staff of the dead", "Ursine chainmace", "Zamorakian hasta", "Zamorakian spear"
	);

	private AttackDistribution memoizedDist;

	public PlayerVsNPCCalc(Player player, Monster monster) {
		this(player, monster, new CalcOpts());
	}

	public PlayerVsNPCCalc(Player player, Monster monster, CalcOpts opts) {
		super(player, monster, opts);
		if (!this.opts.noInit && isSpecSupported() == FeatureStatus.UNIMPLEMENTED)
			addIssue("This loadout's weapon special attack is not yet supported in the calculator.");
	}

	/**
	 * The NPC defence roll for this loadout, which is based on the player's current combat style
	 */
	public double getNPCDefenceRoll() {
		if (opts.overrideDefenceRoll != null)
			return opts.overrideDefenceRoll;

		String defenceStyle = player.style.type;
		if (opts.usingSpecialAttack) {
			if (wearing("Dragon claws", "Dragon dagger", "Dragon halberd", "Dragon longsword", "Dragon scimitar", "Crystal halberd",
				"Abyssal dagger", "Saradomin sword", "Arkan blade") || isWearingGodsword()) {
				defenceStyle = "slash";
			} else if (wearing("Arclight", "Emberlight", "Dragon sword")) {
				defenceStyle = "stab";
			} else if (wearing("Voidwaker", "Saradomin's blessed sword")) {
				defenceStyle = "magic";
			} else if (wearing("Dragon mace", "Crimson kisten")) {
				defenceStyle = "crush";
			}
		}

		double level = "magic".equals(defenceStyle) && !has(USES_DEFENCE_LEVEL_FOR_MAGIC_DEFENCE_NPC_IDS, monster.id)
			? monster.skills.magic
			: monster.skills.def;
		double effectiveLevel = trackAdd(level, 9);

		double bonus;
		if ("ranged".equals(defenceStyle)) {
			String rangedType = CombatStyles.getRangedDamageType(player.equipment.weapon.category);
			bonus = rangedType.equals("mixed")
				? trunc((monster.defensive.light + monster.defensive.standard + monster.defensive.heavy) / 3)
				: monster.defensive.get(rangedType);
		} else {
			bonus = monster.defensive.get(defenceStyle == null ? "crush" : defenceStyle);
		}

		double statBonus = trackAdd(defenceStyle != null ? bonus : 0, 64);
		double defenceRoll = trackFactor(effectiveLevel, statBonus, 1);

		boolean isCustomMonster = monster.id == -1;
		double invocation = monster.inputs.toaInvocationLevel;
		if (((has(TOMBS_OF_AMASCUT_MONSTER_IDS, monster.id) && !has(KEPHRI_OVERLORD_IDS, monster.id)) || isCustomMonster)
			&& invocation != 0 && !Double.isNaN(invocation)) {
			defenceRoll = trackFactor(defenceRoll, 250 + invocation, 250);
		}

		return defenceRoll;
	}

	private double getPlayerMaxMeleeAttackRoll() {
		var style = player.style;

		double effectiveLevel = trackAdd(player.skills.atk, player.boosts.atk);
		for (Prayer p : getCombatPrayers(PrayerFilter.ACCURACY))
			effectiveLevel = trackFactor(effectiveLevel, p.factorAccuracy);

		double stanceBonus = 8;
		if ("Accurate".equals(style.stance))
			stanceBonus += 3;
		else if ("Controlled".equals(style.stance))
			stanceBonus += 1;

		effectiveLevel = trackAdd(effectiveLevel, stanceBonus);

		if (isWearingMeleeVoid())
			effectiveLevel = trackFactor(effectiveLevel, 11, 10);

		double gearBonus = trackAdd(style.type != null ? player.offensive.get(style.type) : 0, 64);
		double baseRoll = trackFactor(effectiveLevel, gearBonus, 1);
		double attackRoll = baseRoll;
		var mattrs = monster.attributes;
		var buffs = player.buffs;

		// These bonuses do not stack with each other
		if (wearing("Amulet of avarice") && monster.name.startsWith("Revenant")) {
			attackRoll = trackFactor(attackRoll, buffs.forinthrySurge ? 27 : 24, 20);
		} else if (wearing("Salve amulet (e)", "Salve amulet(ei)") && mattrs.contains(MonsterAttribute.UNDEAD)) {
			attackRoll = trackFactor(attackRoll, 6, 5);
		} else if (wearing("Salve amulet", "Salve amulet(i)") && mattrs.contains(MonsterAttribute.UNDEAD)) {
			attackRoll = trackFactor(attackRoll, 7, 6);
		} else if (isWearingBlackMask() && isSlayerMonster() && buffs.onSlayerTask) {
			attackRoll = trackFactor(attackRoll, 7, 6);
		}

		if (isWearingTzhaarWeapon() && isWearingObsidian()) {
			double obsidianBonus = trackFactor(baseRoll, 1, 10);
			attackRoll = trackAdd(attackRoll, obsidianBonus);
		}

		if (isRevWeaponBuffApplicable())
			attackRoll = trackFactor(attackRoll, 3, 2);
		if (wearing("Arclight", "Emberlight") && mattrs.contains(MonsterAttribute.DEMON))
			attackRoll = trackAddFactor(attackRoll, demonbaneFactor(70));
		if (wearing("Silverlight", "Darklight") && mattrs.contains(MonsterAttribute.DEMON))
			attackRoll = trackAddFactor(attackRoll, demonbaneFactor(60));
		if (wearing("Bone claws", "Burning claws") && mattrs.contains(MonsterAttribute.DEMON))
			attackRoll = trackAddFactor(attackRoll, demonbaneFactor(5));
		if (mattrs.contains(MonsterAttribute.DRAGON)) {
			if (wearing("Dragon hunter lance"))
				attackRoll = trackFactor(attackRoll, 6, 5);
			else if (wearing("Dragon hunter wand"))
				attackRoll = trackFactor(attackRoll, 7, 4);
		}
		if (wearing("Keris partisan of breaching") && mattrs.contains(MonsterAttribute.KALPHITE))
			attackRoll = trackFactor(attackRoll, 133, 100);
		if (wearing("Keris partisan of the sun")
			&& has(TOMBS_OF_AMASCUT_MONSTER_IDS, monster.id)
			&& monster.inputs.monsterCurrentHp < trunc(monster.skills.hp / 4))
			attackRoll = trackFactor(attackRoll, 5, 4);
		if (wearing("Blisterwood flail", "Blisterwood sickle") && MonsterAttribute.isVampyre(mattrs))
			attackRoll = trackFactor(attackRoll, 105, 100);
		if (wearing("Hallowed flail", "Sunspear") && MonsterAttribute.isVampyre(mattrs))
			attackRoll = trackFactor(attackRoll, 125, 100);
		if (isWearingSilverWeapon() && wearing("Efaritay's aid") && MonsterAttribute.isVampyre(mattrs))
			attackRoll = trackFactor(attackRoll, 23, 20);

		if (wearing("Granite hammer") && mattrs.contains(MonsterAttribute.GOLEM))
			attackRoll = trackFactor(attackRoll, 13, 10);

		// Inquisitor's armour set gives bonuses when using the crush attack style
		if ("crush".equals(style.type)) {
			double inqBonus = inquisitorBonus();
			if (inqBonus > 0)
				attackRoll = trackFactor(attackRoll, 200 + inqBonus, 200);
		}

		if (opts.usingSpecialAttack) {
			if (isWearingGodsword()) {
				attackRoll = trackFactor(attackRoll, 2, 1);
			} else if (isWearingFang() || wearing("Arkan blade") || wearing("Granite hammer")) {
				attackRoll = trackFactor(attackRoll, 3, 2);
			} else if (wearing("Elder maul", "Dragon mace", "Dragon sword", "Dragon scimitar", "Abyssal whip")) {
				attackRoll = trackFactor(attackRoll, 5, 4);
			} else if (wearing("Dragon dagger")) {
				attackRoll = trackFactor(attackRoll, 23, 20);
			} else if (wearing("Abyssal dagger")) {
				attackRoll = trackFactor(attackRoll, 5, 4);
			} else if (wearing("Soulreaper axe")) {
				double stacks = Math.max(0, Math.min(5, buffs.soulreaperStacks));
				attackRoll = trackFactor(attackRoll, 100 + 12 * stacks, 100);
			} else if (wearing("Brine sabre")) {
				attackRoll = trackFactor(attackRoll, 2, 1);
			} else if (wearing("Barrelchest anchor")) {
				attackRoll = trackFactor(attackRoll, 2, 1);
			} else if (isSunspearFinisher()) {
				attackRoll = trackFactor(attackRoll, 7, 10);
			}
		}

		return attackRoll;
	}

	private double inquisitorBonus() {
		double inqBonus = 0;
		if (wearing("Inquisitor's great helm"))
			inqBonus += 1;
		if (wearing("Inquisitor's hauberk"))
			inqBonus += 2;
		if (wearing("Inquisitor's plateskirt"))
			inqBonus += 2;
		return inqBonus;
	}

	private double crystalPieces() {
		return (wearing("Crystal helm") ? 1 : 0) + (wearing("Crystal legs") ? 2 : 0) + (wearing("Crystal body") ? 3 : 0);
	}

	/**
	 * The player's min and max melee hit
	 */
	private double[] getPlayerMaxMeleeHit() {
		var style = player.style;
		var buffs = player.buffs;

		double baseLevel = trackAdd(player.skills.str, player.boosts.str);
		double effectiveLevel = baseLevel;

		for (Prayer p : getCombatPrayers(PrayerFilter.STRENGTH)) {
			if (p == Prayer.BURST_OF_STRENGTH && effectiveLevel <= 20)
				effectiveLevel = trackAdd(effectiveLevel, 1);
			else
				effectiveLevel = trackFactor(effectiveLevel, p.factorStrength);
		}

		if (wearing("Soulreaper axe") && !opts.usingSpecialAttack) {
			// does not stack multiplicatively with prayers
			double stacks = Math.max(0, Math.min(5, buffs.soulreaperStacks));
			double bonus = trackFactor(baseLevel, stacks * 6, 100);
			effectiveLevel = trackAdd(effectiveLevel, bonus);
		}

		double stanceBonus = 8;
		if ("Aggressive".equals(style.stance))
			stanceBonus += 3;
		else if ("Controlled".equals(style.stance))
			stanceBonus += 1;

		effectiveLevel = trackAdd(effectiveLevel, stanceBonus);

		if (isWearingMeleeVoid())
			effectiveLevel = trackFactor(effectiveLevel, 11, 10);

		double gearBonus = trackAdd(player.bonuses.str, 64);
		double baseMax = trackMaxHitFromEffective(effectiveLevel, gearBonus);
		double minHit = 0;
		double maxHit = baseMax;

		if (wearing("Crystal blessing"))
			maxHit = trunc(maxHit * (40 + crystalPieces()) / 40);

		var mattrs = monster.attributes;

		// These bonuses do not stack with each other
		if (wearing("Amulet of avarice") && monster.name.startsWith("Revenant")) {
			maxHit = trackFactor(maxHit, buffs.forinthrySurge ? 27 : 24, 20);
		} else if (wearing("Salve amulet (e)", "Salve amulet(ei)") && mattrs.contains(MonsterAttribute.UNDEAD)) {
			maxHit = trackFactor(maxHit, 6, 5);
		} else if (wearing("Salve amulet", "Salve amulet(i)") && mattrs.contains(MonsterAttribute.UNDEAD)) {
			maxHit = trackFactor(maxHit, 7, 6);
		} else if (isWearingBlackMask() && isSlayerMonster() && buffs.onSlayerTask) {
			maxHit = trackFactor(maxHit, 7, 6);
		}

		if (wearing("Arclight", "Emberlight") && mattrs.contains(MonsterAttribute.DEMON))
			maxHit = trackAddFactor(maxHit, demonbaneFactor(70));
		if (wearing("Bone claws", "Burning claws") && mattrs.contains(MonsterAttribute.DEMON))
			maxHit = trackAddFactor(maxHit, demonbaneFactor(5));
		if (isWearingTzhaarWeapon() && isWearingObsidian()) {
			double obsidianBonus = trackFactor(baseMax, 1, 10);
			maxHit = trackAdd(maxHit, obsidianBonus);
		}
		if (wearing("Dragon hunter lance") && mattrs.contains(MonsterAttribute.DRAGON))
			maxHit = trackFactor(maxHit, 6, 5);
		if (wearing("Dragon hunter wand") && mattrs.contains(MonsterAttribute.DRAGON))
			maxHit = trackFactor(maxHit, 7, 5);
		if (isWearingKeris() && mattrs.contains(MonsterAttribute.KALPHITE)) {
			if (wearing("Keris partisan of amascut"))
				maxHit = trackFactor(maxHit, 115, 100);
			else
				maxHit = trackFactor(maxHit, 133, 100);
		}
		if (wearing("Barronite mace") && mattrs.contains(MonsterAttribute.GOLEM))
			maxHit = trackFactor(maxHit, 23, 20);
		if (wearing("Granite hammer") && mattrs.contains(MonsterAttribute.GOLEM))
			maxHit = trackFactor(maxHit, 13, 10);
		if (isRevWeaponBuffApplicable())
			maxHit = trackFactor(maxHit, 3, 2);
		if (wearing("Silverlight", "Darklight", "Silverlight (dyed)") && mattrs.contains(MonsterAttribute.DEMON))
			maxHit = trackAddFactor(maxHit, demonbaneFactor(60));
		if (wearing("Leaf-bladed battleaxe") && mattrs.contains(MonsterAttribute.LEAFY))
			maxHit = trackFactor(maxHit, 47, 40);
		if (wearing("Colossal blade"))
			maxHit = trackAdd(maxHit, Math.min(monster.size * 2, 10));

		if (isWearingRatBoneWeapon() && mattrs.contains(MonsterAttribute.RAT)) {
			// applies before inq, tested 2024-01-25, str level 99 str gear 112
			maxHit = trackAdd(maxHit, 10);
		}
		// Inquisitor's armour set gives bonuses when using the crush attack style
		if ("crush".equals(style.type)) {
			double inqBonus = inquisitorBonus();
			if (inqBonus > 0)
				maxHit = trackFactor(maxHit, 200 + inqBonus, 200);
		}

		if (isWearingFang()) {
			double shrink = trunc(maxHit * 3 / 20);
			minHit = shrink;
			// not reduced during spec, but min hit is changed as usual
			if (!opts.usingSpecialAttack)
				maxHit = trackAdd(maxHit, -shrink);
		}

		if (opts.usingSpecialAttack) {
			if (isWearingGodsword())
				maxHit = trackFactor(maxHit, 11, 10);

			if (wearing("Bandos godsword", "Saradomin sword")) {
				maxHit = trackFactor(maxHit, 11, 10);
			} else if (wearing("Armadyl godsword", "Dragon sword", "Dragon longsword", "Saradomin's blessed sword")) {
				maxHit = trackFactor(maxHit, 5, 4);
			} else if (wearing("Dragon mace", "Dragon warhammer", "Arkan blade")) {
				maxHit = trackFactor(maxHit, 3, 2);
			} else if (wearing("Voidwaker")) {
				minHit = trackFactor(maxHit, 1, 2);
				maxHit = trackAdd(maxHit, minHit);
			} else if (wearing("Dragon halberd", "Crystal halberd")) {
				maxHit = trackFactor(maxHit, 11, 10);
			} else if (wearing("Dragon dagger")) {
				maxHit = trackFactor(maxHit, 23, 20);
			} else if (wearing("Abyssal dagger")) {
				maxHit = trackFactor(maxHit, 17, 20);
			} else if (wearing("Abyssal bludgeon")) {
				double prayerMissing = Math.max(-player.boosts.prayer, 0);
				maxHit = trackFactor(maxHit, 100 + (prayerMissing / 2), 100);
			} else if (wearing("Barrelchest anchor")) {
				maxHit = trackFactor(maxHit, 110, 100);
			} else if (isWearingBloodMoonSet()) {
				minHit = trackFactor(maxHit, 1, 4);
				maxHit = trackAdd(maxHit, minHit);
			} else if (wearing("Soulreaper axe")) {
				double stacks = Math.max(0, Math.min(5, player.buffs.soulreaperStacks));
				minHit = trackFactor(maxHit, 6 * stacks, 100);
				maxHit = trackFactor(maxHit, 100 + 6 * stacks, 100);
			}
		}

		if ("Respiratory system".equals(monster.name))
			minHit = trackAdd(minHit, trunc(maxHit / 2));

		if (monster.version != null && monster.version.contains("Glyphic Attenuation"))
			maxHit = trackFactor(maxHit, getGlyphicAttenuationFactor());

		return new double[] { minHit, maxHit };
	}

	private double getPlayerMaxRangedAttackRoll() {
		var style = player.style;

		double effectiveLevel = player.skills.ranged + player.boosts.ranged;
		for (Prayer p : getCombatPrayers(PrayerFilter.ACCURACY))
			effectiveLevel = trackFactor(effectiveLevel, p.factorAccuracy);

		if ("Accurate".equals(style.stance))
			effectiveLevel += 3;
		effectiveLevel += 8;

		if (isWearingRangedVoid())
			effectiveLevel = trunc(effectiveLevel * 11 / 10);

		double attackRoll = effectiveLevel * (player.offensive.ranged + 64);

		if (isWearingCrystalBow())
			attackRoll = trunc(attackRoll * (20 + crystalPieces()) / 20);

		var mattrs = monster.attributes;
		var buffs = player.buffs;

		if (wearing("Amulet of avarice") && monster.name.startsWith("Revenant")) {
			attackRoll = trackFactor(attackRoll, buffs.forinthrySurge ? 27 : 24, 20);
		} else if (wearing("Salve amulet(ei)") && mattrs.contains(MonsterAttribute.UNDEAD)) {
			attackRoll = trunc(attackRoll * 6 / 5);
		} else if (wearing("Salve amulet(i)") && mattrs.contains(MonsterAttribute.UNDEAD)) {
			attackRoll = trunc(attackRoll * 7 / 6);
		} else if (isWearingImbuedBlackMask() && isSlayerMonster() && buffs.onSlayerTask) {
			attackRoll = trunc(attackRoll * 23 / 20);
		}

		if (wearing("Twisted bow")) {
			double cap = mattrs.contains(MonsterAttribute.XERICIAN) ? 350 : 250;
			double tbowMagic = Math.min(cap, Math.max(monster.skills.magic, monster.offensive.magic));
			attackRoll = tbowScaling(attackRoll, tbowMagic, true);
			// Game update on 2023-06-21 caused this bonus to be applied twice at P2 Wardens
			if (has(P2_WARDEN_IDS, monster.id))
				attackRoll = tbowScaling(attackRoll, tbowMagic, true);
		}
		if (isRevWeaponBuffApplicable())
			attackRoll = trunc(attackRoll * 3 / 2);
		if (wearing("Dragon hunter crossbow") && mattrs.contains(MonsterAttribute.DRAGON))
			attackRoll = trunc(attackRoll * 13 / 10);
		var weapon = player.equipment.weapon;
		if (weapon != null && EquipmentCategory.CHINCHOMPA.equals(weapon.category)) {
			double distance = Math.min(7, Math.max(1, buffs.chinchompaDistance));
			double numerator = 4;
			if ("Short fuse".equals(style.name)) {
				if (distance >= 7)
					numerator = 2;
				else if (distance >= 4)
					numerator = 3;
			} else if ("Medium fuse".equals(style.name)) {
				if (distance < 4 || distance >= 7)
					numerator = 3;
			} else if ("Long fuse".equals(style.name)) {
				if (distance < 4)
					numerator = 2;
				else if (distance < 7)
					numerator = 3;
			}
			attackRoll = trackFactor(attackRoll, numerator, 4);
		}

		if (wearing("Scorching bow") && mattrs.contains(MonsterAttribute.DEMON))
			attackRoll = trackAddFactor(attackRoll, demonbaneFactor(30));

		if (opts.usingSpecialAttack) {
			if (wearing("Zaryte crossbow", "Webweaver bow") || isWearingBlowpipe())
				attackRoll = trackFactor(attackRoll, 2, 1);
			else if (isWearingMsb())
				attackRoll = trackFactor(attackRoll, 10, 7);
			else if (wearing("Heavy ballista", "Light ballista"))
				attackRoll = trackFactor(attackRoll, 5, 4);
			else if (wearing("Tonalztics of Ralos"))
				attackRoll = trackFactor(attackRoll, 150, 100);
		}

		if (has(TITAN_BOSS_IDS, monster.id) && "Out of Melee Range".equals(monster.inputs.phase))
			attackRoll = trackFactor(attackRoll, 6, 1);

		return attackRoll;
	}

	/**
	 * The player's min and max ranged hit
	 */
	private double[] getPlayerMaxRangedHit() {
		var style = player.style;

		double effectiveLevel = player.skills.ranged + player.boosts.ranged;
		boolean scalesWithStr = wearing("Eclipse atlatl", "Hunter's spear");
		if (scalesWithStr) {
			// atlatl uses strength instead of ranged skill, melee strength bonus, and melee buff from slayer helmet/salve,
			// but works with ranged void
			effectiveLevel = player.skills.str + player.boosts.str;
		}

		if (wearing("Holy water")) {
			// can't be used against non-demons
			if (!monster.attributes.contains(MonsterAttribute.DEMON))
				return new double[] { 0, 0 };

			// similar to msb + mlb + seercull below
			effectiveLevel = trackAdd(effectiveLevel, 10);
			double str = 64 + player.equipment.weapon.bonuses.ranged_str;
			double maxHit = trackMaxHitFromEffective(effectiveLevel, str);
			maxHit = trackAddFactor(maxHit, demonbaneFactor(60));
			if ("Nezikchened".equals(monster.name))
				maxHit = trackAdd(maxHit, 5);
			return new double[] { 0, maxHit };
		}

		if ((opts.usingSpecialAttack && (isWearingMsb() || isWearingMlb() || wearing("Seercull"))) || isWearingOgreBow()) {
			// why +10 when that's not used anywhere else? who knows
			effectiveLevel += 10;
			// ignores all other gear and bonuses
			var ammo = player.equipment.ammo;
			double bonusStr = ammo == null ? 0 : ammo.bonuses.ranged_str;
			return new double[] { 0, trunc((effectiveLevel * (bonusStr + 64) + 320) / 640) };
		}

		for (Prayer p : getCombatPrayers(PrayerFilter.STRENGTH)) {
			if (p == Prayer.SHARP_EYE && effectiveLevel <= 20)
				effectiveLevel = trackAdd(effectiveLevel, 1); // force 1 level gain
			else
				effectiveLevel = trackFactor(effectiveLevel, p.factorStrength);
		}

		if ("Accurate".equals(style.stance))
			effectiveLevel += 3;
		effectiveLevel += 8;

		if (isWearingEliteRangedVoid())
			effectiveLevel = trunc(effectiveLevel * 9 / 8);
		else if (isWearingRangedVoid())
			effectiveLevel = trunc(effectiveLevel * 11 / 10);

		double bonusStr = scalesWithStr ? player.bonuses.str : player.bonuses.ranged_str;
		double baseMax = trackMaxHitFromEffective(effectiveLevel, 64 + bonusStr);
		double minHit = 0;
		double maxHit = baseMax;

		// tested in-game: max hit would be 1 higher if placed after slayer helm
		if (isWearingCrystalBow())
			maxHit = trunc(maxHit * (40 + crystalPieces()) / 40);

		var mattrs = monster.attributes;
		boolean needRevWeaponBonus = isRevWeaponBuffApplicable();
		boolean needDragonbane = wearing("Dragon hunter crossbow") && mattrs.contains(MonsterAttribute.DRAGON);
		boolean needDemonbane = wearing("Scorching bow") && mattrs.contains(MonsterAttribute.DEMON);

		var buffs = player.buffs;
		if (wearing("Amulet of avarice") && monster.name.startsWith("Revenant")) {
			maxHit = trackFactor(maxHit, buffs.forinthrySurge ? 27 : 24, 20);
		} else if ((wearing("Salve amulet(ei)") || (scalesWithStr && wearing("Salve amulet (e)"))) && mattrs.contains(MonsterAttribute.UNDEAD)) {
			maxHit = trunc(maxHit * 6 / 5);
		} else if ((wearing("Salve amulet(i)") || (scalesWithStr && wearing("Salve amulet"))) && mattrs.contains(MonsterAttribute.UNDEAD)) {
			maxHit = trunc(maxHit * 7 / 6);
		} else if (scalesWithStr && isWearingBlackMask() && isSlayerMonster() && buffs.onSlayerTask) {
			maxHit = trunc(maxHit * 7 / 6);
		} else if (isWearingImbuedBlackMask() && isSlayerMonster() && buffs.onSlayerTask) {
			double numerator = 23;
			// these are additive with slayer only
			if (needRevWeaponBonus) {
				needRevWeaponBonus = false;
				numerator += 10;
			}
			if (needDragonbane) {
				needDragonbane = false;
				numerator += 5;
			}
			if (needDemonbane) {
				needDemonbane = false;
				numerator += 6;
			}
			maxHit = trackFactor(maxHit, numerator, 20);
		}

		if (wearing("Twisted bow")) {
			double cap = mattrs.contains(MonsterAttribute.XERICIAN) ? 350 : 250;
			double tbowMagic = Math.min(cap, Math.max(monster.skills.magic, monster.offensive.magic));
			maxHit = tbowScaling(maxHit, tbowMagic, false);
		}

		// multiplicative if not with slayer helm
		if (needRevWeaponBonus)
			maxHit = trunc(maxHit * 3 / 2);
		if (needDragonbane)
			maxHit = trunc(maxHit * 5 / 4);
		if (needDemonbane)
			maxHit = trackAddFactor(maxHit, demonbaneFactor(30));

		if (isWearingRatBoneWeapon() && mattrs.contains(MonsterAttribute.RAT))
			maxHit = trackAdd(maxHit, 10);

		// rolls 75% of max hit, but can hit twice, which is implemented in the hit distribution
		if (wearing("Tonalztics of Ralos"))
			maxHit = trackFactor(maxHit, 3, 4);

		if (opts.usingSpecialAttack) {
			if (isWearingBlowpipe()) {
				maxHit = trackFactor(maxHit, 3, 2);
			} else if (wearing("Webweaver bow")) {
				double maxReduction = trunc(maxHit * 6 / 10);
				maxHit = trackAdd(maxHit, -maxReduction);
			} else if (wearing("Heavy ballista", "Light ballista")) {
				maxHit = trackFactor(maxHit, 5, 4);
			}

			if (wearing("Dark bow")) {
				boolean descentOfDragons = wearing("Dragon arrow");
				minHit = descentOfDragons ? 8 : 5;
				maxHit = trackFactor(maxHit, descentOfDragons ? 15 : 13, 10);
			}
		}

		if (has(P2_WARDEN_IDS, monster.id)) {
			double[] r = applyP2WardensDamageModifier(maxHit);
			minHit = r[0];
			maxHit = r[1];
		}

		if ("Respiratory system".equals(monster.name))
			minHit = trackAdd(minHit, trunc(maxHit / 2));

		if (monster.version != null && monster.version.contains("Glyphic Attenuation"))
			maxHit = trackFactor(maxHit, getGlyphicAttenuationFactor());

		return new double[] { minHit, maxHit };
	}

	private double getPlayerMaxMagicAttackRoll() {
		var style = player.style;

		double effectiveLevel = player.skills.magic + player.boosts.magic;
		for (Prayer p : getCombatPrayers(PrayerFilter.ACCURACY))
			effectiveLevel = trackFactor(effectiveLevel, p.factorAccuracy);

		if ("Accurate".equals(style.stance))
			effectiveLevel += 2;
		effectiveLevel += 9;

		if (isWearingMagicVoid())
			effectiveLevel = trunc(effectiveLevel * 29 / 20);

		var mattrs = monster.attributes;
		var buffs = player.buffs;
		double magicBonus = player.offensive.magic;

		double baseRoll = effectiveLevel * (magicBonus + 64);
		double attackRoll = baseRoll;

		double additiveBonus = 0;
		boolean blackMaskBonus = false;
		if (wearing("Amulet of avarice") && monster.name.startsWith("Revenant")) {
			additiveBonus = trackAdd(additiveBonus, buffs.forinthrySurge ? 35 : 20);
		} else if (wearing("Salve amulet(ei)") && mattrs.contains(MonsterAttribute.UNDEAD)) {
			additiveBonus = trackAdd(additiveBonus, 20);
		} else if (wearing("Salve amulet(i)") && mattrs.contains(MonsterAttribute.UNDEAD)) {
			additiveBonus = trackAdd(additiveBonus, 15);
		} else if (isWearingImbuedBlackMask() && isSlayerMonster() && buffs.onSlayerTask) {
			blackMaskBonus = true;
		}

		if (wearing("Efaritay's aid") && MonsterAttribute.isVampyre(mattrs) && isWearingSilverWeapon())
			additiveBonus = trackAdd(additiveBonus, 15);

		if (isWearingSmokeStaff() && player.spell != null && "standard".equals(player.spell.spellbook))
			additiveBonus = trackAdd(additiveBonus, 10);

		if (additiveBonus != 0)
			attackRoll = trackFactor(attackRoll, 100 + additiveBonus, 100);

		if (mattrs.contains(MonsterAttribute.DRAGON)) {
			// this still applies to dhl and dhcb when autocasting
			if (wearing("Dragon hunter crossbow"))
				attackRoll = trackFactor(attackRoll, 13, 10);
			else if (wearing("Dragon hunter lance"))
				attackRoll = trackFactor(attackRoll, 6, 5);
			else if (wearing("Dragon hunter wand"))
				attackRoll = trackFactor(attackRoll, 7, 4);
		}

		if (blackMaskBonus)
			attackRoll = trackFactor(attackRoll, 23, 20);

		if (player.spell != null && player.spell.name.contains("Demonbane") && mattrs.contains(MonsterAttribute.DEMON)) {
			double demonbanePercent = buffs.markOfDarknessSpell ? 40 : 20;
			if (wearing("Purging staff"))
				demonbanePercent *= 2;
			attackRoll = trackAddFactor(attackRoll, demonbaneFactor(demonbanePercent));
		}
		if (isRevWeaponBuffApplicable())
			attackRoll = trackFactor(attackRoll, 3, 2);
		if (wearing("Tome of Water") && ("water".equals(getSpellement()) || Spells.isBindSpell(player.spell)))
			attackRoll = trackFactor(attackRoll, 6, 5);

		if (opts.usingSpecialAttack) {
			if (isWearingAccursedSceptre())
				attackRoll = trackFactor(attackRoll, 3, 2);
			else if (wearing("Volatile Nightmare staff"))
				attackRoll = trackFactor(attackRoll, 3, 2);
			else if (wearing("Eye of Ayak"))
				attackRoll = trackFactor(attackRoll, 2, 1);
		}

		String spellement = getSpellement();
		var weakness = getMonsterWeakness();
		if (spellement != null && weakness != null && spellement.equals(weakness.element)) {
			double bonus = trackFactor(baseRoll, weakness.severity, 100);
			attackRoll = trackAdd(attackRoll, bonus);
		}

		return attackRoll;
	}

	/**
	 * The player's min and max magic hit
	 */
	private double[] getPlayerMaxMagicHit() {
		double minHit = 0;
		double maxHit = 0;
		double magicLevel = player.skills.magic + player.boosts.magic;
		var spell = player.spell;
		var mattrs = monster.attributes;
		var buffs = player.buffs;

		if (spell != null) {
			maxHit = Spells.getSpellMaxHit(spell, magicLevel);
			if (spell.name.equals("Magic Dart")) {
				if (wearing("Slayer's staff (e)") && isSlayerMonster() && buffs.onSlayerTask)
					maxHit = trunc(13 + magicLevel / 6);
				else
					maxHit = trunc(10 + magicLevel / 10);
			}
		} else if (wearing("Starter staff")) {
			maxHit = 8;
		} else if (wearing("Trident of the Seas", "Trident of the Seas (e)")) {
			maxHit = Math.max(1, trunc(magicLevel / 3 - 5));
		} else if (wearing("Thammaron's sceptre")) {
			maxHit = Math.max(1, trunc(magicLevel / 3 - 8));
		} else if (wearing("Accursed sceptre") || (wearing("Accursed sceptre (a)") && opts.usingSpecialAttack)) {
			maxHit = Math.max(1, trunc(magicLevel / 3 - 6));
		} else if (wearing("Trident of the Swamp", "Trident of the Swamp (e)")) {
			maxHit = Math.max(1, trunc(magicLevel / 3 - 2));
		} else if (wearing("Sanguinesti staff", "Holy sanguinesti staff")) {
			maxHit = Math.max(1, trunc(magicLevel / 3));
		} else if (wearing("Dawnbringer")) {
			maxHit = Math.max(1, trunc(magicLevel / 3 - 2));
			// guaranteed hit between 75-150, ignores bonuses
			if (opts.usingSpecialAttack)
				return new double[] { 75, 150 };
		} else if (wearing("Tumeken's shadow")) {
			maxHit = Math.max(1, trunc(magicLevel / 3) + 1);
		} else if (wearing("Eye of Ayak")) {
			maxHit = Math.max(1, trunc(magicLevel / 3) - 6);
		} else if (wearing("Warped sceptre")) {
			maxHit = Math.max(1, trunc((8 * magicLevel + 96) / 37));
		} else if (wearing("Bone staff")) {
			// the +10 is technically a ratbane bonus, but the weapon can't be used against non-rats
			maxHit = Math.max(1, trunc(magicLevel / 3) - 5) + 10;
		} else if (wearing("Eldritch Nightmare staff") && opts.usingSpecialAttack) {
			maxHit = Math.max(1, Math.min(44, trunc((99 + 44 * magicLevel) / 99)));
		} else if (wearing("Volatile Nightmare staff") && opts.usingSpecialAttack) {
			maxHit = Math.max(1, Math.min(58, trunc((99 + 58 * magicLevel) / 99)));
		} else if (wearing("Crystal staff (basic)", "Corrupted staff (basic)")) {
			maxHit = 23;
		} else if (wearing("Crystal staff (attuned)", "Corrupted staff (attuned)")) {
			maxHit = 31;
		} else if (wearing("Crystal staff (perfected)", "Corrupted staff (perfected)")) {
			maxHit = 39;
		} else if (wearing("Swamp lizard")) {
			maxHit = trunc((magicLevel * (56 + 64) + 320) / 640);
		} else if (wearing("Orange salamander")) {
			maxHit = trunc((magicLevel * (59 + 64) + 320) / 640);
		} else if (wearing("Red salamander")) {
			maxHit = trunc((magicLevel * (77 + 64) + 320) / 640);
		} else if (wearing("Black salamander")) {
			maxHit = trunc((magicLevel * (92 + 64) + 320) / 640);
		} else if (wearing("Tecu salamander")) {
			maxHit = trunc((magicLevel * (104 + 64) + 320) / 640);
		}

		// either a 0-dmg spell, or a staff-casting option without a spell
		if (maxHit == 0)
			return new double[] { 0, 0 };

		if (opts.usingSpecialAttack && wearing("Eye of Ayak"))
			maxHit = trackFactor(maxHit, 13, 10);

		if (wearing("Chaos gauntlets") && spell != null && spell.name.toLowerCase().contains("bolt"))
			maxHit += 3;
		if (isChargeSpellApplicable())
			maxHit += 10;
		if (hasMatchingElementalAmulet())
			maxHit += 2;

		// the base hit is needed for the elemental bonus later
		double baseMax = maxHit;
		double magicDmgBonus = player.bonuses.magic_str;

		if (isWearingSmokeStaff() && spell != null && "standard".equals(spell.spellbook))
			magicDmgBonus += 100;

		boolean blackMaskBonus = false;
		if (wearing("Salve amulet(ei)") && mattrs.contains(MonsterAttribute.UNDEAD)) {
			magicDmgBonus += 200;
		} else if (wearing("Salve amulet(i)") && mattrs.contains(MonsterAttribute.UNDEAD)) {
			magicDmgBonus += 150;
		} else if (wearing("Amulet of avarice") && monster.name.startsWith("Revenant")) {
			magicDmgBonus += buffs.forinthrySurge ? 350 : 200;
		} else if (isWearingImbuedBlackMask() && isSlayerMonster() && buffs.onSlayerTask) {
			blackMaskBonus = true;
		}

		for (Prayer p : getCombatPrayers(PrayerFilter.MAGIC_DAMAGE))
			magicDmgBonus += p.magicDamageBonus;

		maxHit = trackAddFactor(maxHit, new double[] { magicDmgBonus, 1000 });

		if (blackMaskBonus)
			maxHit = trunc(maxHit * 23 / 20);

		if (wearing("Dawnbringer"))
			maxHit = Math.max(2, trunc(maxHit / 2));

		if (mattrs.contains(MonsterAttribute.DRAGON)) {
			// this still applies to dhl and dhcb when autocasting
			if (wearing("Dragon hunter lance"))
				maxHit = trackFactor(maxHit, 6, 5);
			else if (wearing("Dragon hunter wand"))
				maxHit = trackFactor(maxHit, 7, 5);
			else if (wearing("Dragon hunter crossbow"))
				maxHit = trackFactor(maxHit, 5, 4);
		}

		if (isRevWeaponBuffApplicable())
			maxHit = trunc(maxHit * 3 / 2);

		if (opts.usingSpecialAttack && isWearingAccursedSceptre())
			maxHit = trackFactor(maxHit, 3, 2);

		String spellement = getSpellement();
		var weakness = getMonsterWeakness();
		if (spellement != null && weakness != null && spellement.equals(weakness.element))
			maxHit += trunc(baseMax * (weakness.severity / 100));

		// sunfire runes are applied pre-tome
		if (buffs.usingSunfireRunes && Spells.canUseSunfireRunes(spell))
			minHit = trackFactor(maxHit, 1, 10);

		var shield = player.equipment.shield;
		boolean chargedTome = shield != null && "Charged".equals(shield.version);
		if (chargedTome && ((wearing("Tome of Fire") && "fire".equals(spellement))
			|| (wearing("Tome of Water") && "water".equals(spellement))
			|| (wearing("Tome of Earth") && "earth".equals(spellement))))
			maxHit = trackFactor(maxHit, 11, 10);

		if (has(P2_WARDEN_IDS, monster.id)) {
			double[] r = applyP2WardensDamageModifier(maxHit);
			minHit = r[0];
			maxHit = r[1];
		}

		if ("Respiratory system".equals(monster.name))
			minHit = trackAdd(minHit, trunc(maxHit / 2));

		if (monster.version != null && monster.version.contains("Glyphic Attenuation"))
			maxHit = trackFactor(maxHit, getGlyphicAttenuationFactor());

		return new double[] { minHit, maxHit };
	}

	private enum PrayerFilter
	{
		ACCURACY, STRENGTH, MAGIC_DAMAGE
	}

	/**
	 * The non-overhead prayers for the current combat style
	 */
	private List<Prayer> getCombatPrayers(PrayerFilter filter) {
		String style = player.style.type;
		String wanted = isUsingMeleeStyle() ? "melee" : "ranged".equals(style) ? "ranged" : "magic";
		var list = new ArrayList<Prayer>();
		for (int ordinal : player.prayers) {
			Prayer p = Prayer.of(ordinal);
			if (!wanted.equals(p.combatStyle))
				continue;
			boolean ok;
			switch (filter) {
				case ACCURACY:
					ok = p.factorAccuracy != null;
					break;
				case STRENGTH:
					ok = p.factorStrength != null;
					break;
				default:
					ok = p.magicDamageBonus != 0;
					break;
			}
			if (ok)
				list.add(p);
		}
		return list;
	}

	/**
	 * The min and max hit for this loadout. Don't use this for player-facing values, use {@link #getMax()}.
	 */
	public double[] getMinAndMax() {
		if (!"Manual Cast".equals(player.style.stance) && isAmmoInvalid())
			return new double[] { 0, 0 };

		String style = player.style.type;
		double[] minMax = { 0, 0 };
		if (isUsingMeleeStyle())
			minMax = getPlayerMaxMeleeHit();
		if ("ranged".equals(style))
			minMax = getPlayerMaxRangedHit();
		if ("magic".equals(style))
			minMax = getPlayerMaxMagicHit();

		if (minMax[0] > minMax[1])
			minMax[1] = minMax[0];

		// some cursed (literally, cursed amulet of magic) stuff throws this off
		if (minMax[0] <= 0)
			minMax[0] = 0;
		if (minMax[1] <= 0)
			minMax[1] = 0;
		return minMax;
	}

	/**
	 * The max attack roll for this loadout, which is based on the player's current combat style
	 */
	public double getMaxAttackRoll() {
		if (opts.overrideAttackRoll != null)
			return opts.overrideAttackRoll;

		if (!"Manual Cast".equals(player.style.stance) && isAmmoInvalid())
			return 0.0;

		String style = player.style.type;
		double atkRoll = 0;
		if (isUsingMeleeStyle())
			atkRoll = getPlayerMaxMeleeAttackRoll();
		if ("ranged".equals(style))
			atkRoll = getPlayerMaxRangedAttackRoll();
		if ("magic".equals(style))
			atkRoll = getPlayerMaxMagicAttackRoll();
		return atkRoll;
	}

	public double getDisplayHitChance() {
		double hitChance = getHitChance();

		// probably a special effect
		if (hitChance == 1.0 || hitChance == 0.0)
			return hitChance;

		double atk = getMaxAttackRoll();
		double def = getNPCDefenceRoll();

		boolean isMaggotKingMeleePunish = isUsingMeleeStyle()
			&& has(MAGGOT_KING_ID, monster.id)
			&& "Melee Punish".equals(monster.inputs.phase)
			&& "crush".equals(player.style.type);

		if (isMaggotKingMeleePunish) {
			double reducedDefRoll = trackFactor(def, 15, 100);
			var subOpts = new CalcOpts();
			subOpts.overrideDefenceRoll = reducedDefRoll;
			hitChance = noInitSubCalc(player, monster, subOpts).getHitChance();
		}

		if ("magic".equals(player.style.type) && wearing("Brimstone ring")) {
			double effectHitChance = getNormalAccuracyRoll(atk, trunc(def * 9 / 10));
			hitChance = 0.75 * hitChance + 0.25 * effectHitChance;
		}

		return hitChance;
	}

	/**
	 * The highest offensive bonus, in the order the calculator totals them
	 */
	private static String getHighestOffensiveStyle(StyleBonuses offensive) {
		String highest = "slash";
		for (String style : new String[] { "stab", "crush", "ranged", "magic" })
			if (offensive.get(style) > offensive.get(highest))
				highest = style;
		return highest;
	}

	public double getHitChance() {
		if (opts.overrideAccuracy != null && opts.overrideAccuracy != 0 && !Double.isNaN(opts.overrideAccuracy))
			return opts.overrideAccuracy;

		int id = monster.id;
		String phase = monster.inputs.phase;
		String type = player.style.type;

		if (has(GUARANTEED_ACCURACY_MONSTERS, id))
			return 1.0;
		if (has(DOOM_OF_MOKHAIOTL_IDS, id) && !"Normal".equals(phase))
			return 1.0;
		if (has(VERZIK_P1_IDS, id) && wearing("Dawnbringer"))
			return 1.0;
		if (has(P2_WARDEN_IDS, id))
			return 1.0;
		// Giant rat (Scurrius)
		if (id == 7223 && !"Manual Cast".equals(player.style.stance))
			return 1.0;
		if ("Tormented Demon".equals(monster.name) && !"Shielded".equals(phase))
			return 1.0;

		// Ice elemental and Fire elemental (Royal Titans)
		if (has(TITAN_ELEMENTAL_IDS, id) && "magic".equals(type)) {
			double accuracy = Math.min(1.0, Math.max(0, player.offensive.magic) / 100 + 0.3);
			if (isWearingEliteMagicVoid() || isWearingMagicVoid())
				accuracy = Math.min(1.0, accuracy * 1.45);
			return accuracy;
		}

		// Eclipse Moon clone phase
		if (has(ECLIPSE_MOON_IDS, id) && "Clone".equals(monster.version) && isUsingMeleeStyle())
			return 1.0;

		if ("magic".equals(type) && has(ALWAYS_MAX_HIT_MONSTERS.get("magic"), id))
			return 1.0;
		if ("ranged".equals(type) && has(ALWAYS_MAX_HIT_MONSTERS.get("ranged"), id))
			return 1.0;
		if (isUsingMeleeStyle() && has(ALWAYS_MAX_HIT_MONSTERS.get("melee"), id))
			return 1.0;

		if (opts.usingSpecialAttack && wearing("Voidwaker", "Dawnbringer"))
			return 1.0;

		if (opts.usingSpecialAttack && (wearing("Seercull") || isWearingMlb()))
			return isAmmoInvalid() ? 0.0 : 1.0;

		if ("Acidic Araxyte".equals(monster.name) || "Mirrorback Araxyte".equals(monster.name)) {
			var weapon = player.equipment.weapon;
			if (weapon != null && "Noxious halberd".equals(weapon.name))
				return 1.0;
			if ("ranged".equals(type) && getHighestOffensiveStyle(player.offensive).equals("ranged") && !isAmmoInvalid()
				&& (wearing("Heavy ballista", "Light ballista") || (weapon != null && EquipmentCategory.CROSSBOW.equals(weapon.category))))
				return 1.0;
			if ("crush".equals(type) && getHighestOffensiveStyle(player.offensive).equals("crush"))
				return 1.0;
		}

		double atk = getMaxAttackRoll();
		double def = getNPCDefenceRoll();

		double hitChance = getNormalAccuracyRoll(atk, def);

		if (isSunspearFinisher())
			return getFixedAttackHitChance(atk, def);

		boolean fangAccuracy = isWearingFang() && "stab".equals(type);
		if (fangAccuracy) {
			if (has(TOMBS_OF_AMASCUT_MONSTER_IDS, id))
				hitChance = 1 - Math.pow(1 - hitChance, 2);
			else
				hitChance = getFangAccuracyRoll(atk, def);
		}

		var weapon = player.equipment.weapon;
		if (wearing("Confliction gauntlets") && "magic".equals(type) && !(weapon != null && weapon.isTwoHanded))
			hitChance = getConflictionGauntletsAccuracyRoll(atk, def);

		return hitChance;
	}

	public double getDoTExpected() {
		double ret = 0;
		if (opts.usingSpecialAttack) {
			if (wearing("Bone claws", "Burning claws") && !isImmuneToNormalBurns())
				ret = Claws.burningClawDoT(getHitChance());
			else if (wearing("Scorching bow") && !isImmuneToNormalBurns())
				ret = monster.attributes.contains(MonsterAttribute.DEMON) ? 5 : 1;
			else if (wearing("Arkan blade") && !isImmuneToNormalBurns())
				ret = 10 * getHitChance();
		}
		return ret;
	}

	public double getDoTMax() {
		double ret = 0;
		if (opts.usingSpecialAttack) {
			if (wearing("Bone claws", "Burning claws") && !isImmuneToNormalBurns())
				ret = 29;
			else if (wearing("Scorching bow") && !isImmuneToNormalBurns())
				ret = monster.attributes.contains(MonsterAttribute.DEMON) ? 5 : 1;
			else if (wearing("Arkan blade") && !isImmuneToNormalBurns())
				ret = 10;
		}
		return ret;
	}

	public double getMax() {
		return getDistribution().getMax() + getDoTMax();
	}

	public double getExpectedDamage() {
		return getDistribution().getExpectedDamage() + getDoTExpected();
	}

	public AttackDistribution getDistribution() {
		if (memoizedDist == null)
			memoizedDist = getDistributionImpl();
		return memoizedDist;
	}

	private AttackDistribution getDistributionImpl() {
		AttackDistribution attackerDist = getAttackerDist();

		String styleType = player.style.type;
		if (opts.usingSpecialAttack && wearing("Voidwaker"))
			styleType = "magic";

		String phase = monster.inputs.phase;
		if (has(MAD_ANGEL_IDS, monster.id) && ("Sword Cleave".equals(phase) || "Perfect Lightning".equals(phase))) {
			attackerDist = attackerDist.firstHitAccurate();
			var weapon = player.equipment.weapon;
			if ("Perfect Lightning".equals(phase)) {
				attackerDist = attackerDist.firstHitMax();
			} else if (weapon == null || !"Dual macuahuitl".equals(weapon.name)) {
				double minimum = trackFactor(attackerDist.getMax(), 1, 2);
				attackerDist = attackerDist.firstHitMinimum(minimum);
			}
		}

		return attackerDist.transform(applyNpcTransforms(styleType));
	}

	private static AttackDistribution dist(HitDistribution... dists) {
		return new AttackDistribution(dists);
	}

	private static HitDistribution merged(HitDistribution a, HitDistribution b) {
		var d = new HitDistribution();
		d.hits.addAll(a.hits);
		d.hits.addAll(b.hits);
		return d;
	}

	private static HitDistribution one(Hitsplat... hitsplats) {
		return new HitDistribution(new WeightedHit(1.0, hitsplats));
	}

	private boolean isAraxyteAutoMax(String style) {
		var weapon = player.equipment.weapon;
		if (weapon != null && "Noxious halberd".equals(weapon.name))
			return true;
		if ("ranged".equals(style) && getHighestOffensiveStyle(player.offensive).equals("ranged")
			&& (wearing("Heavy ballista", "Light ballista") || (weapon != null && EquipmentCategory.CROSSBOW.equals(weapon.category))))
			return true;
		return "crush".equals(style) && getHighestOffensiveStyle(player.offensive).equals("crush");
	}

	private static final Map<String, Double> PICK_BONUSES = new HashMap<>();

	static {
		PICK_BONUSES.put("Bronze pickaxe", 1.0);
		PICK_BONUSES.put("Iron pickaxe", 1.0);
		PICK_BONUSES.put("Steel pickaxe", 6.0);
		PICK_BONUSES.put("Black pickaxe", 11.0);
		PICK_BONUSES.put("Mithril pickaxe", 21.0);
		PICK_BONUSES.put("Adamant pickaxe", 31.0);
		PICK_BONUSES.put("Rune pickaxe", 41.0);
		PICK_BONUSES.put("Gilded pickaxe", 41.0);
	}

	private AttackDistribution getAttackerDist() {
		var mattrs = monster.attributes;
		double acc = getHitChance();
		double[] minMax = getMinAndMax();
		double min = minMax[0];
		double max = minMax[1];
		String style = player.style.type;
		var weapon = player.equipment.weapon;

		if (max == 0)
			return dist(one(Hitsplat.INACCURATE));

		// standard linear
		var standardHitDist = HitDistribution.linear(acc, min, max);
		AttackDistribution dist = dist(standardHitDist);

		// Monsters that always die in one hit no matter what
		if (has(ONE_HIT_MONSTERS, monster.id))
			return dist(HitDistribution.single(1.0, new Hitsplat(monster.skills.hp)));

		if (wearing("Sunspear") && opts.usingSpecialAttack) {
			double effectDmg = trackFactor(max, 7, 10);
			return dist(HitDistribution.single(acc, new Hitsplat(effectDmg)));
		}

		if ("Respiratory system".equals(monster.name) && isUsingDemonbane())
			return dist(HitDistribution.single(acc, new Hitsplat(monster.skills.hp)));

		if ("ranged".equals(style) && wearing("Tonalztics of Ralos") && weapon != null && "Charged".equals(weapon.version)) {
			// roll two independent hits
			if (!opts.usingSpecialAttack) {
				dist = dist(standardHitDist, standardHitDist);
			} else {
				// the defence reduction from the first hit applies to the second hit,
				// so we need a full subcalc with the new defence value to determine the dist
				var lowered = baseMonster.copy();
				lowered.inputs = baseMonster.inputs.copy();
				lowered.inputs.defenceReductions = baseMonster.inputs.defenceReductions.copy();
				lowered.inputs.defenceReductions.tonalztic = baseMonster.inputs.defenceReductions.tonalztic + 1;
				double loweredDefHitAccuracy = noInitSubCalc(player, MonsterScaling.scaleMonster(lowered), null).getHitChance();

				var loweredDefHitDist = HitDistribution.linear(loweredDefHitAccuracy, min, max);
				dist = dist.transform(firstHit -> {
					var firstHitDist = HitDistribution.single(1.0, firstHit);
					var secondHitDist = firstHit.accurate ? loweredDefHitDist : standardHitDist;
					return firstHitDist.zip(secondHitDist);
				});
			}
		}

		if (isUsingMeleeStyle() && wearing("Crimson kisten") && opts.usingSpecialAttack) {
			var effectDist = new HitDistribution();
			effectDist.addHit(new WeightedHit(Math.pow(1 - acc, 4), new Hitsplat[] { Hitsplat.INACCURATE }));
			for (int i = 0; i < 4; i++) {
				// chance that exactly (i+1) accuracy rolls passed
				double chanceThisProc = MathUtil.binomial(acc, i + 1, 4);
				double effectMin = trunc(max * (70 + (i * 20)) / 100);
				double effectMax = trunc(max * (110 + (i * 20)) / 100);
				effectDist.addHits(HitDistribution.linear(1.0, effectMin, effectMax).scaleProbability(chanceThisProc).hits);
			}
			dist = dist(effectDist);
		}

		if (isUsingMeleeStyle() && wearing("Gadderhammer") && mattrs.contains(MonsterAttribute.SHADE)) {
			dist = dist(merged(
				standardHitDist.scaleProbability(0.95).scaleDamage(5, 4),
				standardHitDist.scaleProbability(0.05).scaleDamage(2)
			));
		}

		if ("ranged".equals(style) && wearing("Dark bow")) {
			dist = dist(standardHitDist, standardHitDist);
			if (opts.usingSpecialAttack)
				dist = dist.transform(Transformers.flatLimit(48, min));
		}

		boolean accurateZeroApplicable = true;
		if (opts.usingSpecialAttack) {
			if (wearing("Dragon claws")) {
				accurateZeroApplicable = false;
				dist = Claws.dClawDist(acc, max);
			} else if (wearing("Bone claws", "Burning claws")) {
				accurateZeroApplicable = false;
				dist = Claws.burningClawSpec(acc, max);
			}
		}

		if (opts.usingSpecialAttack && wearing("Dragon halberd", "Crystal halberd") && monster.size > 1) {
			double secondHitAttackRoll = trunc(getMaxAttackRoll() * 3 / 4);
			var subOpts = new CalcOpts();
			subOpts.overrideAttackRoll = secondHitAttackRoll;
			double secondHitAcc = noInitSubCalc(player, monster, subOpts).getHitChance();
			dist = dist(standardHitDist, HitDistribution.linear(secondHitAcc, min, max));
		}

		// simple multi-hit specs
		if (opts.usingSpecialAttack) {
			int hitCount = 1;
			if (wearing("Dragon dagger", "Dragon knife", "Rosewood blowpipe") || isWearingMsb())
				hitCount = 2;
			else if (wearing("Webweaver bow"))
				hitCount = 4;

			if (hitCount != 1) {
				var list = new ArrayList<HitDistribution>();
				for (int i = 0; i < hitCount; i++)
					list.add(standardHitDist);
				dist = new AttackDistribution(list);
			}
		}

		if (opts.usingSpecialAttack && wearing("Abyssal dagger")) {
			var secondHit = HitDistribution.linear(1.0, min, max);
			dist = dist.transform(h -> one(h).zip(secondHit), false);
		}

		if (opts.usingSpecialAttack && wearing("Saradomin sword")) {
			var magicHit = HitDistribution.linear(1.0, 1, 16);
			dist = dist.transform(h -> {
				if (h.accurate && !has(IMMUNE_TO_MAGIC_DAMAGE_NPC_IDS, monster.id))
					return one(h).zip(magicHit);
				return one(h, Hitsplat.INACCURATE);
			});
		}

		if (opts.usingSpecialAttack && wearing("Granite hammer"))
			dist = dist.transform(Transformers.flatAdd(5), true);

		if (isUsingMeleeStyle() && isWearingVeracs()) {
			dist = dist(merged(
				standardHitDist.scaleProbability(0.75),
				HitDistribution.linear(1.0, 1, max + 1).scaleProbability(0.25)
			));
		}

		if ("ranged".equals(style) && isWearingKarils()) {
			// 25% chance to deal a second hitsplat at half the damage of the first (flat, not rolled)
			dist = dist.transform(h -> new HitDistribution(
				new WeightedHit(0.75, new Hitsplat[] { h }),
				new WeightedHit(0.25, new Hitsplat[] { h, new Hitsplat(trunc(h.damage / 2)) })
			), false);
		}

		if (("Acidic Araxyte".equals(monster.name) || "Mirrorback Araxyte".equals(monster.name)) && isAraxyteAutoMax(style))
			return dist(HitDistribution.single(1.0, new Hitsplat(max)));

		boolean isMaggotKingMeleePunish = isUsingMeleeStyle() && has(MAGGOT_KING_ID, monster.id) && "Melee Punish".equals(monster.inputs.phase);
		double firstHitAcc = acc;
		if (isMaggotKingMeleePunish) {
			double normalDefRoll = getNPCDefenceRoll();
			double reducedDefRoll = "crush".equals(player.style.type) ? trunc(normalDefRoll * 15 / 100) : normalDefRoll;
			var subOpts = new CalcOpts();
			subOpts.overrideDefenceRoll = reducedDefRoll;
			firstHitAcc = noInitSubCalc(player, monster, subOpts).getHitChance();
		}

		if (isUsingMeleeStyle() && isWearingScythe()) {
			var hits = new ArrayList<HitDistribution>();
			for (int i = 0; i < Math.min(Math.max(monster.size, 1), 3); i++) {
				double splatMax = trunc(max / Math.pow(2, i));
				if (isMaggotKingMeleePunish && i == 0)
					splatMax = trackFactor(splatMax, 150, 100);
				double splatAcc = i == 0 ? firstHitAcc : acc;
				hits.add(HitDistribution.linear(splatAcc, min, Math.max(min, splatMax)));
			}
			dist = new AttackDistribution(hits);
		}

		if (isUsingMeleeStyle() && wearing("Dual macuahuitl")) {
			double firstMax = trunc(max / 2);
			double secondMax = max - firstMax;
			if (isMaggotKingMeleePunish)
				firstMax = trackFactor(firstMax, 150, 100);

			var firstHit = dist(HitDistribution.linear(firstHitAcc, min, Math.max(min, firstMax)));
			if (has(MAD_ANGEL_IDS, monster.id) && "Sword Cleave".equals(monster.inputs.phase)) {
				double minimum = trackFactor(firstMax, 1, 2);
				firstHit = firstHit.firstHitMinimum(minimum);
			}

			var secondHit = HitDistribution.linear(acc, min, Math.max(min, secondMax));
			dist = firstHit.transform(h -> {
				if (h.accurate)
					return one(h).zip(secondHit);
				return one(h, Hitsplat.INACCURATE);
			});
		}

		if (isUsingMeleeStyle() && isWearingTwoHitWeapon()) {
			double firstMax = trunc(max / 2);
			double secondMax = max - firstMax;
			if (isMaggotKingMeleePunish)
				firstMax = trackFactor(firstMax, 150, 100);
			dist = dist(
				HitDistribution.linear(firstHitAcc, min, Math.max(min, firstMax)),
				HitDistribution.linear(firstHitAcc, min, Math.max(min, secondMax))
			);
		}

		if (isUsingMeleeStyle() && isWearingKeris() && mattrs.contains(MonsterAttribute.KALPHITE)) {
			dist = dist(merged(
				standardHitDist.scaleProbability(50.0 / 51.0),
				standardHitDist.scaleProbability(1.0 / 51.0).scaleDamage(3)
			));
		}

		if (isUsingMeleeStyle() && has(GUARDIAN_IDS, monster.id) && weapon != null && EquipmentCategory.PICKAXE.equals(weapon.category)) {
			// just the level required to wield; there's a lot of dragon pickaxe variants
			double pickBonus = PICK_BONUSES.getOrDefault(weapon.name, 61.0);
			double factor = 50 + player.skills.mining + pickBonus;
			double divisor = 150;
			dist = dist.transform(Transformers.multiply(factor, divisor));
		}

		if (wearing("Sanguinesti staff", "Holy sanguinesti staff")) {
			dist = dist.transform(h -> new HitDistribution(
				new WeightedHit(0.8, new Hitsplat[] { h }),
				new WeightedHit(0.2, new Hitsplat[] { new Hitsplat(h.damage + 8, h.accurate) })
			), false);
		}

		var spell = player.spell;
		if (player.buffs.markOfDarknessSpell && spell != null && spell.name.contains("Demonbane") && mattrs.contains(MonsterAttribute.DEMON)) {
			double demonbaneFactor = wearing("Purging staff") ? 50 : 25;
			double vulnerability = demonbaneVulnerability();
			dist = dist.transform(h -> HitDistribution.single(1.0, new Hitsplat(
				h.damage + trunc(trunc(h.damage * demonbaneFactor / 100) * vulnerability / 100), h.accurate)));
		}

		if ("magic".equals(player.style.type) && isWearingAhrims()) {
			dist = dist.transform(h -> new HitDistribution(
				new WeightedHit(0.75, new Hitsplat[] { h }),
				new WeightedHit(0.25, new Hitsplat[] { new Hitsplat(trunc(h.damage * 13 / 10), h.accurate) })
			));
		}

		if (tdUnshieldedBonusApplies()) {
			double bonusDmg = Math.max(0, Math.pow(getAttackSpeed(), 2) - 16);
			dist = dist.transform(Transformers.flatAdd(bonusDmg), false);
		}

		if (isUsingMeleeStyle() && isWearingDharok()) {
			double newMax = player.skills.hp;
			double curr = player.skills.hp + player.boosts.hp;
			if (isMaggotKingMeleePunish) {
				dist = dist(HitDistribution.linear(firstHitAcc, min, max));
				dist = dist.scaleDamage(10000 + (newMax - curr) * newMax, 10000);
				dist = dist.scaleDamage(150, 100);
			} else {
				dist = dist.scaleDamage(10000 + (newMax - curr) * newMax, 10000);
			}
		}

		if (isUsingMeleeStyle() && isWearingBerserkerNecklace() && isWearingTzhaarWeapon())
			dist = dist.scaleDamage(6, 5);

		if (!(isWearingScythe() || isWearingTwoHitWeapon() || wearing("Dual macuahuitl") || isWearingDharok()) && isMaggotKingMeleePunish) {
			dist = dist(HitDistribution.linear(firstHitAcc, min, Math.max(min, max)));
			dist = dist.scaleDamage(150, 100);
		}

		// all this vampyre stuff was tested methodically by @jmyaeger
		if (MonsterAttribute.isVampyre(mattrs)) {
			// efaritay's bonus only applies if we can deal uncapped damage
			boolean efaritay = wearing("Efaritay's aid");
			double num = 0, den = 1;
			if (wearing("Blisterwood flail", "Hallowed flail", "Blisterwood stake")) {
				num = 125;
				den = 100;
			} else if (wearing("Sunspear")) {
				num = 150;
				den = 100;
			} else if (wearing("Blisterwood sickle")) {
				num = 23;
				den = 20;
			} else if (wearing("Ivandis flail")) {
				num = 6;
				den = 5;
			} else if (wearing("Rod of ivandis") && !mattrs.contains(MonsterAttribute.VAMPYRE_3)) {
				num = 11;
				den = 10;
			} else if (isWearingSilverWeapon() && mattrs.contains(MonsterAttribute.VAMPYRE_1)) {
				num = 11;
				den = 10;
			}
			if (num != 0) {
				if (efaritay)
					dist = dist.scaleDamage(11, 10);
				dist = dist.scaleDamage(num, den);
			}
		}

		// bolt effects
		var boltContext = new Bolts.BoltContext();
		boltContext.maxHit = max;
		boltContext.rangedLvl = player.skills.ranged + player.boosts.ranged;
		boltContext.zcb = wearing("Zaryte crossbow");
		boltContext.spec = opts.usingSpecialAttack;
		boltContext.kandarinDiary = player.buffs.kandarinDiary;
		boltContext.monster = monster;
		boolean crossbow = "ranged".equals(player.style.type) && weapon != null && EquipmentCategory.CROSSBOW.equals(weapon.category);
		if (crossbow) {
			if (wearing("Opal bolts (e)", "Opal dragon bolts (e)"))
				dist = dist.transform(Bolts.opalBolts(boltContext));
			else if (wearing("Pearl bolts (e)", "Pearl dragon bolts (e)"))
				dist = dist.transform(Bolts.pearlBolts(boltContext));
			else if (wearing("Diamond bolts (e)", "Diamond dragon bolts (e)"))
				dist = dist.transform(Bolts.diamondBolts(boltContext));
			else if (wearing("Dragonstone bolts (e)", "Dragonstone dragon bolts (e)"))
				dist = dist.transform(Bolts.dragonstoneBolts(boltContext));
			else if (wearing("Onyx bolts (e)", "Onyx dragon bolts (e)") && !mattrs.contains(MonsterAttribute.UNDEAD))
				dist = dist.transform(Bolts.onyxBolts(boltContext));
		}

		// don't raise things like bind
		if (spell != null && spell.max_hit == 0)
			accurateZeroApplicable = false;

		var ammo = player.equipment.ammo;
		if ("ranged".equals(player.style.type) && ammo != null && ammo.name.contains("Seeking")
			&& EquipmentCalc.ammoApplicability(weapon == null ? null : weapon.id, ammo.id) == EquipmentCalc.AmmoApplicability.INCLUDED) {
			dist = dist.transform(h -> HitDistribution.single(1.0, new Hitsplat(Math.max(h.damage, 3), h.accurate)), false);
		}

		// raise accurate 0s to 1
		if (accurateZeroApplicable)
			dist = dist.transform(h -> HitDistribution.single(1.0, new Hitsplat(Math.max(h.damage, 1))), false);

		if ("magic".equals(player.style.type) && spell != null && "standard".equals(spell.spellbook)) {
			boolean twinflameCompat = spell.name.contains("Bolt") || spell.name.contains("Blast") || spell.name.contains("Wave");
			if (wearing("Twinflame staff") && twinflameCompat) {
				dist = dist.transform(h -> HitDistribution.single(1.0,
					new Hitsplat(h.damage), new Hitsplat(trunc(h.damage * 4 / 10))));
			}
		}

		// corp takes full ruby bolt effect damage but reduced damage from bolts otherwise
		if ("Corporeal Beast".equals(monster.name) && !isWearingCorpbaneWeapon())
			dist = dist.transform(Transformers.division(2));

		if (crossbow) {
			double currentHp = player.skills.hp + player.boosts.hp;
			if (wearing("Ruby bolts (e)", "Ruby dragon bolts (e)") && currentHp >= 10)
				dist = dist.transform(Bolts.rubyBolts(boltContext));
		}

		if ("magic".equals(player.style.type) && wearing("Brimstone ring")
			&& (opts.overrideDefenceRoll == null || opts.overrideDefenceRoll == 0 || Double.isNaN(opts.overrideDefenceRoll))) {
			double effectChance = 0.25;
			double effectDef = trackFactor(getNPCDefenceRoll(), 9, 10);
			var subOpts = new CalcOpts();
			subOpts.overrideDefenceRoll = effectDef;
			var effectDist = noInitSubCalc(player, monster, subOpts).getAttackerDist();

			var zippedDists = new ArrayList<HitDistribution>();
			for (int i = 0; i < dist.dists.size(); i++)
				zippedDists.add(merged(
					dist.dists.get(i).scaleProbability(1 - effectChance),
					effectDist.dists.get(i).scaleProbability(effectChance)
				));
			dist = new AttackDistribution(zippedDists).flatten();
		}

		// monsters that are always max hit no matter what
		String type = player.style.type;
		if (("magic".equals(type) && has(ALWAYS_MAX_HIT_MONSTERS.get("magic"), monster.id))
			|| (isUsingMeleeStyle() && has(ALWAYS_MAX_HIT_MONSTERS.get("melee"), monster.id))
			|| ("ranged".equals(type) && has(ALWAYS_MAX_HIT_MONSTERS.get("ranged"), monster.id))) {
			if (has(YAMA_VOID_FLARE_IDS, monster.id) && player.buffs.markOfDarknessSpell && spell != null && spell.name.contains("Demonbane")) {
				double demonbaneFactor = wearing("Purging staff") ? 50 : 25;
				return dist(HitDistribution.single(1.0, new Hitsplat(max + trunc(trunc(max * demonbaneFactor / 100) * demonbaneVulnerability() / 100))));
			}
			return dist(HitDistribution.single(1.0, new Hitsplat(dist.getMax())));
		}

		return dist;
	}

	private final Map<String, HitTransformer> npcTransformCache = new HashMap<>();

	private static final class Effect
	{
		final HitTransformer transformer;
		final boolean transformInaccurate;

		Effect(HitTransformer transformer) {
			this(transformer, true);
		}

		Effect(HitTransformer transformer, boolean transformInaccurate) {
			this.transformer = transformer;
			this.transformInaccurate = transformInaccurate;
		}
	}

	/**
	 * Damage limiters and modifiers applied by the NPC
	 */
	public HitTransformer applyNpcTransforms(String styleType) {
		String cacheKey = String.valueOf(styleType);
		var cached = npcTransformCache.get(cacheKey);
		if (cached != null)
			return cached;

		if (isImmune(styleType)) {
			HitTransformer immune = h -> HitDistribution.single(1.0, Hitsplat.INACCURATE);
			npcTransformCache.put(cacheKey, immune);
			return immune;
		}

		var mattrs = monster.attributes;
		var effects = new ArrayList<Effect>();
		int id = monster.id;
		String name = monster.name;
		var weapon = player.equipment.weapon;
		String weaponCategory = weapon == null ? null : weapon.category;

		if ("Zulrah".equals(name))
			effects.add(new Effect(Transformers.cappedReroll(50, 5, 45)));
		if ("Fragment of Seren".equals(name))
			effects.add(new Effect(Transformers.linearMin(2, 22)));
		if (("Kraken".equals(name) || "Cave kraken".equals(name)) && "ranged".equals(styleType))
			effects.add(new Effect(Transformers.division(7, 1)));
		if (has(VERZIK_P1_IDS, id) && !wearing("Dawnbringer"))
			effects.add(new Effect(Transformers.linearMin(isUsingMeleeStyle() ? 10 : 3)));
		if (has(TEKTON_IDS, id) && "magic".equals(styleType))
			effects.add(new Effect(Transformers.division(5, 1)));
		if (has(GLOWING_CRYSTAL_IDS, id) && "magic".equals(styleType))
			effects.add(new Effect(Transformers.division(3)));
		if ((has(OLM_MELEE_HAND_IDS, id) || has(OLM_HEAD_IDS, id)) && "magic".equals(styleType))
			effects.add(new Effect(Transformers.division(3)));
		if ((has(OLM_MAGE_HAND_IDS, id) || has(OLM_MELEE_HAND_IDS, id)) && "ranged".equals(styleType))
			effects.add(new Effect(Transformers.division(3)));
		if (has(ICE_DEMON_IDS, id) && !"fire".equals(getSpellement()) && !isUsingDemonbane())
			effects.add(new Effect(Transformers.division(3)));
		if ("Slagilith".equals(name) && !EquipmentCategory.PICKAXE.equals(weaponCategory))
			effects.add(new Effect(Transformers.division(3)));
		if (has(NIGHTMARE_TOTEM_IDS, id) && "magic".equals(styleType))
			effects.add(new Effect(Transformers.multiply(2)));
		if ("Slash Bash".equals(name) || "Zogre".equals(name) || "Skogre".equals(name)) {
			var ammo = player.equipment.ammo;
			if (player.spell != null && "Crumble Undead".equals(player.spell.name)) {
				effects.add(new Effect(Transformers.division(2)));
			} else if (!"ranged".equals(player.style.type)
				|| ammo == null || !ammo.name.contains(" brutal")
				|| weapon == null || !"Comp ogre bow".equals(weapon.name)) {
				effects.add(new Effect(Transformers.division(4)));
			}
		}
		if (has(BA_ATTACKER_MONSTERS, id) && player.buffs.baAttackerLevel != 0)
			effects.add(new Effect(Transformers.flatAdd(player.buffs.baAttackerLevel), true));
		if ("Tormented Demon".equals(name)) {
			// 20% damage reduction when not using demonbane or abyssal
			if (!"Unshielded".equals(monster.inputs.phase) && !isUsingDemonbane() && !isUsingAbyssal())
				effects.add(new Effect(Transformers.multiply(4, 5, 1)));
		}
		if (mattrs.contains(MonsterAttribute.VAMPYRE_2)) {
			if (!wearingVampyrebane(MonsterAttribute.VAMPYRE_2) && wearing("Efaritay's aid"))
				effects.add(new Effect(Transformers.division(2)));
			else if (isWearingSilverWeapon())
				effects.add(new Effect(Transformers.flatLimit(10)));
		}
		if (has(HUEYCOATL_TAIL_IDS, id)) {
			boolean crush = "crush".equals(styleType)
				&& player.offensive.crush > player.offensive.slash
				&& player.offensive.crush > player.offensive.stab;
			boolean earth = "earth".equals(getSpellement());

			// crush and earth spells have a higher limiter
			effects.add(new Effect(Transformers.linearMin((crush || earth) ? 9 : 4)));

			// and crush also gets misses turned into 1s
			if (crush) {
				effects.add(new Effect(h -> h.damage > 0
					? HitDistribution.single(1.0, h)
					: HitDistribution.single(1.0, new Hitsplat(1))));
			}
		}
		if (has(HUEYCOATL_PHASE_IDS, id) && "With Pillar".equals(monster.inputs.phase))
			effects.add(new Effect(Transformers.multiply(13, 10)));
		if (has(ABYSSAL_SIRE_TRANSITION_IDS, id) && "Transition".equals(monster.inputs.phase))
			effects.add(new Effect(Transformers.division(2)));

		double flatArmour = monster.defensive.flat_armour;
		if (flatArmour != 0 && !Double.isNaN(flatArmour) && !"magic".equals(styleType))
			effects.add(new Effect(Transformers.flatAdd(-flatArmour), false));

		HitTransformer transformer = hitsplat -> {
			var d = HitDistribution.single(1.0, hitsplat);
			for (Effect e : effects)
				d = d.wideTransform(e.transformer, e.transformInaccurate);
			return d.flatten();
		};
		npcTransformCache.put(cacheKey, transformer);
		return transformer;
	}

	public boolean isImmune(String styleType) {
		int monsterId = monster.id;
		var mattrs = monster.attributes;
		var weapon = player.equipment.weapon;
		String weaponCategory = weapon == null ? null : weapon.category;
		var ammo = player.equipment.ammo;

		if (has(IMMUNE_TO_MAGIC_DAMAGE_NPC_IDS, monsterId) && "magic".equals(styleType))
			return true;
		if (has(IMMUNE_TO_RANGED_DAMAGE_NPC_IDS, monsterId) && "ranged".equals(styleType))
			return true;
		if (has(IMMUNE_TO_MELEE_DAMAGE_NPC_IDS, monsterId) && isUsingMeleeStyle())
			return !(has(ZULRAH_IDS, monsterId) && EquipmentCategory.POLEARM.equals(weaponCategory));
		if (mattrs.contains(MonsterAttribute.FLYING) && isUsingMeleeStyle()) {
			// Vespula is immune to melee despite flying attribute
			if (has(VESPULA_IDS, monsterId))
				return true;
			return !(EquipmentCategory.POLEARM.equals(weaponCategory) || EquipmentCategory.SALAMANDER.equals(weaponCategory));
		}
		if (has(IMMUNE_TO_NON_SALAMANDER_MELEE_DAMAGE_NPC_IDS, monsterId) && isUsingMeleeStyle()
			&& !EquipmentCategory.SALAMANDER.equals(weaponCategory))
			return true;
		if (mattrs.contains(MonsterAttribute.VAMPYRE_3) && !wearingVampyrebane(MonsterAttribute.VAMPYRE_3))
			return true;
		if (mattrs.contains(MonsterAttribute.VAMPYRE_2) && !wearingVampyrebane(MonsterAttribute.VAMPYRE_2)
			&& !wearing("Efaritay's aid") && !isWearingSilverWeapon())
			return true;
		if (has(GUARDIAN_IDS, monsterId) && (!isUsingMeleeStyle() || !EquipmentCategory.PICKAXE.equals(weaponCategory)))
			return true;
		if (mattrs.contains(MonsterAttribute.LEAFY) && !isWearingLeafBladedWeapon())
			return true;
		if (has(DOOM_OF_MOKHAIOTL_IDS, monsterId) && "Shielded".equals(monster.inputs.phase) && !isUsingDemonbane())
			return true;
		if (!mattrs.contains(MonsterAttribute.RAT) && isWearingRatBoneWeapon())
			return true;
		if ("Fire Warrior of Lesarkus".equals(monster.name)
			&& (!"ranged".equals(styleType) || ammo == null || !"Ice arrows".equals(ammo.name)))
			return true;
		if ("Fareed".equals(monster.name)) {
			if (("magic".equals(styleType) && !"water".equals(getSpellement()))
				|| ("ranged".equals(styleType) && (ammo == null || ammo.name == null || !ammo.name.contains("arrow"))))
				return true;
		}
		// Eclipse moon clone is immune to non-melee attacks
		return has(ECLIPSE_MOON_IDS, monsterId) && "Clone".equals(monster.version) && !isUsingMeleeStyle();
	}

	public double getAttackSpeed() {
		return Double.isNaN(player.attackSpeed) ? EquipmentCalc.calculateAttackSpeed(player, monster) : player.attackSpeed;
	}

	public double getExpectedAttackSpeed() {
		if (isWearingBloodMoonSet()) {
			double acc = getHitChance();
			double procChance = opts.usingSpecialAttack
				? 1 - Math.pow(1 - acc, 2) // always if hit
				: (acc / 3) + ((acc * acc) * 2 / 9); // 1/3 per hit
			return getAttackSpeed() - procChance;
		}

		if (tdUnshieldedBonusApplies())
			return getAttackSpeed() - 1;

		if (opts.usingSpecialAttack && wearing("Eye of Ayak"))
			return 5;

		return getAttackSpeed();
	}

	/**
	 * Expected damage per tick
	 */
	public double getDpt() {
		return getExpectedDamage() / getExpectedAttackSpeed();
	}

	/**
	 * Expected damage per second
	 */
	public double getDps() {
		return getDpt() / SECONDS_PER_TICK;
	}

	/**
	 * Average hits to kill
	 */
	public double getHtk() {
		var dist = getDistribution();
		var single = dist.getSingleHitsplat();

		var hitMap = new HashMap<Double, Double>();
		for (WeightedHit h : single.hits)
			hitMap.merge(h.getSum(), h.probability, Double::sum);
		int histMax = (int) single.getMax();
		double[] hist = new double[Math.max(histMax, 0) + 1];
		boolean[] present = new boolean[hist.length];
		for (int i = 0; i < hist.length; i++) {
			Double prob = hitMap.get((double) i);
			hist[i] = prob == null ? 0 : prob;
			present[i] = true;
		}

		int startHp = (int) monster.inputs.monsterCurrentHp;
		double max = Math.min(startHp, dist.getMax());
		if (max == 0)
			return 0;

		// 0 hits left to do if hp = 0
		double[] htk = new double[startHp + 1];
		for (int hp = 1; hp <= startHp; hp++) {
			double val = 1.0; // takes at least one hit
			for (int hit = 1; hit <= Math.min(hp, max); hit++) {
				if (hit < hist.length && present[hit])
					val += hist[hit] * htk[hp - hit];
			}
			htk[hp] = val / (1 - hist[0]);
		}
		return htk[startHp];
	}

	/**
	 * Average time to kill, in seconds
	 */
	public double getTtk() {
		return getHtk() * getExpectedAttackSpeed() * SECONDS_PER_TICK;
	}

	public double getSpecDps() {
		if (wearing("Soulreaper axe")) {
			// assumes using spec every time you reach the current stack count
			double ticksPerSpec = getAttackSpeed() * player.buffs.soulreaperStacks;
			return getDps() * getExpectedAttackSpeed() / ticksPerSpec;
		}

		Integer specCost = getSpecCost();
		if (specCost == null || specCost == 0)
			return 0;

		double ticksToRegen = wearing("Lightbearer") ? 25 : 50;
		double ticksPerSpec = specCost * (ticksToRegen / 10);
		return getDps() * getExpectedAttackSpeed() / ticksPerSpec;
	}

	/**
	 * A sub-calc sharing this one's sanitized inputs. Passing null opts keeps this calc's overrides.
	 */
	private PlayerVsNPCCalc noInitSubCalc(Player p, Monster m, CalcOpts overrides) {
		var subOpts = opts.copy();
		if (overrides != null) {
			subOpts.overrideAccuracy = overrides.overrideAccuracy;
			subOpts.overrideAttackRoll = overrides.overrideAttackRoll;
			subOpts.overrideDefenceRoll = overrides.overrideDefenceRoll;
		}
		subOpts.noInit = true;
		var subCalc = new PlayerVsNPCCalc(p, m, subOpts);
		subCalc.baseMonster = baseMonster;
		subCalc.allEquippedItems = allEquippedItems;
		return subCalc;
	}

	/**
	 * @param weaponDemonbane as a percent out of 100
	 */
	private double[] demonbaneFactor(double weaponDemonbane) {
		Double v = monster.inputs.demonbaneVulnerability;
		double vulnerability = v == null ? 100 : v;
		double percent = trackFactor(weaponDemonbane, vulnerability, 100);
		return new double[] { percent, 100 };
	}

	private static double tbowScaling(double current, double magic, boolean accuracyMode) {
		double factor = accuracyMode ? 10 : 14;
		double base = accuracyMode ? 140 : 250;
		double clamp = accuracyMode ? 140 : 250;

		double t2 = trunc((3 * magic - factor) / 100);
		double t3 = trunc(Math.pow(trunc(3 * magic / 10) - (10 * factor), 2) / 100);
		double bonus = Math.max(0, Math.min(clamp, base + t2 - t3));
		return trunc(current * bonus / 100);
	}

	public Integer getSpecCost() {
		var weapon = player.equipment.weapon;
		if (weapon == null || weapon.name == null || weapon.name.isEmpty())
			return null;
		return EquipmentCalc.WEAPON_SPEC_COSTS.get(weapon.name);
	}

	public FeatureStatus isSpecSupported() {
		var weapon = player.equipment.weapon;
		if (weapon == null || weapon.name == null || weapon.name.isEmpty())
			return FeatureStatus.NOT_APPLICABLE;
		String weaponName = weapon.name;

		if (wearing("Dual macuahuitl") && !isWearingBloodMoonSet())
			return FeatureStatus.NOT_APPLICABLE;
		if (wearing("Soulreaper axe"))
			return player.buffs.soulreaperStacks == 0 ? FeatureStatus.NOT_APPLICABLE : FeatureStatus.IMPLEMENTED;
		if (wearing("Brine sabre"))
			return has(UNDERWATER_MONSTERS, monster.id) ? FeatureStatus.IMPLEMENTED : FeatureStatus.NOT_APPLICABLE;
		if (PARTIALLY_IMPLEMENTED_SPECS.contains(weaponName))
			return FeatureStatus.PARTIALLY_IMPLEMENTED;
		if (getSpecCost() != null)
			return FeatureStatus.IMPLEMENTED;
		if (UNIMPLEMENTED_SPECS.contains(weaponName))
			return FeatureStatus.UNIMPLEMENTED;
		return FeatureStatus.NOT_APPLICABLE;
	}

	public PlayerVsNPCCalc getSpecCalc() {
		switch (isSpecSupported()) {
			case IMPLEMENTED:
			case PARTIALLY_IMPLEMENTED:
				var specOpts = opts.copy();
				specOpts.usingSpecialAttack = true;
				return new PlayerVsNPCCalc(player, baseMonster, specOpts);
			default:
				return null;
		}
	}

	private double[] applyP2WardensDamageModifier(double max) {
		// 1/3 of enemy defence is removed from accuracy
		double reducedNpcDefence = trunc(getNPCDefenceRoll() / 3);
		double accuracyDelta = Math.max(getMaxAttackRoll() - reducedNpcDefence, 0);

		// remaining accuracy provides a % dmg modifier from 15% - 40% based on lerp from 0 to 42k MAR
		double modifier = Math.max(Math.min(MathUtil.iLerp(15, 40, 0, 42_000, accuracyDelta), 40), 15);

		// in effect, we're dealing between [15-35, 40-60]% of normal damage
		double maxPctRange = 20;
		return new double[] {
			trunc(max * modifier / 100),
			trunc(max * (modifier + maxPctRange) / 100),
		};
	}

	private boolean isSunspearFinisher() {
		if (!wearing("Sunspear") || !opts.usingSpecialAttack)
			return false;
		double max = getMinAndMax()[1];
		return monster.inputs.monsterCurrentHp <= trunc(max * 7 / 10);
	}

	private double[] getGlyphicAttenuationFactor() {
		double prayerBonus = player.bonuses.prayer;
		double bonus = Math.floor(0.85 * prayerBonus);
		if (prayerBonus >= 20)
			bonus += 7;
		if (prayerBonus >= 30)
			bonus += 10;
		if (prayerBonus >= 40)
			bonus += 15;
		if (prayerBonus >= 50)
			bonus += 15;
		return new double[] { 100 + bonus, 100 };
	}

	private Monster.Weakness getMonsterWeakness() {
		String spellement = getSpellement();
		var baseWeakness = monster.weakness;
		if (spellement == null)
			return null;

		boolean usingRightSpell = baseWeakness != null && spellement.equals(baseWeakness.element);
		var w = new Monster.Weakness();
		w.element = spellement;
		w.severity = usingRightSpell ? baseWeakness.severity : 0;
		return w;
	}
}
