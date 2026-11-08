package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * Dropped by elite monsters (one per sin elite, two per region elite). An alchemy ingredient for the
 * elixir of sin and the bomb of seven sins.
 */
public class SinEssence extends Item implements EssenceItem {

	{
		image = ItemSpriteSheet.SIN_ESSENCE;
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
	public int value() {
		return 50 * quantity;
	}

	@Override
	public int energyVal() {
		return 3 * quantity;
	}
}
