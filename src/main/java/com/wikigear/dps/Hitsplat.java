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

public final class Hitsplat
{
	public static final Hitsplat INACCURATE = new Hitsplat(0, false);

	public final double damage;
	public final boolean accurate;

	public Hitsplat(double damage) {
		this(damage, true);
	}

	public Hitsplat(double damage, boolean accurate) {
		this.damage = damage;
		this.accurate = accurate;
	}

	public HitDistribution transform(HitTransformer t, boolean transformInaccurate) {
		if (!accurate && !transformInaccurate)
			return new HitDistribution(new WeightedHit(1.0, new Hitsplat[] { this }));
		return t.apply(this);
	}
}
