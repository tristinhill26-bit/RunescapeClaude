package com.wikigear;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decides whether a monster or activity takes place in the Wilderness, where blighted items can be used
 */
public class WildernessDetector
{
	/**
	 * Wilderness bosses and areas, matched against the start of the main page's name
	 */
	private static final List<String> KNOWN = List.of(
		"callisto", "artio", "vet'ion", "calvar'ion", "venenatis", "spindel",
		"chaos elemental", "chaos fanatic", "crazy archaeologist", "scorpia",
		"revenant", "lava dragon", "rogue", "mage arena"
	);
	private static final Pattern WILDERNESS = Pattern.compile("wilderness", Pattern.CASE_INSENSITIVE);
	/**
	 * Pages about other content can mention the Wilderness in passing, so it has to come up a few times
	 */
	private static final int MIN_MENTIONS = 3;

	private WildernessDetector() {
	}

	public static boolean isWilderness(String title, String wikitext) {
		String name = WikiClient.mainPage(title).toLowerCase(Locale.ROOT);
		if (name.contains("wilderness"))
			return true;
		for (var known : KNOWN)
			if (name.startsWith(known))
				return true;

		if (wikitext == null)
			return false;
		Matcher matcher = WILDERNESS.matcher(wikitext);
		int mentions = 0;
		while (matcher.find())
			if (++mentions >= MIN_MENTIONS)
				return true;
		return false;
	}

	public static boolean isBlighted(String itemName) {
		return itemName.toLowerCase(Locale.ROOT).startsWith("blighted");
	}
}
