package com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret;

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.sewerboss.SewerBossEntranceRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.GoetiaSigil;
import com.watabou.utils.Point;

/**
 * Seven Sins: the hidden room of a boss floor's demonic sigil (owner, Nov 13 2026: tucked away like the rat king's).
 * The sigil lies there from the start but stays dormant until the floor's boss is dead.
 */
public class GoetiaSigilRoom extends SecretRoom {

	@Override
	public boolean canConnect(Room r) {
		return !(r instanceof SewerBossEntranceRoom) && super.canConnect(r);
	}

	@Override
	public boolean canPlaceWater(Point p) {
		return false;
	}

	@Override
	public boolean canPlaceGrass(Point p) {
		return false;
	}

	@Override
	public boolean canPlaceTrap(Point p) {
		return false;
	}

	@Override
	public int minWidth() { return 5; }
	public int maxWidth() { return 5; }
	public int minHeight() { return 5; }
	public int maxHeight() { return 5; }

	@Override
	public void paint(Level level) {
		Painter.fill(level, this, Terrain.WALL);
		Painter.fill(level, this, 1, Terrain.EMPTY_SP);

		int centre = level.pointToCell(center());
		level.setTrap(new GoetiaSigil(), centre);
		Painter.set(level, centre, Terrain.TRAP);

		entrance().set(Door.Type.HIDDEN);
	}
}
