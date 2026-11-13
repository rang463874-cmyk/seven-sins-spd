package com.shatteredpixel.shatteredpixeldungeon.levels.traps;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.goetia.GoetiaDemon;
import com.shatteredpixel.shatteredpixeldungeon.goetia.GoetiaHooks;
import com.shatteredpixel.shatteredpixeldungeon.items.DemonEssence;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;

/**
 * Seven Sins: the hidden sigil of a boss floor (found by searching, like a hidden trap). Stepping on it
 * asks for demon essences; paid, it takes the hero to the floor's demon king. Never wears out.
 */
public class GoetiaSigil extends Trap {

	{
		color = VIOLET;
		shape = STARS;
		disarmedByActivation = false;
		canBeHidden = true;
		avoidsClosedSpaces = true;
	}

	@Override
	public void activate() {
		final Hero hero = Dungeon.hero;
		if (hero == null || hero.pos != pos) return;
		final GoetiaDemon lord = GoetiaDemon.lordOf(Dungeon.depth);
		if (lord == null) return;
		//in its hidden room from the start, but asleep until the floor's boss has fallen
		if (!GoetiaHooks.sigils.contains(Dungeon.depth)) {
			GLog.i(Messages.get(this, "dormant"));
			return;
		}
		if (GoetiaHooks.lordsBeaten.contains(Dungeon.depth)) {
			GLog.i(Messages.get(this, "spent"));
			return;
		}
		//traps go off on the actor thread: windows must be opened on the render thread (like NPC dialogs)
		com.watabou.noosa.Game.runOnRenderThread(new com.watabou.utils.Callback() {
			@Override
			public void call() {
				showOffer(hero, lord);
			}
		});
	}

	/** the sigil leads to the king's ritual chamber; the essences are offered there, on its four altars */
	private void showOffer(final Hero hero, final GoetiaDemon lord) {
		DemonEssence held = hero.belongings.getItem(DemonEssence.class);
		final int have = held == null ? 0 : held.quantity();
		String body = Messages.get(this, "offer", lord.rankName(), lord.title(), GoetiaHooks.RITUAL_ESSENCES, have);
		GameScene.show(new WndOptions(new ItemSprite(new DemonEssence()), Messages.titleCase(name()), body,
				Messages.get(this, "accept"), Messages.get(this, "leave")) {
			@Override
			protected void onSelect(int index) {
				if (index != 0) return;
				GoetiaHooks.travelToArena();
			}
		});
	}
}
