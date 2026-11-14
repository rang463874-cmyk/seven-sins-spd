package com.shatteredpixel.shatteredpixeldungeon.sins;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Drowsy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnvyCovet;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GreedVault;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PredatorStomach;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PrideGlory;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WrathGrudge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.elites.RegionElite;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.elites.EliteHooks;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 * What a sin's class talent (tiers 1-3) does for a hero of ANOTHER class, who got it from the scroll of
 * metamorphosis (owner's table, Nov 13 2026). The talent's own class keeps its normal effect: every method here
 * goes through {@link #m}, which is 0 for the talent's home class.
 *
 * 13 talents work for anyone as they are (some only needed their effect copied here, because the original
 * lives inside the home class's buff), 23 follow five shared rules (A points, B stats, C meal, D cost,
 * E resource maximum) and 41 have an effect of their own.
 */
public final class MetaTalents {

	private MetaTalents() {}

	// ------------------------------------------------------------------ basics

	/** the class a sin talent belongs to (by its name's prefix), null for anything else */
	public static HeroClass home(Talent t) {
		String n = t.name();
		if (n.startsWith("PREDATOR_")) return HeroClass.GLUTTONY;
		if (n.startsWith("GREED_")) return HeroClass.GREED;
		if (n.startsWith("WRATH_")) return HeroClass.WRATH;
		if (n.startsWith("PRIDE_")) return HeroClass.PRIDE;
		if (n.startsWith("LUST_")) return HeroClass.LUST;
		if (n.startsWith("ENVY_")) return HeroClass.ENVY;
		if (n.startsWith("SLOTH_")) return HeroClass.SLOTH;
		return null;
	}

	/** points in a sin talent held by a hero of another class; 0 for the talent's own class */
	public static int m(Hero h, Talent t) {
		if (h == null) return 0;
		int p = h.pointsInTalent(t);
		return p == 0 || h.heroClass == home(t) ? 0 : p;
	}

	private static float pick(int p, float a, float b) {
		return p <= 0 ? 0 : p == 1 ? a : b;
	}

	/** rounds a fraction up or down by chance, so small amounts still add up */
	private static int roll(float v) {
		int i = (int) Math.floor(v);
		return Random.Float() < v - i ? i + 1 : i;
	}

	private static boolean tough(Char ch) {
		return ch.properties().contains(Char.Property.BOSS) || ch.properties().contains(Char.Property.MINIBOSS);
	}

	private static void heal(Hero h, int amount) {
		amount = Math.min(h.HT - h.HP, amount);
		if (amount <= 0) return;
		h.HP += amount;
		if (h.sprite != null) h.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(amount), FloatingText.HEALING);
	}

	private static void gainGold(Hero h, int amount) {
		if (amount <= 0) return;
		Dungeon.gold += amount;
		if (h.sprite != null) h.sprite.showStatusWithIcon(CharSprite.NEUTRAL, Integer.toString(amount), FloatingText.GOLD);
	}

	// ------------------------------------------------------------------ the hero's own sin

	/** "my resource": refills this share of the hero's own skill resource */
	public static void restore(Hero h, float share) {
		if (h == null || share <= 0 || !h.isAlive()) return;
		switch (SinRewards.sinOf(h)) {
			case 0: {
				Hunger hunger = h.buff(Hunger.class);
				if (hunger != null) hunger.satisfy(Hunger.STARVING * share);
				break;
			}
			case 1: {
				GreedVault v = h.buff(GreedVault.class);
				if (v != null) v.addCharge(v.maxCharge() * share);
				break;
			}
			case 2: {
				WrathGrudge g = h.buff(WrathGrudge.class);
				if (g != null) g.rage = Math.min(g.maxRage(), g.rage + g.maxRage() * share);
				break;
			}
			case 3: {
				PrideGlory g = h.buff(PrideGlory.class);
				if (g != null) {
					int n = roll(g.maxDignity() * share);
					if (n > 0) g.addDignity(n);
				}
				break;
			}
			case 4:
				heal(h, roll(h.HT * share));
				break;
			case 5: {
				EnvyCovet c = h.buff(EnvyCovet.class);
				if (c != null) c.addJealousy(c.maxJealousy() * share);
				break;
			}
			case 6:
				//sloth pays in time: the next pillow skill's nap is that much shorter
				state(h).dozeCredit = Math.min(1f, state(h).dozeCredit + share);
				break;
			default:
				return;
		}
		Item.updateQuickslot();
		BuffIndicator.refreshHero();
	}

	/** SlothRepose.doze: spends the saved-up nap credit on this nap */
	public static int useDozeCredit(Hero h, int turns) {
		State s = h == null ? null : h.buff(State.class);
		if (s == null || s.dozeCredit <= 0 || turns <= 1) return turns;
		int left = Math.max(1, Math.round(turns * (1f - s.dozeCredit)));
		s.dozeCredit = 0;
		return left;
	}

	/** permanent points for the hero's own sin; Greed's points are its gold, so it is paid in gold */
	public static void grantPoints(Hero h, float pts) {
		if (h == null || pts <= 0) return;
		if (h.heroClass == HeroClass.GREED) {
			gainGold(h, Math.round(pts * Math.max(1, Dungeon.depth)));
			GreedVault v = h.buff(GreedVault.class);
			if (v != null) v.goldChanged();
		} else {
			SinRewards.grant(h, pts);
		}
	}

	// ------------------------------------------------------------------ shared rules

	private static final Talent[] RULE_A = {Talent.PREDATOR_EFFICIENCY, Talent.WRATH_SPITE, Talent.PRIDE_VANITY,
			Talent.LUST_YEARNING, Talent.ENVY_COVET, Talent.SLOTH_IDLE};
	private static final Talent[] RULE_B = {Talent.PREDATOR_APEX, Talent.GREED_PRESTIGE, Talent.WRATH_FURY,
			Talent.PRIDE_MAJESTY, Talent.LUST_OBSESSION, Talent.ENVY_JEALOUSY, Talent.SLOTH_APATHY};
	private static final Talent[] RULE_C = {Talent.GREED_HOARD, Talent.WRATH_DRUMMER, Talent.PRIDE_REGALIA,
			Talent.ENVY_MIRROR, Talent.SLOTH_PILLOW};
	private static final Talent[] RULE_D = {Talent.GREED_DISCOUNT, Talent.WRATH_TEMPO, Talent.ENVY_LEVEL};
	private static final Talent[] RULE_E = {Talent.PRIDE_COMPOSURE, Talent.ENVY_POLISH};

	private static int sum(Hero h, Talent[] list) {
		if (h == null) return 0;
		int n = 0;
		for (Talent t : list) n += m(h, t);
		return n;
	}

	/** rule A: multiplier on the hero's own sin points gained (+10% per point; "balanced diet" +5%) */
	public static float pointGain(Hero h) {
		if (h == null) return 1f;
		return 1f + 0.10f * sum(h, RULE_A) + 0.05f * m(h, Talent.PREDATOR_ADAPT);
	}

	/** rule B: added to the "stat effects of my points" multiplier (+5% per point) */
	public static float statBonus(Hero h) {
		return 0.05f * sum(h, RULE_B);
	}

	/** rule C: the share of the hero's own resource a meal refills (10% per point) */
	public static float mealShare(Hero h) {
		return 0.10f * sum(h, RULE_C);
	}

	/** rule D: multiplier on the hero's own skill costs (-8% / -15% per talent) */
	public static float costMult(Hero h) {
		if (h == null) return 1f;
		float cut = 0;
		for (Talent t : RULE_D) cut += pick(m(h, t), 0.08f, 0.15f);
		//Seven Sins: the abstinence challenge doubles every sin's skill costs
		return Math.max(0.4f, 1f - cut) * ChallengeHooks.skillCost();
	}

	/** rule E: multiplier on the hero's own resource maximum (+10% per point) */
	public static float maxMult(Hero h) {
		return 1f + 0.10f * sum(h, RULE_E);
	}

	/** Gold.doPickUp: rule A for Greed, whose points are the gold it carries */
	public static int bonusGold(Hero h, int quantity) {
		if (h == null || h.heroClass != HeroClass.GREED) return 0;
		return Math.round(quantity * (pointGain(h) - 1f));
	}

	// ------------------------------------------------------------------ hero stats

	/** Hero.updateHT */
	public static int bonusHT(Hero h, int ht) {
		State s = h.buff(State.class);
		int trophy = m(h, Talent.ENVY_TROPHY);
		if (trophy > 0 && s != null) ht += s.kinds.size() * trophy;
		float mult = 1f + 0.04f * m(h, Talent.SLOTH_SNOOZE);
		//rule E for the sins without a resource gauge to enlarge
		int sin = SinRewards.sinOf(h);
		if (sin == 0 || sin == 4 || sin == 6) mult += 0.05f * sum(h, RULE_E);
		return mult == 1f ? ht : Math.round(ht * mult);
	}

	/** Hero.attackSkill */
	public static float accuracy(Hero h, float acc) {
		int p = m(h, Talent.LUST_REFILL);
		if (p > 0 && h.belongings.attackingWeapon() instanceof MissileWeapon) acc *= 1f + 0.10f * p;
		return acc;
	}

	/** Hero.defenseSkill */
	public static float evasion(Hero h, float eva) {
		return eva * (1f + 0.04f * m(h, Talent.LUST_WAFT));
	}

	/** Regeneration: natural healing */
	public static float regenMult(Char ch) {
		if (!(ch instanceof Hero)) return 1f;
		Hero h = (Hero) ch;
		float mult = healMult(ch);
		if (h.resting) mult *= 1f + 0.5f * m(h, Talent.SLOTH_LINGER);
		return mult;
	}

	/** Healing buff (potions etc.) and natural healing */
	public static float healMult(Char ch) {
		if (!(ch instanceof Hero)) return 1f;
		return 1f + 0.10f * m((Hero) ch, Talent.LUST_THIRST);
	}

	/** Char.resist: shorter paralysis, sleep, slowness and roots */
	public static float resist(Char ch, Class effect) {
		if (!(ch instanceof Hero)) return 1f;
		int p = m((Hero) ch, Talent.SLOTH_SHOVE);
		if (p <= 0) return 1f;
		if (Paralysis.class.isAssignableFrom(effect) || MagicalSleep.class.isAssignableFrom(effect)
				|| Drowsy.class.isAssignableFrom(effect) || Slow.class.isAssignableFrom(effect)
				|| Roots.class.isAssignableFrom(effect)) {
			return 1f - 0.25f * p;
		}
		return 1f;
	}

	/** Shopkeeper.sellPrice */
	public static float shopPrice() {
		return 1f - pick(m(Dungeon.hero, Talent.GREED_BRIBERY), 0.08f, 0.15f);
	}

	// ------------------------------------------------------------------ sight (dream sight for any ally)

	private static boolean allyOf(Mob mob) {
		return mob.alignment == Char.Alignment.ALLY && mob.isAlive();
	}

	/** Level.updateFieldOfView, +2: the hero sees what its allies see */
	public static boolean sharesSight(Mob mob) {
		return m(Dungeon.hero, Talent.SLOTH_COMFY) >= 2 && allyOf(mob);
	}

	/** Level.updateFieldOfView, +1: the tiles within 2 of every ally */
	public static void revealAroundAllies(boolean[] fov) {
		if (m(Dungeon.hero, Talent.SLOTH_COMFY) != 1) return;
		int w = Dungeon.level.width();
		for (Mob mob : Dungeon.level.mobs) {
			if (!allyOf(mob)) continue;
			int px = mob.pos % w, py = mob.pos / w;
			for (int y = py - 2; y <= py + 2; y++) {
				for (int x = px - 2; x <= px + 2; x++) {
					if (x < 0 || y < 0 || x >= w || y >= Dungeon.level.height()) continue;
					fov[x + y * w] = true;
				}
			}
		}
	}

	// ------------------------------------------------------------------ attacking

	/** Hero.attackProc, for every attack */
	public static int onAttack(Hero h, Char enemy, int dmg) {
		if (enemy == null) return dmg;
		boolean thrown = h.belongings.attackingWeapon() instanceof MissileWeapon;
		boolean melee = !thrown;
		boolean tough = tough(enemy);
		float mult = 1f;
		int p;

		if (melee && (p = m(h, Talent.GREED_MIDAS)) > 0 && Random.Float() < 0.05f * p) {
			gainGold(h, 5 + Dungeon.depth);
		}
		if (thrown && (p = m(h, Talent.GREED_COINSHOT)) > 0) mult += pick(p, 0.08f, 0.15f);

		if ((p = m(h, Talent.WRATH_FIRST_BLOOD)) > 0) {
			Mark mk = Buff.affect(enemy, Mark.class);
			if (!mk.struck) mult += 0.15f * p;
			mk.struck = true;
		}
		if ((p = m(h, Talent.WRATH_GLARE)) > 0 && enemy instanceof Mob && ((Mob) enemy).surprisedBy(h)) {
			Buff.prolong(enemy, Vulnerable.class, pick(p, 3f, 5f));
		}
		if (melee && (p = m(h, Talent.WRATH_DASH_STRIKE)) > 0) {
			int near = 0;
			for (int n : PathFinder.NEIGHBOURS8) {
				Char c = com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(h.pos + n);
				if (c != null && c.alignment == Char.Alignment.ENEMY) near++;
			}
			if (near >= 2) mult += 0.10f * p;
		}
		if ((p = m(h, Talent.WRATH_LONG_GRUDGE)) > 0) {
			State s = state(h);
			if (s.lastTarget == enemy.id()) s.chain = Math.min(5, s.chain + 1);
			else {
				s.lastTarget = enemy.id();
				s.chain = 0;
			}
			mult += 0.03f * p * s.chain;
		}
		if (melee && !tough && (p = m(h, Talent.WRATH_CHAOS)) > 0 && Random.Float() < 0.05f * p) {
			Buff.affect(enemy, Amok.class, 3f);
		}

		if ((p = m(h, Talent.PRIDE_POISE)) > 0 && h.HP >= h.HT) mult += pick(p, 0.08f, 0.15f);
		if (melee && !tough && (p = m(h, Talent.PRIDE_SCORN)) > 0 && Random.Float() < 0.05f * p) {
			Buff.prolong(enemy, Paralysis.class, 1f);
		}

		if (melee && !tough && (p = m(h, Talent.LUST_LINGER)) > 0 && Random.Float() < 0.05f * p) {
			Buff.affect(enemy, Charm.class, 3f).object = h.id();
			if (enemy.sprite != null) enemy.sprite.emitter().burst(Speck.factory(Speck.HEART), 2);
		}
		if ((p = m(h, Talent.LUST_BLUSH)) > 0 && helpless(enemy)) mult += 0.10f * p;

		Petty petty = h.buff(Petty.class);
		if (melee && petty != null) {
			mult += 0.15f * m(h, Talent.ENVY_PETTY);
			petty.detach();
		}
		if ((p = m(h, Talent.ENVY_SNATCH)) > 0 && strong(enemy)) mult += pick(p, 0.08f, 0.15f);
		if ((p = m(h, Talent.ENVY_RESENT)) > 0) {
			Mark mk = enemy.buff(Mark.class);
			if (mk != null && mk.resented) {
				mult += 0.10f * p;
				mk.resented = false;
			}
		}

		if (melee && !tough && (p = m(h, Talent.SLOTH_YAWN)) > 0 && Random.Float() < pick(p, 0.08f, 0.15f)) {
			Buff.prolong(enemy, Slow.class, 2f);
		}

		if (mult != 1f) dmg = Math.round(dmg * mult);

		if (melee && (p = m(h, Talent.LUST_SWEET)) > 0) {
			heal(h, roll(Math.min(dmg, enemy.HP + enemy.shielding()) * 0.03f * p));
		}
		return dmg;
	}

	private static boolean helpless(Char enemy) {
		if (enemy.buff(Charm.class) != null || enemy.buff(MagicalSleep.class) != null
				|| enemy.buff(Paralysis.class) != null || enemy.buff(Terror.class) != null) return true;
		return enemy instanceof Mob && ((Mob) enemy).state == ((Mob) enemy).SLEEPING;
	}

	private static boolean strong(Char enemy) {
		return tough(enemy) || enemy instanceof RegionElite || EliteHooks.sinElite(enemy) != null
				|| !enemy.buffs(ChampionEnemy.class).isEmpty();
	}

	/** SinArtifact.onSpent: one of the hero's own sin skills was just paid for */
	public static void onSkillSpent(Hero h) {
		if (m(h, Talent.ENVY_PETTY) > 0) Buff.prolong(h, Petty.class, 10f);
	}

	// ------------------------------------------------------------------ being attacked

	/** Hero.defenseProc, before everything else: an enemy's attack has landed */
	public static int onDefense(Hero h, Char attacker, int dmg) {
		if (attacker == null || dmg <= 0) return dmg;
		int p;

		if ((p = m(h, Talent.PRIDE_DECREE)) > 0) {
			State s = state(h);
			int key = Dungeon.depth * 100 + Dungeon.branch;
			if (s.decreeKey != key) {
				s.decreeKey = key;
				s.decreeUsed = 0;
			}
			if (s.decreeUsed < p) {
				s.decreeUsed++;
				if (h.sprite != null) h.sprite.showStatus(CharSprite.POSITIVE, Messages.get(MetaTalents.class, "decree"));
				return 0;
			}
		}

		boolean track = m(h, Talent.PRIDE_PROCESSION) > 0 || m(h, Talent.PRIDE_SUPREMACY) > 0;
		int resent = m(h, Talent.ENVY_RESENT);
		if (track || resent > 0) {
			Mark mk = Buff.affect(attacker, Mark.class);
			mk.hitHero = true;
			if (resent > 0) mk.resented = true;
		}

		if ((p = m(h, Talent.WRATH_FUEL)) > 0) restore(h, 0.015f * p);

		if ((p = m(h, Talent.SLOTH_SNORE)) > 0 && attacker instanceof Mob && !tough(attacker)
				&& Random.Float() < 0.05f * p) {
			Buff.affect(attacker, MagicalSleep.class);
		}
		return dmg;
	}

	/** Char.attack, miss branch: the hero dodged an enemy */
	public static void onDodge(Char attacker, Char defender) {
		if (!(defender instanceof Hero) || !(attacker instanceof Mob) || !attacker.isAlive()) return;
		Hero h = (Hero) defender;
		int p = m(h, Talent.LUST_HEARTSTRINGS);
		if (p > 0 && !tough(attacker) && Random.Float() < 0.15f * p) {
			Buff.affect(attacker, Charm.class, 2f).object = h.id();
			if (attacker.sprite != null) attacker.sprite.emitter().burst(Speck.factory(Speck.HEART), 2);
		}
	}

	/** Char.damage, before the damage is dealt */
	public static int modifyDamage(Char ch, Object src, int dmg) {
		if (dmg <= 0 || !(ch instanceof Hero) || src instanceof Hunger) return dmg;
		Hero h = (Hero) ch;
		int p = m(h, Talent.PRIDE_GRACE);
		if (p > 0 && h.HP >= h.HT) dmg = Math.max(1, Math.round(dmg * (1f - 0.25f * p)));
		return dmg;
	}

	/** Char.damage, after the damage was dealt */
	public static void afterDamage(Char ch, Object src, int dmg) {
		if (dmg <= 0 || !(ch instanceof Hero) || src instanceof Hunger) return;
		Hero h = (Hero) ch;
		int p = m(h, Talent.GREED_COMPENSATION);
		//losing 10% of max HP refills 5% / 10% of the resource
		if (p > 0) restore(h, dmg / (float) Math.max(1, h.HT) * 0.5f * p);
	}

	// ------------------------------------------------------------------ kills

	private static boolean byHero(Hero h, Object cause) {
		return cause == h || cause instanceof Weapon || cause instanceof Weapon.Enchantment
				|| cause instanceof Wand
				//the sins' skills pass their signature item's class as the cause
				|| (cause instanceof Class && com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SinArtifact.class.isAssignableFrom((Class) cause));
	}

	private static final Class[] CURES = {
			Poison.class, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness.class,
			Vulnerable.class, Slow.class, Ooze.class, Corrosion.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hex.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo.class, Roots.class, Charm.class,
	};

	/** Mob.die */
	@SuppressWarnings("unchecked")
	public static void onMobDeath(Mob mob, Object cause) {
		Hero h = Dungeon.hero;
		if (h == null || !h.isAlive() || mob.alignment != Char.Alignment.ENEMY) return;
		State s = state(h);
		int p;

		//every kind of enemy that has died this run (old saves start counting from here)
		if (s.kinds.add(mob.getClass().getName())) {
			if ((p = m(h, Talent.ENVY_SWITCH)) > 0) grantPoints(h, 2f * p);
			if (m(h, Talent.ENVY_TROPHY) > 0) h.updateHT(true);
		}

		if (!byHero(h, cause)) {
			if ((p = m(h, Talent.WRATH_SCAVENGE)) > 0) restore(h, 0.05f * p);
			return;
		}

		if ((p = m(h, Talent.PREDATOR_FANG)) > 0) {
			Hunger hunger = h.buff(Hunger.class);
			if (hunger != null) hunger.satisfy(PredatorStomach.FANG_KILL * p);
		}
		if ((p = m(h, Talent.GREED_LOOT)) > 0) gainGold(h, (2 + Dungeon.depth) * p);
		if ((p = m(h, Talent.ENVY_GRASP)) > 0) restore(h, 0.03f * p);
		if ((p = m(h, Talent.LUST_EMBRACE)) > 0) heal(h, roll(h.HT * 0.01f * p));

		if ((p = m(h, Talent.SLOTH_RESTFUL)) > 0 && Random.Float() < 0.10f * p) {
			ArrayList<Buff> found = new ArrayList<>();
			for (Class c : CURES) {
				Buff b = h.buff(c);
				if (b != null) found.add(b);
			}
			if (!found.isEmpty()) {
				Random.element(found).detach();
				if (h.sprite != null) h.sprite.emitter().burst(Speck.factory(Speck.HEALING), 2);
			}
		}

		//a kill this enemy never answered
		Mark mk = mob.buff(Mark.class);
		if (mk == null || !mk.hitHero) {
			if ((p = m(h, Talent.PRIDE_PROCESSION)) > 0) Buff.prolong(h, Haste.class, pick(p, 2f, 4f));
			if ((p = m(h, Talent.PRIDE_SUPREMACY)) > 0) heal(h, roll(h.HT * 0.02f * p));
		}
	}

	// ------------------------------------------------------------------ gold and consumables

	/** Gold.doPickUp, after the gold is in the wallet */
	public static void onGoldPickup(Hero h, int quantity) {
		if (h == null || quantity <= 0 || com.shatteredpixel.shatteredpixeldungeon.greed.GreedHooks.selling) return;
		int p;
		if ((p = m(h, Talent.GREED_GILDED_SKIN)) > 0) {
			int shield = Math.min(Math.round(quantity * 0.04f * p), Math.round(h.HT * 0.15f * p));
			if (shield > 0) Buff.affect(h, Barrier.class).setShield(shield);
		}
		if ((p = m(h, Talent.GREED_EXPRESS)) > 0) Buff.prolong(h, Haste.class, pick(p, 2f, 4f));
	}

	/** every potion, scroll, stone, seed or meal a hero other than Gluttony uses */
	public static void onConsumed(Hero h, Item item) {
		if (h == null || item == null || h.heroClass == HeroClass.GLUTTONY) return;
		int cat = PredatorStomach.category(item);
		if (cat < 0) return;
		int p;

		if ((p = m(h, Talent.PREDATOR_FEAST)) > 0 && cat == PredatorStomach.CAT_FOOD) {
			heal(h, Math.round(h.HT * 0.10f * p));
		}
		if ((p = m(h, Talent.PREDATOR_TASTE)) > 0) {
			for (Item it : h.belongings.backpack.items) {
				if (it instanceof EquipableItem && !it.isIdentified()) {
					if (p >= 2) it.identify();
					else if (!it.cursedKnown) it.cursedKnown = true;
					else continue;
					GLog.i(Messages.get(PredatorStomach.class, "taste", it.name()));
					break;
				}
			}
		}
		if ((p = m(h, Talent.PREDATOR_FANG)) > 0) {
			Hunger hunger = h.buff(Hunger.class);
			if (hunger != null) hunger.satisfy(PredatorStomach.FANG_CONSUME * p);
		}
		if ((p = m(h, Talent.PREDATOR_HIDE)) > 0) {
			Buff.affect(h, Barrier.class).incShield(Math.max(1, Math.round(h.HT * 0.04f * p)));
		}
		//"wide gullet": every consumable is worth a point or two of the hero's own sin
		if ((p = m(h, Talent.PREDATOR_SCRAPS)) > 0) grantPoints(h, p);
	}

	// ------------------------------------------------------------------ per-turn state

	public static State state(Hero h) {
		State s = h.buff(State.class);
		return s != null ? s : Buff.affect(h, State.class);
	}

	/** Hero.act */
	public static void tick(Hero h) {
		if (h == null || !h.isAlive() || Dungeon.level == null) return;
		State s = state(h);
		int key = Dungeon.depth * 100 + Dungeon.branch;
		if (s.floorKey != key) {
			boolean first = s.floorKey == -1;
			s.floorKey = key;
			int p = m(h, Talent.SLOTH_ROLL);
			if (p > 0 && !first) Buff.affect(h, Barrier.class).incShield((5 + h.lvl) * p);
		}
	}

	/** Invisible bookkeeping on the hero: kinds of enemies killed, hit chains, per-floor counters. */
	public static class State extends Buff {

		{
			revivePersists = true;
		}

		public HashSet<String> kinds = new HashSet<>();
		public int lastTarget = -1, chain = 0;
		public int decreeKey = -1, decreeUsed = 0;
		public int floorKey = -1;
		public float dozeCredit = 0;

		private static final Class[] ACIDS = {Poison.class, Ooze.class, Corrosion.class};

		private Hero hero() {
			return target instanceof Hero ? (Hero) target : null;
		}

		//"acid stomach" for the other classes (Gluttony's own lives in PredatorStomach)
		@Override
		public HashSet<Class> resistances() {
			HashSet<Class> r = super.resistances();
			if (m(hero(), Talent.PREDATOR_CURSE_EATER) == 1) r.addAll(Arrays.asList(ACIDS));
			return r;
		}

		@Override
		public HashSet<Class> immunities() {
			HashSet<Class> r = super.immunities();
			if (m(hero(), Talent.PREDATOR_CURSE_EATER) >= 2) r.addAll(Arrays.asList(ACIDS));
			return r;
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("kinds", kinds.toArray(new String[0]));
			bundle.put("last_target", lastTarget);
			bundle.put("chain", chain);
			bundle.put("decree_key", decreeKey);
			bundle.put("decree_used", decreeUsed);
			bundle.put("floor_key", floorKey);
			bundle.put("doze_credit", dozeCredit);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			kinds.clear();
			if (bundle.contains("kinds")) kinds.addAll(Arrays.asList(bundle.getStringArray("kinds")));
			lastTarget = bundle.getInt("last_target");
			chain = bundle.getInt("chain");
			decreeKey = bundle.getInt("decree_key");
			decreeUsed = bundle.getInt("decree_used");
			floorKey = bundle.getInt("floor_key");
			dozeCredit = bundle.getFloat("doze_credit");
		}
	}

	/** Invisible marks on an enemy: struck once already, has hurt the hero, owed a payback. */
	public static class Mark extends Buff {

		public boolean struck = false, hitHero = false, resented = false;

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("struck", struck);
			bundle.put("hit_hero", hitHero);
			bundle.put("resented", resented);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			struck = bundle.getBoolean("struck");
			hitHero = bundle.getBoolean("hit_hero");
			resented = bundle.getBoolean("resented");
		}
	}

	/** "pettiness" for the other classes: the next melee hit after one of the hero's own skills */
	public static class Petty extends FlavourBuff {
	}
}
