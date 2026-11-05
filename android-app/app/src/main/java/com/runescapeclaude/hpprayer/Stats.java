package com.runescapeclaude.hpprayer;

import org.json.JSONException;
import org.json.JSONObject;

/** One update from the RuneLite plugin. */
final class Stats {
	final boolean loggedIn;
	final String name;
	final int hp;
	final int maxHp;
	final int prayer;
	final int maxPrayer;
	final boolean prayerActive;

	private Stats(boolean loggedIn, String name, int hp, int maxHp, int prayer, int maxPrayer, boolean prayerActive) {
		this.loggedIn = loggedIn;
		this.name = name;
		this.hp = hp;
		this.maxHp = maxHp;
		this.prayer = prayer;
		this.maxPrayer = maxPrayer;
		this.prayerActive = prayerActive;
	}

	static Stats parse(String json) throws JSONException {
		JSONObject o = new JSONObject(json);
		return new Stats(
			o.optBoolean("loggedIn", false),
			o.optString("name", ""),
			o.optInt("hp", 0),
			o.optInt("maxHp", 0),
			o.optInt("prayer", 0),
			o.optInt("maxPrayer", 0),
			o.optBoolean("prayerActive", false));
	}
}
