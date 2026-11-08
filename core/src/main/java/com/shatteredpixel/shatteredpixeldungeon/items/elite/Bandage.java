package com.shatteredpixel.shatteredpixeldungeon.items.elite;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

/**
 * Herbal bandage (alchemy: 2 sungrass seeds -> 2 bandages). Wrapping it takes 1 turn: heals 35% of max
 * HP over time and stops bleeding and crippling.
 */
public class Bandage extends Item {

	public static final String AC_APPLY = "APPLY";

	{
		image = ItemSpriteSheet.BANDAGE;
		stackable = true;
		defaultAction = AC_APPLY;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_APPLY);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_APPLY)) return Messages.get(this, "ac_apply");
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (!action.equals(AC_APPLY)) return;
		detach(hero.belongings.backpack);
		com.shatteredpixel.shatteredpixeldungeon.predator.PredatorHooks.onConsumed(hero, this);
		Buff.detach(hero, Bleeding.class);
		Buff.detach(hero, Cripple.class);
		Buff.affect(hero, Healing.class).setHeal(Math.round(hero.HT * 0.35f), 0.1f, 1);
		hero.sprite.emitter().burst(Speck.factory(Speck.HEALING), 4);
		Sample.INSTANCE.play(Assets.Sounds.UNLOCK, 0.6f, 1.4f);
		GLog.i(Messages.get(this, "applied"));
		hero.sprite.operate(hero.pos);
		hero.spendAndNext(1f);
	}

	@Override public boolean isUpgradable() { return false; }
	@Override public boolean isIdentified() { return true; }
	@Override public int value() { return 15 * quantity; }
	@Override public int energyVal() { return quantity; }

	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe.StrictRecipe {
		{
			inputs = new Class[]{Sungrass.Seed.class};
			inQuantity = new int[]{2};
			cost = 1;
			output = Bandage.class;
			outQuantity = 2;
		}
	}
}
