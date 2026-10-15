package net.runelite.client.plugins.shadowtiles;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Projectile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Shadow Tiles",
	description = "Highlights tiles about to be hit by boss AoE attacks: falling rock shadows, Ba-Ba, Olm, Vorkath and your own custom bosses",
	tags = {"aoe", "shadow", "tiles", "boss", "highlight", "baba", "toa", "pvm"}
)
public class ShadowTilesPlugin extends Plugin
{
	/**
	 * Client cycles (20ms each) per 600ms game tick.
	 */
	private static final int CYCLES_PER_TICK = 30;

	@AllArgsConstructor
	private static class TrackedProjectile
	{
		HazardDefinition definition;
		LocalPoint target;
	}

	@AllArgsConstructor
	private static class TrackedAnim
	{
		HazardDefinition definition;
		int endTick;
	}

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ShadowTilesConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ShadowTilesOverlay overlay;

	private Map<Long, HazardDefinition> definitions = Collections.emptyMap();

	private final Map<GraphicsObject, HazardDefinition> graphics = new HashMap<>();
	private final Map<Projectile, TrackedProjectile> projectiles = new HashMap<>();
	private final Map<TileObject, HazardDefinition> objects = new HashMap<>();
	private final Map<NPC, HazardDefinition> npcs = new HashMap<>();
	private final Map<NPC, TrackedAnim> anims = new HashMap<>();

	private final Set<String> loggedIds = new HashSet<>();
	private int sceneLoadedTick = -1;

