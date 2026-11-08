package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Prophet;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ConquestCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DeathScythe;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.FamineScales;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WarSword;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.SealFragment;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ProphetSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ItemButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

/** The prophet's reward: pick one of the four horsemen artifacts. */
public class WndProphet extends Window {

	private static final int WIDTH    = 120;
	private static final int BTN_SIZE = 26;
	private static final int BTN_GAP  = 3;
	private static final int GAP      = 2;

	private final Prophet prophet;

	public WndProphet(final Prophet prophet) {
		super();
		this.prophet = prophet;

		IconTitle titlebar = new IconTitle();
		titlebar.icon(new ProphetSprite());
		titlebar.label(Messages.titleCase(prophet.name()));
		titlebar.setRect(0, 0, WIDTH, 0);
		add(titlebar);

		RenderedTextBlock message = PixelScene.renderTextBlock(Messages.get(this, "message"), 6);
		message.maxWidth(WIDTH);
		message.setPos(0, titlebar.bottom() + GAP);
		add(message);

		Item[] rewards = {new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ConquestSpear().identify(), new WarSword(), new FamineScales(), new DeathScythe()};
		float rowWidth = rewards.length * BTN_SIZE + (rewards.length - 1) * BTN_GAP;
		float x = (WIDTH - rowWidth) / 2f;
		float y = message.bottom() + BTN_GAP + 2;
		ItemButton last = null;
		for (Item reward : rewards) {
			reward.identify(false);
			ItemButton btn = new ItemButton() {
				@Override
				protected void onClick() {
					GameScene.show(new RewardWindow(item()));
				}
			};
			btn.item(reward);
			btn.setRect(x, y, BTN_SIZE, BTN_SIZE);
			add(btn);
			x += BTN_SIZE + BTN_GAP;
			last = btn;
		}

		resize(WIDTH, (int) last.bottom() + BTN_GAP);
	}

	private void selectReward(Item reward) {
		hide();
		if (reward == null) return;

		SealFragment fragments = Dungeon.hero.belongings.getItem(SealFragment.class);
		if (fragments == null || fragments.quantity() < Prophet.FRAGMENTS) return;
		if (fragments.quantity() == Prophet.FRAGMENTS) {
			fragments.detachAll(Dungeon.hero.belongings.backpack);
		} else {
			fragments.quantity(fragments.quantity() - Prophet.FRAGMENTS);
		}

		if (reward.doPickUp(Dungeon.hero)) {
			GLog.i(Messages.capitalize(Messages.get(Dungeon.hero, "you_now_have", reward.name())));
		} else {
			Dungeon.level.drop(reward, prophet.pos).sprite.drop();
		}
		prophet.yell(Messages.get(this, "farewell"));
		prophet.die(null);
		Prophet.Quest.complete();
	}

	private class RewardWindow extends WndInfoItem {

		public RewardWindow(Item item) {
			super(item);

			RedButton btnConfirm = new RedButton(Messages.get(WndProphet.class, "confirm")) {
				@Override
				protected void onClick() {
					RewardWindow.this.hide();
					WndProphet.this.selectReward(item);
				}
			};
			btnConfirm.setRect(0, height + 2, width / 2 - 1, 16);
			add(btnConfirm);

			RedButton btnCancel = new RedButton(Messages.get(WndProphet.class, "cancel")) {
				@Override
				protected void onClick() {
					RewardWindow.this.hide();
				}
			};
			btnCancel.setRect(btnConfirm.right() + 2, height + 2, btnConfirm.width(), 16);
			add(btnCancel);

			resize(width, (int) btnCancel.bottom());
		}
	}
}
