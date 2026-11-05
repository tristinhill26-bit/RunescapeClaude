package com.wikigear;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * Extracts {{Recommended equipment}} tables from wiki markup.
 * <p>
 * The template takes a style, and numbered parameters per slot from best to worst, e.g.
 * {@code {{Recommended equipment|style=Ranged|head1={{plink|Armadyl helmet}}|head2={{plink|Blessed coif}}}}}.
 */
public class RecommendedEquipmentParser
{
	private static final Pattern TEMPLATE_START = Pattern.compile("\\{\\{\\s*recommended[ _]equipment\\s*(?=\\||}})", Pattern.CASE_INSENSITIVE);
	private static final Pattern INVENTORY_START = Pattern.compile("\\{\\{\\s*(inventory|loadout)\\s*(?=\\||}})", Pattern.CASE_INSENSITIVE);
	/**
	 * Equipment parameters of {{Loadout}}, which shows a whole setup with its inventory
	 */
	private static final String[][] LOADOUT_SLOTS = {
		{ "head", "head" }, { "cape", "cape" }, { "neck", "neck" }, { "ammo", "ammo" }, { "weapon", "weapon" },
		{ "shield", "shield" }, { "torso", "body" }, { "body", "body" }, { "legs", "legs" }, { "gloves", "hands" },
		{ "hands", "hands" }, { "boots", "feet" }, { "feet", "feet" }, { "ring", "ring" },
	};
	private static final Pattern SLOT_PARAM = Pattern.compile("^([a-z0-9]+?)(\\d+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern HEADING = Pattern.compile("(?m)^(={2,6})\\s*([^=].*?)\\s*\\1\\s*$");
	private static final Pattern TAB_LABEL = Pattern.compile("(?:<tabber>|\\|-\\|)\\s*([^=\\n|<]+?)\\s*=");
	private static final Pattern LINK = Pattern.compile("\\[\\[(?:[^|\\]]*\\|)?([^\\]]*)]]");
	private static final Pattern MARKUP = Pattern.compile("'{2,}|<[^>]*>");

	private RecommendedEquipmentParser() {
	}

	public static List<GearSetup> parse(String wikitext) {
		var setups = new ArrayList<GearSetup>();
		var contexts = new ArrayList<String>();
		var positions = new ArrayList<Integer>();
		if (wikitext == null)
			return setups;

		Matcher matcher = TEMPLATE_START.matcher(wikitext);
		int searchFrom = 0;
		while (matcher.find(searchFrom)) {
			int start = matcher.start();
			int end = findClosing(wikitext, start, "{{", "}}");
			if (end < 0)
				break;
			searchFrom = end;

			var params = parseParams(wikitext.substring(start + 2, end - 2));
			var slots = parseSlots(params);
			if (slots.isEmpty())
				continue;

			String context = findContext(wikitext, start);
			String style = cleanText(params.get("style"));
			String label = style != null ? style : context != null ? context : "Setup " + (setups.size() + 1);
			setups.add(new GearSetup(label, slots));
			contexts.add(context);
			positions.add(start);
		}

		var inventories = findInventories(wikitext);
		attachInventories(setups, positions, inventories);
		addLoadoutSetups(setups, contexts, inventories);
		return disambiguate(setups, contexts);
	}

	/**
	 * An {{Inventory}}, or the inventory of a {{Loadout}} along with its worn gear
	 */
	private static final class InventorySource
	{
		final List<String> items;
		final int position;
		/**
		 * The tab or section it's in, e.g. "Magic Only"
		 */
		@Nullable
		final String context;
		/**
		 * The worn gear, for a {{Loadout}}
		 */
		final Map<GearSlot, List<List<String>>> slots;

		InventorySource(List<String> items, int position, @Nullable String context, Map<GearSlot, List<List<String>>> slots) {
			this.items = items;
			this.position = position;
			this.context = context;
			this.slots = slots;
		}
	}

	private static List<InventorySource> findInventories(String wikitext) {
		var sources = new ArrayList<InventorySource>();
		Matcher matcher = INVENTORY_START.matcher(wikitext);
		int searchFrom = 0;
		while (matcher.find(searchFrom)) {
			int start = matcher.start();
			int end = findClosing(wikitext, start, "{{", "}}");
			if (end < 0)
				break;
			searchFrom = end;

			String body = wikitext.substring(start + 2, end - 2);
			var items = parseInventory(body);
			if (items.stream().allMatch(item -> item == null))
				continue;
			var slots = matcher.group(1).equalsIgnoreCase("loadout") ? parseLoadoutSlots(parseParams(body)) : GearSetup.emptySlots();
			sources.add(new InventorySource(items, start, findContext(wikitext, start), slots));
		}
		return sources;
	}

	/**
	 * The worn gear of a {{Loadout}}, each slot holding a single item, like head = Ahrim's hood
	 */
	private static Map<GearSlot, List<List<String>>> parseLoadoutSlots(Map<String, String> params) {
		var slots = GearSetup.emptySlots();
		for (String[] mapping : LOADOUT_SLOTS) {
			String value = params.get(mapping[0]);
			if (value == null || value.isEmpty())
				continue;
			var items = value.contains("{{") || value.contains("[[") ? extractItems(value) : new ArrayList<String>();
			if (items.isEmpty()) {
				String item = cleanText(value);
				if (item != null && !item.isEmpty())
					items.add(item);
			}
			if (items.isEmpty())
				continue;
			GearSlot slot = GearSlot.fromWikiName(mapping[1]);
			slots.computeIfAbsent(slot, k -> new ArrayList<>()).add(items);
		}
		return slots;
	}

	/**
	 * The combat styles a name mentions, e.g. {magic, ranged} for "Max Magic & Ranged"
	 */
	private static Set<String> stylesIn(@Nullable String text) {
		var styles = new HashSet<String>();
		if (text == null)
			return styles;
		String lower = text.toLowerCase(Locale.ROOT);
		if (lower.contains("magic") || lower.contains("mage"))
			styles.add("magic");
		if (lower.contains("rang"))
			styles.add("ranged");
		if (lower.contains("melee") || lower.contains("stab") || lower.contains("slash") || lower.contains("crush"))
			styles.add("melee");
		return styles;
	}

	/**
	 * How well an inventory's tab or section suits a setup: 3 for the same name, like "Magic" and "Magic Only",
	 * 2 for the same styles, like "Magic (Diary kill)" and "Magic Only", 1 for a hybrid including the style, and 0
	 * for no match
	 */
	private static int matchScore(String label, @Nullable String context) {
		if (context == null)
			return 0;
		String a = label.toLowerCase(Locale.ROOT).trim();
		String b = context.toLowerCase(Locale.ROOT).replaceAll("\\bonly\\b", "").trim();
		if (a.equals(b))
			return 3;
		var labelStyles = stylesIn(label);
		var contextStyles = stylesIn(context);
		if (labelStyles.isEmpty())
			return 0;
		if (labelStyles.equals(contextStyles))
			return 2;
		return contextStyles.containsAll(labelStyles) ? 1 : 0;
	}

	/**
	 * Give each equipment table an inventory: the one in a tab or section named for it, e.g. "Magic Only" for the
	 * Magic table, or else the one that follows it before the next table, or else the nearest one, preferring the
	 * first one after it, since pages often list every table first and the inventories after.
	 */
	private static void attachInventories(List<GearSetup> setups, List<Integer> positions, List<InventorySource> inventories) {
		if (inventories.isEmpty())
			return;

		for (int i = 0; i < setups.size(); i++) {
			InventorySource best = null;
			int bestScore = 0;
			for (var source : inventories) {
				int score = matchScore(setups.get(i).getLabel(), source.context);
				if (score > bestScore) {
					best = source;
					bestScore = score;
				}
			}

			if (best == null) {
				int from = positions.get(i);
				int to = i + 1 < positions.size() ? positions.get(i + 1) : Integer.MAX_VALUE;
				InventorySource after = null;
				for (var source : inventories) {
					if (source.position > from && source.position < to) {
						best = source;
						break;
					}
					if (after == null && source.position > from)
						after = source;
				}
				if (best == null)
					best = after != null ? after : inventories.get(inventories.size() - 1);
			}
			setups.set(i, setups.get(i).withInventory(best.items));
		}
	}

	/**
	 * Offer {{Loadout}}s that aren't just one of the tables' styles as setups of their own, like Zulrah's
	 * "Max Magic & Ranged", which wears magic gear and carries ranged switches
	 */
	private static void addLoadoutSetups(List<GearSetup> setups, List<String> contexts, List<InventorySource> inventories) {
		var existing = new ArrayList<>(setups);
		for (var source : inventories) {
			if (source.slots.isEmpty())
				continue;
			boolean matchesTable = false;
			for (var setup : existing)
				matchesTable |= matchScore(setup.getLabel(), source.context) >= 2;
			if (matchesTable)
				continue;
			String label = source.context != null ? source.context : "Loadout " + (setups.size() + 1);
			setups.add(new GearSetup(label, source.slots, source.items));
			contexts.add(source.context);
		}
	}

	/**
	 * Read the items of an {{Inventory}} by slot. Items can carry an amount after a backslash, like "Blood rune\\500",
	 * and options after a semicolon, like ";n" for noted items.
	 */
	static List<String> parseInventory(String body) {
		var inventory = new ArrayList<String>();
		for (int i = 0; i < GearSetup.INVENTORY_SIZE; i++)
			inventory.add(null);

		var parts = splitTopLevel(body);
		int position = 0;
		for (int i = 1; i < parts.size(); i++) {
			String part = parts.get(i);
			int slot;
			int equals = part.indexOf('=');
			if (equals >= 0) {
				String key = part.substring(0, equals).trim();
				if (!key.matches("\\d+"))
					continue; // A setting like align=right
				slot = Integer.parseInt(key) - 1;
				part = part.substring(equals + 1);
			} else {
				slot = position++;
			}
			if (slot < 0 || slot >= GearSetup.INVENTORY_SIZE)
				continue;

			String item = part.trim();
			if (item.startsWith("{{")) {
				// A template standing in for an item, like {{plinkp|Shark}} or a placeholder like {{Cheap food}}
				int end = findClosing(item, 0, "{{", "}}");
				if (end > 0) {
					inventory.set(slot, resolveItemTemplate(item.substring(2, end - 2)));
					continue;
				}
			}
			int semicolon = item.indexOf(';');
			if (semicolon >= 0)
				item = item.substring(0, semicolon);
			int backslash = item.indexOf('\\');
			if (backslash >= 0)
				item = item.substring(0, backslash);
			inventory.set(slot, cleanText(item));
		}
		return inventory;
	}

	/**
	 * Give setups which share a label, e.g. several "Melee" tables for different phases, unique labels
	 */
	private static List<GearSetup> disambiguate(List<GearSetup> setups, List<String> contexts) {
		var counts = new HashMap<String, Integer>();
		for (var setup : setups)
			counts.merge(setup.getLabel(), 1, Integer::sum);

		var result = new ArrayList<GearSetup>();
		var used = new HashMap<String, Integer>();
		for (int i = 0; i < setups.size(); i++) {
			var setup = setups.get(i);
			String label = setup.getLabel();
			if (counts.get(label) > 1) {
				String context = contexts.get(i);
				if (context != null && !context.equalsIgnoreCase(label))
					label = label + " (" + context + ")";
			}
			int n = used.merge(label, 1, Integer::sum);
			if (n > 1)
				label = label + " #" + n;
			result.add(setup.withLabel(label));
		}
		return result;
	}

	private static Map<GearSlot, List<List<String>>> parseSlots(Map<String, String> params) {
		var ranked = new LinkedHashMap<GearSlot, TreeMap<Integer, List<String>>>();
		for (var entry : params.entrySet()) {
			Matcher matcher = SLOT_PARAM.matcher(entry.getKey());
			if (!matcher.matches())
				continue;
			GearSlot slot = GearSlot.fromWikiName(matcher.group(1));
			if (slot == null)
				continue;
			var items = extractItems(entry.getValue());
			if (items.isEmpty())
				continue;
			ranked.computeIfAbsent(slot, k -> new TreeMap<>()).put(Integer.parseInt(matcher.group(2)), items);
		}

		var slots = GearSetup.emptySlots();
		for (var entry : ranked.entrySet())
			slots.put(entry.getKey(), new ArrayList<>(entry.getValue().values()));
		return slots;
	}

	/**
	 * Find the item names in a slot's value, from {{plink|Item}} style templates and [[Item]] links.
	 * Footnotes and other templates are skipped, so links inside them aren't mistaken for items.
	 */
	static List<String> extractItems(String value) {
		var items = new ArrayList<String>();
		if (value == null)
			return items;

		int i = 0;
		while (i < value.length()) {
			if (value.startsWith("{{", i)) {
				int end = findClosing(value, i, "{{", "}}");
				if (end < 0)
					break;
				var parts = splitTopLevel(value.substring(i + 2, end - 2));
				String name = parts.get(0).trim().toLowerCase(Locale.ROOT);
				if (name.startsWith("plink") && parts.size() > 1) {
					String item = cleanText(parts.get(1));
					if (item != null && !parts.get(1).contains("="))
						addItem(items, item);
				}
				i = end;
			} else if (value.startsWith("[[", i)) {
				int end = value.indexOf("]]", i);
				if (end < 0)
					break;
				String target = value.substring(i + 2, end);
				int pipe = target.indexOf('|');
				if (pipe >= 0)
					target = target.substring(0, pipe);
				int hash = target.indexOf('#');
				if (hash >= 0)
					target = target.substring(0, hash);
				if (!target.contains(":"))
					addItem(items, cleanText(target));
				i = end + 2;
			} else {
				i++;
			}
		}
		return items;
	}

	private static void addItem(List<String> items, @Nullable String item) {
		if (item != null && !item.isEmpty() && !items.contains(item))
			items.add(item);
	}

	private static Map<String, String> parseParams(String body) {
		var params = new LinkedHashMap<String, String>();
		var parts = splitTopLevel(body);
		for (int i = 1; i < parts.size(); i++) {
			String part = parts.get(i);
			int equals = part.indexOf('=');
			if (equals < 0)
				continue;
			params.put(part.substring(0, equals).trim().toLowerCase(Locale.ROOT), part.substring(equals + 1).trim());
		}
		return params;
	}

	/**
	 * Split on | characters which aren't inside nested templates or links
	 */
	private static List<String> splitTopLevel(String text) {
		var parts = new ArrayList<String>();
		int depth = 0;
		int start = 0;
		for (int i = 0; i < text.length(); i++) {
			if (text.startsWith("{{", i) || text.startsWith("[[", i)) {
				depth++;
				i++;
			} else if ((text.startsWith("}}", i) || text.startsWith("]]", i)) && depth > 0) {
				depth--;
				i++;
			} else if (text.charAt(i) == '|' && depth == 0) {
				parts.add(text.substring(start, i));
				start = i + 1;
			}
		}
		parts.add(text.substring(start));
		return parts;
	}

	/**
	 * @return the index just past the closing token matching the opening token at start, or -1
	 */
	private static int findClosing(String text, int start, String open, String close) {
		int depth = 0;
		for (int i = start; i < text.length() - 1; i++) {
			if (text.startsWith(open, i)) {
				depth++;
				i++;
			} else if (text.startsWith(close, i)) {
				depth--;
				i++;
				if (depth == 0)
					return i + 1;
			}
		}
		return -1;
	}

	/**
	 * The nearest section heading or tab label before the given position, e.g. "Phase 2" or "Magic"
	 */
	@Nullable
	private static String findContext(String text, int position) {
		String before = text.substring(0, position);
		String context = null;
		int contextPos = -1;

		Matcher heading = HEADING.matcher(before);
		while (heading.find()) {
			context = heading.group(2);
			contextPos = heading.start();
		}

		Matcher tab = TAB_LABEL.matcher(before);
		while (tab.find()) {
			if (tab.start() > contextPos) {
				context = tab.group(1);
				contextPos = tab.start();
			}
		}

		return cleanText(context);
	}

	/**
	 * Placeholder templates the wiki uses in inventories, like {{Cheap prayer}}, matched by keyword, and the item
	 * to use in their place. Any item from the same potion group can then stand in for it.
	 */
	private static final String[][] PLACEHOLDER_KEYWORDS = {
		{ "food", InventorySubstitutes.ANY_FOOD },
		{ "brew", "Saradomin brew(4)" },
		{ "restore", "Super restore(4)" },
		{ "prayer", "Prayer potion(4)" },
		{ "combat", "Super combat potion(4)" },
		{ "rang", "Ranging potion(4)" },
		{ "magic", "Magic potion(4)" },
		{ "antifire", "Extended super antifire(4)" },
		{ "venom", "Anti-venom+(4)" },
		{ "poison", "Anti-venom+(4)" },
		{ "stamina", "Stamina potion(4)" },
		{ "energy", "Stamina potion(4)" },
	};

	/**
	 * Work out the item a template in an inventory slot stands for
	 */
	@Nullable
	static String resolveItemTemplate(String body) {
		var parts = splitTopLevel(body);
		String name = parts.get(0).trim().toLowerCase(Locale.ROOT);
		if (name.startsWith("plink") && parts.size() > 1)
			return cleanText(parts.get(1));
		for (var placeholder : PLACEHOLDER_KEYWORDS)
			if (name.contains(placeholder[0]))
				return placeholder[1];
		return null;
	}

	@Nullable
	static String cleanText(@Nullable String text) {
		if (text == null)
			return null;
		String cleaned = LINK.matcher(text).replaceAll("$1");
		cleaned = MARKUP.matcher(cleaned).replaceAll("");
		cleaned = cleaned.replace("&amp;", "&").replace("&#39;", "'").replace('_', ' ').replaceAll("\\s+", " ").trim();
		return cleaned.isEmpty() ? null : cleaned;
	}
}
