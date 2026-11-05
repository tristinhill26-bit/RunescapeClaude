package com.phonestats;

import java.util.ArrayList;
import java.util.List;

/**
 * A drop to tell the phone about: who dropped it and the items worth a ping. Kept free of RuneLite code so the JSON
 * can be tested on its own.
 */
public class DropMessage
{
	private final String source;
	private final List<String> items = new ArrayList<>();
	private long total;

	public DropMessage(String source)
	{
		this.source = source;
	}

	/**
	 * @param value the whole stack's value in coins
	 */
	public void add(String name, int quantity, long value, boolean tradeable)
	{
		items.add("{\"name\":\"" + StatsSnapshot.escape(name) + "\"" +
			",\"quantity\":" + quantity +
			",\"value\":" + value +
			",\"tradeable\":" + tradeable +
			"}");
		total += value;
	}

	public boolean isEmpty()
	{
		return items.isEmpty();
	}

	public String toJson()
	{
		return "{\"source\":\"" + StatsSnapshot.escape(source) + "\"" +
			",\"total\":" + total +
			",\"items\":[" + String.join(",", items) + "]}";
	}
}
