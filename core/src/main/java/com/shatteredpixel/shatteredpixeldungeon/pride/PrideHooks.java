package com.shatteredpixel.shatteredpixeldungeon.pride;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PrideGlory;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Random;

/**
 * Game hooks for the Pride class. Every method is a cheap pass-through
 * when the hero has no PrideGlory.
 */
public final class PrideHooks {

	private PrideHooks() {}

	/** pride is a ranged class: its melee attacks deal 30% less */
	public static final float MELEE_PENALTY = 0.7f;

	private static PrideGlory glory(Hero hero) {
		return hero == null ? null : hero.buff(PrideGlory.class);
	}

	/** true when the hero has no allies in sight (for the Paragon) */
	public static boolean alone(Hero hero) {
		for (Mob m : Dungeon.level.mobs) {
			if (m.alignment == Char.Alignment.ALLY && Dungeon.level.heroFOV[m.pos]
					&& !(m instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.PrideEffigy)) return false;
		}
		return true;
	}

	private static boolean heroCaused(Object cause) {
		return cause == Dungeon.hero
				|| cause instanceof Weapon
				|| cause instanceof Weapon.Enchantment
				|| cause instanceof Wand
				|| cause instanceof ArmorAbility
				|| cause == com.shatteredpixel.shatteredpixeldungeon.items.PrideCrown.class;
	}

	// ------------------------------------------------------------------ damage

	/** Called from Char.damage before damage is applied. */
	public static int modifyDamage(Char victim, Object src, int dmg) {
		if (dmg <= 0) return dmg;
		Hero hero = Dungeon.hero;
		PrideGlory g = glory(hero);
		if (g == null) return dmg;

		//vassals fight harder for their monarch
		if (src instanceof Char && ((Char) src).buff(PrideGlory.Vassal.class) != null) {
			dmg = Math.round(dmg * (1f + 0.15f * hero.pointsInTalent(Talent.MONARCH_LOYALTY)));
		}

		if (victim != hero || !(src instanceof Char)) return dmg;
		Char attacker = (Char) src;

		PrideGlory.Decree decree = attacker.buff(PrideGlory.Decree.class);
		if (decree != null) {
			if (--decree.uses <= 0) decree.detach();
			hero.sprite.showStatus(CharSprite.NEUTRAL, Messages.get(PrideGlory.Decree.class, "nullified"));
			return 0;
		}

		PrideGlory.Duel duel = hero.buff(PrideGlory.Duel.class);
		if (duel != null && attacker.id() != duel.object) {
			hero.sprite.showStatus(CharSprite.NEUTRAL, Messages.get(PrideGlory.Duel.class, "interfere"));
			return 0;
		}

		PrideGlory.Aegis aegis = hero.buff(PrideGlory.Aegis.class);
		if (aegis != null) {
			if (--aegis.blocks <= 0) aegis.detach();
			hero.sprite.showStatus(CharSprite.NEUTRAL, Messages.get(PrideGlory.Aegis.class, "blocked"));
			int riposte = hero.pointsInTalent(Talent.AEGIS_RIPOSTE);
			if (riposte > 0 && attacker.isAlive() && Dungeon.level.adjacent(hero.pos, attacker.pos)) {
				attacker.damage(Math.round(hero.damageRoll() * 0.25f * riposte), hero);
			}
			int dig = hero.pointsInTalent(Talent.AEGIS_DIGNITY);
			if (dig > 0) g.addDignity(dig);
			return 0;
		}
		return dmg;
	}

	/** Called from Char.damage after HP is reduced. */
	public static void afterDamage(Char victim, Object src, int dmg) {
		if (dmg <= 0) return;
		Hero hero = Dungeon.hero;
		if (victim != hero) return;
		PrideGlory g = glory(hero);
		if (g == null) return;
		if (src instanceof Char && src != hero) {
			Buff.affect((Char) src, PrideGlory.Insolent.class);
		}
		if (!(src instanceof Hunger)) g.shatter();
	}

