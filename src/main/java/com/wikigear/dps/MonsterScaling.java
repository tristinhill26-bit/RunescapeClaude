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
import static com.wikigear.dps.MathUtil.addPercent;
import static com.wikigear.dps.MathUtil.iSqrt;
import static com.wikigear.dps.MathUtil.trunc;

/**
 * Raid scaling, Vardorvis scaling, monster phases and defence reductions
 */
public final class MonsterScaling
{
	private static final int CM_SCALE_PERCENT = 50;

	public static Monster scaleMonster(Monster m) {
		m = applyCoxScaling(m);
		m = applyTobScaling(m);
		m = applyToaScaling(m);
		m = applyVardScaling(m);
		m = applyMonsterPhases(m);
		m = applyDefenceReductions(m);
		return m;
	}

	/**
	 * Saves compute work when hp is the only thing that changes
	 */
	public static Monster scaleMonsterHpOnly(Monster m) {
		if ("Vardorvis".equals(m.name))
			return applyVardScaling(m);
		return m;
	}

	private static Monster withSkills(Monster m, Monster.Skills skills) {
		var copy = m.copy();
		copy.skills = skills;
		return copy;
	}

	private static double getSkill(Monster.Skills s, String k) {
		switch (k) {
			case "atk":
				return s.atk;
			case "def":
				return s.def;
			case "hp":
				return s.hp;
			case "magic":
				return s.magic;
			case "ranged":
				return s.ranged;
			case "str":
				return s.str;
			default:
				throw new IllegalArgumentException(k);
		}
	}

	private static void setSkill(Monster.Skills s, String k, double v) {
		switch (k) {
			case "atk":
				s.atk = v;
				break;
			case "def":
				s.def = v;
				break;
			case "hp":
				s.hp = v;
				break;
			case "magic":
				s.magic = v;
				break;
			case "ranged":
				s.ranged = v;
				break;
			case "str":
				s.str = v;
				break;
			default:
				throw new IllegalArgumentException(k);
		}
	}

	// ---- Chambers of Xeric

	private static final class SkillMeta
	{
		List<String> offensives = new ArrayList<>();
		double baseOffensive;
		List<String> defensives = new ArrayList<>();
		double baseDefensive;
		double baseHp;
	}

	private static SkillMeta getSkillMeta(Monster m) {
		boolean magicIsDefensive = has(COX_MAGIC_IS_DEFENSIVE_IDS, m.id);
		var offensivesAll = new ArrayList<>(List.of("atk", "str", "ranged"));
		var defensivesAll = new ArrayList<>(List.of("def"));
		if (magicIsDefensive)
			defensivesAll.add("magic");
		else
			offensivesAll.add("magic");

		var meta = new SkillMeta();
		// don't scale skills set at 1, they should remain at 1
		for (String k : offensivesAll)
			if (getSkill(m.skills, k) != 1)
				meta.offensives.add(k);
		for (String k : defensivesAll)
			if (getSkill(m.skills, k) != 1)
				meta.defensives.add(k);

		// the "base" stat of the remaining skills, which should all be the same since they're linked
		meta.baseOffensive = maxSkill(m.skills, meta.offensives);
		meta.baseDefensive = maxSkill(m.skills, meta.defensives);
		meta.baseHp = has(GUARDIAN_IDS, m.id)
			? 151 + trunc(m.inputs.partySumMiningLevel / m.inputs.partySize)
			: m.skills.hp;
		return meta;
	}

	private static double maxSkill(Monster.Skills s, List<String> keys) {
		double max = Double.NaN;
		for (String k : keys) {
			double v = getSkill(s, k);
			if (Double.isNaN(max) || v > max)
				max = v;
		}
		return Double.isNaN(max) ? 1 : max;
	}

	/**
	 * Things intended to be fought solo, like scavenger beasts and vespine soldiers
	 */
	private static Monster applySinglesCoxScaling(Monster m) {
		var inputs = m.inputs;
		var meta = getSkillMeta(m);

		double hpScaler = Math.max(Math.min(inputs.partyMaxCombatLevel, 126), 60);
		double statScaler = Math.max(Math.min(inputs.partyMaxHpLevel, 99), 55);
		if (inputs.isFromCoxCm) {
			statScaler = addPercent(statScaler, CM_SCALE_PERCENT);
			hpScaler = addPercent(hpScaler, CM_SCALE_PERCENT);
		}

		var skills = m.skills.copy();
		skills.hp = Math.max(trunc(meta.baseHp * hpScaler / 126), 5);
		for (String o : meta.offensives)
			setSkill(skills, o, Math.max(trunc(meta.baseOffensive * statScaler / 99), 1));
		for (String d : meta.defensives)
			setSkill(skills, d, Math.max(trunc(meta.baseDefensive * statScaler / 99), 1));
		return withSkills(m, skills);
	}

