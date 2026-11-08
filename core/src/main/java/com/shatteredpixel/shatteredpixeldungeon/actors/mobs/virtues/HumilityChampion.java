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

/** Saint of humility (nemesis of pride): humbling blows that weaken, and a veil that turns blows aside. */
public class HumilityChampion extends VirtueChampion {
	private static final int HUMBLE = 0, VEIL = 1;
	{ spriteClass = com.shatteredpixel.shatteredpixeldungeon.sprites.VirtueSprites.Humility.class; }
	@Override public Virtue virtue() { return Virtue.HUMILITY; }
	@Override protected int skillCount() { return 2; }
	@Override protected int cooldown(int s) { return s == HUMBLE ? 4 : 9; }
	@Override protected String skillKey(int s) { return s == HUMBLE ? "humble" : "veil"; }

	@Override
	protected int[] prepare(int s, Char target) {
		if (s == HUMBLE) return dist(target) <= 6 ? plus(target.pos) : null;
		return shielding() == 0 ? new int[0] : null;
	}

	@Override
	protected void release(int s, int[] cells) {
		if (s == HUMBLE) {
			for (int c : cells) {
				CellEmitter.center(c).burst(Speck.factory(Speck.LIGHT), 2);
				Char ch = strike(c, sMin(), sMax());
				if (ch != null && ch.isAlive()) Buff.prolong(ch, Weakness.class, 5f);
			}
			Sample.INSTANCE.play(Assets.Sounds.HIT_MAGIC);
		} else {
			Buff.affect(this, Barrier.class).setShield(Math.round(HT * 0.25f));
			if (sprite != null) sprite.emitter().burst(Speck.factory(Speck.LIGHT), 8);
			Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
		}
	}
}
