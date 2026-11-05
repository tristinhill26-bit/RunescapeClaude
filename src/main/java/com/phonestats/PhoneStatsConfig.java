package com.phonestats;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(PhoneStatsConfig.CONFIG_GROUP)
public interface PhoneStatsConfig extends Config
{
	String CONFIG_GROUP = "phonestats";

	@Range(min = 1024, max = 65535)
	@ConfigItem(
		keyName = "port",
		name = "Port",
		description = "The port the phone app connects to. Enter the same number in the app.",
		position = 0
	)
	default int port()
	{
		return 8765;
	}

	@ConfigItem(
		keyName = "showAddressOnLogin",
		name = "Show address on login",
		description = "Print the address to type into the phone app in the chat box when you log in.",
		position = 1
	)
	default boolean showAddressOnLogin()
	{
		return true;
	}

	@ConfigSection(
		name = "Drop alerts",
		description = "Ping your phone when you get a good drop",
		position = 10
	)
	String dropSection = "drops";

	@ConfigItem(
		keyName = "dropAlerts",
		name = "Send drops to phone",
		description = "Ping the phone app when an NPC or player drops something worth it.",
		position = 11,
		section = dropSection
	)
	default boolean dropAlerts()
	{
		return true;
	}

	@Range(min = 0)
	@ConfigItem(
		keyName = "dropMinValue",
		name = "Minimum value",
		description = "Ping for an item stack worth at least this many coins (Grand Exchange price). 0 pings for everything.",
		position = 12,
		section = dropSection
	)
	default int dropMinValue()
	{
		return 10000;
	}

	@ConfigItem(
		keyName = "dropUntradeables",
		name = "Untradeables",
		description = "Also ping for untradeable items, like pets, clue scrolls and boss jars.",
		position = 13,
		section = dropSection
	)
	default boolean dropUntradeables()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dropIgnore",
		name = "Ignore items",
		description = "Item names that never ping, separated by commas, e.g. Ensouled dragon head, Brimstone key",
		position = 14,
		section = dropSection
	)
	default String dropIgnore()
	{
		return "";
	}
}
