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

/** Saint of chastity (nemesis of lust): never charmed; purifying light strips blessings, a halo blinds. */
public class ChastityChampion extends VirtueChampion {
	private static final int PURIFY = 0, HALO = 1;
	{
		immunities.add(Charm.class);
	}
	{ spriteClass = com.shatteredpixel.shatteredpixeldungeon.sprites.VirtueSprites.Chastity.class; }
	@Override public Virtue virtue() { return Virtue.CHASTITY; }
	@Override protected int skillCount() { return 2; }
	@Override protected int cooldown(int s) { return s == PURIFY ? 5 : 4; }
	@Override protected String skillKey(int s) { return s == PURIFY ? "purify" : "halo"; }

	@Override
	protected int[] prepare(int s, Char target) {
		if (s == PURIFY) {
			if (dist(target) > 6) return null;
			Ballistica b = new Ballistica(pos, target.pos, Ballistica.STOP_SOLID);
			ArrayList<Integer> cells = new ArrayList<>();
			for (int i = 1; i <= Math.min(6, b.dist); i++) addIfOpen(cells, b.path.get(i));
			return toArray(cells);
		}
		return dist(target) <= 1 ? ring() : null;
	}

	@Override
	protected void release(int s, int[] cells) {
		for (int c : cells) {
			CellEmitter.center(c).burst(Speck.factory(Speck.LIGHT), 2);
			Char ch = strike(c, sMin(), sMax());
			if (ch == null || !ch.isAlive()) continue;
			if (s == PURIFY) {
				for (Buff b : ch.buffs().toArray(new Buff[0])) {
					if (b.type == Buff.buffType.POSITIVE && b instanceof FlavourBuff) b.detach();
				}
			} else {
				Buff.prolong(ch, Blindness.class, 3f);
			}
		}
		Sample.INSTANCE.play(Assets.Sounds.HIT_MAGIC);
	}
}
