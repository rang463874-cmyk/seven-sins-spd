package com.shatteredpixel.shatteredpixeldungeon.predator;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PredatorStomach;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;

/**
 * Combat stat hooks for the Gluttony class (the Predator), called from Hero.
 * Every method is a cheap pass-through when the hero has no PredatorStomach.
 */
public final class PredatorHooks {

	private PredatorHooks() {}

	private static PredatorStomach stomach(Hero hero) {
		return hero.buff(PredatorStomach.class);
	}

	/** Melee damage bonus, called from Hero.attackProc for non-thrown attacks. */
	public static int onMeleeAttack(Hero hero, Char enemy, int dmg) {
		PredatorStomach st = stomach(hero);
		if (st != null) {
			dmg += st.meleeBonus();
		}
		PredatorStomach.Fang fang = hero.buff(PredatorStomach.Fang.class);
		if (fang != null) {
			dmg = Math.round(dmg * (1f + 0.15f * hero.pointsInTalent(Talent.PREDATOR_FANG)));
			if (--fang.hits <= 0) fang.detach();
		}
		int instinct = hero.pointsInTalent(Talent.PREDATOR_INSTINCT);
		if (instinct > 0 && enemy != null && enemy.HP * 2 <= enemy.HT) {
			dmg = Math.round(dmg * (1f + 0.10f * instinct));
		}
		if (st != null && hero.subClass == HeroSubClass.INHERITOR && enemy != null) {
			dmg = st.procEnchants(hero, enemy, dmg);
		}
		return dmg;
	}

	/** Damage taken, called from Hero.defenseProc before the armor's glyph. */
	public static int onDefense(Hero hero, Char attacker, int dmg) {
		if (dmg <= 0) return dmg;
		PredatorStomach.Coma coma = hero.buff(PredatorStomach.Coma.class);
		if (coma != null) {
			int pct = PredatorStomach.Coma.comaPercent(hero.pointsInTalent(Talent.GOURMAND_FOOD_COMA));
			dmg = Math.round(dmg * (100 - pct) / 100f);
			coma.detach();
		}
		int thorns = hero.pointsInTalent(Talent.GUT_THORNS);
		if (thorns > 0 && attacker != null && attacker.isAlive()) {
			Barrier b = hero.buff(Barrier.class);
			if (b != null && b.shielding() > 0 && Dungeon.level.adjacent(hero.pos, attacker.pos)) {
				int reflect = Math.max(1, Math.round(dmg * 0.10f * thorns));
				attacker.damage(reflect, hero);
			}
		}
		PredatorStomach st = stomach(hero);
		if (st != null && hero.subClass == HeroSubClass.INHERITOR) {
			dmg = st.procGlyphs(hero, attacker, dmg);
		}
		return dmg;
	}

	public static float accuracy(Hero hero, float mult) {
		PredatorStomach st = stomach(hero);
		return st == null ? mult : mult * (1f + st.accuracyBonus());
	}

	public static float evasion(Hero hero, float mult) {
		PredatorStomach st = stomach(hero);
		return st == null ? mult : mult * (1f + st.evasionBonus());
	}

	public static int drBonus(Hero hero, int dr) {
		PredatorStomach st = stomach(hero);
		return st == null ? dr : dr + st.drRoll();
	}

	public static int bonusHT(Hero hero, int ht) {
		PredatorStomach st = stomach(hero);
		return st == null ? ht : ht + st.hpBonus();
	}
}
