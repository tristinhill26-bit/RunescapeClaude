package rs117.hd.interaction;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.AgilityShortcut;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;

/**
 * Finds things worth lighting up around the player, for the scan pulse and shortcut glint.
 */
@Slf4j
@Singleton
class TargetScanner {
	private static final Set<String> EXIT_ACTIONS = new HashSet<>(Arrays.asList(
		"climb-up", "climb-down", "climb", "enter", "exit", "go-up", "go-down", "climb-into", "leave", "use-portal"
	));

	@RequiredArgsConstructor
	enum Category {
		RESOURCE(null, 1),
		SHORTCUT(new Color(110, 255, 130), 1),
		SHORTCUT_ABOVE_LEVEL(new Color(255, 85, 60), .8f),
		EXIT(new Color(120, 190, 255), 1),
		OBJECT_MARKER(new Color(255, 170, 60), 1),
		GROUND_ITEM(new Color(255, 250, 235), .8f),
		EXAMINABLE(new Color(255, 240, 210), .3f);

		/**
		 * The light colour, or null to use the marker colour
		 */
		@Nullable
		final Color color;
		final float strength;
	}

	@RequiredArgsConstructor
	static class Hit {
		final MarkerTarget target;
		final Category category;
		final int distance;
	}

	@Inject
	private Client client;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private InteractionLightsConfig config;

	private final Map<Integer, List<AgilityShortcut>> shortcutsByObjectId = new HashMap<>();

	TargetScanner() {
		for (var shortcut : AgilityShortcut.values())
			for (int id : shortcut.getObstacleIds())
				shortcutsByObjectId.computeIfAbsent(id, k -> new ArrayList<>()).add(shortcut);
	}

	/**
	 * Collect things to light up within the given radius around the center, sorted by priority and then distance.
	 *
	 * @param resourceName name of the player's last interaction target, used for "resources like your target"
	 */
	List<Hit> scan(LocalPoint center, int radius, @Nullable String resourceName, int maxHits) {
		var hits = new ArrayList<Hit>();
		var wv = client.getTopLevelWorldView();
		int plane = wv.getPlane();
		var tiles = wv.getScene().getTiles()[plane];
		boolean everything = config.scanLights() == ScanLightMode.EVERYTHING_EXAMINABLE;
		String resource = resourceName == null || resourceName.isEmpty() ? null : resourceName.toLowerCase(Locale.ROOT);
		var markedObjects = config.scanObjectMarkers() ? new MarkedObjects() : null;
		var highlightedItems = config.scanGroundItemsHighlights() ? getHighlightedItems() : null;

		int cx = center.getSceneX();
		int cy = center.getSceneY();
		for (int x = Math.max(0, cx - radius); x <= Math.min(tiles.length - 1, cx + radius); x++) {
			for (int y = Math.max(0, cy - radius); y <= Math.min(tiles[x].length - 1, cy + radius); y++) {
				var tile = tiles[x][y];
				if (tile == null)
					continue;

				int distance = Math.max(Math.abs(x - cx), Math.abs(y - cy));
				for (var object : tile.getGameObjects()) {
					// Skip actors, and only visit large objects once, from their south-west tile
					if (object == null || object.getRenderable() instanceof Actor)
						continue;
					var min = object.getSceneMinLocation();
					if (min.getX() != x || min.getY() != y)
						continue;
					checkObject(hits, object, distance, resource, everything, markedObjects);
				}
				checkObject(hits, tile.getWallObject(), distance, resource, everything, markedObjects);
				checkObject(hits, tile.getDecorativeObject(), distance, resource, everything, markedObjects);
				checkObject(hits, tile.getGroundObject(), distance, resource, everything, markedObjects);

				var items = tile.getGroundItems();
				if (items != null) {
					for (var item : items) {
						var category = classifyItem(item, highlightedItems, everything);
						if (category == null)
							continue;
						String name = itemManager.getItemComposition(item.getId()).getName();
						hits.add(new Hit(MarkerTarget.groundItem(tile.getLocalLocation(), plane, item.getId(), name), category, distance));
					}
				}
			}
		}

		for (var npc : wv.npcs()) {
			var lp = npc.getLocalLocation();
			if (lp == null)
				continue;
			int distance = Math.max(Math.abs(lp.getSceneX() - cx), Math.abs(lp.getSceneY() - cy));
			if (distance > radius)
				continue;
			String name = npc.getName();
			if (name == null || name.equals("null"))
				continue;
			Category category = null;
			if (config.scanResourcesLikeTarget() && resource != null && Text.removeTags(name).toLowerCase(Locale.ROOT).equals(resource)) {
				category = Category.RESOURCE;
			} else if (everything) {
				category = Category.EXAMINABLE;
			}
			if (category != null)
				hits.add(new Hit(MarkerTarget.actor(npc, plane), category, distance));
		}

		hits.sort((a, b) -> a.category != b.category ?
			Integer.compare(a.category.ordinal(), b.category.ordinal()) :
			Integer.compare(a.distance, b.distance));
		if (hits.size() > maxHits)
			hits.subList(maxHits, hits.size()).clear();
		return hits;
	}

