package com.shatteredpixel.shatteredpixeldungeon.items.remains;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SolomonArcana;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;

/** Solomon's remains (Nov 18 2026): the last spark of the broken seal refills mana and recharges wands for a while. */
public class BrokenSignet extends RemainsItem {

	{
		image = ItemSpriteSheet.REMAINS_SOLOMON;
	}

	@Override
	protected void doEffect(Hero hero) {
		SolomonArcana a = hero.buff(SolomonArcana.class);
		if (a != null) a.addMana(a.maxMana());
		Buff.affect(hero, Recharging.class, 10f);
		hero.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 8);
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
	}
}
