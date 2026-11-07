package com.hdos.render.layout;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;

public class VerticalLayout extends BaseLayout {

    public VerticalLayout() {
    }

    public VerticalLayout(int gap) {
        this.gap = gap;
    }

    @Override
    protected Dimension computeSize(Container parent, int sizeMode) {
        LayoutStats stats = gatherStats(parent, sizeMode);
        Insets insets = parent.getInsets();
        int width = stats.maxWidth + insets.left + insets.right;
        int height = stats.totalHeight + insets.top + insets.bottom;
        if (stats.visibleCount > 1) {
            height += gap * (stats.visibleCount - 1);
        }
        return new Dimension(width, height);
    }

    @Override
    public void layoutContainer(Container parent) {
        Insets insets = parent.getInsets();
        int x = insets.left;
        int y = insets.top;
        int availableWidth = parent.getWidth() - insets.left - insets.right;

        for (int i = 0; i < parent.getComponentCount(); i++) {
            Component comp = parent.getComponent(i);
            if (!comp.isVisible()) continue;

            Dimension pref = comp.getPreferredSize();
            comp.setBounds(x, y, availableWidth, pref.height);
            y += pref.height + gap;
        }
    }
}
