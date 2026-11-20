package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.sprites.AscentSprites;
import com.watabou.utils.PathFinder;

/** Seven Sins: Azazel, who taught men to forge weapons (the ascent's demon boss of floor 15). */
public class Azazel extends AscentBoss {

	{
		spriteClass = AscentSprites.Azazel.class;
		properties.add(Property.DEMONIC);
	}

	@Override protected int skillCount() { return 3; }

	@Override
	protected String skillKey(int s) {
		return s == 0 ? "blades" : s == 1 ? "chains" : "armory";
	}

	@Override
	protected int[] prepare(int s, Char target) {
		if (s == 0) return scatter(target.pos, 3, 8);
		if (s == 1) {
			int[] l = line(target, 8);
			return l.length >= 2 ? l : null;
		}
		return enraged() ? crossLines(target.pos, 5) : null;
	}

	@Override
	protected void release(int s, int[] cells) {
		for (int c : cells) {
			Char ch = s == 1 ? Actor.findChar(c) : strike(c, skillMin(), skillMax());
			if (ch == null || ch == this || ch.alignment == alignment || !ch.isAlive()) continue;
			if (s == 0) Buff.affect(ch, Bleeding.class).set(skillMin() / 3f);
			if (s == 1) {
				strike(c, skillMin() / 2, skillMax() / 2);
				if (ch.isAlive()) {
					pull(ch);
					Buff.prolong(ch, Cripple.class, 3f);
				}
			}
		}
	}

	private void pull(Char ch) {
		if (Char.hasProp(ch, Property.IMMOVABLE)) return;
		int to = -1;
		for (int n : PathFinder.NEIGHBOURS8) {
			int c = pos + n;
			if (Dungeon.level.passable[c] && Actor.findChar(c) == null
					&& (to == -1 || Dungeon.level.distance(ch.pos, c) < Dungeon.level.distance(ch.pos, to))) to = c;
		}
		if (to == -1) return;
		Actor.add(new com.shatteredpixel.shatteredpixeldungeon.effects.Pushing(ch, ch.pos, to));
		ch.pos = to;
		Dungeon.level.occupyCell(ch);
	}

	@Override
	protected int cooldown(int s) {
		return s == 0 ? cd(5) : s == 1 ? cd(6) : cd(5);
	}
}
