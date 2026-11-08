package com.shatteredpixel.shatteredpixeldungeon.items.bombs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

/**
 * Caltrop bomb (alchemy: bomb + blindweed seed). No blast: it scatters iron caltrops over a 2-tile radius,
 * and every enemy there takes light damage, bleeds and is crippled for 8 turns. It doesn't hurt you.
 */
public class CaltropBomb extends Bomb {

	{
		image = ItemSpriteSheet.CALTROP_BOMB;
	}

	@Override
	public boolean explodesDestructively() {
		return false;
	}

	@Override
	protected int explosionRange() {
		return 2;
	}

	@Override
	public void explode(int cell) {
		super.explode(cell);
		PathFinder.buildDistanceMap(cell, BArray.not(Dungeon.level.solid, null), explosionRange());
		for (int i = 0; i < PathFinder.distance.length; i++) {
			if (PathFinder.distance[i] == Integer.MAX_VALUE) continue;
			CellEmitter.get(i).burst(Speck.factory(Speck.ROCK), 2);
			Char ch = Actor.findChar(i);
			if (ch == null || !ch.isAlive() || ch == Dungeon.hero || ch.alignment == Char.Alignment.ALLY) continue;
			ch.damage(Random.NormalIntRange(2 + Dungeon.scalingDepth() / 2, 6 + Dungeon.scalingDepth()), this);
			if (ch.isAlive()) {
				Buff.affect(ch, Bleeding.class).set(2 + Dungeon.scalingDepth() / 4f);
				Buff.prolong(ch, Cripple.class, 8f);
			}
		}
		Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH);
	}

	@Override
	protected void onThrow(int cell) {
		//the glutton tastes the seven sins' bombs as they leave its hand
		if (curUser != null) com.shatteredpixel.shatteredpixeldungeon.predator.PredatorHooks.onConsumed(curUser, this);
		super.onThrow(cell);
	}

	@Override
	public int value() {
		return quantity * (20 + 10);
	}
}
