package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.mod;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShaftParticle;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/** T4 9~22: blessed steel, +UNDEAD_BONUS damage against the undead and demons (owner). */
public class CrusaderMace extends ModMeleeWeapon {
	public static final float UNDEAD_BONUS = 0.6f;
	{
		image = ItemSpriteSheet.CRUSADER_MACE; hitSound = Assets.Sounds.HIT_CRUSH; hitSoundPitch = 1f;
		tier = 4; baseMin = 9; baseMax = 22;
	}
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (defender.properties().contains(Char.Property.UNDEAD) || defender.properties().contains(Char.Property.DEMONIC)) {
			damage = Math.round(damage * (1f + UNDEAD_BONUS));
			if (defender.sprite != null) defender.sprite.emitter().start(ShaftParticle.FACTORY, 0.2f, 3);
		}
		return super.proc(attacker, defender, damage);
	}
	@Override protected Object[] statsArgs() { return new Object[]{Math.round(UNDEAD_BONUS * 100)}; }
}
