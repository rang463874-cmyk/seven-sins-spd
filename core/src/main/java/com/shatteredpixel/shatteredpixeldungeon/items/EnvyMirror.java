package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.skills.BasicSkill;
import com.shatteredpixel.shatteredpixeldungeon.skills.SinSkill;
import com.shatteredpixel.shatteredpixeldungeon.skills.SkillHolder;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndSkills;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnvyCovet;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * The Envy class's signature item. Doesn't take an equipment slot.
 * Uses recharging charges (stored in EnvyCovet) to take what others have.
 */
public class EnvyMirror extends Item implements SkillHolder {

	public static final String AC_GAZE = "GAZE";
	public static final String AC_CODEX = "CODEX";

	public static final int COST_SNATCH = 1;
	public static final int COST_EQUALIZE = 2;
	public static final int COST_SWAP = 1;
	public static final int COST_MIMIC = 1;

	{
		image = ItemSpriteSheet.ENVY_MIRROR;
		defaultAction = AC_GAZE;
		unique = true;
		bones = false;
		keptThoughLostInvent = true;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.remove(AC_DROP);
		actions.remove(AC_THROW);
		actions.add(AC_GAZE);
		actions.add(AC_CODEX);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_GAZE)) return Messages.get(this, "ac_gaze");
		if (action.equals(AC_CODEX)) return Messages.get(this, "ac_codex");
		return super.actionName(action, hero);
	}

	static EnvyCovet covet(Hero hero) {
		return Buff.affect(hero, EnvyCovet.class);
	}

	public static int swapRange(Hero hero) {
		return 6 + 2 * hero.pointsInTalent(Talent.ENVY_SWITCH);
	}

	public static float snatchDuration(Hero hero) {
		return 8f + 4f * hero.pointsInTalent(Talent.ENVY_SNATCH);
	}

	public static boolean royal(Char ch) {
		return ch.properties().contains(Char.Property.BOSS) || ch.properties().contains(Char.Property.MINIBOSS);
	}

	// ------------------------------------------------------------------ skills list

	@Override
	public String skillHeader(Hero hero) {
		EnvyCovet g = covet(hero);
		return Messages.get(this, "skill_header", g.charges, g.maxCharges());
	}

	@Override
	public ArrayList<SinSkill> skills(final Hero hero) {
		ArrayList<SinSkill> list = new ArrayList<>();
		list.add(new BasicSkill(EnvyMirror.class, "snatch", 40, SinSkill.CostType.CHARGES)
				.cost(h -> COST_SNATCH)
				.usableIf(h -> covet(h).charges >= COST_SNATCH)
				.descArgs(h -> new Object[]{Math.round(snatchDuration(h))})
				.onUse(h -> GameScene.selectCell(snatch)));
		list.add(new BasicSkill(EnvyMirror.class, "equalize", 41, SinSkill.CostType.CHARGES)
				.cost(h -> COST_EQUALIZE)
				.usableIf(h -> covet(h).charges >= COST_EQUALIZE)
				.onUse(h -> GameScene.selectCell(equalize)));
		list.add(new BasicSkill(EnvyMirror.class, "swap", 42, SinSkill.CostType.CHARGES)
				.cost(h -> COST_SWAP)
				.usableIf(h -> covet(h).charges >= COST_SWAP)
				.descArgs(h -> new Object[]{swapRange(h)})
				.onUse(h -> GameScene.selectCell(swap)));
		list.add(new BasicSkill(EnvyMirror.class, "mimic", 43, SinSkill.CostType.CHARGES)
				.cost(h -> COST_MIMIC)
				.usableIf(h -> covet(h).charges >= COST_MIMIC)
				.onUse(h -> GameScene.selectCell(mimic)));
		return list;
	}

	@Override
	public void execute(final Hero hero, String action) {
		super.execute(hero, action);
		final EnvyCovet c = covet(hero);

		if (action.equals(AC_CODEX)) {
			GameScene.show(new WndTitledMessage(new ItemSprite(this),
					Messages.get(this, "codex_title", c.codex.size(), EnvyCovet.CODEX.size()), c.codexText()));
			return;
		}
		if (!action.equals(AC_GAZE)) return;

		GameScene.show(new WndSkills(this, hero));
	}

	// ------------------------------------------------------------------ helpers

	private static Mob enemyAt(Integer cell) {
		if (cell == null) return null;
		Char ch = Actor.findChar(cell);
		if (!(ch instanceof Mob) || ch.alignment == Char.Alignment.ALLY || !Dungeon.level.heroFOV[cell]) {
			GLog.w(Messages.get(EnvyMirror.class, "no_target"));
			return null;
		}
		return (Mob) ch;
	}

	private static void mirrorFX(Hero hero, int pos) {
		hero.sprite.zap(pos);
		CellEmitter.center(pos).burst(Speck.factory(Speck.LIGHT), 4);
		Sample.INSTANCE.play(Assets.Sounds.TELEPORT, 0.6f, 1.4f);
		Invisibility.dispel();
	}

	/**
	 * Drags an enemy's health ratio down to the hero's. Bosses only lose half the difference.
	 * Shared with the Downfall armor ability. Returns damage dealt.
	 */
	public static int dragDown(Hero hero, Char enemy, float mult, Object src) {
		float heroRatio = hero.HP / (float) hero.HT;
		int target = Math.max(1, (int) Math.ceil(heroRatio * enemy.HT));
		if (enemy.HP <= target) return 0;
		int dmg = enemy.HP - target;
		if (royal(enemy)) dmg /= 2;
		dmg = Math.round(dmg * mult);
		dmg = Math.min(dmg, enemy.HP - 1);
		if (dmg <= 0) return 0;
		enemy.damage(dmg, src);
		return dmg;
	}

	// ------------------------------------------------------------------ skills

	private final CellSelector.Listener snatch = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero hero = Dungeon.hero;
			Mob mob = enemyAt(cell);
			if (mob == null) return;
			if (!covet(hero).useCharges(COST_SNATCH)) return;

			float dur = snatchDuration(hero);
			Buff.prolong(mob, Weakness.class, dur);
			Buff.prolong(hero, EnvyCovet.Stolen.class, dur);
			EnvyCovet.rob(hero, mob, 3);
			mob.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(EnvyMirror.class, "snatched"));
			mirrorFX(hero, mob.pos);
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(EnvyMirror.class, "prompt_snatch");
		}
	};

	private final CellSelector.Listener equalize = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero hero = Dungeon.hero;
			Mob mob = enemyAt(cell);
			if (mob == null) return;
			if (mob.HP / (float) mob.HT <= hero.HP / (float) hero.HT) {
				GLog.w(Messages.get(EnvyMirror.class, "not_envious"));
				return;
			}
			if (!covet(hero).useCharges(COST_EQUALIZE)) return;

			int dmg = dragDown(hero, mob, 1f, EnvyMirror.class);
			int lvl = hero.pointsInTalent(Talent.ENVY_LEVEL);
			if (lvl > 0 && mob.isAlive()) Buff.prolong(mob, Weakness.class, 1f + 2f * lvl);
			if (dmg > 0) GLog.i(Messages.get(EnvyMirror.class, "equalized", mob.name()));
			mirrorFX(hero, mob.pos);
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(EnvyMirror.class, "prompt_equalize");
		}
	};

	private final CellSelector.Listener swap = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero hero = Dungeon.hero;
			Mob mob = enemyAt(cell);
			if (mob == null) return;
			if (royal(mob) || mob.properties().contains(Char.Property.IMMOVABLE)
					|| Dungeon.level.distance(hero.pos, mob.pos) > swapRange(hero)) {
				GLog.w(Messages.get(EnvyMirror.class, "cant_swap"));
				return;
			}
			if (!covet(hero).useCharges(COST_SWAP)) return;

			int heroPos = hero.pos;
			int mobPos = mob.pos;
			hero.sprite.interruptMotion();
			mob.sprite.interruptMotion();
			hero.pos = mobPos;
			mob.pos = heroPos;
			hero.sprite.place(mobPos);
			mob.sprite.place(heroPos);
			Dungeon.level.occupyCell(hero);
			Dungeon.level.occupyCell(mob);
			Dungeon.observe();
			GameScene.updateFog();

			CellEmitter.center(heroPos).burst(Speck.factory(Speck.LIGHT), 4);
			CellEmitter.center(mobPos).burst(Speck.factory(Speck.LIGHT), 4);
			Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(EnvyMirror.class, "prompt_swap", swapRange(Dungeon.hero));
		}
	};

	private final CellSelector.Listener mimic = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero hero = Dungeon.hero;
			Mob mob = enemyAt(cell);
			if (mob == null) return;
			if (!covet(hero).useCharges(COST_MIMIC)) return;

			//take every positive timed effect the enemy has
			int taken = 0;
			for (Buff b : mob.buffs()) {
				if (b.type == Buff.buffType.POSITIVE && b instanceof FlavourBuff && !(b instanceof AllyBuff)) {
					float left = ((FlavourBuff) b).cooldown();
					try {
						Buff.prolong(hero, ((FlavourBuff) b).getClass(), left);
						b.detach();
						taken++;
					} catch (Throwable t) {
						//not every buff can be moved
					}
				}
			}

			//and mirror a share of its vitality as shielding
			float mult = 1f + 0.5f * hero.pointsInTalent(Talent.COPY_MASK);
			int shield = Math.min(Math.round(hero.HT * 0.5f), Math.round(mob.HT * 0.2f * mult));
			Buff.affect(hero, Barrier.class).setShield(shield);
			hero.sprite.showStatus(CharSprite.POSITIVE, Integer.toString(shield));
			if (taken > 0) GLog.p(Messages.get(EnvyMirror.class, "mimicked", taken));
			mirrorFX(hero, mob.pos);
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(EnvyMirror.class, "prompt_mimic");
		}
	};

	// ------------------------------------------------------------------ info

	@Override
	public String status() {
		EnvyCovet c = EnvyCovet.of();
		return c == null ? null : c.charges + "/" + c.maxCharges();
	}

	@Override
	public String desc() {
		String s = Messages.get(this, "desc");
		EnvyCovet c = EnvyCovet.of();
		if (c != null) s += "\n\n" + c.statsText();
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
