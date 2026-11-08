package com.shatteredpixel.shatteredpixeldungeon.envy;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LustDesire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PredatorStomach;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PrideGlory;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnvyCovet;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

import static com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnvyCovet.Trait;

/**
 * Game hooks for the Envy class. Every method is a cheap pass-through
 * when the hero has no EnvyCovet.
 */
public final class EnvyHooks {

	private EnvyHooks() {}

	private static EnvyCovet covet(Hero hero) {
		return hero == null ? null : hero.buff(EnvyCovet.class);
	}

	private static boolean roll(EnvyCovet c, Trait t, float chance) {
		float p = c.trait(t, chance);
		return p > 0 && Random.Float() < p;
	}

	// ------------------------------------------------------------------ combat

	/** Called from Hero.attackProc for every attack. */
	public static int onAttack(Hero hero, Char enemy, int dmg) {
		EnvyCovet c = covet(hero);
		if (c == null || enemy == null) return dmg;

		KindOfWeapon wep = hero.belongings.attackingWeapon();
		boolean melee = !(wep instanceof MissileWeapon);
		float mult = 1f;
		dmg += c.meleeBonus();
		if (melee) {
			mult *= c.meleeMult();
		} else {
			mult *= 1f + c.trait(Trait.GNOLL_TRICKSTER, 0.20f);
		}
		if (hero.buff(EnvyCovet.Stolen.class) != null) mult *= 1.25f;

		EnvyCovet.Spite spite = hero.buff(EnvyCovet.Spite.class);
		if (melee && spite != null) {
			mult *= EnvyCovet.Spite.mult(hero);
			spite.detach();
		}
		dmg = Math.round(dmg * mult);
		dmg = com.shatteredpixel.shatteredpixeldungeon.sins.AwakeningHooks.envyDamage(hero, dmg); //Seven Sins: awakening

		//imitating lust: drink from charmed enemies
		if (melee && c.imitating(EnvyMimicry.Growth.LUST) && LustDesire.charmedByHero(enemy) && dmg > 0) {
			int stolen = Math.round(dmg * LustDesire.BASE_LIFESTEAL * c.efficiency(EnvyMimicry.Growth.LUST));
			int heal = Math.min(stolen, hero.HT - hero.HP);
			if (heal > 0) {
				hero.HP += heal;
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			}
			c.addEnvy(stolen * LustDesire.DESIRE_PER_HP);
		}

		//bestiary traits that trigger on hit
		if (roll(c, Trait.ALBINO, 0.20f)) Buff.affect(enemy, Bleeding.class).set(Math.max(1, dmg * 0.3f));
		if (roll(c, Trait.CAUSTIC_SLIME, 0.20f)) Buff.affect(enemy, Ooze.class).set(Ooze.DURATION);
		if (roll(c, Trait.BANDIT, 0.15f)) Buff.prolong(enemy, Blindness.class, 2f);
		if (roll(c, Trait.FETID_RAT, 0.20f)) Buff.affect(enemy, Poison.class).set(3f);
		if (roll(c, Trait.NEWBORN_ELEMENTAL, 0.10f)) Buff.affect(enemy, Burning.class).reignite(enemy, 3f);
		if (roll(c, Trait.CHAOS_ELEMENTAL, 0.15f)) {
			if (Random.Int(2) == 0) Buff.affect(enemy, Burning.class).reignite(enemy, 3f);
			else Buff.prolong(enemy, Chill.class, 3f);
		}
		return dmg;
	}

	/** Called from Char.damage before damage is applied (robbed stacks are gone; nothing to do now). */
	public static int modifyDamage(Char victim, Object src, int dmg) {
		return dmg;
	}

	/** Called from Hero.defenseProc. */
	public static int onDefense(Hero hero, Char attacker, int dmg) {
		EnvyCovet c = covet(hero);
		if (c == null || attacker == null || dmg <= 0) return dmg;
		float thorns = c.trait(Trait.ACIDIC, 0.15f);
		if (thorns > 0 && attacker.isAlive() && Dungeon.level.adjacent(hero.pos, attacker.pos)) {
			attacker.damage(Math.max(1, Math.round(dmg * thorns)), hero);
		}
		if (c.imitating(EnvyMimicry.Growth.PRIDE)) Buff.affect(attacker, PrideGlory.Insolent.class);
		int resent = hero.pointsInTalent(Talent.ENVY_RESENT);
		if (resent > 0) c.addJealousy(resent);
		return dmg;
	}

	// ------------------------------------------------------------------ kills & bestiary

