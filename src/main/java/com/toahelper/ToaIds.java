package com.toahelper;

import static com.toahelper.AttackStyle.MAGIC;
import static com.toahelper.AttackStyle.MELEE;
import static com.toahelper.AttackStyle.RANGED;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tombs of Amascut IDs. Names in comments are from net.runelite.api.gameval (OSRS cache), so
 * they hold for any server running an OSRS cache.
 */
final class ToaIds
{
	private ToaIds()
	{
	}

	// ---- Projectiles: what to pray when one is heading at you ----

	static final Map<Integer, AttackStyle> PROJECTILE_STYLES;

	static
	{
		Map<Integer, AttackStyle> m = new HashMap<>();
		// Not Akkha's projectiles (AKKHA_MAGIC_TRAVEL 2253, AKKHA_RANGED_TRAVEL 2255): the wiki says his
		// damage is decided when the projectile spawns, so a countdown to impact is too late. His
		// form decides the prayer instead (see the standing threats in ToaHelperPlugin).
		// Zebak: a big rock that splits into smaller ones aimed at each player
		m.put(2175, MELEE); // ZEBAK_MELEE_PROJANIM_INITIAL
		m.put(2180, MELEE); // ZEBAK_MELEE_PROJANIM_SPLIT
		m.put(2176, MAGIC); // ZEBAK_MAGE_PROJANIM_INITIAL
		m.put(2177, MAGIC); // ZEBAK_MAGE_PROJANIM_INITIAL_ENRAGED
		m.put(2181, MAGIC); // ZEBAK_MAGE_PROJANIM_SPLIT
		m.put(2178, RANGED); // ZEBAK_RANGE_PROJANIM_INITIAL
		m.put(2179, RANGED); // ZEBAK_RANGE_PROJANIM_INITIAL_ENRAGED
		m.put(2182, RANGED); // ZEBAK_RANGE_PROJANIM_SPLIT
		m.put(2185, RANGED); // ZEBAK_RANGED_SPLIT
		m.put(2186, MAGIC); // ZEBAK_MAGE_SPLIT
		m.put(2187, RANGED); // ZEBAK_RANGED_FRAGMENT01
		m.put(2188, RANGED); // ZEBAK_RANGED_FRAGMENT02
		m.put(2189, RANGED); // ZEBAK_RANGED_FRAGMENT03
		m.put(2190, RANGED); // ZEBAK_RANGED_FRAGMENT04
		// Wardens (phase 2 and 3 prayer orbs)
		m.put(2204, MELEE); // TOA_WARDENS_PRAYER_MELEE_TRAVEL
		m.put(2206, RANGED); // TOA_WARDENS_PRAYER_RANGED_TRAVEL
		m.put(2208, MAGIC); // TOA_WARDENS_PRAYER_MAGIC_TRAVEL
		m.put(3027, MAGIC); // TOA_WARDENS_PRAYER_MAGIC_TRAVEL_SMALL
		m.put(2241, RANGED); // PROJECTILE_WARDEN_RANGED01: phase 2 Warden's ranged skull
		// Ba-Ba and the Apmeken baboons
		m.put(2244, RANGED); // TOA_BABA_RANGED_TRAVEL
		m.put(2242, RANGED); // TOA_BABOON_RANGED_TRAVEL_1
		m.put(2243, RANGED); // TOA_BABOON_RANGED_TRAVEL_2
		m.put(2247, MAGIC); // TOA_BABOON_MAGIC_TRAVEL_1
		m.put(2248, MAGIC); // TOA_BABOON_MAGIC_TRAVEL_2
		// Kephri's spitting / agile scarabs
		m.put(2152, RANGED); // TOA_SCARAB_RANGED_PROJECTILE01
		m.put(2154, RANGED); // TOA_SCARAB_RANGED_PROJECTILE02
		PROJECTILE_STYLES = Collections.unmodifiableMap(m);
	}

	// ---- Bosses and NPCs ----

