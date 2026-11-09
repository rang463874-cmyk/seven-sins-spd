package com.shatteredpixel.shatteredpixeldungeon.items.elite;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot;
import com.shatteredpixel.shatteredpixeldungeon.plants.Icecap;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.HashMap;
import java.util.Map;

/**
 * Stone dust (alchemy: earthroot seed + icecap seed). Where it lands, a wall three tiles wide rises
 * across the throw for {@link #DURATION} turns, then crumbles back into what was there. Only on open
 * ground: never on a monster, an item, a plant, a trap or the stairs.
 */
public class StoneDust extends TerrainCharm {

	public static final int DURATION = 20;

	{
		image = ItemSpriteSheet.STONE_DUST;
	}

	@Override
	protected void land(int cell) {
		Level level = Dungeon.level;
		int[] d = direction(cell);
		//across the throw: a vertical line for a sideways throw, a horizontal one otherwise
		int step = Math.abs(d[0]) >= Math.abs(d[1]) && d[0] != 0 ? level.width() : 1;
		int raised = 0;
		for (int c : new int[]{cell, cell - step, cell + step}) {
			if (raise(level, c)) raised++;
		}
		CellEmitter.get(cell).burst(Speck.factory(Speck.DUST), 8);
		if (raised > 0) {
			Sample.INSTANCE.play(Assets.Sounds.ROCKS);
			Dungeon.observe();
			GameScene.updateFog();
		} else {
			GLog.i(Messages.get(this, "no_room"));
		}
	}

	private static boolean canRaise(Level level, int c) {
		if (c < 0 || c >= level.length() || onBorder(level, c)) return false;
		if (!level.passable[c] || level.pit[c] || level.solid[c]) return false;
		if (Actor.findChar(c) != null || level.heaps.get(c) != null) return false;
		if (level.plants.get(c) != null || level.traps.get(c) != null) return false;
		if (level.getTransition(c) != null) return false;
		int t = level.map[c];
		return t == Terrain.EMPTY || t == Terrain.EMPTY_SP || t == Terrain.EMPTY_DECO || t == Terrain.GRASS
				|| t == Terrain.HIGH_GRASS || t == Terrain.FURROWED_GRASS || t == Terrain.EMBERS
				|| t == Terrain.WATER || t == Terrain.DOOR || t == Terrain.OPEN_DOOR;
	}

	private static boolean raise(Level level, int c) {
		if (!canRaise(level, c)) return false;
		RisenWall blob = Blob.seed(c, DURATION, RisenWall.class, level);
		blob.original.put(c, level.map[c]);
		Level.set(c, Terrain.WALL);
		GameScene.updateMap(c);
		CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 4);
		return true;
	}

	@Override public int value() { return 20 * quantity; }

	/** counts the raised walls down and puts the old terrain back */
	public static class RisenWall extends Blob {

		{
			actPriority = HERO_PRIO + 1;
		}

		final HashMap<Integer, Integer> original = new HashMap<>();

		@Override
		protected void evolve() {
			Level l = Dungeon.level;
			boolean changed = false;
			for (int i = area.left; i < area.right; i++) {
				for (int j = area.top; j < area.bottom; j++) {
					int cell = i + j * l.width();
					off[cell] = cur[cell] > 0 ? cur[cell] - 1 : 0;
					volume += off[cell];
					if (off[cell] == 0 && cur[cell] > 0) {
						Integer t = original.remove(cell);
						if (l.map[cell] == Terrain.WALL) {
							Level.set(cell, t == null ? Terrain.EMPTY : t);
							GameScene.updateMap(cell);
							if (l.heroFOV[cell]) CellEmitter.get(cell).burst(Speck.factory(Speck.ROCK), 4);
							changed = true;
						}
					}
				}
			}
			if (changed) {
				Dungeon.observe();
				GameScene.updateFog();
			}
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			int[] cells = new int[original.size()], terrs = new int[original.size()];
			int i = 0;
			for (Map.Entry<Integer, Integer> e : original.entrySet()) {
				cells[i] = e.getKey();
				terrs[i++] = e.getValue();
			}
			bundle.put("orig_cells", cells);
			bundle.put("orig_terrs", terrs);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			original.clear();
			if (bundle.contains("orig_cells")) {
				int[] cells = bundle.getIntArray("orig_cells"), terrs = bundle.getIntArray("orig_terrs");
				for (int i = 0; i < cells.length && i < terrs.length; i++) original.put(cells[i], terrs[i]);
			}
		}

		@Override
		public String tileDesc() {
			return Messages.get(this, "desc");
		}
	}

	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe.SimpleRecipe {
		{
			inputs = new Class[]{Earthroot.Seed.class, Icecap.Seed.class};
			inQuantity = new int[]{1, 1};
			cost = 2;
			output = StoneDust.class;
			outQuantity = 2;
		}
	}
}
