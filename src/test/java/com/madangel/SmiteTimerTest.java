package com.madangel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class SmiteTimerTest
{
	@Test
	public void singleStrikeCountsDown()
	{
		SmiteTimer timer = new SmiteTimer();
		timer.start(100, 4);

		assertEquals(4, timer.ticksUntilStrike(100));
		assertEquals(1, timer.ticksUntilStrike(103));
		assertEquals(0, timer.ticksUntilStrike(104));
		assertEquals(1, timer.nextStrike(104));
		assertFalse(timer.isFinished(104));
		assertEquals(-1, timer.ticksUntilStrike(105));
		assertTrue(timer.isFinished(105));
	}

	@Test
	public void enragedStrikesFollowEachOther()
	{
		SmiteTimer timer = new SmiteTimer();
		timer.start(100, 4, 4, 2);

		assertEquals(3, timer.strikeCount());
		assertEquals(0, timer.ticksUntilStrike(104));
		assertEquals(3, timer.ticksUntilStrike(105));
		assertEquals(2, timer.nextStrike(105));
		assertEquals(0, timer.ticksUntilStrike(108));
		assertEquals(1, timer.ticksUntilStrike(109));
		assertEquals(3, timer.nextStrike(109));
		assertEquals(0, timer.ticksUntilStrike(110));
		assertTrue(timer.isFinished(111));
	}

	@Test
	public void idleTimerHasNothingComing()
	{
		SmiteTimer timer = new SmiteTimer();
		assertEquals(-1, timer.ticksUntilStrike(5));
		assertEquals(0, timer.nextStrike(5));
		assertFalse(timer.isFinished(5));
	}
}
