package com.shatteredpixel.shatteredpixeldungeon.items.elite;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Firebloom;
import com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

/**
 * Drying powder (alchemy: firebloom seed + swiftthistle seed). Dries every water tile within
 * {@link #RADIUS} of where it lands into bare floor, for good.
 */
public class DryingPowder extends TerrainCharm {

	public static final int RADIUS = 2;

	{
		image = ItemSpriteSheet.DRYING_POWDER;
	}

	@Override
	protected void land(int cell) {
		Level level = Dungeon.level;
		int w = level.width(), cx = cell % w, cy = cell / w;
		int dried = 0;
		for (int y = cy - RADIUS; y <= cy + RADIUS; y++) {
			for (int x = cx - RADIUS; x <= cx + RADIUS; x++) {
				if (x < 0 || y < 0 || x >= w || y >= level.height()) continue;
				int c = x + y * w;
				if (level.map[c] != Terrain.WATER) continue;
				Level.set(c, Terrain.EMPTY);
				GameScene.updateMap(c);
				CellEmitter.get(c).burst(Speck.factory(Speck.STEAM), 3);
				dried++;
			}
		}
		CellEmitter.get(cell).burst(Speck.factory(Speck.DUST), 6);
		Sample.INSTANCE.play(Assets.Sounds.PUFF);
		if (dried > 0) {
			Dungeon.observe();
		} else {
			GLog.i(Messages.get(this, "no_water"));
		}
	}

	@Override public int value() { return 15 * quantity; }

	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe.SimpleRecipe {
		{
			inputs = new Class[]{Firebloom.Seed.class, Swiftthistle.Seed.class};
			inQuantity = new int[]{1, 1};
			cost = 1;
			output = DryingPowder.class;
			outQuantity = 2;
		}
	}
}
