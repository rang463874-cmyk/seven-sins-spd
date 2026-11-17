package com.shatteredpixel.shatteredpixeldungeon.solomon;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SolomonArcana;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

/**
 * Seven Sins: Solomon, the hidden eighth class (Nov 17 2026, owner). Unlocked for good once a single run records
 * all 72 demons in the Lemegeton (GoetiaHooks.record); the flag lives in the game settings, so it survives runs.
 */
public final class SolomonHooks {

	private SolomonHooks() {}

	public static boolean unlocked() {
		return SPDSettings.solomonUnlocked();
	}

	/** from GoetiaHooks.record when the codex is complete */
	public static void unlock() {
		if (unlocked()) return;
		SPDSettings.solomonUnlocked(true);
		GLog.p(Messages.get(SolomonHooks.class, "unlocked"));
	}

	public static boolean is(Hero hero) {
		return hero != null && hero.heroClass == HeroClass.SOLOMON;
	}

	/** from Hero.updateHT */
	public static int bonusHT(Hero hero, int ht) {
		SolomonArcana a = hero.buff(SolomonArcana.class);
		return a == null ? ht : ht + a.hpBonus();
	}

	/** from Hero.defenseSkill */
	public static float evasion(Hero hero, float evasion) {
		SolomonArcana a = hero.buff(SolomonArcana.class);
		return a == null ? evasion : evasion * a.evasionMult();
	}

	/** from GoetiaHooks.record: every demon recorded teaches Solomon a little more */
	public static void onRecorded(Hero hero) {
		if (hero == null) return;
		SolomonArcana a = hero.buff(SolomonArcana.class);
		if (a != null) a.addWisdom(WISDOM_PER_DEMON);
	}

	public static final float WISDOM_PER_DEMON = 3f;
}
