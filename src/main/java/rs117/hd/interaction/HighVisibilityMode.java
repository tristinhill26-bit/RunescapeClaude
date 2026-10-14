package rs117.hd.interaction;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import rs117.hd.HdPluginConfig;
import rs117.hd.config.FogDepthMode;

/**
 * Switches 117 HD to an easier to see preset, such as higher contrast, no fog and no shadows, and back again.
 * Every setting it changes is saved first, so switching back restores exactly what the player had.
 * It applies to the whole game equally, rather than highlighting anything in particular.
 */
@Slf4j
@Singleton
class HighVisibilityMode {
	private static final String KEY_ACTIVE = "highVisibilityActive";
	private static final String KEY_SAVED = "highVisibilitySaved";

	private static final String CONTRAST = "fContrast";
	private static final String SATURATION = "fSaturation";
	private static final String BRIGHTNESS = HdPluginConfig.KEY_BRIGHTNESS;
	private static final String NIGHT_BRIGHTNESS = HdPluginConfig.KEY_NIGHT_BRIGHTNESS;
	private static final String SHADOW_MODE = HdPluginConfig.KEY_SHADOW_MODE;
	private static final String FOG_DEPTH_MODE = "fogDepthMode";
	private static final String GROUND_FOG = "groundFog";
	private static final String COLOR_FILTER = HdPluginConfig.KEY_COLOR_FILTER;

	private static final String[] KEYS = {
		CONTRAST, SATURATION, BRIGHTNESS, NIGHT_BRIGHTNESS, SHADOW_MODE, FOG_DEPTH_MODE, GROUND_FOG, COLOR_FILTER
	};

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private InteractionLightsConfig config;

	@Inject
	private Gson gson;

	boolean isActive() {
		return Boolean.parseBoolean(configManager.getConfiguration(InteractionLightsConfig.CONFIG_GROUP, KEY_ACTIVE));
	}

	void toggle() {
		if (isActive()) {
			restore();
			message("High visibility off. Your 117 HD settings have been restored.");
		} else {
			save();
			apply();
			message("High visibility on. Press the key again to switch back.");
		}
	}

	/**
	 * Apply the preset, e.g. after its settings were changed while it's on
	 */
	void apply() {
		set(CONTRAST, config.highVisibilityContrast());
		set(SATURATION, config.highVisibilitySaturation());
		set(BRIGHTNESS, config.highVisibilityBrightness());
		set(NIGHT_BRIGHTNESS, config.highVisibilityNightBrightness());
		set(SHADOW_MODE, config.highVisibilityShadows());
		if (config.highVisibilityRemoveFog()) {
			set(FOG_DEPTH_MODE, FogDepthMode.NONE);
			set(GROUND_FOG, false);
		} else {
			restoreKey(FOG_DEPTH_MODE, loadSaved());
			restoreKey(GROUND_FOG, loadSaved());
		}
		set(COLOR_FILTER, config.highVisibilityColorFilter());
	}

	void restore() {
		var saved = loadSaved();
		for (String key : KEYS)
			restoreKey(key, saved);
		configManager.unsetConfiguration(InteractionLightsConfig.CONFIG_GROUP, KEY_SAVED);
		configManager.unsetConfiguration(InteractionLightsConfig.CONFIG_GROUP, KEY_ACTIVE);
	}

	private void save() {
		// Store the raw values, with null meaning the setting was left at its default
		var saved = new LinkedHashMap<String, String>();
		for (String key : KEYS)
			saved.put(key, configManager.getConfiguration(HdPluginConfig.CONFIG_GROUP, key));
		configManager.setConfiguration(InteractionLightsConfig.CONFIG_GROUP, KEY_SAVED, gson.toJson(saved));
		configManager.setConfiguration(InteractionLightsConfig.CONFIG_GROUP, KEY_ACTIVE, true);
	}

	private Map<String, String> loadSaved() {
		String json = configManager.getConfiguration(InteractionLightsConfig.CONFIG_GROUP, KEY_SAVED);
		if (json == null)
			return new LinkedHashMap<>();
		try {
			Map<String, String> saved = gson.fromJson(json, new TypeToken<Map<String, String>>() {}.getType());
			return saved == null ? new LinkedHashMap<>() : saved;
		} catch (Exception ex) {
			log.warn("Unable to read saved 117 HD settings", ex);
			return new LinkedHashMap<>();
		}
	}

	private void restoreKey(String key, Map<String, String> saved) {
		if (!saved.containsKey(key))
			return;
		String value = saved.get(key);
		if (value == null) {
			configManager.unsetConfiguration(HdPluginConfig.CONFIG_GROUP, key);
		} else {
			configManager.setConfiguration(HdPluginConfig.CONFIG_GROUP, key, value);
		}
	}

	private void set(String key, Object value) {
		configManager.setConfiguration(HdPluginConfig.CONFIG_GROUP, key, value);
	}

	private void message(String text) {
		clientThread.invoke(() -> {
			if (client.getGameState() == GameState.LOGGED_IN)
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", text, null);
		});
	}
}
