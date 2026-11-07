package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;

/**
 * Scales of Famine, the black horseman. The hungrier the hero, the faster it moves and attacks
 * and the harder it hits (full effect once hungry, a bit more while starving). The hero grows
 * hungry 10% faster. Its power burns the food in the belly down to hunger, healing from what
 * was burned, and doubles down on the famine's gifts for a while.
 */
public class FamineScales extends HorsemanArtifact {

	{
		image = ItemSpriteSheet.ARTIFACT_FAMINE;
	}

	@Override
	protected float rechargeTurns() {
		return 150f - 6f * level();
	}

	public float attackSpeedBonus() {
		return 0.30f + 0.03f * level();
	}

	public float moveSpeedBonus() {
		return 0.20f + 0.02f * level();
	}

	public float damageBonus() {
		return 0.10f + 0.02f * level();
	}

	/** heal from a full stomach burned down, as a share of max HP */
	public float healPercent() {
		return 0.25f + 0.02f * level();
	}

	public float frenzyTurns() {
		return 10f + level();
	}

	/** 0 when full, 1 when hungry, 1.25 starving, 1.5 in a frenzy */
	public static float hungerFactor(Hero hero) {
		if (hero.buff(Frenzy.class) != null) return 1.5f;
		Hunger hunger = hero.buff(Hunger.class);
		if (hunger == null) return 0;
		if (hunger.isStarving()) return 1.25f;
		return Math.min(1f, hunger.hunger() / Hunger.HUNGRY);
	}

	public float attackSpeedMult(Hero hero) {
		return 1f + hungerFactor(hero) * attackSpeedBonus();
	}

	public float moveSpeedMult(Hero hero) {
		return 1f + hungerFactor(hero) * moveSpeedBonus();
	}

	public int modifyAttack(Hero hero, int dmg) {
		return Math.round(dmg * (1f + hungerFactor(hero) * damageBonus()));
	}

	@Override
	protected boolean usePower(Hero hero) {
		Hunger hunger = hero.buff(Hunger.class);
		if (hunger != null) {
			float burned = Math.max(0, Hunger.HUNGRY - hunger.hunger());
			if (burned > 0) {
				hunger.affectHunger(-burned);
				int heal = Math.min(hero.HT - hero.HP, Math.round(hero.HT * healPercent() * burned / Hunger.HUNGRY));
				if (heal > 0) {
					hero.HP += heal;
					hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
				}
			}
		}
		Buff.prolong(hero, Frenzy.class, frenzyTurns());
		hero.sprite.emitter().burst(Speck.factory(Speck.HEALING), 4);
		Sample.INSTANCE.play(Assets.Sounds.EAT);
		spendCharge(1);
		hero.spendAndNext(1f);
		return true;
	}

	@Override
	protected String statsDesc() {
		Hero h = com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;
		float f = h == null ? 0 : hungerFactor(h);
		return Messages.get(this, "stats",
				Math.round(attackSpeedBonus() * 100), Math.round(moveSpeedBonus() * 100),
				Math.round(damageBonus() * 100), Math.round(healPercent() * 100),
				Math.round(frenzyTurns()), Math.round(f * 100));
	}

	@Override
	protected ArtifactBuff passiveBuff() {
		return new Famine();
	}

	public class Famine extends HorsemanBuff {
		@Override
		protected void tick() {
			if (isCursed() || !(target instanceof Hero) || !target.isAlive()) return;
			//the scales demand their due: 10% faster hunger
			Hunger hunger = target.buff(Hunger.class);
			if (hunger != null && !hunger.isStarving()) hunger.affectHunger(-0.1f);
		}
	}

	/** the famine's frenzy: acts as if starving, and then some */
	public static class Frenzy extends FlavourBuff {

		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.HASTE;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.4f, 0.3f, 0.2f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, 1f - visualcooldown() / 20f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}
}
