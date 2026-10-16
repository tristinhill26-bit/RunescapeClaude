package com.shadowtiles;

import static com.shadowtiles.HazardType.GRAPHIC;
import static com.shadowtiles.HazardType.NPC;
import static com.shadowtiles.HazardType.OBJECT;
import static com.shadowtiles.HazardType.PROJECTILE;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Hazards that ship with the plugin, grouped so each group can be toggled in the config.
 * <p>
 * IDs are OSRS cache IDs (names in comments match net.runelite.api.gameval). If your server
 * runs a custom cache, or a radius is off for your server's mechanics, override the entry from
 * the "Custom attacks" box: a custom line with the same type and id replaces the built-in one.
 */
@Getter
@RequiredArgsConstructor
enum BuiltInHazards
{
	/**
	 * The generic "something is about to land here" shadow used by Grotesque Guardians,
	 * Great Olm, the DT2 bosses and many others. The number in the name is the duration.
	 */
	FALLING_SHADOWS(ShadowTilesConfig::fallingShadows, Arrays.asList(
		HazardDefinition.of(GRAPHIC, 2111, 0, null), // GARGBOSS_DEBRIS_SHADOW_60
		HazardDefinition.of(GRAPHIC, 1446, 0, null), // GARGBOSS_DEBRIS_SHADOW_90
		HazardDefinition.of(GRAPHIC, 1447, 0, null), // GARGBOSS_DEBRIS_SHADOW_120
		HazardDefinition.of(GRAPHIC, 1448, 0, null), // GARGBOSS_DEBRIS_SHADOW_150
		HazardDefinition.of(GRAPHIC, 1449, 0, null), // GARGBOSS_DEBRIS_SHADOW_180
		HazardDefinition.of(GRAPHIC, 1889, 0, null), // GARGBOSS_DEBRIS_SHADOW_210
		HazardDefinition.of(GRAPHIC, 1890, 0, null), // GARGBOSS_DEBRIS_SHADOW_240
		HazardDefinition.of(GRAPHIC, 1938, 0, null), // GARGBOSS_DEBRIS_SHADOW_270
		HazardDefinition.of(GRAPHIC, 1939, 0, null), // GARGBOSS_DEBRIS_SHADOW_300
		HazardDefinition.of(GRAPHIC, 2299, 0, null), // GARGBOSS_DEBRIS_SHADOW_60_SMALL
		HazardDefinition.of(GRAPHIC, 2380, 0, null), // GARGBOSS_DEBRIS_SHADOW_90_SMALL
		HazardDefinition.of(GRAPHIC, 2776, 0, null), // GARGBOSS_RAIN_SHADOW_90
		HazardDefinition.of(GRAPHIC, 3291, 1, null), // GARGBOSS_DEBRIS_SHADOW_180_LARGE
		HazardDefinition.of(GRAPHIC, 3292, 1, null), // GARGBOSS_DEBRIS_SHADOW_270_LARGE
		HazardDefinition.of(GRAPHIC, 3293, 1, null), // GARGBOSS_DEBRIS_SHADOW_300_LARGE
		HazardDefinition.of(GRAPHIC, 3996, 0, null), // GARGBOSS_DEBRIS_SHADOW_30
		HazardDefinition.of(GRAPHIC, 3997, 0, null), // GARGBOSS_DEBRIS_SHADOW_30_DARK
		HazardDefinition.of(GRAPHIC, 3998, 0, null), // GARGBOSS_DEBRIS_SHADOW_60_DARK
		HazardDefinition.of(GRAPHIC, 3999, 0, null), // GARGBOSS_DEBRIS_SHADOW_90_DARK
		HazardDefinition.of(GRAPHIC, 4000, 0, null), // GARGBOSS_DEBRIS_SHADOW_120_DARK
		HazardDefinition.of(GRAPHIC, 4001, 0, null), // GARGBOSS_DEBRIS_SHADOW_150_DARK
		HazardDefinition.of(GRAPHIC, 4002, 0, null), // GARGBOSS_DEBRIS_SHADOW_180_DARK
		HazardDefinition.of(GRAPHIC, 4003, 1, null), // GARGBOSS_DEBRIS_SHADOW_180_LARGE_DARK
		HazardDefinition.of(GRAPHIC, 4004, 0, null), // GARGBOSS_DEBRIS_SHADOW_210_DARK
		HazardDefinition.of(GRAPHIC, 4005, 0, null), // GARGBOSS_DEBRIS_SHADOW_240_DARK
		HazardDefinition.of(GRAPHIC, 4006, 0, null), // GARGBOSS_DEBRIS_SHADOW_270_DARK
		HazardDefinition.of(GRAPHIC, 4007, 0, null)  // GARGBOSS_DEBRIS_SHADOW_300_DARK
	)),

