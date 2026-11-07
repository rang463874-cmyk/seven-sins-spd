package com.shatteredpixel.shatteredpixeldungeon.predator;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PredatorStomach;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.*;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.Brew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.Elixir;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.*;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRage;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMetamorphosis;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.*;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Combat stat hooks for the Gluttony class (the Predator), called from Hero.
 * Every method is a cheap pass-through when the hero has no PredatorStomach.
 */
public final class PredatorHooks {

	private PredatorHooks() {}

	private static PredatorStomach stomach(Hero hero) {
		return hero.buff(PredatorStomach.class);
	}

	/** Melee damage bonus, called from Hero.attackProc for non-thrown attacks. */
	public static int onMeleeAttack(Hero hero, Char enemy, int dmg) {
		PredatorStomach st = stomach(hero);
		if (st != null) {
			dmg += st.meleeBonus();
		}
		PredatorStomach.Fang fang = hero.buff(PredatorStomach.Fang.class);
		if (fang != null) {
			dmg = Math.round(dmg * (1f + 0.15f * hero.pointsInTalent(Talent.PREDATOR_FANG)));
			if (--fang.hits <= 0) fang.detach();
		}
		int instinct = hero.pointsInTalent(Talent.PREDATOR_INSTINCT);
		if (instinct > 0 && enemy != null && enemy.HP * 2 <= enemy.HT) {
			dmg = Math.round(dmg * (1f + 0.10f * instinct));
		}
		if (enemy != null) {
			for (Buff b : hero.buffs()) {
				if (b instanceof GluttonImbue) dmg = ((GluttonImbue) b).proc(hero, enemy, dmg);
			}
		}
		return dmg;
	}

	/** Damage taken, called from Hero.defenseProc before the armor's glyph. */
	public static int onDefense(Hero hero, Char attacker, int dmg) {
		if (dmg <= 0) return dmg;
		PredatorStomach.Coma coma = hero.buff(PredatorStomach.Coma.class);
		if (coma != null) {
			int pct = PredatorStomach.Coma.comaPercent(hero.pointsInTalent(Talent.GOURMAND_FOOD_COMA));
			dmg = Math.round(dmg * (100 - pct) / 100f);
			coma.detach();
		}
		int thorns = hero.pointsInTalent(Talent.GUT_THORNS);
		if (thorns > 0 && attacker != null && attacker.isAlive()) {
			Barrier b = hero.buff(Barrier.class);
			if (b != null && b.shielding() > 0 && Dungeon.level.adjacent(hero.pos, attacker.pos)) {
				int reflect = Math.max(1, Math.round(dmg * 0.10f * thorns));
				attacker.damage(reflect, hero);
			}
		}
		return dmg;
	}

	// ------------------------------------------------------------------ consumables

	private static boolean glutton(Hero hero) {
		return hero != null && hero.heroClass == HeroClass.GLUTTONY;
	}

	/** Called whenever the hero drinks, reads, eats or uses a consumable. */
	public static void onConsumed(Hero hero, Item item) {
		if (!glutton(hero) || item == null) return;
		Buff.affect(hero, PredatorStomach.class).consume(hero, item, false);
	}

	/** Called from Food.execute after eating. */
	public static void onFoodEaten(Hero hero, Food food) {
		if (!glutton(hero)) return;
		if (hero.subClass == HeroSubClass.GOURMAND) {
			Hunger hunger = hero.buff(Hunger.class);
			if (hunger != null) hunger.satisfy(food.energy * 0.5f);
		}
		onConsumed(hero, food);
	}

	/** how long a converted (harmful turned helpful) buff lasts */
	public static float imbueDuration(Hero hero) {
		float d = GluttonImbue.DURATION;
		if (PredatorStomach.hasCodex(hero, PredatorStomach.CODEX_LINGER)) d *= 1.5f;
		if (hero.subClass == HeroSubClass.INHERITOR) d *= 1f + 0.25f * hero.pointsInTalent(Talent.INHERITOR_MEMORY);
		return d;
	}

