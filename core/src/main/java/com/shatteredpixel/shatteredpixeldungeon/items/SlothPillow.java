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
 * Each skill costs time: the sloth dozes off for a few turns afterwards. Summons dream puppets, naps,
 * swap places with a puppet and lull enemies to sleep.
 */
public class SlothPillow extends SinArtifact {

	public static final String AC_LAZE = "LAZE";

	public static final int COST_SUMMON = 4;
	public static final int COST_NAP = 3;
	public static final int COST_SWAP = 2;
	public static final int COST_YAWN = 3;

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
		if (isEquipped(hero)) actions.add(AC_LAZE);
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

	// ------------------------------------------------------------------ skills list

	@Override
	public String skillHeader(Hero hero) {
		SlothRepose r = repose(hero);
		return Messages.get(this, "skill_header", DreamPuppet.puppets(false).size(), r.maxPuppets());
	}

	@Override
	protected String levelEffects(Hero hero) {
		return Messages.get(SinArtifact.class, "effects_turns", Math.round((1f - SinArtifact.rechargeScale(hero)) * 100),
				SlothRepose.pillowPuppets(hero));
	}

	@Override
	public ArrayList<SinSkill> skills(final Hero hero) {
		ArrayList<SinSkill> list = new ArrayList<>();
		list.add(new BasicSkill(SlothPillow.class, "summon", 48, SinSkill.CostType.TURNS)
				.nameFrom(h -> DreamPuppet.puppets(false).size() >= repose(h).maxPuppets() ? "mend" : "summon")
				.cost(h -> SlothRepose.turnCost(h, COST_SUMMON))
				.descArgs(h -> new Object[]{repose(h).maxPuppets()})
				.onUse(h -> summon(h, repose(h))));
		list.add(new BasicSkill(SlothPillow.class, "nap", 49, SinSkill.CostType.TURNS)
				.cost(h -> SlothRepose.turnCost(h, COST_NAP))
				.onUse(h -> nap(h, repose(h))));
		list.add(new BasicSkill(SlothPillow.class, "swap", 50, SinSkill.CostType.TURNS)
				.cost(h -> SlothRepose.turnCost(h, COST_SWAP))
				.usableIf(h -> !DreamPuppet.active().isEmpty())
				.onUse(h -> GameScene.selectCell(swap)));
		list.add(new BasicSkill(SlothPillow.class, "yawn", 51, SinSkill.CostType.TURNS)
				.cost(h -> SlothRepose.turnCost(h, COST_YAWN))
				.descArgs(h -> new Object[]{YAWN_RADIUS})
				.onUse(h -> yawn(h, repose(h))));
		return list;
	}

	@Override
	public void execute(final Hero hero, String action) {
		super.execute(hero, action);
		if (!canUseSkills(hero, action)) return;
		if (!action.equals(AC_LAZE)) return;

		GameScene.show(new WndSkills(this, hero));
	}

	// ------------------------------------------------------------------ skills

	private static void summon(Hero hero, SlothRepose r) {
		ArrayList<DreamPuppet> mine = DreamPuppet.puppets(false);
		if (mine.size() >= r.maxPuppets()) {
			//all puppets are out already: patch them up instead
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
			int snore = hero.pointsInTalent(Talent.SLOTH_SNORE);
			if (snore > 0) SlothRepose.Taunted.applyAround(p, 3, 1f + 2f * snore);
		}
		hero.sprite.operate(hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.LULLABY, 0.6f);
		SlothRepose.doze(hero, SlothRepose.turnCost(hero, COST_SUMMON));
	}

	private static void nap(Hero hero, SlothRepose r) {
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
		SlothRepose.doze(hero, SlothRepose.turnCost(hero, COST_NAP));
	}

	private static void yawn(Hero hero, SlothRepose r) {
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
		SlothRepose.doze(hero, SlothRepose.turnCost(hero, COST_YAWN));
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
			SlothRepose.doze(hero, SlothRepose.turnCost(hero, COST_SWAP));
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
		return r == null ? null : DreamPuppet.puppets(false).size() + "/" + r.maxPuppets();
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
