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

public class Monster
{
	public int id;
	public String name;
	public String image;
	public String version;
	public double size;
	public double speed;
	/**
	 * stab, slash, crush, magic, ranged or null
	 */
	public String style;
	public double maxHit;
	public Skills skills;
	public Offensive offensive;
	public Defensive defensive;
	public List<String> attributes = new ArrayList<>();
	public Weakness weakness;
	public Immunities immunities;
	public boolean is_slayer_monster;
	public Inputs inputs;

	public static class Skills
	{
		public double atk;
		public double def;
		public double hp;
		public double magic;
		public double ranged;
		public double str;

		public Skills copy() {
			var s = new Skills();
			s.atk = atk;
			s.def = def;
			s.hp = hp;
			s.magic = magic;
			s.ranged = ranged;
			s.str = str;
			return s;
		}
	}

	public static class Offensive
	{
		public double atk;
		public double magic;
		public double magic_str;
		public double ranged;
		public double ranged_str;
		public double str;

		public Offensive copy() {
			var o = new Offensive();
			o.atk = atk;
			o.magic = magic;
			o.magic_str = magic_str;
			o.ranged = ranged;
			o.ranged_str = ranged_str;
			o.str = str;
			return o;
		}
	}

	public static class Defensive
	{
		public double flat_armour;
		public double stab;
		public double slash;
		public double crush;
		public double magic;
		public double light;
		public double standard;
		public double heavy;

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
				case "light":
					return light;
				case "standard":
					return standard;
				case "heavy":
					return heavy;
				default:
					throw new IllegalArgumentException("Unknown defence type " + type);
			}
		}

		public Defensive copy() {
			var d = new Defensive();
			d.flat_armour = flat_armour;
			d.stab = stab;
			d.slash = slash;
			d.crush = crush;
			d.magic = magic;
			d.light = light;
			d.standard = standard;
			d.heavy = heavy;
			return d;
		}
	}

	public static class Weakness
	{
		public String element;
		public double severity;
	}

	public static class Immunities
	{
		/**
		 * Weak, Normal, Strong or null
		 */
		public String burn;
	}

	public static class Inputs
	{
		public boolean isFromCoxCm;
		public double toaInvocationLevel;
		public double toaPathLevel;
		public double partyMaxCombatLevel;
		public Double partyAvgMiningLevel;
		public double partySumMiningLevel;
		public double partyMaxHpLevel;
		public double partySize;
		public double monsterCurrentHp;
		public DefenceReductions defenceReductions;
		public Double demonbaneVulnerability;
		public String tormentedDemonPhase;
		public String phase;

		public Inputs copy() {
			var i = new Inputs();
			i.isFromCoxCm = isFromCoxCm;
			i.toaInvocationLevel = toaInvocationLevel;
			i.toaPathLevel = toaPathLevel;
			i.partyMaxCombatLevel = partyMaxCombatLevel;
			i.partyAvgMiningLevel = partyAvgMiningLevel;
			i.partySumMiningLevel = partySumMiningLevel;
			i.partyMaxHpLevel = partyMaxHpLevel;
			i.partySize = partySize;
			i.monsterCurrentHp = monsterCurrentHp;
			i.defenceReductions = defenceReductions;
			i.demonbaneVulnerability = demonbaneVulnerability;
			i.tormentedDemonPhase = tormentedDemonPhase;
			i.phase = phase;
			return i;
		}

		public static Inputs initial() {
			var i = new Inputs();
			i.isFromCoxCm = false;
			i.toaInvocationLevel = 0;
			i.toaPathLevel = 0;
			i.partyMaxCombatLevel = 126;
			i.partySumMiningLevel = 99;
			i.partyMaxHpLevel = 99;
			i.partySize = 1;
			i.monsterCurrentHp = 150;
			i.defenceReductions = new DefenceReductions();
			return i;
		}
	}

	public static class DefenceReductions
	{
		public boolean vulnerability;
		public boolean accursed;
		public double elderMaul;
		public double dwh;
		public double arclight;
		public double emberlight;
		public double bgs;
		public double tonalztic;
		public double seercull;
		public double ayak;

		public DefenceReductions copy() {
			var d = new DefenceReductions();
			d.vulnerability = vulnerability;
			d.accursed = accursed;
			d.elderMaul = elderMaul;
			d.dwh = dwh;
			d.arclight = arclight;
			d.emberlight = emberlight;
			d.bgs = bgs;
			d.tonalztic = tonalztic;
			d.seercull = seercull;
			d.ayak = ayak;
			return d;
		}
	}

	/**
	 * A shallow copy, like the calculator's object spread
	 */
	public Monster copy() {
		var m = new Monster();
		m.id = id;
		m.name = name;
		m.image = image;
		m.version = version;
		m.size = size;
		m.speed = speed;
		m.style = style;
		m.maxHit = maxHit;
		m.skills = skills;
		m.offensive = offensive;
		m.defensive = defensive;
		m.attributes = attributes;
		m.weakness = weakness;
		m.immunities = immunities;
		m.is_slayer_monster = is_slayer_monster;
		m.inputs = inputs;
		return m;
	}

	@Override
	public String toString() {
		return name + (version == null || version.isEmpty() ? "" : "#" + version) + " (" + id + ")";
	}
}
