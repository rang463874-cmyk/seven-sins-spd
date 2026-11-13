package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.mod;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/** T4 9~23: deals more the more wounded the target is (up to +EXEC_MAX at 0 HP). */
public class ExecutionerSword extends ModMeleeWeapon {
	public static final float EXEC_MAX = 0.8f;
	{
		image = ItemSpriteSheet.EXECUTIONER_SWORD; hitSound = Assets.Sounds.HIT_SLASH; hitSoundPitch = 0.8f;
		tier = 4; baseMin = 9; baseMax = 23;
	}
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		float missing = 1f - defender.HP / (float) Math.max(1, defender.HT);
		damage = Math.round(damage * (1f + EXEC_MAX * Math.max(0, missing)));
		return super.proc(attacker, defender, damage);
	}
	@Override protected Object[] statsArgs() { return new Object[]{Math.round(EXEC_MAX * 100)}; }
}
