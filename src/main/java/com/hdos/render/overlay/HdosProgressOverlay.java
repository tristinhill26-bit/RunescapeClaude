package com.hdos.render.overlay;

import com.hdos.render.GraphicsUtils;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

public class HdosProgressOverlay extends Overlay {

    private static final int BORDER_RADIUS = 6;
    private static final int BORDER_WIDTH = 1;
    private static final int TEXT_PADDING = 4;

    private final Client client;
    private final HdosRenderPlugin plugin;
    private final HdosRenderConfig config;

    @Inject
    public HdosProgressOverlay(Client client, HdosRenderPlugin plugin, HdosRenderConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.enableOverlay() || !config.showProgressBar()) {
            return null;
        }

        int percent = plugin.getProgressPercent();
        if (percent <= 0) {
            return null;
        }

        Object oldHints = GraphicsUtils.enableAntiAliasing(graphics);

        int barWidth = config.progressBarWidth();
        int barHeight = config.progressBarHeight();

        graphics.setComposite(AlphaComposite.SrcOver);

        graphics.setColor(config.progressBarBgColor());
        graphics.fillRoundRect(0, 0, barWidth, barHeight, BORDER_RADIUS, BORDER_RADIUS);

        int fillWidth = percent * barWidth / 100;

        graphics.setClip(0, 0, fillWidth, barHeight);
        graphics.setColor(config.progressBarColor());
        graphics.fillRoundRect(0, 0, barWidth, barHeight, BORDER_RADIUS, BORDER_RADIUS);
        graphics.setClip(null);

        graphics.setColor(new Color(80, 80, 80));
        graphics.drawRoundRect(0, 0, barWidth - 1, barHeight - 1, BORDER_RADIUS, BORDER_RADIUS);

        String percentText = percent + "%";
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, Math.max(barHeight - 6, 8));
        graphics.setFont(font);

        int textWidth = graphics.getFontMetrics().stringWidth(percentText);
        int textHeight = graphics.getFontMetrics().getAscent();
        int textX = (barWidth - textWidth) / 2;
        int textY = (barHeight + textHeight) / 2 - 1;

        graphics.setColor(Color.BLACK);
        graphics.drawString(percentText, textX + 1, textY + 1);

        graphics.setColor(Color.WHITE);
        graphics.drawString(percentText, textX, textY);

        GraphicsUtils.restoreHints(graphics, oldHints);

        return new Dimension(barWidth, barHeight);
    }
}
