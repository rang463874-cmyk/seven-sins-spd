package com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest;

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.watabou.utils.Point;

/**
 * Seven Sins: the trial of sin's sanctum (sins/TrialHooks, a city floor). Same shape as a virtue sanctum, so the
 * shadow reuses the champions' arena rules (it seals the door, never leaves), but dark: plain statues, the
 * shadow's pedestal in the middle.
 */
public class TrialSanctumRoom extends VirtueSanctumRoom {

	@Override
	public void paint(Level level) {
		Painter.fill(level, this, Terrain.WALL);
		Painter.fill(level, this, 1, Terrain.EMPTY_SP);

		Painter.set(level, left + 3, top + 3, Terrain.STATUE);
		Painter.set(level, right - 3, top + 3, Terrain.STATUE);
		Painter.set(level, left + 3, bottom - 3, Terrain.STATUE);
		Painter.set(level, right - 3, bottom - 3, Terrain.STATUE);

		Point a = altar();
		Painter.set(level, a.x, a.y, Terrain.PEDESTAL);

		for (Door door : connected.values()) {
			door.set(Door.Type.REGULAR);
		}
	}
}
