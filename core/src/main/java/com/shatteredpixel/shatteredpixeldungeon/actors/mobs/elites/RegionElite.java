package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.elites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.elites.EliteHooks;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * A region's named elite (one per region, see EliteHooks). Uses the same telegraph pattern as the
 * Seal Warden: a skill marks cells (or just announces itself), and is released on the elite's next
 * turn, so stepping out of the marks avoids it. Subclasses provide the skills.
 * Drops two sin essences and a guaranteed piece of equipment.
 */
public abstract class RegionElite extends Mob {

	{
		HUNTING = new Hunting();
		properties.add(Property.MINIBOSS);
	}

	protected static final int NONE = -1;

	protected int skill = NONE;
	protected int[] marked = new int[0];
	protected float skillCooldown = 2;
	private boolean greeted = false;

	/** how many skills this elite has; ids 0..n-1 */
	protected abstract int skillCount();

	/** marks for a skill against the target; null = can't use it now, empty = no marks (self skill) */
	protected abstract int[] prepare(int skill, Char target);

	protected abstract void release(int skill, int[] cells);

	protected abstract int cooldown(int skill);

	protected abstract String skillKey(int skill);

	/** whether this one drops the region reward when it dies (twins: only the last one) */
	protected boolean dropsReward() {
		return true;
	}

	@Override
	protected boolean act() {
		if (skill != NONE && (paralysed > 0 || state != HUNTING)) {
			skill = NONE;
			marked = new int[0];
		}
		if (paralysed <= 0 && skillCooldown > 0) skillCooldown--;
		return super.act();
	}

