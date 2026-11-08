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

/** Paladin of diligence (nemesis of sloth): never sleeps, charges through the hero, and quickens its pace. */
public class DiligenceChampion extends VirtueChampion {
	private static final int CHARGE = 0, VIGIL = 1;
	{
		immunities.add(Sleep.class);
		immunities.add(MagicalSleep.class);
		immunities.add(Paralysis.class);
	}
	{ spriteClass = com.shatteredpixel.shatteredpixeldungeon.sprites.VirtueSprites.Diligence.class; }
	@Override public Virtue virtue() { return Virtue.DILIGENCE; }
	@Override protected int skillCount() { return 2; }
	@Override protected int cooldown(int s) { return s == CHARGE ? 5 : 9; }
	@Override protected String skillKey(int s) { return s == CHARGE ? "charge" : "vigil"; }

	@Override
	protected int[] prepare(int s, Char target) {
		if (s == VIGIL) return buff(Haste.class) == null ? new int[0] : null;
		if (rooted || dist(target) < 2 || dist(target) > 6) return null;
		ArrayList<Integer> cells = new ArrayList<>();
		Ballistica line = new Ballistica(pos, target.pos, Ballistica.STOP_SOLID);
		boolean through = false;
		for (int i = 1; i < line.path.size() && cells.size() < 7; i++) {
			int c = line.path.get(i);
			if (!Dungeon.level.passable[c] || Dungeon.level.solid[c] || !canReach(c)) break;
			cells.add(c);
			if (c == target.pos) through = true;
		}
		return through ? toArray(cells) : null;
	}

	@Override
	protected void release(int s, int[] cells) {
		if (s == VIGIL) {
			Buff.prolong(this, Haste.class, 4f);
			if (sprite != null) sprite.emitter().burst(Speck.factory(Speck.JET), 6);
			Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
			return;
		}
		int landing = -1;
		for (int i = cells.length - 1; i >= 0; i--) {
			if (Dungeon.level.passable[cells[i]] && Actor.findChar(cells[i]) == null) { landing = cells[i]; break; }
		}
		if (landing == -1 || rooted) return;
		for (int c : cells) {
			CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 2);
			Char ch = strike(c, sMin(), sMax());
			if (ch != null && ch.isAlive()) Buff.prolong(ch, Cripple.class, 2f);
			if (c == landing) break;
		}
		int from = pos;
		pos = landing;
		Actor.add(new Pushing(this, from, landing));
		Dungeon.level.occupyCell(this);
		Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
	}
}
