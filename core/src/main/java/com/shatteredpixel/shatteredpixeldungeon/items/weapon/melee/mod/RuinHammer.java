package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.mod;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/** T5 14~32: every hit sends a shockwave: enemies next to the target take SHOCK of it. */
public class RuinHammer extends ModMeleeWeapon {
	public static final float SHOCK = 0.4f;
	{
		image = ItemSpriteSheet.RUIN_HAMMER; hitSound = Assets.Sounds.HIT_CRUSH; hitSoundPitch = 0.75f;
		tier = 5; baseMin = 14; baseMax = 32;
	}
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		CellEmitter.get(defender.pos).burst(Speck.factory(Speck.ROCK), 4);
		splash(attacker, defender.pos, 1, Math.round(damage * SHOCK), defender, this);
		return super.proc(attacker, defender, damage);
	}
	@Override protected Object[] statsArgs() { return new Object[]{Math.round(SHOCK * 100)}; }
}
