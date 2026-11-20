package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.elites.RegionElite;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * Seven Sins: the giant that chases the hero through the sewers on the way up (Nov 20 2026, owner): Behemoth for the
 * demons' faction, Metatron for the angels'. AscentHooks raises one at the stairs below a few turns after the hero
 * reaches each of floors 5..1. It can't be killed: blows fill a stagger meter that stuns it for a few turns.
 */
public abstract class AscentChaser extends RegionElite {

	public static final int STAGGER = 90;

	{
		HP = HT = 1000;
		defenseSkill = 20;
		EXP = 0;
		maxLvl = -2;
		baseSpeed = 0.8f;
		viewDistance = 12;
		properties.remove(Property.MINIBOSS);
		properties.add(Property.BOSS);
		immunities.add(AllyBuff.class);
		immunities.add(Charm.class);
		immunities.add(Amok.class);
		immunities.add(Terror.class);
		immunities.add(Sleep.class);
		immunities.add(MagicalSleep.class);
		immunities.add(Dread.class);
	}

	private float stagger = 0;

	protected int skillMin() {
		return 22;
	}

	protected int skillMax() {
		return 40;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(20, 38);
	}

	@Override
	public int attackSkill(Char target) {
		return 45;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(6, 16);
	}

	@Override
	protected boolean dropsReward() {
		return false;
	}

	@Override
	protected boolean act() {
		if (Dungeon.hero != null && Dungeon.hero.isAlive()
				&& (fieldOfView == null || fieldOfView.length != Dungeon.level.length() || !fieldOfView[Dungeon.hero.pos])) {
			beckon(Dungeon.hero.pos);
		}
		return super.act();
	}

	/** blows only stagger it */
	@Override
	public void damage(int dmg, Object src) {
		if (dmg <= 0 || !isAlive()) return;
		if (sprite != null) {
			sprite.flash();
			sprite.showStatus(CharSprite.NEUTRAL, Integer.toString(dmg));
		}
		stagger += dmg;
		if (stagger >= STAGGER) {
			stagger = 0;
			Buff.prolong(this, Paralysis.class, 3f);
			if (sprite != null) sprite.showStatus(CharSprite.POSITIVE, Messages.get(AscentChaser.class, "staggered"));
		}
	}

	/** nothing slays it: whatever would have, staggers it longer */
	@Override
	public void die(Object cause) {
		if (cause == AscentChaser.class) {
			super.die(cause);
			return;
		}
		HP = HT;
		stagger = 0;
		Buff.prolong(this, Paralysis.class, 5f);
		if (sprite != null) sprite.showStatus(CharSprite.POSITIVE, Messages.get(AscentChaser.class, "staggered"));
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("stagger", stagger);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		stagger = bundle.getFloat("stagger");
	}
}
