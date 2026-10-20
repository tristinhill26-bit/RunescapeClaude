package com.wikigear;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
	private static final Pattern INVENTORY_START = Pattern.compile("\\{\\{\\s*inventory\\s*(?=\\||}})", Pattern.CASE_INSENSITIVE);
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

		attachInventories(wikitext, setups, positions);
		return disambiguate(setups, contexts);
	}

	/**
	 * Attach each {{Inventory}} to its equipment table: the table in the tab or section of the same name, e.g. an
	 * inventory under "Magic" goes with the Magic table, or else the table it follows. Tables still without an
	 * inventory then take the nearest one, preferring the first one after them, since pages often list every
	 * table first and the inventories after, like a Magic and a Ranged table followed by an Inventory section.
	 */
	private static void attachInventories(String wikitext, List<GearSetup> setups, List<Integer> positions) {
		if (setups.isEmpty())
			return;

		var inventories = new ArrayList<List<String>>();
		var inventoryPositions = new ArrayList<Integer>();
		Matcher matcher = INVENTORY_START.matcher(wikitext);
		int searchFrom = 0;
		while (matcher.find(searchFrom)) {
			int start = matcher.start();
			int end = findClosing(wikitext, start, "{{", "}}");
			if (end < 0)
				break;
			searchFrom = end;

			var inventory = parseInventory(wikitext.substring(start + 2, end - 2));
			if (inventory.stream().allMatch(item -> item == null))
				continue;
			inventories.add(inventory);
			inventoryPositions.add(start);

			int owner = -1;
			String context = findContext(wikitext, start);
			if (context != null) {
				for (int i = 0; i < setups.size(); i++)
					if (setups.get(i).getLabel().equalsIgnoreCase(context) && !setups.get(i).hasInventory())
						owner = i;
			}
			if (owner < 0) {
				owner = 0;
				for (int i = 0; i < positions.size(); i++)
					if (positions.get(i) < start)
						owner = i;
			}
			if (!setups.get(owner).hasInventory())
				setups.set(owner, setups.get(owner).withInventory(inventory));
		}
		if (inventories.isEmpty())
			return;

		for (int i = 0; i < setups.size(); i++) {
			if (setups.get(i).hasInventory())
				continue;
			int nearest = inventories.size() - 1;
			for (int j = 0; j < inventories.size(); j++) {
				if (inventoryPositions.get(j) > positions.get(i)) {
					nearest = j;
					break;
				}
			}
			setups.set(i, setups.get(i).withInventory(inventories.get(nearest)));
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
