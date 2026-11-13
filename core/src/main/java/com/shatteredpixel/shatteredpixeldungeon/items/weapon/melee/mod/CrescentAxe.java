package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.mod;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

/** T4 10~24: critical hits (CRIT_CHANCE) deal CRIT_MULT damage. */
public class CrescentAxe extends ModMeleeWeapon {
	public static final float CRIT_CHANCE = 0.2f, CRIT_MULT = 2.25f;
	{
		image = ItemSpriteSheet.CRESCENT_AXE; hitSound = Assets.Sounds.HIT_SLASH; hitSoundPitch = 0.85f;
		tier = 4; baseMin = 10; baseMax = 24;
	}
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (Random.Float() < CRIT_CHANCE) {
			damage = Math.round(damage * CRIT_MULT);
			Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
			if (defender.sprite != null) defender.sprite.showStatus(CharSprite.WARNING, Messages.get(CrescentAxe.class, "crit"));
		}
		return super.proc(attacker, defender, damage);
	}
	@Override protected Object[] statsArgs() { return new Object[]{Math.round(CRIT_CHANCE * 100), Math.round(CRIT_MULT * 100)}; }
}
