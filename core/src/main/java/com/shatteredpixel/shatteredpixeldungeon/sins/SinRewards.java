package com.shatteredpixel.shatteredpixeldungeon.sins;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnvyCovet;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GreedVault;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LustDesire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PredatorStomach;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PrideGlory;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SlothRepose;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WrathGrudge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

/**
 * Generic "sin points" rewards (elite kills, the elixir of sin), routed to whichever sin the hero is.
 * Sin indices follow the usual order: gluttony 0, greed 1, wrath 2, pride 3, lust 4, envy 5, sloth 6.
 */
public class SinRewards {

	public static final int NONE = -1;

	/** the hero's sin index, or NONE for the original classes */
	public static int sinOf(Hero h) {
		if (h == null) return NONE;
		HeroClass c = h.heroClass;
		if (c == HeroClass.GLUTTONY) return 0;
		if (c == HeroClass.GREED) return 1;
		if (c == HeroClass.WRATH) return 2;
		if (c == HeroClass.PRIDE) return 3;
		if (c == HeroClass.LUST) return 4;
		if (c == HeroClass.ENVY) return 5;
		if (c == HeroClass.SLOTH) return 6;
		return NONE;
	}

	/** adds permanent sin points to the hero's own sin; returns false for non-sin heroes */
	public static boolean grant(Hero h, float pts) {
		if (h == null || pts <= 0) return false;
		boolean done = false;
		PredatorStomach st = h.buff(PredatorStomach.class);
		if (st != null) {
			st.addPoints(PredatorStomach.CAT_POTION, pts / 3f);
			st.addPoints(PredatorStomach.CAT_FOOD, pts / 3f);
			st.addPoints(PredatorStomach.CAT_SCROLL, pts / 3f);
			done = true;
		}
		if (h.buff(GreedVault.class) != null) {
			//greed's points are the gold it carries (100 gold per point), paid out at half rate
			Dungeon.gold += Math.round(pts * 50);
			done = true;
		}
		WrathGrudge wg = h.buff(WrathGrudge.class);
		if (wg != null) { wg.addGrudge(pts); done = true; }
		PrideGlory pg = h.buff(PrideGlory.class);
		if (pg != null) { pg.addGlory(pts); done = true; }
		LustDesire ld = h.buff(LustDesire.class);
		if (ld != null) { ld.addDesire(pts); done = true; }
		EnvyCovet ec = h.buff(EnvyCovet.class);
		if (ec != null) { ec.addEnvy(pts); done = true; }
		SlothRepose sr = h.buff(SlothRepose.class);
		if (sr != null) { sr.addSloth(pts); done = true; }
		if (done) {
			h.updateHT(true);
			BuffIndicator.refreshHero();
		}
		return done;
	}

	/** refills the hero's skill resource (rage, dignity, gauge, jealousy...) */
	public static void refill(Hero h) {
		if (h == null) return;
		WrathGrudge wg = h.buff(WrathGrudge.class);
		if (wg != null) wg.rage = wg.maxRage();
		PrideGlory pg = h.buff(PrideGlory.class);
		if (pg != null) pg.dignity = pg.maxDignity();
		GreedVault gv = h.buff(GreedVault.class);
		if (gv != null) gv.addCharge(gv.maxCharge());
		EnvyCovet ec = h.buff(EnvyCovet.class);
		if (ec != null) ec.jealousy = ec.maxJealousy();
		BuffIndicator.refreshHero();
	}
}
