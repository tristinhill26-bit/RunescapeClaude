package com.toahelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.toahelper.StandingGeometry.Slam;
import com.toahelper.StandingGeometry.SlamAdvice;
import com.toahelper.StandingGeometry.Tile;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class ToaHelperLogicTest
{
	// ---- Prayer ----

	@Test
	public void soonestProjectileWins()
	{
		PrayerAdvisor.Advice advice = PrayerAdvisor.advise(Arrays.asList(
			new PrayerAdvisor.Incoming(AttackStyle.RANGED, 3, "a"),
			new PrayerAdvisor.Incoming(AttackStyle.MAGIC, 1, "b")
		), AttackStyle.MELEE, "Akkha's form");

		assertEquals(AttackStyle.MAGIC, advice.getPray());
		assertEquals(1, advice.getTicks());
		assertEquals(AttackStyle.RANGED, advice.getQueue().get(1).getStyle());
	}

	@Test
	public void standingThreatWhenNothingInFlight()
	{
		PrayerAdvisor.Advice advice = PrayerAdvisor.advise(Collections.emptyList(), AttackStyle.MELEE, "In Ba-Ba's reach");
		assertEquals(AttackStyle.MELEE, advice.getPray());
		assertEquals(-1, advice.getTicks());

		assertNull(PrayerAdvisor.advise(Collections.emptyList(), null, null).getPray());
	}

	@Test
	public void headIcons()
	{
		assertEquals(EnumSet.of(AttackStyle.MAGIC), AttackStyle.fromHeadIcon(2));
		assertEquals(EnumSet.of(AttackStyle.RANGED, AttackStyle.MELEE), AttackStyle.fromHeadIcon(7));
		assertTrue(AttackStyle.fromHeadIcon(4).isEmpty()); // smite
	}

	// ---- Damage ----

	@Test
	public void akkhaBlocksHisOwnStyleAndIsWeakToMagic()
	{
		TargetAdvisor.Advice melee = TargetAdvisor.advise(ToaIds.AKKHA_MELEE, EnumSet.noneOf(AttackStyle.class));
		assertEquals(EnumSet.of(AttackStyle.MELEE), melee.getBlocked());
		assertEquals(AttackStyle.MAGIC, melee.getBest());

		TargetAdvisor.Advice mage = TargetAdvisor.advise(ToaIds.AKKHA_MAGE, EnumSet.noneOf(AttackStyle.class));
		assertNull(mage.getBest()); // magic is blocked, so no "best"
		assertEquals(EnumSet.of(AttackStyle.MELEE, AttackStyle.RANGED), mage.getUse());
	}

	@Test
	public void wardensAlwaysBlockMelee()
	{
		Set<AttackStyle> overhead = AttackStyle.fromHeadIcon(1); // ranged
		TargetAdvisor.Advice advice = TargetAdvisor.advise(11753, overhead);
		assertEquals(EnumSet.of(AttackStyle.MELEE, AttackStyle.RANGED), advice.getBlocked());
		assertEquals(EnumSet.of(AttackStyle.MAGIC), advice.getUse());
		assertEquals(2.0, advice.getPointsFactor(), 0);
	}

	// ---- Points ----

	@Test
	public void pointsCapsAndDeath()
	{
		PointsCalculator p = new PointsCalculator();
		assertEquals(0, p.lootPoints());

		assertEquals(2000, p.addDamage(1000, 2.0, 20_000, false));
		assertEquals(2000, p.getRoom());
		assertEquals(7000, p.getTotal());

		// Zebak's room caps at 10,000.
		p.roomEnded();
		p.addDamage(20_000, 1.5, 10_000, false);
		assertEquals(10_000, p.getRoom());
		assertEquals(17_000, p.getTotal());

		// 20% of 17,000.
		assertEquals(3400, p.died());
		assertEquals(13_600, p.getTotal());
	}

	@Test
	public void deathCostsAtLeastAThousand()
	{
		PointsCalculator p = new PointsCalculator();
		assertEquals(1000, p.deathPenalty()); // 20% of 5,000 is less
		p.died();
		assertEquals(4000, p.getTotal());
	}

	@Test
	public void wardensStopScoringAfterThreeDowns()
	{
		PointsCalculator p = new PointsCalculator();
		p.wardenDowned();
		p.wardenDowned();
		p.wardenDowned();
		assertEquals(0, p.addDamage(100, 2.0, 60_000, true));
		assertEquals(250, p.addDamage(100, 2.5, 60_000, false));
	}

	@Test
	public void totalCapsAt64000()
	{
		PointsCalculator p = new PointsCalculator();
		for (int i = 0; i < 5; i++)
		{
			p.addDamage(20_000, 1.0, 20_000, false);
			p.roomEnded();
		}
		assertEquals(64_000, p.getTotal());
	}

	@Test
	public void uniqueChanceMatchesWikiExample()
	{
		// Wiki: at raid level 400, 1% per 3,700 points.
		assertEquals(0.01, PointsCalculator.uniqueChance(400, 3700), 1e-9);
		// Below 310 it's linear: 10,500 - 20 x 150 = 7,500 points per 1%.
		assertEquals(0.02, PointsCalculator.uniqueChance(150, 15_000), 1e-9);
		// Capped at 55%.
		assertEquals(0.55, PointsCalculator.uniqueChance(500, 1_000_000), 1e-9);
	}

	// ---- Where to stand ----

	@Test
	public void shelterIsBehindTheRock()
	{
		// Zebak to the west, 1x1 rock to the east of him: shelter is further east.
		Set<Tile> shelter = StandingGeometry.roarShelter(new Tile(0, 0), new Tile(5, 0), 1);
		assertEquals(3, shelter.size());
		assertTrue(shelter.contains(new Tile(6, 0)));
		assertTrue(shelter.contains(new Tile(8, 0)));
		assertFalse(shelter.contains(new Tile(4, 0)));
	}

	@Test
	public void shelterForBigRockDiagonal()
	{
		// 2x2 rock to the north-east: shelter steps diagonally away, never onto the rock.
		Set<Tile> shelter = StandingGeometry.roarShelter(new Tile(0, 0), new Tile(5, 5), 2);
		assertTrue(shelter.contains(new Tile(7, 7)));
		assertFalse(shelter.contains(new Tile(5, 5)));
		assertFalse(shelter.contains(new Tile(6, 6)));
	}

	@Test
	public void ringAroundFootprint()
	{
		List<Tile> ring = StandingGeometry.adjacentRing(new Tile(10, 10), 2);
		assertEquals(12, ring.size());
		assertTrue(ring.contains(new Tile(9, 9)));
		assertTrue(ring.contains(new Tile(12, 12)));
		assertFalse(ring.contains(new Tile(10, 10)));
	}

	@Test
	public void slamSafeTiles()
	{
		Tile centre = new Tile(50, 20);
		// Right (east) side raised: go at least one tile west of centre.
		SlamAdvice right = StandingGeometry.slam(Slam.RIGHT, 50, centre);
		assertEquals(new Tile(49, 20), right.getSafe());
		assertFalse(right.isAlreadySafe());

		// Already far enough west: stay put.
		assertTrue(StandingGeometry.slam(Slam.RIGHT, 50, new Tile(46, 20)).isAlreadySafe());

		assertEquals(new Tile(51, 20), StandingGeometry.slam(Slam.LEFT, 50, centre).getSafe());

		// Both: back to the centre column.
		assertEquals(new Tile(50, 20), StandingGeometry.slam(Slam.BOTH, 50, new Tile(53, 20)).getSafe());
		assertEquals(Slam.LEFT, Slam.RIGHT.flipped());
	}

	@Test
	public void roomsFromRegions()
	{
		assertEquals(ToaIds.Room.ZEBAK, ToaIds.Room.forRegion(15700));
		assertEquals(ToaIds.Room.WARDENS, ToaIds.Room.forRegion(15696));
		assertNull(ToaIds.Room.forRegion(12850));
		assertEquals(60_000, ToaIds.Room.WARDENS.pointsCap);
	}
}
