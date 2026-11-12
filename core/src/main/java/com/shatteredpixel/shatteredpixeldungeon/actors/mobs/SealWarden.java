package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.SealFragment;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.SealSanctumRoom;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.StatueSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The Seal Warden, the sewer prophet's miniboss. It waits on the pedestal of its arena
 * ({@link SealSanctumRoom}) and seals the door once the hero steps inside; the door opens again
 * when it dies or the hero leaves (e.g. by teleporting). It never leaves the arena.
 *
 * Each time it loses a quarter of its health it takes on the aspect of the next horseman, which
 * changes its passive and adds a telegraphed skill (marked cells, released on its next turn, so
 * stepping out of the marks dodges it). Skills accumulate: in later aspects it picks from every
 * skill it has learned, preferring the newest.
 *   Conquest (full HP): heavy armor.            Skill: Volley, arrows on the hero's cell and its 4 neighbours.
 *   War (below 75%): 1.5x attack speed.         Skill: Charge, a straight dash through the hero, crippling.
 *   Famine (below 50%): hits weaken/starve.     Skill: Blight, a 3x3 patch that weakens, starves and feeds it.
 *   Death (below 25%): +50% vs a hero <50% HP.  Skill: Reap, a sweep over every cell around it.
 * It drops the seal when defeated.
 *
 * Wardens from older saves have no arena (home = -1) and fight like a plain mob with the skills.
 */
public class SealWarden extends Mob {

	public static final int CONQUEST = 0, WAR = 1, FAMINE = 2, DEATH = 3;
	private static final String[] ASPECTS = {"conquest", "war", "famine", "death"};

	//skills, indexed like the aspects that teach them
	private static final int NONE = -1, VOLLEY = 0, CHARGE = 1, BLIGHT = 2, REAP = 3;
	private static final String[] SKILLS = {"volley", "charge", "blight", "reap"};
	private static final int[] COOLDOWN = {4, 5, 4, 3};
	private static final int CHARGE_RANGE = 7;

	{
		spriteClass = WardenSprite.class;

		HP = HT = 60;
		defenseSkill = 8;

		EXP = 12;
		maxLvl = 12;

		WANDERING = new Wandering();
		HUNTING = new Hunting();
		state = WANDERING;

		properties.add(Property.MINIBOSS);
		properties.add(Property.INORGANIC);
	}

	public int aspect = CONQUEST;

	/** the arena's pedestal, or -1 for a warden without an arena */
	public int home = -1;
	/** door cells walled up while the fight is on */
	private int[] sealedDoors = new int[0];

