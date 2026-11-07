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
		if (wep instanceof MissileWeapon) return dmg;
		return dmg + r.meleeBonus();
	}

	/** Called from Hero.defenseProc when an enemy hits the hero. */
	public static int onDefense(Hero hero, Char attacker, int dmg) {
		SlothRepose r = repose(hero);
		if (r == null || dmg <= 0) return dmg;

		//pillow fort: dozing after a skill softens blows
		int fort = hero.pointsInTalent(Talent.SLOTH_PILLOW);
		if (fort > 0 && hero.buff(SlothRepose.Doze.class) != null) {
			dmg = Math.round(dmg * (1f - 0.2f * fort));
		}

		//comfy: a puppet nearby softens every blow
		int comfy = hero.pointsInTalent(Talent.SLOTH_COMFY);
		if (comfy > 0) {
			DreamPuppet near = DreamPuppet.nearestTo(hero.pos);
			if (near != null && Dungeon.level.distance(hero.pos, near.pos) <= 2) {
				dmg = Math.round(dmg * (1f - 0.10f * comfy));
			}
		}

		//the sleepwalker barely notices hits while asleep
		int deep = hero.pointsInTalent(Talent.WALK_DEEP);
		if (deep > 0 && r.asleep()) {
			dmg = Math.round(dmg * (1f - 0.08f * deep));
		}

		//"not my problem": whoever hits the sloth gets taunted by a puppet
		int shove = hero.pointsInTalent(Talent.SLOTH_SHOVE);
		if (shove > 0 && attacker != null && attacker.isAlive()) {
			DreamPuppet p = DreamPuppet.nearestTo(attacker.pos);
			if (p != null) SlothRepose.Taunted.apply(attacker, p, 2f * shove);
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

		//dream lord: what a puppet defeats rises again as a short-lived nightmare
		if (byPuppet && hero.subClass == HeroSubClass.DREAM_LORD
				&& DreamPuppet.puppets(true).size() < r.maxLordNightmares()) {
			spawnNear(mob.pos, DreamPuppet.create(true, SlothRepose.lordNightmareLife(hero)));
		}
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
		return r == null ? ht : ht + r.hpBonus();
	}
}
