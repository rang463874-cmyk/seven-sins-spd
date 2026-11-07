package com.shatteredpixel.shatteredpixeldungeon.skills;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;

/**
 * One active skill of a signature item, shown both in the item's skill window (with its
 * description) and as an icon on the skill bar above the quickslots.
 *
 * To add a skill: create a subclass (usually anonymous) in the item's skills() list,
 * give it an icon index in interfaces/skill_icons.png, and add the item's
 * "skill_[id]" and "skill_[id]_desc" strings.
 */
public abstract class SinSkill {

	public enum CostType { NONE, CHARGES, HP, GAUGE, GOLD, SATIETY, RAGE, DIGNITY, TURNS, JEALOUSY, VARIES }

	public static final int ICON_SIZE = 16;
	private static TextureFilm film;

	public final String id;
	protected final Class<?> owner;
	public final int icon;

	public SinSkill(Class<?> owner, String id, int icon) {
		this.owner = owner;
		this.id = id;
		this.icon = icon;
	}

	public String name(Hero hero) {
		return Messages.get(owner, "skill_" + id);
	}

	public String desc(Hero hero) {
		return Messages.get(owner, "skill_" + id + "_desc", descArgs(hero));
	}

	/** values for the description's %d/%s placeholders */
	protected Object[] descArgs(Hero hero) {
		return new Object[0];
	}

	public abstract CostType costType();

	/** the cost in the units of costType(), ignored for NONE and VARIES */
	public int cost(Hero hero) {
		return 0;
	}

	/** short label for the skill bar ("" when free) */
	public String costShort(Hero hero) {
		switch (costType()) {
			case NONE: return "";
			case VARIES: return "?";
			default: return Integer.toString(cost(hero));
		}
	}

	/** full label for the skill window, e.g. "charges 2" */
	public String costLabel(Hero hero) {
		String key = "cost_" + costType().name().toLowerCase();
		return Messages.get(SinSkill.class, key, cost(hero));
	}

	/** whether the skill can be paid for and used right now */
	public abstract boolean usable(Hero hero);

	/** hidden skills (e.g. subclass-only ones) are left out of the window and the bar */
	public boolean visible(Hero hero) {
		return true;
	}

	/** starts the skill; may open a cell or item selector */
	public abstract void use(Hero hero);

	public Image iconImage() {
		Image img = new Image(Assets.Interfaces.SKILL_ICONS);
		if (film == null) film = new TextureFilm(img.texture, ICON_SIZE, ICON_SIZE);
		img.frame(film.get(icon));
		return img;
	}
}
