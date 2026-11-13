package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.mod;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.DemonEssence;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.sins.SinWeapons;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * Seven Sins: tier 6 weapons. Not found in the dungeon: forged in the alchemy pot from a tier 5 melee weapon and
 * demon essences (Recipe, keeps level/enchantment). Scrolls of upgrade don't work on them; they grow by feeding
 * demon essences instead (AC_INFUSE: 2 + level essences per level).
 */
public abstract class ApocalypseWeapon extends ModMeleeWeapon {

	public static final String AC_INFUSE = "INFUSE";
	public static final int RECIPE_ESSENCES = 2, RECIPE_ENERGY = 10;

	{
		tier = 6;
	}

	public int infuseCost() {
		return 2 + level();
	}

	@Override
	public boolean isUpgradable() {
		return false; //demon essences only
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_INFUSE);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_INFUSE)) return Messages.get(ApocalypseWeapon.class, "ac_infuse", infuseCost());
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (!action.equals(AC_INFUSE)) return;
		int cost = infuseCost();
		DemonEssence ess = hero.belongings.getItem(DemonEssence.class);
		int have = ess == null ? 0 : ess.quantity();
		if (have < cost) {
			GLog.w(Messages.get(ApocalypseWeapon.class, "need_essence", cost, have));
			return;
		}
		for (int i = 0; i < cost; i++) {
			DemonEssence e = hero.belongings.getItem(DemonEssence.class);
			if (e != null) e.detach(hero.belongings.backpack);
		}
		upgrade();
		levelKnown = true;
		hero.sprite.operate(hero.pos);
		hero.sprite.emitter().burst(Speck.factory(Speck.UP), 6);
		Sample.INSTANCE.play(Assets.Sounds.EVOKE);
		GLog.p(Messages.get(ApocalypseWeapon.class, "infused", name()));
		Item.updateQuickslot();
		hero.spendAndNext(1f);
	}

	@Override
	public String info() {
		return super.info() + "\n\n" + Messages.get(ApocalypseWeapon.class, "infuse_info", infuseCost());
	}

	@Override
	public ItemSprite.Glowing glowing() {
		ItemSprite.Glowing g = super.glowing();
		return g != null ? g : new ItemSprite.Glowing(0x660022, 3f);
	}

	@SuppressWarnings("unchecked")
	public static final Class<? extends ApocalypseWeapon>[] ALL = new Class[]{
			DoomBlade.class, VoidScythe.class, SunJudgment.class, WorldsEnd.class};

	/** which tier 6 weapon a tier 5 weapon becomes (by kind), else a random one */
	public static Class<? extends ApocalypseWeapon> forgedFrom(MeleeWeapon w) {
		if (w instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword || w instanceof GiantSlayer) return DoomBlade.class;
		if (w instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WarScythe || w instanceof AbyssScythe) return VoidScythe.class;
		if (w instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Glaive || w instanceof HeavenlySpear) return SunJudgment.class;
		if (w instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WarHammer || w instanceof RuinHammer) return WorldsEnd.class;
		return null;
	}

	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe {

		private static MeleeWeapon weapon(ArrayList<Item> in) {
			MeleeWeapon w = null;
			for (Item i : in) if (i instanceof MeleeWeapon) { if (w != null) return null; w = (MeleeWeapon) i; }
			return w;
		}

		@Override
		public boolean testIngredients(ArrayList<Item> in) {
			MeleeWeapon w = weapon(in);
			if (w == null || w.tier != 5 || w instanceof ApocalypseWeapon || !w.isIdentified() || w.cursed) return false;
			int ess = 0;
			for (Item i : in) {
				if (i instanceof DemonEssence) ess += i.quantity();
				else if (i != w) return false;
			}
			return ess >= RECIPE_ESSENCES;
		}

		@Override
		public int cost(ArrayList<Item> in) {
			return RECIPE_ENERGY;
		}

		@Override
		public Item brew(ArrayList<Item> in) {
			if (!testIngredients(in)) return null;
			MeleeWeapon old = weapon(in);
			Item out = sampleOutput(in);
			int need = RECIPE_ESSENCES;
			for (Item i : in) {
				if (i instanceof DemonEssence && need > 0) {
					int take = Math.min(need, i.quantity());
					i.quantity(i.quantity() - take);
					need -= take;
				}
			}
			old.quantity(0);
			return out;
		}

		@Override
		public Item sampleOutput(ArrayList<Item> in) {
			MeleeWeapon old = weapon(in);
			Class<? extends ApocalypseWeapon> c = old == null ? null : forgedFrom(old);
			if (c == null) c = old == null ? DoomBlade.class : ALL[Math.abs(old.getClass().getName().hashCode()) % ALL.length];
			ApocalypseWeapon neu = Reflection.newInstance(c);
			if (old != null) {
				neu.level(old.trueLevel());
				neu.enchant(old.enchantment);
				neu.enchantHardened = old.enchantHardened;
				neu.augment = old.augment;
				neu.sinSigil = old.sinSigil && !isTier5Sin(old);
				neu.warEdge = old.warEdge;
				neu.identify();
			}
			return neu;
		}

		private static boolean isTier5Sin(MeleeWeapon w) {
			for (com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass c
					: com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.values()) {
				Class<?> t5 = SinWeapons.tier5(c);
				if (t5 != null && t5.isInstance(w)) return true;
			}
			return false;
		}
	}

	protected static boolean roll(float chance) {
		return Random.Float() < chance;
	}
}