	/**
	 * Find agility shortcuts the player can use within the given radius, for the shortcut glint.
	 */
	List<MarkerTarget> findUsableShortcuts(LocalPoint center, int radius) {
		var result = new ArrayList<MarkerTarget>();
		var wv = client.getTopLevelWorldView();
		var tiles = wv.getScene().getTiles()[wv.getPlane()];
		int agilityLevel = client.getBoostedSkillLevel(Skill.AGILITY);
		int cx = center.getSceneX();
		int cy = center.getSceneY();
		for (int x = Math.max(0, cx - radius); x <= Math.min(tiles.length - 1, cx + radius); x++) {
			for (int y = Math.max(0, cy - radius); y <= Math.min(tiles[x].length - 1, cy + radius); y++) {
				var tile = tiles[x][y];
				if (tile == null)
					continue;
				for (var object : tile.getGameObjects()) {
					if (object == null)
						continue;
					var min = object.getSceneMinLocation();
					if (min.getX() != x || min.getY() != y)
						continue;
					addIfUsableShortcut(result, object, agilityLevel);
				}
				addIfUsableShortcut(result, tile.getWallObject(), agilityLevel);
				addIfUsableShortcut(result, tile.getDecorativeObject(), agilityLevel);
				addIfUsableShortcut(result, tile.getGroundObject(), agilityLevel);
			}
		}
		return result;
	}

	private void addIfUsableShortcut(List<MarkerTarget> result, @Nullable TileObject object, int agilityLevel) {
		if (object == null)
			return;
		var shortcut = getShortcut(object);
		if (shortcut != null && shortcut.getLevel() <= agilityLevel)
			result.add(MarkerTarget.object(object, shortcut.getDescription()));
	}

	@Nullable
	String getObjectName(TileObject object) {
		var composition = getComposition(object.getId());
		if (composition == null)
			return null;
		String name = composition.getName();
		return name == null || name.equals("null") ? null : name;
	}

	@Nullable
	private ObjectComposition getComposition(int id) {
		var composition = client.getObjectDefinition(id);
		if (composition != null && composition.getImpostorIds() != null) {
			var impostor = composition.getImpostor();
			if (impostor != null)
				composition = impostor;
		}
		return composition;
	}

	@Nullable
	private AgilityShortcut getShortcut(TileObject object) {
		var candidates = shortcutsByObjectId.get(object.getId());
		if (candidates == null)
			return null;
		// Several shortcuts can share obstacle IDs, so pick the closest one, like the Agility plugin does
		var worldLocation = object.getWorldLocation();
		AgilityShortcut closest = null;
		int closestDistance = Integer.MAX_VALUE;
		for (var shortcut : candidates) {
			if (!shortcut.matches(client, object))
				continue;
			var location = shortcut.getWorldLocation();
			if (location == null)
				return shortcut;
			int distance = location.distanceTo2D(worldLocation);
			if (distance < closestDistance) {
				closest = shortcut;
				closestDistance = distance;
			}
		}
		return closest;
	}

