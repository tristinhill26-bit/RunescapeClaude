package com.wikigear;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;

/**
 * Picks the best owned item for each slot of a recommended setup.
 * <ol>
 *   <li>Each slot gets the highest ranked item from the wiki's table which the player owns.</li>
 *   <li>Gear which only works as a full set, like Void, is completed or dropped.</li>
 *   <li>Slots that are still empty can be filled with the player's best item for the setup's combat style.</li>
 * </ol>
 */
public class GearPicker
{
	private static final Pattern TAGS = Pattern.compile("<[^>]*>");
	/**
	 * Charges, doses, imbues and ornament kits, e.g. "Amulet of glory(4)", "Slayer helmet (i)" or "Dharok's helm 100"
	 */
	private static final Pattern VARIANT_SUFFIX = Pattern.compile("(?:\\s*\\([^)]*\\)|\\s+\\d+)$");

	/**
	 * Slots which can be filled from the player's own items. Ammo is left out since it has to suit the weapon.
	 */
	private static final GearSlot[] FILLABLE_SLOTS = {
		GearSlot.HEAD, GearSlot.CAPE, GearSlot.NECK, GearSlot.BODY, GearSlot.LEGS,
		GearSlot.HANDS, GearSlot.FEET, GearSlot.RING, GearSlot.WEAPON, GearSlot.SHIELD
	};

	public enum Source
	{
		/**
		 * Recommended on the wiki
		 */
		WIKI,
		/**
		 * Added to complete a set the wiki recommends
		 */
		SET,
		/**
		 * Not recommended for this slot, but the best the player owns for the combat style
		 */
		FILLED
	}

	@RequiredArgsConstructor
	public static class Pick
	{
		public final GearSlot slot;
		public final int itemId;
		/**
		 * The wiki's name for the item, or the item's own name if it wasn't on the wiki's list
		 */
		public final String name;
		/**
		 * 1 for the wiki's top recommendation, 0 if the item wasn't ranked by the wiki
		 */
		public final int rank;
		public final int rankCount;
		public final Source source;
	}

	@RequiredArgsConstructor
	public static class Result
	{
		public final Map<GearSlot, Pick> picks;
		/**
		 * Things worth telling the player, e.g. a set that was dropped because a piece is missing
		 */
		public final List<String> notes;
		@Nullable
		public final CombatStyle style;
	}

	@RequiredArgsConstructor
	public static class OwnedItem
	{
		public final int itemId;
		public final String name;
		@Nullable
		public final ItemBonuses bonuses;
	}

	/**
	 * An index from item names to owned items, which also matches charged, imbued and degraded variants.
	 */
	public static class OwnedItems
	{
		private final Map<String, OwnedItem> exact = new HashMap<>();
		private final Map<String, OwnedItem> loose = new HashMap<>();
		private final List<OwnedItem> all = new ArrayList<>();

		/**
		 * @param baseName name of the item's base variant, if it has one, e.g. "Slayer helmet" for "Slayer helmet (i)"
		 * @param bonuses  the item's equipment stats, or null if it can't be equipped
		 */
		public void add(int itemId, String name, @Nullable String baseName, @Nullable ItemBonuses bonuses) {
			var item = new OwnedItem(itemId, name, bonuses);
			if (exact.putIfAbsent(normalize(name), item) == null)
				all.add(item);
			if (baseName != null)
				loose.putIfAbsent(normalize(baseName), item);
			loose.putIfAbsent(stripVariant(normalize(name)), item);
		}

		public void add(int itemId, String name, @Nullable String baseName) {
			add(itemId, name, baseName, null);
		}

		@Nullable
		public OwnedItem findItem(String wikiName) {
			String name = normalize(wikiName);
			OwnedItem item = exact.get(name);
			return item != null ? item : loose.get(name);
		}

		@Nullable
		public Integer find(String wikiName) {
			OwnedItem item = findItem(wikiName);
			return item == null ? null : item.itemId;
		}

		public boolean isEmpty() {
			return exact.isEmpty();
		}
	}

	private GearPicker() {
	}

