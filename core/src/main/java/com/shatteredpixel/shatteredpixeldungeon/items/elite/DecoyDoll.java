package com.shatteredpixel.shatteredpixeldungeon.items.elite;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.PrideEffigy;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.plants.Fadeleaf;
import com.shatteredpixel.shatteredpixeldungeon.plants.Stormvine;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * Decoy doll (alchemy: stormvine seed + fadeleaf seed). Throw it onto an empty tile: it springs up into
 * a straw dummy that draws every enemy within 4 tiles to attack it for 15 turns.
 */
public class DecoyDoll extends Item {

	public static final float LIFE = 15f;

	{
		image = ItemSpriteSheet.DECOY_DOLL;
		stackable = true;
		defaultAction = AC_THROW;
	}

	@Override
	protected void onThrow(int cell) {
		if (Dungeon.level.passable[cell] && Actor.findChar(cell) == null && !Dungeon.level.pit[cell]) {
			Decoy d = new Decoy();
			d.HT = d.HP = 20 + 5 * Dungeon.depth;
			d.lifespan = LIFE;
			d.pos = cell;
			GameScene.add(d);
			Dungeon.level.occupyCell(d);
			d.sprite.emitter().burst(Speck.factory(Speck.WOOL), 8);
		} else {
			super.onThrow(cell);
		}
	}

	@Override public boolean isUpgradable() { return false; }
	@Override public boolean isIdentified() { return true; }
	@Override public int value() { return 20 * quantity; }
	@Override public int energyVal() { return quantity; }

	/** the dummy: an effigy with its own name and look */
	public static class Decoy extends PrideEffigy {
		{
			spriteClass = DecoySprite.class;
		}
	}

	public static class DecoySprite extends PrideEffigy.EffigySprite {
		@Override
		public void link(Char ch) {
			super.link(ch);
			hardlight(0.9f, 0.75f, 0.45f);
		}

		@Override
		public void resetColor() {
			super.resetColor();
			hardlight(0.9f, 0.75f, 0.45f);
		}
	}

	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe.SimpleRecipe {
		{
			inputs = new Class[]{Stormvine.Seed.class, Fadeleaf.Seed.class};
			inQuantity = new int[]{1, 1};
			cost = 2;
			output = DecoyDoll.class;
			outQuantity = 2;
		}
	}
}
