package com.phonestats;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
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
}
