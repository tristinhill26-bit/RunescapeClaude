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

public class Player
{
	public String name;
	public CombatStyle style;
	/**
	 * Base skill levels, before boosts
	 */
	public PlayerSkills skills;
	/**
	 * Added to the base skill levels to get the current levels
	 */
	public PlayerSkills boosts;
	public PlayerEquipment equipment;
	public double attackSpeed;
	/**
	 * {@link Prayer} ordinals
	 */
	public List<Integer> prayers = new ArrayList<>();
	public Bonuses bonuses;
	public StyleBonuses offensive;
	public StyleBonuses defensive;
	public Buffs buffs;
	public Spell spell;

	public static class Buffs
	{
		/**
		 * {@link Potion} ordinals, for display only
		 */
		public List<Integer> potions = new ArrayList<>();
		public boolean onSlayerTask;
		public boolean inWilderness;
		public boolean forinthrySurge;
		public double soulreaperStacks;
		public double baAttackerLevel;
		public double chinchompaDistance;
		public boolean kandarinDiary;
		public boolean chargeSpell;
		public boolean markOfDarknessSpell;
		public boolean usingSunfireRunes;

		public Buffs copy() {
			var b = new Buffs();
			b.potions = new ArrayList<>(potions);
			b.onSlayerTask = onSlayerTask;
			b.inWilderness = inWilderness;
			b.forinthrySurge = forinthrySurge;
			b.soulreaperStacks = soulreaperStacks;
			b.baAttackerLevel = baAttackerLevel;
			b.chinchompaDistance = chinchompaDistance;
			b.kandarinDiary = kandarinDiary;
			b.chargeSpell = chargeSpell;
			b.markOfDarknessSpell = markOfDarknessSpell;
			b.usingSunfireRunes = usingSunfireRunes;
			return b;
		}
	}

	/**
	 * A shallow copy, like the calculator's object spread
	 */
	public Player copy() {
		var p = new Player();
		p.name = name;
		p.style = style;
		p.skills = skills;
		p.boosts = boosts;
		p.equipment = equipment;
		p.attackSpeed = attackSpeed;
		p.prayers = prayers;
		p.bonuses = bonuses;
		p.offensive = offensive;
		p.defensive = defensive;
		p.buffs = buffs;
		p.spell = spell;
		return p;
	}
}
