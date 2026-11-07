package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.elites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.RatSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Sewers (floors 2-4): the rat chieftain. Squeals for help (two rats, at most four alive) and rushes
 * in a straight line through the hero. Its bites make you bleed.
 */
public class RatChieftain extends RegionElite {

	private static final int SQUEAL = 0, RUSH = 1;

	{
		spriteClass = ChieftainSprite.class;
		HP = HT = 55;
		defenseSkill = 6;
		EXP = 14;
		maxLvl = 12;
	}

	@Override public int damageRoll() { return Random.NormalIntRange(2, 6); }
	@Override public int attackSkill(Char target) { return 12; }
	@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 2); }

	@Override
	public int attackProc(Char enemy, int damage) {
		damage = super.attackProc(enemy, damage);
		if (damage > 0 && Random.Int(3) == 0) Buff.affect(enemy, Bleeding.class).set(2f);
		return damage;
	}

	@Override protected int skillCount() { return 2; }
	@Override protected int cooldown(int s) { return s == SQUEAL ? 8 : 5; }
	@Override protected String skillKey(int s) { return s == SQUEAL ? "squeal" : "rush"; }

	private int ratsAlive() {
		int n = 0;
		for (Mob m : Dungeon.level.mobs) if (m instanceof Rat && m.isAlive() && m.alignment == alignment) n++;
		return n;
	}

	@Override
	protected int[] prepare(int s, Char target) {
		if (s == SQUEAL) {
			return ratsAlive() < 4 ? new int[0] : null;
		}
		if (rooted || Dungeon.level.distance(pos, target.pos) < 2) return null;
		ArrayList<Integer> cells = new ArrayList<>();
		Ballistica line = new Ballistica(pos, target.pos, Ballistica.STOP_SOLID);
		boolean through = false;
		for (int i = 1; i < line.path.size() && cells.size() < 6; i++) {
			int c = line.path.get(i);
			if (!Dungeon.level.passable[c] || Dungeon.level.solid[c]) break;
			cells.add(c);
			if (c == target.pos) through = true;
		}
		return through ? toArray(cells) : null;
	}

	@Override
	protected void release(int s, int[] cells) {
		if (s == SQUEAL) {
			int made = 0;
			ArrayList<Integer> spots = new ArrayList<>();
			for (int n : PathFinder.NEIGHBOURS8) {
				int c = pos + n;
				if (Dungeon.level.passable[c] && Actor.findChar(c) == null) spots.add(c);
			}
			while (made < 2 && !spots.isEmpty() && ratsAlive() < 4) {
				int c = spots.remove(Random.Int(spots.size()));
				Rat r = new Rat();
				r.pos = c;
				r.state = r.HUNTING;
				GameScene.add(r);
				CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 4);
				made++;
			}
			Sample.INSTANCE.play(Assets.Sounds.MIMIC, 0.8f, 1.6f);
			return;
		}
		//rush: run to the last free marked cell, biting everyone on the way
		int landing = -1;
		for (int i = cells.length - 1; i >= 0; i--) {
			if (Dungeon.level.passable[cells[i]] && Actor.findChar(cells[i]) == null) { landing = cells[i]; break; }
		}
		if (landing == -1 || rooted) return;
		for (int c : cells) {
			CellEmitter.get(c).burst(BloodParticle.BURST, 2);
			Char ch = strike(c, 3, 7);
			if (ch != null && ch.isAlive()) Buff.prolong(ch, Cripple.class, 2f);
			if (c == landing) break;
		}
		int from = pos;
		pos = landing;
		Actor.add(new Pushing(this, from, landing));
		Dungeon.level.occupyCell(this);
		Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
	}

	public static class ChieftainSprite extends RatSprite {
		@Override
		public void resetColor() {
			super.resetColor();
			hardlight(1f, 0.75f, 0.45f);
		}

		@Override
		public void link(Char ch) {
			super.link(ch);
			hardlight(1f, 0.75f, 0.45f);
		}
	}
}
