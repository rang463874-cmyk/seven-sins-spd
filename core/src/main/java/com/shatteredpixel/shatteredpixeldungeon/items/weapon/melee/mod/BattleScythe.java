package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.mod;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/** T3 7~17: a wide sweep: enemies next to both you and the target take part of the hit. */
public class BattleScythe extends ModMeleeWeapon {
	public static final float SWEEP = 0.5f;
	{
		image = ItemSpriteSheet.BATTLE_SCYTHE; hitSound = Assets.Sounds.HIT_SLASH; hitSoundPitch = 0.9f;
		tier = 3; baseMin = 7; baseMax = 17;
	}
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		int d = Math.round(damage * SWEEP);
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (m == defender || !m.isAlive() || m.alignment == attacker.alignment) continue;
			if (Dungeon.level.adjacent(m.pos, attacker.pos) && Dungeon.level.adjacent(m.pos, defender.pos)) {
				int dd = Math.max(0, d - m.drRoll());
				if (dd > 0) m.damage(dd, this);
			}
		}
		return super.proc(attacker, defender, damage);
	}
	@Override protected Object[] statsArgs() { return new Object[]{Math.round(SWEEP * 100)}; }
}
