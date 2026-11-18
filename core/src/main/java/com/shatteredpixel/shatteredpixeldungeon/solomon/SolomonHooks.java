package com.shatteredpixel.shatteredpixeldungeon.solomon;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SolomonArcana;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

/**
 * Seven Sins: Solomon, the hidden eighth class (Nov 17 2026, owner). Unlocked for good once a single run records
 * all 72 demons in the Lemegeton (GoetiaHooks.record); the flag lives in the game settings, so it survives runs.
 * Also holds the talent effects (Nov 18 2026) and the hooks into Hero / Char.damage / Mob.die.
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

	private static int pts(Hero hero, Talent t) {
		return hero == null ? 0 : hero.pointsInTalent(t);
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
		if (a != null) a.addWisdom(WISDOM_PER_DEMON + pts(hero, Talent.SOL_LORE));
	}

	public static final float WISDOM_PER_DEMON = 3f;

	// ------------------------------------------------------------------ talents

	/** time to draw a symbol: 0.5 turns, -0.1 per point of Deft Hands */
	public static float drawTime(Hero hero) {
		return Math.max(0.2f, SolomonSpells.DRAW_TIME - 0.1f * pts(hero, Talent.SOL_DEFTNESS));
	}

	/** mana cost after the Triad talent (three-symbol spells) and Panacea */
	public static int adjustCost(Hero hero, int base, boolean triad) {
		float c = base;
		if (triad) c *= 1f - 0.1f * pts(hero, Talent.SOL_TRIAD);
		if (hero != null && hero.buff(SolomonPrinciples.Elixir.class) != null) c *= 0.5f;
		return Math.max(1, Math.round(c));
	}

	/** single-symbol spells: +25% per point of Elemental Affinity */
	public static float singleMult(Hero hero) {
		return 1f + 0.25f * pts(hero, Talent.SOL_ELEMENTAL);
	}

	public static int summonCost(Hero hero) {
		return Math.max(1, Math.round(SolomonSpells.SUMMON_COST * (1f - 0.15f * pts(hero, Talent.SOL_LEGION))));
	}

	public static int summonChoices(Hero hero) {
		return SolomonSpells.SUMMON_CHOICES + 3 * pts(hero, Talent.MASTER_KINGS);
	}

	/** after every spell: pays, then the Ward / Echo / Twin Circle talents */
	public static void afterCast(Hero hero, int cost) {
		SolomonArcana a = SolomonSpells.arcana(hero);
		if (a == null) return;
		a.useMana(cost);
		int ward = pts(hero, Talent.SOL_WARD);
		if (ward > 0) Buff.affect(hero, Barrier.class).incShield(ward * (1 + hero.lvl / 5));
		int echo = pts(hero, Talent.SOL_ECHO);
		if (echo > 0 && Random.Int(10) < echo) {
			a.addMana(cost);
			hero.sprite.showStatus(0xCC99FF, Messages.get(SolomonHooks.class, "echo"));
		}
		int circle = pts(hero, Talent.MAGUS_CIRCLE);
		if (circle > 0 && Random.Int(100) < 15 * circle) {
			hero.sprite.showStatus(0xCC99FF, Messages.get(SolomonHooks.class, "circle_kept"));
		} else {
			a.circle.clear();
		}
	}

	/** from Char.damage */
	public static int modifyDamage(Char target, Object src, int dmg) {
		if (dmg <= 0) return dmg;
		if (target instanceof Hero && target.buff(SolomonPrinciples.Immutable.class) != null) {
			dmg = Math.round(dmg * 0.4f);
		}
		if (src instanceof Mob && ((Mob) src).buff(SolomonSpells.SummonPact.class) != null) {
			int brand = pts(Dungeon.hero, Talent.MASTER_BRAND);
			if (brand > 0) dmg = Math.round(dmg * (1f + 0.15f * brand));
		}
		return dmg;
	}

	/** from Mob.die */
	public static void onMobDeath(Mob mob, Object cause) {
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive()) return;
		SolomonArcana a = SolomonSpells.arcana(hero);
		if (a == null) return;
		if (cause == SolomonSpells.class || cause == SolomonTriads.class || cause == SolomonPrinciples.class) {
			int siphon = pts(hero, Talent.MAGUS_SIPHON);
			if (siphon > 0) a.addMana(siphon);
		}
		if (mob.buff(SolomonSpells.SummonPact.class) != null) {
			int offer = pts(hero, Talent.MASTER_OFFERING);
			if (offer > 0) {
				int heal = Math.min(hero.HT - hero.HP, Math.round(hero.HT * 0.05f * offer));
				if (heal > 0) {
					hero.HP += heal;
					hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
				}
				a.addMana(2 * offer);
			}
		}
	}

	/** from SinMeals: Meditative Meal refills mana */
	public static void onFoodEaten(Hero hero) {
		int p = pts(hero, Talent.SOL_MEDITATION);
		SolomonArcana a = SolomonSpells.arcana(hero);
		if (p > 0 && a != null) a.addMana(a.maxMana() * 0.3f * p);
	}
}
