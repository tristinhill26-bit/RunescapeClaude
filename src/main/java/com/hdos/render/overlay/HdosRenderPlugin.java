package com.hdos.render.overlay;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
        name = "HDOS Renderer",
        description = "Custom render engine overlay with styled text, progress tracking, and animations",
        tags = {"hdos", "render", "overlay", "progress", "animation"}
)
public class HdosRenderPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private HdosRenderConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private HdosRenderOverlay renderOverlay;

    @Inject
    private HdosProgressOverlay progressOverlay;

    @Getter
    private String statusText = "";

    @Getter
    private int progressPercent = 0;

    @Getter
    private long trackingStartTime = 0;

    @Getter
    private int trackingStartXp = 0;

    @Getter
    private int trackingTargetXp = 0;

    @Getter
    private boolean isTracking = false;

    @Override
    protected void startUp() {
        overlayManager.add(renderOverlay);
        overlayManager.add(progressOverlay);
        log.info("HDOS Render Engine started");
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(renderOverlay);
        overlayManager.remove(progressOverlay);
        renderOverlay.dispose();
        log.info("HDOS Render Engine stopped");
    }

    @Provides
    HdosRenderConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(HdosRenderConfig.class);
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!config.enableOverlay()) {
            return;
        }

        if (isTracking && trackingTargetXp > 0) {
            updateTrackingProgress();
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event) {
        if (!isTracking) {
            return;
        }

        updateTrackingProgress();
    }

    public void startTracking(int skillId, int targetXp) {
        this.trackingStartXp = client.getSkillExperience(net.runelite.api.Skill.values()[skillId]);
        this.trackingTargetXp = targetXp;
        this.trackingStartTime = System.currentTimeMillis();
        this.isTracking = true;
        this.statusText = "Tracking " + net.runelite.api.Skill.values()[skillId].getName();
        this.progressPercent = 0;
    }

    public void stopTracking() {
        this.isTracking = false;
        this.statusText = "";
        this.progressPercent = 0;
        this.trackingStartTime = 0;
    }

    public void setStatus(String text, int percent) {
        this.statusText = text;
        this.progressPercent = Math.max(0, Math.min(100, percent));
    }

    private void updateTrackingProgress() {
        if (trackingTargetXp <= trackingStartXp) {
            return;
        }

        int totalNeeded = trackingTargetXp - trackingStartXp;
        int currentXp = 0;
        try {
            for (net.runelite.api.Skill skill : net.runelite.api.Skill.values()) {
                int xp = client.getSkillExperience(skill);
                if (xp > trackingStartXp && xp <= trackingTargetXp) {
                    currentXp = xp;
                    break;
                }
            }
        } catch (Exception e) {
            return;
        }

        if (currentXp > 0) {
            int gained = currentXp - trackingStartXp;
            this.progressPercent = Math.min(100, (int) ((long) gained * 100 / totalNeeded));

            long elapsed = System.currentTimeMillis() - trackingStartTime;
            if (gained > 0 && elapsed > 0) {
                long remaining = (long) (totalNeeded - gained) * elapsed / gained;
                long seconds = remaining / 1000;
                long minutes = seconds / 60;
                seconds = seconds % 60;
                this.statusText = String.format("Tracking - %dm %02ds remaining", minutes, seconds);
            }
        }
    }

    public long getElapsedMs() {
        if (!isTracking || trackingStartTime == 0) {
            return 0;
        }
        return System.currentTimeMillis() - trackingStartTime;
    }
}