	/** Called from Mob.die. */
	public static void onMobDeath(Mob mob, Object cause) {
		Hero hero = Dungeon.hero;
		PrideGlory g = glory(hero);
		if (g == null || !hero.isAlive()) return;
		if (mob.alignment != Char.Alignment.ENEMY || mob.buff(PrideGlory.Insolent.class) != null) return;

		float mult;
		if (heroCaused(cause)) mult = 1f;
		else if (cause instanceof Char && ((Char) cause).buff(PrideGlory.Vassal.class) != null) mult = 0.5f;
		else return;

		g.addGlory(PrideGlory.gloryFor(mob) * mult);
		hero.sprite.showStatus(CharSprite.POSITIVE, Messages.get(PrideGlory.class, "flawless"));

		int sup = hero.pointsInTalent(Talent.PRIDE_SUPREMACY);
		if (sup > 0) {
			int heal = Math.min(hero.HT - hero.HP, Math.round(hero.HT * 0.03f * sup));
			if (heal > 0) {
				hero.HP += heal;
				hero.sprite.emitter().burst(Speck.factory(Speck.HEALING), 2);
			}
		}
		int legend = hero.pointsInTalent(Talent.PARAGON_LEGEND);
		if (hero.subClass == HeroSubClass.PARAGON && legend > 0) g.addDignity(legend);
	}

	// ------------------------------------------------------------------ hero stats

	public static float accuracy(Hero hero, float mult) {
		PrideGlory g = glory(hero);
		return g == null ? mult : mult * (1f + g.accuracyBonus());
	}

	public static float evasion(Hero hero, float mult) {
		PrideGlory g = glory(hero);
		if (g == null) return mult;
		mult *= 1f + g.evasionBonus();
		if (hero.subClass == HeroSubClass.PARAGON && alone(hero)) mult *= 1.2f;
		return mult;
	}

	public static int drBonus(Hero hero, int dr) {
		PrideGlory g = glory(hero);
		return g == null ? dr : dr + g.drRoll();
	}

	public static int bonusHT(Hero hero, int ht) {
		PrideGlory g = glory(hero);
		return g == null ? ht : ht + g.hpBonus();
	}

	/** Called from Hero.attackProc for every attack. */
	public static int onAttack(Hero hero, Char enemy, int dmg) {
		PrideGlory g = glory(hero);
		if (g == null) return dmg;
		KindOfWeapon wep = hero.belongings.attackingWeapon();
		float mult = 1f;
		if (wep instanceof MissileWeapon) {
			//pride fights from afar: glory and dignity feed arrows and thrown weapons
			dmg += g.meleeBonus();
			mult *= g.dignityMult();
			if (com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.sins.SinWeapons.wields(hero,
					com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.sins.KingsAegis.class,
					com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.PRIDE)) {
				mult *= 1f + com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.sins.KingsAegis.RANGED_BONUS;
			}
		} else {
			//and is clumsy up close
			mult *= MELEE_PENALTY;
		}
		if (hero.subClass == HeroSubClass.PARAGON) {
			if (alone(hero)) mult *= 1.25f + 0.10f * hero.pointsInTalent(Talent.PARAGON_SOLITUDE);
			if (enemy != null && enemy.HP > hero.HP) mult *= 1.25f + 0.15f * hero.pointsInTalent(Talent.PARAGON_GIANTSLAYER);
		}
		PrideGlory.Duel duel = hero.buff(PrideGlory.Duel.class);
		if (duel != null && enemy != null && enemy.id() == duel.object) mult *= 1.2f;
		mult *= com.shatteredpixel.shatteredpixeldungeon.sins.AwakeningHooks.prideDamage(hero, enemy, wep instanceof MissileWeapon); //Seven Sins: awakening
		return Math.round(dmg * mult);
	}

	/** PRIDE_SWIFT_CROWN ("royal retrieval"): a thrown weapon may fly straight back into the hand.
	 *  Called from MissileWeapon.rangedHit/rangedMiss instead of dropping it; true if it came back. */
	public static boolean swiftReturn(com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon w) {
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive()) return false;
		int pts = hero.pointsInTalent(Talent.PRIDE_SWIFT_CROWN);
		if (pts <= 0 || w instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow.SpiritArrow) return false;
		if (com.watabou.utils.Random.Float() >= SWIFT_RETURN_CHANCE * pts) return false;
		if (!w.collect(hero.belongings.backpack)) return false;
		if (hero.sprite != null) {
			hero.sprite.showStatus(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.POSITIVE,
					com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(PrideHooks.class, "swift_return"));
		}
		com.shatteredpixel.shatteredpixeldungeon.items.Item.updateQuickslot();
		return true;
	}

	public static final float SWIFT_RETURN_CHANCE = 0.25f;
}
