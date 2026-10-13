package com.wikigear;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;

/**
 * Picks the best owned item for each slot of a recommended setup.
 */
public class GearPicker
{
	private static final Pattern TAGS = Pattern.compile("<[^>]*>");
	/**
	 * Charges, doses, imbues and ornament kits, e.g. "Amulet of glory(4)", "Slayer helmet (i)" or "Dharok's helm 100"
	 */
	private static final Pattern VARIANT_SUFFIX = Pattern.compile("(?:\\s*\\([^)]*\\)|\\s+\\d+)$");

	@RequiredArgsConstructor
	public static class Pick
	{
		public final GearSlot slot;
		public final int itemId;
		/**
		 * The wiki's name for the item which was matched
		 */
		public final String wikiName;
		/**
		 * 1 for the wiki's top recommendation
		 */
		public final int rank;
		public final int rankCount;
	}

	/**
	 * An index from item names to owned item IDs, which also matches charged, imbued and degraded variants.
	 */
	public static class OwnedItems
	{
		private final Map<String, Integer> exact = new HashMap<>();
		private final Map<String, Integer> loose = new HashMap<>();

		/**
		 * @param baseName name of the item's base variant, if it has one, e.g. "Slayer helmet" for "Slayer helmet (i)"
		 */
		public void add(int itemId, String name, @Nullable String baseName) {
			exact.putIfAbsent(normalize(name), itemId);
			if (baseName != null)
				loose.putIfAbsent(normalize(baseName), itemId);
			loose.putIfAbsent(stripVariant(normalize(name)), itemId);
		}

		@Nullable
		public Integer find(String wikiName) {
			String name = normalize(wikiName);
			Integer id = exact.get(name);
			return id != null ? id : loose.get(name);
		}

		public boolean isEmpty() {
			return exact.isEmpty();
		}
	}

	private GearPicker() {
	}

	public static Map<GearSlot, Pick> pick(GearSetup setup, OwnedItems owned) {
		var picks = new EnumMap<GearSlot, Pick>(GearSlot.class);
		for (var entry : setup.getSlots().entrySet()) {
			List<List<String>> ranks = entry.getValue();
			search:
			for (int rank = 0; rank < ranks.size(); rank++) {
				for (String name : ranks.get(rank)) {
					Integer itemId = owned.find(name);
					if (itemId != null) {
						picks.put(entry.getKey(), new Pick(entry.getKey(), itemId, name, rank + 1, ranks.size()));
						break search;
					}
				}
			}
		}
		return picks;
	}

	static String normalize(String name) {
		return TAGS.matcher(name).replaceAll("")
			.replace(' ', ' ')
			.replace('_', ' ')
			.replace('’', '\'')
			.replaceAll("\\s+", " ")
			.trim()
			.toLowerCase(Locale.ROOT);
	}

	static String stripVariant(String name) {
		String stripped = name;
		String previous;
		do {
			previous = stripped;
			stripped = VARIANT_SUFFIX.matcher(stripped).replaceFirst("").trim();
		} while (!stripped.equals(previous) && !stripped.isEmpty());
		return stripped.isEmpty() ? name : stripped;
	}
}
