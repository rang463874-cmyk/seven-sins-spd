package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.predator;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PredatorStomach;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

/** Spew digested metal around the hero, hitting every visible enemy nearby. */
public class Regurgitate extends ArmorAbility {

	{
		baseChargeUse = 50f;
	}

	public static int radius(Hero hero) {
		return 2 + (hero.pointsInTalent(Talent.REGURG_RANGE) + 1) / 2;
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		PredatorStomach st = hero.buff(PredatorStomach.class);
		float total = st == null ? 0 : st.weaponPts + st.armorPts + st.trinketPts;
		int r = radius(hero);
		int acid = hero.pointsInTalent(Talent.REGURG_ACID);
		int disgust = hero.pointsInTalent(Talent.REGURG_DISGUST);

		ArrayList<Char> targets = new ArrayList<Char>();
		for (Char ch : Actor.chars()) {
			if (ch != hero && ch.alignment == Char.Alignment.ENEMY
					&& Dungeon.level.heroFOV[ch.pos]
					&& Dungeon.level.distance(hero.pos, ch.pos) <= r) {
				targets.add(ch);
			}
		}

		Splash.at(hero.pos, 0x88AA33, 20);
		for (Char ch : targets) {
			int dmg = Random.NormalIntRange(hero.lvl / 2 + 2, hero.lvl + 5) + Math.round(total * 0.10f);
			Splash.at(ch.pos, 0x88AA33, 8);
			ch.damage(dmg, this);
			if (ch.isAlive()) {
				if (acid > 0) Buff.affect(ch, Poison.class).set(3f * acid);
				if (disgust > 0) Buff.affect(ch, Weakness.class, 2f * disgust);
			}
		}

		hero.sprite.operate(hero.pos);
		Sample.INSTANCE.play("sounds/gas.mp3");
		Invisibility.dispel();
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.REGURGITATE;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.REGURG_RANGE, Talent.REGURG_ACID, Talent.REGURG_DISGUST, Talent.HEROIC_ENERGY};
	}
}
