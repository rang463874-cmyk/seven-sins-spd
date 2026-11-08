package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.heaven;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Chains;
import com.shatteredpixel.shatteredpixeldungeon.effects.Effects;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeavenSprites;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * A seraph: a rare guardian of the heavens that fills 2x2 tiles (its own cell is the top-left one,
 * three invisible {@link SeraphPart}s fill the rest) and never leaves the room it was placed in.
 * Anyone standing next to it burns every turn; its spear reaches 2 tiles; once per fight it can pull
 * a target to itself with a chain of light (like a prison guard, but no cripple).
 */
public class Seraph extends Mob {

	public static final int REACH = 2;

	protected int d = 27;
	/** room interior it may stand in (inclusive) */
	public int left, top, right, bottom;
	/** actor ids of its 3 parts: right, below, below-right */
	public int[] parts = new int[]{-1, -1, -1};
	private boolean chainUsed = false;

	{
		spriteClass = HeavenSprites.Seraph.class;
		EXP = 45;
		maxLvl = 34;
		properties.add(Property.MINIBOSS);
		WANDERING = new Wandering();
		HUNTING = new Hunting();
	}

	public Seraph() {
		super();
		d = HeavenMobs.depth();
		HP = HT = 8 * d + 60;
		defenseSkill = d;
	}

	// ------------------------------------------------------------------ body

	/** the 4 cells of the body if its top-left cell were c */
	private int[] body(int c) {
		return body(Dungeon.level, c);
	}

	//the level is passed in while it is being built (Dungeon.level is not set yet then)
	private static int[] body(Level level, int c) {
		int w = level.width();
		return new int[]{c, c + 1, c + w, c + w + 1};
	}

	public int blockDist(int cell) {
		int best = Integer.MAX_VALUE;
		for (int b : body(pos)) best = Math.min(best, Dungeon.level.distance(b, cell));
		return best;
	}

	private int nearestBody(int cell) {
		int best = pos;
		for (int b : body(pos)) {
			if (Dungeon.level.distance(b, cell) < Dungeon.level.distance(best, cell)) best = b;
		}
		return best;
	}

	private boolean isMine(Char ch) {
		if (ch == this) return true;
		if (ch instanceof SeraphPart) return ((SeraphPart) ch).owner == id();
		return false;
	}

	/** whether the body fits with its top-left cell at c */
	public boolean fits(Level level, int c) {
		Point p = level.cellToPoint(c);
		if (p.x < left || p.y < top || p.x + 1 > right || p.y + 1 > bottom) return false;
		for (int b : body(level, c)) {
			if (b < 0 || b >= level.length() || !level.passable[b] || level.solid[b]) return false;
			Char ch = Actor.findChar(b);
			if (ch != null && !isMine(ch)) return false;
			if (level == Dungeon.level && level.findMob(b) != null && !isMine(level.findMob(b))) return false;
		}
		return true;
	}

	/** places the seraph and its parts on the level being built (room bounds already set) */
	public void place(Level level, int cell) {
		pos = cell;
		level.mobs.add(this);
		int[] b = body(level, cell);
		for (int i = 0; i < 3; i++) {
			SeraphPart part = new SeraphPart();
			part.owner = id();
			part.pos = b[i + 1];
			level.mobs.add(part);
			parts[i] = part.id();
		}
	}

	private SeraphPart part(int i) {
		Actor a = Actor.findById(parts[i]);
		return a instanceof SeraphPart ? (SeraphPart) a : null;
	}

	private void moveParts() {
		int[] b = body(pos);
		for (int i = 0; i < 3; i++) {
			SeraphPart p = part(i);
			if (p == null) continue;
			p.pos = b[i + 1];
			if (p.sprite != null) p.sprite.place(p.pos);
		}
	}

	// ------------------------------------------------------------------ behaviour

	/** a seraph made without the level builder (dev summon): give it a body and the whole floor */
	private void ensureBody() {
		if (parts[0] != -1 || Dungeon.level == null) return;
		if (right == 0 && bottom == 0) {
			left = 1; top = 1;
			right = Dungeon.level.width() - 2;
			bottom = Dungeon.level.height() - 2;
		}
		int[] b = body(pos);
		for (int i = 0; i < 3; i++) {
			int c = b[i + 1];
			if (!Dungeon.level.passable[c] || Actor.findChar(c) != null) continue;
			SeraphPart part = new SeraphPart();
			part.owner = id();
			part.pos = c;
			GameScene.add(part);
			parts[i] = part.id();
		}
		if (parts[0] == -1) parts[0] = -2; //tried, no room
	}

	@Override
	protected boolean act() {
		ensureBody();
		//holy fire on everyone touching it
		if (state != SLEEPING && paralysed <= 0) {
			for (Char ch : Actor.chars()) {
				if (ch.alignment == Alignment.ENEMY || !ch.isAlive() || blockDist(ch.pos) != 1) continue;
				ch.damage(Random.NormalIntRange(d / 5, d / 3), this);
				if (ch.sprite != null) ch.sprite.emitter().burst(SparkParticle.FACTORY, 3);
				if (ch == Dungeon.hero && !ch.isAlive()) {
					Dungeon.fail(this);
					GLog.n(Messages.get(this, "burned"));
				}
			}
		}
		return super.act();
	}

