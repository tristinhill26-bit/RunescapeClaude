package com.shadowtiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import org.junit.Test;

public class FallingRockSizeTest
{
	private static HazardDefinition find(BuiltInHazards group, int id)
	{
		return group.getDefinitions().stream().filter(d -> d.getId() == id).findFirst().orElseThrow(AssertionError::new);
	}

	@Test
	public void babaRocksCanBeOneTileOr3x3()
	{
		HazardDefinition rock = find(BuiltInHazards.BA_BA, 2250);
		assertEquals(1, rock.getRadius());
		assertEquals(0, FallingRockSize.SOURCE_TILE.apply(rock).getRadius());
		assertEquals(1, FallingRockSize.THREE_BY_THREE.apply(rock).getRadius());
		assertSame(rock, FallingRockSize.PER_ATTACK.apply(rock));
	}

	@Test
	public void sharedShadowsAreResizedToo()
	{
		HazardDefinition shadow = find(BuiltInHazards.FALLING_SHADOWS, 1447);
		assertEquals(0, shadow.getRadius());
		assertEquals(1, FallingRockSize.THREE_BY_THREE.apply(shadow).getRadius());
		assertEquals("keeps its other fields", shadow.getLabel(), FallingRockSize.THREE_BY_THREE.apply(shadow).getLabel());
	}

	@Test
	public void otherHazardsKeepTheirSize()
	{
		HazardDefinition boulder = find(BuiltInHazards.BA_BA, 11782); // rolling boulder NPC
		assertSame(boulder, FallingRockSize.THREE_BY_THREE.apply(boulder));
		HazardDefinition firebomb = find(BuiltInHazards.VORKATH, 1481);
		assertSame(firebomb, FallingRockSize.SOURCE_TILE.apply(firebomb));
	}
}
