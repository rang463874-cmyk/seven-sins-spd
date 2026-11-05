package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.envy;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnvyCovet;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.EnvyMirror;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

/** Drags every visible enemy that is healthier than the hero down toward the hero's level. */
public class Downfall extends ArmorAbility {

	{
		baseChargeUse = 50f;
	}

	public static int reach(Hero hero) {
		return 4 + 2 * hero.pointsInTalent(Talent.FALL_REACH);
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		int r = reach(hero);
		float mult = 0.5f * (1f + 0.25f * hero.pointsInTalent(Talent.FALL_POWER));
		int rob = hero.pointsInTalent(Talent.FALL_ROB);

		for (Mob mob : new ArrayList<>(Dungeon.level.mobs)) {
			if (mob.alignment == Char.Alignment.ENEMY && Dungeon.level.heroFOV[mob.pos]
					&& Dungeon.level.distance(hero.pos, mob.pos) <= r) {
				EnvyMirror.dragDown(hero, mob, mult, this);
				if (rob > 0 && mob.isAlive()) EnvyCovet.rob(hero, mob, 2 * rob);
				mob.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 3);
			}
		}

		hero.sprite.operate(hero.pos);
		GameScene.flash(0x4033CC33);
		Sample.INSTANCE.play(Assets.Sounds.BLAST);
		Invisibility.dispel();
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.DOWNFALL;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.FALL_POWER, Talent.FALL_REACH, Talent.FALL_ROB, Talent.HEROIC_ENERGY};
	}
}
