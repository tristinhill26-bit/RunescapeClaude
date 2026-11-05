package com.wikigear;

import com.wikigear.dps.BaseCalc;
import com.wikigear.dps.CombatStyles;
import com.wikigear.dps.Constants;
import com.wikigear.dps.EquipmentCalc;
import com.wikigear.dps.EquipmentCategory;
import com.wikigear.dps.EquipmentPiece;
import com.wikigear.dps.Monster;
import com.wikigear.dps.MonsterScaling;
import com.wikigear.dps.Player;
import com.wikigear.dps.PlayerEquipment;
import com.wikigear.dps.PlayerSkills;
import com.wikigear.dps.PlayerVsNPCCalc;
import com.wikigear.dps.Potion;
import com.wikigear.dps.Prayer;
import com.wikigear.dps.Spell;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Finds the owned gear with the highest DPS against a monster, using the wiki DPS calculator's maths.
 * <p>
 * Every owned weapon and combat style is scored with the current armour, then the best few are improved one slot at a
 * time until no single swap raises DPS. Sets which only work together, like Void, are also tried as a whole. Swaps are
 * only made when they strictly raise DPS, so the wiki's recommendations are kept whenever they're just as good.
 */
public class DpsOptimizer
{
	/**
	 * Calculator slots other than the weapon, in the order they're improved
	 */
	private static final String[] ARMOUR_SLOTS = { "head", "cape", "neck", "ammo", "body", "shield", "legs", "hands", "feet", "ring" };
	/**
	 * How many of the best weapon and style combinations are fully optimised
	 */
	private static final int FINALISTS = 6;
	private static final int MAX_PASSES = 4;
	private static final double EPSILON = 1e-9;

	/**
	 * Gear sets whose bonus needs every piece. Each entry is a slot followed by the names that count for it.
	 */
	private static final String[][][] SETS = {
		voidSet("Void melee helm"),
		voidSet("Void ranger helm"),
		voidSet("Void mage helm"),
		eliteVoidSet("Void ranger helm"),
		eliteVoidSet("Void mage helm"),
		{ { "head", "Obsidian helmet" }, { "body", "Obsidian platebody" }, { "legs", "Obsidian platelegs" } },
		{ { "head", "Dharok's helm" }, { "body", "Dharok's platebody" }, { "legs", "Dharok's platelegs" } },
		{ { "head", "Verac's helm" }, { "body", "Verac's brassard" }, { "legs", "Verac's plateskirt" } },
		{ { "head", "Torag's helm" }, { "body", "Torag's platebody" }, { "legs", "Torag's platelegs" }, { "neck", "Amulet of the damned" } },
		{ { "head", "Karil's coif" }, { "body", "Karil's leathertop" }, { "legs", "Karil's leatherskirt" }, { "neck", "Amulet of the damned" } },
		{ { "head", "Ahrim's hood" }, { "body", "Ahrim's robetop" }, { "legs", "Ahrim's robeskirt" }, { "neck", "Amulet of the damned" } },
		{ { "head", "Blood moon helm" }, { "body", "Blood moon chestplate" }, { "legs", "Blood moon tassets" } },
		{ { "head", "Inquisitor's great helm" }, { "body", "Inquisitor's hauberk" }, { "legs", "Inquisitor's plateskirt" } },
		{ { "head", "Crystal helm" }, { "body", "Crystal body" }, { "legs", "Crystal legs" } },
	};

	private static String[][] voidSet(String helm) {
		return new String[][] {
			{ "head", helm, helm + " (or)" },
			{ "body", "Elite void top", "Elite void top (or)", "Void knight top", "Void knight top (or)" },
			{ "legs", "Elite void robe", "Elite void robe (or)", "Void knight robe", "Void knight robe (or)" },
			{ "hands", "Void knight gloves" },
		};
	}

	private static String[][] eliteVoidSet(String helm) {
		return new String[][] {
			{ "head", helm, helm + " (or)" },
			{ "body", "Elite void top", "Elite void top (or)" },
			{ "legs", "Elite void robe", "Elite void robe (or)" },
			{ "hands", "Void knight gloves" },
		};
	}

