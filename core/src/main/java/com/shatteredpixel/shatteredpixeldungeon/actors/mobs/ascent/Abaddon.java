package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.sprites.AscentSprites;
import com.watabou.utils.PathFinder;

/** Seven Sins: Abaddon, king of the bottomless pit and its locusts (the ascent's demon boss of floor 10). */
public class Abaddon extends AscentBoss {

	{
		spriteClass = AscentSprites.Abaddon.class;
		properties.add(Property.DEMONIC);
	}

	@Override protected int skillCount() { return 3; }

	@Override
	protected String skillKey(int s) {
		return s == 0 ? "locusts" : s == 1 ? "pit" : "carapace";
	}

	@Override
	protected int[][] prepareWaves(int s, Char target) {
		return s == 0 ? bandWaves(target.pos, 10, 2) : null;
	}

	@Override
	protected int[] prepare(int s, Char target) {
		if (s == 1) return scatter(target.pos, 2, 5);
		if (s == 2) return HP < HT * 3 / 4 && buff(Barrier.class) == null ? new int[0] : null;
		return null;
	}

	@Override
	protected void release(int s, int[] cells) {
		if (s == 2) {
			Buff.affect(this, Barrier.class).setShield(HT / 8);
			summon(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Swarm.class, 2);
			return;
		}
		for (int c : cells) {
			Char ch = strike(c, skillMin(), skillMax());
			if (ch == null || !ch.isAlive()) continue;
			if (s == 0) Buff.affect(ch, Poison.class).set(5);
			else Buff.prolong(ch, Roots.class, 3f);
		}
	}

	@Override
	protected int cooldown(int s) {
		return s == 0 ? cd(6) : s == 1 ? cd(4) : cd(12);
	}
}
