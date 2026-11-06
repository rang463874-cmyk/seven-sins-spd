package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.skills.SinSkill;
import com.shatteredpixel.shatteredpixeldungeon.skills.SkillHolder;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;

/** A signature item's skills, each with its cost and a short description. */
public class WndSkills extends Window {

	private static final int WIDTH = 132;
	private static final int BTN_HEIGHT = 18;
	private static final int GAP = 2;

	public WndSkills(Item item, final Hero hero) {
		super();
		SkillHolder holder = (SkillHolder) item;

		IconTitle title = new IconTitle(new ItemSprite(item), Messages.titleCase(item.name()));
		title.setRect(0, 0, WIDTH, 0);
		add(title);
		float y = title.bottom() + GAP;

		String header = holder.skillHeader(hero);
		if (header != null) {
			RenderedTextBlock head = PixelScene.renderTextBlock(header, 6);
			head.maxWidth(WIDTH);
			head.setPos(0, y);
			add(head);
			y = head.bottom() + GAP + 1;
		}

		for (final SinSkill skill : SkillHolder.visibleSkills(holder, hero)) {
			String label = skill.name(hero);
			if (skill.costType() != SinSkill.CostType.NONE) label += " (" + skill.costLabel(hero) + ")";
			RedButton btn = new RedButton(label, 7) {
				@Override
				protected void onClick() {
					hide();
					skill.use(hero);
				}
			};
			btn.icon(skill.iconImage());
			btn.enable(skill.usable(hero));
			btn.setRect(0, y, WIDTH, BTN_HEIGHT);
			add(btn);
			y = btn.bottom() + 1;

			RenderedTextBlock desc = PixelScene.renderTextBlock(skill.desc(hero), 6);
			desc.maxWidth(WIDTH - 4);
			desc.setPos(2, y);
			add(desc);
			y = desc.bottom() + GAP + 2;
		}

		resize(WIDTH, (int) y - GAP);
	}
}
