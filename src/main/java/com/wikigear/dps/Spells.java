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

/**
 * Spell lookups. {@link #spells} is filled in from the calculator's spell data.
 */
public final class Spells
{
	public static List<Spell> spells = new ArrayList<>();

	public static Spell spellByName(String name) {
		for (Spell s : spells)
			if (s.name.equals(name))
				return s;
		return null;
	}

	public static boolean isBindSpell(Spell spell) {
		return spell != null && (spell.name.equals("Bind") || spell.name.equals("Snare") || spell.name.equals("Entangle"));
	}

	public static double getSpellMaxHit(Spell spell, double magicLevel) {
		if (spell.element == null || spell.element.isEmpty())
			return spell.max_hit;

		String[] words = spell.name.split(" ");
		String spellClass = words.length > 1 ? words[1] : "";
		switch (spellClass) {
			case "Strike":
				return pick(spellClass, magicLevel, 13, 9, 5);
			case "Bolt":
				return pick(spellClass, magicLevel, 35, 29, 23);
			case "Blast":
				return pick(spellClass, magicLevel, 59, 53, 47);
			case "Wave":
				return pick(spellClass, magicLevel, 75, 70, 65);
			case "Surge":
				return pick(spellClass, magicLevel, 95, 90, 85);
			default:
				throw new IllegalArgumentException("No dynamic max hit available for " + spell);
		}
	}

	private static double pick(String spellClass, double magicLevel, int fire, int earth, int water) {
		String element;
		if (magicLevel >= fire)
			element = "Fire";
		else if (magicLevel >= earth)
			element = "Earth";
		else if (magicLevel >= water)
			element = "Water";
		else
			element = "Wind";
		return spellByName(element + " " + spellClass).max_hit;
	}

	public static boolean canUseSunfireRunes(Spell spell) {
		return spell != null && "fire".equals(spell.element);
	}

	private Spells() {
	}
}
