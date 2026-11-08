package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.heaven;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeavenSprites;
import com.watabou.utils.Bundle;

/**
 * One of the three extra tiles a seraph fills (it is 2x2). Invisible, never acts; hits on it land on
 * the seraph. Moves with it and vanishes when it dies.
 */
public class SeraphPart extends Mob {

	{
		spriteClass = HeavenSprites.Blank.class;
		HP = HT = 1000;
		EXP = 0;
		maxLvl = -2;
		state = PASSIVE;
		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
	}

	/** the seraph's actor id */
	public int owner = -1;

	public Seraph owner() {
		Actor a = Actor.findById(owner);
		return a instanceof Seraph && ((Seraph) a).isAlive() ? (Seraph) a : null;
	}

	@Override
	protected boolean act() {
		if (owner() == null) {
			vanish();
			return true;
		}
		spend(TICK);
		return true;
	}

	public void vanish() {
		destroy();
		if (sprite != null) sprite.killAndErase();
	}

	@Override
	public void damage(int dmg, Object src) {
		Seraph s = owner();
		if (s != null) s.damage(dmg, src);
	}

	@Override
	public int defenseSkill(Char enemy) {
		Seraph s = owner();
		return s == null ? 0 : s.defenseSkill(enemy);
	}

	@Override
	public int drRoll() {
		Seraph s = owner();
		return s == null ? 0 : s.drRoll();
	}

	@Override
	public boolean isInvulnerable(Class effect) {
		Seraph s = owner();
		return s == null || s.isInvulnerable(effect);
	}

	@Override
	public boolean add(Buff buff) {
		return false;
	}

	@Override
	public boolean reset() {
		return true;
	}

	@Override
	public void beckon(int cell) {
		//it is only a body
	}

	@Override
	public String name() {
		return Messages.get(Seraph.class, "name");
	}

	@Override
	public String description() {
		return Messages.get(Seraph.class, "desc");
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("owner", owner);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		owner = bundle.getInt("owner");
	}
}
