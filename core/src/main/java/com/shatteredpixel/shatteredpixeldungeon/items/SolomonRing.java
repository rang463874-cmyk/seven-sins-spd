package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SolomonArcana;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SinArtifact;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.skills.SinSkill;
import com.shatteredpixel.shatteredpixeldungeon.solomon.SolomonSpells;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndSkills;

import java.util.ArrayList;

/**
 * Seven Sins: the Ring of Solomon, Solomon's signature item (Nov 17 2026). Its skills are the four element
 * symbols, Cast (the spell of the drawn symbols) and Summon (a demon of this run's Lemegeton).
 */
public class SolomonRing extends SinArtifact {

	public static final String AC_INVOKE = "INVOKE";

	{
		image = ItemSpriteSheet.SOLOMON_RING;
		defaultAction = AC_INVOKE;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.remove(AC_DROP);
		actions.remove(AC_THROW);
		if (isEquipped(hero)) actions.add(AC_INVOKE);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_INVOKE)) return Messages.get(this, "ac_invoke");
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (!canUseSkills(hero, action)) return;
		if (action.equals(AC_INVOKE)) GameScene.show(new WndSkills(this, hero));
	}

	static SolomonArcana arcana(Hero hero) {
		return Buff.affect(hero, SolomonArcana.class);
	}

	@Override
	public String skillHeader(Hero hero) {
		SolomonArcana a = arcana(hero);
		return Messages.get(this, "skill_header", (int) a.mana, Math.round(a.maxMana()), SolomonSpells.circleText(a));
	}

	@Override
	protected String levelEffects(Hero hero) {
		return Messages.get(this, "effects", Math.round((1f / SinArtifact.rechargeScale(hero) - 1f) * 100),
				3 * SinArtifact.bonusCharges(hero));
	}

	@Override
	public ArrayList<SinSkill> skills(final Hero hero) {
		ArrayList<SinSkill> list = new ArrayList<>();
		for (final SolomonSpells.Element e : SolomonSpells.Element.values()) {
			list.add(new SymbolSkill(e));
		}
		list.add(new CastSkill());
		list.add(new SummonSkill());
		return list;
	}

	/** drawing a symbol: free and instant */
	public static class SymbolSkill extends SinSkill {
		final SolomonSpells.Element e;

		SymbolSkill(SolomonSpells.Element e) {
			super(SolomonRing.class, "el_" + e.name().toLowerCase(), e.icon());
			this.e = e;
		}

		@Override
		public String name(Hero hero) {
			return e.title();
		}

		@Override
		public String desc(Hero hero) {
			return Messages.get(SolomonRing.class, "symbol_desc", e.title());
		}

		@Override
		public CostType costType() {
			return CostType.NONE;
		}

		@Override
		public boolean usable(Hero hero) {
			return true;
		}

		@Override
		public void use(Hero hero) {
			SolomonSpells.draw(hero, e);
		}
	}

	public static class CastSkill extends SinSkill {
		CastSkill() {
			super(SolomonRing.class, "cast", 156);
		}

		private SolomonSpells.Spell spell(Hero hero) {
			return SolomonSpells.current(hero.buff(SolomonArcana.class));
		}

		@Override
		public String name(Hero hero) {
			SolomonSpells.Spell s = spell(hero);
			return s == null ? Messages.get(SolomonRing.class, "skill_cast") : s.title();
		}

		@Override
		public String desc(Hero hero) {
			SolomonSpells.Spell s = spell(hero);
			String list = Messages.get(SolomonRing.class, "skill_cast_desc");
			return s == null ? list : s.desc() + "\n\n" + list;
		}

		@Override
		public CostType costType() {
			return CostType.VARIES;
		}

		@Override
		public String costShort(Hero hero) {
			SolomonSpells.Spell s = spell(hero);
			return s == null ? "" : Integer.toString(SolomonSpells.cost(hero, s));
		}

		@Override
		public String costLabel(Hero hero) {
			SolomonSpells.Spell s = spell(hero);
			return Messages.get(SolomonRing.class, "cost_mana", s == null ? 0 : SolomonSpells.cost(hero, s));
		}

		@Override
		public boolean usable(Hero hero) {
			SolomonSpells.Spell s = spell(hero);
			SolomonArcana a = hero.buff(SolomonArcana.class);
			return s != null && a != null && a.mana >= SolomonSpells.cost(hero, s);
		}

		@Override
		public int targeting() {
			SolomonSpells.Spell s = Dungeon_hero_spell();
			return s == null ? TARGET_NONE : s.targeting;
		}

		private static SolomonSpells.Spell Dungeon_hero_spell() {
			Hero h = com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;
			return h == null ? null : SolomonSpells.current(h.buff(SolomonArcana.class));
		}

		@Override
		public void use(Hero hero) {
			SolomonSpells.cast(hero);
		}
	}

	public static class SummonSkill extends SinSkill {
		SummonSkill() {
			super(SolomonRing.class, "summon", 157);
		}

		@Override
		public String desc(Hero hero) {
			return Messages.get(SolomonRing.class, "skill_summon_desc", SolomonSpells.SUMMON_COST,
					Math.round(SolomonSpells.summonTurns(hero)), SolomonSpells.maxSummons(hero));
		}

		@Override
		public CostType costType() {
			return CostType.VARIES;
		}

		@Override
		public String costShort(Hero hero) {
			return Integer.toString(SolomonSpells.SUMMON_COST);
		}

		@Override
		public String costLabel(Hero hero) {
			return Messages.get(SolomonRing.class, "cost_mana", SolomonSpells.SUMMON_COST);
		}

		@Override
		public boolean usable(Hero hero) {
			SolomonArcana a = hero.buff(SolomonArcana.class);
			return a != null && a.mana >= SolomonSpells.SUMMON_COST;
		}

		@Override
		public void use(Hero hero) {
			SolomonSpells.summon(hero);
		}
	}
}
