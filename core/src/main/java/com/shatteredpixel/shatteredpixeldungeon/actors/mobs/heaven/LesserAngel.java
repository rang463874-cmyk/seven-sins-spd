package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.heaven;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ShieldBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeavenSprites;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * A lesser angel, only ever summoned by a priest's sacrifice. It cannot move and throws white spears
 * that hit hard. It arrives wrapped in radiance (a shield that wears away every turn, like a brute's
 * rage) and fades away after LIFESPAN turns.
 */
public class LesserAngel extends Mob {

	public static final int LIFESPAN = 20;

	protected int d = 27;
	private int life = LIFESPAN;
	private boolean shielded = false;

	{
		spriteClass = HeavenSprites.LesserAngel.class;
		EXP = 8;
		maxLvl = 32;
		state = HUNTING;
		properties.add(Property.IMMOVABLE);
		flying = true;
	}

	public LesserAngel() {
		super();
		d = HeavenMobs.depth();
		HP = HT = d + 10;
		defenseSkill = d / 2;
	}

	@Override
	protected boolean act() {
		if (!shielded) {
			shielded = true;
			Buff.affect(this, Radiance.class).setShield(2 * d);
		}
		if (--life <= 0) {
			if (sprite != null) CellEmitter.get(pos).burst(Speck.factory(Speck.LIGHT), 8);
			die(null);
			return true;
		}
		return super.act();
	}

	@Override
	protected boolean canAttack(Char enemy) {
		return new Ballistica(pos, enemy.pos, Ballistica.PROJECTILE).collisionPos == enemy.pos;
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
	public int damageRoll() {
		return Random.NormalIntRange(d / 2 + 3, d + 6);
	}

	@Override
	public int attackSkill(Char target) {
		return d + 18;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("depth", d);
		bundle.put("life", life);
		bundle.put("shielded", shielded);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		if (bundle.contains("depth")) d = bundle.getInt("depth");
		life = bundle.getInt("life");
		shielded = bundle.getBoolean("shielded");
	}

	/** the angel's radiance: a shield that loses a share of its strength every turn */
	public static class Radiance extends ShieldBuff {

		{
			type = buffType.POSITIVE;
		}

		@Override
		public boolean act() {
			absorbDamage(Math.max(2, (int) Math.ceil(shielding() / 8f)));
			if (shielding() <= 0) {
				detach();
				return true;
			}
			spend(TICK);
			return true;
		}
	}
}
