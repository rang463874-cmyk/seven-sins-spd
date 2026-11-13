package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.demons.DemonLord;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.LeviathanFx;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.DemonSprites;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Leviathan's true form (owner's rework, Nov 14 2026): only its head rises out of the abyss at the far side of
 * its arena (DemonArenaLevel's "sea" layout), like the holy giant over its void. It never moves and has three
 * telegraphed attacks:
 *  - LASER: it opens its mouth, and a turn later a beam burns the straight line through where the target stood,
 *    as far as the first pillar or wall (three tiles wide once enraged);
 *  - TSUNAMI: a tidal wave rolls from the left edge to the right over the whole floor, a few columns per turn.
 *    Only the two tiles right behind a pillar stay dry;
 *  - DROPS: water falls on marked tiles all over the floor.
 * Saves from before the rework had a wall form with LeviathanCoil pieces: those pieces are cleared on its first
 * turn, and the attacks work from wherever the head is.
 */
public class LeviathanBoss extends DemonLordBoss {

	{
		lord = DemonLord.LEVIATHAN;
		spriteClass = DemonSprites.LeviathanHead.class;
		properties.add(Property.IMMOVABLE);
		//it watches its whole arena
		viewDistance = 20;
	}

	private static final int LASER = 0, TSUNAMI = 1, DROPS = 2;

	/** columns the tidal wave covers each turn */
	private static final int WAVE_STEP = 3;
	/** tiles right behind a pillar that the wave leaves dry */
	private static final int SHELTER = 2;

	@Override
	public CharSprite sprite() {
		return new DemonSprites.LeviathanHead();
	}

	/** the mouth is open while the laser is charging */
	public boolean charging() {
		return skill == LASER;
	}

	// ------------------------------------------------------------------ it never moves

	@Override
	protected boolean getCloser(int target) {
		return false;
	}

	@Override
	protected boolean getFurther(int target) {
		return false;
	}

	@Override
	protected boolean act() {
		//saves from before the rework: the body pieces along the wall are gone
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (m instanceof LeviathanCoil && ((LeviathanCoil) m).head == id()) ((LeviathanCoil) m).vanish();
		}
		boolean result = super.act();
		syncMouth();
		return result;
	}

	private void syncMouth() {
		if (sprite instanceof DemonSprites.LeviathanHead) ((DemonSprites.LeviathanHead) sprite).open(charging());
	}

	// ------------------------------------------------------------------ skills

	@Override
	protected int skillCount() {
		return 3;
	}

	@Override
	protected String skillKey(int skill) {
		switch (skill) {
			case LASER:   return "laser";
			case TSUNAMI: return "tsunami";
			default:      return "drops";
		}
	}

	@Override
	protected int cooldown(int skill) {
		int cd;
		switch (skill) {
			case LASER:   cd = 3; break;
			case TSUNAMI: cd = 6; break;
			default:      cd = 3; break;
		}
		return enraged ? Math.max(2, cd / 2) : cd;
	}

	private boolean floor(int c) {
		Level l = Dungeon.level;
		return c >= 0 && c < l.length() && l.passable[c] && !l.solid[c] && !l.pit[c];
	}

	/** the tidal wave: bands of columns from the left edge to the right; the tiles right behind a pillar stay dry */
	@Override
	protected int[][] prepareWaves(int skill, Char target) {
		if (skill != TSUNAMI) return null;
		Level l = Dungeon.level;
		int w = l.width(), h = l.height();
		//which tiles the wave reaches, row by row
		boolean[] wet = new boolean[l.length()];
		for (int y = 1; y < h - 1; y++) {
			boolean started = false;
			int cover = 0;
			for (int x = 1; x < w - 1; x++) {
				int c = x + y * w;
				if (floor(c)) {
					started = true;
					if (cover > 0) cover--;
					else if (c != pos) wet[c] = true;
				} else if (started && l.solid[c]) {
					cover = SHELTER;
				}
			}
		}
		ArrayList<int[]> waves = new ArrayList<>();
		for (int x0 = 1; x0 < w - 1; x0 += WAVE_STEP) {
			ArrayList<Integer> band = new ArrayList<>();
			for (int x = x0; x < Math.min(w - 1, x0 + WAVE_STEP); x++) {
				for (int y = 1; y < h - 1; y++) if (wet[x + y * w]) band.add(x + y * w);
			}
			if (!band.isEmpty()) waves.add(toArray(band));
		}
		return waves.toArray(new int[0][]);
	}

	@Override
	protected int[] prepare(int skill, Char target) {
		Level l = Dungeon.level;
		switch (skill) {
			case LASER: {
				if (target.pos == pos) return null;
				Ballistica b = new Ballistica(pos, target.pos, Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
				ArrayList<Integer> cells = new ArrayList<>();
				for (int i = 1; i <= b.dist; i++) {
					int c = b.path.get(i);
					addIfOpen(cells, c);
					if (enraged) {
						if (c % l.width() > 0) addIfOpen(cells, c - 1);
						if (c % l.width() < l.width() - 1) addIfOpen(cells, c + 1);
					}
				}
				cells.remove((Integer) pos);
				if (cells.isEmpty()) return null;
				syncMouthSoon();
				return toArray(cells);
			}
			case DROPS: {
				ArrayList<Integer> open = new ArrayList<>();
				for (int c = 0; c < l.length(); c++) if (floor(c) && c != pos) open.add(c);
				if (open.isEmpty()) return null;
				ArrayList<Integer> cells = new ArrayList<>();
				//one on the target, the rest anywhere
				if (floor(target.pos)) cells.add(target.pos);
				int n = Math.min(open.size(), enraged ? 24 : 16);
				int guard = 0;
				while (cells.size() < n && guard++ < 200) {
					int c = Random.element(open);
					if (!cells.contains(c)) cells.add(c);
				}
				return toArray(cells);
			}
			default:
				return null;
		}
	}

	/** prepare() runs before the skill is recorded, so the mouth opens right after */
	private void syncMouthSoon() {
		if (sprite instanceof DemonSprites.LeviathanHead) ((DemonSprites.LeviathanHead) sprite).open(true);
	}

	@Override
	protected void release(int skill, int[] cells) {
		Level l = Dungeon.level;
		int w = l.width();
		switch (skill) {
			case LASER: {
				int far = pos;
				for (int c : cells) if (l.trueDistance(pos, c) > l.trueDistance(pos, far)) far = c;
				LeviathanFx.beam(pos, far, enraged);
				for (int c : cells) {
					CellEmitter.center(c).burst(Speck.factory(Speck.LIGHT), 2);
					strike(c, 20, 36);
				}
				GameScene.flash(0x4040E0FF);
				Sample.INSTANCE.play(Assets.Sounds.RAY);
				syncMouth();
				break;
			}
			case TSUNAMI: {
				if (cells.length == 0) break;
				int left = w, right = 0, top = l.height(), bottom = 0;
				for (int c : cells) {
					left = Math.min(left, c % w);
					right = Math.max(right, c % w);
				}
				//the picture spans the whole floor, not just this band's rows
				for (int c = 0; c < l.length(); c++) {
					if (floor(c)) {
						top = Math.min(top, c / w);
						bottom = Math.max(bottom, c / w);
					}
				}
				LeviathanFx.wave(left, right, top, bottom);
				for (int c : cells) {
					if (Random.Int(3) == 0) Splash.at(c, 0x3A7BD5, 4);
					strike(c, 16, 28);
				}
				Sample.INSTANCE.play(Assets.Sounds.WATER, 1f, 0.8f);
				break;
			}
			default: {
				for (int c : cells) {
					LeviathanFx.drop(c);
					Splash.at(c, 0x6AB8F0, 6);
					strike(c, 10, 20);
				}
				Sample.INSTANCE.play(Assets.Sounds.WATER, 1f, 1.3f);
				break;
			}
		}
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		lord = DemonLord.LEVIATHAN;
	}
}
