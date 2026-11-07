package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.skills.SinSkill;
import com.shatteredpixel.shatteredpixeldungeon.skills.SkillHolder;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;

/**
 * A row of skill buttons that sits right above the quickslot toolbar. It shows the skills of
 * the signature item the hero carries (see SkillHolder), and is hidden for heroes without one.
 * Tap a button to use the skill; long-press it to read its description.
 */
public class SkillBar extends Component {

	public static final int BTN_SIZE = 20;
	public static final int GAP = 1;

	private final ArrayList<SkillButton> buttons = new ArrayList<>();
	/** identity of the skill list currently shown, to rebuild only when it changes */
	private String shownKey = "";
	private float refreshTimer = 0;

	@Override
	protected void createChildren() {
		super.createChildren();
	}

	/** height the bar takes up above the toolbar (0 when hidden) */
	public float barHeight() {
		return buttons.isEmpty() ? 0 : BTN_SIZE + GAP;
	}

	/** width of the row of buttons (0 when hidden) */
	public float barWidth() {
		return buttons.isEmpty() ? 0 : buttons.size() * (BTN_SIZE + GAP) - GAP;
	}

	private String keyFor(Item item, ArrayList<SinSkill> skills) {
		StringBuilder sb = new StringBuilder(item == null ? "-" : item.getClass().getName());
		for (SinSkill s : skills) sb.append('|').append(s.id);
		return sb.toString();
	}

	public void refresh() {
		Hero hero = Dungeon.hero;
		Item item = SkillHolder.find(hero);
		ArrayList<SinSkill> skills = item == null ? new ArrayList<SinSkill>()
				: SkillHolder.visibleSkills((SkillHolder) item, hero);
		String key = keyFor(item, skills);
		if (key.equals(shownKey)) {
			for (SkillButton b : buttons) b.updateState();
			return;
		}
		shownKey = key;
		for (SkillButton b : buttons) {
			remove(b);
			b.destroy();
		}
		buttons.clear();
		for (SinSkill s : skills) {
			SkillButton b = new SkillButton(s);
			add(b);
			buttons.add(b);
		}
		layout();
		GameScene.layoutTags();
		GameScene.syncSkillBarPos();
	}

	@Override
	protected void layout() {
		float x = this.x;
		for (SkillButton b : buttons) {
			b.setRect(x, y, BTN_SIZE, BTN_SIZE);
			x += BTN_SIZE + GAP;
		}
		width = Math.max(0, x - this.x - GAP);
		height = barHeight();
	}

	@Override
	public void update() {
		super.update();
		refreshTimer -= com.watabou.noosa.Game.elapsed;
		if (refreshTimer <= 0) {
			refreshTimer = 0.25f;
			refresh();
		}
	}

	public static class SkillButton extends Button {

		private final SinSkill skill;
		private NinePatch bg;
		private Image icon;
		private BitmapText cost;

		public SkillButton(SinSkill skill) {
			super();
			this.skill = skill;
			icon = skill.iconImage();
			add(icon);
			cost = new BitmapText(PixelScene.pixelFont);
			add(cost);
			updateState();
		}

		@Override
		protected void createChildren() {
			super.createChildren();
			bg = Chrome.get(Chrome.Type.TOAST_TR);
			add(bg);
		}

		@Override
		protected void layout() {
			super.layout();
			bg.x = x;
			bg.y = y;
			bg.size(width, height);
			if (icon != null) {
				icon.x = x + (width - icon.width()) / 2f;
				icon.y = y + (height - icon.height()) / 2f;
				PixelScene.align(icon);
			}
			if (cost != null) {
				cost.x = x + width - cost.width() - 1;
				cost.y = y + height - cost.baseLine() - 1;
				PixelScene.align(cost);
			}
		}

		void updateState() {
			Hero hero = Dungeon.hero;
			if (hero == null) return;
			boolean ok = skill.usable(hero);
			icon.alpha(ok ? 1f : 0.35f);
			String c = skill.costShort(hero);
			if (!c.equals(cost.text())) {
				cost.text(c);
				cost.measure();
				layout();
			}
			cost.hardlight(ok ? 0xFFFFFF : 0xCC5555);
		}

		@Override
		protected void onClick() {
			Hero hero = Dungeon.hero;
			if (hero == null || !hero.isAlive()) return;
			if (!hero.ready || GameScene.interfaceBlockingHero()) return;
			if (!skill.usable(hero)) {
				GLog.w(Messages.get(SkillBar.class, "unusable", skill.name(hero)));
				return;
			}
			skill.use(hero);
		}

		@Override
		protected boolean onLongClick() {
			Hero hero = Dungeon.hero;
			if (hero == null) return true;
			String text = skill.desc(hero);
			if (skill.costType() != SinSkill.CostType.NONE) {
				text = Messages.get(SkillBar.class, "cost", skill.costLabel(hero)) + "\n\n" + text;
			}
			GameScene.show(new WndTitledMessage(skill.iconImage(), skill.name(hero), text));
			return true;
		}

		@Override
		protected String hoverText() {
			Hero hero = Dungeon.hero;
			return hero == null ? null : skill.name(hero);
		}
	}
}
