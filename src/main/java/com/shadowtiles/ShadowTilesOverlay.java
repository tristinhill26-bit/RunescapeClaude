package com.shadowtiles;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

class ShadowTilesOverlay extends Overlay
{
	private static final int TILE = Perspective.LOCAL_TILE_SIZE;

	private final Client client;
	private final ShadowTilesPlugin plugin;
	private final ShadowTilesConfig config;

	@Inject
	ShadowTilesOverlay(Client client, ShadowTilesPlugin plugin, ShadowTilesConfig config)
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
		List<ActiveHazard> hazards = plugin.getActiveHazards();
		if (hazards.isEmpty())
		{
			return null;
		}

		// Collect tiles first so overlapping hazards don't stack their fill and turn opaque.
		Map<Long, LocalPoint> tiles = new LinkedHashMap<>();
		Map<Long, HazardDefinition> owners = new LinkedHashMap<>();
		for (ActiveHazard hazard : hazards)
		{
			LocalPoint center = hazard.getCenter();
			int width = hazard.getWidth();
			// Centre of the south-west tile. Works for odd widths (centre on a tile) and even
			// widths (centre on a tile corner, e.g. 2x2 NPCs).
			int swX = center.getX() - (width - 1) * TILE / 2;
			int swY = center.getY() - (width - 1) * TILE / 2;
			for (int dx = 0; dx < width; dx++)
			{
				for (int dy = 0; dy < width; dy++)
				{
					int x = swX + dx * TILE;
					int y = swY + dy * TILE;
					long key = ((long) center.getWorldView() << 40)
						| ((long) (Math.floorDiv(x, TILE) & 0xFFFFF) << 20)
						| (Math.floorDiv(y, TILE) & 0xFFFFF);
					if (tiles.putIfAbsent(key, new LocalPoint(x, y, center.getWorldView())) == null)
					{
						owners.put(key, hazard.getDefinition());
					}
				}
			}
		}

		Stroke stroke = new BasicStroke(config.borderWidth());
		for (Map.Entry<Long, LocalPoint> e : tiles.entrySet())
		{
			Polygon poly = Perspective.getCanvasTilePoly(client, e.getValue());
			if (poly == null)
			{
				continue;
			}
			Color custom = owners.get(e.getKey()).getColor();
			OverlayUtil.renderPolygon(graphics, poly, borderColor(custom), fillColor(custom), stroke);
		}

		for (ActiveHazard hazard : hazards)
		{
			renderText(graphics, hazard);
		}

		return null;
	}

	private void renderText(Graphics2D graphics, ActiveHazard hazard)
	{
		String label = config.showLabels() ? hazard.getDefinition().getLabel() : null;
		String countdown = config.showCountdown() && hazard.getTicksLeft() >= 0
			? String.valueOf(hazard.getTicksLeft())
			: null;

		String text;
		if (label != null && countdown != null)
		{
			text = label + " " + countdown;
		}
		else if (label != null)
		{
			text = label;
		}
		else if (countdown != null)
		{
			text = countdown;
		}
		else
		{
			return;
		}

		Point location = Perspective.getCanvasTextLocation(client, graphics, hazard.getCenter(), text, 0);
		if (location != null)
		{
			Color custom = hazard.getDefinition().getColor();
			OverlayUtil.renderTextLocation(graphics, location, text, custom != null ? custom : Color.WHITE);
		}
	}

	private Color borderColor(Color custom)
	{
		if (custom == null)
		{
			return config.borderColor();
		}
		return new Color(custom.getRed(), custom.getGreen(), custom.getBlue(), config.borderColor().getAlpha());
	}

	private Color fillColor(Color custom)
	{
		if (custom == null)
		{
			return config.fillColor();
		}
		return new Color(custom.getRed(), custom.getGreen(), custom.getBlue(), config.fillColor().getAlpha());
	}
}
