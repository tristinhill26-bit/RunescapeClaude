package net.runelite.client.plugins.shadowtiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import org.junit.Test;

public class HazardDefinitionParserTest
{
	@Test
	public void parsesFullAndMinimalLines()
	{
		HazardDefinitionParser.Result result = HazardDefinitionParser.parse(
			"# comment\n"
				+ "\n"
				+ "graphic, 2250, 1, Ba-Ba rock, #FF8000\n"
				+ "projectile,1481\n"
				+ "anim, 9000, 2, Slam, , 5\n");

		assertTrue(result.getErrors().isEmpty());
		assertEquals(3, result.getDefinitions().size());

		HazardDefinition rock = result.getDefinitions().get(0);
		assertEquals(HazardType.GRAPHIC, rock.getType());
		assertEquals(2250, rock.getId());
		assertEquals(1, rock.getRadius());
		assertEquals("Ba-Ba rock", rock.getLabel());
		assertEquals(new Color(0xFF8000), rock.getColor());

		HazardDefinition bomb = result.getDefinitions().get(1);
		assertEquals(HazardType.PROJECTILE, bomb.getType());
		assertEquals(0, bomb.getRadius());
		assertNull(bomb.getLabel());
		assertNull(bomb.getColor());
		assertEquals(HazardDefinition.DEFAULT_ANIM_TICKS, bomb.getTicks());

		HazardDefinition slam = result.getDefinitions().get(2);
		assertEquals(HazardType.ANIM, slam.getType());
		assertNull(slam.getColor());
		assertEquals(5, slam.getTicks());
	}

	@Test
	public void reportsBadLinesAndKeepsGoodOnes()
	{
		HazardDefinitionParser.Result result = HazardDefinitionParser.parse(
			"laser, 1\n"
				+ "graphic, abc\n"
				+ "npc, 11782, 0, Boulder\n"
				+ "graphic, 1, 1, x, red\n"
				+ "graphic\n");

		assertEquals(1, result.getDefinitions().size());
		assertEquals(4, result.getErrors().size());
		assertTrue(result.getErrors().get(0).startsWith("line 1:"));
		assertTrue(result.getErrors().get(3).startsWith("line 5:"));
	}

	@Test
	public void customLineOverridesBuiltInKey()
	{
		HazardDefinition builtIn = BuiltInHazards.BA_BA.getDefinitions().get(0);
		HazardDefinition custom = HazardDefinitionParser.parse("gfx, " + builtIn.getId() + ", 2").getDefinitions().get(0);
		assertEquals(builtIn.key(), custom.key());
	}

	@Test
	public void defaultCustomTextIsAllComments()
	{
		HazardDefinitionParser.Result result = HazardDefinitionParser.parse(new ShadowTilesConfig()
		{
		}.customDefinitions());
		assertTrue(result.getDefinitions().isEmpty());
		assertTrue(result.getErrors().isEmpty());
	}
}
