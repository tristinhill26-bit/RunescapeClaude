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
 * Common hit transformers
 */
public final class Transformers
{
	public static HitTransformer flatLimit(double maximum) {
		return flatLimit(maximum, 0);
	}

	public static HitTransformer flatLimit(double maximum, double minimum) {
		return h -> new HitDistribution(one(Math.max(minimum, Math.min(h.damage, maximum)), h.accurate));
	}

	public static HitTransformer linearMin(double maximum) {
		return linearMin(maximum, 0);
	}

	public static HitTransformer linearMin(double maximum, double offset) {
		return h -> {
			var d = new HitDistribution();
			double prob = 1.0 / (maximum + 1);
			for (double i = 0; i <= maximum; i++)
				d.addHit(new WeightedHit(prob, new Hitsplat[] { new Hitsplat(Math.min(h.damage, i + offset), h.accurate) }));
			return d.flatten();
		};
	}

	public static HitTransformer cappedReroll(double limit, double rollMax) {
		return cappedReroll(limit, rollMax, 0);
	}

	public static HitTransformer cappedReroll(double limit, double rollMax, double offset) {
		return h -> {
			if (h.damage <= limit)
				return new HitDistribution(new WeightedHit(1.0, new Hitsplat[] { h }));

			var d = new HitDistribution();
			double prob = 1.0 / (rollMax + 1);
			for (double i = 0; i <= rollMax; i++)
				d.addHit(new WeightedHit(prob, new Hitsplat[] { new Hitsplat(i + offset, h.accurate) }));
			return d.flatten();
		};
	}

	public static HitTransformer multiply(double numerator) {
		return multiply(numerator, 1, 0);
	}

	public static HitTransformer multiply(double numerator, double divisor) {
		return multiply(numerator, divisor, 0);
	}

	public static HitTransformer multiply(double numerator, double divisor, double minimum) {
		return h -> {
			double dmg = MathUtil.trunc(numerator * h.damage / divisor);
			if (minimum != 0) {
				if (h.damage >= minimum) {
					// if the value started above the minimum, make sure it doesn't drop below
					dmg = Math.max(minimum, dmg);
				} else {
					// if the value started below the minimum, make sure it isn't reduced, but respect increases
					dmg = Math.max(h.damage, dmg);
				}
			}
			return new HitDistribution(one(dmg, h.accurate));
		};
	}

	public static HitTransformer division(double divisor) {
		return multiply(1, divisor, 0);
	}

	public static HitTransformer division(double divisor, double minimum) {
		return multiply(1, divisor, minimum);
	}

	public static HitTransformer flatAdd(double addend) {
		return flatAdd(addend, 0);
	}

	public static HitTransformer flatAdd(double addend, double minimum) {
		return h -> new HitDistribution(one(Math.max(minimum, h.damage + addend), h.accurate));
	}

	private static WeightedHit one(double damage, boolean accurate) {
		return new WeightedHit(1.0, new Hitsplat[] { new Hitsplat(damage, accurate) });
	}

	private Transformers() {
	}
}
