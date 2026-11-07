package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.predator;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PredatorStomach;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

/** Lunge at a nearby enemy with the maw. Kills feed the stomach. */
public class GorgeBite extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	public static int range(Hero hero) {
		return 2 + hero.pointsInTalent(Talent.BITE_REACH);
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	public int targetedPos(Char user, int dst) {
		return new Ballistica(user.pos, dst, 7).collisionPos;
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		if (target == null) return;
		if (target == hero.pos) {
			GLog.w(Messages.get(this, "self_target"));
			return;
		}
		Ballistica path = new Ballistica(hero.pos, target, 7);
		int cell = path.collisionPos;
		Char enemy = Actor.findChar(cell);
		if (enemy == null || enemy == hero || enemy.alignment == Char.Alignment.ALLY) {
			GLog.w(Messages.get(this, "no_target"));
			return;
		}
		if (Dungeon.level.distance(hero.pos, cell) > range(hero)) {
			GLog.w(Messages.get(this, "too_far"));
			return;
		}

		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		PredatorStomach st = hero.buff(PredatorStomach.class);
		int bonus = st == null ? 0 : st.meleeBonus() * 2;
		int dmg = Random.NormalIntRange(hero.lvl, 2 * hero.lvl + 4) + bonus;

		hero.sprite.operate(hero.pos);
		Splash.at(cell, 0xCC2244, 10);
		enemy.sprite.emitter().burst(Speck.factory(Speck.BONE), 5);
		Sample.INSTANCE.play("sounds/hit_strong.mp3");
		enemy.damage(dmg, this);

		int tear = hero.pointsInTalent(Talent.BITE_TEAR);
		if (tear > 0 && enemy.isAlive()) {
			Buff.affect(enemy, Bleeding.class).set(dmg * 0.10f * tear);
		}
		if (!enemy.isAlive()) {
			PredatorStomach s = Buff.affect(hero, PredatorStomach.class);
			float pts = 2 + hero.pointsInTalent(Talent.BITE_DIGEST);
			s.addPoints(PredatorStomach.CAT_FOOD, pts);
			int heal = Math.min(Math.round(hero.HT * 0.15f), hero.HT - hero.HP);
			if (heal > 0) {
				hero.HP += heal;
				hero.sprite.emitter().burst(Speck.factory(Speck.HEALING), 3);
			}
			hero.sprite.showStatus(CharSprite.POSITIVE, Messages.get(this, "devoured", PredatorStomach.fmt(pts)));
		}

		Invisibility.dispel();
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.GORGE_BITE;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.BITE_REACH, Talent.BITE_DIGEST, Talent.BITE_TEAR, Talent.HEROIC_ENERGY};
	}
}
