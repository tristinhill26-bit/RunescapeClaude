package net.runelite.client.plugins.shadowtiles;

import java.awt.Color;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * One kind of dangerous thing: "graphic 2250 hits a 3x3 around itself", etc.
 */
@Value
public class HazardDefinition
{
	/**
	 * Default duration for {@link HazardType#ANIM} hazards when none is given.
	 */
	static final int DEFAULT_ANIM_TICKS = 3;

	HazardType type;
	int id;
	/**
	 * Extra tiles around the source. 0 = just the tile (or NPC footprint), 1 = 3x3, 2 = 5x5.
	 */
	int radius;
	@Nullable
	String label;
	/**
	 * Overrides the configured highlight colour when set.
	 */
	@Nullable
	Color color;
	/**
	 * Only used by {@link HazardType#ANIM}: how many game ticks to keep the area highlighted.
	 */
	int ticks;

	static HazardDefinition of(HazardType type, int id, int radius, String label)
	{
		return new HazardDefinition(type, id, radius, label, null, DEFAULT_ANIM_TICKS);
	}

	/**
	 * Key used to match a definition to something seen in game.
	 */
	long key()
	{
		return key(type, id);
	}

	static long key(HazardType type, int id)
	{
		return ((long) type.ordinal() << 32) | (id & 0xFFFFFFFFL);
	}
}
