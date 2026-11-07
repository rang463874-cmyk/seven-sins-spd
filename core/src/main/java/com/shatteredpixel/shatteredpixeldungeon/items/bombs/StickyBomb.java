package com.shatteredpixel.shatteredpixeldungeon.items.bombs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Web;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.EarthParticle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.BArray;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

/**
 * Sticky bomb (alchemy: bomb + earthroot seed). Doesn't blast: it splatters glue over a 2-tile radius,
 * rooting everything there for 8 turns and leaving webs behind.
 */
public class StickyBomb extends Bomb {

	{
		image = ItemSpriteSheet.STICKY_BOMB;
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
			CellEmitter.get(i).burst(EarthParticle.FACTORY, 4);
			if (PathFinder.distance[i] <= 1) GameScene.add(Blob.seed(i, 15, Web.class));
			Char ch = Actor.findChar(i);
			if (ch != null && ch.isAlive() && ch != Dungeon.hero) Buff.prolong(ch, Roots.class, 8f);
		}
		Sample.INSTANCE.play(Assets.Sounds.TRAMPLE, 1f, 0.7f);
	}

	@Override
	public int value() {
		return quantity * (20 + 10);
	}
}
