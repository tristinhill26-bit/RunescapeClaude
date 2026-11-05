package com.fairyringlabels;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * Adds destination names next to fairy ring codes in menu text, e.g. "CIR" becomes "CIR - Farming Guild".
 */
class FairyRingLabeler {
	static final String SEPARATOR = " - ";

	/**
	 * A fairy ring code which isn't part of a longer word. Codes are uppercase, so lowercase colour tags never match.
	 */
	private static final Pattern CODE = Pattern.compile("(?<![A-Za-z])([A-D][I-L][P-S])(?![A-Za-z])");
	private static final Pattern LEADING_TAG = Pattern.compile("^\\(([^)]+)\\)\\s*");
	private static final Pattern LEADING_DIRECTION = Pattern.compile(
		"^(?:(?:north|south|east|west)(?:-(?:east|west))? of|near)\\s+(?:the\\s+)?",
		Pattern.CASE_INSENSITIVE
	);
	private static final Pattern TRAILING_DIRECTION = Pattern.compile(
		",\\s*(?:north|south|east|west)(?:-(?:east|west))? of .*$",
		Pattern.CASE_INSENSITIVE
	);

	private final Function<String, String> destinations;

	/**
	 * @param destinations looks up the destination name for a code, returning null for unknown codes
	 */
	FairyRingLabeler(Function<String, String> destinations) {
		this.destinations = destinations;
	}

	/**
	 * Label every fairy ring code in the text with its destination. Text that is already labelled is left unchanged.
	 */
	String label(String text) {
		if (text == null || text.isEmpty())
			return text;

		Matcher matcher = CODE.matcher(text);
		StringBuffer result = null;
		while (matcher.find()) {
			String destination = destinations.apply(matcher.group(1));
			if (destination == null || destination.isEmpty())
				continue;
			if (text.startsWith(SEPARATOR + destination, matcher.end()))
				continue; // Already labelled

			if (result == null)
				result = new StringBuffer();
			matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(1) + SEPARATOR + destination));
		}

		if (result == null)
			return text;
		matcher.appendTail(result);
		return result.toString();
	}

	/**
	 * Shorten RuneLite's descriptive destination names, e.g. "North-east of the Farming Guild" to "Farming Guild",
	 * and "(Island) South-east of Ardougne" to "Ardougne (island)".
	 */
	static String shorten(String destination) {
		String name = destination.trim();
		String tag = null;
		Matcher tagMatcher = LEADING_TAG.matcher(name);
		if (tagMatcher.find()) {
			tag = tagMatcher.group(1).toLowerCase(Locale.ROOT);
			name = name.substring(tagMatcher.end());
		}

		name = LEADING_DIRECTION.matcher(name).replaceFirst("");
		name = TRAILING_DIRECTION.matcher(name).replaceFirst("").trim();
		if (name.isEmpty())
			return destination;

		return tag == null ? name : name + " (" + tag + ")";
	}

	/**
	 * Parse custom names in the form "CIR=Konar, DKR=GE", separated by commas or new lines.
	 */
	static Map<String, String> parseCustomNames(@Nullable String text) {
		var names = new HashMap<String, String>();
		if (text == null)
			return names;
		for (String entry : text.split("[,\\n]")) {
			int equals = entry.indexOf('=');
			if (equals < 0)
				continue;
			String code = entry.substring(0, equals).trim().toUpperCase(Locale.ROOT);
			String name = entry.substring(equals + 1).trim();
			if (CODE.matcher(code).matches() && !name.isEmpty())
				names.put(code, name);
		}
		return names;
	}
}
