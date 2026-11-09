package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.heaven.HolyGiant;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Random;

/**
 * Seven Sins: floor 31, the holy giant's arena. The stairs from floor 30 open onto a wide hall (city
 * tiles) scattered with wall blocks to hide behind; its far side drops into a dark void (a chasm you
 * can neither walk into nor be thrown into) from which the giant raises its upper body at the edge.
 */
public class HolyGiantLevel extends Level {

	public static final int W = 29, H = 25;
	/** floor: x 2..26, y VOID_BOTTOM+1 .. 22 */
	public static final int LEFT = 2, RIGHT = 26, BOTTOM = 22;
	/** the void: rows 1..5 */
	public static final int VOID_TOP = 1, VOID_BOTTOM = 5;
	public static final int LEDGE = VOID_BOTTOM + 1;

	{
		color1 = 0x2a3a66;
		color2 = 0x9fd8ff;
		viewDistance = 9;
	}

	public static int giantCell() {
		return LEDGE * W + W / 2;
	}

	public static int centerCell() {
		return ((LEDGE + BOTTOM) / 2 + 1) * W + W / 2;
	}

	public static int entranceCell() {
		return BOTTOM * W + W / 2;
	}

	@Override
	public void playLevelMusic() {
		Music.INSTANCE.play(Assets.Music.HALLS_BOSS, true);
	}

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_HEAVEN;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_HEAVEN;
	}

	@Override
	protected boolean build() {
		setSize(W, H);
		for (int i = 0; i < length(); i++) map[i] = Terrain.WALL;

		//the void across the far side
		for (int y = VOID_TOP; y <= VOID_BOTTOM; y++) {
			for (int x = 1; x < W - 1; x++) map[y * W + x] = Terrain.CHASM;
		}
		//the hall
		for (int y = LEDGE; y <= BOTTOM; y++) {
			for (int x = LEFT; x <= RIGHT; x++) {
				map[y * W + x] = Random.Int(7) == 0 ? Terrain.EMPTY_DECO : Terrain.EMPTY_SP;
			}
		}
		//cover: 2x2 wall blocks in two staggered rows, plus single pillars
		int[][] blocks = {
				{5, 10}, {10, 10}, {17, 10}, {22, 10},
				{8, 14}, {19, 14},
				{4, 18}, {11, 18}, {16, 18}, {23, 18}
		};
		for (int[] b : blocks) {
			for (int dx = 0; dx < 2; dx++) {
				for (int dy = 0; dy < 2; dy++) {
					map[(b[1] + dy) * W + b[0] + dx] = dy == 0 ? Terrain.WALL_DECO : Terrain.WALL;
				}
			}
		}
		//keep the way from the stairs to the giant open
		for (int y = LEDGE; y <= BOTTOM; y++) {
			int c = y * W + W / 2;
			if (map[c] == Terrain.WALL || map[c] == Terrain.WALL_DECO) map[c] = Terrain.EMPTY_SP;
		}

		map[entranceCell()] = Terrain.ENTRANCE;
		transitions.add(new LevelTransition(this, entranceCell(), LevelTransition.Type.REGULAR_ENTRANCE));
		return true;
	}

	@Override
	public void buildFlagMaps() {
		super.buildFlagMaps();
		for (int i = 0; i < length(); i++) voidCell(i);
	}

	@Override
	public void updateCellFlags(int cell) {
		super.updateCellFlags(cell);
		voidCell(cell);
	}

	/** the void can be seen across but never entered or fallen into */
	private void voidCell(int i) {
		if (map[i] == Terrain.CHASM) {
			passable[i] = avoid[i] = false;
			pit[i] = false;
		}
	}

	@Override
	public Mob createMob() {
		return null;
	}

	@Override
	protected void createMobs() {
		HolyGiant giant = new HolyGiant();
		giant.pos = giantCell();
		mobs.add(giant);
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
		int tries = 0;
		while (tries++ < 100) {
			int c = (LEDGE + 2 + Random.Int(BOTTOM - LEDGE - 2)) * W + LEFT + Random.Int(RIGHT - LEFT + 1);
			if (passable[c] && Actor.findChar(c) == null) return c;
		}
		return entranceCell() - W;
	}

	@Override
	public String tileName(int tile) {
		if (tile == Terrain.CHASM) return Messages.get(HolyGiantLevel.class, "void_name");
		return super.tileName(tile);
	}

	@Override
	public String tileDesc(int tile) {
		if (tile == Terrain.CHASM) return Messages.get(HolyGiantLevel.class, "void_desc");
		return super.tileDesc(tile);
	}
}
