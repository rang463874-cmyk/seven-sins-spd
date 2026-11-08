package com.shatteredpixel.shatteredpixeldungeon.items.elite;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Adrenaline;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Drowsy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Blindweed;
import com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

/**
 * Stimulant (alchemy: swiftthistle seed + blindweed seed). Instant: 8 turns of haste and adrenaline,
 * and shakes off slow, cripple, weakness and drowsiness.
 */
public class Stimulant extends Item {

	public static final String AC_INJECT = "INJECT";

	{
		image = ItemSpriteSheet.STIMULANT;
		stackable = true;
		defaultAction = AC_INJECT;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_INJECT);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_INJECT)) return Messages.get(this, "ac_inject");
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (!action.equals(AC_INJECT)) return;
		detach(hero.belongings.backpack);
		com.shatteredpixel.shatteredpixeldungeon.predator.PredatorHooks.onConsumed(hero, this);
		Buff.detach(hero, Slow.class);
		Buff.detach(hero, Cripple.class);
		Buff.detach(hero, Weakness.class);
		Buff.detach(hero, Drowsy.class);
		Buff.prolong(hero, Haste.class, 8f);
		Buff.prolong(hero, Adrenaline.class, 8f);
		hero.sprite.emitter().burst(Speck.factory(Speck.UP), 4);
		Sample.INSTANCE.play(Assets.Sounds.DRINK, 1f, 1.5f);
		GLog.p(Messages.get(this, "used"));
		//instant: no turn spent
		hero.next();
	}

	@Override public boolean isUpgradable() { return false; }
	@Override public boolean isIdentified() { return true; }
	@Override public int value() { return 20 * quantity; }
	@Override public int energyVal() { return quantity; }

	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe.SimpleRecipe {
		{
			inputs = new Class[]{Swiftthistle.Seed.class, Blindweed.Seed.class};
			inQuantity = new int[]{1, 1};
			cost = 2;
			output = Stimulant.class;
			outQuantity = 1;
		}
	}
}
