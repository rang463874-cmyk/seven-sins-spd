package com.shatteredpixel.shatteredpixeldungeon.wrath;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WrathGrudge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

/**
 * Game hooks for the Wrath class. Enemy-on-enemy damage and kills feed the hero's grudge.
 * Every method is a cheap pass-through when the hero has no WrathGrudge.
 */
public final class WrathHooks {

	private WrathHooks() {}

	private static WrathGrudge grudge(Hero hero) {
		return hero == null ? null : hero.buff(WrathGrudge.class);
	}

	/** an enemy (not an ally, not the hero) damaging another enemy */
	private static boolean infighting(Char victim, Object src) {
		if (!(src instanceof Mob) || src == victim) return false;
		Mob attacker = (Mob) src;
		return attacker.alignment == Char.Alignment.ENEMY
				&& victim != Dungeon.hero
				&& victim.alignment == Char.Alignment.ENEMY;
	}

	// ------------------------------------------------------------------ infighting

	/** Called from Char.damage before damage is applied. */
	public static int modifyDamage(Char victim, Object src, int dmg) {
		if (dmg <= 0 || !infighting(victim, src)) return dmg;
		Hero hero = Dungeon.hero;
		if (grudge(hero) == null) return dmg;
		Mob attacker = (Mob) src;

		WrathGrudge.FirstBlood fb = attacker.buff(WrathGrudge.FirstBlood.class);
		if (fb != null) {
			dmg = Math.round(dmg * (1f + 0.30f * hero.pointsInTalent(Talent.WRATH_FIRST_BLOOD)));
			fb.detach();
		}
		if (hero.subClass == HeroSubClass.INSTIGATOR && WrathGrudge.enraged(attacker)) {
			dmg = Math.round(dmg * (1.20f + 0.15f * hero.pointsInTalent(Talent.INSTIGATOR_FERVOR)));
		}
		return dmg;
	}

	/** Called from Char.damage after HP is reduced. */
	public static void afterDamage(Char victim, Object src, int dmg) {
		if (dmg <= 0 || !infighting(victim, src)) return;
		Hero hero = Dungeon.hero;
		WrathGrudge g = grudge(hero);
		if (g == null || !hero.isAlive()) return;
		Mob attacker = (Mob) src;

		g.addGrudge(dmg * 0.10f);

		//spreading rage
		if (victim.isAlive() && !WrathGrudge.immune(victim) && victim.buff(Amok.class) == null) {
			boolean spread = attacker.buff(WrathGrudge.Contagion.class) != null;
			int plague = hero.pointsInTalent(Talent.INSTIGATOR_PLAGUE);
			if (!spread && plague > 0 && WrathGrudge.enraged(attacker)) {
				spread = Random.Float() < 0.15f * plague;
			}
			if (spread) {
				Buff.prolong(victim, Amok.class, WrathGrudge.rageDuration(hero, 5f));
				if (victim instanceof Mob) ((Mob) victim).aggro(attacker);
			}
		}

		//the avenger feeds on watching enemies fight
		if (hero.subClass == HeroSubClass.AVENGER && Dungeon.level.heroFOV[victim.pos]) {
			float heal = dmg * (0.10f + 0.05f * hero.pointsInTalent(Talent.AVENGER_FEAST));
			int amt = Math.min(hero.HT - hero.HP, Math.round(heal));
			if (amt > 0) {
				hero.HP += amt;
				hero.sprite.emitter().burst(Speck.factory(Speck.HEALING), 1);
			}
			int emp = hero.pointsInTalent(Talent.AVENGER_EMPOWER);
			if (emp > 0) Buff.affect(hero, WrathGrudge.Vengeance.class).add(3 * emp);
		}
	}

	/** Called from Mob.die. */
	public static void onMobDeath(Mob mob, Object cause) {
		if (!infighting(mob, cause)) return;
		Hero hero = Dungeon.hero;
		WrathGrudge g = grudge(hero);
		if (g == null || !hero.isAlive()) return;

		g.addGrudge(1f + mob.HT / 20f);

		int scav = hero.pointsInTalent(Talent.WRATH_SCAVENGE);
		if (scav > 0) g.gainCharge(0.25f * scav);

		int riot = hero.pointsInTalent(Talent.INSTIGATOR_RIOT);
		if (hero.subClass == HeroSubClass.INSTIGATOR && riot > 0) {
			for (Char ch : Actor.chars()) {
				if (ch instanceof Mob && ch != mob && ch.alignment == Char.Alignment.ENEMY
						&& Dungeon.level.distance(mob.pos, ch.pos) <= 2 && !WrathGrudge.immune(ch)) {
					Buff.prolong(ch, Amok.class, 2f * riot);
				}
			}
		}
	}

	/** Called from Mob.chooseEnemy: rivals set by Discord only hunt each other. */
	public static Char discordTarget(Mob mob) {
		WrathGrudge.Discord d = mob.buff(WrathGrudge.Discord.class);
		if (d == null) return null;
		Actor a = Actor.findById(d.object);
		if (a instanceof Char && ((Char) a).isAlive()) return (Char) a;
		d.detach();
		return null;
	}

	// ------------------------------------------------------------------ hero stats

	public static float accuracy(Hero hero, float mult) {
		WrathGrudge g = grudge(hero);
		return g == null ? mult : mult * (1f + g.accuracyBonus());
	}

	public static float evasion(Hero hero, float mult) {
		WrathGrudge g = grudge(hero);
		return g == null ? mult : mult * (1f + g.evasionBonus());
	}

	public static int drBonus(Hero hero, int dr) {
		WrathGrudge g = grudge(hero);
		return g == null ? dr : dr + g.drRoll();
	}

	public static int bonusHT(Hero hero, int ht) {
		WrathGrudge g = grudge(hero);
		return g == null ? ht : ht + g.hpBonus();
	}

	/** Called from Hero.attackProc for every attack. */
	public static int onAttack(Hero hero, Char enemy, int dmg) {
		WrathGrudge g = grudge(hero);
		if (g == null) return dmg;
		g.onHit();
		KindOfWeapon wep = hero.belongings.attackingWeapon();
		dmg += g.meleeBonus();
		if (!(wep instanceof MissileWeapon)) {
			WrathGrudge.Vengeance v = hero.buff(WrathGrudge.Vengeance.class);
			if (v != null) dmg += v.stacks;
			int exec = hero.pointsInTalent(Talent.AVENGER_EXECUTE);
			if (exec > 0 && enemy != null && WrathGrudge.enraged(enemy)) {
				dmg = Math.round(dmg * (1f + 0.10f * exec));
			}
		}
		return dmg;
	}

	/** Called from Hero.defenseProc. */
	public static int onDefense(Hero hero, Char attacker, int dmg) {
		WrathGrudge g = grudge(hero);
		if (g == null || dmg <= 0) return dmg;
		g.onHurt(dmg);
		int fuel = hero.pointsInTalent(Talent.WRATH_FUEL);
		if (fuel > 0) g.gainCharge(0.10f * fuel);
		return dmg;
	}
}
