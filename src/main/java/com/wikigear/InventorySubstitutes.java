package com.wikigear;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Finds replacements for recommended inventory items the player doesn't own.
 * <ul>
 *   <li>Potions are swapped within groups that do the same job, like Super restore and Prayer potion.</li>
 *   <li>Food is swapped for the player's best healing food, preferring food they have enough of.</li>
 * </ul>
 */
public class InventorySubstitutes implements GearPicker.Substitutes
{
	/**
	 * Potions which do the same job, best first. Doses don't matter.
	 */
	static final List<List<String>> POTION_GROUPS = List.of(
		List.of("Super restore", "Prayer potion", "Blighted super restore", "Sanfew serum"),
		List.of("Divine super combat potion", "Super combat potion", "Combat potion", "Divine super strength potion",
			"Super strength", "Strength potion"),
		List.of("Divine bastion potion", "Bastion potion", "Divine ranging potion", "Ranging potion"),
		List.of("Divine battlemage potion", "Battlemage potion", "Divine magic potion", "Magic potion"),
		List.of("Extended super antifire", "Super antifire potion", "Extended antifire", "Antifire potion"),
		List.of("Anti-venom+", "Anti-venom", "Antidote++", "Antidote+", "Superantipoison", "Antipoison"),
		List.of("Stamina potion", "Super energy", "Energy potion"),
		List.of("Divine super defence potion", "Super defence", "Defence potion")
	);

	private final ToIntFunction<String> healByName;
	private final ToIntFunction<GearPicker.OwnedItem> healOfItem;

	/**
	 * @param healByName how much a food heals, by the wiki's name for it, or 0 if it isn't food
	 * @param healOfItem how much an owned item heals, or 0 if it isn't food
	 */
	public InventorySubstitutes(ToIntFunction<String> healByName, ToIntFunction<GearPicker.OwnedItem> healOfItem) {
		this.healByName = healByName;
		this.healOfItem = healOfItem;
	}

	@Override
	public List<GearPicker.OwnedItem> find(String wikiName, GearPicker.OwnedItems owned, int needed) {
		var result = new ArrayList<GearPicker.OwnedItem>();

		String base = GearPicker.stripVariant(GearPicker.normalize(wikiName));
		for (var group : POTION_GROUPS) {
			if (group.stream().noneMatch(potion -> GearPicker.normalize(potion).equals(base)))
				continue;
			for (var potion : group) {
				var item = owned.findItem(potion);
				if (item != null && !result.contains(item))
					result.add(item);
			}
			return result;
		}

		if (healByName.applyAsInt(wikiName) <= 0)
			return result;

		var foods = new ArrayList<FoodOption>();
		for (var item : owned.getAll()) {
			int heal = healOfItem.applyAsInt(item);
			if (heal > 0)
				foods.add(new FoodOption(item, heal, item.quantity >= needed));
		}
		// Prefer food there's enough of for every slot, then the food that heals most
		foods.sort(Comparator.comparing((FoodOption food) -> !food.enough)
			.thenComparing(food -> -food.heal)
			.thenComparing(food -> -food.item.quantity));
		for (var food : foods)
			result.add(food.item);
		return result;
	}

	private static class FoodOption
	{
		final GearPicker.OwnedItem item;
		final int heal;
		final boolean enough;

		FoodOption(GearPicker.OwnedItem item, int heal, boolean enough) {
			this.item = item;
			this.heal = heal;
			this.enough = enough;
		}
	}
}
