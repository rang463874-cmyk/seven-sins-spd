package com.shatteredpixel.shatteredpixeldungeon.items.bombs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

/**
 * Bomb of seven sins (alchemy: bomb + sin essence). A normal blast, then every enemy within 2 tiles
 * suffers one random sin: charm, weakness, vulnerability, slow, cripple, terror or magical sleep.
 */
public class SinBomb extends Bomb {

	{
		image = ItemSpriteSheet.SIN_BOMB;
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
			Char ch = Actor.findChar(i);
			if (ch == null || !ch.isAlive() || ch.alignment != Char.Alignment.ENEMY) continue;
			CellEmitter.get(i).burst(ShadowParticle.UP, 5);
			switch (Random.Int(7)) {
				case 0:
					Charm c = Buff.affect(ch, Charm.class, 6f);
					if (c != null && Dungeon.hero != null) c.object = Dungeon.hero.id();
					break;
				case 1: Buff.prolong(ch, Weakness.class, 10f); break;
				case 2: Buff.prolong(ch, Vulnerable.class, 10f); break;
				case 3: Buff.prolong(ch, Slow.class, 6f); break;
				case 4: Buff.prolong(ch, Cripple.class, 10f); break;
				case 5:
					Terror t = Buff.affect(ch, Terror.class, 6f);
					if (t != null && Dungeon.hero != null) t.object = Dungeon.hero.id();
					break;
				default: Buff.affect(ch, MagicalSleep.class); break;
			}
		}
	}

	@Override
	protected void onThrow(int cell) {
		//the glutton tastes the seven sins' bombs as they leave its hand
		if (curUser != null) com.shatteredpixel.shatteredpixeldungeon.predator.PredatorHooks.onConsumed(curUser, this);
		super.onThrow(cell);
	}

	@Override
	public int value() {
		return quantity * (20 + 50);
	}
}
