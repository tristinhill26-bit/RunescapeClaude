package com.wikigear;

import com.wikigear.dps.Constants;
import com.wikigear.dps.EquipmentPiece;
import com.wikigear.dps.GameData;
import com.wikigear.dps.Monster;
import com.wikigear.dps.MonsterAttribute;
import com.wikigear.dps.PlayerSkills;
import com.wikigear.dps.Potion;
import com.wikigear.dps.Prayer;
import com.wikigear.dps.Spell;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * Prepares the DPS optimiser's inputs from what the player owns and has unlocked, and turns its result into tag picks
 */
public class DpsPlanner
{
	/**
	 * Weapons which can't attack without charges, by their version in the calculator's data or their name
	 */
	private static final Pattern UNCHARGED = Pattern.compile("(?i)\\b(uncharged|inactive|empty)\\b");
	private static final Pattern DOSES = Pattern.compile("\\s*\\(\\d+\\)$");

	/**
	 * What the optimiser needs to know about the player, read from the client
	 */
	public static class PlayerState
	{
		public PlayerSkills skills = new PlayerSkills();
		public boolean knightWaves;
		public boolean rigour;
		public boolean augury;
		public boolean deadeye;
		public boolean mysticVigour;
		public boolean desertTreasure;
		public boolean kandarinHardDiary;
	}

	/**
	 * Potions by the names of the items that give their boost
	 */
	private static final Map<String, Potion[]> POTION_ITEMS = new HashMap<>();

	static {
		potion(new Potion[] { Potion.SUPER_COMBAT }, "Super combat potion", "Divine super combat potion");
		potion(new Potion[] { Potion.SUPER_ATTACK }, "Super attack", "Divine super attack potion");
		potion(new Potion[] { Potion.SUPER_STRENGTH }, "Super strength", "Divine super strength potion");
		potion(new Potion[] { Potion.ATTACK }, "Attack potion");
		potion(new Potion[] { Potion.STRENGTH }, "Strength potion");
		// A combat potion boosts attack and strength like the regular potions
		potion(new Potion[] { Potion.ATTACK, Potion.STRENGTH }, "Combat potion");
		potion(new Potion[] { Potion.RANGING }, "Ranging potion", "Divine ranging potion", "Bastion potion", "Divine bastion potion");
		potion(new Potion[] { Potion.MAGIC }, "Magic potion", "Divine magic potion", "Battlemage potion", "Divine battlemage potion");
		potion(new Potion[] { Potion.IMBUED_HEART }, "Imbued heart");
		potion(new Potion[] { Potion.SATURATED_HEART }, "Saturated heart");
		potion(new Potion[] { Potion.ANCIENT }, "Ancient brew");
		potion(new Potion[] { Potion.FORGOTTEN_BREW }, "Forgotten brew");
		potion(new Potion[] { Potion.MOONLIGHT }, "Moonlight potion");
		potion(new Potion[] { Potion.RUBY_HARVEST }, "Ruby harvest");
		potion(new Potion[] { Potion.BLACK_WARLOCK }, "Black warlock");
		potion(new Potion[] { Potion.SAPPHIRE_GLACIALIS }, "Sapphire glacialis");
	}

	private static void potion(Potion[] potions, String... names) {
		for (String name : names)
			POTION_ITEMS.put(name.toLowerCase(Locale.ROOT), potions);
	}

	/**
	 * Magic level needed for each combat spell. God spells, Iban Blast and Magic Dart also need their staff, which
	 * the calculator checks.
	 */
	private static final Map<String, Integer> SPELL_LEVELS = new HashMap<>();
	private static final Set<String> ANCIENT_SPELLS = new HashSet<>();