	public static Result pick(GearSetup setup, OwnedItems owned, boolean fillEmptySlots) {
		var notes = new ArrayList<String>();
		var excluded = new HashSet<String>();
		var picks = pickFromWiki(setup, owned, excluded);

		var weaponBonuses = new EnumMap<GearSlot, ItemBonuses>(GearSlot.class);
		for (var slot : new GearSlot[] { GearSlot.WEAPON, GearSlot.TWO_HANDED }) {
			var pick = picks.get(slot);
			var item = pick == null ? null : owned.findItem(pick.name);
			if (item != null && item.bonuses != null)
				weaponBonuses.put(slot, item.bonuses);
		}
		CombatStyle style = CombatStyle.infer(setup.getLabel(), weaponBonuses);

		for (var set : GearSet.values()) {
			if (picks.values().stream().noneMatch(pick -> set.contains(pick.name)))
				continue;

			var pieces = set.findPieces(owned, style);
			if (pieces != null) {
				boolean completed = false;
				for (var entry : pieces.entrySet()) {
					var current = picks.get(entry.getKey());
					var piece = entry.getValue();
					if (current == null || current.itemId != piece.itemId) {
						picks.put(entry.getKey(), new Pick(entry.getKey(), piece.itemId, piece.name, 0, 0, Source.SET));
						completed = true;
					}
				}
				if (completed)
					notes.add("Added the rest of your " + set.getName() + ", since it only works as a full set.");
			} else {
				// Wearing part of the set is worse than not wearing it, so fall back to the next recommendations
				notes.add(set.getName() + " was skipped since you don't own the whole set.");
				excluded.addAll(set.allPieceNames());
				picks = pickFromWiki(setup, owned, excluded);
			}
		}

		if (fillEmptySlots && style != null)
			fillEmptySlots(picks, owned, style, excluded);

		return new Result(picks, notes, style);
	}

	private static Map<GearSlot, Pick> pickFromWiki(GearSetup setup, OwnedItems owned, Set<String> excluded) {
		var picks = new EnumMap<GearSlot, Pick>(GearSlot.class);
		for (var entry : setup.getSlots().entrySet()) {
			List<List<String>> ranks = entry.getValue();
			search:
			for (int rank = 0; rank < ranks.size(); rank++) {
				for (String name : ranks.get(rank)) {
					String normalized = normalize(name);
					if (excluded.contains(normalized) || excluded.contains(stripVariant(normalized)))
						continue;
					Integer itemId = owned.find(name);
					if (itemId != null) {
						picks.put(entry.getKey(), new Pick(entry.getKey(), itemId, name, rank + 1, ranks.size(), Source.WIKI));
						break search;
					}
				}
			}
		}
		return picks;
	}

	private static void fillEmptySlots(Map<GearSlot, Pick> picks, OwnedItems owned, CombatStyle style, Set<String> excluded) {
		var used = new HashSet<Integer>();
		for (var pick : picks.values())
			used.add(pick.itemId);

		for (var slot : FILLABLE_SLOTS) {
			if (picks.containsKey(slot))
				continue;
			// Don't add a one-handed weapon next to a two-handed one, nor a shield to only a two-handed weapon
			if (slot == GearSlot.WEAPON && picks.containsKey(GearSlot.TWO_HANDED))
				continue;
			if (slot == GearSlot.SHIELD && picks.containsKey(GearSlot.TWO_HANDED) && !picks.containsKey(GearSlot.WEAPON))
				continue;

			OwnedItem best = null;
			double bestScore = 0;
			for (var item : owned.all) {
				if (item.bonuses == null || item.bonuses.getSlot() != slot.getEquipmentSlot() || used.contains(item.itemId))
					continue;
				if (excluded.contains(stripVariant(normalize(item.name))))
					continue;
				double score = style.score(item.bonuses);
				if (score > bestScore) {
					best = item;
					bestScore = score;
				}
			}

			if (best != null) {
				GearSlot target = slot == GearSlot.WEAPON && best.bonuses.isTwoHanded() ? GearSlot.TWO_HANDED : slot;
				picks.put(target, new Pick(target, best.itemId, best.name, 0, 0, Source.FILLED));
				used.add(best.itemId);
			}
		}
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
