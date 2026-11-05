package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.lust;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LustDesire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
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

/** A dance that charms every visible enemy nearby. */
public class EnchantingDance extends ArmorAbility {

	{
		baseChargeUse = 50f;
	}

	public static int reach(Hero hero) {
		return 4 + 2 * hero.pointsInTalent(Talent.DANCE_REACH);
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		int r = reach(hero);
		float dur = 6f + 2f * hero.pointsInTalent(Talent.DANCE_ENCORE);
		int daze = hero.pointsInTalent(Talent.DANCE_DAZE);

		for (Mob mob : new ArrayList<>(Dungeon.level.mobs)) {
			if (mob.alignment == Char.Alignment.ENEMY && Dungeon.level.heroFOV[mob.pos]
					&& Dungeon.level.distance(hero.pos, mob.pos) <= r && !LustDesire.charmImmune(mob)) {
				LustDesire.charm(hero, mob, dur);
				if (daze > 0) Buff.prolong(mob, Vertigo.class, 2f * daze);
				mob.sprite.emitter().burst(Speck.factory(Speck.HEART), 4);
			}
		}

		hero.sprite.operate(hero.pos);
		hero.sprite.centerEmitter().start(Speck.factory(Speck.HEART), 0.2f, 5);
		GameScene.flash(0x40FF66AA);
		Sample.INSTANCE.play(Assets.Sounds.CHARMS);
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.ENCHANTING_DANCE;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.DANCE_REACH, Talent.DANCE_ENCORE, Talent.DANCE_DAZE, Talent.HEROIC_ENERGY};
	}
}
