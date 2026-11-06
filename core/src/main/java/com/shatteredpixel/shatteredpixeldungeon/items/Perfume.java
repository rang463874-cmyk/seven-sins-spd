package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SinArtifact;
import com.shatteredpixel.shatteredpixeldungeon.skills.BasicSkill;
import com.shatteredpixel.shatteredpixeldungeon.skills.SinSkill;
import com.shatteredpixel.shatteredpixeldungeon.skills.SkillHolder;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndSkills;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LustDesire;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The Lust class's signature item. Doesn't take an equipment slot.
 * Its skills are paid for with the wearer's own blood (HP) instead of charges.
 */
public class Perfume extends SinArtifact {

	public static final String AC_SPRAY = "SPRAY";

	/** HP costs as a share of max HP */
	public static final float COST_KISS = 0.05f;
	public static final float COST_STEP = 0.03f;
	public static final float COST_RAPTURE = 0.10f;

	{
		image = ItemSpriteSheet.PERFUME;
		defaultAction = AC_SPRAY;
		unique = true;
		bones = false;
		keptThoughLostInvent = true;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.remove(AC_DROP);
		actions.remove(AC_THROW);
		if (isEquipped(hero)) actions.add(AC_SPRAY);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_SPRAY)) return Messages.get(this, "ac_spray");
		return super.actionName(action, hero);
	}

	static LustDesire desire(Hero hero) {
		return Buff.affect(hero, LustDesire.class);
	}

	public static int kissRange(Hero hero) {
		return 3 + hero.pointsInTalent(Talent.LUST_REFILL);
	}

	public static float stepTurns(Hero hero) {
		return 5f + 2f * hero.pointsInTalent(Talent.LUST_WAFT);
	}

	// ------------------------------------------------------------------ skills list

	@Override
	public String skillHeader(Hero hero) {
		return Messages.get(this, "skill_header", hero.HP, hero.HT);
	}

	@Override
	public ArrayList<SinSkill> skills(final Hero hero) {
		ArrayList<SinSkill> list = new ArrayList<>();
		list.add(new BasicSkill(Perfume.class, "kiss", 32, SinSkill.CostType.HP)
				.cost(h -> desire(h).hpCost(COST_KISS))
				.usableIf(h -> desire(h).canPay(desire(h).hpCost(COST_KISS)))
				.descArgs(h -> new Object[]{kissRange(h), Math.round(3f + 2f * h.pointsInTalent(Talent.LUST_EMBRACE))})
				.onUse(h -> GameScene.selectCell(bloodKiss)));
		list.add(new BasicSkill(Perfume.class, "step", 33, SinSkill.CostType.HP)
				.cost(h -> desire(h).hpCost(COST_STEP))
				.usableIf(h -> desire(h).canPay(desire(h).hpCost(COST_STEP)))
				.descArgs(h -> new Object[]{Math.round(stepTurns(h))})
				.onUse(h -> step(h, desire(h))));
		list.add(new BasicSkill(Perfume.class, "drain", 34, SinSkill.CostType.NONE)
				.onUse(h -> drain(h, desire(h))));
		list.add(new BasicSkill(Perfume.class, "rapture", 35, SinSkill.CostType.HP)
				.cost(h -> desire(h).hpCost(COST_RAPTURE))
				.usableIf(h -> desire(h).canPay(desire(h).hpCost(COST_RAPTURE)))
				.descArgs(h -> new Object[]{Math.round(LustDesire.Rapture.DURATION)})
				.onUse(h -> rapture(h, desire(h))));
		return list;
	}

	@Override
	public void execute(final Hero hero, String action) {
		super.execute(hero, action);
		if (!canUseSkills(hero, action)) return;
		if (!action.equals(AC_SPRAY)) return;

		GameScene.show(new WndSkills(this, hero));
	}

	// ------------------------------------------------------------------ skills

	private static void step(Hero hero, LustDesire d) {
		if (!d.pay(d.hpCost(COST_STEP))) {
			GLog.w(Messages.get(Perfume.class, "no_blood"));
			return;
		}
		Buff.prolong(hero, LustDesire.Sway.class, stepTurns(hero));
		hero.sprite.emitter().burst(Speck.factory(Speck.HEART), 3);
		Sample.INSTANCE.play(Assets.Sounds.CHARMS, 0.6f);
		hero.spendAndNext(1f);
	}

	private static void drain(Hero hero, LustDesire d) {
		ArrayList<Mob> victims = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8) {
			Char ch = Actor.findChar(hero.pos + n);
			if (ch instanceof Mob && ch.alignment == Char.Alignment.ENEMY && LustDesire.charmedByHero(ch)) {
				victims.add((Mob) ch);
			}
		}
		if (victims.isEmpty()) {
			GLog.w(Messages.get(Perfume.class, "no_charmed"));
			return;
		}
		float bonus = 1f + 0.25f * hero.pointsInTalent(Talent.LUST_THIRST);
		for (Mob m : victims) {
			Charm c = m.buff(Charm.class);
			float left = c == null ? 0 : c.cooldown();
			if (c != null) c.detach();
			int dmg = Math.round((Random.NormalIntRange(2, 4 + hero.lvl / 2) + 2f * left) * bonus);
			dmg = Math.min(dmg, m.HP);
			m.damage(dmg, Perfume.class);
			d.drain(dmg, 1f);
			if (m.sprite != null) m.sprite.emitter().burst(Speck.factory(Speck.HEALING), 3);
		}
		hero.sprite.operate(hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.DRINK);
		hero.spendAndNext(1f);
	}

	private static void rapture(Hero hero, LustDesire d) {
		if (!d.pay(d.hpCost(COST_RAPTURE))) {
			GLog.w(Messages.get(Perfume.class, "no_blood"));
			return;
		}
		Buff.prolong(hero, LustDesire.Rapture.class, LustDesire.Rapture.DURATION);
		Buff.prolong(hero, Haste.class, 4f);
		hero.sprite.emitter().burst(Speck.factory(Speck.HEART), 6);
		Sample.INSTANCE.play(Assets.Sounds.CHARMS);
		hero.spendAndNext(1f);
	}

	private final CellSelector.Listener bloodKiss = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			if (cell == null) return;
			Hero hero = Dungeon.hero;
			Char ch = Actor.findChar(cell);
			if (!(ch instanceof Mob) || ch.alignment != Char.Alignment.ENEMY || !Dungeon.level.heroFOV[cell]
					|| Dungeon.level.distance(hero.pos, cell) > kissRange(hero)) {
				GLog.w(Messages.get(Perfume.class, "no_target"));
				return;
			}
			Mob mob = (Mob) ch;

			//land on the free tile next to the target that is closest to the hero
			int land = -1;
			if (Dungeon.level.adjacent(hero.pos, mob.pos)) {
				land = hero.pos;
			} else {
				for (int n : PathFinder.NEIGHBOURS8) {
					int c = mob.pos + n;
					if (Dungeon.level.passable[c] && !Dungeon.level.avoid[c] && Actor.findChar(c) == null
							&& Dungeon.level.heroFOV[c]
							&& (land == -1 || Dungeon.level.distance(hero.pos, c) < Dungeon.level.distance(hero.pos, land))) {
						land = c;
					}
				}
			}
			if (land == -1) {
				GLog.w(Messages.get(Perfume.class, "no_room"));
				return;
			}
			LustDesire d = desire(hero);
			if (!d.pay(d.hpCost(COST_KISS))) {
				GLog.w(Messages.get(Perfume.class, "no_blood"));
				return;
			}

			if (land != hero.pos) {
				ScrollOfTeleportation.appear(hero, land);
				Dungeon.level.occupyCell(hero);
				Dungeon.observe();
				GameScene.updateFog();
			}
			hero.sprite.turnTo(hero.pos, mob.pos);
			hero.attack(mob, 1f, 0f, 1000f);
			if (mob.isAlive() && !LustDesire.charmImmune(mob)) {
				//charmed after the kiss lands, so the kiss itself doesn't shake it off
				LustDesire.charm(hero, mob, 3f + 2f * hero.pointsInTalent(Talent.LUST_EMBRACE));
			}
			if (mob.sprite != null) mob.sprite.emitter().burst(Speck.factory(Speck.HEART), 4);
			Sample.INSTANCE.play(Assets.Sounds.CHARMS);
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(Perfume.class, "prompt_kiss", kissRange(Dungeon.hero));
		}
	};

	// ------------------------------------------------------------------ info

	@Override
	public String desc() {
		String s = Messages.get(this, "desc");
		LustDesire d = LustDesire.of();
		if (d != null) s += "\n\n" + d.statsText();
		return s;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 0;
	}
}
