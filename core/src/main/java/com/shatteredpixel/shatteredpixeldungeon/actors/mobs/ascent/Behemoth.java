package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.AscentSprites;

import java.util.ArrayList;

/** Seven Sins: Behemoth, the giant beast of the land, chasing the demons' new god through the sewers. */
public class Behemoth extends AscentChaser {

	{
		spriteClass = AscentSprites.Behemoth.class;
		properties.add(Property.DEMONIC);
	}

	@Override protected int skillCount() { return 2; }

	@Override
	protected String skillKey(int s) {
		return s == 0 ? "stampede" : "quake";
	}

	@Override
	protected int[] prepare(int s, Char target) {
		if (s == 1) return scatter(target.pos, 3, 10);
		Ballistica b = new Ballistica(pos, target.pos, Ballistica.STOP_SOLID);
		ArrayList<Integer> out = new ArrayList<>();
		for (int i = 1; i < b.path.size() && out.size() < 12; i++) {
			int c = b.path.get(i);
			if (Dungeon.level.solid[c]) break;
			out.add(c);
		}
		return out.size() >= 2 ? toArray(out) : null;
	}

	@Override
	protected void release(int s, int[] cells) {
		for (int c : cells) {
			Char ch = strike(c, skillMin(), skillMax());
			if (ch != null && ch.isAlive() && s == 1) Buff.prolong(ch, Cripple.class, 3f);
		}
		PixelScene.shake(2, 0.4f);
		if (s == 0) {
			int dest = -1;
			for (int c : cells) if (Dungeon.level.passable[c] && Actor.findChar(c) == null) dest = c;
			if (dest != -1) {
				Actor.add(new Pushing(this, pos, dest));
				pos = dest;
				Dungeon.level.occupyCell(this);
			}
		}
	}

	@Override
	protected int cooldown(int s) {
		return s == 0 ? 5 : 4;
	}
}
