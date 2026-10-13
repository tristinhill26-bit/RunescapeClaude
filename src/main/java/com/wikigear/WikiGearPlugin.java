package com.wikigear;

import com.google.inject.Provides;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
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
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.plugins.banktags.TagManager;
import net.runelite.client.plugins.banktags.tabs.Layout;
import net.runelite.client.plugins.banktags.tabs.LayoutManager;
import net.runelite.client.plugins.itemstats.Effect;
import net.runelite.client.plugins.itemstats.FoodBase;
import net.runelite.client.plugins.itemstats.ItemStatChangesService;
import net.runelite.client.plugins.itemstats.ItemStatPlugin;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Wiki Gear Loadouts",
	description = "Search a monster or activity and create a bank tag with the best gear you own, using the wiki's recommended equipment",
	tags = { "wiki", "gear", "bank", "tags", "loadout", "setup", "equipment", "strategy", "bis", "raids", "minigames", "barrows" }
)
@PluginDependency(BankTagsPlugin.class)
@PluginDependency(ItemStatPlugin.class)
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
	private ItemStatChangesService itemStatChangesService;

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
		public final GearPicker.Result result;
		/**
		 * Text for Bank Tags' "Import tag tab", which creates the tab with this layout
		 */
		public final String importText;
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
			var heals = new HashMap<Integer, Integer>();
			var substitutes = config.substituteInventory() ? new InventorySubstitutes(
				name -> healByName(name, heals),
				item -> healOf(item.itemId, heals)
			) : null;
			addOwned(owned, bank);
			addOwned(owned, client.getItemContainer(InventoryID.WORN));
			addOwned(owned, client.getItemContainer(InventoryID.INV));

			var results = new ArrayList<LoadoutResult>();
			for (var setup : setups) {
				var result = GearPicker.pick(setup, owned, config.fillEmptySlots(), substitutes);
				if (!config.includeSpecial())
					result.picks.remove(GearSlot.SPECIAL);
				if (!config.includeInventory()) {
					Arrays.fill(result.inventory, -1);
					result.missingInventory.clear();
					result.substitutions.clear();
				}
				String tag = tagName(monster, setup);
				int[] layout = TagExport.layout(result);
				saveTag(tag, result, layout);
				String importText = TagExport.toImportText(tag, TagExport.iconItem(result, layout), layout);
				results.add(new LoadoutResult(setup, tag, result, importText));
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
			owned.add(item.getId(), composition.getName(), baseName, getBonuses(item.getId()), item.getQuantity());
		}
	}

	/**
	 * How much a food heals, using the Item Stats plugin's data, or 0 for anything that isn't food
	 */
	private int healOf(int itemId, Map<Integer, Integer> cache) {
		return cache.computeIfAbsent(itemId, id -> {
			Effect effect = itemStatChangesService.getItemStatChanges(id);
			return effect instanceof FoodBase ? Math.max(0, ((FoodBase) effect).heals(client)) : 0;
		});
	}

	/**
	 * How much a food heals, by name. Only tradeable items can be found by name, which covers nearly all food.
	 */
	private int healByName(String name, Map<Integer, Integer> cache) {
		for (var item : itemManager.search(name))
			if (item.getName().equalsIgnoreCase(name))
				return healOf(item.getId(), cache);
		return 0;
	}

	@Nullable
	private ItemBonuses getBonuses(int itemId) {
		ItemStats stats = itemManager.getItemStats(itemId);
		if (stats == null || !stats.isEquipable() || stats.getEquipment() == null)
			return null;
		ItemEquipmentStats e = stats.getEquipment();
		return ItemBonuses.builder()
			.slot(e.getSlot())
			.twoHanded(e.isTwoHanded())
			.stab(e.getAstab())
			.slash(e.getAslash())
			.crush(e.getAcrush())
			.magic(e.getAmagic())
			.ranged(e.getArange())
			.strength(e.getStr())
			.rangedStrength(e.getRstr())
			.magicDamage(e.getMdmg())
			.prayer(e.getPrayer())
			.defence(e.getDstab() + e.getDslash() + e.getDcrush() + e.getDmagic() + e.getDrange())
			.build();
	}

	private String tagName(String monster, GearSetup setup) {
		String name = config.tagPrefix().trim() + " " + monster + " " + setup.getLabel();
		// Use a name that survives Bank Tags' import unchanged
		return Text.standardize(TagExport.cleanTagName(name));
	}

	private void saveTag(String tag, GearPicker.Result result, int[] layout) {
		// Replace any previous version of this tag
		tagManager.removeTag(tag);
		for (int itemId : layout)
			if (itemId != -1)
				tagManager.addTag(itemId, tag, false);
		layoutManager.saveLayout(new Layout(tag, layout));
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
