package com.shatteredpixel.shatteredpixeldungeon.sins;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

import java.util.ArrayList;

/**
 * The second awakening's talents: 3 per path (7 sins x control/monster), up to 3 points each, paid from
 * Hero.awakeningPoints (8 at the awakening). Levels are kept in Hero.awakeningTalents. Effects read
 * {@link #pts(Char, AwakeningTalent)} at their hook sites (see each sin's *Hooks / buff).
 */
public enum AwakeningTalent {

	//gluttony
	G_DIGESTION(HeroClass.GLUTTONY, SinAwakening.CONTROL),
	G_RESTRAINT(HeroClass.GLUTTONY, SinAwakening.CONTROL),
	G_NOURISH(HeroClass.GLUTTONY, SinAwakening.CONTROL),
	G_STOMACH(HeroClass.GLUTTONY, SinAwakening.MONSTER),
	G_ACID(HeroClass.GLUTTONY, SinAwakening.MONSTER),
	G_DASH(HeroClass.GLUTTONY, SinAwakening.MONSTER),
	//greed
	R_INTEREST(HeroClass.GREED, SinAwakening.CONTROL),
	R_DISCOUNT(HeroClass.GREED, SinAwakening.CONTROL),
	R_GOLDSHIELD(HeroClass.GREED, SinAwakening.CONTROL),
	R_GOLDSKIN(HeroClass.GREED, SinAwakening.MONSTER),
	R_PLUNDER(HeroClass.GREED, SinAwakening.MONSTER),
	R_GOLDFIST(HeroClass.GREED, SinAwakening.MONSTER),
	//wrath
	W_SIMMER(HeroClass.WRATH, SinAwakening.CONTROL),
	W_PRECISE(HeroClass.WRATH, SinAwakening.CONTROL),
	W_TEMPER(HeroClass.WRATH, SinAwakening.CONTROL),
	W_BLOODLUST(HeroClass.WRATH, SinAwakening.MONSTER),
	W_UNDYING(HeroClass.WRATH, SinAwakening.MONSTER),
	W_SLAUGHTER(HeroClass.WRATH, SinAwakening.MONSTER),
	//pride
	P_POISE(HeroClass.PRIDE, SinAwakening.CONTROL),
	P_ROYALSHOT(HeroClass.PRIDE, SinAwakening.CONTROL),
	P_GLORY(HeroClass.PRIDE, SinAwakening.CONTROL),
	P_ABSOLUTE(HeroClass.PRIDE, SinAwakening.MONSTER),
	P_REIGN(HeroClass.PRIDE, SinAwakening.MONSTER),
	P_DIVINEEYE(HeroClass.PRIDE, SinAwakening.MONSTER),
	//lust
	L_PRICE(HeroClass.LUST, SinAwakening.CONTROL),
	L_DEEPCHARM(HeroClass.LUST, SinAwakening.CONTROL),
	L_ELEGANT(HeroClass.LUST, SinAwakening.CONTROL),
	L_FEAST(HeroClass.LUST, SinAwakening.MONSTER),
	L_NIGHTDANCE(HeroClass.LUST, SinAwakening.MONSTER),
	L_AURA(HeroClass.LUST, SinAwakening.MONSTER),
	//envy
	E_SLOTS(HeroClass.ENVY, SinAwakening.CONTROL),
	E_THRIFT(HeroClass.ENVY, SinAwakening.CONTROL),
	E_GROWTH(HeroClass.ENVY, SinAwakening.CONTROL),
	E_STEAL(HeroClass.ENVY, SinAwakening.MONSTER),
	E_RIVALRY(HeroClass.ENVY, SinAwakening.MONSTER),
	E_TAKEFORM(HeroClass.ENVY, SinAwakening.MONSTER),
	//sloth
	S_NAP(HeroClass.SLOTH, SinAwakening.CONTROL),
	S_PUPPETS(HeroClass.SLOTH, SinAwakening.CONTROL),
	S_MORE(HeroClass.SLOTH, SinAwakening.CONTROL),
	S_LEGION(HeroClass.SLOTH, SinAwakening.MONSTER),
	S_VEIL(HeroClass.SLOTH, SinAwakening.MONSTER),
	S_SLEEPWALK(HeroClass.SLOTH, SinAwakening.MONSTER);

	public static final int MAX_POINTS = 3;

	public final HeroClass sin;
	public final int path;

	AwakeningTalent(HeroClass sin, int path) {
		this.sin = sin;
		this.path = path;
	}

	public String title() {
		return Messages.get(AwakeningTalent.class, name().toLowerCase() + ".title");
	}

	public String desc() {
		return Messages.get(AwakeningTalent.class, name().toLowerCase() + ".desc");
	}

	/** the talents of the hero's chosen path */
	public static ArrayList<AwakeningTalent> forPath(HeroClass sin, int path) {
		ArrayList<AwakeningTalent> out = new ArrayList<>();
		for (AwakeningTalent t : values()) if (t.sin == sin && t.path == path) out.add(t);
		return out;
	}

	public static int pts(Char ch, AwakeningTalent t) {
		if (!(ch instanceof Hero)) return 0;
		Hero h = (Hero) ch;
		if (h.awakening != t.path || h.heroClass != t.sin) return 0;
		Integer p = h.awakeningTalents.get(t.name());
		return p == null ? 0 : p;
	}

	public static int pts(AwakeningTalent t) {
		return pts(Dungeon.hero, t);
	}

	/** whether the hero is the monster (or master) of this sin */
	public static boolean is(Char ch, HeroClass sin, int path) {
		return ch instanceof Hero && ((Hero) ch).heroClass == sin && ((Hero) ch).awakening == path;
	}

	public static boolean monster(Char ch, HeroClass sin) {
		return is(ch, sin, SinAwakening.MONSTER);
	}

	public static boolean canUpgrade(Hero h, AwakeningTalent t) {
		return h.awakening == t.path && h.heroClass == t.sin && h.awakeningPoints > 0 && pts(h, t) < MAX_POINTS;
	}

	public static void upgrade(Hero h, AwakeningTalent t) {
		if (!canUpgrade(h, t)) return;
		h.awakeningTalents.put(t.name(), pts(h, t) + 1);
		h.awakeningPoints--;
		h.updateHT(false);
	}
}
