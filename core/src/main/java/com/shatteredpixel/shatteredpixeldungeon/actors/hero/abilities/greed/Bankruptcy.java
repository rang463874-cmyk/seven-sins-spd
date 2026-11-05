package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.greed;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GreedVault;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
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

/** Declares every visible enemy bankrupt. Debtors take damage based on what they owe. */
public class Bankruptcy extends ArmorAbility {

	{
		baseChargeUse = 50f;
	}

	public static int reach(Hero hero) {
		return 4 + 2 * hero.pointsInTalent(Talent.BANKRUPT_REACH);
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		int r = reach(hero);
		float debtMult = 0.5f + 0.25f * hero.pointsInTalent(Talent.BANKRUPT_SEIZE);
		int dread = hero.pointsInTalent(Talent.BANKRUPT_DREAD);

		ArrayList<Char> targets = new ArrayList<>();
		for (Char ch : Actor.chars()) {
			if (ch != hero && ch.alignment == Char.Alignment.ENEMY
					&& Dungeon.level.heroFOV[ch.pos]
					&& Dungeon.level.distance(hero.pos, ch.pos) <= r) {
				targets.add(ch);
			}
		}

		for (Char ch : targets) {
			int dmg = Random.NormalIntRange(hero.lvl, 2 * hero.lvl + 4);
			GreedVault.Debt debt = ch.buff(GreedVault.Debt.class);
			if (debt != null) dmg += Math.round(debt.principal * debtMult);
			CellEmitter.get(ch.pos).burst(Speck.factory(Speck.COIN), 4);
			ch.damage(dmg, this);
			if (ch.isAlive() && dread > 0) {
				Buff.prolong(ch, Terror.class, 2f * dread).object = hero.id();
			}
		}

		hero.sprite.operate(hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.BLAST);
		Invisibility.dispel();
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.BANKRUPTCY;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.BANKRUPT_REACH, Talent.BANKRUPT_SEIZE, Talent.BANKRUPT_DREAD, Talent.HEROIC_ENERGY};
	}
}
