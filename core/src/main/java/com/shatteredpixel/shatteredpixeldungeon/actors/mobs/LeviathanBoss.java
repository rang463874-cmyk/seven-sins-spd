package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.demons.DemonLord;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.DemonSprites;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Leviathan's true form: a gimmick boss that fills one whole wall of the arena. The head (this mob) sits
 * in the middle of the wall; the rest of the wall is its body ({@link LeviathanCoil}s), which can be hit
 * like the head. It never moves. Skills, all telegraphed:
 *  - Great Wave: floods every lane of the arena except one or two safe lanes, pushing victims away;
 *  - Bite: the 6 cells in front of the head;
 *  - Undertow: drags everyone on the hero's line back to the wall and cripples them;
 *  - Summon: two wraiths rise from the water.
 * Enraged below half HP: one safe lane only, shorter cooldowns.
 */
public class LeviathanBoss extends DemonLordBoss {

	{
		lord = DemonLord.LEVIATHAN;
		spriteClass = DemonSprites.Leviathan.class;
		properties.add(Property.IMMOVABLE);
	}

	private static final int WAVE = 0, BITE = 1, UNDERTOW = 2, SUMMON = 3;
	private static final int DEPTH = 8;

	/** step from the wall into the arena */
	public int dir = 0;
	/** the wall cells Leviathan lies on (head included), in order */
	public int[] run = new int[0];
	/** lateral step along the wall */
	public int lateral = 1;

	// ------------------------------------------------------------------ emerging

	private static boolean stops(int cell) {
		int t = Dungeon.level.map[cell];
		return Dungeon.level.solid[cell] || !Dungeon.level.passable[cell] || t == Terrain.DOOR
				|| t == Terrain.OPEN_DOOR || t == Terrain.LOCKED_DOOR || t == Terrain.CRYSTAL_DOOR;
	}

	private static boolean isDoor(int cell) {
		int t = Dungeon.level.map[cell];
		return t == Terrain.DOOR || t == Terrain.OPEN_DOOR || t == Terrain.LOCKED_DOOR || t == Terrain.CRYSTAL_DOOR;
	}

	/** a real wall behind the body (never a door, so the way out stays open) */
	private static boolean isWall(int cell) {
		return Dungeon.level.solid[cell] && !isDoor(cell);
	}

	/** finds the wall to lie along, starting from a cell inside the arena; null if there is no room */
	private static int[][] findWall(int start) {
		int w = Dungeon.level.width();
		int[] dirs = {-w, w, -1, 1};
		int[][] best = null;
		int bestScore = 0;
		for (int d : dirs) {
			int c = start;
			int guard = 0;
			while (!stops(c + d) && guard++ < 20) c += d;
			if (!isWall(c + d)) continue;
			int lat = Math.abs(d) == 1 ? w : 1;
			ArrayList<Integer> cells = new ArrayList<>();
			cells.add(c);
			for (int sgn = -1; sgn <= 1; sgn += 2) {
				int x = c;
				for (int i = 0; i < 5; i++) {
					x += sgn * lat;
					if (stops(x) || !isWall(x + d)) break;
					if (sgn < 0) cells.add(0, x); else cells.add(x);
				}
			}
			//prefer horizontal walls (the body art lies along them), then the far (top) wall
			int score = cells.size() * 10 + (Math.abs(d) == w ? 5 : 0) + (d == -w ? 1 : 0);
			if (cells.size() >= 3 && score > bestScore) {
				bestScore = score;
				int[] arr = new int[cells.size()];
				for (int i = 0; i < arr.length; i++) arr[i] = cells.get(i);
				best = new int[][]{arr, {-d, lat}};
			}
		}
		return best;
	}

