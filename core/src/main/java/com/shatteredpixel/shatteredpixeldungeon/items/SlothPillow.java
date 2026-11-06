package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DreamPuppet;
import com.shatteredpixel.shatteredpixeldungeon.sloth.SlothHooks;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SlothRepose;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

/**
 * The Sloth class's signature item. Doesn't take an equipment slot.
 * Uses recharging charges (stored in SlothRepose) to summon dream puppets, nap,
 * swap places with a puppet and lull enemies to sleep.
 */
public class SlothPillow extends Item {

	public static final String AC_LAZE = "LAZE";

	public static final int COST_SUMMON = 2;
	public static final int COST_NAP = 1;
	public static final int COST_SWAP = 1;
	public static final int COST_YAWN = 2;

	public static final int YAWN_RADIUS = 2;

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

	@Override
	public void execute(final Hero hero, String action) {
		super.execute(hero, action);
		if (!action.equals(AC_LAZE)) return;
		final SlothRepose r = repose(hero);
		final boolean full = DreamPuppet.puppets(false).size() >= r.maxPuppets();

		GameScene.show(new WndOptions(new ItemSprite(this), Messages.titleCase(name()),
				Messages.get(this, "prompt", r.charges, r.maxCharges(),
						DreamPuppet.puppets(false).size(), r.maxPuppets()),
				Messages.get(this, full ? "opt_mend" : "opt_summon", COST_SUMMON),
				Messages.get(this, "opt_nap", COST_NAP),
				Messages.get(this, "opt_swap", COST_SWAP),
				Messages.get(this, "opt_yawn", COST_YAWN)) {
			@Override
			protected boolean enabled(int index) {
				int[] costs = {COST_SUMMON, COST_NAP, COST_SWAP, COST_YAWN};
				if (index == 2 && DreamPuppet.active().isEmpty()) return false;
				return r.charges >= costs[index];
			}

			@Override
			protected void onSelect(int index) {
				switch (index) {
					case 0: summon(hero, r); break;
					case 1: nap(hero, r); break;
					case 2: GameScene.selectCell(swap); break;
					case 3: yawn(hero, r); break;
				}
			}
		});
	}

	// ------------------------------------------------------------------ skills

	private static void summon(Hero hero, SlothRepose r) {
		ArrayList<DreamPuppet> mine = DreamPuppet.puppets(false);
		if (mine.size() >= r.maxPuppets()) {
			//all puppets are out already: patch them up instead
			if (!r.useCharges(COST_SUMMON)) return;
			for (DreamPuppet p : mine) {
				int heal = p.HT - p.HP;
				if (heal > 0) {
					p.HP = p.HT;
					p.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
				}
			}
		} else {
			DreamPuppet p = DreamPuppet.create(false, 0);
			if (!SlothHooks.spawnNear(hero.pos, p)) {
				GLog.w(Messages.get(SlothPillow.class, "no_room"));
				return;
			}
			r.useCharges(COST_SUMMON);
			int snore = hero.pointsInTalent(Talent.SLOTH_SNORE);
			if (snore > 0) SlothRepose.Taunted.applyAround(p, 3, 1f + 2f * snore);
		}
		hero.sprite.operate(hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.LULLABY, 0.6f);
		hero.spendAndNext(1f);
	}

	private static void nap(Hero hero, SlothRepose r) {
		if (!r.useCharges(COST_NAP)) return;
		int heal = Math.min(hero.HT - hero.HP, Math.round(hero.HT * 0.10f));
		if (heal > 0) {
			hero.HP += heal;
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
		}
		for (DreamPuppet p : DreamPuppet.active()) {
			p.HP = Math.min(p.HT, p.HP + Math.round(p.HT * 0.25f));
		}
		hero.sprite.emitter().burst(Speck.factory(Speck.NOTE), 3);
		Sample.INSTANCE.play(Assets.Sounds.LULLABY, 0.6f);
		hero.spendAndNext(1f);
	}

	private static void yawn(Hero hero, SlothRepose r) {
		if (!r.useCharges(COST_YAWN)) return;
		int n = 0;
		for (Mob mob : new ArrayList<>(Dungeon.level.mobs)) {
			if (mob.alignment == Char.Alignment.ENEMY && Dungeon.level.heroFOV[mob.pos]
					&& Dungeon.level.distance(hero.pos, mob.pos) <= YAWN_RADIUS
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

	private final CellSelector.Listener swap = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero hero = Dungeon.hero;
			if (cell == null) return;
			Char ch = Actor.findChar(cell);
			if (!(ch instanceof DreamPuppet) || !Dungeon.level.heroFOV[cell]) {
				GLog.w(Messages.get(SlothPillow.class, "not_puppet"));
				return;
			}
			SlothRepose r = repose(hero);
			if (!r.useCharges(COST_SWAP)) return;

			DreamPuppet p = (DreamPuppet) ch;
			int heroPos = hero.pos;
			int puppetPos = p.pos;
			hero.sprite.interruptMotion();
			p.sprite.interruptMotion();
			p.pos = heroPos;
			p.sprite.place(heroPos);
			hero.pos = puppetPos;
			hero.sprite.place(puppetPos);
			Dungeon.level.occupyCell(hero);
			Dungeon.level.occupyCell(p);
			Dungeon.observe();
			GameScene.updateFog();
			//swapping is lazy enough that it doesn't wake you up
			r.lastPos = hero.pos;

			int roll = hero.pointsInTalent(Talent.SLOTH_ROLL);
			if (roll > 0) {
				int shield = Math.round((5 + hero.lvl) * roll);
				Buff.affect(p, Barrier.class).setShield(shield);
				p.sprite.showStatus(CharSprite.POSITIVE, Integer.toString(shield));
			}
			CellEmitter.bottom(hero.pos).burst(Speck.factory(Speck.DUST), 5);
			Sample.INSTANCE.play(Assets.Sounds.PUFF);
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(SlothPillow.class, "prompt_swap");
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
