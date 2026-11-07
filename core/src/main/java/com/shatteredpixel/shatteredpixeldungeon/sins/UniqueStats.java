package com.shatteredpixel.shatteredpixeldungeon.sins;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnvyCovet;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GreedVault;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LustDesire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PredatorStomach;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PrideGlory;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SlothRepose;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WrathGrudge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Reflection;

import java.lang.reflect.Modifier;
import java.util.HashMap;

/**
 * Each sin's one-of-a-kind stat, on top of the shared ones (melee, accuracy, HP, armor, evasion).
 * All scale with that sin's growth points and are capped:
 *   Wrath: attack speed (+50%)        Gluttony: HP regeneration (1.5 HP/turn)
 *   Greed: loot drop chance (+100%)   Pride: debuff resistance (50%)
 *   Lust: movement speed (+30%)       Envy: rare variant spawns (+300%)
 *   Sloth: slower hunger (50%)
 * Every method is a cheap pass-through for heroes without the matching buff.
 */
public final class UniqueStats {

	private UniqueStats() {}

	// ------------------------------------------------------------------ values

	public static float gluttonyPts(PredatorStomach s) {
		return s.weaponPts + s.armorPts + s.trinketPts;
	}

	/** extra HP regenerated per turn */
	public static float regenPerTurn(PredatorStomach s) {
		return Math.min(1.5f, 0.003f * gluttonyPts(s));
	}

	public static float attackSpeedBonus(WrathGrudge w) {
		return Math.min(0.50f, 0.004f * w.pts());
	}

	public static float lootBonus(GreedVault v) {
		return Math.min(1.00f, 0.02f * v.pts());
	}

	/** fraction by which harmful effects are shortened/weakened */
	public static float debuffResist(PrideGlory p) {
		return Math.min(0.50f, 0.004f * p.pts());
	}

	public static float moveSpeedBonus(LustDesire d) {
		return Math.min(0.30f, 0.003f * d.pts());
	}

	/** extra rare-variant spawn chance (+300% = 4x at most) */
	public static final float RARE_CAP = 3.00f;

	public static float rareBonus(EnvyCovet e) {
		return Math.min(RARE_CAP, 0.02f * e.pts());
	}

	/** RatSkull.exoticChanceMultiplier: multiply by this */
	public static float rareMultiplier() {
		Hero hero = Dungeon.hero;
		EnvyCovet e = hero == null ? null : hero.buff(EnvyCovet.class);
		return e == null ? 1f : 1f + rareBonus(e);
	}

	/** Envy at the cap: kills have a 5% chance to grant mind vision for 3 turns */
	public static void onKill(Hero hero) {
		EnvyCovet e = hero == null ? null : hero.buff(EnvyCovet.class);
		if (e == null || rareBonus(e) < RARE_CAP || com.watabou.utils.Random.Float() >= 0.05f) return;
		Buff.prolong(hero, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MindVision.class, 3f);
		Dungeon.observe();
		com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.updateFog();
		hero.sprite.showStatus(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.POSITIVE,
				Messages.get(UniqueStats.class, "envy_sight"));
	}

	/** fraction by which hunger is slowed */
	public static float hungerSlow(SlothRepose r) {
		return Math.min(0.50f, 0.004f * r.pts());
	}

	// ------------------------------------------------------------------ hooks

	/** Hero.attackDelay: divide the delay by this */
	public static float attackSpeed(Hero hero) {
		WrathGrudge w = hero.buff(WrathGrudge.class);
		return w == null ? 1f : 1f + attackSpeedBonus(w);
	}

	/** Hero.speed: multiply by this */
	public static float moveSpeed(Hero hero) {
		LustDesire d = hero.buff(LustDesire.class);
		return d == null ? 1f : 1f + moveSpeedBonus(d);
	}

	/** Hero.earnExp (Envy used to gain bonus exp; no sin does now) */
	public static int bonusExp(Hero hero, int exp) {
		return exp;
	}

