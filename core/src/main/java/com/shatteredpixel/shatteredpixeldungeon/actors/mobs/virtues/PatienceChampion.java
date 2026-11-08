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

/** Paladin of patience (nemesis of wrath): braces behind a great shield, then strikes back all around. */
public class PatienceChampion extends VirtueChampion {
	private static final int ENDURE = 0, RETRIBUTION = 1;
	{ spriteClass = com.shatteredpixel.shatteredpixeldungeon.sprites.VirtueSprites.Patience.class; }
	@Override public Virtue virtue() { return Virtue.PATIENCE; }
	@Override protected int skillCount() { return 2; }
	@Override protected int cooldown(int s) { return s == ENDURE ? 9 : 4; }
	@Override protected String skillKey(int s) { return s == ENDURE ? "endure" : "retribution"; }
	@Override public int drRoll() { return super.drRoll() + 2; }

	@Override
	protected int[] prepare(int s, Char target) {
		if (s == ENDURE) return shielding() == 0 && HP < HT * 0.8f ? new int[0] : null;
		return dist(target) <= 1 ? ring() : null;
	}

	@Override
	protected void release(int s, int[] cells) {
		if (s == ENDURE) {
			Buff.affect(this, Barrier.class).setShield(Math.round(HT * 0.3f));
			if (sprite != null) sprite.emitter().burst(Speck.factory(Speck.UP), 6);
			Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
			return;
		}
		for (int c : cells) {
			CellEmitter.center(c).burst(Speck.factory(Speck.STAR), 2);
			Char ch = strike(c, sMin(), sMax());
			if (ch != null && ch.isAlive() && !Char.hasProp(ch, Property.IMMOVABLE)) {
				int far = ch.pos + (ch.pos - pos) * 3;
				if (far >= 0 && far < Dungeon.level.length()) {
					WandOfBlastWave.throwChar(ch, new Ballistica(ch.pos, far, Ballistica.MAGIC_BOLT), 2, true, true, this);
				}
			}
		}
		Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
	}
}
