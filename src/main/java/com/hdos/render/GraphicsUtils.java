package com.hdos.render;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;

public class GraphicsUtils {

    public static Object enableAntiAliasing(Graphics2D g) {
        RenderingHints oldHints = g.getRenderingHints();
        RenderingHints newHints = new RenderingHints(null);
        newHints.add(oldHints);
        newHints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        newHints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHints(newHints);
        return oldHints;
    }

    public static void restoreHints(Graphics2D g, Object previousHints) {
        if (previousHints != null) {
            g.setRenderingHints((RenderingHints) previousHints);
        }
    }

    public static void centerWindow(Window window) {
        Rectangle screenBounds;
        try {
            screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .getDefaultConfiguration()
                    .getBounds();
        } catch (Throwable t) {
            Dimension screenSize = window.getToolkit().getScreenSize();
            screenBounds = new Rectangle(screenSize);
        }
        int w = window.getWidth();
        int h = window.getHeight();
        window.setBounds(
                screenBounds.x + (screenBounds.width - w) / 2,
                screenBounds.y + (screenBounds.height - h) / 2,
                w, h);
    }
}
