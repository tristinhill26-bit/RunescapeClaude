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
import static com.wikigear.dps.EquipmentCategory.*;

/**
 * Combat styles available to each weapon category
 */
public final class CombatStyles
{
	public static boolean isCombatStyleType(String s) {
		return s == null || s.equals("stab") || s.equals("slash") || s.equals("crush") || s.equals("magic") || s.equals("ranged");
	}

	/**
	 * light, standard, heavy or mixed
	 */
	public static String getRangedDamageType(String category) {
		switch (category) {
			case THROWN:
				return "light";
			case BOW:
				return "standard";
			case CROSSBOW:
			case CHINCHOMPA:
				return "heavy";
			case SALAMANDER:
				return "mixed";
			default:
				throw new IllegalArgumentException("Not a ranged weapon category: " + category);
		}
	}

	public static List<CombatStyle> getCombatStylesForCategory(String category) {
		var ret = new ArrayList<CombatStyle>();
		switch (category) {
			case TWO_HANDED_SWORD:
				add(ret, "Chop", "slash", "Accurate");
				add(ret, "Slash", "slash", "Aggressive");
				add(ret, "Smash", "crush", "Aggressive");
				add(ret, "Block", "slash", "Defensive");
				break;
			case BANNER:
				add(ret, "Lunge", "stab", "Accurate");
				add(ret, "Swipe", "slash", "Aggressive");
				add(ret, "Pound", "crush", "Controlled");
				add(ret, "Block", "stab", "Defensive");
				break;
			case BLADED_STAFF:
				add(ret, "Jab", "stab", "Accurate");
				add(ret, "Swipe", "slash", "Aggressive");
				add(ret, "Fend", "crush", "Defensive");
				add(ret, "Spell", "magic", "Defensive Autocast");
				add(ret, "Spell", "magic", "Autocast");
				break;
			case BLASTER:
				return ret;
			case BOW:
			case CROSSBOW:
			case THROWN:
				add(ret, "Accurate", "ranged", "Accurate");
				add(ret, "Rapid", "ranged", "Rapid");
				add(ret, "Longrange", "ranged", "Longrange");
				break;
			case GUN:
				add(ret, "Kick", "crush", "Aggressive");
				break;
			case BULWARK:
				add(ret, "Pummel", "crush", "Accurate");
				add(ret, "Block", null, null);
				break;
			case MULTI_MELEE:
				add(ret, "Poke", "stab", "Accurate");
				add(ret, "Slash", "slash", "Aggressive");
				add(ret, "Pound", "crush", "Aggressive");
				add(ret, "Block", "slash", "Defensive");
				break;
			case PARTISAN:
				add(ret, "Stab", "stab", "Accurate");
				add(ret, "Lunge", "stab", "Aggressive");
				add(ret, "Pound", "crush", "Aggressive");
				add(ret, "Block", "stab", "Defensive");
				break;
			case PICKAXE:
				add(ret, "Spike", "stab", "Accurate");
				add(ret, "Impale", "stab", "Aggressive");
				add(ret, "Smash", "crush", "Aggressive");
				add(ret, "Block", "stab", "Defensive");
				break;
			case POLEARM:
				add(ret, "Jab", "stab", "Controlled");
				add(ret, "Swipe", "slash", "Aggressive");
				add(ret, "Fend", "stab", "Defensive");
				break;
			case POWERED_STAFF:
			case POWERED_WAND:
				add(ret, "Accurate", "magic", "Accurate");
				add(ret, "Accurate", "magic", "Accurate");
				add(ret, "Longrange", "magic", "Longrange");
				break;
			case SALAMANDER:
				add(ret, "Scorch", "slash", "Aggressive");
				add(ret, "Flare", "ranged", "Rapid");
				add(ret, "Blaze", "magic", "Defensive");
				break;
			case CHINCHOMPA:
				add(ret, "Short fuse", "ranged", "Accurate");
				add(ret, "Medium fuse", "ranged", "Rapid");
				add(ret, "Long fuse", "ranged", "Longrange");
				break;
			case CLAW:
				add(ret, "Chop", "slash", "Accurate");
				add(ret, "Slash", "slash", "Aggressive");
				add(ret, "Lunge", "stab", "Controlled");
				add(ret, "Block", "slash", "Defensive");
				break;
			case BLUDGEON:
				add(ret, "Pound", "crush", "Aggressive");
				add(ret, "Pummel", "crush", "Aggressive");
				add(ret, "Smash", "crush", "Aggressive");
				break;
			case BLUNT:
				add(ret, "Pound", "crush", "Accurate");
				add(ret, "Pummel", "crush", "Aggressive");
				add(ret, "Block", "crush", "Defensive");
				break;
			case POLESTAFF:
				add(ret, "Bash", "crush", "Accurate");
				add(ret, "Pound", "crush", "Aggressive");
				add(ret, "Block", "crush", "Defensive");
				break;
			case SPIKED:
				add(ret, "Pound", "crush", "Accurate");
				add(ret, "Pummel", "crush", "Aggressive");
				add(ret, "Spike", "stab", "Controlled");
				add(ret, "Block", "crush", "Defensive");
				break;
			case STAFF:
				add(ret, "Bash", "crush", "Accurate");
				add(ret, "Pound", "crush", "Aggressive");
				add(ret, "Focus", "crush", "Defensive");
				add(ret, "Spell", "magic", "Defensive Autocast");
				add(ret, "Spell", "magic", "Autocast");
				break;
			case AXE:
				add(ret, "Chop", "slash", "Accurate");
				add(ret, "Hack", "slash", "Aggressive");
				add(ret, "Smash", "crush", "Aggressive");
				add(ret, "Block", "slash", "Defensive");
				break;
			case NONE:
			case UNARMED:
				add(ret, "Punch", "crush", "Accurate");
				add(ret, "Kick", "crush", "Aggressive");
				add(ret, "Block", "crush", "Defensive");
				break;
			case SCYTHE:
				add(ret, "Reap", "slash", "Accurate");
				add(ret, "Chop", "slash", "Aggressive");
				add(ret, "Jab", "crush", "Aggressive");
				add(ret, "Block", "slash", "Defensive");
				break;
			case SLASH_SWORD:
				add(ret, "Chop", "slash", "Accurate");
				add(ret, "Slash", "slash", "Aggressive");
				add(ret, "Lunge", "stab", "Controlled");
				add(ret, "Block", "slash", "Defensive");
				break;
			case SPEAR:
				add(ret, "Lunge", "stab", "Controlled");
				add(ret, "Swipe", "slash", "Controlled");
				add(ret, "Pound", "crush", "Controlled");
				add(ret, "Block", "stab", "Defensive");
				break;
			case STAB_SWORD:
				add(ret, "Stab", "stab", "Accurate");
				add(ret, "Lunge", "stab", "Aggressive");
				add(ret, "Slash", "slash", "Aggressive");
				add(ret, "Block", "stab", "Defensive");
				break;
			case WHIP:
				add(ret, "Flick", "slash", "Accurate");
				add(ret, "Lash", "slash", "Controlled");
				add(ret, "Deflect", "slash", "Defensive");
				break;
			case FLAIL:
				add(ret, "Chop", "slash", "Accurate");
				add(ret, "Slash", "slash", "Aggressive");
				add(ret, "Block", "slash", "Defensive");
				break;
			default:
				break;
		}

		// A pseudo combat style for manual casting
		add(ret, "Spell", "magic", "Manual Cast");
		return ret;
	}

	private static void add(List<CombatStyle> list, String name, String type, String stance) {
		list.add(new CombatStyle(name, type, stance));
	}

	private CombatStyles() {
	}
}
