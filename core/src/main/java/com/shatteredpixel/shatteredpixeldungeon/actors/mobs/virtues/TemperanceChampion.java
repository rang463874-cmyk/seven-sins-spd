package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.virtues;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.virtues.Virtue;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

/** Paladin of temperance (nemesis of gluttony): blows that empty the stomach and measured strikes that cripple. */
public class TemperanceChampion extends VirtueChampion {
	private static final int FAST = 0, MEASURE = 1;
	{ spriteClass = com.shatteredpixel.shatteredpixeldungeon.sprites.VirtueSprites.Temperance.class; }
	@Override public Virtue virtue() { return Virtue.TEMPERANCE; }
	@Override protected int skillCount() { return 2; }
	@Override protected int cooldown(int s) { return s == FAST ? 5 : 4; }
	@Override protected String skillKey(int s) { return s == FAST ? "fast" : "measure"; }

	@Override
	protected int[] prepare(int s, Char target) {
		if (dist(target) > 6) return null;
		return s == FAST ? xCross(target.pos, 2) : plus(target.pos);
	}

	@Override
	protected void release(int s, int[] cells) {
		for (int c : cells) {
			CellEmitter.center(c).burst(Speck.factory(Speck.HEALING), 2);
			Char ch = strike(c, sMin(), sMax());
			if (ch == null || !ch.isAlive()) continue;
			if (s == FAST) {
				Hunger h = ch.buff(Hunger.class);
				if (h != null) h.affectHunger(-80);
			} else {
				Buff.prolong(ch, Cripple.class, 4f);
			}
		}
		Sample.INSTANCE.play(Assets.Sounds.HIT_MAGIC);
	}
}
