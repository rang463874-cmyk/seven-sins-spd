package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SinArtifact;
import com.shatteredpixel.shatteredpixeldungeon.skills.BasicSkill;
import com.shatteredpixel.shatteredpixeldungeon.skills.SinSkill;
import com.shatteredpixel.shatteredpixeldungeon.skills.SkillHolder;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndSkills;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GreedVault;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTradeItem;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The Greed class's signature item. Doesn't take an equipment slot.
 * Carried gold raises stats; picking up gold fills a payment gauge spent on payment skills,
 * including selling items anywhere at shop prices.
 */
public class GoldenCard extends SinArtifact {

	/** gilding costs 300, +150 per earlier use, but never more than this (owner's call) */
	public static final int GILD_MAX = 600;
	/** Treasure Map: gauge cost (before payment discounts) */
	public static final int COST_MAP = 150;

	/** reveals the whole floor (layout, items and secrets) */
	private static void treasureMap(Hero hero) {
		int cost = discounted(hero, COST_MAP);
		if (vault(hero).charge < cost) return;
		vault(hero).spend(cost);
		com.shatteredpixel.shatteredpixeldungeon.skills.MapReveal.reveal(true);
		com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite.show(hero, com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite.MAP);
		com.watabou.noosa.audio.Sample.INSTANCE.play(com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.GOLD);
		com.shatteredpixel.shatteredpixeldungeon.utils.GLog.p(Messages.get(GoldenCard.class, "treasuremap_done"));
		hero.sprite.operate(hero.pos);
		hero.spendAndNext(1f);
	}

	public static final String AC_PAY = "PAY";
	public static final String AC_SELL = "SELL";

	{
		image = ItemSpriteSheet.GOLDEN_CARD;
		defaultAction = AC_PAY;
		unique = true;
		bones = false;
		keptThoughLostInvent = true;
	}

	// ------------------------------------------------------------------ costs

	private static int discounted(Hero hero, int cost) {
		float mult = 1f - 0.15f * hero.pointsInTalent(Talent.GREED_DISCOUNT);
		GreedVault v = hero.buff(GreedVault.class);
		if (v != null && v.mirrored) mult *= 0.75f;
		return Math.max(1, Math.round(cost * mult));
	}

	public static int coinShotCost(Hero hero) {
		return discounted(hero, 15 + 3 * Dungeon.depth);
	}

	public static int guardCost(Hero hero) {
		return discounted(hero, 20 + 4 * Dungeon.depth);
	}

	public static int expressCost(Hero hero) {
		return discounted(hero, 10 + 2 * Dungeon.depth);
	}

	public static int buyCost(Hero hero) {
		return discounted(hero, 10 + 2 * Dungeon.depth);
	}

	/** gauge cost of the next upgrade bought with the card */
	public static int gildCost(Hero hero) {
		GreedVault v = hero.buff(GreedVault.class);
		return discounted(hero, Math.min(GILD_MAX, 300 + 150 * (v == null ? 0 : v.gilded)));
	}

	public static int sellCost(Hero hero) {
		return discounted(hero, 10 + 2 * Dungeon.depth);
	}

	public static int bribeCost(Hero hero, Char target) {
		return discounted(hero, 2 * target.HT);
	}

	public static int loanAmount(Char target) {
		return 3 * target.HT;
	}

	public static int expressRange(Hero hero) {
		return 4 + hero.pointsInTalent(Talent.GREED_EXPRESS);
	}

	public static int guardShield(Hero hero, GreedVault v) {
		return 6 + hero.lvl + Math.round(v.pts() * 0.5f);
	}

