package com.shadowtiles;

import lombok.RequiredArgsConstructor;

/**
 * How big to draw built-in falling rock and debris shadows.
 */
@RequiredArgsConstructor
public enum FallingRockSize
{
	PER_ATTACK("Per attack", -1),
	SOURCE_TILE("Source tile", 0),
	THREE_BY_THREE("3x3", 1);

	private final String displayName;
	/**
	 * The radius to force, or -1 to keep each attack's own.
	 */
	private final int radius;

	/**
	 * The definition with this size applied, if it's a falling rock.
	 */
	HazardDefinition apply(HazardDefinition def)
	{
		if (radius < 0 || !BuiltInHazards.isFallingRock(def))
		{
			return def;
		}
		return new HazardDefinition(def.getType(), def.getId(), radius, def.getLabel(), def.getColor(), def.getTicks());
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
