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
	/** Seconds left on a goading potion, 0 when none is active. */
	final int goading;

	private Stats(boolean loggedIn, String name, int hp, int maxHp, int prayer, int maxPrayer, boolean prayerActive,
		int goading) {
		this.loggedIn = loggedIn;
		this.name = name;
		this.hp = hp;
		this.maxHp = maxHp;
		this.prayer = prayer;
		this.maxPrayer = maxPrayer;
		this.prayerActive = prayerActive;
		this.goading = goading;
	}

	/** "1:24" */
	static String duration(int seconds) {
		return seconds / 60 + ":" + (seconds % 60 < 10 ? "0" : "") + seconds % 60;
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
			o.optBoolean("prayerActive", false),
			o.optInt("goading", 0));
	}

	boolean sameAs(Stats o) {
		return loggedIn == o.loggedIn && hp == o.hp && maxHp == o.maxHp && prayer == o.prayer
			&& maxPrayer == o.maxPrayer && prayerActive == o.prayerActive && goading == o.goading
			&& name.equals(o.name);
	}
}
