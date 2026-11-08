package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;


/**
 * The archangel's reward (dropped where it falls, every class): a one-use item that opens a third
 * shared artifact/ring slot ({@code Belongings.misc3}) for the rest of the run. Works whether or not
 * the mirror case was used; only one can be used per run.
 */
public class HeavenlyHalo extends Item {

	public static final String AC_OPEN = "OPEN";

	{
		image = ItemSpriteSheet.HEAVENLY_HALO;
		defaultAction = AC_OPEN;
		unique = true;
		bones = false;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_OPEN);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_OPEN)) return Messages.get(this, "ac_open");
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (!action.equals(AC_OPEN)) return;

		if (hero.belongings.extraSlot2) {
			GLog.w(Messages.get(this, "already"));
			return;
		}
		hero.belongings.extraSlot2 = true;
		detach(hero.belongings.backpack);
		hero.sprite.operate(hero.pos);
		hero.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 12);
		Sample.INSTANCE.play(Assets.Sounds.READ);
		GLog.p(Messages.get(this, "opened"));
		updateQuickslot();
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
