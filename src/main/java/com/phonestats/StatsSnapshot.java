package com.phonestats;

import lombok.Value;

/**
 * What the phone shows. Kept free of RuneLite code so the JSON can be tested on its own.
 */
@Value
public class StatsSnapshot
{
	public static final StatsSnapshot LOGGED_OUT = new StatsSnapshot(false, "", 0, 0, 0, 0, false, 0);

	boolean loggedIn;
	String name;
	int hp;
	int maxHp;
	int prayer;
	int maxPrayer;
	boolean prayerActive;
	// Seconds left on a goading potion, 0 when none is active
	int goadingSeconds;

	public String toJson()
	{
		return "{\"loggedIn\":" + loggedIn +
			",\"name\":\"" + escape(name) + "\"" +
			",\"hp\":" + hp +
			",\"maxHp\":" + maxHp +
			",\"prayer\":" + prayer +
			",\"maxPrayer\":" + maxPrayer +
			",\"prayerActive\":" + prayerActive +
			",\"goading\":" + goadingSeconds +
			"}";
	}

	static String escape(String text)
	{
		if (text == null)
		{
			return "";
		}
		StringBuilder sb = new StringBuilder(text.length());
		for (char c : text.toCharArray())
		{
			switch (c)
			{
				case '"':
					sb.append("\\\"");
					break;
				case '\\':
					sb.append("\\\\");
					break;
				default:
					if (c < 0x20)
					{
						sb.append(String.format("\\u%04x", (int) c));
					}
					else
					{
						sb.append(c);
					}
			}
		}
		return sb.toString();
	}
}
