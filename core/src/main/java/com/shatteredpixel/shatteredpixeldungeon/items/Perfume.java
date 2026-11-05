package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LustDesire;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
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
 * The Lust class's signature item. Doesn't take an equipment slot.
 * Uses recharging charges (stored in LustDesire) to charm enemies and drain their life.
 */
public class Perfume extends Item {

	public static final String AC_SPRAY = "SPRAY";

	public static final int COST_GLANCE = 1;
	public static final int COST_SCENT = 2;
	public static final int COST_DRAIN = 1;
	public static final int COST_ALLURE = 1;

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
		actions.add(AC_SPRAY);
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

	public static int scentRadius(Hero hero) {
		return 2 + hero.pointsInTalent(Talent.LUST_WAFT);
	}

	@Override
	public void execute(final Hero hero, String action) {
		super.execute(hero, action);
		if (!action.equals(AC_SPRAY)) return;
		final LustDesire d = desire(hero);

		GameScene.show(new WndOptions(new ItemSprite(this), Messages.titleCase(name()),
				Messages.get(this, "prompt", d.charges, d.maxCharges()),
				Messages.get(this, "opt_glance", COST_GLANCE),
				Messages.get(this, "opt_scent", COST_SCENT),
				Messages.get(this, "opt_drain", COST_DRAIN),
				Messages.get(this, "opt_allure", COST_ALLURE)) {
			@Override
			protected boolean enabled(int index) {
				int[] costs = {COST_GLANCE, COST_SCENT, COST_DRAIN, COST_ALLURE};
				return d.charges >= costs[index];
			}

			@Override
			protected void onSelect(int index) {
				switch (index) {
					case 0: GameScene.selectCell(glance); break;
					case 1: scent(hero, d); break;
					case 2: GameScene.selectCell(drainSel); break;
					case 3: GameScene.selectCell(allure); break;
				}
			}
		});
	}

	// ------------------------------------------------------------------ helpers

	private static Mob targetAt(Integer cell, boolean mustBeCharmed) {
		if (cell == null) return null;
		Char ch = Actor.findChar(cell);
		if (!(ch instanceof Mob) || ch.alignment == Char.Alignment.ALLY || !Dungeon.level.heroFOV[cell]) {
			GLog.w(Messages.get(Perfume.class, "no_target"));
			return null;
		}
		if (mustBeCharmed && !LustDesire.charmedByHero(ch)) {
			GLog.w(Messages.get(Perfume.class, "not_charmed"));
			return null;
		}
		if (!mustBeCharmed && LustDesire.charmImmune(ch)) {
			GLog.w(Messages.get(Perfume.class, "immune"));
			return null;
		}
		return (Mob) ch;
	}

	/**
	 * Drains life from a charmed enemy without breaking the charm.
	 * Shared by the Drain skill and the Soul Siphon armor ability.
	 * Returns the amount of life drained.
	 */
	public static int drain(Hero hero, Mob mob, float mult, Object src) {
		LustDesire d = desire(hero);
		float dmg = Random.NormalIntRange(2 + hero.lvl / 2, 5 + hero.lvl) * mult;
		dmg *= 1f + 0.20f * hero.pointsInTalent(Talent.LUST_THIRST);

		boolean asleep = mob.state == mob.SLEEPING;
		if (hero.subClass == HeroSubClass.DREAMWEAVER && asleep) {
			dmg *= 1.5f + 0.25f * hero.pointsInTalent(Talent.DREAM_DEEP);
		}

		Charm c = mob.buff(Charm.class);
		if (c != null) c.ignoreNextHit = true;

		int amount = Math.min(mob.HP, Math.round(dmg));
		mob.damage(Math.round(dmg), src);
		if (mob.isAlive() && asleep && hero.subClass == HeroSubClass.DREAMWEAVER) {
			mob.state = mob.SLEEPING;
		}

		int heal = Math.round(amount * (1f + 0.20f * hero.pointsInTalent(Talent.LUST_SWEET)));
		int missing = hero.HT - hero.HP;
		hero.HP += Math.min(heal, missing);
		if (heal > 0) {
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(Math.min(heal, missing)), FloatingText.HEALING);
		}
		int over = heal - missing;
		int shield = hero.pointsInTalent(Talent.SIPHON_SHIELD);
		if (src instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility && over > 0 && shield > 0) {
			Buff.affect(hero, Barrier.class).incShield(Math.round(over * 0.25f * shield));
		}

