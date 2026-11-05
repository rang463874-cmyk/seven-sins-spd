package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.predator;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PredatorStomach;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.audio.Sample;

/** Harden the stomach lining: gain a large shield scaled by armor points. */
public class IronGut extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	public static int shieldAmount(Hero hero) {
		PredatorStomach st = hero.buff(PredatorStomach.class);
		float a = st == null ? 0 : st.armorPts;
		return Math.round((10 + hero.lvl + a) * (1f + 0.15f * hero.pointsInTalent(Talent.GUT_THICKNESS)));
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		int shield = shieldAmount(hero);
		Buff.affect(hero, Barrier.class).setShield(shield);
		hero.sprite.showStatus(CharSprite.POSITIVE, Integer.toString(shield));

		int rec = hero.pointsInTalent(Talent.GUT_RECOVER);
		if (rec > 0) {
			int heal = Math.min(Math.round(hero.HT * 0.05f * rec), hero.HT - hero.HP);
			if (heal > 0) hero.HP += heal;
		}

		hero.sprite.operate(hero.pos);
		hero.sprite.centerEmitter().burst(Speck.factory(Speck.FORGE), 8);
		Sample.INSTANCE.play("sounds/chargeup.mp3");
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.IRON_GUT;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.GUT_THICKNESS, Talent.GUT_THORNS, Talent.GUT_RECOVER, Talent.HEROIC_ENERGY};
	}
}
