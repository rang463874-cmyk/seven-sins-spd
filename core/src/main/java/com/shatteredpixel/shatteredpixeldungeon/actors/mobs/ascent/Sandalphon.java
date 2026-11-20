package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.sprites.AscentSprites;
import com.watabou.utils.PathFinder;

/** Seven Sins: Sandalphon, the towering angel who sings the prayers, twin of Metatron (the ascent's angel boss of floor 10). */
public class Sandalphon extends AscentBoss {

	{
		spriteClass = AscentSprites.Sandalphon.class;
	}

	@Override protected int skillCount() { return 3; }

	@Override
	protected String skillKey(int s) {
		return s == 0 ? "song" : s == 1 ? "hymn" : "voice";
	}

	@Override
	protected int[][] prepareWaves(int s, Char target) {
		return s == 0 ? ringWaves(1, 7) : null;
	}

	@Override
	protected int[] prepare(int s, Char target) {
		if (s == 1) return minions() < 3 ? new int[0] : null;
		if (s == 2) return crossLines(target.pos, 6);
		return null;
	}

	@Override
	protected void release(int s, int[] cells) {
		if (s == 1) {
			Buff.affect(this, Barrier.class).setShield(HT / 10);
			summon(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.heaven.Believer.class, 2);
			return;
		}
		for (int c : cells) {
			Char ch = strike(c, skillMin(), skillMax());
			if (ch == null || !ch.isAlive()) continue;
			if (s == 0) Buff.prolong(ch, Vertigo.class, 2f);
			else if (enraged()) Buff.prolong(ch, Paralysis.class, 1f);
		}
	}

	@Override
	protected int cooldown(int s) {
		return s == 0 ? cd(7) : s == 1 ? cd(11) : cd(5);
	}
}
