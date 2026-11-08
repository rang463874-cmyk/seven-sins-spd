package com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ConquestCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * Spears of conquest, the white horseman's relic (replaces the gun of conquest as the prophet's reward).
 * A set of 2 tier-4 thrown spears. They CONQUER unequipped weapons, thrown weapons, armor and rings:
 * each conquest refills the set's durability, reforges a lost spear, and adds permanent damage
 * (+1 min per 10 conquest, +1 max per 5); a good enchantment on a conquered weapon passes to the spears.
 * A casting mold only adds a spear every 3 molds.
 */
public class ConquestSpear extends MissileWeapon {

	public static final String AC_CONQUER = "CONQUER";
	public static final int MOLDS_PER_SPEAR = 3;

	{
		image = ItemSpriteSheet.CONQUEST_SPEAR;
		hitSound = Assets.Sounds.HIT_STAB;
		hitSoundPitch = 0.9f;
		tier = 4;
		baseUses = 6;
		bones = false;
	}

	/** conquest points from devoured gear: permanent bonus damage */
	public int conquest = 0;
	/** casting molds poured since the last extra spear */
	public int moldProgress = 0;

	@Override
	public int defaultQuantity() {
		return 2;
	}

	public int bonusMin() {
		return conquest / 10;
	}

	public int bonusMax() {
		return conquest / 5;
	}

	@Override
	public int min(int lvl) {
		return super.min(lvl) + bonusMin();
	}

	@Override
	public int max(int lvl) {
		return super.max(lvl) + bonusMax();
	}

	/** a casting mold: one more spear for every 3; returns true if the set grew */
	public boolean pourMold() {
		moldProgress++;
		if (moldProgress < MOLDS_PER_SPEAR) return false;
		moldProgress = 0;
		bonusSetSize++;
		quantity(quantity() + 1);
		return true;
	}

	// ------------------------------------------------------------------ conquering

	public static int conquestFor(Hero hero, Item item) {
		if (item instanceof ConquestSpear) return 0;
		return ConquestCrown.bulletsFor(hero, item);
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (hero.belongings.backpack.contains(this)) actions.add(AC_CONQUER);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_CONQUER)) return Messages.get(this, "ac_conquer");
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_CONQUER)) selectConquest(hero);
	}

	private void selectConquest(final Hero hero) {
		GameScene.selectItem(new WndBag.ItemSelector() {
			@Override
			public String textPrompt() {
				return Messages.get(ConquestSpear.class, "prompt");
			}

			@Override
			public boolean itemSelectable(Item item) {
				return conquestFor(hero, item) > 0;
			}

			@Override
			public void onSelect(Item item) {
				if (item == null) return;
				int pts = conquestFor(hero, item);
				if (pts <= 0) return;
				conquer(hero, item, pts);
			}
		});
	}

	private void conquer(Hero hero, Item item, int pts) {
		if (item instanceof Weapon && ((Weapon) item).hasGoodEnchant()) {
			enchant(((Weapon) item).enchantment);
			GLog.p(Messages.get(ConquestSpear.class, "enchanted", ((Weapon) item).enchantment.name()));
		}
		item.detachAll(hero.belongings.backpack);
		conquest += pts;
		//the conquered metal mends the spears and reforges a lost one
		durability = MAX_DURABILITY;
		boolean reforged = false;
		if (quantity() < setSize()) {
			quantity(quantity() + 1);
			reforged = true;
		}
		Item.updateQuickslot();
		hero.sprite.showStatus(CharSprite.POSITIVE, Messages.get(ConquestSpear.class, "conquered", pts));
		if (reforged) GLog.p(Messages.get(ConquestSpear.class, "reforged"));
		hero.sprite.operate(hero.pos);
		hero.sprite.emitter().burst(Speck.factory(Speck.STAR), 8);
		Sample.INSTANCE.play(Assets.Sounds.EVOKE);
		Invisibility.dispel();
		hero.spendAndNext(1f);
	}

	@Override
	public Item merge(Item other) {
		if (other instanceof ConquestSpear) {
			ConquestSpear o = (ConquestSpear) other;
			conquest = Math.max(conquest, o.conquest);
			moldProgress = Math.max(moldProgress, o.moldProgress);
		}
		return super.merge(other);
	}

	@Override
	public String info() {
		String info = super.info();
		info += "\n\n" + Messages.get(ConquestSpear.class, "stats", conquest, bonusMin(), bonusMax());
		if (moldProgress > 0) info += "\n" + Messages.get(ConquestSpear.class, "molds", moldProgress, MOLDS_PER_SPEAR);
		return info;
	}

	@Override
	public int value() {
		return 0;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("conquest", conquest);
		bundle.put("molds", moldProgress);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		conquest = bundle.getInt("conquest");
		moldProgress = bundle.getInt("molds");
	}
}
