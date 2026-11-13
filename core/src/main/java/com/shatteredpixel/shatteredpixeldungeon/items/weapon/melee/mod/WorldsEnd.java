package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.mod;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

/** T6 22~48: every hit is a quake: enemies within 2 tiles of the target (5x5) take SHOCK of it. */
public class WorldsEnd extends ApocalypseWeapon {
	public static final float SHOCK = 0.5f;
	{
		image = ItemSpriteSheet.WORLDS_END; hitSound = Assets.Sounds.HIT_CRUSH; hitSoundPitch = 0.5f;
		baseMin = 22; baseMax = 48;
	}
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		PixelScene.shake(2, 0.3f);
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.7f, 0.8f);
		for (int n : PathFinder.NEIGHBOURS9) {
			int c = defender.pos + n;
			if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 2);
		}
		splash(attacker, defender.pos, 2, Math.round(damage * SHOCK), defender, this);
		return super.proc(attacker, defender, damage);
	}
	@Override protected Object[] statsArgs() { return new Object[]{Math.round(SHOCK * 100)}; }
}
