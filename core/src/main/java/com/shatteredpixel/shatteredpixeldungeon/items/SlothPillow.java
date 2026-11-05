package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SlothRepose;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The Sloth class's signature item. Doesn't take an equipment slot.
 * Uses recharging charges (stored in SlothRepose) to rest, shoo enemies away and lull them to sleep.
 */
public class SlothPillow extends Item {

	public static final String AC_LAZE = "LAZE";

	public static final int COST_NAP = 1;
	public static final int COST_SHOO = 1;
	public static final int COST_YAWN = 2;
	public static final int COST_ROLL = 1;

	{
		image = ItemSpriteSheet.SLOTH_PILLOW;
		defaultAction = AC_LAZE;
		unique = true;
		bones = false;
		keptThoughLostInvent = true;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.remove(AC_DROP);
		actions.remove(AC_THROW);
		actions.add(AC_LAZE);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_LAZE)) return Messages.get(this, "ac_laze");
		return super.actionName(action, hero);
	}

	static SlothRepose repose(Hero hero) {
		return Buff.affect(hero, SlothRepose.class);
	}

	public static int rollRange(Hero hero) {
		return 3 + hero.pointsInTalent(Talent.SLOTH_ROLL);
	}

	public static int yawnRadius(Hero hero) {
		return 2 + hero.pointsInTalent(Talent.SLOTH_YAWN);
	}

	@Override
	public void execute(final Hero hero, String action) {
		super.execute(hero, action);
		if (!action.equals(AC_LAZE)) return;
		final SlothRepose r = repose(hero);

		GameScene.show(new WndOptions(new ItemSprite(this), Messages.titleCase(name()),
				Messages.get(this, "prompt", r.charges, r.maxCharges()),
				Messages.get(this, "opt_nap", COST_NAP),
				Messages.get(this, "opt_shoo", COST_SHOO),
				Messages.get(this, "opt_yawn", COST_YAWN),
				Messages.get(this, "opt_roll", COST_ROLL)) {
			@Override
			protected boolean enabled(int index) {
				int[] costs = {COST_NAP, COST_SHOO, COST_YAWN, COST_ROLL};
				return r.charges >= costs[index];
			}

			@Override
			protected void onSelect(int index) {
				switch (index) {
					case 0: nap(hero, r); break;
					case 1: shoo(hero, r); break;
					case 2: yawn(hero, r); break;
					case 3: GameScene.selectCell(roll); break;
				}
			}
		});
	}

	// ------------------------------------------------------------------ skills

	private static void nap(Hero hero, SlothRepose r) {
		if (!r.useCharges(COST_NAP)) return;
		int heal = Math.min(hero.HT - hero.HP, Math.round(hero.HT * 0.10f));
		if (heal > 0) {
			hero.HP += heal;
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
		}
		r.addTorpor(10);

		int snooze = hero.pointsInTalent(Talent.SLOTH_SNOOZE);
		for (Buff b : hero.buffs()) {
			if (snooze <= 0) break;
			if (b.type == Buff.buffType.NEGATIVE && !(b instanceof Hunger)) {
				b.detach();
				snooze--;
			}
		}

		hero.sprite.emitter().burst(Speck.factory(Speck.NOTE), 3);
		Sample.INSTANCE.play(Assets.Sounds.LULLABY, 0.6f);
		hero.spendAndNext(1f);
	}

	private static void shoo(Hero hero, SlothRepose r) {
		ArrayList<Char> targets = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8) {
			Char ch = Actor.findChar(hero.pos + n);
			if (ch != null && ch.alignment == Char.Alignment.ENEMY) targets.add(ch);
		}
		if (targets.isEmpty()) {
			GLog.w(Messages.get(SlothPillow.class, "nobody_near"));
			return;
		}
		if (!r.useCharges(COST_SHOO)) return;

		int shove = hero.pointsInTalent(Talent.SLOTH_SHOVE);
		for (Char ch : targets) {
			int dmg = Random.NormalIntRange(1, 3 + hero.lvl / 3) * (1 + shove);
			ch.damage(dmg, SlothPillow.class);
			if (!ch.isAlive()) continue;
			//push straight away from the hero, the same way a blast wave does
			Ballistica trajectory = new Ballistica(ch.pos, ch.pos + (ch.pos - hero.pos), Ballistica.MAGIC_BOLT);
			WandOfBlastWave.throwChar(ch, trajectory, 2 + shove, false, true, SlothPillow.class);
		}
		hero.sprite.operate(hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.7f);
		hero.spendAndNext(1f);
	}

	private static void yawn(Hero hero, SlothRepose r) {
		if (!r.useCharges(COST_YAWN)) return;
		int rad = yawnRadius(hero);
		int n = 0;
		for (Mob mob : new ArrayList<>(Dungeon.level.mobs)) {
			if (mob.alignment == Char.Alignment.ENEMY && Dungeon.level.heroFOV[mob.pos]
					&& Dungeon.level.distance(hero.pos, mob.pos) <= rad
					&& !mob.properties().contains(Char.Property.BOSS)
					&& !mob.properties().contains(Char.Property.MINIBOSS)) {
				if (Buff.affect(mob, MagicalSleep.class) != null) {
					mob.sprite.emitter().burst(Speck.factory(Speck.NOTE), 2);
					n++;
				}
			}
		}
		if (n == 0) GLog.i(Messages.get(SlothPillow.class, "yawn_none"));
		hero.sprite.operate(hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.LULLABY);
		hero.spendAndNext(1f);
	}

	private final CellSelector.Listener roll = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero hero = Dungeon.hero;
			if (cell == null || cell == hero.pos) return;
			boolean ok = Dungeon.level.heroFOV[cell]
					&& Dungeon.level.passable[cell]
					&& Actor.findChar(cell) == null
					&& Dungeon.level.distance(hero.pos, cell) <= rollRange(hero)
					&& new Ballistica(hero.pos, cell, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.STOP_CHARS).collisionPos.equals(cell);
			if (!ok) {
				GLog.w(Messages.get(SlothPillow.class, "bad_cell"));
				return;
			}
			SlothRepose r = repose(hero);
			if (!r.useCharges(COST_ROLL)) return;

			hero.sprite.interruptMotion();
			hero.move(cell, false);
			hero.sprite.place(cell);
			Dungeon.level.occupyCell(hero);
			Dungeon.observe();
			GameScene.updateFog();
			//rolling doesn't count as getting up
			r.lastPos = hero.pos;
			CellEmitter.bottom(hero.pos).burst(Speck.factory(Speck.DUST), 5);
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(SlothPillow.class, "prompt_roll", rollRange(Dungeon.hero));
		}
	};

	// ------------------------------------------------------------------ info

	@Override
	public String status() {
		SlothRepose r = SlothRepose.of();
		return r == null ? null : r.charges + "/" + r.maxCharges();
	}

	@Override
	public String desc() {
		String s = Messages.get(this, "desc");
		SlothRepose r = SlothRepose.of();
		if (r != null) s += "\n\n" + r.statsText();
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