	/** Called from Mob.die. */
	public static void onMobDeath(Mob mob, Object cause) {
		Hero hero = Dungeon.hero;
		EnvyCovet c = covet(hero);
		if (c == null || !hero.isAlive()) return;

		Trait t = EnvyCovet.CODEX.get(mob.getClass());
		boolean known = t != null && c.has(t);
		if (t != null && c.register(t)) {
			GLog.p(Messages.get(EnvyCovet.class, "registered", t.title(), t.desc()));
			hero.sprite.emitter().burst(Speck.factory(Speck.STAR), 8);
			//Envy's own growth: learning from what it has seen
			float pts = registerEnvy(mob);
			c.addEnvy(pts);
			hero.sprite.showStatus(CharSprite.POSITIVE, "+" + Math.round(pts));
			c.refreshSlots();
		}

		if (mob.alignment != Char.Alignment.ENEMY) return;
		mimicKill(hero, c, mob, cause);
		if (!heroCaused(cause)) return;
		com.shatteredpixel.shatteredpixeldungeon.sins.UniqueStats.onKill(hero);

		if (t != null) {
			int grasp = hero.pointsInTalent(Talent.ENVY_GRASP);
			if (grasp > 0) c.addJealousy(10f * grasp);
			int echo = hero.pointsInTalent(Talent.COPY_ECHO);
			if (known && echo > 0 && hero.subClass == HeroSubClass.COPYCAT) {
				c.addEnvy(registerEnvy(mob) * 0.25f * echo);
			}
		}

		float heal = c.trait(Trait.SPECTRAL_NECRO, 3f);
		if (heal > 0 && hero.HP < hero.HT) {
			hero.HP = Math.min(hero.HT, hero.HP + Math.round(heal));
			hero.sprite.emitter().burst(Speck.factory(Speck.HEALING), 1);
		}
	}

	/** envy for registering a new bestiary entry */
	public static float registerEnvy(Mob mob) {
		float pts = 10f + mob.HT / 5f;
		if (mob.properties().contains(Char.Property.BOSS)) pts *= 2f;
		return pts;
	}

	// ------------------------------------------------------------------ imitated growth

	private static boolean heroCaused(Object cause) {
		return cause == Dungeon.hero || cause instanceof Weapon || cause instanceof Weapon.Enchantment
				|| cause instanceof Wand || cause instanceof ArmorAbility || cause == EnvyMimicry.class;
	}

	/** kill-based growth of the sin being imitated (wrath, pride, sloth) */
	private static void mimicKill(Hero hero, EnvyCovet c, Mob mob, Object cause) {
		for (EnvyMimicry.Growth g : new EnvyMimicry.Growth[]{c.growth, c.growth2}) {
			if (c.imitating(g)) mimicKill(hero, c, mob, cause, g, c.efficiency(g));
		}
	}

	private static void mimicKill(Hero hero, EnvyCovet c, Mob mob, Object cause, EnvyMimicry.Growth g, float eff) {
		switch (g) {
			case WRATH:
				if (cause instanceof Mob && cause != mob && ((Mob) cause).alignment == Char.Alignment.ENEMY) {
					c.addEnvy((1f + mob.HT / 20f) * eff);
				}
				break;
			case PRIDE:
				if (heroCaused(cause) && mob.buff(PrideGlory.Insolent.class) == null) {
					c.addEnvy(PrideGlory.gloryFor(mob) * eff);
					hero.sprite.showStatus(CharSprite.POSITIVE, Messages.get(PrideGlory.class, "flawless"));
				}
				break;
			case SLOTH:
				float worth = (1f + mob.HT / 15f) * eff;
				if (cause instanceof Char && cause != hero && ((Char) cause).alignment == Char.Alignment.ALLY) {
					c.addEnvy(worth);
				} else if (heroCaused(cause)) {
					c.addEnvy(worth / 3f);
				}
				break;
			default:
				break;
		}
	}

	/** Called from PredatorHooks.onConsumed: imitating gluttony. */
	public static void onConsumed(Hero hero, Item item) {
		EnvyCovet c = covet(hero);
		if (c == null || !c.imitating(EnvyMimicry.Growth.GLUTTONY)) return;
		if (PredatorStomach.category(item) < 0) return;
		float pts = PredatorStomach.baseScore(item) * c.efficiency(EnvyMimicry.Growth.GLUTTONY);
		c.addEnvy(pts);
		hero.sprite.showStatus(CharSprite.POSITIVE, "+" + PredatorStomach.fmt(pts));
	}

	/** Extra gold granted when picking up gold (Golden Mimic trait). */
	public static int bonusGold(Hero hero, int quantity) {
		EnvyCovet c = covet(hero);
		return c == null ? 0 : Math.round(quantity * c.trait(Trait.GOLDEN_MIMIC, 0.20f));
	}

	// ------------------------------------------------------------------ hero stats

	public static float accuracy(Hero hero, float mult) {
		EnvyCovet c = covet(hero);
		return c == null ? mult : mult * (1f + c.accuracyBonus());
	}

	public static float evasion(Hero hero, float mult) {
		EnvyCovet c = covet(hero);
		if (c == null) return mult;
		mult *= 1f + c.evasionBonus();
		if (hero.buff(EnvyCovet.Sway.class) != null) mult *= EnvyCovet.Sway.EVASION_MULT;
		return mult;
	}

	public static int drBonus(Hero hero, int dr) {
		EnvyCovet c = covet(hero);
		return c == null ? dr : dr + c.drRoll();
	}

	public static int bonusHT(Hero hero, int ht) {
		EnvyCovet c = covet(hero);
		return c == null ? ht : Math.round((ht + c.hpBonus()) * c.hpMult());
	}
}
