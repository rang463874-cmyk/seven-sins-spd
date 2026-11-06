package com.shatteredpixel.shatteredpixeldungeon.items.remains;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;

/** Gluttony's remains: chewing the old fang fills the belly a little and mends some HP. */
public class MawFang extends RemainsItem {

	{
		image = ItemSpriteSheet.REMAINS_GLUTTONY;
	}

	@Override
	protected void doEffect(Hero hero) {
		Hunger hunger = hero.buff(Hunger.class);
		if (hunger != null) hunger.satisfy(150);
		int heal = Math.min(hero.HT - hero.HP, Math.round(hero.HT / 20f));
		if (heal > 0) {
			hero.HP += heal;
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
		}
		Sample.INSTANCE.play(Assets.Sounds.EAT);
	}
}
