package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.sprites.AscentSprites;
import com.watabou.utils.PathFinder;

/** Seven Sins: the Cherubim guarding Eden with a flaming sword that turns every way (the ascent's angel boss of floor 25). */
public class Cherubim extends AscentBoss {

	{
		spriteClass = AscentSprites.Cherubim.class;
	}

	@Override protected int skillCount() { return 3; }

	@Override
	protected String skillKey(int s) {
		return s == 0 ? "sword" : s == 1 ? "faces" : "guard";
	}

	@Override
	protected int[][] prepareWaves(int s, Char target) {
		if (s != 0) return null;
		if (Dungeon.level.distance(pos, target.pos) > 4) return new int[0][];
		int w = Dungeon.level.width();
		int dx = target.pos % w - pos % w, dy = target.pos / w - pos / w;
		int start = Math.abs(dy) >= Math.abs(dx) ? (dy < 0 ? 0 : 2) : (dx > 0 ? 1 : 3);
		//the sword sweeps clockwise, beginning one quarter before the target's
		int[][] out = new int[4][];
		for (int i = 0; i < 4; i++) out[i] = quarter(1, 4, (start + 3 + i) % 4);
		return out;
	}

	@Override
	protected int[] prepare(int s, Char target) {
		if (s == 1) return xCross(target.pos, 4);
		if (s == 2) return crossLines(target.pos, 5);
		return null;
	}

	@Override
	protected void release(int s, int[] cells) {
		for (int c : cells) {
			Char ch = strike(c, skillMin(), skillMax());
			if (ch == null || !ch.isAlive()) continue;
			if (s == 0) Buff.affect(ch, Burning.class).reignite(ch);
			else if (s == 1) Buff.prolong(ch, Weakness.class, 5f);
			else Buff.prolong(ch, Blindness.class, 2f);
		}
	}

	@Override
	protected int cooldown(int s) {
		return s == 0 ? cd(7) : s == 1 ? cd(4) : cd(5);
	}
}
