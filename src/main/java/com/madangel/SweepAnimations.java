package com.madangel;

import java.util.HashMap;
import java.util.Map;

/**
 * Mad Angel's sweep animations (names from net.runelite.api.gameval.AnimationID).
 * <p>
 * She draws her sword back to one side, cleaves, and the sword swaps sides between cleaves:
 * {@code SWEEP_RIGHT_LEFT} swings from the right and ends with the sword on the left. For each
 * animation this records the side the sword is on for the <em>next</em> cleave, which is the
 * one you still have time to dodge.
 */
final class SweepAnimations
{
	enum Side
	{
		LEFT,
		RIGHT;

		Side opposite()
		{
			return this == LEFT ? RIGHT : LEFT;
		}
	}

	/**
	 * Animations that end the sweep.
	 */
	static final int SWEEP_RIGHT_RESET = 14432;
	static final int SWEEP_LEFT_RESET = 14436;

	private static final Map<Integer, Side> SWORD_SIDE = new HashMap<>();

	static
	{
		SWORD_SIDE.put(14429, Side.RIGHT); // NPC_MAD_ANGEL_ATTACK_SWEEP_RIGHT_01
		SWORD_SIDE.put(14430, Side.RIGHT); // NPC_MAD_ANGEL_ATTACK_SWEEP_RIGHT_02
		SWORD_SIDE.put(14431, Side.LEFT); // NPC_MAD_ANGEL_ATTACK_SWEEP_RIGHT_LEFT
		SWORD_SIDE.put(14433, Side.LEFT); // NPC_MAD_ANGEL_ATTACK_SWEEP_LEFT_01
		SWORD_SIDE.put(14434, Side.LEFT); // NPC_MAD_ANGEL_ATTACK_SWEEP_LEFT_02
		SWORD_SIDE.put(14435, Side.RIGHT); // NPC_MAD_ANGEL_ATTACK_SWEEP_LEFT_RIGHT
		SWORD_SIDE.put(14437, Side.RIGHT); // NPC_MAD_ANGEL_ATTACK_SWEEP_RIGHT_ENRAGED_01
		SWORD_SIDE.put(14438, Side.RIGHT); // NPC_MAD_ANGEL_ATTACK_SWEEP_RIGHT_ENRAGED_02
		SWORD_SIDE.put(14439, Side.LEFT); // NPC_MAD_ANGEL_ATTACK_SWEEP_RIGHT_LEFT_ENRAGED
		SWORD_SIDE.put(14440, Side.LEFT); // NPC_MAD_ANGEL_ATTACK_SWEEP_LEFT_ENRAGED_01
		SWORD_SIDE.put(14441, Side.LEFT); // NPC_MAD_ANGEL_ATTACK_SWEEP_LEFT_ENRAGED_02
		SWORD_SIDE.put(14442, Side.RIGHT); // NPC_MAD_ANGEL_ATTACK_SWEEP_LEFT_RIGHT_ENRAGED
	}

	private SweepAnimations()
	{
	}

	/**
	 * The side the sword will cleave from next, or null if this isn't a sweep animation.
	 */
	static Side swordSide(int animation)
	{
		return SWORD_SIDE.get(animation);
	}

	static boolean isReset(int animation)
	{
		return animation == SWEEP_RIGHT_RESET || animation == SWEEP_LEFT_RESET;
	}
}
