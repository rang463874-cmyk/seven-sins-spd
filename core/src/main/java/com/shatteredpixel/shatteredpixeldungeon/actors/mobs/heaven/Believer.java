package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.heaven;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeavenSprites;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * A believer of the heavens: plain stats for its depth, comes in groups of 2-3 (HeavenLevel adds the
 * companions). Its death cry draws every enemy within 3 tiles to the hero.
 */
public class Believer extends Mob {

	public static final int CRY_RANGE = 3;

	protected int d = 27;

	{
		spriteClass = HeavenSprites.Believer.class;
		EXP = 14;
		maxLvl = 32;
	}

	public Believer() {
		super();
		d = HeavenMobs.depth();
		HP = HT = 3 * d + 10;
		defenseSkill = d + 4;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(d / 2, d);
	}

	@Override
	public int attackSkill(Char target) {
		return d + 14;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, d / 3);
	}

	@Override
	public void die(Object cause) {
		super.die(cause);
		if (Dungeon.hero == null || !Dungeon.hero.isAlive()) return;
		boolean called = false;
		for (Mob m : HeavenMobs.alliesNear(this, CRY_RANGE)) {
			m.aggro(Dungeon.hero);
			m.beckon(Dungeon.hero.pos);
			called = true;
		}
		if (called && Dungeon.level.heroFOV[pos]) {
			GLog.w(Messages.get(Believer.class, "death_cry"));
		}
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("depth", d);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		if (bundle.contains("depth")) d = bundle.getInt("depth");
	}
}
