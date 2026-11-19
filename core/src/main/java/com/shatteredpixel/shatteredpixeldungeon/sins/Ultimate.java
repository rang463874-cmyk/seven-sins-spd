package com.shatteredpixel.shatteredpixeldungeon.sins;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.skills.SinSkill;

import java.util.ArrayList;

/**
 * Seven Sins: ultimate skills (Nov 17 2026, owner). Every sin has six: at hero level 10 it learns one of its
 * three tier 1 ultimates, at level 20 one of its three tier 2 ones (UltimateSkills / actors/buffs/UltimateBook).
 * Solomon learns his at 15 / 25 (`level`). Ultimates cost nothing but a cooldown. Icons: skill_icons.png row 11 + sin index (88 + 8k + slot).
 * Strings: sins.ultimate.[id] / [id]_desc (tools/strings/ultimates_items_*).
 */
public enum Ultimate {

	// ---- gluttony
	DEVOUR(HeroClass.GLUTTONY, 1, 0, SinSkill.TARGET_ENEMY, 80),
	ACID_RAIN(HeroClass.GLUTTONY, 1, 1, SinSkill.TARGET_NONE, 80),
	IRON_STOMACH(HeroClass.GLUTTONY, 1, 2, SinSkill.TARGET_NONE, 100),
	ABYSS_MAW(HeroClass.GLUTTONY, 2, 3, SinSkill.TARGET_NONE, 150),
	FEAST(HeroClass.GLUTTONY, 2, 4, SinSkill.TARGET_NONE, 200),
	GLUTTON_AVATAR(HeroClass.GLUTTONY, 2, 5, SinSkill.TARGET_NONE, 150),
	// ---- greed
	GOLD_STORM(HeroClass.GREED, 1, 0, SinSkill.TARGET_NONE, 80),
	BOUNTY(HeroClass.GREED, 1, 1, SinSkill.TARGET_ENEMY, 80),
	GOLD_ARMOR(HeroClass.GREED, 1, 2, SinSkill.TARGET_NONE, 100),
	MIDAS(HeroClass.GREED, 2, 3, SinSkill.TARGET_ENEMY, 150),
	COMPOUND(HeroClass.GREED, 2, 4, SinSkill.TARGET_NONE, 200),
	GOLDEN_AGE(HeroClass.GREED, 2, 5, SinSkill.TARGET_NONE, 150),
	// ---- wrath
	QUAKE(HeroClass.WRATH, 1, 0, SinSkill.TARGET_NONE, 80),
	BLOOD_RAGE(HeroClass.WRATH, 1, 1, SinSkill.TARGET_NONE, 100),
	VENDETTA(HeroClass.WRATH, 1, 2, SinSkill.TARGET_ENEMY, 80),
	BERSERK(HeroClass.WRATH, 2, 3, SinSkill.TARGET_NONE, 200),
	ERUPTION(HeroClass.WRATH, 2, 4, SinSkill.TARGET_NONE, 150),
	WAR_DRUMS(HeroClass.WRATH, 2, 5, SinSkill.TARGET_NONE, 150),
	// ---- pride
	JUDGMENT(HeroClass.PRIDE, 1, 0, SinSkill.TARGET_PROJECTILE, 80),
	KNEEL(HeroClass.PRIDE, 1, 1, SinSkill.TARGET_NONE, 100),
	AEGIS(HeroClass.PRIDE, 1, 2, SinSkill.TARGET_NONE, 100),
	BLADE_RAIN(HeroClass.PRIDE, 2, 3, SinSkill.TARGET_NONE, 150),
	ABSOLUTE(HeroClass.PRIDE, 2, 4, SinSkill.TARGET_NONE, 150),
	DIVINE_RIGHT(HeroClass.PRIDE, 2, 5, SinSkill.TARGET_NONE, 150),
	// ---- lust
	DANCE(HeroClass.LUST, 1, 0, SinSkill.TARGET_NONE, 80),
	BLOOD_FEAST(HeroClass.LUST, 1, 1, SinSkill.TARGET_NONE, 80),
	SHADOW_DANCE(HeroClass.LUST, 1, 2, SinSkill.TARGET_NONE, 100),
	THRALL(HeroClass.LUST, 2, 3, SinSkill.TARGET_ENEMY, 200),
	CRIMSON_NIGHT(HeroClass.LUST, 2, 4, SinSkill.TARGET_NONE, 150),
	FATAL_KISS(HeroClass.LUST, 2, 5, SinSkill.TARGET_ENEMY, 150),
	// ---- envy
	FLOOD(HeroClass.ENVY, 1, 0, SinSkill.TARGET_NONE, 100),
	MIRROR_IMAGES(HeroClass.ENVY, 1, 1, SinSkill.TARGET_NONE, 100),
	EVIL_EYE(HeroClass.ENVY, 1, 2, SinSkill.TARGET_ENEMY, 80),
	USURP(HeroClass.ENVY, 2, 3, SinSkill.TARGET_ENEMY, 150),
	REFLECTION(HeroClass.ENVY, 2, 4, SinSkill.TARGET_NONE, 150),
	ENVY_STORM(HeroClass.ENVY, 2, 5, SinSkill.TARGET_NONE, 150),
	// ---- sloth
	SLUMBER(HeroClass.SLOTH, 1, 0, SinSkill.TARGET_NONE, 100),
	LEGION(HeroClass.SLOTH, 1, 1, SinSkill.TARGET_NONE, 100),
	REVERIE(HeroClass.SLOTH, 1, 2, SinSkill.TARGET_NONE, 100),
	ETERNAL_SLEEP(HeroClass.SLOTH, 2, 3, SinSkill.TARGET_ENEMY, 150),
	NIGHTMARE(HeroClass.SLOTH, 2, 4, SinSkill.TARGET_NONE, 150),
	DAYDREAM(HeroClass.SLOTH, 2, 5, SinSkill.TARGET_NONE, 150),
	// ---- Solomon (Nov 19 2026, owner: learned at levels 15 and 25, between his principles at 10 and 20)
	LEGION_GOETIA(HeroClass.SOLOMON, 1, 0, SinSkill.TARGET_NONE, 150),
	ETERNAL_CIRCLE(HeroClass.SOLOMON, 1, 1, SinSkill.TARGET_NONE, 120),
	RING_COMMAND(HeroClass.SOLOMON, 1, 2, SinSkill.TARGET_ENEMY, 100),
	BRASS_VESSEL(HeroClass.SOLOMON, 2, 3, SinSkill.TARGET_ENEMY, 200),
	MAGNUM_OPUS(HeroClass.SOLOMON, 2, 4, SinSkill.TARGET_NONE, 180),
	THRONE_OF_WISDOM(HeroClass.SOLOMON, 2, 5, SinSkill.TARGET_NONE, 200);

