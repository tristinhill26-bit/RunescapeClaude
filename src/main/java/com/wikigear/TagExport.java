package com.wikigear;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builds bank tag layouts, and the text Bank Tags reads with "Import tag tab", in the same format as its
 * "Export tag tab": {@code banktags,1,<tag>,<icon item>,layout,<position>,<item>,<position>,<item>,...}
 */
public class TagExport
{
	private static final GearSlot[] ICON_PREFERENCE = {
		GearSlot.WEAPON, GearSlot.TWO_HANDED, GearSlot.HEAD, GearSlot.BODY
	};

	private TagExport() {
	}

	/**
	 * Lay out the gear like the equipment screen, with the inventory in a 4 by 7 grid to its right
	 */
	public static int[] layout(GearPicker.Result result) {
		int[] positions = new int[GearSlot.BANK_COLUMNS * GearSlot.LAYOUT_ROWS];
		Arrays.fill(positions, -1);
		for (var pick : result.picks.values())
			positions[pick.slot.getLayoutPosition()] = pick.itemId;
		// Items can fill several inventory slots, like potions, and are shown in each of them
		for (int slot = 0; slot < result.inventory.length; slot++)
			if (result.inventory[slot] != -1)
				positions[GearSlot.inventoryLayoutPosition(slot)] = result.inventory[slot];
		return positions;
	}

	public static int iconItem(GearPicker.Result result, int[] layout) {
		for (var slot : ICON_PREFERENCE) {
			var pick = result.picks.get(slot);
			if (pick != null)
				return pick.itemId;
		}
		for (int itemId : layout)
			if (itemId != -1)
				return itemId;
		return -1;
	}

	/**
	 * Bank Tags strips these characters from imported tag names, and commas would break the format
	 */
	public static String cleanTagName(String tag) {
		var name = new StringBuilder();
		for (char c : tag.toCharArray())
			if ("</>:,".indexOf(c) == -1)
				name.append(c);
		return name.toString().replaceAll("\\s+", " ").trim();
	}

	public static String toImportText(String tag, int iconItemId, int[] layout) {
		List<String> data = new ArrayList<>();
		data.add("banktags");
		data.add("1");
		data.add(cleanTagName(tag));
		data.add(String.valueOf(iconItemId));
		data.add("layout");
		for (int position = 0; position < layout.length; position++) {
			if (layout[position] == -1)
				continue;
			data.add(String.valueOf(position));
			data.add(String.valueOf(layout[position]));
		}
		return String.join(",", data);
	}
}
