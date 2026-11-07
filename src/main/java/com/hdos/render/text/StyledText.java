package com.hdos.render.text;

import com.hdos.render.GraphicsUtils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StyledText {

    public static final int STYLE_OUTLINE = 1;
    public static final int STYLE_SHADOW = 2;
    public static final int STYLE_GLOW = 4;
    public static final int STYLE_UNDERLINE = 8;

    public static final Pattern COLOR_TAG_PATTERN = Pattern.compile("#([0-9a-fA-FxX]+)");
    protected static final Pattern ESCAPE_PATTERN = Pattern.compile("[%#]");

    protected String plainText;
    protected String markedUpText;
    protected int styleFlags;
    protected int alignment = -1;
    protected Dimension constraintSize = new Dimension();
    protected Dimension renderedSize = new Dimension();
    protected float[] lineOffsets;
    protected Font font;
    protected TextLayout[] textLayouts;
    protected Rectangle2D[] lineBounds;
    protected Color backgroundColor = null;
    protected Color foregroundColor = null;
    protected boolean antiAliased = true;

    public static String stripColorTags(String text) {
        if (text == null) return null;
        return COLOR_TAG_PATTERN.matcher(text).replaceAll("");
    }

    public StyledText() {
        setText("");
    }

    public StyledText(String text) {
        setText(text);
    }

    public StyledText(String text, Color foreground, Font font) {
        setText(text);
        setForegroundColor(foreground);
        setFont(font);
    }

    public StyledText(String text, int styleFlags, Color foreground, Color background, Font font) {
        setText(text);
        setStyleFlags(styleFlags);
        setForegroundColor(foreground);
        setBackgroundColor(background);
        setFont(font);
    }

    public boolean setText(String text) {
        if (text == null || text.isEmpty()) {
            text = " ";
        }

        String currentText = (markedUpText != null) ? markedUpText : plainText;
        if (text.equals(currentText)) {
            return false;
        }

        String stripped = stripColorTags(text);
        this.plainText = processEscapes(stripped, true);
        this.markedUpText = text.equals(stripped) ? null : processEscapes(text, false);

        invalidate();
        return true;
    }

    private static String processEscapes(String text, boolean useHash) {
        if (text == null) return null;
        String replacement = useHash ? "#" : "%";
        return ESCAPE_PATTERN.matcher(text).replaceAll(replacement + "$0");
    }

    public void setFont(Font font) {
        this.font = font;
        invalidate();
    }

    public void setForegroundColor(Color color) {
        this.foregroundColor = color;
    }

    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
    }

    public void setStyleFlags(int flags) {
        this.styleFlags = flags;
        invalidate();
    }

    public void setMaxWidth(int width) {
        if (width <= 0) throw new IllegalArgumentException("width must be > 0, got " + width);
        constraintSize.width = width;
        constraintSize.height = 0;
        invalidate();
    }

    public void setConstraintHeight(int height) {
        constraintSize.height = height;
        invalidate();
    }

    public Dimension getRenderedSize() {
        return renderedSize;
    }

    public boolean isLaidOut() {
        return textLayouts != null;
    }

    public void layout(Graphics2D g) {
        Object oldHints = GraphicsUtils.enableAntiAliasing(g);
        FontRenderContext frc = g.getFontRenderContext();

        List<LineInfo> lines = null;

        if (constraintSize.height > 0) {
            int targetHeight = constraintSize.height;
            TextLayout probe = new TextLayout(getAttributedIterator(g), frc);
            Rectangle2D probeBounds = getLogicalBounds(probe);

            int lineCount = 1;
            double totalWidth = computeWidth(probeBounds) / lineCount;
            double totalHeight = computeLineHeight(probe) * lineCount;
            double bestRatioDiff = Math.abs(totalWidth / totalHeight - 1.618034);

            while (true) {
                double nextWidth = computeWidth(probeBounds) / (lineCount + 1);
                double nextHeight = computeLineHeight(probe) * (lineCount + 1);
                double ratioDiff = Math.abs(nextWidth / nextHeight - 1.618034);
                if (ratioDiff >= bestRatioDiff) break;
                bestRatioDiff = ratioDiff;
                totalHeight = nextHeight;
                if (++lineCount >= 200) break;
            }

            int wrapWidth = (int) Math.ceil(totalHeight);
            for (int attempt = 0; attempt < 10; attempt++) {
                LineBreakMeasurer lbm = new LineBreakMeasurer(getAttributedIterator(g), frc);
                lines = measureLines(lbm, wrapWidth, renderedSize, true);
                if (lines != null && lines.size() <= lineCount) break;
                wrapWidth = (int) Math.round(wrapWidth * 1.1);
            }

        } else if (constraintSize.width > 0) {
            LineBreakMeasurer lbm = new LineBreakMeasurer(getAttributedIterator(g), frc);
            lines = measureLines(lbm, constraintSize.width, renderedSize, false);
        }

        if (lines == null) {
            TextLayout layout = new TextLayout(getAttributedIterator(g), frc);
            Rectangle2D bounds = getLogicalBounds(layout);
            renderedSize.setSize(Math.ceil(computeWidth(bounds)), Math.ceil(computeLineHeight(layout)));
            lines = new ArrayList<>();
            lines.add(new LineInfo(layout, bounds));
        }

        int lineCount = lines.size();
        textLayouts = new TextLayout[lineCount];
        lineBounds = new Rectangle2D[lineCount];
        lineOffsets = new float[lineCount];

        for (int i = 0; i < lineCount; i++) {
            LineInfo info = lines.get(i);
            textLayouts[i] = info.layout;
            lineBounds[i] = info.bounds;
            if (lineBounds[i].getX() < 0) {
                lineOffsets[i] = (float) (-lineBounds[i].getX());
            }
        }

        GraphicsUtils.restoreHints(g, oldHints);
    }

    protected List<LineInfo> measureLines(LineBreakMeasurer lbm, int wrapWidth,
                                          Dimension outSize, boolean strict) {
        double maxWidth = 0;
        double totalHeight = 0;
        List<LineInfo> lines = new ArrayList<>();

        try {
            int textLength = plainText.length();
            while (true) {
                int nextNewline = plainText.indexOf('\n', lbm.getPosition() + 1);
                if (nextNewline == -1) nextNewline = textLength;

                TextLayout tl = lbm.nextLayout(wrapWidth, nextNewline, strict);
                if (tl == null) break;

                Rectangle2D bounds = getLogicalBounds(tl);
                maxWidth = Math.max(maxWidth, computeWidth(bounds));
                totalHeight += computeLineHeight(tl);
                lines.add(new LineInfo(tl, bounds));
            }

            outSize.setSize(Math.ceil(maxWidth), Math.ceil(totalHeight));

            if (lbm.getPosition() < textLength) {
                return null;
            }
        } catch (Throwable t) {
            // layout failure
        }

        return lines;
    }

    public void paint(Graphics2D g, float x, float y) {
        if (textLayouts == null) return;

        Object oldHints = GraphicsUtils.enableAntiAliasing(g);

        float currentY = y;
        for (int i = 0; i < textLayouts.length; i++) {
            TextLayout tl = textLayouts[i];
            float lineX = x + lineOffsets[i];

            currentY += tl.getAscent();

            if ((styleFlags & STYLE_SHADOW) != 0 && backgroundColor != null) {
                Color saved = g.getColor();
                g.setColor(backgroundColor);
                tl.draw(g, lineX + 2, currentY + 2);
                g.setColor(saved);
            }

            if ((styleFlags & STYLE_GLOW) != 0 && backgroundColor != null) {
                Color saved = g.getColor();
                g.setColor(backgroundColor);
                tl.draw(g, lineX + 1, currentY + 1);
                g.setColor(saved);
            }

            if (foregroundColor != null) {
                g.setColor(foregroundColor);
            }
            tl.draw(g, lineX, currentY);

            currentY += tl.getDescent() + tl.getLeading();
        }

        GraphicsUtils.restoreHints(g, oldHints);
    }

    protected AttributedCharacterIterator getAttributedIterator(Graphics2D g) {
        Font effectiveFont = (font != null) ? font : g.getFont();

        HashMap<TextAttribute, Serializable> attrs = new HashMap<>();
        attrs.put(TextAttribute.FONT, effectiveFont);

        if ((styleFlags & STYLE_UNDERLINE) != 0) {
            attrs.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_ONE_PIXEL);
        }

        AttributedString as = new AttributedString(plainText, attrs);
        applyColorTags(as);
        return as.getIterator();
    }

    protected void applyColorTags(AttributedString as) {
        if (markedUpText == null) return;

        Matcher m = COLOR_TAG_PATTERN.matcher(markedUpText);
        int colorStart = 0;
        int offset = 0;
        Color currentColor = null;

        while (m.find()) {
            offset += m.start();
            if (currentColor != null && colorStart < offset && offset <= plainText.length()) {
                as.addAttribute(TextAttribute.FOREGROUND, currentColor, colorStart, offset);
            }

            String hexStr = m.group(1);
            currentColor = "x".equalsIgnoreCase(hexStr)
                    ? null
                    : new Color(Integer.parseInt(hexStr, 16));
            colorStart = offset;
            offset -= m.end();
        }

        if (currentColor != null && colorStart < plainText.length()) {
            as.addAttribute(TextAttribute.FOREGROUND, currentColor, colorStart, plainText.length());
        }
    }

    protected double computeWidth(Rectangle2D bounds) {
        double w = Math.max(bounds.getX(), 0) + bounds.getWidth();
        if ((styleFlags & STYLE_SHADOW) != 0) w += 2.0;
        else if ((styleFlags & STYLE_GLOW) != 0) w += 1.0;
        else if ((styleFlags & STYLE_OUTLINE) != 0) w += 1.0;
        return w;
    }

    protected Rectangle2D getLogicalBounds(TextLayout tl) {
        return tl.getBounds();
    }

    protected float computeLineHeight(TextLayout tl) {
        float h = tl.getLeading() + tl.getAscent() + tl.getDescent();
        if ((styleFlags & STYLE_SHADOW) != 0) h += 2.0f;
        else if ((styleFlags & STYLE_GLOW) != 0) h += 1.0f;
        return h;
    }

    protected void invalidate() {
        textLayouts = null;
    }

    protected static class LineInfo {
        final TextLayout layout;
        final Rectangle2D bounds;

        LineInfo(TextLayout layout, Rectangle2D bounds) {
            this.layout = layout;
            this.bounds = bounds;
        }
    }
}
