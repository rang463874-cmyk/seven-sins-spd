package com.shatteredpixel.shatteredpixeldungeon.sins;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PredatorStomach;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicalHolster;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.PotionBandolier;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.ScrollHolder;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.VelvetPouch;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

import static com.shatteredpixel.shatteredpixeldungeon.sins.AwakeningTalent.*;

/**
 * Effects of the second awakening that don't belong to one sin's existing hooks (paths' drawbacks,
 * gluttony's swallowed bags...). Call sites are marked "Seven Sins: awakening".
 */
public class AwakeningHooks {

	// ------------------------------------------------------------------ common

	public static boolean control(Char ch, HeroClass sin) {
		return is(ch, sin, SinAwakening.CONTROL);
	}

	/** called right after the awakening is chosen */
	public static void onAwakened(Hero hero) {
		if (monster(hero, HeroClass.GLUTTONY)) swallowBags(hero);
	}

	/** max HP multiplier of the monster paths (greed -25%, envy -20%) */
	public static float htMultiplier(Hero hero) {
		if (monster(hero, HeroClass.GREED)) return 0.75f;
		if (monster(hero, HeroClass.ENVY)) return 0.80f;
		return 1f;
	}

	/** move speed (gluttony's monster: +50%) */
	public static float moveSpeed(Hero hero) {
		return monster(hero, HeroClass.GLUTTONY) ? 1.5f : 1f;
	}

	/** attack speed (gluttony's monster: +50%) */
	public static float attackSpeed(Hero hero) {
		return monster(hero, HeroClass.GLUTTONY) ? 1.5f : 1f;
	}

	/** natural regeneration (wrath's and lust's monsters: halved) */
	public static float regenMultiplier(Char ch) {
		if (monster(ch, HeroClass.WRATH) || monster(ch, HeroClass.LUST)) return 0.5f;
		return 1f;
	}

	/** healing over time from potions etc. (wrath's and lust's monsters: halved) */
	public static float healMultiplier(Char ch) {
		return regenMultiplier(ch);
	}

	// ------------------------------------------------------------------ wrath

	/** a war drum skill's rage cost */
	public static int rageCost(Hero hero, int base) {
		return Math.max(1, Math.round(base * (1f - 0.10f * pts(hero, W_PRECISE))));
	}

	/** how much rage cools per turn with no enemy in view (normally 1) */
	public static float rageCooling(Hero hero) {
		int p = pts(hero, W_SIMMER);
		return p >= 3 ? 0f : p == 2 ? 0.25f : p == 1 ? 0.5f : 1f;
	}

