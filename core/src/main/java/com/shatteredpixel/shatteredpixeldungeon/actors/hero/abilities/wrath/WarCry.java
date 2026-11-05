package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.wrath;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WrathGrudge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.WarDrum;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

/** A furious roar that drives every visible enemy nearby into a rage. */
public class WarCry extends ArmorAbility {

	{
		baseChargeUse = 50f;
	}

	public static int reach(Hero hero) {
		return 4 + 2 * hero.pointsInTalent(Talent.CRY_REACH);
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		int r = reach(hero);
		float dur = 6f + 2f * hero.pointsInTalent(Talent.CRY_ENDURE);
		int fright = hero.pointsInTalent(Talent.CRY_FRIGHT);

		for (Mob mob : new ArrayList<>(Dungeon.level.mobs)) {
			if (mob.alignment == com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY
					&& Dungeon.level.heroFOV[mob.pos]
					&& Dungeon.level.distance(hero.pos, mob.pos) <= r
					&& !WrathGrudge.immune(mob)) {
				WarDrum.enrage(hero, mob, dur);
				if (fright > 0) Buff.prolong(mob, Weakness.class, 2f * fright);
				mob.sprite.emitter().burst(Speck.factory(Speck.SCREAM), 2);
			}
		}

		hero.sprite.operate(hero.pos);
		hero.sprite.centerEmitter().start(Speck.factory(Speck.SCREAM), 0.3f, 3);
		GameScene.flash(0x40FF2200);
		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
		Invisibility.dispel();
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.WAR_CRY;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.CRY_REACH, Talent.CRY_ENDURE, Talent.CRY_FRIGHT, Talent.HEROIC_ENERGY};
	}
}
