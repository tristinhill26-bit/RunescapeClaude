package com.hdos.render;

import java.awt.Image;
import java.util.List;

public final class AnimationController {

    protected long frameEnteredAt;
    protected int currentFrame;
    protected Image[] frames;
    protected Image backgroundImage;
    protected int[] progressThresholds;
    protected int[] frameDelays;

    public AnimationController() {
        this.currentFrame = -1;
        this.progressThresholds = new int[0];
        this.frameDelays = new int[0];
        this.frames = new Image[0];
    }

    public AnimationController(Image staticImage) {
        this.currentFrame = -1;
        this.progressThresholds = new int[]{0};
        this.frameDelays = new int[]{0};
        this.frames = new Image[]{staticImage};
        this.backgroundImage = this.frames[0];
    }

    public AnimationController(List<String> frameSpecs, String bgImageName, ImageLoader imageLoader) {
        this.currentFrame = -1;
        int count = frameSpecs.size();
        this.progressThresholds = new int[count];
        this.frameDelays = new int[count];
        this.frames = new Image[count];

        for (int i = 0; i < count; i++) {
            String spec = frameSpecs.get(i);
            String[] parts = spec.split(";");
            if (parts.length != 2) {
                clear();
                return;
            }
            this.frames[i] = imageLoader.loadImage(parts[0]);
            try {
                this.frameDelays[i] = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                clear();
                return;
            }
            this.progressThresholds[i] = (int) ((float) i / (float) count * 100.0f);
        }

        if (count > 1) {
            this.progressThresholds[1] = 1;
        }

        this.backgroundImage = (bgImageName != null)
                ? imageLoader.loadImage(bgImageName)
                : this.frames[0];
    }

    public Image getFrame(int progressPercent) {
        if (frames.length == 0) {
            return null;
        }

        long now = System.currentTimeMillis();

        if (currentFrame != frames.length - 1) {
            boolean shouldAdvance = false;
            if (currentFrame == -1) {
                shouldAdvance = true;
            } else if (progressPercent >= progressThresholds[currentFrame + 1]) {
                long elapsed = (now - frameEnteredAt) / 1000L - (long) frameDelays[currentFrame];
                shouldAdvance = elapsed > 0;
            }

            if (shouldAdvance) {
                ++currentFrame;
                frameEnteredAt = now;
            }
        }

        return frames[currentFrame];
    }

    public Image getBackgroundImage() {
        return backgroundImage;
    }

    public int getFrameCount() {
        return frames.length;
    }

    public void reset() {
        this.currentFrame = -1;
        this.frameEnteredAt = 0;
    }

    public void dispose() {
        for (Image img : frames) {
            if (img != null) {
                img.flush();
            }
        }
        if (backgroundImage != null) {
            backgroundImage.flush();
        }
    }

    protected void clear() {
        this.progressThresholds = new int[0];
        this.frameDelays = new int[0];
        this.frames = new Image[0];
    }

    public interface ImageLoader {
        Image loadImage(String resourceName);
    }
}
