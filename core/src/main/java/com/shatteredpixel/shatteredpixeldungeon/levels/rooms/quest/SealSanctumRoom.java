package com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest;

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.watabou.utils.Point;

/**
 * The Seal Warden's arena, added to the sewer prophet's floor (see {@code Prophet.Quest.spawnRoom}).
 * An 11x11 dead end (9x9 inside) with a single door, special floor, a horseman statue near each
 * corner (cover against the warden's charge and arrows) and the warden's pedestal in the middle.
 * The warden seals the door once the fight starts (see {@code SealWarden}).
 */
public class SealSanctumRoom extends StandardRoom {

	public static final int SIZE = 11;

	@Override
	public float[] sizeCatProbs() {
		return new float[]{0, 1, 0};
	}

	@Override public int minWidth() { return SIZE; }
	@Override public int maxWidth() { return SIZE; }
	@Override public int minHeight() { return SIZE; }
	@Override public int maxHeight() { return SIZE; }

	@Override
	public int maxConnections(int direction) {
		return 1;
	}

	@Override
	public int mobSpawnWeight() {
		return 0;
	}

	/** the warden's pedestal (the room is odd-sized, so this is exact) */
	public Point altar() {
		return new Point((left + right) / 2, (top + bottom) / 2);
	}

	@Override
	public void paint(Level level) {
		Painter.fill(level, this, Terrain.WALL);
		Painter.fill(level, this, 1, Terrain.EMPTY_SP);

		//one statue per horseman, two tiles in from each corner
		Painter.set(level, left + 3, top + 3, Terrain.STATUE_SP);
		Painter.set(level, right - 3, top + 3, Terrain.STATUE_SP);
		Painter.set(level, left + 3, bottom - 3, Terrain.STATUE_SP);
		Painter.set(level, right - 3, bottom - 3, Terrain.STATUE_SP);

		Point a = altar();
		Painter.set(level, a.x, a.y, Terrain.PEDESTAL);

		for (Door door : connected.values()) {
			door.set(Door.Type.REGULAR);
		}
	}

	@Override public boolean canPlaceWater(Point p) { return false; }
	@Override public boolean canPlaceGrass(Point p) { return false; }
	@Override public boolean canPlaceTrap(Point p) { return false; }

	@Override
	public boolean canPlaceItem(Point p, Level l) {
		return false;
	}

	@Override
	public boolean canPlaceCharacter(Point p, Level l) {
		return false;
	}
}
