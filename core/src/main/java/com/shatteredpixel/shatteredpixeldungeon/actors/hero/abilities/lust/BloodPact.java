package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.lust;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LustDesire;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;

/** Offers up a third of max HP for a thick shield and fierce lifesteal on every hit. */
public class BloodPact extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	public static float duration(Hero hero) {
		return 10f + 3f * hero.pointsInTalent(Talent.PACT_DURATION);
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		//the offering can't kill
		int cost = Math.min(hero.HP - 1, Math.round(hero.HT * 0.30f));
		if (cost > 0) {
			hero.HP -= cost;
			hero.sprite.showStatus(CharSprite.NEGATIVE, Integer.toString(cost));
		}
		int shield = Math.round(Math.max(cost, hero.HT * 0.10f) * (1.5f + 0.25f * hero.pointsInTalent(Talent.PACT_SHIELD)));
		Buff.affect(hero, Barrier.class).setShield(shield);
		Buff.prolong(hero, LustDesire.BloodPact.class, duration(hero));

		hero.sprite.operate(hero.pos);
		hero.sprite.emitter().burst(Speck.factory(Speck.HEALING), 6);
		GameScene.flash(0x60AA1020);
		Sample.INSTANCE.play(Assets.Sounds.CURSED);
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.BLOOD_PACT;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.PACT_SHIELD, Talent.PACT_DURATION, Talent.PACT_THIRST, Talent.HEROIC_ENERGY};
	}
}
