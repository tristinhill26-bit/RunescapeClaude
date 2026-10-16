package com.toahelper;

import com.google.inject.Provides;
import com.toahelper.StandingGeometry.Slam;
import com.toahelper.StandingGeometry.Tile;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.Getter;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "ToA Helper",
	description = "Tombs of Amascut: which prayer to use, what to hit and with what, where to stand, and your raid points",
	tags = {"toa", "tombs", "amascut", "raid", "prayer", "akkha", "zebak", "kephri", "baba", "wardens", "points"}
)
public class ToaHelperPlugin extends Plugin
{
	private static final int CYCLES_PER_TICK = 30;
	/**
	 * How long after Ba-Ba's rockfall to keep showing cover from her thrown rock.
	 */
	private static final int BABA_COVER_TICKS = 12;
	/**
	 * How long after a slam animation to keep showing the safe tile.
	 */
	private static final int SLAM_TICKS = 4;

	private static final String RAID_START = "You enter the Tombs of Amascut";
	private static final String DEATH = "You have died";
	private static final String ROOM_COMPLETE = "Challenge complete";
	private static final String ROOM_FAILED = "Your party failed to complete";

	@Inject
	private Client client;

	@Inject
	private ToaHelperConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ToaInfoOverlay infoOverlay;

	@Inject
	private ToaSceneOverlay sceneOverlay;

	@Inject
	private PrayerBookOverlay prayerBookOverlay;

	@Getter
	private ToaIds.Room room;
	@Getter
	private int raidLevel;
	@Getter
	private final PointsCalculator points = new PointsCalculator();

	private final Map<Projectile, AttackStyle> incoming = new HashMap<>();

	@Getter
	private AttackStyle standingStyle;
	@Getter
	private String standingReason;
	@Getter
	private boolean zebakMeleeWarning;

	private int babaRockfallTick = -100;
	private Slam slam;
	private NPC slamWarden;
	private int slamTick = -100;

	@Provides
	ToaHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ToaHelperConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(infoOverlay);
		overlayManager.add(sceneOverlay);
		overlayManager.add(prayerBookOverlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(infoOverlay);
		overlayManager.remove(sceneOverlay);
		overlayManager.remove(prayerBookOverlay);
		incoming.clear();
		room = null;
		slam = null;
		slamWarden = null;
	}

	boolean inRaid()
	{
		return room != null;
	}

	// ---- Prayer ----

	/**
	 * What to pray right now. Called by the overlays each frame on the client thread.
	 */
	PrayerAdvisor.Advice getPrayerAdvice()
	{
		int cycle = client.getGameCycle();
		List<PrayerAdvisor.Incoming> list = new ArrayList<>();
		for (Iterator<Map.Entry<Projectile, AttackStyle>> it = incoming.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry<Projectile, AttackStyle> e = it.next();
			int cyclesLeft = e.getKey().getEndCycle() - cycle;
			if (cyclesLeft <= 0)
			{
				it.remove();
				continue;
			}
			int ticks = (cyclesLeft + CYCLES_PER_TICK - 1) / CYCLES_PER_TICK;
			list.add(new PrayerAdvisor.Incoming(e.getValue(), ticks, "projectile"));
		}

		boolean standing = config.standingPrayer();
		return PrayerAdvisor.advise(list, standing ? standingStyle : null, standing ? standingReason : null);
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		if (!inRaid())
		{
			return;
		}

		Projectile projectile = event.getProjectile();
		AttackStyle style = ToaIds.PROJECTILE_STYLES.get(projectile.getId());
		Player me = client.getLocalPlayer();
		if (style == null || me == null || incoming.containsKey(projectile))
		{
			return;
		}

		Actor targetActor = projectile.getTargetActor();
		LocalPoint target = event.getPosition();
		LocalPoint mine = me.getLocalLocation();
		boolean atMe = targetActor == me;
		if (targetActor == null && target != null && mine != null)
		{
			atMe = landsNextTo(target.getX() >> 7, target.getY() >> 7, mine.getX() >> 7, mine.getY() >> 7);
		}

		if (atMe)
		{
			incoming.put(projectile, style);
		}
	}

