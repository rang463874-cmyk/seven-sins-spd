package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.sprites.AscentSprites;
import com.watabou.utils.PathFinder;

/** Seven Sins: Moloch, the bull-headed furnace god (the ascent's demon boss of floor 25). */
public class Moloch extends AscentBoss {

	{
		spriteClass = AscentSprites.Moloch.class;
		properties.add(Property.DEMONIC);
		properties.add(Property.FIERY);
	}

	@Override protected int skillCount() { return 3; }

	@Override
	protected String skillKey(int s) {
		return s == 0 ? "charge" : s == 1 ? "furnace" : "offering";
	}

	@Override
	protected int[] prepare(int s, Char target) {
		if (s == 0) {
			int[] l = line(target, 10);
			return l.length >= 2 ? l : null;
		}
		if (s == 1) return Dungeon.level.distance(pos, target.pos) <= 3 ? donut(1, 2) : null;
		return minions() < 3 ? new int[0] : null;
	}

	@Override
	protected void release(int s, int[] cells) {
		if (s == 2) {
			summon(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental.FireElemental.class, 2);
			return;
		}
		for (int c : cells) {
			Char ch = strike(c, skillMin(), skillMax());
			if (ch != null && s == 1 && ch.isAlive()) Buff.affect(ch, Burning.class).reignite(ch);
		}
		if (s == 0) dashAlong(cells);
	}

	@Override
	protected int cooldown(int s) {
		return s == 0 ? cd(5) : s == 1 ? cd(4) : cd(9);
	}
}
