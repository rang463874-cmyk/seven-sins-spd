package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sins.AwakeningTalent;
import com.shatteredpixel.shatteredpixeldungeon.sins.SinAwakening;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;

/** The second awakening's talents: the path, points left, and each talent with a button to raise it. */
public class WndAwakening extends Window {

	private static final int WIDTH = 132;
	private static final int BTN_HEIGHT = 18;
	private static final int GAP = 2;

	public WndAwakening(final Hero hero) {
		super();
		RenderedTextBlock title = PixelScene.renderTextBlock(
				Messages.titleCase(SinAwakening.pathName(hero.heroClass, hero.awakening)), 9);
		title.hardlight(TITLE_COLOR);
		title.maxWidth(WIDTH);
		title.setPos(0, 0);
		add(title);
		float y = title.bottom() + GAP;

		RenderedTextBlock head = PixelScene.renderTextBlock(
				SinAwakening.pathDesc(hero.heroClass, hero.awakening) + "\n\n"
						+ Messages.get(WndAwakening.class, "points", hero.awakeningPoints), 6);
		head.maxWidth(WIDTH);
		head.setPos(0, y);
		add(head);
		y = head.bottom() + GAP + 2;

		for (final AwakeningTalent t : AwakeningTalent.forPath(hero.heroClass, hero.awakening)) {
			int p = AwakeningTalent.pts(hero, t);
			RedButton btn = new RedButton(t.title() + " (" + p + "/" + AwakeningTalent.MAX_POINTS + ")"
					+ (AwakeningTalent.canUpgrade(hero, t) ? "  +" : ""), 7) {
				@Override
				protected void onClick() {
					if (!AwakeningTalent.canUpgrade(hero, t)) return;
					AwakeningTalent.upgrade(hero, t);
					hide();
					GameScene.show(new WndAwakening(hero));
				}
			};
			btn.enable(AwakeningTalent.canUpgrade(hero, t));
			btn.setRect(0, y, WIDTH, BTN_HEIGHT);
			add(btn);
			y = btn.bottom() + 1;

			RenderedTextBlock desc = PixelScene.renderTextBlock(t.desc(), 6);
			desc.maxWidth(WIDTH - 4);
			desc.setPos(2, y);
			add(desc);
			y = desc.bottom() + GAP + 2;
		}
		resize(WIDTH, (int) y - GAP);
	}
}
