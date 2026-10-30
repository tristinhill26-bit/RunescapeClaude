package com.toahelper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.Text;

/**
 * Highlights the protection prayer to click in the prayer book.
 */
class PrayerBookOverlay extends Overlay
{
	/**
	 * gameval InterfaceID.Prayerbook.PRAYER1; the prayers follow it in order.
	 */
	private static final int FIRST_PRAYER_COMPONENT = 0x021d_0009;
	private static final int PRAYER_COUNT = 30;

	private final Client client;
	private final ToaHelperPlugin plugin;
	private final ToaHelperConfig config;

	@Inject
	PrayerBookOverlay(Client client, ToaHelperPlugin plugin, ToaHelperConfig config)
	{
		super(plugin);
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.highlightPrayer() || !plugin.inRaid())
		{
			return null;
		}

		AttackStyle style = plugin.getPrayerAdvice().getPray();
		if (style == null)
		{
			return null;
		}

		Widget widget = findPrayer(style.getPrayerName());
		if (widget == null || widget.isHidden())
		{
			return null;
		}

		Rectangle bounds = widget.getBounds();
		Color color = config.prayerColor();
		if (client.isPrayerActive(prayer(style)))
		{
			// Already on: a thin green frame so you know it's right.
			graphics.setColor(new Color(0, 255, 0));
			graphics.setStroke(new BasicStroke(1));
			graphics.draw(bounds);
			return null;
		}

		graphics.setColor(color);
		graphics.fill(bounds);
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue()));
		graphics.setStroke(new BasicStroke(3));
		graphics.draw(bounds);
		return null;
	}

	private Widget findPrayer(String name)
	{
		for (int i = 0; i < PRAYER_COUNT; i++)
		{
			Widget widget = client.getWidget(FIRST_PRAYER_COMPONENT + i);
			if (widget != null && widget.getName() != null && Text.removeTags(widget.getName()).equals(name))
			{
				return widget;
			}
		}
		return null;
	}

	static Prayer prayer(AttackStyle style)
	{
		switch (style)
		{
			case MELEE:
				return Prayer.PROTECT_FROM_MELEE;
			case RANGED:
				return Prayer.PROTECT_FROM_MISSILES;
			default:
				return Prayer.PROTECT_FROM_MAGIC;
		}
	}
}
