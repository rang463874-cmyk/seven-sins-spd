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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PrideGlory;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.PrideEffigy;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The Pride class's signature item. Doesn't take an equipment slot.
 * Spends dignity (stored in PrideGlory) to command the lesser beings of the dungeon.
 */
public class PrideCrown extends SinArtifact {

	/** growth copy: dignity cost (the base maximum) */
	public static final int COPY_COST = 10;

	public static final String AC_DECREE = "COMMAND";

	public static final int COST_COMMAND = 3;
	public static final int COST_DUEL = 5;
	public static final int COST_PROCESSION = 3;
	public static final int COST_DECREE = 4;

	{
		image = ItemSpriteSheet.PRIDE_CROWN;
		defaultAction = AC_DECREE;
		unique = true;
		bones = false;
		keptThoughLostInvent = true;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.remove(AC_DROP);
		actions.remove(AC_THROW);
		if (isEquipped(hero)) actions.add(AC_DECREE);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_DECREE)) return Messages.get(this, "ac_command");
		return super.actionName(action, hero);
	}

	static PrideGlory glory(Hero hero) {
		return Buff.affect(hero, PrideGlory.class);
	}

	public static int processionRange(Hero hero) {
		return 4 + hero.pointsInTalent(Talent.PRIDE_PROCESSION);
	}

	public static float commandDuration(Hero hero) {
		return 3f + hero.pointsInTalent(Talent.PRIDE_SCORN);
	}

	public static int maxVassals(Hero hero) {
		return 1 + hero.pointsInTalent(Talent.MONARCH_COURT);
	}

	public static float vassalThreshold(Hero hero) {
		return 0.5f + 0.1f * hero.pointsInTalent(Talent.MONARCH_DOMINION);
	}

	public static int vassalCount() {
		int n = 0;
		for (Mob m : Dungeon.level.mobs) if (m.buff(PrideGlory.Vassal.class) != null) n++;
		return n;
	}

	// ------------------------------------------------------------------ skills list

	@Override
	public String skillHeader(Hero hero) {
		PrideGlory g = glory(hero);
		return Messages.get(this, "skill_header", g.dignity, g.maxDignity());
	}

	@Override
	protected String levelEffects(Hero hero) {
		return Messages.get(SinArtifact.class, "effects_dignity", 2 * SinArtifact.bonusCharges(hero),
				Math.round((1f / SinArtifact.rechargeScale(hero) - 1f) * 100));
	}

	@Override
	public ArrayList<SinSkill> skills(final Hero hero) {
		ArrayList<SinSkill> list = new ArrayList<>();
		list.add(new BasicSkill(PrideCrown.class, "disdain", 28, SinSkill.CostType.DIGNITY).projectile()
				.cost(h -> COST_DISDAIN)
				.usableIf(h -> glory(h).dignity >= COST_DISDAIN)
				.descArgs(h -> new Object[]{disdainMin(h), disdainMax(h)})
				.onUse(h -> GameScene.selectCell(disdain)));
		list.add(new BasicSkill(PrideCrown.class, "repel", 30, SinSkill.CostType.DIGNITY)
				.cost(h -> COST_REPEL)
				.usableIf(h -> glory(h).dignity >= COST_REPEL)
				.descArgs(h -> new Object[]{REPEL_RADIUS, repelPower(h)})
				.onUse(PrideCrown::repel));
		list.add(new BasicSkill(PrideCrown.class, "command", 24, SinSkill.CostType.DIGNITY).targetsEnemy()
				.cost(h -> COST_COMMAND)
				.usableIf(h -> glory(h).dignity >= COST_COMMAND)
				.descArgs(h -> new Object[]{Math.round(commandDuration(h))})
				.onUse(h -> GameScene.selectCell(command)));
		list.add(new BasicSkill(PrideCrown.class, "duel", 25, SinSkill.CostType.DIGNITY).targetsEnemy()
				.cost(h -> COST_DUEL)
				.usableIf(h -> glory(h).dignity >= COST_DUEL)
				.onUse(h -> GameScene.selectCell(duel)));
		list.add(new BasicSkill(PrideCrown.class, "procession", 26, SinSkill.CostType.DIGNITY)
				.cost(h -> COST_PROCESSION)
				.usableIf(h -> glory(h).dignity >= COST_PROCESSION)
				.descArgs(h -> new Object[]{processionRange(h)})
				.onUse(h -> GameScene.selectCell(procession)));
		list.add(new BasicSkill(PrideCrown.class, "decree", 27, SinSkill.CostType.DIGNITY).targetsEnemy()
				.cost(h -> COST_DECREE)
				.usableIf(h -> glory(h).dignity >= COST_DECREE)
				.descArgs(h -> new Object[]{1 + h.pointsInTalent(Talent.PRIDE_DECREE)})
				.onUse(h -> GameScene.selectCell(decree)));
		list.add(new BasicSkill(PrideCrown.class, "effigy", 29, SinSkill.CostType.DIGNITY)
				.cost(h -> COST_EFFIGY)
				.visibleIf(h -> h.subClass == HeroSubClass.PARAGON)
				.usableIf(h -> glory(h).dignity >= COST_EFFIGY)
				.descArgs(h -> new Object[]{effigyHP(h), Math.round(EFFIGY_LIFE), PrideEffigy.TAUNT_RADIUS})
				.onUse(h -> GameScene.selectCell(effigy)));
		//growth copy: a royal grant (all of the base dignity)
		com.shatteredpixel.shatteredpixeldungeon.skills.GrowthCopy.add(list, PrideCrown.class, 3, com.shatteredpixel.shatteredpixeldungeon.skills.SinSkill.CostType.DIGNITY,
				h -> COPY_COST, h -> glory(h).dignity >= COPY_COST, h -> glory(h).useDignity(COPY_COST), true);
		return list;
	}

	@Override
	public void execute(final Hero hero, String action) {
		super.execute(hero, action);
		if (!canUseSkills(hero, action)) return;
		if (!action.equals(AC_DECREE)) return;
		final PrideGlory g = glory(hero);

		GameScene.show(new WndSkills(this, hero));
	}

	// ------------------------------------------------------------------ helpers

	private static Mob enemyAt(Integer cell, boolean allowBoss) {
		if (cell == null) return null;
		Char ch = Actor.findChar(cell);
		if (!(ch instanceof Mob) || ch.alignment == Char.Alignment.ALLY || !Dungeon.level.heroFOV[cell]) {
			GLog.w(Messages.get(PrideCrown.class, "no_target"));
			return null;
		}
		if (!allowBoss && PrideGlory.royalImmune(ch)) {
			GLog.w(Messages.get(PrideCrown.class, "defiant"));
			return null;
		}
		return (Mob) ch;
	}

	private static void crownFX(Hero hero, int pos) {
		hero.sprite.zap(pos);
		new com.shatteredpixel.shatteredpixeldungeon.effects.Flare(6, 24)
				.color(0xCC88FF, true).show(hero.sprite, 1.5f);
		Sample.INSTANCE.play(Assets.Sounds.READ);
	}

	// ------------------------------------------------------------------ skills

	// ------------------------------------------------------------------ repel (keeps enemies at bay)

	public static final int COST_REPEL = 2;
	public static final int REPEL_RADIUS = 2;

	public static int repelPower(Hero hero) {
		return 3;
	}

	/** "kneel back": every enemy within 2 tiles is thrown away from the hero and staggered */
	private static void repel(Hero hero) {
		PrideGlory g = glory(hero);
		if (!g.useDignity(COST_REPEL)) return;
		int pushed = 0;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (m.alignment == Char.Alignment.ALLY || !m.isAlive()) continue;
			if (Dungeon.level.distance(hero.pos, m.pos) > REPEL_RADIUS || !Dungeon.level.heroFOV[m.pos]) continue;
			if (Char.hasProp(m, Char.Property.IMMOVABLE)) continue;
			int dx = Integer.signum(Dungeon.level.cellToPoint(m.pos).x - Dungeon.level.cellToPoint(hero.pos).x);
			int dy = Integer.signum(Dungeon.level.cellToPoint(m.pos).y - Dungeon.level.cellToPoint(hero.pos).y);
			int far = m.pos + (dx + dy * Dungeon.level.width()) * (repelPower(hero) + 1);
			if (far < 0 || far >= Dungeon.level.length() || far == m.pos) continue;
			com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave.throwChar(m,
					new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(m.pos, far,
							com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.MAGIC_BOLT),
					repelPower(hero), true, true, PrideCrown.class);
			if (m.isAlive()) Buff.prolong(m, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo.class, 2f);
			pushed++;
		}
		com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave.BlastWave.blast(hero.pos);
		hero.sprite.operate(hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.8f, 1.2f);
		if (pushed == 0) GLog.i(Messages.get(PrideCrown.class, "repel_none"));
		hero.spendAndNext(1f);
	}

	// ------------------------------------------------------------------ disdain (ranged)

	public static final int COST_DISDAIN = 1;

	public static int disdainMin(Hero hero) {
		return 2 + hero.lvl / 3;
	}

	public static int disdainMax(Hero hero) {
		PrideGlory g = hero.buff(PrideGlory.class);
		return 5 + hero.lvl * 2 / 3 + (g == null ? 0 : g.meleeBonus());
	}

	/** disdain only gets half of dignity's damage bonus */
	public static float disdainMult(PrideGlory g) {
		return 1f + (g.dignityMult() - 1f) * 0.5f;
	}

	/** a contemptuous glance from afar: lets Pride fight without ever being touched */
	private final CellSelector.Listener disdain = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			if (cell == null) return;
			final Hero hero = Dungeon.hero;
			if (cell == hero.pos) return;
			final int target = new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(hero.pos, cell,
					com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.PROJECTILE).collisionPos;
			final Char ch = Actor.findChar(target);
			if (ch == null || ch == hero || ch.alignment == Char.Alignment.ALLY) {
				GLog.w(Messages.get(PrideCrown.class, "no_target"));
				return;
			}
			final PrideGlory g = glory(hero);
			if (!g.useDignity(COST_DISDAIN)) return;
			hero.sprite.zap(target);
			hero.busy();
			com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile.boltFromChar(hero.sprite.parent,
					com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile.SHAMAN_PURPLE, hero.sprite, target, () -> {
				com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton.target(ch);
				int dmg = Math.round(Random.NormalIntRange(disdainMin(hero), disdainMax(hero)) * disdainMult(g));
				ch.damage(dmg, PrideCrown.class);
				if (ch.sprite != null) ch.sprite.emitter().burst(Speck.factory(Speck.STAR), 3);
				Sample.INSTANCE.play(Assets.Sounds.HIT_MAGIC);
				com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility.dispel();
				hero.spendAndNext(1f);
			});
		}

		@Override
		public String prompt() {
			return Messages.get(PrideCrown.class, "prompt_disdain");
		}
	};

	private final CellSelector.Listener command = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero hero = Dungeon.hero;
			Mob mob = enemyAt(cell, false);
			if (mob == null) return;
			if (!glory(hero).useDignity(COST_COMMAND)) return;

			boolean vassal = hero.subClass == HeroSubClass.MONARCH
					&& mob.HP <= mob.HT * vassalThreshold(hero)
					&& vassalCount() < maxVassals(hero)
					&& !mob.isImmune(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corruption.class);
			if (vassal) {
				AllyBuff.affectAndLoot(mob, hero, PrideGlory.Vassal.class);
				mob.sprite.emitter().burst(Speck.factory(Speck.HEART), 4);
				GLog.p(Messages.get(PrideCrown.class, "vassal", mob.name()));
			} else {
				Buff.prolong(mob, Paralysis.class, commandDuration(hero));
				mob.sprite.emitter().burst(Speck.factory(Speck.STAR), 4);
			}
			crownFX(hero, mob.pos);
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(PrideCrown.class, "prompt_command");
		}
	};

	private final CellSelector.Listener duel = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero hero = Dungeon.hero;
			Mob mob = enemyAt(cell, true);
			if (mob == null) return;
			if (!glory(hero).useDignity(COST_DUEL)) return;

			Buff.prolong(hero, PrideGlory.Duel.class, 12f).object = mob.id();
			mob.aggro(hero);
			mob.state = mob.HUNTING;
			GLog.i(Messages.get(PrideCrown.class, "duel", mob.name()));
			crownFX(hero, mob.pos);
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(PrideCrown.class, "prompt_duel");
		}
	};

	private final CellSelector.Listener procession = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero hero = Dungeon.hero;
			if (cell == null || cell == hero.pos) return;
			boolean ok = Dungeon.level.heroFOV[cell]
					&& Dungeon.level.passable[cell]
					&& Actor.findChar(cell) == null
					&& Dungeon.level.distance(hero.pos, cell) <= processionRange(hero)
					&& new Ballistica(hero.pos, cell, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID).collisionPos.equals(cell);
			if (!ok) {
				GLog.w(Messages.get(PrideCrown.class, "bad_cell"));
				return;
			}
			if (!glory(hero).useDignity(COST_PROCESSION)) return;

			ScrollOfTeleportation.appear(hero, cell);
			Dungeon.level.occupyCell(hero);
			Dungeon.observe();
			GameScene.updateFog();
			hero.sprite.emitter().burst(Speck.factory(Speck.STAR), 6);
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(PrideCrown.class, "prompt_procession", processionRange(Dungeon.hero));
		}
	};

	// ------------------------------------------------------------------ effigy (paragon only)

	public static final int COST_EFFIGY = 3;
	public static final int EFFIGY_RANGE = 4;
	public static final float EFFIGY_LIFE = 20f;

	public static int effigyHP(Hero hero) {
		PrideGlory g = glory(hero);
		return 15 + 5 * hero.lvl + (g == null ? 0 : Math.round(g.glory) / 5);
	}

	/** a golden statue of the paragon that draws enemies away, so it can keep fighting alone from afar */
	private final CellSelector.Listener effigy = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero hero = Dungeon.hero;
			if (cell == null || cell == hero.pos) return;
			boolean ok = Dungeon.level.heroFOV[cell]
					&& Dungeon.level.passable[cell]
					&& Actor.findChar(cell) == null
					&& Dungeon.level.distance(hero.pos, cell) <= EFFIGY_RANGE;
			if (!ok) {
				GLog.w(Messages.get(PrideCrown.class, "bad_cell"));
				return;
			}
			if (!glory(hero).useDignity(COST_EFFIGY)) return;

			//only one effigy stands at a time
			for (PrideEffigy old : PrideEffigy.active()) old.die(null);

			PrideGlory g = glory(hero);
			PrideEffigy e = PrideEffigy.create(hero.lvl, g == null ? 0 : Math.round(g.glory), EFFIGY_LIFE);
			e.HT = e.HP = effigyHP(hero);
			e.pos = cell;
			GameScene.add(e);
			Dungeon.level.occupyCell(e);
			e.sprite.emitter().burst(Speck.factory(Speck.STAR), 8);
			com.watabou.noosa.audio.Sample.INSTANCE.play(com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.ROCKS);
			hero.sprite.zap(cell);
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(PrideCrown.class, "prompt_effigy", EFFIGY_RANGE);
		}
	};

	private final CellSelector.Listener decree = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero hero = Dungeon.hero;
			Mob mob = enemyAt(cell, true);
			if (mob == null) return;
			if (!glory(hero).useDignity(COST_DECREE)) return;

			PrideGlory.Decree d = Buff.affect(mob, PrideGlory.Decree.class);
			d.uses = Math.max(d.uses, 1 + hero.pointsInTalent(Talent.PRIDE_DECREE));
			crownFX(hero, mob.pos);
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(PrideCrown.class, "prompt_decree");
		}
	};

	// ------------------------------------------------------------------ info

	@Override
	public String status() {
		PrideGlory g = PrideGlory.of();
		return g == null ? null : g.dignity + "/" + g.maxDignity();
	}

	@Override
	public String desc() {
		String d = Messages.get(this, "desc");
		PrideGlory g = PrideGlory.of();
		if (g != null) d += "\n\n" + g.statsText();
		return d;
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
