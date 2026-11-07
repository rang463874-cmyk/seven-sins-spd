package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.elites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlastParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SmokeParticle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GnollSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Caves (floors 11-14): the gnoll foreman. Lobs bombs onto a marked 3x3 area around the hero, and
 * brings the ceiling down in a plus shape (stuns briefly).
 */
public class GnollForeman extends RegionElite {

	private static final int BOMB = 0, CAVE_IN = 1;

	{
		spriteClass = ForemanSprite.class;
		HP = HT = 160;
		defenseSkill = 16;
		EXP = 26;
		maxLvl = 22;
	}

	@Override public int damageRoll() { return Random.NormalIntRange(8, 16); }
	@Override public int attackSkill(Char target) { return 26; }
	@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 8); }

	@Override protected int skillCount() { return 2; }
	@Override protected int cooldown(int s) { return s == BOMB ? 4 : 6; }
	@Override protected String skillKey(int s) { return s == BOMB ? "bomb" : "cavein"; }

	@Override
	protected int[] prepare(int s, Char target) {
		ArrayList<Integer> cells = new ArrayList<>();
		if (s == BOMB) {
			if (Dungeon.level.distance(pos, target.pos) < 2) return null;
			addIfOpen(cells, target.pos);
			for (int n : PathFinder.NEIGHBOURS8) addIfOpen(cells, target.pos + n);
			cells.remove((Integer) pos);
		} else {
			addIfOpen(cells, target.pos);
			for (int n : PathFinder.NEIGHBOURS4) {
				addIfOpen(cells, target.pos + n);
				addIfOpen(cells, target.pos + 2 * n);
			}
			cells.remove((Integer) pos);
		}
		return toArray(cells);
	}

	@Override
	protected void release(int s, int[] cells) {
		if (s == BOMB) {
			for (int c : cells) {
				CellEmitter.center(c).burst(BlastParticle.FACTORY, 6);
				CellEmitter.get(c).burst(SmokeParticle.FACTORY, 3);
				strike(c, 10, 18);
			}
			Sample.INSTANCE.play(Assets.Sounds.BLAST);
		} else {
			for (int c : cells) {
				CellEmitter.get(c).start(Speck.factory(Speck.ROCK), 0.07f, 6);
				Char ch = strike(c, 7, 13);
				if (ch != null && ch.isAlive()) Buff.prolong(ch, Paralysis.class, 1f);
			}
			Sample.INSTANCE.play(Assets.Sounds.ROCKS);
			GameScene.shake(2, 0.5f);
		}
	}

	public static class ForemanSprite extends GnollSprite {
		@Override
		public void resetColor() {
			super.resetColor();
			hardlight(1f, 0.6f, 0.3f);
		}

		@Override
		public void link(Char ch) {
			super.link(ch);
			hardlight(1f, 0.6f, 0.3f);
		}
	}
}