	/**
	 * Everything the optimiser needs to know about the player and the fight
	 */
	public static class Context
	{
		public Monster monster;
		/**
		 * Real skill levels
		 */
		public PlayerSkills skills = new PlayerSkills();
		/**
		 * Potions the player can drink for this fight
		 */
		public Set<Potion> potions = EnumSet.noneOf(Potion.class);
		/**
		 * Prayers the player has unlocked and has the levels for
		 */
		public Set<Prayer> prayers = EnumSet.noneOf(Prayer.class);
		/**
		 * Spells the player can cast
		 */
		public List<Spell> spells = new ArrayList<>();
		public boolean onSlayerTask;
		public boolean inWilderness;
		public boolean kandarinDiary;
		/**
		 * Owned equipment for each calculator slot
		 */
		public Map<String, List<EquipmentPiece>> candidates = new HashMap<>();
		/**
		 * Gear to start from, normally the wiki's picks. Kept wherever nothing owned does better.
		 */
		public Map<String, EquipmentPiece> seed = new HashMap<>();
		/**
		 * The dart assumed to be loaded in a blowpipe, or null for none
		 */
		@Nullable
		public EquipmentPiece blowpipeDart;
	}

	public static class Result
	{
		public PlayerEquipment equipment;
		public com.wikigear.dps.CombatStyle style;
		@Nullable
		public Spell spell;
		public List<Prayer> prayers;
		public List<Potion> potions;
		public double dps;
		public double maxHit;
		public double accuracy;
		/**
		 * Average seconds to kill from full health
		 */
		public double ttk;
		public double attackSpeed;
	}

	private static final class Combo
	{
		final EquipmentPiece weapon;
		final com.wikigear.dps.CombatStyle style;
		final Spell spell;
		double estimate;

		Combo(EquipmentPiece weapon, com.wikigear.dps.CombatStyle style, Spell spell) {
			this.weapon = weapon;
			this.style = style;
			this.spell = spell;
		}
	}

	private final Context ctx;
	private final CombatStyle family;
	private final Monster baseMonster;
	private final Monster scaledMonster;
	private final PlayerSkills boosts;
	private final List<Prayer> prayers;

	private DpsOptimizer(Context ctx, CombatStyle family) {
		this.ctx = ctx;
		this.family = family;

		var monster = ctx.monster.copy();
		monster.inputs = Monster.Inputs.initial();
		// Fight from full health. Vardorvis scales with his current health, everything else is clamped to its max.
		monster.inputs.monsterCurrentHp = "Vardorvis".equals(monster.name) ? monster.skills.hp : 0;
		baseMonster = monster;
		scaledMonster = MonsterScaling.scaleMonster(monster);

		boosts = Potion.combinedBoosts(ctx.potions, ctx.skills);
		prayers = bestPrayers(family, ctx.prayers);
	}

	/**
	 * The highest DPS gear for a combat style, or null if nothing owned can attack with it
	 */
	@Nullable
	public static Result optimise(Context ctx, CombatStyle family) {
		return new DpsOptimizer(ctx, family).run();
	}

	/**
	 * The highest DPS over all three combat styles
	 */
	@Nullable
	public static Result optimiseAny(Context ctx) {
		Result best = null;
		for (CombatStyle family : CombatStyle.values()) {
			Result r = optimise(ctx, family);
			if (r != null && (best == null || r.dps > best.dps))
				best = r;
		}
		return best;
	}

