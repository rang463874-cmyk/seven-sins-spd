package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

/**
 * Scales of Famine, the black horseman. Enemies near the hero are weakened (deal 33% less damage)
 * while the hero grows hungry a little faster; its power turns food in the belly into health.
 */
public class FamineScales extends HorsemanArtifact {

	public static final int HUNGER_COST = 120;

	{
		image = ItemSpriteSheet.ARTIFACT_FAMINE;
	}

	@Override
	protected float rechargeTurns() {
		return 60f - 2f * level();
	}

	public int auraRadius() {
		return 1 + level() / 4;
	}

	public float healPercent() {
		return 0.15f + 0.015f * level();
	}

	@Override
	protected boolean usePower(Hero hero) {
		Hunger hunger = hero.buff(Hunger.class);
		if (hunger == null || hunger.hunger() >= Hunger.HUNGRY) {
			GLog.w(Messages.get(this, "too_hungry"));
			return false;
		}
		hunger.affectHunger(-HUNGER_COST);
		int heal = Math.min(hero.HT - hero.HP, Math.round(hero.HT * healPercent()));
		if (heal > 0) {
			hero.HP += heal;
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
		}
		hero.sprite.emitter().burst(Speck.factory(Speck.HEALING), 4);
		Sample.INSTANCE.play(Assets.Sounds.EAT);
		spendCharge(1);
		hero.spendAndNext(1f);
		return true;
	}

	@Override
	protected String statsDesc() {
		return Messages.get(this, "stats", auraRadius(), Math.round(healPercent() * 100), HUNGER_COST);
	}

	@Override
	protected ArtifactBuff passiveBuff() {
		return new Famine();
	}

	public class Famine extends HorsemanBuff {
		@Override
		protected void tick() {
			if (isCursed() || !(target instanceof Hero) || !target.isAlive()) return;
			int r = auraRadius();
			for (Char ch : Actor.chars()) {
				if (ch.alignment == Char.Alignment.ENEMY && ch.isAlive()
						&& Dungeon.level.heroFOV[ch.pos]
						&& Dungeon.level.distance(target.pos, ch.pos) <= r) {
					Buff.prolong(ch, Weakness.class, 2f);
				}
			}
			//the scales demand their due: 10% faster hunger
			Hunger hunger = target.buff(Hunger.class);
			if (hunger != null && !hunger.isStarving()) hunger.affectHunger(-0.1f);
		}
	}
}
