package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.sprites.AscentSprites;
import com.watabou.utils.PathFinder;

/** Seven Sins: Lilith, queen of the night (the ascent's demon boss of floor 20). */
public class Lilith extends AscentBoss {

	{
		spriteClass = AscentSprites.Lilith.class;
		properties.add(Property.DEMONIC);
	}

	@Override protected int skillCount() { return 3; }

	@Override
	protected String skillKey(int s) {
		return s == 0 ? "night" : s == 1 ? "spirits" : "kiss";
	}

	@Override
	protected int[] prepare(int s, Char target) {
		if (s == 0) return checker(target.pos, 3);
		if (s == 1) return minions() < 4 ? new int[0] : null;
		return cone(target.pos, 5);
	}

	@Override
	protected void release(int s, int[] cells) {
		if (s == 1) {
			summon(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Wraith.class, 2);
			return;
		}
		int hits = 0;
		for (int c : cells) {
			Char ch = strike(c, skillMin(), skillMax());
			if (ch == null) continue;
			hits++;
			if (ch.isAlive()) {
				Charm charm = Buff.affect(ch, Charm.class, s == 0 ? 3f : 2f);
				charm.object = id();
			}
		}
		if (hits > 0 && isAlive()) HP = Math.min(HT, HP + hits * skillMin());
	}

	@Override
	protected int cooldown(int s) {
		return s == 0 ? cd(5) : s == 1 ? cd(10) : cd(4);
	}
}
