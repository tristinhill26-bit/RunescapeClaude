package com.toahelper;

import lombok.Getter;

/**
 * Estimates your own raid points, per the wiki's Chest (Tombs of Amascut) page:
 * <ul>
 *     <li>you start with 5,000, which don't count towards loot</li>
 *     <li>damage earns points times a per-NPC factor (Ba-Ba 2x, Wardens P3 2.5x, ...)</li>
 *     <li>rooms cap at 20,000 (Zebak 10,000, the Wardens 60,000 over their three phases)</li>
 *     <li>the total caps at 64,000</li>
 *     <li>dying costs 20% of your total, at least 1,000</li>
 * </ul>
 * The MVP bonus (300 x team size per room) isn't counted, since the client can't see it.
 */
final class PointsCalculator
{
	static final int BASE_POINTS = 5_000;
	static final int MAX_TOTAL_POINTS = 64_000;
	static final int MIN_DEATH_PENALTY = 1_000;
	/**
	 * Phase 2 Wardens stop giving points after this many downs.
	 */
	static final int MAX_WARDEN_DOWNS = 3;

	@Getter
	private int total = BASE_POINTS;
	@Getter
	private int room;
	@Getter
	private int wardenDowns;

	void reset()
	{
		total = BASE_POINTS;
		room = 0;
		wardenDowns = 0;
	}

	/**
	 * @return the points this hit earned
	 */
	int addDamage(int damage, double factor, int roomCap, boolean phase2Warden)
	{
		if (damage <= 0 || (phase2Warden && wardenDowns >= MAX_WARDEN_DOWNS))
		{
			return 0;
		}

		int earned = (int) (damage * factor);
		earned = Math.min(earned, Math.max(0, roomCap - room));
		earned = Math.min(earned, Math.max(0, MAX_TOTAL_POINTS - total));
		room += earned;
		total += earned;
		return earned;
	}

	void wardenDowned()
	{
		wardenDowns++;
	}

	/**
	 * @return the points lost
	 */
	int died()
	{
		int lost = Math.min(total, deathPenalty());
		total -= lost;
		return lost;
	}

	int deathPenalty()
	{
		return Math.max((int) (total * 0.2), MIN_DEATH_PENALTY);
	}

	void roomEnded()
	{
		room = 0;
	}

	/**
	 * Points that count towards loot (the starting 5,000 don't).
	 */
	int lootPoints()
	{
		return Math.max(0, total - BASE_POINTS);
	}

	/**
	 * Chance of a unique (purple), 0-1, from the wiki: 1% per (10,500 - 20 x level) points, where
	 * raid levels above 310 count a third and above 430 a half, capped at level 550 and 55%.
	 */
	static double uniqueChance(int raidLevel, int lootPoints)
	{
		int level = Math.min(550, Math.max(0, raidLevel));
		if (level > 430)
		{
			level = 430 + (level - 430) / 2;
		}
		if (level > 310)
		{
			level = 310 + (level - 310) / 3;
		}

		double pointsPerPercent = 10_500 - 20.0 * level;
		double percent = Math.min(55.0, lootPoints / pointsPerPercent);
		return Math.max(0, percent) / 100.0;
	}
}
