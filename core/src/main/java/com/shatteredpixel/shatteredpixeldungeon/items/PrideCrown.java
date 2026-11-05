package com.shatteredpixel.shatteredpixeldungeon.items;

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

import java.util.ArrayList;

/**
 * The Pride class's signature item. Doesn't take an equipment slot.
 * Uses recharging charges (stored in PrideGlory) to command the lesser beings of the dungeon.
 */
public class PrideCrown extends Item {

	public static final String AC_DECREE = "COMMAND";

	public static final int COST_COMMAND = 1;
	public static final int COST_DUEL = 2;
	public static final int COST_PROCESSION = 1;
	public static final int COST_DECREE = 1;

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
		actions.add(AC_DECREE);
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

	@Override
	public void execute(final Hero hero, String action) {
		super.execute(hero, action);
		if (!action.equals(AC_DECREE)) return;
		final PrideGlory g = glory(hero);

		GameScene.show(new WndOptions(new ItemSprite(this), Messages.titleCase(name()),
				Messages.get(this, "prompt", g.charges, g.maxCharges()),
				Messages.get(this, "opt_command", COST_COMMAND),
				Messages.get(this, "opt_duel", COST_DUEL),
				Messages.get(this, "opt_procession", COST_PROCESSION),
				Messages.get(this, "opt_decree", COST_DECREE)) {
			@Override
			protected boolean enabled(int index) {
				int[] costs = {COST_COMMAND, COST_DUEL, COST_PROCESSION, COST_DECREE};
				return g.charges >= costs[index];
			}

			@Override
			protected void onSelect(int index) {
				switch (index) {
					case 0: GameScene.selectCell(command); break;
					case 1: GameScene.selectCell(duel); break;
					case 2: GameScene.selectCell(procession); break;
					case 3: GameScene.selectCell(decree); break;
				}
			}
		});
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

	private final CellSelector.Listener command = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero hero = Dungeon.hero;
			Mob mob = enemyAt(cell, false);
			if (mob == null) return;
			if (!glory(hero).useCharges(COST_COMMAND)) return;

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
			if (!glory(hero).useCharges(COST_DUEL)) return;

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
			if (!glory(hero).useCharges(COST_PROCESSION)) return;

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

	private final CellSelector.Listener decree = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero hero = Dungeon.hero;
			Mob mob = enemyAt(cell, true);
			if (mob == null) return;
			if (!glory(hero).useCharges(COST_DECREE)) return;

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
		return g == null ? null : g.charges + "/" + g.maxCharges();
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
