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

import static com.wikigear.dps.MathUtil.trunc;

/**
 * Dragon claw and burning claw special attacks
 */
public final class Claws
{
	/**
	 * {chance per damage, low, high}
	 */
	private static double[] generateTotals(int accRoll, int totalRolls, double acc, double max, double highOffset) {
		double low = trunc(max * (totalRolls - accRoll) / 4);
		double high = max + low + highOffset;
		double chancePreviousRollsFail = Math.pow(1 - acc, accRoll);
		double chanceThisRollPasses = chancePreviousRollsFail * acc;
		double chancePerDmg = chanceThisRollPasses / (high - low + 1);
		return new double[] { chancePerDmg, low, high };
	}

	private static Hitsplat hit(double damage) {
		return new Hitsplat(damage);
	}

	public static AttackDistribution dClawDist(double acc, double max) {
		var dist = new HitDistribution();
		var miss = Hitsplat.INACCURATE;
		for (int accRoll = 0; accRoll < 4; accRoll++) {
			double[] t = generateTotals(accRoll, 4, acc, max, -1);
			double chancePerDmg = t[0];
			for (double dmg = t[1]; dmg <= t[2]; dmg++) {
				Hitsplat[] splats;
				switch (accRoll) {
					case 0:
						splats = new Hitsplat[] { hit(trunc(dmg / 2)), hit(trunc(dmg / 4)), hit(trunc(dmg / 8)), hit(trunc(dmg / 8) + 1) };
						break;
					case 1:
						splats = new Hitsplat[] { hit(trunc(dmg / 2)), hit(trunc(dmg / 4)), hit(trunc(dmg / 4) + 1), miss };
						break;
					case 2:
						splats = new Hitsplat[] { hit(trunc(dmg / 2)), hit(trunc(dmg / 2) + 1), miss, miss };
						break;
					default:
						splats = new Hitsplat[] { hit(dmg + 1), miss, miss, miss };
						break;
				}
				dist.addHit(new WeightedHit(chancePerDmg, splats));
			}
		}

		double chanceAllFail = Math.pow(1 - acc, 4);
		dist.addHit(new WeightedHit(chanceAllFail * 2 / 3, new Hitsplat[] { new Hitsplat(1, false), new Hitsplat(1, false), miss, miss }));
		dist.addHit(new WeightedHit(chanceAllFail / 3, new Hitsplat[] { miss, miss, miss, miss }));
		return new AttackDistribution(dist);
	}

	public static AttackDistribution burningClawSpec(double acc, double max) {
		var dist = new HitDistribution();
		var miss = Hitsplat.INACCURATE;
		for (int accRoll = 0; accRoll < 3; accRoll++) {
			double[] t = generateTotals(accRoll, 3, acc, max, 0);
			double chancePerDmg = t[0];
			for (double dmg = t[1]; dmg <= t[2]; dmg++) {
				Hitsplat[] splats;
				switch (accRoll) {
					case 0:
						splats = new Hitsplat[] { hit(trunc(dmg / 2)), hit(trunc(dmg / 4)), hit(trunc(dmg / 4)) };
						break;
					case 1:
						splats = new Hitsplat[] { hit(trunc(dmg / 2) - 1), hit(trunc(dmg / 2) - 1), hit(2) };
						break;
					default:
						splats = new Hitsplat[] { hit(dmg - 2), hit(1), hit(1) };
						break;
				}
				dist.addHit(new WeightedHit(chancePerDmg, splats));
			}
		}

		double chanceAllFail = Math.pow(1 - acc, 3);
		dist.addHit(new WeightedHit(chanceAllFail / 5, new Hitsplat[] { miss, miss, miss }));
		dist.addHit(new WeightedHit(2 * chanceAllFail / 5, new Hitsplat[] { new Hitsplat(1, false), miss, miss }));
		dist.addHit(new WeightedHit(2 * chanceAllFail / 5, new Hitsplat[] { new Hitsplat(1, false), new Hitsplat(1, false), miss }));
		return new AttackDistribution(dist);
	}

	/**
	 * Truth table of which of the three hitsplats burn
	 */
	private static final int[][] BURN_MATRIX = {
		{ 0, 0, 0 }, { 0, 0, 1 }, { 0, 1, 0 }, { 0, 1, 1 }, { 1, 0, 0 }, { 1, 0, 1 }, { 1, 1, 0 }, { 1, 1, 1 },
	};

	private static final double[] BURN_EXPECTED = new double[3];

	static {
		for (int accRoll = 0; accRoll < 3; accRoll++) {
			double total = 0;
			for (int[] row : BURN_MATRIX) {
				double burnChance = 0.15 * (accRoll + 1);
				double burn1 = row[0] == 1 ? burnChance : (1 - burnChance);
				double burn2 = row[1] == 1 ? burnChance : (1 - burnChance);
				double burn3 = row[2] == 1 ? burnChance : (1 - burnChance);
				double chanceOfRow = burn1 * burn2 * burn3;

				double damage = (row[0] + row[1] + row[2]) * 10;
				// a presumed bug where the first two burns overlap and miss 1 damage on the first tick
				if (row[0] == 1 && row[1] == 1)
					damage -= 1;
				total += chanceOfRow * damage;
			}
			BURN_EXPECTED[accRoll] = total;
		}
	}

	/**
	 * Expected burn damage: 10 damage burn x3 hitsplats, 15/30/45% chance per splat depending on which roll hits
	 */
	public static double burningClawDoT(double acc) {
		double accumulator = 0;
		for (int accRoll = 0; accRoll < 3; accRoll++) {
			double prevRollsFail = Math.pow(1 - acc, accRoll);
			double thisRollHits = prevRollsFail * acc;
			accumulator += thisRollHits * BURN_EXPECTED[accRoll];
		}
		return accumulator;
	}

	private Claws() {
	}
}