	/**
	 * Everything intended to be fought in a group
	 */
	private static Monster applyMultiCoxScaling(Monster m) {
		var inputs = m.inputs;
		int id = m.id;
		var meta = getSkillMeta(m);

		double partySize = Math.min(Math.max(inputs.partySize, 1), 100);
		double partySizeM1 = partySize - 1;
		double highestComLevel = Math.max(Math.min(inputs.partyMaxCombatLevel, 126), 60);
		double highestHp = Math.max(Math.min(55 + trunc(44 * inputs.partyMaxHpLevel / 99), 99), 55);

		double offensive = trunc(meta.baseOffensive * highestHp / 99);
		double defensive = trunc(meta.baseDefensive * highestHp / 99);
		double hp = trunc(meta.baseHp * highestComLevel / 126);

		double offensiveScalePct = 100 + iSqrt(partySizeM1) * 7 + partySizeM1;
		offensive = trunc(offensive * offensiveScalePct / 100);

		double defensiveScalePct = 100 + iSqrt(partySizeM1) + trunc(partySizeM1 * 7 / 10);
		defensive = trunc(defensive * defensiveScalePct / 100);

		hp += hp * trunc(partySize * 50 / 100);

		if (inputs.isFromCoxCm) {
			offensive = addPercent(offensive, CM_SCALE_PERCENT);
			if (!has(GLOWING_CRYSTAL_IDS, id))
				hp = addPercent(hp, CM_SCALE_PERCENT);

			if (has(GLOWING_CRYSTAL_IDS, id)) {
				// not scaled
			} else if (has(TEKTON_IDS, id)) {
				// tekton gets special treatment to make specs easier, especially for small party sizes
				defensive = addPercent(defensive, partySize < 4 ? 20 : 35);
			} else {
				defensive = addPercent(defensive, CM_SCALE_PERCENT);
			}
		}

		var skills = m.skills.copy();
		skills.hp = Math.max(Math.min(hp, 30_000), 50);
		for (String o : meta.offensives)
			setSkill(skills, o, Math.max(Math.min(offensive, 5_000), 50));
		for (String d : meta.defensives)
			setSkill(skills, d, Math.max(Math.min(defensive, 20_000), 50));
		return withSkills(m, skills);
	}

	private static Monster applyOlmScaling(Monster m) {
		boolean lhand = has(OLM_MELEE_HAND_IDS, m.id);
		boolean rhand = has(OLM_MAGE_HAND_IDS, m.id);

		// partySize - 3 * extraPhases, basically
		double partySizeScaleFactor = Math.min(m.inputs.partySize - 1, 50) - 3 * trunc(Math.min(m.inputs.partySize, 50) / 8);

		var base = applyMultiCoxScaling(m);
		var skills = base.skills.copy();
		skills.magic = rhand ? trunc(base.skills.magic / 2) : base.skills.magic;
		skills.hp = (lhand || rhand) ? 600 + 300 * partySizeScaleFactor : 800 + 400 * partySizeScaleFactor;
		return withSkills(base, skills);
	}

	private static Monster applyCoxScaling(Monster m) {
		if (!m.attributes.contains(MonsterAttribute.XERICIAN))
			return m;
		if (has(COX_USE_SINGLES_SCALING_IDS, m.id))
			return applySinglesCoxScaling(m);
		if (has(OLM_IDS, m.id))
			return applyOlmScaling(m);
		return applyMultiCoxScaling(m);
	}

	// ---- Theatre of Blood

	private static final double[][] ENTRY_MODE_SCALING = {
		null, { 10, 40 }, { 19, 40 }, { 27, 40 }, { 34, 40 }, { 40, 40 }
	};

