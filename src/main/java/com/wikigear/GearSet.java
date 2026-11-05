package com.wikigear;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Getter;

/**
 * Gear which is only worth wearing as a complete set, since the pieces do little on their own
 */
public enum GearSet
{
	VOID("Void knight equipment",
		piece(GearSlot.HEAD, "Void melee helm", "Void ranger helm", "Void mage helm"),
		piece(GearSlot.BODY, "Elite void top", "Void knight top"),
		piece(GearSlot.LEGS, "Elite void robe", "Void knight robe"),
		piece(GearSlot.HANDS, "Void knight gloves"));

	@Getter
	private final String name;
	private final List<Piece> pieces;

	GearSet(String name, Piece... pieces) {
		this.name = name;
		this.pieces = List.of(pieces);
	}

	/**
	 * Whether the item belongs to this set
	 */
	public boolean contains(String itemName) {
		String name = GearPicker.stripVariant(GearPicker.normalize(itemName));
		for (var piece : pieces)
			for (var option : piece.options)
				if (GearPicker.normalize(option).equals(name))
					return true;
		return false;
	}

	public List<String> allPieceNames() {
		var names = new ArrayList<String>();
		for (var piece : pieces)
			for (var option : piece.options)
				names.add(GearPicker.normalize(option));
		return names;
	}

	/**
	 * Find the best owned item for every piece of the set.
	 *
	 * @return the pieces by slot, or null if the player is missing any piece
	 */
	@Nullable
	public Map<GearSlot, GearPicker.OwnedItem> findPieces(GearPicker.OwnedItems owned, @Nullable CombatStyle style) {
		var found = new EnumMap<GearSlot, GearPicker.OwnedItem>(GearSlot.class);
		for (var piece : pieces) {
			GearPicker.OwnedItem item = null;
			for (var option : piece.orderFor(style)) {
				item = owned.findItem(option);
				if (item != null)
					break;
			}
			if (item == null)
				return null;
			found.put(piece.slot, item);
		}
		return found;
	}

	private static Piece piece(GearSlot slot, String... options) {
		return new Piece(slot, List.of(options));
	}

	private static class Piece
	{
		final GearSlot slot;
		/**
		 * Item names in order of preference
		 */
		final List<String> options;

		Piece(GearSlot slot, List<String> options) {
			this.slot = slot;
			this.options = options;
		}

		/**
		 * Options in order of preference, with the helm matching the combat style first
		 */
		List<String> orderFor(@Nullable CombatStyle style) {
			if (style == null || slot != GearSlot.HEAD)
				return options;
			String keyword = style == CombatStyle.MAGIC ? "mage" : style == CombatStyle.RANGED ? "ranger" : "melee";
			var ordered = new ArrayList<String>();
			for (var option : options)
				if (option.toLowerCase().contains(keyword))
					ordered.add(option);
			for (var option : options)
				if (!ordered.contains(option))
					ordered.add(option);
			return ordered;
		}
	}
}
