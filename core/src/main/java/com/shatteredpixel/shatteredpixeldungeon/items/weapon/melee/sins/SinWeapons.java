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
		if (w == null) return false;
		if (cls.isInstance(w)) return true;
		//a weapon with the sigil of sin counts as the bearer's tier 5 sin weapon
		return w instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon
				&& ((com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon) w).sinSigil && cls == tier5(owner);
	}

	/** the tier 5 effect for a hit with a weapon carrying the sigil of sin (the hero's own sin) */
	public static int sigilProc(Hero hero, com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon w,
	                            Char defender, int damage) {
		Class<? extends MeleeWeapon> t5 = tier5(hero.heroClass);
		if (t5 == null || t5.isInstance(w)) return damage;
		switch (hero.heroClass) {
			case GLUTTONY: return GluttonScythe.effect(hero, w, defender, damage);
			case GREED:    return MidasAxe.effect(hero, w, defender, damage);
			case WRATH:    return WrathGreatsword.effect(hero, w, defender, damage);
			case PRIDE:    return damage; //passive only (ranged bonus, max dignity)
			case LUST:     return RoseGlaive.effect(hero, w, defender, damage);
			case ENVY:     return UsurpersGauntlets.effect(hero, w, defender, damage);
			case SLOTH:    return DreamHammer.effect(hero, w, defender, damage);
			default:       return damage;
		}
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
