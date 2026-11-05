package com.shatteredpixel.shatteredpixeldungeon.sloth;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SlothRepose;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.SlothPillow;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;

/**
 * Game hooks for the Sloth class. Every method is a cheap pass-through
 * when the hero has no SlothRepose.
 */
public final class SlothHooks {

	private SlothHooks() {}

	public static final int MIN_TORPOR = 5;

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

	/** Called from Hero.attackProc for every attack. */
	public static int onAttack(Hero hero, Char enemy, int dmg) {
		SlothRepose r = repose(hero);
		if (r == null) return dmg;
		KindOfWeapon wep = hero.belongings.attackingWeapon();
		if (wep instanceof MissileWeapon) return dmg;

		dmg += r.meleeBonus();

		//the sleeping lion pours all of its stored laziness into one blow
		if (hero.subClass == HeroSubClass.SLEEPING_LION && r.torpor >= MIN_TORPOR) {
			float per = 0.05f * (1f + 0.2f * hero.pointsInTalent(Talent.LION_POUNCE));
			dmg = Math.round(dmg * (1f + per * r.torpor));
			r.pounceTorpor = r.torpor;
			r.pounceTime = Actor.now();
			r.torpor = Math.round(r.torpor * 0.2f * hero.pointsInTalent(Talent.LION_WAKE));
			hero.sprite.showStatus(CharSprite.WARNING, Messages.get(SlothRepose.class, "pounce"));
		}
		return dmg;
	}

	/** Called from Mob.die. */
	public static void onMobDeath(Mob mob, Object cause) {
		Hero hero = Dungeon.hero;
		SlothRepose r = repose(hero);
		if (r == null || !hero.isAlive() || mob.alignment != Char.Alignment.ENEMY || !heroCaused(cause)) return;

		int t = r.creditedTorpor();
		if (t < MIN_TORPOR) return;
		r.addSloth((1f + mob.HT / 15f) * Math.min(2f, t / 10f));

		int roar = hero.pointsInTalent(Talent.LION_ROAR);
		if (hero.subClass == HeroSubClass.SLEEPING_LION && roar > 0 && r.pounceTorpor > 0) {
			for (Char ch : Actor.chars()) {
				if (ch instanceof Mob && ch != mob && ch.alignment == Char.Alignment.ENEMY
						&& Dungeon.level.distance(mob.pos, ch.pos) <= 3) {
					Buff.prolong(ch, Terror.class, 2f * roar).object = hero.id();
				}
			}
		}
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
