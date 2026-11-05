package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PredatorStomach;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;

import java.util.ArrayList;

/** The Predator's signature tool: devours equipment to grant permanent stats. */
public class PredatorMaw extends Item {

	public static final String AC_DEVOUR = "DEVOUR";

	{
		image = ItemSpriteSheet.PREDATOR_MAW;
		defaultAction = AC_DEVOUR;
		unique = true;
		bones = false;
		keptThoughLostInvent = true;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.remove(AC_DROP);
		actions.remove(AC_THROW);
		actions.add(AC_DEVOUR);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_DEVOUR)) return Messages.get(this, "ac_devour");
		return super.actionName(action, hero);
	}

	@Override
	public void execute(final Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_DEVOUR)) {
			selectDevour(hero, devoursPerUse(), false);
		}
	}

	/** how many items one use can devour (the mirror's Golden Maw eats two) */
	public int devoursPerUse() {
		return 1;
	}

	private void selectDevour(final Hero hero, final int remaining, final boolean ateAlready) {
		GameScene.selectItem(new WndBag.ItemSelector() {
			@Override
			public String textPrompt() {
				return Messages.get(PredatorMaw.class, ateAlready ? "prompt_more" : "prompt");
			}

			@Override
			public boolean itemSelectable(Item item) {
				return PredatorStomach.category(hero, item) >= 0;
			}

			@Override
			public void onSelect(Item item) {
				if (item == null) {
					//stopping after the first bite still takes the turn
					if (ateAlready) hero.spendAndNext(1f);
					return;
				}
				PredatorStomach stomach = Buff.affect(hero, PredatorStomach.class);
				if (stomach.devour(hero, item)) {
					GLog.p(Messages.get(PredatorMaw.class, "devoured", item.name()));
					hero.sprite.operate(hero.pos);
					Invisibility.dispel();
					if (remaining > 1) {
						selectDevour(hero, remaining - 1, true);
					} else {
						hero.spendAndNext(1f);
					}
				} else {
					GLog.w(Messages.get(PredatorMaw.class, "cant"));
					if (ateAlready) hero.spendAndNext(1f);
				}
			}
		});
	}


	@Override
	public String desc() {
		String d = Messages.get(this, "desc");
		if (curUser != null || com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero != null) {
			Hero h = com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;
			PredatorStomach s = h == null ? null : h.buff(PredatorStomach.class);
			if (s != null) d += "\n\n" + s.statsText();
		}
		return d;
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
