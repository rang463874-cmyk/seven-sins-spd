package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.lust;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LustDesire;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Perfume;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

/** Drains the life of every visible charmed enemy at once. */
public class SoulSiphon extends ArmorAbility {

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

		float mult = 1.5f * (1f + 0.25f * hero.pointsInTalent(Talent.SIPHON_POWER));
		int extend = hero.pointsInTalent(Talent.SIPHON_CHARM);
		for (Mob mob : targets) {
			Perfume.drain(hero, mob, mult, this);
			if (mob.isAlive() && extend > 0) {
				Charm c = mob.buff(Charm.class);
				if (c != null) Buff.prolong(mob, Charm.class, c.cooldown() + 2f * extend);
			}
		}

		hero.sprite.operate(hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.DRINK);
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.SOUL_SIPHON;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.SIPHON_POWER, Talent.SIPHON_SHIELD, Talent.SIPHON_CHARM, Talent.HEROIC_ENERGY};
	}
}
