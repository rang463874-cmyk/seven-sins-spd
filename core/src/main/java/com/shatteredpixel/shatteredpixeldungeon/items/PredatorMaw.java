package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SinArtifact;
import com.shatteredpixel.shatteredpixeldungeon.skills.BasicSkill;
import com.shatteredpixel.shatteredpixeldungeon.skills.SinSkill;
import com.shatteredpixel.shatteredpixeldungeon.skills.SkillHolder;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.audio.Sample;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PredatorStomach;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;

import java.util.ArrayList;

/**
 * The Predator's signature tool: devours equipment to grant permanent stats,
 * and can spend satiety on a ravenous bite that heals from the flesh it tears.
 */
public class PredatorMaw extends SinArtifact {

	public static final String AC_DEVOUR = "DEVOUR";
	public static final String AC_BITE = "BITE";

	/** satiety spent per bite */
	public static final int BITE_HUNGER = 60;
	public static final float BITE_DAMAGE = 1.5f;
	public static final float BITE_LIFESTEAL = 0.30f;

	{
		image = ItemSpriteSheet.PREDATOR_MAW;
		defaultAction = AC_DEVOUR;
		unique = true;
		bones = false;
		keptThoughLostInvent = true;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.remove(AC_DROP);
		actions.remove(AC_THROW);
		if (isEquipped(hero)) actions.add(AC_DEVOUR);
		if (isEquipped(hero)) actions.add(AC_BITE);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_DEVOUR)) return Messages.get(this, "ac_devour");
		if (action.equals(AC_BITE)) return Messages.get(this, "ac_bite");
		return super.actionName(action, hero);
	}

	@Override
	public void execute(final Hero hero, String action) {
		super.execute(hero, action);
		if (!canUseSkills(hero, action)) return;
		if (action.equals(AC_DEVOUR)) {
			selectDevour(hero, devoursPerUse(), false);
		} else if (action.equals(AC_BITE)) {
			Hunger hunger = hero.buff(Hunger.class);
			if (hunger == null || hunger.hunger() + BITE_HUNGER > Hunger.STARVING) {
				GLog.w(Messages.get(PredatorMaw.class, "bite_hungry"));
				return;
			}
			GameScene.selectCell(biteSelector);
		}
	}

	// ------------------------------------------------------------------ skills list

	@Override
	public String skillHeader(Hero hero) {
		return null;
	}

	@Override
	public ArrayList<SinSkill> skills(final Hero hero) {
		ArrayList<SinSkill> list = new ArrayList<>();
		list.add(new BasicSkill(PredatorMaw.class, "devour", 0, SinSkill.CostType.NONE)
				.onUse(h -> selectDevour(h, devoursPerUse(), false)));
		list.add(new BasicSkill(PredatorMaw.class, "bite", 1, SinSkill.CostType.SATIETY)
				.cost(h -> BITE_HUNGER)
				.usableIf(h -> {
					Hunger hunger = h.buff(Hunger.class);
					return hunger != null && hunger.hunger() + BITE_HUNGER <= Hunger.STARVING;
				})
				.descArgs(h -> new Object[]{BITE_HUNGER, Math.round(BITE_DAMAGE * 100), Math.round(BITE_LIFESTEAL * 100)})
				.onUse(h -> GameScene.selectCell(biteSelector)));
		return list;
	}

	/** how many items one use can devour (the mirror's Golden Maw eats two) */
	public int devoursPerUse() {
		return 1;
	}

	private void selectDevour(final Hero hero, final int remaining, final boolean ateAlready) {
		GameScene.selectItem(new WndBag.ItemSelector() {
			@Override
			public String textPrompt() {
				return Messages.get(PredatorMaw.class, ateAlready ? "prompt_more" : "prompt");
			}

			@Override
			public boolean itemSelectable(Item item) {
				return PredatorStomach.category(hero, item) >= 0;
			}

			@Override
			public void onSelect(Item item) {
				if (item == null) {
					//stopping after the first bite still takes the turn
					if (ateAlready) hero.spendAndNext(1f);
					return;
				}
				PredatorStomach stomach = Buff.affect(hero, PredatorStomach.class);
				if (stomach.devour(hero, item)) {
					GLog.p(Messages.get(PredatorMaw.class, "devoured", item.name()));
					hero.sprite.operate(hero.pos);
					Invisibility.dispel();
					if (remaining > 1) {
						selectDevour(hero, remaining - 1, true);
					} else {
						hero.spendAndNext(1f);
					}
				} else {
					GLog.w(Messages.get(PredatorMaw.class, "cant"));
					if (ateAlready) hero.spendAndNext(1f);
				}
			}
		});
	}


	private final CellSelector.Listener biteSelector = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			if (cell == null) return;
			final Hero hero = Dungeon.hero;
			Char enemy = Actor.findChar(cell);
			if (enemy == null || enemy == hero || enemy.alignment == Char.Alignment.ALLY
					|| !Dungeon.level.adjacent(hero.pos, cell)) {
				GLog.w(Messages.get(PredatorMaw.class, "bite_target"));
				return;
			}
			Hunger hunger = hero.buff(Hunger.class);
			if (hunger == null || hunger.hunger() + BITE_HUNGER > Hunger.STARVING) {
				GLog.w(Messages.get(PredatorMaw.class, "bite_hungry"));
				return;
			}
			hunger.affectHunger(-BITE_HUNGER);

			int before = enemy.HP + enemy.shielding();
			hero.sprite.turnTo(hero.pos, cell);
			hero.sprite.operate(hero.pos);
			hero.attack(enemy, BITE_DAMAGE, 0f, 1000f);
			int dealt = Math.max(0, before - (enemy.isAlive() ? enemy.HP + enemy.shielding() : 0));
			int heal = Math.min(hero.HT - hero.HP, Math.round(dealt * BITE_LIFESTEAL));
			if (heal > 0) {
				hero.HP += heal;
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
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
		if (curUser != null || com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero != null) {
			Hero h = com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;
			PredatorStomach s = h == null ? null : h.buff(PredatorStomach.class);
			if (s != null) d += "\n\n" + s.statsText();
		}
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
