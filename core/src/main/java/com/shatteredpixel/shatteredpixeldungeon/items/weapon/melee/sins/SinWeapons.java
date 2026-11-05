package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.sins;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Reflection;

/**
 * Helpers for the seven sins' signature weapons. Anyone can wield them,
 * but their special effects only work for the sin they belong to.
 */
public final class SinWeapons {

	private SinWeapons() {}

	/** true if ch is a hero of the given class */
	public static boolean active(Char ch, HeroClass owner) {
		return ch instanceof Hero && ((Hero) ch).heroClass == owner;
	}

	/** true if the hero wields a weapon of this type and is of its sin */
	public static boolean wields(Hero hero, Class<? extends KindOfWeapon> cls, HeroClass owner) {
		if (hero == null || hero.heroClass != owner) return false;
		KindOfWeapon w = hero.belongings.weapon();
		return w != null && cls.isInstance(w);
	}

	/** approximate check for a hit that will kill (used before damage is applied) */
	public static boolean lethal(Char defender, int damage) {
		return damage >= defender.HP + defender.shielding();
	}

	/** the tier 5 signature weapon for a sin, or null for other classes */
	public static Class<? extends MeleeWeapon> tier5(HeroClass cls) {
		switch (cls) {
			case GLUTTONY: return GluttonScythe.class;
			case GREED:    return MidasAxe.class;
			case WRATH:    return WrathGreatsword.class;
			case PRIDE:    return KingsAegis.class;
			case LUST:     return RoseGlaive.class;
			case ENVY:     return UsurpersGauntlets.class;
			case SLOTH:    return DreamHammer.class;
			default:       return null;
		}
	}

	public static MeleeWeapon newTier5(HeroClass cls) {
		Class<? extends MeleeWeapon> c = tier5(cls);
		return c == null ? null : Reflection.newInstance(c);
	}

	/** extra description line shown on every sin weapon */
	public static String ownerNote(HeroClass owner, Hero viewer) {
		String key = (viewer != null && viewer.heroClass == owner) ? "sin_active" : "sin_inactive";
		return Messages.get(SinWeapons.class, key, owner.title());
	}
}
