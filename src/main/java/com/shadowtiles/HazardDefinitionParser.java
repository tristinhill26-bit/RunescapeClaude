package com.shadowtiles;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import lombok.Value;

/**
 * Parses the "Custom attacks" config box. One attack per line:
 *
 * <pre>
 * type, id, radius, label, color, ticks
 * </pre>
 *
 * Only {@code type} and {@code id} are required. Lines starting with {@code #} or {@code //}
 * are comments. Examples:
 *
 * <pre>
 * graphic, 2250, 1, Ba-Ba rock
 * projectile, 1481, 1, Firebomb, #FF8000
 * anim, 12345, 2, Slam, , 4
 * </pre>
 */
final class HazardDefinitionParser
{
	@Value
	static class Result
	{
		List<HazardDefinition> definitions;
		List<String> errors;
	}

	private HazardDefinitionParser()
	{
	}

	static Result parse(String text)
	{
		List<HazardDefinition> definitions = new ArrayList<>();
		List<String> errors = new ArrayList<>();
		if (text == null)
		{
			return new Result(definitions, errors);
		}

		String[] lines = text.split("\\r?\\n");
		for (int i = 0; i < lines.length; i++)
		{
			String line = lines[i].trim();
			if (line.isEmpty() || line.startsWith("#") || line.startsWith("//"))
			{
				continue;
			}

			try
			{
				definitions.add(parseLine(line));
			}
			catch (IllegalArgumentException e)
			{
				errors.add("line " + (i + 1) + ": " + e.getMessage());
			}
		}

		return new Result(definitions, errors);
	}

	private static HazardDefinition parseLine(String line)
	{
		String[] parts = line.split(",", -1);
		if (parts.length < 2)
		{
			throw new IllegalArgumentException("expected at least \"type, id\"");
		}

		HazardType type = HazardType.parse(parts[0]);
		if (type == null)
		{
			throw new IllegalArgumentException("unknown type \"" + parts[0].trim()
				+ "\" (use graphic, projectile, object, npc or anim)");
		}

		int id = parseInt(parts[1], "id", 0, Integer.MAX_VALUE);
		int radius = field(parts, 2).isEmpty() ? 0 : parseInt(parts[2], "radius", 0, 10);
		String label = field(parts, 3);
		Color color = field(parts, 4).isEmpty() ? null : parseColor(parts[4]);
		int ticks = field(parts, 5).isEmpty()
			? HazardDefinition.DEFAULT_ANIM_TICKS
			: parseInt(parts[5], "ticks", 1, 100);

		return new HazardDefinition(type, id, radius, label.isEmpty() ? null : label, color, ticks);
	}

	private static String field(String[] parts, int index)
	{
		return index < parts.length ? parts[index].trim() : "";
	}

	private static int parseInt(String s, String name, int min, int max)
	{
		int value;
		try
		{
			value = Integer.parseInt(s.trim());
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException(name + " \"" + s.trim() + "\" is not a number");
		}

		if (value < min || value > max)
		{
			throw new IllegalArgumentException(name + " must be between " + min + " and " + max);
		}
		return value;
	}

	private static Color parseColor(String s)
	{
		String hex = s.trim();
		if (hex.startsWith("#"))
		{
			hex = hex.substring(1);
		}

		if (!hex.matches("[0-9a-fA-F]{6}"))
		{
			throw new IllegalArgumentException("color \"" + s.trim() + "\" must look like #RRGGBB");
		}
		return new Color(Integer.parseInt(hex, 16));
	}
}
