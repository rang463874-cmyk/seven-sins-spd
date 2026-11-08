package com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest;

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.watabou.utils.Point;

/**
 * A virtue champion's sanctum (added by {@code VirtueHooks.addSanctum} to the champion's floor).
 * A 13x13 dead end (11x11 inside) with a single door, special floor, a pillar of light near each
 * corner (cover against the champion's skills) and the champion's pedestal in the middle.
 * The champion waits here and seals the door once the hero steps inside (see {@code VirtueChampion}).
 */
public class VirtueSanctumRoom extends StandardRoom {

	public static final int SIZE = 13;

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

	@Override
	public boolean canMerge(Level l, Room other, Point p, int mergeTerrain) {
		//its walls must stay whole, or the sealed door would not keep anyone in
		return false;
	}

	/** the champion's pedestal (the room is odd-sized, so this is exact) */
	public Point altar() {
		return new Point((left + right) / 2, (top + bottom) / 2);
	}

	@Override
	public void paint(Level level) {
		Painter.fill(level, this, Terrain.WALL);
		Painter.fill(level, this, 1, Terrain.EMPTY_SP);

		//four pillars, three tiles in from each corner
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
