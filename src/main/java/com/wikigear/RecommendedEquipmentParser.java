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
		}

		return disambiguate(setups, contexts);
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
