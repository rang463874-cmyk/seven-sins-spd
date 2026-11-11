package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * Seven Sins: the essence of one of Solomon's demons (one kind for all of them). Dropped by the demons;
 * offered at the hidden sigil of a boss floor to call that floor's demon king. Kept in the essence bag.
 */
public class DemonEssence extends Item implements EssenceItem {

	{
		image = ItemSpriteSheet.DEMON_ESSENCE;
		stackable = true;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return new ItemSprite.Glowing(0x9020C0, 1.5f);
	}

	@Override
	public int value() {
		return 60 * quantity;
	}
}
