package com.shatteredpixel.shatteredpixeldungeon.sloth;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SlothRepose;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DreamPuppet;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.SlothPillow;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

/**
 * Game hooks for the Sloth class. Every method is a cheap pass-through
 * when the hero has no SlothRepose.
 */
public final class SlothHooks {

	private SlothHooks() {}

	private static SlothRepose repose(Hero hero) {
		return hero == null ? null : hero.buff(SlothRepose.class);
	}

	private static boolean heroCaused(Object cause) {
		return cause == Dungeon.hero
				|| cause instanceof Weapon
				|| cause instanceof Weapon.Enchantment
				|| cause instanceof Wand
				|| cause instanceof ArmorAbility
				|| cause == SlothPillow.class;
	}

	// ------------------------------------------------------------------ combat

	/** Called from Hero.attackProc for every attack. */
	public static int onAttack(Hero hero, Char enemy, int dmg) {
		SlothRepose r = repose(hero);
		if (r == null) return dmg;
		KindOfWeapon wep = hero.belongings.attackingWeapon();
		dmg += r.meleeBonus();
		if (wep instanceof MissileWeapon) return dmg;
		//sloth lets the puppets do the fighting: its own swings are feeble
		//(the sleepwalker's automatic attacks have their own multiplier instead)
		//Seven Sins: awakening (sloth's monster: x0.5; the sleepwalk talent boosts automatic attacks)
		if (!SlothRepose.autoAttacking) dmg = Math.round(dmg * com.shatteredpixel.shatteredpixeldungeon.sins.AwakeningHooks.slothMelee(hero, MELEE_PENALTY));
		else dmg = Math.round(dmg * com.shatteredpixel.shatteredpixeldungeon.sins.AwakeningHooks.slothAutoAttack(hero));
		return dmg;
	}

	/** SLOTH_COMFY was "dream sight" until Nov 15 2026; only the other classes' version still shares sight */
	public static boolean sharesSight(Mob m) {
		return com.shatteredpixel.shatteredpixeldungeon.sins.MetaTalents.sharesSight(m); //the other classes: any ally
	}

	/** dream sight +1: the hero sees the tiles within 2 of every puppet */
	public static void revealAroundPuppets(boolean[] fov) {
		com.shatteredpixel.shatteredpixeldungeon.sins.MetaTalents.revealAroundAllies(fov); //the other classes: any ally
	}

	/** Sloth's puppets no longer show tiles to the hero (the talent became dream delivery) */
	public static boolean puppetSees(int cell) {
		return false;
	}

	// ------------------------------------------------------------------ dream delivery (SLOTH_COMFY)

	/** tiles where the hero put something down on this floor: the puppets leave those alone */
	private static final java.util.HashSet<Integer> noFetch = new java.util.HashSet<>();
	private static int noFetchFloor = -1;

	private static void syncFloor() {
		int key = Dungeon.depth * 100 + Dungeon.branch;
		if (key != noFetchFloor) {
			noFetchFloor = key;
			noFetch.clear();
		}
	}

	/** Item.doDrop: what the hero drops on purpose stays where it is */
	public static void onHeroDropped(int cell) {
		syncFloor();
		noFetch.add(cell);
	}