	private int skill = NONE;
	private int[] marked = new int[0];
	private float skillCooldown = 2;

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(2, 6);
	}

	@Override
	public int attackSkill(Char target) {
		return 12;
	}

	@Override
	public int drRoll() {
		int dr = super.drRoll() + Random.NormalIntRange(0, 2);
		//the conqueror's armor
		if (aspect == CONQUEST) dr += Random.NormalIntRange(1, 4);
		return dr;
	}

	@Override
	public float attackDelay() {
		float d = super.attackDelay();
		return aspect == WAR ? d / 1.5f : d;
	}

	@Override
	public int attackProc(Char enemy, int damage) {
		damage = super.attackProc(enemy, damage);
		if (aspect == FAMINE && enemy instanceof Hero) {
			Buff.prolong(enemy, Weakness.class, 3f);
			Hunger hunger = enemy.buff(Hunger.class);
			if (hunger != null && !hunger.isStarving()) hunger.affectHunger(-20);
		}
		if (aspect == DEATH && enemy.HP < enemy.HT / 2) {
			damage = Math.round(damage * 1.5f);
		}
		return damage;
	}

	// ---- the arena ----

	private Room arena() {
		if (home == -1 || !(Dungeon.level instanceof RegularLevel)) return null;
		Room r = ((RegularLevel) Dungeon.level).room(home);
		return r instanceof SealSanctumRoom ? r : null;
	}

	private boolean inArena(Room arena, int cell) {
		return arena != null && cell >= 0 && arena.inside(Dungeon.level.cellToPoint(cell));
	}

	private boolean canReach(Room arena, int cell) {
		return arena == null || inArena(arena, cell);
	}

	private void sealArena(Room arena) {
		ArrayList<Integer> doors = new ArrayList<>();
		for (Room.Door d : arena.connected.values()) {
			int cell = Dungeon.level.pointToCell(d);
			//someone is standing in the doorway, try again next turn
			if (Actor.findChar(cell) != null || Dungeon.level.heaps.get(cell) != null) return;
			doors.add(cell);
		}
		if (doors.isEmpty()) return;

		sealedDoors = new int[doors.size()];
		for (int i = 0; i < sealedDoors.length; i++) {
			int cell = doors.get(i);
			sealedDoors[i] = cell;
			Level.set(cell, Terrain.WALL);
			GameScene.updateMap(cell);
			CellEmitter.get(cell).burst(Speck.factory(Speck.ROCK), 6);
		}
		Dungeon.observe();
		Sample.INSTANCE.play(Assets.Sounds.ROCKS);
		GLog.w(Messages.get(this, "sealed"));
	}

	private void unsealArena() {
		if (sealedDoors.length == 0) return;
		for (int cell : sealedDoors) {
			Level.set(cell, Terrain.DOOR);
			GameScene.updateMap(cell);
			CellEmitter.get(cell).burst(Speck.factory(Speck.ROCK), 4);
		}
		sealedDoors = new int[0];
		Dungeon.observe();
		Sample.INSTANCE.play(Assets.Sounds.ROCKS);
		GLog.i(Messages.get(this, "unsealed"));
	}

	@Override
	protected boolean act() {
		Room arena = arena();
		if (arena != null && Dungeon.hero != null) {
			boolean heroInside = inArena(arena, Dungeon.hero.pos);
			if (sealedDoors.length == 0 && heroInside && state == HUNTING) {
				sealArena(arena);
			} else if (sealedDoors.length > 0 && !heroInside) {
				unsealArena();
			}
		}
		//a readied skill fizzles if it can't be released on the next turn (its marks would be gone)
		if (skill != NONE && (paralysed > 0 || state != HUNTING)) {
			skill = NONE;
			marked = new int[0];
		}
		if (paralysed <= 0 && skillCooldown > 0) skillCooldown--;
		return super.act();
	}

	@Override
	protected boolean getCloser(int target) {
		Room arena = arena();
		if (arena == null || inArena(arena, target)) return super.getCloser(target);

		//stay inside: head for the arena cell closest to the target
		int best = -1;
		Point p = new Point();
		for (int x = arena.left + 1; x < arena.right; x++) {
			for (int y = arena.top + 1; y < arena.bottom; y++) {
				p.set(x, y);
				int c = Dungeon.level.pointToCell(p);
				if (!Dungeon.level.passable[c] || (Actor.findChar(c) != null && c != pos)) continue;
				if (best == -1 || Dungeon.level.trueDistance(c, target) < Dungeon.level.trueDistance(best, target)) {
					best = c;
				}
			}
		}
		if (best == -1 || best == pos) return false;
		return super.getCloser(best);
	}

	private class Wandering extends Mob.Wandering {
		@Override
		protected int randomDestination() {
			//in its arena it just returns to the pedestal
			return arena() != null ? home : super.randomDestination();
		}
	}

	// ---- skills ----

	private class Hunting extends Mob.Hunting {
		@Override
		public boolean act(boolean enemyInFOV, boolean justAlerted) {
			if (skill != NONE) {
				releaseSkill();
				return true;
			}
			if (skillCooldown <= 0 && enemyInFOV && enemy != null && !isCharmedBy(enemy)
					&& prepareSkill(enemy)) {
				return true;
			}
			return super.act(enemyInFOV, justAlerted);
		}
	}

	/** picks a skill (newest aspect's 50% of the time, else any learned one) and marks its cells */
	private boolean prepareSkill(Char target) {
		ArrayList<Integer> order = new ArrayList<>();
		if (Random.Int(2) == 0) order.add(aspect);
		ArrayList<Integer> rest = new ArrayList<>();
		for (int s = 0; s <= aspect; s++) if (!order.contains(s)) rest.add(s);
		while (!rest.isEmpty()) order.add(rest.remove(Random.Int(rest.size())));

		for (int s : order) {
			int[] cells = cellsFor(s, target);
			if (cells != null && cells.length > 0) {
				skill = s;
				marked = cells;
				for (int c : marked) GameScene.targetedCell(c, 1f);
				if (sprite != null) {
					sprite.turnTo(pos, target.pos);
					sprite.showStatus(CharSprite.WARNING, Messages.get(this, "skill_" + SKILLS[s]));
				}
				Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 0.6f, 1.3f);
				spend(TICK);
				return true;
			}
		}
		return false;
	}

	private int[] cellsFor(int s, Char target) {
		Level level = Dungeon.level;
		Room arena = arena();
		ArrayList<Integer> cells = new ArrayList<>();
		switch (s) {
			case VOLLEY:
				//any distance, as long as it can see the target
				addIfOpen(cells, target.pos);
				for (int n : PathFinder.NEIGHBOURS4) addIfOpen(cells, target.pos + n);
				break;
			case CHARGE:
				if (level.distance(pos, target.pos) < 2 || rooted) return null;
				Ballistica line = new Ballistica(pos, target.pos, Ballistica.STOP_SOLID);
				boolean throughTarget = false;
				for (int i = 1; i < line.path.size() && cells.size() < CHARGE_RANGE; i++) {
					int c = line.path.get(i);
					if (!level.passable[c] || level.solid[c] || !canReach(arena, c)) break;
					cells.add(c);
					if (c == target.pos) throughTarget = true;
				}
				//it has to pass through the target, and land somewhere free
				if (!throughTarget || landingCell(cells) == -1) return null;
				break;
			case BLIGHT: {
				//an X of withering through the target (diagonals, 2 each way)
				addIfOpen(cells, target.pos);
				int w = level.width(), tx = target.pos % w, ty = target.pos / w;
				for (int i = 1; i <= 2; i++) {
					for (int[] d : new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
						int x = tx + d[0] * i, y = ty + d[1] * i;
						if (x >= 0 && y >= 0 && x < w && y < level.height()) addIfOpen(cells, x + y * w);
					}
				}
				cells.remove((Integer) pos);
				break;
			}
			case REAP:
				//only worth it with the target close by
				if (level.distance(pos, target.pos) > 2) return null;
				for (int n : PathFinder.NEIGHBOURS8) addIfOpen(cells, pos + n);
				break;
		}
		int[] out = new int[cells.size()];
		for (int i = 0; i < out.length; i++) out[i] = cells.get(i);
		return out;
	}

	private void addIfOpen(ArrayList<Integer> cells, int c) {
		if (c >= 0 && c < Dungeon.level.length() && !Dungeon.level.solid[c] && !cells.contains(c)) cells.add(c);
	}

	/** the furthest free cell along a charge path */
	private int landingCell(int[] path) {
		for (int i = path.length - 1; i >= 0; i--) {
			int c = path[i];
			if (Dungeon.level.passable[c] && Actor.findChar(c) == null) return c;
		}
		return -1;
	}

	private int landingCell(ArrayList<Integer> path) {
		int[] a = new int[path.size()];
		for (int i = 0; i < a.length; i++) a[i] = path.get(i);
		return landingCell(a);
	}

	private void releaseSkill() {
		int s = skill;
		int[] cells = marked;
		skill = NONE;
		marked = new int[0];
		skillCooldown = COOLDOWN[s];

		switch (s) {
			case VOLLEY:
				for (int c : cells) {
					CellEmitter.center(c).burst(SparkParticle.FACTORY, 6);
					strike(c, 3, 6, s);
				}
				Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH);
				break;

			case CHARGE:
				int landing = rooted ? -1 : landingCell(cells);
				if (landing != -1) {
					for (int c : cells) {
						CellEmitter.get(c).burst(BloodParticle.BURST, 3);
						Char ch = strike(c, 4, 8, s);
						if (ch != null && ch.isAlive()) Buff.prolong(ch, Cripple.class, 2f);
						if (c == landing) break;
					}
					int from = pos;
					pos = landing;
					Actor.add(new Pushing(this, from, landing));
					Dungeon.level.occupyCell(this);
					Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
				}
				break;

			case BLIGHT:
				int fed = 0;
				for (int c : cells) {
					CellEmitter.get(c).burst(ShadowParticle.UP, 4);
					Char ch = strike(c, 2, 4, s);
					if (ch != null) {
						fed++;
						if (ch.isAlive()) Buff.prolong(ch, Weakness.class, 5f);
						Hunger hunger = ch.buff(Hunger.class);
						if (hunger != null && !hunger.isStarving()) hunger.affectHunger(-30);
					}
				}
				if (fed > 0 && isAlive()) {
					int heal = Math.min(HT - HP, 4 * fed);
					if (heal > 0) {
						HP += heal;
						if (sprite != null) sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
					}
				}
				Sample.INSTANCE.play(Assets.Sounds.CURSED);
				break;

			case REAP:
				for (int c : cells) {
					CellEmitter.get(c).burst(ShadowParticle.CURSE, 4);
					strike(c, 5, 10, s);
				}
				Sample.INSTANCE.play(Assets.Sounds.BLAST);
				break;
		}
		spend(TICK);
	}

	/** hits whoever stands on a marked cell (not the warden's allies); returns them */
	private Char strike(int cell, int min, int max, int s) {
		Char ch = Actor.findChar(cell);
		if (ch == null || ch == this || ch.alignment == alignment) return null;

		int dmg = Random.NormalIntRange(min, max);
		if (s == REAP && ch.HP < ch.HT / 2) dmg = Math.round(dmg * 1.5f);
		dmg = Math.max(0, dmg - ch.drRoll());
		ch.damage(dmg, this);
		if (ch.sprite != null) ch.sprite.flash();

		if (ch == Dungeon.hero && !ch.isAlive()) {
			Dungeon.fail(this);
			GLog.n(Messages.get(this, "skill_kill"));
		}
		return ch;
	}

	// ---- aspects ----

	@Override
	public void damage(int dmg, Object src) {
		super.damage(dmg, src);
		if (!isAlive()) return;
		int next = HP < HT / 4 ? DEATH : HP < HT / 2 ? FAMINE : HP < HT * 3 / 4 ? WAR : CONQUEST;
		if (next > aspect) {
			aspect = next;
			//the shift interrupts a skill being readied, and the new skill comes soon after
			skill = NONE;
			marked = new int[0];
			skillCooldown = 1;
			if (sprite != null) {
				((WardenSprite) sprite).tint(aspect);
				sprite.emitter().burst(Speck.factory(Speck.STAR), 6);
				sprite.showStatus(CharSprite.WARNING, Messages.get(this, "aspect_" + ASPECTS[aspect]));
			}
			yell(Messages.get(this, "yell_" + ASPECTS[aspect]));
			GameScene.flash(0x40000000, false);
			Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
		}
	}

	private boolean greeted = false;

	@Override
	public void notice() {
		super.notice();
		if (!greeted) {
			greeted = true;
			yell(Messages.get(this, "notice"));
		}
	}

	@Override
	public void die(Object cause) {
		super.die(cause);
		unsealArena();
		yell(Messages.get(this, "defeated"));
		if (Dungeon.level != null) Dungeon.level.drop(new SealFragment(), pos).sprite.drop();
	}

	@Override
	public String description() {
		StringBuilder d = new StringBuilder(Messages.get(this, "desc"));
		d.append("\n\n").append(Messages.get(this, "desc_" + ASPECTS[aspect]));
		d.append("\n\n").append(Messages.get(this, "desc_skills"));
		for (int s = 0; s <= aspect; s++) {
			d.append("\n").append(Messages.get(this, "desc_skill_" + SKILLS[s]));
		}
		return d.toString();
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("aspect", aspect);
		bundle.put("greeted", greeted);
		bundle.put("home", home);
		bundle.put("sealed_doors", sealedDoors);
		bundle.put("skill", skill);
		bundle.put("marked", marked);
		bundle.put("skill_cd", skillCooldown);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		aspect = bundle.getInt("aspect");
		greeted = bundle.getBoolean("greeted");
		//older wardens have no arena
		home = bundle.contains("home") ? bundle.getInt("home") : -1;
		if (bundle.contains("sealed_doors")) sealedDoors = bundle.getIntArray("sealed_doors");
		skill = bundle.contains("skill") ? bundle.getInt("skill") : NONE;
		if (bundle.contains("marked")) marked = bundle.getIntArray("marked");
		skillCooldown = bundle.contains("skill_cd") ? bundle.getFloat("skill_cd") : 2;
		//older wardens had 45 max HP
		if (HT < 60) {
			HP = Math.round(HP * 60f / HT);
			HT = 60;
		}
	}

	/** placeholder art: an armored statue, tinted by the current horseman's colour */
	public static class WardenSprite extends StatueSprite {

		private static final float[][] COLORS = {
				{1.0f, 1.0f, 1.0f},  //conquest: white
				{1.0f, 0.3f, 0.3f},  //war: red
				{0.35f, 0.35f, 0.4f},//famine: black
				{0.6f, 0.9f, 0.6f},  //death: pale green
		};

		private int aspect = CONQUEST;

		public WardenSprite() {
			super();
			setArmor(3);
		}

		@Override
		public void link(Char ch) {
			super.link(ch);
			if (ch instanceof SealWarden) aspect = ((SealWarden) ch).aspect;
			tint(aspect);
		}

		public void tint(int aspect) {
			this.aspect = aspect;
			resetColor();
		}

		@Override
		public void resetColor() {
			super.resetColor();
			float[] c = COLORS[Math.max(0, Math.min(3, aspect))];
			hardlight(c[0], c[1], c[2]);
		}
	}
}
