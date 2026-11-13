package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.mod;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/** T5 12~30: +GIANT_BONUS damage to large enemies, minibosses and bosses. */
public class GiantSlayer extends ModMeleeWeapon {
	public static final float GIANT_BONUS = 0.5f;
	{
		image = ItemSpriteSheet.GIANT_SLAYER; hitSound = Assets.Sounds.HIT_SLASH; hitSoundPitch = 0.75f;
		tier = 5; baseMin = 12; baseMax = 30;
	}
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (big(defender)) damage = Math.round(damage * (1f + GIANT_BONUS));
		return super.proc(attacker, defender, damage);
	}
	@Override protected Object[] statsArgs() { return new Object[]{Math.round(GIANT_BONUS * 100)}; }
}