	private void checkObject(
		List<Hit> hits,
		@Nullable TileObject object,
		int distance,
		@Nullable String resource,
		boolean everything,
		@Nullable MarkedObjects markedObjects
	) {
		if (object == null)
			return;

		var composition = getComposition(object.getId());
		if (composition == null)
			return;
		String name = composition.getName();
		boolean named = name != null && !name.equals("null");

		Category category = null;
		var shortcut = getShortcut(object);
		if (shortcut != null && config.scanAgilityShortcuts()) {
			if (shortcut.getLevel() <= client.getBoostedSkillLevel(Skill.AGILITY)) {
				category = Category.SHORTCUT;
			} else if (config.scanShortcutsAboveLevel()) {
				category = Category.SHORTCUT_ABOVE_LEVEL;
			}
		}

		if (category == null && named && resource != null && config.scanResourcesLikeTarget() &&
			name.toLowerCase(Locale.ROOT).equals(resource))
			category = Category.RESOURCE;

		if (category == null && markedObjects != null && markedObjects.contains(object))
			category = Category.OBJECT_MARKER;

		if (category == null && named && config.scanExits() && isExit(composition))
			category = Category.EXIT;

		if (category == null && named && everything)
			category = Category.EXAMINABLE;

		if (category != null)
			hits.add(new Hit(MarkerTarget.object(object, named ? name : ""), category, distance));
	}

	private static boolean isExit(ObjectComposition composition) {
		var actions = composition.getActions();
		if (actions == null)
			return false;
		for (var action : actions)
			if (action != null && EXIT_ACTIONS.contains(action.toLowerCase(Locale.ROOT)))
				return true;
		return false;
	}

	@Nullable
	private Category classifyItem(TileItem item, @Nullable List<String> highlightedItems, boolean everything) {
		if (config.scanOwnItems() && item.getOwnership() == TileItem.OWNERSHIP_SELF)
			return Category.GROUND_ITEM;

		if (config.scanValuableItems()) {
			long value = itemManager.getItemPrice(item.getId()) * (long) item.getQuantity();
			if (value > 0 && value >= config.scanItemValue())
				return Category.GROUND_ITEM;
		}

		if (highlightedItems != null && !highlightedItems.isEmpty()) {
			String name = itemManager.getItemComposition(item.getId()).getName();
			for (var pattern : highlightedItems)
				if (WildcardMatcher.matches(pattern, name))
					return Category.GROUND_ITEM;
		}

		return everything ? Category.EXAMINABLE : null;
	}

	@Nullable
	private List<String> getHighlightedItems() {
		String list = configManager.getConfiguration("grounditems", "highlightedItems");
		return list == null ? null : Text.fromCSV(list);
	}

	/**
	 * Objects marked using RuneLite's Object Markers plugin, parsed lazily per region
	 */
	private class MarkedObjects {
		private final Map<Integer, Set<Long>> regions = new HashMap<>();

		boolean contains(TileObject object) {
			var lp = object.getLocalLocation();
			if (lp == null)
				return false;
			var wp = WorldPoint.fromLocalInstance(client, lp, object.getPlane());
			if (wp == null)
				return false;
			var points = regions.computeIfAbsent(wp.getRegionID(), this::load);
			return points.contains(pack(object.getId(), wp.getRegionX(), wp.getRegionY(), wp.getPlane()));
		}

		private Set<Long> load(int regionId) {
			var points = new HashSet<Long>();
			String json = configManager.getConfiguration("objectindicators", "region_" + regionId);
			if (json == null || json.isEmpty())
				return points;
			try {
				for (JsonElement element : new JsonParser().parse(json).getAsJsonArray()) {
					var point = element.getAsJsonObject();
					points.add(pack(
						point.get("id").getAsInt(),
						point.get("regionX").getAsInt(),
						point.get("regionY").getAsInt(),
						point.get("z").getAsInt()
					));
				}
			} catch (Exception ex) {
				log.debug("Unable to parse object markers for region {}", regionId, ex);
			}
			return points;
		}

		private long pack(int id, int regionX, int regionY, int plane) {
			return (long) id << 20 | (long) regionX << 12 | (long) regionY << 4 | plane;
		}
	}
}
