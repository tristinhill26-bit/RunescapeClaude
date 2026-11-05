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
 * Enchanted bolt effects
 */
public final class Bolts
{
	public static final class BoltContext
	{
		public double rangedLvl;
		public double maxHit;
		public boolean zcb;
		public boolean spec;
		public boolean kandarinDiary;
		public Monster monster;
	}

	private static double kandarinFactor(BoltContext ctx) {
		return ctx.kandarinDiary ? 1.1 : 1.0;
	}

	private static HitDistribution unchanged(Hitsplat h) {
		return new HitDistribution(new WeightedHit(1.0, new Hitsplat[] { h }));
	}

	private static HitTransformer bonusDamageTransform(BoltContext ctx, double chance, double bonusDmg, boolean accurateOnly) {
		return h -> {
			if (h.accurate && ctx.zcb && ctx.spec)
				return HitDistribution.single(1.0, new Hitsplat(h.damage + bonusDmg));
			if (!h.accurate && accurateOnly)
				return unchanged(h);
			return new HitDistribution(
				new WeightedHit(chance, new Hitsplat[] { new Hitsplat(h.damage + bonusDmg, h.accurate) }),
				new WeightedHit(1 - chance, new Hitsplat[] { new Hitsplat(h.damage, h.accurate) })
			);
		};
	}

	private static HitDistribution withEffect(HitDistribution effectDist, double chance, Hitsplat h) {
		var d = effectDist.scaleProbability(chance);
		d.addHit(new WeightedHit(1 - chance, new Hitsplat[] { new Hitsplat(h.damage, h.accurate) }));
		return d;
	}

	public static HitTransformer opalBolts(BoltContext ctx) {
		double chance = 0.05 * kandarinFactor(ctx);
		double bonusDmg = trunc(ctx.rangedLvl / (ctx.zcb ? 9 : 10));
		return bonusDamageTransform(ctx, chance, bonusDmg, false);
	}

	public static HitTransformer pearlBolts(BoltContext ctx) {
		double chance = 0.06 * kandarinFactor(ctx);
		double divisor = ctx.monster.attributes.contains(MonsterAttribute.FIERY) ? 15 : 20;
		double bonusDmg = trunc(ctx.rangedLvl / (ctx.zcb ? divisor - 2 : divisor));
		return bonusDamageTransform(ctx, chance, bonusDmg, false);
	}

	public static HitTransformer diamondBolts(BoltContext ctx) {
		double chance = 0.1 * kandarinFactor(ctx);
		double effectMax = trunc(ctx.maxHit * (ctx.zcb ? 126 : 115) / 100);
		var effectDist = HitDistribution.linear(1.0, 0, effectMax);
		return h -> {
			if (h.accurate && ctx.zcb && ctx.spec)
				return effectDist;
			return withEffect(effectDist, chance, h);
		};
	}

	public static HitTransformer dragonstoneBolts(BoltContext ctx) {
		var attrs = ctx.monster.attributes;
		// immune to dragonfire
		if (attrs.contains(MonsterAttribute.FIERY) || attrs.contains(MonsterAttribute.DRAGON))
			return Bolts::unchanged;

		double chance = 0.06 * kandarinFactor(ctx);
		double bonusDmg = trunc(ctx.rangedLvl * 2 / (ctx.zcb ? 9 : 10));
		return bonusDamageTransform(ctx, chance, bonusDmg, true);
	}

	public static HitTransformer onyxBolts(BoltContext ctx) {
		// immune to life leech
		if (ctx.monster.attributes.contains(MonsterAttribute.UNDEAD))
			return Bolts::unchanged;

		double chance = 0.11 * kandarinFactor(ctx);
		double effectMax = trunc(ctx.maxHit * (ctx.zcb ? 132 : 120) / 100);
		var effectDist = HitDistribution.linear(1.0, 0, effectMax);
		return h -> {
			if (!h.accurate)
				return unchanged(h);
			if (ctx.zcb && ctx.spec)
				return effectDist;
			return withEffect(effectDist, chance, h);
		};
	}

	public static HitTransformer rubyBolts(BoltContext ctx) {
		double chance = 0.06 * kandarinFactor(ctx);
		double cap = Constants.has(Constants.INFINITE_HEALTH_MONSTERS, ctx.monster.id)
			? (ctx.zcb ? 66 : 60)
			: (ctx.zcb ? 110 : 100);

		double effectDmg = trunc(ctx.monster.inputs.monsterCurrentHp * (ctx.zcb ? 22 : 20) / 100);
		var effectHit = HitDistribution.single(1.0, new Hitsplat(Math.min(cap, effectDmg)));
		return h -> {
			if (h.accurate && ctx.zcb && ctx.spec)
				return effectHit;
			return withEffect(effectHit, chance, h);
		};
	}

	private Bolts() {
	}
}
