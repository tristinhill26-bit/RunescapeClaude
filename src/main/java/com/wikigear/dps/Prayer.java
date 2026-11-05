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

import java.util.List;

/**
 * Prayers, with ordinals matching the calculator's Prayer enum
 */
public enum Prayer
{
	BURST_OF_STRENGTH("Burst of Strength", "melee", 0, null, f(105), null, null),
	CLARITY_OF_THOUGHT("Clarity of Thought", "melee", 0, f(105), null, null, null),
	SHARP_EYE("Sharp Eye", "ranged", 0, f(105), f(105), null, null),
	MYSTIC_WILL("Mystic Will", "magic", 0, f(105), null, null, f(105)),
	SUPERHUMAN_STRENGTH("Superhuman Strength", "melee", 0, null, f(110), null, null),
	IMPROVED_REFLEXES("Improved Reflexes", "melee", 0, f(110), null, null, null),
	HAWK_EYE("Hawk Eye", "ranged", 0, f(110), f(110), null, null),
	MYSTIC_LORE("Mystic Lore", "magic", 10, f(110), null, null, f(110)),
	ULTIMATE_STRENGTH("Ultimate Strength", "melee", 0, null, f(115), null, null),
	INCREDIBLE_REFLEXES("Incredible Reflexes", "melee", 0, f(115), null, null, null),
	EAGLE_EYE("Eagle Eye", "ranged", 0, f(115), f(115), null, null),
	MYSTIC_MIGHT("Mystic Might", "magic", 20, f(115), null, null, f(115)),
	CHIVALRY("Chivalry", "melee", 0, f(115), f(118), f(120), null),
	PIETY("Piety", "melee", 0, f(120), f(123), f(125), null),
	RIGOUR("Rigour", "ranged", 0, f(120), f(123), f(125), null),
	AUGURY("Augury", "magic", 40, f(125), null, f(125), f(125)),
	THICK_SKIN("Thick Skin", null, 0, null, null, f(105), null),
	ROCK_SKIN("Rock Skin", null, 0, null, null, f(110), null),
	STEEL_SKIN("Steel Skin", null, 0, null, null, f(115), null),
	DEADEYE("Deadeye", "ranged", 0, f(118), f(118), f(105), null),
	MYSTIC_VIGOUR("Mystic Vigour", "magic", 30, f(118), null, f(105), f(118));

	public static final List<Prayer> BRAIN_PRAYERS = List.of(CLARITY_OF_THOUGHT, IMPROVED_REFLEXES, INCREDIBLE_REFLEXES);
	public static final List<Prayer> ARM_PRAYERS = List.of(BURST_OF_STRENGTH, SUPERHUMAN_STRENGTH, ULTIMATE_STRENGTH);

	public final String displayName;
	/**
	 * melee, ranged, magic or null
	 */
	public final String combatStyle;
	public final double magicDamageBonus;
	/**
	 * Factors as {factor, divisor}, or null
	 */
	public final double[] factorAccuracy;
	public final double[] factorStrength;
	public final double[] factorDefence;
	public final double[] factorDefenceMagic;

	Prayer(
		String displayName, String combatStyle, double magicDamageBonus,
		double[] factorAccuracy, double[] factorStrength, double[] factorDefence, double[] factorDefenceMagic
	) {
		this.displayName = displayName;
		this.combatStyle = combatStyle;
		this.magicDamageBonus = magicDamageBonus;
		this.factorAccuracy = factorAccuracy;
		this.factorStrength = factorStrength;
		this.factorDefence = factorDefence;
		this.factorDefenceMagic = factorDefenceMagic;
	}

	private static double[] f(double factor) {
		return new double[] { factor, 100 };
	}

	public static Prayer of(int ordinal) {
		return values()[ordinal];
	}
}
