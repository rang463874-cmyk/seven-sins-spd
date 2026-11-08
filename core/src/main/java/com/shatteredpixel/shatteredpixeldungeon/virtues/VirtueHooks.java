package com.shatteredpixel.shatteredpixeldungeon.virtues;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.virtues.VirtueChampion;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.VirtueSanctumRoom;
import com.shatteredpixel.shatteredpixeldungeon.sins.SinRewards;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * The virtues' crusade: one champion of a virtue waits for the hero in its own sanctum
 * ({@link VirtueSanctumRoom}) in each region from the prison on (regions 1-4, chance 1/floors left,
 * forced on the region's last regular floor). Every virtue appears
 * at most once per run; for a sin hero, the virtue opposing its sin (its nemesis) is guaranteed in one
 * random region. A nemesis fight deals and takes +25% damage, and pays a bigger reward.
 * Called from RegularLevel.build (addSanctum) and EliteHooks.onLevelMobs; state saved with the quests in Dungeon.
 */
public class VirtueHooks {

	public static final float NEMESIS_DAMAGE = 1.25f;

	/** bit r = region r already had its champion */
	private static int regionsDone = 0;
	/** bit = Virtue.ordinal() already used */
	private static int virtuesUsed = 0;
	/** region (1-4) where the nemesis comes, 0 = not rolled yet */
	private static int nemesisRegion = 0;

	/** the archangel after Yog: 0 not yet, 1 waiting by the amulet, 2 defeated */
	public static int angelStage = 0;

	public static void reset() {
		regionsDone = 0;
		virtuesUsed = 0;
		nemesisRegion = 0;
		angelStage = 0;
	}

	private static final String NODE = "sevenSinsVirtues";

	public static void storeInBundle(Bundle bundle) {
		Bundle node = new Bundle();
		node.put("regions", regionsDone);
		node.put("used", virtuesUsed);
		node.put("nemesis", nemesisRegion);
		node.put("angel", angelStage);
		bundle.put(NODE, node);
	}

	public static void restoreFromBundle(Bundle bundle) {
		reset();
		Bundle node = bundle.getBundle(NODE);
		if (node.isNull()) return;
		regionsDone = node.getInt("regions");
		virtuesUsed = node.getInt("used");
		nemesisRegion = node.getInt("nemesis");
		angelStage = node.getInt("angel");
	}

	/** the virtue opposing the hero's sin, or null for the original classes */
	public static Virtue nemesisOf(Hero hero) {
		int sin = SinRewards.sinOf(hero);
		return sin == SinRewards.NONE ? null : Virtue.against(sin);
	}

	public static boolean isNemesis(Virtue v) {
		return v != null && Dungeon.hero != null && nemesisOf(Dungeon.hero) == v;
	}

	// ------------------------------------------------------------------ spawning

	private static Virtue pick(int region) {
		Virtue nem = Dungeon.hero == null ? null : nemesisOf(Dungeon.hero);
		if (nem != null && (virtuesUsed & (1 << nem.ordinal())) == 0) {
			if (nemesisRegion == 0) nemesisRegion = 1 + Random.Int(4);
			if (region >= nemesisRegion) return nem;
		}
		ArrayList<Virtue> pool = new ArrayList<>();
		for (Virtue v : Virtue.values()) {
			if ((virtuesUsed & (1 << v.ordinal())) != 0) continue;
			//keep the nemesis for its own region
			if (v == nem) continue;
			pool.add(v);
		}
		return pool.isEmpty() ? null : Random.element(pool);
	}

	/** the virtue rolled for the floor being built (its sanctum was added to the rooms), or null */
	private static Virtue pendingVirtue = null;

	/**
	 * RegularLevel.build, right after initRooms: maybe a champion of a virtue waits on this floor.
	 * Rolls the odds (1/floors left in the region, forced on its last regular floor) and adds the
	 * champion's sanctum to the rooms; {@link #onLevelMobs} puts the champion in it.
	 */
	public static void addSanctum(ArrayList<Room> rooms) {
		pendingVirtue = null;
		if (Dungeon.branch != 0 || Dungeon.bossLevel() || Dungeon.depth < 6 || Dungeon.depth > 24) return;
		int region = (Dungeon.depth - 1) / 5;
		if (region < 1 || (regionsDone & (1 << region)) != 0) return;
		int lastFloor = region * 5 + 4;
		int floorsLeft = lastFloor - Dungeon.depth + 1;
		if (floorsLeft < 1 || Random.Int(floorsLeft) != 0) return;

		Virtue v = pick(region);
		if (v == null) return;
		rooms.add(new VirtueSanctumRoom());
		pendingVirtue = v;
	}

	/** EliteHooks.onLevelMobs: the rolled champion takes its place on the pedestal of its sanctum */
	public static void onLevelMobs(Level level, ArrayList<Room> rooms, Room entrance) {
		Virtue v = pendingVirtue;
		pendingVirtue = null;
		if (v == null) return;
		VirtueSanctumRoom sanctum = null;
		for (Room r : rooms) {
			if (r instanceof VirtueSanctumRoom) sanctum = (VirtueSanctumRoom) r;
		}
		if (sanctum == null) return;
		VirtueChampion c = Reflection.newInstance(v.champion);
		if (c == null) return;
		c.pos = level.pointToCell(sanctum.altar());
		c.home = c.pos;
		level.mobs.add(c);
		int region = (Dungeon.depth - 1) / 5;
		regionsDone |= 1 << region;
		virtuesUsed |= 1 << v.ordinal();
	}

	// ------------------------------------------------------------------ the archangel (final boss)

	/** which archangel comes: the one opposing the hero's sin, else the one opposing its demon pact, else Michael */
	public static Virtue angelFor(Hero hero) {
		Virtue v = hero == null ? null : nemesisOf(hero);
		if (v == null && com.shatteredpixel.shatteredpixeldungeon.demons.DemonHooks.lord != null) {
			v = Virtue.against(com.shatteredpixel.shatteredpixeldungeon.demons.DemonHooks.lord.sin);
		}
		return v == null ? Virtue.HUMILITY : v;
	}

	/** LastLevel.createMobs: the archangel stands guard before the amulet */
	public static void onAmuletLevel(Level level, int amuletPos) {
		if (angelStage != 0) return;
		com.shatteredpixel.shatteredpixeldungeon.actors.mobs.virtues.ArchangelBoss a =
				new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.virtues.ArchangelBoss();
		a.virtue = angelFor(Dungeon.hero);
		a.pos = amuletPos + 2 * level.width();
		level.mobs.add(a);
		angelStage = 1;
	}

	/** Amulet.doPickUp: no one takes the amulet while the archangel stands */
	public static boolean amuletGuarded() {
		if (Dungeon.level == null) return false;
		for (Mob m : Dungeon.level.mobs) {
			if (m instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.virtues.ArchangelBoss && m.isAlive()) return true;
		}
		return false;
	}

	// ------------------------------------------------------------------ nemesis damage

	/** Char.damage: the hero and its nemesis hurt each other 25% more */
	public static int modifyDamage(Char target, Object src, int dmg) {
		if (dmg <= 0) return dmg;
		if (target instanceof VirtueFoe && ((VirtueFoe) target).nemesis()) {
			if (src == Dungeon.hero || !(src instanceof Mob)) return Math.round(dmg * NEMESIS_DAMAGE);
		}
		if (target instanceof Hero && src instanceof VirtueFoe && ((VirtueFoe) src).nemesis()) {
			return Math.round(dmg * NEMESIS_DAMAGE);
		}
		return dmg;
	}
}
