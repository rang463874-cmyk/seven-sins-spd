package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * Seven Sins: the essence of one of Solomon's demons (one kind for all of them). Dropped by the demons;
 * offered at the hidden sigil of a boss floor to call that floor's demon king. Kept in the essence bag.
 */
public class DemonEssence extends Item implements EssenceItem {

	{
		image = ItemSpriteSheet.DEMON_ESSENCE;
		stackable = true;
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
		if (action.equals(AC_CODEX)) return com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(
				com.shatteredpixel.shatteredpixeldungeon.items.bags.EssenceBag.class, "ac_codex");
		return super.actionName(action, hero);
	}

	@Override
	public void execute(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_CODEX)) com.shatteredpixel.shatteredpixeldungeon.items.bags.EssenceBag.showCodex();
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
	public ItemSprite.Glowing glowing() {
		return new ItemSprite.Glowing(0x9020C0, 1.5f);
	}

	@Override
	public int value() {
		return 60 * quantity;
	}
}