	// Akkha changes form, and always prays against the style he attacks with.
	static final int AKKHA_MELEE = 11790;
	static final int AKKHA_RANGE = 11791;
	static final int AKKHA_MAGE = 11792;
	// Final stand: prays Magic and Missiles at once, so only melee hurts him.
	static final Set<Integer> AKKHA_ENRAGE = set(11793, 11794, 11795, 11796);
	// NPC_AKKHA_ATTACK_{MELEE,RANGED,MAGIC}_{SPEAR,SWORD}: his regular attacks, counted for specials
	static final Set<Integer> AKKHA_ATTACK_ANIMATIONS = set(9770, 9771, 9772, 9773, 9774, 9775);

	static final int BABA = 11778; // TOA_BABA
	static final int BABA_BOULDER_WEAK = 11783; // TOA_BABA_BOULDER_WEAK: the cracked one, guaranteed max hit
	// The wiki: standing next to debris, an unopened sarcophagus or a baboon blocks her thrown boulder.
	static final Set<Integer> BABA_COVER = set(
		11784, 11785, 11786, 11787, // TOA_BABA_RUBBLE(_2/_3/_4)
		11788, // TOA_BABA_SARC_NPC
		11781 // TOA_BABA_BABOON
	);
	static final Set<Integer> BABA_ROCKFALL_GRAPHICS = set(2250, 2251, 2252); // TOA_BABA_ROCK_FALL(_FASTER/_FASTEST)

	static final Set<Integer> ZEBAK = set(11730, 11732); // TOA_ZEBAK, TOA_ZEBAK_ENRAGED
	static final int ZEBAK_SAFESPOT = 11737; // TOA_ZEBAK_SAFESPOT: the rocks you hide behind for the roar
	static final Set<Integer> ZEBAK_ROAR_ANIMATIONS = set(9628, 9629); // NPC_ZEBAK0x_ATTACK_ROAR
	static final Set<Integer> ZEBAK_MELEE_ANIMATIONS = set(9620, 9621, 9622, 9623); // NPC_ZEBAK0x_ATTACK_MELEE(_ENRAGED)

	static final int KEPHRI_GUARDIAN_MELEE = 11724; // soldier scarab
	static final int KEPHRI_GUARDIAN_RANGED = 11725; // spitting scarab
	static final int KEPHRI_GUARDIAN_MAGE = 11726; // arcane scarab
	static final int KEPHRI_MAGE_CHARGE = 2149; // TOA_KEPHRI_MAGE_CHARGE: arcane scarab charging its big magic attack
	static final Set<Integer> KEPHRI = set(11719, 11720, 11721); // TOA_KEPHRI_BOSS_{SHIELDED,WEAK,ENRAGE}
	static final int KEPHRI_SWARM = 11723; // TOA_KEPHRI_SHIELD_SCARAB
	static final Set<Integer> KEPHRI_EGGS = set(11728, 11729); // KEPHRI_EGG_{EXPLODE,HATCH}
	static final int SCABARAS_PUZZLE_SCARAB = 11697; // TOA_SCABARAS_SCARAB: ranged pests in the puzzle rooms
	static final int KEPHRI_DUNG_MARK = 2146; // KEPHRI_INDICATE_PLAYER_FOR_DUNG: scarabs circling the dung target

	static final int HET_SEAL_VULNERABLE = 11707; // TOA_HET_GOAL_VULNERABLE

	static final Set<Integer> WARDENS_P2 = set(11753, 11754, 11756, 11757); // TOA_WARDEN_{ELIDINIS,TUMEKEN}_PHASE2_{MAGE,RANGE}
	static final Set<Integer> TUMEKEN_P2 = set(11756, 11757);
	static final int WARDENS_SIPHON = 11769; // TOA_WARDENS_ENERGY: Energy Siphon, melee only
	static final Set<Integer> WARDENS_P2_EXPOSED = set(11755, 11758);
	static final Set<Integer> WARDENS_P3 = set(11761, 11762, 11763, 11764); // TOA_WARDEN_*_PHASE3(_CHARGING)

	// Phase 3 floor slams (NPC_WARDENS_ATTACK{LEFT,RIGHT,CENTER}0x)
	static final Set<Integer> WARDEN_SLAM_LEFT = set(9674, 9675);
	static final Set<Integer> WARDEN_SLAM_RIGHT = set(9676, 9677);
	static final Set<Integer> WARDEN_SLAM_BOTH = set(9678, 9679);

