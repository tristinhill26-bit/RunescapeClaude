package rs117.hd;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import rs117.hd.config.AntiAliasingMode;
import rs117.hd.config.ColorFilter;
import rs117.hd.config.DefaultSkyColor;
import rs117.hd.config.GroundBlending;
import rs117.hd.config.ShadowFiltering;
import rs117.hd.config.ShadowMode;
import rs117.hd.config.ShadowResolution;

import static rs117.hd.HdPluginConfig.*;

/**
 * A one-click preset that makes 117 HD look like 2008-2009 era RuneScape HD: a long view distance,
 * the warm tan distance haze, soft shadows, anti-aliasing and the Classic HD color grade.
 * Every setting it changes is saved first, so turning it off restores exactly what the player had.
 */
@Slf4j
@Singleton
public class ClassicHdLook {
	private static final String KEY_SAVED = "classicHdLookSaved";

	private static final Map<String, Object> PRESET = new LinkedHashMap<>();

	static {
		PRESET.put("drawDistance", 100);
		PRESET.put(KEY_EXPANDED_MAP_LOADING_CHUNKS, 5);
		PRESET.put(KEY_ANTI_ALIASING_MODE, AntiAliasingMode.MSAA_8);
		PRESET.put(KEY_SHADOW_MODE, ShadowMode.DETAILED);
		PRESET.put(KEY_SHADOW_RESOLUTION, ShadowResolution.RES_4096);
		PRESET.put(KEY_SHADOW_FILTERING, ShadowFiltering.PCSS);
		PRESET.put(KEY_GROUND_BLENDING, GroundBlending.ON);
		PRESET.put(KEY_GROUND_TEXTURES, true);
		PRESET.put(KEY_MODEL_TEXTURES, true);
		PRESET.put("defaultSkyColor", DefaultSkyColor.HD2008);
		PRESET.put("groundFog", true);
		PRESET.put(KEY_WATER_SPECULAR_REFLECTIONS, true);
		PRESET.put(KEY_FLAT_SHADING, false);
		PRESET.put(KEY_COLOR_FILTER, ColorFilter.CLASSIC_HD);
	}

	@Inject
	private ConfigManager configManager;

	@Inject
	private Gson gson;

	/**
	 * Called when the Classic HD look setting is switched on or off
	 */
	public void onToggled(boolean enabled) {
		boolean hasSaved = configManager.getConfiguration(CONFIG_GROUP, KEY_SAVED) != null;
		if (enabled) {
			// Only save once, so switching on twice in a row can't overwrite the player's own settings with the preset
			if (!hasSaved)
				save();
			for (var entry : PRESET.entrySet())
				configManager.setConfiguration(CONFIG_GROUP, entry.getKey(), entry.getValue());
		} else if (hasSaved) {
			restore();
		}
	}

	private void save() {
		// Store the raw values, with null meaning the setting was left at its default
		var saved = new LinkedHashMap<String, String>();
		for (String key : PRESET.keySet())
			saved.put(key, configManager.getConfiguration(CONFIG_GROUP, key));
		configManager.setConfiguration(CONFIG_GROUP, KEY_SAVED, gson.toJson(saved));
	}

	private void restore() {
		Map<String, String> saved;
		try {
			saved = gson.fromJson(
				configManager.getConfiguration(CONFIG_GROUP, KEY_SAVED),
				new TypeToken<Map<String, String>>() {}.getType()
			);
		} catch (Exception ex) {
			log.warn("Unable to read the settings saved before Classic HD look was turned on", ex);
			saved = null;
		}

		if (saved != null) {
			for (var entry : saved.entrySet()) {
				if (entry.getValue() == null) {
					configManager.unsetConfiguration(CONFIG_GROUP, entry.getKey());
				} else {
					configManager.setConfiguration(CONFIG_GROUP, entry.getKey(), entry.getValue());
				}
			}
		}
		configManager.unsetConfiguration(CONFIG_GROUP, KEY_SAVED);
	}
}