	@Provides
	ShadowTilesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ShadowTilesConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		clientThread.invoke(this::rebuildDefinitions);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		clientThread.invoke(() ->
		{
			clearTracked();
			definitions = Collections.emptyMap();
			loggedIds.clear();
		});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!ShadowTilesConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		if ("logIds".equals(event.getKey()))
		{
			loggedIds.clear();
		}
		clientThread.invoke(this::rebuildDefinitions);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING
			|| event.getGameState() == GameState.LOGIN_SCREEN
			|| event.getGameState() == GameState.HOPPING)
		{
			// Everything is re-sent by spawn events once the new scene loads.
			clearTracked();
		}
		else if (event.getGameState() == GameState.LOGGED_IN)
		{
			sceneLoadedTick = client.getTickCount();
		}
	}

	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		GraphicsObject graphic = event.getGraphicsObject();
		logId("graphic", graphic.getId(), graphic.getLocation(), null);

		HazardDefinition def = definitions.get(HazardDefinition.key(HazardType.GRAPHIC, graphic.getId()));
		if (def != null)
		{
			graphics.put(graphic, def);
		}
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		Projectile projectile = event.getProjectile();
		LocalPoint target = event.getPosition();

		TrackedProjectile tracked = projectiles.get(projectile);
		if (tracked != null)
		{
			// Projectiles aimed at an actor follow it, so keep the latest landing spot.
			tracked.target = target;
			return;
		}

		logId("projectile", projectile.getId(), target, null);

		HazardDefinition def = definitions.get(HazardDefinition.key(HazardType.PROJECTILE, projectile.getId()));
		if (def != null)
		{
			projectiles.put(projectile, new TrackedProjectile(def, target));
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		objectSpawned(event.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		objects.remove(event.getGameObject());
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		objectSpawned(event.getGroundObject());
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		objects.remove(event.getGroundObject());
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();
		logId("npc", npc.getId(), npc.getLocalLocation(), npc.getName());
		trackNpc(npc);
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		npcs.remove(event.getNpc());
		anims.remove(event.getNpc());
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		Actor actor = event.getActor();
		if (!(actor instanceof NPC) || actor.getAnimation() == -1)
		{
			return;
		}

		NPC npc = (NPC) actor;
		int animation = npc.getAnimation();
		logId("anim", animation, npc.getLocalLocation(), npc.getName() + " (npc " + npc.getId() + ")");

		HazardDefinition def = definitions.get(HazardDefinition.key(HazardType.ANIM, animation));
		if (def != null)
		{
			anims.put(npc, new TrackedAnim(def, client.getTickCount() + def.getTicks()));
		}
	}

	/**
	 * Everything currently dangerous, as squares of tiles. Called by the overlay each frame,
	 * on the client thread; also drops hazards that have finished.
	 */
	List<ActiveHazard> getActiveHazards()
	{
		List<ActiveHazard> hazards = new ArrayList<>();
		int cycle = client.getGameCycle();
		int tick = client.getTickCount();

		for (Iterator<Map.Entry<GraphicsObject, HazardDefinition>> it = graphics.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry<GraphicsObject, HazardDefinition> e = it.next();
			GraphicsObject graphic = e.getKey();
			if (graphic.finished())
			{
				it.remove();
				continue;
			}
			hazards.add(area(graphic.getLocation(), 1, e.getValue(), -1));
		}

		for (Iterator<Map.Entry<Projectile, TrackedProjectile>> it = projectiles.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry<Projectile, TrackedProjectile> e = it.next();
			int cyclesLeft = e.getKey().getEndCycle() - cycle;
			if (cyclesLeft <= 0)
			{
				it.remove();
				continue;
			}
			TrackedProjectile tracked = e.getValue();
			int ticksLeft = (cyclesLeft + CYCLES_PER_TICK - 1) / CYCLES_PER_TICK;
			hazards.add(area(tracked.target, 1, tracked.definition, ticksLeft));
		}

		for (Map.Entry<TileObject, HazardDefinition> e : objects.entrySet())
		{
			hazards.add(area(e.getKey().getLocalLocation(), 1, e.getValue(), -1));
		}

		for (Map.Entry<NPC, HazardDefinition> e : npcs.entrySet())
		{
			NPC npc = e.getKey();
			hazards.add(area(npc.getLocalLocation(), npcSize(npc), e.getValue(), -1));
		}

		for (Iterator<Map.Entry<NPC, TrackedAnim>> it = anims.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry<NPC, TrackedAnim> e = it.next();
			TrackedAnim tracked = e.getValue();
			if (tick >= tracked.endTick)
			{
				it.remove();
				continue;
			}
			NPC npc = e.getKey();
			hazards.add(area(npc.getLocalLocation(), npcSize(npc), tracked.definition, tracked.endTick - tick));
		}

		hazards.removeIf(h -> h.getCenter() == null);
		return hazards;
	}

	private static ActiveHazard area(LocalPoint center, int sourceSize, HazardDefinition def, int ticksLeft)
	{
		return new ActiveHazard(center, sourceSize + 2 * def.getRadius(), def, ticksLeft);
	}

	private static int npcSize(NPC npc)
	{
		NPCComposition composition = npc.getTransformedComposition();
		return composition == null ? 1 : Math.max(1, composition.getSize());
	}

	private void objectSpawned(TileObject object)
	{
		// Skip the flood of static scenery sent while a scene loads.
		if (client.getTickCount() > sceneLoadedTick + 1)
		{
			logId("object", object.getId(), object.getLocalLocation(), null);
		}

		HazardDefinition def = definitions.get(HazardDefinition.key(HazardType.OBJECT, object.getId()));
		if (def != null)
		{
			objects.put(object, def);
		}
	}

	private void trackNpc(NPC npc)
	{
		HazardDefinition def = definitions.get(HazardDefinition.key(HazardType.NPC, npc.getId()));
		if (def != null)
		{
			npcs.put(npc, def);
		}
		else
		{
			npcs.remove(npc);
		}
	}

	private void rebuildDefinitions()
	{
		Map<Long, HazardDefinition> defs = new HashMap<>();
		for (BuiltInHazards group : BuiltInHazards.values())
		{
			if (group.getEnabled().test(config))
			{
				for (HazardDefinition def : group.getDefinitions())
				{
					defs.put(def.key(), def);
				}
			}
		}

		HazardDefinitionParser.Result custom = HazardDefinitionParser.parse(config.customDefinitions());
		for (HazardDefinition def : custom.getDefinitions())
		{
			defs.put(def.key(), def);
		}

		for (String error : custom.getErrors())
		{
			log.warn("Custom attacks {}", error);
			chat("Custom attacks " + error);
		}

		definitions = defs;

		// Re-apply to what is already in the scene so changes show without reloading it.
		graphics.replaceAll((g, d) -> defs.get(d.key()));
		graphics.values().removeIf(d -> d == null);
		projectiles.values().removeIf(p -> (p.definition = defs.get(p.definition.key())) == null);
		objects.replaceAll((o, d) -> defs.get(d.key()));
		objects.values().removeIf(d -> d == null);
		anims.values().removeIf(a -> (a.definition = defs.get(a.definition.key())) == null);
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			for (NPC npc : client.getNpcs())
			{
				trackNpc(npc);
			}
		}
	}

	private void clearTracked()
	{
		graphics.clear();
		projectiles.clear();
		objects.clear();
		npcs.clear();
		anims.clear();
	}

	private void logId(String type, int id, LocalPoint location, String extra)
	{
		if (!config.logIds() || location == null)
		{
			return;
		}

		Player player = client.getLocalPlayer();
		LocalPoint playerLocation = player == null ? null : player.getLocalLocation();
		if (playerLocation == null
			|| playerLocation.distanceTo(location) > config.logDistance() * Perspective.LOCAL_TILE_SIZE)
		{
			return;
		}

		if (!loggedIds.add(type + ":" + id))
		{
			return;
		}

		chat(type + " " + id + (extra == null ? "" : " - " + extra));
	}

	private void chat(String message)
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "[Shadow Tiles] " + message, null);
		}
	}
}
