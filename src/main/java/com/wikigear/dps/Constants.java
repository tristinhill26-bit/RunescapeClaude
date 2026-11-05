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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generated from the calculator's lib/constants.ts
 */
public final class Constants
{
	private Constants() {
	}

	public static boolean has(int[] ids, int id) {
		for (int i : ids)
			if (i == id)
				return true;
		return false;
	}

	public static final int[] ABYSSAL_PORTAL_IDS = { 7533 };
	public static final List<String> ABYSSAL_SIRE_PHASES = Arrays.asList("Standard", "Transition");
	public static final int[] ABYSSAL_SIRE_TRANSITION_IDS = { 5886, 5889, 5891 };
	public static final double ACCURACY_PRECISION = 2;
	public static final int[] AKKHA_IDS = { 11789, 11790, 11791, 11792, 11793, 11794, 11795, 11796 };
	public static final int[] AKKHA_SHADOW_IDS = { 11797, 11798, 11799 };
	public static final int[] ALWAYS_ACCURATE_MONSTERS = { 931, 4135, 7691, 12335 };
	public static final Map<String, int[]> ALWAYS_MAX_HIT_MONSTERS = new HashMap<>();
	static {
		ALWAYS_MAX_HIT_MONSTERS.put("melee", new int[] { 11710, 11713, 12814, 11755, 11758, 14179, 14180 });
		ALWAYS_MAX_HIT_MONSTERS.put("ranged", new int[] { 11711, 11714, 12815, 11717, 11715, 14179, 14180 });
		ALWAYS_MAX_HIT_MONSTERS.put("magic", new int[] { 11709, 11712, 12816, 14151, 14150, 14179, 14180 });
	}
	public static final int[] ARAXXOR_IDS = { 13668 };
	public static final List<String> ARAXXOR_PHASES = Arrays.asList("Standard", "Enraged");
	public static final List<String> AUTOCAST_STANCES = Arrays.asList("Autocast", "Defensive Autocast");
	public static final int[] BABA_IDS = { 11778, 11779, 11780 };
	public static final int[] BA_ATTACKER_MONSTERS = { 1667, 5739, 5740, 5741, 5742, 5743, 5744, 5745, 5746, 5747, 1668, 5757, 5758, 5759, 5760, 5761, 5762, 5763, 5764, 5765 };
	public static final int[] BLOWPIPE_IDS = { 12926, 28688, 31575, 31579, 31583 };
	public static final List<String> CAST_STANCES = Arrays.asList("Autocast", "Defensive Autocast", "Manual Cast");
	public static final int[] CORRUPTED_GAUNTLET_MONSTER_IDS = { 9035, 9040, 9041, 9042, 9043, 9044, 9045, 9046, 9047, 9048 };
	public static final int[] COX_MAGIC_IS_DEFENSIVE_IDS = { 7559, 7540, 7543, 7544, 7545, 7533, 7530, 7531, 7532, 7538, 7539, 7552, 7555, 7550, 7553 };
	public static final int[] COX_USE_SINGLES_SCALING_IDS = { 7548, 7549, 7538, 7539 };
	public static final int[] DEATHLY_RANGER_IDS = { 7559 };
	public static final double DEFAULT_ATTACK_SPEED = 4;
	public static final int[] DOOM_OF_MOKHAIOTL_IDS = { 14707 };
	public static final List<String> DOOM_OF_MOKHAIOTL_PHASES = Arrays.asList("Normal", "Shielded", "Burrowing");
	public static final double DPS_PRECISION = 3;
	public static final int[] ECLIPSE_MOON_IDS = { 13012 };
	public static final double EXPECTED_HIT_PRECISION = 1;
	public static final int[] FRAGMENT_OF_SEREN_IDS = { 8917, 8918, 8919, 8920 };
	public static final int[] GAUNTLET_MONSTER_IDS = { 9021, 9026, 9027, 9028, 9029, 9030, 9031, 9032, 9033, 9034 };
	public static final int[] GLOWING_CRYSTAL_IDS = { 7568 };
	public static final int[] GUARANTEED_ACCURACY_MONSTERS = { 5916, 14180 };
	public static final int[] GUARDIAN_IDS = { 7569, 7571, 7570, 7572 };
	public static final int[] HUEYCOATL_BODY_IDS = { 14017 };
	public static final int[] HUEYCOATL_HEAD_IDS = { 14009, 14010, 14013 };
	public static final int[] HUEYCOATL_IDS = { 14009, 14010, 14013, 14017, 14014 };
	public static final List<String> HUEYCOATL_PHASES = Arrays.asList("Without Pillar", "With Pillar");
	public static final int[] HUEYCOATL_PHASE_IDS = { 14009, 14010, 14013, 14014 };
	public static final int[] HUEYCOATL_TAIL_IDS = { 14014 };
	public static final int[] ICE_DEMON_IDS = { 7584, 7585 };
	public static final int[] IMMUNE_TO_BURN_DAMAGE_NPC_IDS = { 7540, 7543, 7544, 7545, 7851, 7854, 7855, 7882, 7883, 7886, 7887, 7888, 7889, 7568, 2463, 2465, 2467, 2464, 2466, 2468, 2137, 2138, 2139, 2140, 2141, 2142 };
	public static final int[] IMMUNE_TO_MAGIC_DAMAGE_NPC_IDS = { 7851, 7854, 7855, 7882, 7883, 7886, 7887, 7888, 7889, 2463, 2465, 2467, 2464, 2466, 2468, 2137, 2138, 2139, 2140, 2141, 2142 };
	public static final int[] IMMUNE_TO_MELEE_DAMAGE_NPC_IDS = { 494, 7533, 7706, 7708, 12214, 12215, 12219, 2042, 2043, 2044 };
	public static final int[] IMMUNE_TO_NON_SALAMANDER_MELEE_DAMAGE_NPC_IDS = { 3169, 3170, 3171, 3172, 3173, 3174, 3175, 3176, 3177, 3178, 3179, 3180, 3181, 3182, 3183, 7037 };
	public static final int[] IMMUNE_TO_RANGED_DAMAGE_NPC_IDS = { 7540, 7543, 7544, 7545, 7851, 7854, 7855, 7882, 7883, 7886, 7887, 7888, 7889, 7568, 2463, 2465, 2467, 2464, 2466, 2468, 2137, 2138, 2139, 2140, 2141, 2142 };
	public static final int[] INFINITE_HEALTH_MONSTERS = { 14779 };
	public static final int[] KEPHRI_OVERLORD_IDS = { 11724, 11725, 11726 };
	public static final int[] KEPHRI_SHIELDED_IDS = { 11719 };
	public static final int[] KEPHRI_UNSHIELDED_IDS = { 11721 };
	public static final int[] MAD_ANGEL_IDS = { 16309, 16315, 16305, 16314 };
	public static final List<String> MAD_ANGEL_PHASES = Arrays.asList("Standard", "Sword Cleave", "Perfect Lightning");
	public static final int[] MAGGOT_KING_ID = { 15742 };
	public static final List<String> MAGGOT_KING_PHASES = Arrays.asList("Standard", "Melee Punish");
	public static final Map<Integer, List<String>> MONSTER_PHASES_BY_ID = new HashMap<>();
	static {
		MONSTER_PHASES_BY_ID.put(5886, Arrays.asList("Standard", "Transition"));
		MONSTER_PHASES_BY_ID.put(5889, Arrays.asList("Standard", "Transition"));
		MONSTER_PHASES_BY_ID.put(5891, Arrays.asList("Standard", "Transition"));
		MONSTER_PHASES_BY_ID.put(12596, Arrays.asList("In Melee Range", "Out of Melee Range"));
		MONSTER_PHASES_BY_ID.put(13599, Arrays.asList("Shielded", "Shielded (Defenceless)", "Unshielded"));
		MONSTER_PHASES_BY_ID.put(13600, Arrays.asList("Shielded", "Shielded (Defenceless)", "Unshielded"));
		MONSTER_PHASES_BY_ID.put(13601, Arrays.asList("Shielded", "Shielded (Defenceless)", "Unshielded"));
		MONSTER_PHASES_BY_ID.put(13602, Arrays.asList("Shielded", "Shielded (Defenceless)", "Unshielded"));
		MONSTER_PHASES_BY_ID.put(13603, Arrays.asList("Shielded", "Shielded (Defenceless)", "Unshielded"));
		MONSTER_PHASES_BY_ID.put(13604, Arrays.asList("Shielded", "Shielded (Defenceless)", "Unshielded"));
		MONSTER_PHASES_BY_ID.put(13605, Arrays.asList("Shielded", "Shielded (Defenceless)", "Unshielded"));
		MONSTER_PHASES_BY_ID.put(13606, Arrays.asList("Shielded", "Shielded (Defenceless)", "Unshielded"));
		MONSTER_PHASES_BY_ID.put(13668, Arrays.asList("Standard", "Enraged"));
		MONSTER_PHASES_BY_ID.put(14009, Arrays.asList("Without Pillar", "With Pillar"));
		MONSTER_PHASES_BY_ID.put(14010, Arrays.asList("Without Pillar", "With Pillar"));
		MONSTER_PHASES_BY_ID.put(14013, Arrays.asList("Without Pillar", "With Pillar"));
		MONSTER_PHASES_BY_ID.put(14014, Arrays.asList("Without Pillar", "With Pillar"));
		MONSTER_PHASES_BY_ID.put(14147, Arrays.asList("In Melee Range", "Out of Melee Range"));
		MONSTER_PHASES_BY_ID.put(14176, Arrays.asList("Tank using magic", "Tank not using magic"));
		MONSTER_PHASES_BY_ID.put(14707, Arrays.asList("Normal", "Shielded", "Burrowing"));
		MONSTER_PHASES_BY_ID.put(15742, Arrays.asList("Standard", "Melee Punish"));
		MONSTER_PHASES_BY_ID.put(16305, Arrays.asList("Standard", "Sword Cleave", "Perfect Lightning"));
		MONSTER_PHASES_BY_ID.put(16309, Arrays.asList("Standard", "Sword Cleave", "Perfect Lightning"));
		MONSTER_PHASES_BY_ID.put(16314, Arrays.asList("Standard", "Sword Cleave", "Perfect Lightning"));
		MONSTER_PHASES_BY_ID.put(16315, Arrays.asList("Standard", "Sword Cleave", "Perfect Lightning"));
	}
	public static final int[] NEX_IDS = { 11278, 11279, 11280, 11281, 11282 };
	public static final int[] NIGHTMARE_IDS = { 378, 9425, 9426, 9427, 9428, 9429, 9430, 9431, 9432, 9433, 9460, 377, 9423, 9416, 9417, 9418, 9419, 9420, 9421, 9422, 9424, 11153, 11154, 11155 };
	public static final int[] NIGHTMARE_TOTEM_IDS = { 9434, 9437, 9440, 9443, 9435, 9438, 9441, 9444 };
	public static final Map<Integer, Double> NPC_HARDCODED_MAX_HIT = new HashMap<>();
	static {
		NPC_HARDCODED_MAX_HIT.put(5947, 10.0);
		NPC_HARDCODED_MAX_HIT.put(5961, 10.0);
	}
	public static final double NUMBER_OF_LOADOUTS = 6;
	public static final int[] OLM_HEAD_IDS = { 7551, 7554 };
	public static final int[] OLM_IDS = { 7551, 7554, 7552, 7555, 7550, 7553 };
	public static final int[] OLM_MAGE_HAND_IDS = { 7550, 7553 };
	public static final int[] OLM_MELEE_HAND_IDS = { 7552, 7555 };
	public static final int[] ONE_HIT_MONSTERS = { 7223, 8584, 11193 };
	public static final int[] P2_WARDEN_IDS = { 11753, 11754, 11756, 11757 };
	public static final int[] P3_WARDEN_IDS = { 11761, 11763, 11762, 11764 };
	public static final int[] PARTY_SIZE_REQUIRED_MONSTER_IDS = { 11789, 11790, 11791, 11792, 11793, 11794, 11795, 11796, 11797, 11798, 11799, 11778, 11779, 11780, 11719, 11721, 11724, 11725, 11726, 11730, 11732, 11733, 11751, 11750, 11752, 11753, 11754, 11756, 11757, 11755, 11758, 11761, 11763, 11762, 11764, 10830, 10831, 10832, 8369, 8370, 8371, 10847, 10848, 10849, 8360, 8361, 8362, 8363, 8364, 8365, 8366, 8367, 8359, 8342, 8343, 8344, 8345, 8346, 8347, 8348, 8349, 8350, 8351, 8352, 8353, 8355, 8356, 8357, 8387, 8388, 10867, 10868, 8339, 8340, 8372, 8373, 8374, 8376, 8381, 8382, 8383, 8384, 8385, 10822, 10823, 10824, 10825, 10826, 10827, 10828, 10829, 10813, 10791, 10792, 10793, 10794, 10795, 10796, 10797, 10798, 10799, 10800, 10801, 10802, 10804, 10805, 10806, 10808, 10809, 10810, 10770, 10771, 10772, 10850, 10851, 10852, 10854, 10858, 10859, 10860, 10861, 10862, 10814, 10815, 10816, 10817, 10818, 10819, 10820, 10821, 10812, 10774, 10775, 10776, 10777, 10778, 10779, 10780, 10781, 10782, 10783, 10784, 10785, 10787, 10788, 10789, 10864, 10865, 10767, 10768, 10833, 10834, 10835, 10837, 10841, 10842, 10843, 10844, 10845 };
	public static final List<String> ROYAL_TITANS_PHASES = Arrays.asList("In Melee Range", "Out of Melee Range");
	public static final int[] SCAVENGER_BEAST_IDS = { 7548, 7549 };
	public static final double SECONDS_PER_TICK = 0.6;
	public static final int[] SOTETSEG_IDS = { 8387, 8388, 10867, 10868 };
	public static final int[] TD_IDS = { 13599, 13600, 13601, 13602, 13603, 13604, 13605, 13606 };
	public static final List<String> TD_PHASES = Arrays.asList("Shielded", "Shielded (Defenceless)", "Unshielded");
	public static final int[] TEKTON_IDS = { 7540, 7543, 7544, 7545 };
	public static final int[] TITAN_BOSS_IDS = { 12596, 14147 };
	public static final int[] TITAN_ELEMENTAL_IDS = { 14150, 14151 };
	public static final int[] TOA_OBELISK_IDS = { 11751, 11750, 11752 };
	public static final int[] TOA_WARDEN_CORE_EJECTED_IDS = { 11755, 11758 };
	public static final int[] TOB_EM_MONSTER_IDS = { 10814, 10815, 10816, 10817, 10818, 10819, 10820, 10821, 10812, 10774, 10775, 10776, 10777, 10778, 10779, 10780, 10781, 10782, 10783, 10784, 10785, 10787, 10788, 10789, 10864, 10865, 10767, 10768, 10833, 10834, 10835, 10837, 10841, 10842, 10843, 10844, 10845 };
	public static final int[] TOB_MONSTER_IDS = { 10830, 10831, 10832, 8369, 8370, 8371, 10847, 10848, 10849, 8360, 8361, 8362, 8363, 8364, 8365, 8366, 8367, 8359, 8342, 8343, 8344, 8345, 8346, 8347, 8348, 8349, 8350, 8351, 8352, 8353, 8355, 8356, 8357, 8387, 8388, 10867, 10868, 8339, 8340, 8372, 8373, 8374, 8376, 8381, 8382, 8383, 8384, 8385, 10822, 10823, 10824, 10825, 10826, 10827, 10828, 10829, 10813, 10791, 10792, 10793, 10794, 10795, 10796, 10797, 10798, 10799, 10800, 10801, 10802, 10804, 10805, 10806, 10808, 10809, 10810, 10770, 10771, 10772, 10850, 10851, 10852, 10854, 10858, 10859, 10860, 10861, 10862 };
	public static final int[] TOMBS_OF_AMASCUT_MONSTER_IDS = { 11789, 11790, 11791, 11792, 11793, 11794, 11795, 11796, 11797, 11798, 11799, 11778, 11779, 11780, 11719, 11721, 11724, 11725, 11726, 11730, 11732, 11733, 11751, 11750, 11752, 11753, 11754, 11756, 11757, 11755, 11758, 11761, 11763, 11762, 11764 };
	public static final int[] TOMBS_OF_AMASCUT_PATH_MONSTER_IDS = { 11789, 11790, 11791, 11792, 11793, 11794, 11795, 11796, 11797, 11798, 11799, 11778, 11779, 11780, 11719, 11721, 11724, 11725, 11726, 11730, 11732, 11733 };
	public static final double TTK_DIST_EPSILON = 0.0001;
	public static final double TTK_DIST_MAX_ITER_ROUNDS = 1000;
	public static final int[] UNDERWATER_MONSTERS = { 7796 };
	public static final int[] USES_DEFENCE_LEVEL_FOR_MAGIC_DEFENCE_NPC_IDS = { 7584, 7585, 10830, 10831, 10832, 8369, 8370, 8371, 10847, 10848, 10849, 10833, 10834, 10835, 8372, 8373, 8374, 10850, 10851, 10852, 8917, 8918, 8919, 8920, 11709, 11712, 9118 };
	public static final int[] VARDORVIS_IDS = { 12223, 12224, 12228, 12425, 12426, 13656 };
	public static final int[] VERZIK_IDS = { 10830, 10831, 10832, 8369, 8370, 8371, 10847, 10848, 10849, 10833, 10834, 10835, 8372, 8373, 8374, 10850, 10851, 10852 };
	public static final int[] VERZIK_P1_IDS = { 10830, 10831, 10832, 8369, 8370, 8371, 10847, 10848, 10849 };
	public static final int[] VESPINE_SOLDIER_IDS = { 7538, 7539 };
	public static final int[] VESPULA_IDS = { 7530, 7531, 7532 };
	public static final int[] YAMA_IDS = { 14176 };
	public static final List<String> YAMA_PHASES = Arrays.asList("Tank using magic", "Tank not using magic");
	public static final int[] YAMA_VOID_FLARE_IDS = { 14179 };
	public static final int[] ZEBAK_IDS = { 11730, 11732, 11733 };
	public static final int[] ZULRAH_IDS = { 2042, 2043, 2044 };
}