	private static Monster applyTobScaling(Monster m) {
		var inputs = m.inputs;

		// tob only scales hp and nothing else
		if (has(TOB_MONSTER_IDS, m.id)) {
			double partySize = Math.min(5, Math.max(3, inputs.partySize));
			var skills = m.skills.copy();
			skills.hp = trunc(m.skills.hp * (partySize + 3) / 8);
			return withSkills(m, skills);
		}

		if (has(TOB_EM_MONSTER_IDS, m.id)) {
			double partySize = Math.min(5, Math.max(1, inputs.partySize));
			double[] factor = ENTRY_MODE_SCALING[(int) partySize];
			var skills = m.skills.copy();
			skills.hp = trunc(m.skills.hp * factor[0] / factor[1]);
			return withSkills(m, skills);
		}

		return m;
	}

	// ---- Tombs of Amascut

	private static Monster applyToaScaling(Monster m) {
		var inputs = m.inputs;

		// toa multiplies rolled values, not stats, except for hp
		if (!has(TOMBS_OF_AMASCUT_MONSTER_IDS, m.id))
			return m;

		boolean coreEjected = has(TOA_WARDEN_CORE_EJECTED_IDS, m.id);
		double newHp = coreEjected ? 4500 : m.skills.hp;

		double invoFactor = trunc((coreEjected ? 1 : 4) * inputs.toaInvocationLevel / 10);
		newHp += trunc(newHp * invoFactor / 100);

		double pathLevel = Math.min(6, Math.max(0, inputs.toaPathLevel));
		if (has(TOMBS_OF_AMASCUT_PATH_MONSTER_IDS, m.id) && pathLevel >= 1) {
			double pathLevelFactor = 3 + 5 * inputs.toaPathLevel;
			newHp = trunc(newHp * (100 + pathLevelFactor) / 100);
		}

		double partySize = Math.min(8, Math.max(1, inputs.partySize));
		if (partySize >= 2) {
			double partyFactor = 9 * (partySize >= 3 ? 2 : 1);
			if (partySize >= 4)
				partyFactor += 6 * (partySize - 3);
			newHp = trunc(newHp * (10 + partyFactor) / 10);
		}

		// some rounding, for once
		if (newHp > 100) {
			double roundTo = newHp > 300 ? 10 : 5;
			newHp = trunc((newHp + trunc(roundTo / 2)) / roundTo) * roundTo;
		}

		var skills = m.skills.copy();
		skills.hp = newHp;
		return withSkills(m, skills);
	}

	// ---- Vardorvis

	private static Monster applyVardScaling(Monster m) {
		if (!"Vardorvis".equals(m.name))
			return m;

		// vard's strength and defence scale linearly throughout the fight based on hp
		double maxHp, strStart, strEnd, defStart, defEnd;
		if ("Quest".equals(m.version)) {
			maxHp = 500;
			strStart = 210;
			strEnd = 280;
			defStart = 180;
			defEnd = 130;
		} else if ("Awakened".equals(m.version)) {
			maxHp = 1400;
			strStart = 391;
			strEnd = 522;
			defStart = 268;
			defEnd = 181;
		} else {
			maxHp = 700;
			strStart = 270;
			strEnd = 360;
			defStart = 215;
			defEnd = 145;
		}

		double currHp = Double.isFinite(m.inputs.monsterCurrentHp) ? m.inputs.monsterCurrentHp : maxHp;
		var skills = m.skills.copy();
		skills.str = MathUtil.lerp(currHp, maxHp, 0, strStart, strEnd);
		skills.def = MathUtil.lerp(currHp, maxHp, 0, defStart, defEnd);
		return withSkills(m, skills);
	}

	// ---- Phases

	private static Monster applyMonsterPhases(Monster m) {
		String phase = m.inputs.phase;
		if (phase == null || phase.isEmpty())
			return m;

		if (has(ARAXXOR_IDS, m.id) && phase.equals("Enraged")) {
			var skills = m.skills.copy();
			skills.def = m.skills.def + 35;
			skills.magic = m.skills.magic + 28;
			skills.ranged = m.skills.ranged + 31;
			return withSkills(m, skills);
		}

		// yama goes to 60 magic defence if the tank is using magic
		if (has(YAMA_IDS, m.id) && !"Enraged".equals(m.version)) {
			var copy = m.copy();
			copy.defensive = m.defensive.copy();
			copy.defensive.magic = phase.equals("Tank using magic") ? 60 : -30;
			return copy;
		}

		return m;
	}

	// ---- Defence reductions

