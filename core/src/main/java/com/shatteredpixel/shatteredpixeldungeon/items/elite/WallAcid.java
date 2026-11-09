package com.shatteredpixel.shatteredpixeldungeon.items.elite;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Firebloom;
import com.shatteredpixel.shatteredpixeldungeon.plants.Sorrowmoss;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

/**
 * Wall acid (alchemy: sorrowmoss seed + firebloom seed). Thrown at a wall, it eats away every wall
 * around where it lands and bores on in the throw direction, up to {@link #DEPTH} walls deep.
 * Never on boss floors or special levels, never the level's outer edge, never into locked vaults
 * or quest arenas.
 */
public class WallAcid extends TerrainCharm {

	public static final int DEPTH = 3;

	{
		image = ItemSpriteSheet.WALL_ACID;
	}

	@Override
	protected void land(int cell) {
		Level level = Dungeon.level;
		Sample.INSTANCE.play(Assets.Sounds.SHATTER);
		if (Dungeon.bossLevel() || !(level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel)) {
			GLog.w(Messages.get(this, "no_effect"));
			CellEmitter.get(cell).burst(Speck.factory(Speck.STEAM), 6);
			return;
		}
		int dissolved = 0;
		for (int n : PathFinder.NEIGHBOURS8) {
			if (dissolve(level, cell + n)) dissolved++;
		}
		//bore on along the throw
		int[] d = direction(cell);
		int c = cell;
		for (int i = 0; i < DEPTH; i++) {
			c += d[0] + d[1] * level.width();
			if (c < 0 || c >= level.length()) break;
			if (!isWall(level.map[c]) && i > 0) break;
			if (dissolve(level, c)) dissolved++;
		}
		CellEmitter.get(cell).burst(Speck.factory(Speck.STEAM), 8);
		if (dissolved > 0) {
			Sample.INSTANCE.play(Assets.Sounds.ROCKS);
			Dungeon.observe();
			GameScene.updateFog();
		} else {
			GLog.i(Messages.get(this, "no_wall"));
		}
	}

	private static boolean isWall(int t) {
		return t == Terrain.WALL || t == Terrain.WALL_DECO || t == Terrain.BOOKSHELF || t == Terrain.SECRET_DOOR;
	}

	private boolean dissolve(Level level, int cell) {
		if (cell < 0 || cell >= level.length() || onBorder(level, cell)) return false;
		int t = level.map[cell];
		if (!isWall(t) || protectedCell(level, cell)) return false;
		Level.set(cell, t == Terrain.SECRET_DOOR ? Terrain.DOOR : Terrain.EMPTY);
		//cells inside solid rock start out undiscoverable and would stay black (like vanilla mining)
		for (int n : PathFinder.NEIGHBOURS9) {
			int c = cell + n;
			if (c >= 0 && c < level.length()) level.discoverable[c] = true;
		}
		for (int n : PathFinder.NEIGHBOURS9) {
			int c = cell + n;
			if (c >= 0 && c < level.length()) GameScene.updateMap(c);
		}
		CellEmitter.get(cell).burst(Speck.factory(Speck.ROCK), 4);
		return true;
	}

	@Override public int value() { return 40 * quantity; }

	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe.SimpleRecipe {
		{
			inputs = new Class[]{Sorrowmoss.Seed.class, Firebloom.Seed.class};
			inQuantity = new int[]{1, 1};
			cost = 3;
			output = WallAcid.class;
			outQuantity = 1;
		}
	}
}
