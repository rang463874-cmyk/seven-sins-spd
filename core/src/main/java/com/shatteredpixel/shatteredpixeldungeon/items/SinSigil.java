package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.sins.SinWeapons;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

/**
 * Given by the blacksmith for 2000 favor. Reforges a melee weapon into the hero's tier 5
 * sin weapon, keeping its upgrades and enchantment.
 */
public class SinSigil extends Item {

	public static final String AC_IMPRINT = "IMPRINT";
	public static final int COST = 2000;

	{
		image = ItemSpriteSheet.SIN_SIGIL;
		defaultAction = AC_IMPRINT;
		unique = true;
		bones = false;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_IMPRINT);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_IMPRINT)) return Messages.get(this, "ac_imprint");
		return super.actionName(action, hero);
	}

	@Override
	public void execute(final Hero hero, String action) {
		super.execute(hero, action);
		if (!action.equals(AC_IMPRINT)) return;
		if (SinWeapons.tier5(hero.heroClass) == null) {
			GLog.w(Messages.get(this, "no_sin"));
			return;
		}
		GameScene.selectItem(new WndBag.ItemSelector() {
			@Override
			public String textPrompt() {
				return Messages.get(SinSigil.class, "prompt");
			}

			@Override
			public boolean itemSelectable(Item item) {
				return convertible(hero, item);
			}

			@Override
			public void onSelect(Item item) {
				if (item == null || !convertible(hero, item)) return;
				imprint(hero, (MeleeWeapon) item);
			}
		});
	}

	public static boolean convertible(Hero hero, Item item) {
		Class<?> target = SinWeapons.tier5(hero.heroClass);
		return item instanceof MeleeWeapon && target != null && !target.isInstance(item) && !item.unique;
	}

	/** replaces the weapon with the sin weapon, keeping upgrades and enchantment */
	public void imprint(Hero hero, MeleeWeapon old) {
		MeleeWeapon neu = SinWeapons.newTier5(hero.heroClass);
		if (neu == null) return;

		neu.level(old.trueLevel());
		neu.enchant(old.enchantment);
		neu.enchantHardened = old.enchantHardened;
		neu.curseInfusionBonus = old.curseInfusionBonus;
		neu.sinInfusions = old.sinInfusions;
		neu.warEdge = old.warEdge;
		neu.masteryPotionBonus = old.masteryPotionBonus;
		neu.augment = old.augment;
		neu.cursed = old.cursed;
		neu.levelKnown = old.levelKnown;
		neu.cursedKnown = old.cursedKnown;

		if (hero.belongings.weapon == old) {
			hero.belongings.weapon = neu;
			neu.activate(hero);
		} else if (hero.belongings.secondWep == old) {
			hero.belongings.secondWep = neu;
		} else {
			old.detach(hero.belongings.backpack);
			if (!neu.collect(hero.belongings.backpack)) {
				Dungeon.level.drop(neu, hero.pos).sprite.drop();
			}
		}

		detach(hero.belongings.backpack);
		Item.updateQuickslot();
		hero.sprite.operate(hero.pos);
		hero.sprite.emitter().burst(Speck.factory(Speck.STAR), 10);
		Sample.INSTANCE.play(Assets.Sounds.EVOKE);
		GLog.p(Messages.get(SinSigil.class, "imprinted", old.name(), neu.name()));
		hero.spendAndNext(1f);
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
