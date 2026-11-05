package com.wikigear;

import org.junit.Test;

import static org.junit.Assert.*;

public class WildernessDetectorTest {
	@Test
	public void recognisesWildernessBossesByName() {
		assertTrue(WildernessDetector.isWilderness("Callisto/Strategies", ""));
		assertTrue(WildernessDetector.isWilderness("Vet'ion", ""));
		assertTrue(WildernessDetector.isWilderness("Revenant Caves/Strategies", ""));
		assertTrue(WildernessDetector.isWilderness("Wilderness Slayer Cave", ""));
	}

	@Test
	public void recognisesWildernessFromThePage() {
		String page = "This boss is fought deep in the [[Wilderness]]. Bring blighted supplies, since the Wilderness... " +
			"Beware of PKers in the Wilderness.";
		assertTrue(WildernessDetector.isWilderness("Some new boss/Strategies", page));
	}

	@Test
	public void ignoresPassingMentions() {
		String page = "Unlike bosses in the Wilderness, Mad Angel can't be attacked by other players.";
		assertFalse(WildernessDetector.isWilderness("Mad Angel/Strategies", page));
		assertFalse(WildernessDetector.isWilderness("Barrows/Strategy", null));
	}

	@Test
	public void spotsBlightedItems() {
		assertTrue(WildernessDetector.isBlighted("Blighted super restore(4)"));
		assertTrue(WildernessDetector.isBlighted("Blighted karambwan"));
		assertFalse(WildernessDetector.isBlighted("Super restore(4)"));
	}
}