		d.addDesire(amount * 0.3f);
		if (asleep && hero.subClass == HeroSubClass.DREAMWEAVER) {
			int rev = hero.pointsInTalent(Talent.DREAM_REVERIE);
			if (rev > 0) d.gainCharge(0.15f * rev);
		}

		CellEmitter.center(mob.pos).burst(Speck.factory(Speck.HEART), 3);
		return amount;
	}

	// ------------------------------------------------------------------ skills

	private final CellSelector.Listener glance = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero hero = Dungeon.hero;
			Mob mob = targetAt(cell, false);
			if (mob == null) return;
			if (!desire(hero).useCharges(COST_GLANCE)) return;

			LustDesire.charm(hero, mob, 8f);
			mob.sprite.emitter().burst(Speck.factory(Speck.HEART), 5);
			hero.sprite.zap(mob.pos);
			Sample.INSTANCE.play(Assets.Sounds.CHARMS);
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(Perfume.class, "prompt_glance");
		}
	};

	private static void scent(Hero hero, LustDesire d) {
		if (!d.useCharges(COST_SCENT)) return;
		int r = scentRadius(hero);
		int n = 0;
		for (Mob mob : new ArrayList<>(Dungeon.level.mobs)) {
			if (mob.alignment == Char.Alignment.ENEMY && Dungeon.level.heroFOV[mob.pos]
					&& Dungeon.level.distance(hero.pos, mob.pos) <= r && !LustDesire.charmImmune(mob)) {
				LustDesire.charm(hero, mob, 5f);
				mob.sprite.emitter().burst(Speck.factory(Speck.HEART), 3);
				n++;
			}
		}
		for (int i : PathFinder.NEIGHBOURS9) {
			if (Dungeon.level.heroFOV[hero.pos + i]) CellEmitter.get(hero.pos + i).burst(Speck.factory(Speck.HEART), 1);
		}
		if (n == 0) GLog.i(Messages.get(Perfume.class, "scent_none"));
		hero.sprite.operate(hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.CHARMS);
		hero.spendAndNext(1f);
	}

	private final CellSelector.Listener drainSel = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero hero = Dungeon.hero;
			Mob mob = targetAt(cell, true);
			if (mob == null) return;
			if (!desire(hero).useCharges(COST_DRAIN)) return;

			hero.sprite.zap(mob.pos);
			drain(hero, mob, 1f, Perfume.class);
			Sample.INSTANCE.play(Assets.Sounds.DRINK);
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(Perfume.class, "prompt_drain");
		}
	};

	private final CellSelector.Listener allure = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero hero = Dungeon.hero;
			Mob mob = targetAt(cell, true);
			if (mob == null) return;

			int best = -1;
			for (int n : PathFinder.NEIGHBOURS8) {
				int c = mob.pos + n;
				if (Dungeon.level.passable[c] && Actor.findChar(c) == null
						&& (best == -1 || Dungeon.level.trueDistance(hero.pos, c) < Dungeon.level.trueDistance(hero.pos, best))) {
					best = c;
				}
			}
			if (best == -1) {
				GLog.w(Messages.get(Perfume.class, "no_room"));
				return;
			}
			if (!desire(hero).useCharges(COST_ALLURE)) return;

			ScrollOfTeleportation.appear(hero, best);
			Dungeon.level.occupyCell(hero);
			Dungeon.observe();
			GameScene.updateFog();

			int embrace = hero.pointsInTalent(Talent.LUST_EMBRACE);
			if (embrace > 0) Buff.affect(hero, Barrier.class).setShield(Math.round(hero.HT * 0.10f * embrace));
			hero.sprite.emitter().burst(Speck.factory(Speck.HEART), 4);
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(Perfume.class, "prompt_allure");
		}
	};

	// ------------------------------------------------------------------ info

	@Override
	public String status() {
		LustDesire d = LustDesire.of();
		return d == null ? null : d.charges + "/" + d.maxCharges();
	}

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
