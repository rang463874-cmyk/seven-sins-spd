package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
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
	/** a second row above the main one: skills that don't come from the signature item (the demon lord's) */
	private final ArrayList<SkillButton> extras = new ArrayList<>();
	/** identity of the skill list currently shown, to rebuild only when it changes */
	private String shownKey = "";
	private float refreshTimer = 0;

	@Override
	protected void createChildren() {
		super.createChildren();
	}

	/** height the bar takes up above the toolbar (0 when hidden) */
	public float barHeight() {
		return rows() * (BTN_SIZE + GAP) + (circle != null ? CircleStrip.HEIGHT + GAP : 0);
	}

	/** Seven Sins (Nov 18 2026, owner): Solomon sees the symbols drawn in his circle above the element buttons */
	private CircleStrip circle = null;

	private static boolean showsCircle(Hero hero, Item item) {
		return item instanceof com.shatteredpixel.shatteredpixeldungeon.items.SolomonRing
				&& hero != null && hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SolomonArcana.class) != null;
	}

	private int rows() {
		return (buttons.isEmpty() ? 0 : 1) + (extras.isEmpty() ? 0 : 1);
	}

	private static float rowWidth(ArrayList<SkillButton> row) {
		return row.isEmpty() ? 0 : row.size() * (BTN_SIZE + GAP) - GAP;
	}

	/** width of the widest row of buttons (0 when hidden) */
	public float barWidth() {
		return Math.max(rowWidth(buttons), rowWidth(extras));
	}

	private static ArrayList<SinSkill> extraSkills(Hero hero) {
		ArrayList<SinSkill> out = new ArrayList<>();
		SinSkill demon = com.shatteredpixel.shatteredpixeldungeon.demons.DemonHooks.skill(hero);
		if (demon != null) out.add(demon);
		out.addAll(com.shatteredpixel.shatteredpixeldungeon.sins.UltimateSkills.skills(hero)); //Seven Sins: ultimates
		return out;
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
		ArrayList<SinSkill> extra = extraSkills(hero);
		String key = keyFor(item, skills) + "/" + keyFor(null, extra);
		if (key.equals(shownKey)) {
			for (SkillButton b : buttons) b.updateState();
			for (SkillButton b : extras) b.updateState();
			if (circle != null) circle.sync(hero);
			return;
		}
		shownKey = key;
		for (SkillButton b : buttons) {
			remove(b);
			b.destroy();
		}
		buttons.clear();
		for (SkillButton b : extras) {
			remove(b);
			b.destroy();
		}
		extras.clear();
		if (circle != null) {
			remove(circle);
			circle.destroy();
			circle = null;
		}
		if (!skills.isEmpty() && showsCircle(hero, item)) {
			circle = new CircleStrip();
			add(circle);
			circle.sync(hero);
		}
		for (SinSkill s : skills) {
			SkillButton b = new SkillButton(s);
			add(b);
			buttons.add(b);
		}
		for (SinSkill s : extra) {
			SkillButton b = new SkillButton(s);
			add(b);
			extras.add(b);
		}
		layout();
		GameScene.layoutTags();
		GameScene.syncSkillBarPos();
	}

	@Override
	protected void layout() {
		width = barWidth();
		height = barHeight();
		//rows are right-aligned; the extra row sits on top
		float rowY = y;
		if (!extras.isEmpty()) {
			float x = this.x + width - rowWidth(extras);
			for (SkillButton b : extras) {
				b.setRect(x, rowY, BTN_SIZE, BTN_SIZE);
				x += BTN_SIZE + GAP;
			}
			rowY += BTN_SIZE + GAP;
		}
		float x = this.x + width - rowWidth(buttons);
		if (circle != null) {
			circle.setPos(x, rowY);
			rowY += CircleStrip.HEIGHT + GAP;
		}
		for (SkillButton b : buttons) {
			b.setRect(x, rowY, BTN_SIZE, BTN_SIZE);
			x += BTN_SIZE + GAP;
		}
	}

	@Override
	public void update() {
		super.update();
		updateAiming();
		refreshTimer -= com.watabou.noosa.Game.elapsed;
		if (refreshTimer <= 0) {
			refreshTimer = 0.25f;
			refresh();
		}
	}

	// ------------------------------------------------------------------ auto-aim

	private static SkillButton aiming = null;
	private static Char aimTarget = null;
	private static Image aimMark = null;
	private static Image aimButtonMark = null;

	/** the last target if it's still fair game, otherwise the nearest visible enemy */
	public static Char autoTarget() {
		Hero hero = Dungeon.hero;
		if (hero == null) return null;
		Char last = QuickSlotButton.lastTarget;
		if (validTarget(last)) return last;
		Char best = null;
		for (Char ch : Actor.chars()) {
			if (!validTarget(ch)) continue;
			if (best == null || Dungeon.level.distance(hero.pos, ch.pos) < Dungeon.level.distance(hero.pos, best.pos)) {
				best = ch;
			}
		}
		return best;
	}

	private static boolean validTarget(Char ch) {
		return ch != null && ch != Dungeon.hero && Actor.chars().contains(ch) && ch.isAlive()
				&& ch.alignment == Char.Alignment.ENEMY && Dungeon.level.heroFOV[ch.pos]
				&& !Char.hasProp(ch, Char.Property.OBJECT) && ch.sprite != null;
	}

	private static void startAiming(SkillButton b) {
		Char t = autoTarget();
		if (t == null) return;
		aiming = b;
		aimTarget = t;
		//like a thrown weapon's quickslot: remember the target and show its health bar
		QuickSlotButton.target(t);
		//fresh images each time: a killed Gizmo never draws again once re-added
		removeMarks();
		aimMark = Icons.TARGET.get();
		aimButtonMark = Icons.TARGET.get();
		if (t.sprite.parent != null) {
			t.sprite.parent.addToFront(aimMark);
			aimMark.point(t.sprite.center(aimMark));
		}
		if (b.parent != null) {
			b.parent.addToFront(aimButtonMark);
			aimButtonMark.x = b.left() + (b.width() - aimButtonMark.width()) / 2f;
			aimButtonMark.y = b.top() + (b.height() - aimButtonMark.height()) / 2f;
		}
	}

	private static void removeMarks() {
		if (aimMark != null) {
			aimMark.remove();
			aimMark.destroy();
			aimMark = null;
		}
		if (aimButtonMark != null) {
			aimButtonMark.remove();
			aimButtonMark.destroy();
			aimButtonMark = null;
		}
	}

	public static void stopAiming() {
		aiming = null;
		aimTarget = null;
		removeMarks();
	}

	private static void updateAiming() {
		if (aiming == null) return;
		if (!GameScene.isSelectingCell() || !validTarget(aimTarget)) {
			stopAiming();
			return;
		}
		if (aimMark != null && aimTarget.sprite != null) {
			//the target sprite can move between layers (e.g. after a level redraw): follow it
			if (aimMark.parent != aimTarget.sprite.parent && aimTarget.sprite.parent != null) {
				aimMark.remove();
				aimTarget.sprite.parent.addToFront(aimMark);
			}
			aimMark.point(aimTarget.sprite.center(aimMark));
		}
	}

	/** three small slots showing the circle's symbols in drawing order (first = form, middle = core, last = family) */
	public static class CircleStrip extends Component {

		public static final int SLOT = 9;
		public static final int HEIGHT = SLOT + 2;
		private static com.watabou.noosa.TextureFilm film;

		private NinePatch bg;
		private final Image[] icons = new Image[com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SolomonArcana.MAX_SYMBOLS];
		private final com.watabou.noosa.ColorBlock[] empty = new com.watabou.noosa.ColorBlock[icons.length];
		private String shown = null;

		@Override
		protected void createChildren() {
			super.createChildren();
			bg = Chrome.get(Chrome.Type.TOAST_TR);
			add(bg);
			for (int i = 0; i < icons.length; i++) {
				empty[i] = new com.watabou.noosa.ColorBlock(3, 3, 0x66CC99FF);
				add(empty[i]);
				icons[i] = new Image(com.shatteredpixel.shatteredpixeldungeon.Assets.Interfaces.SKILL_ICONS);
				if (film == null) film = new com.watabou.noosa.TextureFilm(icons[i].texture, SinSkill.ICON_SIZE, SinSkill.ICON_SIZE);
				icons[i].scale.set(0.5f);
				icons[i].visible = false;
				add(icons[i]);
			}
			width = icons.length * SLOT + 2;
			height = HEIGHT;
		}

		void sync(Hero hero) {
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SolomonArcana a =
					hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SolomonArcana.class);
			String key = a == null ? "" : a.circle.toString();
			if (key.equals(shown)) return;
			shown = key;
			for (int i = 0; i < icons.length; i++) {
				boolean on = a != null && i < a.circle.size();
				icons[i].visible = on;
				empty[i].visible = !on;
				if (on) icons[i].frame(film.get(a.circle.get(i).icon()));
			}
			layout();
		}

		@Override
		protected void layout() {
			bg.x = x;
			bg.y = y;
			bg.size(width, height);
			for (int i = 0; i < icons.length; i++) {
				float sx = x + 1 + i * SLOT;
				icons[i].x = sx + (SLOT - icons[i].width()) / 2f;
				icons[i].y = y + (height - icons[i].height()) / 2f;
				PixelScene.align(icons[i]);
				empty[i].x = sx + (SLOT - 3) / 2f;
				empty[i].y = y + (height - 3) / 2f;
				PixelScene.align(empty[i]);
			}
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
			//second tap while aiming: fire at the auto-picked target, like a thrown weapon's quickslot
			if (aiming == this && aimTarget != null && GameScene.isSelectingCell()) {
				Char t = aimTarget;
				int cell = skill.targeting() == SinSkill.TARGET_PROJECTILE ? QuickSlotButton.autoAim(t) : t.pos;
				if (cell == -1) cell = t.pos;
				stopAiming();
				GameScene.handleCell(cell);
				return;
			}
			if (!hero.ready || GameScene.interfaceBlockingHero()) return;
			if (!skill.usable(hero)) {
				GLog.w(Messages.get(SkillBar.class, "unusable", skill.name(hero)));
				return;
			}
			stopAiming();
			skill.use(hero);
			if (skill.targeting() != SinSkill.TARGET_NONE && GameScene.isSelectingCell()) startAiming(this);
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
