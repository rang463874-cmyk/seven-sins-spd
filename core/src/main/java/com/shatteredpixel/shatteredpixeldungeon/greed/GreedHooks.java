package com.shatteredpixel.shatteredpixeldungeon.greed;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GreedVault;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.watabou.utils.Random;

/**
 * Game hooks for the Greed class. Every method is a cheap pass-through
 * for heroes without a GreedVault (and for non-Greed mechanics like debt checks).
 */
public final class GreedHooks {

	private GreedHooks() {}

	private static GreedVault vault(Hero hero) {
		return hero.buff(GreedVault.class);
	}

	// ------------------------------------------------------------------ stats

	public static float accuracy(Hero hero, float mult) {
		GreedVault v = vault(hero);
		return v == null ? mult : mult * (1f + v.accuracyBonus());
	}

	public static float evasion(Hero hero, float mult) {
		GreedVault v = vault(hero);
		return v == null ? mult : mult * (1f + v.evasionBonus());
	}

	public static int drBonus(Hero hero, int dr) {
		GreedVault v = vault(hero);
		return v == null ? dr : dr + v.drRoll() + com.shatteredpixel.shatteredpixeldungeon.sins.AwakeningHooks.greedDR(hero);
	}

	public static int bonusHT(Hero hero, int ht) {
		GreedVault v = vault(hero);
		return v == null ? ht : ht + v.hpBonus();
	}

	// ------------------------------------------------------------------ combat

	/** Called from Hero.attackProc for every attack. */
	public static int onAttack(Hero hero, Char enemy, int dmg) {
		GreedVault v = vault(hero);
		if (v == null) return dmg;

		KindOfWeapon wep = hero.belongings.attackingWeapon();
		boolean melee = !(wep instanceof MissileWeapon);
		dmg += v.meleeBonus();
		if (melee) dmg += com.shatteredpixel.shatteredpixeldungeon.sins.AwakeningHooks.greedMelee(hero); //Seven Sins: awakening

		if (enemy != null) {
			GreedVault.Debt debt = enemy.buff(GreedVault.Debt.class);
			if (debt != null) dmg += debt.bonusDamage(hero);
		}

		int midas = hero.pointsInTalent(Talent.GREED_MIDAS);
		if (melee && midas > 0 && Random.Float() < 0.05f * midas) {
			v.earnGold(hero, 5 + Dungeon.depth);
		}
		return dmg;
	}

	/** Called from Hero.defenseProc before the armor's glyph. */
	public static int onDefense(Hero hero, Char attacker, int dmg) {
		GreedVault v = vault(hero);
		if (v == null || dmg <= 0) return dmg;

		int lien = hero.pointsInTalent(Talent.USURER_LIEN);
		if (lien > 0 && attacker != null && attacker.buff(GreedVault.Debt.class) != null) {
			dmg = Math.round(dmg * (1f - 0.10f * lien));
		}

		int comp = hero.pointsInTalent(Talent.GREED_COMPENSATION);
		if (comp > 0 && dmg > 0) {
			v.addCharge(dmg * 0.5f * comp);
		}
		return dmg;
	}

	// ------------------------------------------------------------------ gold & kills

	/** true while an item is being sold, so sale proceeds don't fill the payment gauge */
	public static boolean selling = false;

	/** Called from Gold.doPickUp after the gold is added to the wallet. */
	public static void onGoldPickup(Hero hero, int quantity) {
		GreedVault v = vault(hero);
		if (v == null) return;
		if (selling) v.goldChanged();
		else {
			v.onGoldPickup(hero, quantity);
			com.shatteredpixel.shatteredpixeldungeon.sins.AwakeningHooks.onGoldPickup(hero, quantity); //Seven Sins: awakening
		}
	}

	/** Extra gold granted when picking up a gold pile (Penny Pincher talent). */
	public static int bonusGold(Hero hero, int quantity) {
		int penny = hero.pointsInTalent(Talent.GREED_PENNY);
		return penny == 0 ? 0 : Math.round(quantity * 0.10f * penny);
	}

	/** Called from Mob.die for every mob death. */
	public static void onMobDeath(Mob mob, Object cause) {
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive()) return;
		GreedVault v = vault(hero);
		if (v == null) return;

		GreedVault.Debt.collect(hero, mob);
		com.shatteredpixel.shatteredpixeldungeon.sins.AwakeningHooks.greedPlunder(mob); //Seven Sins: awakening

		int loot = hero.pointsInTalent(Talent.GREED_LOOT);
		if (loot > 0 && mob.alignment == Char.Alignment.ENEMY) {
			v.earnGold(hero, (2 + Dungeon.depth) * loot);
		}
	}
}