	/** raises Leviathan's wall form near a cell; returns null if no wall fits (use the normal boss then) */
	public static LeviathanBoss emerge(int near) {
		int[][] wall = findWall(near);
		if (wall == null) return null;
		int[] cells = wall[0];
		int mid = cells.length / 2;
		//the head goes on the free cell nearest the middle
		int headIdx = -1;
		for (int off = 0; off <= cells.length; off++) {
			for (int sgn = -1; sgn <= 1; sgn += 2) {
				int i = mid + sgn * off;
				if (i >= 0 && i < cells.length && Actor.findChar(cells[i]) == null) { headIdx = i; break; }
			}
			if (headIdx != -1) break;
		}
		if (headIdx == -1) return null;

		LeviathanBoss boss = new LeviathanBoss();
		boss.run = cells;
		boss.dir = wall[1][0];
		boss.lateral = wall[1][1];
		boss.pos = cells[headIdx];
		GameScene.add(boss);
		CellEmitter.get(boss.pos).burst(Speck.factory(Speck.STEAM), 20);

		for (int i = 0; i < cells.length; i++) {
			if (i == headIdx || Actor.findChar(cells[i]) != null) continue;
			LeviathanCoil c = new LeviathanCoil();
			c.head = boss.id();
			c.pos = cells[i];
			c.variant = pieceAt(i, headIdx, cells.length);
			//the tail curls outward: art curls to the right, so the left side is mirrored
			c.flip = i < headIdx;
			GameScene.add(c);
			CellEmitter.get(c.pos).burst(Speck.factory(Speck.STEAM), 6);
		}
		Sample.INSTANCE.play(Assets.Sounds.WATER);
		return boss;
	}

	/** which 48px piece a wall cell shows: one piece every 3 cells from the head, the outermost being the tail */
	static int pieceAt(int i, int headIdx, int length) {
		int d = Math.abs(i - headIdx);
		int end = i < headIdx ? headIdx : length - 1 - headIdx;   //cells on this side
		int last = (end / 3) * 3;                                    //farthest regular piece
		if (end - last >= 2) last = end;                             //uncovered end: the tail goes there
		if (d == last && d > 0) return DemonSprites.Coil.TAIL;
		if (d % 3 == 0 && d > 0 && d < last) return DemonSprites.Coil.BODY;
		return DemonSprites.Coil.HIDDEN;
	}

	private ArrayList<LeviathanCoil> coils() {
		ArrayList<LeviathanCoil> out = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs) {
			if (m instanceof LeviathanCoil && ((LeviathanCoil) m).head == id()) out.add((LeviathanCoil) m);
		}
		return out;
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
	protected boolean canAttack(Char enemy) {
		if (Dungeon.level.adjacent(pos, enemy.pos)) return true;
		for (int c : run) if (Dungeon.level.adjacent(c, enemy.pos)) return true;
		return false;
	}

	// ------------------------------------------------------------------ skills

	@Override
	protected int skillCount() {
		return 4;
	}

	@Override
	protected String skillKey(int skill) {
		switch (skill) {
			case WAVE:     return "wave";
			case BITE:     return "bite";
			case UNDERTOW: return "undertow";
			default:       return "summon";
		}
	}

	@Override
	protected int cooldown(int skill) {
		int cd;
		switch (skill) {
			case WAVE:     cd = 5; break;
			case BITE:     cd = 3; break;
			case UNDERTOW: cd = 6; break;
			default:       cd = 9; break;
		}
		return enraged ? Math.max(2, cd / 2) : cd;
	}

	private int headIndex() {
		for (int i = 0; i < run.length; i++) if (run[i] == pos) return i;
		return run.length / 2;
	}

	/** how far a cell is from the wall (1 = right in front of the body), or -1 if it isn't in front of it */
	private int depthOf(int cell) {
		for (int i = 0; i < run.length; i++) {
			int c = run[i];
			for (int k = 1; k <= DEPTH; k++) {
				c += dir;
				if (c < 0 || c >= Dungeon.level.length() || Dungeon.level.solid[c]) break;
				if (c == cell) return k;
			}
		}
		return -1;
	}

	private void lane(ArrayList<Integer> cells, int i, int fromDepth, int toDepth) {
		int c = run[i];
		for (int k = 1; k <= toDepth; k++) {
			c += dir;
			if (c < 0 || c >= Dungeon.level.length() || Dungeon.level.solid[c]) break;
			if (k >= fromDepth) addIfOpen(cells, c);
		}
	}