	@Override
	protected boolean canAttack(Char enemy) {
		if (blockDist(enemy.pos) > REACH) return false;
		int from = nearestBody(enemy.pos);
		Ballistica line = new Ballistica(from, enemy.pos, Ballistica.PROJECTILE);
		return line.collisionPos == enemy.pos || Dungeon.level.adjacent(from, enemy.pos);
	}

	@Override
	protected boolean getCloser(int target) {
		if (rooted) return false;
		int best = -1;
		int bestDist = blockDistFrom(pos, target);
		for (int n : PathFinder.NEIGHBOURS8) {
			int c = pos + n;
			if (!fits(Dungeon.level, c)) continue;
			int dist = blockDistFrom(c, target);
			if (dist < bestDist) {
				bestDist = dist;
				best = c;
			}
		}
		if (best == -1) return false;
		move(best);
		moveParts();
		return true;
	}

	@Override
	protected boolean getFurther(int target) {
		return false;
	}

	private int blockDistFrom(int c, int target) {
		int best = Integer.MAX_VALUE;
		for (int b : body(c)) best = Math.min(best, Dungeon.level.distance(b, target));
		return best;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(d / 2, d + 10);
	}

	@Override
	public int attackSkill(Char target) {
		return d + 20;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, d / 2);
	}

	@Override
	public void die(Object cause) {
		super.die(cause);
		for (int i = 0; i < 3; i++) {
			SeraphPart p = part(i);
			if (p != null) p.vanish();
		}
	}

	/** once per fight: a chain of light pulls the target next to its body */
	private boolean chain() {
		if (chainUsed || enemy == null || enemy.properties().contains(Property.IMMOVABLE)) return false;
		int from = nearestBody(enemy.pos);
		Ballistica line = new Ballistica(from, enemy.pos, Ballistica.PROJECTILE);
		if (line.collisionPos != enemy.pos || line.path.size() < 2) return false;
		int newPos = -1;
		for (int c : line.subPath(1, line.dist)) {
			if (!Dungeon.level.solid[c] && Actor.findChar(c) == null && !Dungeon.level.pit[c]) {
				newPos = c;
				break;
			}
		}
		if (newPos == -1) return false;
		chainUsed = true;
		final int dest = newPos;
		final Char victim = enemy;
		if (sprite != null && (sprite.visible || victim.sprite.visible)) {
			yell(Messages.get(this, "chain"));
			new Item().throwSound();
			Sample.INSTANCE.play(Assets.Sounds.CHAINS);
			sprite.parent.add(new Chains(sprite.center(), victim.sprite.destinationCenter(), Effects.Type.LIGHT_RAY,
					new Callback() {
						public void call() {
							Actor.add(new Pushing(victim, victim.pos, dest, new Callback() {
								public void call() {
									victim.pos = dest;
									victim.sprite.place(dest);
									Dungeon.level.occupyCell(victim);
								}
							}));
							next();
						}
					}));
		} else {
			victim.pos = dest;
			Dungeon.level.occupyCell(victim);
		}
		return true;
	}

	private class Hunting extends Mob.Hunting {
		@Override
		public boolean act(boolean enemyInFOV, boolean justAlerted) {
			if (!chainUsed && enemyInFOV && enemy != null && !isCharmedBy(enemy) && !canAttack(enemy)
					&& blockDist(enemy.pos) <= 6 && chain()) {
				spend(TICK);
				return !(sprite.visible || enemy.sprite.visible);
			}
			return super.act(enemyInFOV, justAlerted);
		}
	}

	/** wanders around its own room */
	private class Wandering extends Mob.Wandering {
		@Override
		protected int randomDestination() {
			ArrayList<Integer> cells = new ArrayList<>();
			for (int x = left; x <= right; x++) {
				for (int y = top; y <= bottom; y++) {
					int c = x + y * Dungeon.level.width();
					if (Dungeon.level.passable[c]) cells.add(c);
				}
			}
			return cells.isEmpty() ? pos : Random.element(cells);
		}
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("depth", d);
		bundle.put("bounds", new int[]{left, top, right, bottom});
		bundle.put("parts", parts);
		bundle.put("chain_used", chainUsed);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		if (bundle.contains("depth")) d = bundle.getInt("depth");
		int[] b = bundle.getIntArray("bounds");
		if (b != null && b.length == 4) {
			left = b[0]; top = b[1]; right = b[2]; bottom = b[3];
		}
		int[] p = bundle.getIntArray("parts");
		if (p != null && p.length == 3) parts = p;
		chainUsed = bundle.getBoolean("chain_used");
	}
}
