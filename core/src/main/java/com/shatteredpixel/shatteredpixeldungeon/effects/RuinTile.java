package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;

import java.util.HashMap;

/**
 * Seven Sins (Nov 20 2026, owner): a tile of the sewers broken by the ascent's chaser, glowing in the chaser's colour
 * (violet for Behemoth, platinum for Metatron). The cell itself is turned into a statue tile, so nothing can enter it;
 * this covers it. Kept per level object and rebuilt after a reload (AscentHooks).
 */
public class RuinTile extends ColorBlock {

	private static final HashMap<Integer, RuinTile> shown = new HashMap<>();
	private static Level shownLevel = null;
	private static Object shownScene = null;

	/** a jagged fissure through the middle, in a lighter shade */
	private final ColorBlock crack;
	private float time;

	private RuinTile(int cell, int color, int crackColor) {
		super(DungeonTilemap.SIZE, DungeonTilemap.SIZE, 0xFF000000 | color);
		x = (cell % levelWidth()) * DungeonTilemap.SIZE;
		y = (cell / levelWidth()) * DungeonTilemap.SIZE;
		crack = new ColorBlock(DungeonTilemap.SIZE - 6, 2, 0xFF000000 | crackColor);
		crack.x = x + 3;
		crack.y = y + 5 + (cell * 7) % 6;
		crack.angle = ((cell * 37) % 50) - 25;
		time = (cell % 11) * 0.2f;
	}

	private static int levelWidth() {
		return com.shatteredpixel.shatteredpixeldungeon.Dungeon.level.width();
	}

	@Override
	public void update() {
		super.update();
		time += Game.elapsed;
		float k = 0.5f + 0.5f * (float) Math.sin(time * 2.5f);
		alpha(0.88f + 0.12f * k);
		crack.alpha(0.45f + 0.55f * k);
	}

	@Override
	public void killAndErase() {
		crack.killAndErase();
		super.killAndErase();
	}

	/** may be called from the actor thread */
	public static void show(final Level level, final int[] cells, final int color, final int crackColor) {
		Game.runOnRenderThread(() -> {
			if (level != shownLevel || Game.scene() != shownScene) {
				for (RuinTile t : shown.values()) t.killAndErase();
				shown.clear();
				shownLevel = level;
				shownScene = Game.scene();
			}
			for (int c : cells) {
				if (shown.containsKey(c)) continue;
				RuinTile t = new RuinTile(c, color, crackColor);
				shown.put(c, t);
				GameScene.effect(t);
				GameScene.effect(t.crack);
			}
		});
	}

	/** the tiles shown belong to this level object (false after a reload or a floor change) */
	public static boolean showingFor(Level level) {
		return shownLevel == level && shownScene == Game.scene();
	}

	public static void forget() {
		Game.runOnRenderThread(() -> {
			for (RuinTile t : shown.values()) t.killAndErase();
			shown.clear();
			shownLevel = null;
			shownScene = null;
		});
	}
}
