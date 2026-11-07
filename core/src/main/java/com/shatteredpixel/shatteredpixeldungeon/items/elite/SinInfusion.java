package com.shatteredpixel.shatteredpixeldungeon.items.elite;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.SinEssence;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.CurseInfusion;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.InventorySpell;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

/**
 * Sin infusion (alchemy: curse infusion + 2 sin essences). Like curse infusion it curses a melee weapon,
 * armor or wand (keeping or adding a curse), but its bonus stacks: every infusion adds +1 level, with no
 * limit. All stacks are lost if the curse is ever removed.
 */
public class SinInfusion extends InventorySpell {

	{
		image = ItemSpriteSheet.SIN_INFUSION;
	}

	@Override
	protected boolean usableOnItem(Item item) {
		return item instanceof MeleeWeapon || item instanceof Armor || item instanceof Wand;
	}

	public static String infoLine(int stacks) {
		return Messages.get(SinInfusion.class, "info_line", stacks);
	}

	@Override
	protected void onItemSelected(Item item) {
		CellEmitter.get(curUser.pos).burst(ShadowParticle.CURSE, 8);
		Sample.INSTANCE.play(Assets.Sounds.CURSED);

		item.cursed = true;
		item.cursedKnown = true;
		int stacks = 0;
		if (item instanceof Weapon) {
			Weapon w = (Weapon) item;
			if (w.enchantment == null || !w.enchantment.curse()) {
				w.enchant(w.enchantment == null ? Weapon.Enchantment.randomCurse()
						: Weapon.Enchantment.randomCurse(w.enchantment.getClass()));
			}
			stacks = ++w.sinInfusions;
			if (w instanceof MagesStaff) ((MagesStaff) w).updateWand(true);
		} else if (item instanceof Armor) {
			Armor a = (Armor) item;
			if (a.glyph == null || !a.glyph.curse()) {
				a.inscribe(a.glyph == null ? Armor.Glyph.randomCurse() : Armor.Glyph.randomCurse(a.glyph.getClass()));
			}
			stacks = ++a.sinInfusions;
		} else if (item instanceof Wand) {
			Wand wd = (Wand) item;
			stacks = ++wd.sinInfusions;
			wd.updateLevel();
		}
		GLog.n(Messages.get(this, "infused", item.name(), stacks));
		Badges.validateItemLevelAquired(item);
		updateQuickslot();
	}

	@Override
	public int value() {
		return 120 * quantity;
	}

	@Override
	public int energyVal() {
		return 8 * quantity;
	}

	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe.SimpleRecipe {
		{
			inputs = new Class[]{CurseInfusion.class, SinEssence.class};
			inQuantity = new int[]{1, 2};
			cost = 5;
			output = SinInfusion.class;
			outQuantity = 1;
		}
	}
}
