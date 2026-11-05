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
 * The player's worn equipment, by slot
 */
public class PlayerEquipment
{
	/**
	 * Slot names in the order the calculator's equipment object declares them
	 */
	public static final String[] SLOTS = {
		"head", "cape", "neck", "ammo", "weapon", "body", "shield", "legs", "hands", "feet", "ring"
	};

	public EquipmentPiece head;
	public EquipmentPiece cape;
	public EquipmentPiece neck;
	public EquipmentPiece ammo;
	public EquipmentPiece weapon;
	public EquipmentPiece body;
	public EquipmentPiece shield;
	public EquipmentPiece legs;
	public EquipmentPiece hands;
	public EquipmentPiece feet;
	public EquipmentPiece ring;

	public EquipmentPiece get(String slot) {
		switch (slot) {
			case "head":
				return head;
			case "cape":
				return cape;
			case "neck":
				return neck;
			case "ammo":
				return ammo;
			case "weapon":
				return weapon;
			case "body":
				return body;
			case "shield":
				return shield;
			case "legs":
				return legs;
			case "hands":
				return hands;
			case "feet":
				return feet;
			case "ring":
				return ring;
			default:
				throw new IllegalArgumentException("Unknown slot " + slot);
		}
	}

	public void set(String slot, EquipmentPiece piece) {
		switch (slot) {
			case "head":
				head = piece;
				break;
			case "cape":
				cape = piece;
				break;
			case "neck":
				neck = piece;
				break;
			case "ammo":
				ammo = piece;
				break;
			case "weapon":
				weapon = piece;
				break;
			case "body":
				body = piece;
				break;
			case "shield":
				shield = piece;
				break;
			case "legs":
				legs = piece;
				break;
			case "hands":
				hands = piece;
				break;
			case "feet":
				feet = piece;
				break;
			case "ring":
				ring = piece;
				break;
			default:
				throw new IllegalArgumentException("Unknown slot " + slot);
		}
	}

	/**
	 * Equipped pieces, skipping empty slots, in declaration order
	 */
	public List<EquipmentPiece> values() {
		var list = new ArrayList<EquipmentPiece>();
		for (String slot : SLOTS) {
			var piece = get(slot);
			if (piece != null)
				list.add(piece);
		}
		return list;
	}

	public PlayerEquipment copy() {
		var e = new PlayerEquipment();
		for (String slot : SLOTS)
			e.set(slot, get(slot));
		return e;
	}
}
