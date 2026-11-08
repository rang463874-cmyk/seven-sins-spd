package com.shatteredpixel.shatteredpixeldungeon.lust;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LustDesire;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;

/**
 * Game hooks for the Lust class. Every method is a cheap pass-through
 * when the hero has no LustDesire.
 */
public final class LustHooks {

	private LustHooks() {}

	/** guards the Night Dancer's counter so a counter can't trigger another counter */
	private static boolean countering = false;

	private static LustDesire desire(Hero hero) {
		return hero == null ? null : hero.buff(LustDesire.class);
	}

	// ------------------------------------------------------------------ combat

	/** Called from Hero.attackProc for every attack: bonus damage and lifesteal. */
	public static int onAttack(Hero hero, Char enemy, int dmg) {
		LustDesire d = desire(hero);
		if (d == null || enemy == null) return dmg;
		KindOfWeapon wep = hero.belongings.attackingWeapon();
		boolean melee = !(wep instanceof MissileWeapon);
		dmg += d.meleeBonus();

		boolean charmed = LustDesire.charmedByHero(enemy);
		int blush = hero.pointsInTalent(Talent.LUST_BLUSH);
		if (blush > 0 && charmed) {
			dmg = Math.round(dmg * (1f + 0.10f * blush));
		}

		//Seven Sins: awakening (the feast drinks from every blow, thrown ones too)
		float rate = melee ? d.lifestealFor(enemy) : 0f;
		rate = com.shatteredpixel.shatteredpixeldungeon.sins.AwakeningHooks.lustSteal(hero, enemy, rate);
		if (rate > 0) d.drain(Math.min(dmg, enemy.HP + enemy.shielding()), rate);
		return dmg;
	}

	/**
	 * Called from Char.attack when an attack misses. When the hero dodges an enemy,
	 * the dance charms it, and the Night Dancer strikes back.
	 */
	public static void onDodge(Char attacker, Char defender) {
		if (!(defender instanceof Hero) || !(attacker instanceof Mob) || !attacker.isAlive()) return;
		Hero hero = (Hero) defender;
		LustDesire d = desire(hero);
		if (d == null || !hero.isAlive()) return;

		Mob mob = (Mob) attacker;
		if (!LustDesire.charmImmune(mob)) {
			LustDesire.charm(hero, mob, 2f + hero.pointsInTalent(Talent.LUST_HEARTSTRINGS));
			if (mob.sprite != null) mob.sprite.emitter().burst(Speck.factory(Speck.HEART), 2);
		}

		if (hero.subClass != HeroSubClass.NIGHT_DANCER) return;

		int tempo = hero.pointsInTalent(Talent.DANCER_TEMPO);
		if (tempo > 0 && hero.HP < hero.HT) {
			hero.HP = Math.min(hero.HT, hero.HP + 2 * tempo + hero.lvl / 10);
		}

		if (!countering && hero.canAttack(mob)) {
			countering = true;
			try {
				float mult = 0.75f + 0.15f * hero.pointsInTalent(Talent.DANCER_COUNTER);
				hero.sprite.turnTo(hero.pos, mob.pos);
				hero.attack(mob, mult, 0f, 1f);
			} finally {
				countering = false;
			}
		}
	}

	/** Called from Mob.die. Desire now comes from stolen life, not from kills. */
	public static void onMobDeath(Mob mob, Object cause) {
		//no-op, kept so the call site stays stable
	}

	// ------------------------------------------------------------------ hero stats

	public static float accuracy(Hero hero, float mult) {
		LustDesire d = desire(hero);
		return d == null ? mult : mult * (1f + d.accuracyBonus());
	}

	public static float evasion(Hero hero, float mult) {
		LustDesire d = desire(hero);
		if (d == null) return mult;
		mult *= 1f + d.evasionBonus();
		if (hero.subClass == HeroSubClass.NIGHT_DANCER) {
			mult *= 1f + 0.04f * hero.pointsInTalent(Talent.DANCER_GRACE);
		}
		if (hero.buff(LustDesire.Sway.class) != null) mult *= LustDesire.Sway.EVASION_MULT;
		mult *= com.shatteredpixel.shatteredpixeldungeon.sins.AwakeningHooks.lustEvasion(hero); //Seven Sins: awakening
		return mult;
	}

	public static int drBonus(Hero hero, int dr) {
		LustDesire d = desire(hero);
		return d == null ? dr : dr + d.drRoll();
	}

	public static int bonusHT(Hero hero, int ht) {
		LustDesire d = desire(hero);
		return d == null ? ht : ht + d.hpBonus();
	}

	/** true when nothing else should know about the hero's counterattack in progress */
	public static boolean countering() {
		return countering;
	}
}
