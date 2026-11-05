package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.sloth;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SlothRepose;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;

/** Curls up for a well-armored nap: big shield, instantly maxed torpor. */
public class FortifiedNap extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		SlothRepose r = Buff.affect(hero, SlothRepose.class);
		r.torpor = r.maxTorpor();
		r.lastPos = hero.pos;

		int shield = Math.round((10 + hero.lvl + r.torpor) * (1f + 0.25f * hero.pointsInTalent(Talent.NAP_SHIELD)));
		Buff.affect(hero, Barrier.class).setShield(shield);
		hero.sprite.showStatus(CharSprite.POSITIVE, Integer.toString(shield));

		int heal = hero.pointsInTalent(Talent.NAP_HEAL);
		if (heal > 0) hero.HP = Math.min(hero.HT, hero.HP + Math.round(hero.HT * 0.10f * heal));
		int dreams = hero.pointsInTalent(Talent.NAP_DREAMS);
		if (dreams > 0) r.gainCharge(0.5f * dreams);

		hero.sprite.operate(hero.pos);
		hero.sprite.centerEmitter().burst(Speck.factory(Speck.NOTE), 5);
		Sample.INSTANCE.play(Assets.Sounds.LULLABY, 0.8f);
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.FORTIFIED_NAP;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.NAP_SHIELD, Talent.NAP_HEAL, Talent.NAP_DREAMS, Talent.HEROIC_ENERGY};
	}
}