	/**
	 * Whether a projectile aimed at a tile (rather than at someone) will land on or next to you.
	 * Zebak's rocks are thrown at the ground and break into fragments, so they don't land exactly
	 * on your tile.
	 */
	static boolean landsNextTo(int targetX, int targetY, int myX, int myY)
	{
		return Math.abs(targetX - myX) <= 1 && Math.abs(targetY - myY) <= 1;
	}

	private void updateStandingThreat()
	{
		standingStyle = null;
		standingReason = null;
		zebakMeleeWarning = false;

		Player me = client.getLocalPlayer();
		LocalPoint mine = me == null ? null : me.getLocalLocation();
		if (mine == null)
		{
			return;
		}
		Tile myTile = new Tile(mine.getX() >> 7, mine.getY() >> 7);

		int priority = Integer.MAX_VALUE;
		for (NPC npc : client.getNpcs())
		{
			int id = npc.getId();
			if (id == ToaIds.KEPHRI_GUARDIAN_MAGE && npc.hasSpotAnim(ToaIds.KEPHRI_MAGE_CHARGE) && priority > 0)
			{
				priority = 0;
				standingStyle = AttackStyle.MAGIC;
				standingReason = "Arcane Scarab charging";
			}
			else if (id == ToaIds.BABA && inMeleeReach(npc, myTile) && priority > 1)
			{
				priority = 1;
				standingStyle = AttackStyle.MELEE;
				standingReason = "In Ba-Ba's reach";
			}
			else if (priority > 2 && (id == ToaIds.AKKHA_MELEE || id == ToaIds.AKKHA_RANGE || id == ToaIds.AKKHA_MAGE))
			{
				priority = 2;
				standingStyle = id == ToaIds.AKKHA_MELEE ? AttackStyle.MELEE
					: id == ToaIds.AKKHA_RANGE ? AttackStyle.RANGED : AttackStyle.MAGIC;
				standingReason = "Akkha's form";
			}
			else if (priority > 3 && (ToaIds.BABOON_MELEE.contains(id) || id == ToaIds.KEPHRI_GUARDIAN_MELEE)
				&& npc.getInteracting() == me && inMeleeReach(npc, myTile))
			{
				priority = 3;
				standingStyle = AttackStyle.MELEE;
				standingReason = id == ToaIds.KEPHRI_GUARDIAN_MELEE ? "Soldier Scarab next to you" : "Brawler next to you";
			}

			if (ToaIds.ZEBAK.contains(id) && ToaIds.ZEBAK_MELEE_ANIMATIONS.contains(npc.getAnimation())
				&& inMeleeReach(npc, myTile))
			{
				zebakMeleeWarning = true;
			}
		}
	}

	private static boolean inMeleeReach(NPC npc, Tile me)
	{
		Tile sw = southWest(npc);
		if (sw == null)
		{
			return false;
		}
		int size = size(npc);
		int dx = me.getX() < sw.getX() ? sw.getX() - me.getX() : Math.max(0, me.getX() - (sw.getX() + size - 1));
		int dy = me.getY() < sw.getY() ? sw.getY() - me.getY() : Math.max(0, me.getY() - (sw.getY() + size - 1));
		// Melee reaches the tiles touching an NPC's sides, not its corners.
		return dx + dy == 1;
	}

	// ---- Damage ----

	/**
	 * Your current target, if it's an NPC.
	 */
	NPC getTarget()
	{
		Player me = client.getLocalPlayer();
		Actor target = me == null ? null : me.getInteracting();
		return target instanceof NPC ? (NPC) target : null;
	}

	static Set<AttackStyle> overheadPrayers(NPC npc)
	{
		Set<AttackStyle> styles = EnumSet.noneOf(AttackStyle.class);
		int[] archives = npc.getOverheadArchiveIds();
		short[] sprites = npc.getOverheadSpriteIds();
		if (archives == null || sprites == null)
		{
			return styles;
		}
		for (int i = 0; i < archives.length && i < sprites.length; i++)
		{
			if (archives[i] == AttackStyle.HEADICONS_PRAYER_ARCHIVE)
			{
				styles.addAll(AttackStyle.fromHeadIcon(sprites[i]));
			}
		}
		return styles;
	}

	// ---- Where to stand ----

