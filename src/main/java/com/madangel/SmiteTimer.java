package com.madangel;

/**
 * Counts down to each lightning strike of Mad Angel's smite, the attack that needs Protect from Magic.
 * <p>
 * Smite starts with her sword at her chest. Normally one strike lands a few ticks later. Enraged she
 * strikes three times: the second strike a few ticks after the first, and the third sooner still.
 * Turning the prayer on the tick a strike lands fully blocks it.
 */
final class SmiteTimer
{
	/**
	 * NPC_MAD_ANGEL_ATTACK_SMITE and NPC_MAD_ANGEL_ATTACK_SMITE_ENRAGED.
	 */
	static final int SMITE = 4590;
	static final int SMITE_ENRAGED = 8543;

	private int[] strikeTicks;

	/**
	 * Starts the countdown. {@code gaps} are the ticks from the start of the smite to the first strike,
	 * then from each strike to the next.
	 */
	void start(int tick, int... gaps)
	{
		strikeTicks = new int[gaps.length];
		int at = tick;
		for (int i = 0; i < gaps.length; i++)
		{
			at += gaps[i];
			strikeTicks[i] = at;
		}
	}

	void stop()
	{
		strikeTicks = null;
	}

	/**
	 * Ticks until the next strike (0 means pray now), or -1 when no strike is coming.
	 */
	int ticksUntilStrike(int tick)
	{
		if (strikeTicks == null)
		{
			return -1;
		}
		for (int strike : strikeTicks)
		{
			if (strike >= tick)
			{
				return strike - tick;
			}
		}
		return -1;
	}

	/**
	 * Which strike is next, counted from 1, or 0 when no strike is coming.
	 */
	int nextStrike(int tick)
	{
		if (strikeTicks == null)
		{
			return 0;
		}
		for (int i = 0; i < strikeTicks.length; i++)
		{
			if (strikeTicks[i] >= tick)
			{
				return i + 1;
			}
		}
		return 0;
	}

	int strikeCount()
	{
		return strikeTicks == null ? 0 : strikeTicks.length;
	}

	boolean isFinished(int tick)
	{
		return strikeTicks != null && ticksUntilStrike(tick) < 0;
	}
}
