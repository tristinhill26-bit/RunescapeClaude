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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class HitDistribution
{
	public final List<WeightedHit> hits;

	public HitDistribution() {
		hits = new ArrayList<>();
	}

	public HitDistribution(List<WeightedHit> hits) {
		this.hits = hits;
	}

	public HitDistribution(WeightedHit... hits) {
		this.hits = new ArrayList<>(List.of(hits));
	}

	public void addHit(WeightedHit w) {
		hits.add(w);
	}

	public void addHits(List<WeightedHit> w) {
		hits.addAll(w);
	}

	public HitDistribution zip(HitDistribution other) {
		var list = new ArrayList<WeightedHit>(hits.size() * other.hits.size());
		for (WeightedHit a : hits)
			for (WeightedHit b : other.hits)
				list.add(a.zip(b));
		return new HitDistribution(list);
	}

	public HitDistribution wideTransform(HitTransformer t, boolean transformInaccurate) {
		var d = new HitDistribution();
		for (WeightedHit h : hits)
			d.hits.addAll(h.transform(t, transformInaccurate).hits);
		return d;
	}

	public HitDistribution transform(HitTransformer t) {
		return transform(t, true);
	}

	public HitDistribution transform(HitTransformer t, boolean transformInaccurate) {
		return wideTransform(t, transformInaccurate).flatten();
	}

	public HitDistribution scaleProbability(double factor) {
		var list = new ArrayList<WeightedHit>(hits.size());
		for (WeightedHit h : hits)
			list.add(h.scale(factor));
		return new HitDistribution(list);
	}

	public HitDistribution scaleDamage(double factor) {
		return scaleDamage(factor, 1);
	}

	public HitDistribution scaleDamage(double factor, double divisor) {
		var list = new ArrayList<WeightedHit>(hits.size());
		for (WeightedHit h : hits) {
			var splats = new Hitsplat[h.hitsplats.length];
			for (int i = 0; i < splats.length; i++) {
				var s = h.hitsplats[i];
				splats[i] = new Hitsplat(MathUtil.trunc(s.damage * factor / divisor), s.accurate);
			}
			list.add(new WeightedHit(h.probability, splats));
		}
		return new HitDistribution(list);
	}

	/**
	 * Merges the probabilities of hits with identical damage values
	 */
	public HitDistribution flatten() {
		var acc = new LinkedHashMap<Object, double[]>();
		var hitLists = new LinkedHashMap<Object, Hitsplat[]>();
		for (WeightedHit hit : hits) {
			Object hash = hit.getHash();
			double[] prev = acc.get(hash);
			if (prev == null) {
				acc.put(hash, new double[] { hit.probability });
				hitLists.put(hash, hit.hitsplats);
			} else {
				prev[0] += hit.probability;
			}
		}

		var d = new HitDistribution();
		for (Map.Entry<Object, double[]> e : acc.entrySet()) {
			double prob = e.getValue()[0];
			if (prob > 0)
				d.addHit(new WeightedHit(prob, hitLists.get(e.getKey())));
		}
		return d;
	}

	/**
	 * Converts multi-hits into a single cumulative damage total
	 */
	public HitDistribution cumulative() {
		var acc = new LinkedHashMap<Long, double[]>();
		for (WeightedHit hit : hits) {
			// if 1 splat is accurate, treat the whole hit as accurate; otherwise take the bitwise inverse
			long sum = MathUtil.toInt32(hit.getSum());
			long key = hit.anyAccurate() ? (long) hit.getSum() : ~sum;
			double[] prev = acc.get(key);
			if (prev == null)
				acc.put(key, new double[] { hit.probability });
			else
				prev[0] += hit.probability;
		}

		var d = new HitDistribution();
		for (Map.Entry<Long, double[]> e : acc.entrySet()) {
			long key = e.getKey();
			boolean accurate = key >= 0;
			double dmg = accurate ? key : ~key;
			double prob = e.getValue()[0];
			if (prob > 0)
				d.addHit(new WeightedHit(prob, new Hitsplat[] { new Hitsplat(dmg, accurate) }));
		}
		return d;
	}

	public double expectedHit() {
		double sum = 0;
		for (WeightedHit h : hits) {
			double v = h.getExpectedValue();
			if (!Double.isNaN(v))
				sum += v;
		}
		return sum;
	}

	public int size() {
		return hits.size();
	}

	/**
	 * The smallest total damage, or NaN (undefined) when empty
	 */
	public double getMin() {
		double min = Double.NaN;
		for (WeightedHit h : hits) {
			double s = h.getSum();
			if (Double.isNaN(min) || s < min)
				min = s;
		}
		return min;
	}

	/**
	 * The largest total damage, or NaN (undefined) when empty
	 */
	public double getMax() {
		double max = Double.NaN;
		for (WeightedHit h : hits) {
			double s = h.getSum();
			if (Double.isNaN(max) || s > max)
				max = s;
		}
		return max;
	}

	/**
	 * Hits with how many ticks until the next attack, merged by damage and delay
	 */
	public List<DelayedHit> withProbabilisticDelays(Function<WeightedHit, List<double[]>> delayProvider) {
		var acc = new LinkedHashMap<Integer, double[]>();
		for (WeightedHit wh : hits) {
			double sum = wh.getSum();
			for (double[] pd : delayProvider.apply(wh)) {
				double probability = wh.probability * pd[0];
				int key = (MathUtil.toInt32(sum) & 0xFFFFFF) | (MathUtil.toInt32(pd[1]) << 24);
				double[] prev = acc.get(key);
				if (prev == null)
					acc.put(key, new double[] { probability });
				else
					prev[0] += probability;
			}
		}

		var d = new ArrayList<DelayedHit>();
		for (Map.Entry<Integer, double[]> e : acc.entrySet()) {
			int key = e.getKey();
			int delay = (key & 0x8F000000) >> 24;
			int dmg = key & 0xFFFFFF;
			d.add(new DelayedHit(new WeightedHit(e.getValue()[0], new Hitsplat[] { new Hitsplat(dmg, true) }), delay));
		}
		return d;
	}

	public static final class DelayedHit
	{
		public final WeightedHit wh;
		public final double delay;

		public DelayedHit(WeightedHit wh, double delay) {
			this.wh = wh;
			this.delay = delay;
		}
	}

	public static HitDistribution linear(double accuracy, double minimum, double maximum) {
		var d = new HitDistribution();
		double hitProb = accuracy / (maximum - minimum + 1);
		for (double i = minimum; i <= maximum; i++)
			d.addHit(new WeightedHit(hitProb, new Hitsplat[] { new Hitsplat(i) }));
		d.addHit(new WeightedHit(1 - accuracy, new Hitsplat[] { Hitsplat.INACCURATE }));
		return d;
	}

	public static HitDistribution single(double accuracy, Hitsplat... hitsplats) {
		var d = new HitDistribution(new WeightedHit(accuracy, hitsplats));
		if (accuracy != 1.0)
			d.addHit(new WeightedHit(1 - accuracy, new Hitsplat[] { Hitsplat.INACCURATE }));
		return d;
	}
}