	/** hero level that opens each tier's choice */
	public static final int TIER1_LEVEL = 10;
	public static final int TIER2_LEVEL = 20;
	/** Solomon learns his at 15 and 25 (his principles come at 10 and 20) */
	public static final int SOLOMON_TIER1_LEVEL = 15;
	public static final int SOLOMON_TIER2_LEVEL = 25;
	/** Solomon's ultimates sit in skill_icons.png row 20, after the salt principle (160) */
	public static final int SOLOMON_ICONS = 161;

	/** hero level that opens a class's tier choice */
	public static int level(HeroClass cls, int tier) {
		if (cls == HeroClass.SOLOMON) return tier == 1 ? SOLOMON_TIER1_LEVEL : SOLOMON_TIER2_LEVEL;
		return tier == 1 ? TIER1_LEVEL : TIER2_LEVEL;
	}
	/** icon of the "learn an ultimate" button (skill_icons.png row 18) */
	public static final int LEARN_ICON = 144;

	public final HeroClass sin;
	public final int tier;
	public final int icon;
	public final int targeting;
	public final int cooldown;

	Ultimate(HeroClass sin, int tier, int slot, int targeting, int cooldown) {
		this.sin = sin;
		this.tier = tier;
		this.icon = sin == HeroClass.SOLOMON ? SOLOMON_ICONS + slot
				: 88 + 8 * (sin.ordinal() - HeroClass.GLUTTONY.ordinal()) + slot;
		this.targeting = targeting;
		this.cooldown = cooldown;
	}

	public String id() {
		return name().toLowerCase();
	}

	public String title() {
		return Messages.get(Ultimate.class, id());
	}

	public String desc(Object... args) {
		return Messages.get(Ultimate.class, id() + "_desc", args);
	}

	public static ArrayList<Ultimate> choices(HeroClass sin, int tier) {
		ArrayList<Ultimate> out = new ArrayList<>();
		for (Ultimate u : values()) if (u.sin == sin && u.tier == tier) out.add(u);
		return out;
	}

	public static Ultimate byName(String name) {
		if (name == null) return null;
		try {
			return valueOf(name);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
