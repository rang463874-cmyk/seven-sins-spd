package com.shatteredpixel.shatteredpixeldungeon.sins;

import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnvyCovet;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GreedVault;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LustDesire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PredatorStomach;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PrideGlory;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SlothRepose;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WrathGrudge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;

/**
 * The mod's own challenges (the masks are in Challenges). The ones with their own state live where
 * that state is (VirtueHooks, GoetiaHooks, EliteHooks); the stateless rules are here.
 */
public final class ChallengeHooks {

	private ChallengeHooks() {}

	// ------------------------------------------------------------------ the price of sin

	/** enemies gain this much damage and toughness once the sin's unique stat is at its cap */
	public static final float PRICE_PER_CAP = 0.5f;
	/** and never more than this */
	public static final float PRICE_MAX = 1.5f;

	/**
	 * How far the hero's sin has grown, measured against the points that cap its unique stat
	 * (1 = at the cap; it keeps rising past that, the points being unbounded).
	 */
	public static float sinProgress(Hero h) {
		if (h == null) return 0;
		PredatorStomach st = h.buff(PredatorStomach.class);
		if (st != null) return UniqueStats.gluttonyPts(st) / 500f;
		GreedVault v = h.buff(GreedVault.class);
		if (v != null) return v.pts() / 50f;
		WrathGrudge w = h.buff(WrathGrudge.class);
		if (w != null) return w.pts() / 125f;
		PrideGlory p = h.buff(PrideGlory.class);
		if (p != null) return p.pts() / 125f;
		LustDesire d = h.buff(LustDesire.class);
		if (d != null) return d.pts() / 100f;
		EnvyCovet e = h.buff(EnvyCovet.class);
		if (e != null) return e.pts() / 150f;
		SlothRepose r = h.buff(SlothRepose.class);
		if (r != null) return r.pts() / 125f;
		return 0;
	}

	/** the enemies' bonus under the price of sin: 0 without the challenge */
	public static float sinPrice() {
		if (!Dungeon.isChallenged(Challenges.SIN_PRICE)) return 0;
		return Math.min(PRICE_MAX, PRICE_PER_CAP * Math.max(0, sinProgress(Dungeon.hero)));
	}

	/** Char.damage: enemies hit harder and take less, the further the hero's sin has grown */
	public static int modifyDamage(Char target, Object src, int dmg) {
		if (dmg <= 0) return dmg;
		float price = sinPrice();
		if (price <= 0) return dmg;
		if (target instanceof Hero) {
			if (src instanceof Mob && ((Mob) src).alignment == Char.Alignment.ENEMY) return Math.round(dmg * (1f + price));
		} else if (target instanceof Mob && target.alignment == Char.Alignment.ENEMY) {
			return Math.max(1, Math.round(dmg / (1f + price)));
		}
		return dmg;
	}

	// ------------------------------------------------------------------ abstinence

	/** multiplier on what the signature item's skills cost (Pride: on how long dignity takes to return) */
	public static float skillCost() {
		return Dungeon.isChallenged(Challenges.ABSTINENCE) ? 2f : 1f;
	}

	// ------------------------------------------------------------------ dried essence

	/** false when sin and demon essences are not handed out */
	public static boolean essences() {
		return !Dungeon.isChallenged(Challenges.NO_ESSENCE);
	}
}
