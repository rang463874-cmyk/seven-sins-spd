package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.heaven;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeavenSprites;
import com.watabou.utils.Bundle;

/**
 * A tile of the holy giant's arm lying across the floor after a slam (the fist is at its end). Invisible
 * (the fist's sprite draws the whole arm), never acts, blocks the way; hits on it land on the fist.
 */
public class GiantArmPart extends Mob {

	{
		spriteClass = HeavenSprites.Blank.class;
		HP = HT = 1000;
		EXP = 0;
		maxLvl = -2;
		state = PASSIVE;
		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
	}

	/** the fist's actor id */
	public int owner = -1;

	public GiantFist owner() {
		Actor a = Actor.findById(owner);
		return a instanceof GiantFist && ((GiantFist) a).isAlive() ? (GiantFist) a : null;
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
		GiantFist f = owner();
		if (f != null) f.damage(dmg, src);
	}

	@Override
	public int defenseSkill(Char enemy) {
		return 0;
	}

	@Override
	public int drRoll() {
		GiantFist f = owner();
		return f == null ? 0 : f.drRoll();
	}

	@Override
	public boolean isInvulnerable(Class effect) {
		GiantFist f = owner();
		return f == null || f.isInvulnerable(effect);
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
	}

	@Override
	public String name() {
		GiantFist f = owner();
		return f != null ? f.name() : Messages.get(GiantFist.class, "name_left");
	}

	@Override
	public String description() {
		return Messages.get(GiantFist.class, "desc");
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