	public static double getDefenceFloor(Monster m) {
		int id = m.id;
		if (has(VERZIK_IDS, id) || has(VARDORVIS_IDS, id))
			return m.skills.def;
		if (has(SOTETSEG_IDS, id))
			return 100;
		if (has(NIGHTMARE_IDS, id))
			return 120;
		if (has(AKKHA_IDS, id))
			return 70;
		if (has(BABA_IDS, id))
			return 60;
		if (has(KEPHRI_UNSHIELDED_IDS, id) || has(KEPHRI_SHIELDED_IDS, id))
			return 60;
		if (has(ZEBAK_IDS, id))
			return 50;
		if (has(P3_WARDEN_IDS, id))
			return 120;
		if (has(TOA_OBELISK_IDS, id))
			return 60;
		if (has(NEX_IDS, id))
			return 250;
		if (has(ARAXXOR_IDS, id))
			return 90;
		if (has(HUEYCOATL_IDS, id))
			return 120;
		if (has(YAMA_IDS, id))
			return 145;
		// no limit
		return 0;
	}

	/**
	 * Sets the given skills, respecting the defence floor, on a copy of the monster
	 */
	private static Monster newSkills(Monster current, double defenceFloor, String[] keys, double[] values) {
		var skills = current.skills.copy();
		for (int i = 0; i < keys.length; i++) {
			double floor = keys[i].equals("def") ? defenceFloor : 0;
			setSkill(skills, keys[i], Math.max(floor, values[i]));
		}
		return withSkills(current, skills);
	}

	private static Monster reduceArclight(Monster monster, Monster.Skills baseSkills, double floor, double iter, double num, double den) {
		if (iter == 0)
			return monster;
		return newSkills(monster, floor, new String[] { "atk", "str", "def" }, new double[] {
			monster.skills.atk - (iter * (trunc(num * baseSkills.atk / den) + 1)),
			monster.skills.str - (iter * (trunc(num * baseSkills.str / den) + 1)),
			monster.skills.def - (iter * (trunc(num * baseSkills.def / den) + 1)),
		});
	}

	private static Monster applyDefenceReductions(Monster m) {
		var baseSkills = m.skills;
		double floor = getDefenceFloor(m);
		var reductions = m.inputs.defenceReductions;
		String[] def = { "def" };

		if (reductions.accursed) {
			m = newSkills(m, floor, new String[] { "def", "magic" },
				new double[] { trunc(m.skills.def * 17 / 20), trunc(m.skills.magic * 17 / 20) });
		} else if (reductions.vulnerability) {
			m = newSkills(m, floor, def, new double[] { trunc(m.skills.def * 9 / 10) });
		}

		for (int i = 0; i < reductions.elderMaul; i++)
			m = newSkills(m, floor, def, new double[] { m.skills.def - trunc(m.skills.def * 35 / 100) });
		for (int i = 0; i < reductions.dwh; i++)
			m = newSkills(m, floor, def, new double[] { m.skills.def - trunc(m.skills.def * 3 / 10) });

		boolean demon = m.attributes.contains(MonsterAttribute.DEMON);
		m = reduceArclight(m, baseSkills, floor, reductions.arclight, demon ? 2 : 1, 20);
		m = reduceArclight(m, baseSkills, floor, reductions.emberlight, demon ? 3 : 1, 20);

		for (int i = 0; i < reductions.tonalztic; i++)
			m = newSkills(m, floor, def, new double[] { m.skills.def - trunc(m.skills.magic / 8) });

		if (reductions.seercull > 0)
			m = newSkills(m, floor, new String[] { "magic" }, new double[] { m.skills.magic - reductions.seercull });

		double bgsDmg = reductions.bgs;
		if (bgsDmg > 0) {
			// order matters here
			for (String k : new String[] { "def", "str", "atk", "magic", "ranged" }) {
				double startLevel = getSkill(m.skills, k);
				m = newSkills(m, floor, new String[] { k }, new double[] { startLevel - bgsDmg });
				if (getSkill(m.skills, k) > 0) {
					// if a skill fails to drain to 0, even if because of a drain floor, the bgs does not propagate further
					bgsDmg = 0;
				} else {
					bgsDmg -= startLevel;
				}
			}
		}

		if (reductions.ayak > 0 && m.defensive.magic > 0) {
			var copy = m.copy();
			copy.defensive = m.defensive.copy();
			copy.defensive.magic = Math.max(0, m.defensive.magic - reductions.ayak);
			m = copy;
		}

		return m;
	}

	private MonsterScaling() {
	}
}
