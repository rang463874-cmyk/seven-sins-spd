package com.shatteredpixel.shatteredpixeldungeon.items.bags;

import com.shatteredpixel.shatteredpixeldungeon.items.EssenceItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * Seven Sins: holds essences (anything implementing {@link EssenceItem}: sin essences now, the
 * essences of Solomon's 72 demons later). Every class starts with one (HeroClass.initHero).
 */
public class EssenceBag extends Bag {

	{
		image = ItemSpriteSheet.ESSENCE_BAG;
	}

	@Override
	public boolean canHold(Item item) {
		return item instanceof EssenceItem && super.canHold(item);
	}

	@Override
	public int capacity() {
		return 24;
	}

	@Override
	public int value() {
		return 40;
	}
}
