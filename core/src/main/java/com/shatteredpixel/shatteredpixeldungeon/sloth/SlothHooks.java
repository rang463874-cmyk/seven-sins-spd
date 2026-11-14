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

	/** the other classes' version of SLOTH_COMFY: the hero sees what its allies see */
	public static boolean sharesSight(Mob m) {
		return com.shatteredpixel.shatteredpixeldungeon.sins.MetaTalents.sharesSight(m);
	}

	/** the other classes' version of SLOTH_COMFY +1: the tiles within 2 of every ally */
	public static void revealAroundPuppets(boolean[] fov) {
		com.shatteredpixel.shatteredpixeldungeon.sins.MetaTalents.revealAroundAllies(fov);
	}

	/** Sloth no longer sees through its puppets (dream sight became dream delivery) */
	public static boolean puppetSees(int cell) {
		return false;
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
	// ------------------------------------------------------------------ dream delivery (SLOTH_COMFY)

	/** items left this close to the Sloth are where they should be */
	public static final int DELIVERY_RANGE = 2;

	/** points of dream delivery: the puppets bring the items they find to the Sloth */
	public static int delivery() {
		Hero h = Dungeon.hero;
		if (h == null || !h.isAlive() || h.heroClass != com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.SLOTH) return 0;
		return h.pointsInTalent(Talent.SLOTH_COMFY);
	}

	/** true if a puppet may take the top of this heap to the Sloth */
	public static boolean fetchable(com.shatteredpixel.shatteredpixeldungeon.items.Heap heap) {
		if (heap == null || heap.type != com.shatteredpixel.shatteredpixeldungeon.items.Heap.Type.HEAP || heap.isEmpty()) return false;
		//offerings on altars and pedestals (rituals, displays) are not litter
		if (Dungeon.level.map[heap.pos] == com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.PEDESTAL) return false;
		if (Dungeon.level.distance(heap.pos, Dungeon.hero.pos) <= DELIVERY_RANGE) return false;
		for (com.shatteredpixel.shatteredpixeldungeon.items.Item i : heap.items) {
			if (!carriable(i)) return false;
		}
		return !heap.peek().dreamDelivered;
	}

	private static boolean carriable(com.shatteredpixel.shatteredpixeldungeon.items.Item i) {
		if (i instanceof com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb
				&& ((com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb) i).fuse != null) return false;
		return !(i instanceof com.shatteredpixel.shatteredpixeldungeon.items.quest.CeremonialCandle)
				&& !(i instanceof com.shatteredpixel.shatteredpixeldungeon.items.LostBackpack)
				&& !(i instanceof com.shatteredpixel.shatteredpixeldungeon.items.Honeypot.ShatteredPot);
	}

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

	/** restful sleep: chance drops of enemies killed by dream puppets are this likely */
	public static final float RESTFUL_LOOT = 0.5f;

	/** what is killing the mob whose loot is being rolled right now (set by Mob.die) */
	public static Object lootKill = null;

	/**
	 * Mob.lootChance: restful sleep halves the chance drops of a puppet's kills. Guaranteed drops (base
	 * chance 1), bosses and minibosses are untouched, and so is everything a monster drops outside its
	 * loot roll (a mimic's items, a statue's weapon, essences, quest drops).
	 */
	public static float lootFactor(Mob mob, float baseChance) {
		Hero hero = Dungeon.hero;
		if (!(lootKill instanceof DreamPuppet) || hero == null || hero.pointsInTalent(Talent.SLOTH_RESTFUL) <= 0) return 1f;
		if (baseChance >= 1f) return 1f;
		if (mob.properties().contains(Char.Property.BOSS) || mob.properties().contains(Char.Property.MINIBOSS)) return 1f;
		return RESTFUL_LOOT;
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
