package com.toahelper;

import com.toahelper.StandingGeometry.Slam;
import com.toahelper.StandingGeometry.SlamAdvice;
import com.toahelper.StandingGeometry.Tile;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Tiles to stand on and targets to hit.
 */
class ToaSceneOverlay extends Overlay
{
	private static final Stroke THIN = new BasicStroke(1);
	private static final Stroke THICK = new BasicStroke(3);

	private final Client client;
	private final ToaHelperPlugin plugin;
	private final ToaHelperConfig config;

	@Inject
	ToaSceneOverlay(Client client, ToaHelperPlugin plugin, ToaHelperConfig config)
	{
		super(plugin);
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.inRaid())
		{
			return null;
		}

		List<NPC> npcs = client.getNpcs();
		NPC zebak = null;
		List<NPC> rocks = new ArrayList<>();
		List<NPC> cover = new ArrayList<>();
		for (NPC npc : npcs)
		{
			int id = npc.getId();
			if (config.zebakJugSplash() && ToaIds.ZEBAK_JUGS.contains(id))
			{
				renderSplash(graphics, npc);
			}
			if (config.baboonKillOrder())
			{
				renderKillOrder(graphics, npc);
			}
			if (ToaIds.ZEBAK.contains(id))
			{
				zebak = npc;
			}
			else if (id == ToaIds.ZEBAK_SAFESPOT)
			{
				rocks.add(npc);
			}
			else if (ToaIds.BABA_COVER.contains(id))
			{
				cover.add(npc);
			}

			if (config.priorityTargets())
			{
				renderPriority(graphics, npc);
			}
		}

