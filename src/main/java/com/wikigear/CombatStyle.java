package com.wikigear;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * The combat style a setup is for, used to rank items the wiki didn't recommend
 */
public enum CombatStyle
{
	MELEE,
	RANGED,
	MAGIC;

	/**
	 * How much an item helps this style. Offence matters most, with a little weight on prayer and defence.
	 */
	public double score(ItemBonuses bonuses) {
		double offence;
		switch (this) {
			case RANGED:
				offence = bonuses.getRangedStrength() + bonuses.getRanged() * .5;
				break;
			case MAGIC:
				offence = bonuses.getMagicDamage() * 3 + bonuses.getMagic() * .5;
				break;
			default:
				offence = bonuses.getStrength() + Math.max(bonuses.getStab(), Math.max(bonuses.getSlash(), bonuses.getCrush())) * .5;
				break;
		}
		return offence + bonuses.getPrayer() * .1 + bonuses.getDefence() * .005;
	}

	/**
	 * The styles a setup's label names, in the order {@link #infer} prefers them, e.g. Magic then Ranged for
	 * "Max Magic & Ranged". A hybrid setup names more than one.
	 */
	public static List<CombatStyle> mentioned(String label) {
		String lower = label.toLowerCase(Locale.ROOT);
		var styles = new ArrayList<CombatStyle>();
		if (lower.contains("magic") || lower.contains("mage"))
			styles.add(MAGIC);
		if (lower.contains("rang"))
			styles.add(RANGED);
		if (lower.contains("melee") || lower.contains("stab") || lower.contains("slash") || lower.contains("crush"))
			styles.add(MELEE);
		return styles;
	}

	/**
	 * Work out a setup's style from its label, e.g. "Ranged", or else from the bonuses of its weapon.
	 */
	@Nullable
	public static CombatStyle infer(String label, Map<GearSlot, ItemBonuses> weaponBonuses) {
		String lower = label.toLowerCase(Locale.ROOT);
		if (lower.contains("magic") || lower.contains("mage"))
			return MAGIC;
		if (lower.contains("rang"))
			return RANGED;
		if (lower.contains("melee") || lower.contains("stab") || lower.contains("slash") || lower.contains("crush"))
			return MELEE;

		ItemBonuses weapon = weaponBonuses.get(GearSlot.TWO_HANDED);
		if (weapon == null)
			weapon = weaponBonuses.get(GearSlot.WEAPON);
		if (weapon == null)
			return null;

		int melee = Math.max(weapon.getStab(), Math.max(weapon.getSlash(), weapon.getCrush()));
		if (weapon.getMagic() > melee && weapon.getMagic() > weapon.getRanged())
			return MAGIC;
		if (weapon.getRanged() > melee)
			return RANGED;
		return MELEE;
	}
}
