package com.shatteredpixel.shatteredpixeldungeon.sins;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnvyCovet;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GreedVault;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PrideGlory;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SlothRepose;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WrathGrudge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;

/**
 * The sins' first tier-2 talents are meal talents, like the original classes' Iron Stomach and
 * friends: eating takes 1 turn instead of 3 and refills the sin's own resource.
 */
public final class SinMeals {

	private SinMeals() {}

	public static final Talent[] MEAL_TALENTS = {
			Talent.PREDATOR_GLUTTONY, Talent.GREED_HOARD, Talent.WRATH_DRUMMER, Talent.PRIDE_REGALIA,
			Talent.LUST_BOTTLE, Talent.ENVY_MIRROR, Talent.SLOTH_PILLOW
	};

	/** Food.eatingTime: true if eating takes 1 turn */
	public static boolean quickEater(Hero hero) {
		if (hero == null) return false;
		for (Talent t : MEAL_TALENTS) if (hero.hasTalent(t)) return true;
		return false;
	}

	/** Called from Talent.onFoodEaten. */
	public static void onFoodEaten(Hero hero, float foodVal) {
		int p;
		if ((p = hero.pointsInTalent(Talent.PREDATOR_GLUTTONY)) > 0) {
			//a bigger appetite: more satiety from every meal
			Hunger hunger = hero.buff(Hunger.class);
			if (hunger != null) hunger.satisfy(foodVal * 0.2f * p);
		}
		if ((p = hero.pointsInTalent(Talent.GREED_HOARD)) > 0) {
			GreedVault v = hero.buff(GreedVault.class);
			if (v != null) v.addCharge(v.maxCharge() * 0.15f * p);
		}
		if ((p = hero.pointsInTalent(Talent.WRATH_DRUMMER)) > 0) {
			WrathGrudge g = hero.buff(WrathGrudge.class);
			//raw rage, not scaled by rage gain bonuses
			if (g != null) g.rage = Math.min(g.maxRage(), g.rage + 25f * p);
		}
		if ((p = hero.pointsInTalent(Talent.PRIDE_REGALIA)) > 0) {
			PrideGlory g = hero.buff(PrideGlory.class);
			if (g != null) g.addDignity(3 * p);
		}
		if ((p = hero.pointsInTalent(Talent.LUST_BOTTLE)) > 0) {
			int heal = Math.min(hero.HT - hero.HP, Math.round(hero.HT * 0.10f * p));
			if (heal > 0) {
				hero.HP += heal;
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			}
		}
		if ((p = hero.pointsInTalent(Talent.ENVY_MIRROR)) > 0) {
			EnvyCovet c = hero.buff(EnvyCovet.class);
			if (c != null) c.addJealousy(15f * p);
		}
		if ((p = hero.pointsInTalent(Talent.SLOTH_PILLOW)) > 0) {
			Buff.affect(hero, SlothRepose.Refreshed.class).skills = p;
		}
		if (quickEater(hero)) hero.sprite.emitter().burst(Speck.factory(Speck.STAR), 3);
		Item.updateQuickslot();
	}
}