	boolean isBabaCoverActive()
	{
		return client.getTickCount() - babaRockfallTick <= BABA_COVER_TICKS;
	}

	/**
	 * The slam to dodge right now, after the "Flip slam sides" setting, or null.
	 */
	Slam getActiveSlam()
	{
		if (slam == null || slamWarden == null || client.getTickCount() - slamTick > SLAM_TICKS)
		{
			return null;
		}
		return config.flipSlams() ? slam.flipped() : slam;
	}

	NPC getSlamWarden()
	{
		return slamWarden;
	}

	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		GraphicsObject graphic = event.getGraphicsObject();
		if (ToaIds.BABA_ROCKFALL_GRAPHICS.contains(graphic.getId()))
		{
			babaRockfallTick = client.getTickCount();
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (!(event.getActor() instanceof NPC))
		{
			return;
		}

		NPC npc = (NPC) event.getActor();
		if (!ToaIds.WARDENS_P3.contains(npc.getId()))
		{
			return;
		}

		int animation = npc.getAnimation();
		Slam s = ToaIds.WARDEN_SLAM_LEFT.contains(animation) ? Slam.LEFT
			: ToaIds.WARDEN_SLAM_RIGHT.contains(animation) ? Slam.RIGHT
			: ToaIds.WARDEN_SLAM_BOTH.contains(animation) ? Slam.BOTH
			: null;
		if (s != null)
		{
			slam = s;
			slamWarden = npc;
			slamTick = client.getTickCount();
		}
	}

	// ---- Points ----

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (!inRaid() || !(event.getActor() instanceof NPC) || !event.getHitsplat().isMine())
		{
			return;
		}

		NPC npc = (NPC) event.getActor();
		double factor = ToaIds.POINTS_FACTORS.getOrDefault(npc.getId(), 1.0);
		points.addDamage(event.getHitsplat().getAmount(), factor, room.pointsCap, ToaIds.WARDENS_P2.contains(npc.getId()));
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		NPCComposition old = event.getOld();
		if (old != null && ToaIds.WARDENS_P2.contains(old.getId())
			&& ToaIds.WARDENS_P2_EXPOSED.contains(event.getNpc().getId()))
		{
			points.wardenDowned();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String message = event.getMessage();
		if (message.startsWith(RAID_START))
		{
			points.reset();
		}
		else if (!inRaid())
		{
			return;
		}
		else if (message.startsWith(DEATH))
		{
			points.died();
		}
		else if (message.startsWith(ROOM_COMPLETE) || message.startsWith(ROOM_FAILED))
		{
			points.roomEnded();
		}
	}

	// ---- Bookkeeping ----

	@Subscribe
	public void onGameTick(GameTick event)
	{
		Player me = client.getLocalPlayer();
		LocalPoint mine = me == null ? null : me.getLocalLocation();
		room = mine == null ? null : ToaIds.Room.forRegion(WorldPoint.fromLocalInstance(client, mine).getRegionID());
		if (!inRaid())
		{
			incoming.clear();
			standingStyle = null;
			standingReason = null;
			return;
		}

		raidLevel = client.getVarbitValue(ToaIds.RAID_LEVEL_VARBIT);
		updateStandingThreat();

		if (slamWarden != null && (slamWarden.isDead() || client.getTickCount() - slamTick > SLAM_TICKS))
		{
			slamWarden = null;
			slam = null;
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING || event.getGameState() == GameState.LOGIN_SCREEN
			|| event.getGameState() == GameState.HOPPING)
		{
			incoming.clear();
			slamWarden = null;
			slam = null;
		}
	}

	// ---- Helpers shared with the overlays ----

	static int size(NPC npc)
	{
		NPCComposition composition = npc.getTransformedComposition();
		return composition == null ? 1 : Math.max(1, composition.getSize());
	}

	/**
	 * An NPC's south-west tile (local coordinates / 128).
	 */
	static Tile southWest(NPC npc)
	{
		LocalPoint center = npc.getLocalLocation();
		if (center == null)
		{
			return null;
		}
		int offset = (size(npc) - 1) * 64;
		return new Tile((center.getX() - offset) >> 7, (center.getY() - offset) >> 7);
	}
}