	static {
		String[] elements = { "Wind", "Water", "Earth", "Fire" };
		int[][] elementLevels = {
			{ 1, 5, 9, 13 }, // Strike
			{ 17, 23, 29, 35 }, // Bolt
			{ 41, 47, 53, 59 }, // Blast
			{ 62, 65, 70, 75 }, // Wave
			{ 81, 85, 90, 95 }, // Surge
		};
		String[] classes = { "Strike", "Bolt", "Blast", "Wave", "Surge" };
		for (int c = 0; c < classes.length; c++)
			for (int e = 0; e < elements.length; e++)
				SPELL_LEVELS.put(elements[e] + " " + classes[c], elementLevels[c][e]);

		String[] ancients = { "Smoke", "Shadow", "Blood", "Ice" };
		int[] ancientOffsets = { 0, 2, 6, 8 };
		String[] ancientClasses = { "Rush", "Burst", "Blitz", "Barrage" };
		for (int c = 0; c < ancientClasses.length; c++) {
			for (int a = 0; a < ancients.length; a++) {
				String name = ancients[a] + " " + ancientClasses[c];
				// e.g. Smoke Rush is level 50, Ice Rush 58 and Ice Barrage 94
				SPELL_LEVELS.put(name, 50 + c * 12 + ancientOffsets[a]);
				ANCIENT_SPELLS.add(name);
			}
		}

		SPELL_LEVELS.put("Crumble Undead", 39);
		SPELL_LEVELS.put("Iban Blast", 50);
		SPELL_LEVELS.put("Magic Dart", 50);
		SPELL_LEVELS.put("Saradomin Strike", 60);
		SPELL_LEVELS.put("Claws of Guthix", 60);
		SPELL_LEVELS.put("Flames of Zamorak", 60);
		SPELL_LEVELS.put("Ghostly Grasp", 35);
		SPELL_LEVELS.put("Skeletal Grasp", 56);
		SPELL_LEVELS.put("Undead Grasp", 79);
		SPELL_LEVELS.put("Inferior Demonbane", 44);
		SPELL_LEVELS.put("Superior Demonbane", 62);
		SPELL_LEVELS.put("Dark Demonbane", 82);
	}

	static int spellLevel(String spell) {
		return SPELL_LEVELS.getOrDefault(spell, Integer.MAX_VALUE);
	}

	/**
	 * Owned equipment the calculator knows, by calculator slot. Each piece is a copy carrying the owned item's id.
	 */
	public static Map<String, List<EquipmentPiece>> candidates(GameData data, GearPicker.OwnedItems owned) {
		var bySlot = new HashMap<String, List<EquipmentPiece>>();
		var seen = new HashSet<Integer>();
		for (var item : owned.getAll()) {
			EquipmentPiece piece = data.equipmentById(item.itemId);
			if (piece == null)
				piece = data.equipmentById(data.canonicalItemId(item.itemId));
			if (piece == null || piece.slot == null || !seen.add(item.itemId))
				continue;
			if (piece.slot.equals("weapon") && (matches(UNCHARGED, piece.version) || matches(UNCHARGED, item.name)))
				continue;
			var copy = piece.copy();
			copy.id = item.itemId;
			bySlot.computeIfAbsent(piece.slot, k -> new ArrayList<>()).add(copy);
		}
		return bySlot;
	}

	private static boolean matches(Pattern pattern, @Nullable String text) {
		return text != null && pattern.matcher(text).find();
	}

	public static Set<Prayer> unlockedPrayers(PlayerState state) {
		double prayer = state.skills.prayer;
		double defence = state.skills.def;
		var unlocked = EnumSet.noneOf(Prayer.class);
		Object[][] levels = {
			{ Prayer.BURST_OF_STRENGTH, 4 }, { Prayer.CLARITY_OF_THOUGHT, 7 }, { Prayer.SHARP_EYE, 8 }, { Prayer.MYSTIC_WILL, 9 },
			{ Prayer.SUPERHUMAN_STRENGTH, 13 }, { Prayer.IMPROVED_REFLEXES, 16 }, { Prayer.HAWK_EYE, 26 }, { Prayer.MYSTIC_LORE, 27 },
			{ Prayer.ULTIMATE_STRENGTH, 31 }, { Prayer.INCREDIBLE_REFLEXES, 34 }, { Prayer.EAGLE_EYE, 44 }, { Prayer.MYSTIC_MIGHT, 45 },
		};
		for (Object[] l : levels)
			if (prayer >= (int) l[1])
				unlocked.add((Prayer) l[0]);
		if (state.knightWaves && prayer >= 60 && defence >= 65)
			unlocked.add(Prayer.CHIVALRY);
		if (state.knightWaves && prayer >= 70 && defence >= 70)
			unlocked.add(Prayer.PIETY);
		if (state.deadeye && prayer >= 62)
			unlocked.add(Prayer.DEADEYE);
		if (state.mysticVigour && prayer >= 63)
			unlocked.add(Prayer.MYSTIC_VIGOUR);
		if (state.rigour && prayer >= 74 && defence >= 70)
			unlocked.add(Prayer.RIGOUR);
		if (state.augury && prayer >= 77 && defence >= 70)
			unlocked.add(Prayer.AUGURY);
		return unlocked;
	}

	public static List<Spell> castableSpells(GameData data, PlayerState state) {
		var spells = new ArrayList<Spell>();
		for (Spell spell : data.spells) {
			if (spell.max_hit <= 0 || state.skills.magic < spellLevel(spell.name))
				continue;
			if (ANCIENT_SPELLS.contains(spell.name) && !state.desertTreasure)
				continue;
			spells.add(spell);
		}
		return spells;
	}

