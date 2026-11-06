package com.shatteredpixel.shatteredpixeldungeon.skills;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

import java.util.ArrayList;

/** An item (a sin's signature item) whose active skills appear on the skill bar. */
public interface SkillHolder {

	/** every skill, in display order; skills hidden for this hero are filtered by the caller */
	ArrayList<SinSkill> skills(Hero hero);

	/** a line shown at the top of the skill window, such as remaining charges; may be null */
	String skillHeader(Hero hero);

	/** the first worn artifact/ring slot item that holds skills, or null (skills need it equipped) */
	static Item find(Hero hero) {
		if (hero == null || hero.belongings == null) return null;
		Item[] worn = {hero.belongings.artifact(), hero.belongings.misc(), hero.belongings.misc2()};
		for (Item item : worn) {
			if (item instanceof SkillHolder) return item;
		}
		return null;
	}

	static ArrayList<SinSkill> visibleSkills(SkillHolder holder, Hero hero) {
		ArrayList<SinSkill> out = new ArrayList<>();
		for (SinSkill s : holder.skills(hero)) {
			if (s.visible(hero)) out.add(s);
		}
		return out;
	}
}
