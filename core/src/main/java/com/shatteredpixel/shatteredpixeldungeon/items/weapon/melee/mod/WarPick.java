package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.mod;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/** T3 7~18: the pick bites deeper into armor: bonus damage equal to a roll of the target's armor. */
public class WarPick extends ModMeleeWeapon {
	{
		image = ItemSpriteSheet.WAR_PICK; hitSound = Assets.Sounds.HIT_STAB; hitSoundPitch = 0.85f;
		tier = 3; baseMin = 7; baseMax = 18;
	}
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage += Math.max(0, defender.drRoll());
		return super.proc(attacker, defender, damage);
	}
}
