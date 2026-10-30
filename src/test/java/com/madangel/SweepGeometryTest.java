package com.madangel;

import static com.madangel.SweepGeometry.TILE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.madangel.SweepAnimations.Side;
import com.madangel.SweepGeometry.Result;
import com.madangel.SweepGeometry.Tile;
import org.junit.Test;

public class SweepGeometryTest
{
	private static final int SOUTH = 0;
	private static final int EAST = 1536;
	private static final int NORTH_EAST = 1280;

	// Centre of a 3x3 NPC sits on a tile centre.
	private static final int CX = 64 + 10 * TILE;
	private static final int CY = 64 + 10 * TILE;

	private static Tile at(int dx, int dy)
	{
		return new Tile(CX + dx * TILE, CY + dy * TILE);
	}

	@Test
	public void facingSouthSwordOnHerRight()
	{
		// Facing south, her right hand is to the west, so dodge east.
		Result r = SweepGeometry.compute(CX, CY, 3, SOUTH, Side.RIGHT, 4);
		assertEquals(at(2, 0), r.getDodge());
		assertEquals(at(1, 0), r.getDodgeInside());
		assertEquals(at(0, 2), r.getBehind());

		assertTrue(r.getDanger().contains(at(0, -2))); // in front
		assertTrue(r.getDanger().contains(at(-2, 0))); // sword side
		assertTrue(r.getDanger().contains(at(-3, -3)));
		assertFalse(r.getDanger().contains(r.getDodge()));
		assertFalse(r.getDanger().contains(r.getBehind()));
		assertFalse(r.getDanger().contains(at(0, 0))); // her own tiles
	}

	@Test
	public void facingEastSwordOnHerLeft()
	{
		// Facing east, her left hand is to the north, so dodge south.
		Result r = SweepGeometry.compute(CX, CY, 3, EAST, Side.LEFT, 4);
		assertEquals(at(0, -2), r.getDodge());
		assertEquals(at(-2, 0), r.getBehind());
		assertTrue(r.getDanger().contains(at(2, 0)));
		assertTrue(r.getDanger().contains(at(0, 2)));
		assertFalse(r.getDanger().contains(at(0, -2)));
	}

	@Test
	public void swappingSidesMovesTheDodgeTile()
	{
		Result right = SweepGeometry.compute(CX, CY, 3, SOUTH, Side.RIGHT, 4);
		Result left = SweepGeometry.compute(CX, CY, 3, SOUTH, Side.LEFT, 4);
		assertEquals(at(2, 0), right.getDodge());
		assertEquals(at(-2, 0), left.getDodge());
		assertEquals(right.getBehind(), left.getBehind());
	}

	@Test
	public void diagonalFacingPicksCornerTiles()
	{
		// Facing north-east, her right is south-east, so dodge north-west.
		Result r = SweepGeometry.compute(CX, CY, 3, NORTH_EAST, Side.RIGHT, 4);
		assertEquals(at(-2, 2), r.getDodge());
		assertEquals(at(-1, 1), r.getDodgeInside());
		assertEquals(at(-2, -2), r.getBehind());
	}

	@Test
	public void dangerAreaCoversRange()
	{
		Result r = SweepGeometry.compute(CX, CY, 3, SOUTH, Side.RIGHT, 2);
		assertTrue(r.getDanger().contains(at(0, -3)));
		assertFalse(r.getDanger().contains(at(0, -4)));
	}

	@Test
	public void animationSides()
	{
		assertEquals(Side.RIGHT, SweepAnimations.swordSide(14429));
		// Swinging from the right leaves the sword on the left for the next cleave.
		assertEquals(Side.LEFT, SweepAnimations.swordSide(14431));
		assertEquals(Side.RIGHT, SweepAnimations.swordSide(14442));
		assertNull(SweepAnimations.swordSide(14432));
		assertTrue(SweepAnimations.isReset(14432));
		assertNull(SweepAnimations.swordSide(4588)); // idle
	}
}
