package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.pride;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PrideGlory;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

/** Every visible lesser being kneels before the hero. */
public class Reign extends ArmorAbility {

	{
		baseChargeUse = 50f;
	}

	public static int reach(Hero hero) {
		return 4 + 2 * hero.pointsInTalent(Talent.REIGN_REACH);
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		int r = reach(hero);
		float dur = 3f + hero.pointsInTalent(Talent.REIGN_DURATION);
		int humble = hero.pointsInTalent(Talent.REIGN_HUMBLE);

		for (Mob mob : new ArrayList<>(Dungeon.level.mobs)) {
			if (mob.alignment == Char.Alignment.ENEMY
					&& Dungeon.level.heroFOV[mob.pos]
					&& Dungeon.level.distance(hero.pos, mob.pos) <= r
					&& !PrideGlory.royalImmune(mob)) {
				Buff.prolong(mob, Paralysis.class, dur);
				if (humble > 0) Buff.prolong(mob, Vulnerable.class, 2f * humble);
				mob.sprite.emitter().burst(Speck.factory(Speck.STAR), 4);
			}
		}

		hero.sprite.operate(hero.pos);
		new Flare(8, 48).color(0xCC88FF, true).show(hero.sprite, 2f);
		GameScene.flash(0x40CC88FF);
		Sample.INSTANCE.play(Assets.Sounds.READ);
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.REIGN;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.REIGN_REACH, Talent.REIGN_DURATION, Talent.REIGN_HUMBLE, Talent.HEROIC_ENERGY};
	}
}
