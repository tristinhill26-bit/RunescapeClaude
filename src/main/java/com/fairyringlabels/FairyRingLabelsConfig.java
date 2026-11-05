package com.fairyringlabels;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(FairyRingLabelsConfig.CONFIG_GROUP)
public interface FairyRingLabelsConfig extends Config
{
	String CONFIG_GROUP = "fairyringlabels";

	@ConfigItem(
		keyName = "destinationStyle",
		name = "Destination names",
		description = "Use short names, like 'Farming Guild', or RuneLite's full descriptions, like 'North-east of the Farming Guild'.",
		position = 0
	)
	default DestinationStyle destinationStyle() {
		return DestinationStyle.SHORT;
	}

	@ConfigItem(
		keyName = "labelLastDestination",
		name = "Label last destination",
		description = "Also add the destination to the 'Last-destination (CIR)' option.",
		position = 1
	)
	default boolean labelLastDestination() {
		return true;
	}

	@ConfigItem(
		keyName = "customNames",
		name = "Custom names",
		description = "Your own names for codes, one per line or comma separated, e.g. CIR=Konar, DKR=GE.",
		position = 2
	)
	default String customNames() {
		return "";
	}
}
