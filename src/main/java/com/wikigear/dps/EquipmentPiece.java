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
 * An item from the calculator's equipment data
 */
public class EquipmentPiece
{
	public String name;
	public int id;
	public double weight;
	public String version;
	public String slot;
	public String image;
	public double speed;
	public String category;
	public boolean isTwoHanded;
	public Bonuses bonuses;
	public StyleBonuses offensive;
	public StyleBonuses defensive;
	public ItemVars itemVars;

	public static class ItemVars
	{
		public String blowpipeDartName;
		public Integer blowpipeDartId;

		public ItemVars copy() {
			var v = new ItemVars();
			v.blowpipeDartName = blowpipeDartName;
			v.blowpipeDartId = blowpipeDartId;
			return v;
		}
	}

	public EquipmentPiece copy() {
		var e = new EquipmentPiece();
		e.name = name;
		e.id = id;
		e.weight = weight;
		e.version = version;
		e.slot = slot;
		e.image = image;
		e.speed = speed;
		e.category = category;
		e.isTwoHanded = isTwoHanded;
		e.bonuses = bonuses;
		e.offensive = offensive;
		e.defensive = defensive;
		e.itemVars = itemVars;
		return e;
	}

	@Override
	public String toString() {
		return name + (version == null || version.isEmpty() ? "" : "#" + version) + " (" + id + ")";
	}
}
