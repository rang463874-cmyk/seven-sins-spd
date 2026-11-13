package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.mod;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/** T2 2~8: a weak club in melee, but while it is wielded thrown weapons hit harder (MissileWeapon.damageRoll). */
public class Sling extends ModMeleeWeapon {
	public static final float THROWN_BONUS = 0.25f, PER_LEVEL = 0.05f;
	{
		image = ItemSpriteSheet.SLING; hitSound = Assets.Sounds.HIT; hitSoundPitch = 1.2f;
		tier = 2; baseMin = 2; baseMax = 8; maxScale = 2;
	}
	public float bonus() { return THROWN_BONUS + PER_LEVEL * buffedLvl(); }
	/** from MissileWeapon.damageRoll */
	public static int thrownDamage(Char owner, int damage) {
		if (owner instanceof Hero && ((Hero) owner).belongings.weapon instanceof Sling) {
			return Math.round(damage * (1f + ((Sling) ((Hero) owner).belongings.weapon).bonus()));
		}
		return damage;
	}
	@Override protected Object[] statsArgs() {
		return new Object[]{Math.round(THROWN_BONUS * 100), Math.round(PER_LEVEL * 100), Math.round(bonus() * 100)};
	}
}
