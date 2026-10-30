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
import java.util.function.UnaryOperator;

/**
 * The hit distributions of each hit of one attack
 */
public final class AttackDistribution
{
	public final List<HitDistribution> dists;

	private HitDistribution zipped;
	private HitDistribution singleHitsplat;

	public AttackDistribution(List<HitDistribution> dists) {
		this.dists = dists;
	}

	public AttackDistribution(HitDistribution... dists) {
		this.dists = new ArrayList<>(List.of(dists));
	}

	public HitDistribution getZipped() {
		if (zipped == null) {
			HitDistribution acc = dists.get(0);
			for (int i = 1; i < dists.size(); i++)
				acc = acc.zip(dists.get(i));
			zipped = acc;
		}
		return zipped;
	}

	public HitDistribution getSingleHitsplat() {
		if (singleHitsplat == null) {
			HitDistribution acc = dists.get(0);
			for (int i = 1; i < dists.size(); i++)
				acc = acc.zip(dists.get(i)).cumulative();
			singleHitsplat = acc;
		}
		return singleHitsplat;
	}

	public void addDist(HitDistribution d) {
		dists.add(d);
	}

	public AttackDistribution transform(HitTransformer t) {
		return transform(t, true);
	}

	public AttackDistribution transform(HitTransformer t, boolean transformInaccurate) {
		return map(d -> d.transform(t, transformInaccurate));
	}

	public AttackDistribution flatten() {
		return map(HitDistribution::flatten);
	}

	public AttackDistribution scaleProbability(double factor) {
		return map(d -> d.scaleProbability(factor));
	}

	public AttackDistribution scaleDamage(double factor) {
		return scaleDamage(factor, 1);
	}

	public AttackDistribution scaleDamage(double factor, double divisor) {
		return map(d -> d.scaleDamage(factor, divisor));
	}

	public double getMin() {
		double sum = 0;
		for (HitDistribution d : dists) {
			double v = d.getMin();
			if (!Double.isNaN(v))
				sum += v;
		}
		return sum;
	}

	public double getMax() {
		double sum = 0;
		for (HitDistribution d : dists) {
			double v = d.getMax();
			if (!Double.isNaN(v))
				sum += v;
		}
		return sum;
	}

	public double getExpectedDamage() {
		double sum = 0;
		for (HitDistribution d : dists) {
			double v = d.expectedHit();
			if (!Double.isNaN(v))
				sum += v;
		}
		return sum;
	}

	public AttackDistribution firstHitMax() {
		if (dists.isEmpty())
			return this;

		var firstDist = dists.get(0);
		double firstMax = Double.NaN;
		for (WeightedHit h : firstDist.hits)
			if (h.hitsplats.length > 0 && h.hitsplats[0].accurate && (Double.isNaN(firstMax) || h.hitsplats[0].damage > firstMax))
				firstMax = h.hitsplats[0].damage;
		if (Double.isNaN(firstMax))
			firstMax = 0;

		var maxed = new ArrayList<WeightedHit>();
		for (WeightedHit h : firstDist.hits) {
			if (h.hitsplats.length == 0 || !h.hitsplats[0].accurate) {
				maxed.add(h);
				continue;
			}
			var splats = h.hitsplats.clone();
			splats[0] = new Hitsplat(firstMax, true);
			maxed.add(new WeightedHit(h.probability, splats));
		}

		var list = new ArrayList<HitDistribution>();
		list.add(new HitDistribution(maxed).flatten());
		list.addAll(dists.subList(1, dists.size()));
		return new AttackDistribution(list);
	}

	public AttackDistribution firstHitAccurate() {
		if (dists.isEmpty())
			return this;

		var accurate = new HitDistribution();
		double totalProbability = 0;
		for (WeightedHit h : dists.get(0).hits) {
			if (h.hitsplats.length > 0 && h.hitsplats[0].accurate) {
				accurate.addHit(new WeightedHit(h.probability, h.hitsplats.clone()));
				totalProbability += h.probability;
			}
		}

		var list = new ArrayList<HitDistribution>();
		list.add(accurate.scaleProbability(1 / totalProbability));
		list.addAll(dists.subList(1, dists.size()));
		return new AttackDistribution(list);
	}

	public AttackDistribution firstHitMinimum(double minimum) {
		if (dists.isEmpty())
			return this;

		var first = dists.get(0).transform(
			h -> HitDistribution.single(1.0, new Hitsplat(Math.max(h.damage, minimum), h.accurate)),
			false
		);
		var list = new ArrayList<HitDistribution>();
		list.add(first);
		list.addAll(dists.subList(1, dists.size()));
		return new AttackDistribution(list).flatten();
	}

	private AttackDistribution map(UnaryOperator<HitDistribution> m) {
		var list = new ArrayList<HitDistribution>(dists.size());
		for (HitDistribution d : dists)
			list.add(m.apply(d));
		return new AttackDistribution(list);
	}
}
