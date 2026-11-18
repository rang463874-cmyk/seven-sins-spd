package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.solomon;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MindVision;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;

/**
 * Seven Sins: Solomon's Foresight (Nov 18 2026, owner: from his sight of the future). For a few turns Solomon has
 * already seen every blow coming: absolute evasion (Hero.defenseSkill), and each dodge feeds mana (SIGHT_ECHO).
 */
public class Foresight extends ArmorAbility {

	{
		baseChargeUse = 50f;
	}

	public static float duration(Hero hero) {
		return 3f + hero.pointsInTalent(Talent.SIGHT_LENGTH);
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		Buff.affect(hero, Vision.class, duration(hero));
		int prophecy = hero.pointsInTalent(Talent.SIGHT_PROPHECY);
		if (prophecy > 0) {
			Buff.affect(hero, MindVision.class, 3f * prophecy);
			Dungeon.observe();
		}

		hero.sprite.operate(hero.pos);
		hero.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 10);
		Sample.INSTANCE.play(Assets.Sounds.TELEPORT, 1f, 1.4f);
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.FORESIGHT;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.SIGHT_LENGTH, Talent.SIGHT_ECHO, Talent.SIGHT_PROPHECY, Talent.HEROIC_ENERGY};
	}

	/** absolute evasion while it lasts */
	public static class Vision extends FlavourBuff {

		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.FORESIGHT;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.8f, 0.6f, 1f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (7f - visualcooldown()) / 7f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}
}
