package com.shatteredpixel.shatteredpixeldungeon.items;

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
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The Greed class's signature item. Doesn't take an equipment slot.
 * Deposit gold to permanently raise stats, then spend the balance on payment skills.
 */
public class GoldenCard extends Item {

	public static final String AC_PAY = "PAY";
	public static final String AC_DEPOSIT = "DEPOSIT";
	public static final String AC_WITHDRAW = "WITHDRAW";

	{
		image = ItemSpriteSheet.GOLDEN_CARD;
		defaultAction = AC_PAY;
		unique = true;
		bones = false;
		keptThoughLostInvent = true;
	}

	// ------------------------------------------------------------------ costs

	private static int discounted(Hero hero, int cost) {
		return Math.max(1, Math.round(cost * (1f - 0.15f * hero.pointsInTalent(Talent.GREED_DISCOUNT))));
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
		return 6 + hero.lvl + Math.round(v.creditPts() * 0.5f);
	}

	// ------------------------------------------------------------------ actions

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.remove(AC_DROP);
		actions.remove(AC_THROW);
		actions.add(AC_PAY);
		actions.add(AC_DEPOSIT);
		actions.add(AC_WITHDRAW);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		switch (action) {
			case AC_PAY: return Messages.get(this, "ac_pay");
			case AC_DEPOSIT: return Messages.get(this, "ac_deposit");
			case AC_WITHDRAW: return Messages.get(this, "ac_withdraw");
		}
		return super.actionName(action, hero);
	}

	static GreedVault vault(Hero hero) {
		return Buff.affect(hero, GreedVault.class);
	}

	@Override
	public void execute(final Hero hero, String action) {
		super.execute(hero, action);
		final GreedVault v = vault(hero);

		if (action.equals(AC_DEPOSIT)) {
			final int gold = Dungeon.gold;
			if (gold <= 0) {
				GLog.w(Messages.get(this, "no_gold"));
				return;
			}
			final int[] amounts = {gold, Math.max(1, gold / 2), Math.min(100, gold)};
			GameScene.show(new WndOptions(new ItemSprite(this), Messages.titleCase(name()),
					Messages.get(this, "deposit_prompt", gold),
					Messages.get(this, "opt_all", amounts[0]),
					Messages.get(this, "opt_half", amounts[1]),
					Messages.get(this, "opt_amount", amounts[2])) {
				@Override
				protected void onSelect(int index) {
					v.deposit(hero, amounts[index]);
					hero.spendAndNext(1f);
				}
			});

		} else if (action.equals(AC_WITHDRAW)) {
			if (v.balance <= 0) {
				GLog.w(Messages.get(this, "no_balance"));
				return;
			}
			final int all = (int) Math.min(Integer.MAX_VALUE / 2, v.balance);
			final int[] amounts = {all, Math.min(500, all), Math.min(100, all)};
			GameScene.show(new WndOptions(new ItemSprite(this), Messages.titleCase(name()),
					Messages.get(this, "withdraw_prompt", v.balance),
					Messages.get(this, "opt_all", amounts[0]),
					Messages.get(this, "opt_amount", amounts[1]),
					Messages.get(this, "opt_amount", amounts[2])) {
				@Override
				protected void onSelect(int index) {
					v.withdraw(hero, amounts[index]);
				}
			});

		} else if (action.equals(AC_PAY)) {
			showPayMenu(hero, v);
		}
	}

	private void showPayMenu(final Hero hero, final GreedVault v) {
		final boolean usurer = hero.subClass == HeroSubClass.USURER;
		ArrayList<String> opts = new ArrayList<>();
		opts.add(Messages.get(this, "pay_coinshot", coinShotCost(hero)));
		opts.add(Messages.get(this, "pay_guard", guardCost(hero)));
		opts.add(Messages.get(this, "pay_express", expressCost(hero)));
		opts.add(Messages.get(this, "pay_bribe"));
		if (usurer) opts.add(Messages.get(this, "pay_loan"));

		GameScene.show(new WndOptions(new ItemSprite(this), Messages.titleCase(name()),
				Messages.get(this, "pay_prompt", v.balance), opts.toArray(new String[0])) {
			@Override
			protected boolean enabled(int index) {
				switch (index) {
					case 0: return v.balance >= coinShotCost(hero);
					case 1: return v.balance >= guardCost(hero);
					case 2: return v.balance >= expressCost(hero);
					default: return v.balance > 0;
				}
			}

			@Override
			protected void onSelect(int index) {
				switch (index) {
					case 0: GameScene.selectCell(coinShot); break;
					case 1: goldGuard(hero, v); break;
					case 2: GameScene.selectCell(express); break;
					case 3: GameScene.selectCell(bribe); break;
					case 4: GameScene.selectCell(loan); break;
				}
			}
		});
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
			if (!v.spend(amount)) {
				GLog.w(Messages.get(GoldenCard.class, "too_expensive", amount));
				return;
			}
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

	// ------------------------------------------------------------------ info

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
