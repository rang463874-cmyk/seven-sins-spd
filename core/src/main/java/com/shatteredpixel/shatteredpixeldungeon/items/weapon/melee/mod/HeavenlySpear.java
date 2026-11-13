package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.mod;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/** T5 12~29, reach 2; the thrust goes through: the enemy right behind the target takes the same hit. */
public class HeavenlySpear extends ModMeleeWeapon {
	{
		image = ItemSpriteSheet.HEAVENLY_SPEAR; hitSound = Assets.Sounds.HIT_STAB; hitSoundPitch = 0.9f;
		tier = 5; baseMin = 12; baseMax = 29; RCH = 2;
	}
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		Ballistica line = new Ballistica(attacker.pos, defender.pos, Ballistica.STOP_SOLID);
		int i = line.path.indexOf(defender.pos);
		if (i >= 0 && i + 1 < line.path.size() && i + 1 <= line.dist) {
			Char behind = Actor.findChar(line.path.get(i + 1));
			if (behind != null && behind != attacker && behind.alignment != attacker.alignment && behind.isAlive()) {
				int d = Math.max(0, damage - behind.drRoll());
				if (d > 0) behind.damage(d, this);
			}
		}
		return super.proc(attacker, defender, damage);
	}
}
