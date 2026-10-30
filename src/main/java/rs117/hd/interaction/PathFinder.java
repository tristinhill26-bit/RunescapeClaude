package rs117.hd.interaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.*;

import static net.runelite.api.CollisionDataFlag.*;

/**
 * Breadth-first search over the scene's collision flags, using the same neighbour order as the game,
 * so the path drawn closely matches the one the player will walk.
 */
class PathFinder {
	private static final int MAX_PATH_LENGTH = 128;

	private int[] parents = new int[0];
	private int[] queue = new int[0];

	/**
	 * Find a path between two scene tiles.
	 *
	 * @return a list of packed scene coordinates (x << 16 | y), from start to end inclusive,
	 * or an empty list if no path was found
	 */
	List<Integer> findPath(CollisionData collisionData, int startX, int startY, int endX, int endY) {
		var path = new ArrayList<Integer>();
		int[][] flags = collisionData.getFlags();
		int sizeX = flags.length;
		int sizeY = sizeX == 0 ? 0 : flags[0].length;
		if (!inBounds(startX, startY, sizeX, sizeY) || !inBounds(endX, endY, sizeX, sizeY))
			return path;

		int numTiles = sizeX * sizeY;
		if (parents.length < numTiles) {
			parents = new int[numTiles];
			queue = new int[numTiles];
		}
		Arrays.fill(parents, 0, numTiles, -1);

		int start = startX * sizeY + startY;
		int end = endX * sizeY + endY;
		parents[start] = start;
		int head = 0, tail = 0;
		queue[tail++] = start;

		boolean found = start == end;
		while (!found && head < tail) {
			int current = queue[head++];
			int x = current / sizeY;
			int y = current % sizeY;

			// West, east, south, north, south-west, south-east, north-west, north-east
			for (int dir = 0; dir < 8; dir++) {
				int dx, dy;
				switch (dir) {
					case 0: dx = -1; dy = 0; break;
					case 1: dx = 1; dy = 0; break;
					case 2: dx = 0; dy = -1; break;
					case 3: dx = 0; dy = 1; break;
					case 4: dx = -1; dy = -1; break;
					case 5: dx = 1; dy = -1; break;
					case 6: dx = -1; dy = 1; break;
					default: dx = 1; dy = 1; break;
				}

				int nx = x + dx;
				int ny = y + dy;
				if (!inBounds(nx, ny, sizeX, sizeY))
					continue;
				int next = nx * sizeY + ny;
				if (parents[next] != -1 || !canMove(flags, x, y, dx, dy))
					continue;

				parents[next] = current;
				if (next == end) {
					found = true;
					break;
				}
				queue[tail++] = next;
			}
		}

		if (!found)
			return path;

		for (int node = end; ; node = parents[node]) {
			path.add((node / sizeY) << 16 | (node % sizeY));
			if (node == start || path.size() > MAX_PATH_LENGTH)
				break;
		}

		if (path.size() > MAX_PATH_LENGTH) {
			path.clear();
			return path;
		}

		Collections.reverse(path);
		return path;
	}

	private static boolean inBounds(int x, int y, int sizeX, int sizeY) {
		return x >= 0 && y >= 0 && x < sizeX && y < sizeY;
	}

	private static boolean canMove(int[][] flags, int x, int y, int dx, int dy) {
		int nx = x + dx;
		int ny = y + dy;
		if (dx == 0 || dy == 0) {
			int blocking;
			if (dx == -1) {
				blocking = BLOCK_MOVEMENT_EAST;
			} else if (dx == 1) {
				blocking = BLOCK_MOVEMENT_WEST;
			} else if (dy == -1) {
				blocking = BLOCK_MOVEMENT_NORTH;
			} else {
				blocking = BLOCK_MOVEMENT_SOUTH;
			}
			return (flags[nx][ny] & (BLOCK_MOVEMENT_FULL | blocking)) == 0;
		}

		// Diagonal movement also requires both adjacent orthogonal tiles to be passable
		int horizontal = dx == -1 ? BLOCK_MOVEMENT_EAST : BLOCK_MOVEMENT_WEST;
		int vertical = dy == -1 ? BLOCK_MOVEMENT_NORTH : BLOCK_MOVEMENT_SOUTH;
		int corner;
		if (dx == -1) {
			corner = dy == -1 ? BLOCK_MOVEMENT_NORTH_EAST : BLOCK_MOVEMENT_SOUTH_EAST;
		} else {
			corner = dy == -1 ? BLOCK_MOVEMENT_NORTH_WEST : BLOCK_MOVEMENT_SOUTH_WEST;
		}

		return
			(flags[nx][ny] & (BLOCK_MOVEMENT_FULL | horizontal | vertical | corner)) == 0 &&
			(flags[nx][y] & (BLOCK_MOVEMENT_FULL | horizontal)) == 0 &&
			(flags[x][ny] & (BLOCK_MOVEMENT_FULL | vertical)) == 0;
	}
}
