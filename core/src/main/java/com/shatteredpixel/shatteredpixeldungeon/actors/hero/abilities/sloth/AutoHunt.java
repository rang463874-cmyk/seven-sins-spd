package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.sloth;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SlothRepose;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DreamPuppet;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.sloth.SlothHooks;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;

/** The Sloth dozes into "auto mode": a free automatic attack on nearby enemies every turn. */
public class AutoHunt extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	public static float duration(Hero hero) {
		return 10f + 3f * hero.pointsInTalent(Talent.AUTO_DURATION);
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		Buff.prolong(hero, SlothRepose.AutoHunt.class, duration(hero));

		hero.sprite.operate(hero.pos);
		hero.sprite.centerEmitter().burst(Speck.factory(Speck.NOTE), 5);
		Sample.INSTANCE.play(Assets.Sounds.LULLABY, 0.8f);
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.AUTO_HUNT;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.AUTO_DURATION, Talent.AUTO_POWER, Talent.AUTO_REACH, Talent.HEROIC_ENERGY};
	}
}
