package com.madangel;

import com.google.inject.Provides;
import com.madangel.SweepAnimations.Side;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import lombok.Getter;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Mad Angel Helper",
	description = "Shows which tile to click to dodge Mad Angel's sweep, and where to stand to bounce back Exploding Light",
	tags = {"mad", "angel", "sweep", "cleave", "dodge", "boss", "pvm", "wyrmscraig"}
)
public class MadAngelPlugin extends Plugin
{
	/**
	 * VFX_MAD_ANGEL_ATTACK_BOMB_THROW_PROJECTILE: the Exploding Light ball.
	 */
	static final int EXPLODING_LIGHT_PROJECTILE = 4015;

	/**
	 * Game ticks without a sweep animation before we decide the sweep is over.
	 */
	private static final int SWEEP_TIMEOUT_TICKS = 4;

	@Inject
	private Client client;

	@Inject
	private MadAngelConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MadAngelOverlay overlay;

	@Getter
	private NPC sweepNpc;
	private Side swordSide;
	private int lastSweepTick;

	/**
	 * Exploding Light balls in flight, and the tile each will land on.
	 */
	@Getter
	private final Map<Projectile, LocalPoint> lights = new HashMap<>();

	@Provides
	MadAngelConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MadAngelConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		endSweep();
		lights.clear();
	}

	/**
	 * The side the sword will cleave from next, after the "Flip left/right" setting.
	 */
	Side getSwordSide()
	{
		if (swordSide == null)
		{
			return null;
		}
		return config.flipSides() ? swordSide.opposite() : swordSide;
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		Actor actor = event.getActor();
		if (!(actor instanceof NPC))
		{
			return;
		}

		NPC npc = (NPC) actor;
		int animation = npc.getAnimation();
		Side side = SweepAnimations.swordSide(animation);
		if (side != null)
		{
			sweepNpc = npc;
			swordSide = side;
			lastSweepTick = client.getTickCount();
		}
		else if (npc == sweepNpc && SweepAnimations.isReset(animation))
		{
			endSweep();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (sweepNpc == null)
		{
			return;
		}

		if (sweepNpc.isDead())
		{
			endSweep();
			return;
		}

		int tick = client.getTickCount();
		if (SweepAnimations.swordSide(sweepNpc.getAnimation()) != null)
		{
			lastSweepTick = tick;
		}
		else if (tick - lastSweepTick > SWEEP_TIMEOUT_TICKS)
		{
			endSweep();
		}
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		Projectile projectile = event.getProjectile();
		if (projectile.getId() == EXPLODING_LIGHT_PROJECTILE)
		{
			lights.put(projectile, event.getPosition());
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (event.getNpc() == sweepNpc)
		{
			endSweep();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING
			|| event.getGameState() == GameState.LOGIN_SCREEN
			|| event.getGameState() == GameState.HOPPING)
		{
			endSweep();
			lights.clear();
		}
	}

	private void endSweep()
	{
		sweepNpc = null;
		swordSide = null;
	}
}