	/**
	 * The best prayers for a style. Strength and accuracy prayers stack with each other, the rest replace each other.
	 */
	static List<Prayer> bestPrayers(CombatStyle family, Set<Prayer> unlocked) {
		var result = new ArrayList<Prayer>();
		switch (family) {
			case MELEE:
				if (firstUnlocked(unlocked, result, Prayer.PIETY, Prayer.CHIVALRY))
					break;
				firstUnlocked(unlocked, result, Prayer.ULTIMATE_STRENGTH, Prayer.SUPERHUMAN_STRENGTH, Prayer.BURST_OF_STRENGTH);
				firstUnlocked(unlocked, result, Prayer.INCREDIBLE_REFLEXES, Prayer.IMPROVED_REFLEXES, Prayer.CLARITY_OF_THOUGHT);
				break;
			case RANGED:
				firstUnlocked(unlocked, result, Prayer.RIGOUR, Prayer.DEADEYE, Prayer.EAGLE_EYE, Prayer.HAWK_EYE, Prayer.SHARP_EYE);
				break;
			case MAGIC:
				firstUnlocked(unlocked, result, Prayer.AUGURY, Prayer.MYSTIC_VIGOUR, Prayer.MYSTIC_MIGHT, Prayer.MYSTIC_LORE, Prayer.MYSTIC_WILL);
				break;
		}
		return result;
	}

	private static boolean firstUnlocked(Set<Prayer> unlocked, List<Prayer> out, Prayer... choices) {
		for (Prayer p : choices) {
			if (unlocked.contains(p)) {
				out.add(p);
				return true;
			}
		}
		return false;
	}

	@Nullable
	private Result run() {
		var combos = new ArrayList<Combo>();
		var weapons = new ArrayList<EquipmentPiece>();
		// The wiki's weapon goes first, so it wins ties
		EquipmentPiece seedWeapon = ctx.seed.get("weapon");
		if (seedWeapon != null)
			weapons.add(seedWeapon);
		for (EquipmentPiece w : ctx.candidates.getOrDefault("weapon", List.of()))
			if (w != seedWeapon)
				weapons.add(w);
		// Fighting unarmed, or casting with no weapon
		weapons.add(null);

		for (EquipmentPiece weapon : weapons) {
			String category = weapon == null || weapon.category == null ? EquipmentCategory.NONE : weapon.category;
			var styles = CombatStyles.getCombatStylesForCategory(category);
			boolean autocasts = styles.stream().anyMatch(s -> "Autocast".equals(s.stance));
			boolean powered = EquipmentCategory.POWERED_STAFF.equals(category) || EquipmentCategory.POWERED_WAND.equals(category);
			var seen = new HashSet<String>();
			for (var style : styles) {
				if (!suits(style, autocasts, powered) || !seen.add(style.type + "/" + style.stance))
					continue;
				if (Constants.CAST_STANCES.contains(style.stance)) {
					for (Spell spell : ctx.spells)
						combos.add(new Combo(weapon, style, spell));
				} else {
					combos.add(new Combo(weapon, style, null));
				}
			}
		}
		if (combos.isEmpty())
			return null;

		// Score every combination with the starting armour, then fully optimise the best few
		for (Combo c : combos) {
			var eq = startingEquipment(c.weapon);
			c.estimate = eq == null ? -1 : evaluate(eq, c);
		}
		combos.sort(Comparator.comparingDouble((Combo c) -> c.estimate).reversed());

		Result best = null;
		var tried = new HashSet<String>();
		for (Combo c : combos) {
			if (c.estimate < 0 || tried.size() >= FINALISTS)
				break;
			// Only one spell per weapon and style reaches the final round, so the finalists cover more weapons
			if (!tried.add((c.weapon == null ? -1 : c.weapon.id) + "/" + c.style.stance + "/" + c.style.type))
				continue;
			var eq = startingEquipment(c.weapon);
			double dps = improve(eq, c, new HashSet<>());
			dps = trySets(eq, c, dps);
			if (best == null || dps > best.dps + EPSILON)
				best = result(eq, c);
		}
		return best == null || best.dps <= 0 ? null : best;
	}

	private boolean suits(com.wikigear.dps.CombatStyle style, boolean autocasts, boolean powered) {
		if (style.type == null)
			return false;
		switch (family) {
			case MELEE:
				return style.type.equals("stab") || style.type.equals("slash") || style.type.equals("crush");
			case RANGED:
				return style.type.equals("ranged");
			default:
				if (!style.type.equals("magic"))
					return false;
				// Defensive autocast hits the same as autocast
				if ("Defensive Autocast".equals(style.stance))
					return false;
				// Weapons that can autocast don't need to manually cast, and powered staves use their own spell
				if ("Manual Cast".equals(style.stance))
					return !autocasts && !powered && !ctx.spells.isEmpty();
				return !"Autocast".equals(style.stance) || !ctx.spells.isEmpty();
		}
	}

