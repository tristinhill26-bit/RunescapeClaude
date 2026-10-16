package com.toahelper;

import java.util.EnumSet;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
enum AttackStyle
{
	MELEE("Melee", "Protect from Melee"),
	RANGED("Ranged", "Protect from Missiles"),
	MAGIC("Magic", "Protect from Magic");

	/**
	 * The sprite archive NPC overhead prayer icons come from (gameval SpriteID.HEADICONS_PRAYER).
	 */
	static final int HEADICONS_PRAYER_ARCHIVE = 440;

	private final String displayName;
	/**
	 * The prayer's name as shown in the prayer book, used to find its widget.
	 */
	private final String prayerName;

	/**
	 * Decodes an NPC overhead prayer icon (index into archive 440) into the styles it protects
	 * against. The order matches net.runelite.api.HeadIcon.
	 */
	static Set<AttackStyle> fromHeadIcon(int spriteIndex)
	{
		switch (spriteIndex)
		{
			case 0:
				return EnumSet.of(MELEE);
			case 1:
				return EnumSet.of(RANGED);
			case 2:
				return EnumSet.of(MAGIC);
			case 6:
				return EnumSet.of(RANGED, MAGIC);
			case 7:
				return EnumSet.of(RANGED, MELEE);
			case 8:
				return EnumSet.of(MAGIC, MELEE);
			case 9:
				return EnumSet.allOf(AttackStyle.class);
			default:
				return EnumSet.noneOf(AttackStyle.class);
		}
	}
}
