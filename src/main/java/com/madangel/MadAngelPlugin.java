package com.madangel;

import com.google.inject.Provides;
import com.madangel.SweepAnimations.Side;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.Getter;
import net.runelite.api.Actor;
import net.runelite.api.ActorSpotAnim;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Mad Angel Helper",
	description = "Shows which tile to click to dodge Mad Angel's sweep, where to stand to bounce back Exploding Light, and when to pray against her smite",
	tags = {"mad", "angel", "sweep", "cleave", "dodge", "smite", "prayer", "boss", "pvm", "wyrmscraig"}
)
public class MadAngelPlugin extends Plugin
{
	/**
	 * VFX_MAD_ANGEL_ATTACK_BOMB_THROW_PROJECTILE: the Exploding Light ball.
	 */
	static final int EXPLODING_LIGHT_PROJECTILE = 4015;

	/**
	 * How many ticks after a smite starts the timing log keeps posting.
	 */
	private static final int SMITE_REPORT_TICKS = 20;

	/**
	 * A prayer clicked on one tick is up on the next, so the countdown ends a tick before each strike lands.
	 */
	private static final int PRAYER_DELAY = 1;

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

	@Getter
	private NPC smiteNpc;
	private final SmiteTimer smiteTimer = new SmiteTimer();
	private int smiteStartTick = -1;

	/**
	 * The NPC that last cast smite, kept after the countdown ends so the timing log can follow it.
	 */
	private NPC smiteCaster;
	private final Set<Projectile> loggedProjectiles = new HashSet<>();

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
		endSmite();
		lights.clear();
	}

	/**
	 * Ticks until the next smite strike (0 means pray now), or -1 when none is coming.
	 */
	int getSmiteTicks()
	{
		return smiteNpc == null ? -1 : smiteTimer.ticksUntilStrike(client.getTickCount());
	}

	/**
	 * Which strike is next and how many there are, as "2/3", or null for a single strike.
	 */
	String getSmiteStrikeLabel()
	{
		int count = smiteTimer.strikeCount();
		if (smiteNpc == null || count < 2)
		{
			return null;
		}
		return smiteTimer.nextStrike(client.getTickCount()) + "/" + count;
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
		if (npc == smiteCaster && animation != SmiteTimer.SMITE && animation != SmiteTimer.SMITE_ENRAGED)
		{
			logTiming("boss animation " + animation);
		}

		if (animation == SmiteTimer.SMITE || animation == SmiteTimer.SMITE_ENRAGED)
		{
			startSmite(npc, animation == SmiteTimer.SMITE_ENRAGED);
			return;
		}

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
		if (smiteNpc != null && (smiteNpc.isDead() || smiteTimer.isFinished(client.getTickCount())))
		{
			endSmite();
		}
		if (getSmiteTicks() == 0)
		{
			logTiming("countdown says PRAY MAGE");
		}

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
		if (loggedProjectiles.add(projectile))
		{
			logTiming("projectile " + projectile.getId());
		}

		if (projectile.getId() == EXPLODING_LIGHT_PROJECTILE)
		{
			lights.put(projectile, event.getPosition());
		}
	}

	@Subscribe
	public void onGraphicChanged(GraphicChanged event)
	{
		Actor actor = event.getActor();
		if (actor != smiteCaster && actor != client.getLocalPlayer())
		{
			return;
		}

		StringBuilder ids = new StringBuilder();
		for (ActorSpotAnim spotAnim : actor.getSpotAnims())
		{
			ids.append(ids.length() == 0 ? "" : ", ").append(spotAnim.getId());
		}
		logTiming((actor == smiteCaster ? "boss" : "your") + " graphics: " + (ids.length() == 0 ? "none" : ids));
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (event.getActor() == client.getLocalPlayer())
		{
			logTiming("you took " + event.getHitsplat().getAmount());
		}
	}

	/**
	 * Posts a step of the smite with how many ticks after its start it happened, while the smite is recent.
	 */
	private void logTiming(String what)
	{
		int ticks = client.getTickCount() - smiteStartTick;
		if (smiteStartTick >= 0 && ticks <= SMITE_REPORT_TICKS && config.smiteChat())
		{
			chat("t+" + ticks + ": " + what);
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (event.getNpc() == smiteNpc)
		{
			endSmite();
		}
		if (event.getNpc() == smiteCaster)
		{
			smiteCaster = null;
		}

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
			endSmite();
			lights.clear();
		}
	}

	private void startSmite(NPC npc, boolean enraged)
	{
		int tick = client.getTickCount();
		if (npc == smiteNpc && !smiteTimer.isFinished(tick))
		{
			// Enraged she may replay the animation between strikes; keep counting from the first.
			return;
		}

		smiteNpc = npc;
		smiteCaster = npc;
		smiteStartTick = tick;
		loggedProjectiles.clear();
		if (enraged)
		{
			smiteTimer.start(tick, config.enragedFirstTicks() - PRAYER_DELAY, config.enragedSecondTicks(), config.enragedThirdTicks());
		}
		else
		{
			smiteTimer.start(tick, config.smiteTicks() - PRAYER_DELAY);
		}

		if (config.smiteChat())
		{
			chat((enraged ? "Enraged smite started" : "Smite started") + " (animation " + (enraged ? SmiteTimer.SMITE_ENRAGED : SmiteTimer.SMITE) + ")");
		}
	}

	private void endSmite()
	{
		smiteNpc = null;
		smiteTimer.stop();
	}

	private void chat(String message)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "[Mad Angel] " + message, null);
	}

	private void endSweep()
	{
		sweepNpc = null;
		swordSide = null;
	}
}