	private static final HashSet<Class<? extends Potion>> CONVERTED = new HashSet<>(Arrays.asList(
			PotionOfToxicGas.class, PotionOfLiquidFlame.class, PotionOfFrost.class, PotionOfParalyticGas.class,
			PotionOfCorrosiveGas.class, PotionOfSnapFreeze.class));

	/** true if the glutton can drink this potion without the usual harm */
	public static boolean drinksSafely(Hero hero, Potion potion) {
		return glutton(hero) && CONVERTED.contains(potion.getClass());
	}

	/**
	 * Called from Potion.drink instead of apply. Returns true if the potion's effect was
	 * replaced by a glutton's buff.
	 */
	public static boolean drinkOverride(Hero hero, Potion potion) {
		if (!drinksSafely(hero, potion)) return false;
		float d = imbueDuration(hero);
		Class<?> c = potion.getClass();
		if (c == PotionOfToxicGas.class) {
			Buff.prolong(hero, GluttonImbue.Venom.class, d);
		} else if (c == PotionOfLiquidFlame.class) {
			Buff.affect(hero, FireImbue.class).set(d);
		} else if (c == PotionOfFrost.class) {
			Buff.prolong(hero, FrostImbue.class, d);
		} else if (c == PotionOfSnapFreeze.class) {
			Buff.prolong(hero, FrostImbue.class, d * 2f);
		} else if (c == PotionOfParalyticGas.class) {
			Buff.prolong(hero, GluttonImbue.Numbing.class, d);
		} else if (c == PotionOfCorrosiveGas.class) {
			Buff.prolong(hero, GluttonImbue.Acid.class, d);
		}
		potion.identify();
		GLog.p(Messages.get(PredatorStomach.class, "converted"));
		hero.sprite.emitter().burst(Speck.factory(Speck.STAR), 5);
		return true;
	}

	private static final HashSet<Class<? extends Potion>> NO_ECHO = new HashSet<>(Arrays.asList(
			PotionOfStrength.class, PotionOfExperience.class, PotionOfDivineInspiration.class,
			PotionOfMastery.class, PotionOfDragonsBreath.class));

	/** potions that may be echoed (codex) or ruminated */
	public static boolean canRepeat(Class<?> c) {
		if (c == null || NO_ECHO.contains(c)) return false;
		if (Elixir.class.isAssignableFrom(c) || Brew.class.isAssignableFrom(c)) return false;
		if (ScrollOfUpgrade.class.isAssignableFrom(c) || ScrollOfTransmutation.class.isAssignableFrom(c)
				|| ScrollOfEnchantment.class.isAssignableFrom(c) || ScrollOfMetamorphosis.class.isAssignableFrom(c)) return false;
		return true;
	}

	/** Called from Potion.drink after the effect: codex echo. */
	public static boolean drinkTwice(Hero hero, Potion potion) {
		if (!glutton(hero) || !hero.isAlive() || CONVERTED.contains(potion.getClass())) return false;
		if (!canRepeat(potion.getClass())) return false;
		if (!PredatorStomach.hasCodex(hero, PredatorStomach.CODEX_ECHO) || Random.Float() >= 0.25f) return false;
		GLog.p(Messages.get(PredatorStomach.class, "echo"));
		return true;
	}

	/** Called from Scroll.readAnimation: consumption, plus the codex chance to keep the scroll. */
	public static void onScrollRead(Hero hero, Scroll scroll) {
		if (!glutton(hero) || scroll.isAnonymous()) return;
		onConsumed(hero, scroll);
		if (PredatorStomach.hasCodex(hero, PredatorStomach.CODEX_PRESERVE) && canRepeat(scroll.getClass())
				&& Random.Float() < 0.15f) {
			Item copy = Reflection.newInstance(scroll.getClass());
			if (copy != null) {
				copy.identify();
				if (!copy.collect(hero.belongings.backpack)) Dungeon.level.drop(copy, hero.pos).sprite.drop();
				GLog.p(Messages.get(PredatorStomach.class, "preserved", copy.name()));
			}
		}
	}

