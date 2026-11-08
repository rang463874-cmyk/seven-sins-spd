package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.heaven;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeavenSprites;
import com.watabou.utils.Bundle;

/**
 * One of the holy giant's fists, lying where it slammed for a few turns. It doesn't fight back; hitting
 * it hurts the giant (HP is the arm's, kept by the giant between slams). Destroyed at 0 HP.
 */
public class GiantFist extends Mob {

	public int giant = -1;
	/** 0 = left, 1 = right */
	public int side = 0;
	public int turns = HolyGiant.FIST_TURNS;

	{
		spriteClass = HeavenSprites.GiantFist.class;
		HP = HT = HolyGiant.ARM_HT;
		defenseSkill = 0;
		EXP = 0;
		maxLvl = -2;
		state = PASSIVE;
		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
		properties.add(Property.MINIBOSS);
	}

	public HolyGiant giant() {
		Actor a = Actor.findById(giant);
		return a instanceof HolyGiant && ((HolyGiant) a).isAlive() ? (HolyGiant) a : null;
	}

	@Override
	protected boolean act() {
		HolyGiant g = giant();
		if (g == null) {
			destroy();
			if (sprite != null) sprite.killAndErase();
			return true;
		}
		if (--turns <= 0) {
			g.onFistRetracted(side, HP);
			if (sprite != null) CellEmitter.get(pos).burst(Speck.factory(Speck.DUST), 6);
			destroy();
			if (sprite != null) sprite.killAndErase();
			return true;
		}
		spend(TICK);
		return true;
	}

	@Override
	public void damage(int dmg, Object src) {
		super.damage(dmg, src);
		HolyGiant g = giant();
		if (g != null && isAlive()) g.onFistDamaged(side, HP);
	}

	@Override
	public void die(Object cause) {
		HolyGiant g = giant();
		super.die(cause);
		if (g != null) g.onFistDestroyed(side);
	}

	@Override
	public int drRoll() {
		return super.drRoll() + com.watabou.utils.Random.NormalIntRange(0, 8);
	}

	@Override
	public boolean add(Buff buff) {
		return false;
	}

	@Override
	public void beckon(int cell) {
	}

	@Override
	public String name() {
		return Messages.get(this, side == 0 ? "name_left" : "name_right");
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("giant", giant);
		bundle.put("side", side);
		bundle.put("turns", turns);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		giant = bundle.getInt("giant");
		side = bundle.getInt("side");
		turns = bundle.getInt("turns");
	}
}