	/** Char.damage on the hero: cold fury's guard, pride's monster's frailty */
	public static int onHeroDamaged(Hero hero, int dmg) {
		if (dmg <= 0) return dmg;
		int temper = pts(hero, W_TEMPER);
		if (temper > 0) {
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WrathGrudge g = hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WrathGrudge.class);
			if (g != null && g.rage >= 50) dmg = Math.round(dmg * (1f - 0.08f * temper));
		}
		if (monster(hero, HeroClass.PRIDE) && hero.HP < hero.HT) dmg = Math.round(dmg * 1.2f);
		int veil = pts(hero, S_VEIL);
		if (veil > 0 && (hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SlothRepose.Doze.class) != null
				|| hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep.class) != null)) {
			dmg = Math.round(dmg * (1f - 0.20f * veil));
		}
		return dmg;
	}

	/** wrath's monster: armor protects half as well */
	public static float armorMultiplier(Hero hero) {
		return monster(hero, HeroClass.WRATH) ? 0.5f : 1f;
	}

	/** WrathHooks.onAttack, melee: +0.3/0.6/0.9% per point of rage */
	public static int wrathMelee(Hero hero, int dmg) {
		int p = pts(hero, W_BLOODLUST);
		if (p <= 0) return dmg;
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WrathGrudge g = hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WrathGrudge.class);
		return g == null ? dmg : Math.round(dmg * (1f + 0.003f * p * g.rage));
	}

	/** Mob.die: anything the awakening does on a death */
	public static void onMobDeath(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob mob, Object cause) {
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive() || hero.awakening == SinAwakening.NONE) return;
		if (heroCaused(cause)) onHeroKill(hero, mob);
		slothLegion(hero, mob, cause);
	}

	public static boolean heroCaused(Object cause) {
		return cause == Dungeon.hero
				|| cause instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon
				|| cause instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment
				|| cause instanceof com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand
				|| cause instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility
				|| cause instanceof Class;
	}

	/** Hero kills a mob: wrath's slaughter, envy's theft */
	public static void onHeroKill(Hero hero, com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob mob) {
		if (mob.alignment != Char.Alignment.ENEMY) return;
		int s = pts(hero, W_SLAUGHTER);
		if (s > 0) {
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WrathGrudge g = hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WrathGrudge.class);
			if (g != null) g.addRage(10f * s);
		}
		if (monster(hero, HeroClass.ENVY)) {
			Integer k = hero.awakeningCounters.get("e_kills");
			hero.awakeningCounters.put("e_kills", (k == null ? 0 : k) + 1);
			int form = pts(hero, E_TAKEFORM);
			if (form > 0 && com.watabou.utils.Random.Float() < 0.05f * form) envyTakeForm(hero, mob);
		}
	}

	/** the undying rage: a lethal blow is survived with 1 HP for 100 rage (1/2/3 times per floor) */
	public static boolean wrathUndying(Hero hero) {
		int p = pts(hero, W_UNDYING);
		if (p <= 0) return false;
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WrathGrudge g = hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WrathGrudge.class);
		if (g == null || g.rage < 100) return false;
		String key = "w_undying_" + Dungeon.depth + "_" + Dungeon.branch;
		Integer used = hero.awakeningCounters.get(key);
		if (used != null && used >= p) return false;
		hero.awakeningCounters.put(key, (used == null ? 0 : used) + 1);
		g.rage -= 100;
		hero.HP = 1;
		hero.sprite.emitter().burst(Speck.factory(Speck.STAR), 8);
		GLog.p(Messages.get(AwakeningHooks.class, "undying"));
		return true;
	}

	// ------------------------------------------------------------------ lust

	public static float lustCost(Hero hero) {
		return 1f - 0.15f * pts(hero, L_PRICE);
	}

	public static float lustCharm(Hero hero) {
		return 1f + 0.25f * pts(hero, L_DEEPCHARM);
	}

	/** lifesteal rate on top of LustDesire.lifestealFor (charmed: elegant; anyone: the feast) */
	public static float lustSteal(Hero hero, Char enemy, float base) {
		float r = base * (1f + 0.20f * pts(hero, L_ELEGANT));
		r += 0.06f * pts(hero, L_FEAST);
		return r;
	}

	public static float lustEvasion(Hero hero) {
		return 1f + 0.05f * pts(hero, L_NIGHTDANCE);
	}

	// ------------------------------------------------------------------ envy

	public static int envySlots(Hero hero) {
		int p = pts(hero, E_SLOTS);
		return p >= 3 ? 2 : p > 0 ? 1 : 0;
	}

	public static float envyCost(Hero hero) {
		return 1f - 0.10f * pts(hero, E_THRIFT);
	}

	public static float envyGrowth(Hero hero) {
		return 0.05f * pts(hero, E_GROWTH);
	}

	/** EnvyHooks.onAttack: stolen strength and rivalry */
	public static int envyDamage(Hero hero, int dmg) {
		int steal = pts(hero, E_STEAL);
		if (steal > 0) {
			Integer k = hero.awakeningCounters.get("e_kills");
			if (k != null) dmg += Math.round(k * 0.1f * steal);
		}
		int riv = pts(hero, E_RIVALRY);
		if (riv > 0 && rivalInView(hero)) dmg = Math.round(dmg * (1f + 0.10f * riv));
		return dmg;
	}

	private static boolean rivalInView(Hero h) {
		for (Char ch : com.shatteredpixel.shatteredpixeldungeon.actors.Actor.chars()) {
			if (ch.alignment != Char.Alignment.ENEMY || !ch.isAlive() || !Dungeon.level.heroFOV[ch.pos]) continue;
			if (ch.properties().contains(Char.Property.BOSS) || ch.properties().contains(Char.Property.MINIBOSS)
					|| ch.HT >= h.HT / 2) return true;
		}
		return false;
	}

	/** take its form: a quarter of the jealousy gauge at once */
	private static void envyTakeForm(Hero hero, com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob mob) {
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnvyCovet c = hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnvyCovet.class);
		if (c == null) return;
		c.addJealousy(c.maxJealousy() * 0.25f);
		hero.sprite.emitter().burst(Speck.factory(Speck.STAR), 4);
		GLog.p(Messages.get(AwakeningHooks.class, "take_form", mob.name()));
	}

	// ------------------------------------------------------------------ sloth

	public static float slothDoze(Hero hero) {
		return 1f - 0.20f * pts(hero, S_NAP);
	}

	public static float slothPuppetStats() {
		return 1f + 0.15f * pts(Dungeon.hero, S_PUPPETS);
	}

	public static int slothMorePuppets(Hero hero) {
		int p = pts(hero, S_MORE);
		return p >= 3 ? 2 : p > 0 ? 1 : 0;
	}

	public static float slothMelee(Hero hero, float base) {
		return monster(hero, HeroClass.SLOTH) ? 0.5f : base;
	}

	public static float slothAutoAttack(Hero hero) {
		return 1f + 0.30f * pts(hero, S_SLEEPWALK);
	}

	/** a puppet's kill may raise another nightmare */
	private static void slothLegion(Hero hero, com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob mob, Object cause) {
		int p = pts(hero, S_LEGION);
		if (p <= 0 || !(cause instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DreamPuppet)) return;
		if (mob.alignment != Char.Alignment.ENEMY || com.watabou.utils.Random.Float() >= 0.10f * p) return;
		com.shatteredpixel.shatteredpixeldungeon.sloth.SlothHooks.spawnNear(mob.pos,
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DreamPuppet.create(true, 20f));
	}

	// ------------------------------------------------------------------ per-turn (AwakeningTicker)

	public static void tick(Hero hero) {
		int aura = pts(hero, L_AURA);
		if (aura > 0) {
			for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob m : Dungeon.level.mobs.toArray(new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob[0])) {
				if (m.alignment != Char.Alignment.ENEMY || !m.isAlive() || Dungeon.level.distance(hero.pos, m.pos) > 2) continue;
				if (com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LustDesire.charmImmune(m) || m.isCharmedBy(hero)) continue;
				if (com.watabou.utils.Random.Float() < 0.10f * aura) {
					com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LustDesire.charm(hero, m, 3f);
					if (m.sprite != null) m.sprite.emitter().burst(Speck.factory(Speck.HEART), 2);
				}
			}
		}
		if (monster(hero, HeroClass.ENVY)) {
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnvyCovet c = hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnvyCovet.class);
			if (c != null && c.jealousy < 1f && hero.visibleEnemies() > 0) {
				hero.damage(Math.max(1, hero.HT / 100), c);
				if (!hero.isAlive()) {
					Dungeon.fail(AwakeningHooks.class);
					GLog.n(Messages.get(AwakeningHooks.class, "envy_consumed"));
				}
			}
		}
	}

	/** ticks the awakening's per-turn effects; attached at the awakening and kept on the hero */
	public static class AwakeningTicker extends Buff {
		{
			revivePersists = true;
		}
		@Override
		public boolean act() {
			if (target instanceof Hero && target.isAlive()) tick((Hero) target);
			spend(TICK);
			return true;
		}
	}

	// ------------------------------------------------------------------ pride

	public static boolean prideKeepsPoise(Hero hero) {
		int p = pts(hero, P_POISE);
		return p > 0 && com.watabou.utils.Random.Float() < 0.20f * p;
	}

	public static int prideReign(Hero hero) {
		int p = pts(hero, P_REIGN);
		return p == 0 || hero == null ? 0 : hero.visibleEnemies() * p;
	}

	/** PrideHooks.onAttack multiplier (also counts the divine eye's sure shots) */
	public static float prideDamage(Hero hero, Char enemy, boolean ranged) {
		float m = 1f;
		if (ranged) m *= 1f + 0.10f * pts(hero, P_ROYALSHOT);
		int abs = pts(hero, P_ABSOLUTE);
		if (abs > 0 && hero.HP >= hero.HT) m *= 1f + 0.20f * abs;
		if (ranged && enemy != null && pts(hero, P_DIVINEEYE) > 0) {
			Buff.affect(enemy, DivineMark.class).count++;
		}
		return m;
	}

	public static boolean divineEye(Hero hero, Char target) {
		int p = pts(hero, P_DIVINEEYE);
		if (p <= 0 || target == null) return false;
		if (!(hero.belongings.attackingWeapon() instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon)) return false;
		DivineMark m = target.buff(DivineMark.class);
		return m == null || m.count < p;
	}

	/** how many thrown attacks pride's monster has landed on this foe */
	public static class DivineMark extends Buff {
		public int count = 0;
		{
			revivePersists = true;
		}
		@Override
		public void storeInBundle(com.watabou.utils.Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("count", count);
		}
		@Override
		public void restoreFromBundle(com.watabou.utils.Bundle bundle) {
			super.restoreFromBundle(bundle);
			count = bundle.getInt("count");
		}
	}

	// ------------------------------------------------------------------ greed

	/** GreedVault: extra interest rate on each new floor */
	public static float greedInterest(Hero hero) {
		return 0.02f * pts(hero, R_INTEREST);
	}

	/** Shopkeeper.sellPrice multiplier */
	public static float shopPrice() {
		return 1f - 0.10f * pts(Dungeon.hero, R_DISCOUNT);
	}

	/** greed's monster can't use shops */
	public static boolean shopBanned() {
		return monster(Dungeon.hero, HeroClass.GREED);
	}

	/** GreedHooks.onGoldPickup */
	public static void onGoldPickup(Hero hero, int quantity) {
		int p = pts(hero, R_GOLDSHIELD);
		if (p <= 0 || quantity <= 0) return;
		Barrier b = hero.buff(Barrier.class);
		int cur = b == null ? 0 : b.shielding();
		int cap = Math.round(hero.HT * 0.25f);
		int add = Math.min(Math.round(quantity * 0.10f * p), cap - cur);
		if (add > 0) Buff.affect(hero, Barrier.class).incShield(add);
	}

	/** GreedHooks.drBonus */
	public static int greedDR(Hero hero) {
		int p = pts(hero, R_GOLDSKIN);
		return p == 0 ? 0 : (int) Math.min(1000, (long) Math.max(0, Dungeon.gold) / 1000 * p);
	}

	/** GreedHooks.onAttack (melee) */
	public static int greedMelee(Hero hero) {
		int p = pts(hero, R_GOLDFIST);
		return p == 0 ? 0 : (int) Math.min(1000, (long) Math.max(0, Dungeon.gold) / 500 * p);
	}

	/** GreedHooks.onMobDeath: extra gold from each kill */
	public static void greedPlunder(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob mob) {
		int p = pts(Dungeon.hero, R_PLUNDER);
		if (p <= 0 || mob.alignment != Char.Alignment.ENEMY) return;
		Item g = new com.shatteredpixel.shatteredpixeldungeon.items.Gold().random();
		int q = Math.max(1, Math.round(g.quantity() * 0.5f * p));
		Dungeon.level.drop(new com.shatteredpixel.shatteredpixeldungeon.items.Gold(q), mob.pos).sprite.drop();
	}

	// ------------------------------------------------------------------ gluttony

	public static final String[] BAG_KEYS = {"g_bag_pouch", "g_bag_bandolier", "g_bag_holder", "g_bag_holster"};
	public static final int BAG_POINTS = 150;

	private static int bagIndex(Item bag) {
		if (bag instanceof VelvetPouch) return 0;
		if (bag instanceof PotionBandolier) return 1;
		if (bag instanceof ScrollHolder) return 2;
		if (bag instanceof MagicalHolster) return 3;
		return -1;
	}

	public static boolean swallowed(Hero hero, int idx) {
		Integer v = hero.awakeningCounters.get(BAG_KEYS[idx]);
		return v != null && v > 0;
	}

	/** biting reaches this far (gluttony's monster: 2) */
	public static int biteRange(Hero hero) {
		return monster(hero, HeroClass.GLUTTONY) ? 2 : 1;
	}

	/** gluttony's monster swallows its side bags (and all they hold) for a huge burst of growth */
	public static void swallowBags(Hero hero) {
		PredatorStomach st = Buff.affect(hero, PredatorStomach.class);
		int count = 0;
		for (Item i : new ArrayList<>(hero.belongings.backpack.items)) {
			int idx = bagIndex(i);
			//the magical holster is never swallowed (owner's call): wands and thrown weapons stay usable
			if (idx < 0 || idx == 3) continue;
			swallowBag(hero, st, (Bag) i, idx);
			count++;
		}
		if (count > 0) {
			Sample.INSTANCE.play(Assets.Sounds.EAT);
			hero.sprite.emitter().burst(Speck.factory(Speck.STAR), 10);
		}
	}

	private static void swallowBag(Hero hero, PredatorStomach st, Bag bag, int idx) {
		for (Item content : new ArrayList<>(bag.items)) {
			if (PredatorStomach.category(content) >= 0) {
				for (int q = 0; q < content.quantity(); q++) st.consume(hero, content, true);
			} else {
				st.addPoints(PredatorStomach.CAT_FOOD, 5f * content.quantity());
			}
		}
		bag.items.clear();
		bag.detachAll(hero.belongings.backpack);
		st.addPoints(PredatorStomach.CAT_FOOD, BAG_POINTS / 3f);
		st.addPoints(PredatorStomach.CAT_POTION, BAG_POINTS / 3f);
		st.addPoints(PredatorStomach.CAT_SCROLL, BAG_POINTS / 3f);
		hero.awakeningCounters.put(BAG_KEYS[idx], 1);
		GLog.p(Messages.get(AwakeningHooks.class, "bag_swallowed", bag.name()));
		Item.updateQuickslot();
	}

	/**
	 * Item.doPickUp, after the item went into the pack: gluttony's monster uses items of a swallowed
	 * bag's kind on the spot (and swallows any new side bag of those kinds).
	 */
	public static void onPickedUp(Hero hero, Item item) {
		if (!monster(hero, HeroClass.GLUTTONY) || item == null) return;
		int bag = bagIndex(item);
		if (bag >= 0 && bag != 3) {
			swallowBag(hero, Buff.affect(hero, PredatorStomach.class), (Bag) item, bag);
			return;
		}
		boolean used = false;
		autoUsing = true;
		try {
		if (item instanceof Potion && swallowed(hero, 1)) {
			Potion p = (Potion) item.detach(hero.belongings.backpack);
			if (p != null) {
				p.forceDrink(hero);
				used = true;
			}
		} else if (item instanceof Scroll && swallowed(hero, 2)) {
			((Scroll) item).forceRead(hero);
			used = true;
		} else if (item instanceof Plant.Seed && swallowed(hero, 0)) {
			Plant.Seed s = (Plant.Seed) item.detach(hero.belongings.backpack);
			if (s != null) {
				hero.belongings.backpack.items.add(s); //eatSeed detaches it itself
				com.shatteredpixel.shatteredpixeldungeon.predator.PredatorHooks.eatSeed(hero, s);
				used = true;
			}
		} else if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone && swallowed(hero, 0)) {
			Item s = item.detachAll(hero.belongings.backpack);
			PredatorStomach st = Buff.affect(hero, PredatorStomach.class);
			for (int q = 0; q < s.quantity(); q++) st.consume(hero, s, true);
			used = true;
		} else if ((item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon
				|| item instanceof com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand) && swallowed(hero, 3)) {
			Item s = item.detachAll(hero.belongings.backpack);
			Buff.affect(hero, PredatorStomach.class).addPoints(PredatorStomach.CAT_FOOD, 5f * s.quantity());
			used = true;
		}
		} finally {
			autoUsing = false;
		}
		if (used) onAutoUsed(hero);
	}

	/** an item went off on pickup: the monster's talents */
	private static void onAutoUsed(Hero hero) {
		int acid = pts(hero, G_ACID);
		if (acid > 0) {
			int heal = Math.min(hero.HT - hero.HP, Math.round(hero.HT * 0.03f * acid));
			if (heal > 0) {
				hero.HP += heal;
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			}
		}
		int dash = pts(hero, G_DASH);
		if (dash > 0) Buff.prolong(hero, Haste.class, 1 + dash);
		Item.updateQuickslot();
	}

	/** extra growth for items used on pickup (PredatorStomach.score) */
	public static boolean autoUsing = false;

	/** PredatorStomach.score multiplier */
	public static float gluttonyScore(Hero hero) {
		float m = 1f + 0.20f * pts(hero, G_DIGESTION);
		if (autoUsing) m += 0.25f * pts(hero, G_STOMACH);
		return m;
	}

	/** PredatorMaw.hungerCost multiplier */
	public static float gluttonyCost(Hero hero) {
		return 1f - 0.15f * pts(hero, G_RESTRAINT);
	}

	/** PredatorHooks.onFoodEaten */
	public static void onFoodEaten(Hero hero) {
		int p = pts(hero, G_NOURISH);
		if (p > 0) Buff.affect(hero, Barrier.class).setShield(Math.round(hero.HT * 0.10f * p));
	}
}
