package rs117.hd.scene.lights;

import java.util.List;
import javax.annotation.Nonnull;
import rs117.hd.scene.SceneContext;

/**
 * Supplies lights which aren't driven by lights.json, such as the lights spawned by the interaction lights plugin.
 * Called on the client thread once per frame, before lights are culled and sorted.
 */
public interface ExternalLightProvider {
	/**
	 * Append this frame's lights to the destination list. Each light must be flagged as {@link Light#external},
	 * and is removed from the list again before the next call, so providers should reuse their {@link Light} instances.
	 * Strength and radius are read from the light's {@link LightDefinition} every frame.
	 */
	void addLights(@Nonnull SceneContext sceneContext, @Nonnull List<Light> destination);

	/**
	 * Multiplier applied to the strength of regular scene lights, such as torches and fires.
	 */
	default float getSceneLightStrengthScale() {
		return 1;
	}

	/**
	 * Multiplier applied to the radius of regular scene lights, such as torches and fires.
	 */
	default float getSceneLightRadiusScale() {
		return 1;
	}
}
