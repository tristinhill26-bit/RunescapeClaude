package com.madangel;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(MadAngelConfig.GROUP)
public interface MadAngelConfig extends Config
{
	String GROUP = "madangelhelper";

	@ConfigSection(
		name = "Sweep",
		description = "The sword cleave that swaps sides",
		position = 0
	)
	String sweepSection = "sweep";

	@ConfigSection(
		name = "Exploding Light",
		description = "The ball of light you bounce back by standing on its tile",
		position = 1
	)
	String lightSection = "light";

	@ConfigItem(
		keyName = "showDodge",
		name = "Show dodge tile",
		description = "Mark the tile to click: beside her, opposite the sword. Dodging here gives the accuracy bonus.",
		section = sweepSection,
		position = 0
	)
	default boolean showDodge()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showBehind",
		name = "Show behind tile",
		description = "Also mark the tile behind her, which is safe but gives no bonus",
		section = sweepSection,
		position = 1
	)
	default boolean showBehind()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDanger",
		name = "Show cleave area",
		description = "Shade the tiles the cleave hits",
		section = sweepSection,
		position = 2
	)
	default boolean showDanger()
	{
		return true;
	}

	@Range(min = 1, max = 15)
	@ConfigItem(
		keyName = "dangerRange",
		name = "Cleave area size",
		description = "How many tiles out from her to shade",
		section = sweepSection,
		position = 3
	)
	default int dangerRange()
	{
		return 6;
	}

	@ConfigItem(
		keyName = "flipSides",
		name = "Flip left/right",
		description = "Turn this on if the dodge tile shows up on the sword's side instead of the side away from it",
		section = sweepSection,
		position = 4
	)
	default boolean flipSides()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "dodgeColor",
		name = "Dodge colour",
		description = "Colour of the tile to click",
		section = sweepSection,
		position = 5
	)
	default Color dodgeColor()
	{
		return new Color(0, 255, 0, 120);
	}

	@Alpha
	@ConfigItem(
		keyName = "behindColor",
		name = "Behind colour",
		description = "Colour of the tile behind her",
		section = sweepSection,
		position = 6
	)
	default Color behindColor()
	{
		return new Color(255, 220, 0, 90);
	}

	@Alpha
	@ConfigItem(
		keyName = "dangerColor",
		name = "Cleave colour",
		description = "Colour of the tiles the cleave hits",
		section = sweepSection,
		position = 7
	)
	default Color dangerColor()
	{
		return new Color(255, 0, 0, 45);
	}

	@ConfigItem(
		keyName = "showLight",
		name = "Show landing tile",
		description = "Mark the tile to stand on to bounce the ball back",
		section = lightSection,
		position = 0
	)
	default boolean showLight()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "lightColor",
		name = "Landing tile colour",
		description = "Colour of the tile to stand on",
		section = lightSection,
		position = 1
	)
	default Color lightColor()
	{
		return new Color(0, 220, 255, 120);
	}
}