	protected class Hunting extends Mob.Hunting {
		@Override
		public boolean act(boolean enemyInFOV, boolean justAlerted) {
			if (skill != NONE) {
				int s = skill;
				int[] cells = marked;
				skill = NONE;
				marked = new int[0];
				skillCooldown = cooldown(s);
				release(s, cells);
				spend(TICK);
				return true;
			}
			if (skillCooldown <= 0 && enemyInFOV && enemy != null && !isCharmedBy(enemy)) {
				int start = Random.Int(skillCount());
				for (int i = 0; i < skillCount(); i++) {
					int s = (start + i) % skillCount();
					int[] cells = prepare(s, enemy);
					if (cells == null) continue;
					skill = s;
					marked = cells;
					for (int c : cells) GameScene.targetedCell(c, 1f);
					if (sprite != null) {
						sprite.turnTo(pos, enemy.pos);
						sprite.showStatus(CharSprite.WARNING, Messages.get(RegionElite.this, "skill_" + skillKey(s)));
					}
					Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 0.6f, 1.3f);
					spend(TICK);
					return true;
				}
			}
			return super.act(enemyInFOV, justAlerted);
		}
	}

	// ---- helpers for subclasses ----

	protected void addIfOpen(ArrayList<Integer> cells, int c) {
		if (c >= 0 && c < Dungeon.level.length() && !Dungeon.level.solid[c] && !cells.contains(c)) cells.add(c);
	}

	// ---- telegraph shapes (Nov 2026, owner: no more 3x3 squares centred on the hero) ----

	/** an X: both diagonals through the centre, r cells each way (the centre included) */
	protected int[] xCross(int center, int r) {
		ArrayList<Integer> cells = new ArrayList<>();
		addIfOpen(cells, center);
		int w = Dungeon.level.width();
		int cx = center % w, cy = center / w;
		for (int i = 1; i <= r; i++) {
			for (int[] d : new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
				int x = cx + d[0] * i, y = cy + d[1] * i;
				if (inside(x, y)) addIfOpen(cells, x + y * w);
			}
		}
		cells.remove((Integer) pos);
		return toArray(cells);
	}

	/** the whole row and column through the centre, r cells each way */
	protected int[] crossLines(int center, int r) {
		ArrayList<Integer> cells = new ArrayList<>();
		addIfOpen(cells, center);
		int w = Dungeon.level.width();
		int cx = center % w, cy = center / w;
		for (int i = 1; i <= r; i++) {
			for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
				int x = cx + d[0] * i, y = cy + d[1] * i;
				if (inside(x, y)) addIfOpen(cells, x + y * w);
			}
		}
		cells.remove((Integer) pos);
		return toArray(cells);
	}

	/** every other tile (the centre's colour, like a chessboard) within r: a step straight sideways is safe */
	protected int[] checker(int center, int r) {
		ArrayList<Integer> cells = new ArrayList<>();
		int w = Dungeon.level.width();
		int cx = center % w, cy = center / w;
		for (int dy = -r; dy <= r; dy++) {
			for (int dx = -r; dx <= r; dx++) {
				if (((dx + dy) & 1) != 0) continue;
				int x = cx + dx, y = cy + dy;
				if (inside(x, y)) addIfOpen(cells, x + y * w);
			}
		}
		cells.remove((Integer) pos);
		return toArray(cells);
	}

	/** the centre plus n random open tiles within r (a rain of strikes) */
	protected int[] scatter(int center, int r, int n) {
		ArrayList<Integer> cells = new ArrayList<>();
		addIfOpen(cells, center);
		ArrayList<Integer> pool = new ArrayList<>();
		for (int c = 0; c < Dungeon.level.length(); c++) {
			if (c != center && c != pos && Dungeon.level.distance(center, c) <= r) addIfOpen(pool, c);
		}
		while (n-- > 0 && !pool.isEmpty()) cells.add(pool.remove(Random.Int(pool.size())));
		cells.remove((Integer) pos);
		return toArray(cells);
	}

	/** a fan from this mob toward the target: up to len tiles out, within about 35 degrees of the line */
	protected int[] cone(int target, int len) {
		ArrayList<Integer> cells = new ArrayList<>();
		int w = Dungeon.level.width();
		double ax = target % w - pos % w, ay = target / w - pos / w;
		double alen = Math.hypot(ax, ay);
		if (alen == 0) return toArray(cells);
		double cosMax = Math.cos(Math.toRadians(35));
		for (int c = 0; c < Dungeon.level.length(); c++) {
			if (c == pos || Dungeon.level.distance(pos, c) > len) continue;
			double bx = c % w - pos % w, by = c / w - pos / w;
			double blen = Math.hypot(bx, by);
			if ((ax * bx + ay * by) / (alen * blen) < cosMax) continue;
			//walls block the fan
			com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica b = new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(
					pos, c, com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.STOP_SOLID | com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.IGNORE_SOFT_SOLID);
			int at = b.path.indexOf(c);
			if (at < 0 || at > b.dist) continue;
			addIfOpen(cells, c);
		}
		return toArray(cells);
	}

	/** a ring around this mob: tiles at distance rIn..rOut (stand close or far to be safe) */
	protected int[] donut(int rIn, int rOut) {
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c = 0; c < Dungeon.level.length(); c++) {
			int d = Dungeon.level.distance(pos, c);
			if (d >= rIn && d <= rOut) addIfOpen(cells, c);
		}
		return toArray(cells);
	}

	private static boolean inside(int x, int y) {
		return x >= 0 && y >= 0 && x < Dungeon.level.width() && y < Dungeon.level.height();
	}

	protected static int[] toArray(ArrayList<Integer> list) {
		int[] out = new int[list.size()];
		for (int i = 0; i < out.length; i++) out[i] = list.get(i);
		return out;
	}

	/** hits whoever stands on a cell (not this elite's allies) for min..max minus armor; returns them */
	protected Char strike(int cell, int min, int max) {
		Char ch = Actor.findChar(cell);
		if (ch == null || ch == this || ch.alignment == alignment) return null;
		int dmg = Math.max(0, Random.NormalIntRange(min, max) - ch.drRoll());
		ch.damage(dmg, this);
		if (ch.sprite != null) ch.sprite.flash();
		if (ch == Dungeon.hero && !ch.isAlive()) {
			Dungeon.fail(this);
			GLog.n(Messages.get(RegionElite.class, "skill_kill", name()));
		}
		return ch;
	}

	@Override
	public void notice() {
		super.notice();
		if (!greeted) {
			greeted = true;
			yell(noticeLine());
		}
	}

	/** what it shouts the first time it notices the hero */
	protected String noticeLine() {
		return Messages.get(this, "notice");
	}

	@Override
	public void die(Object cause) {
		super.die(cause);
		if (dropsReward()) EliteHooks.regionReward(pos);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("skill", skill);
		bundle.put("marked", marked);
		bundle.put("skill_cd", skillCooldown);
		bundle.put("greeted", greeted);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		skill = bundle.getInt("skill");
		marked = bundle.getIntArray("marked");
		if (marked == null) marked = new int[0];
		skillCooldown = bundle.getFloat("skill_cd");
		greeted = bundle.getBoolean("greeted");
	}


	/**
	 * Seven Sins: the big fights soften heavy blows like the vanilla bosses' damage caps (Pylon, demon
	 * spawner): damage past the threshold only adds about sqrt(2 x excess).
	 */
	public static int soften(int dmg, int threshold) {
		if (dmg <= threshold) return dmg;
		return threshold + (int) (Math.sqrt(8 * (dmg - threshold) + 1) - 1) / 2;
	}

	/** the threshold for a boss of this many max HP (1/12 of it, at least 12) */
	public static int softThreshold(int ht) {
		return Math.max(12, ht / 12);
	}
}
