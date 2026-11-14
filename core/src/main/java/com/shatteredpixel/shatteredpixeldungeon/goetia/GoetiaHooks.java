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
	public static final int[] MILESTONES = {3, 6, 10, 15, 20, 25, 30, 35, 40, 50, 60, 72};
	/** indexes into MILESTONES (the owner's list of Nov 14 2026) */
	public static final int BRAND = 0, SIGHT = 1, GUIDE = 2, WARD = 3, HARVEST = 4, MERCHANT = 5, MIGHT = 6,
			FOCUS = 7, PURIFY = 8, SLAUGHTER = 9, SAFETY = 10, SOLOMON = 11;

	public static boolean power(int index) {
		return has(MILESTONES[index]);
	}
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

	/** the demon army challenge: every milestone needs twice the kills */
	public static int need(int milestone) {
		return Dungeon.isChallenged(com.shatteredpixel.shatteredpixeldungeon.Challenges.DEMON_ARMY)
				? Math.min(GoetiaDemon.values().length, milestone * 2) : milestone;
	}

	public static boolean has(int milestone) {
		return count() >= need(milestone);
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
		//the demon army challenge: twice as many (kinds repeat once the region's list runs out)
		if (Dungeon.isChallenged(com.shatteredpixel.shatteredpixeldungeon.Challenges.DEMON_ARMY)) n *= 2;
		ArrayList<GoetiaDemon> chosen = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			if (pool.isEmpty()) pool = GoetiaDemon.ofRegion(region);
			if (pool.isEmpty()) break;
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
		int drops = power(HARVEST) ? 2 : 1;
		if (com.shatteredpixel.shatteredpixeldungeon.sins.ChallengeHooks.essences()) Dungeon.level.drop(new DemonEssence().quantity(drops), f.pos).sprite.drop();
		record(f.demon);
		if (hero != null) hero.updateHT(false);
	}

	public static void record(GoetiaDemon d) {
		if (slain.contains(d.name())) return;
		int before = count();
		slain.add(d.name());
		GLog.p(Messages.get(GoetiaHooks.class, "recorded", d.rankName(), d.title(), count()));
		for (int i = 0; i < MILESTONES.length; i++) {
			if (before < need(MILESTONES[i]) && count() >= need(MILESTONES[i])) {
				GLog.p(Messages.get(GoetiaHooks.class, "milestone", Messages.get(GoetiaHooks.class, "power_" + i)));
			}
		}
	}

	/** the codex text for the essence bag's window */
	public static String codexText() {
		StringBuilder sb = new StringBuilder();
		sb.append(Messages.get(GoetiaHooks.class, "codex_intro", count(), GoetiaDemon.values().length)).append("\n\n");
		for (int i = 0; i < MILESTONES.length; i++) {
			sb.append(has(MILESTONES[i]) ? "_" : "").append(need(MILESTONES[i])).append(": ")
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

	/** 3: +10 max HP */
	public static int bonusHT(Hero hero, int ht) {
		if (power(BRAND)) ht += 10;
		return ht;
	}

	/** 6: demons on the floor are always sensed */
	public static void revealDemons(boolean[] mindFov) {
		if (!power(SIGHT) || Dungeon.level == null) return;
		for (Mob m : Dungeon.level.mobs) {
			if ((m instanceof GoetiaFiend || m instanceof GoetiaLord) && m.pos >= 0 && m.pos < mindFov.length) mindFov[m.pos] = true;
		}
	}

	/** Char.damage: 15: demons hurt the hero 20% less; 30: the hero deals 10% more */
	public static int modifyDamage(Char target, Object src, int dmg) {
		if (dmg <= 0) return dmg;
		if (target == Dungeon.hero && power(WARD) && (src instanceof GoetiaFiend || src instanceof GoetiaLord)) {
			dmg = Math.round(dmg * 0.8f);
		}
		if (src == Dungeon.hero && target != Dungeon.hero && power(MIGHT)) dmg = Math.round(dmg * 1.1f);
		return dmg;
	}

	/** (the old +3 armor, +20% experience and +10% accuracy powers are gone; the call sites stay) */
	public static int drBonus(Hero hero, int dr) {
		return dr;
	}

	public static int bonusExp(Hero hero, int exp) {
		return exp;
	}

	public static float accuracy(Hero hero, float acc) {
		return acc;
	}

	// ---- 10: the labyrinth's guidance

	/** the level that was last looked over for the guidance */
	private static Level guided = null;

	/** from tick: stairs, chests and the rough place of hidden rooms show on the map of each floor entered */
	private static void guide(Level level) {
		if (level == guided) return;
		if (!power(GUIDE)) return;
		guided = level;
		boolean found = false;
		for (com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition t : level.transitions) {
			found |= mapAround(level, t.cell());
		}
		for (com.shatteredpixel.shatteredpixeldungeon.items.Heap h : level.heaps.valueList()) {
			if (h.type == com.shatteredpixel.shatteredpixeldungeon.items.Heap.Type.CHEST
					|| h.type == com.shatteredpixel.shatteredpixeldungeon.items.Heap.Type.LOCKED_CHEST
					|| h.type == com.shatteredpixel.shatteredpixeldungeon.items.Heap.Type.CRYSTAL_CHEST) {
				if (!h.seen) found = true;
				h.seen = true;
				if (h.sprite != null) h.sprite.visible = true;
				if (!level.mapped[h.pos] && !level.visited[h.pos]) {
					level.mapped[h.pos] = true;
					found = true;
				}
			}
		}
		if (level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel) {
			for (com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room room
					: ((com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel) level).rooms()) {
				if (room instanceof com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SecretRoom) {
					//only the middle of the room: where it is, not how to get in
					found |= mapAround(level, level.pointToCell(room.center()));
				}
			}
		}
		if (found) {
			GameScene.updateFog();
			GLog.i(Messages.get(GoetiaHooks.class, "guided"));
		}
	}

	private static boolean mapAround(Level level, int cell) {
		boolean found = false;
		if (!level.insideMap(cell)) return false;
		for (int n : com.watabou.utils.PathFinder.NEIGHBOURS9) {
			int c = cell + n;
			if (c < 0 || c >= level.length() || !level.discoverable[c]) continue;
			if (!level.mapped[c] && !level.visited[c]) {
				level.mapped[c] = true;
				found = true;
			}
		}
		return found;
	}

	// ---- 25: the merchant's blessing

	public static final float SHOP_DISCOUNT = 0.85f;

	/** Shopkeeper.sellPrice */
	public static float shopPrice() {
		return power(MERCHANT) ? SHOP_DISCOUNT : 1f;
	}

	/** ShopRoom.generateItems: two of the mod's consumables join the stock */
	public static void addShopStock(ArrayList<com.shatteredpixel.shatteredpixeldungeon.items.Item> stock) {
		if (!power(MERCHANT)) return;
		Class<?>[] pool = {
				com.shatteredpixel.shatteredpixeldungeon.items.elite.ElixirOfSin.class,
				com.shatteredpixel.shatteredpixeldungeon.items.elite.Stimulant.class,
				com.shatteredpixel.shatteredpixeldungeon.items.elite.Bandage.class,
				com.shatteredpixel.shatteredpixeldungeon.items.elite.DecoyDoll.class,
				com.shatteredpixel.shatteredpixeldungeon.items.bombs.SinBomb.class,
				com.shatteredpixel.shatteredpixeldungeon.items.bombs.StickyBomb.class,
				com.shatteredpixel.shatteredpixeldungeon.items.bombs.CaltropBomb.class,
		};
		ArrayList<Class<?>> left = new ArrayList<>(java.util.Arrays.asList(pool));
		for (int i = 0; i < 2 && !left.isEmpty(); i++) {
			Class<?> c = left.remove(Random.Int(left.size()));
			Object item = com.watabou.utils.Reflection.newInstance(c);
			if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.Item) {
				stock.add((com.shatteredpixel.shatteredpixeldungeon.items.Item) item);
			}
		}
	}

	// ---- 35 and 72: charge

	public static final float FOCUS_CHARGE = 1.15f;
	public static final float SOLOMON_CHARGE = 1.5f;

	/** RingOfEnergy.wandChargeMultiplier / artifactChargeMultiplier */
	public static float chargeMult(Char target) {
		if (target != Dungeon.hero) return 1f;
		float mult = 1f;
		if (power(FOCUS)) mult *= FOCUS_CHARGE;
		if (power(SOLOMON)) mult *= SOLOMON_CHARGE;
		return mult;
	}

	// ---- 40: the purifying ward

	/** Generator: half of the curses on freshly generated gear are lifted */
	public static com.shatteredpixel.shatteredpixeldungeon.items.Item purify(com.shatteredpixel.shatteredpixeldungeon.items.Item item) {
		if (item == null || !item.cursed || Dungeon.hero == null || !power(PURIFY) || Random.Int(2) != 0) return item;
		item.cursed = false;
		if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon
				&& ((com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon) item).hasCurseEnchant()) {
			((com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon) item).enchant(null);
		}
		if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor
				&& ((com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor) item).hasCurseGlyph()) {
			((com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor) item).inscribe(null);
		}
		return item;
	}

	/** AlchemyScene: recipes cost 1 energy less (never free) */
	public static int alchemyCost(int cost) {
		return cost > 1 && power(PURIFY) ? cost - 1 : cost;
	}

	// ---- 50: the slaughter combo

	private static void slaughter(Mob mob) {
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive() || mob.alignment != Char.Alignment.ENEMY || !power(SLAUGHTER)) return;
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DemonSlaughter s = com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.prolong(
				hero, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DemonSlaughter.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DemonSlaughter.DURATION);
		s.kill();
	}

	private static int slaughterStacks(Hero hero) {
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DemonSlaughter s =
				hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DemonSlaughter.class);
		return s == null ? 0 : s.stacks;
	}

	/** Hero.speed */
	public static float moveSpeed(Hero hero) {
		return 1f + com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DemonSlaughter.SPEED * slaughterStacks(hero);
	}

	/** Hero.defenseSkill */
	public static float evasion(Hero hero, float evasion) {
		return evasion * (1f + com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DemonSlaughter.EVASION * slaughterStacks(hero));
	}

	// ---- 60: the safety net

	public static final float SAFETY_HP = 0.20f;

	/** Char.damage, after the HP is taken: at 20% or less the hero blasts its enemies back and blinks away */
	public static void afterDamage(Char target) {
		if (!(target instanceof Hero) || target != Dungeon.hero || Dungeon.level == null) return;
		Hero hero = (Hero) target;
		if (hero.HP <= 0 || hero.HP > hero.HT * SAFETY_HP || !power(SAFETY)) return;
		if (hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SafetyNetCooldown.class) != null) return;
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(hero,
				com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SafetyNetCooldown.class,
				com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SafetyNetCooldown.DURATION);
		Level level = Dungeon.level;

		//the blast
		for (Mob m : level.mobs.toArray(new Mob[0])) {
			if (m.alignment != Char.Alignment.ENEMY || !m.isAlive() || level.distance(m.pos, hero.pos) > 2) continue;
			if (m.pos == hero.pos) continue;
			//the line from the hero through the enemy, carried on past it
			com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica line =
					new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(hero.pos, m.pos,
							com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.WONT_STOP);
			int at = line.path.indexOf(m.pos);
			if (at == -1 || at + 1 >= line.path.size()) continue;
			com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica away =
					new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(m.pos, line.path.get(at + 1),
							com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.MAGIC_BOLT);
			com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave.throwChar(m, away, 3, false, false, GoetiaHooks.class);
		}

		//the blink: somewhere 4-8 tiles off that can be walked to, away from every enemy
		com.watabou.utils.PathFinder.buildDistanceMap(hero.pos,
				com.watabou.utils.BArray.or(level.passable, level.avoid, null), 12);
		ArrayList<Integer> cells = new ArrayList<>();
		for (int i = 0; i < level.length(); i++) {
			int d = com.watabou.utils.PathFinder.distance[i];
			if (d == Integer.MAX_VALUE || d < 4 || d > 8 || !level.passable[i] || level.pit[i]
					|| Actor.findChar(i) != null || level.traps.get(i) != null) continue;
			boolean near = false;
			for (Mob m : level.mobs) {
				if (m.alignment == Char.Alignment.ENEMY && level.distance(m.pos, i) <= 2) { near = true; break; }
			}
			if (!near) cells.add(i);
		}
		if (!cells.isEmpty()) {
			hero.interrupt();
			com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation.teleportToLocation(hero, Random.element(cells));
		}
		if (hero.sprite != null) {
			hero.sprite.showStatus(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.POSITIVE,
					Messages.get(GoetiaHooks.class, "safety_net"));
		}
		GLog.p(Messages.get(GoetiaHooks.class, "safety_net_log"));
	}

	// ---- 72: Solomon's authority

	/** Char.isImmune: the one who bound all 72 shrugs off harmful states */
	public static boolean immune(Char target, Class<?> effect) {
		if (target != Dungeon.hero || effect == null || !power(SOLOMON)) return false;
		for (Class<?> c : com.shatteredpixel.shatteredpixeldungeon.sloth.SlothHooks.HARMFUL_STATES) {
			if (c == effect) return true;
		}
		return effect == com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis.class
				|| effect == com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost.class;
	}

	// ------------------------------------------------------------------ the hidden sigils

	/** Mob.die: a floor boss fell, so its floor gets a hidden sigil once it is unlocked */
	public static void onMobDeath(Mob mob, Object cause) {
		slaughter(mob);
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
		if (Dungeon.level != null) guide(Dungeon.level);
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