	/**
	 * The seed gear with this weapon, fixed up so it's wearable: no shield with a two-handed weapon, and ammo that
	 * suits the weapon. Null if the weapon needs ammo the player doesn't own.
	 */
	@Nullable
	private PlayerEquipment startingEquipment(@Nullable EquipmentPiece weapon) {
		var eq = new PlayerEquipment();
		for (String slot : ARMOUR_SLOTS)
			eq.set(slot, ctx.seed.get(slot));
		eq.weapon = withDart(weapon);
		if (weapon != null && weapon.isTwoHanded)
			eq.shield = null;
		if (!ammoOk(eq, eq.ammo)) {
			eq.ammo = null;
			for (EquipmentPiece ammo : ctx.candidates.getOrDefault("ammo", List.of())) {
				if (ammoOk(eq, ammo)) {
					eq.ammo = ammo;
					break;
				}
			}
			if (requiresAmmo(eq) && eq.ammo == null)
				return null;
		}
		return eq;
	}

	@Nullable
	private EquipmentPiece withDart(@Nullable EquipmentPiece weapon) {
		if (weapon == null || ctx.blowpipeDart == null || !Constants.has(Constants.BLOWPIPE_IDS, canonicalId(weapon)))
			return weapon;
		var copy = weapon.copy();
		copy.itemVars = new EquipmentPiece.ItemVars();
		copy.itemVars.blowpipeDartId = ctx.blowpipeDart.id;
		copy.itemVars.blowpipeDartName = ctx.blowpipeDart.name;
		return copy;
	}

	private static int canonicalId(EquipmentPiece piece) {
		var data = com.wikigear.dps.GameData.get();
		return data == null ? piece.id : data.canonicalItemId(piece.id);
	}

	private static boolean requiresAmmo(PlayerEquipment eq) {
		return eq.weapon != null && EquipmentCalc.usesAmmo(canonicalId(eq.weapon));
	}

	private static boolean ammoOk(PlayerEquipment eq, @Nullable EquipmentPiece ammo) {
		if (!requiresAmmo(eq))
			return true;
		return ammo != null && EquipmentCalc.ammoApplicability(canonicalId(eq.weapon), canonicalId(ammo)) == EquipmentCalc.AmmoApplicability.INCLUDED;
	}

	/**
	 * Swap one slot at a time to whatever raises DPS the most, until nothing does
	 *
	 * @return the final DPS
	 */
	private double improve(PlayerEquipment eq, Combo combo, Set<String> locked) {
		double best = evaluate(eq, combo);
		for (int pass = 0; pass < MAX_PASSES; pass++) {
			boolean changed = false;
			for (String slot : ARMOUR_SLOTS) {
				if (locked.contains(slot) || (slot.equals("shield") && eq.weapon != null && eq.weapon.isTwoHanded))
					continue;

				EquipmentPiece current = eq.get(slot);
				EquipmentPiece bestPiece = current;
				var options = new ArrayList<EquipmentPiece>(ctx.candidates.getOrDefault(slot, List.of()));
				options.add(null);
				for (EquipmentPiece option : options) {
					if (option == current || (slot.equals("ammo") && !ammoOk(eq, option)))
						continue;
					eq.set(slot, option);
					double dps = evaluate(eq, combo);
					if (dps > best + EPSILON) {
						best = dps;
						bestPiece = option;
					}
				}
				eq.set(slot, bestPiece);
				changed |= bestPiece != current;
			}
			if (!changed)
				break;
		}
		return best;
	}

