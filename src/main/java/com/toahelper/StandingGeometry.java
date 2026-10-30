package com.toahelper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.Value;

/**
 * Where to stand, in tile coordinates (local coordinate / 128). Kept free of RuneLite types so it
 * can be tested on its own.
 */
final class StandingGeometry
{
	@Value
	static class Tile
	{
		int x;
		int y;
	}

	enum Slam
	{
		/**
		 * The Warden raises the floor on its left (west, per the wiki).
		 */
		LEFT,
		/**
		 * The Warden raises the floor on its right (east).
		 */
		RIGHT,
		/**
		 * The Warden slams both directions; only the central column in front of it is safe.
		 */
		BOTH;

		Slam flipped()
		{
			return this == LEFT ? RIGHT : this == RIGHT ? LEFT : BOTH;
		}
	}

	@Value
	static class SlamAdvice
	{
		/**
		 * The nearest safe tile on your row.
		 */
		Tile safe;
		boolean alreadySafe;
	}

	/**
	 * How far behind a Zebak rock its shelter from the roar reaches, per the wiki.
	 */
	static final int SHELTER_DEPTH = 3;

	private StandingGeometry()
	{
	}

	/**
	 * The tiles of an NPC's footprint, given its south-west tile and size.
	 */
	static List<Tile> footprint(Tile southWest, int size)
	{
		List<Tile> tiles = new ArrayList<>();
		for (int dx = 0; dx < size; dx++)
		{
			for (int dy = 0; dy < size; dy++)
			{
				tiles.add(new Tile(southWest.getX() + dx, southWest.getY() + dy));
			}
		}
		return tiles;
	}

	/**
	 * Tiles up to three behind a rock, on the side away from Zebak. Standing there blocks his roar.
	 */
	static Set<Tile> roarShelter(Tile zebakCenter, Tile rockSouthWest, int rockSize)
	{
		double rockCx = rockSouthWest.getX() + (rockSize - 1) / 2.0;
		double rockCy = rockSouthWest.getY() + (rockSize - 1) / 2.0;
		double dx = rockCx - zebakCenter.getX();
		double dy = rockCy - zebakCenter.getY();
		Set<Tile> shelter = new LinkedHashSet<>();
		if (dx == 0 && dy == 0)
		{
			return shelter;
		}

		// Snap the direction away from Zebak to one of 8 compass directions.
		int octant = (int) Math.round(Math.atan2(dy, dx) / (Math.PI / 4));
		int stepX = (int) Math.round(Math.cos(octant * Math.PI / 4));
		int stepY = (int) Math.round(Math.sin(octant * Math.PI / 4));

		List<Tile> rock = footprint(rockSouthWest, rockSize);
		for (int k = 1; k <= SHELTER_DEPTH; k++)
		{
			for (Tile t : rock)
			{
				Tile behind = new Tile(t.getX() + k * stepX, t.getY() + k * stepY);
				if (!rock.contains(behind))
				{
					shelter.add(behind);
				}
			}
		}
		return shelter;
	}

	/**
	 * The ring of tiles touching an NPC's footprint. Ba-Ba's thrown rocks miss players standing
	 * next to debris or a sarcophagus.
	 */
	static List<Tile> adjacentRing(Tile southWest, int size)
	{
		List<Tile> ring = new ArrayList<>();
		for (int x = southWest.getX() - 1; x <= southWest.getX() + size; x++)
		{
			for (int y = southWest.getY() - 1; y <= southWest.getY() + size; y++)
			{
				boolean inside = x >= southWest.getX() && x < southWest.getX() + size
					&& y >= southWest.getY() && y < southWest.getY() + size;
				if (!inside)
				{
					ring.add(new Tile(x, y));
				}
			}
		}
		return ring;
	}

	/**
	 * Wardens phase 3: for a side slam stand at least one tile off centre on the side away from
	 * the raised floor; for a double slam stand on the central column in front of the Warden.
	 *
	 * @param wardenCenterX the Warden's centre column
	 * @param player        your tile
	 */
	static SlamAdvice slam(Slam slam, int wardenCenterX, Tile player)
	{
		int safeX;
		switch (slam)
		{
			case RIGHT:
				safeX = Math.min(player.getX(), wardenCenterX - 1);
				break;
			case LEFT:
				safeX = Math.max(player.getX(), wardenCenterX + 1);
				break;
			default:
				safeX = wardenCenterX;
				break;
		}
		return new SlamAdvice(new Tile(safeX, player.getY()), safeX == player.getX());
	}
}
