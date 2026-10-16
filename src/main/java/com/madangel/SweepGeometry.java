package com.madangel;

import com.madangel.SweepAnimations.Side;
import java.util.ArrayList;
import java.util.List;
import lombok.Value;

/**
 * Works out where Mad Angel's sweep hits and where to click to dodge it.
 * <p>
 * Per the wiki, she cleaves "towards the side where the player was, as well as the side it
 * drew the sword back from", which guides describe as half the room cut diagonally. So the
 * danger is the half-plane in front of her and on the sword side, split by the diagonal
 * between her back-sword corner and her front-other corner. Directly opposite the sword and
 * directly behind her are both safe; only the side dodge gives the accuracy bonus.
 * <p>
 * Everything here is in local coordinates (128 per tile, tile centres at 64 + 128n).
 */
final class SweepGeometry
{
	static final int TILE = 128;

	@Value
	static class Tile
	{
		int x;
		int y;
	}

	@Value
	static class Result
	{
		/**
		 * The tile to click: next to her, on the side opposite the sword. Gives the bonus.
		 */
		Tile dodge;
		/**
		 * Her own edge tile on the dodge side, for when {@link #dodge} is a wall. Clicking it
		 * still walks you to the dodge side.
		 */
		Tile dodgeInside;
		/**
		 * Directly behind her. Also safe, but no bonus.
		 */
		Tile behind;
		List<Tile> danger;
	}

	private SweepGeometry()
	{
	}

	/**
	 * @param centerX     her centre, local x (from NPC#getLocalLocation)
	 * @param centerY     her centre, local y
	 * @param size        her size in tiles
	 * @param orientation her facing, 0-2047 with 0 = south, 512 = west, 1024 = north, 1536 = east
	 * @param sword       which of her sides the sword is on
	 * @param range       how many tiles out from her edge to mark as danger
	 */
	static Result compute(int centerX, int centerY, int size, int orientation, Side sword, int range)
	{
		double angle = (orientation & 2047) * Math.PI * 2 / 2048;
		// Facing vector, x = east, y = north.
		double fx = -Math.sin(angle);
		double fy = -Math.cos(angle);
		// Her right hand: facing rotated 90 degrees clockwise (seen from above).
		double rightX = fy;
		double rightY = -fx;
		double swordX = sword == Side.RIGHT ? rightX : -rightX;
		double swordY = sword == Side.RIGHT ? rightY : -rightY;

		double half = size / 2.0;

		Tile dodge = bestTile(centerX, centerY, half + 1, -swordX, -swordY);
		Tile dodgeInside = bestTile(centerX, centerY, half, -swordX, -swordY);
		Tile behind = bestTile(centerX, centerY, half + 1, -fx, -fy);

		// Danger: in front of her and on the sword side, outside her own footprint.
		double nx = fx + swordX;
		double ny = fy + swordY;
		List<Tile> danger = new ArrayList<>();
		int width = size + 2 * range;
		int swX = centerX - (width - 1) * TILE / 2;
		int swY = centerY - (width - 1) * TILE / 2;
		for (int i = 0; i < width; i++)
		{
			for (int j = 0; j < width; j++)
			{
				int x = swX + i * TILE;
				int y = swY + j * TILE;
				double dx = (double) (x - centerX) / TILE;
				double dy = (double) (y - centerY) / TILE;
				if (Math.abs(dx) < half && Math.abs(dy) < half)
				{
					continue;
				}
				// Tiles on the dividing diagonal count as danger, to be safe.
				if (dx * nx + dy * ny > -0.01)
				{
					danger.add(new Tile(x, y));
				}
			}
		}

		return new Result(dodge, dodgeInside, behind, danger);
	}

	/**
	 * Of the ring of tiles whose centres are {@code ring} tiles out from her centre (on the
	 * Chebyshev square), the one pointing most closely along (dirX, dirY).
	 */
	private static Tile bestTile(int centerX, int centerY, double ring, double dirX, double dirY)
	{
		// Tile centre offsets: whole numbers for odd sizes, halves for even sizes.
		double start = -(ring - 0.5);
		int steps = (int) Math.round(2 * ring) - 1;

		Tile best = null;
		double bestCos = -2;
		for (int i = 0; i <= steps; i++)
		{
			for (int j = 0; j <= steps; j++)
			{
				double dx = start + i;
				double dy = start + j;
				// Only the outer ring.
				if (Math.abs(Math.abs(dx) - (ring - 0.5)) > 0.01 && Math.abs(Math.abs(dy) - (ring - 0.5)) > 0.01)
				{
					continue;
				}
				double length = Math.hypot(dx, dy);
				if (length == 0)
				{
					continue;
				}
				double cos = (dx * dirX + dy * dirY) / length;
				if (cos > bestCos + 1e-9)
				{
					bestCos = cos;
					best = new Tile(centerX + (int) Math.round(dx * TILE), centerY + (int) Math.round(dy * TILE));
				}
			}
		}
		return best;
	}
}
