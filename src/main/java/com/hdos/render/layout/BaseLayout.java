package com.hdos.render.layout;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import java.util.HashMap;

public abstract class BaseLayout implements LayoutManager2 {

    protected static final int SIZE_MINIMUM = 0;
    protected static final int SIZE_PREFERRED = 1;
    protected static final int SIZE_MAXIMUM = 2;

    protected int gap = 5;
    protected HashMap<Component, LayoutConstraint> constraints;

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        if (constraints != null) {
            constraints.remove(comp);
        }
    }

    @Override
    public void addLayoutComponent(Component comp, Object constraint) {
        if (constraint == null) return;
        if (constraint instanceof LayoutConstraint) {
            if (constraints == null) {
                constraints = new HashMap<>();
            }
            constraints.put(comp, (LayoutConstraint) constraint);
        } else {
            throw new RuntimeException("Unsupported constraint type: " + constraint.getClass());
        }
    }

    @Override
    public float getLayoutAlignmentX(Container target) {
        return 0.0f;
    }

    @Override
    public float getLayoutAlignmentY(Container target) {
        return 0.0f;
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return computeSize(parent, SIZE_MINIMUM);
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return computeSize(parent, SIZE_PREFERRED);
    }

    @Override
    public Dimension maximumLayoutSize(Container parent) {
        return computeSize(parent, SIZE_MAXIMUM);
    }

    @Override
    public void invalidateLayout(Container target) {
    }

    protected abstract Dimension computeSize(Container parent, int sizeMode);

    @Override
    public abstract void layoutContainer(Container parent);

    protected LayoutConstraint getConstraint(Component comp) {
        if (constraints != null) {
            LayoutConstraint c = constraints.get(comp);
            if (c != null) return c;
        }
        return LayoutConstraint.DEFAULT;
    }

    protected LayoutStats gatherStats(Container parent, int sizeMode) {
        int count = parent.getComponentCount();
        LayoutStats stats = new LayoutStats();
        stats.componentSizes = new Dimension[count];

        for (int i = 0; i < count; i++) {
            Component comp = parent.getComponent(i);
            if (!comp.isVisible()) continue;

            Dimension size;
            switch (sizeMode) {
                case SIZE_MINIMUM:
                    size = comp.getMinimumSize();
                    break;
                case SIZE_MAXIMUM:
                    size = comp.getMaximumSize();
                    break;
                default:
                    size = comp.getPreferredSize();
            }

            stats.visibleCount++;
            stats.totalWidth += size.width;
            stats.totalHeight += size.height;
            stats.maxWidth = Math.max(stats.maxWidth, size.width);
            stats.maxHeight = Math.max(stats.maxHeight, size.height);

            LayoutConstraint constraint = getConstraint(comp);
            if (constraint.isWeighted()) {
                stats.weightedWidth += size.width;
                stats.weightedHeight += size.height;
                stats.weightedCount++;
            } else {
                stats.totalWeight += constraint.getWeight();
                stats.fixedMaxWidth = Math.max(stats.fixedMaxWidth, size.width);
                stats.fixedMaxHeight = Math.max(stats.fixedMaxHeight, size.height);
            }

            stats.componentSizes[i] = size;
        }

        return stats;
    }

    public static class LayoutConstraint {
        public static final LayoutConstraint DEFAULT = new LayoutConstraint(1);

        private final int weight;

        public LayoutConstraint(int weight) {
            this.weight = weight;
        }

        public int getWeight() {
            return weight;
        }

        public boolean isWeighted() {
            return weight > 0;
        }
    }

    protected static class LayoutStats {
        int visibleCount;
        int totalWidth;
        int totalHeight;
        int maxWidth;
        int maxHeight;
        int weightedWidth;
        int weightedHeight;
        int weightedCount;
        int totalWeight;
        int fixedMaxWidth;
        int fixedMaxHeight;
        Dimension[] componentSizes;
    }
}
