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
		keyName = "includeInventory",
		name = "Include inventory",
		description = "Also add the wiki's recommended inventory, laid out to the right of the gear.",
		position = 2
	)
	default boolean includeInventory() {
		return true;
	}

	@ConfigItem(
		keyName = "substituteInventory",
		name = "Substitute inventory items",
		description = "Replace recommended inventory items you don't own with the closest thing you do:<br>" +
			"your best food for food, or a potion that does the same job, like a Prayer potion for a Super restore.",
		position = 3
	)
	default boolean substituteInventory() {
		return true;
	}

	@ConfigItem(
		keyName = "fillEmptySlots",
		name = "Fill empty slots",
		description = "When the wiki recommends nothing you own for a slot, use your best item for the setup's combat style,<br>" +
			"judged by its stats. For example an Amulet of glory when the wiki doesn't list a necklace.",
		position = 4
	)
	default boolean fillEmptySlots() {
		return true;
	}

	@ConfigItem(
		keyName = "alwaysAllowBlighted",
		name = "Always allow blighted items",
		description = "Blighted items only work in the Wilderness, so they're left out unless the monster or activity<br>" +
			"is in the Wilderness. Turn this on to always allow them.",
		position = 5
	)
	default boolean alwaysAllowBlighted() {
		return false;
	}

	@ConfigItem(
		keyName = "addTab",
		name = "Add as bank tab",
		description = "Add each new tag to your bank's tag tabs, so you don't need to import it.<br>" +
			"The tab appears the next time you open your bank.",
		position = 6
	)
	default boolean addTab() {
		return true;
	}

	@ConfigItem(
		keyName = "openTag",
		name = "Open tag after creating",
		description = "Open the new tag in your bank straight away, if your bank is open.",
		position = 7
	)
	default boolean openTag() {
		return true;
	}

	@ConfigItem(
		keyName = "optimiseDps",
		name = "Highest DPS",
		description = "Choose the gear with the highest DPS against the monster, using the OSRS Wiki DPS calculator's maths,<br>" +
			"your levels, your best potions and the best prayers you've unlocked. Turn off to follow the wiki's order.",
		position = 8
	)
	default boolean optimiseDps() {
		return true;
	}

	@ConfigItem(
		keyName = "assumeAnyPotion",
		name = "Assume potions you don't own",
		description = "For Highest DPS, assume Super combat, Ranging and Magic potions even if you don't own them.<br>" +
			"Otherwise only potions in your bank, inventory or potion store count.",
		position = 9
	)
	default boolean assumeAnyPotion() {
		return false;
	}

	@ConfigItem(
		keyName = "combineStyles",
		name = "Combine ticked styles",
		description = "When you tick more than one style, make a single tag that wears the first one and puts the<br>" +
			"others' gear in the inventory as switches, like Magic and Ranged at Zulrah.",
		position = 10
	)
	default boolean combineStyles() {
		return false;
	}
}
