package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.DemonSprites;
import com.watabou.utils.Bundle;

/**
 * One piece of Leviathan's body, lying along the arena wall in its wall form. It never moves or acts;
 * hits on it land on Leviathan itself (same evasion and armor), so the whole wall is a target.
 * Vanishes when Leviathan dies.
 */
public class LeviathanCoil extends Mob {

	{
		spriteClass = DemonSprites.Coil.class;
		HP = HT = 1000;
		EXP = 0;
		maxLvl = -2;
		state = PASSIVE;
		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
		properties.add(Property.DEMONIC);
	}

	/** Leviathan's actor id */
	public int head = -1;
	/** DemonSprites.Coil variant */
	public int variant = DemonSprites.Coil.HUMP;
	public boolean flip = false;

	public LeviathanBoss head() {
		Actor a = Actor.findById(head);
		return a instanceof LeviathanBoss && ((LeviathanBoss) a).isAlive() ? (LeviathanBoss) a : null;
	}

	@Override
	protected boolean act() {
		if (head() == null) {
			vanish();
			return true;
		}
		spend(TICK);
		return true;
	}

	public void vanish() {
		destroy();
		if (sprite != null) sprite.die();
	}

	@Override
	public void damage(int dmg, Object src) {
		LeviathanBoss h = head();
		if (h != null) {
			h.damage(dmg, src);
			if (sprite != null) sprite.flash();
		}
	}

	@Override
	public int defenseSkill(Char enemy) {
		LeviathanBoss h = head();
		return h == null ? 0 : h.defenseSkill(enemy);
	}

	@Override
	public int drRoll() {
		LeviathanBoss h = head();
		return h == null ? 0 : h.drRoll();
	}

	@Override
	public boolean isInvulnerable(Class effect) {
		LeviathanBoss h = head();
		return h == null || h.isInvulnerable(effect);
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
		return Messages.get(this, "name");
	}

	@Override
	public String description() {
		return Messages.get(this, "desc");
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("head", head);
		bundle.put("variant", variant);
		bundle.put("flip", flip);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		head = bundle.getInt("head");
		variant = bundle.getInt("variant");
		flip = bundle.getBoolean("flip");
	}
}
