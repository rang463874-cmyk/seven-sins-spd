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

/** For a while, every blow aimed at the Sloth lands on its puppets instead. */
public class Proxy extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	public static float duration(Hero hero) {
		return 10f + 3f * hero.pointsInTalent(Talent.PROXY_DURATION);
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		Buff.prolong(hero, SlothRepose.ProxyGuard.class, duration(hero));

		int mend = hero.pointsInTalent(Talent.PROXY_MEND);
		if (mend > 0) {
			for (DreamPuppet p : DreamPuppet.active()) {
				p.HP = Math.min(p.HT, p.HP + Math.round(p.HT * 0.20f * mend));
				p.sprite.emitter().burst(Speck.factory(Speck.HEALING), 2);
			}
		}
		int guard = hero.pointsInTalent(Talent.PROXY_SHIELD);
		if (guard > 0) {
			int shield = Math.round(hero.HT * 0.05f * guard);
			Buff.affect(hero, Barrier.class).setShield(shield);
			hero.sprite.showStatus(CharSprite.POSITIVE, Integer.toString(shield));
		}

		hero.sprite.operate(hero.pos);
		hero.sprite.centerEmitter().burst(Speck.factory(Speck.NOTE), 5);
		Sample.INSTANCE.play(Assets.Sounds.LULLABY, 0.8f);
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.PROXY;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.PROXY_DURATION, Talent.PROXY_MEND, Talent.PROXY_SHIELD, Talent.HEROIC_ENERGY};
	}
}
