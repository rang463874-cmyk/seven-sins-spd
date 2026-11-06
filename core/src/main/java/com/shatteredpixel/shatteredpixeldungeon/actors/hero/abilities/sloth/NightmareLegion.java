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

/** Summons a pack of temporary nightmares that fight (and can taunt) for the Sloth. */
public class NightmareLegion extends ArmorAbility {

	{
		baseChargeUse = 50f;
	}

	public static int count(Hero hero) {
		return 2 + (hero.pointsInTalent(Talent.LEGION_COUNT) + 1) / 2;
	}

	public static float lifespan(Hero hero) {
		return 20f + 5f * hero.pointsInTalent(Talent.LEGION_DURATION);
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		int taunt = hero.pointsInTalent(Talent.LEGION_TAUNT);
		for (int i = 0; i < count(hero); i++) {
			DreamPuppet p = DreamPuppet.create(true, lifespan(hero));
			if (!SlothHooks.spawnNear(hero.pos, p)) break;
			if (taunt > 0) SlothRepose.Taunted.applyAround(p, 3, 2f * taunt);
		}

		hero.sprite.operate(hero.pos);
		hero.sprite.centerEmitter().burst(Speck.factory(Speck.NOTE), 8);
		Sample.INSTANCE.play(Assets.Sounds.LULLABY);
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.NIGHTMARE_LEGION;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.LEGION_COUNT, Talent.LEGION_DURATION, Talent.LEGION_TAUNT, Talent.HEROIC_ENERGY};
	}
}
