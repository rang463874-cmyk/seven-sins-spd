package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.envy;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnvyCovet;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;

import com.watabou.utils.Random;

import java.util.ArrayList;

/** Robs every visible enemy nearby at once. */
public class Plunder extends ArmorAbility {

	{
		baseChargeUse = 50f;
	}

	public static int reach(Hero hero) {
		return 4 + 2 * hero.pointsInTalent(Talent.PLUNDER_REACH);
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		int r = reach(hero);
		float jealousy = 5f + 2.5f * hero.pointsInTalent(Talent.PLUNDER_STACKS);
		int spoils = hero.pointsInTalent(Talent.PLUNDER_SPOILS);
		EnvyCovet c = hero.buff(EnvyCovet.class);

		for (Mob mob : new ArrayList<>(Dungeon.level.mobs)) {
			if (mob.alignment == Char.Alignment.ENEMY && Dungeon.level.heroFOV[mob.pos]
					&& Dungeon.level.distance(hero.pos, mob.pos) <= r) {
				mob.damage(Random.NormalIntRange(hero.lvl / 2 + 2, hero.lvl + 5), this);
				if (mob.isAlive()) Buff.prolong(mob, Weakness.class, 5f);
				if (c != null) c.addJealousy(jealousy);
				if (c != null && spoils > 0) c.addEnvy(0.5f * spoils);
				mob.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 3);
			}
		}

		hero.sprite.operate(hero.pos);
		GameScene.flash(0x4033CC33);
		Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
		Invisibility.dispel();
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.PLUNDER;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.PLUNDER_REACH, Talent.PLUNDER_STACKS, Talent.PLUNDER_SPOILS, Talent.HEROIC_ENERGY};
	}
}
