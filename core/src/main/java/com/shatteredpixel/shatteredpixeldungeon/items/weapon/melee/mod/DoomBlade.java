package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.mod;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;

/** T6 10~12 + ruin: every hit stores ruin energy for good (bosses x5), +1 min/max damage per RUIN_PER_DMG energy. */
public class DoomBlade extends ApocalypseWeapon {
	public static final float RUIN_PER_DMG = 15f;
	public float ruin = 0;
	{
		image = ItemSpriteSheet.DOOM_BLADE; hitSound = Assets.Sounds.HIT_SLASH; hitSoundPitch = 0.6f;
		baseMin = 10; baseMax = 12;
	}
	public int ruinBonus() { return (int) (ruin / RUIN_PER_DMG); }
	@Override public int min(int lvl) { return super.min(lvl) + ruinBonus(); }
	@Override public int max(int lvl) { return super.max(lvl) + ruinBonus(); }
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (attacker instanceof Hero) {
			float gain = 1f + 0.25f * buffedLvl();
			if (defender.properties().contains(Char.Property.BOSS)) gain *= 5;
			ruin += gain;
			updateQuickslot();
		}
		return super.proc(attacker, defender, damage);
	}
	@Override protected Object[] statsArgs() { return new Object[]{Math.round(ruin), ruinBonus(), (int) RUIN_PER_DMG}; }
	@Override public void storeInBundle(Bundle b) { super.storeInBundle(b); b.put("ruin", ruin); }
	@Override public void restoreFromBundle(Bundle b) { super.restoreFromBundle(b); ruin = b.getFloat("ruin"); }
}
