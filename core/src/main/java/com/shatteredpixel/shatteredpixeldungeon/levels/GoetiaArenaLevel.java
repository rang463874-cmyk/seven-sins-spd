package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.goetia.GoetiaLord;
import com.shatteredpixel.shatteredpixeldungeon.goetia.GoetiaDemon;
import com.shatteredpixel.shatteredpixeldungeon.goetia.GoetiaHooks;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Random;

/**
 * Seven Sins: the hidden room of a boss floor (branch GoetiaHooks.ARENA_BRANCH, same depth): a ritual hall in
 * the floor's own tileset with a summoning circle of embers, where the floor's demon king waits. The way
 * back (stairs to the boss floor's exit) stays sealed until the king falls.
 */
public class GoetiaArenaLevel extends Level {

	private static final int W = 17, H = 17;
	private static final int LEFT = 2, TOP = 2, RIGHT = 14, BOTTOM = 14;

	{
		color1 = 0x401040;
		color2 = 0x9020c0;
		viewDistance = 8;
	}

	public static int arrivalCell() {
		return BOTTOM * W + W / 2;
	}

	public static int portalCell() {
		return (BOTTOM + 1) * W + W / 2;
	}

	private static int centerCell() {
		return ((TOP + BOTTOM) / 2 - 1) * W + W / 2;
	}

	@Override
	public String tilesTex() {
		switch (Math.min(5, Math.max(1, (Dungeon.depth + 4) / 5))) {
			case 1: return Assets.Environment.TILES_SEWERS;
			case 2: return Assets.Environment.TILES_PRISON;
			case 3: return Assets.Environment.TILES_CAVES;
			case 4: return Assets.Environment.TILES_CITY;
			default: return Assets.Environment.TILES_HALLS;
		}
	}

	@Override
	public String waterTex() {
		switch (Math.min(5, Math.max(1, (Dungeon.depth + 4) / 5))) {
			case 1: return Assets.Environment.WATER_SEWERS;
			case 2: return Assets.Environment.WATER_PRISON;
			case 3: return Assets.Environment.WATER_CAVES;
			case 4: return Assets.Environment.WATER_CITY;
			default: return Assets.Environment.WATER_HALLS;
		}
	}

	@Override
	protected boolean build() {
		setSize(W, H);
		for (int i = 0; i < length(); i++) map[i] = Terrain.WALL;
		int cx = W / 2, cy = (TOP + BOTTOM) / 2 - 1;
		for (int y = TOP; y <= BOTTOM; y++) {
			for (int x = LEFT; x <= RIGHT; x++) {
				int dx = x - cx, dy = y - cy;
				int r2 = dx * dx + dy * dy;
				//a rough round hall
				if (r2 > 46) continue;
				map[y * W + x] = Random.Int(7) == 0 ? Terrain.EMPTY_DECO : Terrain.EMPTY;
				//the summoning circle
				if (r2 >= 8 && r2 <= 10) map[y * W + x] = Terrain.EMBERS;
			}
		}
		//the corridor to the stairs
		for (int y = cy; y <= BOTTOM; y++) if (map[y * W + cx] == Terrain.WALL) map[y * W + cx] = Terrain.EMPTY;
		int[] pillars = {(cy - 4) * W + cx - 4, (cy - 4) * W + cx + 4, (cy + 4) * W + cx - 4, (cy + 4) * W + cx + 4};
		for (int p : pillars) map[p] = Terrain.STATUE;
		map[centerCell()] = Terrain.EMPTY_SP;

		map[portalCell()] = GoetiaHooks.lordsBeaten.contains(Dungeon.depth) ? Terrain.ENTRANCE : Terrain.LOCKED_EXIT;
		transitions.add(new LevelTransition(this, portalCell(), LevelTransition.Type.BRANCH_ENTRANCE,
				Dungeon.depth, 0, LevelTransition.Type.REGULAR_EXIT));
		return true;
	}

	/** the king fell: the stairs back open */
	public void unlockPortal() {
		set(portalCell(), Terrain.ENTRANCE);
		GameScene.updateMap(portalCell());
	}

	@Override
	public Mob createMob() {
		return null;
	}

	@Override
	protected void createMobs() {
		GoetiaDemon d = GoetiaDemon.lordOf(Dungeon.depth);
		if (d == null || GoetiaHooks.lordsBeaten.contains(Dungeon.depth)) return;
		GoetiaLord lord = new GoetiaLord().setup(d, Dungeon.depth);
		lord.pos = centerCell();
		lord.state = lord.WANDERING;
		mobs.add(lord);
	}

	@Override
	public Actor addRespawner() {
		return null;
	}

	@Override
	protected void createItems() {
	}

	@Override
	public int randomRespawnCell(Char ch) {
		return arrivalCell() - W;
	}
}
