package com.wikigear;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.*;

public class TagExportTest {
	private static GearPicker.Result result() {
		var picks = new EnumMap<GearSlot, GearPicker.Pick>(GearSlot.class);
		picks.put(GearSlot.HEAD, new GearPicker.Pick(GearSlot.HEAD, 4708, "Ahrim's hood", 1, 1, GearPicker.Source.WIKI));
		picks.put(GearSlot.WEAPON, new GearPicker.Pick(GearSlot.WEAPON, 11905, "Trident of the seas", 1, 1, GearPicker.Source.WIKI));
		int[] inventory = new int[GearSetup.INVENTORY_SIZE];
		Arrays.fill(inventory, -1);
		inventory[0] = 2434;
		inventory[1] = 2434;
		inventory[27] = 952;
		return new GearPicker.Result(picks, new ArrayList<>(), CombatStyle.MAGIC, inventory, new ArrayList<>(), new HashMap<>());
	}

	@Test
	public void writesBankTagsImportFormat() {
		var result = result();
		int[] layout = TagExport.layout(result);
		String text = TagExport.toImportText("gear barrows magic", TagExport.iconItem(result, layout), layout);

		assertTrue(text.startsWith("banktags,1,gear barrows magic,11905,layout,"));

		// Read it back the way Bank Tags' "Import tag tab" does
		Iterator<String> data = Arrays.asList(text.split(",")).iterator();
		assertEquals("banktags", data.next());
		assertEquals("1", data.next());
		assertEquals("gear barrows magic", data.next());
		assertEquals(11905, Integer.parseInt(data.next()));
		assertEquals("layout", data.next());
		Map<Integer, Integer> positions = new HashMap<>();
		while (data.hasNext())
			positions.put(Integer.parseInt(data.next()), Integer.parseInt(data.next()));

		assertEquals(Integer.valueOf(4708), positions.get(GearSlot.HEAD.getLayoutPosition()));
		assertEquals(Integer.valueOf(11905), positions.get(GearSlot.WEAPON.getLayoutPosition()));
		// Repeated inventory items keep every slot
		assertEquals(Integer.valueOf(2434), positions.get(GearSlot.inventoryLayoutPosition(0)));
		assertEquals(Integer.valueOf(2434), positions.get(GearSlot.inventoryLayoutPosition(1)));
		assertEquals(Integer.valueOf(952), positions.get(GearSlot.inventoryLayoutPosition(27)));
		assertEquals(5, positions.size());
	}

	@Test
	public void removesCharactersBankTagsWouldStrip() {
		assertEquals("gear zulrah magic (phase 2)", TagExport.cleanTagName("gear zulrah: magic, (phase 2)"));
		assertEquals("gear ab", TagExport.cleanTagName("gear <a>/b"));
	}

	@Test
	public void usesAnyItemAsIconWhenThereIsNoGear() {
		var empty = new GearPicker.Result(new EnumMap<>(GearSlot.class), Collections.emptyList(), null,
			new int[] { -1, 385 }, Collections.emptyList(), Collections.emptyMap());
		int[] layout = TagExport.layout(empty);
		assertEquals(385, TagExport.iconItem(empty, layout));
	}
}