	@Override
	protected int[] prepare(int skill, Char target) {
		int hd = depthOf(target.pos);
		switch (skill) {
			case WAVE: {
				if (hd < 1 || run.length < 3) return null;
				ArrayList<Integer> lanes = new ArrayList<>();
				for (int i = 0; i < run.length; i++) if (run[i] != pos) lanes.add(i);
				int safe = enraged ? 1 : 2;
				ArrayList<Integer> safeLanes = new ArrayList<>();
				while (safeLanes.size() < safe && !lanes.isEmpty()) {
					safeLanes.add(lanes.remove(Random.Int(lanes.size())));
				}
				ArrayList<Integer> cells = new ArrayList<>();
				for (int i = 0; i < run.length; i++) {
					if (!safeLanes.contains(i)) lane(cells, i, 1, DEPTH);
				}
				return toArray(cells);
			}
			case BITE: {
				if (Dungeon.level.distance(pos, target.pos) > 2) return null;
				ArrayList<Integer> cells = new ArrayList<>();
				int hi = headIndex();
				for (int i = hi - 1; i <= hi + 1; i++) {
					if (i >= 0 && i < run.length) lane(cells, i, 1, 2);
				}
				return toArray(cells);
			}
			case UNDERTOW: {
				if (hd < 3) return null;
				ArrayList<Integer> cells = new ArrayList<>();
				for (int i = 0; i < run.length; i++) lane(cells, i, hd, hd);
				return toArray(cells);
			}
			default:
				return minionCount() < (enraged ? 4 : 2) ? new int[0] : null;
		}
	}

	private int minionCount() {
		int n = 0;
		for (Mob m : Dungeon.level.mobs) if (m instanceof Wraith && m.alignment == Alignment.ENEMY) n++;
		return n;
	}

	@Override
	protected void release(int skill, int[] cells) {
		switch (skill) {
			case WAVE:
				for (int c : cells) {
					CellEmitter.center(c).burst(Speck.factory(Speck.STEAM), 3);
					Char ch = strike(c, 14, 26);
					if (ch != null && ch.isAlive()) push(ch, 2);
				}
				GameScene.flash(0x302060C0);
				Sample.INSTANCE.play(Assets.Sounds.WATER, 1f, 0.8f);
				break;
			case BITE:
				if (sprite != null && cells.length > 0) sprite.attack(cells[0]);
				for (int c : cells) strike(c, 18, 30);
				Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
				break;
			case UNDERTOW:
				for (int c : cells) {
					CellEmitter.center(c).burst(Speck.factory(Speck.STEAM), 2);
					Char ch = Actor.findChar(c);
					if (ch == null || ch == this || ch.alignment == alignment) continue;
					pull(ch);
					Buff.prolong(ch, Cripple.class, 3f);
				}
				Sample.INSTANCE.play(Assets.Sounds.WATER, 1f, 1.2f);
				break;
			default:
				for (int i = 0; i < 2; i++) {
					Wraith w = Wraith.spawnAt(pos + dir);
					if (w != null) w.beckon(Dungeon.hero.pos);
				}
				CellEmitter.get(pos + dir).burst(ShadowParticle.CURSE, 10);
				Sample.INSTANCE.play(Assets.Sounds.CURSED);
				break;
		}
	}

	/** pushes a character away from the wall */
	private void push(Char ch, int power) {
		int far = ch.pos + dir * (power + 1);
		if (far < 0 || far >= Dungeon.level.length()) return;
		WandOfBlastWave.throwChar(ch, new Ballistica(ch.pos, far, Ballistica.MAGIC_BOLT), power, true, true, this);
	}

	/** drags a character back toward the wall, as far as the cells in front of the body */
	private void pull(Char ch) {
		if (Char.hasProp(ch, Property.IMMOVABLE)) return;
		int dest = ch.pos;
		while (true) {
			int next = dest - dir;
			if (next < 0 || next >= Dungeon.level.length() || Dungeon.level.solid[next]
					|| !Dungeon.level.passable[next] || Actor.findChar(next) != null) break;
			dest = next;
		}
		if (dest == ch.pos) return;
		Actor.add(new Pushing(ch, ch.pos, dest));
		ch.pos = dest;
		Dungeon.level.occupyCell(ch);
		if (ch == Dungeon.hero) {
			Dungeon.hero.interrupt();
			Dungeon.observe();
			GameScene.updateFog();
		}
	}

	// ------------------------------------------------------------------ end

	@Override
	public void die(Object cause) {
		for (LeviathanCoil c : coils()) c.vanish();
		super.die(cause);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("dir", dir);
		bundle.put("run", run);
		bundle.put("lateral", lateral);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		dir = bundle.getInt("dir");
		run = bundle.getIntArray("run");
		if (run == null) run = new int[0];
		lateral = bundle.getInt("lateral");
		lord = DemonLord.LEVIATHAN;
	}
}
