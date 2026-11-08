package com.shatteredpixel.shatteredpixeldungeon.items.elite;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.LiquidMetal;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.InventorySpell;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

/**
 * Casting mold (alchemy: 12 liquid metal). Pour it over a set of thrown weapons to cast one more of
 * them: the set grows by one weapon for good, so upgrading, repairing and picking the set back up all
 * work with the bigger size. No limit.
 */
public class CastingMold extends InventorySpell {

	public static final int METAL = 12;

	{
		image = ItemSpriteSheet.CASTING_MOLD;
	}

	@Override
	protected boolean usableOnItem(Item item) {
		return item instanceof MissileWeapon && !(item instanceof Dart) && !(item instanceof SpiritBow.SpiritArrow);
	}

	@Override
	protected void onItemSelected(Item item) {
		com.shatteredpixel.shatteredpixeldungeon.predator.PredatorHooks.onConsumed(curUser, this);
		MissileWeapon m = (MissileWeapon) item;
		m.bonusSetSize++;
		m.quantity(m.quantity() + 1);
		curUser.sprite.emitter().burst(Speck.factory(Speck.STAR), 6);
		Sample.INSTANCE.play(Assets.Sounds.EVOKE);
		GLog.p(Messages.get(this, "cast", m.name(), m.setSize()));
		updateQuickslot();
	}

	@Override
	public int value() {
		return 60 * quantity;
	}

	@Override
	public int energyVal() {
		return 3 * quantity;
	}

	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe.SimpleRecipe {
		{
			inputs = new Class[]{LiquidMetal.class};
			inQuantity = new int[]{METAL};
			cost = 2;
			output = CastingMold.class;
			outQuantity = 1;
		}
	}
}
