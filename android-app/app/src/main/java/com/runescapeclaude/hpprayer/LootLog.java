package com.runescapeclaude.hpprayer;

import android.content.Context;
import android.util.AtomicFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Every drop the plugin has sent, saved on the phone so the loot summary survives closing the app. The plugin
 * replays its whole session when the phone connects, so drops are remembered by ID and only counted once, even
 * after a reset.
 */
final class LootLog {
	interface Listener {
		void onLootChanged();
	}

	static final class Item {
		final String name;
		final long quantity;
		final long value;
		final boolean tradeable;

		Item(String name, long quantity, long value, boolean tradeable) {
			this.name = name;
			this.quantity = quantity;
			this.value = value;
			this.tradeable = tradeable;
		}
	}

	static final class Drop {
		final String id;
		final long time;
		final String source;
		final List<Item> items;
		final long total;

		Drop(String id, long time, String source, List<Item> items, long total) {
			this.id = id;
			this.time = time;
			this.source = source;
			this.items = items;
			this.total = total;
		}
	}

	/** A source (NPC or player) or an item, added up. */
	static final class Tally {
		final String name;
		long count;
		long value;
		boolean tradeable = true;

		Tally(String name) {
			this.name = name;
		}
	}

	private static final int MAX_DROPS = 10000;
	private static final int MAX_SEEN_IDS = 20000;
	private static LootLog instance;

	private final AtomicFile file;
	private final List<Drop> drops = new ArrayList<>();
	private final LinkedHashSet<String> seenIds = new LinkedHashSet<>();
	private final List<Listener> listeners = new CopyOnWriteArrayList<>();

	static synchronized LootLog get(Context context) {
		if (instance == null) {
			instance = new LootLog(new File(context.getApplicationContext().getFilesDir(), "loot.json"));
		}
		return instance;
	}

	private LootLog(File path) {
		file = new AtomicFile(path);
		load();
	}

	void addListener(Listener listener) {
		listeners.add(listener);
	}

	void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	/** Adds a drop from the plugin's JSON, unless it's already been counted. */
	void add(String json) {
		synchronized (this) {
			try {
				Drop drop = parse(new JSONObject(json));
				if (drop.id.isEmpty() || !seenIds.add(drop.id)) {
					return;
				}
				drops.add(drop);
				trim();
				save();
			} catch (JSONException e) {
				return;
			}
		}
		notifyListeners();
	}

	/** Forgets the totals. Drops already counted stay remembered, so a reconnect doesn't bring them back. */
	void clear() {
		synchronized (this) {
			drops.clear();
			save();
		}
		notifyListeners();
	}

	synchronized int dropCount() {
		return drops.size();
	}

	synchronized long totalValue() {
		long total = 0;
		for (Drop drop : drops) {
			total += drop.total;
		}
		return total;
	}

	synchronized long firstTime() {
		return drops.isEmpty() ? 0 : drops.get(0).time;
	}

	/** Sources by total value, with how many drops each gave. */
	synchronized List<Tally> bySource() {
		Map<String, Tally> tallies = new HashMap<>();
		for (Drop drop : drops) {
			Tally tally = tallies.computeIfAbsent(drop.source, Tally::new);
			tally.count++;
			tally.value += drop.total;
		}
		return sorted(tallies);
	}

	/** Items by total value, with how many of each. */
	synchronized List<Tally> byItem() {
		Map<String, Tally> tallies = new HashMap<>();
		for (Drop drop : drops) {
			for (Item item : drop.items) {
				Tally tally = tallies.computeIfAbsent(item.name, Tally::new);
				tally.count += item.quantity;
				tally.value += item.value;
				tally.tradeable &= item.tradeable;
			}
		}
		return sorted(tallies);
	}

	/** The newest drops first. */
	synchronized List<Drop> recent(int count) {
		List<Drop> recent = new ArrayList<>(drops.subList(Math.max(0, drops.size() - count), drops.size()));
		Collections.reverse(recent);
		return recent;
	}

	private static List<Tally> sorted(Map<String, Tally> tallies) {
		List<Tally> list = new ArrayList<>(tallies.values());
		// Untradeables are worth 0 coins but are often the best drops, so they go right after anything valuable
		list.sort((a, b) -> {
			if (a.value != b.value) {
				return Long.compare(b.value, a.value);
			}
			if (a.tradeable != b.tradeable) {
				return a.tradeable ? 1 : -1;
			}
			return Long.compare(b.count, a.count);
		});
		return list;
	}

	private void notifyListeners() {
		for (Listener listener : listeners) {
			listener.onLootChanged();
		}
	}

	private void trim() {
		while (drops.size() > MAX_DROPS) {
			drops.remove(0);
		}
		Iterator<String> oldest = seenIds.iterator();
		while (seenIds.size() > MAX_SEEN_IDS && oldest.hasNext()) {
			oldest.next();
			oldest.remove();
		}
	}

	private static Drop parse(JSONObject o) throws JSONException {
		JSONArray array = o.getJSONArray("items");
		List<Item> items = new ArrayList<>();
		long total = 0;
		for (int i = 0; i < array.length(); i++) {
			JSONObject item = array.getJSONObject(i);
			long value = item.optLong("value");
			items.add(new Item(item.getString("name"), item.optLong("quantity", 1), value,
				item.optBoolean("tradeable", true)));
			total += value;
		}
		return new Drop(o.optString("id", ""), o.optLong("time", System.currentTimeMillis()),
			o.optString("source", "?"), items, total);
	}

	private static JSONObject toJson(Drop drop) throws JSONException {
		JSONArray items = new JSONArray();
		for (Item item : drop.items) {
			items.put(new JSONObject()
				.put("name", item.name)
				.put("quantity", item.quantity)
				.put("value", item.value)
				.put("tradeable", item.tradeable));
		}
		return new JSONObject()
			.put("id", drop.id)
			.put("time", drop.time)
			.put("source", drop.source)
			.put("items", items);
	}

	private void load() {
		try (FileInputStream in = file.openRead()) {
			JSONObject root = new JSONObject(new String(readAll(in), StandardCharsets.UTF_8));
			JSONArray savedDrops = root.getJSONArray("drops");
			for (int i = 0; i < savedDrops.length(); i++) {
				drops.add(parse(savedDrops.getJSONObject(i)));
			}
			JSONArray ids = root.getJSONArray("seenIds");
			for (int i = 0; i < ids.length(); i++) {
				seenIds.add(ids.getString(i));
			}
		} catch (IOException | JSONException e) {
			// Nothing saved yet
		}
	}

	private void save() {
		FileOutputStream out = null;
		try {
			JSONArray savedDrops = new JSONArray();
			for (Drop drop : drops) {
				savedDrops.put(toJson(drop));
			}
			JSONObject root = new JSONObject()
				.put("drops", savedDrops)
				.put("seenIds", new JSONArray(seenIds));
			out = file.startWrite();
			out.write(root.toString().getBytes(StandardCharsets.UTF_8));
			file.finishWrite(out);
		} catch (IOException | JSONException e) {
			if (out != null) {
				file.failWrite(out);
			}
		}
	}

	private static byte[] readAll(FileInputStream in) throws IOException {
		java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		int read;
		while ((read = in.read(buffer)) != -1) {
			bytes.write(buffer, 0, read);
		}
		return bytes.toByteArray();
	}
}