	/**
	 * The potions the player can use for this fight: ones they own, plus the raid's own potions inside raids
	 *
	 * @param anyPotion assume the common tradeable potions even if the player doesn't own them
	 */
	public static Set<Potion> potions(GearPicker.OwnedItems owned, Monster monster, boolean anyPotion, List<String> notes) {
		var potions = EnumSet.noneOf(Potion.class);
		for (var item : owned.getAll()) {
			String name = DOSES.matcher(item.name.toLowerCase(Locale.ROOT).trim()).replaceFirst("");
			Potion[] found = POTION_ITEMS.get(name);
			if (found != null)
				potions.addAll(List.of(found));
		}
		if (anyPotion)
			potions.addAll(List.of(Potion.SUPER_COMBAT, Potion.RANGING, Potion.MAGIC));

		if (monster.attributes.contains(MonsterAttribute.XERICIAN)) {
			potions.add(Potion.OVERLOAD_PLUS);
			notes.add("Assumes Overloads (+), which you make during the raid.");
		} else if (Constants.has(Constants.TOMBS_OF_AMASCUT_MONSTER_IDS, monster.id)) {
			potions.add(Potion.SMELLING_SALTS);
			notes.add("Assumes Smelling salts, which you get during the raid.");
		}
		return potions;
	}

	/**
	 * The dart to assume in a blowpipe: the best one owned, or adamant darts
	 */
	@Nullable
	public static EquipmentPiece blowpipeDart(GameData data, Map<String, List<EquipmentPiece>> candidates) {
		EquipmentPiece best = null;
		for (EquipmentPiece piece : candidates.getOrDefault("weapon", List.of()))
			if (piece.name.endsWith(" dart") && (best == null || piece.bonuses.ranged_str > best.bonuses.ranged_str))
				best = piece;
		if (best != null)
			return best;
		for (EquipmentPiece piece : data.equipment)
			if (piece.name.equals("Adamant dart"))
				return piece;
		return null;
	}

	/**
	 * The wiki's picks as optimiser starting gear
	 */
	public static Map<String, EquipmentPiece> seed(Map<GearSlot, GearPicker.Pick> picks, Map<String, List<EquipmentPiece>> candidates) {
		var seed = new HashMap<String, EquipmentPiece>();
		for (var pick : picks.values()) {
			if (pick.slot == GearSlot.SPECIAL)
				continue;
			String slot = pick.slot == GearSlot.TWO_HANDED ? "weapon" : pick.slot.getWikiName();
			for (EquipmentPiece piece : candidates.getOrDefault(slot, List.of())) {
				if (piece.id == pick.itemId) {
					seed.put(slot, piece);
					break;
				}
			}
		}
		return seed;
	}

	/**
	 * Replace the worn gear picks with the optimiser's, keeping the wiki's pick wherever it's the same item
	 */
	public static void applyResult(Map<GearSlot, GearPicker.Pick> picks, DpsOptimizer.Result result, GearPicker.OwnedItems owned) {
		var previous = new HashMap<>(picks);
		picks.keySet().removeIf(slot -> slot != GearSlot.SPECIAL);
		for (String calcSlot : com.wikigear.dps.PlayerEquipment.SLOTS) {
			EquipmentPiece piece = result.equipment.get(calcSlot);
			if (piece == null)
				continue;
			GearSlot slot = calcSlot.equals("weapon")
				? (piece.isTwoHanded ? GearSlot.TWO_HANDED : GearSlot.WEAPON)
				: GearSlot.fromWikiName(calcSlot);
			if (slot == null)
				continue;

			var old = previous.get(slot);
			if (old != null && old.itemId == piece.id) {
				picks.put(slot, old);
				continue;
			}
			var item = owned.findById(piece.id);
			String name = item != null ? item.name : piece.name;
			picks.put(slot, new GearPicker.Pick(slot, piece.id, name, 0, 0, GearPicker.Source.DPS));
		}
	}

	/**
	 * Calculator monsters for a wiki page name: exact name matches, or else names that contain or are contained by it
	 */
	public static List<Monster> findMonsters(GameData data, String name, int limit) {
		String wanted = name.toLowerCase(Locale.ROOT).trim();
		var exact = new ArrayList<Monster>();
		var partial = new ArrayList<Monster>();
		for (Monster m : data.monsters) {
			String n = m.name.toLowerCase(Locale.ROOT);
			if (n.equals(wanted))
				exact.add(m);
			else if (n.contains(wanted) || (n.length() >= 4 && wanted.contains(n)))
				partial.add(m);
		}
		var result = exact.isEmpty() ? partial : exact;
		return result.size() > limit ? new ArrayList<>(result.subList(0, limit)) : result;
	}

	private DpsPlanner() {
	}
}
