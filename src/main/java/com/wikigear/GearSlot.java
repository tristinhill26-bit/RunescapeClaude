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
	HEAD("head", "Head", 0, 1),
	CAPE("cape", "Cape", 1, 0),
	NECK("neck", "Neck", 1, 1),
	AMMO("ammo", "Ammo", 1, 2),
	WEAPON("weapon", "Weapon", 2, 0),
	BODY("body", "Body", 2, 1),
	SHIELD("shield", "Shield", 2, 2),
	LEGS("legs", "Legs", 3, 1),
	HANDS("hands", "Hands", 4, 0),
	FEET("feet", "Feet", 4, 1),
	RING("ring", "Ring", 4, 2),
	TWO_HANDED("2h", "Two-handed", 2, 4),
	SPECIAL("special", "Special attack", 2, 5);

	public static final int BANK_COLUMNS = 8;

	private final String wikiName;
	private final String displayName;
	private final int row;
	private final int column;

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
