package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.sloth;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
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

import java.util.ArrayList;

/** The world slows down to the hero's pace: every visible enemy nearby is slowed. */
public class Stagnation extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	public static int reach(Hero hero) {
		return 4 + 2 * hero.pointsInTalent(Talent.STAG_REACH);
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		int r = reach(hero);
		float dur = 6f + 2f * hero.pointsInTalent(Talent.STAG_DURATION);
		int cripple = hero.pointsInTalent(Talent.STAG_CRIPPLE);

		for (Mob mob : new ArrayList<>(Dungeon.level.mobs)) {
			if (mob.alignment == Char.Alignment.ENEMY && Dungeon.level.heroFOV[mob.pos]
					&& Dungeon.level.distance(hero.pos, mob.pos) <= r) {
				Buff.prolong(mob, Slow.class, dur);
				if (cripple > 0) Buff.prolong(mob, Cripple.class, 2f * cripple);
				mob.sprite.emitter().burst(Speck.factory(Speck.STEAM), 3);
			}
		}

		hero.sprite.operate(hero.pos);
		GameScene.flash(0x30668899);
		Sample.INSTANCE.play(Assets.Sounds.TELEPORT, 0.6f, 0.6f);
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.STAGNATION;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.STAG_REACH, Talent.STAG_DURATION, Talent.STAG_CRIPPLE, Talent.HEROIC_ENERGY};
	}
}
