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
 * Monster attributes, as the strings used in the monster data
 */
public final class MonsterAttribute
{
	public static final String DEMON = "demon";
	public static final String DRAGON = "dragon";
	public static final String FIERY = "fiery";
	public static final String FLYING = "flying";
	public static final String GOLEM = "golem";
	public static final String KALPHITE = "kalphite";
	public static final String LEAFY = "leafy";
	public static final String PENANCE = "penance";
	public static final String RAT = "rat";
	public static final String SHADE = "shade";
	public static final String SPECTRAL = "spectral";
	public static final String UNDEAD = "undead";
	public static final String VAMPYRE_1 = "vampyre1";
	public static final String VAMPYRE_2 = "vampyre2";
	public static final String VAMPYRE_3 = "vampyre3";
	public static final String XERICIAN = "xerician";

	public static boolean isVampyre(String attr) {
		return VAMPYRE_1.equals(attr) || VAMPYRE_2.equals(attr) || VAMPYRE_3.equals(attr);
	}

	public static boolean isVampyre(List<String> attrs) {
		return attrs.stream().anyMatch(MonsterAttribute::isVampyre);
	}

	private MonsterAttribute() {
	}
}
