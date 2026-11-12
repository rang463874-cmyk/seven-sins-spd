package com.shatteredpixel.shatteredpixeldungeon.tiles.custom;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.watabou.noosa.Tilemap;

/**
 * Seven Sins: the heavens' chasm (floors 27-31). One continuous night sky (the owner's picture, made by
 * tools/sinsart/make_heaven_sky.py) drawn over every chasm tile of the level, picked by the tile's position so
 * neighbouring chasm tiles join up; a chasm tile right under a floor gets the shadowed copy.
 */
public class HeavenSky extends CustomTilemap {

	/** the sheet is SKY_W x SKY_H tiles of sky, then the same again with a shadow along the top */
	public static final int SKY_W = 26, SKY_H = 7;

	{
		texture = Assets.Environment.HEAVEN_SKY;
	}

	public static void addTo(Level level) {
		for (CustomTilemap t : level.customTiles) {
			if (t instanceof HeavenSky) return;
		}
		HeavenSky sky = new HeavenSky();
		sky.setRect(0, 0, level.width(), level.height());
		//first, so carpets and the like draw over it
		level.customTiles.add(0, sky);
	}

	@Override
	public Tilemap create() {
		Tilemap v = super.create();
		Level level = Dungeon.level;
		int[] data = new int[tileW * tileH];
		for (int y = 0; y < tileH; y++) {
			for (int x = 0; x < tileW; x++) {
				int i = x + y * tileW;
				int cell = (tileX + x) + (tileY + y) * level.width();
				if (cell < 0 || cell >= level.length() || level.map[cell] != Terrain.CHASM) {
					data[i] = -1;
					continue;
				}
				int above = cell - level.width();
				boolean shadow = above >= 0 && level.map[above] != Terrain.CHASM;
				int lx = tileX + x, ly = tileY + y;
				data[i] = (ly % SKY_H) * SKY_W + (lx % SKY_W) + (shadow ? SKY_W * SKY_H : 0);
			}
		}
		v.map(data, tileW);
		return v;
	}

	@Override
	public String name(int tileX, int tileY) {
		return null;
	}

	@Override
	public String desc(int tileX, int tileY) {
		return null;
	}
}
