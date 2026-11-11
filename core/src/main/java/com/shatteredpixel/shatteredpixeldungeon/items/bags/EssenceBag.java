package com.shatteredpixel.shatteredpixeldungeon.items.bags;

import com.shatteredpixel.shatteredpixeldungeon.items.EssenceItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * Seven Sins: holds essences (anything implementing {@link EssenceItem}: sin essences now, the
 * essences of Solomon's 72 demons later). Every class starts with one (HeroClass.initHero).
 */
public class EssenceBag extends Bag {

	{
		image = ItemSpriteSheet.ESSENCE_BAG;
	}

	public static final String AC_CODEX = "CODEX";

	@Override
	public java.util.ArrayList<String> actions(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero hero) {
		java.util.ArrayList<String> actions = super.actions(hero);
		actions.add(AC_CODEX);
		return actions;
	}

	@Override
	public String actionName(String action, com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero hero) {
		if (action.equals(AC_CODEX)) return com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(this, "ac_codex");
		return super.actionName(action, hero);
	}

	/** the Lesser Key: the run's codex of Solomon's demons */
	@Override
	public void execute(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_CODEX)) {
			com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.show(new com.shatteredpixel.shatteredpixeldungeon.windows.WndScrollMessage(
					new com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite(this),
					com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(this, "codex_title"),
					com.shatteredpixel.shatteredpixeldungeon.goetia.GoetiaHooks.codexText()));
		}
	}

	@Override
	public boolean canHold(Item item) {
		return item instanceof EssenceItem && super.canHold(item);
	}

	@Override
	public int capacity() {
		return 24;
	}

	@Override
	public int value() {
		return 40;
	}
}