	/**
	 * Try putting on each owned set as a whole, then improving the other slots around it
	 */
	private double trySets(PlayerEquipment eq, Combo combo, double dps) {
		for (String[][] set : SETS) {
			var pieces = new LinkedHashMap<String, EquipmentPiece>();
			for (String[] slotNames : set) {
				EquipmentPiece piece = findOwned(slotNames);
				if (piece == null) {
					pieces = null;
					break;
				}
				pieces.put(slotNames[0], piece);
			}
			if (pieces == null || alreadyWearing(eq, pieces))
				continue;

			var trial = eq.copy();
			pieces.forEach(trial::set);
			if (!ammoOk(trial, trial.ammo))
				continue;
			double setDps = improve(trial, combo, pieces.keySet());
			if (setDps > dps + EPSILON) {
				dps = setDps;
				for (String slot : PlayerEquipment.SLOTS)
					eq.set(slot, trial.get(slot));
			}
		}
		return dps;
	}

	private static boolean alreadyWearing(PlayerEquipment eq, Map<String, EquipmentPiece> pieces) {
		for (var e : pieces.entrySet())
			if (e.getValue() != eq.get(e.getKey()))
				return false;
		return true;
	}

	@Nullable
	private EquipmentPiece findOwned(String[] slotNames) {
		var owned = ctx.candidates.getOrDefault(slotNames[0], List.of());
		for (int i = 1; i < slotNames.length; i++)
			for (EquipmentPiece piece : owned)
				if (piece.name.equalsIgnoreCase(slotNames[i]))
					return piece;
		return null;
	}

	private Player player(PlayerEquipment eq, Combo combo) {
		var p = new Player();
		p.name = "optimiser";
		p.style = combo.style;
		p.skills = ctx.skills;
		p.boosts = boosts;
		p.equipment = eq.copy();
		p.prayers = new ArrayList<>();
		for (Prayer prayer : prayers)
			p.prayers.add(prayer.ordinal());
		p.spell = combo.spell;
		p.buffs = new Player.Buffs();
		p.buffs.onSlayerTask = ctx.onSlayerTask;
		p.buffs.inWilderness = ctx.inWilderness;
		p.buffs.kandarinDiary = ctx.kandarinDiary;
		// The calculator's defaults: the best range for medium fuse chinchompas
		p.buffs.chinchompaDistance = 4;
		EquipmentCalc.applyEquipmentBonuses(p, baseMonster);
		return p;
	}

	private PlayerVsNPCCalc calc(PlayerEquipment eq, Combo combo) {
		var opts = new BaseCalc.CalcOpts();
		// The monster is scaled once up front
		opts.disableMonsterScaling = true;
		return new PlayerVsNPCCalc(player(eq, combo), scaledMonster, opts);
	}

	private double evaluate(PlayerEquipment eq, Combo combo) {
		try {
			double dps = calc(eq, combo).getDps();
			return Double.isFinite(dps) ? dps : 0;
		} catch (RuntimeException ex) {
			// An odd combination the calculator can't handle is never the best one
			return 0;
		}
	}

	private Result result(PlayerEquipment eq, Combo combo) {
		var calc = calc(eq, combo);
		var r = new Result();
		r.equipment = eq.copy();
		r.style = combo.style;
		r.spell = combo.spell;
		r.prayers = prayers;
		r.potions = usefulPotions();
		r.dps = calc.getDps();
		r.maxHit = calc.getDistribution().getMax();
		r.accuracy = calc.getHitChance();
		r.attackSpeed = calc.getAttackSpeed();
		try {
			r.ttk = calc.getTtk();
		} catch (RuntimeException ex) {
			r.ttk = Double.NaN;
		}
		return r;
	}

	/**
	 * The potions giving the highest boost to the skills this style uses
	 */
	private List<Potion> usefulPotions() {
		int[] skills;
		switch (family) {
			case MELEE:
				skills = new int[] { 0, 1 };
				break;
			case RANGED:
				skills = new int[] { 4 };
				break;
			default:
				skills = new int[] { 3 };
				break;
		}
		var result = new ArrayList<Potion>();
		for (int skill : skills) {
			Potion best = null;
			double bestBoost = 0;
			for (Potion p : ctx.potions) {
				Double boost = p.boosts(ctx.skills)[skill];
				if (boost != null && boost > bestBoost) {
					best = p;
					bestBoost = boost;
				}
			}
			if (best != null && !result.contains(best))
				result.add(best);
		}
		return result;
	}
}
