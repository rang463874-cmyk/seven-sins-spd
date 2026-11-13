package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.mod;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

/** T3 6~17: the wavy blade tears wounds that bleed. */
public class Flamberge extends ModMeleeWeapon {
	public static final float BLEED_CHANCE = 0.5f, BLEED_SHARE = 0.5f;
	{
		image = ItemSpriteSheet.FLAMBERGE; hitSound = Assets.Sounds.HIT_SLASH; hitSoundPitch = 0.95f;
		tier = 3; baseMin = 6; baseMax = 17;
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
