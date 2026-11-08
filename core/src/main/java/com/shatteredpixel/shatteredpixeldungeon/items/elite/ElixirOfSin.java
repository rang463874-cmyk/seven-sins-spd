package com.shatteredpixel.shatteredpixeldungeon.items.elite;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bless;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;
import com.shatteredpixel.shatteredpixeldungeon.items.SinEssence;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sins.SinRewards;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

/**
 * Elixir of sin (alchemy: 2 sin essences + any potion). For a sin hero: +20 permanent sin points and a
 * full skill resource. For anyone else: heals fully over a few turns and blesses for 30 turns.
 */
public class ElixirOfSin extends Item {

	public static final String AC_DRINK = "DRINK";
	public static final float POINTS = 20f;

	{
		image = ItemSpriteSheet.ELIXIR_OF_SIN;
		stackable = true;
		defaultAction = AC_DRINK;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_DRINK);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_DRINK)) return Messages.get(this, "ac_drink");
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (!action.equals(AC_DRINK)) return;
		detach(hero.belongings.backpack);
		com.shatteredpixel.shatteredpixeldungeon.predator.PredatorHooks.onConsumed(hero, this);
		if (SinRewards.grant(hero, POINTS)) {
			SinRewards.refill(hero);
			GLog.p(Messages.get(this, "drink_sin"));
		} else {
			Buff.affect(hero, Healing.class).setHeal(hero.HT, 0.25f, 0);
			Buff.prolong(hero, Bless.class, 30f);
			GLog.p(Messages.get(this, "drink_other"));
		}
		hero.sprite.emitter().burst(Speck.factory(Speck.UP), 6);
		Sample.INSTANCE.play(Assets.Sounds.DRINK);
		hero.sprite.operate(hero.pos);
		hero.spendAndNext(1f);
	}

	@Override public boolean isUpgradable() { return false; }
	@Override public boolean isIdentified() { return true; }
	@Override public int value() { return 150 * quantity; }
	@Override public int energyVal() { return 8 * quantity; }

	/** 2 essences + any potion (regular or exotic) */
	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe {

		private static boolean isPotion(Item i) {
			return ExoticPotion.regToExo.containsKey(i.getClass()) || ExoticPotion.regToExo.containsValue(i.getClass());
		}

		@Override
		public boolean testIngredients(ArrayList<Item> ingredients) {
			boolean essence = false, potion = false;
			for (Item i : ingredients) {
				if (i instanceof SinEssence && i.quantity() >= 2) essence = true;
				else if (isPotion(i)) potion = true;
			}
			return essence && potion && ingredients.size() == 2;
		}

		@Override
		public int cost(ArrayList<Item> ingredients) {
			return 4;
		}

		@Override
		public Item brew(ArrayList<Item> ingredients) {
			for (Item i : ingredients) {
				i.quantity(i.quantity() - (i instanceof SinEssence ? 2 : 1));
			}
			return sampleOutput(null);
		}

		@Override
		public Item sampleOutput(ArrayList<Item> ingredients) {
			return new ElixirOfSin();
		}
	}

}
