package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.sloth;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
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

/** Every visible enemy nearby falls into a deep, magical sleep. */
public class EternalSlumber extends ArmorAbility {

	{
		baseChargeUse = 50f;
	}

	public static int reach(Hero hero) {
		return 4 + 2 * hero.pointsInTalent(Talent.SLUMBER_REACH);
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		int r = reach(hero);
		int deep = hero.pointsInTalent(Talent.SLUMBER_DEEP);
		int rest = hero.pointsInTalent(Talent.SLUMBER_REST);
		int slept = 0;

		for (Mob mob : new ArrayList<>(Dungeon.level.mobs)) {
			if (mob.alignment == Char.Alignment.ENEMY && Dungeon.level.heroFOV[mob.pos]
					&& Dungeon.level.distance(hero.pos, mob.pos) <= r
					&& !mob.properties().contains(Char.Property.BOSS)
					&& !mob.properties().contains(Char.Property.MINIBOSS)) {
				if (Buff.affect(mob, MagicalSleep.class) != null) {
					if (deep > 0) Buff.prolong(mob, Paralysis.class, 2f * deep);
					mob.sprite.emitter().burst(Speck.factory(Speck.NOTE), 4);
					slept++;
				}
			}
		}
		if (rest > 0 && slept > 0) {
			hero.HP = Math.min(hero.HT, hero.HP + Math.round(hero.HT * 0.05f * rest * slept));
			hero.sprite.emitter().burst(Speck.factory(Speck.HEALING), 3);
		}

		hero.sprite.operate(hero.pos);
		GameScene.flash(0x403366FF);
		Sample.INSTANCE.play(Assets.Sounds.LULLABY);
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.ETERNAL_SLUMBER;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.SLUMBER_REACH, Talent.SLUMBER_DEEP, Talent.SLUMBER_REST, Talent.HEROIC_ENERGY};
	}
}
