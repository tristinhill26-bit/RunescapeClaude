package com.hdos.render.overlay;

import com.hdos.render.text.StyledText;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

public class HdosRenderOverlay extends Overlay {

    private final Client client;
    private final HdosRenderPlugin plugin;
    private final HdosRenderConfig config;

    private final StyledText statusLabel;
    private final StyledText percentLabel;
    private final StyledText etaLabel;

    private int animationPhase = 0;
    private long lastAnimationTick = 0;

    @Inject
    public HdosRenderOverlay(Client client, HdosRenderPlugin plugin, HdosRenderConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);

        this.statusLabel = new StyledText();
        this.percentLabel = new StyledText();
        this.etaLabel = new StyledText();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.enableOverlay()) {
            return null;
        }

        String status = plugin.getStatusText();
        if (status == null || status.isEmpty()) {
            return null;
        }

        int styleFlags = 0;
        if (config.textShadow()) styleFlags |= StyledText.STYLE_SHADOW;
        if (config.textGlow()) styleFlags |= StyledText.STYLE_GLOW;

        Font font = new Font(Font.SANS_SERIF, Font.BOLD, config.fontSize());
        Color fg = config.textColor();
        Color shadow = config.textShadowColor();

        updateAnimation();
        String displayText = status;
        if (config.enableAnimation() && plugin.isTracking()) {
            displayText = status + ".".repeat(animationPhase);
        }

        statusLabel.setText(displayText);
        statusLabel.setFont(font);
        statusLabel.setForegroundColor(fg);
        statusLabel.setBackgroundColor(shadow);
        statusLabel.setStyleFlags(styleFlags);
        statusLabel.layout(graphics);
        statusLabel.paint(graphics, 0, 0);

        float yOffset = statusLabel.getRenderedSize().height + 4;

        if (plugin.getProgressPercent() > 0) {
            percentLabel.setText(plugin.getProgressPercent() + "%");
            percentLabel.setFont(font);
            percentLabel.setForegroundColor(fg);
            percentLabel.setBackgroundColor(shadow);
            percentLabel.setStyleFlags(styleFlags);
            percentLabel.layout(graphics);
            percentLabel.paint(graphics, 0, yOffset);

            yOffset += percentLabel.getRenderedSize().height + 2;
        }

        long elapsed = plugin.getElapsedMs();
        if (elapsed > 0) {
            long seconds = elapsed / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            String etaText = String.format("Elapsed: %dm %02ds", minutes, seconds);

            etaLabel.setText(etaText);
            etaLabel.setFont(font.deriveFont(Font.PLAIN, config.fontSize() - 2f));
            etaLabel.setForegroundColor(new Color(200, 200, 200));
            etaLabel.setBackgroundColor(shadow);
            etaLabel.setStyleFlags(styleFlags);
            etaLabel.layout(graphics);
            etaLabel.paint(graphics, 0, yOffset);

            yOffset += etaLabel.getRenderedSize().height;
        }

        int maxWidth = Math.max(
                statusLabel.getRenderedSize().width,
                Math.max(
                        percentLabel.getRenderedSize().width,
                        etaLabel.getRenderedSize().width
                )
        );

        return new Dimension(maxWidth + 4, (int) yOffset + 4);
    }

    private void updateAnimation() {
        long now = System.currentTimeMillis();
        if (now - lastAnimationTick >= config.animationSpeed()) {
            animationPhase = animationPhase % 3 + 1;
            lastAnimationTick = now;
        }
    }

    public void dispose() {
        // cleanup if needed
    }
}
