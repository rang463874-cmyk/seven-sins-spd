package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.envy;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

/** Pushes the hero's misfortunes onto an enemy: timed debuffs move from the hero to the target. */
public class Transference extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		if (target == null) return;
		Char enemy = Actor.findChar(target);
		if (enemy == null || enemy == hero || enemy.alignment == Char.Alignment.ALLY || !Dungeon.level.heroFOV[target]) {
			GLog.w(Messages.get(this, "no_target"));
			return;
		}

		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		float amp = 1f + 0.25f * hero.pointsInTalent(Talent.TRANS_AMPLIFY);
		int moved = 0;
		for (Buff b : hero.buffs()) {
			if (b.type == Buff.buffType.NEGATIVE && !(b instanceof Hunger)) {
				if (b instanceof FlavourBuff) {
					try {
						Buff.prolong(enemy, ((FlavourBuff) b).getClass(), ((FlavourBuff) b).cooldown() * amp);
					} catch (Throwable t) {
						//some debuffs can't be given to others; they are still removed
					}
				}
				b.detach();
				moved++;
			}
		}

		int heal = hero.pointsInTalent(Talent.TRANS_HEAL);
		if (heal > 0) {
			hero.HP = Math.min(hero.HT, hero.HP + Math.round(hero.HT * 0.05f * heal));
			hero.sprite.emitter().burst(Speck.factory(Speck.HEALING), 3);
		}
		int weak = hero.pointsInTalent(Talent.TRANS_WEAKEN);
		if (weak > 0 && enemy.isAlive()) Buff.prolong(enemy, Weakness.class, 2f * weak);

		GLog.i(Messages.get(this, "moved", moved));
		hero.sprite.zap(target);
		enemy.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 5);
		Sample.INSTANCE.play(Assets.Sounds.CURSED);
		Invisibility.dispel();
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.TRANSFERENCE;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.TRANS_AMPLIFY, Talent.TRANS_HEAL, Talent.TRANS_WEAKEN, Talent.HEROIC_ENERGY};
	}
}
