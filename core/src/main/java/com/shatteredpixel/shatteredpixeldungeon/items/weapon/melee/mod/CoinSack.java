package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.mod;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/** T3 5~15: a sack of coins swung like a club; enemies it kills spill gold. */
public class CoinSack extends ModMeleeWeapon {
	{
		image = ItemSpriteSheet.COIN_SACK; hitSound = Assets.Sounds.GOLD; hitSoundPitch = 0.9f;
		tier = 3; baseMin = 5; baseMax = 15;
	}
	public static int goldFor() { return 4 + 2 * Dungeon.scalingDepth(); }
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (attacker == Dungeon.hero && lethal(defender, damage) && defender.alignment == Char.Alignment.ENEMY) {
			Dungeon.level.drop(new Gold(goldFor()), defender.pos).sprite.drop();
		}
		return super.proc(attacker, defender, damage);
	}
	@Override protected Object[] statsArgs() { return new Object[]{goldFor()}; }
}
