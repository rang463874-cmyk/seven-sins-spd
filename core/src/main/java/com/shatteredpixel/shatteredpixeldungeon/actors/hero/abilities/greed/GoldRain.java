package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.greed;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GreedVault;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

/** Showers every visible enemy nearby with gold coins. Scales with credit. */
public class GoldRain extends ArmorAbility {

	{
		baseChargeUse = 50f;
	}

	public static int radius(Hero hero) {
		return 2 + (hero.pointsInTalent(Talent.RAIN_RADIUS) + 1) / 2;
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		GreedVault v = hero.buff(GreedVault.class);
		float credit = v == null ? 0 : v.creditPts();
		int r = radius(hero);
		int dazzle = hero.pointsInTalent(Talent.RAIN_DAZZLE);
		int refund = hero.pointsInTalent(Talent.RAIN_REFUND);

		ArrayList<Char> targets = new ArrayList<>();
		for (Char ch : Actor.chars()) {
			if (ch != hero && ch.alignment == Char.Alignment.ENEMY
					&& Dungeon.level.heroFOV[ch.pos]
					&& Dungeon.level.distance(hero.pos, ch.pos) <= r) {
				targets.add(ch);
			}
		}

		for (Char ch : targets) {
			int dmg = Random.NormalIntRange(hero.lvl / 2 + 2, hero.lvl + 6) + Math.round(credit * 0.3f);
			CellEmitter.get(ch.pos).burst(Speck.factory(Speck.COIN), 6);
			ch.damage(dmg, this);
			if (ch.isAlive() && dazzle > 0) Buff.prolong(ch, Blindness.class, 2f * dazzle);
			if (v != null && refund > 0) v.addBalance(5L * refund * Math.max(1, Dungeon.depth / 5 + 1));
		}

		hero.sprite.operate(hero.pos);
		CellEmitter.center(hero.pos).burst(Speck.factory(Speck.COIN), 12);
		Sample.INSTANCE.play(Assets.Sounds.GOLD);
		Invisibility.dispel();
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.GOLD_RAIN;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.RAIN_RADIUS, Talent.RAIN_DAZZLE, Talent.RAIN_REFUND, Talent.HEROIC_ENERGY};
	}
}
