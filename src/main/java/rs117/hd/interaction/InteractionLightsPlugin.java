package rs117.hd.interaction;

import com.google.inject.Provides;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.*;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import rs117.hd.HdPlugin;
import rs117.hd.scene.LightManager;
import rs117.hd.scene.SceneContext;
import rs117.hd.scene.lights.ExternalLightProvider;
import rs117.hd.scene.lights.Light;

import static net.runelite.api.Perspective.*;

@Slf4j
@PluginDescriptor(
	name = "117 HD Interaction Lights",
	description = "Light beams, mouse-over lights, scan pulses and shortcut glints, rendered as real lights by 117 HD Enhanced",
	tags = { "hd", "lights", "click", "marker", "beam", "mouse", "hover", "scan", "highlight", "shortcut", "agility" }
)
public class InteractionLightsPlugin extends Plugin implements ExternalLightProvider {
	private static final float FADE_IN = .15f;
	private static final float FADE_OUT = .4f;
	private static final float FLASH_DURATION = .35f;
	private static final float FLASH_STRENGTH = 2.5f;

	private static final float MARKER_STRENGTH = 8;
	private static final float MARKER_RADIUS = 420;
	private static final float HOVER_STRENGTH = 10;
	private static final float HOVER_RADIUS = 360;
	private static final float HINT_STRENGTH = 10;
	private static final float HINT_RADIUS = 500;
	private static final float SCAN_STRENGTH = 9;
	private static final float SCAN_RADIUS = 320;
	private static final float GLINT_STRENGTH = 6;
	private static final float GLINT_RADIUS = 260;
	private static final Color HINT_ARROW_COLOR = new Color(255, 205, 70);
	private static final Color GLINT_COLOR = new Color(170, 255, 190);

	private static final int MAX_SCAN_LIGHTS = 120;
	private static final int GLINT_SEARCH_RADIUS = 20;
	private static final int GLINT_REFRESH_TICKS = 5;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private InteractionLightsConfig config;

	@Inject
	private InteractionLightsOverlay overlay;

	@Inject
	private TargetScanner scanner;

	@Inject
	private HighVisibilityMode highVisibilityMode;

	@Nullable
	private LightManager attachedLightManager;

	private final PathFinder pathFinder = new PathFinder();

	/**
	 * Time in seconds of the current frame
	 */
	@Getter
	private double now;

	@Getter
	@Nullable
	private LitTarget clickMarker;
	@Getter
	private boolean clickMarkerIsWalk;
	@Getter
	private double clickTime = Double.NEGATIVE_INFINITY;
	@Getter
	private final List<LitTarget> fadingMarkers = new ArrayList<>();

	@Getter
	@Nullable
	private LitTarget hover;
	private final List<LitTarget> fadingHovers = new ArrayList<>();

	@Getter
	@Nullable
	private LitTarget hintArrow;
	private final List<LitTarget> fadingHintArrows = new ArrayList<>();

	@Getter
	private List<Integer> path = Collections.emptyList();
	private int pathStart = -1;
	private int pathEnd = -1;

	@Getter
	private final List<LitTarget> scanLights = new ArrayList<>();
	@Getter
	@Nullable
	private LocalPoint scanCenter;
	@Getter
	private int scanPlane;
	@Getter
	private double scanStartTime = Double.NEGATIVE_INFINITY;

	private final List<LitTarget> glints = new ArrayList<>();
	private int ticksUntilGlintRefresh;

	/**
	 * Name of the last object or NPC the player interacted with, used to find similar resources when scanning
	 */
	@Nullable
	private String lastInteractedName;

	private final HotkeyListener scanHotkeyListener = new HotkeyListener(() -> config.scanKey()) {
		@Override
		public void hotkeyPressed() {
			clientThread.invoke(InteractionLightsPlugin.this::startScan);
		}
	};

	private final HotkeyListener highVisibilityHotkeyListener = new HotkeyListener(() -> config.highVisibilityKey()) {
		@Override
		public void hotkeyPressed() {
			highVisibilityMode.toggle();
		}
	};

