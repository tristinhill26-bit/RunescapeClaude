package com.wikigear;

import lombok.Builder;
import lombok.Value;

/**
 * The equipment stats of an item, as far as picking gear is concerned
 */
@Value
@Builder
public class ItemBonuses
{
	/**
	 * Index in the equipment inventory, matching {@link net.runelite.api.EquipmentInventorySlot}
	 */
	int slot;
	boolean twoHanded;
	int stab;
	int slash;
	int crush;
	int magic;
	int ranged;
	int strength;
	int rangedStrength;
	float magicDamage;
	int prayer;
	/**
	 * Sum of all defence bonuses
	 */
	int defence;
}
