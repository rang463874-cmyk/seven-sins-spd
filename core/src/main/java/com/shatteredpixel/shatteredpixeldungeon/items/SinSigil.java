package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
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
 * Given by the blacksmith for 2000 favor. Pressed into any melee weapon like the sword of war, it gives that weapon
 * the bearer's tier 5 sin weapon effect (and counts as that weapon for the sin's passives); it can be taken back off
 * and moved to another weapon. (It used to reforge the weapon into the tier 5 sin weapon.)
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
		curItem = this;
		GameScene.selectItem(new WndBag.ItemSelector() {
			@Override
			public String textPrompt() {
				return Messages.get(SinSigil.class, "prompt");
			}

			@Override
			public Class<? extends Bag> preferredBag() {
				return Belongings.Backpack.class;
			}

			@Override
			public boolean itemSelectable(Item item) {
				return canHold(item);
			}

			@Override
			public void onSelect(Item item) {
				if (item instanceof MeleeWeapon && canHold(item) && curItem instanceof SinSigil) {
					((SinSigil) curItem).affixTo(hero, (MeleeWeapon) item);
				}
			}
		});
	}

	/** any melee weapon without a sigil, except the tier 5 sin weapons (they already have the effect) */
	public static boolean canHold(Item item) {
		if (!(item instanceof MeleeWeapon) || ((MeleeWeapon) item).sinSigil) return false;
		for (com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass c
				: com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.values()) {
			Class<?> t5 = SinWeapons.tier5(c);
			if (t5 != null && t5.isInstance(item)) return false;
		}
		return true;
	}

	public void affixTo(Hero hero, Weapon weapon) {
		if (!weapon.cursedKnown) {
			GLog.w(Messages.get(SinSigil.class, "unknown_weapon"));
			return;
		}
		detach(hero.belongings.backpack);
		weapon.sinSigil = true;
		Item.updateQuickslot();
		hero.sprite.operate(hero.pos);
		hero.sprite.emitter().burst(Speck.factory(Speck.STAR), 10);
		Sample.INSTANCE.play(Assets.Sounds.EVOKE);
		GLog.p(Messages.get(SinSigil.class, "affix", weapon.name()));
		hero.spendAndNext(1f);
	}

	/** MeleeWeapon's DETACH_SIGIL action */
	public static void detachFrom(Hero hero, Weapon weapon) {
		weapon.sinSigil = false;
		SinSigil sigil = new SinSigil();
		GLog.i(Messages.get(SinSigil.class, "detach"));
		hero.sprite.operate(hero.pos);
		if (!sigil.collect()) {
			Dungeon.level.drop(sigil, hero.pos).sprite.drop();
		}
		Item.updateQuickslot();
		hero.spendAndNext(1f);
	}

	/** the line a weapon with the sigil shows: the current hero's tier 5 effect */
	public static String weaponLine() {
		Class<? extends MeleeWeapon> t5 = Dungeon.hero == null ? null : SinWeapons.tier5(Dungeon.hero.heroClass);
		if (t5 == null) return Messages.get(SinSigil.class, "attached_none");
		return Messages.get(SinSigil.class, "attached", Messages.get(t5, "effect"));
	}

	@Override
	public String info() {
		String s = super.info();
		Class<? extends MeleeWeapon> t5 = Dungeon.hero == null ? null : SinWeapons.tier5(Dungeon.hero.heroClass);
		if (t5 != null) s += "\n\n" + Messages.get(SinSigil.class, "your_effect", Messages.get(t5, "effect"));
		return s;
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
