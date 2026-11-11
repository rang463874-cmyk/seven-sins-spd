package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PredatorStomach;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SinArtifact;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.predator.PredatorHooks;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.skills.BasicSkill;
import com.shatteredpixel.shatteredpixeldungeon.skills.SinSkill;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndSkills;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * The Glutton's signature maw. Its growth comes from consumables (see PredatorStomach);
 * its skills are paid for with satiety, except for swallowing a consumable whole.
 */
public class PredatorMaw extends SinArtifact {

	/** growth copy: base satiety cost (a full stomach) */
	public static final int COPY_COST = 300;

	public static final String AC_FEED = "FEED";
	public static final String AC_CODEX = "CODEX";

	/** base satiety costs, before talents, codex and the maw's level */
	public static final int COST_BITE = 60; //old satiety cost, kept for reference
	/** bite costs this share of max HP (min 1) and fills satiety when it kills */
	public static final float BITE_HP_COST = 0.06f;
	public static final int BITE_FEED = 80;

	public static int biteHpCost(Hero hero) {
		return Math.max(1, Math.round(hero.HT * BITE_HP_COST));
	}
	public static final int COST_DASH = 50;
	public static final int COST_ACID = 80;
	public static final int COST_RUMINATE = 150;

	public static final float BITE_DAMAGE = 1.5f;
	public static final float BITE_LIFESTEAL = 0.30f;
	public static final int ACID_RADIUS = 2;

