package com.shatteredpixel.shatteredpixeldungeon.elites;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.elites.ChainWarden;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.elites.FallenPriest;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.elites.GnollForeman;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.elites.Hellhound;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.elites.RatChieftain;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.elites.RegionElite;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.SinEssence;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sins.SinRewards;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Elite monsters: sin elites (a possessed ordinary monster, 25% of floors from floor 2) and one named
 * region elite per region (forced by the region's last regular floor). Called from
 * RegularLevel.createMobs, Mob.die and Mob.name; the region record is saved with the quests.
 */
public class EliteHooks {

	public static final float SIN_ELITE_CHANCE = 0.25f;
	public static final float SIN_POINTS_FOR_OWN_SIN = 15f;

	/** bit r set = region r's named elite has already appeared */
	private static int regionsDone = 0;

	public static void reset() {
		regionsDone = 0;
	}

	private static final String NODE = "sevenSinsElites";

	public static void storeInBundle(Bundle bundle) {
		Bundle node = new Bundle();
		node.put("regions", regionsDone);
		bundle.put(NODE, node);
	}

	public static void restoreFromBundle(Bundle bundle) {
		Bundle node = bundle.getBundle(NODE);
		regionsDone = node.isNull() ? 0 : node.getInt("regions");
	}

	// ---- spawning ----

	private static boolean eligible(Mob m) {
		return m.alignment == Char.Alignment.ENEMY && !(m instanceof NPC) && !(m instanceof Mimic)
				&& !(m instanceof Statue) && !(m instanceof RegionElite)
				&& !m.properties().contains(Char.Property.BOSS)
				&& !m.properties().contains(Char.Property.MINIBOSS)
				&& !m.properties().contains(Char.Property.IMMOVABLE)
				&& m.buffs(ChampionEnemy.class).isEmpty();
	}

	/** end of RegularLevel.createMobs */
	public static void onLevelMobs(Level level, ArrayList<Room> rooms, Room entrance) {
		if (Dungeon.branch != 0 || Dungeon.bossLevel() || Dungeon.depth < 2 || Dungeon.depth > 24) return;

		//the virtues' champions hunt the hero from the prison on
		com.shatteredpixel.shatteredpixeldungeon.virtues.VirtueHooks.onLevelMobs(level, rooms, entrance);

		//sin elite
		if (Random.Float() < SIN_ELITE_CHANCE) {
			ArrayList<Mob> pool = new ArrayList<>();
			for (Mob m : level.mobs) if (eligible(m)) pool.add(m);
			if (!pool.isEmpty()) {
				Mob m = Random.element(pool);
				@SuppressWarnings("unchecked")
				Class<? extends Buff> cls = (Class<? extends Buff>) Random.element(SinElite.ALL);
				Buff.affect(m, cls);
			}
		}

		//region elite
		int region = (Dungeon.depth - 1) / 5;
		if ((regionsDone & (1 << region)) != 0) return;
		int lastFloor = region * 5 + 4;
		int floorsLeft = lastFloor - Dungeon.depth + 1;
		if (floorsLeft < 1 || Random.Int(floorsLeft) != 0) return;
		if (spawnRegionElite(level, rooms, entrance, region)) regionsDone |= 1 << region;
	}

	private static int farCell(Level level, ArrayList<Room> rooms, Room entrance) {
		ArrayList<Room> cands = new ArrayList<>();
		for (Room r : rooms) {
			if (r instanceof StandardRoom && r != entrance && ((StandardRoom) r).mobSpawnWeight() > 0
					&& r.width() > 3 && r.height() > 3) cands.add(r);
		}
		for (int tries = 0; tries < 60 && !cands.isEmpty(); tries++) {
			Room r = Random.element(cands);
			int c = level.pointToCell(r.random(1));
			if (level.passable[c] && !level.solid[c] && level.findMob(c) == null && c != level.exit()
					&& c != level.entrance() && level.openSpace[c]
					&& level.trueDistance(c, level.entrance()) >= 10
					&& r.canPlaceCharacter(level.cellToPoint(c), level)) {
				return c;
			}
		}
		return -1;
	}

	private static boolean spawnRegionElite(Level level, ArrayList<Room> rooms, Room entrance, int region) {
		int cell = farCell(level, rooms, entrance);
		if (cell == -1) return false;
		Mob elite;
		switch (region) {
			case 0: elite = new RatChieftain(); break;
			case 1: elite = new ChainWarden(); break;
			case 2: elite = new GnollForeman(); break;
			case 3: elite = new FallenPriest(); break;
			default: elite = new Hellhound(); break;
		}
		elite.pos = cell;
		level.mobs.add(elite);

		if (elite instanceof Hellhound) {
			int twinCell = -1;
			for (int n : PathFinder.NEIGHBOURS8) {
				int c = cell + n;
				if (level.passable[c] && !level.solid[c] && level.findMob(c) == null) { twinCell = c; break; }
			}
			if (twinCell != -1) {
				Hellhound a = (Hellhound) elite;
				Hellhound b = new Hellhound();
				b.pos = twinCell;
				level.mobs.add(b);
				a.twin = b.id();
				b.twin = a.id();
			}
		}
		return true;
	}

	/** the sin elite buff on a character, if any (Char.buff only matches exact classes) */
	public static SinElite sinElite(Char ch) {
		for (SinElite e : ch.buffs(SinElite.class)) return e;
		return null;
	}

	// ---- rewards ----

	/** Mob.die */
	public static void onMobDeath(Mob mob, Object cause) {
		SinElite e = sinElite(mob);
		if (e == null || Dungeon.level == null) return;
		Dungeon.level.drop(new SinEssence(), mob.pos).sprite.drop();
		if (e instanceof SinElite.Greedy) {
			Dungeon.level.drop(((SinElite.Greedy) e).loot(), mob.pos).sprite.drop();
		}
		if (Dungeon.hero != null && Dungeon.hero.isAlive() && SinRewards.sinOf(Dungeon.hero) == e.sin()) {
			if (SinRewards.grant(Dungeon.hero, SIN_POINTS_FOR_OWN_SIN)) {
				GLog.p(Messages.get(SinElite.class, "own_sin", e.prefix()));
			}
		}
	}

	/** a region elite's reward: two essences and an uncursed +1 weapon, armor or ring */
	public static void regionReward(int pos) {
		if (Dungeon.level == null) return;
		SinEssence ess = new SinEssence();
		ess.quantity(2);
		Dungeon.level.drop(ess, pos).sprite.drop();

		Item it;
		switch (Random.Int(3)) {
			case 0: it = Generator.random(Generator.Category.WEAPON); break;
			case 1: it = Generator.random(Generator.Category.ARMOR); break;
			default: it = Generator.random(Generator.Category.RING); break;
		}
		if (it instanceof Weapon && ((Weapon) it).hasCurseEnchant()) ((Weapon) it).enchant(null);
		if (it instanceof Armor && ((Armor) it).hasCurseGlyph()) ((Armor) it).inscribe(null);
		it.cursed = false;
		if (it.isUpgradable() && it.level() < 1) it.upgrade();
		Dungeon.level.drop(it, pos).sprite.drop();
	}

	// ---- naming ----

	/** Mob.name: "Wrathful rat" */
	public static String name(Mob mob, String base) {
		SinElite e = sinElite(mob);
		if (e == null) return base;
		return Messages.get(SinElite.class, "mob_name", e.prefix(), base);
	}
}
