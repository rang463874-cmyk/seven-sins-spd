package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.mod;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

/** T2 4~12: quick (x0.8 attack time) and its hits may bleed. */
public class Kukri extends ModMeleeWeapon {
	public static final float BLEED_CHANCE = 0.3f, BLEED_SHARE = 0.4f;
	{
		image = ItemSpriteSheet.KUKRI; hitSound = Assets.Sounds.HIT_SLASH; hitSoundPitch = 1.15f;
		tier = 2; baseMin = 4; baseMax = 12; DLY = 0.8f;
	}
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (defender.isAlive() && Random.Float() < BLEED_CHANCE) {
			Buff.affect(defender, Bleeding.class).set(Math.max(1, Math.round(damage * BLEED_SHARE)));
		}
		return super.proc(attacker, defender, damage);
	}
	@Override protected Object[] statsArgs() { return new Object[]{Math.round(BLEED_CHANCE * 100)}; }
}
