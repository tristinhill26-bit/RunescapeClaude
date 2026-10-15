package com.shadowtiles;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(ShadowTilesConfig.GROUP)
public interface ShadowTilesConfig extends Config
{
	String GROUP = "shadowtiles";

	@ConfigSection(
		name = "Built-in attacks",
		description = "Toggle the attacks that ship with the plugin",
		position = 0
	)
	String builtInSection = "builtIn";

	@ConfigSection(
		name = "Custom attacks",
		description = "Your own server's bosses and attacks",
		position = 1
	)
	String customSection = "custom";

	@ConfigSection(
		name = "Appearance",
		description = "How highlighted tiles look",
		position = 2
	)
	String appearanceSection = "appearance";

	@ConfigSection(
		name = "ID finder",
		description = "Helps you find the IDs of new attacks",
		position = 3,
		closedByDefault = true
	)
	String debugSection = "debug";

	@ConfigItem(
		keyName = "fallingShadows",
		name = "Falling debris shadows",
		description = "The generic ground shadow shown before rocks/debris land (Grotesque Guardians, Olm, DT2 bosses, ...)",
		section = builtInSection,
		position = 0
	)
	default boolean fallingShadows()
	{
		return true;
	}

	@ConfigItem(
		keyName = "baba",
		name = "Ba-Ba",
		description = "Ba-Ba's falling rocks and rolling boulders",
		section = builtInSection,
		position = 1
	)
	default boolean baba()
	{
		return true;
	}

	@ConfigItem(
		keyName = "olm",
		name = "Great Olm",
		description = "Falling crystals, crystal bombs and acid pools",
		section = builtInSection,
		position = 2
	)
	default boolean olm()
	{
		return true;
	}

	@ConfigItem(
		keyName = "vorkath",
		name = "Vorkath",
		description = "Firebomb landing area and acid phase projectiles",
		section = builtInSection,
		position = 3
	)
	default boolean vorkath()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hydra",
		name = "Alchemical Hydra",
		description = "Poison splat landing areas",
		section = builtInSection,
		position = 4
	)
	default boolean hydra()
	{
		return true;
	}

	@ConfigItem(
		keyName = "zulrah",
		name = "Zulrah",
		description = "Toxic clouds",
		section = builtInSection,
		position = 5
	)
	default boolean zulrah()
	{
		return true;
	}

	@ConfigItem(
		keyName = "cerberus",
		name = "Cerberus",
		description = "Lava pools",
		section = builtInSection,
		position = 6
	)
	default boolean cerberus()
	{
		return true;
	}

	@ConfigItem(
		keyName = "verzik",
		name = "Verzik",
		description = "Phase 2 ranged bomb landing areas",
		section = builtInSection,
		position = 7
	)
	default boolean verzik()
	{
		return true;
	}

	@ConfigItem(
		keyName = "madAngel",
		name = "Mad Angel",
		description = "Smites, thrown bombs and sweeps (its falling debris is under Falling debris shadows)",
		section = builtInSection,
		position = 8
	)
	default boolean madAngel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "muspah",
		name = "Phantom Muspah",
		description = "Spike warnings",
		section = builtInSection,
		position = 9
	)
	default boolean muspah()
	{
		return true;
	}

	@ConfigItem(
		keyName = "customDefinitions",
		name = "Custom attacks",
		description = "One per line: type, id, radius, label, color, ticks<br>"
			+ "type = graphic, projectile, object, npc or anim<br>"
			+ "Only type and id are required. A line with the same type and id as a built-in replaces it.",
		section = customSection,
		position = 0
	)
	default String customDefinitions()
	{
		return "# type, id, radius, label, color, ticks\n"
			+ "# Use the ID finder section to get IDs, then remove the # to enable a line.\n"
			+ "# Example: make Mad Angel's bomb purple and 5x5\n"
			+ "# projectile, 4015, 2, Bomb, #B000FF\n";
	}

	@Alpha
	@ConfigItem(
		keyName = "fillColor",
		name = "Fill colour",
		description = "Fill colour of dangerous tiles",
		section = appearanceSection,
		position = 0
	)
	default Color fillColor()
	{
		return new Color(255, 0, 0, 50);
	}

	@Alpha
	@ConfigItem(
		keyName = "borderColor",
		name = "Border colour",
		description = "Outline colour of dangerous tiles",
		section = appearanceSection,
		position = 1
	)
	default Color borderColor()
	{
		return new Color(255, 0, 0, 200);
	}

	@Range(min = 1, max = 5)
	@ConfigItem(
		keyName = "borderWidth",
		name = "Border width",
		description = "Outline thickness in pixels",
		section = appearanceSection,
		position = 2
	)
	default int borderWidth()
	{
		return 1;
	}

	@ConfigItem(
		keyName = "showLabels",
		name = "Show labels",
		description = "Draw the attack's label on its centre tile",
		section = appearanceSection,
		position = 3
	)
	default boolean showLabels()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showCountdown",
		name = "Show tick countdown",
		description = "Show game ticks until a projectile lands, or until an animation hazard ends",
		section = appearanceSection,
		position = 4
	)
	default boolean showCountdown()
	{
		return true;
	}

	@ConfigItem(
		keyName = "logIds",
		name = "Log nearby IDs",
		description = "Print graphics, projectile, object and NPC animation IDs seen near you to the chatbox, "
			+ "once per ID. Use this to fill in the Custom attacks box.",
		section = debugSection,
		position = 0
	)
	default boolean logIds()
	{
		return false;
	}

	@Range(min = 1, max = 30)
	@ConfigItem(
		keyName = "logDistance",
		name = "Log distance",
		description = "Only log things within this many tiles of you",
		section = debugSection,
		position = 1
	)
	default int logDistance()
	{
		return 15;
	}
}
