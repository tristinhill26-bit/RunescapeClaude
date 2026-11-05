/*
 * Translated to Java from the OSRS Wiki DPS calculator, https://github.com/weirdgloop/osrs-dps-calc
 * Copyright (C) Jayden Bailey (Weird Gloop) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License along with this program, see
 * licenses/osrs-dps-calc-GPL-3.0.txt, or https://www.gnu.org/licenses/.
 */
package com.wikigear.dps;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * The calculator's equipment, monster, spell and alias data
 */
public class GameData
{
	private static final Pattern LEADING_INT = Pattern.compile("^\\s*([+-]?\\d+)");

	private static volatile GameData current;

	public final List<EquipmentPiece> equipment;
	public final List<Monster> monsters;
	public final List<Spell> spells;
	/**
	 * Variant item ID to base item ID
	 */
	public final Map<Integer, Integer> aliases;

	private final Map<Integer, EquipmentPiece> equipmentById = new HashMap<>();

	public GameData(List<EquipmentPiece> equipment, List<Monster> monsters, List<Spell> spells, Map<Integer, Integer> aliases) {
		this.equipment = Collections.unmodifiableList(equipment);
		this.monsters = Collections.unmodifiableList(monsters);
		this.spells = Collections.unmodifiableList(spells);
		this.aliases = aliases;
		for (EquipmentPiece e : equipment)
			equipmentById.putIfAbsent(e.id, e);
	}

	public static GameData get() {
		return current;
	}

	/**
	 * Makes this the data the calculator uses
	 */
	public void install() {
		Spells.spells = spells;
		current = this;
	}

	public EquipmentPiece equipmentById(int id) {
		return equipmentById.get(id);
	}

	public int canonicalItemId(int itemId) {
		return aliases.getOrDefault(itemId, itemId);
	}

	/**
	 * The snapshot of the calculator's data bundled with the plugin, for when it can't be downloaded
	 */
	public static GameData loadBundled(Gson gson) throws IOException {
		return parse(gson,
			readBundled("equipment.json.gz"),
			readBundled("monsters.json.gz"),
			readBundled("spells.json.gz"),
			readBundled("equipment_aliases.json.gz"));
	}

	private static String readBundled(String name) throws IOException {
		try (InputStream raw = GameData.class.getResourceAsStream(name)) {
			if (raw == null)
				throw new IOException("Missing bundled DPS data " + name);
			try (var in = new GZIPInputStream(raw)) {
				var out = new ByteArrayOutputStream();
				in.transferTo(out);
				return out.toString(StandardCharsets.UTF_8);
			}
		}
	}

	public static GameData parse(Gson gson, String equipmentJson, String monstersJson, String spellsJson, String aliasesJson) {
		List<EquipmentPiece> equipment = gson.fromJson(equipmentJson, new TypeToken<List<EquipmentPiece>>() {}.getType());
		for (EquipmentPiece e : equipment) {
			if (e.bonuses == null)
				e.bonuses = new Bonuses();
			if (e.offensive == null)
				e.offensive = new StyleBonuses();
			if (e.defensive == null)
				e.defensive = new StyleBonuses();
			if (e.category == null)
				e.category = EquipmentCategory.NONE;
		}

		List<Spell> spells = gson.fromJson(spellsJson, new TypeToken<List<Spell>>() {}.getType());

		Map<String, Integer> rawAliases = gson.fromJson(aliasesJson, new TypeToken<Map<String, Integer>>() {}.getType());
		var aliases = new HashMap<Integer, Integer>();
		rawAliases.forEach((k, v) -> aliases.put(Integer.parseInt(k), v));

		var monsters = new ArrayList<Monster>();
		JsonArray raw = gson.fromJson(monstersJson, JsonArray.class);
		for (JsonElement el : raw)
			monsters.add(parseMonster(gson, el.getAsJsonObject()));

		return new GameData(equipment, monsters, spells, aliases);
	}

	/**
	 * Converts a raw monster like the calculator's getMonsters
	 */
	private static Monster parseMonster(Gson gson, JsonObject m) {
		var monster = new Monster();
		monster.id = m.get("id").getAsInt();
		monster.name = str(m, "name");
		monster.version = str(m, "version");
		monster.image = str(m, "image");
		monster.size = num(m, "size");
		monster.speed = num(m, "speed");

		String style = null;
		if (m.has("style") && m.get("style").isJsonArray()) {
			var parts = new ArrayList<String>();
			for (JsonElement s : m.getAsJsonArray("style"))
				parts.add(s.isJsonNull() ? "null" : s.getAsString());
			String joined = String.join(",", parts).toLowerCase();
			if (!joined.isEmpty() && CombatStyles.isCombatStyleType(joined))
				style = joined;
		}
		monster.style = style;

		double maxHit = 0;
		if (m.has("max_hit") && !m.get("max_hit").isJsonNull()) {
			Matcher matcher = LEADING_INT.matcher(m.get("max_hit").getAsString());
			if (matcher.find())
				maxHit = Double.parseDouble(matcher.group(1));
		}
		monster.maxHit = maxHit;

		monster.skills = gson.fromJson(m.get("skills"), Monster.Skills.class);
		monster.offensive = gson.fromJson(m.get("offensive"), Monster.Offensive.class);
		monster.defensive = gson.fromJson(m.get("defensive"), Monster.Defensive.class);
		if (monster.skills == null)
			monster.skills = new Monster.Skills();
		if (monster.offensive == null)
			monster.offensive = new Monster.Offensive();
		if (monster.defensive == null)
			monster.defensive = new Monster.Defensive();

		monster.attributes = new ArrayList<>();
		if (m.has("attributes") && m.get("attributes").isJsonArray())
			for (JsonElement a : m.getAsJsonArray("attributes"))
				monster.attributes.add(a.getAsString());

		if (m.has("weakness") && m.get("weakness").isJsonObject())
			monster.weakness = gson.fromJson(m.get("weakness"), Monster.Weakness.class);

		monster.immunities = new Monster.Immunities();
		if (m.has("immunities") && m.get("immunities").isJsonObject()) {
			var burn = m.getAsJsonObject("immunities").get("burn");
			if (burn != null && !burn.isJsonNull())
				monster.immunities.burn = burn.getAsString();
		}

		monster.is_slayer_monster = m.has("is_slayer_monster") && !m.get("is_slayer_monster").isJsonNull()
			&& m.get("is_slayer_monster").getAsBoolean();
		monster.inputs = Monster.Inputs.initial();
		return monster;
	}

	private static String str(JsonObject o, String key) {
		var e = o.get(key);
		return e == null || e.isJsonNull() ? "" : e.getAsString();
	}

	private static double num(JsonObject o, String key) {
		var e = o.get(key);
		return e == null || e.isJsonNull() ? 0 : e.getAsDouble();
	}
}
