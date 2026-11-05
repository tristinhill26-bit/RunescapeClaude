package com.wikigear;

import com.google.gson.Gson;
import com.wikigear.dps.GameData;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Loads the DPS calculator's equipment, monster and spell data. The latest data is downloaded from the wiki's DPS
 * calculator once a day and cached, with a copy bundled in the plugin for when it can't be reached.
 */
@Slf4j
@Singleton
public class DpsDataService
{
	private static final String[] FILES = { "equipment.json", "monsters.json", "spells.json", "equipment_aliases.json" };
	private static final String[] SOURCES = {
		"https://tools.runescape.wiki/osrs-dps/cdn/json/",
		"https://raw.githubusercontent.com/weirdgloop/osrs-dps-calc/main/cdn/json/",
	};
	private static final long MAX_AGE_MS = TimeUnit.DAYS.toMillis(1);
	private static final File CACHE_DIR = new File(RuneLite.RUNELITE_DIR, "wiki-gear-dps");

	@Inject
	private OkHttpClient httpClient;

	@Inject
	private Gson gson;

	@Nullable
	private volatile GameData data;

	/**
	 * The loaded data, or null while it's still loading
	 */
	@Nullable
	public GameData get() {
		return data;
	}

	/**
	 * Load the bundled or cached data, then fetch newer data if the cache is old. Blocks, so call it off the client thread.
	 */
	public void load() {
		try {
			var cached = readCache();
			install(cached != null ? cached : GameData.loadBundled(gson));
			if (cached == null || isStale())
				download();
		} catch (Exception ex) {
			log.warn("Unable to load DPS calculator data", ex);
		}
	}

	private void install(GameData loaded) {
		loaded.install();
		data = loaded;
	}

	private boolean isStale() {
		var file = new File(CACHE_DIR, FILES[0]);
		return !file.exists() || System.currentTimeMillis() - file.lastModified() > MAX_AGE_MS;
	}

	@Nullable
	private GameData readCache() {
		try {
			var text = new String[FILES.length];
			for (int i = 0; i < FILES.length; i++) {
				var file = new File(CACHE_DIR, FILES[i]);
				if (!file.exists())
					return null;
				text[i] = Files.readString(file.toPath(), StandardCharsets.UTF_8);
			}
			return GameData.parse(gson, text[0], text[1], text[2], text[3]);
		} catch (Exception ex) {
			log.debug("Ignoring unreadable DPS data cache", ex);
			return null;
		}
	}

	private void download() {
		for (String source : SOURCES) {
			try {
				var text = new String[FILES.length];
				for (int i = 0; i < FILES.length; i++)
					text[i] = fetch(source + FILES[i]);
				// Make sure it parses before replacing anything
				var fresh = GameData.parse(gson, text[0], text[1], text[2], text[3]);
				install(fresh);
				Files.createDirectories(CACHE_DIR.toPath());
				for (int i = 0; i < FILES.length; i++)
					Files.writeString(new File(CACHE_DIR, FILES[i]).toPath(), text[i], StandardCharsets.UTF_8);
				return;
			} catch (Exception ex) {
				log.debug("Unable to download DPS calculator data from {}", source, ex);
			}
		}
	}

	private String fetch(String url) throws IOException {
		var request = new Request.Builder().url(url).build();
		try (Response response = httpClient.newCall(request).execute()) {
			if (!response.isSuccessful() || response.body() == null)
				throw new IOException("HTTP " + response.code() + " for " + url);
			return response.body().string();
		}
	}
}
