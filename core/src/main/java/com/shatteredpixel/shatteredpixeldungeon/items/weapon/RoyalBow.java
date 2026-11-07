package com.shatteredpixel.shatteredpixeldungeon.items.weapon;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfSharpshooting;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * Pride's royal longbow: the spirit bow's rules (endless arrows, grows with the hero's level, can be
 * enchanted and augmented), but it also takes scrolls of upgrade (+1/+2 damage per upgrade).
 * Pride's glory and dignity bonuses apply to its arrows (see PrideHooks.onAttack).
 */
public class RoyalBow extends SpiritBow {

	{
		image = ItemSpriteSheet.ROYAL_BOW;
	}

	@Override
	public int min(int lvl) {
		int hero = Dungeon.hero == null ? 1 : Dungeon.hero.lvl;
		int dmg = 1 + hero / 5 + trueLevel()
				+ RingOfSharpshooting.levelDamageBonus(Dungeon.hero)
				+ (curseInfusionBonus ? 1 + hero / 30 : 0);
		return Math.max(0, dmg);
	}

	@Override
	public int max(int lvl) {
		int hero = Dungeon.hero == null ? 1 : Dungeon.hero.lvl;
		int dmg = 6 + (int) (hero / 2.5f) + 2 * trueLevel()
				+ 2 * RingOfSharpshooting.levelDamageBonus(Dungeon.hero)
				+ (curseInfusionBonus ? 2 + hero / 15 : 0);
		return Math.max(0, dmg);
	}

	@Override
	public int level() {
		int level = (Dungeon.hero == null ? 0 : Dungeon.hero.lvl / 5) + trueLevel();
		if (curseInfusionBonus) level += 1 + level / 6;
		return level;
	}

	@Override
	public boolean isUpgradable() {
		return true;
	}

	@Override
	public SpiritArrow knockArrow() {
		return new RoyalArrow();
	}

	public class RoyalArrow extends SpiritArrow {
		{
			image = ItemSpriteSheet.ROYAL_ARROW;
		}
	}
}