	@Provides
	InteractionLightsConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(InteractionLightsConfig.class);
	}

	@Override
	protected void startUp() {
		now = System.nanoTime() / 1e9;
		overlayManager.add(overlay);
		keyManager.registerKeyListener(scanHotkeyListener);
		keyManager.registerKeyListener(highVisibilityHotkeyListener);
		clientThread.invoke(this::attachToHd);
	}

	@Override
	protected void shutDown() {
		overlayManager.remove(overlay);
		keyManager.unregisterKeyListener(scanHotkeyListener);
		keyManager.unregisterKeyListener(highVisibilityHotkeyListener);
		// Don't leave the player stuck with the high visibility settings
		if (highVisibilityMode.isActive())
			highVisibilityMode.restore();
		clientThread.invoke(() -> {
			if (attachedLightManager != null)
				attachedLightManager.setExternalLightProvider(null);
			attachedLightManager = null;
			clearAll();
		});
	}

	/**
	 * Hook into the light manager of 117 HD Enhanced, if it's installed, so markers are rendered as real lights.
	 * Without it, only the overlays are drawn.
	 */
	private void attachToHd() {
		LightManager lightManager = null;
		for (var plugin : pluginManager.getPlugins()) {
			if (plugin instanceof HdPlugin && plugin.getInjector() != null) {
				lightManager = plugin.getInjector().getInstance(LightManager.class);
				break;
			}
		}

		if (lightManager == attachedLightManager)
			return;
		if (attachedLightManager != null)
			attachedLightManager.setExternalLightProvider(null);
		attachedLightManager = lightManager;
		if (lightManager != null)
			lightManager.setExternalLightProvider(this);
	}

	@Subscribe
	public void onPluginChanged(PluginChanged event) {
		if (event.getPlugin() instanceof HdPlugin)
			clientThread.invoke(this::attachToHd);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		// Apply changes to the high visibility preset straight away while it's on
		if (event.getGroup().equals(InteractionLightsConfig.CONFIG_GROUP) &&
			event.getKey().startsWith("highVisibility") &&
			!event.getKey().equals("highVisibilityKey") &&
			!event.getKey().equals("highVisibilityActive") &&
			!event.getKey().equals("highVisibilitySaved") &&
			highVisibilityMode.isActive())
			highVisibilityMode.apply();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event) {
		// Scene coordinates change when a new region loads
		if (event.getGameState() != GameState.LOGGED_IN)
			clearAll();
	}

	private void clearAll() {
		clickMarker = null;
		hover = null;
		hintArrow = null;
		fadingMarkers.clear();
		fadingHovers.clear();
		fadingHintArrows.clear();
		scanLights.clear();
		scanCenter = null;
		glints.clear();
		path = Collections.emptyList();
		pathStart = pathEnd = -1;
		ticksUntilGlintRefresh = 0;
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		var entry = event.getMenuEntry();
		var type = entry.getType();
		if (isExamine(type))
			return;

		var target = resolveTarget(entry);
		if (target == null)
			return;

		if (target.kind == MarkerTarget.Kind.OBJECT || target.kind == MarkerTarget.Kind.ACTOR) {
			if (!target.name.isEmpty())
				lastInteractedName = target.name;
		}

		if (!config.clickMarker())
			return;

		now = System.nanoTime() / 1e9;
		clickTime = now;
		if (clickMarker != null) {
			clickMarker.fadeOut(now);
			fadingMarkers.add(clickMarker);
		}
		clickMarker = new LitTarget(target, now, FADE_IN, FADE_OUT);
		clickMarkerIsWalk = type == MenuAction.WALK;
		pathStart = pathEnd = -1;
		path = Collections.emptyList();
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (--ticksUntilGlintRefresh <= 0) {
			ticksUntilGlintRefresh = GLINT_REFRESH_TICKS;
			refreshGlints();
		}
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event) {
		if (client.getGameState() != GameState.LOGGED_IN)
			return;

		now = System.nanoTime() / 1e9;
		updateClickMarker();
		updateHover();
		updateHintArrow();
		updateScan();
		updateGlints();
	}

	private void updateClickMarker() {
		var player = client.getLocalPlayer();
		Color color = config.markerColor();
		float lightScale = config.markerLight() / 100f;

		if (clickMarker != null) {
			var marker = clickMarker;
			var target = marker.target;
			boolean exists = target.update(client);
			var destination = client.getLocalDestinationLocation();

			// The game may adjust where a walk click leads, e.g. when clicking an unreachable tile
			if (clickMarkerIsWalk && destination != null)
				target.location = target.minTile = destination;

			if (!config.clickMarker()) {
				marker.fadeOut(now);
			} else if (!exists && !config.keepAfterTargetGone()) {
				marker.fadeOut(now);
			} else if (config.forgetAfter() > 0 && now - marker.startTime > config.forgetAfter()) {
				marker.fadeOut(now);
			} else if (clickMarkerIsWalk && config.clearOnArrival() && destination == null && player != null) {
				// Without a destination, the player has either arrived, or stopped short of an unreachable tile
				double elapsed = now - marker.startTime;
				boolean arrived = target.tileDistanceTo(player.getLocalLocation()) == 0;
				if (arrived ? elapsed > .3 : elapsed > 1.2)
					marker.fadeOut(now);
			}

			if (marker.isFadingOut(now)) {
				fadingMarkers.add(marker);
				clickMarker = null;
			} else {
				updatePath(player, destination);
			}
		}

		if (clickMarker == null && !path.isEmpty()) {
			path = Collections.emptyList();
			pathStart = pathEnd = -1;
		}

		float flash = 1;
		if (config.flashOnClick()) {
			float t = (float) (now - clickTime) / FLASH_DURATION;
			if (t >= 0 && t < 1)
				flash += (FLASH_STRENGTH - 1) * (1 - t) * (1 - t);
		}

		if (clickMarker != null)
			updateLight(clickMarker, color, MARKER_STRENGTH * lightScale * flash, MARKER_RADIUS);
		fadingMarkers.removeIf(marker -> marker.isDone(now));
		for (var marker : fadingMarkers)
			updateLight(marker, color, MARKER_STRENGTH * lightScale, MARKER_RADIUS);
	}

	private void updatePath(@Nullable Player player, @Nullable LocalPoint destination) {
		if (!config.pathDots() || player == null || destination == null) {
			if (!path.isEmpty())
				path = Collections.emptyList();
			pathStart = pathEnd = -1;
			return;
		}

		var start = player.getLocalLocation();
		int packedStart = start.getSceneX() << 16 | start.getSceneY();
		int packedEnd = destination.getSceneX() << 16 | destination.getSceneY();
		if (packedStart == pathStart && packedEnd == pathEnd)
			return;
		pathStart = packedStart;
		pathEnd = packedEnd;

		var wv = client.getTopLevelWorldView();
		var collisionMaps = wv.getCollisionMaps();
		if (collisionMaps == null || wv.getPlane() >= collisionMaps.length || collisionMaps[wv.getPlane()] == null) {
			path = Collections.emptyList();
			return;
		}
		path = pathFinder.findPath(
			collisionMaps[wv.getPlane()],
			start.getSceneX(), start.getSceneY(),
			destination.getSceneX(), destination.getSceneY()
		);
	}

	private void updateHover() {
		boolean enabled = config.markOnMouseOver() || config.mouseOverLight() > 0;
		MarkerTarget target = null;
		if (enabled) {
			if (client.isMenuOpen()) {
				// Keep the hovered target while a menu is open
				target = hover == null ? null : hover.target;
			} else {
				var entries = client.getMenu().getMenuEntries();
				if (entries.length > 0)
					target = resolveTarget(entries[entries.length - 1]);
			}
		}

		if (hover != null && (target == null || !target.isSameAs(hover.target) || !hover.target.update(client))) {
			hover.fadeOut(now);
			fadingHovers.add(hover);
			hover = null;
		}
		if (hover == null && target != null && target.update(client))
			hover = new LitTarget(target, now, FADE_IN, FADE_OUT);

		Color color = config.mouseOverColor();
		float strength = HOVER_STRENGTH * config.mouseOverLight() / 100f;
		if (hover != null)
			updateLight(hover, color, strength, HOVER_RADIUS);
		fadingHovers.removeIf(lit -> lit.isDone(now));
		for (var lit : fadingHovers)
			updateLight(lit, color, strength, HOVER_RADIUS);
	}

	private void updateHintArrow() {
		MarkerTarget target = null;
		if (config.hintArrowMode() != HintArrowMode.OFF) {
			var wv = client.getTopLevelWorldView();
			switch (client.getHintArrowType()) {
				case HintArrowType.NPC:
					var npc = client.getHintArrowNpc();
					if (npc != null)
						target = MarkerTarget.actor(npc, wv.getPlane());
					break;
				case HintArrowType.PLAYER:
					var player = client.getHintArrowPlayer();
					if (player != null)
						target = MarkerTarget.actor(player, wv.getPlane());
					break;
				case HintArrowType.COORDINATE:
					var point = client.getHintArrowPoint();
					if (point != null && point.getPlane() == wv.getPlane()) {
						var lp = LocalPoint.fromWorld(wv, point);
						if (lp != null)
							target = MarkerTarget.tile(lp, point.getPlane());
					}
					break;
			}
		}

		if (hintArrow != null && (target == null || !target.isSameAs(hintArrow.target) || !hintArrow.target.update(client))) {
			hintArrow.fadeOut(now);
			fadingHintArrows.add(hintArrow);
			hintArrow = null;
		}
		if (hintArrow == null && target != null)
			hintArrow = new LitTarget(target, now, FADE_IN, FADE_OUT);

		float strength = config.hintArrowMode() == HintArrowMode.BEAM_AND_LIGHT ? HINT_STRENGTH : 0;
		if (hintArrow != null)
			updateLight(hintArrow, HINT_ARROW_COLOR, strength, HINT_RADIUS);
		fadingHintArrows.removeIf(lit -> lit.isDone(now));
		for (var lit : fadingHintArrows)
			updateLight(lit, HINT_ARROW_COLOR, strength, HINT_RADIUS);
	}

	private void startScan() {
		var player = client.getLocalPlayer();
		if (client.getGameState() != GameState.LOGGED_IN || player == null)
			return;

		now = System.nanoTime() / 1e9;
		int radius = config.scanRadius();
		float sweepTime = config.scanSweepTime() / 1000f;
		float duration = config.scanDuration();

		scanCenter = player.getLocalLocation();
		scanPlane = client.getTopLevelWorldView().getPlane();
		scanStartTime = now;
		scanLights.clear();

		for (var hit : scanner.scan(scanCenter, radius, lastInteractedName, MAX_SCAN_LIGHTS)) {
			// Light things up as the pulse passes over them
			double revealTime = now + sweepTime * hit.distance / radius;
			var lit = new LitTarget(hit.target, revealTime, .12f, .8f);
			lit.endTime = revealTime + duration;
			lit.baseStrength = hit.category.strength;
			lit.setColor(hit.category.color == null ? config.markerColor() : hit.category.color);
			scanLights.add(lit);
		}
	}

	private void updateScan() {
		float strength = SCAN_STRENGTH * config.scanLightStrength() / 100f;
		scanLights.removeIf(lit -> lit.isDone(now) || !lit.target.update(client));
		for (var lit : scanLights) {
			var target = lit.target;
			float radius = Math.max(SCAN_RADIUS, Math.max(target.sizeX, target.sizeY) * LOCAL_TILE_SIZE + target.height * .5f);
			updateLight(lit, lit.awtColor, strength * lit.baseStrength, radius);
		}

		if (scanLights.isEmpty() && now - scanStartTime > config.scanSweepTime() / 1000f + 1)
			scanCenter = null;
	}

	private void refreshGlints() {
		var player = client.getLocalPlayer();
		if (!config.shortcutGlint() || player == null || client.getGameState() != GameState.LOGGED_IN) {
			glints.clear();
			return;
		}

		var shortcuts = scanner.findUsableShortcuts(player.getLocalLocation(), GLINT_SEARCH_RADIUS);
		// Keep existing glints, so they don't restart their fade in
		glints.removeIf(glint -> shortcuts.stream().noneMatch(glint.target::isSameAs));
		for (var shortcut : shortcuts)
			if (glints.stream().noneMatch(glint -> glint.target.isSameAs(shortcut)))
				glints.add(new LitTarget(shortcut, now, 1, FADE_OUT));
	}

	private void updateGlints() {
		if (!config.shortcutGlint()) {
			glints.clear();
			return;
		}

		float strength = GLINT_STRENGTH * config.shortcutGlintStrength() / 100f;
		glints.removeIf(glint -> !glint.target.update(client));
		for (var glint : glints) {
			updateLight(glint, GLINT_COLOR, strength, GLINT_RADIUS);
			// Twinkle with occasional sharp peaks
			float wave = (float) Math.sin(now * 1.7 + glint.twinklePhase);
			glint.intensity *= .35f + .65f * (float) Math.pow(Math.max(0, wave), 6);
		}
	}

	private void updateLight(LitTarget lit, Color color, float strength, float radius) {
		if (lit.awtColor != color)
			lit.setColor(color);
		lit.updateAlpha(now);
		lit.intensity = lit.alpha * strength;
		lit.baseRadius = radius;
	}

	@Override
	public void addLights(@Nonnull SceneContext sceneContext, @Nonnull List<Light> destination) {
		if (client.getGameState() != GameState.LOGGED_IN)
			return;

		if (clickMarker != null)
			addLight(clickMarker, destination);
		for (var lit : fadingMarkers)
			addLight(lit, destination);
		if (hover != null)
			addLight(hover, destination);
		for (var lit : fadingHovers)
			addLight(lit, destination);
		if (hintArrow != null)
			addLight(hintArrow, destination);
		for (var lit : fadingHintArrows)
			addLight(lit, destination);
		for (var lit : scanLights)
			addLight(lit, destination);
		for (var lit : glints)
			addLight(lit, destination);
	}

	private void addLight(LitTarget lit, List<Light> destination) {
		if (lit.intensity < .001f)
			return;

		var target = lit.target;
		var location = target.location;
		if (location == null)
			return;

		float x = location.getX();
		float z = location.getY();
		float ground = getTileHeight(client, location, target.plane);
		float height;
		float radius = lit.baseRadius;

		if (target.kind == MarkerTarget.Kind.TILE || target.kind == MarkerTarget.Kind.GROUND_ITEM) {
			// Hover just above the ground, to cast a pool of light around the tile
			height = 64;
		} else {
			height = Math.max(48, Math.min(target.height * .6f, 600));

			// Lights inside of a model would only light up its back faces, so move the light towards the camera,
			// to make the visible side of the target light up instead
			float footprint = Math.max(target.sizeX, target.sizeY) * LOCAL_HALF_TILE_SIZE;
			float dx = client.getCameraX() - x;
			float dz = client.getCameraY() - z;
			float length = (float) Math.sqrt(dx * dx + dz * dz);
			if (length > 1) {
				float push = footprint + LOCAL_HALF_TILE_SIZE;
				x += dx / length * push;
				z += dz / length * push;
			}
			radius = Math.max(radius, footprint * 2 + target.height * .8f);
		}

		var light = lit.light;
		light.def.strength = lit.intensity;
		light.def.radius = (int) radius;
		System.arraycopy(lit.color, 0, light.def.color, 0, 3);
		light.origin[0] = x;
		light.origin[1] = ground - height;
		light.origin[2] = z;
		light.plane = target.plane;
		destination.add(light);
	}

	@Override
	public float getSceneLightStrengthScale() {
		return config.sceneLightStrength() / 100f;
	}

	@Override
	public float getSceneLightRadiusScale() {
		return config.sceneLightRadius() / 100f;
	}

	/**
	 * Resolve the scene target of a menu entry, if any.
	 */
	@Nullable
	private MarkerTarget resolveTarget(MenuEntry entry) {
		var wv = client.getTopLevelWorldView();
		if (entry.getWorldViewId() != wv.getId())
			return null;

		int plane = wv.getPlane();
		switch (entry.getType()) {
			case GAME_OBJECT_FIRST_OPTION:
			case GAME_OBJECT_SECOND_OPTION:
			case GAME_OBJECT_THIRD_OPTION:
			case GAME_OBJECT_FOURTH_OPTION:
			case GAME_OBJECT_FIFTH_OPTION:
			case EXAMINE_OBJECT:
			case WIDGET_TARGET_ON_GAME_OBJECT:
			case ITEM_USE_ON_GAME_OBJECT: {
				var lp = LocalPoint.fromScene(entry.getParam0(), entry.getParam1(), wv);
				var object = MarkerTarget.findObject(MarkerTarget.getTile(wv, lp, plane), entry.getIdentifier());
				if (object == null)
					return null;
				String name = scanner.getObjectName(object);
				return MarkerTarget.object(object, name == null ? "" : name);
			}
			case NPC_FIRST_OPTION:
			case NPC_SECOND_OPTION:
			case NPC_THIRD_OPTION:
			case NPC_FOURTH_OPTION:
			case NPC_FIFTH_OPTION:
			case EXAMINE_NPC:
			case WIDGET_TARGET_ON_NPC:
			case ITEM_USE_ON_NPC: {
				var npc = entry.getNpc();
				return npc == null ? null : MarkerTarget.actor(npc, plane);
			}
			case PLAYER_FIRST_OPTION:
			case PLAYER_SECOND_OPTION:
			case PLAYER_THIRD_OPTION:
			case PLAYER_FOURTH_OPTION:
			case PLAYER_FIFTH_OPTION:
			case PLAYER_SIXTH_OPTION:
			case PLAYER_SEVENTH_OPTION:
			case PLAYER_EIGHTH_OPTION:
			case WIDGET_TARGET_ON_PLAYER:
			case ITEM_USE_ON_PLAYER:
			case RUNELITE_PLAYER: {
				var player = entry.getPlayer();
				return player == null ? null : MarkerTarget.actor(player, plane);
			}
			case GROUND_ITEM_FIRST_OPTION:
			case GROUND_ITEM_SECOND_OPTION:
			case GROUND_ITEM_THIRD_OPTION:
			case GROUND_ITEM_FOURTH_OPTION:
			case GROUND_ITEM_FIFTH_OPTION:
			case EXAMINE_ITEM_GROUND:
			case WIDGET_TARGET_ON_GROUND_ITEM:
			case ITEM_USE_ON_GROUND_ITEM: {
				var lp = LocalPoint.fromScene(entry.getParam0(), entry.getParam1(), wv);
				int itemId = entry.getIdentifier();
				return MarkerTarget.groundItem(lp, plane, itemId, itemManager.getItemComposition(itemId).getName());
			}
			case WALK: {
				var tile = wv.getSelectedSceneTile();
				return tile == null ? null : MarkerTarget.tile(tile.getLocalLocation(), plane);
			}
			default:
				return null;
		}
	}

	private static boolean isExamine(MenuAction type) {
		return type == MenuAction.EXAMINE_OBJECT || type == MenuAction.EXAMINE_NPC || type == MenuAction.EXAMINE_ITEM_GROUND;
	}

	/**
	 * Whether the target is far enough away to use long range markers
	 */
	boolean isLongRange(LitTarget lit, boolean isWalk) {
		var player = client.getLocalPlayer();
		if (player == null || (isWalk && !config.longRangeWhenWalking()))
			return false;
		return lit.target.tileDistanceTo(player.getLocalLocation()) > config.longRangeDistance();
	}
}
