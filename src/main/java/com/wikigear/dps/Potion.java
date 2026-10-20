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

/**
 * Boosting potions, with ordinals matching the calculator's Potion enum
 */
public enum Potion
{
	ANCIENT("Ancient brew"),
	ATTACK("Attack potion"),
	FORGOTTEN_BREW("Forgotten brew"),
	IMBUED_HEART("Imbued heart"),
	MAGIC("Magic potion"),
	OVERLOAD("Overload"),
	OVERLOAD_PLUS("Overload (+)"),
	RANGING("Ranging potion"),
	SATURATED_HEART("Saturated heart"),
	SMELLING_SALTS("Smelling salts"),
	STRENGTH("Strength potion"),
	SUPER_ATTACK("Super attack"),
	SUPER_STRENGTH("Super strength"),
	SUPER_RANGING("Super ranging"),
	SUPER_COMBAT("Super combat"),
	SUPER_MAGIC("Super magic"),
	DEFENCE("Defence potion"),
	SUPER_DEFENCE("Super defence"),
	MOONLIGHT("Moonlight potion"),
	RUBY_HARVEST("Ruby Harvest"),
	BLACK_WARLOCK("Black Warlock"),
	SAPPHIRE_GLACIALIS("Sapphire Glacialis");

	public final String displayName;

	Potion(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * The boosts this potion gives, with null for skills it doesn't change (PotionMap's calculateFn)
	 */
	public Double[] boosts(PlayerSkills s) {
		Double atk = null, str = null, def = null, magic = null, ranged = null;
		switch (this) {
			case OVERLOAD_PLUS:
				atk = floor(6 + s.atk * 0.16);
				str = floor(6 + s.str * 0.16);
				def = floor(6 + s.def * 0.16);
				magic = floor(6 + s.magic * 0.16);
				ranged = floor(6 + s.ranged * 0.16);
				break;
			case SMELLING_SALTS:
				atk = floor(11 + s.atk * 0.16);
				str = floor(11 + s.str * 0.16);
				def = floor(11 + s.def * 0.16);
				magic = floor(11 + s.magic * 0.16);
				ranged = floor(11 + s.ranged * 0.16);
				break;
			case SUPER_COMBAT:
				atk = floor(5 + s.atk * 0.15);
				str = floor(5 + s.str * 0.15);
				def = floor(5 + s.def * 0.15);
				break;
			case RANGING:
				ranged = floor(4 + s.ranged * 0.1);
				break;
			case SATURATED_HEART:
				magic = floor(4 + s.magic * 0.1);
				break;
			case IMBUED_HEART:
				magic = floor(1 + s.magic * 0.1);
				break;
			case FORGOTTEN_BREW:
				magic = floor(3 + s.magic * 0.08);
				atk = floor(-2 - s.atk * 0.1);
				str = floor(-2 - s.str * 0.1);
				def = floor(-2 - s.def * 0.1);
				break;
			case SUPER_ATTACK:
				atk = floor(5 + s.atk * 0.15);
				break;
			case SUPER_STRENGTH:
				str = floor(5 + s.str * 0.15);
				break;
			case ANCIENT:
				magic = floor(2 + s.magic * 0.05);
				atk = floor(-2 - s.atk * 0.1);
				str = floor(-2 - s.str * 0.1);
				def = floor(-2 - s.def * 0.1);
				break;
			case OVERLOAD:
				atk = floor(5 + s.atk * 0.13);
				str = floor(5 + s.str * 0.13);
				def = floor(5 + s.def * 0.13);
				magic = floor(5 + s.magic * 0.13);
				ranged = floor(5 + s.ranged * 0.13);
				break;
			case MAGIC:
				magic = 4d;
				break;
			case ATTACK:
				atk = floor(3 + s.atk * 0.1);
				break;
			case STRENGTH:
				str = floor(3 + s.str * 0.1);
				break;
			case SUPER_RANGING:
				ranged = floor(5 + s.ranged * 0.15);
				break;
			case SUPER_MAGIC:
				magic = floor(5 + s.magic * 0.15);
				break;
			case DEFENCE:
				def = floor(3 + s.def * 0.1);
				break;
			case SUPER_DEFENCE:
				def = floor(5 + s.def * 0.15);
				break;
			case RUBY_HARVEST:
				atk = floor(4 + s.atk * 0.15);
				break;
			case BLACK_WARLOCK:
				str = floor(4 + s.str * 0.15);
				break;
			case SAPPHIRE_GLACIALIS:
				def = floor(4 + s.def * 0.15);
				break;
			case MOONLIGHT:
				if (s.herblore >= 45)
					atk = trunc(5 + s.atk * 0.15);
				else if (s.herblore >= 3)
					atk = trunc(3 + s.atk * 0.10);
				if (s.herblore >= 55)
					str = trunc(5 + s.str * 0.15);
				else if (s.herblore >= 12)
					str = trunc(3 + s.str * 0.10);
				if (s.herblore >= 70)
					def = trunc(7 + s.def * 0.20);
				else if (s.herblore >= 66)
					def = trunc(5 + s.def * 0.15);
				else if (s.herblore >= 30)
					def = trunc(3 + s.def * 0.10);
				break;
		}
		return new Double[] { atk, str, def, magic, ranged };
	}

	/**
	 * Combined boosts of several potions, taking the highest boost to each skill like the calculator does
	 */
	public static PlayerSkills combinedBoosts(Iterable<Potion> potions, PlayerSkills skills) {
		var boosts = new PlayerSkills();
		for (Potion p : potions) {
			Double[] b = p.boosts(skills);
			if (b[0] != null && b[0] > boosts.atk)
				boosts.atk = b[0];
			if (b[1] != null && b[1] > boosts.str)
				boosts.str = b[1];
			if (b[2] != null && b[2] > boosts.def)
				boosts.def = b[2];
			if (b[3] != null && b[3] > boosts.magic)
				boosts.magic = b[3];
			if (b[4] != null && b[4] > boosts.ranged)
				boosts.ranged = b[4];
		}
		return boosts;
	}

	private static double floor(double x) {
		return Math.floor(x);
	}

	private static double trunc(double x) {
		return MathUtil.trunc(x);
	}
}
