package net.runelite.client.plugins.shadowtiles;

import lombok.Value;
import net.runelite.api.coords.LocalPoint;

/**
 * A hazard that is on screen right now, resolved to the square of tiles it covers.
 */
@Value
class ActiveHazard
{
	/**
	 * Centre of the dangerous square, in local coordinates.
	 */
	LocalPoint center;
	/**
	 * Side length of the dangerous square, in tiles.
	 */
	int width;
	HazardDefinition definition;
	/**
	 * Game ticks until it lands / ends, or -1 when unknown.
	 */
	int ticksLeft;
}
