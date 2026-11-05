package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.lust;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LustDesire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

/** Breaks every visible charmed enemy's heart: the longer the charm had left, the more it hurts. */
public class Heartbreak extends ArmorAbility {

	{
		baseChargeUse = 50f;
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		ArrayList<Mob> targets = new ArrayList<>();
		for (Mob mob : Dungeon.level.mobs) {
			if (Dungeon.level.heroFOV[mob.pos] && LustDesire.charmedByHero(mob)) targets.add(mob);
		}
		if (targets.isEmpty()) {
			GLog.w(Messages.get(this, "no_targets"));
			return;
		}

		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		float power = 1f + 0.25f * hero.pointsInTalent(Talent.BREAK_POWER);
		int fear = hero.pointsInTalent(Talent.BREAK_FEAR);
		int bonus = hero.pointsInTalent(Talent.BREAK_DESIRE);
		LustDesire d = hero.buff(LustDesire.class);

		for (Mob mob : targets) {
			Charm c = mob.buff(Charm.class);
			float left = c == null ? 0 : c.cooldown();
			if (c != null) c.detach();
			int dmg = Math.round((Random.NormalIntRange(hero.lvl, 2 * hero.lvl + 4) + 3f * left) * power);
			mob.sprite.emitter().burst(Speck.factory(Speck.HEART), 6);
			mob.damage(dmg, this);
			if (d != null && bonus > 0) d.addDesire(bonus);
			//the charm was broken first, so count the kill here
			if (d != null && !mob.isAlive()) d.addDesire(1f + mob.HT / 15f);
			if (mob.isAlive() && fear > 0) Buff.prolong(mob, Terror.class, 2f * fear).object = hero.id();
		}

		hero.sprite.operate(hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.SHATTER);
		Invisibility.dispel();
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.HEARTBREAK;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.BREAK_POWER, Talent.BREAK_FEAR, Talent.BREAK_DESIRE, Talent.HEROIC_ENERGY};
	}
}
