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
 * Number helpers, using doubles like the calculator's JavaScript numbers
 */
public final class MathUtil
{
	public static double trunc(double x) {
		return x < 0 ? Math.ceil(x) : Math.floor(x);
	}

	public static double lerp(double curr, double srcStart, double srcEnd, double dstStart, double dstEnd) {
		double srcRange = srcEnd - srcStart;
		double dstRange = dstEnd - dstStart;
		double currNorm = curr - srcStart;
		return trunc(((currNorm * dstRange) / srcRange) + dstStart);
	}

	public static double iSqrt(double x) {
		return trunc(Math.sqrt(x));
	}

	public static double iLerp(double x1, double x2, double y1, double y2, double yc) {
		return x1 + trunc((x2 - x1) * (yc - y1) / (y2 - y1));
	}

	public static double addPercent(double x, double pct) {
		return x + trunc(x * pct / 100);
	}

	/**
	 * Count of permutations of k selections from n items
	 */
	public static double choose(double k, double n) {
		double acc = 1;
		for (int i = 1; i <= k; i++) {
			acc *= n - i + 1;
			acc /= i;
		}
		return acc;
	}

	/**
	 * Chance of exactly k events happening in n trials
	 */
	public static double binomial(double p, double k, double n) {
		return choose(k, n) * Math.pow(p, k) * Math.pow(1 - p, n - k);
	}

	/**
	 * JavaScript's Math.round, which rounds halves up
	 */
	public static double round(double x) {
		return Math.floor(x + 0.5);
	}

	/**
	 * JavaScript's ToInt32, for bitwise operators
	 */
	public static int toInt32(double x) {
		if (Double.isNaN(x) || Double.isInfinite(x))
			return 0;
		return (int) (long) trunc(x);
	}

	private MathUtil() {
	}
}
