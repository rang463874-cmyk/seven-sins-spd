package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.mod;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/** T2 5~13: its spikes punch through half of the target's armor. */
public class Morningstar extends ModMeleeWeapon {
	public static final float ARMOR = 0.5f;
	{
		image = ItemSpriteSheet.MORNINGSTAR; hitSound = Assets.Sounds.HIT_CRUSH; hitSoundPitch = 1.1f;
		tier = 2; baseMin = 5; baseMax = 13;
	}
	@Override public float armorFactor(Char attacker, Char defender) { return ARMOR; }
	@Override protected Object[] statsArgs() { return new Object[]{Math.round((1 - ARMOR) * 100)}; }
}