	/** Mob.lootChance: added to the drop bonus */
	public static float lootChance() {
		Hero hero = Dungeon.hero;
		GreedVault v = hero == null ? null : hero.buff(GreedVault.class);
		return v == null ? 0f : lootBonus(v);
	}

	/** whether each buff class is harmful, looked up once per class */
	private static final HashMap<Class<?>, Boolean> harmful = new HashMap<>();

	private static boolean isHarmful(Class<?> effect) {
		Boolean known = harmful.get(effect);
		if (known != null) return known;
		boolean neg = false;
		boolean innerClass = effect.isMemberClass() && !Modifier.isStatic(effect.getModifiers());
		if (Buff.class.isAssignableFrom(effect) && !Modifier.isAbstract(effect.getModifiers()) && !innerClass) {
			Object b = Reflection.newInstance(effect);
			neg = b instanceof Buff && ((Buff) b).type == Buff.buffType.NEGATIVE;
		}
		harmful.put(effect, neg);
		return neg;
	}

	/** Char.resist: multiplier for harmful effects (negative buffs) on the hero */
	public static float resist(Char ch, Class<?> effect) {
		if (!(ch instanceof Hero) || effect == null) return 1f;
		PrideGlory p = ch.buff(PrideGlory.class);
		if (p == null || !isHarmful(effect)) return 1f;
		return 1f - debuffResist(p);
	}

	/** Hunger.act: hunger delay is divided by this (lower = slower hunger) */
	public static float hungerRate(Char ch) {
		SlothRepose r = ch.buff(SlothRepose.class);
		return r == null ? 1f : 1f - hungerSlow(r);
	}

	/** partial HP carried between turns for gluttony's regeneration */
	private static float partialRegen = 0;

	/** Hunger.act, once per turn: gluttony's regeneration */
	public static void tick(Hero hero) {
		if (!hero.isAlive()) return;
		PredatorStomach s = hero.buff(PredatorStomach.class);
		if (s == null || hero.HP >= hero.HT) {
			partialRegen = 0;
			return;
		}
		partialRegen += regenPerTurn(s);
		if (partialRegen >= 1f) {
			int heal = Math.min(hero.HT - hero.HP, (int) partialRegen);
			hero.HP += heal;
			partialRegen -= (int) partialRegen;
		}
	}

	// ------------------------------------------------------------------ text

	/** one line describing the hero's unique stat, appended to each sin's stats text */
	public static String line(Char ch) {
		if (ch == null) return "";
		String s = null;
		if (ch.buff(WrathGrudge.class) != null) {
			s = Messages.get(UniqueStats.class, "wrath", Math.round(attackSpeedBonus(ch.buff(WrathGrudge.class)) * 100));
		} else if (ch.buff(PredatorStomach.class) != null) {
			s = Messages.get(UniqueStats.class, "gluttony", String.format("%.2f", regenPerTurn(ch.buff(PredatorStomach.class))));
		} else if (ch.buff(GreedVault.class) != null) {
			s = Messages.get(UniqueStats.class, "greed", Math.round(lootBonus(ch.buff(GreedVault.class)) * 100));
		} else if (ch.buff(PrideGlory.class) != null) {
			s = Messages.get(UniqueStats.class, "pride", Math.round(debuffResist(ch.buff(PrideGlory.class)) * 100));
		} else if (ch.buff(LustDesire.class) != null) {
			s = Messages.get(UniqueStats.class, "lust", Math.round(moveSpeedBonus(ch.buff(LustDesire.class)) * 100));
		} else if (ch.buff(EnvyCovet.class) != null) {
			s = Messages.get(UniqueStats.class, "envy", Math.round(rareBonus(ch.buff(EnvyCovet.class)) * 100));
		} else if (ch.buff(SlothRepose.class) != null) {
			s = Messages.get(UniqueStats.class, "sloth", Math.round(hungerSlow(ch.buff(SlothRepose.class)) * 100));
		}
		return s == null ? "" : "\n" + s;
	}
}
