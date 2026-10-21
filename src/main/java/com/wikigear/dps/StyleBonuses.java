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
 * Attack or defence bonuses per combat style (PlayerOffensive / PlayerDefensive)
 */
public class StyleBonuses
{
	public double stab;
	public double slash;
	public double crush;
	public double magic;
	public double ranged;

	public double get(String type) {
		switch (type) {
			case "stab":
				return stab;
			case "slash":
				return slash;
			case "crush":
				return crush;
			case "magic":
				return magic;
			case "ranged":
				return ranged;
			default:
				throw new IllegalArgumentException("Unknown style " + type);
		}
	}

	public StyleBonuses copy() {
		var b = new StyleBonuses();
		b.stab = stab;
		b.slash = slash;
		b.crush = crush;
		b.magic = magic;
		b.ranged = ranged;
		return b;
	}
}
