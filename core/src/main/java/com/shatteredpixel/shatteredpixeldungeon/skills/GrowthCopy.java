package com.shatteredpixel.shatteredpixeldungeon.skills;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Seven Sins: every sin's "copy a growth item" skill (Nov 2026, owner's call: no use limit, the cost is a
 * full gauge of the sin's own resource; stronger monsters are meant to balance it). Pick an identified
 * scroll of upgrade, potion of strength or scroll of enchantment from the pack, pay, and get another one.
 * Each signature item adds it with {@link #add}; strings are "skill_replicate(_desc)" in the item's namespace.
 */
public class GrowthCopy {

	/** what can be copied */
	@SuppressWarnings("unchecked")
	public static final Class<? extends Item>[] COPYABLE = new Class[]{
			ScrollOfUpgrade.class, PotionOfStrength.class, ScrollOfEnchantment.class};

	/** skill bar icons: row 9 of skill_icons.png, one per sin (72 + sin index) */
	public static final int ICON_ROW = 72;

	public interface Payment {
		/** pays the cost; false if it couldn't be paid (nothing is copied then) */
		boolean pay(Hero hero);
	}

	public static boolean copyable(Item item) {
		if (item == null || !item.isIdentified()) return false;
		for (Class<? extends Item> c : COPYABLE) {
			if (item.getClass() == c) return true;
		}
		return false;
	}

	public static boolean hasTarget(Hero hero) {
		for (Item i : hero.belongings) {
			if (copyable(i)) return true;
		}
		return false;
	}

	/**
	 * Adds the skill to a signature item's list.
	 * @param endsTurn false when the payment itself already ends the hero's turn (sloth's dozing)
	 */
	public static void add(ArrayList<SinSkill> list, final Class<?> owner, int sinIndex, SinSkill.CostType type,
	                       ToIntFunction<Hero> cost, Predicate<Hero> canPay, final Payment payment, final boolean endsTurn) {
		list.add(new BasicSkill(owner, "replicate", ICON_ROW + sinIndex, type)
				.cost(cost)
				.usableIf(h -> canPay.test(h) && hasTarget(h))
				.onUse(h -> choose(h, owner, payment, endsTurn)));
	}

	private static void choose(final Hero hero, final Class<?> owner, final Payment payment, final boolean endsTurn) {
		GameScene.selectItem(new WndBag.ItemSelector() {
			@Override
			public String textPrompt() {
				return Messages.get(GrowthCopy.class, "prompt");
			}

			@Override
			public boolean itemSelectable(Item item) {
				return copyable(item);
			}

			@Override
			public void onSelect(Item item) {
				if (!copyable(item) || !hero.isAlive()) return;
				if (!payment.pay(hero)) {
					GLog.w(Messages.get(GrowthCopy.class, "cant_pay"));
					return;
				}
				Item copy = Reflection.newInstance(item.getClass());
				if (copy == null) return;
				copy.identify();
				if (!copy.collect()) Dungeon.level.drop(copy, hero.pos).sprite.drop();
				GLog.p(Messages.get(GrowthCopy.class, "copied", copy.name()));
				if (hero.sprite != null) {
					hero.sprite.emitter().burst(Speck.factory(Speck.STAR), 8);
					hero.sprite.operate(hero.pos);
				}
				Sample.INSTANCE.play(Assets.Sounds.EVOKE);
				Item.updateQuickslot();
				if (endsTurn) hero.spendAndNext(1f);
			}
		});
	}
}
