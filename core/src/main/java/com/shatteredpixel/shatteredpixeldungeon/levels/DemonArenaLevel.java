package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DemonLordBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.LeviathanBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.demons.DemonHooks;
import com.shatteredpixel.shatteredpixeldungeon.demons.DemonLord;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Random;

/**
 * The demon lord's own domain, a side floor of depth 10 (branch DemonHooks.ARENA_BRANCH) where its true
 * form is fought. A 15x13 hall of the halls tileset with a long straight top wall (Leviathan lies along
 * it) and burning braziers. The hero arrives at the bottom; the way back (stairs up to Tengu's floor) is
 * sealed until the lord falls.
 */
public class DemonArenaLevel extends Level {

	private static final int W = 19, H = 17;
	/** interior: x 2..16, y 2..14 */
	private static final int LEFT = 2, TOP = 2, RIGHT = 16, BOTTOM = 14;

	{
		color1 = 0x801500;
		color2 = 0xa68521;
		viewDistance = 9;
	}

	public static int arrivalCell() {
		return (BOTTOM) * W + W / 2;
	}

	public static int portalCell() {
		return (BOTTOM + 1) * W + W / 2;
	}

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_HALLS;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_HALLS;
	}

	@Override
	protected boolean build() {
		setSize(W, H);
		for (int i = 0; i < length(); i++) map[i] = Terrain.WALL;

		for (int y = TOP; y <= BOTTOM; y++) {
			for (int x = LEFT; x <= RIGHT; x++) {
				map[y * W + x] = Random.Int(6) == 0 ? Terrain.EMPTY_DECO : Terrain.EMPTY;
			}
		}
		//a band of embers along the far wall and a few pillars at the corners
		for (int x = LEFT; x <= RIGHT; x++) map[(TOP + 1) * W + x] = Terrain.EMBERS;
		int[] pillars = {(TOP + 3) * W + LEFT + 2, (TOP + 3) * W + RIGHT - 2,
				(BOTTOM - 2) * W + LEFT + 2, (BOTTOM - 2) * W + RIGHT - 2};
		for (int p : pillars) map[p] = Terrain.STATUE;
		//decorated walls
		for (int x = LEFT; x <= RIGHT; x += 3) map[(TOP - 1) * W + x] = Terrain.WALL_DECO;

		//the way back: sealed until the lord is defeated
		map[portalCell()] = DemonHooks.stage == DemonHooks.DEFEATED ? Terrain.ENTRANCE : Terrain.LOCKED_EXIT;
		transitions.add(new LevelTransition(this, portalCell(), LevelTransition.Type.BRANCH_ENTRANCE,
				DemonHooks.ARENA_DEPTH, 0, LevelTransition.Type.REGULAR_EXIT));
		return true;
	}

	/** the lord fell: the stairs back to Tengu's floor open */
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
		DemonLord lord = DemonHooks.lord;
		if (lord == null || DemonHooks.stage == DemonHooks.DEFEATED) return;
		int center = (TOP + 4) * W + W / 2;
		if (lord == DemonLord.LEVIATHAN) {
			//along the far wall
			LeviathanBoss lev = LeviathanBoss.emerge(this, (TOP + 2) * W + W / 2, false);
			if (lev != null) {
				lev.state = lev.WANDERING;
				return;
			}
		}
		DemonLordBoss boss = new DemonLordBoss();
		boss.lord = lord;
		boss.pos = center;
		//awake and waiting (notice() needs a sprite, so no beckon while the level is being built)
		boss.state = boss.WANDERING;
		mobs.add(boss);
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

	@Override
	public String tileName(int tile) {
		if (tile == Terrain.EMBERS) return Messages.get(DemonArenaLevel.class, "embers_name");
		return super.tileName(tile);
	}
}