	{
		image = ItemSpriteSheet.PREDATOR_MAW;
		defaultAction = AC_FEED;
		unique = true;
		bones = false;
		keptThoughLostInvent = true;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.remove(AC_DROP);
		actions.remove(AC_THROW);
		if (isEquipped(hero)) actions.add(AC_FEED);
		actions.add(AC_CODEX);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_FEED)) return Messages.get(this, "ac_feed");
		if (action.equals(AC_CODEX)) return Messages.get(this, "ac_codex");
		return super.actionName(action, hero);
	}

	@Override
	public void execute(final Hero hero, String action) {
		super.execute(hero, action);
		//the codex can be read whether or not the maw is worn
		if (action.equals(AC_CODEX)) {
			PredatorStomach st = Buff.affect(hero, PredatorStomach.class);
			GameScene.show(new com.shatteredpixel.shatteredpixeldungeon.windows.WndScrollMessage(
					new com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite(this),
					Messages.get(this, "codex_title", st.codex.size(), PredatorStomach.codexTotal()), st.codexText()));
			return;
		}
		if (!canUseSkills(hero, action)) return;
		if (action.equals(AC_FEED)) GameScene.show(new WndSkills(this, hero));
	}

	// ------------------------------------------------------------------ costs

	/** satiety cost of a skill with this base cost */
	public static int hungerCost(Hero hero, int base) {
		float mult = SinArtifact.satietyScale(hero) * com.shatteredpixel.shatteredpixeldungeon.sins.AwakeningHooks.gluttonyCost(hero); //Seven Sins: awakening
		if (PredatorStomach.hasCodex(hero, PredatorStomach.CODEX_FRUGAL)) mult *= 0.5f;
		return Math.max(1, Math.round(base * mult));
	}

	public static boolean canAfford(Hero hero, int base) {
		Hunger hunger = hero.buff(Hunger.class);
		return hunger != null && hunger.hunger() + hungerCost(hero, base) <= Hunger.STARVING;
	}

	private static boolean pay(Hero hero, int base) {
		if (!canAfford(hero, base)) {
			GLog.w(Messages.get(PredatorMaw.class, "bite_hungry"));
			return false;
		}
		hero.buff(Hunger.class).affectHunger(-hungerCost(hero, base));
		SinArtifact.onSpent(hero, 1);
		return true;
	}

	public static float dashTurns(Hero hero) {
		return 5f + SinArtifact.level(hero) / 2f;
	}

	public static int acidMin(Hero hero) {
		return 1 + hero.lvl / 4;
	}

	public static int acidMax(Hero hero) {
		return 3 + hero.lvl / 2;
	}

	// ------------------------------------------------------------------ skills list

	@Override
	public String skillHeader(Hero hero) {
		Hunger hunger = hero.buff(Hunger.class);
		int left = hunger == null ? 0 : Math.max(0, Math.round(Hunger.STARVING - hunger.hunger()));
		return Messages.get(this, "skill_header", left);
	}

	@Override
	protected String levelEffects(Hero hero) {
		return Messages.get(SinArtifact.class, "effects_maw", Math.round((1f - SinArtifact.satietyScale(hero)) * 100),
				Math.round((SinArtifact.devourScale(hero) - 1f) * 100));
	}

	@Override
	public ArrayList<SinSkill> skills(final Hero hero) {
		ArrayList<SinSkill> list = new ArrayList<>();
		list.add(new BasicSkill(PredatorMaw.class, "bite", 1, SinSkill.CostType.HP).targetsEnemy()
				.cost(h -> biteHpCost(h))
				.usableIf(h -> h.HP > biteHpCost(h))
				.descArgs(h -> new Object[]{Math.round(BITE_DAMAGE * 100), Math.round(BITE_LIFESTEAL * 100), BITE_FEED})
				.onUse(h -> GameScene.selectCell(biteSelector)));
		list.add(new BasicSkill(PredatorMaw.class, "dash", 3, SinSkill.CostType.SATIETY)
				.cost(h -> hungerCost(h, COST_DASH))
				.usableIf(h -> canAfford(h, COST_DASH))
				.descArgs(h -> new Object[]{Math.round(dashTurns(h))})
				.onUse(PredatorMaw::dash));
		list.add(new BasicSkill(PredatorMaw.class, "acid", 4, SinSkill.CostType.SATIETY)
				.cost(h -> hungerCost(h, COST_ACID))
				.usableIf(h -> canAfford(h, COST_ACID))
				.descArgs(h -> new Object[]{ACID_RADIUS, acidMin(h), acidMax(h)})
				.onUse(PredatorMaw::acid));
		list.add(new BasicSkill(PredatorMaw.class, "swallow", 0, SinSkill.CostType.NONE)
				.descArgs(h -> new Object[]{Math.round(3 + 1.5f * h.pointsInTalent(Talent.PREDATOR_SCRAPS))})
				.onUse(PredatorMaw::selectSwallow));
		list.add(new BasicSkill(PredatorMaw.class, "ruminate", 2, SinSkill.CostType.SATIETY)
				.cost(h -> hungerCost(h, COST_RUMINATE))
				.usableIf(h -> ruminable(h) != null && canAfford(h, COST_RUMINATE))
				.descArgs(h -> {
					Class c = ruminable(h);
					return new Object[]{c == null ? Messages.get(PredatorMaw.class, "nothing") : Messages.get(c, "name")};
				})
				.onUse(PredatorMaw::ruminate));
		//Regurgitate (Nov 2026): spit the last potion back up as a thrown one
		list.add(new BasicSkill(PredatorMaw.class, "regurgitate", 5, SinSkill.CostType.SATIETY).projectile()
				.cost(h -> hungerCost(h, COST_REGURGITATE))
				.usableIf(h -> regurgitable(h) != null && canAfford(h, COST_REGURGITATE))
				.descArgs(h -> {
					Class c = regurgitable(h);
					return new Object[]{c == null ? Messages.get(PredatorMaw.class, "nothing") : Messages.get(c, "name")};
				})
				.onUse(h -> GameScene.selectCell(regurgitateSelector)));
		//growth copy: regurgitate a growth item (a full stomach's worth of satiety)
		com.shatteredpixel.shatteredpixeldungeon.skills.GrowthCopy.add(list, PredatorMaw.class, 0, com.shatteredpixel.shatteredpixeldungeon.skills.SinSkill.CostType.SATIETY,
				h -> hungerCost(h, COPY_COST), h -> canAfford(h, COPY_COST), h -> pay(h, COPY_COST), true);
		return list;
	}

	// ------------------------------------------------------------------ skills

	private static void dash(Hero hero) {
		if (!pay(hero, COST_DASH)) return;
		Buff.prolong(hero, Haste.class, dashTurns(hero));
		hero.sprite.emitter().burst(Speck.factory(Speck.JET), 6);
		Sample.INSTANCE.play(Assets.Sounds.EAT, 1f, 1.4f);
		hero.spendAndNext(Actor.TICK);
	}

	private static void acid(Hero hero) {
		ArrayList<Char> targets = new ArrayList<>();
		for (Char ch : Actor.chars()) {
			if (ch != hero && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()
					&& Dungeon.level.heroFOV[ch.pos]
					&& Dungeon.level.distance(hero.pos, ch.pos) <= ACID_RADIUS) {
				targets.add(ch);
			}
		}
		if (targets.isEmpty()) {
			GLog.w(Messages.get(PredatorMaw.class, "acid_none"));
			return;
		}
		if (!pay(hero, COST_ACID)) return;
		Splash.at(hero.pos, 0x99BB22, 16);
		for (Char ch : targets) {
			Splash.at(ch.pos, 0x99BB22, 8);
			ch.damage(Random.NormalIntRange(acidMin(hero), acidMax(hero)), PredatorMaw.class);
			if (ch.isAlive()) {
				Buff.prolong(ch, Weakness.class, 6f);
				Buff.prolong(ch, Cripple.class, 4f);
			}
		}
		hero.sprite.operate(hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.GAS);
		Invisibility.dispel();
		hero.spendAndNext(Actor.TICK);
	}

	/** satiety eaten by bringing back a growth item; whatever the stomach can't pay becomes hunger debt */
	public static final int HEAVY_RUMINATE = 600;

	/** the last potion or scroll, if it can be brought back up (growth items can't since Nov 2026: growth copy does that) */
	public static Class ruminable(Hero hero) {
		PredatorStomach st = hero.buff(PredatorStomach.class);
		if (st == null || st.lastUsed == null) return null;
		if (com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.Brew.class.isAssignableFrom(st.lastUsed)) return null;
		if (!PredatorHooks.canRepeat(st.lastUsed)) return null;
		return st.lastUsed;
	}

	/** Regurgitate: satiety cost */
	public static final int COST_REGURGITATE = 100;

	/** the last potion drunk, if it can be spat back up */
	public static Class regurgitable(Hero hero) {
		PredatorStomach st = hero.buff(PredatorStomach.class);
		if (st == null || st.lastPotion == null) return null;
		if (!Potion.class.isAssignableFrom(st.lastPotion) || !PredatorHooks.canRepeat(st.lastPotion)) return null;
		if (com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.Brew.class.isAssignableFrom(st.lastPotion)) return null;
		return st.lastPotion;
	}

	private static final CellSelector.Listener regurgitateSelector = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			if (cell == null) return;
			final Hero hero = Dungeon.hero;
			Class c = regurgitable(hero);
			if (c == null || cell == hero.pos) return;
			final Potion p = (Potion) Reflection.newInstance(c);
			if (p == null || !pay(hero, COST_REGURGITATE)) return;
			p.anonymize();
			final int target = new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(hero.pos, cell,
					com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.PROJECTILE).collisionPos;
			Sample.INSTANCE.play(Assets.Sounds.EAT, 1f, 0.6f);
			hero.sprite.zap(target);
			((com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite) hero.sprite.parent.recycle(
					com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite.class)).reset(hero.sprite, target, p,
					new com.watabou.utils.Callback() {
						@Override
						public void call() {
							Item.curUser = hero;
							p.shatter(target);
							GLog.p(Messages.get(PredatorMaw.class, "regurgitated", p.name()));
							hero.spendAndNext(Actor.TICK);
						}
					});
		}

		@Override
		public String prompt() {
			return Messages.get(PredatorMaw.class, "prompt_regurgitate");
		}
	};

	/** whether ruminating right now would bring back a growth item */
	public static boolean heavyRumination(Hero hero) {
		Class c = ruminable(hero);
		return c != null && !PredatorHooks.canRepeat(c);
	}

	/** eats 600 satiety: the stomach empties, and the rest is owed as hunger debt */
	private static void payHeavy(Hero hero) {
		Hunger hunger = hero.buff(Hunger.class);
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WellFed fed = hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WellFed.class);
		if (fed != null) fed.detach();
		float available = hunger == null ? 0 : Math.max(0, Hunger.STARVING - hunger.hunger());
		if (hunger != null && available > 0) hunger.affectHunger(-available);
		float owed = HEAVY_RUMINATE - available;
		if (owed > 0) {
			Buff.affect(hero, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GluttonDebt.class).add(owed);
			GLog.n(Messages.get(PredatorMaw.class, "ruminate_debt", Math.round(owed)));
		}
	}

	private static void ruminate(Hero hero) {
		Class c = ruminable(hero);
		if (c == null) {
			GLog.w(Messages.get(PredatorMaw.class, "ruminate_none"));
			return;
		}
		if (!pay(hero, COST_RUMINATE)) return;
		Item it = (Item) Reflection.newInstance(c);
		if (it == null) return;
		if (it instanceof Potion) ((Potion) it).anonymize();
		if (it instanceof Scroll) ((Scroll) it).anonymize();
		GLog.p(Messages.get(PredatorMaw.class, "ruminated", it.name()));
		Sample.INSTANCE.play(Assets.Sounds.EAT, 1f, 0.7f);
		Item.curUser = hero;
		Item.curItem = it;
		if (it instanceof Potion) {
			hero.sprite.operate(hero.pos);
			if (!PredatorHooks.drinkOverride(hero, (Potion) it)) ((Potion) it).apply(hero);
			hero.spendAndNext(Actor.TICK);
		} else if (it instanceof Scroll) {
			//reading takes its own turn through readAnimation
			((Scroll) it).doRead();
		}
	}

	private static void selectSwallow(final Hero hero) {
		GameScene.selectItem(new WndBag.ItemSelector() {
			@Override
			public String textPrompt() {
				return Messages.get(PredatorMaw.class, "prompt_swallow");
			}

			@Override
			public boolean itemSelectable(Item item) {
				return PredatorStomach.category(item) >= 0;
			}

			@Override
			public void onSelect(Item item) {
				if (item == null || PredatorStomach.category(item) < 0) return;
				Item one = item.detach(hero.belongings.backpack);
				if (one == null) return;
				Buff.affect(hero, PredatorStomach.class).consume(hero, one, true);
				GLog.p(Messages.get(PredatorMaw.class, "swallowed", one.name()));
				hero.sprite.operate(hero.pos);
				Sample.INSTANCE.play(Assets.Sounds.EAT);
				Invisibility.dispel();
				hero.spendAndNext(Actor.TICK);
			}
		});
	}

	private static final CellSelector.Listener biteSelector = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			if (cell == null) return;
			final Hero hero = Dungeon.hero;
			Char enemy = Actor.findChar(cell);
			//Seven Sins: awakening (gluttony's monster bites from 2 tiles away)
			int reach = com.shatteredpixel.shatteredpixeldungeon.sins.AwakeningHooks.biteRange(hero);
			boolean inReach = Dungeon.level.adjacent(hero.pos, cell)
					|| (reach > 1 && Dungeon.level.distance(hero.pos, cell) <= reach
						&& new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(hero.pos, cell,
							com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.PROJECTILE).collisionPos == cell);
			if (enemy == null || enemy == hero || enemy.alignment == Char.Alignment.ALLY || !inReach) {
				GLog.w(Messages.get(PredatorMaw.class, "bite_target"));
				return;
			}
			//bite is paid in blood, never enough to kill you
			int cost = biteHpCost(hero);
			if (hero.HP <= cost) {
				GLog.w(Messages.get(PredatorMaw.class, "bite_weak"));
				return;
			}
			hero.HP -= cost;
			hero.sprite.showStatus(CharSprite.NEGATIVE, Integer.toString(cost));

			int before = enemy.HP + enemy.shielding();
			hero.sprite.turnTo(hero.pos, cell);
			hero.sprite.operate(hero.pos);
			hero.attack(enemy, BITE_DAMAGE, 0f, 1000f);
			int dealt = Math.max(0, before - (enemy.isAlive() ? enemy.HP + enemy.shielding() : 0));
			float steal = BITE_LIFESTEAL * (PredatorStomach.hasCodex(hero, PredatorStomach.CODEX_FANGS) ? 2f : 1f);
			int heal = Math.min(hero.HT - hero.HP, Math.round(dealt * steal));
			if (heal > 0) {
				hero.HP += heal;
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			}
			//a kill is a meal
			if (!enemy.isAlive()) {
				Hunger hunger = hero.buff(Hunger.class);
				if (hunger != null) hunger.satisfy(BITE_FEED);
				hero.sprite.emitter().burst(com.shatteredpixel.shatteredpixeldungeon.effects.Speck.factory(com.shatteredpixel.shatteredpixeldungeon.effects.Speck.HEALING), 2);
				GLog.p(Messages.get(PredatorMaw.class, "bite_fed"));
			}
			Sample.INSTANCE.play(Assets.Sounds.EAT);
			Invisibility.dispel();
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(PredatorMaw.class, "bite_prompt");
		}
	};

	@Override
	public String desc() {
		String d = Messages.get(this, "desc");
		Hero h = Dungeon.hero;
		PredatorStomach s = h == null ? null : h.buff(PredatorStomach.class);
		if (s != null) d += "\n\n" + s.statsText();
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
