package com.shatteredpixel.shatteredpixeldungeon.lust;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LustDesire;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;

/**
 * Game hooks for the Lust class. Every method is a cheap pass-through
 * when the hero has no LustDesire.
 */
public final class LustHooks {

	private LustHooks() {}

	private static LustDesire desire(Hero hero) {
		return hero == null ? null : hero.buff(LustDesire.class);
	}

	private static boolean sirenCharmed(Object ch) {
		Hero hero = Dungeon.hero;
		return ch instanceof Mob && hero != null && hero.subClass == HeroSubClass.SIREN
				&& desire(hero) != null && LustDesire.charmedByHero((Char) ch);
	}

	// ------------------------------------------------------------------ siren

	/** Called from Mob.chooseEnemy: the Siren's charmed enemies turn on their own kind. */
	public static Char sirenTarget(Mob mob) {
		if (!sirenCharmed(mob)) return null;
		Char best = null;
		for (Char ch : Actor.chars()) {
			if (ch == mob || ch == Dungeon.hero || ch.alignment != Char.Alignment.ENEMY) continue;
			if (LustDesire.charmedByHero(ch)) continue;
			boolean sees = mob.fieldOfView != null && ch.pos < mob.fieldOfView.length
					? mob.fieldOfView[ch.pos]
					: Dungeon.level.distance(mob.pos, ch.pos) <= 8;
			if (!sees) continue;
			if (best == null || Dungeon.level.distance(mob.pos, ch.pos) < Dungeon.level.distance(mob.pos, best.pos)) {
				best = ch;
			}
		}
		return best;
	}

	/** Called from Char.damage before damage is applied. */
	public static int modifyDamage(Char victim, Object src, int dmg) {
		if (dmg <= 0 || !sirenCharmed(src) || victim == Dungeon.hero) return dmg;
		return Math.round(dmg * (1f + 0.15f * Dungeon.hero.pointsInTalent(Talent.SIREN_SONG)));
	}

	/** Called from Char.damage after HP is reduced. */
	public static void afterDamage(Char victim, Object src, int dmg) {
		if (dmg <= 0 || !sirenCharmed(src) || victim == Dungeon.hero || victim.alignment != Char.Alignment.ENEMY) return;
		Hero hero = Dungeon.hero;
		LustDesire d = desire(hero);
		d.addDesire(dmg * 0.10f * (1f + 0.5f * hero.pointsInTalent(Talent.SIREN_DEVOTION)));
		int echo = hero.pointsInTalent(Talent.SIREN_ECHO);
		if (echo > 0) {
			Charm c = ((Char) src).buff(Charm.class);
			if (c != null) Buff.prolong((Char) src, Charm.class, c.cooldown() + echo);
		}
	}

	// ------------------------------------------------------------------ kills

	/** Called from Mob.die. Defeating an enemy that is charmed by the hero feeds desire. */
	public static void onMobDeath(Mob mob, Object cause) {
		Hero hero = Dungeon.hero;
		LustDesire d = desire(hero);
		if (d == null || !hero.isAlive()) return;
		if (mob.alignment != Char.Alignment.ENEMY || !LustDesire.charmedByHero(mob)) return;

		d.addDesire(1f + mob.HT / 15f);
		int hs = hero.pointsInTalent(Talent.LUST_HEARTSTRINGS);
		if (hs > 0) d.gainCharge(0.2f * hs);
	}

	// ------------------------------------------------------------------ hero stats

	public static float accuracy(Hero hero, float mult) {
		LustDesire d = desire(hero);
		return d == null ? mult : mult * (1f + d.accuracyBonus());
	}

	public static float evasion(Hero hero, float mult) {
		LustDesire d = desire(hero);
		return d == null ? mult : mult * (1f + d.evasionBonus());
	}

	public static int drBonus(Hero hero, int dr) {
		LustDesire d = desire(hero);
		return d == null ? dr : dr + d.drRoll();
	}

	public static int bonusHT(Hero hero, int ht) {
		LustDesire d = desire(hero);
		return d == null ? ht : ht + d.hpBonus();
	}

	/** Called from Hero.attackProc for every attack. */
	public static int onAttack(Hero hero, Char enemy, int dmg) {
		LustDesire d = desire(hero);
		if (d == null) return dmg;
		KindOfWeapon wep = hero.belongings.attackingWeapon();
		if (!(wep instanceof MissileWeapon)) dmg += d.meleeBonus();
		int blush = hero.pointsInTalent(Talent.LUST_BLUSH);
		if (blush > 0 && enemy != null && LustDesire.charmedByHero(enemy)) {
			dmg = Math.round(dmg * (1f + 0.10f * blush));
		}
		return dmg;
	}
}
