package com.shatteredpixel.shatteredpixeldungeon.items.remains;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.greed.GreedHooks;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;

/** Greed's remains: the melted gold of an old card, worth 20 gold per floor of depth. */
public class MeltedCard extends RemainsItem {

	{
		image = ItemSpriteSheet.REMAINS_GREED;
	}

	@Override
	protected void doEffect(Hero hero) {
		int gold = 20 * Math.max(1, Dungeon.depth);
		Dungeon.gold += gold;
		GreedHooks.onGoldPickup(hero, gold);
		hero.sprite.showStatusWithIcon(CharSprite.NEUTRAL, Integer.toString(gold), FloatingText.GOLD);
		Sample.INSTANCE.play(Assets.Sounds.GOLD);
	}
}
