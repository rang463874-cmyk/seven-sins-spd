package com.shatteredpixel.shatteredpixeldungeon.goetia;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.goetia.GoetiaFiend;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.goetia.GoetiaLord;
import com.shatteredpixel.shatteredpixeldungeon.items.DemonEssence;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.GoetiaSigil;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Seven Sins: Solomon's demons (Nov 2026). Per run:
 * - 2-3 regular demons on every sewer floor 2-4, 3-4 on every regular floor 5-24, drawn from the floor's region (unmet ones first);
 * - each one killed goes into the codex ("the Lesser Key"); every milestone in {@link #MILESTONES} gives a
 *   lasting power for the run; each drops demon essences;
 * - after a floor boss (5..25) falls, a hidden sigil appears somewhere on its floor; offering essences
 *   there opens a side arena ({@link #ARENA_BRANCH}) with that floor's demon king ({@link GoetiaLord}).
 * State lives in the quests bundle like the other mod quests.
 */
public class GoetiaHooks {

	public static final int ARENA_BRANCH = 8;
	/** codex milestones (demons slain this run) */
	public static final int[] MILESTONES = {3, 6, 10, 15, 20, 25, 30, 40};
	/** essences asked at the sigil of floor 5, 10, 15, 20, 25 */
	public static final int[] OFFER = {3, 4, 5, 6, 7};

	public static final HashSet<String> slain = new HashSet<>();
	/** boss floors whose sigil has been placed / whose king has been beaten */
	public static final HashSet<Integer> sigils = new HashSet<>();
	public static final HashSet<Integer> lordsBeaten = new HashSet<>();
	/** a boss floor waiting for its sigil (placed once the floor is unlocked) */
	public static int pendingSigil = -1;

	public static void reset() {
		slain.clear();
		sigils.clear();
		lordsBeaten.clear();
		pendingSigil = -1;
	}

	public static boolean slain(GoetiaDemon d) {
		return slain.contains(d.name());
	}

	public static int count() {
		return slain.size();
	}

	public static boolean has(int milestone) {
		return count() >= milestone;
	}

	// ------------------------------------------------------------------ spawning

	/** from EliteHooks.onLevelMobs (regular floors 2-24, main branch) */
	public static void onLevelMobs(Level level) {
		int depth = Dungeon.depth;
		if (Dungeon.branch != 0 || depth < 2 || depth > 24 || Dungeon.bossLevel()) return;
		int region = Math.min(4, (depth - 1) / 5);
		ArrayList<GoetiaDemon> pool = GoetiaDemon.ofRegion(region);
		//owner (Nov 13 2026): more demons now that their essences have more uses; the sewers keep 2-3 (only 3 kinds live there)
		int n = region == 0 ? 2 + (Random.Int(4) == 0 ? 1 : 0) : 3 + (Random.Int(2) == 0 ? 1 : 0);
		ArrayList<GoetiaDemon> chosen = new ArrayList<>();
		for (int i = 0; i < n && !pool.isEmpty(); i++) {
			ArrayList<GoetiaDemon> unmet = new ArrayList<>();
			for (GoetiaDemon d : pool) if (!slain(d)) unmet.add(d);
			GoetiaDemon d = (!unmet.isEmpty() && Random.Int(4) != 0) ? Random.element(unmet) : Random.element(pool);
			pool.remove(d);
			chosen.add(d);
		}
		for (GoetiaDemon d : chosen) {
			GoetiaFiend f = new GoetiaFiend().setup(d, depth);
			int cell = -1;
			for (int t = 0; t < 40 && cell == -1; t++) {
				int c = level.randomRespawnCell(f);
				if (c != -1 && level.distance(c, level.entrance()) > 7 && level.findMob(c) == null) cell = c;
			}
			if (cell == -1) continue;
			f.pos = cell;
			level.mobs.add(f);
		}
	}

	// ------------------------------------------------------------------ codex

	public static void onFiendKilled(GoetiaFiend f) {
		Hero hero = Dungeon.hero;
		int drops = has(15) ? 2 : 1;
		Dungeon.level.drop(new DemonEssence().quantity(drops), f.pos).sprite.drop();
		record(f.demon);
		if (hero != null) hero.updateHT(false);
	}

	public static void record(GoetiaDemon d) {
		if (slain.contains(d.name())) return;
		int before = count();
		slain.add(d.name());
		GLog.p(Messages.get(GoetiaHooks.class, "recorded", d.rankName(), d.title(), count()));
		for (int i = 0; i < MILESTONES.length; i++) {
			if (before < MILESTONES[i] && count() >= MILESTONES[i]) {
				GLog.p(Messages.get(GoetiaHooks.class, "milestone", Messages.get(GoetiaHooks.class, "power_" + i)));
			}
		}
	}

	/** the codex text for the essence bag's window */
	public static String codexText() {
		StringBuilder sb = new StringBuilder();
		sb.append(Messages.get(GoetiaHooks.class, "codex_intro", count(), GoetiaDemon.values().length)).append("\n\n");
		for (int i = 0; i < MILESTONES.length; i++) {
			sb.append(has(MILESTONES[i]) ? "_" : "").append(MILESTONES[i]).append(": ")
					.append(Messages.get(GoetiaHooks.class, "power_" + i)).append(" - ")
					.append(Messages.get(GoetiaHooks.class, "power_" + i + "_desc"))
					.append(has(MILESTONES[i]) ? "_" : "").append("\n");
		}
		String[] regions = {"region_0", "region_1", "region_2", "region_3", "region_4"};
		for (int r = 0; r < 5; r++) {
			sb.append("\n").append(Messages.get(GoetiaHooks.class, regions[r])).append("\n");
			for (GoetiaDemon d : GoetiaDemon.ofRegion(r)) sb.append(line(d));
		}
		sb.append("\n").append(Messages.get(GoetiaHooks.class, "region_lords")).append("\n");
		for (GoetiaDemon d : GoetiaDemon.values()) if (d.isLord()) sb.append(line(d));
		return sb.toString();
	}

	private static String line(GoetiaDemon d) {
		return (slain(d) ? "_" + d.number() + ". " + d.rankName() + " " + d.title() + "_"
				: d.number() + ". ???") + "\n";
	}

	// ------------------------------------------------------------------ milestone powers

	/** 3: +10 max HP; 40: +30 more */
	public static int bonusHT(Hero hero, int ht) {
		if (has(MILESTONES[0])) ht += 10;
		if (has(MILESTONES[7])) ht += 30;
		return ht;
	}

	/** 6: demons on the floor are always sensed */
	public static void revealDemons(boolean[] mindFov) {
		if (!has(MILESTONES[1]) || Dungeon.level == null) return;
		for (Mob m : Dungeon.level.mobs) {
			if ((m instanceof GoetiaFiend || m instanceof GoetiaLord) && m.pos >= 0 && m.pos < mindFov.length) mindFov[m.pos] = true;
		}
	}

	/** Char.damage: 10: demons hurt the hero 20% less; 20: the hero deals 10% more */
	public static int modifyDamage(Char target, Object src, int dmg) {
		if (dmg <= 0) return dmg;
		if (target == Dungeon.hero && has(MILESTONES[2]) && (src instanceof GoetiaFiend || src instanceof GoetiaLord)) {
			dmg = Math.round(dmg * 0.8f);
		}
		if (src == Dungeon.hero && target != Dungeon.hero && has(MILESTONES[4])) dmg = Math.round(dmg * 1.1f);
		return dmg;
	}

	/** 25: +3 armor */
	public static int drBonus(Hero hero, int dr) {
		return has(MILESTONES[5]) ? dr + 3 : dr;
	}

	/** 30: +20% experience */
	public static int bonusExp(Hero hero, int exp) {
		return has(MILESTONES[6]) ? Math.round(exp * 1.2f) : exp;
	}

	/** 40: +10% accuracy */
	public static float accuracy(Hero hero, float acc) {
		return has(MILESTONES[7]) ? acc * 1.1f : acc;
	}

	// ------------------------------------------------------------------ the hidden sigils

	/** Mob.die: a floor boss fell, so its floor gets a hidden sigil once it is unlocked */
	public static void onMobDeath(Mob mob, Object cause) {
		if (Dungeon.branch != 0) return;
		int d = Dungeon.depth;
		if (d % 5 != 0 || d > 25 || sigils.contains(d)) return;
		if (mob instanceof Goo || mob instanceof Tengu || mob instanceof DM300 || mob instanceof DwarfKing
				|| mob instanceof YogDzewa) {
			pendingSigil = d;
		}
	}

	/** from Hero.act: place a pending sigil once the boss floor is no longer locked */
	public static void tick(Hero hero) {
		if (Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.GoetiaArenaLevel) {
			((com.shatteredpixel.shatteredpixeldungeon.levels.GoetiaArenaLevel) Dungeon.level).checkRitual();
		}
		if (Dungeon.branch != 0 || Dungeon.level == null || Dungeon.level.locked) return;
		int depth = Dungeon.depth;
		if (depth % 5 != 0 || depth > 25 || sigils.contains(depth)) return;
		//the boss fell just now, or (saves from before the sigils, a floor revisited) the hero has been deeper already
		if (pendingSigil != depth && com.shatteredpixel.shatteredpixeldungeon.Statistics.deepestFloor <= depth) return;
		Level level = Dungeon.level;
		pendingSigil = -1;
		sigils.add(depth);

		//owner (Nov 13 2026): a hidden room off the boss floor, like the rat king's.
		//The sewer boss floor is built with one (GoetiaSigilRoom); the fixed boss maps get one dug into a wall
		//where there is room. Caves and city have no spare wall: there the sigil appears beside the exit.
		for (com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap t : level.traps.valueList()) {
			if (t instanceof GoetiaSigil) {
				GLog.w(Messages.get(GoetiaHooks.class, "chamber_hint"));
				return;
			}
		}
		int dist = carveChamber(level, hero);
		if (dist >= 0) {
			GLog.w(Messages.get(GoetiaHooks.class, dist <= 8 ? "chamber_hint_near" : "chamber_hint"));
			return;
		}

		//no wall thick enough for a room: the sigil appears in the open instead
		int target = level.exit();
		if (target <= 0 || !level.insideMap(target)) target = hero.pos;
		int cell = -1, best = Integer.MAX_VALUE;
		for (int i = 0; i < level.length(); i++) {
			if (openFloor(level, i) && Actor.findChar(i) == null && level.heaps.get(i) == null
					&& level.traps.get(i) == null && level.plants.get(i) == null) {
				int d = level.distance(i, target);
				//beside the stairs, not right in the way
				if (d >= 2 && d < best) {
					best = d;
					cell = i;
				}
			}
		}
		if (cell == -1) return;
		level.setTrap(new GoetiaSigil(), cell);
		Level.set(cell, Terrain.TRAP);
		GameScene.updateMap(cell);
		GLog.w(Messages.get(GoetiaHooks.class, "sigil_hint"));
	}

	private static boolean openFloor(Level level, int i) {
		if (!level.insideMap(i)) return false;
		int t = level.map[i];
		return (t == Terrain.EMPTY || t == Terrain.EMPTY_SP || t == Terrain.EMPTY_DECO || t == Terrain.GRASS)
				&& level.passable[i] && !level.pit[i] && level.getTransition(i) == null;
	}

	private static boolean covered(ArrayList<com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap> list, int x, int y) {
		if (list == null) return false;
		for (com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap c : list) {
			if (x + 4 >= c.tileX && x < c.tileX + c.tileW && y + 4 >= c.tileY && y < c.tileY + c.tileH) return true;
		}
		return false;
	}

	/** a 5x5 block of plain wall (no custom tile art over it) with room to spare from the level's edge */
	private static boolean solidBlock(Level level, int x, int y) {
		int w = level.width();
		for (int j = y; j < y + 5; j++) {
			for (int i = x; i < x + 5; i++) {
				int t = level.map[i + j * w];
				if (t != Terrain.WALL && t != Terrain.WALL_DECO) return false;
			}
		}
		return !covered(level.customTiles, x, y) && !covered(level.customWalls, x, y);
	}

	//door cell inside the block (dx, dy) and the step from it to the floor outside (ox, oy)
	private static final int[][] DOORS = {{2, 0, 0, -1}, {2, 4, 0, 1}, {0, 2, -1, 0}, {4, 2, 1, 0}};

	/**
	 * Digs the sigil's chamber into solid wall: a 3x3 room behind a secret door, as close to the floor's exit as the
	 * walls allow, with the sigil in its middle. Returns the door's distance from the exit, or -1 if no wall fits.
	 */
	private static int carveChamber(Level level, Hero hero) {
		int w = level.width(), h = level.height();
		int target = level.exit();
		if (target <= 0 || !level.insideMap(target)) target = hero.pos;

		int bestX = -1, bestY = -1, bestDoor = -1, bestDist = Integer.MAX_VALUE;
		for (int y = 1; y + 5 <= h - 1; y++) {
			for (int x = 1; x + 5 <= w - 1; x++) {
				if (!solidBlock(level, x, y)) continue;
				for (int d = 0; d < DOORS.length; d++) {
					int door = (x + DOORS[d][0]) + (y + DOORS[d][1]) * w;
					int out = door + DOORS[d][2] + DOORS[d][3] * w;
					if (!openFloor(level, out)) continue;
					int dist = level.distance(out, target);
					if (dist < bestDist) {
						bestDist = dist;
						bestX = x;
						bestY = y;
						bestDoor = door;
					}
				}
			}
		}
		if (bestDoor == -1) return -1;

		for (int j = bestY + 1; j <= bestY + 3; j++) {
			for (int i = bestX + 1; i <= bestX + 3; i++) {
				Level.set(i + j * w, Terrain.EMPTY_SP, level);
			}
		}
		Level.set(bestDoor, Terrain.SECRET_DOOR, level);
		int centre = (bestX + 2) + (bestY + 2) * w;
		level.setTrap(new GoetiaSigil(), centre);
		Level.set(centre, Terrain.TRAP, level);
		level.cleanWalls();
		GameScene.updateMap();
		Dungeon.observe();
		return bestDist;
	}

	/** essences thrown onto the ritual chamber's four altars (one each) summon the king */
	public static final int RITUAL_ESSENCES = 4;

	public static int offerCost(int depth) {
		return OFFER[Math.max(0, Math.min(OFFER.length - 1, depth / 5 - 1))];
	}

	/** takes the hero to the demon king's arena of this boss floor */
	public static void travelToArena() {
		InterlevelScene.mode = InterlevelScene.Mode.RETURN;
		InterlevelScene.returnDepth = Dungeon.depth;
		InterlevelScene.returnBranch = ARENA_BRANCH;
		InterlevelScene.returnPos = com.shatteredpixel.shatteredpixeldungeon.levels.GoetiaArenaLevel.arrivalCell();
		Game.switchScene(InterlevelScene.class);
	}

	public static void onLordDefeated(GoetiaLord lord) {
		lordsBeaten.add(Dungeon.depth);
		record(lord.demon);
		if (Dungeon.hero != null) Dungeon.hero.updateHT(false);
		if (Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.GoetiaArenaLevel) {
			((com.shatteredpixel.shatteredpixeldungeon.levels.GoetiaArenaLevel) Dungeon.level).unlockPortal();
		}
	}

	// ------------------------------------------------------------------ saving

	private static final String NODE = "sevenSinsGoetia";

	public static void storeInBundle(Bundle bundle) {
		Bundle node = new Bundle();
		node.put("slain", slain.toArray(new String[0]));
		int[] s = new int[sigils.size()];
		int i = 0;
		for (int v : sigils) s[i++] = v;
		node.put("sigils", s);
		int[] l = new int[lordsBeaten.size()];
		i = 0;
		for (int v : lordsBeaten) l[i++] = v;
		node.put("lords", l);
		node.put("pending", pendingSigil);
		bundle.put(NODE, node);
	}

	public static void restoreFromBundle(Bundle bundle) {
		reset();
		if (!bundle.contains(NODE)) return;
		Bundle node = bundle.getBundle(NODE);
		String[] names = node.getStringArray("slain");
		if (names != null) for (String n : names) slain.add(n);
		int[] s = node.getIntArray("sigils");
		if (s != null) for (int v : s) sigils.add(v);
		int[] l = node.getIntArray("lords");
		if (l != null) for (int v : l) lordsBeaten.add(v);
		pendingSigil = node.contains("pending") ? node.getInt("pending") : -1;
	}
}
