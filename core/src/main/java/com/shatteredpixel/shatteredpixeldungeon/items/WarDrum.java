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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WrathGrudge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
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
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The Wrath class's signature item. Doesn't take an equipment slot.
 * Spends rage (stored in WrathGrudge) to set enemies against each other.
 */
public class WarDrum extends SinArtifact {

	public static final String AC_BEAT = "BEAT";

	public static final int COST_PROVOKE = 25;
	public static final int COST_DISCORD = 25;
	public static final int COST_CONTAGION = 50;
	public static final int COST_DASH = 25;

	{
		image = ItemSpriteSheet.WAR_DRUM;
		defaultAction = AC_BEAT;
		unique = true;
		bones = false;
		keptThoughLostInvent = true;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.remove(AC_DROP);
		actions.remove(AC_THROW);
		if (isEquipped(hero)) actions.add(AC_BEAT);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_BEAT)) return Messages.get(this, "ac_beat");
		return super.actionName(action, hero);
	}

	static WrathGrudge grudge(Hero hero) {
		return Buff.affect(hero, WrathGrudge.class);
	}

	public static int dashRange(Hero hero) {
		return 3 + hero.pointsInTalent(Talent.WRATH_DASH_STRIKE) / 2;
	}

	// ------------------------------------------------------------------ skills list

	@Override
	public String skillHeader(Hero hero) {
		WrathGrudge g = grudge(hero);
		return Messages.get(this, "skill_header", (int) g.rage, g.maxRage());
	}

	@Override
	protected String levelEffects(Hero hero) {
		return Messages.get(SinArtifact.class, "effects_rage", 10 * SinArtifact.bonusCharges(hero),
				Math.round((1f / SinArtifact.rechargeScale(hero) - 1f) * 100));
	}

	@Override
	public ArrayList<SinSkill> skills(final Hero hero) {
		ArrayList<SinSkill> list = new ArrayList<>();
		list.add(new BasicSkill(WarDrum.class, "provoke", 16, SinSkill.CostType.RAGE).targetsEnemy()
				.cost(h -> COST_PROVOKE)
				.usableIf(h -> grudge(h).rage >= COST_PROVOKE)
				.descArgs(h -> new Object[]{Math.round(WrathGrudge.rageDuration(h, 8f))})
				.onUse(h -> GameScene.selectCell(provoke)));
		list.add(new BasicSkill(WarDrum.class, "discord", 17, SinSkill.CostType.RAGE).targetsEnemy()
				.cost(h -> COST_DISCORD)
				.usableIf(h -> grudge(h).rage >= COST_DISCORD)
				.descArgs(h -> new Object[]{Math.round(WrathGrudge.rageDuration(h, 12f))})
				.onUse(h -> GameScene.selectCell(discordFirst)));
		list.add(new BasicSkill(WarDrum.class, "contagion", 18, SinSkill.CostType.RAGE).targetsEnemy()
				.cost(h -> COST_CONTAGION)
				.usableIf(h -> grudge(h).rage >= COST_CONTAGION)
				.descArgs(h -> new Object[]{Math.round(WrathGrudge.rageDuration(h, 10f))})
				.onUse(h -> GameScene.selectCell(contagion)));
		list.add(new BasicSkill(WarDrum.class, "dash", 19, SinSkill.CostType.RAGE).targetsEnemy()
				.cost(h -> COST_DASH)
				.usableIf(h -> grudge(h).rage >= COST_DASH)
				.descArgs(h -> new Object[]{dashRange(h)})
				.onUse(h -> GameScene.selectCell(dash)));
		return list;
	}

	@Override
	public void execute(final Hero hero, String action) {
		super.execute(hero, action);
		if (!canUseSkills(hero, action)) return;
		if (!action.equals(AC_BEAT)) return;
		final WrathGrudge g = grudge(hero);

		GameScene.show(new WndSkills(this, hero));
	}

	// ------------------------------------------------------------------ helpers

	/** a visible, non-immune enemy at cell, or null (with a log message) */
	private static Mob rageTarget(Integer cell) {
		if (cell == null) return null;
		Char ch = Actor.findChar(cell);
		if (!(ch instanceof Mob) || ch == Dungeon.hero || ch.alignment == Char.Alignment.ALLY
				|| !Dungeon.level.heroFOV[cell]) {
			GLog.w(Messages.get(WarDrum.class, "no_target"));
			return null;
		}
		if (WrathGrudge.immune(ch)) {
			GLog.w(Messages.get(WarDrum.class, "immune"));
			return null;
		}
		return (Mob) ch;
	}

	private static void drumFX(Hero hero, int targetPos) {
		hero.sprite.zap(targetPos);
		CellEmitter.center(targetPos).burst(Speck.factory(Speck.SCREAM), 3);
		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 0.8f, Random.Float(0.9f, 1.1f));
		Invisibility.dispel();
	}

	/** applies amok from a War Drum skill, including the talents that ride along with it */
	public static void enrage(Hero hero, Mob mob, float baseDuration) {
		Buff.prolong(mob, Amok.class, WrathGrudge.rageDuration(hero, baseDuration));
		mob.state = mob.HUNTING;
		int glare = hero.pointsInTalent(Talent.WRATH_GLARE);
		if (glare > 0) Buff.prolong(mob, Vulnerable.class, 1f + 2f * glare);
		if (hero.hasTalent(Talent.WRATH_FIRST_BLOOD)) Buff.affect(mob, WrathGrudge.FirstBlood.class);
	}

	// ------------------------------------------------------------------ skills

	private final CellSelector.Listener provoke = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero hero = Dungeon.hero;
			Mob mob = rageTarget(cell);
			if (mob == null) return;
			if (!grudge(hero).useRage(COST_PROVOKE)) return;

			enrage(hero, mob, 8f);

			//Chaos talent: chance to also enrage an adjacent enemy
			int chaos = hero.pointsInTalent(Talent.WRATH_CHAOS);
			if (chaos > 0 && Random.Float() < 0.2f * chaos) {
				for (int n : PathFinder.NEIGHBOURS8) {
					Char other = Actor.findChar(mob.pos + n);
					if (other instanceof Mob && other.alignment == Char.Alignment.ENEMY
							&& !WrathGrudge.immune(other) && other.buff(Amok.class) == null) {
						enrage(hero, (Mob) other, 8f);
						break;
					}
				}
			}
			drumFX(hero, mob.pos);
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(WarDrum.class, "prompt_provoke");
		}
	};

	private Mob discordPick = null;

	private final CellSelector.Listener discordFirst = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Mob mob = rageTarget(cell);
			if (mob == null) return;
			discordPick = mob;
			//the scene resets the cell selector right after this returns, so ask for the
			//second target on the next frame instead of immediately
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.selectCell(discordSecond);
				}
			});
		}

		@Override
		public String prompt() {
			return Messages.get(WarDrum.class, "prompt_discord_1");
		}
	};

	private final CellSelector.Listener discordSecond = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero hero = Dungeon.hero;
			Mob a = discordPick;
			discordPick = null;
			Mob b = rageTarget(cell);
			if (a == null || b == null || !a.isAlive()) return;
			if (a == b) {
				GLog.w(Messages.get(WarDrum.class, "same_target"));
				return;
			}
			if (!grudge(hero).useRage(COST_DISCORD)) return;

			float dur = WrathGrudge.rageDuration(hero, 12f);
			setRivals(hero, a, b, dur);
			setRivals(hero, b, a, dur);
			drumFX(hero, a.pos);
			CellEmitter.center(b.pos).burst(Speck.factory(Speck.SCREAM), 3);
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(WarDrum.class, "prompt_discord_2");
		}
	};

	private static void setRivals(Hero hero, Mob mob, Mob rival, float dur) {
		Buff.prolong(mob, WrathGrudge.Discord.class, dur).object = rival.id();
		mob.state = mob.HUNTING;
		mob.aggro(rival);
		int glare = hero.pointsInTalent(Talent.WRATH_GLARE);
		if (glare > 0) Buff.prolong(mob, Vulnerable.class, 1f + 2f * glare);
		if (hero.hasTalent(Talent.WRATH_FIRST_BLOOD)) Buff.affect(mob, WrathGrudge.FirstBlood.class);
	}

	private final CellSelector.Listener contagion = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero hero = Dungeon.hero;
			Mob mob = rageTarget(cell);
			if (mob == null) return;
			if (!grudge(hero).useRage(COST_CONTAGION)) return;

			float dur = WrathGrudge.rageDuration(hero, 10f);
			enrage(hero, mob, 10f);
			Buff.prolong(mob, WrathGrudge.Contagion.class, dur);
			drumFX(hero, mob.pos);
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(WarDrum.class, "prompt_contagion");
		}
	};

	private final CellSelector.Listener dash = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero hero = Dungeon.hero;
			if (cell == null || cell == hero.pos) return;
			boolean ok = Dungeon.level.heroFOV[cell]
					&& Dungeon.level.passable[cell]
					&& Actor.findChar(cell) == null
					&& Dungeon.level.distance(hero.pos, cell) <= dashRange(hero)
					&& new Ballistica(hero.pos, cell, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.STOP_CHARS).collisionPos.equals(cell);
			if (!ok) {
				GLog.w(Messages.get(WarDrum.class, "bad_cell"));
				return;
			}
			if (!grudge(hero).useRage(COST_DASH)) return;

			ScrollOfTeleportation.appear(hero, cell);
			Dungeon.level.occupyCell(hero);
			Dungeon.observe();
			GameScene.updateFog();

			//Dash Strike talent: slam adjacent enemies on landing
			int strike = hero.pointsInTalent(Talent.WRATH_DASH_STRIKE);
			if (strike > 0) {
				for (int n : PathFinder.NEIGHBOURS8) {
					Char ch = Actor.findChar(hero.pos + n);
					if (ch != null && ch.alignment == Char.Alignment.ENEMY) {
						int dmg = Math.round(Random.NormalIntRange(1 + hero.lvl / 3, 4 + hero.lvl / 2) * (0.5f * strike + 0.5f));
						ch.damage(dmg, WarDrum.class);
					}
				}
			}
			CellEmitter.bottom(hero.pos).burst(Speck.factory(Speck.DUST), 6);
			Sample.INSTANCE.play(Assets.Sounds.MISS);
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(WarDrum.class, "prompt_dash", dashRange(Dungeon.hero));
		}
	};

	// ------------------------------------------------------------------ info

	@Override
	public String status() {
		WrathGrudge g = Dungeon.hero == null ? null : Dungeon.hero.buff(WrathGrudge.class);
		return g == null ? null : (int) g.rage + "/" + g.maxRage();
	}

	@Override
	public String desc() {
		String d = Messages.get(this, "desc");
		WrathGrudge g = WrathGrudge.of();
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
