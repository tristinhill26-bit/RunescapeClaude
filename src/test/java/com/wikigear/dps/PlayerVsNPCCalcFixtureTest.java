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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Checks the Java port against results recorded from the original TypeScript calculator on random scenarios.
 * Set the dps.fixtures system property to a (optionally gzipped) fixture file to run a different set.
 */
public class PlayerVsNPCCalcFixtureTest
{
	private static final String[] FIELDS = {
		"npcDefRoll", "maxAttackRoll", "accuracy", "displayAccuracy", "maxHit", "minHit", "expectedHit",
		"attackSpeed", "expectedAttackSpeed", "dps", "dotExpected", "htk", "ttk",
	};

	private static final Gson GSON = new Gson();

	@BeforeClass
	public static void loadData() throws IOException {
		GameData.loadBundled(GSON).install();
	}

	private static JsonArray loadFixtures() throws IOException {
		String path = System.getProperty("dps.fixtures");
		InputStream raw = path != null
			? new FileInputStream(path)
			: PlayerVsNPCCalcFixtureTest.class.getResourceAsStream("fixtures.json.gz");
		assertNotNull("fixtures.json.gz is missing", raw);
		boolean gzipped = path == null || path.endsWith(".gz");
		try (var in = gzipped ? new GZIPInputStream(raw) : raw;
			var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			return GSON.fromJson(reader, JsonArray.class);
		}
	}

	private static boolean close(double expected, double actual) {
		if (expected == actual)
			return true;
		double diff = Math.abs(expected - actual);
		return diff <= 1e-9 || diff <= 1e-9 * Math.max(Math.abs(expected), Math.abs(actual));
	}

	private static double get(PlayerVsNPCCalc calc, String field) {
		switch (field) {
			case "npcDefRoll":
				return calc.getNPCDefenceRoll();
			case "maxAttackRoll":
				return calc.getMaxAttackRoll();
			case "accuracy":
				return calc.getHitChance();
			case "displayAccuracy":
				return calc.getDisplayHitChance();
			case "maxHit":
				return calc.getDistribution().getMax();
			case "minHit":
				return calc.getDistribution().getMin();
			case "expectedHit":
				return calc.getDistribution().getExpectedDamage();
			case "attackSpeed":
				return calc.getAttackSpeed();
			case "expectedAttackSpeed":
				return calc.getExpectedAttackSpeed();
			case "dps":
				return calc.getDps();
			case "dotExpected":
				return calc.getDoTExpected();
			case "htk":
				return calc.getHtk();
			case "ttk":
				return calc.getTtk();
			default:
				throw new IllegalArgumentException(field);
		}
	}

	private static String describe(int i, JsonObject entry, String mode) {
		var p = entry.getAsJsonObject("player");
		var eq = p.getAsJsonObject("equipment");
		var names = new ArrayList<String>();
		for (String slot : PlayerEquipment.SLOTS) {
			var e = eq.get(slot);
			if (e != null && e.isJsonObject())
				names.add(e.getAsJsonObject().get("name").getAsString());
		}
		return "#" + i + " " + mode + " vs " + entry.getAsJsonObject("monster").get("name").getAsString()
			+ " style=" + p.get("style") + " spell=" + p.get("spell") + " gear=" + names;
	}

	private static void check(List<String> failures, int i, JsonObject entry, String mode, boolean spec) {
		JsonElement expectedEl = entry.get(mode);
		Player player = GSON.fromJson(entry.get("player"), Player.class);
		Monster monster = GSON.fromJson(entry.get("monster"), Monster.class);
		var opts = new BaseCalc.CalcOpts();
		opts.usingSpecialAttack = spec;

		// the original calculator threw on this scenario
		if (expectedEl == null)
			return;

		JsonObject expected = expectedEl.getAsJsonObject();
		try {
			var calc = new PlayerVsNPCCalc(player, monster, opts);
			for (String field : FIELDS) {
				JsonElement e = expected.get(field);
				double actual = get(calc, field);
				boolean ok = e == null || e.isJsonNull()
					? Double.isNaN(actual) || Double.isInfinite(actual)
					: close(e.getAsDouble(), actual);
				if (!ok) {
					failures.add(describe(i, entry, mode) + ": " + field + " expected " + e + " but was " + actual);
					return;
				}
			}
		} catch (RuntimeException ex) {
			failures.add(describe(i, entry, mode) + ": threw " + ex);
		}
	}

	@Test
	public void matchesReferenceCalculator() throws IOException {
		var fixtures = loadFixtures();
		var failures = new ArrayList<String>();
		for (int i = 0; i < fixtures.size(); i++) {
			var entry = fixtures.get(i).getAsJsonObject();
			check(failures, i, entry, "normal", false);
			check(failures, i, entry, "spec", true);
		}
		if (!failures.isEmpty()) {
			failures.stream().limit(40).forEach(System.err::println);
			fail(failures.size() + " of " + fixtures.size() * 2 + " calculations differ from the reference calculator");
		}
	}

	@Test
	public void equipmentBonusesMatchReferenceCalculator() throws IOException {
		var fixtures = loadFixtures();
		var failures = new ArrayList<String>();
		for (int i = 0; i < fixtures.size(); i++) {
			var entry = fixtures.get(i).getAsJsonObject();
			Player expected = GSON.fromJson(entry.get("player"), Player.class);
			Player player = GSON.fromJson(entry.get("player"), Player.class);
			Monster monster = GSON.fromJson(entry.get("monster"), Monster.class);
			EquipmentCalc.applyEquipmentBonuses(player, monster);
			String a = GSON.toJson(new Object[] { expected.bonuses, expected.offensive, expected.defensive, expected.attackSpeed });
			String b = GSON.toJson(new Object[] { player.bonuses, player.offensive, player.defensive, player.attackSpeed });
			if (!a.equals(b))
				failures.add(describe(i, entry, "bonuses") + ": expected " + a + " but was " + b);
		}
		if (!failures.isEmpty()) {
			failures.stream().limit(40).forEach(System.err::println);
			fail(failures.size() + " equipment bonus totals differ from the reference calculator");
		}
	}
}
