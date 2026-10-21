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

public class PlayerSkills
{
	public double atk;
	public double def;
	public double hp;
	public double magic;
	public double prayer;
	public double ranged;
	public double str;
	public double mining;
	public double herblore;

	public PlayerSkills copy() {
		var s = new PlayerSkills();
		s.atk = atk;
		s.def = def;
		s.hp = hp;
		s.magic = magic;
		s.prayer = prayer;
		s.ranged = ranged;
		s.str = str;
		s.mining = mining;
		s.herblore = herblore;
		return s;
	}
}
