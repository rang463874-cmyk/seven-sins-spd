package com.shatteredpixel.shatteredpixeldungeon.items.elite;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.SealSanctumRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.VirtueSanctumRoom;

/**
 * Seven Sins: shared base of the thrown terrain-shaping alchemy items (wall acid, stone dust, drying
 * powder). Each one is used up when it lands; {@link #land} does the work.
 */
public abstract class TerrainCharm extends Item {

	{
		stackable = true;
		defaultAction = AC_THROW;
	}

	/** where it was thrown from (the throw direction matters for the acid and the dust) */
	protected int from = -1;

	@Override
	protected void onThrow(int cell) {
		if (curUser != null) {
			from = curUser.pos;
			com.shatteredpixel.shatteredpixeldungeon.predator.PredatorHooks.onConsumed(curUser, this);
		}
		if (Dungeon.level.pit[cell]) {
			super.onThrow(cell);
			return;
		}
		land(cell);
	}

	protected abstract void land(int cell);

	/** the throw direction as one of the 8 steps (dominant axis wins unless it is close to diagonal) */
	protected int[] direction(int cell) {
		int w = Dungeon.level.width();
		if (from < 0 || from == cell) return new int[]{0, -1};
		int dx = cell % w - from % w, dy = cell / w - from / w;
		int ax = Math.abs(dx), ay = Math.abs(dy);
		int sx = Integer.signum(dx), sy = Integer.signum(dy);
		if (ax > 2 * ay) sy = 0;
		else if (ay > 2 * ax) sx = 0;
		return new int[]{sx, sy};
	}

	protected static boolean onBorder(Level level, int cell) {
		int w = level.width(), x = cell % w, y = cell / w;
		return x <= 0 || y <= 0 || x >= w - 1 || y >= level.height() - 1;
	}

	/** walls of locked/crystal vaults and of quest arenas can't be touched */
	protected static boolean protectedCell(Level level, int cell) {
		if (!(level instanceof RegularLevel)) return true;
		java.util.ArrayList<Room> rooms = ((RegularLevel) level).rooms();
		if (rooms == null) return false;
		int w = level.width(), x = cell % w, y = cell / w;
		for (Room r : rooms) {
			if (x < r.left || x > r.right || y < r.top || y > r.bottom) continue;
			if (r instanceof VirtueSanctumRoom || r instanceof SealSanctumRoom) return true;
			for (Room.Door d : r.connected.values()) {
				if (d != null && (d.type == Room.Door.Type.LOCKED || d.type == Room.Door.Type.CRYSTAL)) return true;
			}
		}
		return false;
	}

	@Override public boolean isUpgradable() { return false; }
	@Override public boolean isIdentified() { return true; }
	@Override public int energyVal() { return quantity; }
}
