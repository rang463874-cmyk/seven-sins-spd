package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.heaven;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.elites.RegionElite;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.HolyGem;
import com.shatteredpixel.shatteredpixeldungeon.levels.HolyGiantLevel;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeavenSprites;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The holy giant, final boss of the heavens (floor 31). It raises its upper body from the void at the
 * edge of the arena and never moves. Its body can't be hurt directly: its two arms can.
 * - Arm slam (left or right, on the hero's side): marks a 3x8 strip from its ledge down through the
 *   hero's column, slams the whole arm across it next turn and leaves it there for 3 turns: the fist
 *   ({@link GiantFist}) at the far end, the rest of the arm as {@link GiantArmPart}s that pass hits on. Fists keep their own HP between slams; damage to a
 *   fist is also the giant's damage (its HP = what both arms have left). A fist at 0 HP is destroyed.
 * - Lightning: a branching bolt (the owner's picture, {@link GiantLightningPattern}) spreading over the
 *   arena from the giant, randomly shifted and mirrored; marked tiles are struck next turn and paralysed.
 * - Judgment: each time its HP falls past 1/2 and 1/4 it charges for 3 turns, then hits every tile it
 *   can see. Standing behind a wall block (out of its line of sight) is safe.
 * Both arms destroyed: it collapses, a blue gem ({@link HolyGem}) appears in the middle of the arena and
 * heaven monsters pour in from the edges. Reaching (picking up) the gem ends the fight.
 */
public class HolyGiant extends RegionElite {

	public static final int ARM_HT = 300;
	public static final int SLAM = 0, BOLT = 1;
	public static final int FIST_TURNS = 3;
	public static final int JUDGMENT_CHARGE = 3;
	public static final int SWARM = 14;
	/** the slammed arm: 3 wide, 8 long from the ledge down */
	public static final int ARM_LEN = 8;

	/** HP left on the left / right arm; 0 = destroyed */
	public int[] armHP = {ARM_HT, ARM_HT};
	/** actor ids of the fists lying on the floor, -1 = arm raised */
	public int[] fists = {-1, -1};
	private int pendingSide = 0;
	/** the slam's middle column, the bolt's left column and mirroring */
	private int pendingCol = 0;
	private int boltX = 0;
	private boolean boltMirror = false;
	private int judgment = 0;
	/** 0 = none yet, 1 = after 1/2, 2 = after 1/4 */
	private int judgmentsDone = 0;
	private boolean fallen = false;
	private boolean started = false;

	{
		spriteClass = HeavenSprites.HolyGiant.class;
		HP = HT = 2 * ARM_HT;
		defenseSkill = 0;
		EXP = 0;
		maxLvl = -2;
		state = WANDERING;
		properties.remove(Property.MINIBOSS);
		properties.add(Property.BOSS);
		properties.add(Property.IMMOVABLE);
		HUNTING = new GiantHunting();
	}

	/** it never loses the hero: while the hero hides out of sight it simply waits */
	private class GiantHunting extends RegionElite.Hunting {
		@Override
		public boolean act(boolean enemyInFOV, boolean justAlerted) {
			if (!enemyInFOV) {
				if (Dungeon.hero != null) enemy = Dungeon.hero;
				spend(TICK);
				return true;
			}
			return super.act(enemyInFOV, justAlerted);
		}
	}

	protected int d() {
		return HeavenMobs.depth();
	}

	// ------------------------------------------------------------------ the arms

	public GiantFist fist(int side) {
		Actor a = Actor.findById(fists[side]);
		return a instanceof GiantFist && ((GiantFist) a).isAlive() ? (GiantFist) a : null;
	}

	/** a fist took damage: it is the giant's damage too */
	public void onFistDamaged(int side, int hpLeft) {
		armHP[side] = Math.max(0, hpLeft);
		syncHP();
		checkJudgment();
	}

	/** a fist pulls back after its turns on the floor */
	public void onFistRetracted(int side, int hpLeft) {
		armHP[side] = Math.max(0, hpLeft);
		fists[side] = -1;
		syncHP();
	}

	public void onFistDestroyed(int side) {
		armHP[side] = 0;
		fists[side] = -1;
		syncHP();
		yell(Messages.get(this, "arm_lost"));
		if (armHP[0] <= 0 && armHP[1] <= 0) collapse();
		else checkJudgment();
	}

	private void syncHP() {
		HP = Math.max(fallen ? 0 : 1, armHP[0] + armHP[1]);
		if (HP <= 0) HP = 1;
	}

	private void checkJudgment() {
		if (fallen) return;
		if (judgmentsDone == 0 && HP <= HT / 2) {
			judgmentsDone = 1;
			startJudgment();
		} else if (judgmentsDone == 1 && HP <= HT / 4) {
			judgmentsDone = 2;
			startJudgment();
		}
	}

	private void startJudgment() {
		judgment = JUDGMENT_CHARGE;
		skill = NONE;
		marked = new int[0];
		yell(Messages.get(this, "judgment_start"));
		GLog.w(Messages.get(this, "judgment_warn"));
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
		if (sprite != null) sprite.showStatus(CharSprite.WARNING, Messages.get(this, "judgment_count", judgment));
	}

	// ------------------------------------------------------------------ turns

	@Override
	protected boolean act() {
		if (fallen) {
			spend(TICK);
			return true;
		}
		if (!started) {
			if (Dungeon.hero != null && Dungeon.level.distance(pos, Dungeon.hero.pos) <= 10
					&& Dungeon.level.heroFOV[pos]) {
				started = true;
				state = HUNTING;
				aggro(Dungeon.hero);
				notice();
				BossHealthBar.assignBoss(this);
			} else {
				spend(TICK);
				return true;
			}
		}
		if (state != HUNTING) state = HUNTING;
		if (judgment > 0) {
			judgment--;
			if (judgment > 0) {
				if (sprite != null) sprite.showStatus(CharSprite.WARNING, Messages.get(this, "judgment_count", judgment));
				spend(TICK);
				return true;
			}
			releaseJudgment();
			spend(TICK);
			return true;
		}
		return super.act();
	}

	@Override
	protected boolean canAttack(Char enemy) {
		return false;
	}

	@Override
	protected boolean getCloser(int target) {
		return false;
	}

	@Override
	protected boolean getFurther(int target) {
		return false;
	}

	@Override
	public void damage(int dmg, Object src) {
		//its body shrugs everything off; only its arms can be hurt
		if (sprite != null && dmg > 0 && !fallen) sprite.showStatus(CharSprite.NEUTRAL, Messages.get(this, "immune"));
	}

	@Override
	public boolean isInvulnerable(Class effect) {
		return true;
	}

	@Override
	public boolean add(Buff buff) {
		return false;
	}

	// ------------------------------------------------------------------ skills

	@Override
	protected int skillCount() {
		return 2;
	}

	@Override
	protected int cooldown(int s) {
		return s == SLAM ? 2 : 3;
	}

	@Override
	protected String skillKey(int s) {
		return s == SLAM ? "slam" : "bolt";
	}

	@Override
	protected int[] prepare(int s, Char target) {
		int w = Dungeon.level.width();
		int gx = pos % w;
		if (s == SLAM) {
			int tx = target.pos % w, ty = target.pos / w;
			//the arm reaches 8 rows down from the ledge
			if (ty > HolyGiantLevel.LEDGE + ARM_LEN - 1) return null;
			int side = tx < gx ? 0 : 1;
			if (!armReady(side)) return null;
			pendingSide = side;
			pendingCol = Math.max(HolyGiantLevel.LEFT + 1, Math.min(HolyGiantLevel.RIGHT - 1, tx));
			ArrayList<Integer> cells = new ArrayList<>();
			for (int y = HolyGiantLevel.LEDGE; y < HolyGiantLevel.LEDGE + ARM_LEN; y++) {
				for (int x = pendingCol - 1; x <= pendingCol + 1; x++) {
					int c = x + y * w;
					if (c != pos && Dungeon.level.passable[c]) addIfOpen(cells, c);
				}
			}
			return cells.isEmpty() ? null : toArray(cells);
		}
		//lightning: the owner's branching bolt from the giant across the arena
		boltMirror = Random.Int(2) == 0;
		boltX = gx - GiantLightningPattern.W / 2 + Random.IntRange(-2, 2);
		boltX = Math.max(HolyGiantLevel.LEFT, Math.min(HolyGiantLevel.RIGHT + 1 - GiantLightningPattern.W, boltX));
		ArrayList<Integer> cells = new ArrayList<>();
		for (int j = 0; j < GiantLightningPattern.H; j++) {
			String row = GiantLightningPattern.ROWS[j];
			for (int i = 0; i < GiantLightningPattern.W; i++) {
				if (row.charAt(boltMirror ? GiantLightningPattern.W - 1 - i : i) != '#') continue;
				int x = boltX + i, y = HolyGiantLevel.LEDGE + j;
				if (x < 0 || x >= w || y >= Dungeon.level.height()) continue;
				int c = x + y * w;
				if (c != pos && Dungeon.level.passable[c]) addIfOpen(cells, c);
			}
		}
		//a faint glimpse of where it will fall
		HeavenSprites.lightning(boltX, HolyGiantLevel.LEDGE, boltMirror, 0.3f, 1.2f);
		return cells.isEmpty() ? null : toArray(cells);
	}

	private boolean armReady(int side) {
		return armHP[side] > 0 && fist(side) == null;
	}

	@Override
	protected void release(int s, int[] cells) {
		int d = d();
		int w = Dungeon.level.width();
		if (s == SLAM) {
			if (!armReady(pendingSide)) return;
			for (int c : cells) {
				CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 3);
				Char ch = strike(c, d, 2 * d);
				if (ch != null && ch.isAlive()) Buff.prolong(ch, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple.class, 1f);
			}
			PixelScene.shake(4, 0.6f);
			Sample.INSTANCE.play(Assets.Sounds.ROCKS);
			Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.7f);
			//the fist lands at the far end of the strip (the nearest free tile of its last rows)
			int bottom = HolyGiantLevel.LEDGE + ARM_LEN - 1;
			int[] tries = {
					pendingCol + bottom * w, pendingCol + (bottom - 1) * w,
					pendingCol - 1 + bottom * w, pendingCol + 1 + bottom * w,
					pendingCol - 1 + (bottom - 1) * w, pendingCol + 1 + (bottom - 1) * w};
			int spot = -1;
			for (int c : tries) {
				if (Dungeon.level.passable[c] && Actor.findChar(c) == null) { spot = c; break; }
			}
			if (spot == -1) {
				for (int i = cells.length - 1; i >= 0; i--) {
					if (Actor.findChar(cells[i]) == null && Dungeon.level.passable[cells[i]]) { spot = cells[i]; break; }
				}
			}
			if (spot != -1) {
				GiantFist f = new GiantFist();
				f.giant = id();
				f.side = pendingSide;
				f.HT = ARM_HT;
				f.HP = armHP[pendingSide];
				f.turns = FIST_TURNS;
				f.pos = spot;
				GameScene.add(f);
				fists[pendingSide] = f.id();
				//the rest of the arm lies up the fist's column to the ledge
				ArrayList<Integer> partIds = new ArrayList<>();
				for (int c = spot - w; c / w >= HolyGiantLevel.LEDGE; c -= w) {
					if (!Dungeon.level.passable[c] || Actor.findChar(c) != null) continue;
					GiantArmPart p = new GiantArmPart();
					p.owner = f.id();
					p.pos = c;
					GameScene.add(p);
					partIds.add(p.id());
				}
				f.parts = toArray(partIds);
			}
			return;
		}
		//the branching lightning
		HeavenSprites.lightning(boltX, HolyGiantLevel.LEDGE, boltMirror, 1f, 0.8f);
		GameScene.flash(0x5070B0);
		for (int c : cells) {
			if (Dungeon.level.heroFOV[c] && Random.Int(3) == 0) CellEmitter.center(c).burst(SparkParticle.FACTORY, 4);
			Char ch = strike(c, d / 2, d);
			if (ch != null && ch.isAlive()) {
				Buff.prolong(ch, Paralysis.class, 2f);
				if (ch.sprite != null) CellEmitter.center(c).burst(SparkParticle.FACTORY, 8);
			}
		}
		Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
	}

	/** every tile it can see takes a crushing blow; walls block it */
	private void releaseJudgment() {
		int d = d();
		GameScene.flash(0x88BBFF);
		PixelScene.shake(5, 1f);
		Sample.INSTANCE.play(Assets.Sounds.BLAST);
		yell(Messages.get(this, "judgment_release"));
		for (Char ch : Actor.chars()) {
			if (ch == this || ch.alignment == alignment || !ch.isAlive()) continue;
			Ballistica line = new Ballistica(pos, ch.pos, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID);
			if (line.collisionPos != ch.pos) continue; //behind cover
			int dmg = Math.max(0, Random.NormalIntRange(2 * d, 3 * d) - ch.drRoll());
			ch.damage(dmg, this);
			if (ch.sprite != null) ch.sprite.emitter().burst(SparkParticle.FACTORY, 6);
			if (ch == Dungeon.hero && !ch.isAlive()) {
				Dungeon.fail(this);
				GLog.n(Messages.get(this, "judgment_kill"));
			}
		}
	}

	// ------------------------------------------------------------------ the end

	/** both arms are gone: it slumps over, the gem appears and the heavens send their host */
	private void collapse() {
		fallen = true;
		HP = 1;
		skill = NONE;
		marked = new int[0];
		judgment = 0;
		yell(Messages.get(this, "collapse"));
		GLog.p(Messages.get(this, "gem_appears"));
		int center = HolyGiantLevel.centerCell();
		Dungeon.level.drop(new HolyGem(), center).sprite.drop();
		CellEmitter.get(center).burst(Speck.factory(Speck.LIGHT), 16);

		//the host pours in from the edges and hunts the hero
		ArrayList<Integer> edges = new ArrayList<>();
		for (int i = 0; i < Dungeon.level.length(); i++) {
			if (!Dungeon.level.passable[i] || Actor.findChar(i) != null) continue;
			int x = i % Dungeon.level.width(), y = i / Dungeon.level.width();
			boolean edge = x <= HolyGiantLevel.LEFT + 1 || x >= HolyGiantLevel.RIGHT - 1 || y >= HolyGiantLevel.BOTTOM - 1;
			if (edge && Dungeon.level.distance(i, Dungeon.hero.pos) > 3) edges.add(i);
		}
		Random.shuffle(edges);
		for (int i = 0; i < SWARM && i < edges.size(); i++) {
			Mob m = Random.Int(4) == 0 ? new Priest() : new Believer();
			m.pos = edges.get(i);
			m.state = m.HUNTING;
			GameScene.add(m);
			m.aggro(Dungeon.hero);
			m.beckon(Dungeon.hero.pos);
		}
	}

	public boolean fallen() {
		return fallen;
	}

	/** the hero reached the gem: every monster here fades and the giant is defeated */
	public void vanquish() {
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (m == this || m.alignment != Alignment.ENEMY) continue;
			if (m.sprite != null) CellEmitter.get(m.pos).burst(Speck.factory(Speck.LIGHT), 4);
			m.destroy();
			if (m.sprite != null) m.sprite.killAndErase();
		}
		yell(Messages.get(this, "death"));
		GameScene.bossSlain();
		super.die(null);
	}

	@Override
	public void die(Object cause) {
		//only reaching the gem ends it
		if (!fallen) return;
		vanquish();
	}

	@Override
	protected boolean dropsReward() {
		return false;
	}

	@Override
	protected String noticeLine() {
		return Messages.get(this, "notice");
	}

	@Override
	public void notice() {
		super.notice();
		BossHealthBar.assignBoss(this);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("arms", armHP);
		bundle.put("fists", fists);
		bundle.put("judgment", judgment);
		bundle.put("judgments_done", judgmentsDone);
		bundle.put("fallen", fallen);
		bundle.put("started", started);
		bundle.put("pending_side", pendingSide);
		bundle.put("pending_col", pendingCol);
		bundle.put("bolt_x", boltX);
		bundle.put("bolt_mirror", boltMirror);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		int[] a = bundle.getIntArray("arms");
		if (a != null && a.length == 2) armHP = a;
		int[] f = bundle.getIntArray("fists");
		if (f != null && f.length == 2) fists = f;
		judgment = bundle.getInt("judgment");
		judgmentsDone = bundle.getInt("judgments_done");
		fallen = bundle.getBoolean("fallen");
		started = bundle.getBoolean("started");
		pendingSide = bundle.getInt("pending_side");
		pendingCol = bundle.getInt("pending_col");
		boltX = bundle.getInt("bolt_x");
		boltMirror = bundle.getBoolean("bolt_mirror");
		if (started && !fallen) BossHealthBar.assignBoss(this);
	}
}
