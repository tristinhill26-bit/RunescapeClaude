package com.wikigear;

import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Equipment slots used by the wiki's {{Recommended equipment}} template, along with where each slot goes in the
 * generated bank tag layout. The layout mirrors the equipment screen, using the bank's 8 item columns.
 */
@Getter
@RequiredArgsConstructor
public enum GearSlot
{
	HEAD("head", "Head", 0, 1, 0),
	CAPE("cape", "Cape", 1, 0, 1),
	NECK("neck", "Neck", 1, 1, 2),
	AMMO("ammo", "Ammo", 1, 2, 13),
	WEAPON("weapon", "Weapon", 2, 0, 3),
	BODY("body", "Body", 2, 1, 4),
	SHIELD("shield", "Shield", 2, 2, 5),
	LEGS("legs", "Legs", 3, 1, 7),
	HANDS("hands", "Hands", 4, 0, 9),
	FEET("feet", "Feet", 4, 1, 10),
	RING("ring", "Ring", 4, 2, 12),
	TWO_HANDED("2h", "Two-handed", 2, 4, 3),
	SPECIAL("special", "Special attack", 2, 5, 3);

	public static final int BANK_COLUMNS = 8;

	private final String wikiName;
	private final String displayName;
	private final int row;
	private final int column;
	/**
	 * The slot's index in the equipment inventory, matching {@link net.runelite.api.EquipmentInventorySlot}
	 */
	private final int equipmentSlot;

	public int getLayoutPosition() {
		return row * BANK_COLUMNS + column;
	}

	@Nullable
	public static GearSlot fromWikiName(String name) {
		for (GearSlot slot : values())
			if (slot.wikiName.equalsIgnoreCase(name))
				return slot;
		return null;
	}
}