		if (config.zebakShelter() && zebak != null && !rocks.isEmpty())
		{
			renderShelter(graphics, zebak, rocks);
		}
		if (config.babaCover() && plugin.isBabaCoverActive() && !cover.isEmpty())
		{
			renderCover(graphics, cover);
		}
		if (config.wardenSlams())
		{
			renderSlam(graphics);
		}
		renderLightningSkulls(graphics);
		return null;
	}

	private void renderSplash(Graphics2D graphics, NPC jug)
	{
		LocalPoint at = jug.getLocalLocation();
		if (at == null)
		{
			return;
		}
		// Wiki: a broken jug's water covers 5x5, or 3x3 with Upset Stomach.
		int radius = config.upsetStomach() ? 1 : 2;
		Tile center = new Tile(at.getX() >> 7, at.getY() >> 7);
		for (Tile tile : ToaMechanics.square(center, radius))
		{
			drawTile(graphics, tile, at.getWorldView(), config.splashColor(), THIN, null);
		}
	}

	private void renderKillOrder(Graphics2D graphics, NPC npc)
	{
		int order = TargetAdvisor.baboonKillOrder(npc.getId());
		if (order == 0)
		{
			return;
		}
		String text = String.valueOf(order);
		Point at = npc.getCanvasTextLocation(graphics, text, npc.getLogicalHeight() + 40);
		if (at != null)
		{
			OverlayUtil.renderTextLocation(graphics, at, text, order == 1 ? Color.RED : Color.WHITE);
		}
	}

	private void renderLightningSkulls(Graphics2D graphics)
	{
		for (ToaHelperPlugin.SkullMark mark : plugin.getLightningSkulls())
		{
			ToaMechanics.LightningSkull skull = ToaMechanics.lightningSkull(mark.getLanding());
			for (Tile tile : skull.getDanger())
			{
				drawTile(graphics, tile, mark.getWorldView(), config.dangerColor(), THIN, null);
			}
			for (Tile tile : skull.getSafe())
			{
				drawTile(graphics, tile, mark.getWorldView(), config.safeColor(), THICK, null);
			}
		}
	}

	private void renderShelter(Graphics2D graphics, NPC zebak, List<NPC> rocks)
	{
		LocalPoint center = zebak.getLocalLocation();
		if (center == null)
		{
			return;
		}
		Tile zebakTile = new Tile(center.getX() >> 7, center.getY() >> 7);
		int worldView = center.getWorldView();
		for (NPC rock : rocks)
		{
			Tile sw = ToaHelperPlugin.southWest(rock);
			if (sw == null)
			{
				continue;
			}
			boolean first = true;
			for (Tile tile : StandingGeometry.roarShelter(zebakTile, sw, ToaHelperPlugin.size(rock)))
			{
				drawTile(graphics, tile, worldView, config.safeColor(), THIN, first ? "SHELTER" : null);
				first = false;
			}
		}
	}

	private void renderCover(Graphics2D graphics, List<NPC> cover)
	{
		Set<Tile> tiles = new LinkedHashSet<>();
		int worldView = -1;
		for (NPC npc : cover)
		{
			Tile sw = ToaHelperPlugin.southWest(npc);
			if (sw == null)
			{
				continue;
			}
			worldView = npc.getLocalLocation().getWorldView();
			tiles.addAll(StandingGeometry.adjacentRing(sw, ToaHelperPlugin.size(npc)));
		}
		for (Tile tile : tiles)
		{
			drawTile(graphics, tile, worldView, config.safeColor(), THIN, null);
		}
	}

	private void renderSlam(Graphics2D graphics)
	{
		Slam slam = plugin.getActiveSlam();
		NPC warden = plugin.getSlamWarden();
		Player me = client.getLocalPlayer();
		LocalPoint wardenLocation = warden == null ? null : warden.getLocalLocation();
		LocalPoint mine = me == null ? null : me.getLocalLocation();
		if (slam == null || wardenLocation == null || mine == null)
		{
			return;
		}

		SlamAdvice advice = StandingGeometry.slam(slam, wardenLocation.getX() >> 7,
			new Tile(mine.getX() >> 7, mine.getY() >> 7));
		drawTile(graphics, advice.getSafe(), mine.getWorldView(), config.safeColor(), THICK,
			advice.isAlreadySafe() ? "SAFE" : "MOVE HERE");
	}

	private void renderPriority(Graphics2D graphics, NPC npc)
	{
		String label;
		switch (npc.getId())
		{
			case ToaIds.BABA_BOULDER_WEAK:
				label = "MAX HIT";
				break;
			case ToaIds.HET_SEAL_VULNERABLE:
				label = "HIT NOW";
				break;
			case ToaIds.KEPHRI_GUARDIAN_MAGE:
				label = "KILL FIRST";
				break;
			case ToaIds.WARDENS_SIPHON:
				label = "MELEE";
				break;
			default:
				if (ToaIds.KEPHRI_EGGS.contains(npc.getId()))
				{
					label = "EGG";
				}
				else if (ToaIds.WARDEN_CORES.contains(npc.getId()))
				{
					label = "CORE x5";
				}
				else
				{
					return;
				}
				break;
		}

		Polygon poly = npc.getCanvasTilePoly();
		Color color = config.priorityColor();
		Color solid = new Color(color.getRed(), color.getGreen(), color.getBlue());
		if (poly != null)
		{
			OverlayUtil.renderPolygon(graphics, poly, solid, color, THICK);
		}
		Point text = npc.getCanvasTextLocation(graphics, label, npc.getLogicalHeight() + 40);
		if (text != null)
		{
			OverlayUtil.renderTextLocation(graphics, text, label, solid);
		}
	}

	private void drawTile(Graphics2D graphics, Tile tile, int worldView, Color fill, Stroke stroke, String label)
	{
		LocalPoint local = new LocalPoint((tile.getX() << 7) + 64, (tile.getY() << 7) + 64, worldView);
		Polygon poly = Perspective.getCanvasTilePoly(client, local);
		Color solid = new Color(fill.getRed(), fill.getGreen(), fill.getBlue());
		if (poly != null)
		{
			OverlayUtil.renderPolygon(graphics, poly, solid, fill, stroke);
		}
		if (label != null)
		{
			Point text = Perspective.getCanvasTextLocation(client, graphics, local, label, 0);
			if (text != null)
			{
				OverlayUtil.renderTextLocation(graphics, text, label, solid);
			}
		}
	}
}
