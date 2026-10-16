package com.toahelper;

import java.util.EnumSet;
import java.util.Set;
import lombok.Value;

/**
 * What to hit your target with, and wiki tips for it.
 */
final class TargetAdvisor
{
	@Value
	static class Advice
	{
		/**
		 * Styles the target blocks. Empty when it isn't praying or we can't tell.
		 */
		Set<AttackStyle> blocked;
		/**
		 * Styles that get through, best first when one is known to be best.
		 */
		Set<AttackStyle> use;
		/**
		 * A style it is especially weak to, if any.
		 */
		AttackStyle best;
		String tip;
		/**
		 * Room points per damage.
		 */
		double pointsFactor;
	}

	private TargetAdvisor()
	{
	}

	/**
	 * @param npcId    the target's NPC id
	 * @param overhead what its overhead prayer blocks (empty if none)
	 */
	static Advice advise(int npcId, Set<AttackStyle> overhead)
	{
		Set<AttackStyle> blocked = EnumSet.noneOf(AttackStyle.class);
		blocked.addAll(overhead);

		AttackStyle best = null;
		String tip = null;

		switch (npcId)
		{
			case ToaIds.AKKHA_MELEE:
				blocked.add(AttackStyle.MELEE);
				break;
			case ToaIds.AKKHA_RANGE:
				blocked.add(AttackStyle.RANGED);
				break;
			case ToaIds.AKKHA_MAGE:
				blocked.add(AttackStyle.MAGIC);
				break;
			default:
				break;
		}

		if (npcId >= 11789 && npcId <= 11796)
		{
			best = AttackStyle.MAGIC;
			tip = "Prays against the style he attacks with. Very weak to Magic.";
		}
		else if (npcId == ToaIds.BABA || npcId == 11779 || npcId == 11780)
		{
			best = AttackStyle.MELEE;
			tip = "Weakest to Stab. Meleeing her makes her stop throwing boulders early.";
		}
		else if (npcId == ToaIds.BABA_BOULDER_WEAK)
		{
			best = AttackStyle.RANGED;
			tip = "Cracked boulder: guaranteed max hit. Range it.";
		}
		else if (ToaIds.ZEBAK.contains(npcId))
		{
			tip = "Stay out of melee range: his melee hits through prayer and bleeds. Room cap 10,000.";
		}
		else if (npcId == ToaIds.KEPHRI_GUARDIAN_MAGE)
		{
			tip = "Arcane Scarab: kill first. Pray Magic if its charged attack goes off.";
		}
		else if (npcId == ToaIds.KEPHRI_GUARDIAN_MELEE)
		{
			tip = "Soldier Scarab: kill after the Arcane Scarab, or it heals Kephri.";
		}
		else if (npcId == ToaIds.KEPHRI_GUARDIAN_RANGED)
		{
			tip = "Spitting Scarab: poisons and hits through prayer. Kill it quickly.";
		}
		else if (ToaIds.WARDENS_P2.contains(npcId))
		{
			blocked.add(AttackStyle.MELEE);
			tip = "Both Wardens always pray Melee. Points stop after 3 downs.";
		}
		else if (ToaIds.WARDENS_P3.contains(npcId))
		{
			tip = "Melee the skulls at 80/60/40%.";
		}
		else if (npcId == ToaIds.HET_SEAL_VULNERABLE)
		{
			tip = "Open now: hit it.";
		}

		Set<AttackStyle> use = EnumSet.complementOf(toEnumSet(blocked));
		if (best != null && !use.contains(best))
		{
			best = null;
		}

		double factor = ToaIds.POINTS_FACTORS.getOrDefault(npcId, 1.0);
		return new Advice(blocked, use, best, tip, factor);
	}

	private static EnumSet<AttackStyle> toEnumSet(Set<AttackStyle> styles)
	{
		EnumSet<AttackStyle> s = EnumSet.noneOf(AttackStyle.class);
		s.addAll(styles);
		return s;
	}
}