	// ------------------------------------------------------------------ actions

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.remove(AC_DROP);
		actions.remove(AC_THROW);
		if (isEquipped(hero)) actions.add(AC_PAY);
		if (isEquipped(hero)) actions.add(AC_SELL);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		switch (action) {
			case AC_PAY: return Messages.get(this, "ac_pay");
			case AC_SELL: return Messages.get(this, "ac_sell");
		}
		return super.actionName(action, hero);
	}

	static GreedVault vault(Hero hero) {
		return Buff.affect(hero, GreedVault.class);
	}

	@Override
	public void execute(final Hero hero, String action) {
		super.execute(hero, action);
		if (!canUseSkills(hero, action)) return;
		final GreedVault v = vault(hero);

		if (action.equals(AC_SELL)) {
			if (v.charge < sellCost(hero)) {
				GLog.w(Messages.get(this, "no_balance"));
				return;
			}
			GameScene.selectItem(sellSelector);

		} else if (action.equals(AC_PAY)) {
			showPayMenu(hero, v);
		}
	}

	private void showPayMenu(final Hero hero, final GreedVault v) {
		GameScene.show(new WndSkills(this, hero));
	}

	// ------------------------------------------------------------------ skills list

	@Override
	protected String levelEffects(Hero hero) {
		return Messages.get(SinArtifact.class, "effects_gauge", Math.round((SinArtifact.gaugeScale(hero) - 1f) * 100), Math.round((SinArtifact.gaugeScale(hero) - 1f) * 100));
	}

	@Override
	public String skillHeader(Hero hero) {
		GreedVault v = vault(hero);
		return Messages.get(this, "skill_header", v.chargeInt(), Math.round(v.maxCharge()), Dungeon.gold);
	}

	@Override
	public ArrayList<SinSkill> skills(final Hero hero) {
		ArrayList<SinSkill> list = new ArrayList<>();
		list.add(new BasicSkill(GoldenCard.class, "coinshot", 8, SinSkill.CostType.GAUGE).projectile()
				.cost(h -> coinShotCost(h))
				.usableIf(h -> vault(h).charge >= coinShotCost(h))
				.descArgs(h -> new Object[]{2 + h.lvl / 2, 6 + h.lvl})
				.onUse(h -> GameScene.selectCell(coinShot)));
		list.add(new BasicSkill(GoldenCard.class, "guard", 9, SinSkill.CostType.GAUGE)
				.cost(h -> guardCost(h))
				.usableIf(h -> vault(h).charge >= guardCost(h))
				.descArgs(h -> new Object[]{guardShield(h, vault(h))})
				.onUse(h -> goldGuard(h, vault(h))));
		list.add(new BasicSkill(GoldenCard.class, "express", 10, SinSkill.CostType.GAUGE)
				.cost(h -> expressCost(h))
				.usableIf(h -> vault(h).charge >= expressCost(h))
				.descArgs(h -> new Object[]{expressRange(h)})
				.onUse(h -> GameScene.selectCell(express)));
		list.add(new BasicSkill(GoldenCard.class, "bribe", 11, SinSkill.CostType.VARIES).targetsEnemy()
				.usableIf(h -> vault(h).charge > 0)
				.descArgs(h -> new Object[]{Math.round(10f * (1f + 0.5f * h.pointsInTalent(Talent.GREED_BRIBERY)))})
				.onUse(h -> GameScene.selectCell(bribe)));
		list.add(new BasicSkill(GoldenCard.class, "loan", 12, SinSkill.CostType.VARIES).targetsEnemy()
				.visibleIf(h -> h.subClass == HeroSubClass.USURER)
				.usableIf(h -> Dungeon.gold > 0)
				.onUse(h -> GameScene.selectCell(loan)));
		list.add(new BasicSkill(GoldenCard.class, "sell", 13, SinSkill.CostType.GAUGE)
				.cost(h -> sellCost(h))
				.usableIf(h -> vault(h).charge >= sellCost(h))
				.onUse(h -> GameScene.selectItem(sellSelector)));
		list.add(new BasicSkill(GoldenCard.class, "shop", 14, SinSkill.CostType.GAUGE)
				.cost(h -> buyCost(h))
				.usableIf(h -> vault(h).charge >= buyCost(h))
				.onUse(GoldenCard::mailOrder));
		//Treasure Map replaced Gilding in Nov 2026 (growth copy covers upgrades now); gilding code kept for reference
		list.add(new BasicSkill(GoldenCard.class, "treasuremap", 15, SinSkill.CostType.GAUGE)
				.cost(h -> discounted(h, COST_MAP))
				.usableIf(h -> vault(h).charge >= discounted(h, COST_MAP))
				.onUse(GoldenCard::treasureMap));
		//growth copy: forgery (the whole payment gauge)
		com.shatteredpixel.shatteredpixeldungeon.skills.GrowthCopy.add(list, GoldenCard.class, 1, com.shatteredpixel.shatteredpixeldungeon.skills.SinSkill.CostType.GAUGE,
				h -> (int) vault(h).maxCharge(), h -> vault(h).chargeInt() >= (int) vault(h).maxCharge(),
				h -> vault(h).spend((int) vault(h).maxCharge()), true);
		return list;
	}

	// ------------------------------------------------------------------ payment skills

	private static Char enemyAt(Integer cell) {
		if (cell == null) return null;
		Char ch = Actor.findChar(cell);
		if (ch == null || ch == Dungeon.hero || ch.alignment == Char.Alignment.ALLY) return null;
		if (!Dungeon.level.heroFOV[cell]) return null;
		return ch;
	}

	private final CellSelector.Listener coinShot = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			final Hero hero = Dungeon.hero;
			if (cell == null || cell == hero.pos) return;
			final GreedVault v = vault(hero);
			final int cost = coinShotCost(hero);
			final Ballistica path = new Ballistica(hero.pos, cell, Ballistica.PROJECTILE);
			final Char enemy = Actor.findChar(path.collisionPos);
			if (enemy == null || enemy == hero || enemy.alignment == Char.Alignment.ALLY) {
				GLog.w(Messages.get(GoldenCard.class, "no_target"));
				return;
			}
			if (!v.spend(cost)) {
				GLog.w(Messages.get(GoldenCard.class, "no_balance"));
				return;
			}
			hero.sprite.zap(enemy.pos);
			hero.busy();
			((MissileSprite) hero.sprite.parent.recycle(MissileSprite.class)).reset(
					hero.sprite, enemy.pos, new Gold(), new Callback() {
						@Override
						public void call() {
							int dmg = Random.NormalIntRange(2 + hero.lvl / 2, 6 + hero.lvl) + v.meleeBonus() / 2;
							dmg = Math.round(dmg * (1f + 0.25f * hero.pointsInTalent(Talent.GREED_COINSHOT)));
							enemy.damage(dmg, GoldenCard.class);
							Sample.INSTANCE.play(Assets.Sounds.GOLD, 1f, 1.3f);
							Invisibility.dispel();
							hero.spendAndNext(1f);
						}
					});
		}

		@Override
		public String prompt() {
			return Messages.get(GoldenCard.class, "prompt_coinshot");
		}
	};

	private static void goldGuard(Hero hero, GreedVault v) {
		if (!v.spend(guardCost(hero))) {
			GLog.w(Messages.get(GoldenCard.class, "no_balance"));
			return;
		}
		int shield = guardShield(hero, v);
		Buff.affect(hero, Barrier.class).setShield(shield);
		hero.sprite.showStatus(CharSprite.POSITIVE, Integer.toString(shield));
		hero.sprite.operate(hero.pos);
		hero.sprite.centerEmitter().burst(Speck.factory(Speck.COIN), 8);
		Sample.INSTANCE.play(Assets.Sounds.GOLD);
		hero.spendAndNext(1f);
	}

	private final CellSelector.Listener express = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero hero = Dungeon.hero;
			if (cell == null || cell == hero.pos) return;
			GreedVault v = vault(hero);
			boolean ok = Dungeon.level.heroFOV[cell]
					&& Dungeon.level.passable[cell]
					&& Actor.findChar(cell) == null
					&& Dungeon.level.distance(hero.pos, cell) <= expressRange(hero)
					&& new Ballistica(hero.pos, cell, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID).collisionPos.equals(cell);
			if (!ok) {
				GLog.w(Messages.get(GoldenCard.class, "bad_cell"));
				return;
			}
			if (!v.spend(expressCost(hero))) {
				GLog.w(Messages.get(GoldenCard.class, "no_balance"));
				return;
			}
			ScrollOfTeleportation.appear(hero, cell);
			Dungeon.level.occupyCell(hero);
			Dungeon.observe();
			GameScene.updateFog();
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(GoldenCard.class, "prompt_express", expressRange(Dungeon.hero));
		}
	};

	private final CellSelector.Listener bribe = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero hero = Dungeon.hero;
			Char enemy = enemyAt(cell);
			if (enemy == null) {
				if (cell != null) GLog.w(Messages.get(GoldenCard.class, "no_target"));
				return;
			}
			if (!(enemy instanceof Mob) || enemy.properties().contains(Char.Property.BOSS)
					|| enemy.properties().contains(Char.Property.MINIBOSS)) {
				GLog.w(Messages.get(GoldenCard.class, "bribe_refused"));
				return;
			}
			GreedVault v = vault(hero);
			int cost = bribeCost(hero, enemy);
			if (!v.spend(cost)) {
				GLog.w(Messages.get(GoldenCard.class, "too_expensive", cost));
				return;
			}
			float dur = 10f * (1f + 0.5f * hero.pointsInTalent(Talent.GREED_BRIBERY));
			Buff.affect(enemy, Charm.class, dur).object = hero.id();
			enemy.sprite.showStatusWithIcon(CharSprite.NEUTRAL, Integer.toString(cost), com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.GOLD);
			Sample.INSTANCE.play(Assets.Sounds.GOLD);
			hero.sprite.zap(enemy.pos);
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(GoldenCard.class, "prompt_bribe");
		}
	};

	private final CellSelector.Listener loan = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero hero = Dungeon.hero;
			Char enemy = enemyAt(cell);
			if (enemy == null) {
				if (cell != null) GLog.w(Messages.get(GoldenCard.class, "no_target"));
				return;
			}
			if (enemy.buff(GreedVault.Debt.class) != null) {
				GLog.w(Messages.get(GoldenCard.class, "already_indebted"));
				return;
			}
			GreedVault v = vault(hero);
			int amount = loanAmount(enemy);
			if (Dungeon.gold < amount) {
				GLog.w(Messages.get(GoldenCard.class, "loan_no_gold", amount));
				return;
			}
			Dungeon.gold -= amount;
			v.goldChanged();
			Buff.affect(enemy, GreedVault.Debt.class).principal = amount;
			enemy.sprite.showStatusWithIcon(CharSprite.NEGATIVE, Integer.toString(amount), com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.GOLD);
			GLog.i(Messages.get(GoldenCard.class, "loaned", enemy.name(), amount));
			Sample.INSTANCE.play(Assets.Sounds.GOLD);
			hero.sprite.zap(enemy.pos);
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(GoldenCard.class, "prompt_loan");
		}
	};

	// ------------------------------------------------------------------ remote selling

	private final WndBag.ItemSelector sellSelector = new WndBag.ItemSelector() {
		@Override
		public String textPrompt() {
			return Messages.get(GoldenCard.class, "sell_prompt", sellCost(Dungeon.hero));
		}

		@Override
		public boolean itemSelectable(Item item) {
			return Shopkeeper.canSell(item);
		}

		@Override
		public void onSelect(final Item item) {
			final Hero hero = Dungeon.hero;
			if (item == null || hero == null || !hero.isAlive()) return;
			final GreedVault v = vault(hero);
			final int cost = sellCost(hero);
			final boolean stack = item.quantity() > 1 && !(item instanceof MissileWeapon && item.isUpgradable());
			final int priceAll = item.value();
			ArrayList<String> opts = new ArrayList<>();
			if (stack) {
				opts.add(Messages.get(GoldenCard.class, "sell_one", priceAll / item.quantity()));
				opts.add(Messages.get(GoldenCard.class, "sell_all", priceAll));
			} else {
				opts.add(Messages.get(GoldenCard.class, "sell_all", priceAll));
			}
			GameScene.show(new WndOptions(new ItemSprite(item), Messages.titleCase(item.name()),
					Messages.get(GoldenCard.class, "sell_confirm", cost, v.chargeInt()),
					opts.toArray(new String[0])) {
				@Override
				protected void onSelect(int index) {
					if (!v.spend(cost)) {
						GLog.w(Messages.get(GoldenCard.class, "no_balance"));
						return;
					}
					if (stack && index == 0) WndTradeItem.sellOne(item);
					else WndTradeItem.sell(item);
					hero.sprite.centerEmitter().burst(Speck.factory(Speck.COIN), 6);
					updateQuickslot();
				}
			});
		}
	};

	// ------------------------------------------------------------------ mail order

	public static int buyPrice(Item item) {
		return Shopkeeper.sellPrice(item);
	}

	private static void mailOrder(final Hero hero) {
		final GreedVault v = vault(hero);
		final ArrayList<Item> stock = v.catalog();
		if (stock.isEmpty()) {
			GLog.w(Messages.get(GoldenCard.class, "shop_empty"));
			return;
		}
		String[] labels = new String[stock.size()];
		for (int i = 0; i < stock.size(); i++) {
			Item it = stock.get(i);
			labels[i] = Messages.titleCase(it.name()) + "  (" + buyPrice(it) + "g)";
		}
		GameScene.show(new WndOptions(new ItemSprite(ItemSpriteSheet.GOLDEN_CARD),
				Messages.get(GoldenCard.class, "shop_title"),
				Messages.get(GoldenCard.class, "shop_msg", buyCost(hero), Dungeon.gold), labels) {
			@Override
			protected void onSelect(int index) {
				Item it = stock.get(index);
				int price = buyPrice(it);
				if (Dungeon.gold < price) {
					GLog.w(Messages.get(GoldenCard.class, "shop_no_gold"));
					return;
				}
				if (!v.spend(buyCost(hero))) {
					GLog.w(Messages.get(GoldenCard.class, "no_balance"));
					return;
				}
				Dungeon.gold -= price;
				stock.remove(index);
				if (!it.doPickUp(hero)) {
					Dungeon.level.drop(it, hero.pos).sprite.drop();
				}
				hero.updateHT(false);
				GLog.i(Messages.get(GoldenCard.class, "shop_bought", it.name(), price));
				Sample.INSTANCE.play(Assets.Sounds.GOLD);
				hero.sprite.centerEmitter().burst(Speck.factory(Speck.COIN), 6);
				updateQuickslot();
			}

			@Override
			protected boolean hasInfo(int index) {
				return true;
			}

			@Override
			protected void onInfo(int index) {
				Item it = stock.get(index);
				GameScene.show(new com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage(
						new ItemSprite(it), Messages.titleCase(it.name()), it.info()));
			}
		});
	}

	// ------------------------------------------------------------------ gilding (upgrades)

	private final WndBag.ItemSelector gildSelector = new WndBag.ItemSelector() {
		@Override
		public String textPrompt() {
			return Messages.get(GoldenCard.class, "gild_prompt", gildCost(Dungeon.hero));
		}

		@Override
		public boolean itemSelectable(Item item) {
			return item.isUpgradable();
		}

		@Override
		public void onSelect(Item item) {
			final Hero hero = Dungeon.hero;
			if (item == null || hero == null || !hero.isAlive() || !item.isUpgradable()) return;
			GreedVault v = vault(hero);
			if (!v.spend(gildCost(hero))) {
				GLog.w(Messages.get(GoldenCard.class, "no_balance"));
				return;
			}
			v.gilded++;
			item.upgrade();
			com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade.upgrade(hero);
			GLog.p(Messages.get(GoldenCard.class, "gilded", item.name()));
			hero.sprite.centerEmitter().burst(Speck.factory(Speck.COIN), 10);
			hero.spendAndNext(1f);
			updateQuickslot();
		}
	};

	// ------------------------------------------------------------------ info

	@Override
	public String status() {
		Hero h = Dungeon.hero;
		GreedVault v = h == null ? null : h.buff(GreedVault.class);
		return v == null ? null : Integer.toString(v.chargeInt());
	}

	@Override
	public String desc() {
		String d = Messages.get(this, "desc");
		Hero h = Dungeon.hero;
		GreedVault v = h == null ? null : h.buff(GreedVault.class);
		if (v != null) d += "\n\n" + v.statsText();
		return d;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 0;
	}
}
