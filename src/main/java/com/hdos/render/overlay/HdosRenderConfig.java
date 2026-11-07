package com.hdos.render.overlay;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("hdosrender")
public interface HdosRenderConfig extends Config {

    @ConfigSection(
            name = "Text Rendering",
            description = "Styled text overlay settings",
            position = 0
    )
    String textSection = "textSection";

    @ConfigSection(
            name = "Progress Bar",
            description = "Progress bar overlay settings",
            position = 1
    )
    String progressSection = "progressSection";

    @ConfigSection(
            name = "Animation",
            description = "Animation overlay settings",
            position = 2
    )
    String animationSection = "animationSection";

    @ConfigItem(
            keyName = "enableOverlay",
            name = "Enable Overlay",
            description = "Show the HDOS render overlay",
            position = 0
    )
    default boolean enableOverlay() {
        return true;
    }

    @ConfigItem(
            keyName = "showProgressBar",
            name = "Show Progress Bar",
            description = "Display a styled progress bar for tracked activities",
            section = progressSection,
            position = 0
    )
    default boolean showProgressBar() {
        return true;
    }

    @Range(min = 50, max = 400)
    @ConfigItem(
            keyName = "progressBarWidth",
            name = "Bar Width",
            description = "Width of the progress bar in pixels",
            section = progressSection,
            position = 1
    )
    default int progressBarWidth() {
        return 200;
    }

    @Range(min = 8, max = 40)
    @ConfigItem(
            keyName = "progressBarHeight",
            name = "Bar Height",
            description = "Height of the progress bar in pixels",
            section = progressSection,
            position = 2
    )
    default int progressBarHeight() {
        return 20;
    }

    @Alpha
    @ConfigItem(
            keyName = "progressBarColor",
            name = "Bar Color",
            description = "Progress bar fill color",
            section = progressSection,
            position = 3
    )
    default Color progressBarColor() {
        return new Color(0, 180, 0);
    }

    @Alpha
    @ConfigItem(
            keyName = "progressBarBgColor",
            name = "Bar Background",
            description = "Progress bar background color",
            section = progressSection,
            position = 4
    )
    default Color progressBarBgColor() {
        return new Color(40, 40, 40, 180);
    }

    @ConfigItem(
            keyName = "textShadow",
            name = "Text Shadow",
            description = "Enable shadow effect on overlay text",
            section = textSection,
            position = 0
    )
    default boolean textShadow() {
        return true;
    }

    @ConfigItem(
            keyName = "textGlow",
            name = "Text Glow",
            description = "Enable glow effect on overlay text",
            section = textSection,
            position = 1
    )
    default boolean textGlow() {
        return false;
    }

    @Alpha
    @ConfigItem(
            keyName = "textColor",
            name = "Text Color",
            description = "Primary text color",
            section = textSection,
            position = 2
    )
    default Color textColor() {
        return Color.WHITE;
    }

    @Alpha
    @ConfigItem(
            keyName = "textShadowColor",
            name = "Shadow Color",
            description = "Text shadow/glow color",
            section = textSection,
            position = 3
    )
    default Color textShadowColor() {
        return Color.BLACK;
    }

    @Range(min = 8, max = 24)
    @ConfigItem(
            keyName = "fontSize",
            name = "Font Size",
            description = "Overlay text font size",
            section = textSection,
            position = 4
    )
    default int fontSize() {
        return 12;
    }

    @ConfigItem(
            keyName = "enableAnimation",
            name = "Enable Animation",
            description = "Enable animated loading dots on status text",
            section = animationSection,
            position = 0
    )
    default boolean enableAnimation() {
        return true;
    }

    @Range(min = 250, max = 3000)
    @ConfigItem(
            keyName = "animationSpeed",
            name = "Animation Speed (ms)",
            description = "Milliseconds per animation frame",
            section = animationSection,
            position = 1
    )
    default int animationSpeed() {
        return 1000;
    }
}
