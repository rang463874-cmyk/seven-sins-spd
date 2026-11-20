package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.sprites.AscentSprites;
import com.watabou.utils.PathFinder;

/** Seven Sins: the Ophanim, wheels within wheels full of eyes (the ascent's angel boss of floor 20). */
public class Ophanim extends AscentBoss {

	{
		spriteClass = AscentSprites.Ophanim.class;
	}

	@Override protected int skillCount() { return 3; }

	@Override
	protected String skillKey(int s) {
		return s == 0 ? "wheel" : s == 1 ? "gaze" : "eyes";
	}

	@Override
	protected int[][] prepareWaves(int s, Char target) {
		return s == 0 ? bandWaves(target.pos, 12, 3) : null;
	}

	@Override
	protected int[] prepare(int s, Char target) {
		if (s == 1) {
			int[] l = line(target, 12);
			return l.length >= 1 ? l : null;
		}
		if (s == 2) return checker(target.pos, 3);
		return null;
	}

	@Override
	protected void release(int s, int[] cells) {
		for (int c : cells) {
			Char ch = strike(c, skillMin(), skillMax());
			if (ch == null || !ch.isAlive()) continue;
			if (s == 0) Buff.prolong(ch, Cripple.class, 3f);
			else if (s == 1) Buff.prolong(ch, Vulnerable.class, 5f);
			else Buff.prolong(ch, Hex.class, 5f);
		}
	}

	@Override
	protected int cooldown(int s) {
		return s == 0 ? cd(6) : s == 1 ? cd(4) : cd(5);
	}
}
