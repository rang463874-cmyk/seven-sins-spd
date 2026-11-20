package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.elites.RegionElite;
import com.shatteredpixel.shatteredpixeldungeon.ascent.AscentHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * Seven Sins: the ascent's floor bosses (Nov 20 2026, owner: new figures from the same myths, fixed per faction).
 * Demons: Moloch 25, Lilith 20, Azazel 15, Abaddon 10. Angels: Cherubim 25, Ophanim 20, Azrael 15, Sandalphon 10.
 * Put on the boss floor's first visit during the ascent by AscentHooks, which seals the floor until it dies.
 * `tier` 0..3 = 25..10: they get stronger towards the surface.
 */
public abstract class AscentBoss extends RegionElite {

	{
		HP = HT = 600;
		defenseSkill = 26;
		EXP = 50;
		maxLvl = -2;
		viewDistance = 10;
		properties.remove(Property.MINIBOSS);
		properties.add(Property.BOSS);
	}

	public int tier = 0;

	public AscentBoss setTier(int t) {
		tier = Math.max(0, Math.min(3, t));
		HP = HT = 600 + 120 * tier;
		defenseSkill = 26 + 2 * tier;
		return this;
	}

	protected boolean enraged() {
		return HP < HT / 2;
	}

	/** damage of a skill hit */
	protected int skillMin() {
		return 18 + 3 * tier;
	}

	protected int skillMax() {
		return 34 + 5 * tier;
	}

	/** a skill's cooldown, one turn shorter below half health */
	protected int cd(int base) {
		return Math.max(2, base - (enraged() ? 1 : 0));
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(14 + 3 * tier, 28 + 4 * tier);
	}

	@Override
	public int attackSkill(Char target) {
		return 40 + 3 * tier;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(3 + tier, 10 + 2 * tier);
	}

	@Override
	public void damage(int dmg, Object src) {
		super.damage(soften(dmg, softThreshold(HT)), src);
	}

	@Override
	public void die(Object cause) {
		super.die(cause);
		AscentHooks.onBossSlain(this);
	}

	// ---- helpers ----

	/** the straight line from this boss through the target to the first wall (at most len cells) */
	protected int[] line(Char target, int len) {
		Ballistica b = new Ballistica(pos, target.pos, Ballistica.STOP_SOLID);
		ArrayList<Integer> out = new ArrayList<>();
		for (int i = 1; i < b.path.size() && out.size() < len; i++) {
			int c = b.path.get(i);
			if (Dungeon.level.solid[c]) break;
			out.add(c);
		}
		return toArray(out);
	}

	/** the cells of a ring around this boss (rIn..rOut) whose direction falls in quarter q (0 up, 1 right, 2 down, 3 left) */
	protected int[] quarter(int rIn, int rOut, int q) {
		int w = Dungeon.level.width();
		int cx = pos % w, cy = pos / w;
		ArrayList<Integer> out = new ArrayList<>();
		for (int c : donut(rIn, rOut)) {
			int dx = c % w - cx, dy = c / w - cy;
			int quad;
			if (Math.abs(dy) >= Math.abs(dx)) quad = dy < 0 ? 0 : 2;
			else quad = dx > 0 ? 1 : 3;
			if (quad == q) out.add(c);
		}
		return toArray(out);
	}

	/** dashes to the last free cell of a line */
	protected void dashAlong(int[] cells) {
		int dest = -1;
		for (int c : cells) {
			if (Dungeon.level.passable[c] && Actor.findChar(c) == null) dest = c;
		}
		if (dest == -1 || Char.hasProp(this, Property.IMMOVABLE)) return;
		Actor.add(new Pushing(this, pos, dest));
		pos = dest;
		Dungeon.level.occupyCell(this);
	}

	/** living minions it called */
	protected int minions() {
		int n = 0;
		for (Mob m : Dungeon.level.mobs) if (m.isAlive() && m.buff(Minion.class) != null) n++;
		return n;
	}

	/** calls n monsters of a kind next to itself; they count for nothing on the ascent's scale */
	protected void summon(Class<? extends Mob> cls, int n) {
		ArrayList<Integer> cells = new ArrayList<>();
		for (int o : PathFinder.NEIGHBOURS8) {
			int c = pos + o;
			if (Dungeon.level.passable[c] && !Dungeon.level.avoid[c] && Actor.findChar(c) == null) cells.add(c);
		}
		for (int i = 0; i < n && !cells.isEmpty(); i++) {
			Mob m = Reflection.newInstance(cls);
			if (m == null) return;
			m.pos = cells.remove(Random.Int(cells.size()));
			m.state = m.HUNTING;
			GameScene.add(m);
			Buff.affect(m, Minion.class);
			Buff.affect(m, AscentHooks.Herald.class);
			if (Dungeon.hero != null) m.beckon(Dungeon.hero.pos);
		}
	}

	/** marks a boss's minion */
	public static class Minion extends Buff {
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("ascent_tier", tier);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		tier = bundle.getInt("ascent_tier");
	}
}
