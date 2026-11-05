package com.toahelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Value;

/**
 * Picks the protection prayer to have on, from what is about to hit you.
 */
final class PrayerAdvisor
{
	@Value
	static class Incoming
	{
		AttackStyle style;
		/**
		 * Game ticks until it hits, 0 = this tick.
		 */
		int ticks;
		String source;
	}

	@Value
	static class Advice
	{
		/**
		 * The prayer to have on now, or null for no advice.
		 */
		AttackStyle pray;
		/**
		 * Ticks until that attack lands, or -1 for a standing threat (e.g. a boss's current form).
		 */
		int ticks;
		String reason;
		/**
		 * Everything incoming, soonest first, for the on-screen list.
		 */
		List<Incoming> queue;
	}

	private PrayerAdvisor()
	{
	}

	/**
	 * @param incoming attacks in flight at you
	 * @param standing the style to pray when nothing is in flight (from the boss's form or
	 *                 because you're in melee range), or null
	 * @param standingReason why, for display
	 */
	static Advice advise(List<Incoming> incoming, AttackStyle standing, String standingReason)
	{
		List<Incoming> queue = new ArrayList<>(incoming);
		queue.sort(Comparator.comparingInt(Incoming::getTicks));

		if (!queue.isEmpty())
		{
			Incoming next = queue.get(0);
			return new Advice(next.getStyle(), next.getTicks(), next.getSource(), queue);
		}

		if (standing != null)
		{
			return new Advice(standing, -1, standingReason, queue);
		}

		return new Advice(null, -1, null, queue);
	}
}