	BA_BA(ShadowTilesConfig::baba, Arrays.asList(
		HazardDefinition.of(GRAPHIC, 2250, 1, "Rock"), // TOA_BABA_ROCK_FALL
		HazardDefinition.of(GRAPHIC, 2251, 1, "Rock"), // TOA_BABA_ROCK_FALL_FASTER
		HazardDefinition.of(GRAPHIC, 2252, 1, "Rock"), // TOA_BABA_ROCK_FALL_FASTEST
		HazardDefinition.of(NPC, 11782, 0, "Boulder"), // TOA_BABA_BOULDER
		HazardDefinition.of(NPC, 11783, 0, "Boulder") // TOA_BABA_BOULDER_WEAK
	)),

	OLM(ShadowTilesConfig::olm, Arrays.asList(
		HazardDefinition.of(GRAPHIC, 1357, 1, "Crystal"), // OLM_CRYSTALROCK_FALLING
		HazardDefinition.of(OBJECT, 29766, 1, "Bomb"), // OLM_CRYSTAL_BOMB
		HazardDefinition.of(OBJECT, 30032, 0, null) // OLM_ACID_POOL
	)),

	VORKATH(ShadowTilesConfig::vorkath, Arrays.asList(
		HazardDefinition.of(PROJECTILE, 1481, 1, "Firebomb"), // VORKATH_AREA_TRAVEL
		HazardDefinition.of(PROJECTILE, 1482, 0, null), // VORKATH_AREA_SMALL_TRAVEL (acid phase rapid fire)
		HazardDefinition.of(PROJECTILE, 2266, 0, null), // VORKATH_AREA_TRAVEL_SMALL
		HazardDefinition.of(PROJECTILE, 1483, 0, null) // VORKATH_ACID_TRAVEL
	)),

	HYDRA(ShadowTilesConfig::hydra, Arrays.asList(
		HazardDefinition.of(PROJECTILE, 1644, 1, "Poison") // HYDRABOSS_POOLS_PROJ
	)),

	ZULRAH(ShadowTilesConfig::zulrah, Arrays.asList(
		// The cloud is a 3x3 object; its location is the centre tile.
		HazardDefinition.of(OBJECT, 11700, 1, null) // SNAKEBOSS_POISONCLOUD
	)),

	CERBERUS(ShadowTilesConfig::cerberus, Arrays.asList(
		HazardDefinition.of(GRAPHIC, 1246, 1, "Lava") // FIRE_LIQUID
	)),

	VERZIK(ShadowTilesConfig::verzik, Arrays.asList(
		HazardDefinition.of(PROJECTILE, 1598, 1, "Bomb") // VERZIK_ACIDBOMB_PROJANIM
	)),

	/**
	 * Only the smite. Its falling debris uses the *_DARK shadows in FALLING_SHADOWS. The sweep
	 * and Exploding Light are handled by the Mad Angel Helper plugin instead: for both, the
	 * thing to show is a tile to move onto, not a tile to avoid.
	 */
	MAD_ANGEL(ShadowTilesConfig::madAngel, Arrays.asList(
		HazardDefinition.of(GRAPHIC, 4011, 1, "Smite"), // VFX_MAD_ANGEL_ATTACK_SMITE
		HazardDefinition.of(GRAPHIC, 4012, 1, "Smite") // VFX_MAD_ANGEL_ATTACK_SMITE_ENRAGED
	)),

	/**
	 * Tombs of Amascut hazards to step away from. Where to stand instead is handled by the
	 * ToA Helper plugin.
	 */
	TOMBS_OF_AMASCUT(ShadowTilesConfig::toa, Arrays.asList(
		HazardDefinition.of(GRAPHIC, 2198, 0, "Lightning"), // WARDENS_LIGHTNING_WARN
		HazardDefinition.of(GRAPHIC, 2195, 0, "Rock"), // ZEBAK_ROCK_FALL
		HazardDefinition.of(PROJECTILE, 2210, 0, "Entomb"), // TOA_WARDENS_ENTOMB_TRAVEL
		HazardDefinition.of(NPC, 11804, 0, "Orb"), // AKKHA_ENRAGE_ORB
		HazardDefinition.of(NPC, 11742, 0, "Blood"), // TOA_ZEBAK_BLOOD_CLOUD
		HazardDefinition.of(NPC, 11743, 0, "Blood"), // TOA_ZEBAK_BLOOD_CLOUD_SMALL
		HazardDefinition.of(NPC, 11738, 0, null), // TOA_ZEBAK_WAVE
		HazardDefinition.of(NPC, 11739, 0, null) // TOA_ZEBAK_WAVE_BLOODY
	)),

	MUSPAH(ShadowTilesConfig::muspah, Arrays.asList(
		HazardDefinition.of(GRAPHIC, 2335, 0, "Spike"), // VFX_MUSPAH_SPIKE_WARNING_01
		HazardDefinition.of(GRAPHIC, 2336, 0, "Spike") // VFX_MUSPAH_SPIKE_WARNING_02
	));

	private final Predicate<ShadowTilesConfig> enabled;
	private final List<HazardDefinition> definitions;
}
