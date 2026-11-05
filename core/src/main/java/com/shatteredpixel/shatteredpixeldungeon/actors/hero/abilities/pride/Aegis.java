package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.pride;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PrideGlory;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;

/** Untouchable: the next few blows are turned aside without harming the hero's dignity. */
public class Aegis extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	public static int blocks(Hero hero) {
		return 3 + hero.pointsInTalent(Talent.AEGIS_COUNT);
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		PrideGlory.Aegis a = Buff.affect(hero, PrideGlory.Aegis.class);
		a.blocks = Math.max(a.blocks, blocks(hero));
		hero.sprite.operate(hero.pos);
		hero.sprite.centerEmitter().burst(Speck.factory(Speck.FORGE), 8);
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.AEGIS;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.AEGIS_COUNT, Talent.AEGIS_RIPOSTE, Talent.AEGIS_DIGNITY, Talent.HEROIC_ENERGY};
	}
}
