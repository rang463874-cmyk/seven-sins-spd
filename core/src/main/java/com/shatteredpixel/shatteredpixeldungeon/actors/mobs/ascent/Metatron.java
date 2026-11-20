package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.sprites.AscentSprites;

/** Seven Sins: Metatron, the voice of God, tall as heaven, chasing the one who remained a sin through the sewers. */
public class Metatron extends AscentChaser {

	{
		spriteClass = AscentSprites.Metatron.class;
	}

	@Override protected int skillCount() { return 2; }

	@Override
	protected String skillKey(int s) {
		return s == 0 ? "pillars" : "voice";
	}

	@Override
	protected int[][] prepareWaves(int s, Char target) {
		return s == 1 ? ringWaves(1, 6) : null;
	}

	@Override
	protected int[] prepare(int s, Char target) {
		return s == 0 ? scatter(target.pos, 3, 10) : null;
	}

	@Override
	protected void release(int s, int[] cells) {
		for (int c : cells) {
			Char ch = strike(c, skillMin(), skillMax());
			if (ch == null || !ch.isAlive()) continue;
			if (s == 0) Buff.prolong(ch, Blindness.class, 1f);
			else Buff.prolong(ch, Vertigo.class, 2f);
		}
	}

	@Override
	protected int cooldown(int s) {
		return s == 0 ? 4 : 6;
	}
}