	// Apmeken baboons (TOA_PATH_APMEKEN_BABOON_*)
	static final Set<Integer> BABOON_MELEE = set(11709, 11712); // brawlers
	static final Set<Integer> BABOON_RANGED = set(11710, 11713); // throwers
	static final Set<Integer> BABOON_MAGIC = set(11711, 11714); // mages
	static final int BABOON_SHAMAN = 11715;
	static final int BABOON_VOLATILE = 11716; // _ZOMBIE
	static final int BABOON_CURSED = 11717;
	static final int BABOON_THRALL = 11718;

	static final Set<Integer> ZEBAK_JUGS = set(11735, 11736); // TOA_ZEBAK_JUG(_ROLLING)

	static final Set<Integer> OBELISKS = set(11750, 11751, 11752); // TOA_WARDENS_P{1,2}_OBELISK_NPC
	static final Set<Integer> WARDEN_CORES = set(11770, 11771); // TOA_WARDEN_{TUMEKEN,ELIDINIS}_CORE

	// ---- Points: room points per damage, from the wiki's Chest (Tombs of Amascut) table ----

	static final Map<Integer, Double> POINTS_FACTORS;

	static
	{
		Map<Integer, Double> m = new HashMap<>();
		put(m, 0.0, 11770, 11771); // Warden cores
		put(m, 0.0, 11782, 11783); // Ba-Ba's boulders
		put(m, 1.2, 11709, 11710, 11711, 11712, 11713, 11714, 11715, 11716, 11717, 11718); // Apmeken baboons
		put(m, 2.0, 11778, 11779, 11780); // Ba-Ba
		put(m, 1.5, 10680, 11730, 11732, 11733); // Zebak
		put(m, 0.5, 11724, 11725, 11726); // Kephri's guardian scarabs
		put(m, 2.5, HET_SEAL_VULNERABLE);
		put(m, 1.5, 11750, 11751, 11752); // Obelisks
		put(m, 0.0, 11746, 11747, 11748, 11749, 11759, 11760); // Wardens while not fightable
		put(m, 2.0, 11753, 11754, 11756, 11757); // Wardens phase 2
		put(m, 0.0, 11755, 11758); // Wardens phase 2, downed
		put(m, 2.5, 11761, 11762, 11763, 11764); // Wardens phase 3
		POINTS_FACTORS = Collections.unmodifiableMap(m);
	}

	// ---- Rooms (map region of the instance template) ----

	enum Room
	{
		NEXUS("Nexus", 20_000, 14160),
		CRONDIS("Crondis", 20_000, 15698),
		ZEBAK("Zebak", 10_000, 15700),
		SCABARAS("Scabaras", 20_000, 14162),
		KEPHRI("Kephri", 20_000, 14164),
		APMEKEN("Apmeken", 20_000, 15186),
		BABA("Ba-Ba", 20_000, 15188),
		HET("Het", 20_000, 14674),
		AKKHA("Akkha", 20_000, 14676),
		WARDENS("Wardens", 60_000, 15184, 15696),
		TOMB("Tomb", 20_000, 14672);

		final String displayName;
		final int pointsCap;
		private final int[] regions;

		Room(String displayName, int pointsCap, int... regions)
		{
			this.displayName = displayName;
			this.pointsCap = pointsCap;
			this.regions = regions;
		}

		static Room forRegion(int region)
		{
			for (Room room : values())
			{
				for (int r : room.regions)
				{
					if (r == region)
					{
						return room;
					}
				}
			}
			return null;
		}
	}

	/**
	 * TOA_CLIENT_RAID_LEVEL
	 */
	static final int RAID_LEVEL_VARBIT = 14380;

	private static void put(Map<Integer, Double> m, double factor, int... ids)
	{
		for (int id : ids)
		{
			m.put(id, factor);
		}
	}

	private static Set<Integer> set(Integer... ids)
	{
		Set<Integer> s = new HashSet<>();
		Collections.addAll(s, ids);
		return Collections.unmodifiableSet(s);
	}
}
