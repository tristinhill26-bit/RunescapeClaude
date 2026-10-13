package com.wikigear;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(WikiGearConfig.CONFIG_GROUP)
public interface WikiGearConfig extends Config
{
	String CONFIG_GROUP = "wikigear";

	@ConfigItem(
		keyName = "tagPrefix",
		name = "Tag prefix",
		description = "Start of the bank tag names this plugin creates, e.g. 'gear' makes 'gear vorkath ranged'.<br>" +
			"Tags with this prefix are replaced when you regenerate them.",
		position = 0
	)
	default String tagPrefix() {
		return "gear";
	}

	@ConfigItem(
		keyName = "includeSpecial",
		name = "Include special attack weapon",
		description = "Also add the wiki's recommended special attack weapon to the tag.",
		position = 1
	)
	default boolean includeSpecial() {
		return true;
	}

	@ConfigItem(
		keyName = "openTag",
		name = "Open tag after creating",
		description = "Open the new tag in your bank straight away, if your bank is open.",
		position = 2
	)
	default boolean openTag() {
		return true;
	}
}
