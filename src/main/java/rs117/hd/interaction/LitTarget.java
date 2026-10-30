package rs117.hd.interaction;

import java.awt.Color;
import rs117.hd.scene.lights.Light;
import rs117.hd.scene.lights.LightDefinition;
import rs117.hd.utils.ColorUtils;

/**
 * A marked target along with the light it casts, and timing for fading it in and out.
 */
class LitTarget {
	final MarkerTarget target;
	final Light light;
	final float[] color = new float[3];
	final float twinklePhase = (float) (Math.random() * Math.PI * 2);

	Color awtColor;
	float baseStrength;
	float baseRadius;

	double startTime;
	double endTime = Double.POSITIVE_INFINITY;
	float fadeInDuration;
	float fadeOutDuration;

	/**
	 * Fade in/out opacity for the current frame
	 */
	float alpha;
	/**
	 * Final light strength multiplier for the current frame, including alpha, flashes and twinkling
	 */
	float intensity;

	LitTarget(MarkerTarget target, double startTime, float fadeInDuration, float fadeOutDuration) {
		this.target = target;
		this.startTime = startTime;
		this.fadeInDuration = fadeInDuration;
		this.fadeOutDuration = fadeOutDuration;

		var def = new LightDefinition();
		def.description = "Interaction light";
		def.fadeInDuration = 0;
		def.fadeOutDuration = 0;
		def.visibleFromOtherPlanes = true;
		def.normalize();
		light = new Light(def);
		light.external = true;
	}

	void setColor(Color color) {
		awtColor = color;
		float[] linear = ColorUtils.rgb(color);
		System.arraycopy(linear, 0, this.color, 0, 3);
	}

	float updateAlpha(double now) {
		if (now < startTime) {
			alpha = 0;
		} else {
			alpha = fadeInDuration <= 0 ? 1 : (float) Math.min(1, (now - startTime) / fadeInDuration);
			if (now > endTime)
				alpha *= fadeOutDuration <= 0 ? 0 : (float) Math.max(0, 1 - (now - endTime) / fadeOutDuration);
		}
		return alpha;
	}

	void fadeOut(double now) {
		if (endTime > now)
			endTime = now;
	}

	boolean isFadingOut(double now) {
		return now >= endTime;
	}

	boolean isDone(double now) {
		return now > endTime + fadeOutDuration;
	}
}
