package com.fairyringlabels;

import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.*;

public class FairyRingLabelerTest {
	private final FairyRingLabeler labeler = new FairyRingLabeler(Map.of(
		"CIR", "Farming Guild",
		"DKR", "Edgeville, Grand Exchange"
	)::get);

	@Test
	public void labelsPlainCode() {
		assertEquals("CIR - Farming Guild", labeler.label("CIR"));
	}

	@Test
	public void labelsCodeInsideColourTags() {
		assertEquals("<col=ff9040>CIR - Farming Guild</col>", labeler.label("<col=ff9040>CIR</col>"));
	}

	@Test
	public void labelsLastDestinationOption() {
		assertEquals("Last-destination (DKR - Edgeville, Grand Exchange)", labeler.label("Last-destination (DKR)"));
	}

	@Test
	public void doesNotLabelTwice() {
		String once = labeler.label("CIR");
		assertEquals(once, labeler.label(once));
	}

	@Test
	public void ignoresUnknownCodesAndWords() {
		assertEquals("AIR", labeler.label("AIR"));
		assertEquals("CIRCLE", labeler.label("CIRCLE"));
		assertEquals("Configure", labeler.label("Configure"));
	}

	@Test
	public void shortensRuneLiteDestinations() {
		assertEquals("Farming Guild", FairyRingLabeler.shorten("North-east of the Farming Guild"));
		assertEquals("Ardougne (island)", FairyRingLabeler.shorten("(Island) South-east of Ardougne"));
		assertEquals("Zul-Andra (island)", FairyRingLabeler.shorten("(Island) Near Zul-Andra"));
		assertEquals("Mort Myre Swamp", FairyRingLabeler.shorten("Mort Myre Swamp, south of Canifis"));
		assertEquals("Edgeville, Grand Exchange", FairyRingLabeler.shorten("Edgeville, Grand Exchange"));
		assertEquals("Cave south of Dorgesh-Kaan", FairyRingLabeler.shorten("Cave south of Dorgesh-Kaan"));
		assertEquals("Zanaris", FairyRingLabeler.shorten("Zanaris"));
	}

	@Test
	public void parsesCustomNames() {
		var names = FairyRingLabeler.parseCustomNames("cir=Konar, DKR = GE\nnope=x\nBKS=");
		assertEquals(Map.of("CIR", "Konar", "DKR", "GE"), names);
	}
}
