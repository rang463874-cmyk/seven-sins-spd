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

	/**
	 * Seven Sins (Nov 20 2026, owner): it breaks the dungeon on its way. It walks straight at its target through walls
	 * (smashing them), and every tile it leaves crumbles into a ruin nothing can enter again (AscentHooks.ruin).
	 */
	@Override
	protected boolean getCloser(int target) {
		if (rooted || target == pos || paralysed > 0) return false;
		int w = Dungeon.level.width();
		int best = -1;
		for (int n : com.watabou.utils.PathFinder.NEIGHBOURS8) {
			int c = pos + n;
			if (c < 0 || c >= Dungeon.level.length()) continue;
			int x = c % w, y = c / w;
			if (x <= 0 || y <= 0 || x >= w - 1 || y >= Dungeon.level.height() - 1) continue;
			if (com.shatteredpixel.shatteredpixeldungeon.ascent.AscentHooks.isRuin(c)) continue;
			if (com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(c) != null) continue;
			if (best == -1 || Dungeon.level.trueDistance(c, target) < Dungeon.level.trueDistance(best, target)) best = c;
		}
		if (best == -1 || Dungeon.level.trueDistance(best, target) >= Dungeon.level.trueDistance(pos, target)) {
			return super.getCloser(target);
		}
		if (Dungeon.level.solid[best]) {
			com.shatteredpixel.shatteredpixeldungeon.ascent.AscentHooks.smash(best);
			if (Dungeon.level.solid[best]) return super.getCloser(target);
			com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.shake(1, 0.2f);
		}
		int old = pos;
		move(best);
		com.shatteredpixel.shatteredpixeldungeon.ascent.AscentHooks.ruin(old);
		return true;
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
