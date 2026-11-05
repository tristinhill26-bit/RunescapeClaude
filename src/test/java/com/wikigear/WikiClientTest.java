package com.wikigear;

import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;

public class WikiClientTest {
	@Test
	public void namesSubpagesAfterTheirMainPage() {
		assertEquals("Barrows", WikiClient.mainPage("Barrows/Strategy"));
		assertEquals("Chambers of Xeric", WikiClient.mainPage("Chambers of Xeric/Strategies"));
		assertEquals("Vorkath", WikiClient.mainPage("Vorkath"));
	}

	@Test
	public void combinesActivitiesWithMonsters() {
		var guides = List.of(
			new WikiClient.SearchHit(0, "Barrows/Strategy", null),
			new WikiClient.SearchHit(1, "Ahrim the Blighted/Strategies", null)
		);
		var monsters = List.of(
			new WikiClient.SearchHit(0, "Ahrim the Blighted", "https://example/ahrim.png"),
			new WikiClient.SearchHit(1, "Dharok the Wretched", "https://example/dharok.png")
		);

		var targets = WikiClient.combine("Barrows", guides, monsters);
		assertEquals(3, targets.size());

		assertEquals("Barrows", targets.get(0).name);
		assertEquals("Barrows/Strategy", targets.get(0).page);
		assertTrue(targets.get(0).isGuide());

		// The monster result is merged into the strategy page result, keeping the strategy page
		assertEquals("Ahrim the Blighted", targets.get(1).name);
		assertEquals("Ahrim the Blighted/Strategies", targets.get(1).page);

		assertEquals("Dharok the Wretched", targets.get(2).name);
		assertNull(targets.get(2).page);
		assertEquals("https://example/dharok.png", targets.get(2).thumbnailUrl);
	}

	@Test
	public void putsNamesStartingWithTheQueryFirst() {
		var guides = List.of(
			new WikiClient.SearchHit(0, "Galvek/Strategies", null),
			new WikiClient.SearchHit(1, "Vorkath/Strategies", null)
		);
		var targets = WikiClient.combine("vork", guides, List.of());
		assertEquals("Vorkath", targets.get(0).name);
		assertEquals("Galvek", targets.get(1).name);
	}
}
