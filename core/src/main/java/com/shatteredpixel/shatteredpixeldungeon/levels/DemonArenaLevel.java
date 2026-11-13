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

	/** Leviathan's arena (owner, Nov 14 2026): the far side is an abyss (rows 1..4) and only its head rises over
	 *  the ledge (row 5), like the holy giant; pillars on the floor give shelter from its wave and laser. */
	public boolean sea = false;
	private static final int SEA_VOID_TOP = 1, SEA_VOID_BOTTOM = 4, SEA_LEDGE = 5;
	private static final int[][] SEA_PILLARS = {{5, 8}, {13, 8}, {7, 10}, {11, 10}, {4, 12}, {14, 12}};

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

		sea = DemonHooks.lord == DemonLord.LEVIATHAN;
		if (sea) {
			for (int y = SEA_VOID_TOP; y <= SEA_VOID_BOTTOM; y++) {
				for (int x = 1; x < W - 1; x++) map[y * W + x] = Terrain.CHASM;
			}
			for (int y = SEA_LEDGE; y <= BOTTOM; y++) {
				for (int x = LEFT; x <= RIGHT; x++) {
					map[y * W + x] = Random.Int(6) == 0 ? Terrain.EMPTY_DECO : Terrain.EMPTY;
				}
			}
			for (int[] p : SEA_PILLARS) map[p[1] * W + p[0]] = Terrain.STATUE;
			map[portalCell()] = DemonHooks.stage == DemonHooks.DEFEATED ? Terrain.ENTRANCE : Terrain.LOCKED_EXIT;
			transitions.add(new LevelTransition(this, portalCell(), LevelTransition.Type.BRANCH_ENTRANCE,
					DemonHooks.ARENA_DEPTH, 0, LevelTransition.Type.REGULAR_EXIT));
			return true;
		}

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
			//its head over the abyss, on the middle of the ledge
			LeviathanBoss lev = new LeviathanBoss();
			lev.pos = (sea ? SEA_LEDGE : TOP) * W + W / 2;
			lev.state = lev.WANDERING;
			mobs.add(lev);
			return;
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
	public void buildFlagMaps() {
		super.buildFlagMaps();
		if (sea) for (int i = 0; i < length(); i++) voidCell(i);
	}

	@Override
	public void updateCellFlags(int cell) {
		super.updateCellFlags(cell);
		if (sea) voidCell(cell);
	}

	/** the abyss can be seen across but never entered or fallen into */
	private void voidCell(int i) {
		if (map[i] == Terrain.CHASM) {
			passable[i] = avoid[i] = false;
			pit[i] = false;
		}
	}

	@Override
	public void storeInBundle(com.watabou.utils.Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("sea", sea);
	}

	@Override
	public void restoreFromBundle(com.watabou.utils.Bundle bundle) {
		//before super: it builds the flag maps
		sea = bundle.getBoolean("sea");
		super.restoreFromBundle(bundle);
	}

	@Override
	public String tileName(int tile) {
		if (tile == Terrain.EMBERS) return Messages.get(DemonArenaLevel.class, "embers_name");
		if (sea && tile == Terrain.CHASM) return Messages.get(DemonArenaLevel.class, "abyss_name");
		return super.tileName(tile);
	}

	@Override
	public String tileDesc(int tile) {
		if (sea && tile == Terrain.CHASM) return Messages.get(DemonArenaLevel.class, "abyss_desc");
		return super.tileDesc(tile);
	}
}
