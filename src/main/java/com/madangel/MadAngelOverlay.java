package com.madangel;

import com.madangel.SweepAnimations.Side;
import com.madangel.SweepGeometry.Tile;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.Iterator;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Projectile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

class MadAngelOverlay extends Overlay
{
	private static final Stroke THIN = new BasicStroke(1);
	private static final Stroke THICK = new BasicStroke(3);

	private final Client client;
	private final MadAngelPlugin plugin;
	private final MadAngelConfig config;

	@Inject
	MadAngelOverlay(Client client, MadAngelPlugin plugin, MadAngelConfig config)
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
		renderSweep(graphics);
		renderLights(graphics);
		return null;
	}

	private void renderSweep(Graphics2D graphics)
	{
		NPC npc = plugin.getSweepNpc();
		Side side = plugin.getSwordSide();
		LocalPoint center = npc == null ? null : npc.getLocalLocation();
		if (center == null || side == null)
		{
			return;
		}

		NPCComposition composition = npc.getTransformedComposition();
		int size = composition == null ? 1 : Math.max(1, composition.getSize());
		SweepGeometry.Result result = SweepGeometry.compute(
			center.getX(), center.getY(), size, npc.getOrientation(), side, config.dangerRange());
		int worldView = center.getWorldView();

		if (config.showDanger())
		{
			Color danger = config.dangerColor();
			for (Tile tile : result.getDanger())
			{
				drawTile(graphics, local(tile, worldView), solid(danger), danger, THIN);
			}
		}

		if (config.showBehind())
		{
			drawLabelledTile(graphics, local(result.getBehind(), worldView), config.behindColor(), "BEHIND");
		}

		if (config.showDodge())
		{
			LocalPoint dodge = local(result.getDodge(), worldView);
			if (isBlocked(dodge))
			{
				// Against a wall: clicking her own edge tile still moves you to the dodge side.
				dodge = local(result.getDodgeInside(), worldView);
			}
			drawLabelledTile(graphics, dodge, config.dodgeColor(), "CLICK");
		}
	}

	private void renderLights(Graphics2D graphics)
	{
		Map<Projectile, LocalPoint> lights = plugin.getLights();
		int cycle = client.getGameCycle();
		for (Iterator<Map.Entry<Projectile, LocalPoint>> it = lights.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry<Projectile, LocalPoint> e = it.next();
			if (e.getKey().getEndCycle() <= cycle)
			{
				it.remove();
				continue;
			}
			if (config.showLight())
			{
				drawLabelledTile(graphics, e.getValue(), config.lightColor(), "STAND");
			}
		}
	}

	private void drawLabelledTile(Graphics2D graphics, LocalPoint tile, Color fill, String label)
	{
		if (tile == null)
		{
			return;
		}

		drawTile(graphics, tile, solid(fill), fill, THICK);
		Point text = Perspective.getCanvasTextLocation(client, graphics, tile, label, 0);
		if (text != null)
		{
			OverlayUtil.renderTextLocation(graphics, text, label, solid(fill));
		}
	}

	private void drawTile(Graphics2D graphics, LocalPoint tile, Color border, Color fill, Stroke stroke)
	{
		Polygon poly = Perspective.getCanvasTilePoly(client, tile);
		if (poly != null)
		{
			OverlayUtil.renderPolygon(graphics, poly, border, fill, stroke);
		}
	}

	private boolean isBlocked(LocalPoint tile)
	{
		WorldView wv = client.getWorldView(tile.getWorldView());
		CollisionData[] maps = wv == null ? null : wv.getCollisionMaps();
		if (maps == null || wv.getPlane() >= maps.length || maps[wv.getPlane()] == null)
		{
			return false;
		}

		int[][] flags = maps[wv.getPlane()].getFlags();
		int x = tile.getSceneX();
		int y = tile.getSceneY();
		if (x < 0 || y < 0 || x >= flags.length || y >= flags[x].length)
		{
			return false;
		}
		return (flags[x][y] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0;
	}

	private static LocalPoint local(Tile tile, int worldView)
	{
		return tile == null ? null : new LocalPoint(tile.getX(), tile.getY(), worldView);
	}

	private static Color solid(Color color)
	{
		return new Color(color.getRed(), color.getGreen(), color.getBlue());
	}
}
