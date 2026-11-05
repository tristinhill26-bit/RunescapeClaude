package com.shadowtiles;

import java.util.Locale;

/**
 * How a hazard shows up in the client, which decides what we watch for.
 */
public enum HazardType
{
	/**
	 * A graphics object (spot anim) drawn on the ground, e.g. falling rock shadows.
	 * The highlighted area is centred on the graphic.
	 */
	GRAPHIC,
	/**
	 * A projectile in flight. The highlighted area is centred on where it will land
	 * and stays until the projectile reaches its target.
	 */
	PROJECTILE,
	/**
	 * A game or ground object, e.g. acid pools or poison clouds.
	 */
	OBJECT,
	/**
	 * An NPC whose footprint (plus radius) is dangerous, e.g. rolling boulders.
	 */
	NPC,
	/**
	 * An NPC animation. The area around the animating NPC is highlighted for a
	 * fixed number of game ticks, e.g. a boss slam.
	 */
	ANIM;

	static HazardType parse(String s)
	{
		switch (s.trim().toLowerCase(Locale.ROOT))
		{
			case "graphic":
			case "graphics":
			case "gfx":
			case "spotanim":
				return GRAPHIC;
			case "projectile":
			case "proj":
				return PROJECTILE;
			case "object":
			case "obj":
				return OBJECT;
			case "npc":
				return NPC;
			case "anim":
			case "animation":
				return ANIM;
			default:
				return null;
		}
	}
}
