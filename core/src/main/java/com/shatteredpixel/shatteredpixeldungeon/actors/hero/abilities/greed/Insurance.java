package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.greed;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GreedVault;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invulnerability;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

/** Takes out a policy: the next lethal blow within the term is survived. */
public class Insurance extends ArmorAbility {

	{
		baseChargeUse = 50f;
	}

	public static float term(Hero hero) {
		return 50f + 25f * hero.pointsInTalent(Talent.INS_TERM);
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();
		Buff.prolong(hero, GreedVault.Insurance.class, term(hero));
		hero.sprite.operate(hero.pos);
		hero.sprite.centerEmitter().burst(Speck.factory(Speck.COIN), 10);
		Sample.INSTANCE.play(Assets.Sounds.READ);
		hero.spendAndNext(1f);
	}

	/** Called from Hero.die. Returns true if the hero was saved. */
	public static boolean tryPayout(Hero hero) {
		GreedVault.Insurance pol = hero.buff(GreedVault.Insurance.class);
		if (pol == null) return false;
		pol.detach();
		hero.HP = Math.max(1, Math.round(hero.HT * (0.30f + 0.10f * hero.pointsInTalent(Talent.INS_COVERAGE))));
		int payout = hero.pointsInTalent(Talent.INS_PAYOUT);
		if (payout > 0) Buff.prolong(hero, Invulnerability.class, 1f + payout);
		hero.sprite.emitter().burst(Speck.factory(Speck.HEALING), 6);
		GameScene.flash(0x80FFDD44);
		Sample.INSTANCE.play(Assets.Sounds.GOLD);
		GLog.p(Messages.get(Insurance.class, "payout"));
		return true;
	}

	@Override
	public int icon() {
		return HeroIcon.INSURANCE;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.INS_COVERAGE, Talent.INS_TERM, Talent.INS_PAYOUT, Talent.HEROIC_ENERGY};
	}
}
