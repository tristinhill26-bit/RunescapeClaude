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

import java.util.Arrays;
import java.util.List;

/**
 * Weapon categories, as the strings used in the equipment data
 *
 * @see <a href="https://oldschool.runescape.wiki/w/Property:Combat_style">Property:Combat style</a>
 */
public final class EquipmentCategory
{
	public static final String NONE = "";
	public static final String TWO_HANDED_SWORD = "2h Sword";
	public static final String AXE = "Axe";
	public static final String BANNER = "Banner";
	public static final String BLADED_STAFF = "Bladed Staff";
	public static final String BLASTER = "Blaster";
	public static final String BLUDGEON = "Bludgeon";
	public static final String BLUNT = "Blunt";
	public static final String BOW = "Bow";
	public static final String BULWARK = "Bulwark";
	public static final String CHINCHOMPA = "Chinchompas";
	public static final String CLAW = "Claw";
	public static final String CROSSBOW = "Crossbow";
	public static final String DAGGER = "Dagger";
	public static final String FLAIL = "Flail";
	public static final String GUN = "Gun";
	public static final String MULTI_MELEE = "Multi-Melee";
	public static final String PARTISAN = "Partisan";
	public static final String PICKAXE = "Pickaxe";
	public static final String POLEARM = "Polearm";
	public static final String POLESTAFF = "Polestaff";
	public static final String POWERED_STAFF = "Powered Staff";
	public static final String POWERED_WAND = "Powered Wand";
	public static final String SALAMANDER = "Salamander";
	public static final String SCYTHE = "Scythe";
	public static final String SLASH_SWORD = "Slash Sword";
	public static final String SPEAR = "Spear";
	public static final String SPIKED = "Spiked";
	public static final String STAB_SWORD = "Stab Sword";
	public static final String STAFF = "Staff";
	public static final String THROWN = "Thrown";
	public static final String UNARMED = "Unarmed";
	public static final String WHIP = "Whip";

	public static final List<String> MAGIC_WEAPONS = Arrays.asList(STAFF, POWERED_WAND, POWERED_STAFF, BLADED_STAFF, POLESTAFF);

	private EquipmentCategory() {
	}
}
