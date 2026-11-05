package rs117.hd.interaction;

import java.util.List;
import net.runelite.api.CollisionData;
import org.junit.Test;

import static net.runelite.api.CollisionDataFlag.*;
import static org.junit.Assert.*;

public class PathFinderTest {
	private static final int SIZE = 16;

	private static CollisionData collision(int[][] flags) {
		return () -> flags;
	}

	private static int packed(int x, int y) {
		return x << 16 | y;
	}

	@Test
	public void walksStraightAcrossOpenGround() {
		var path = new PathFinder().findPath(collision(new int[SIZE][SIZE]), 2, 2, 7, 2);
		assertEquals(6, path.size());
		assertEquals(packed(2, 2), (int) path.get(0));
		assertEquals(packed(7, 2), (int) path.get(path.size() - 1));
	}

	@Test
	public void prefersDiagonalsLikeTheGame() {
		var path = new PathFinder().findPath(collision(new int[SIZE][SIZE]), 0, 0, 4, 4);
		assertEquals(5, path.size());
		for (int i = 0; i < path.size(); i++)
			assertEquals(packed(i, i), (int) path.get(i));
	}

	@Test
	public void walksAroundBlockedTiles() {
		int[][] flags = new int[SIZE][SIZE];
		// A wall of solid objects along x = 5, with a gap at y = 8
		for (int y = 0; y < SIZE; y++)
			if (y != 8)
				flags[5][y] = BLOCK_MOVEMENT_OBJECT;

		List<Integer> path = new PathFinder().findPath(collision(flags), 2, 2, 8, 2);
		assertFalse(path.isEmpty());
		assertTrue(path.contains(packed(5, 8)));
		for (int node : path)
			assertNotEquals(BLOCK_MOVEMENT_OBJECT, flags[node >>> 16][node & 0xFFFF]);
	}

	@Test
	public void cannotCutCornersPastWalls() {
		int[][] flags = new int[SIZE][SIZE];
		// A wall on the east side of (3, 3) blocks the diagonal step from (3, 3) to (4, 4)
		flags[3][3] |= BLOCK_MOVEMENT_EAST;
		flags[4][3] |= BLOCK_MOVEMENT_WEST;

		var path = new PathFinder().findPath(collision(flags), 3, 3, 4, 4);
		assertEquals(3, path.size());
		assertEquals(packed(3, 4), (int) path.get(1));
	}

	@Test
	public void returnsEmptyPathWhenUnreachable() {
		int[][] flags = new int[SIZE][SIZE];
		for (int y = 0; y < SIZE; y++)
			flags[5][y] = BLOCK_MOVEMENT_FULL;

		assertTrue(new PathFinder().findPath(collision(flags), 2, 2, 8, 2).isEmpty());
	}
}
