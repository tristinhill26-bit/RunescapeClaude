package com.wikigear;

import com.google.inject.Provides;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.plugins.banktags.TagManager;
import net.runelite.client.plugins.banktags.tabs.Layout;
import net.runelite.client.plugins.banktags.tabs.LayoutManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Wiki Gear Loadouts",
	description = "Search a monster and create a bank tag with the best gear you own, using the wiki's recommended equipment",
	tags = { "wiki", "gear", "bank", "tags", "loadout", "setup", "equipment", "strategy", "bis" }
)
@PluginDependency(BankTagsPlugin.class)
public class WikiGearPlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ItemManager itemManager;

	@Inject
	private TagManager tagManager;

	@Inject
	private LayoutManager layoutManager;

	@Inject
	private BankTagsService bankTagsService;

	@Inject
	private WikiGearConfig config;

	private WikiGearPanel panel;
	private NavigationButton navigationButton;

	/**
	 * The outcome of generating one setup's bank tag
	 */
	@RequiredArgsConstructor
	public static class LoadoutResult
	{
		public final GearSetup setup;
		public final String tag;
		public final Map<GearSlot, GearPicker.Pick> picks;
	}

	@Provides
	WikiGearConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(WikiGearConfig.class);
	}

	@Override
	protected void startUp() {
		panel = injector.getInstance(WikiGearPanel.class);
		navigationButton = NavigationButton.builder()
			.tooltip("Wiki Gear Loadouts")
			.icon(createIcon())
			.priority(8)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
	}

	@Override
	protected void shutDown() {
		clientToolbar.removeNavigation(navigationButton);
		navigationButton = null;
		panel = null;
	}

	/**
	 * Create a bank tag for each setup with the best gear the player owns, then call back on the client thread.
	 */
	void createLoadouts(String monster, List<GearSetup> setups, Consumer<List<LoadoutResult>> onDone, Consumer<String> onError) {
		clientThread.invoke(() -> {
			if (client.getGameState() != GameState.LOGGED_IN) {
				onError.accept("Log in first.");
				return;
			}

			ItemContainer bank = client.getItemContainer(InventoryID.BANK);
			if (bank == null) {
				onError.accept("Open your bank once so the plugin can see what you own, then try again.");
				return;
			}

			var owned = new GearPicker.OwnedItems();
			addOwned(owned, bank);
			addOwned(owned, client.getItemContainer(InventoryID.WORN));
			addOwned(owned, client.getItemContainer(InventoryID.INV));

			var results = new ArrayList<LoadoutResult>();
			for (var setup : setups) {
				var picks = GearPicker.pick(setup, owned);
				if (!config.includeSpecial())
					picks.remove(GearSlot.SPECIAL);
				String tag = tagName(monster, setup);
				saveTag(tag, picks);
				results.add(new LoadoutResult(setup, tag, picks));
			}

			if (config.openTag() && !results.isEmpty() && isBankOpen())
				bankTagsService.openBankTag(results.get(0).tag, BankTagsService.OPTION_ALLOW_MODIFICATIONS);

			onDone.accept(results);
		});
	}

	private void addOwned(GearPicker.OwnedItems owned, ItemContainer container) {
		if (container == null)
			return;
		for (Item item : container.getItems()) {
			if (item.getId() <= 0 || item.getQuantity() <= 0)
				continue;
			var composition = itemManager.getItemComposition(item.getId());
			// Skip bank placeholders and noted items
			if (composition.getPlaceholderTemplateId() != -1 || composition.getNote() != -1)
				continue;

			String baseName = null;
			int baseId = ItemVariationMapping.map(item.getId());
			if (baseId != item.getId())
				baseName = itemManager.getItemComposition(baseId).getName();
			owned.add(item.getId(), composition.getName(), baseName);
		}
	}

	private String tagName(String monster, GearSetup setup) {
		String name = config.tagPrefix().trim() + " " + monster + " " + setup.getLabel();
		// Tags are stored comma separated
		return Text.standardize(name.replace(',', ' ').replaceAll("\\s+", " "));
	}

	private void saveTag(String tag, Map<GearSlot, GearPicker.Pick> picks) {
		// Replace any previous version of this tag
		tagManager.removeTag(tag);

		int[] positions = new int[GearSlot.BANK_COLUMNS * 5];
		Arrays.fill(positions, -1);
		for (var pick : picks.values()) {
			tagManager.addTag(pick.itemId, tag, false);
			positions[pick.slot.getLayoutPosition()] = pick.itemId;
		}
		layoutManager.saveLayout(new Layout(tag, positions));
	}

	private boolean isBankOpen() {
		var items = client.getWidget(InterfaceID.Bankmain.ITEMS);
		return items != null && !items.isHidden();
	}

	private static BufferedImage createIcon() {
		var image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		// A simple helmet
		g.setColor(ColorScheme.BRAND_ORANGE);
		g.fillArc(2, 2, 12, 14, 0, 180);
		g.fillRect(2, 8, 12, 4);
		g.setColor(new Color(30, 30, 30));
		g.setStroke(new BasicStroke(1.5f));
		g.drawLine(5, 10, 11, 10);
		g.drawLine(8, 4, 8, 9);
		g.dispose();
		return image;
	}
}
