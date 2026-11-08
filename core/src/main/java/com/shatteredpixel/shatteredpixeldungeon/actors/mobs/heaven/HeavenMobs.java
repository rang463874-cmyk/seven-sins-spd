package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.heaven;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;

import java.util.ArrayList;

/** Shared helpers for the heavens' monsters (floors 27-30). */
public class HeavenMobs {

	/** the depth their stats are built for (never below 26, so dev summons on early floors stay sane-ish) */
	public static int depth() {
		return Math.max(26, Dungeon.scalingDepth());
	}

	/** living enemy-side mobs within r of the cell, except the seraph's body parts and the given mob */
	public static ArrayList<Mob> alliesNear(Mob self, int r) {
		ArrayList<Mob> out = new ArrayList<>();
		if (Dungeon.level == null) return out;
		for (Mob m : Dungeon.level.mobs) {
			if (m == self || !m.isAlive() || m instanceof SeraphPart) continue;
			if (m.alignment != Char.Alignment.ENEMY) continue;
			if (Dungeon.level.distance(self.pos, m.pos) <= r) out.add(m);
		}
		return out;
	}
}
