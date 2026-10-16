package com.toahelper;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(ToaHelperConfig.GROUP)
public interface ToaHelperConfig extends Config
{
	String GROUP = "toahelper";

	@ConfigSection(name = "Prayers", description = "Which protection prayer to use", position = 0)
	String prayerSection = "prayer";

	@ConfigSection(name = "Damage", description = "What to attack, and with what", position = 1)
	String damageSection = "damage";

	@ConfigSection(name = "Where to stand", description = "Safe tiles for boss specials", position = 2)
	String standingSection = "standing";

	@ConfigSection(name = "Points", description = "Raid points and purple chance", position = 3)
	String pointsSection = "points";

	@ConfigItem(
		keyName = "highlightPrayer",
		name = "Highlight prayer",
		description = "Highlight the protection prayer to click in the prayer book",
		section = prayerSection,
		position = 0
	)
	default boolean highlightPrayer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showIncoming",
		name = "Show incoming attacks",
		description = "List attacks heading at you, with the prayer and ticks until each lands",
		section = prayerSection,
		position = 1
	)
	default boolean showIncoming()
	{
		return true;
	}

	@ConfigItem(
		keyName = "standingPrayer",
		name = "Advise between attacks",
		description = "When nothing is in flight, advise a prayer from Akkha's form, being in Ba-Ba's or a melee"
			+ " baboon's reach, or an Arcane Scarab charging",
		section = prayerSection,
		position = 2
	)
	default boolean standingPrayer()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "prayerColor",
		name = "Prayer highlight colour",
		description = "Colour of the prayer to click",
		section = prayerSection,
		position = 3
	)
	default Color prayerColor()
	{
		return new Color(255, 255, 0, 110);
	}

	@ConfigItem(
		keyName = "showTarget",
		name = "Show target info",
		description = "Show what your target is praying, what to hit it with, its points multiplier and a wiki tip",
		section = damageSection,
		position = 0
	)
	default boolean showTarget()
	{
		return true;
	}

	@ConfigItem(
		keyName = "priorityTargets",
		name = "Highlight priority targets",
		description = "Mark Ba-Ba's cracked boulder, Het's seal while it's open and Kephri's Arcane Scarab",
		section = damageSection,
		position = 1
	)
	default boolean priorityTargets()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "priorityColor",
		name = "Priority target colour",
		description = "Colour of priority targets",
		section = damageSection,
		position = 2
	)
	default Color priorityColor()
	{
		return new Color(0, 200, 255, 90);
	}

	@ConfigItem(
		keyName = "zebakShelter",
		name = "Zebak roar shelter",
		description = "Mark the tiles behind Zebak's rocks that block his roar",
		section = standingSection,
		position = 0
	)
	default boolean zebakShelter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "babaCover",
		name = "Ba-Ba rock throw cover",
		description = "After a rockfall, mark the tiles next to debris and sarcophagi, which her thrown rock can't hit",
		section = standingSection,
		position = 1
	)
	default boolean babaCover()
	{
		return true;
	}

	@ConfigItem(
		keyName = "wardenSlams",
		name = "Wardens P3 slams",
		description = "Mark the nearest safe tile on your row when the Warden slams the floor",
		section = standingSection,
		position = 2
	)
	default boolean wardenSlams()
	{
		return true;
	}

	@ConfigItem(
		keyName = "flipSlams",
		name = "Flip slam sides",
		description = "Turn on if the slam safe tile shows up on the raised side",
		section = standingSection,
		position = 3
	)
	default boolean flipSlams()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "safeColor",
		name = "Safe tile colour",
		description = "Colour of tiles to stand on",
		section = standingSection,
		position = 4
	)
	default Color safeColor()
	{
		return new Color(0, 255, 0, 70);
	}

	@ConfigItem(
		keyName = "showPoints",
		name = "Show points",
		description = "Estimate your raid points, what a death would cost, and your purple chance",
		section = pointsSection,
		position = 0
	)
	default boolean showPoints()
	{
		return true;
	}
}
