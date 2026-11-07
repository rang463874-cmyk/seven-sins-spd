package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.elites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.RipperSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Halls (floors 21-24): the hellhound twins, always spawned as a pair. Each pounces onto a marked
 * plus around the hero, or breathes fire down a marked line. When one dies the other is enraged
 * (+50% damage, attacks faster). Only the last one drops the reward.
 */
public class Hellhound extends RegionElite {

	private static final int POUNCE = 0, BREATH = 1;

	{
		spriteClass = HoundSprite.class;
		HP = HT = 190;
		defenseSkill = 30;
		EXP = 36;
		maxLvl = 30;
		properties.add(Property.DEMONIC);
		immunities.add(Burning.class);
	}

	/** the other twin's actor id (0 = none) */
	public int twin = 0;
	public boolean enraged = false;

	@Override
	public int damageRoll() {
		int d = Random.NormalIntRange(14, 26);
		return enraged ? Math.round(d * 1.5f) : d;
	}

	@Override public int attackSkill(Char target) { return 42; }
	@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 12); }

	@Override
	public float attackDelay() {
		return enraged ? super.attackDelay() / 1.5f : super.attackDelay();
	}

	@Override protected int skillCount() { return 2; }
	@Override protected int cooldown(int s) { return enraged ? 3 : 5; }
	@Override protected String skillKey(int s) { return s == POUNCE ? "pounce" : "breath"; }

	private Hellhound partner() {
		Actor a = twin == 0 ? null : Actor.findById(twin);
		return a instanceof Hellhound && ((Hellhound) a).isAlive() ? (Hellhound) a : null;
	}

	@Override
	protected boolean dropsReward() {
		return partner() == null;
	}

	@Override
	protected int[] prepare(int s, Char target) {
		ArrayList<Integer> cells = new ArrayList<>();
		if (s == POUNCE) {
			if (rooted || Dungeon.level.distance(pos, target.pos) < 2 || Dungeon.level.distance(pos, target.pos) > 5) return null;
			addIfOpen(cells, target.pos);
			for (int n : PathFinder.NEIGHBOURS4) addIfOpen(cells, target.pos + n);
			return toArray(cells);
		}
		int d = Dungeon.level.distance(pos, target.pos);
		if (d > 4) return null;
		com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica line =
				new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(pos, target.pos,
						com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.STOP_SOLID);
		for (int i = 1; i < line.path.size() && cells.size() < 5; i++) {
			int c = line.path.get(i);
			if (Dungeon.level.solid[c]) break;
			cells.add(c);
		}
		return cells.contains(target.pos) ? toArray(cells) : null;
	}

	@Override
	protected void release(int s, int[] cells) {
		if (s == BREATH) {
			for (int c : cells) {
				CellEmitter.get(c).burst(FlameParticle.FACTORY, 6);
				Char ch = strike(c, 12, 22);
				if (ch != null && ch.isAlive() && !Dungeon.level.water[ch.pos]) Buff.affect(ch, Burning.class).reignite(ch);
			}
			Sample.INSTANCE.play(Assets.Sounds.BURNING);
			return;
		}
		if (rooted || cells.length == 0) return;
		//land next to the centre of the marks (or on it, if it is free)
		int centre = cells[0];
		int landing = -1;
		if (Actor.findChar(centre) == null && Dungeon.level.passable[centre]) landing = centre;
		for (int i = 1; landing == -1 && i < cells.length; i++) {
			if (Actor.findChar(cells[i]) == null && Dungeon.level.passable[cells[i]]) landing = cells[i];
		}
		for (int c : cells) {
			CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 3);
			strike(c, 14, 24);
		}
		Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
		if (landing != -1) {
			final int dest = landing;
			int from = pos;
			pos = dest;
			Dungeon.level.occupyCell(this);
			if (sprite != null && sprite.visible) {
				sprite.jump(from, dest, new Callback() {
					@Override
					public void call() {
						sprite.idle();
					}
				});
			} else if (sprite != null) {
				sprite.place(dest);
			}
		}
	}

	@Override
	public void die(Object cause) {
		Hellhound p = partner();
		super.die(cause);
		if (p != null && !p.enraged) {
			p.enraged = true;
			if (p.sprite != null) {
				p.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(Hellhound.class, "enraged"));
				p.sprite.emitter().burst(FlameParticle.FACTORY, 10);
			}
			p.yell(Messages.get(Hellhound.class, "yell_enraged"));
		}
	}

	@Override
	public String description() {
		String d = super.description();
		if (enraged) d += "\n\n" + Messages.get(this, "desc_enraged");
		return d;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("twin", twin);
		bundle.put("enraged", enraged);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		twin = bundle.getInt("twin");
		enraged = bundle.getBoolean("enraged");
	}

	public static class HoundSprite extends RipperSprite {
		@Override
		public void resetColor() {
			super.resetColor();
			hardlight(1f, 0.35f, 0.2f);
		}

		@Override
		public void link(Char ch) {
			super.link(ch);
			hardlight(1f, 0.35f, 0.2f);
		}
	}
}
