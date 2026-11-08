package com.shatteredpixel.shatteredpixeldungeon.virtues;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.virtues.CharityChampion;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.virtues.ChastityChampion;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.virtues.DiligenceChampion;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.virtues.HumilityChampion;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.virtues.KindnessChampion;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.virtues.PatienceChampion;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.virtues.TemperanceChampion;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.virtues.VirtueChampion;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/**
 * The seven heavenly virtues, each the enemy of one sin (sin index: gluttony 0, greed 1, wrath 2, pride 3,
 * lust 4, envy 5, sloth 6). Their champions (paladins and saints) hunt the hero through the dungeon.
 */
public enum Virtue {
	HUMILITY(3, 0xFFF0C8, HumilityChampion.class),
	CHARITY(1, 0xFFD050, CharityChampion.class),
	CHASTITY(4, 0xF8E8FF, ChastityChampion.class),
	PATIENCE(2, 0x80C8FF, PatienceChampion.class),
	TEMPERANCE(0, 0xA0E0A0, TemperanceChampion.class),
	KINDNESS(5, 0xFFC8A0, KindnessChampion.class),
	DILIGENCE(6, 0xFFA040, DiligenceChampion.class);

	/** the sin this virtue opposes */
	public final int sin;
	public final int color;
	public final Class<? extends VirtueChampion> champion;

	Virtue(int sin, int color, Class<? extends VirtueChampion> champion) {
		this.sin = sin;
		this.color = color;
		this.champion = champion;
	}

	/** the archangel who stands for this virtue (the final boss after Yog) */
	public String angel() {
		return Messages.get(Virtue.class, name().toLowerCase() + "_angel");
	}

	public String title() {
		return Messages.get(Virtue.class, name().toLowerCase());
	}

	public static Virtue against(int sin) {
		for (Virtue v : values()) if (v.sin == sin) return v;
		return null;
	}
}
