package com.fairyringlabels;

import com.google.inject.Provides;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.PostMenuSort;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.fairyring.FairyRing;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Fairy Ring Labels",
	description = "Shows the destination next to fairy ring codes in right-click menus, e.g. 'CIR - Farming Guild'",
	tags = { "fairy", "ring", "favorites", "favourites", "codes", "destination", "teleport", "menu" }
)
public class FairyRingLabelsPlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private FairyRingLabelsConfig config;

	private FairyRingLabeler labeler;

	@Provides
	FairyRingLabelsConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(FairyRingLabelsConfig.class);
	}

	@Override
	protected void startUp() {
		updateLabeler();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (event.getGroup().equals(FairyRingLabelsConfig.CONFIG_GROUP))
			updateLabeler();
	}

	private void updateLabeler() {
		Map<String, String> customNames = FairyRingLabeler.parseCustomNames(config.customNames());
		boolean shortNames = config.destinationStyle() == DestinationStyle.SHORT;
		labeler = new FairyRingLabeler(code -> {
			String custom = customNames.get(code);
			if (custom != null)
				return custom;
			FairyRing ring = FairyRing.forCode(code);
			if (ring == null)
				return null;
			return shortNames ? FairyRingLabeler.shorten(ring.getDestination()) : ring.getDestination();
		});
	}

	@Subscribe
	public void onPostMenuSort(PostMenuSort event) {
		// The menu is rebuilt every frame until it's opened, so relabel it before the game sizes the menu
		if (client.isMenuOpen() || labeler == null)
			return;

		for (MenuEntry entry : client.getMenu().getMenuEntries()) {
			if (isFairyRing(entry.getTarget()))
				labelEntry(entry, false);
		}
	}

	/**
	 * Label an entry on a fairy ring, and everything in its submenu, such as the Favorites list.
	 */
	private void labelEntry(MenuEntry entry, boolean inSubMenu) {
		Menu subMenu = entry.getSubMenu();
		if (subMenu != null) {
			for (MenuEntry subEntry : subMenu.getMenuEntries())
				labelEntry(subEntry, true);
		}

		if (!inSubMenu && !config.labelLastDestination())
			return;

		String option = entry.getOption();
		String labelledOption = labeler.label(option);
		if (labelledOption != null && !labelledOption.equals(option))
			entry.setOption(labelledOption);

		// The fairy ring itself is the target of its top-level entries, so only submenu targets can hold codes
		if (inSubMenu) {
			String target = entry.getTarget();
			String labelledTarget = labeler.label(target);
			if (labelledTarget != null && !labelledTarget.equals(target))
				entry.setTarget(labelledTarget);
		}
	}

	private static boolean isFairyRing(@Nullable String target) {
		return target != null && Text.removeTags(target).toLowerCase(Locale.ROOT).contains("fairy");
	}
}
