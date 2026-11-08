package com.shatteredpixel.shatteredpixeldungeon.demons;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DemonPerk;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.DemonSeal;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.skills.SinSkill;

/**
 * The demon lord's skill, granted after its trial. It sits in its own row above the skill bar (every class
 * gets it, with or without a signature item) and is paid with the boon's charge (100, +1 per turn,
 * +2 once the lord is defeated). The effects live in DemonSeal (the old item form, kept for saves).
 */
public class DemonSkill extends SinSkill {

	public static final int ICON_BASE = 64;

	private final DemonLord lord;

	public DemonSkill(DemonLord lord) {
		super(DemonSkill.class, "demon_" + lord.key(), ICON_BASE + lord.ordinal());
		this.lord = lord;
	}

	private static DemonPerk perk(Hero hero) {
		return hero == null ? null : hero.buff(DemonPerk.class);
	}

	@Override
	public String name(Hero hero) {
		return lord.get("skill_name");
	}

	@Override
	public String desc(Hero hero) {
		DemonPerk p = perk(hero);
		return Messages.get(DemonSkill.class, "desc", lord.title(), lord.get("skill_desc"),
				p == null ? 0 : (int) p.charge);
	}

	@Override
	public CostType costType() {
		return CostType.VARIES;
	}

	@Override
	public String costShort(Hero hero) {
		DemonPerk p = perk(hero);
		if (p == null || p.ready()) return "";
		return Integer.toString((int) p.charge);
	}

	@Override
	public String costLabel(Hero hero) {
		return Messages.get(DemonSkill.class, "cost");
	}

	@Override
	public boolean usable(Hero hero) {
		DemonPerk p = perk(hero);
		return p != null && p.ready() && DemonHooks.stage >= DemonHooks.GRANTED;
	}

	@Override
	public int targeting() {
		return DemonSeal.targeted(lord) ? TARGET_ENEMY : TARGET_NONE;
	}

	@Override
	public void use(Hero hero) {
		DemonSeal.invokeSkill(hero);
	}
}
