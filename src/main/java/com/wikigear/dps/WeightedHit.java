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

import java.math.BigInteger;
import java.util.Arrays;

public final class WeightedHit
{
	public final double probability;
	public final Hitsplat[] hitsplats;

	private boolean summed;
	private double sum;

	public WeightedHit(double probability, Hitsplat[] hitsplats) {
		this.probability = probability;
		this.hitsplats = hitsplats;
	}

	public WeightedHit scale(double factor) {
		return new WeightedHit(probability * factor, hitsplats.clone());
	}

	public WeightedHit zip(WeightedHit other) {
		Hitsplat[] h = Arrays.copyOf(hitsplats, hitsplats.length + other.hitsplats.length);
		System.arraycopy(other.hitsplats, 0, h, hitsplats.length, other.hitsplats.length);
		return new WeightedHit(probability * other.probability, h);
	}

	public HitDistribution transform(HitTransformer t, boolean transformInaccurate) {
		if (hitsplats.length == 1)
			return hitsplats[0].transform(t, transformInaccurate).scaleProbability(probability);

		// recursively zip first hitsplat with remaining hitsplats
		var head = new WeightedHit(probability, new Hitsplat[] { hitsplats[0] });
		var tail = new WeightedHit(1.0, Arrays.copyOfRange(hitsplats, 1, hitsplats.length));
		return head.transform(t, transformInaccurate).zip(tail.transform(t, transformInaccurate));
	}

	public boolean anyAccurate() {
		for (Hitsplat h : hitsplats)
			if (h.accurate)
				return true;
		return false;
	}

	public double getSum() {
		if (!summed) {
			double s = 0;
			for (Hitsplat h : hitsplats)
				s += h.damage;
			sum = s;
			summed = true;
		}
		return sum;
	}

	public double getExpectedValue() {
		return probability * getSum();
	}

	/**
	 * The calculator's BigInt hash, as a Long when it fits so equal hashes compare equal
	 */
	public Object getHash() {
		boolean small = hitsplats.length <= 6;
		for (Hitsplat h : hitsplats)
			small &= h.damage >= 0 && h.damage < 4096;
		if (small) {
			long acc = 0;
			for (Hitsplat h : hitsplats) {
				acc <<= 8;
				acc |= (long) h.damage;
				acc <<= 1;
				acc |= h.accurate ? 1 : 0;
			}
			return acc;
		}
		BigInteger acc = BigInteger.ZERO;
		for (Hitsplat h : hitsplats) {
			acc = acc.shiftLeft(8).or(BigInteger.valueOf((long) h.damage)).shiftLeft(1);
			if (h.accurate)
				acc = acc.or(BigInteger.ONE);
		}
		return acc.bitLength() < 64 ? (Object) acc.longValue() : acc;
	}
}
