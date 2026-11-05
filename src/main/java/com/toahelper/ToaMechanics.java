package com.toahelper;

import com.toahelper.StandingGeometry.Tile;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Value;

/**
 * Timings and areas from the wiki's Tombs of Amascut/Strategies page.
 */
final class ToaMechanics
{
	private ToaMechanics()
	{
	}

	/**
	 * Akkha uses a special after this many regular attacks (6 with Stay Vigilant).
	 */
	static int akkhaAttacksPerSpecial(boolean stayVigilant)
	{
		return stayVigilant ? 6 : 7;
	}

	/**
	 * Regular attacks left before Akkha's next special, 0 = the next attack is a special.
	 */
	static int akkhaAttacksUntilSpecial(int attacksSinceSpecial, boolean stayVigilant)
	{
		return Math.max(0, akkhaAttacksPerSpecial(stayVigilant) - attacksSinceSpecial);
	}

	/**
	 * How long the Warden's core stays out in phase 2, by the Warden's health when it's ejected:
	 * 21 ticks at 80%+, 29 at 60-80%, 37 below 60%.
	 */
	static int coreExposureTicks(double wardenHealthPercent)
	{
		if (wardenHealthPercent >= 80)
		{
			return 21;
		}
		if (wardenHealthPercent >= 60)
		{
			return 29;
		}
		return 37;
	}

	/**
	 * Whether Ba-Ba is about to hit a Rolling Stones threshold (66.6% / 33.3%), which knocks
	 * everyone to the bottom of the arena (lethal with Mind the Gap!).
	 *
	 * @param warnWithin how many percent above a threshold to start warning
	 */
	static boolean babaNearRollingStones(double healthPercent, double warnWithin)
	{
		return near(healthPercent, 66.6, warnWithin) || near(healthPercent, 33.3, warnWithin);
	}

	private static boolean near(double health, double threshold, double within)
	{
		return health > threshold && health <= threshold + within;
	}

	/**
	 * A health bar as a percentage, or -1 if it isn't showing.
	 */
	static double healthPercent(int ratio, int scale)
	{
		if (ratio < 0 || scale <= 0)
		{
			return -1;
		}
		return 100.0 * ratio / scale;
	}

	/**
	 * The square of tiles within {@code radius} of a tile.
	 */
	static Set<Tile> square(Tile center, int radius)
	{
		Set<Tile> tiles = new LinkedHashSet<>();
		for (int dx = -radius; dx <= radius; dx++)
		{
			for (int dy = -radius; dy <= radius; dy++)
			{
				tiles.add(new Tile(center.getX() + dx, center.getY() + dy));
			}
		}
		return tiles;
	}

	@Value
	static class LightningSkull
	{
		Set<Tile> danger;
		Set<Tile> safe;
	}

	/**
	 * Wardens phase 2 Lightning Skull: lightning hits the 7x7 around where the skull lands, except
	 * the two tiles in each cardinal direction from it, which are safe.
	 */
	static LightningSkull lightningSkull(Tile landing)
	{
		Set<Tile> safe = new LinkedHashSet<>();
		for (int d = 1; d <= 2; d++)
		{
			safe.add(new Tile(landing.getX() + d, landing.getY()));
			safe.add(new Tile(landing.getX() - d, landing.getY()));
			safe.add(new Tile(landing.getX(), landing.getY() + d));
			safe.add(new Tile(landing.getX(), landing.getY() - d));
		}
		Set<Tile> danger = square(landing, 3);
		danger.removeAll(safe);
		return new LightningSkull(danger, safe);
	}
}
