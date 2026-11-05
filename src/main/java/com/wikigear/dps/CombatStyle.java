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
 * A weapon's combat style (PlayerCombatStyle). Type is stab, slash, crush, magic, ranged or null,
 * and stance is e.g. Accurate, Aggressive, Rapid, Autocast or Manual Cast.
 */
public class CombatStyle
{
	public String name;
	public String type;
	public String stance;

	public CombatStyle() {
	}

	public CombatStyle(String name, String type, String stance) {
		this.name = name;
		this.type = type;
		this.stance = stance;
	}

	@Override
	public String toString() {
		return name + " (" + type + ", " + stance + ")";
	}
}
