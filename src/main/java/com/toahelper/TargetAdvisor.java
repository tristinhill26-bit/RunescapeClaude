package com.toahelper;

import java.util.EnumSet;
import java.util.Set;
import lombok.Value;

/**
 * What to hit your target with, and tips for it from the wiki's Tombs of Amascut/Strategies page.
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
		 * Styles that get through.
		 */
		Set<AttackStyle> use;
		/**
		 * A style it is especially weak to, if any, and only when it isn't blocked.
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

		if (npcId == ToaIds.AKKHA_MELEE || npcId == ToaIds.AKKHA_RANGE || npcId == ToaIds.AKKHA_MAGE || npcId == 11789)
		{
			if (npcId == ToaIds.AKKHA_MELEE)
			{
				blocked.add(AttackStyle.MELEE);
			}
			else if (npcId == ToaIds.AKKHA_RANGE)
			{
				blocked.add(AttackStyle.RANGED);
			}
			else if (npcId == ToaIds.AKKHA_MAGE)
			{
				blocked.add(AttackStyle.MAGIC);
			}
			best = AttackStyle.MAGIC;
			tip = "Prays against the style he attacks with. Keep him in melee form as long as you can.";
		}
		else if (ToaIds.AKKHA_ENRAGE.contains(npcId))
		{
			blocked.add(AttackStyle.MAGIC);
			blocked.add(AttackStyle.RANGED);
			best = AttackStyle.MELEE;
			tip = "Final stand: melee only. Path slowly around the orbs; don't rush him.";
		}
		else if (npcId == 11797 || npcId == 11798)
		{
			tip = "Kill one shadow at a time, before its hourglass fills.";
		}
		else if (npcId == ToaIds.BABA || npcId == 11779 || npcId == 11780)
		{
			best = AttackStyle.MELEE;
			tip = "Melee only; Protect from Melee blocks it all. Before 66%/33%, get to the top or sides.";
		}
		else if (npcId == ToaIds.BABA_BOULDER_WEAK)
		{
			best = AttackStyle.RANGED;
			tip = "Cracked boulder: ranged always max-hits it.";
		}
		else if (npcId == 11781)
		{
			tip = "10 HP, you always max-hit it. Kill it before it opens a sarcophagus.";
		}
		else if (ToaIds.ZEBAK.contains(npcId))
		{
			tip = "Specials at 85/70/55/40%. Under 25% he enrages and attacks every 4 ticks."
				+ " Stay out of melee: it hits through prayer and bleeds.";
		}
		else if (ToaIds.KEPHRI.contains(npcId))
		{
			best = AttackStyle.MELEE;
			tip = "Weakest to stab, very resistant to ranged and magic. Step off the fireball shadow.";
		}
		else if (npcId == ToaIds.KEPHRI_SWARM)
		{
			tip = "Swarm: heals her shield. You always max-hit swarms; clear them with a fast ranged weapon.";
		}
		else if (ToaIds.KEPHRI_EGGS.contains(npcId))
		{
			tip = "Egg: you always max-hit it. Left alone it explodes in a 3x3.";
		}
		else if (npcId == ToaIds.KEPHRI_GUARDIAN_MAGE)
		{
			best = AttackStyle.MELEE;
			tip = "Arcane Scarab: kill first. Heavy hits make it move and drop its charge.";
		}
		else if (npcId == ToaIds.KEPHRI_GUARDIAN_MELEE)
		{
			best = AttackStyle.MELEE;
			tip = "Soldier Scarab: its swarms recharge Kephri's shield.";
		}
		else if (npcId == ToaIds.KEPHRI_GUARDIAN_RANGED)
		{
			best = AttackStyle.MELEE;
			tip = "Spitting Scarab: hits everyone within 10 tiles, through prayer, and poisons. Kill it fast.";
		}
		else if (ToaIds.BABOON_MELEE.contains(npcId))
		{
			best = AttackStyle.MAGIC;
			tip = "Brawler: Magic always max-hits it. Kill last.";
		}
		else if (ToaIds.BABOON_RANGED.contains(npcId))
		{
			best = AttackStyle.MELEE;
			tip = "Thrower: Melee always max-hits it.";
		}
		else if (ToaIds.BABOON_MAGIC.contains(npcId))
		{
			best = AttackStyle.RANGED;
			tip = "Mage: Ranged always max-hits it. Kill first.";
		}
		else if (npcId == ToaIds.BABOON_SHAMAN)
		{
			best = AttackStyle.RANGED;
			tip = "Shaman: kill first, it summons thralls. Poison stops the summoning.";
		}
		else if (npcId == ToaIds.BABOON_VOLATILE)
		{
			best = AttackStyle.RANGED;
			tip = "Volatile: explodes in a 3x3 and kills baboons caught in it. Use it on clusters.";
		}
		else if (npcId == ToaIds.BABOON_CURSED)
		{
			best = AttackStyle.RANGED;
			tip = "Cursed: leaves venom pools. Kill it when nothing else is a threat.";
		}
		else if (npcId == ToaIds.BABOON_THRALL)
		{
			tip = "Thrall: kill last.";
		}
		else if (ToaIds.OBELISKS.contains(npcId))
		{
			best = AttackStyle.RANGED;
			tip = "Weakest to standard ranged (bows). In phase 2, don't stand next to it.";
		}
		else if (ToaIds.WARDENS_P2.contains(npcId))
		{
			blocked.add(AttackStyle.MELEE);
			tip = "Always prays Melee; Tumeken's starts on Magic, Elidinis' on Ranged."
				+ " A third down is worth more points; later ones aren't.";
		}
		else if (ToaIds.WARDEN_CORES.contains(npcId))
		{
			best = AttackStyle.MELEE;
			tip = "Core: melee always max-hits it, and it takes 5x damage. Dump your specs.";
		}
		else if (ToaIds.WARDENS_P3.contains(npcId))
		{
			tip = "Doesn't pray; equally weak to Magic and Ranged. Siphons at 80/60/40/20% need melee.";
		}
		else if (npcId == ToaIds.WARDENS_SIPHON)
		{
			best = AttackStyle.MELEE;
			blocked.add(AttackStyle.RANGED);
			blocked.add(AttackStyle.MAGIC);
			tip = "Energy Siphon: immune to ranged and magic.";
		}
		else if (npcId == ToaIds.HET_SEAL_VULNERABLE)
		{
			tip = "Open for 15 ticks: mine it now.";
		}

		Set<AttackStyle> use = EnumSet.complementOf(toEnumSet(blocked));
		if (best != null && !use.contains(best))
		{
			best = null;
		}

		double factor = ToaIds.POINTS_FACTORS.getOrDefault(npcId, 1.0);
		return new Advice(blocked, use, best, tip, factor);
	}

	/**
	 * The wiki's kill order for Apmeken's baboons (1 first), or 0 for no number.
	 */
	static int baboonKillOrder(int npcId)
	{
		if (ToaIds.BABOON_MAGIC.contains(npcId) || npcId == ToaIds.BABOON_SHAMAN)
		{
			return 1;
		}
		if (ToaIds.BABOON_RANGED.contains(npcId))
		{
			return 2;
		}
		if (npcId == ToaIds.BABOON_CURSED)
		{
			return 3;
		}
		if (ToaIds.BABOON_MELEE.contains(npcId) || npcId == ToaIds.BABOON_THRALL)
		{
			return 4;
		}
		return 0;
	}

	private static EnumSet<AttackStyle> toEnumSet(Set<AttackStyle> styles)
	{
		EnumSet<AttackStyle> s = EnumSet.noneOf(AttackStyle.class);
		s.addAll(styles);
		return s;
	}
}
