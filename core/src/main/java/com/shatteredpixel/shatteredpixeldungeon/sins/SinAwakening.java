package com.shatteredpixel.shatteredpixeldungeon.sins;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/**
 * The second awakening of a sin hero, granted by the holy gem: master the sin (CONTROL: plays as before,
 * with solid advantages) or become the sin's monster (MONSTER: hard drawbacks, great power). The choice
 * is stored on the hero (Hero.awakening) with POINTS talent points to spend on the path's talents.
 */
public class SinAwakening {

	public static final int NONE = 0, CONTROL = 1, MONSTER = 2;
	public static final int POINTS = 8;

	public static boolean isSin(Hero hero) {
		return hero != null && hero.heroClass.ordinal() >= HeroClass.GLUTTONY.ordinal()
				&& hero.heroClass.ordinal() <= HeroClass.SLOTH.ordinal();
	}

	public static boolean canAwaken(Hero hero) {
		return isSin(hero) && hero.awakening == NONE;
	}

	public static void awaken(Hero hero, int path) {
		if (!canAwaken(hero) || (path != CONTROL && path != MONSTER)) return;
		hero.awakening = path;
		hero.awakeningPoints = POINTS;
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(hero, AwakeningHooks.AwakeningTicker.class);
		AwakeningHooks.onAwakened(hero);
	}

	/** e.g. "식탐의 제어자" / "식탐의 괴물" */
	public static String pathName(HeroClass cls, int path) {
		String key = cls.name().toLowerCase();
		return Messages.get(SinAwakening.class, (path == MONSTER ? "monster_" : "control_") + key);
	}

	public static String pathDesc(HeroClass cls, int path) {
		String key = cls.name().toLowerCase();
		return Messages.get(SinAwakening.class, (path == MONSTER ? "monster_" : "control_") + key + "_desc");
	}
}