	/** Called at the start of ScrollOfRage.doRead. Returns true if the glutton read it its own way. */
	public static boolean readRage(Hero hero, ScrollOfRage scroll) {
		if (!glutton(hero)) return false;
		scroll.detach(hero.belongings.backpack);
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (mob.alignment != Char.Alignment.ALLY && Dungeon.level.heroFOV[mob.pos]) {
				Buff.prolong(mob, Amok.class, 5f);
			}
		}
		Buff.prolong(hero, GluttonImbue.Frenzy.class, imbueDuration(hero));
		GLog.p(Messages.get(PredatorStomach.class, "converted"));
		scroll.identify();
		hero.sprite.centerEmitter().start(Speck.factory(Speck.SCREAM), 0.3f, 3);
		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
		scroll.readAnimation();
		return true;
	}

	/** psionic blast: the glutton doesn't suffer the backlash */
	public static boolean backlashImmune(Hero hero) {
		return glutton(hero);
	}

	/** Runestones (thrown or used from the bag). */
	public static void onStoneUsed(Item stone) {
		onConsumed(Dungeon.hero, stone);
	}

	// ------------------------------------------------------------------ seeds

	public static final String AC_EAT_SEED = "EAT_SEED";

	public static boolean canEatSeeds(Hero hero) {
		return glutton(hero) && PredatorStomach.hasCodex(hero, PredatorStomach.CODEX_SEEDS);
	}

	/** eats a seed: points plus the plant's helpful side only */
	public static void eatSeed(Hero hero, Plant.Seed seed) {
		seed.detach(hero.belongings.backpack);
		hero.busy();
		hero.sprite.operate(hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.EAT);
		if (seed instanceof Sungrass.Seed) {
			Buff.affect(hero, Healing.class).setHeal(Math.round(hero.HT * 0.25f), 0.25f, 0);
		} else if (seed instanceof Earthroot.Seed) {
			Buff.affect(hero, Barrier.class).incShield(Math.round(hero.HT * 0.20f));
		} else if (seed instanceof Fadeleaf.Seed) {
			Buff.prolong(hero, MindVision.class, 10f);
			Dungeon.observe();
		} else if (seed instanceof Firebloom.Seed) {
			Buff.affect(hero, FireImbue.class).set(15f);
		} else if (seed instanceof Icecap.Seed) {
			Buff.prolong(hero, FrostImbue.class, 15f);
		} else if (seed instanceof Sorrowmoss.Seed) {
			Buff.prolong(hero, GluttonImbue.Venom.class, 15f);
		} else if (seed instanceof Stormvine.Seed) {
			Buff.prolong(hero, Levitation.class, 10f);
		} else if (seed instanceof Swiftthistle.Seed) {
			Buff.prolong(hero, Haste.class, 8f);
		} else if (seed instanceof Blindweed.Seed) {
			Buff.prolong(hero, Invisibility.class, 10f);
		} else if (seed instanceof Starflower.Seed) {
			Buff.prolong(hero, Bless.class, 20f);
		} else if (seed instanceof Mageroyal.Seed) {
			Buff.prolong(hero, Recharging.class, 10f);
		} else if (seed instanceof Rotberry.Seed) {
			Buff.prolong(hero, Adrenaline.class, 10f);
		}
		Hunger hunger = hero.buff(Hunger.class);
		if (hunger != null) hunger.satisfy(20f);
		onConsumed(hero, seed);
		hero.spend(1f);
	}

	public static float accuracy(Hero hero, float mult) {
		PredatorStomach st = stomach(hero);
		return st == null ? mult : mult * (1f + st.accuracyBonus());
	}

	public static float evasion(Hero hero, float mult) {
		PredatorStomach st = stomach(hero);
		return st == null ? mult : mult * (1f + st.evasionBonus());
	}

	public static int drBonus(Hero hero, int dr) {
		PredatorStomach st = stomach(hero);
		return st == null ? dr : dr + st.drRoll();
	}

	public static int bonusHT(Hero hero, int ht) {
		PredatorStomach st = stomach(hero);
		return st == null ? ht : ht + st.hpBonus();
	}
}