	/**
	 * Dream delivery, from the end of DreamPuppet.act: loose items next to the puppet (+1: its own and the
	 * adjacent tiles, +2: tiles it can see within 2) are carried to a tile beside the hero. Only plain heaps:
	 * chests, shop goods and remains stay, and so does anything on a pedestal (room prizes, ritual offerings).
	 */
	public static void fetchItems(DreamPuppet p) {
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive() || !p.isAlive()) return;
		int pts = hero.pointsInTalent(Talent.SLOTH_COMFY);
		if (pts <= 0) return;
		com.shatteredpixel.shatteredpixeldungeon.levels.Level l = Dungeon.level;
		if (l.distance(p.pos, hero.pos) <= 2) return;        //already beside the hero
		syncFloor();
		int w = l.width(), px = p.pos % w, py = p.pos / w;
		for (int y = py - pts; y <= py + pts; y++) {
			for (int x = px - pts; x <= px + pts; x++) {
				if (x < 0 || y < 0 || x >= w || y >= l.height()) continue;
				int cell = x + y * w;
				com.shatteredpixel.shatteredpixeldungeon.items.Heap h = l.heaps.get(cell);
				if (h == null || h.type != com.shatteredpixel.shatteredpixeldungeon.items.Heap.Type.HEAP || h.isEmpty()) continue;
				if (noFetch.contains(cell) || l.distance(cell, hero.pos) <= 2) continue;
				if (l.map[cell] == com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.PEDESTAL) continue;
				if (cell != p.pos && (p.fieldOfView == null || cell >= p.fieldOfView.length || !p.fieldOfView[cell])) continue;
				int dest = deliveryCell(hero);
				String first = h.peek().name();
				int count = 0;
				while (!h.isEmpty()) {
					com.shatteredpixel.shatteredpixeldungeon.items.Item it = h.pickUp();
					if (it == null) break;
					l.drop(it, dest).sprite.drop();
					count++;
				}
				if (count > 0) {
					if (l.heroFOV[cell]) {
						com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter.get(cell).burst(
								com.shatteredpixel.shatteredpixeldungeon.effects.Speck.factory(
										com.shatteredpixel.shatteredpixeldungeon.effects.Speck.NOTE), 2);
					}
					com.shatteredpixel.shatteredpixeldungeon.utils.GLog.i(count > 1
							? com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(DreamPuppet.class, "fetch_many", first, count - 1)
							: com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(DreamPuppet.class, "fetch", first));
				}
			}
		}
	}

	/** a free tile next to the hero for delivered items, else the hero's own tile */
	private static int deliveryCell(Hero hero) {
		com.shatteredpixel.shatteredpixeldungeon.levels.Level l = Dungeon.level;
		ArrayList<Integer> free = new ArrayList<>();
		for (int d : PathFinder.NEIGHBOURS8) {
			int c = hero.pos + d;
			if (c < 0 || c >= l.length() || !l.passable[c] || l.pit[c] || l.traps.get(c) != null) continue;
			if (l.map[c] == com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.PEDESTAL) continue;
			com.shatteredpixel.shatteredpixeldungeon.items.Heap h = l.heaps.get(c);
			if (h != null && h.type != com.shatteredpixel.shatteredpixeldungeon.items.Heap.Type.HEAP) continue;
			if (Actor.findChar(c) != null) continue;
			free.add(c);
		}
		return free.isEmpty() ? hero.pos : free.get(com.watabou.utils.Random.Int(free.size()));
	}

	/** enemies the hero only sees through the puppets: they don't stop resting or searching */
	private static final java.util.HashSet<Integer> puppetOnly = new java.util.HashSet<>();

	/**
	 * Called from Hero.checkVisibleMobs for each visible enemy. Returns true if it should count as newly
	 * spotted (interrupting rest): not when only a puppet sees it, but yes once the hero sees it with
	 * its own eyes after having watched it through a puppet.
	 */
	public static boolean spotted(Hero hero, Mob m, boolean wasVisible) {
		boolean onlyPuppet = hero.mindVisionEnemies.contains(m) && puppetSees(m.pos);
		boolean wasPuppetOnly = puppetOnly.contains(m.id());
		if (onlyPuppet) puppetOnly.add(m.id());
		else puppetOnly.remove(m.id());
		if (onlyPuppet) return false;
		return !wasVisible || wasPuppetOnly;
	}

	/** sloth's own melee attacks deal 30% less */
	public static final float MELEE_PENALTY = 0.7f;

	/** Called from Hero.defenseProc when an enemy hits the hero. */
	public static int onDefense(Hero hero, Char attacker, int dmg) {
		SlothRepose r = repose(hero);
		if (r == null || dmg <= 0) return dmg;


		//the sleepwalker barely notices hits while asleep
		int deep = hero.pointsInTalent(Talent.WALK_DEEP);
		if (deep > 0 && r.asleep()) {
			dmg = Math.round(dmg * (1f - 0.08f * deep));
		}

		//dream lord: each puppet shares a little of the pain
		if (hero.subClass == HeroSubClass.DREAM_LORD) {
			int bond = hero.pointsInTalent(Talent.LORD_BOND);
			if (bond > 0) {
				int n = DreamPuppet.active().size();
				dmg = Math.round(dmg * (1f - Math.min(0.5f, 0.03f * bond * n)));
			}
		}
		return dmg;
	}

	/** Called from Char.damage before damage is applied. Redirects hero damage during Proxy. */
	public static int modifyDamage(Char target, Object src, int dmg) {
		//any harm breaks the Dream Forge's sleep
		if (target instanceof Hero && dmg > 0) {
			SlothRepose.DreamForge forge = target.buff(SlothRepose.DreamForge.class);
			if (forge != null) forge.interrupt();
		}
		if (!(target instanceof Hero) || dmg <= 0 || src instanceof Hunger) return dmg;
		Hero hero = (Hero) target;
		if (hero.buff(SlothRepose.ProxyGuard.class) == null) return dmg;

		ArrayList<DreamPuppet> ps = DreamPuppet.active();
		if (ps.isEmpty()) return Math.round(dmg * 0.5f);
		int share = (int) Math.ceil(dmg / (float) ps.size());
		for (DreamPuppet p : ps) {
			p.damage(share, src == null ? hero : src);
		}
		return 0;
	}

	/** Called from Mob.chooseEnemy: a taunted enemy only hunts its puppet. */
	public static Char tauntTarget(Mob mob) {
		SlothRepose.Taunted t = mob.buff(SlothRepose.Taunted.class);
		if (t == null) return null;
		Actor a = Actor.findById(t.object);
		if (a instanceof Char && ((Char) a).isAlive()) return (Char) a;
		t.detach();
		return null;
	}

	/** Called from Mob.die. */
	public static void onMobDeath(Mob mob, Object cause) {
		Hero hero = Dungeon.hero;
		SlothRepose r = repose(hero);
		if (r == null || !hero.isAlive() || mob.alignment != Char.Alignment.ENEMY) return;

		boolean byPuppet = cause instanceof DreamPuppet;
		float worth = 1f + mob.HT / 15f;
		if (byPuppet || (SlothRepose.autoAttacking && heroCaused(cause))) {
			r.addSloth(worth);
		} else if (heroCaused(cause)) {
			r.addSloth(worth / 3f);
		} else {
			return;
		}

		if (byPuppet) restfulSleep(hero);

		//dream lord: what a puppet defeats rises again as a short-lived nightmare
		if (byPuppet && hero.subClass == HeroSubClass.DREAM_LORD
				&& DreamPuppet.puppets(true).size() < r.maxLordNightmares()) {
			spawnNear(mob.pos, DreamPuppet.create(true, SlothRepose.lordNightmareLife(hero)));
		}
	}

	/** satiety per point of restful sleep for each kill a puppet makes */
	public static final int RESTFUL_FEED = 15;

	/** the harmful states restful sleep can wash away */
	@SuppressWarnings("unchecked")
	public static final Class<? extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff>[] HARMFUL_STATES = new Class[]{
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hex.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Degrade.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Daze.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Drowsy.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Dread.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok.class,
	};

	/** set by Mob.die around rollToDropLoot when a puppet killed the mob under restful sleep */
	public static boolean puppetKill = false;

	/** restful sleep: a puppet's kill rolls its loot at half the chance; monsters that always drop
	 *  (base chance 1 or more, and what mimics/statues carry, which isn't rolled) are untouched */
	public static float lootFactor(float baseChance) {
		return puppetKill && baseChance < 1f ? 0.5f : 1f;
	}

	/** restful sleep: false when a dream puppet killed the mob (its loot roll is halved) */
	public static boolean dropsLoot(Mob mob, Object cause) {
		Hero hero = Dungeon.hero;
		return !(cause instanceof DreamPuppet && hero != null && hero.pointsInTalent(Talent.SLOTH_RESTFUL) > 0);
	}

	/** restful sleep: each puppet kill feeds the sloth and cures one (+1) or every (+2) harmful state */
	private static void restfulSleep(Hero hero) {
		int pts = hero.pointsInTalent(Talent.SLOTH_RESTFUL);
		if (pts <= 0) return;
		Hunger hunger = hero.buff(Hunger.class);
		if (hunger != null) hunger.satisfy(RESTFUL_FEED * pts);
		ArrayList<com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff> found = new ArrayList<>();
		for (Class<? extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff> c : HARMFUL_STATES) {
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff b = hero.buff(c);
			if (b != null) found.add(b);
		}
		if (found.isEmpty()) return;
		if (pts >= 2) {
			for (com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff b : found) b.detach();
		} else {
			com.watabou.utils.Random.element(found).detach();
		}
		if (hero.sprite != null) hero.sprite.emitter().burst(com.shatteredpixel.shatteredpixeldungeon.effects.Speck.factory(
				com.shatteredpixel.shatteredpixeldungeon.effects.Speck.HEALING), 2);
	}

	/** places a puppet on or next to a cell; returns false if there was no room */
	public static boolean spawnNear(int cell, DreamPuppet p) {
		int at = -1;
		if (Actor.findChar(cell) == null && Dungeon.level.passable[cell]) {
			at = cell;
		} else {
			for (int n : PathFinder.NEIGHBOURS8) {
				int c = cell + n;
				if (Actor.findChar(c) == null && Dungeon.level.passable[c] && !Dungeon.level.avoid[c]) {
					at = c;
					break;
				}
			}
		}
		if (at == -1) return false;
		p.pos = at;
		GameScene.add(p);
		Dungeon.level.occupyCell(p);
		if (p.sprite != null) p.sprite.emitter().burst(com.shatteredpixel.shatteredpixeldungeon.effects.Speck.factory(
				com.shatteredpixel.shatteredpixeldungeon.effects.Speck.NOTE), 4);
		return true;
	}

	// ------------------------------------------------------------------ hero stats

	public static float accuracy(Hero hero, float mult) {
		SlothRepose r = repose(hero);
		return r == null ? mult : mult * (1f + r.accuracyBonus());
	}

	public static float evasion(Hero hero, float mult) {
		SlothRepose r = repose(hero);
		return r == null ? mult : mult * (1f + r.evasionBonus());
	}

	public static int drBonus(Hero hero, int dr) {
		SlothRepose r = repose(hero);
		return r == null ? dr : dr + r.drRoll();
	}

	public static int bonusHT(Hero hero, int ht) {
		SlothRepose r = repose(hero);
		if (r == null) return ht;
		ht += r.hpBonus();
		//every dream puppet out borrows part of the sloth's max HP
		return Math.max(1, Math.round(ht * (1f - SlothRepose.puppetDrain(hero))));
	}
}
