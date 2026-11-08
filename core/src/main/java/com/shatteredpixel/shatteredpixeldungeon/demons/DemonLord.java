package com.shatteredpixel.shatteredpixeldungeon.demons;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.BruteSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GolemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ScorpioSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.StatueSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.SuccubusSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.WarlockSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ElementalSprite;

/**
 * The seven princes of hell (Binsfeld's classification), one per sin. Each sets a trial (a penalty for
 * the prison, floors 6-10), then grants a skill (the demon seal) and a passive after Tengu, and can be
 * fought as a boss. Sin index follows the usual order: gluttony 0, greed 1, wrath 2, pride 3, lust 4,
 * envy 5, sloth 6.
 */
public enum DemonLord {

	LUCIFER   (3, 0xFFE070),
	MAMMON    (1, 0xFFB000),
	ASMODEUS  (4, 0xFF5AAA),
	LEVIATHAN (5, 0x40A0FF),
	BEELZEBUB (0, 0x90C030),
	SATAN     (2, 0xFF3020),
	BELPHEGOR (6, 0x9070E0);

	public final int sin;
	public final int color;

	DemonLord(int sin, int color) {
		this.sin = sin;
		this.color = color;
	}

	public String key() {
		return name().toLowerCase();
	}

	public String title() {
		return Messages.get(DemonLord.class, key() + "_name");
	}

	/** "Lucifer, Prince of Pride" */
	public String fullTitle() {
		return Messages.get(DemonLord.class, key() + "_title");
	}

	public String get(String part, Object... args) {
		return Messages.get(DemonLord.class, key() + "_" + part, args);
	}

	public static DemonLord forSin(int sin) {
		for (DemonLord d : values()) if (d.sin == sin) return d;
		return null;
	}

	/** placeholder art: a tinted base sprite per lord */
	public Class<? extends CharSprite> baseSprite() {
		switch (this) {
			case LUCIFER:   return StatueSprite.class;
			case MAMMON:    return GolemSprite.class;
			case ASMODEUS:  return SuccubusSprite.class;
			case LEVIATHAN: return ElementalSprite.Frost.class;
			case BEELZEBUB: return ScorpioSprite.class;
			case SATAN:     return WarlockSprite.class;
			default:        return BruteSprite.class;
		}
	}
}
